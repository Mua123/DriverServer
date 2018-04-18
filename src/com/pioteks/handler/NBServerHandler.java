package com.pioteks.handler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.Properties;
import java.util.Timer;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.piokeks.model.RequestResponse;
import com.pioteks.timer.AlarmAckTimerTask;
import com.pioteks.utils.BytesHexString;
import com.pioteks.utils.CommandInstance;
import com.pioteks.utils.WebServerSessionInstance;

public class NBServerHandler extends IoHandlerAdapter{

	private final Logger logger = (Logger) LoggerFactory.getLogger(getClass());
	
	private final static String UpCoverOpened = "Up Cover Opened";
	private final static String DownCoverOpened = "Down Cover Opened";
	private final static String UpCoverClosed = "Up Cover Closed";
	private final static String DownCoverClosed = "Down Cover Closed";
	private final static String Vibrating = "Vibrating";
	private final static String VibCancel = "VibCancel";
	
	private static Timer t = new Timer();
	private static AlarmAckTimerTask timerTask;
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
//        SocketAddress remoteAddress = session.getRemoteAddress();
//        System.out.println(remoteAddress.toString());
    }

    @Override
    public void sessionCreated(IoSession session) throws Exception {
        System.out.println("NB Session created...");
        logger.info("NB Session created...");
//        SocketAddress remoteAddress = session.getRemoteAddress();
//        System.out.println(remoteAddress.toString());
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
            
            response(data, session);			
            
        }
    }
	
    
    private void response(byte[] data, IoSession session) throws InterruptedException, IOException {
    	CommandInstance command = CommandInstance.getCommandInstance();
    	List<RequestResponse> commandList = null;
    	
    	if(command.getMode() == CommandInstance.ALARM) {
    		commandList = command.getCommandListMode1();
    	}else if(command.getMode() == CommandInstance.OPERATE) {
    		commandList = command.getCommandListMode2();
    	}
    	
		int flag = 0;
		byte[] result = null;

    	for(int i = 0; i < commandList.size(); i++) {
    		RequestResponse rr = commandList.get(i);
    		String request = rr.getRequest();
			if(byteToStr(data).equals(request)) {
				flag = 1;
				result =  strToByte(rr.getResponse());
				if(command.getMode() == CommandInstance.ALARM ) {
					switch(i) {
					case 0:
						sendToWeb(UpCoverOpened);
						command.setLastStatusOne(UpCoverOpened);
						Thread.currentThread().sleep(500);
						sendToWeb(DownCoverOpened);
						command.setLastStatusTwo(DownCoverOpened);
						Thread.currentThread().sleep(500);
						sendToWeb(Vibrating);
						command.setLastStatusThree(Vibrating);
						break;
					case 1:
						sendToWeb(UpCoverClosed);
						command.setLastStatusOne(UpCoverClosed);
						Thread.currentThread().sleep(500);
						sendToWeb(DownCoverOpened);
						command.setLastStatusTwo(DownCoverOpened);
						Thread.currentThread().sleep(500);
						sendToWeb(Vibrating);
						command.setLastStatusThree(Vibrating);
						break;
					case 2:
						sendToWeb(UpCoverOpened);
						command.setLastStatusOne(UpCoverOpened);
						Thread.currentThread().sleep(500);
						sendToWeb(DownCoverClosed);
						command.setLastStatusTwo(DownCoverClosed);
						Thread.currentThread().sleep(500);
						sendToWeb(Vibrating);
						command.setLastStatusThree(Vibrating);
						break;
					case 3:
						sendToWeb(UpCoverClosed);
						command.setLastStatusOne(UpCoverClosed);
						Thread.currentThread().sleep(500);
						sendToWeb(DownCoverClosed);
						command.setLastStatusTwo(DownCoverClosed);
						Thread.currentThread().sleep(500);
						sendToWeb(Vibrating);
						command.setLastStatusThree(Vibrating);
						break;
					case 4:
						sendToWeb(UpCoverOpened);
						command.setLastStatusOne(UpCoverOpened);
						Thread.currentThread().sleep(500);
						sendToWeb(DownCoverOpened);
						command.setLastStatusTwo(DownCoverOpened);
						Thread.currentThread().sleep(500);
						sendToWeb(VibCancel);
						command.setLastStatusThree(VibCancel);
						break;
					case 5:
						sendToWeb(UpCoverClosed);
						command.setLastStatusOne(UpCoverClosed);
						Thread.currentThread().sleep(500);
						sendToWeb(DownCoverOpened);
						command.setLastStatusTwo(DownCoverOpened);
						Thread.currentThread().sleep(500);
						sendToWeb(VibCancel);
						command.setLastStatusThree(VibCancel);
						break;
					case 6:
						sendToWeb(UpCoverOpened);
						command.setLastStatusOne(UpCoverOpened);
						Thread.currentThread().sleep(500);
						sendToWeb(DownCoverClosed);
						command.setLastStatusTwo(DownCoverClosed);
						Thread.currentThread().sleep(500);
						sendToWeb(VibCancel);
						command.setLastStatusThree(VibCancel);
						break;
					case 7:
						sendToWeb(UpCoverClosed);
						command.setLastStatusOne(UpCoverClosed);
						Thread.currentThread().sleep(500);
						sendToWeb(DownCoverClosed);
						command.setLastStatusTwo(DownCoverClosed);
						Thread.currentThread().sleep(500);
						sendToWeb(VibCancel);
						command.setLastStatusThree(VibCancel);
						break;
					}
		    	}else if(command.getMode() == CommandInstance.OPERATE) {
		    		commandList = command.getCommandListMode2();
		    		switch(i) {
					case 0:
						sendToWeb("Down Cover Opening");
						break;
					case 1:
						sendToWeb("Down Cover Closing");
						break;
					}
		    	}
				//增加定时任务，发送确定的消息
				Properties prop=new Properties();
			    InputStream in=new FileInputStream(new File(System.getProperty("user.dir")+File.separator+"DriverServer.properties"));
			    prop.load(in);
			    int delayTime = Integer.parseInt(prop.getProperty("time_interval"));
			    if(timerTask != null) {
			    	timerTask.cancel();							//先取消上次的定时任务
			    }
				timerTask = new AlarmAckTimerTask(command.getLastStatusOne(), command.getLastStatusTwo(),			
						command.getLastStatusThree());													//创建新的定时任务
				t.schedule(timerTask, delayTime*1000);													//将定时任务加入计时器
				in.close();
				
				break;
			}
    	}
    	
    	if(flag == 0) {
    		StringBuffer sbu = new StringBuffer();
    		sbu.append((char) 69);sbu.append((char) 82);sbu.append((char) 82);sbu.append((char) 79);
    		sbu.append((char) 82);
    		result = sbu.toString().getBytes();
    	}
    	
    	Thread.currentThread().sleep(command.getDelayTime());
    	
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
	private String byteToStr(byte[] data) {
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
}
