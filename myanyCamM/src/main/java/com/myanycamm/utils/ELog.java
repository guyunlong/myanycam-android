package com.myanycamm.utils;



public class ELog {
	public static void i(String tag,String msg){
		if(Constants.isDebug){
			android.util.Log.i(tag, msg);
		}
	}
	
	public static void i(String msg){
		if(Constants.isDebug){
			android.util.Log.i(Constants.TAG, msg);
		}
	}
 
	public static void i(String tag, String msg, Throwable tr){
		if(Constants.isDebug){
			android.util.Log.i(tag, msg,tr);
		}
	}
	
	public static void d(String msg){
		if(Constants.isDebug){
			android.util.Log.d(Constants.TAG, msg);
		}
	}
	
	public static void d(String tag,String msg){
		if(Constants.isDebug){
			android.util.Log.d(tag, msg);
		}
	}
	
	public static  void e(String msg){
		if(Constants.isDebug){
			android.util.Log.e(Constants.TAG, msg);
		}
	}
	
	public static  void e(String tag,String msg){
		if(Constants.isDebug){
			android.util.Log.e(tag, msg);
		}
	}
	
	public static void e(String tag, String msg, Throwable tr){
		if(Constants.isDebug){
			android.util.Log.e(tag, msg,tr);
		}
	}
	
	public static void v(String msg){
		if(Constants.isDebug){
			android.util.Log.v(Constants.TAG, msg);
		}
	}
	
	public static void v(String tag, String msg){
		if(Constants.isDebug){
			android.util.Log.v(tag, msg);
		}
	}
	
	public static void v(String tag, String msg, Throwable tr){
		if(Constants.isDebug){
			android.util.Log.v(tag, msg,tr);
		}
	}
	
	public static void w( String msg){
		if(Constants.isDebug){
			android.util.Log.w(Constants.TAG, msg);
		}
	}
	
	public static void w(String tag, String msg){
		if(Constants.isDebug){
			android.util.Log.w(tag, msg);
		}
	}
	
	public static void w(String tag, Throwable tr){
		if(Constants.isDebug){
			android.util.Log.w(tag,tr);
		}
	}
	
	public static void w(String tag, String msg, Throwable tr){
		if(Constants.isDebug){
			android.util.Log.w(tag,msg,tr);
		}
	}
	
	public static void wtf( String msg){
		if(Constants.isDebug){
			android.util.Log.wtf(Constants.TAG, msg);
		}
	}
	
	public static void wtf(String tag, String msg){
		if(Constants.isDebug){
			android.util.Log.wtf(tag, msg);
		}
	}
	
	public static void wtf(String tag, Throwable tr){
		if(Constants.isDebug){
			android.util.Log.wtf(tag,tr);
		}
	}
	
	public static void wtf(String tag, String msg, Throwable tr){
		if(Constants.isDebug){
			android.util.Log.wtf(tag,msg,tr);
		}
	}
}
