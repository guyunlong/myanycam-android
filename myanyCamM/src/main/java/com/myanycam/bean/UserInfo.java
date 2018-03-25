package com.myanycam.bean;

public class UserInfo {
	private String name;
	private String password;
	private int userId;
	private int action = 0;//
	private int loginType = 0;//登录类型
	private String loginToken;//用户用其他系统登录时使用	
	private String natIp;
//	private boolean isOnline;
	
	

//	public boolean isOnline() {
//		return isOnline;
//	}

//	public void setOnline(boolean isOnline) {
//		this.isOnline = isOnline;
//	}

	
	
	public int getLoginType() {
		return loginType;
	}

	public String getNatIp() {
		return natIp;
	}

	public void setNatIp(String natIp) {
		this.natIp = natIp;
	}

	public void setLoginType(int loginType) {
		this.loginType = loginType;
	}

	public String getLoginToken() {
		return loginToken;
	}

	public void setLoginToken(String loginToken) {
		this.loginToken = loginToken;
	}

	public int getUserId() {
		return userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public int getAction() {
		return action;
	}

	public void setAction(int action) {
		this.action = action;
	}
	
	

}
