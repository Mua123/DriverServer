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

	private BlockingQueue<byte[]> queue;//��Ϣ����
	
	private final Logger logger = (Logger) LoggerFactory.getLogger(getClass());
	
	private boolean isRunning = true;
	
	
	public ClientJSConsumer(BlockingQueue<byte[]> queue) {
		super();
		this.queue = queue;
	}



	@Override
	public void run() {
		System.out.println("������ʨ�������̣߳�");
		
        isRunning = true;
        try {
            while (isRunning) {
                System.out.println("��ʨ���г���" + queue.size());
                logger.info("JS size: " + queue.size());
                
                //�����ǰ����Ϊ�գ� ����map���Ƿ���δ���͵�����
                if(queue.size() == 0) {
                	long currentTime = System.currentTimeMillis();
                	Iterator<Map.Entry<Integer, JsonTemp>> it = jsonMap.entrySet().iterator();
                	while(it.hasNext()) {
                		Map.Entry<Integer, JsonTemp> entry = it.next();
                		//���ʱ�䳬��3���ӣ���ֱ�ӷ���
                		if(currentTime - entry.getValue().getTime() >= 3*60*1000 && entry.getValue().getTime() != 0) {
                			checkJson(entry.getValue().getJson());
                			JSONObject jsonObject = new JSONObject();
                			jsonObject.put("id", entry.getKey() + "");
                			jsonObject.put("dataType", 0);
            				jsonObject.put("data", entry.getValue().getJson());
            				//��ʱ����
            				sendToJS(jsonObject.toString());
            				it.remove();
                		}
                	}
                }
                byte[] data = queue.take();
                System.out.println("��ʨ������Ϣ" + NBServerHandler.byteToStr(data));
                logger.info("JS send" +NBServerHandler.byteToStr(data));
                send(data);
            }
        }catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
            System.out.println("�˳���ʨ�������̣߳�");
        }
	}
	
	/**
	 * ���json�����Ƿ�����������������-1���
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
			//��ʱ���β��ֱ��
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
		
		//��ʱ����
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
    	//��ʨ�ӿ�
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
    
    
    /**
     * ��form����ʽ�ύ���ݣ�����post����
     * @explain 
     *   1.����ͷ��httppost.setHeader("Content-Type","application/x-www-form-urlencoded")
     *   2.�ύ�����ݸ�ʽ��key1=value1&key2=value2...
     * @param url �����ַ
     * @param paramsMap ��������
     * @return ��������������
     */
    public static String httpPostWithForm(String url,Map<String, String> paramsMap){
        // ���ڽ��շ��صĽ��
        String resultData ="";
         try {
                HttpPost post = new HttpPost(url);
                List<BasicNameValuePair> pairList = new ArrayList<BasicNameValuePair>();
                // ����Map-->ȡ��key,value�ŵ�BasicNameValuePair������-->��ӵ�list��
                for (String key : paramsMap.keySet()) {
                    pairList.add(new BasicNameValuePair(key, paramsMap.get(key)));
                }
                UrlEncodedFormEntity uefe = new UrlEncodedFormEntity(pairList, "utf-8");
                post.setEntity(uefe); 
                // ����һ��http�ͻ���
                CloseableHttpClient httpClient = HttpClientBuilder.create().build();
                // ����post����
                HttpResponse response = httpClient.execute(post);
                
                resultData = EntityUtils.toString(response.getEntity(),"UTF-8");
                
            } catch (Exception e) {
                throw new RuntimeException("�ӿ�����ʧ�ܣ�");
            }
         return resultData;
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
	
	public void end() {
		this.isRunning = false;
	}

}
