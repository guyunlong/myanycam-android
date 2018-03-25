package com.myanycamm.utils;

import java.util.LinkedList;

import android.graphics.drawable.Drawable;
import android.widget.ImageView;

public class Constants {
	
	public static final boolean isDebug = true;
	public static final int VERIFYTYPE = 1002;// 通信验证参数
	public static final String TAG = "MyAnyCam";

	public static final int SOCKETTIMEOUT = 10000;// Socket延时检测时间
	public static final int VIDEOTIMEOUT = 6000;// 视频超时时间
	
	public static final int CONNECTION_TIME_OUT = 25000;

	public static  String SOCKET_SECOND_IP = "app.myanycam.com";
	public static String SOCKETIP = "54.235.154.234";
//	 public static final String SOCKETIP = "192.168.0.102";
	public static final int SOCKETIPPORT = 5200;
	public static final String LOCALSOCKETIP = "192.168.42.1";
	public static final int LOCALSOCKETIPPORT = 5200;

	// twitter相关
	public static String CONSUMER_KEY = "9gHR800sriWxaYeLytw";
	public static String CONSUMER_SECRET = "59JmLaGrl0wL0Ax2WXfIcG5PRdyZ0KRwPktleeiLqY";
	public static final String CALLBACK_URL = "oauth://myanycam";
	public static String PREFERENCE_NAME = "twitter_oauth";
	public static final String PREF_KEY_SECRET = "oauth_token_secret";
	public static final String PREF_KEY_TOKEN = "oauth_token";
	public static final String IEXTRA_AUTH_URL = "auth_url";
	public static final String IEXTRA_OAUTH_VERIFIER = "oauth_verifier";
	public static final String IEXTRA_OAUTH_TOKEN = "oauth_token";
	
	//二维码
	public static final int CAMERADEGREE = 90;//摄像头角度，90正常，需要翻转时改为270

	// 升级相关
	// TODO 打包时需配置的字段
	public static final String CID2 = "bwcn3529_";// bkcn3510_ 正式出包时，请配置该项
	public static final String CHANNELID = "47695";// 测试cid=46804 正式出包时，请配置该项
	public static final String CHANNELID_SUFFIX = "_D_1";
	//
	public static final String CID = "&cid=" + CID2;
	public static final String SOFTWARENAME = "Myanycam";
	public static final String PRODUCTID = "12";// 此productId是版本升级用的
	public static final String updateUrl = "http://121.199.6.197/appclient/kcam/update.xml";

	
	private static LinkedList<String> extens = null;

	public static LinkedList<String> getExtens() {
		if (extens == null) {
			extens = new LinkedList<String>();
			extens.add("JPEG");
			extens.add("JPG");
			extens.add("PNG");
			extens.add("GIF");
			extens.add("BMP");
		}
		return extens;
	}

	// 显示实体
	public static class gridItemEntity {
//		public Drawable image;
		public String path;
		public int index;
		public ImageView imageView;
	}
}
