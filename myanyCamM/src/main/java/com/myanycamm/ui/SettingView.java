package com.myanycamm.ui;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.myanycam.bean.CameraListInfo;
import com.myanycam.net.SocketFunction;
import com.myanycamm.cam.AddCameraActivity;
import com.myanycamm.cam.AppServer;
import com.myanycamm.cam.CameraCenterActivity;
import com.myanycamm.cam.ModifyCameraInfoActivity;
import com.myanycamm.cam.R;
import com.myanycamm.setting.RecSettingActivity;
import com.myanycamm.setting.SysSettingActivity;
import com.myanycamm.setting.WifiSettingActivity;
import com.nmbb.oplayer.util.ELog;
import com.nmbb.oplayer.util.ToastUtils;

public class SettingView extends RelativeLayout implements OnClickListener {
	private static String TAG = "SettingView";
	private CameraCenterActivity mActivity;
	View setView;
	TabHost mTabHost;
	TextView headTitle;
	Button headLeftButton;
	private LinearLayout camInfoLayout, camRecLayout, camAlarmLayout,
			camSnLayout, camRomVersionLayout, camWifiLayout, camRestartLayout,
			camPswLayout;
	private TextView camSn, camRomVersion;
	private ImageView rotateSwitch;
	private boolean isRotate = true;
	private EditText camPswEdit;
	private LinearLayout psw_set_layout;

	public SettingView(CameraCenterActivity mActivity, TabHost tabHost) {
		super(mActivity);
		this.mActivity = mActivity;
		this.mTabHost = tabHost;
		initView();
	}

	private void initView() {
		LayoutInflater inflater = LayoutInflater.from(mActivity);
		setView = inflater.inflate(R.layout.tab_setting,
				mTabHost.getTabContentView());
		headTitle = (TextView) setView
				.findViewById(R.id.settings_head_title_setting);
		headLeftButton = (Button) setView
				.findViewById(R.id.settings_back_setting);
		camInfoLayout = (LinearLayout) setView
				.findViewById(R.id.caminfo_layout);
		camInfoLayout.setOnClickListener(this);
		camRecLayout = (LinearLayout) setView.findViewById(R.id.rec_layout);
		camRecLayout.setOnClickListener(this);
		camAlarmLayout = (LinearLayout) setView.findViewById(R.id.alarm_layout);
		camAlarmLayout.setOnClickListener(this);
		camRecLayout = (LinearLayout) setView.findViewById(R.id.restart_layout);
		camRecLayout.setOnClickListener(this);
		camWifiLayout = (LinearLayout) setView.findViewById(R.id.wifi_layout);
		camPswEdit = (EditText) setView.findViewById(R.id.cam_password_edit);
		camPswEdit.setText(CameraListInfo.currentCam.getPassWord());
		psw_set_layout = (LinearLayout) setView
				.findViewById(R.id.psw_set_layout);
		psw_set_layout.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				if (camPswEdit.getText().toString().length() != 8) {
					ToastUtils.showToast(R.string.psw_no_eight);
					return;
				}
				ELog.i(TAG, "改密码");
				SocketFunction.getInstance().setDeviceConfig(
						camPswEdit.getText().toString());
			
				ToastUtils.showToast(R.string.psw_modify);

			}
		});
		camWifiLayout.setOnClickListener(this);
		camSnLayout = (LinearLayout) setView.findViewById(R.id.cam_sn_layout);
		camPswLayout = (LinearLayout) setView.findViewById(R.id.cam_psw_layout);
		camRomVersionLayout = (LinearLayout) setView
				.findViewById(R.id.cam_rom_layout);

		camSn = (TextView) setView.findViewById(R.id.cam_sn);
		camSn.setText(mActivity.getString(R.string.cam_sn)
				+ CameraListInfo.currentCam.getSn());
		rotateSwitch = (ImageView) setView.findViewById(R.id.is_rotate);
		rotateSwitch.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				if (isRotate) {
					isRotate = false;
					SocketFunction.getInstance().setVideoRotate(0);
					rotateSwitch.setImageResource(R.drawable.off);
				} else {
					isRotate = true;
					SocketFunction.getInstance().setVideoRotate(1);
					rotateSwitch.setImageResource(R.drawable.on);
				}

			}
		});
		camRomVersion = (TextView) setView.findViewById(R.id.rom_version);
		if (AppServer.isAp) {
			headLeftButton.setVisibility(View.GONE);
		} else {
			headLeftButton.setVisibility(View.VISIBLE);
		}
		headLeftButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mActivity.finish();
			}
		});

		if (!AppServer.isAp) {
			camWifiLayout.setVisibility(View.GONE);
			camPswLayout.setVisibility(View.GONE);
		} else {
			camRomVersionLayout.setVisibility(View.GONE);
			camSnLayout.setVisibility(View.GONE);
		}
		setHead();

	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {

		View v = getChildAt(0);
		v.layout(l, t, r, b);
	}

	public void setHead() {

		headTitle.setText(R.string.cam_setting);
		// headLeftButton.setVisibility(View.VISIBLE);
	}

	public void changRotateState(int vflip) {
		rotateSwitch.setImageResource(vflip == 0 ? R.drawable.off
				: R.drawable.on);
	}

	public void updateRomVersion() {
		camRomVersion.setText(mActivity.getString(R.string.rom_version)
				+ CameraListInfo.currentCam.getRomVersion());
	}

	public void fillCamPsw() {
		camPswEdit.setText(CameraListInfo.currentCam.getPassWord());
	}

	@Override
	public void onClick(View v) {
		Intent intent = null;
		switch (v.getId()) {
		case R.id.caminfo_layout:
			if (AppServer.isAp) {
				intent = new Intent(mActivity, SysSettingActivity.class);
			} else {
				intent = new Intent(mActivity, ModifyCameraInfoActivity.class);
				intent.putExtra("position", mActivity.position);
			}
			mActivity.startActivity(intent);
			break;
		case R.id.rec_layout:

			intent = new Intent(mActivity, RecSettingActivity.class);
			intent.putExtra("isRec", true);
			intent.putExtra("camid", CameraListInfo.currentCam.getId());
			mActivity.startActivity(intent);

			break;
		case R.id.alarm_layout:

			intent = new Intent(mActivity, RecSettingActivity.class);
			intent.putExtra("isRec", false);
			intent.putExtra("camid", CameraListInfo.currentCam.getId());
			mActivity.startActivity(intent);

			break;
		case R.id.restart_layout:
			mActivity.showRestartCamDialog();
			break;

		case R.id.wifi_layout:
			intent = new Intent(mActivity, WifiSettingActivity.class);
			mActivity.startActivity(intent);
			break;

		default:
			break;
		}

	}

}
