package com.myanycamm.setting;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.myanycam.bean.CameraListInfo;
import com.myanycam.bean.SettingsItem;
import com.myanycam.net.SocketFunction;
import com.myanycamm.cam.AppServer;
import com.myanycamm.cam.BaseActivity;
import com.myanycamm.cam.R;
import com.myanycamm.utils.ELog;
import com.myanycamm.utils.SharePrefereUtils;

public class RecSettingActivity extends BaseActivity {

	private static final String TAG = "RecSettingActivity";

	private ImageView recAllday, recAlarm, isAlarmImg, moveAlarm, voiceAlarm;
	private TextView recAlldayTxt, recAlarmTxt;
	private LinearLayout recAlldayLayout, recAlarmLayout, isAlarmLayout,
			recLayout, alarmLayout, moveAlarmLayout, voiceAlarmLayout;
	private boolean isRec = true;// 默认为录像模式
	private int camId;
	String pre = "rec_";

	private SocketFunction sf;

	private final static int ISRECSPT = 1;
	private final static int ALLDAY = 2;
	private final static int TIMESELECT = 3;
	private TextView headTitle;
	public final static int GET_REC_CONFIG = 1;
	public final static int GET_ALARM_CONFIG = 2;
	private HashMap<String, String> map;

	public final static byte TYPE_COMMON = 0;

	public final static byte TYPE_BTN = 1;

	public final static byte TYPE_ARROW = 2;
	ArrayList<SettingsItem> settingsItems0 = new ArrayList<SettingsItem>();
	ArrayList<SettingsItem> settingsItems1 = new ArrayList<SettingsItem>();
	ArrayList<SettingsItem> settingsItems2 = new ArrayList<SettingsItem>();
	ArrayList<SettingsItem> settingsItems3 = new ArrayList<SettingsItem>();

	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			Bundle bundle = msg.getData();
			map = (HashMap) bundle.getSerializable("data");
			switch (msg.what) {
			case GET_REC_CONFIG:
			case GET_ALARM_CONFIG:
				svaeNetTimeInfo();
				initTime();
				break;
			default:
				break;
			}
		};
	};

	private OnClickListener recOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View arg0) {

			ELog.i(TAG, "点击了设置全天...");
			if (SharePrefereUtils.getStringWithKey(RecSettingActivity.this,
					pre + "policy").equals("0")) {
				ELog.i(TAG, "要关闭..");
				saveInfo("policy", "1");// 0为全天录像
				recAllday.setImageResource(R.drawable.off);
				recAlarm.setImageResource(R.drawable.on);
			} else {
				ELog.i(TAG, "要打开...");
				saveInfo("policy", "0");// 0为全天录像
				recAllday.setImageResource(R.drawable.on);
				recAlarm.setImageResource(R.drawable.off);
			}

		}
	};

	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_rec_setting);
		recLayout = (LinearLayout) findViewById(R.id.rec_layout);
		alarmLayout = (LinearLayout) findViewById(R.id.alarm_layout);
		recAlldayLayout = (LinearLayout) findViewById(R.id.rec_allday_layout);
		recAlarmLayout = (LinearLayout) findViewById(R.id.rec_alarm_layout);
		isAlarmLayout = (LinearLayout) findViewById(R.id.is_alarm_layout);
		moveAlarmLayout = (LinearLayout) findViewById(R.id.move_alarm_layout);
		voiceAlarmLayout = (LinearLayout) findViewById(R.id.voice_alarm_layout);
		isAlarmImg = (ImageView) findViewById(R.id.is_alarm_img);
		moveAlarm = (ImageView) findViewById(R.id.move_alarm_img);
		voiceAlarm = (ImageView) findViewById(R.id.voice_alarm_img);
		recAlldayTxt = (TextView) findViewById(R.id.rec_allday);
		recAlarmTxt = (TextView) findViewById(R.id.rec_alarm);

		if (AppServer.isAp) {
			recAlldayTxt.setText(R.string.auto_rec);
			recAlarmTxt.setText(R.string.manual_record);
		} else {
			recAlldayTxt.setText(R.string.all_day_rec);
			recAlarmTxt.setText(R.string.alarm_record);
		}
		recAllday = (ImageView) findViewById(R.id.is_allday_rec);
		recAlarm = (ImageView) findViewById(R.id.is_alarm_rec);

		recAllday.setOnClickListener(recOnClickListener);
		recAlarm.setOnClickListener(recOnClickListener);
		sf = (SocketFunction) getApplication();
		headTitle = (TextView) findViewById(R.id.settings_head_title);
		Intent intent = getIntent();
		if (null != intent) {
			//
			isRec = intent.getBooleanExtra("isRec", true);
			camId = intent.getIntExtra("camid", -1);
		}

		ELog.i(TAG, "camId:" + camId);
		saveInfo("camid", String.valueOf(camId));

		Button settingBack = (Button) findViewById(R.id.settings_back);
		settingBack.setVisibility(View.VISIBLE);
		settingBack.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});
		sf.setmHandler(mHandler);
		SharedPreferences sp = getSharedPreferences("SP", MODE_PRIVATE);
		if (isRec) {
			initRecView();
			recLayout.setVisibility(View.VISIBLE);
			alarmLayout.setVisibility(View.GONE);

			if (sp.contains("rec_switch")) {
				// 从本地读取数据
				initTime();
			}
			sf.getRecordConfig(CameraListInfo.currentCam);
		} else {
			ELog.i(TAG, "报警界面显示");
			recLayout.setVisibility(View.GONE);
			alarmLayout.setVisibility(View.VISIBLE);

			initAlarmView();
			if (sp.contains("ala_switch")) {
				// 从本地读取数据
				initTime();
			}
			isAlarmImg.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View arg0) {
					if (SharePrefereUtils.getStringWithKey(
							RecSettingActivity.this, pre + "switch")
							.equals("1")) {
						isAlarmImg.setImageResource(R.drawable.off);
						moveAlarmLayout.setVisibility(View.GONE);
						voiceAlarmLayout.setVisibility(View.GONE);
						saveInfo("switch", "0");
					} else {
						isAlarmImg.setImageResource(R.drawable.on);
						moveAlarmLayout.setVisibility(View.VISIBLE);
						voiceAlarmLayout.setVisibility(View.VISIBLE);
						saveInfo("switch", "1");
					}
				}
			});

			moveAlarm.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View arg0) {

					if (SharePrefereUtils.getStringWithKey(
							RecSettingActivity.this, pre + "movealarm").equals(
							"1")) {
						moveAlarm.setImageResource(R.drawable.off);
						saveInfo("movealarm", "0");
					} else {
						moveAlarm.setImageResource(R.drawable.on);
						saveInfo("movealarm", "1");
					}
				}
			});

			voiceAlarm.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View arg0) {
					voiceAlarm.setImageResource(map.get(pre + "voicealarm")
							.equals("1") ? R.drawable.on : R.drawable.off);
					if (SharePrefereUtils.getStringWithKey(
							RecSettingActivity.this, pre + "voicealarm")
							.equals("1")) {
						voiceAlarm.setImageResource(R.drawable.off);
						saveInfo("voicealarm", "0");
					} else {
						voiceAlarm.setImageResource(R.drawable.on);
						saveInfo("voicealarm", "1");
					}
				}
			});
			sf.getAlarmConfig(CameraListInfo.currentCam);
		}
	}

	@Override
	public void finish() {
		saveInfoToServer();
		super.finish();
	}

	private void initRecView() {
		headTitle.setText(R.string.rec_setting);

	}

	private void initAlarmView() {
		headTitle.setText(R.string.alarm_set);
	}

	private void saveInfo(String key, String value) {
		Context ctx = RecSettingActivity.this;
		if (isRec) {
			SharePrefereUtils.commitStringData(ctx, "rec_" + key, value);
		} else {
			SharePrefereUtils.commitStringData(ctx, "ala_" + key, value);
		}

	}

	private void saveInfoToServer() {
		if (isRec) {
			sf.setRecordConfig(CameraListInfo.currentCam);
		} else {
			sf.setAlarmConfig(CameraListInfo.currentCam);
		}
	}

	private void svaeNetTimeInfo() {
		// 报警设置特别处理
		if (!isRec) {
			saveInfo("movealarm", map.get("movealarm"));
			saveInfo("voicealarm", map.get("voicealarm"));
			saveInfo("record", map.get("record"));
		}
		saveInfo("repeat", map.get("repeat"));
		saveInfo("policy", map.get("policy"));// 录像策略
		saveInfo("switch", map.get("switch"));
		saveInfo("switch1", map.get("switch1"));
		saveInfo("switch2", map.get("switch2"));
		saveInfo("switch3", map.get("switch3"));
		saveInfo("switch4", map.get("switch4"));

		saveInfo("begintime1", map.get("begintime1"));
		saveInfo("endtime1", map.get("endtime1"));
		saveInfo("begintime2", map.get("begintime2"));
		saveInfo("endtime2", map.get("endtime2"));
		saveInfo("begintime3", map.get("begintime3"));
		saveInfo("endtime3", map.get("endtime3"));
		saveInfo("begintime4", map.get("begintime4"));
		saveInfo("endtime4", map.get("endtime4"));
	}

	private void initTime() {

		SharedPreferences sp = getSharedPreferences("SP", MODE_PRIVATE);
		map = (HashMap<String, String>) sp.getAll();
		ELog.i(TAG, "map:" + map);
		try {

			if (isRec) {
				pre = "rec_";

				ELog.i(TAG, "policy:" + map.get(pre + "policy"));

				recAllday
						.setImageResource(map.get(pre + "policy").equals("0") ? R.drawable.on
								: R.drawable.off);

				recAlarm.setImageResource(map.get(pre + "policy").equals("1") ? R.drawable.on
						: R.drawable.off);
			} else {
				pre = "ala_";

				if (map.get(pre + "switch").equals("1")) {
					isAlarmImg.setImageResource(R.drawable.on);
					moveAlarmLayout.setVisibility(View.VISIBLE);
					voiceAlarmLayout.setVisibility(View.VISIBLE);
				} else {
					isAlarmImg.setImageResource(R.drawable.off);
					moveAlarmLayout.setVisibility(View.GONE);
					voiceAlarmLayout.setVisibility(View.GONE);
				}

				moveAlarm.setImageResource(map.get(pre + "movealarm").equals(
						"1") ? R.drawable.on : R.drawable.off);

				voiceAlarm.setImageResource(map.get(pre + "voicealarm").equals(
						"1") ? R.drawable.on : R.drawable.off);

			}

		} catch (NullPointerException e) {
			ELog.i(TAG, "字段不全");
		}

	}

}
