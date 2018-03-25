package com.myanycamm.setting;

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
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;

import com.myanycam.net.SocketFunction;
import com.myanycamm.cam.BaseActivity;
import com.myanycamm.cam.R;
import com.myanycamm.utils.SharePrefereUtils;

public class QualitySettingActivity extends BaseActivity {

	private static final String TAG = "RecSettingActivity";
	public static final int GETLIVEQUALITY = 301;
	public static final int GETRECQUALITY = 302;

	private int camId;

	private SocketFunction sf;

	private TextView headTitle;
	private RadioGroup liveQuality, recQuality;

	private HashMap<String, String> map;
	SharedPreferences sp;

	private OnCheckedChangeListener liveOnCheckedChangeListener = new OnCheckedChangeListener() {

		@Override
		public void onCheckedChanged(RadioGroup group, int checkedId) {
			switch (checkedId) {
			case R.id.live_good:
				saveInfo("live_quality", "1");
				break;
			case R.id.live_better:
				saveInfo("live_quality", "2");
				break;
			case R.id.live_best:
				saveInfo("live_quality", "3");
				break;
			default:
				saveInfo("live_quality", "1");
				break;
				
			}
		}
	};

	private OnCheckedChangeListener recOnCheckedChangeListener = new OnCheckedChangeListener() {

		@Override
		public void onCheckedChanged(RadioGroup group, int checkedId) {
			switch (checkedId) {
			case R.id.rec_good:
				saveInfo("rec_quality", "1");
				break;
			case R.id.rec_better:
				saveInfo("rec_quality", "2");
				break;
			case R.id.rec_best:
				saveInfo("rec_quality", "3");
				break;
			default:
				saveInfo("rec_quality", "1");
				break;
			}
			
		}
		
	};
	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			Bundle bundle = msg.getData();
			map = (HashMap) bundle.getSerializable("data");
			switch (msg.what) {
			case GETLIVEQUALITY:
				saveInfo("live_quality", map.get("videosize"));
				break;
			case GETRECQUALITY:
				saveInfo("rec_quality", map.get("videosize"));
				break;
			}
			initSharePrefrenceView();
		};
	};

	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_quality_setting);
		sf = (SocketFunction) getApplication();
		headTitle = (TextView) findViewById(R.id.settings_head_title);
		headTitle.setText(R.string.quality_setting);
		liveQuality = (RadioGroup) findViewById(R.id.live_quality);
		liveQuality.setOnCheckedChangeListener(liveOnCheckedChangeListener);
		recQuality = (RadioGroup) findViewById(R.id.rec_quality);
		recQuality.setOnCheckedChangeListener(recOnCheckedChangeListener);
		sp = getSharedPreferences("SP", MODE_PRIVATE);
		Intent intent = getIntent();
		if (null != intent) {
			camId = intent.getIntExtra("camid", -1);
		}

		Button settingBack = (Button) findViewById(R.id.settings_back);
		settingBack.setVisibility(View.VISIBLE);
		settingBack.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});
		sf.setmHandler(mHandler);
		sf.getRecVdideoQuality();
		sf.getLiveVdideoQuality();
		initSharePrefrenceView();
	}

	@Override
	public void finish() {
		saveInfoToserver();
		super.finish();
	}

	public void saveInfoToserver(){
		sf.modifyRecVdideoQuality(SharePrefereUtils.getStringWithKey(QualitySettingActivity.this, "rec_quality"));
		sf.modifyLiveVdideoQuality(SharePrefereUtils.getStringWithKey(QualitySettingActivity.this, "live_quality"));
	}

	
	private void saveInfo(String key, String value) {
		Context ctx = QualitySettingActivity.this;
		SharePrefereUtils.commitStringData(ctx, key, value);
	}

	
	private void initSharePrefrenceView() {
		map = (HashMap<String, String>) sp.getAll();
		if (map.get("live_quality") != null && !map.get("live_quality").equals("")) {
			switch (Integer.parseInt(map.get("live_quality"))) {
			case 1:
				liveQuality.check(R.id.live_good);
				break;
			case 2:
				liveQuality.check(R.id.live_better);
				break;
			case 3:
				liveQuality.check(R.id.live_best);
				break;

			default:
				liveQuality.check(R.id.live_good);
				break;
			}
		}
		if (map.get("rec_quality") != null && !map.get("rec_quality").equals("") ){
			switch (Integer.parseInt(map.get("rec_quality"))) {
			case 1:
				recQuality.check(R.id.rec_good);
				break;
			case 2:
				recQuality.check(R.id.rec_better);
				break;
			case 3:
				recQuality.check(R.id.rec_best);
				break;

			default:
				recQuality.check(R.id.rec_good);
				break;
			}
		}
	}

}
