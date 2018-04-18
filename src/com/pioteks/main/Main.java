package com.pioteks.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.piokeks.model.RequestResponse;
import com.pioteks.server.NBServer;
import com.pioteks.server.WebServer;
import com.pioteks.utils.CommandInstance;

public class Main {
	private static final Logger logger = (Logger) LoggerFactory.getLogger(Main.class);
	
	public static void main(String[] args) throws IOException {
		int NBPort = -1;
		int WebPort = -1;
		
		
		//设置log4j配置
		String path = System.getProperty("user.dir")+File.separator;
		
		PropertyConfigurator.configure(path + "log4j.properties");
		logger.error("lauach log properties");
		logger.error("log file path:" + path + "log4j.properties");
		//通过配置文件设置端口
		Properties prop=new Properties();
	    InputStream in=new FileInputStream(new File(System.getProperty("user.dir")+File.separator+"DriverServer.properties"));
	    prop.load(in);
	    
	    if(prop.getProperty("NB_port") != null){
	    	NBPort = Integer.parseInt(prop.getProperty("NB_port"));
	    }
	    if(prop.getProperty("Web_port") != null) {
	    	WebPort = Integer.parseInt(prop.getProperty("Web_port"));
	    }
		in.close();
		//启动NB接收服务
		if(NBPort == -1) {
			NBServer.startNBServer(12344);
		}else {
			NBServer.startNBServer(NBPort);
		}

		//启动web_TCP服务
		if(WebPort == -1) {
			WebServer.startWebServer(12346);
		}else {
			WebServer.startWebServer(WebPort);
		}
		
		//初始化指令表和模式。
		Main.init();
	}
	
	private static void init() throws IOException {
		
		CommandInstance command = CommandInstance.getCommandInstance();
		
		List<RequestResponse> commandListMode1 = new ArrayList<RequestResponse>();
		
		commandListMode1.add(new RequestResponse("41-08-5A", "00-ff-00"));
		commandListMode1.add(new RequestResponse("41-18-5A", "00-ff-00"));
		commandListMode1.add(new RequestResponse("41-28-5A", "00-ff-00"));
		commandListMode1.add(new RequestResponse("41-38-5A", "00-ff-00"));
		commandListMode1.add(new RequestResponse("41-0C-5A", "00-ff-00"));
		commandListMode1.add(new RequestResponse("41-1C-5A", "00-ff-00"));
		commandListMode1.add(new RequestResponse("41-2C-5A", "00-ff-00"));
		commandListMode1.add(new RequestResponse("41-3C-5A", "00-ff-00"));
		
		List<RequestResponse> commandListMode2 = new ArrayList<RequestResponse>();
		
		commandListMode2.add(new RequestResponse("00-41-00", "00-41-00"));
		commandListMode2.add(new RequestResponse("00-42-00", "00-42-00"));
		
		command.setCommandListMode1(commandListMode1);
		command.setCommandListMode2(commandListMode2);
		command.setMode(CommandInstance.ALARM);
	}
	
}
