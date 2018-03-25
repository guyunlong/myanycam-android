package com.myanycamm.process;

import android.os.Bundle;
import android.os.Message;

import com.myanycam.net.SocketFunction;
import com.myanycamm.cam.AppServer;
import com.myanycamm.ui.DialogFactory;
import com.myanycamm.utils.ELog;


public class HeartBeatThread extends Thread {
	
	private final String TAG = "HeartBeatThread";
	private static HeartBeatThread instance;	

	private HeartBeatThread() {
		// TODO Auto-generated constructor stub
	}
	
	public static HeartBeatThread getInstance(){
		if (null == instance) {
			instance = new HeartBeatThread();
		}		
		return instance;
	}
	
	public void closeThread(){
		if (null != instance) {
			instance.interrupt();
			instance = null;
		}
	}
	@Override
	public void run() {
		while (!AppServer.stop) {
			ELog.i(TAG, "Appser发送心跳包");
		
			SocketFunction.getInstance().sendHeartBeat();
//			HttpUtil.queryStringForPost("http://192.168.1.100/test/wirtetxt.php?content=heartbeat");
			if (AppServer.heartCount>1) {
//				HttpUtil.queryStringForPost("http://192.168.1.100/test/wirtetxt.php?content=heartbeat..........2");
				ELog.i(TAG, "两次心跳包没接收了..要停止");
				Bundle mBundle = new Bundle();
				mBundle.putString("key", "" );
				Message msg = new Message();
				msg.what = 1;
				msg.setData(mBundle);
				DialogFactory.dialogHandler.sendMessage(msg);
			}
			AppServer.heartCount++;
			try {
				Thread.sleep(10*1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	


}
