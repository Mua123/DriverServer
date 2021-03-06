package com.pioteks.timer;

import java.util.List;
import java.util.TimerTask;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pioteks.utils.WebServerSessionInstance;

public class AlarmAckTimerTask extends TimerTask {

	private final Logger logger = (Logger) LoggerFactory.getLogger(getClass());
	
	private String lastStatusOne;
	private String lastStatusTwo;
	private String lastStatusThree;
	private List<String> list;
	
	public AlarmAckTimerTask() {
		
	}
	
	public AlarmAckTimerTask(String lastStatusOne, String lastStatusTwo, String lastStatusThree) {
		this.lastStatusOne = lastStatusOne;
		this.lastStatusTwo =  lastStatusTwo;
		this.lastStatusThree = lastStatusThree;
	}
	
	public AlarmAckTimerTask(List<String> list) {
		this.list = list;
	}
	
	@Override
	public void run() {
		WebServerSessionInstance instance = WebServerSessionInstance.getWebServerSessionInstance();
        IoSession session = instance.getWebServerSession();
        
//		try {
//			sendToWeb(lastStatusOne, session);
//			Thread.currentThread().sleep(500);
//			sendToWeb(lastStatusTwo, session);
//			Thread.currentThread().sleep(500);
//			sendToWeb(lastStatusThree, session);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
        
        
		
		
	}
	
    private void sendToWeb(String message, IoSession session) {
        byte[] data = message.getBytes();
        IoBuffer buf = IoBuffer.wrap(data);
        
        if(session != null && session.isConnected()) {					//判断session不为空且session处于连接状态
        	WriteFuture future = session.write(buf);
        	// 在100毫秒超时间内等待写完成
        	future.awaitUninterruptibly(100);
        	// The message has been written successfully
        	if( future.isWritten() ) {									//与web服务器处于连接状态
        		System.out.println("Alarm Ack send to WebServer successfully");
        		logger.info("Alarm Ack send to WebServer successfully");
        		logger.info("message:" + message);
        	}else{
        		System.out.println("Alarm Ack send to WebServer failed");
        		logger.info("Alarm Ack send to WebServer failed");
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
