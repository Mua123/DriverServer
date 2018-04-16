package com.pioteks.main;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.piokeks.model.RequestResponse;
import com.pioteks.server.NBServer;
import com.pioteks.server.WebServer;
import com.pioteks.utils.CommandInstance;

public class Main {
	
	public static void main(String[] args) throws IOException {
		int NBPort = -1;
		int WebPort = -1;
		
		//ͨ�������ļ����ö˿�
		
		
		//����NB���շ���
		if(NBPort == -1) {
			NBServer.startNBServer(12344);
		}else {
			NBServer.startNBServer(NBPort);
		}

		//����web_TCP����
		if(WebPort == -1) {
			WebServer.startWebServer(12346);
		}else {
			WebServer.startWebServer(WebPort);
		}
		
		//��ʼ��ָ����ģʽ��
		Main.init();
	}
	
	private static void init() {
		CommandInstance command = CommandInstance.getCommandInstance();
		
		List<RequestResponse> commandListMode1 = new ArrayList<RequestResponse>();
		
		commandListMode1.add(new RequestResponse("00-41-00", "00-ff-00"));
		commandListMode1.add(new RequestResponse("00-42-00", "00-ff-00"));
		commandListMode1.add(new RequestResponse("00-43-00", "00-ff-00"));
		
		List<RequestResponse> commandListMode2 = new ArrayList<RequestResponse>();

		commandListMode2.add(new RequestResponse("00-41-00", "00-41-00"));
		commandListMode2.add(new RequestResponse("00-42-00", "00-42-00"));
		
		command.setCommandListMode1(commandListMode1);
		command.setCommandListMode2(commandListMode2);
		command.setMode(CommandInstance.ALARM);
	}
	
}
