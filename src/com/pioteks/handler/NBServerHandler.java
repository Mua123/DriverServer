package com.pioteks.handler;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.piokeks.model.RequestResponse;
import com.pioteks.utils.BytesHexString;
import com.pioteks.utils.CommandInstance;
import com.pioteks.utils.WebServerSessionInstance;

public class NBServerHandler extends IoHandlerAdapter{

	private final Logger logger = (Logger) LoggerFactory.getLogger(getClass());
	
	
	 /**
     * MINA���쳣�ص�������
     * <p>
     * �����н����쳣����ʱ������close��ǰ�Ự��
     *
     * @param session �����쳣�ĻỰ
     * @param cause �쳣����
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
        SocketAddress remoteAddress = session.getRemoteAddress();
        System.out.println(remoteAddress.toString());
    }

    @Override
    public void sessionCreated(IoSession session) throws Exception {
        System.out.println("NB Session created...");
        logger.info("NB Session created...");
        SocketAddress remoteAddress = session.getRemoteAddress();
        System.out.println(remoteAddress.toString());
    }

    @Override
    public void sessionOpened(IoSession session) throws Exception {
        System.out.println("NB Session opened...");
        logger.info("NB Session opened...");
        InetSocketAddress remoteAddress = (InetSocketAddress) session.getRemoteAddress();
        logger.info(remoteAddress.getAddress().getHostAddress());
		logger.info(String.valueOf(session.getId()));
    }

    @Override
    public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
        System.out.println("NB Session Idle...");
        logger.info("NB Session Idle...");
    }

    /**
     * MINA������յ��ͻ�����Ϣ�Ļص�������
     * <p>
     * ���ཫ�ڴ˷�����ʵ�������ļ�ʱͨѶ���ݽ����ʹ�����ԡ�
     * <p>
     * Ϊ�������������ܣ��������������ڶ�����MINA��IoProcessor֮����̳߳��У�
     *
     * @param session �յ���Ϣ��Ӧ�ĻỰ����
     * @param message �յ���MINA��ԭʼ��Ϣ��װ���󣬱������� {@link IoBuffer}����
     * @throws Exception ���д�����ʱ���׳��쳣
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
            
            response(data, session);			//jar����ʽ�Ļظ�
            
        }
    }
	
    
    private void response(byte[] data, IoSession session) throws InterruptedException {
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
						sendToWeb("Up Cover Opened");
						break;
					case 1:
						sendToWeb("Down Cover Opened");
						break;
					case 2:
						sendToWeb("Vibrating");
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
    	
    	 // ��֯IoBuffer���ݰ��ķ������������ſ�����ȷ���ÿͻ���UDP�յ�byte����
        IoBuffer buf = IoBuffer.wrap(result);

        // ��ͻ���д����
        WriteFuture future = session.write(buf);
        // ��100���볬ʱ���ڵȴ�д���
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
        if(session.isConnected()) {
        	WriteFuture future = session.write(buf);
        	// ��100���볬ʱ���ڵȴ�д���
        	future.awaitUninterruptibly(100);
        	// The message has been written successfully
        	if( future.isWritten() ) {									//��web��������������״̬
        		System.out.println("send to WebServer successfully");
        		logger.info("send to WebServer successfully");
        		logger.info("message:" + message);
        	}else{
        		System.out.println("send to WebServer failed");
        		logger.info("send to WebServer failed");
        	}
        }else {															//��web���������ӶϿ�
        	
        }
    }
    
    /**
     * �ϲ�����byte���飬�����µ�byte����
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
     * ��16���Ƶ��ַ���ת��byte[]����
     * ��AA-41-42-43��
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
	 * ��byte[]����ת��Ϊ16�����ַ���
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
