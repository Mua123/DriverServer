package com.pioteks.utils;

import org.apache.mina.core.session.IoSession;


/**
 * 保存web服务端的TCP IoSession对象
 * @author zhy
 *
 */
public class WebServerSessionInstance {
	private static WebServerSessionInstance webServerSessionInstance;
	
	private IoSession webServerSession;
	private IoSession webServerGXSession;
	
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

	public IoSession getWebServerGXSession() {
		return webServerGXSession;
	}

	public void setWebServerGXSession(IoSession webServerGXSession) {
		this.webServerGXSession = webServerGXSession;
	}
	

}
