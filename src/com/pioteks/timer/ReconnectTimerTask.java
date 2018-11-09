package com.pioteks.timer;

import java.util.Date;
import java.util.TimerTask;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pioteks.main.Main;

public class ReconnectTimerTask extends TimerTask {

	private final Logger logger = (Logger) LoggerFactory.getLogger(getClass());
	
	@Override
	public void run() {
		logger.info(new Date() + " reconnect task start");
		try {
			Main.clientAdapter.DisConnectUnCheck();
			Thread.sleep(2000);
			System.out.println("��������");
			Main.clientAdapter.Connect("Mua123", "zhyyly4243");
			//�ȴ�����
			Thread.sleep(500);
			//�����˻������豸
			Main.clientAdapter.SubscribeForUsername();
		} catch (MqttException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
