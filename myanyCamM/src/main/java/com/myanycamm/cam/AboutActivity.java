package com.myanycamm.cam;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.myanycamm.update.UpdateSoft;
import com.myanycamm.utils.ELog;
import com.myanycamm.utils.Utils;

public class AboutActivity extends BaseActivity {
	private static String TAG = "AboutActivity";

	private Button moreInfo, versionName, backBtn;
	private TextView mTextView;
	OnClickListener updateOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			UpdateSoft mSoft = new UpdateSoft(AboutActivity.this);
			mSoft.update(true);
		}
	};

	OnClickListener moreInfoClick = new OnClickListener() {

		@Override
		public void onClick(View v) {
			Intent it = new Intent(AboutActivity.this, MoreInfoActivity.class);
			startActivity(it);
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_about);
		ELog.i(TAG, "点击了更新..");
		versionName = (Button) findViewById(R.id.version_name);
		versionName.setText(getResources().getString(R.string.version_name)
				+ " " + Utils.getAppVersionName(AboutActivity.this));
		versionName.setOnClickListener(updateOnClickListener);
		moreInfo = (Button) findViewById(R.id.more_info);
		moreInfo.setOnClickListener(moreInfoClick);
		backBtn = (Button) findViewById(R.id.settings_back);
		backBtn.setVisibility(View.VISIBLE);
		backBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				finish();
			}
		});

		mTextView = (TextView) findViewById(R.id.settings_head_title);
		mTextView.setText(R.string.about);

	}

}
