package com.myanycam.net;

import java.lang.reflect.Method;

import android.content.Context;
import android.content.Intent;
import android.graphics.SweepGradient;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;

public class NetworkManager {

	private Context context;
	private ConnectivityManager connManager;

	public NetworkManager(Context context) {
		this.context = context;
		connManager = (ConnectivityManager) this.context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
	}

	
	public boolean isNetworkConnected() {

		NetworkInfo networkinfo = connManager.getActiveNetworkInfo();

		if (networkinfo != null) {
			return networkinfo.isConnected();
		}

		return false;
	}

	
	public boolean isWifiConnected() {

		NetworkInfo mWifi = connManager
				.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

		if (mWifi != null) {
			return mWifi.isConnected();
		}

		return false;
	}
	
	public String getWifiSSid(){
		WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		return wifiInfo.getSSID();
	}

	
	public boolean isMobileConnected() {

		NetworkInfo mMobile = connManager
				.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

		if (mMobile != null) {
			return mMobile.isConnected();
		}
		return false;
	}

	
	public boolean is3GConnected() {
		boolean result = false;
		NetworkInfo networkinfo = connManager.getActiveNetworkInfo();
		if (isMobileConnected()) {
			// NETWORK_TYPE_EVDO_A是电信3G
			// NETWORK_TYPE_EVDO_A是中国电信3G的getNetworkType
			// NETWORK_TYPE_CDMA电信2G是CDMA
			// 移动2G卡 + CMCC + 2//type = NETWORK_TYPE_EDGE
			// 联通的2G经过测试 China Unicom 1 NETWORK_TYPE_GPRS
			switch (networkinfo.getSubtype()) {
			case TelephonyManager.NETWORK_TYPE_1xRTT:
			case TelephonyManager.NETWORK_TYPE_GPRS:// 联通2G
			case TelephonyManager.NETWORK_TYPE_EDGE:// 移动2G
			case TelephonyManager.NETWORK_TYPE_CDMA:// 电信2G
			case TelephonyManager.NETWORK_TYPE_IDEN:// IDEN(2G)
				result = false;
				break;
			// case 3://联通3G
			case TelephonyManager.NETWORK_TYPE_EVDO_0:// 电信3G
			case TelephonyManager.NETWORK_TYPE_EVDO_A:// 电信3G
			case TelephonyManager.NETWORK_TYPE_EVDO_B:// 电信3G
			case TelephonyManager.NETWORK_TYPE_LTE:// LTE(4G)
			case TelephonyManager.NETWORK_TYPE_EHRPD:
			case TelephonyManager.NETWORK_TYPE_UMTS:
			case TelephonyManager.NETWORK_TYPE_HSUPA:
			case TelephonyManager.NETWORK_TYPE_HSPA:
			case TelephonyManager.NETWORK_TYPE_HSDPA:
			case 15:
				result = true;
				break;

			default:
				break;
			}
		}
		return result;
	}

	
	public void toggleGprs(boolean isEnable) throws Exception {
		Class<?> cmClass = connManager.getClass();
		Class<?>[] argClasses = new Class[1];
		argClasses[0] = boolean.class;

		// 反射ConnectivityManager中hide的方法setMobileDataEnabled，可以开启和关闭GPRS网络
		Method method = cmClass.getMethod("setMobileDataEnabled", argClasses);
		method.invoke(connManager, isEnable);
	}

	
	public boolean toggleWiFi(boolean enabled) {
		WifiManager wm = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);
		return wm.setWifiEnabled(enabled);

	}

	
	public boolean isAirplaneModeOn() {
		// 返回值是1时表示处于飞行模式
		int modeIdx = Settings.System.getInt(context.getContentResolver(),
				Settings.System.AIRPLANE_MODE_ON, 0);
		boolean isEnabled = (modeIdx == 1);
		return isEnabled;
	}

	
	public void toggleAirplaneMode(boolean setAirPlane) {
		Settings.System.putInt(context.getContentResolver(),
				Settings.System.AIRPLANE_MODE_ON, setAirPlane ? 1 : 0);
		// 广播飞行模式信号的改变，让相应的程序可以处理。
		// 不发送广播时，在非飞行模式下，Android 2.2.1上测试关闭了Wifi,不关闭正常的通话网络(如GMS/GPRS等)。
		// 不发送广播时，在飞行模式下，Android 2.2.1上测试无法关闭飞行模式。
		Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
		// intent.putExtra("Sponsor", "Sodino");
		// 2.3及以后，需设置此状态，否则会一直处于与运营商断连的情况
		intent.putExtra("state", setAirPlane);
		context.sendBroadcast(intent);
	}
}
