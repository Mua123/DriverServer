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
	// ���巢�����ݱ���Ŀ�ĵ�  
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
        logger.info("��������NB�ͻ��� :" + remoteAddress.getAddress().getHostAddress() + "������.");
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
            logger.warn("NB received data "+ BytesHexString.bytesToHexString(data));
            
            //�����ݷ��͸�pioteks
            byte[] address = (session.getRemoteAddress() + ":").getBytes();

            byte[] all = addBytes(address, data);			//ƴ���ϴ�����
            
            all = addBytes(all, ":".getBytes());
            
            clientSend(all, session);		// ʹ��NIMA��ܽ����ڲ�ͨ��
            
            //�ظ����������ݸ���ʨ
            response(data, session);			
            
        }
    }
    
    private void clientSend(byte[] all, IoSession session) throws InterruptedException {
		
    	UDPInnerClient udpClient=new UDPInnerClient(DEST_IP,DEST_PORT);  
        udpClient.setConnector(new NioDatagramConnector());  
        udpClient.getConnector().setHandler(new InnerClientIoHandler());  
        IoConnector connector=udpClient.getConnector();  
        ConnectFuture connectFuture=connector.connect(udpClient.getInetSocketAddress());  
        // �ȴ��Ƿ����ӳɹ����൱����ת�첽ִ��Ϊͬ��ִ�С�  
        connectFuture.awaitUninterruptibly();  
        //���ӳɹ����ȡ�Ự�������û������ĵȴ�������connect()�������첽�ģ�  
        //connectFuture.getSession(),session���ܻ��޷���ȡ��  
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
				messageList.add("��");
			}else {
				messageList.add("����");
			}
			if((dataArr[0] & 0x04) == 0x04) {
				messageList.add("�⾮�ǿ���");
			}else {
				messageList.add("�⾮�ǹر�");
			}
			if((dataArr[0] & 0x02) == 0x02) {
				messageList.add("�ھ��ǿ���");
			}else {
				messageList.add("�ھ��ǹر�");
			}
			if((dataArr[0] & 0x08) == 0x08) {
				messageList.add("��������");
			}else {
				messageList.add("�����ر�");
			}
		}else if(command == 2) {
			if((dataArr[0] & 0x10) == 0x10) {
				messageList.add("����δ����");
			}else {
				messageList.add("���ﳬ��");
			}
			if((dataArr[0] & 0x20) == 0x20) {
				messageList.add("����δ����");
				messageList.add("һ����̼δ����");
			}else {
				messageList.add("���鳬��");
				messageList.add("һ����̼����");
			}
			byte distance = dataArr[1];
			int distanceValue = (int)distance;
		}else if(command == 3) {
			if((dataArr[0] & 0x01) == 0x01) {
				messageList.add("��");
			}else {
				messageList.add("����");
			}
			if((dataArr[0] & 0x04) == 0x04) {
				messageList.add("�⾮�ǿ���");
			}else {
				messageList.add("�⾮�ǹر�");
			}
			if((dataArr[0] & 0x02) == 0x02) {
				messageList.add("�ھ��ǿ���");
			}else {
				messageList.add("�ھ��ǹر�");
			}
			if((dataArr[0] & 0x08) == 0x08) {
				messageList.add("��������");
			}else {
				messageList.add("�����ر�");
			}
		}
		
		for(int i = 0; i < messageList.size(); i++)
		{
			sendToWeb(messageList.get(i));
			Thread.currentThread().sleep(500);
		}

		
//		//���Ӷ�ʱ���񣬷���ȷ������Ϣ
//		Properties prop=new Properties();
//	    InputStream in=new FileInputStream(new File(System.getProperty("user.dir")+File.separator+"DriverServer.properties"));
//	    prop.load(in);
//	    int delayTime = Integer.parseInt(prop.getProperty("time_interval"));
//	    if(timerTask != null) {
//	    	timerTask.cancel();							//��ȡ���ϴεĶ�ʱ����
//	    }
//		timerTask = new AlarmAckTimerTask();													//�����µĶ�ʱ����
//		t.schedule(timerTask, delayTime*1000);													//����ʱ��������ʱ��
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
//				//���Ӷ�ʱ���񣬷���ȷ������Ϣ
//				Properties prop=new Properties();
//			    InputStream in=new FileInputStream(new File(System.getProperty("user.dir")+File.separator+"DriverServer.properties"));
//			    prop.load(in);
//			    int delayTime = Integer.parseInt(prop.getProperty("time_interval"));
//			    if(timerTask != null) {
//			    	timerTask.cancel();							//��ȡ���ϴεĶ�ʱ����
//			    }
//				timerTask = new AlarmAckTimerTask(command.getLastStatusOne(), command.getLastStatusTwo(),			
//						command.getLastStatusThree());													//�����µĶ�ʱ����
//				t.schedule(timerTask, delayTime*1000);													//����ʱ��������ʱ��
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
        if(session != null && session.isConnected()) {						//�ж�session��Ϊ����session��������״̬
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
	 * ��byte����ת��Ϊ16�����ַ���
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
