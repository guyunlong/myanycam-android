package com.myanycam.bean;

import com.myanycamm.utils.ELog;

public class CameraWifiInfo {

	private static String TAG = "CameraWifiInfo";

	private String ssid;
	private String safety;
	private String safe;//数字
	private String imageSignal;
	private int signalLevel;
	private String password;
	// private String signal;
	// private boolean isOpen;
	private Boolean isCurrenLink;// 是否当前正在使用
	
	

	public String getSafe() {
		return safe;
	}

	public void setSafe(String safe) {
		this.safe = safe;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public CameraWifiInfo(String ssid, int safety, String signal,
			Boolean isCurrenLink) {
		setSsid(ssid);
		setSafety(safety);
//		setSingalLevel(signal);
		setSingalLevelWithPercent(signal);
		setIsCurrenLink(isCurrenLink);
	}

	public int getSignalLevel() {
		return signalLevel;
	}

	public void setSignalLevel(int signalLevel) {
		this.signalLevel = signalLevel;
	}

	public String getImageSignal() {
		return imageSignal;
	}

	public void setImageSignal(String imageSignal) {
		this.imageSignal = imageSignal;
	}

	
	public void setSingalLevel(String signal) {
		if (signal == null) {
			this.signalLevel = 4;
			return;
		}
		String temp = signal.substring(0, 4).trim();
		int tempInt = Integer.parseInt(temp);
		int singalLv = 1;
		if (tempInt <= -116) {
			singalLv = 1;
		} else if (-116 < tempInt && tempInt <= -80) {
			singalLv = 2;
		} else if (-80 < tempInt && tempInt <= -50) {
			singalLv = 3;
		} else if (-50 < tempInt) {
			singalLv = 4;
		}
		this.signalLevel = singalLv;
		ELog.i(TAG, "WifiInfo" + tempInt + "  " + this.signalLevel);
	}
	
	public void setSingalLevelWithPercent(String signal){
		if (signal == null) {
			this.signalLevel = 4;
			return;
		}
		String temp = signal.trim();
		int tempInt = Integer.parseInt(temp);
		if (tempInt < 10) {
			this.signalLevel = 1;
	        return;
	    }
	    
	    if (tempInt < 30) {
	    	this.signalLevel = 2;
	        return;
	    }
	    
	    if (tempInt < 45) {
	    	this.signalLevel = 3;
	        return;
	    }	    

	}

	// public WifiInfo(Boolean isCurrenLink, String ssid, String safety,
	// String signal) {
	//
	// }

	public Boolean getIsCurrenLink() {
		return isCurrenLink;
	}

	public void setIsCurrenLink(Boolean isCurrenLink) {
		this.isCurrenLink = isCurrenLink;
	}

	public void setIsCurrenLink(String cmdType) {
		if (cmdType.equals("WIFI_INFO")) {
			this.isCurrenLink = true;
		} else {
			this.isCurrenLink = false;
		}
	}

	public String getSsid() {
		return ssid;
	}

	public void setSsid(String ssid) {
		this.ssid = ssid;
	}

	public String getSafety() {
		return safety;
	}

	public void setSafety(String safety) {
		this.safety = safety;
	}

	public void setSafety(int safe) {
		switch (safe) {
		case 0:
			//WPA
			this.safe = "0";
			this.safety = "WPA";
			this.imageSignal = "_lock";
			break;
		case 1:
			//WPA2
			this.safe = "1";
			this.safety = "WPA2";
			this.imageSignal = "_lock";
			break;
		case 2:
			//NONE
			this.safe = "2";
			this.safety = "NONE";			
			this.imageSignal = "";
			break;

		default:
			this.safe = "1";
			this.safety = "WPA2";
			this.imageSignal = "_lock";
			break;
		}
	}
}
