package com.myanycam.bean;

import com.myanycamm.utils.ELog;

public abstract class EventInfo {
	
	private final String TAG = "EventInfo";
	protected String totalName;
	protected int alertType;

	public abstract void parsePic(String totalName);
	

	public int getAlertType() {
		return alertType;
	}

	public void setAlertType(int alertType) {
		this.alertType = alertType;
	}

	public String getTotalName() {
		return totalName;
	}

	public void setTotalName(String totalName) {
		this.totalName = totalName;
	}

	public String formatTime(String time) {
		ELog.i(TAG, "time:"+time);
		String result = time.substring(0, 4) + "-" + time.substring(4, 6) + "-"
				+ time.subSequence(6, 8) + " " + time.substring(8, 10) + ":"
				+ time.substring(10, 12)+":"+time.substring(12, 14);
		return result;
	}

}
