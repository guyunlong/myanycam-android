package com.myanycamm.ui;

import android.gesture.GestureOverlayView.OnGestureListener;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.TextView;

import com.myanycamm.cam.CameraCenterActivity;
import com.myanycamm.cam.R;
import com.myanycamm.model.MyGestureListener;

public abstract class LivingView extends RelativeLayout implements OnGestureListener{
	
	static CameraCenterActivity mActivity;
	TabHost mTabHost;
	TextView headTitle;
	LinearLayout headLayout;
	View camView;
	ImageView rotImageView;
	Animation hyperspaceJumpAnimation;
	TextView playText;
	RelativeLayout playLayout;
	public Handler mHandler;
	private MyGestureListener myGestureListener;

	OnClickListener playBackOnclClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			mActivity.finish();
		}
	};
	
	public abstract void setDeviceStatus(int sdcard,int battery);
	public abstract void stopCam();
	public abstract void initCam();
	public abstract void show(int timeout);
	public abstract void setHead();
	public abstract void showHead();
	public abstract void changScreenToLand();
	public abstract void changScreenToPorait();
	public abstract void showCamMaxDialog();
	public abstract void stopRecord();
	public LivingView(CameraCenterActivity activity, TabHost tabHost) {
		super(activity);
		this.mActivity = activity;
		this.mTabHost = tabHost;	
		// 加载动画
		hyperspaceJumpAnimation = AnimationUtils.loadAnimation(mActivity,
				R.anim.loading_animation);
		myGestureListener = new MyGestureListener(mActivity);
	}		
	
}
