package com.pioteks.utils;

import java.util.List;

import com.pioteks.model.RequestResponse;

public class CommandInstance {
	
	private static CommandInstance commandInstance;
	
	private List<RequestResponse> commandListMode1;
	private List<RequestResponse> commandListMode2;
	
	private boolean mode;

	private int delayTime = 100;
	
	private String lastStatusOne;
	private String lastStatusTwo;
	private String lastStatusThree;
	
	/**
	 * ±¨¾¯½ÓÊÜ×´Ì¬
	 */
	public final static boolean ALARM = false;
	/**
	 * ¾®¸Ç²Ù×÷×´Ì¬
	 */
	public final static boolean OPERATE = true;
	
	private CommandInstance() {
		
	}
	
	
	
	public static CommandInstance getCommandInstance() {
		if(commandInstance == null)
			commandInstance = new CommandInstance();
		return commandInstance;
	}
	

	public List<RequestResponse> getCommandListMode1() {
		return commandListMode1;
	}

	public void setCommandListMode1(List<RequestResponse> commandListMode1) {
		this.commandListMode1 = commandListMode1;
	}

	public List<RequestResponse> getCommandListMode2() {
		return commandListMode2;
	}

	public void setCommandListMode2(List<RequestResponse> commandListMode2) {
		this.commandListMode2 = commandListMode2;
	}

	public boolean getMode() {
		return mode;
	}

	public void setMode(boolean mode) {
		this.mode = mode;
	}

	public int getDelayTime() {
		return delayTime;
	}

	public void setDelayTime(int delayTime) {
		this.delayTime = delayTime;
	}



	public String getLastStatusOne() {
		return lastStatusOne;
	}



	public void setLastStatusOne(String lastStatusOne) {
		this.lastStatusOne = lastStatusOne;
	}



	public String getLastStatusTwo() {
		return lastStatusTwo;
	}



	public void setLastStatusTwo(String lastStatusTwo) {
		this.lastStatusTwo = lastStatusTwo;
	}



	public String getLastStatusThree() {
		return lastStatusThree;
	}



	public void setLastStatusThree(String lastStatusThree) {
		this.lastStatusThree = lastStatusThree;
	}



	
	
	
	
}
