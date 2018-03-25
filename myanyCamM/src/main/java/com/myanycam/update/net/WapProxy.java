package com.myanycam.update.net;

import java.net.InetSocketAddress;
import java.net.Proxy;

import org.apache.http.HttpHost;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;


public class WapProxy {

	
	private static Context ctx;
	
	private static WapProxy wapProxy;
	
	private static String currentWapTypeName;
	
	public static String[] wapType = new String[] { "cmwap", "Uniwap",
			"ctwap", "3gwap" };

	
	public static WapProxy getInstance(Context context, String[] checkedWapType) {
		if (wapProxy == null) {
			wapProxy = new WapProxy(context, checkedWapType);
		}
		return wapProxy;
	}

	
	private WapProxy(Context contx, String[] checkedWapType) {
		ctx = contx;
		if (checkedWapType != null && checkedWapType.length > 0) {
			wapType = checkedWapType;
		}
	}

	
	private static String getActiveNetworkType() {
		ConnectivityManager conn = (ConnectivityManager) ctx
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		String activeNetworkType="";
		NetworkInfo currentNetInfo=conn.getActiveNetworkInfo();
		if(currentNetInfo!=null){
		   activeNetworkType = currentNetInfo.getTypeName();
		}
		return activeNetworkType;
	}

	
	public boolean needWapProxy() {
		ConnectivityManager conn = (ConnectivityManager) ctx
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (isOnline()) {
			String activeNetworkType = getActiveNetworkType();
			if (activeNetworkType!=null&&activeNetworkType.equalsIgnoreCase("MOBILE")) {
				NetworkInfo mobInfo = conn
						.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
				if(mobInfo!=null)
				currentWapTypeName = mobInfo.getExtraInfo();

				for (int i = 0; i < wapType.length; i++) {
					if (currentWapTypeName!=null && currentWapTypeName.equalsIgnoreCase(wapType[i])) {
						return true;
					}
				}
			} else {
				return false;
			}
		} else {
			return false;
		}
		return false;
	}

	
	public Proxy getProxyByProxy() {
		ConnectivityManager conn = (ConnectivityManager) ctx
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (isOnline()) {
			String activeNetworkType = getActiveNetworkType();
			if (activeNetworkType!=null&&activeNetworkType.equalsIgnoreCase("MOBILE")) {
				NetworkInfo mobInfo = conn
						.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
				if(mobInfo!=null)
				currentWapTypeName = mobInfo.getExtraInfo();
				return getProxyByProxyImpl(currentWapTypeName);
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	
	public HttpHost getProxyByHttpHost() {
		ConnectivityManager conn = (ConnectivityManager) ctx
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (isOnline()) {
			String activeNetworkType = getActiveNetworkType();
			if (activeNetworkType!=null&&activeNetworkType.equalsIgnoreCase("MOBILE")) {
				NetworkInfo mobInfo = conn
						.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
				if(mobInfo!=null)
				currentWapTypeName = mobInfo.getExtraInfo();
				return getProxyByHttpHostImpl(currentWapTypeName);
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	
	public String getWapProxyByURL() {
		ConnectivityManager conn = (ConnectivityManager) ctx
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (isOnline()) {
			String activeNetworkType = getActiveNetworkType();
			if (activeNetworkType!=null&&activeNetworkType.equalsIgnoreCase("MOBILE")) {
				NetworkInfo mobInfo = conn
						.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
				if(mobInfo!=null)
				currentWapTypeName = mobInfo.getExtraInfo();
				return getProxyByURLImpl(currentWapTypeName);
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	
	private static boolean isOnline() {
		ConnectivityManager cm = (ConnectivityManager) ctx
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		if (netInfo != null && netInfo.isConnected()) {
			return true;
		} else {
			return false;
		}
	}

	
	private static Proxy getProxyByProxyImpl(String currents) {
		if(currents==null){
			currents="";
		}
		if (wapType[0].equalsIgnoreCase(currents)) {
			return getChinaMobileWapProxyByProxy();
		} else if (wapType[1].equalsIgnoreCase(currents)) {
			return getChinaUnicomWapProxyByProxy();
		} else if (wapType[2].equalsIgnoreCase(currents)) {
			return getChinaTelecomWapProxyByProxy();
		} else if (wapType[3].equalsIgnoreCase(currents)) {
			return getChinaUnicom3GWapProxyByProxy();
		} else {
			return null;
		}
	}

	
	private static HttpHost getProxyByHttpHostImpl(String currents) {
		if(currents==null){
			currents="";
		}
		if (wapType[0].equalsIgnoreCase(currents)) {
			return getChinaMobileWapProxyByHttpHost();
		} else if (wapType[1].equalsIgnoreCase(currents)) {
			return getChinaUnicomWapProxyByHttpHost();
		} else if (wapType[2].equalsIgnoreCase(currents)) {
			return getChinaTelecomWapProxyByHttpHost();
		} else if (wapType[3].equalsIgnoreCase(currents)) {
			return getChinaUnicom3GWapProxyByHttpHost();
		} else {
			return null;
		}
	}

	
	private static String getProxyByURLImpl(String currents) {
		if(currents==null){
			currents="";
		}
		if (wapType[0].equalsIgnoreCase(currents)) {
			return getChinaMobileWapProxyByURL();
		} else if (wapType[1].equalsIgnoreCase(currents)) {
			return getChinaUnicomWapProxyByURL();
		} else if (wapType[2].equalsIgnoreCase(currents)) {
			return getChinaTelecomWapProxyByURL();
		} else if (wapType[3].equalsIgnoreCase(currents)) {
			return getChinaUnicom3GWapProxyByURL();
		} else {
			return null;
		}
	}

	
	private static Proxy getChinaMobileWapProxyByProxy() {
		Proxy proxy = new Proxy(java.net.Proxy.Type.HTTP,
				new InetSocketAddress("10.0.0.172", 80));
		return proxy;
	}

	
	private static Proxy getChinaUnicomWapProxyByProxy() {
		Proxy proxy = new Proxy(java.net.Proxy.Type.HTTP,
				new InetSocketAddress("10.0.0.172", 80));
		return proxy;
	}

	
	private static Proxy getChinaTelecomWapProxyByProxy() {
		Proxy proxy = new Proxy(java.net.Proxy.Type.HTTP,
				new InetSocketAddress("10.0.0.200", 80));
		return proxy;
	}

	
	private static Proxy getChinaUnicom3GWapProxyByProxy() {
		Proxy proxy = new Proxy(java.net.Proxy.Type.HTTP,
				new InetSocketAddress("10.0.0.172", 80));
		return proxy;
	}

	
	private static HttpHost getChinaMobileWapProxyByHttpHost() {
		HttpHost proxy = new HttpHost("10.0.0.172", 80);
		return proxy;
	}

	
	private static HttpHost getChinaUnicomWapProxyByHttpHost() {
		HttpHost proxy = new HttpHost("10.0.0.172", 80);
		return proxy;
	}

	
	private static HttpHost getChinaTelecomWapProxyByHttpHost() {
		HttpHost proxy = new HttpHost("10.0.0.200", 80);
		return proxy;
	}

	
	private static HttpHost getChinaUnicom3GWapProxyByHttpHost() {
		HttpHost proxy = new HttpHost("10.0.0.172", 80);
		return proxy;
	}

	
	private static String getChinaMobileWapProxyByURL() {
		return "http://10.0.0.172:80/";
	}

	
	private static String getChinaUnicomWapProxyByURL() {
		return "http://10.0.0.172:80/";
	}

	
	private static String getChinaTelecomWapProxyByURL() {
		return "http://10.0.0.200:80/";
	}

	
	private static String getChinaUnicom3GWapProxyByURL() {
		return "http://10.0.0.172:80/";
	}

}
