package com.pioteks.handler;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pioteks.utils.BytesHexString;
import com.pioteks.utils.CommandInstance;
import com.pioteks.utils.WebServerSessionInstance;


public class WebServerHandler extends IoHandlerAdapter{
	
	private final Logger logger = (Logger) LoggerFactory.getLogger(getClass());
	private final String success = "successfully";

	
	
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
       System.out.println("Web Session closed...");
       logger.info("Web Session closed...");
//       SocketAddress remoteAddress = session.getRemoteAddress();
//       System.out.println(remoteAddress.toString());
   }

   @Override
   public void sessionCreated(IoSession session) throws Exception {
       System.out.println("Web Session created...");
       logger.info("Web Session created...");
       SocketAddress remoteAddress = session.getRemoteAddress();
       System.out.println(remoteAddress.toString());
   }

   @Override
   public void sessionOpened(IoSession session) throws Exception {
       System.out.println("Web Session opened...");
       logger.info("Web Session opened...");
       InetSocketAddress remoteAddress = (InetSocketAddress) session.getRemoteAddress();
       logger.info("Web Server opened Session ID ="+String.valueOf(session.getId()));
       logger.info("�������Կͻ��� :" + remoteAddress.getAddress().getHostAddress() + "������.");
       
       WebServerSessionInstance instance = WebServerSessionInstance.getWebServerSessionInstance();
       instance.setWebServerSession(session);
   }

   @Override
   public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
       System.out.println("Web Session Idle...");
       logger.info("Web Session Idle...");
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
   	
	   CommandInstance command = CommandInstance.getCommandInstance();
       byte[] result=new byte[1];
       IoBuffer ioBuffer=(IoBuffer) message;
       if(ioBuffer.remaining()>16){
           result[0]=(byte)0xFF;
       }else{
           byte[] data = new byte[ioBuffer.limit()-ioBuffer.position()];
           ioBuffer.get(data);
           System.out.println("Web received data "+ BytesHexString.bytesToHexString(data));    
           System.out.println(Integer.toHexString(data[0] & 0xFF));
	       switch(Integer.toHexString(data[0] & 0xFF)) {
	       		case "0":
	       			int code = data[1] & 0xFF;
	       			if(code == 0) {
	       				System.out.println("message send success");
	       				logger.info("message send success");
	       			}else if(code == 1){
	       				System.out.println("message send fail");
	       				logger.info("message send fail");
	       			}
	       			break;
	       		case "1":
	       			int model = data[1] & 0xFF;
	       			if(model == 0) {
	       				command.setMode(CommandInstance.ALARM);
	       				result = success.getBytes();
	       				response(result, session);
	       			}
	       			else if(model == 1) {
	       				command.setMode(CommandInstance.OPERATE);
	       				result = success.getBytes();
	       				response(result, session);
	       			}
	       			else {
	       				String error = "model change ERROR";
	       				response(error.getBytes(), session);
	       				logger.info("model change ERROR");
	       			}
	       			break;
	       		case "2":
	       			int delayTime = 0;
	       			for(int i = 1; i < data.length; i++) {
	       				delayTime = (delayTime << 8) + (data[i] & 0xFF);
	       			}
	       			if(delayTime < 1 || delayTime > 2000) {
	       				String error = "delayTime ERROR";
	       				response(error.getBytes(), session);
	       				logger.info("delayTime ERROR");
	       			}
	       			else {
	       				command.setDelayTime(delayTime);
	       				result = success.getBytes();
	       				response(result, session);
	       			}
	       			break;
	       }
	       
       }
   }
   
   private void response(byte[] result, IoSession session) {
	   // ��֯IoBuffer���ݰ��ķ������������ſ�����ȷ���ÿͻ���UDP�յ�byte����
       IoBuffer buf = IoBuffer.wrap(result);

       // ��ͻ���д����
       WriteFuture future = session.write(buf);
       // ��100���볬ʱ���ڵȴ�д���
       future.awaitUninterruptibly(100);
       // The message has been written successfully
       if( future.isWritten() ) {
           System.out.println("Web return successfully");
           logger.info("Web return successfully");
       }else{
           System.out.println("Web return failed");
           logger.info("Web return failed");
       }
   }
}
