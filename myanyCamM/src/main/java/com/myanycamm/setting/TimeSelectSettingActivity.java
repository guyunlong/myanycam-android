package com.myanycamm.setting;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.myanycam.bean.CameraListInfo;
import com.myanycam.bean.TimePeriod;
import com.myanycam.net.SocketFunction;
import com.myanycamm.cam.BaseActivity;
import com.myanycamm.cam.R;
import com.myanycamm.ui.SlipButton;
import com.myanycamm.ui.SlipButton.OnChangedListener;
import com.myanycamm.utils.ELog;
import com.myanycamm.utils.SharePrefereUtils;

public class TimeSelectSettingActivity extends BaseActivity {

	private static String TAG = "TimeSelectSettingActivity";

	ArrayList<TimePeriod> timePeriod1 = new ArrayList<TimePeriod>();
	LinearLayout mainLayout;
	private LinearLayout weekSelect;
	private SocketFunction sf;
	private GridView gridview;
	private String weekString = "0000000";
	private char[] weekChar = new char[7];
	private boolean[] mulitBooleans = { false, false, true, false, false,
			false, false };
	private TextView cirlce_day;
	private TimePicker mTimePicker;
	private int mHour;
	private int mMinute;
	private int endHour;
	private int endMinute;
	private static final int TIMEPICKER_DIALOG_1 = 0;
	private Calendar c = null;
	private boolean isStartTimeDilalog = false;
	private int currentPosition;
	private boolean isRec = true;// 录像设置或者报警设置,默认为录像设置
	private Dialog mDialog;
	private HashMap<String, String> map;

	// 时间监听器，当用户改变时间的时候将会调用它
	OnTimeSetListener otsl = new OnTimeSetListener() {

		@Override
		public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
			// TODO Auto-generated method stub
			// 设置小时、分钟，并把时间显示在文本框上
			if (isStartTimeDilalog) {
				onCreateDialog(0, getString(R.string.select_end_time)).show();
				isStartTimeDilalog = false;
				mHour = hourOfDay;
				mMinute = minute;
			} else {
				endHour = hourOfDay;
				endMinute = minute;
				if (compareTime(mHour, endHour, mMinute, endMinute)) {
					setRecTime();
				} else {
					Toast.makeText(TimeSelectSettingActivity.this,
							getString(R.string.set_time_error),
							Toast.LENGTH_SHORT).show();
				}
				ELog.i(TAG,
						"比较时间大小:"
								+ compareTime(mHour, endHour, mMinute,
										endMinute));
			}

			ELog.i(TAG, "mHour:" + mHour + "mMinute:" + mMinute);
		}

	};

	private OnClickListener weekSelectOnclickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			new AlertDialog.Builder(TimeSelectSettingActivity.this)
					.setTitle(getString(R.string.week_circle))
					.setMultiChoiceItems(
							new String[] { getString(R.string.week1),
									getString(R.string.week2),
									getString(R.string.week3),
									getString(R.string.week4),
									getString(R.string.week5),
									getString(R.string.week6),
									getString(R.string.week7) }, mulitBooleans,
							mOnMultiChoiceClickListener)
					.setPositiveButton(getString(R.string.confirm),
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									refreshWeekText();
								}
							})
					.setNegativeButton(getString(R.string.btn_cancel), null)
					.show();
		}
	};

	private OnMultiChoiceClickListener mOnMultiChoiceClickListener = new OnMultiChoiceClickListener() {

		@Override
		public void onClick(DialogInterface dialog, int which, boolean isChecked) {
			ELog.i(TAG, "点击了" + which + weekChar);
			if (isChecked) {
				// 服务器星期日在最前面
				weekChar[(which == 6) ? 0 : (which % 6 + 1)] = '1';
			} else {
				weekChar[(which == 6) ? 0 : (which % 6 + 1)] = '0';
			}
			ELog.i(TAG, String.valueOf(weekChar));

		}
	};

	
	private void refreshWeekText() {
		String s = " ";
		for (int i = 1; i < weekChar.length; i++) {
			switch (i) {
			case 1:
				if (weekChar[i] == '1') {
					s = s + getString(R.string.week1) + " ";
				}
				break;
			case 2:
				if (weekChar[i] == '1') {
					s = s + getString(R.string.week2) + " ";
				}
				break;
			case 3:
				if (weekChar[i] == '1') {
					s = s + getString(R.string.week3) + " ";
				}
				break;
			case 4:
				if (weekChar[i] == '1') {
					s = s + getString(R.string.week4) + " ";
				}
				break;
			case 5:
				if (weekChar[i] == '1') {
					s = s + getString(R.string.week5) + " ";
				}
				break;
			case 6:
				if (weekChar[i] == '1') {
					s = s + getString(R.string.week6) + " ";
				}
				break;

			default:
				break;
			}

		}

		if (weekChar[0] == '1') {
			s = s + getString(R.string.week7);
		}
		cirlce_day.setText(s.trim());
		saveInfo("repeat", String.valueOf(weekChar));

	}

	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_time_select);
		sf = (SocketFunction) getApplication();
		Intent intent = getIntent();
		if(null != intent){
			isRec = intent.getBooleanExtra("isRec", true);
		}
		TextView headTitle = (TextView) findViewById(R.id.settings_head_title);
		headTitle.setText(R.string.time_selcet);// 默认为摄像头列表
		Button settingBack = (Button) findViewById(R.id.settings_back);
		settingBack.setVisibility(View.VISIBLE);
		settingBack.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				finish();
//				saveInfoToServer();
			}
		});
		timePeriod1.add(new TimePeriod());
		timePeriod1.add(new TimePeriod());
		timePeriod1.add(new TimePeriod());
		timePeriod1.add(new TimePeriod());
		mainLayout = (LinearLayout) findViewById(R.id.mainLayout);
		mainLayout.addView(genView(timePeriod1));
		weekSelect = (LinearLayout) findViewById(R.id.weekselect);
		weekSelect.setOnClickListener(weekSelectOnclickListener);
		cirlce_day = (TextView) findViewById(R.id.cirlce_day);
		SharedPreferences sp = getSharedPreferences("SP", MODE_PRIVATE);
		if(isRec){
			if (sp.contains("rec_begintime1")) {
				// 从本地读取数据
				initTime();
			}
		}else{
			if (sp.contains("ala_begintime1")) {
				// 从本地读取数据
				initTime();
			}
		}


	}

	private void charToBoolean() {
		for (int i = 1; i < weekChar.length; i++) {
			if (weekChar[i] == '1') {
				mulitBooleans[i-1] = true;
			} else {
				mulitBooleans[i-1] = false;
			}
		}
		if (weekChar[0] == '1') {
			mulitBooleans[6] = true;
		} else {
			mulitBooleans[6] = false;
		}
	}

	
	private boolean compareTime(int startHour, int endHour, int startMiunte,
			int endMinute) {
		boolean result = false;
		if (startHour != endHour) {
			result = startHour < endHour ? true : false;
			return result;
		}
		result = startMiunte < endMinute ? true : false;
		return result;
	}

	
	private void setRecTime() {
		String s = mHour + ":" + mMinute;
		LinearLayout mLinearLayout = (LinearLayout) mainLayout.getChildAt(0);
		View mView = mLinearLayout.getChildAt(currentPosition);
		Button btn = (Button) mView.findViewById(R.id.time_select_text_view);
		btn.setText(formatTime(mHour, mMinute) + " - "
				+ formatTime(endHour, endMinute));
		saveInfo("begintime" + (currentPosition+1), formatTime(mHour, mMinute)
				+ ":00");
		saveInfo("endtime" + (currentPosition+1), formatTime(endHour, endMinute)
				+ ":00");
	}

	
	
	private String formatTime(int hour, int miunte) {
		String result;
		Date d = new Date(2013, 5, 25, hour, miunte, 0);
		SimpleDateFormat formatter = new SimpleDateFormat("HH:mm");
		result = formatter.format(d);
		return result;
	}

	public View genView(ArrayList<TimePeriod> timePeriods) {
		LinearLayout layout = new LinearLayout(this);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		params.setMargins(0, 20, 0, 0);
		layout.setOrientation(LinearLayout.VERTICAL);
		for (int i = 0; i < timePeriods.size(); i++) {
			View view = genItemView(timePeriods, i);
			layout.addView(view);
		}
		layout.setLayoutParams(params);
		return layout;
	}

	public View genItemView(ArrayList<TimePeriod> timePeriods,
			final int position) {
		View convertView = LayoutInflater.from(getApplicationContext())
				.inflate(R.layout.time_select_item, null);
		convertView.setClickable(true);

		ImageView currentSelect = (ImageView) convertView
				.findViewById(R.id.current_select);

		SlipButton slipButton = (SlipButton) convertView
				.findViewById(R.id.local_settting_slipbtn);
		slipButton.SetOnChangedListener(slipBtnChangedListener, position);
		// slipButton.setVisibility(View.GONE);

		int size = timePeriods.size();
		if (size > 1 && position == 0) {
			convertView
					.setBackgroundResource(R.drawable.privacy_setting_item_top_bg);
			// currentSelect.setVisibility(View.VISIBLE);
		} else if (size > 1 && position == size - 1) {
			convertView
					.setBackgroundResource(R.drawable.privacy_setting_item_bottom_bg);
		} else if (size > 1) {
			convertView
					.setBackgroundResource(R.drawable.privacy_setting_item_mid_bg);
		} else {
			convertView.setBackgroundResource(R.drawable.setting_item_bg);
		}

		final TimePeriod item = timePeriods.get(position);
		convertView.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				getIntentById(position);
			}
		});

		return convertView;
	}

	private void getIntentById(int position) {
		// TimePickerDialog mDialog =new
		// TimePickerDialog(TimeSelectSettingActivity.this, null, mHour,
		// mMinute, true);
		// mDialog.show();
		isStartTimeDilalog = true;
		currentPosition = position;
		Dialog mDialog = onCreateDialog(0,
				getString(R.string.select_start_time));
		mDialog.show();
	}

	OnChangedListener slipBtnChangedListener = new OnChangedListener() {

		@Override
		public void OnChanged(boolean CheckState, int id) {
			ELog.i(TAG, "子项目:" + mainLayout.getChildCount());
			LinearLayout mLinearLayout = (LinearLayout) mainLayout
					.getChildAt(0);
			View mView = mLinearLayout.getChildAt(id);
			if (CheckState) {
				mView.setClickable(true);
			} else {
				mView.setClickable(false);

			}
			currentPosition = id;
			saveInfo("switch" + (currentPosition+1), CheckState ? "1" : "0");
		}
	};

	protected Dialog onCreateDialog(int id, String title) {
		TimePickerDialog dialog = null;
		switch (id) {
		case 0:
			
			dialog = new TimePickerDialog(this, otsl, mHour, mMinute, true);
			// dialog.set
			dialog.setTitle(title);
			break;
		default:
			break;
		}
		return dialog;
	}


	
	private void saveInfo(String key, String value) {
		Context ctx = TimeSelectSettingActivity.this;
		if (isRec) {
			SharePrefereUtils.commitStringData(ctx, "rec_" + key, value);
		} else {
			SharePrefereUtils.commitStringData(ctx, "ala_" + key, value);
		}

	}
	
	
	private void saveInfoToServer(){
		if(isRec){
			sf.setRecordConfig(CameraListInfo.currentCam);
		}else{
			sf.setAlarmConfig(CameraListInfo.currentCam);
		}
	}

	
	private void initTime() {
		String pre = "rec_";
		if (isRec) {
			pre = "rec_";
		} else {
			pre = "ala_";
		}
		SharedPreferences sp = getSharedPreferences("SP", MODE_PRIVATE);
		map = (HashMap<String, String>) sp.getAll();
		weekString = map.get(pre + "repeat");
		ELog.i(TAG, "weekString:" + weekString);
		weekChar = weekString.toCharArray();
		charToBoolean();
		refreshWeekText();
		LinearLayout mLinearLayout = (LinearLayout) mainLayout.getChildAt(0);
		for (int i = 0; i < 4; i++) {
			currentPosition = i;// 当前正在工作的cell
			ELog.i(TAG, "begin:" + map.get(pre + "begintime" + (i + 1)));
			String[] beginTime = {"00","00"} ;
			if(!map.get(pre + "begintime"+ (i + 1)).equals("")){
				beginTime = parseRecTime(map.get(pre + "begintime"
						+ (i + 1)));
			}			
			mHour = Integer.parseInt(beginTime[0]);
			mMinute = Integer.parseInt(beginTime[1]);

			String[] endTime = {"00","00"} ;
			if(!map.get(pre + "endtime" + (i + 1)).equals("")){
			endTime = parseRecTime(map.get(pre + "endtime" + (i + 1)));			
			}
			endHour = Integer.parseInt(endTime[0]);
			endMinute = Integer.parseInt(endTime[1]);	
	
			setRecTime();// 把服务器的时间更新到UI
			View mView = mLinearLayout.getChildAt(i);
			if(null != map.get(pre + "switch" + (i + 1))){
				((SlipButton) mView.findViewById(R.id.local_settting_slipbtn))
				.setState(map.get(pre + "switch" + (i + 1)).equals("1"));
			}
		
		}
	}

	
	private String[] parseRecTime(String time) {
		String[] ss = time.split(":");
		return ss;
	}

}
