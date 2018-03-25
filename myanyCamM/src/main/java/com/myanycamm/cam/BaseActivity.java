package com.myanycamm.cam;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;

import com.myanycamm.process.ScreenManager;
import com.umeng.analytics.MobclickAgent;

public class BaseActivity extends Activity {
	private final String TAG = "BaseActivity"; 

	protected BaseActivity mSelfAct;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		mSelfAct = this;
		ScreenManager.getScreenManager().pushActivity(this);
	}

	
	public void startActivity(Intent intent) {
		// TODO Auto-generated method stub
		super.startActivity(intent);
		 overridePendingTransition(R.anim.slide_left, R.anim.slide_left_out);
	}

	public void startActivity(Intent intent, int anim1, int anim2) {
		// TODO Auto-generated method stub
		super.startActivity(intent);
		overridePendingTransition(anim1, anim2);
	}

	public void startActivityNoAnimation(Intent intent) {
		// TODO Auto-generated method stub
		super.startActivity(intent);

	}

	

	public void finish() {
		super.finish();
		 overridePendingTransition(R.anim.slide_right,R.anim.slide_right_out);
			ScreenManager.getScreenManager().popActivity();
	}

	public void finishApp() {
		super.finish();	
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();	

	}

	@Override
	protected void onResume() {
		super.onResume();	
		MobclickAgent.onResume(this);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		MobclickAgent.onPause(this);
	}
	public void startActivityForResult(Intent intent, int requestCode,
			int anim1, int anim2) {
		// TODO Auto-generated method stub
		super.startActivityForResult(intent, requestCode);
		overridePendingTransition(anim1, anim2);
	}

}
