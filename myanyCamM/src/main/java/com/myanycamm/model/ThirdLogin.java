package com.myanycamm.model;

import android.app.Activity;


public abstract class ThirdLogin {
	protected Activity activity; 
	public ThirdLogin(Activity _activity) {
		this.activity = _activity;
	}
	public  abstract void login();
}
