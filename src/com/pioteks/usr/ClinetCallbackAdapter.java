package com.pioteks.usr;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pioteks.handler.NBServerHandler;
import com.pioteks.model.JsonTemp;
import com.pioteks.model.RequestResponse;
import com.pioteks.utils.BytesHexString;
import com.pioteks.utils.CommandInstance;
import com.pioteks.utils.WebServerSessionInstance;

import cn.usr.UsrCloudMqttCallbackAdapter;

public class ClinetCallbackAdapter extends UsrCloudMqttCallbackAdapter {
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
	
	public static Map<Integer, JsonTemp> jsonMap = new HashMap<>();
	
//	public static JSONObject jsonTemp = null;
//	public static long time = 0;
	
	//���߷������
	public static int snAlarm = 0;
	public static int snAll = 0;

	private final Logger logger = (Logger) LoggerFactory.getLogger(getClass());
	
	private int count = 0;
	private String error = null;
	
	private ClientAdapter clientAdapter;
	
	public ClinetCallbackAdapter(ClientAdapter clientAdapter) {
		super();
		// ����ѭ����������  
		this.clientAdapter = clientAdapter;
		StringBuffer sbu = new StringBuffer();
		sbu.append((char) 69);sbu.append((char) 82);sbu.append((char) 82);sbu.append((char) 79);
		sbu.append((char) 82);
		error = sbu.toString();
	}

	/**
	 * ������Ӧ�ص�����
	 */
	@Override
	public void onConnectAck(int returnCode, String description) {
		// TODO Auto-generated method stub
		super.onConnectAck(returnCode, description);
		if(returnCode == 2) {
			clientAdapter.connect = true;
			System.out.println(description);
			logger.info(description);
		}
		if(returnCode == 3) {
			clientAdapter.connect = false;
			System.out.println(description);
			logger.info(description);
		}
	}

	/**
	 * ȡ��������Ӧ �ص�����
	 */
	@Override
	public void onDisSubscribeAck(int messageId, String clientId, String topics, int returnCode) {
		// TODO Auto-generated method stub
		super.onDisSubscribeAck(messageId, clientId, topics, returnCode);
	}

	/**
	 * ������Ӧ �ص�����
	 */
	@Override
	public void onSubscribeAck(int messageId, String clientId, String topics, int returnCode) {
		// TODO Auto-generated method stub
		super.onSubscribeAck(messageId, clientId, topics, returnCode);
	}

	/**
	 * ����֮��� �ص�������ͨ�� isSuccess��ʾ�����Ƿ�ɹ�
	 * 
	 */
	@Override
	public void onPublishDataAck(int messageId, String topic, boolean isSuccess) {
		// TODO Auto-generated method stub
		super.onPublishDataAck(messageId, topic, isSuccess);
		System.out.println("�������ݣ�"+isSuccess);
	}

	/**
	 * �������ͽ���Ļص�����
	 */
	@Override
	public void onPublishDataResult(int messageId, String topic) {
		// TODO Auto-generated method stub
		super.onPublishDataResult(messageId, topic);
	}

	/**
	 * �յ��豸���ݵĻص�����
	 */
	@Override
	public void onReceiveEvent(int messageId, String topic, byte[] data) {
		super.onReceiveEvent(messageId, topic, data);
		String[] arrs = topic.split("/");
		String devId = arrs[arrs.length-1];
		System.out.println(devId);
		System.out.println(BytesHexString.bytesToHexString(data));
		
		try {
			response(data);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	//16����ת�ַ���
	private String getContent(String contentHex) {
		String content = "";
		String[] contentHexs = contentHex.split("-");
		for(int i = 0; i < contentHexs.length; i++) {
			content  = content + (char)Integer.parseInt(contentHexs[i],16);
		}
		
		return content;
	}

	//byte����ת16����
	private String getContentHex(byte[] data) {
		String contentHex = "";
		for(int i = data.length - 1; i >= 0; i-- ) {
			String s = Integer.toHexString(data[i] & 0xFF);
			if(s.length() == 1) {
				s = "0" + s;
			}
			contentHex = "-" + s + contentHex;  
		}
		contentHex = contentHex.substring(1, contentHex.length());
		
		return contentHex.toUpperCase();
	}

	//�ַ���ת16����
	private static String strTo16(String s) {
	    String str = "";
	    for (int i = 0; i < s.length(); i++) {
	        int ch = (int) s.charAt(i);
	        String s4 = Integer.toHexString(ch);
	        str = str + "-" + s4;
	    }
	    str = str.substring(1);
	    return str;
	}
	
	//�ַ���תbyte 
	private static byte[] strTobyte(String s) {
		String[] sList = s.split("-");
		byte[] bytes = new byte[sList.length];
		for(int i = 0; i < sList.length; i++) {
			int iValue = Integer.parseInt(sList[i], 16);
			bytes[i] = (byte)iValue;
		}
		return bytes;
	}
	
    private void response(byte[] data) throws InterruptedException, IOException {
    	CommandInstance commandInstance = CommandInstance.getCommandInstance();
    	List<RequestResponse> commandList = null;
    	
    	if(commandInstance.getMode() == CommandInstance.ALARM) {
    		commandList = commandInstance.getCommandListMode1();
    	}else if(commandInstance.getMode() == CommandInstance.OPERATE) {
    		commandList = commandInstance.getCommandListMode2();
    	}
    	
		System.out.println("CoAP receive from NB :" + NBServerHandler.byteToStr(data));
		logger.info("CoAP receive from NB :" + NBServerHandler.byteToStr(data));
		
		
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
		
		//��ʨ����
		if(start == 0x41 && end == 0x5A) {
			if( manholeNumberValue < 40960 || manholeNumberValue > 45055) {
				//�����͸���ʨ
				return;
			}
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("id", manholeNumberValue + "");
			if(command == 0) {
				//��ʱ���β��ֱ��
				if(manholeNumberValue == 0xA104) {
					return;
				}
				jsonObject.put("dataType", "1");
				if((dataArr[0] & 0x01) == 0x01) {
					messageList.add(Vibrating);
					jsonObject.put("vibration", "1");
				}else {
					messageList.add(VibCancel);
					jsonObject.put("vibration", "0");
				}
				if((dataArr[0] & 0x02) == 0x02) {
					messageList.add(DownCoverOpened);
					jsonObject.put("downCover", "1");
				}else {
					messageList.add(DownCoverClosed);
					jsonObject.put("downCover", "0");
				}
				if((dataArr[0] & 0x04) == 0x04) {
					messageList.add(UpCoverOpened);
					jsonObject.put("upCover", "1");
				}else {
					messageList.add(UpCoverClosed);
					jsonObject.put("upCover", "0");
				}
				if((dataArr[0] & 0x08) == 0x08) {
					messageList.add(Unlock);
					jsonObject.put("lock", "1");
				}else {
					messageList.add(Lock);
					jsonObject.put("lock", "0");
				}
				if((dataArr[0] & 0x10) == 0x10) {
					messageList.add(SULALarm);
					jsonObject.put("SUL", "1");
				}else {
					messageList.add(SULSafe);
					jsonObject.put("SUL", "0");
				}
				if((dataArr[0] & 0x20) == 0x20) {
					messageList.add(CHALarm);
					messageList.add(COALarm);
					jsonObject.put("CH", "1");
					jsonObject.put("CO", "1");
				}else {
					messageList.add(CHSafe);
					messageList.add(COSafe);
					jsonObject.put("CH", "0");
					jsonObject.put("CO", "0");
				}
			}else if(command == 5) {
				//���������ȡ��֮ǰ������
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
					dataJson.put("vibration", "1");
				}else {
					messageList.add(VibCancel);
					dataJson.put("vibration", "0");
				}
				if((dataArr[0] & 0x02) == 0x02) {
					messageList.add(DownCoverOpened);
					dataJson.put("downCover", "1");
				}else {
					messageList.add(DownCoverClosed);
					dataJson.put("downCover", "0");
				}
				if((dataArr[0] & 0x04) == 0x04) {
					messageList.add(UpCoverOpened);
					dataJson.put("upCover", "1");
				}else {
					messageList.add(UpCoverClosed);
					dataJson.put("upCover", "0");
				}
				if((dataArr[0] & 0x08) == 0x08) {
					messageList.add(Unlock);
					dataJson.put("lock", "1");
				}else {
					messageList.add(Lock);
					dataJson.put("lock", "0");
				}
				if((dataArr[0] & 0x10) == 0x10) {
					messageList.add(SULALarm);
					dataJson.put("SUL", "1");
				}else {
					messageList.add(SULSafe);
					dataJson.put("SUL", "0");
				}
				if((dataArr[0] & 0x20) == 0x20) {
					messageList.add(CHALarm);
					messageList.add(COALarm);
					dataJson.put("CH", "1");
					dataJson.put("CO", "1");
				}else {
					messageList.add(CHSafe);
					messageList.add(COSafe);
					dataJson.put("CH", "0");
					dataJson.put("CO", "0");
				}
				byte data6 = dataArr[1];
				int distanceValue = (int)(data6 &0x0F);
				int batteryValue = (int)(data6 >>> 4 &0x0F);
				messageList.add("Battery" + batteryValue);
				messageList.add("Distance" + distanceValue);
				dataJson.put("battery", batteryValue + "");
				dataJson.put("distance", distanceValue + "");
				
				if(temp == null || ((now - temp.getTime()) >= 15000 && temp.getTime() != 0)
						|| temp.getJson().get("CH4Value") == null || temp.getJson().get("moisture") == null) {
					JsonTemp jsonTemp = new JsonTemp();
					jsonTemp.setJson(dataJson);
					jsonTemp.setTime(System.currentTimeMillis());
					jsonMap.put(manholeNumberValue, jsonTemp);
					return;
				}else {
					jsonObject.put("data", dataJson);
				}
			}else if(command == 6) {
				//���������ȡ��֮ǰ������
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
				dataJson.put("CH4Value", ch4Value + "");
				messageList.add("SValue" + sValue);
				dataJson.put("SValue", ch4Value + "");
				
				if(temp == null || ((now - temp.getTime()) >= 15000 && temp.getTime() != 0)
						|| temp.getJson().get("vibration") == null || temp.getJson().get("moisture") == null) {
					JsonTemp jsonTemp = new JsonTemp();
					jsonTemp.setJson(dataJson);
					jsonTemp.setTime(System.currentTimeMillis());
					jsonMap.put(manholeNumberValue, jsonTemp);
					return;
				}else {
					jsonObject.put("data", dataJson);
				}
			}else if(command == 7) {
				//���������ȡ��֮ǰ������
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
				dataJson.put("moisture", moisture + "");
				messageList.add("Temperature" + temperature);
				dataJson.put("temperature", temperature + "");
				
				if(temp == null || ((now - temp.getTime()) >= 15000 && temp.getTime() != 0)
						|| temp.getJson().get("CH4Value") == null || temp.getJson().get("vibration") == null) {
					JsonTemp jsonTemp = new JsonTemp();
					jsonTemp.setJson(dataJson);
					jsonTemp.setTime(System.currentTimeMillis());
					jsonMap.put(manholeNumberValue, jsonTemp);
					return;
				}else {
					jsonObject.put("data", dataJson);
				}
			}
			
			//��ʱ����
//			sendToJS(jsonObject.toString());
			jsonMap.remove(manholeNumberValue);
			
			for(int i = 0; i < messageList.size(); i++)
			{
				sendToWeb(messageList.get(i));
				Thread.currentThread().sleep(500);
			}
		}else 
		//���߷���
		if(start == 0x45 && end == 0x6F) {
			if( manholeNumberValue < 0xB000 || manholeNumberValue > 0xBFFF) {
				//�����͸�����
				return;
			}
			
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("hardwareid", manholeNumberValue + "");
//			jsonObject.put("r_addr", "FF");
			if(command == 0) {
				jsonObject.put("batterylevel", "FF");
				jsonObject.put("temp", "FF");
				jsonObject.put("humidity", "FF");
				
				jsonObject.put("sn", snAlarm + 1000 + "");
				snAlarm = (snAlarm + 1)%1000;
				
				if((dataArr[0] & 0x08) == 0x08) {
					jsonObject.put("ics", "1");
				}else {
					jsonObject.put("ics", "0");
				}
				
				if((dataArr[0] & 0x04) == 0x04) {
					jsonObject.put("mact", "1");
					jsonObject.put("r_type", "1");
				}else {
					jsonObject.put("mact", "0");
					jsonObject.put("r_type", "2");
				}
				if((dataArr[0] & 0x01) == 0x01) {
					jsonObject.put("vibrate", "1");
				}else {
					jsonObject.put("vibrate", "");
				}
//				//��״̬ 1��ʾ�ɲ��� 0��ʾ���ɲ���
//				jsonObject.put("lockstatus", "0");
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
//				jsonObject.put("temp", "");
//				jsonObject.put("humidity", "");
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
//				//��״̬ 1��ʾ�ɲ��� 0��ʾ���ɲ���
//				jsonObject.put("lockstatus", "0");
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
				//�����ݷ���
				return;
			}else if(command == 7) {
				long now = System.currentTimeMillis();
				JsonTemp temp = jsonMap.get(manholeNumberValue);
				if(temp != null) {
					if(now - temp.getTime() < 15000) {
						jsonObject = temp.getJson();
					}
				}
				
//				jsonObject.put("batterylevel", "");
				int moisture = (int)(dataArr[0] &0xFF);
				int temperature = (int)(dataArr[1] &0xFF);
				
				jsonObject.put("temp", temperature+"");
				jsonObject.put("humidity", moisture+"");
				
//				jsonObject.put("sn", snAll + 2000 + "");
//				snAll = (snAll + 1)%1000;
				
//				jsonObject.put("r_type", "0");
//				jsonObject.put("ics", "");
//				jsonObject.put("ocs", "");
//				jsonObject.put("mact", "0");
//				jsonObject.put("vibrate", "");
//				jsonObject.put("lockstatus", "0");
//				jsonObject.put("inlight", "");
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
		
    	
    	
	}
    
    private void sendToJS(String message) {
    	logger.info("JS message:" + message);
    	System.out.println("JS message:" + message);
    	//��ʨ�ӿ�
        String result = sendPost("", message);
        logger.info("JS result:" + result);
    	System.out.println("JS result:" + result);
    }
    
    private void sendToGX(String message) {
    	logger.info("GX message:" + message);
    	System.out.println("GX message:" + message);
        String result = sendPost("http://140.207.38.66:8000/deviceDataChanged", message);
        logger.info("GX result:" + result);
    	System.out.println("GX result:" + result);
    }
    
    /**
     * ��ָ�� URL ����POST����������
     * 
     * @param url
     *            ��������� URL
     * @param param
     *            ����������������Ӧ���� name1=value1&name2=value2 ����ʽ��
     * @return ������Զ����Դ����Ӧ���
     */
    public static String sendPost(String url, String param) {
        PrintWriter out = null;
        BufferedReader in = null;
        String result = "";
        try {
            URL realUrl = new URL(url);
            // �򿪺�URL֮�������
            URLConnection conn = realUrl.openConnection();
            // ����ͨ�õ���������
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("user-agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            // ����POST�������������������
            conn.setDoOutput(true);
            conn.setDoInput(true);
            // ��ȡURLConnection�����Ӧ�������
            out = new PrintWriter(conn.getOutputStream());
            // �����������
            out.print(param);
            // flush������Ļ���
            out.flush();
            // ����BufferedReader����������ȡURL����Ӧ
            in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            System.out.println("���� POST ��������쳣��"+e);
            e.printStackTrace();
        }
        //ʹ��finally�����ر��������������
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
    
    private void sendToWeb(String message) {
        byte[] data = message.getBytes();
        IoBuffer buf = IoBuffer.wrap(data);
        
        WebServerSessionInstance instance = WebServerSessionInstance.getWebServerSessionInstance();
        IoSession session = instance.getWebServerSession();
        if(session != null && session.isConnected()) {						//�ж�session��Ϊ����session��������״̬
        	WriteFuture future = session.write(buf);
        	// ��100���볬ʱ���ڵȴ�д���
        	future.awaitUninterruptibly(100);
        	// The message has been written successfully
        	if( future.isWritten() ) {									//��web��������������״̬
        		System.out.println("CoAP send to WebServer successfully");
        		logger.info("CoAP send to WebServer successfully");
        		logger.info("message:" + message);
        	}else{
        		System.out.println("CoAP send to WebServer failed");
        		logger.info("CoAP send to WebServer failed");
        	}
        }else {															//��web���������ӶϿ�
        	if(session == null) {										//TCP����δ����
        		System.out.println("TCP never connects");
        		logger.error("TCP never connects");
        		logger.error("message:" + message);
        	}else if(!session.isConnected()) {							//TCP�����쳣�ر�
        		System.out.println("TCP disconnect");
        		logger.error("TCP disconnect");
        		logger.error("message:" + message);
        	}
        }
    }
	
}
