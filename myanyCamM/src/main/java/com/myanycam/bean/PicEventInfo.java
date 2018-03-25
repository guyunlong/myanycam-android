package com.myanycam.bean;

import com.myanycamm.utils.ELog;

public class PicEventInfo extends EventInfo {
	private final static String TAG = "PictureInfo";	
	private String time;
	private boolean isVideo;
	private int videoType;
	private String videoName;
	private String eventUrl;
	
	
	public String getEventUrl() {
		return eventUrl;
	}

	public void setEventUrl(String eventUrl) {
		this.eventUrl = eventUrl;
	}

	public int getVideoType() {
		return videoType;
	}

	public void setVideoType(int videoType) {
		this.videoType = videoType;
	}

	public boolean isVideo() {
		return isVideo;
	}

	public void setVideo(boolean isVideo) {
		this.isVideo = isVideo;
	}

	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}

	public String getVideoName() {
		return videoName;
	}

	public void setVideoName(String videoName) {
		this.videoName = videoName;
	}
	
	public void parsePic(String totalName){
		ELog.i(TAG, "totalName:"+totalName);
		setTotalName(totalName);
//		ELog.i(TAG, totalName);
		String[] s1 = totalName.split("_");
		String suffix = totalName.substring((totalName.length()-4), totalName.length());
		ELog.i(TAG, "suf:"+suffix);
		ELog.i(TAG, "格式化:"+formatTime(s1[0]));
		try {			
			alertType = Integer.parseInt(s1[1]);			
			setTime(formatTime(s1[0]));
			setVideo(s1[2].equals("1"));
			videoType = Integer.parseInt(s1[2]);
			if (s1.length>2) {
				setVideoName(s1[3].substring(0, s1[3].length()-4));
			}
			
		} catch (ArrayIndexOutOfBoundsException e) {
			ELog.i(TAG, "图片名字没那么长...");
		}catch (NumberFormatException e) {
			ELog.i(TAG, "文件格式不对");
		}

//		ELog.i(TAG, "videoname:"+videoName);
	}
	
	public static int getAlarmType(String totalName){
		String[] s1 = totalName.split("_");
		String suffix = totalName.substring((totalName.length()-4), totalName.length());
		try {
			if (s1[1].length()>1) {
				return 0;
			}
		} catch (Exception e) {
			ELog.i(TAG, "获得报警类型出错了");
			return 0;
		}

		return Integer.parseInt(s1[1]);		
	}

}
