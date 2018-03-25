package com.myanycamm.cam;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.myanycam.bean.MainSocket;
import com.myanycam.net.SocketFunction;
import com.myanycamm.process.ScreenManager;
import com.myanycamm.utils.ELog;

public class SystemActivity extends BaseActivity implements OnClickListener {
	private static String TAG = "SystemActivity";
	private SocketFunction sf;

	private LinearLayout userNameLayout, changPwdLayout, aboutLayout;

	View setView;
	TextView headTitle, userName;
	Button headLeftButton, logoutBtn;

	private OnClickListener logoutClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			// 清除用户记录
			SharedPreferences sp = getSharedPreferences("passwordFile",
					MODE_PRIVATE);
			String[] allUserName = sp.getAll().keySet().toArray(new String[0]);
			sp.edit().putString(allUserName[0], "").commit();
			// sp.edit().clear().commit();
			// MyTimerTask.getInstance().closeTimer();
			MainSocket.closeMain();
			AppServer.stop = true;
			Intent intentService = new Intent("com.android.myanycamm.AppServer");
			stopService(intentService);
			ScreenManager.getScreenManager().popAllActivity();
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.system_activity);
		sf = (SocketFunction) getApplication();
		headTitle = (TextView) findViewById(R.id.settings_head_title);
		headLeftButton = (Button) findViewById(R.id.settings_back);
		headLeftButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				finish();
			}
		});
		logoutBtn = (Button) findViewById(R.id.log_out_btn);
		logoutBtn.setOnClickListener(logoutClickListener);
		userNameLayout = (LinearLayout) findViewById(R.id.username_layout);
		userNameLayout.setOnClickListener(this);
		aboutLayout = (LinearLayout) findViewById(R.id.about);
		aboutLayout.setOnClickListener(this);
		changPwdLayout = (LinearLayout) findViewById(R.id.chang_pwd_layout);
		changPwdLayout.setOnClickListener(this);
		userName = (TextView) findViewById(R.id.user_name);
		userName.setText(SocketFunction.getInstance().userInfo.getName());
		setHead();

	}

	public void setHead() {
		ELog.i(TAG, "隐藏列表");
		headTitle.setText(R.string.sys_setting);// 默认为摄像头列表
		headLeftButton.setVisibility(View.VISIBLE);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.chang_pwd_layout:
			Intent intent1 = new Intent(SystemActivity.this,
					ChangPasswordActivity.class);
			startActivity(intent1);
			break;

		case R.id.about:
			Intent intentAbout = new Intent(SystemActivity.this,
					AboutActivity.class);
			startActivity(intentAbout);
			break;
		case R.id.username_layout:
			break;

		default:
			break;
		}

	}

}
