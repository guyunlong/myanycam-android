package com.nmbb.oplayer.util;

/**
 * 日志
 * 
 * @author abo
 * 
 */

public class ELog {
	private final static String TAG = "ELog";
	public static boolean isDebug = true;

	public static void i(String tag, String msg) {
		if (isDebug) {
			android.util.Log.i(tag, msg);
		}
	}
	

	public static void i(String msg) {
		if (isDebug) {
			android.util.Log.i(TAG, msg);
		}
	}

	public static void i(String tag, String msg, Throwable tr) {
		if (isDebug) {
			android.util.Log.i(tag, msg, tr);
		}
	}

	public static void d(String tag, String msg) {
		if (isDebug) {
			android.util.Log.d(tag, msg);
		}
	}

	public static void e(String tag, String msg) {
		if (isDebug) {
			android.util.Log.e(tag, msg);
		}
	}

	public static void e(String tag, String msg, Throwable tr) {
		if (isDebug) {
			android.util.Log.e(tag, msg, tr);
		}
	}

	public static void v(String tag, String msg) {
		if (isDebug) {
			android.util.Log.v(tag, msg);
		}
	}

	public static void v(String tag, String msg, Throwable tr) {
		if (isDebug) {
			android.util.Log.v(tag, msg, tr);
		}
	}

	public static void w(String tag, String msg) {
		if (isDebug) {
			android.util.Log.w(tag, msg);
		}
	}

	public static void w(String tag, Throwable tr) {
		if (isDebug) {
			android.util.Log.w(tag, tr);
		}
	}

	public static void w(String tag, String msg, Throwable tr) {
		if (isDebug) {
			android.util.Log.w(tag, msg, tr);
		}
	}
}
