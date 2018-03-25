package com.myanycamm.ui;

import android.view.View;

import com.myanycam.bean.CameraListInfo;
import com.myanycam.bean.EventInfo;
import com.myanycamm.cam.CameraCenterActivity;

public abstract class AnyCamEvent {
	private static String TAG = "AnyCamEvent";
	protected View mView;
	protected CameraCenterActivity mActivity;
	LoadMoreListView listView;


	public abstract void itemClick(CameraListInfo c, EventInfo e,int position);

	public abstract void goIntent(String url);
	
	public abstract void allDataFinish();
	
	
	
//	public abstract void setEventBean(PictureInfo p);

	public AnyCamEvent(View _mView, CameraCenterActivity _activity) {
		this.mActivity = _activity;
		this.mView = _mView;	
	}







}
