package com.myanycamm.cam;

import java.util.HashMap;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.myanycam.net.SocketFunction;
import com.myanycamm.ui.DialogFactory;
import com.myanycamm.utils.ELog;

public class ChangPasswordActivity extends BaseActivity {
	private static String TAG = "ChangPasswordActivity";
	private SocketFunction sf;
	private Button backBtn, changBtn;
	private TextView mTextView;
	private EditText orginPsw, newPsw, confirmPsw;
	private Dialog mDialog = null;

	public static final int CHANGRESP = 200;

	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			Bundle bundle = msg.getData();
			HashMap<String, String> map = (HashMap) bundle
					.getSerializable("data");
			switch (msg.what) {
			case CHANGRESP:
				if (map.get("ret").equals("0")) {
					ELog.i(TAG, "修改密码成功");		
					Toast.makeText(ChangPasswordActivity.this,
							getString(R.string.chang_succeed),
							Toast.LENGTH_SHORT).show();		
					SharedPreferences sp = getSharedPreferences("passwordFile", MODE_PRIVATE);		
					sp.edit().clear().commit();
					sp.edit().putString(sf.userInfo.getName(),
							newPsw.getText().toString()).commit();
					sf.userInfo.setPassword(newPsw.getText().toString());
					ChangPasswordActivity.this.finish();
				}
				if (map.get("ret").equals("1")) {
					ELog.i(TAG, "修改失败");
					Toast.makeText(ChangPasswordActivity.this,
							getString(R.string.chang_failed),
							Toast.LENGTH_SHORT).show();
				}
				dimissDialog();
				break;
			default:
				break;
			}
		};
	};

	private OnClickListener changOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			ELog.i(TAG, "psw:"+sf.userInfo.getPassword());
			if (!orginPsw.getText().toString().equals(sf.userInfo.getPassword())) {
				Toast.makeText(ChangPasswordActivity.this,
						getString(R.string.orgin_wrong), Toast.LENGTH_SHORT)
						.show();
				return;
			}
			if (newPsw.getText().toString().length()<8 ) {
				Toast.makeText(ChangPasswordActivity.this,
						getString(R.string.new_psw_short), Toast.LENGTH_SHORT)
						.show();
				return;
			}
			if (!confirmPsw.getText().toString().equals(newPsw.getText().toString())) {
				Toast.makeText(ChangPasswordActivity.this,
						getString(R.string.new_psw_no), Toast.LENGTH_SHORT)
						.show();
				return;
			}
			sf.modifyPsw(newPsw.getText().toString());
			showRequestDialog(null);
		}
	};
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_change_password);
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
				ChangPasswordActivity.this.finish();
			}
		});
		mTextView = (TextView) findViewById(R.id.settings_head_title);
		mTextView.setText(R.string.change_password);
		changBtn = (Button) findViewById(R.id.chang_btn);
		changBtn.setOnClickListener(changOnClickListener);
		orginPsw = (EditText) findViewById(R.id.orgin_psw);
		newPsw = (EditText) findViewById(R.id.new_psw);
		confirmPsw = (EditText) findViewById(R.id.confirm);
	}
	
	private void showRequestDialog(String note) {
		if (mDialog != null) {
			mDialog.dismiss();
			mDialog = null;
		}
		mDialog = DialogFactory.createLoadingDialog(ChangPasswordActivity.this, note);
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
