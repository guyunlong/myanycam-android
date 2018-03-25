package com.myanycam.update.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.content.Context;

public class NetWorkManager {
	public static int SSL_ERROR = 1;
	public static int IO_ERROR = 2;
	public static int NET_SUCCESS = 0;
	public static int NET_UNKNOW_ERROR = -1;
	private int errorType = 0;
	private URL serverURL = null;
	private boolean abort = false;
	
	
	public static final int CONNECTION_TIME_OUT = 10000;
	public static final int SOCKET_TIME_OUT = 30000;
	
	public static final String[] wapType = new String[] { "cmwap", "Uniwap",
			"ctwap", "3gwap" };
	

	public NetWorkManager() {
	}

	
	public boolean isAborted() {
		return abort;
	}

	
	public String sendPostRequestAndWaitResponse(Context ctx, String requsetUrl,
			String requestData) {
		if (null == requsetUrl) {
			return null;
		} else {
			try {
				serverURL = new URL(requsetUrl);
			} catch (MalformedURLException e) {
				return null;
			}
		}

		abort = false;

		try {
			HttpPost request = new HttpPost(requsetUrl);
			HttpParams  httpParameters = new BasicHttpParams(); 
		    HttpConnectionParams.setConnectionTimeout(httpParameters, CONNECTION_TIME_OUT);
		    HttpConnectionParams.setSoTimeout(httpParameters, SOCKET_TIME_OUT);  
			if (requestData != null) {
				StringEntity reqEntity = new StringEntity(requestData, "utf-8") ;
				// 设置类型   
				reqEntity.setContentType("text/plain");   
				// 设置请求的数据   
				request.setEntity(reqEntity);  
			}
			
			if (WapProxy.getInstance(ctx, wapType).needWapProxy()) {
				DefaultHttpClient httpclient = new DefaultHttpClient(httpParameters);
				//获取代理设置
				HttpHost proxy = WapProxy.getInstance(ctx, wapType).getProxyByHttpHost();
				httpclient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
				HttpResponse response = httpclient.execute(request);
				if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
					InputStream  is = response.getEntity().getContent();
					String str = convertStreamToString(is);
					return str;
				} else {
					errorType = IO_ERROR;
					return null;
				}
			} else {
				DefaultHttpClient httpclient = new DefaultHttpClient(httpParameters);
				HttpResponse response = httpclient.execute(request);
				if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK||response.getStatusLine().getStatusCode() == HttpStatus.SC_PARTIAL_CONTENT) {
					InputStream  is = response.getEntity().getContent();
					String str = convertStreamToString(is);
					return str;
				} else {
					errorType = IO_ERROR;
					return null;
				}
			}
		} catch (MalformedURLException ex) {
			errorType = IO_ERROR;
			return null;
		} catch (IOException e) {
			errorType = IO_ERROR;
			return null;
		}
	}
	
	
	public String sendGetRequestAndWaitResponse(Context ctx, String requsetUrl) {
		if (null == requsetUrl) {
			return null;
		} else {
			try {
				serverURL = new URL(requsetUrl);
			} catch (MalformedURLException e) {
				return null;
			}
		}
		abort = false;

		try {
			HttpGet request = new HttpGet(requsetUrl);
			HttpParams  httpParameters = new BasicHttpParams(); 
		    HttpConnectionParams.setConnectionTimeout(httpParameters, CONNECTION_TIME_OUT);
		    HttpConnectionParams.setSoTimeout(httpParameters, SOCKET_TIME_OUT);  
			
			
			if (WapProxy.getInstance(ctx, wapType).needWapProxy()) {
				DefaultHttpClient httpclient = new DefaultHttpClient(httpParameters);
				//获取代理设置
				HttpHost proxy = WapProxy.getInstance(ctx, wapType).getProxyByHttpHost();
				httpclient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
				HttpResponse response = httpclient.execute(request);
				if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
					InputStream  is = response.getEntity().getContent();
					String str = convertStreamToString(is);
					return str;
				} else {
					errorType = IO_ERROR;
					return null;
				}
			} else {
				DefaultHttpClient httpclient = new DefaultHttpClient(httpParameters);
				HttpResponse response = httpclient.execute(request);
				if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
					InputStream  is = response.getEntity().getContent();
					String str = convertStreamToString(is);
					return str;
				} else {
					errorType = IO_ERROR;
					return null;
				}
			}
		} catch (MalformedURLException ex) {
			errorType = IO_ERROR;
			return null;
		} catch (IOException e) {
			errorType = IO_ERROR;
			return null;
		}
	}
	
	
	public boolean sendHeadRequestResponseStatus(Context ctx, String requsetUrl) {
		if (null == requsetUrl) {
			return false;
		} else {
			try {
				serverURL = new URL(requsetUrl);
			} catch (MalformedURLException e) {
				return false;
			}
		}

		abort = false;

		try {
			HttpHead request = new HttpHead(requsetUrl);
			HttpParams  httpParameters = new BasicHttpParams(); 
		    HttpConnectionParams.setConnectionTimeout(httpParameters, CONNECTION_TIME_OUT);
		    HttpConnectionParams.setSoTimeout(httpParameters, SOCKET_TIME_OUT);  
			
		    DefaultHttpClient httpclient = new DefaultHttpClient(httpParameters);
			if (WapProxy.getInstance(ctx, wapType).needWapProxy()) {
				
				//获取代理设置
				HttpHost proxy = WapProxy.getInstance(ctx, wapType).getProxyByHttpHost();
				httpclient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
				
			} 
			HttpResponse response = httpclient.execute(request);
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK||response.getStatusLine().getStatusCode() == HttpStatus.SC_PARTIAL_CONTENT){
					
					return true;
			} else {
					
					return false;
				
			}
		} catch (Exception ex) {
			return false;
		}
	}
	
	
	public HttpEntity sendGetRequestAndWaitHttpEntityResponse(Context ctx, String requsetUrl) {
		if (null == requsetUrl) {
			return null;
		} else {
			try {
				serverURL = new URL(requsetUrl);
			} catch (MalformedURLException e) {
				return null;
			}
		}
		abort = false;

		try {
			 HttpGet request = new HttpGet(requsetUrl);
			 HttpParams  httpParameters = new BasicHttpParams(); 
		    HttpConnectionParams.setConnectionTimeout(httpParameters, CONNECTION_TIME_OUT);
		    HttpConnectionParams.setSoTimeout(httpParameters, SOCKET_TIME_OUT);  
			
			if (WapProxy.getInstance(ctx, wapType).needWapProxy()) {
				DefaultHttpClient httpclient = new DefaultHttpClient(httpParameters);
				//获取代理设置
				HttpHost proxy = WapProxy.getInstance(ctx, wapType).getProxyByHttpHost();
				httpclient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
				HttpResponse response = httpclient.execute(request);
				if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
					HttpEntity  entity = response.getEntity();
//					String str=Utils.convertStreamToString(is);
					return entity;
				} else {
					errorType = IO_ERROR;
					return null;
				}
			} else {
				DefaultHttpClient httpclient = new DefaultHttpClient(httpParameters);
				HttpResponse response = httpclient.execute(request);
				if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
					HttpEntity  entity = response.getEntity();
//					String str=Utils.convertStreamToString(is);
					return entity;
				} else {
					errorType = IO_ERROR;
					return null;
				}
			}
		} catch (MalformedURLException ex) {
			errorType = IO_ERROR;
			return null;
		} catch (IOException e) {
			errorType = IO_ERROR;
			return null;
		}
	}
	
	
	
	public HttpResponse sendGetRequestAndWaitHttpEntityResponse(Context ctx, String requsetUrl,Header[]  mHeaders) {
		if (null == requsetUrl) {
			return null;
		} else {
			try {
				serverURL = new URL(requsetUrl);
			} catch (MalformedURLException e) {
				return null;
			}
		}
		abort = false;

		try {
			 HttpGet request = new HttpGet(requsetUrl);
			 if(mHeaders!=null){
				 for(Header header:mHeaders){
					 request.addHeader(header);
				 }
			 }
			HttpParams  httpParameters = new BasicHttpParams(); 
		    HttpConnectionParams.setConnectionTimeout(httpParameters, CONNECTION_TIME_OUT);
		    HttpConnectionParams.setSoTimeout(httpParameters, SOCKET_TIME_OUT);  
			
			if (WapProxy.getInstance(ctx, wapType).needWapProxy()) {
				DefaultHttpClient httpclient = new DefaultHttpClient(httpParameters);
				//获取代理设置
				HttpHost proxy = WapProxy.getInstance(ctx, wapType).getProxyByHttpHost();
				httpclient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
				HttpResponse response = httpclient.execute(request);
				return response;
			} else {
				DefaultHttpClient httpclient = new DefaultHttpClient(httpParameters);
				HttpResponse response = httpclient.execute(request);
				return response;
			}
		} catch (MalformedURLException ex) {
			errorType = IO_ERROR;
			return null;
		} catch (IOException e) {
			errorType = IO_ERROR;
			return null;
		}
	}
	
		

	
	public int getErrorType() {
		return errorType;
	}

	
	public static String convertStreamToString(InputStream is) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();
		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				sb.append(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return sb.toString();
	}
}
