package com.myanycamm.utils;

import android.app.Activity;
import android.util.DisplayMetrics;

public class Configure {

	public static int screenHeight = 0;
	public static int screenWidth = 0;
	public static float screenDensity = 0;

	public static int curentPage = 0;
	public static int countPages = 0;
	public static int removeItem = 0;

	public static void init(Activity context) {
		if (screenDensity == 0 || screenWidth == 0 || screenHeight == 0) {
			DisplayMetrics dm = new DisplayMetrics();
			context.getWindowManager().getDefaultDisplay().getMetrics(dm);
			Configure.screenDensity = dm.density;
			Configure.screenHeight = dm.heightPixels;
			Configure.screenWidth = dm.widthPixels;
		}
		curentPage = 0;
		countPages = 0;
	}

}