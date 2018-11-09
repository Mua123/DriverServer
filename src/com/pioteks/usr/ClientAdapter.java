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
	 * 使用用户名和密码连接
	 */
	@Override
	public void Connect(String userName, String passWord) throws MqttException {
		// TODO Auto-generated method stub
		super.Connect(userName, passWord);
	}

	/**
	 * 通过Token连接
	 */
	@Override
	public void ConnectByToken(String userName, String token) throws MqttException {
		// TODO Auto-generated method stub
		super.ConnectByToken(userName, token);
	}
	
	/**
	 * 断开连接 通过返回值判断是否成功
	 */
	@Override
	public boolean DisConnectUnCheck() throws MqttException {
		// TODO Auto-generated method stub
		return super.DisConnectUnCheck();
	}

	/**
	 * 通过设备ID订阅指定设备的消息
	 */
	@Override
	public void SubscribeForDevId(String devId) throws MqttException {
		// TODO Auto-generated method stub
		super.SubscribeForDevId(devId);
	}

	/**
	 *	无参数：订阅本账户的所有设备
	 */
	@Override
	public void SubscribeForUsername() throws MqttException {
		// TODO Auto-generated method stub
		super.SubscribeForUsername();
	}

	/**
	 * 有参数： 订阅子用户下的所有设备
	 */
	@Override
	public void SubscribeForUsername(String userName) throws MqttException {
		// TODO Auto-generated method stub
		super.SubscribeForUsername(userName);
	}
	
	/**
	 * 取消订阅单个设备
	 */
	@Override
	public void DisSubscribeforDevId(String devId) throws MqttException {
		// TODO Auto-generated method stub
		super.DisSubscribeforDevId(devId);
	}

	/**
	 * 取消订阅账户下的所有设备
	 */
	@Override
	public void DisSubscribeforuName() throws MqttException {
		// TODO Auto-generated method stub
		super.DisSubscribeforuName();
	}
	

	/**
	 * 取消订阅账号下的子用户的所有设备
	 */
	@Override
	public void DisSubscribeforuName(String userName) throws MqttException {
		// TODO Auto-generated method stub
		super.DisSubscribeforuName(userName);
	}

	/**
	 * 向指定设备推送数据，通过设备ID指定目标设备
	 */
	@Override
	public void publishForDevId(String devId, byte[] data) throws MqttException {
		// TODO Auto-generated method stub
		super.publishForDevId(devId, data);
	}

	/**
	 * 向用户下的所有设备发送数据
	 */
	@Override
	public void publishForuName(byte[] data) throws MqttException {
		// TODO Auto-generated method stub
		super.publishForuName(data);
	}

	/**
	 * 向本账号指定的子用户的设备推送数据
	 */
	@Override
	public void publishForuName(String userName, byte[] data) throws MqttException {
		// TODO Auto-generated method stub
		super.publishForuName(userName, data);
	}

	

}
