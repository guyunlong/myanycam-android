package com.myanycamm.model;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.widget.Toast;

import com.myanycam.net.SocketFunction;
import com.myanycamm.cam.CallAcceptActivity;
import com.myanycamm.utils.ELog;

public class AlarmReceiver extends BroadcastReceiver {
	// 声明电源管理器
	private static String TAG = "AlarmReceiver";
	private Context mContext;

	@Override
	public void onReceive(Context context, Intent intent) {
		this.mContext = context;
		if (intent.getAction().equals("short")) {
			Toast.makeText(context, "short alarm", Toast.LENGTH_LONG).show();
		} else if (intent.getAction().equals("sendheart")) {
//			Toast.makeText(context, "发送心跳包", Toast.LENGTH_SHORT).show();
			ELog.i(TAG, "发送心跳包");
			SocketFunction.getInstance().sendHeartBeat();
	
		}else if(intent.getAction().equals("keepudp")){
		ELog.i(TAG, "保持udp");
//			SocketFunction.getInstance().mUdpSocket.keepLive();
		}else if(intent.getAction().equals("startcall")){
			ELog.i(TAG, "启动接收Activity");
			Intent acceptIntent = new Intent(mContext, CallAcceptActivity.class);
			acceptIntent.putExtras(intent.getExtras());
			startActivitySafely(acceptIntent);
//			ScreenManager.getScreenManager().currentActivity().startActivity(acceptIntent);
			}

	}
	
	void startActivitySafely(Intent intent) {
		PowerManager pm = (PowerManager)mContext.getSystemService(Context.POWER_SERVICE);   
		WakeLock wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakeLock");   
		wakeLock.acquire();  
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
	    try {   
	    	mContext.startActivity(intent);   
	    } catch (ActivityNotFoundException e) {   
	        Toast.makeText(mContext, "无法打开", Toast.LENGTH_SHORT).show();   
	    } catch (SecurityException e) {   
	        Toast.makeText(mContext, "安全问题，无法打开",   
	                Toast.LENGTH_SHORT).show();   
	    }   
	    wakeLock.release();
	}  


}
