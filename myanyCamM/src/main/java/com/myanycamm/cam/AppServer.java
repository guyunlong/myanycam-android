package com.myanycamm.cam;

import java.util.HashMap;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.SystemClock;

import com.myanycam.net.SocketFunction;
import com.myanycamm.model.AlarmReceiver;
import com.myanycamm.process.DoCmdThread;
import com.myanycamm.process.ReceiveThread;
import com.myanycamm.process.ScreenManager;
import com.myanycamm.utils.ELog;

public class AppServer extends Service {

	private final static String TAG = "AppServer";
	public static boolean isBackgroud = true;// 后台标志
	public static boolean isAp = false;
	public static boolean stop = false;
	public static boolean isDisplayVideo = false;// 控制视频是否显示
	private ReceiveThread mReceiveThread = null;
	boolean a = true;
	boolean b = true;
	boolean c = true;
	public static int heartCount = 0;
	AlarmManager am;
	PendingIntent sender;
	private final BroadcastReceiver powerReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(final Context context, final Intent intent) {
			if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
				ELog.i(TAG, "电源关闭");
				startRepeatService();
//				ScreenManager.getScreenManager().popAllActivity();
				// HeartBeatThread.getInstance().setHeart(false);
				// HeartBeatThread.getInstance().closeThread();
			}

			if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
				// HeartBeatThread.getInstance().setHeart(true);
				// HeartBeatThread.getInstance().start();
				ELog.i(TAG, "电源打开");
				if (am != null) {
					am.cancel(sender);
				}
				// HeartBeatThread.getInstance().start();
				// try {
				// OutputStream out;
				// out = TestSocket.getInstance().getOutputStream();
				// out.write(4);
				// out.flush();
				// } catch (IOException e) {
				//
				// e.printStackTrace();
				// }
			}

		}

	};

	@Override
	public void onCreate() {
		super.onCreate();
		ELog.i(TAG, "启动了！");
		// super.onStart(intent, startId);
	};

	@Override
	public void onStart(Intent intent, int startId) {
		if (intent == null) {
			this.stopSelf();
			return;
		}
		ELog.i(TAG, "启动了服务" + intent.getFlags());
		ReceiveThread.getInstance().start();
		registerScreenActionReceiver();
		// startRepeatService();
		if (intent.getFlags() == 110) {
			HashMap<String, String> dataMap = new HashMap<String, String>();
			dataMap.put("cmd", "START_HEART");
			DoCmdThread.cmdMaps.add(dataMap);
//			new DoCmdThread(dataMap).start();
		}
	}

	@Override
	public void onDestroy() {
		ELog.i(TAG, "停止服务");
		try {
			unregisterReceiver(powerReceiver);
		} catch (IllegalArgumentException e) {
			ELog.i(TAG, "没有注册..");
		}
	
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		ELog.i(TAG, "调用onStartCommand");
		flags = START_STICKY;
		return super.onStartCommand(intent, flags, startId);
	}

	private void relogin() {
		// if (!mReceiveThread.isAlive()) {
		// mReceiveThread = new ReceiveThread();
		// mReceiveThread.start();
		// }
		// heartCount = 0;
		// sf.reLogin();
	}

	private void registerScreenActionReceiver() {
		final IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		filter.addAction(Intent.ACTION_SCREEN_ON);
		// unregisterReceiver(powerReceiver);
		registerReceiver(powerReceiver, filter);
	}

	public void startRepeatService() {
		Intent intent = new Intent(ScreenManager.getScreenManager()
				.currentActivity(), AlarmReceiver.class);
		intent.setAction("sendheart");
		sender = PendingIntent.getBroadcast(ScreenManager.getScreenManager()
				.currentActivity(), 0, intent, 0);
		// 开始时间
		long firstime = SystemClock.elapsedRealtime();
		am = (AlarmManager) ScreenManager.getScreenManager().currentActivity()
				.getSystemService(Context.ALARM_SERVICE);// 10秒一个周期，不停的发送广播
		am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, firstime,
				10 * 1000, sender);
		// am.cancel(sender);
	}

}