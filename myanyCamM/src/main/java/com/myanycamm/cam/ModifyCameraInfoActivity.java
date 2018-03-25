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
import android.widget.TextView;
import android.widget.Toast;

import com.myanycam.bean.CameraListInfo;
import com.myanycam.net.SocketFunction;
import com.myanycamm.ui.DialogFactory;
import com.myanycamm.utils.ELog;

public class ModifyCameraInfoActivity extends BaseActivity {
	private static String TAG = "ModifyCameraInfoActivity";
	private SocketFunction sf;
	private Button backBtn, addBtn;
	private TextView mTextView;
	private Dialog mDialog = null;
	private CameraListInfo cam;
	private EditText camNamEdit,camMemoEdit;
	public static final int MODIFYRESP = 10;

	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			Bundle bundle = msg.getData();
			HashMap<String, String> map = (HashMap) bundle
					.getSerializable("data");
			switch (msg.what) {
			case MODIFYRESP:
				if (map.get("ret").equals("0")) {
					ELog.i(TAG, "修改成功");		
					Toast.makeText(ModifyCameraInfoActivity.this,
							getString(R.string.modify_success),
							Toast.LENGTH_SHORT).show();
					ModifyCameraInfoActivity.this.finish();
					cam.setName(camNamEdit.getText().toString());
					cam.setMemo(camMemoEdit.getText().toString());
				}
				if (map.get("ret").equals("1")) {
					ELog.i(TAG, "修改失败");
					Toast.makeText(ModifyCameraInfoActivity.this,
							getString(R.string.modify_failed),
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
			showRequestDialog(null);
			cam.setName(camNamEdit.getText().toString());
			cam.setMemo(camMemoEdit.getText().toString());
			sf.modifyCamInfo(cam);
		}
	};
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_modify_camera);
		Intent intent = getIntent();
		cam = CameraListInfo.cams.get(intent.getIntExtra("position", 1));		
		sf = (SocketFunction) getApplication();
		sf.setmHandler(mHandler);
		initView();

	}

	private void initView() {
		backBtn = (Button) findViewById(R.id.settings_back);
		backBtn.setVisibility(View.VISIBLE);
		backBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				ModifyCameraInfoActivity.this.finish();
			}
		});
		mTextView = (TextView) findViewById(R.id.settings_head_title);
		mTextView.setText(R.string.cam_info);
		addBtn = (Button) findViewById(R.id.add_cam_btn);
		addBtn.setOnClickListener(addOnClickListener);
		camNamEdit = (EditText) findViewById(R.id.cam_name);
		camNamEdit.setText(cam.getName());
		camMemoEdit = (EditText) findViewById(R.id.cam_descri);
		camMemoEdit.setText(cam.getMemo());
	}
	
	private void showRequestDialog(String note) {
		if (mDialog != null) {
			mDialog.dismiss();
			mDialog = null;
		}
		mDialog = DialogFactory.createLoadingDialog(ModifyCameraInfoActivity.this, note);
		mDialog.setCanceledOnTouchOutside(false);
		mDialog.show();
	}

	private void dimissDialog() {
		if (mDialog != null) {
			mDialog.dismiss();
			mDialog = null;
		}
	}
	

}
