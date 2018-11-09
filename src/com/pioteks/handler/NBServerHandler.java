package com.pioteks.handler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Timer;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.nio.NioDatagramConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pioteks.model.RequestResponse;
import com.pioteks.server.UDPInnerClient;
import com.pioteks.timer.AlarmAckTimerTask;
import com.pioteks.utils.BytesHexString;
import com.pioteks.utils.CommandInstance;
import com.pioteks.utils.WebServerSessionInstance;

public class NBServerHandler extends IoHandlerAdapter{

	private final Logger logger = (Logger) LoggerFactory.getLogger(getClass());
	// 定义发送数据报的目的地  
    public static final int DEST_PORT = 30000;  
    public static final String DEST_IP = "127.0.0.1";  
	
	public final static String UpCoverOpened = "Up Cover Opened";
	public final static String DownCoverOpened = "Down Cover Opened";
	public final static String UpCoverClosed = "Up Cover Closed";
	public final static String DownCoverClosed = "Down Cover Closed";
	public final static String Vibrating = "Vibrating";
	public final static String VibCancel = "VibCancel";
	
	public static Timer t = new Timer();
	public static AlarmAckTimerTask timerTask;
	 /**
     * MINA的异常回调方法。
     * <p>
     * 本类中将在异常发生时，立即close当前会话。
     *
     * @param session 发生异常的会话
     * @param cause 异常内容
     * @see IoSession#close(boolean)
     */
    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception
    {
    	logger.warn(cause.getMessage(), cause);
        cause.printStackTrace();
        session.closeNow();
    }

    @Override
    public void sessionClosed(IoSession session) throws Exception {
        System.out.println("NB Session closed...");
        logger.info("NB Session closed...");
    }

    @Override
    public void sessionCreated(IoSession session) throws Exception {
        System.out.println("NB Session created...");
        logger.info("NB Session created...");
    }

    @Override
    public void sessionOpened(IoSession session) throws Exception {
    	System.out.println("NB Session opened...");
        logger.info("NB Session opened...");
        InetSocketAddress remoteAddress = (InetSocketAddress) session.getRemoteAddress();
        logger.info("NB Server opened Session ID ="+String.valueOf(session.getId()));
        logger.info("接收来自NB客户端 :" + remoteAddress.getAddress().getHostAddress() + "的连接.");
    }

    @Override
    public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
        System.out.println("NB Session Idle...");
        logger.info("NB Session Idle...");
    }

    /**
     * MINA框架中收到客户端消息的回调方法。
     * <p>
     * 本类将在此方法中实现完整的即时通讯数据交互和处理策略。
     * <p>
     * 为了提升并发性能，本方法将运行在独立于MINA的IoProcessor之外的线程池中，
     *
     * @param session 收到消息对应的会话引用
     * @param message 收到的MINA的原始消息封装对象，本类中是 {@link IoBuffer}对象
     * @throws Exception 当有错误发生时将抛出异常
     */
    @Override
    public void messageReceived(IoSession session, Object message)throws Exception
    {
    	
        byte[] result=new byte[1];
        IoBuffer ioBuffer=(IoBuffer) message;
        if(ioBuffer.remaining()>16){
            result[0]=(byte)0xFF;
        }else{
            byte[] data = new byte[ioBuffer.limit()-ioBuffer.position()];
            ioBuffer.get(data);
            
            System.out.println("NB received data "+ BytesHexString.bytesToHexString(data));
            logger.warn("NB received data "+ BytesHexString.bytesToHexString(data));
            
            //将数据发送给pioteks
            byte[] address = (session.getRemoteAddress() + ":").getBytes();

            byte[] all = addBytes(address, data);			//拼接上传内容
            
            all = addBytes(all, ":".getBytes());
            
            clientSend(all, session);		// 使用NIMA框架进行内部通信
            
            //回复并发送数据给杰狮
            response(data, session);			
            
        }
    }
    
    private void clientSend(byte[] all, IoSession session) throws InterruptedException {
		
    	UDPInnerClient udpClient=new UDPInnerClient(DEST_IP,DEST_PORT);  
        udpClient.setConnector(new NioDatagramConnector());  
        udpClient.getConnector().setHandler(new InnerClientIoHandler());  
        IoConnector connector=udpClient.getConnector();  
        ConnectFuture connectFuture=connector.connect(udpClient.getInetSocketAddress());  
        // 等待是否连接成功，相当于是转异步执行为同步执行。  
        connectFuture.awaitUninterruptibly();  
        //连接成功后获取会话对象。如果没有上面的等待，由于connect()方法是异步的，  
        //connectFuture.getSession(),session可能会无法获取。  
        udpClient.setSession(connectFuture.getSession());  
        udpClient.getSession().setAttribute("session_NB", session);
        IoBuffer buf = IoBuffer.wrap(all);
        udpClient.getSession().write(buf);  
    }

	
    
    private void response(byte[] data, IoSession session) throws InterruptedException, IOException {
    	CommandInstance commandInstance = CommandInstance.getCommandInstance();
    	List<RequestResponse> commandList = null;
    	
    	if(commandInstance.getMode() == CommandInstance.ALARM) {
    		commandList = commandInstance.getCommandListMode1();
    	}else if(commandInstance.getMode() == CommandInstance.OPERATE) {
    		commandList = commandInstance.getCommandListMode2();
    	}
    	
		int flag = 0;
		byte[] result = null;
		System.out.println("receive from NB :" + byteToStr(data));
		logger.info("receive from NB :" + byteToStr(data));
		if(data.length != 8) {
			logger.info("receive error message from NB :" + byteToStr(data));
			return;
		}
		
		List<String> messageList = new ArrayList<String>();
		
		byte start = data[0];
		byte[] manholeNumber = new byte[2];
		System.arraycopy(data, 1, manholeNumber, 0, 2);
	    int manholeNumberValue = (int) ( ((manholeNumber[0] & 0xFF)<<8)|((manholeNumber[1] & 0xFF)));  
	    
	    messageList.add(manholeNumberValue+"");
	    
		byte command = data[3];
		byte[] dataArr = new byte[2];
		System.arraycopy(data, 4, dataArr, 0, 2);
		byte check = data[6];
		byte end = data[7];
		
		if(command == 1) {
			if((dataArr[0] & 0x01) == 0x01) {
				messageList.add("震动");
			}else {
				messageList.add("无震动");
			}
			if((dataArr[0] & 0x04) == 0x04) {
				messageList.add("外井盖开启");
			}else {
				messageList.add("外井盖关闭");
			}
			if((dataArr[0] & 0x02) == 0x02) {
				messageList.add("内井盖开启");
			}else {
				messageList.add("内井盖关闭");
			}
			if((dataArr[0] & 0x08) == 0x08) {
				messageList.add("盖锁开启");
			}else {
				messageList.add("盖锁关闭");
			}
		}else if(command == 2) {
			if((dataArr[0] & 0x10) == 0x10) {
				messageList.add("硫化物未超标");
			}else {
				messageList.add("硫化物超标");
			}
			if((dataArr[0] & 0x20) == 0x20) {
				messageList.add("甲烷未超标");
				messageList.add("一氧化碳未超标");
			}else {
				messageList.add("甲烷超标");
				messageList.add("一氧化碳超标");
			}
			byte distance = dataArr[1];
			int distanceValue = (int)distance;
		}else if(command == 3) {
			if((dataArr[0] & 0x01) == 0x01) {
				messageList.add("震动");
			}else {
				messageList.add("无震动");
			}
			if((dataArr[0] & 0x04) == 0x04) {
				messageList.add("外井盖开启");
			}else {
				messageList.add("外井盖关闭");
			}
			if((dataArr[0] & 0x02) == 0x02) {
				messageList.add("内井盖开启");
			}else {
				messageList.add("内井盖关闭");
			}
			if((dataArr[0] & 0x08) == 0x08) {
				messageList.add("盖锁开启");
			}else {
				messageList.add("盖锁关闭");
			}
		}
		
		for(int i = 0; i < messageList.size(); i++)
		{
			sendToWeb(messageList.get(i));
			Thread.currentThread().sleep(500);
		}

		
//		//增加定时任务，发送确定的消息
//		Properties prop=new Properties();
//	    InputStream in=new FileInputStream(new File(System.getProperty("user.dir")+File.separator+"DriverServer.properties"));
//	    prop.load(in);
//	    int delayTime = Integer.parseInt(prop.getProperty("time_interval"));
//	    if(timerTask != null) {
//	    	timerTask.cancel();							//先取消上次的定时任务
//	    }
//		timerTask = new AlarmAckTimerTask();													//创建新的定时任务
//		t.schedule(timerTask, delayTime*1000);													//将定时任务加入计时器
//		in.close();
		
		
//    	for(int i = 0; i < commandList.size(); i++) {
//    		RequestResponse rr = commandList.get(i);
//    		String request = rr.getRequest();
//    		
//			if(byteToStr(data).equals(request)) {
//				flag = 1;
//				
//				result =  strToByte(rr.getResponse());
//				if(command.getMode() == CommandInstance.ALARM ) {
//					switch(i) {
//					case 0:
//						sendToWeb(UpCoverOpened);
//						command.setLastStatusOne(UpCoverOpened);
//						Thread.currentThread().sleep(500);
//						sendToWeb(DownCoverOpened);
//						command.setLastStatusTwo(DownCoverOpened);
//						Thread.currentThread().sleep(500);
//						sendToWeb(Vibrating);
//						command.setLastStatusThree(Vibrating);
//						break;
//					case 1:
//						sendToWeb(UpCoverClosed);
//						command.setLastStatusOne(UpCoverClosed);
//						Thread.currentThread().sleep(500);
//						sendToWeb(DownCoverOpened);
//						command.setLastStatusTwo(DownCoverOpened);
//						Thread.currentThread().sleep(500);
//						sendToWeb(Vibrating);
//						command.setLastStatusThree(Vibrating);
//						break;
//					case 2:
//						sendToWeb(UpCoverOpened);
//						command.setLastStatusOne(UpCoverOpened);
//						Thread.currentThread().sleep(500);
//						sendToWeb(DownCoverClosed);
//						command.setLastStatusTwo(DownCoverClosed);
//						Thread.currentThread().sleep(500);
//						sendToWeb(Vibrating);
//						command.setLastStatusThree(Vibrating);
//						break;
//					case 3:
//						sendToWeb(UpCoverClosed);
//						command.setLastStatusOne(UpCoverClosed);
//						Thread.currentThread().sleep(500);
//						sendToWeb(DownCoverClosed);
//						command.setLastStatusTwo(DownCoverClosed);
//						Thread.currentThread().sleep(500);
//						sendToWeb(Vibrating);
//						command.setLastStatusThree(Vibrating);
//						break;
//					case 4:
//						sendToWeb(UpCoverOpened);
//						command.setLastStatusOne(UpCoverOpened);
//						Thread.currentThread().sleep(500);
//						sendToWeb(DownCoverOpened);
//						command.setLastStatusTwo(DownCoverOpened);
//						Thread.currentThread().sleep(500);
//						sendToWeb(VibCancel);
//						command.setLastStatusThree(VibCancel);
//						break;
//					case 5:
//						sendToWeb(UpCoverClosed);
//						command.setLastStatusOne(UpCoverClosed);
//						Thread.currentThread().sleep(500);
//						sendToWeb(DownCoverOpened);
//						command.setLastStatusTwo(DownCoverOpened);
//						Thread.currentThread().sleep(500);
//						sendToWeb(VibCancel);
//						command.setLastStatusThree(VibCancel);
//						break;
//					case 6:
//						sendToWeb(UpCoverOpened);
//						command.setLastStatusOne(UpCoverOpened);
//						Thread.currentThread().sleep(500);
//						sendToWeb(DownCoverClosed);
//						command.setLastStatusTwo(DownCoverClosed);
//						Thread.currentThread().sleep(500);
//						sendToWeb(VibCancel);
//						command.setLastStatusThree(VibCancel);
//						break;
//					case 7:
//						sendToWeb(UpCoverClosed);
//						command.setLastStatusOne(UpCoverClosed);
//						Thread.currentThread().sleep(500);
//						sendToWeb(DownCoverClosed);
//						command.setLastStatusTwo(DownCoverClosed);
//						Thread.currentThread().sleep(500);
//						sendToWeb(VibCancel);
//						command.setLastStatusThree(VibCancel);
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
//			    if(timerTask != null) {
//			    	timerTask.cancel();							//先取消上次的定时任务
//			    }
//				timerTask = new AlarmAckTimerTask(command.getLastStatusOne(), command.getLastStatusTwo(),			
//						command.getLastStatusThree());													//创建新的定时任务
//				t.schedule(timerTask, delayTime*1000);													//将定时任务加入计时器
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
    	
    	Thread.currentThread().sleep(commandInstance.getDelayTime());
    	
    	 // 组织IoBuffer数据包的方法：本方法才可以正确地让客户端UDP收到byte数组
        IoBuffer buf = IoBuffer.wrap(result);

        // 向客户端写数据
        WriteFuture future = session.write(buf);
        // 在100毫秒超时间内等待写完成
        future.awaitUninterruptibly(100);
        // The message has been written successfully
        if( future.isWritten() ) {
            System.out.println("NB return successfully");
            logger.info("NB return successfully");
        }else{
            System.out.println("NB return failed");
            logger.info("NB return failed");
        }
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
        		System.out.println("send to WebServer successfully");
        		logger.info("send to WebServer successfully");
        		logger.info("message:" + message);
        	}else{
        		System.out.println("send to WebServer failed");
        		logger.info("send to WebServer failed");
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
     * 合并两个byte数组，返回新的byte数组
     * @param data1
     * @param data2
     * @return
     */
    public static byte[] addBytes(byte[] data1, byte[] data2) {  
        byte[] data3 = new byte[data1.length + data2.length];  
        System.arraycopy(data1, 0, data3, 0, data1.length);  
        System.arraycopy(data2, 0, data3, data1.length, data2.length);  
        return data3;  
      
    }
    
    /**
     * 将16进制的字符串转成byte[]数组
     * “AA-41-42-43”
     * @param s
     * @return
     */
	public static byte[] strToByte(String s) {
		String[] sList = s.split("-");
		byte[] bytes = new byte[sList.length];
		for(int i = 0; i < sList.length; i++) {
			int iValue = Integer.parseInt(sList[i], 16);
			bytes[i] = (byte)iValue;
		}
		
		return bytes;
	}
	
	/**
	 * 将byte[]数组转化为16进制字符串
	 * @param data
	 * @return
	 */
	public static String byteToStr(byte[] data) {
		String contentHex = "";
		for(int i = 0; i < data.length; i++ ) {
			String s = Integer.toHexString(data[i] & 0xFF);
			if(s.length() == 1) {
				s = "0" + s;
			}
			contentHex = contentHex + s + "-";
		}
		contentHex = contentHex.substring(0, contentHex.length()-1);
		
		return contentHex.toUpperCase();
	}
	
	/**
	 * 将byte数组转化为16进制字符串
	 * @param data
	 * @return
	 */
	public static String singleByteToStr(byte data) {
		String contentHex = "";
		String s = Integer.toHexString(data & 0xFF);
		if(s.length() == 1) {
			s = "0" + s;
		}
		
		return contentHex.toUpperCase();
	}
}
