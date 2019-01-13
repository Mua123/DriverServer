package com.pioteks.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.PropertyConfigurator;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pioteks.model.RequestResponse;
import com.pioteks.server.NBServer;
import com.pioteks.server.WebServer;
import com.pioteks.timer.ReconnectTimerTask;
import com.pioteks.usr.ClientAdapter;
import com.pioteks.usr.ClientGXConsumer;
import com.pioteks.usr.ClientJSConsumer;
import com.pioteks.usr.ClinetCallbackAdapter;
import com.pioteks.utils.CommandInstance;

public class Main {
	private static final Logger logger = (Logger) LoggerFactory.getLogger(Main.class);
	
	/* 1.初始化客户端适配 */
	public static ClientAdapter clientAdapter;
	
	/* 2.初始化客户端回调适配 */
	public static ClinetCallbackAdapter clinetCallbackAdapter;
	
	public static void main(String[] args) throws IOException {
		int NBPort = -1;
		int WebPort = -1;
		int WebPortGx = -1;
		
		
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
	    if(prop.getProperty("Web_port_gx") != null) {
	    	WebPortGx = Integer.parseInt(prop.getProperty("Web_port_gx"));
	    }
		in.close();
		//启动NB接收服务
		if(NBPort == -1) {
			NBServer.startNBServer(12344);
		}else {
			NBServer.startNBServer(NBPort);
		}

//		//启动web_TCP服务
//		if(WebPort == -1) {
//			WebServer.startWebServer(12346);
//		}else {
//			WebServer.startWebServer(WebPort);
//		}
		
//		if(WebPort == -1) {
//			WebServer.startWebServerGX(12347);
//		}else {
//			WebServer.startWebServerGX(WebPortGx);
//		}
		
		Timer timer = new Timer();
		timer.schedule(new ReconnectTimerTask(), 5000, 6 * 60 * 60 * 1000);
		//初始化指令表和模式。
		Main.init();
		
	}
	
	private static void init() throws IOException {
		
		CommandInstance command = CommandInstance.getCommandInstance();
		
		List<RequestResponse> commandListMode1 = new ArrayList<RequestResponse>();
		
		commandListMode1.add(new RequestResponse("41-B0-5A", "00-ff-00"));
		commandListMode1.add(new RequestResponse("41-90-5A", "00-ff-00"));
		commandListMode1.add(new RequestResponse("41-A0-5A", "00-ff-00"));
		commandListMode1.add(new RequestResponse("41-80-5A", "00-ff-00"));//震动
		commandListMode1.add(new RequestResponse("41-30-5A", "00-ff-00"));//内井盖开启
		commandListMode1.add(new RequestResponse("41-10-5A", "00-ff-00"));
		commandListMode1.add(new RequestResponse("41-20-5A", "00-ff-00"));//外井盖开启
		commandListMode1.add(new RequestResponse("41-00-5A", "00-ff-00"));//正常
//		commandListMode1.add(new RequestResponse("41-38-5A", "00-ff-00"));
//		commandListMode1.add(new RequestResponse("41-0C-5A", "00-ff-00"));
//		commandListMode1.add(new RequestResponse("41-1C-5A", "00-ff-00"));
//		commandListMode1.add(new RequestResponse("41-2C-5A", "00-ff-00"));
//		commandListMode1.add(new RequestResponse("41-3C-5A", "00-ff-00"));
		
		List<RequestResponse> commandListMode2 = new ArrayList<RequestResponse>();
		
		commandListMode2.add(new RequestResponse("00-41-00", "00-41-00"));
		commandListMode2.add(new RequestResponse("00-42-00", "00-42-00"));
		
		command.setCommandListMode1(commandListMode1);
		command.setCommandListMode2(commandListMode2);
		command.setMode(CommandInstance.ALARM);
		
		BlockingQueue<byte[]> jsQueue = new LinkedBlockingQueue<>();
		BlockingQueue<byte[]> gxQueue = new LinkedBlockingQueue<>();
		
		runSend(jsQueue, gxQueue);
		runAccept(jsQueue, gxQueue);
	}
	
	/**
	 * 运行发送模块
	 * @param jsQueue
	 * @param gxQueue
	 */
	private static void runSend(BlockingQueue<byte[]> jsQueue, BlockingQueue<byte[]> gxQueue) {
		System.out.println("sender");
		new Thread(new ClientJSConsumer(jsQueue)).start();
		new Thread(new ClientGXConsumer(gxQueue)).start();
	}

	/**
	 * 运行有人云平台接收模块
	 * @param queue 消息队列 
	 * @param gxQueue 
	 */
	public static void runAccept(BlockingQueue<byte[]> jsQueue, BlockingQueue<byte[]> gxQueue) {
		clientAdapter = new ClientAdapter(clinetCallbackAdapter);
		clinetCallbackAdapter = new ClinetCallbackAdapter(clientAdapter, jsQueue, gxQueue);
		logger.info(new Date()+"UsrThread start");
		System.out.println(new Date()+"UsrThread");
		try {
			receiveByUsr();
		}  catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void receiveByUsr() throws MqttException, InterruptedException {
		
		  /* 3.客户端设置回调 */
		  clientAdapter.setUsrCloudMqttCallback(clinetCallbackAdapter);

		  /* 4.进行连接 */
		  clientAdapter.Connect("Mua123", "zhyyly4243");
		  //等待连接
		  Thread.sleep(500);
		//订阅账户所有设备
		  clientAdapter.SubscribeForUsername();
		  while(true) {
			  //断线重连
			  Thread.sleep(1000 * 60 * 60);
			  while(!clientAdapter.connect) {
				  clientAdapter.Connect("Mua123", "zhyyly4243");
				  Thread.sleep(500);
				  //订阅账户所有设备
				  clientAdapter.SubscribeForUsername();
			  }
		  }
		  
	}

	
}
