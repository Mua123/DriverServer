package com.pioteks.usr;
import org.eclipse.paho.client.mqttv3.MqttException;

import cn.usr.UsrCloudMqttClientAdapter;

public class ClientAdapter extends UsrCloudMqttClientAdapter {
	
	public boolean connect = false;
	
	public ClinetCallbackAdapter clinetCallbackAdapter;

	
	public ClientAdapter(ClinetCallbackAdapter clinetCallbackAdapter) {
		this.clinetCallbackAdapter = clinetCallbackAdapter;
	}
	/**
	 * ʹ���û�������������
	 */
	@Override
	public void Connect(String userName, String passWord) throws MqttException {
		// TODO Auto-generated method stub
		super.Connect(userName, passWord);
	}

	/**
	 * ͨ��Token����
	 */
	@Override
	public void ConnectByToken(String userName, String token) throws MqttException {
		// TODO Auto-generated method stub
		super.ConnectByToken(userName, token);
	}
	
	/**
	 * �Ͽ����� ͨ������ֵ�ж��Ƿ�ɹ�
	 */
	@Override
	public boolean DisConnectUnCheck() throws MqttException {
		// TODO Auto-generated method stub
		return super.DisConnectUnCheck();
	}

	/**
	 * ͨ���豸ID����ָ���豸����Ϣ
	 */
	@Override
	public void SubscribeForDevId(String devId) throws MqttException {
		// TODO Auto-generated method stub
		super.SubscribeForDevId(devId);
	}

	/**
	 *	�޲��������ı��˻��������豸
	 */
	@Override
	public void SubscribeForUsername() throws MqttException {
		// TODO Auto-generated method stub
		super.SubscribeForUsername();
	}

	/**
	 * �в����� �������û��µ������豸
	 */
	@Override
	public void SubscribeForUsername(String userName) throws MqttException {
		// TODO Auto-generated method stub
		super.SubscribeForUsername(userName);
	}
	
	/**
	 * ȡ�����ĵ����豸
	 */
	@Override
	public void DisSubscribeforDevId(String devId) throws MqttException {
		// TODO Auto-generated method stub
		super.DisSubscribeforDevId(devId);
	}

	/**
	 * ȡ�������˻��µ������豸
	 */
	@Override
	public void DisSubscribeforuName() throws MqttException {
		// TODO Auto-generated method stub
		super.DisSubscribeforuName();
	}
	

	/**
	 * ȡ�������˺��µ����û��������豸
	 */
	@Override
	public void DisSubscribeforuName(String userName) throws MqttException {
		// TODO Auto-generated method stub
		super.DisSubscribeforuName(userName);
	}

	/**
	 * ��ָ���豸�������ݣ�ͨ���豸IDָ��Ŀ���豸��
	 */
	@Override
	public void publishForDevId(String devId, byte[] data) throws MqttException {
		// TODO Auto-generated method stub
		super.publishForDevId(devId, data);
	}

	/**
	 * ���û��µ������豸��������
	 */
	@Override
	public void publishForuName(byte[] data) throws MqttException {
		// TODO Auto-generated method stub
		super.publishForuName(data);
	}

	/**
	 * ���˺�ָ�������û����豸��������
	 */
	@Override
	public void publishForuName(String userName, byte[] data) throws MqttException {
		// TODO Auto-generated method stub
		super.publishForuName(userName, data);
	}

	

}
