package com.pioteks.usr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pioteks.handler.NBServerHandler;
import com.pioteks.model.JsonTemp;

public class ClientGXConsumer implements Runnable {

	private BlockingQueue<byte[]> queue;//消息队列
	
	private final Logger logger = (Logger) LoggerFactory.getLogger(getClass());
	
	private boolean isRunning = true;

	public static Map<Integer, JsonTemp> jsonMap = new HashMap<>();
	public static Map<Integer, Boolean> bluetoothStatus = new HashMap<>();
	public static Map<Integer, Boolean> lockStatus = new HashMap<>();
	//管线服务序号
	public static int snAlarm = 0;
	public static int snAll = 0;
	
	public ClientGXConsumer(BlockingQueue<byte[]> queue) {
		super();
		this.queue = queue;
	}



	@Override
	public void run() {
		System.out.println("启动管线消费者线程！");
		
        isRunning = true;
        try {
            while (isRunning) {
            	System.out.println("管线队列长度" + queue.size());
                logger.info("GX size: " + queue.size());
                byte[] data = queue.take();
                System.out.println("管线消费消息" + NBServerHandler.byteToStr(data));
                logger.info("GX send" +NBServerHandler.byteToStr(data));
                send(data);
            }
        }catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
            System.out.println("退出管线消费者线程！");
        }
	}
	
	private void send(byte[] data) {
		
		if(data.length != 8) {
			return;
		}
		byte start = data[0];
		byte[] manholeNumber = new byte[2];
		System.arraycopy(data, 1, manholeNumber, 0, 2);
	    int manholeNumberValue = (int) ( ((manholeNumber[0] & 0xFF)<<8)|((manholeNumber[1] & 0xFF)));  

		byte command = data[3];
		byte[] dataArr = new byte[2];
		System.arraycopy(data, 4, dataArr, 0, 2);
		byte check = data[6];
		byte end = data[7];
		
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("hardwareid", manholeNumberValue + "");
//		jsonObject.put("r_addr", "FF");
		if(command == 0) {
			jsonObject.put("batterylevel", "FF");
			jsonObject.put("temp", "FF");
			jsonObject.put("humidity", "FF");
			
			jsonObject.put("sn", snAlarm + 1000 + "");
			snAlarm = (snAlarm + 1)%1000;
			
			if((dataArr[0] & 0x08) == 0x08) {
				jsonObject.put("ics", "1");
				if(!bluetoothStatus.containsKey(manholeNumberValue) || !bluetoothStatus.get(manholeNumberValue)) {
					jsonObject.put("mact", "0");
					jsonObject.put("r_type", "2");
				}else {
					jsonObject.put("mact", "1");
					jsonObject.put("r_type", "1");
				}
				if((dataArr[0] & 0x14) != 0x04 && (dataArr[0] & 0x14) != 0x14) {
					lockStatus.put(manholeNumberValue, true);
				}
			}else {
				jsonObject.put("ics", "0");
				if((dataArr[0] & 0x14) != 0x04 && (dataArr[0] & 0x14) != 0x14) {
					lockStatus.put(manholeNumberValue, false);
				}
			}
			if(!jsonObject.has("mact")) {
				if((dataArr[0] & 0x14) == 0x04) {
					jsonObject.put("mact", "1");
					jsonObject.put("r_type", "1");
					bluetoothStatus.put(manholeNumberValue,true);
					if(lockStatus.containsKey(manholeNumberValue)) {
						if(lockStatus.get(manholeNumberValue)) {
							jsonObject.put("ics", "1");
						}else {
							jsonObject.put("ics", "0");
						}
					}
				}else if((dataArr[0] & 0x14) == 0x14){
					jsonObject.put("mact", "0");
					jsonObject.put("r_type", "2");
					bluetoothStatus.put(manholeNumberValue,false);
					if(lockStatus.containsKey(manholeNumberValue)) {
						if(lockStatus.get(manholeNumberValue)) {
							jsonObject.put("ics", "1");
						}else {
							jsonObject.put("ics", "0");
						}
					}
				}else {
					if(bluetoothStatus.containsKey(manholeNumberValue) && bluetoothStatus.get(manholeNumberValue)) {
						jsonObject.put("mact", "1");
						jsonObject.put("r_type", "1");
					}else {
						jsonObject.put("mact", "0");
						jsonObject.put("r_type", "2");
					}
				}
			}
			if((dataArr[0] & 0x01) == 0x01) {
				jsonObject.put("vibrate", "1");
			}else {
				jsonObject.put("vibrate", "");
			}
//			//锁状态 1表示可操作 0表示不可操作
//			jsonObject.put("lockstatus", "0");
			if((dataArr[0] & 0x02) == 0x02) {
				jsonObject.put("inlight", "1");
			}else {
				jsonObject.put("inlight", "0");
			}

		}else if(command == 5) {
			long now = System.currentTimeMillis();
			JsonTemp temp = jsonMap.get(manholeNumberValue);
			if(temp != null) {
				if(now - temp.getTime() < 15000) {
					jsonObject = temp.getJson();
				}
			}
			byte data6 = dataArr[1];
			int batteryValue = (int)(data6 >>> 4 &0x0F);
			
			jsonObject.put("batterylevel", batteryValue+"");
//			jsonObject.put("temp", "");
//			jsonObject.put("humidity", "");
			jsonObject.put("r_type", "0");
			if((dataArr[0] & 0x04) == 0x04) {
				jsonObject.put("mact", "1");
			}else {
				jsonObject.put("mact", "0");
			}
			
			jsonObject.put("sn", snAll + 2000 + "");
			snAll = (snAll + 1)%1000;
			
			if((dataArr[0] & 0x08) == 0x08) {
				jsonObject.put("ics", "1");
			}else {
				jsonObject.put("ics", "0");
			}
			
			if((dataArr[0] & 0x01) == 0x01) {
				jsonObject.put("vibrate", "1");
			}else {
				jsonObject.put("vibrate", "");
			}
//			//锁状态 1表示可操作 0表示不可操作
//			jsonObject.put("lockstatus", "0");
			if((dataArr[0] & 0x02) == 0x02) {
				jsonObject.put("inlight", "1");
			}else {
				jsonObject.put("inlight", "0");
			}
			
			if(temp == null || ((now - temp.getTime()) >= 15000 && temp.getTime() != 0)) {
				JsonTemp jsonTemp = new JsonTemp();
				jsonTemp.setJson(jsonObject);
				jsonTemp.setTime(System.currentTimeMillis());
				jsonMap.put(manholeNumberValue, jsonTemp);
				return;
			}
		}else if(command == 6) {
			//无数据发送
			return;
		}else if(command == 7) {
			long now = System.currentTimeMillis();
			JsonTemp temp = jsonMap.get(manholeNumberValue);
			if(temp != null) {
				if(now - temp.getTime() < 15000) {
					jsonObject = temp.getJson();
				}
			}
			
//			jsonObject.put("batterylevel", "");
			int moisture = (int)(dataArr[0] &0xFF);
			int temperature = (int)(dataArr[1] &0xFF);
			
			jsonObject.put("temp", temperature+"");
			jsonObject.put("humidity", moisture+"");
			
//			jsonObject.put("sn", snAll + 2000 + "");
//			snAll = (snAll + 1)%1000;
			
//			jsonObject.put("r_type", "0");
//			jsonObject.put("ics", "");
//			jsonObject.put("ocs", "");
//			jsonObject.put("mact", "0");
//			jsonObject.put("vibrate", "");
//			jsonObject.put("lockstatus", "0");
//			jsonObject.put("inlight", "");
			if(temp == null || ((now - temp.getTime()) >= 15000 && temp.getTime() != 0)) {
				JsonTemp jsonTemp = new JsonTemp();
				jsonTemp.setJson(jsonObject);
				jsonTemp.setTime(System.currentTimeMillis());
				jsonMap.put(manholeNumberValue, jsonTemp);
				return;
			}
		}

		jsonObject.put("dt", "bluetooth manhole");
		JSONObject service = new JSONObject();
		service.put("serviceId", "serviceId");
		service.put("data", jsonObject);
		
		JSONObject jsonOutsize = new JSONObject();
		jsonOutsize.put("notifyType", "deviceDataChanged");
		jsonOutsize.put("gatewayId", "huaweigateway");
		jsonOutsize.put("deviceId", "deviceId");
		jsonOutsize.put("service", service);
		
		sendToGX(jsonOutsize.toString());
		jsonMap.remove(manholeNumberValue);
		
	}
	
    private void sendToGX(String message) {
    	logger.info("GX message:" + message);
    	System.out.println("GX message:" + message);
        String result = sendPost("http://140.207.38.66:8000/deviceDataChanged", message);
        logger.info("GX result:" + result);
    	System.out.println("GX result:" + result);
    }
    
    /**
     * 向指定 URL 发送POST方法的请求
     * 
     * @param url
     *            发送请求的 URL
     * @param param
     *            请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
     * @return 所代表远程资源的响应结果
     */
    public static String sendPost(String url, String param) {
        PrintWriter out = null;
        BufferedReader in = null;
        String result = "";
        try {
            URL realUrl = new URL(url);
            // 打开和URL之间的连接
            URLConnection conn = realUrl.openConnection();
            // 设置通用的请求属性
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("user-agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            // 发送POST请求必须设置如下两行
            conn.setDoOutput(true);
            conn.setDoInput(true);
            // 获取URLConnection对象对应的输出流
            out = new PrintWriter(conn.getOutputStream());
            // 发送请求参数
            out.print(param);
            // flush输出流的缓冲
            out.flush();
            // 定义BufferedReader输入流来读取URL的响应
            in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            System.out.println("发送 POST 请求出现异常！"+e);
            e.printStackTrace();
        }
        //使用finally块来关闭输出流、输入流
        finally{
            try{
                if(out!=null){
                    out.close();
                }
                if(in!=null){
                    in.close();
                }
            }
            catch(IOException ex){
                ex.printStackTrace();
            }
        }
        return result;
    }  



	public void end() {
		this.isRunning = false;
	}

}
