package com.pioteks.utils;

import org.apache.mina.core.session.IoSession;


/**
 * ����web����˵�TCP IoSession����
 * @author zhy
 *
 */
public class WebServerSessionInstance {
	private static WebServerSessionInstance webServerSessionInstance;
	
	private IoSession webServerSession;
	
	private WebServerSessionInstance() {
		
	}
	
	public static WebServerSessionInstance getWebServerSessionInstance() {
		if( webServerSessionInstance == null) {
			webServerSessionInstance = new WebServerSessionInstance();
		}
		return webServerSessionInstance;
	}

	public IoSession getWebServerSession() {
		return webServerSession;
	}

	public void setWebServerSession(IoSession webServerSession) {
		this.webServerSession = webServerSession;
	}
	
	

}
