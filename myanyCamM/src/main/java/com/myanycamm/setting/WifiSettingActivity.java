package com.myanycamm.setting;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.InputType;
import android.text.Selection;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.myanycam.bean.CameraWifiInfo;
import com.myanycam.net.SocketFunction;
import com.myanycamm.cam.BaseActivity;
import com.myanycamm.cam.R;
import com.myanycamm.ui.RotateIcon;
import com.myanycamm.utils.ELog;
import com.myanycamm.utils.Utils;

public class WifiSettingActivity extends BaseActivity {

	private static String TAG = "WifiSettingActivity";
	private SocketFunction sf = null;
	int refreshPosition;
	public static final int CURRENTLINK = 1;// 当前正在使用的wifi标志
	public static final int OTHERLINK = 2;// 其他可用wifi列表标志
	ArrayList<CameraWifiInfo> cameraWifiInfos = new ArrayList<CameraWifiInfo>();
	RotateIcon refreshView;
	private String currentLinkName = "";

	LinearLayout mainLayout = null;

	Handler mHandler = new Handler() {

		public void handleMessage(Message msg) {
			Bundle bundle = msg.getData();
			HashMap<String, String> map = (HashMap) bundle
					.getSerializable("data");
			//
			ELog.i(TAG, "wifi信息" + map);
			switch (msg.what) {
			case CURRENTLINK:
				if (map.get("ssid").equals("")) {
					return;
				}
				int safety;
				String signal;
				if (map.get("safety").equals("")) {
					safety = 1;
				} else {
					try {
						safety = Integer.parseInt(map.get("safety"));
					} catch (NumberFormatException e) {
						safety = 3;
					}

				}
				if (null == map.get("signal")) {
					signal = "90";
				} else {
					signal = map.get("signal");
				}
				currentLinkName = map.get("ssid");
				CameraWifiInfo cTemp = new CameraWifiInfo(map.get("ssid"),
						safety, signal, true);
				cTemp.setPassword(map.get("password"));// 当前列表，有密码
				// cameraWifiInfos.add(cTemp);
				mainLayout.removeAllViews();
				mainLayout.addView(genWifiView(cameraWifiInfos));
				// mainLayout.addView(gen)
				//
				break;
			case OTHERLINK:
				String name = map.get("ssid");
				try {
					safety = Integer.parseInt(map.get("safety"));
				} catch (NumberFormatException e) {
					safety = 3;
				}
				if (name.equals(currentLinkName)) {
					cameraWifiInfos.add(new CameraWifiInfo(name, safety, map
							.get("signal"), true));
				} else {
					cameraWifiInfos.add(new CameraWifiInfo(name, safety, map
							.get("signal"), false));
				}
				mainLayout.removeAllViews();
				mainLayout.addView(genWifiView(cameraWifiInfos));

				break;

			default:
				break;
			}

		};

	};

	
	public final static byte TYPE_ARROW = 2;
	OnClickListener finishOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			finish();

		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.wifi_net_setting);
		ELog.i(TAG, "到了wifi网络设置");
		Button settingBack = (Button) findViewById(R.id.settings_back);
		settingBack.setVisibility(View.VISIBLE);
		settingBack.setOnClickListener(finishOnClickListener);
		mainLayout = (LinearLayout) findViewById(R.id.wifi_mainlayout);
		refreshView = (RotateIcon) findViewById(R.id.btn_rotate_refresh);
		sf = (SocketFunction) getApplicationContext();
		sf.setmHandler(mHandler);
		refresRotate();
		// try {
		// sf.getCameraWifi();
		// } catch (IOException e) {
		//
		// e.printStackTrace();
		// }

		refreshView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				refresRotate();
			}
		});

	}

	public void refresRotate() {

		// TODO Auto-generated method stub
		synchronized (this) {
			new AsyncTask<String, String, String>() {
				protected void onPreExecute() {
					cameraWifiInfos.removeAll(cameraWifiInfos);
					refreshView.setImageResource(R.drawable.refresh_rotate);
					refreshView.startRotate();
				};

				@Override
				protected String doInBackground(String... arg0) {
					// 发送手动刷新消息
					ELog.d("Artion", "发送手动刷新消息");
					// mHandler.sendEmptyMessage(Constant.MSG_WEATHER_HANDREFRESH);
					try {
						sf.getCameraWifi();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					// 假的转圈
					try {
						Thread.sleep(3000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					return null;
				}

				protected void onPostExecute(String result) {
					refreshView.stopRotate();
					refreshView.setImageResource(R.drawable.refresh);
				};
			}.execute("");
		}

	}

	public View genWifiView(ArrayList<CameraWifiInfo> cameraWifiInfos) {
		LinearLayout layout = new LinearLayout(this);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		// params.setMargins(0, 20, 0, 0);
		layout.setOrientation(LinearLayout.VERTICAL);
		for (int i = 0; i < cameraWifiInfos.size(); i++) {
			View view = genWifiItemView(cameraWifiInfos, i);
			layout.addView(view);
		}
		layout.setLayoutParams(params);
		return layout;
	}

	public View genWifiItemView(ArrayList<CameraWifiInfo> cameraWifiInfos,
			final int position) {
		final CameraWifiInfo mCameraWifiInfo = cameraWifiInfos.get(position);
		// mCameraWifiInfo.setPassword("");//先设置空密码
		View convertView = LayoutInflater.from(getApplicationContext())
				.inflate(R.layout.wifi_setting_item, null);
		convertView.setClickable(true);
		ImageView currentLink = (ImageView) convertView
				.findViewById(R.id.current_link);
		TextView nameTextView = (TextView) convertView
				.findViewById(R.id.local_settting_itemname);// wifi名字
		ImageView wifiSignal = (ImageView) convertView
				.findViewById(R.id.wif_signal);

		ImageView arrowImgView = (ImageView) convertView
				.findViewById(R.id.local_setting_arrow);
		int size = cameraWifiInfos.size();
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
		nameTextView.setText(mCameraWifiInfo.getSsid());
		if (mCameraWifiInfo.getIsCurrenLink()) {
			currentLink.setVisibility(View.VISIBLE);
			refreshPosition = position;
		} else {
			currentLink.setVisibility(View.INVISIBLE);
		}
		String safteyimgString = "ic_wifi" + mCameraWifiInfo.getImageSignal()
				+ "_signal_" + mCameraWifiInfo.getSignalLevel();
		ELog.i(TAG, safteyimgString);
		int id = Utils.getIdentifierNoR(safteyimgString, "drawable", sf);
		ELog.i(TAG, "id:" + id);
		// 若要想要转换成String类型
		wifiSignal.setImageResource(id);
		// currentLink.setVisibility(View.VISIBLE);
		wifiSignal.setVisibility(View.VISIBLE);
		arrowImgView.setVisibility(View.VISIBLE);
		convertView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				setWifiItemInfo(mCameraWifiInfo, position, refreshPosition);
			}
		});
		return convertView;
	}

	public void updateWifiList() {
		mainLayout.removeAllViews();
		mainLayout.addView(genWifiView(cameraWifiInfos));
	}

	

	private void setWifiItemInfo(final CameraWifiInfo cwi, final int position,
			final int rp) {
		LayoutInflater inflater = (LayoutInflater) getApplicationContext()
				.getSystemService(LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.wifi_alert_style, null);
		TextView mTextView = (TextView) view.findViewById(R.id.safety_info);
		mTextView.setText(getString(R.string.safety_pre) + " "
				+ cwi.getSafety());
		final EditText pswEditText = (EditText) view
				.findViewById(R.id.psw_edit);
		pswEditText.setText(cwi.getPassword());
		CheckBox mCheckBox = (CheckBox) view.findViewById(R.id.is_remember);
		mCheckBox
				.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						if (isChecked) {
							pswEditText
									.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
							// pswEditText.setRawInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
						} else {
							pswEditText.setInputType(InputType.TYPE_CLASS_TEXT
									| InputType.TYPE_TEXT_VARIATION_PASSWORD);
						}
						Editable etable = pswEditText.getText();// 光标一直在文本后面
						Selection.setSelection(etable, etable.length());
					}
				});

		AlertDialog.Builder builder = new AlertDialog.Builder(
				WifiSettingActivity.this);
		builder.setView(view);
		builder.setIcon(android.R.drawable.ic_dialog_info);
		builder.setTitle(cwi.getSsid());
		builder.setPositiveButton(getString(R.string.btn_cancel),
				new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {
						try {
							Field field = dialog.getClass().getSuperclass()
									.getDeclaredField("mShowing");
							field.setAccessible(true);
							// 将mShowing变量设为false，表示对话框已关闭
							field.set(dialog, true);
							dialog.dismiss();
						} catch (Exception e) {

						}

					}
				});
		builder.setNegativeButton(getString(R.string.connect),
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (pswEditText.getText().length() > 7) {
							cameraWifiInfos.get(rp).setIsCurrenLink(false);
							cameraWifiInfos.get(position).setIsCurrenLink(true);
							cwi.setPassword(pswEditText.getText().toString());
							sf.setWifiInfo(cameraWifiInfos.get(position)
									.getSsid(), cameraWifiInfos.get(position)
									.getSafe(), pswEditText.getText()
									.toString());
							updateWifiList();
						} else {
							// 不关闭
							try {
								Field field = dialog.getClass().getSuperclass()
										.getDeclaredField("mShowing");
								field.setAccessible(true);
								// 将mShowing变量设为false，表示对话框已关闭
								field.set(dialog, false);
								dialog.dismiss();

							} catch (Exception e) {

							}

						}

					}
				}).create().show();

	}

}
