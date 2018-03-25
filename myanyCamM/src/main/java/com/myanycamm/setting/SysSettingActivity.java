package com.myanycamm.setting;

import java.util.HashMap;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.myanycam.net.SocketFunction;
import com.myanycamm.cam.BaseActivity;
import com.myanycamm.cam.R;
import com.myanycamm.utils.SharePrefereUtils;

public class SysSettingActivity extends BaseActivity {

	private static String TAG = "SysSettingActivity";
	LinearLayout mainLayout = null;
	TextView headTitle;
	private SocketFunction sf;
	SharedPreferences sp;
	private TextView timeZone,camSn,camProducter;	

	private HashMap<String, String> map;
	
	public static final int DEVICE_INFO= 701;

	
	private Handler mHandler = new Handler(){
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case DEVICE_INFO:
				Bundle bundle = msg.getData();
				map = (HashMap) bundle.getSerializable("data");
				saveInfo();
				initSharePrefrenceView();
				break;

			default:
				break;
			}
		};
	};
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.device_info);
		mainLayout = (LinearLayout) findViewById(R.id.mainLayout);
		Button settingBack = (Button) findViewById(R.id.settings_back);
		settingBack.setVisibility(View.VISIBLE);
		settingBack.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		headTitle = (TextView) findViewById(R.id.settings_head_title);
		headTitle.setText(getString(R.string.sys_setting));
		sf = (SocketFunction) getApplication();
		sf.setmHandler(mHandler);
		sp = getSharedPreferences("SP", MODE_PRIVATE);
		timeZone = (TextView) findViewById(R.id.timezone);
		camSn = (TextView) findViewById(R.id.cam_sn);
		camProducter = (TextView) findViewById(R.id.cam_producter);

		if(sp.contains("password")){
			initSharePrefrenceView();
		}
		sf.getDeviceConfig();
	}


//	@Override
//	public void finish() {
//		if (camPassword.getText().toString().length() < 8) {
//			Toast.makeText(SysSettingActivity.this, R.string.new_psw_short, Toast.LENGTH_SHORT).show();
//			return;
//		}
//		SharePrefereUtils.commitStringData(SysSettingActivity.this, "password", camPassword.getText().toString());
//		sf.setDeviceConfig(camPassword.getText().toString());
//		super.finish();
//	}

	
	private void saveInfo(){
		Context ctx = SysSettingActivity.this;
		SharePrefereUtils.commitStringData(ctx, "timezone",map.get("timezone"));
		SharePrefereUtils.commitStringData(ctx, "password",map.get("password"));
		SharePrefereUtils.commitStringData(ctx, "sn",map.get("sn"));
		SharePrefereUtils.commitStringData(ctx, "producter",map.get("producter"));
	}
	
	
	private void initSharePrefrenceView(){
		map = (HashMap<String, String>) sp.getAll();
//		camPassword.setText(map.get("password"));
//		camPassword.setSelection(camPassword.getText().length());
		timeZone.setText(map.get("timezone"));
		camSn.setText(map.get("sn"));
		camProducter.setText(map.get("producter"));
		
	}


}
