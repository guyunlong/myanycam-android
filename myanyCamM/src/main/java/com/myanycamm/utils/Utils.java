package com.myanycamm.utils;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.util.DisplayMetrics;

public class Utils {

	public static int getIdentifierNoR(String name, String resFolder,
			Context mContext) {

		int i = 0;
		i = mContext.getResources().getIdentifier(name, resFolder,
				mContext.getPackageName());
		return i;
	}

	public static int dip2px(Context context, float dpValue) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (dpValue * scale + 0.5f);
	}

	public static int px2dip(Context context, float pxValue) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (pxValue / scale + 0.5f);
	}

	public static int getWidthPixels(Context context) {
		DisplayMetrics dm = new DisplayMetrics();
		dm = context.getResources().getDisplayMetrics();

		return dm.widthPixels;
	}

	public static int getHeightPixels(Context context) {
		DisplayMetrics dm = new DisplayMetrics();
		dm = context.getResources().getDisplayMetrics();

		return dm.heightPixels;
	}

	public static boolean isNetworkAvailable(Context ctx) {
		try {
			ConnectivityManager cm = (ConnectivityManager) ctx
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo info = cm.getActiveNetworkInfo();
			WifiManager wm = (WifiManager) ctx
					.getSystemService(Context.WIFI_SERVICE);
			if (wm.isWifiEnabled()) {
				return true;
			} else {
				boolean check = (info != null && info.isConnected());
				return check;
			}
		} catch (Exception e) {
			return false;
		}
	}

	static public Bitmap decodeYUV420SP(byte[] yuv420sp, int width, int height) {
		final int frameSize = width * height;
		Bitmap mBitmap = null;
		int[] rgba = new int[width * height];
		for (int j = 0, yp = 0; j < height; j++) {
			int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
			for (int i = 0; i < width; i++, yp++) {
				int y = (0xff & ((int) yuv420sp[yp])) - 16;
				if (y < 0)
					y = 0;
				if ((i & 1) == 0) {
					v = (0xff & yuv420sp[uvp++]) - 128;
					u = (0xff & yuv420sp[uvp++]) - 128;
				}

				int y1192 = 1192 * y;
				int r = (y1192 + 1634 * v);
				int g = (y1192 - 833 * v - 400 * u);
				int b = (y1192 + 2066 * u);

				if (r < 0)
					r = 0;
				else if (r > 262143)
					r = 262143;
				if (g < 0)
					g = 0;
				else if (g > 262143)
					g = 262143;
				if (b < 0)
					b = 0;
				else if (b > 262143)
					b = 262143;

				// rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) &
				// 0xff00) | ((b >> 10) & 0xff);
				// rgba, divide 2^10 ( >> 10)
				rgba[yp] = ((r << 14) & 0xff000000) | ((g << 6) & 0xff0000)
						| ((b >> 2) | 0xff00);

			}
		}

		mBitmap = Bitmap.createBitmap(rgba, width, height, Config.RGB_565);
		return mBitmap;
	}

	public static String getAppVersionName(Context context) {

		String versionName = null;

		try {
			// ---get the package info---

			PackageManager pm = context.getPackageManager();
			PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
			versionName = pi.versionName;

			if (versionName == null || versionName.length() <= 0) {
				return null;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return versionName;

	}

	public static boolean isEmail(String email) {

		String str = "[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}";
		Pattern p = Pattern.compile(str);
		Matcher m = p.matcher(email);
		return m.matches();
	}

	/**
	 * 检查域名通不通
	 * 
	 * @param host
	 * @param seconds
	 * @return
	 */
	public static boolean checkIfURLExists(String host, int seconds) {
		HttpURLConnection httpUrlConn;
		try {
			httpUrlConn = (HttpURLConnection) new URL(host).openConnection();

			// Set timeouts in milliseconds
			httpUrlConn.setConnectTimeout(seconds * 1000);
			httpUrlConn.setReadTimeout(seconds * 1000);
			return (httpUrlConn.getResponseCode() == HttpURLConnection.HTTP_OK);
		} catch (Exception e) {
			System.out.println("Error: " + e.getMessage());
			return false;
		}
	}

}
