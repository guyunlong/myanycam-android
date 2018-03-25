package com.myanycamm.setting;

import java.util.ArrayList;
import java.util.TimeZone;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.myanycam.bean.CameraListInfo;
import com.myanycam.bean.SettingsItem;
import com.myanycam.net.SocketFunction;
import com.myanycamm.cam.BaseActivity;
import com.myanycamm.cam.CameraCenterActivity;
import com.myanycamm.cam.R;
import com.myanycamm.process.ScreenManager;
import com.myanycamm.ui.SlipButton;

public class SettingActivity extends BaseActivity {

	private static String TAG = "SettingActivity";
	
	private final String LIST_SETTINGS_WATCH_CAMERA = "LIST_SETTINGS_WATCH_CAMERA";
	private final String LIST_SETTINGS_LOCAL_NET = "LIST_SETTINGS_LOCAL_NET";
	private final String LIST_SETTINGS_WIFI_NET = "LIST_SETTINGS_WIFI_NET";
	private final String LIST_SETTINGS_QUALITY = "LIST_SETTINGS_QUALITY";//视频质量设置

	TextView headTitle;
	
	public final static byte TYPE_COMMON = 0;
	
	public final static byte TYPE_BTN = 1;
	
	public final static byte TYPE_ARROW = 2;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_setting);
		headTitle = (TextView) findViewById(R.id.settings_head_title);
		headTitle.setText(getString(R.string.sys_setting));
		Button settingBack = (Button) findViewById(R.id.settings_back);
		Log.i(TAG, "system:"+System.currentTimeMillis()/1000);
		int timeZone = TimeZone.getDefault().getRawOffset()/3600000;
		long time = System.currentTimeMillis()/1000;
		CameraListInfo.currentCam = new CameraListInfo();
		SocketFunction sf = (SocketFunction) getApplication();
		sf.setTimeZone(time, timeZone);
		settingBack.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				ScreenManager.getScreenManager().popAllActivity();
//				finish();
			}
		});
		ArrayList<SettingsItem> settingsItems1 = new ArrayList<SettingsItem>();
		settingsItems1.add(new SettingsItem(
				getString(R.string.setting_category_titie_wifi_net),
				LIST_SETTINGS_WIFI_NET, TYPE_ARROW));
		settingsItems1.add(new SettingsItem(
				getString(R.string.setting_category_titie_local_net),
				LIST_SETTINGS_LOCAL_NET, TYPE_ARROW));
		ArrayList<SettingsItem> settingsItems2 = new ArrayList<SettingsItem>();
//		settingsItems2.add(new SettingsItem(
//				getString(R.string.quality_setting),
//				LIST_SETTINGS_QUALITY, TYPE_ARROW));

		ArrayList<SettingsItem> settingsItems3 = new ArrayList<SettingsItem>();
		settingsItems3.add(new SettingsItem(
				getString(R.string.watch_camera),
				LIST_SETTINGS_WATCH_CAMERA, TYPE_ARROW));
	
		LinearLayout mainLayout = (LinearLayout) findViewById(R.id.mainLayout);
		mainLayout.addView(genView(settingsItems1));
		mainLayout.addView(genView(settingsItems2));		
		mainLayout.addView(genView(settingsItems3));	
	}
	
	@Override
	public void onBackPressed() {
		Log.i(TAG, "按了返回键...");
		ScreenManager.getScreenManager().popAllActivity();
	}

	
//	@Override
//	public boolean onKeyDown(int keyCode, KeyEvent event) {
//		if (keyCode == KeyEvent.KEYCODE_BACK) {
//			ScreenManager.getScreenManager().popAllActivity();
//			return true;
//		}
//		return super.onKeyDown(keyCode, event);
//	}
	public View genView(ArrayList<SettingsItem> settingsItems) {
		LinearLayout layout = new LinearLayout(this);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		params.setMargins(0, 20, 0, 0);
		layout.setOrientation(LinearLayout.VERTICAL);
		for (int i = 0; i < settingsItems.size(); i++) {
			View view = genItemView(settingsItems, i);
			layout.addView(view);
		}
		layout.setLayoutParams(params);
		return layout;
	}

	public View genItemView(ArrayList<SettingsItem> settingsItems, int position) {
		View convertView = LayoutInflater.from(getApplicationContext())
				.inflate(R.layout.setting_item, null);
		convertView.setClickable(true);

		TextView nameTextView = (TextView) convertView
				.findViewById(R.id.local_settting_itemname);
		SlipButton slipButton = (SlipButton) convertView
				.findViewById(R.id.local_settting_slipbtn);
		ImageView arrowImgView = (ImageView) convertView
				.findViewById(R.id.local_setting_arrow);
		slipButton.setVisibility(View.GONE);

		int size = settingsItems.size();
		if (size > 1 && position == 0) {
			convertView
					.setBackgroundResource(R.drawable.privacy_setting_item_top_bg);
		} else if (size > 1 && position == size - 1) {
			convertView
					.setBackgroundResource(R.drawable.privacy_setting_item_bottom_bg);
		} else if (size > 1) {
			convertView
					.setBackgroundResource(R.drawable.privacy_setting_item_mid_bg);
		} else {
			convertView.setBackgroundResource(R.drawable.setting_item_bg);
		}

		final SettingsItem item = settingsItems.get(position);
		switch (item.type) {
		case TYPE_BTN:
			slipButton.setVisibility(View.VISIBLE);
			// slipButton.setButtonDefault(UserInfo.getIntance().loadNotifactionWeather(getApplicationContext()));
			// slipButton.SetOnChangedListener(slipBtnChangedListener,
			// SLIP_BTN_ID_WEATHER);
			break;
		case TYPE_ARROW:
			arrowImgView.setVisibility(View.VISIBLE);
			break;
		default:
			break;
		}
		nameTextView.setText(item.name);
		if (item.type != TYPE_BTN) {
			convertView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					getIntentById(item.id);
				}
			});
		}
		return convertView;
	}

	private void getIntentById(String id) {
		Intent intent = null;
		if (id.equalsIgnoreCase(LIST_SETTINGS_LOCAL_NET)) {
			intent = new Intent(SettingActivity.this,
					LocalNetSettingActivity.class);
			startActivity(intent);
		}
		if(id.equalsIgnoreCase(LIST_SETTINGS_WIFI_NET)){
			intent = new Intent(SettingActivity.this,
					WifiSettingActivity.class);
			startActivity(intent);
		}
		if (id.equalsIgnoreCase(LIST_SETTINGS_QUALITY)) {
			intent = new Intent(SettingActivity.this,
					QualitySettingActivity.class);
			startActivity(intent);;
		}
		
		if(id.equalsIgnoreCase(LIST_SETTINGS_WATCH_CAMERA)){
			intent = new Intent(SettingActivity.this,
					CameraCenterActivity.class);
			startActivity(intent);;
		}
	}


}
