package com.myanycamm.cam;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class MoreInfoActivity extends BaseActivity {
	private static String TAG = "MoreInfoActivity";
	
	private Button backBtn;
	private TextView mTextView;



	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_more_info);
	
		backBtn = (Button) findViewById(R.id.settings_back);
		backBtn.setVisibility(View.VISIBLE);
		backBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				finish();
			}
		});		
		mTextView = (TextView) findViewById(R.id.settings_head_title);
		mTextView.setText(R.string.more_info);
		
	}

}
