package com.myanycamm.update;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;

import com.myanycamm.utils.ELog;

public class UpdateService extends Service {
	private String tag = "UpdateService";

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		ELog.i(tag, "onBind...");
		return null;
	}

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		ELog.i(tag, "onCreate...");
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		ELog.i(tag, "onDestroy...");
		super.onDestroy();
	}

	@Override
	public void onRebind(Intent intent) {
		ELog.i(tag, "onRebind...");
		super.onRebind(intent);
	}

	@Override
	public void onStart(Intent intent, int startId) {
		ELog.i(tag, "onStart...");
		super.onStart(intent, startId);
		if(intent==null){
			return;
		}
		String url = intent.getStringExtra("URL");
		try {
			UpdateManager.getInstance(getApplicationContext()).downloadApkFile(url);
		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(getApplicationContext(),
					 e.getMessage(),
					Toast.LENGTH_LONG).show();
			UpdateManager.getInstance(getApplicationContext()).showFailedActivity(e.getMessage());
		}
	}

	@Override
	public boolean onUnbind(Intent intent) {
		// TODO Auto-generated method stub
		ELog.i(tag, "onUnbind...");
		return super.onUnbind(intent);
	}

}
