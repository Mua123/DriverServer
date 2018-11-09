package com.pioteks.handler;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pioteks.utils.BytesHexString;

public class InnerClientIoHandler extends IoHandlerAdapter{

	public static final Logger logger=LoggerFactory.getLogger(InnerClientIoHandler.class);
	@Override
	public void sessionOpened(IoSession session) throws Exception {
		System.out.println("����session���ӣ����͵�pioteks");
	}

	@Override
	public void sessionClosed(IoSession session) throws Exception {
		System.out.println("�Ự���ӹرգ����͵�pioteks�Ļػ��ر�");
		session.close(true);
	}

	@Override
	public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
		System.out.println("���͵�pioteks�Ļػ��쳣");
		cause.printStackTrace();
        session.closeNow();
	}

	@Override
	public void messageReceived(IoSession session, Object message) throws Exception {
		
        IoBuffer ioBuffer=(IoBuffer) message;
        byte[] data = new byte[ioBuffer.limit()-ioBuffer.position()];
        ioBuffer.get(data);
         
        IoBuffer buf = IoBuffer.wrap(data);
        
//        IoSession sessionNB = (IoSession)session.getAttribute("session_NB");
//        WriteFuture future = sessionNB.write(buf);
//     // ��100���볬ʱ���ڵȴ�д���
//        future.awaitUninterruptibly(100);
//        // The message has been written successfully
//        if( future.isWritten() ) {
//            System.out.println("return successfully");
//            logger.info("return successfully");
//        }else{
//            System.out.println("return failed");
//            logger.info("return failed");
//        }
	}

	@Override
	public void messageSent(IoSession session, Object message) throws Exception {
		System.out.println("�ͻ�����Ϣ�ѷ��ͳɹ�����������:"+message.toString());
		System.out.println("���͵�pioteks����Ϣ���ͳɹ�" + session.getRemoteAddress() + session.getLocalAddress() +session.getServiceAddress());
	}
	
}
