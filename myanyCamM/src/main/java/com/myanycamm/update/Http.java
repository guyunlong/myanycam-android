package com.myanycamm.update;

import java.io.InputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.myanycamm.utils.ELog;


public class Http implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7584258937152369574L;

	private static final String TAG = "Http";
	
	private boolean mHaveNet = true; // 是否有网络
	private String mNetType = ""; // 网络类型
	private String mAPNType = ""; // 网络接入点
	private boolean mIsError = false; // 是否有错误
	private String mErrorMsg = ""; // 错误信息
	private String mIP = "";
	private int mRespondCode;

	private int mConnectTimeout = 10000; // 设置连接服务器超时时间
	private int mReadTimeout = 10000; // 设置从服务器读取数据超时时间

	/**
	 * 可在一些循环中加入此参数，以在其他线程中控制循环是否继续
	 */
	private boolean mIsLetGo = true;

	public Http() {
	}

	public Http(int connectTimeout, int readTimeout) {
		setConnectTimeout(connectTimeout);
		setRespondCode(readTimeout);
	}


	/**
	 * 获取 一个HttpURLConnection， 这里主要区分CMWAP类型和其他类型
	 * 
	 * @param context
	 * @param httpurl
	 * @param isCmwapType
	 * @return
	 * 
	 * 
	 */
	public HttpURLConnection getHttpURLConnection(Context context,
			String httpurl, boolean isCmwapType) {
		HttpURLConnection conn = null;
		URL url = null;

		try {
			if (isCmwapType) {
				String doMain = Match(httpurl, "//[^/]+").replace("//", "");
				url = new URL(httpurl.replace(doMain, "10.0.0.172"));
				conn = (HttpURLConnection) url.openConnection();
				conn.setRequestProperty("X-Online-Host", doMain);
			} else {
				url = new URL(httpurl);
				conn = (HttpURLConnection) url.openConnection();
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		return conn;
	}

	public static String Match(String content, String reg) {
		Pattern pattern = Pattern.compile(reg);
		Matcher matcher = pattern.matcher(content);
		String value = "";
		if (matcher.find())
			value = matcher.group();
		return value;
	}

	/**
	 * 给url编码
	 * 
	 * @param url
	 * @return
	 */
	public static String encodeUrl(String url) {
		if (null == url)
			return "";

		return java.net.URLEncoder.encode(decodeUrl(url));
	}

	/**
	 * 给url解码
	 * 
	 * @param url
	 * @return
	 */
	public static String decodeUrl(String url) {
		if (null == url)
			return "";

		return java.net.URLDecoder.decode(url);
	}

	/**
	 * 检查网络连接 ，这个给mHaveNet 和 mAPNType赋值！
	 * 
	 * @param context
	 */
	public void checkNetwork(Context context) {
		if (null == context)
			return;

		ConnectivityManager cwjManager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = cwjManager.getActiveNetworkInfo();
		if (info != null && info.isAvailable()) // 有联网
		{
			mHaveNet = true;
			setNetType(info.getTypeName()); // cmwap/cmnet/wifi/uniwap/uninet/HSDPA
			mAPNType = info.getExtraInfo();
			// setIP(IpAddress.GetIP(context));
			return;
		}
		mHaveNet = false;
		return;
		// 如果为True则表示当前Android手机已经联网，可能是WiFi或GPRS、HSDPA等等，具体的可以通过
		// ConnectivityManager 类的getActiveNetworkInfo() 方法判断详细的接入方式。
	}

	/**
	 * 是否CMWAP连接
	 * 
	 * @param context
	 * @return
	 */
	public static boolean isCmwapType(Context context) {
		Http httpB = new Http();
		httpB.checkNetwork(context);
		return httpB.isCmwapType();
	}

	/**
	 * http请求Get
	 * 
	 * @param context
	 * @param httpurl
	 * @param params
	 * @param dealInputStream
	 *            : 在这个回调中处理 返回的流
	 * @return
	 */
	public void get(Context context, String httpurl, String params,
			OnDealConnection dealInputStream) {
		checkNetwork(context);
		ELog.i(TAG,"mHaveNet:"+mHaveNet);
		if (!mHaveNet) {
			setIsError(true);
//			setErrorMsg("没有网络");
			return;
		}
		if (null != params && !params.equals("") && !params.equals(" ")) {
			httpurl = httpurl + "?" + params;
		}

		// CMessage.Show("HttpB get():" + httpurl);
		ELog.i(TAG,"http get:"+httpurl);
		HttpURLConnection conn = getHttpURLConnection(context, httpurl,
				isCmwapType());
		ELog.i(TAG,"isCmwap:"+isCmwapType());

		if (null == conn) {
			setIsError(true);
			setErrorMsg("conn is null");
			return;
		}

		try {
			conn.setDoInput(true);
			conn.setDoOutput(true);

			conn.setConnectTimeout(mConnectTimeout);
			conn.setReadTimeout(mReadTimeout);

			conn.setInstanceFollowRedirects(true);

			conn.setRequestMethod("GET");
			conn.setRequestProperty(
					"Accept",
					"image/gif, image/jpeg, image/pjpeg, image/pjpeg, application/x-shockwave-flash, application/xaml+xml, application/vnd.ms-xpsdocument, application/x-ms-xbap, application/x-ms-application, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, */*");
			conn.setRequestProperty("Accept-Language", "zh-CN");
			// conn.setRequestProperty("Referer", strUrl);
			conn.setRequestProperty("Charset", "UTF-8");
			conn.setRequestProperty(
					"User-Agent",
					"Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727; .NET CLR 3.0.04506.30; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729)");
			conn.setRequestProperty("Connection", "Keep-Alive");

			ELog.i(TAG,"http code="+conn.getResponseCode());
			if (null != dealInputStream) {
				dealInputStream.dealConnection(conn.getInputStream(), conn,conn.getResponseCode());
			}

			setRespondCode(conn.getResponseCode());
			conn.disconnect();
			setIsError(false);
		} catch (Exception e) {
			ELog.e(TAG,"error:"+e.getMessage());
			setIsError(true);
			setErrorMsg(e.toString());
		}

		return;
	}

	/**
	 * 处理流
	 * 
	 * @author Administrator
	 * 
	 */
	public interface OnDealConnection {
		public void dealConnection(InputStream is, URLConnection conn ,int respCode);
	}
	

	/**
	 * 上传状态回调
	 * 
	 * @author Administrator
	 * 
	 */
	public interface OnPostedListener {
		public void posted(long postedSize, long totalSize);
	}


	/**
	 * 可在一些循环中加入此参数，以在其他线程中控制循环是否继续
	 * 
	 * @param isLetGo
	 */
	public void setLetGo(boolean isLetGo) {
		mIsLetGo = isLetGo;
	}

	public boolean isLetGo() {
		return mIsLetGo;
	}

	/**
	 * getter/setter
	 */
	public void setNetType(String netType) {
		this.mNetType = netType;
	}

	public String getNetType() {
		return mNetType;
	}

	public void setIsError(boolean isError) {
		this.mIsError = isError;
	}

	public boolean isError() {
		return mIsError;
	}

	public void setErrorMsg(String errorMsg) {
		this.mErrorMsg = errorMsg;
	}

	public String getErrorMsg() {
		return mErrorMsg;
	}

	public void setIP(String iP) {
		this.mIP = iP;
	}

	public String getIP() {
		return mIP;
	}

	public void setRespondCode(int respondCode) {
		this.mRespondCode = respondCode;
	}

	public int getRespondCode() {
		return mRespondCode;
	}

	public int getConnectTimeout() {
		return mConnectTimeout;
	}

	public void setConnectTimeout(int connectTimeout) {
		this.mConnectTimeout = connectTimeout;
	}

	public int getReadTimeout() {
		return mReadTimeout;
	}

	public void setReadTimeout(int readTimeout) {
		this.mReadTimeout = readTimeout;
	}

	public boolean haveNet() {
		return mHaveNet;
	}

	public void setHaveNet(boolean haveNet) {
		this.mHaveNet = haveNet;
	}

	public String getAPNType() {
		return mAPNType;
	}

	public void setAPNType(String APNType) {
		this.mAPNType = APNType;
	}

	public boolean isCmwapType() {
		return (null == mAPNType) ? false : mAPNType.equals("cmwap");
	}
}
