package com.pioteks.usr;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pioteks.handler.NBServerHandler;
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

	private final Logger logger = (Logger) LoggerFactory.getLogger(getClass());
	
	private int count = 0;
	private String error = null;
	
	private ClientAdapter clientAdapter;
	
	public ClinetCallbackAdapter(ClientAdapter clientAdapter) {
		super();
		// 采用循环接收数据  
		this.clientAdapter = clientAdapter;
		StringBuffer sbu = new StringBuffer();
		sbu.append((char) 69);sbu.append((char) 82);sbu.append((char) 82);sbu.append((char) 79);
		sbu.append((char) 82);
		error = sbu.toString();
	}

	/**
	 * 连接响应回调函数
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
	 * 取消订阅响应 回调函数
	 */
	@Override
	public void onDisSubscribeAck(int messageId, String clientId, String topics, int returnCode) {
		// TODO Auto-generated method stub
		super.onDisSubscribeAck(messageId, clientId, topics, returnCode);
	}

	/**
	 * 订阅响应 回调函数
	 */
	@Override
	public void onSubscribeAck(int messageId, String clientId, String topics, int returnCode) {
		// TODO Auto-generated method stub
		super.onSubscribeAck(messageId, clientId, topics, returnCode);
	}

	/**
	 * 推送之后的 回调函数，通过 isSuccess表示推送是否成功
	 * 
	 */
	@Override
	public void onPublishDataAck(int messageId, String topic, boolean isSuccess) {
		// TODO Auto-generated method stub
		super.onPublishDataAck(messageId, topic, isSuccess);
		System.out.println("发送数据："+isSuccess);
	}

	/**
	 * 本次推送结果的回调函数
	 */
	@Override
	public void onPublishDataResult(int messageId, String topic) {
		// TODO Auto-generated method stub
		super.onPublishDataResult(messageId, topic);
	}

	/**
	 * 收到设备数据的回调函数
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
	
	//16进制转字符串
	private String getContent(String contentHex) {
		String content = "";
		String[] contentHexs = contentHex.split("-");
		for(int i = 0; i < contentHexs.length; i++) {
			content  = content + (char)Integer.parseInt(contentHexs[i],16);
		}
		
		return content;
	}

	//byte数组转16进制
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

	//字符串转16进制
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
	
	//字符串转byte 
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
    	
		int flag = 0;
		byte[] result = null;
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
	    if( manholeNumberValue < 40960 || manholeNumberValue > 45055) {
	    	//不推送给杰狮
	    	return;
	    }
	    messageList.add("No"+manholeNumberValue);
	    
		byte command = data[3];
		byte[] dataArr = new byte[2];
		System.arraycopy(data, 4, dataArr, 0, 2);
		byte check = data[6];
		byte end = data[7];
		
		if(command == 0) {
			if((dataArr[0] & 0x01) == 0x01) {
				messageList.add(Vibrating);
			}else {
				messageList.add(VibCancel);
			}
			if((dataArr[0] & 0x02) == 0x02) {
				messageList.add(DownCoverOpened);
			}else {
				messageList.add(DownCoverClosed);
			}
			if((dataArr[0] & 0x04) == 0x04) {
				messageList.add(UpCoverOpened);
			}else {
				messageList.add(UpCoverClosed);
			}
			if((dataArr[0] & 0x08) == 0x08) {
				messageList.add(Unlock);
			}else {
				messageList.add(Lock);
			}
			if((dataArr[0] & 0x10) == 0x10) {
				messageList.add(SULSafe);
			}else {
				messageList.add(SULALarm);
			}
			if((dataArr[0] & 0x20) == 0x20) {
				messageList.add(CHSafe);
				messageList.add(COSafe);
			}else {
				messageList.add(CHALarm);
				messageList.add(COALarm);
			}
		}else if(command == 5) {
			if((dataArr[0] & 0x01) == 0x01) {
				messageList.add(Vibrating);
			}else {
				messageList.add(VibCancel);
			}
			if((dataArr[0] & 0x02) == 0x02) {
				messageList.add(DownCoverOpened);
			}else {
				messageList.add(DownCoverClosed);
			}
			if((dataArr[0] & 0x04) == 0x04) {
				messageList.add(UpCoverOpened);
			}else {
				messageList.add(UpCoverClosed);
			}
			if((dataArr[0] & 0x08) == 0x08) {
				messageList.add(Unlock);
			}else {
				messageList.add(Lock);
			}
			if((dataArr[0] & 0x10) == 0x10) {
				messageList.add(SULSafe);
			}else {
				messageList.add(SULALarm);
			}
			if((dataArr[0] & 0x20) == 0x20) {
				messageList.add(CHSafe);
				messageList.add(COSafe);
			}else {
				messageList.add(CHALarm);
				messageList.add(COALarm);
			}
			byte data6 = dataArr[1];
			int distanceValue = (int)(data6 &0x0F);
			int batteryValue = (int)(data6 >>> 4 &0x0F);
			messageList.add("Battery" + batteryValue);
			messageList.add("Distance" + distanceValue);
		}else if(command == 6) {
			int ch4Value = dataArr[0] & 0xFF;
			int sValue = dataArr[1] & 0xFF;
			messageList.add("CH4Value" + ch4Value);
			messageList.add("SValue" + sValue);
		}else if(command == 7) {
			int moisture = (int)(dataArr[0] &0xFF);
			int temperature = (int)(dataArr[1] &0xFF);
			messageList.add("Moisture" + moisture);
			messageList.add("Temperature" + temperature);
		}
		
		for(int i = 0; i < messageList.size(); i++)
		{
			sendToWeb(messageList.get(i));
			Thread.currentThread().sleep(500);
		}
		
//    	for(int i = 0; i < commandList.size(); i++) {
//    		RequestResponse rr = commandList.get(i);
//    		String request = rr.getRequest();
//    		
//			if(NBServerHandler.byteToStr(data).equals(request)) {
//				flag = 1;
//				
//				result =  NBServerHandler.strToByte(rr.getResponse());
//				if(command.getMode() == CommandInstance.ALARM ) {
//					switch(i) {
//					case 0:
//						sendToWeb(NBServerHandler.UpCoverOpened);
//						command.setLastStatusOne(NBServerHandler.UpCoverOpened);
//						Thread.currentThread().sleep(500);
//						sendToWeb(NBServerHandler.DownCoverOpened);
//						command.setLastStatusTwo(NBServerHandler.DownCoverOpened);
//						Thread.currentThread().sleep(500);
//						sendToWeb(NBServerHandler.Vibrating);
//						command.setLastStatusThree(NBServerHandler.Vibrating);
//						break;
//					case 1:
//						sendToWeb(NBServerHandler.UpCoverClosed);
//						command.setLastStatusOne(NBServerHandler.UpCoverClosed);
//						Thread.currentThread().sleep(500);
//						sendToWeb(NBServerHandler.DownCoverOpened);
//						command.setLastStatusTwo(NBServerHandler.DownCoverOpened);
//						Thread.currentThread().sleep(500);
//						sendToWeb(NBServerHandler.Vibrating);
//						command.setLastStatusThree(NBServerHandler.Vibrating);
//						break;
//					case 2:
//						sendToWeb(NBServerHandler.UpCoverOpened);
//						command.setLastStatusOne(NBServerHandler.UpCoverOpened);
//						Thread.currentThread().sleep(500);
//						sendToWeb(NBServerHandler.DownCoverClosed);
//						command.setLastStatusTwo(NBServerHandler.DownCoverClosed);
//						Thread.currentThread().sleep(500);
//						sendToWeb(NBServerHandler.Vibrating);
//						command.setLastStatusThree(NBServerHandler.Vibrating);
//						break;
//					case 3:
//						sendToWeb(NBServerHandler.UpCoverClosed);
//						command.setLastStatusOne(NBServerHandler.UpCoverClosed);
//						Thread.currentThread().sleep(500);
//						sendToWeb(NBServerHandler.DownCoverClosed);
//						command.setLastStatusTwo(NBServerHandler.DownCoverClosed);
//						Thread.currentThread().sleep(500);
//						sendToWeb(NBServerHandler.Vibrating);
//						command.setLastStatusThree(NBServerHandler.Vibrating);
//						break;
//					case 4:
//						sendToWeb(NBServerHandler.UpCoverOpened);
//						command.setLastStatusOne(NBServerHandler.UpCoverOpened);
//						Thread.currentThread().sleep(500);
//						sendToWeb(NBServerHandler.DownCoverOpened);
//						command.setLastStatusTwo(NBServerHandler.DownCoverOpened);
//						Thread.currentThread().sleep(500);
//						sendToWeb(NBServerHandler.VibCancel);
//						command.setLastStatusThree(NBServerHandler.VibCancel);
//						break;
//					case 5:
//						sendToWeb(NBServerHandler.UpCoverClosed);
//						command.setLastStatusOne(NBServerHandler.UpCoverClosed);
//						Thread.currentThread().sleep(500);
//						sendToWeb(NBServerHandler.DownCoverOpened);
//						command.setLastStatusTwo(NBServerHandler.DownCoverOpened);
//						Thread.currentThread().sleep(500);
//						sendToWeb(NBServerHandler.VibCancel);
//						command.setLastStatusThree(NBServerHandler.VibCancel);
//						break;
//					case 6:
//						sendToWeb(NBServerHandler.UpCoverOpened);
//						command.setLastStatusOne(NBServerHandler.UpCoverOpened);
//						Thread.currentThread().sleep(500);
//						sendToWeb(NBServerHandler.DownCoverClosed);
//						command.setLastStatusTwo(NBServerHandler.DownCoverClosed);
//						Thread.currentThread().sleep(500);
//						sendToWeb(NBServerHandler.VibCancel);
//						command.setLastStatusThree(NBServerHandler.VibCancel);
//						break;
//					case 7:
//						sendToWeb(NBServerHandler.UpCoverClosed);
//						command.setLastStatusOne(NBServerHandler.UpCoverClosed);
//						Thread.currentThread().sleep(500);
//						sendToWeb(NBServerHandler.DownCoverClosed);
//						command.setLastStatusTwo(NBServerHandler.DownCoverClosed);
//						Thread.currentThread().sleep(500);
//						sendToWeb(NBServerHandler.VibCancel);
//						command.setLastStatusThree(NBServerHandler.VibCancel);
//						break;
//					}
//		    	}else if(command.getMode() == CommandInstance.OPERATE) {
//		    		commandList = command.getCommandListMode2();
//		    		switch(i) {
//					case 0:
//						sendToWeb("Down Cover Opening");
//						break;
//					case 1:
//						sendToWeb("Down Cover Closing");
//						break;
//					}
//		    	}
//				//增加定时任务，发送确定的消息
//				Properties prop=new Properties();
//			    InputStream in=new FileInputStream(new File(System.getProperty("user.dir")+File.separator+"DriverServer.properties"));
//			    prop.load(in);
//			    int delayTime = Integer.parseInt(prop.getProperty("time_interval"));
//			    if(NBServerHandler.timerTask != null) {
//			    	NBServerHandler.timerTask.cancel();							//先取消上次的定时任务
//			    }
//			    NBServerHandler.timerTask = new AlarmAckTimerTask(command.getLastStatusOne(), command.getLastStatusTwo(),			
//						command.getLastStatusThree());							//创建新的定时任务
//			    NBServerHandler.t.schedule(NBServerHandler.timerTask, delayTime*1000);													//将定时任务加入计时器
//				in.close();
//				
//				break;
//			}
//    	}
    	
    	if(flag == 0) {
    		StringBuffer sbu = new StringBuffer();
    		sbu.append((char) 69);sbu.append((char) 82);sbu.append((char) 82);sbu.append((char) 79);
    		sbu.append((char) 82);
    		result = sbu.toString().getBytes();
    	}
    	
//    	Thread.currentThread().sleep(command.getDelayTime());
//    	
//    	 // 组织IoBuffer数据包的方法：本方法才可以正确地让客户端UDP收到byte数组
//        IoBuffer buf = IoBuffer.wrap(result);
//
//        // 向客户端写数据
//        WriteFuture future = session.write(buf);
//        // 在100毫秒超时间内等待写完成
//        future.awaitUninterruptibly(100);
//        // The message has been written successfully
//        if( future.isWritten() ) {
//            System.out.println("NB return successfully");
//            logger.info("NB return successfully");
//        }else{
//            System.out.println("NB return failed");
//            logger.info("NB return failed");
//        }
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
	
}
