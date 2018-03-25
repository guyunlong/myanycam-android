package com.myanycamm.utils;


import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

public class HttpUtil {
	// 声明Base URL常量
	public static final String BASE_URL = "http://192.168.0.32:9876/Handler.ashx?type=";

	// 通过url获得HttpGet对象
	public static HttpGet getHttpGet(String url) {
		// 实例化HttpGet
		HttpGet request = new HttpGet(url);
		return request;
	}

	public static HttpPost getHttpPost(String url) {
		// 实例化HttpPost
		HttpPost request = new HttpPost(url);
		return request;
	}

	// 通过HttpGet获得HttpResponse对象
	public static HttpResponse getHttpResponse(HttpGet request)
			throws ClientProtocolException, IOException {
		// 实例化HttpResponse
		HttpResponse response = new DefaultHttpClient().execute(request);
		return response;
	}

	// 通过HttpPost获得HttpResponse对象
	public static HttpResponse getHttpResponse(HttpPost request)
			throws ClientProtocolException, IOException {
		// 实例化HttpResponse
		HttpResponse response = new DefaultHttpClient().execute(request);
		return response;
	}

	// 通过url发送post请求，返回请求结果
	public static String queryStringForPost(String url) {
		// 获得HttpPost实例
		HttpPost request = HttpUtil.getHttpPost(url);
		String result = null;
		try {
			// 获得HttpResponse实例
			HttpResponse response = HttpUtil.getHttpResponse(request);
			// 判断是否请求成功
			if (response.getStatusLine().getStatusCode() == 200) {
				// 获得返回结果
				result = EntityUtils.toString(response.getEntity());
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			result = "网络异常";
		} catch (IOException e) {
			e.printStackTrace();
			result = "网络异常";
		}
		return result;
	}

	// 通过HttpPost发送get请求，返回请求结果
	public static String queryStringForGet(String url) {
		// 获得HttpGet实例
		HttpGet request = HttpUtil.getHttpGet(url);
		String result = null;
		try {
		
			// 获得HttpResponse实例
			HttpResponse response = HttpUtil.getHttpResponse(request);
			// 判断是否请求成功
			if (response.getStatusLine().getStatusCode() == 200) {
				// 获得返回结果
				result = EntityUtils.toString(response.getEntity());
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			result = "网络异常";
		} catch (IOException e) {
			e.printStackTrace();
			result = "网络异常";
		}
		return result;
	}

	// 通过HttpPost发送post请求，返回请求结果
	public static String queryStringForPost(HttpPost request) {
		String result = null;
		try {
			// 获得HttpResponse实例
			HttpResponse response = HttpUtil.getHttpResponse(request);
			// 判断是否请求成功
			if (response.getStatusLine().getStatusCode() == 200) {
				// 获得返回结果
				result = EntityUtils.toString(response.getEntity());
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			result = "网络异常";
		} catch (IOException e) {
			e.printStackTrace();
			result = "网络异常";
		}
		return result;
	}
}
