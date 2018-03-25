package com.myanycamm.cam;

import java.util.HashMap;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.myanycam.bean.CameraListInfo;
import com.myanycam.net.SocketFunction;
import com.myanycamm.ui.DialogFactory;
import com.myanycamm.utils.ELog;
import com.myanycamm.zxing.client.android.CaptureActivity;

public class AddCameraActivity extends BaseActivity {
	private static String TAG = "AddCameraActivity";
	private SocketFunction sf;
	private Button backBtn, addBtn;
	private TextView mTextView;
	private EditText camSn, camPsw, camName;
	private ImageView qrCorder;
	private Dialog mDialog = null;
	public static final int ADDRESP = 0;

	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			Bundle bundle = msg.getData();
			HashMap<String, String> map = (HashMap) bundle
					.getSerializable("data");
			switch (msg.what) {
			case ADDRESP:
				if (map.get("ret").equals("0")) {
					ELog.i(TAG, "添加成功");
					Toast.makeText(AddCameraActivity.this,
							getString(R.string.add_cam_success),
							Toast.LENGTH_SHORT).show();
					AddCameraActivity.this.finish();
					// sf.downloadCamera();
				}
				if (map.get("ret").equals("1")) {
					ELog.i(TAG, "不存在此摄像头");
					Toast.makeText(AddCameraActivity.this,
							getString(R.string.add_cam_failed2),
							Toast.LENGTH_SHORT).show();
				}
				if (map.get("ret").equals("2")) {
					ELog.i(TAG, "已经被别人添加");
					Toast.makeText(AddCameraActivity.this,
							getString(R.string.add_cam_failed1),
							Toast.LENGTH_SHORT).show();
				}
				dimissDialog();
				break;
			default:
				break;
			}
		};
	};

	private OnClickListener addOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (camSn.getText().toString().equals("")) {
				Toast.makeText(AddCameraActivity.this,
						getString(R.string.cam_sn_wrong), Toast.LENGTH_SHORT)
						.show();
				return;
			}
			for (CameraListInfo cam : CameraListInfo.cams) {
				if (camSn.getText().toString().equalsIgnoreCase(cam.getSn())) {
					Toast.makeText(AddCameraActivity.this,
							getString(R.string.add_cam_failed3),
							Toast.LENGTH_SHORT).show();
					return;
				}
			}
			if (camPsw.getText().toString().equals("")) {
				Toast.makeText(AddCameraActivity.this,
						getString(R.string.cam_psw_wrong), Toast.LENGTH_SHORT)
						.show();
				return;
			}
			if (camName.getText().toString().equals("")) {
				Toast.makeText(AddCameraActivity.this,
						getString(R.string.cam_name_wrong), Toast.LENGTH_SHORT)
						.show();
				return;
			}
			showRequestDialog(null);
			sf.addCamera(camSn.getText().toString().toUpperCase(), camPsw
					.getText().toString(), camName.getText().toString());
		}
	};

	private OnClickListener qrCorderClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			Intent intent = new Intent(AddCameraActivity.this,
					CaptureActivity.class);
			startActivityForResult(intent, 1);
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_add_camera);
		sf = (SocketFunction) getApplication();
		sf.setmHandler(mHandler);
		initView();
		Intent intent = getIntent();

		String camInfo = intent.getStringExtra("caminfo");
		if (null != camInfo) {
			String[] camInfos = camInfo.split("#");
			camSn.setText(camInfos[0]);
			camPsw.setText(camInfos[1]);
			camName.setText("myanycam_"
					+ camInfos[0].substring(camInfos[0].length() - 3));
		}

	}

	private void initView() {
		backBtn = (Button) findViewById(R.id.settings_back);
		backBtn.setVisibility(View.VISIBLE);
		backBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				AddCameraActivity.this.finish();
			}
		});
		mTextView = (TextView) findViewById(R.id.settings_head_title);
		mTextView.setText(R.string.add_camera);
		addBtn = (Button) findViewById(R.id.add_cam_btn);
		addBtn.setOnClickListener(addOnClickListener);
		camSn = (EditText) findViewById(R.id.cam_sn);
		camPsw = (EditText) findViewById(R.id.cam_password);
		camName = (EditText) findViewById(R.id.cam_name);
		// qrCorder = (ImageView) findViewById(R.id.qrcorder);
		// qrCorder.setOnClickListener(qrCorderClickListener);
	}

	private void showRequestDialog(String note) {
		if (mDialog != null) {
			mDialog.dismiss();
			mDialog = null;
		}
		mDialog = DialogFactory.createLoadingDialog(AddCameraActivity.this,
				note);
		mDialog.setCanceledOnTouchOutside(false);
		mDialog.show();
	}

	private void dimissDialog() {
		if (mDialog != null) {
			mDialog.dismiss();
			mDialog = null;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == 3) {
			ELog.i(TAG, "从识别二维码来" + data.getStringExtra("caminfo"));
			String camInfo = data.getStringExtra("caminfo");
			String[] camInfos = camInfo.split("#");
			camSn.setText(camInfos[0]);
			camPsw.setText(camInfos[1]);
			// ELog.i(TAG,
			// "camInfos"+camInfos[0].toUpperCase()+" "+camInfos[1]);
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

}
