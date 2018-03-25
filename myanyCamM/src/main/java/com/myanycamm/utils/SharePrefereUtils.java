package com.myanycamm.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class SharePrefereUtils {
	public static void commitStringData(Context context,String key,String value){
		SharedPreferences sp = context.getSharedPreferences("SP", context.MODE_PRIVATE);
		Editor editor = sp.edit();
		editor.putString(key, value);
		editor.commit();
	}
	
	public static String getStringWithKey(Context context,String key){
		SharedPreferences sp = context.getSharedPreferences("SP", context.MODE_PRIVATE);
		String result = "none";
		result = sp.getString(key, "none");
		return result;
	}
	
}
