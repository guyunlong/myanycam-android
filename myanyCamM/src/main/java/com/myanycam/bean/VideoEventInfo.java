package com.myanycam.bean;

import java.io.File;

import com.myanycamm.ui.VideoEvent;
import com.myanycamm.utils.ELog;

public class VideoEventInfo extends EventInfo {
	private final String TAG = "VideoEventInfo";

	private String time;
	private boolean isDownLoad;//是否下载过	
	
	
	public boolean isDownLoad() {
		return isDownLoad;
	}

	public void setDownLoad(boolean isDownLoad) {
		this.isDownLoad = isDownLoad;
	}


	public void parsePic(String totalName) {

		setTotalName(totalName);
		File file = new File(android.os.Environment.getExternalStorageDirectory()
				.getPath()+"/myanycam/video/"+totalName);
		if (file.exists()) {
			setDownLoad(true);
		}else{
			setDownLoad(false);
		}
//		ELog.i(TAG, totalName);
		String[] s1 = totalName.split("_");
		String suffix = totalName.substring((totalName.length()-4), totalName.length());
		ELog.i(TAG, "suf:"+suffix);
		ELog.i(TAG, "格式化:"+formatTime(s1[0]));
		try {			
			setTime(formatTime(s1[0]));
		} catch (ArrayIndexOutOfBoundsException e) {
			ELog.i(TAG, "视频名字没那么长...");
		}catch (NumberFormatException e) {
			ELog.i(TAG, "文件格式不对");
		}		
	}
	
	



	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}
	
}
