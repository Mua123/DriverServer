package com.pioteks.usr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pioteks.handler.NBServerHandler;
import com.pioteks.model.JsonTemp;
import com.pioteks.utils.WebServerSessionInstance;

public class ClientJSConsumer implements Runnable {
	public final static String UpCoverOpened = "Up Cover Opened";
	public final static String DownCoverOpened = "Down Cover Opened";
	public final static String UpCoverClosed = "Up Cover Closed";
	public final static String DownCoverClosed = "Down Cover Closed";
	public final static String Vibrating = "Vibrating";
	public final static String VibCancel = "VibCancel";
	public final static String Lock = "Lock";
	public final static String Unlock = "Unlock";
	public final static String SULALarm = "SUL Alarm";
	public final static String SULSafe = "SUL Safe";
	public final static String CHALarm = "CH Alarm";
	public final static String CHSafe = "CH Safe";
	public final static String COALarm = "CO Alarm";
	public final static String COSafe = "CO Safe";
	
	private final static String[] dataName = {"vibration","upCover","downCover","lock",
			"SUL","CH","CO","battery","distance","CH4Value","SValue","moisture","temperature"};
	
	public static Map<Integer, JsonTemp> jsonMap = new HashMap<>();

	private BlockingQueue<byte[]> queue;//消息队列
	
	private final Logger logger = (Logger) LoggerFactory.getLogger(getClass());
	
	private boolean isRunning = true;
	
	
	public ClientJSConsumer(BlockingQueue<byte[]> queue) {
		super();
		this.queue = queue;
	}



	@Override
	public void run() {
		System.out.println("启动杰狮消费者线程！");
		
        isRunning = true;
        try {
            while (isRunning) {
                System.out.println("杰狮队列长度" + queue.size());
                logger.info("JS size: " + queue.size());
                
                //如果当前队列为空， 则检查map中是否有未发送的数据
                if(queue.size() == 0) {
                	long currentTime = System.currentTimeMillis();
                	Iterator<Map.Entry<Integer, JsonTemp>> it = jsonMap.entrySet().iterator();
                	while(it.hasNext()) {
                		Map.Entry<Integer, JsonTemp> entry = it.next();
                		//如果时间超过3分钟，则直接发送
                		if(currentTime - entry.getValue().getTime() >= 3*60*1000 && entry.getValue().getTime() != 0) {
                			checkJson(entry.getValue().getJson());
                			JSONObject jsonObject = new JSONObject();
                			jsonObject.put("id", entry.getKey() + "");
                			jsonObject.put("dataType", 0);
            				jsonObject.put("data", entry.getValue().getJson());
            				//暂时屏蔽
            				sendToJS(jsonObject.toString());
            				it.remove();
                		}
                	}
                }
                byte[] data = queue.take();
                System.out.println("杰狮消费消息" + NBServerHandler.byteToStr(data));
                logger.info("JS send" +NBServerHandler.byteToStr(data));
                send(data);
            }
        }catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
            System.out.println("退出杰狮消费者线程！");
        }
	}
	
	/**
	 * 检查json数据是否完整，不完整则以-1填充
	 * @param json
	 */
	private void checkJson(JSONObject json) {
		for(String key: dataName) {
			if(!json.has(key)) {
				json.put(key, -1);
			}
		}
	}



	public void send(byte[] data) throws InterruptedException {
		List<String> messageList = new ArrayList<String>();
		if(data.length != 8) {
			return;
		}
		byte start = data[0];
		byte[] manholeNumber = new byte[2];
		System.arraycopy(data, 1, manholeNumber, 0, 2);
	    int manholeNumberValue = (int) ( ((manholeNumber[0] & 0xFF)<<8)|((manholeNumber[1] & 0xFF)));  

	    messageList.add("No"+manholeNumberValue);
	    
		byte command = data[3];
		byte[] dataArr = new byte[2];
		System.arraycopy(data, 4, dataArr, 0, 2);
		byte check = data[6];
		byte end = data[7];
		
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("id", manholeNumberValue + "");
		if(command == 0) {
			//暂时屏蔽部分编号
			if(manholeNumberValue == 0xA104) {
				return;
			}
			jsonObject.put("dataType", 1);
			JSONObject dataJson = new JSONObject();
			if((dataArr[0] & 0x01) == 0x01) {
				messageList.add(Vibrating);
				dataJson.put("vibration", 1);
			}else {
				messageList.add(VibCancel);
				dataJson.put("vibration", 0);
			}
			if((dataArr[0] & 0x02) == 0x02) {
				messageList.add(DownCoverOpened);
				dataJson.put("downCover", 1);
			}else {
				messageList.add(DownCoverClosed);
				dataJson.put("downCover", 0);
			}
			if((dataArr[0] & 0x04) == 0x04) {
				messageList.add(UpCoverOpened);
				dataJson.put("upCover", 1);
			}else {
				messageList.add(UpCoverClosed);
				dataJson.put("upCover", 0);
			}
			if((dataArr[0] & 0x08) == 0x08) {
				messageList.add(Unlock);
				dataJson.put("lock", 1);
			}else {
				messageList.add(Lock);
				dataJson.put("lock", 0);
			}
			if((dataArr[0] & 0x10) == 0x10) {
				messageList.add(SULALarm);
				dataJson.put("SUL", 1);
			}else {
				messageList.add(SULSafe);
				dataJson.put("SUL", 0);
			}
			if((dataArr[0] & 0x20) == 0x20) {
				messageList.add(CHALarm);
				messageList.add(COALarm);
				dataJson.put("CH", 1);
				dataJson.put("CO", 1);
			}else {
				messageList.add(CHSafe);
				messageList.add(COSafe);
				dataJson.put("CH", 0);
				dataJson.put("CO", 0);
			}
			checkJson(dataJson);
			jsonObject.put("data", dataJson);
		}else if(command == 5) {
			//如果存在则取出之前的数据
			long now = System.currentTimeMillis();
			JsonTemp temp = jsonMap.get(manholeNumberValue);
			JSONObject dataJson = new JSONObject();
			if(temp != null) {
				if(now - temp.getTime() < 15000) {
					dataJson = temp.getJson();
				}
			}
			
			if((dataArr[0] & 0x01) == 0x01) {
				messageList.add(Vibrating);
				dataJson.put("vibration", 1);
			}else {
				messageList.add(VibCancel);
				dataJson.put("vibration", 0);
			}
			if((dataArr[0] & 0x02) == 0x02) {
				messageList.add(DownCoverOpened);
				dataJson.put("downCover", 1);
			}else {
				messageList.add(DownCoverClosed);
				dataJson.put("downCover", 0);
			}
			if((dataArr[0] & 0x04) == 0x04) {
				messageList.add(UpCoverOpened);
				dataJson.put("upCover", 1);
			}else {
				messageList.add(UpCoverClosed);
				dataJson.put("upCover", 0);
			}
			if((dataArr[0] & 0x08) == 0x08) {
				messageList.add(Unlock);
				dataJson.put("lock", 1);
			}else {
				messageList.add(Lock);
				dataJson.put("lock", 0);
			}
			if((dataArr[0] & 0x10) == 0x10) {
				messageList.add(SULALarm);
				dataJson.put("SUL", 1);
			}else {
				messageList.add(SULSafe);
				dataJson.put("SUL", 0);
			}
			if((dataArr[0] & 0x20) == 0x20) {
				messageList.add(CHALarm);
				messageList.add(COALarm);
				dataJson.put("CH", 1);
				dataJson.put("CO", 1);
			}else {
				messageList.add(CHSafe);
				messageList.add(COSafe);
				dataJson.put("CH", 0);
				dataJson.put("CO", 0);
			}
			byte data6 = dataArr[1];
			int distanceValue = (int)(data6 &0x0F);
			int batteryValue = (int)(data6 >>> 4 &0x0F);
			messageList.add("Battery" + batteryValue);
			messageList.add("Distance" + distanceValue);
			dataJson.put("battery", batteryValue);
			dataJson.put("distance", distanceValue);
			
			if(temp == null || ((now - temp.getTime()) >= 15000 && temp.getTime() != 0)
					|| !temp.getJson().has("CH4Value") || !temp.getJson().has("moisture") ) {
				JsonTemp jsonTemp = new JsonTemp();
				jsonTemp.setJson(dataJson);
				jsonTemp.setTime(System.currentTimeMillis());
				jsonMap.put(manholeNumberValue, jsonTemp);
				return;
			}else {
				checkJson(dataJson);
				jsonObject.put("dataType", 0);
				jsonObject.put("data", dataJson);
			}
		}else if(command == 6) {
			//如果存在则取出之前的数据
			long now = System.currentTimeMillis();
			JsonTemp temp = jsonMap.get(manholeNumberValue);
			JSONObject dataJson = new JSONObject();
			if(temp != null) {
				if(now - temp.getTime() < 15000) {
					dataJson = temp.getJson();
				}
			}
			int ch4Value = dataArr[0] & 0xFF;
			int sValue = dataArr[1] & 0xFF;
			messageList.add("CH4Value" + ch4Value);
			dataJson.put("CH4Value", ch4Value);
			messageList.add("SValue" + sValue);
			dataJson.put("SValue", ch4Value);
			
			if(temp == null || ((now - temp.getTime()) >= 15000 && temp.getTime() != 0)
					|| !temp.getJson().has("vibration") || !temp.getJson().has("moisture")) {
				JsonTemp jsonTemp = new JsonTemp();
				jsonTemp.setJson(dataJson);
				jsonTemp.setTime(System.currentTimeMillis());
				jsonMap.put(manholeNumberValue, jsonTemp);
				return;
			}else {
				checkJson(dataJson);
				jsonObject.put("dataType", 0);
				jsonObject.put("data", dataJson);
			}
		}else if(command == 7) {
			//如果存在则取出之前的数据
			long now = System.currentTimeMillis();
			JsonTemp temp = jsonMap.get(manholeNumberValue);
			JSONObject dataJson = new JSONObject();
			if(temp != null) {
				if(now - temp.getTime() < 15000) {
					dataJson = temp.getJson();
				}
			}
			int moisture = (int)(dataArr[0] &0xFF);
			int temperature = (int)(dataArr[1] &0xFF);
			messageList.add("Moisture" + moisture);
			dataJson.put("moisture", moisture);
			messageList.add("Temperature" + temperature);
			dataJson.put("temperature", temperature);
			
			if(temp == null || ((now - temp.getTime()) >= 15000 && temp.getTime() != 0)
					|| !temp.getJson().has("CH4Value") || !temp.getJson().has("vibration")) {
				JsonTemp jsonTemp = new JsonTemp();
				jsonTemp.setJson(dataJson);
				jsonTemp.setTime(System.currentTimeMillis());
				jsonMap.put(manholeNumberValue, jsonTemp);
				return;
			}else {
				checkJson(dataJson);
				jsonObject.put("dataType", 0);
				jsonObject.put("data", dataJson);
			}
		}
		
		//暂时屏蔽
		sendToJS(jsonObject.toString());
		jsonMap.remove(manholeNumberValue);
		
//		for(int i = 0; i < messageList.size(); i++)
//		{
//			sendToWeb(messageList.get(i));
//			Thread.currentThread().sleep(500);
//		}
	}
	
    private void sendToJS(String message) {
    	logger.info("JS message:" + message);
    	System.out.println("JS message:" + message);
    	//杰狮接口
    	HashMap<String,String> map = new HashMap<String,String>();
    	map.put("data", message);
        String result = httpPostWithForm("http://116.228.152.154:28080/tyszSys/auto/nb/addNBAction.action", map);
        logger.info("JS result:" + result);
    	System.out.println("JS result:" + result);
    }
	
    private void sendToWeb(String message) {
        byte[] data = message.getBytes();
        IoBuffer buf = IoBuffer.wrap(data);
        
        WebServerSessionInstance instance = WebServerSessionInstance.getWebServerSessionInstance();
        IoSession session = instance.getWebServerSession();
        if(session != null && session.isConnected()) {						//判断session不为空且session处于连接状态
        	WriteFuture future = session.write(buf);
        	// 在100毫秒超时间内等待写完成
        	future.awaitUninterruptibly(100);
        	// The message has been written successfully
        	if( future.isWritten() ) {									//与web服务器处于连接状态
        		System.out.println("CoAP send to WebServer successfully");
        		logger.info("CoAP send to WebServer successfully");
        		logger.info("message:" + message);
        	}else{
        		System.out.println("CoAP send to WebServer failed");
        		logger.info("CoAP send to WebServer failed");
        	}
        }else {															//和web服务器连接断开
        	if(session == null) {										//TCP连接未建立
        		System.out.println("TCP never connects");
        		logger.error("TCP never connects");
        		logger.error("message:" + message);
        	}else if(!session.isConnected()) {							//TCP连接异常关闭
        		System.out.println("TCP disconnect");
        		logger.error("TCP disconnect");
        		logger.error("message:" + message);
        	}
        }
    }
    
    
    /**
     * 以form表单形式提交数据，发送post请求
     * @explain 
     *   1.请求头：httppost.setHeader("Content-Type","application/x-www-form-urlencoded")
     *   2.提交的数据格式：key1=value1&key2=value2...
     * @param url 请求地址
     * @param paramsMap 具体数据
     * @return 服务器返回数据
     */
    public static String httpPostWithForm(String url,Map<String, String> paramsMap){
        // 用于接收返回的结果
        String resultData ="";
         try {
                HttpPost post = new HttpPost(url);
                List<BasicNameValuePair> pairList = new ArrayList<BasicNameValuePair>();
                // 迭代Map-->取出key,value放到BasicNameValuePair对象中-->添加到list中
                for (String key : paramsMap.keySet()) {
                    pairList.add(new BasicNameValuePair(key, paramsMap.get(key)));
                }
                UrlEncodedFormEntity uefe = new UrlEncodedFormEntity(pairList, "utf-8");
                post.setEntity(uefe); 
                // 创建一个http客户端
                CloseableHttpClient httpClient = HttpClientBuilder.create().build();
                // 发送post请求
                HttpResponse response = httpClient.execute(post);
                
                resultData = EntityUtils.toString(response.getEntity(),"UTF-8");
                
            } catch (Exception e) {
                throw new RuntimeException("接口连接失败！");
            }
         return resultData;
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
