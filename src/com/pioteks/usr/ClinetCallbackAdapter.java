package com.pioteks.usr;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pioteks.handler.NBServerHandler;
import com.pioteks.model.RequestResponse;
import com.pioteks.utils.BytesHexString;
import com.pioteks.utils.CommandInstance;

import cn.usr.UsrCloudMqttCallbackAdapter;

public class ClinetCallbackAdapter extends UsrCloudMqttCallbackAdapter {
	
	private final Logger logger = (Logger) LoggerFactory.getLogger(getClass());
	
	private ClientAdapter clientAdapter;
	
	private BlockingQueue<byte[]> jsQueue;//��ʨ��Ϣ����
	private BlockingQueue<byte[]> gxQueue;//������Ϣ����
	
	public ClinetCallbackAdapter(ClientAdapter clientAdapter, BlockingQueue<byte[]> jsQueue, BlockingQueue<byte[]> gxQueue) {
		super();
		// ����ѭ����������  
		this.jsQueue = jsQueue;
		this.gxQueue = gxQueue;
		this.clientAdapter = clientAdapter;
		StringBuffer sbu = new StringBuffer();
		sbu.append((char) 69);sbu.append((char) 82);sbu.append((char) 82);sbu.append((char) 79);
		sbu.append((char) 82);
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
		
		
		if(data.length != 8) {
			return;
		}
		byte start = data[0];
		byte[] manholeNumber = new byte[2];
		System.arraycopy(data, 1, manholeNumber, 0, 2);
	    int manholeNumberValue = (int) ( ((manholeNumber[0] & 0xFF)<<8)|((manholeNumber[1] & 0xFF)));  
		byte end = data[7];
		
		//��ʨ����
		if(start == 0x41 && end == 0x5A) {
			if( manholeNumberValue < 40960 || manholeNumberValue > 45055) {
				//�����͸���ʨ
				return;
			}
			
			//������Ϣ����
			jsQueue.put(data);

		}else 
		//���߷���
		if(start == 0x45 && end == 0x6F) {
			if( manholeNumberValue < 0xB000 || manholeNumberValue > 0xBFFF) {
				//�����͸�����
				return;
			}
			
			//������Ϣ����
			gxQueue.put(data);
		}
		
	}
    
}
