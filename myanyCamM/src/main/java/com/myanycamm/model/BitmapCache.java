package com.myanycamm.model;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.myanycam.net.SocketFunction;
import com.myanycamm.cam.LoginActivity;
import com.myanycamm.utils.ELog;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

public class BitmapCache {
	private final static String TAG = "BitmapCache";
	static private BitmapCache cache;
	private Context context;
//	private String mURLString;
//	private static String fileName;
	
	
	private static Map<String, SoftReference<Bitmap>> mMemCache = new HashMap<String, SoftReference<Bitmap>>();
	public BitmapCache() {
		// TODO Auto-generated constructor stub
	}

	public BitmapCache(Context context) {
		this.context = context;
//		this.mURLString = mURLString;
//		this.fileName = fileName;
	}
	
	public void downImage(String mURLString,String fileName){
		new DownTask().execute(mURLString,fileName,null,null);
	}
	

	public static Bitmap getBitmapFromDiskCache(Context context, String fileName) {
		Bitmap bitmap = null;
		File path = context.getCacheDir();
		InputStream is = null;
		// String hashedURLString = hashURLString(urlString);

		File file = new File(path, fileName + "");

		if (file.exists() && file.canRead()) {
			// check for timeout

			try {
				is = new FileInputStream(file);
				bitmap = BitmapFactory.decodeStream(is);
				Log.v(TAG, "Retrieved " + fileName + " from disk cache.");
			} catch (Exception ex) {
				Log.e(TAG, "Could not retrieve " + fileName + " from disk cache: "
						+ ex.toString());
			} finally {
				try {
					is.close();
				} catch (Exception ex) {
				}
			}
		}

		return bitmap;

	}
	

	public void addBitmapToMemCache(String fileName, Bitmap bitmap) {
			synchronized (mMemCache) {
				mMemCache.put(fileName, new SoftReference<Bitmap>(bitmap));
			}
	}
	
	public static Bitmap getBitmapFromMemCache(Context context,String fileName) {
		ELog.i(TAG, "从内存中取数据...");
			synchronized (mMemCache) {
				SoftReference<Bitmap> bitmapRef = mMemCache.get(fileName);
//				
				if (bitmapRef != null) {
					Bitmap bitmap = bitmapRef.get();
//					
					if (bitmap == null) {
						bitmap = getBitmapFromDiskCache(context, fileName);
						if (bitmap != null) {							
							mMemCache.put(fileName, new SoftReference<Bitmap>(bitmap));
							return bitmap;
						}else{
							mMemCache.remove(fileName);
						}					
				        Log.v(TAG, "Expiring memory cache for URL " + fileName + ".");
					} else {
				        Log.v(TAG, "Retrieved " + fileName + " from memory cache.");
						return bitmap; 
					}
				}else{
					Bitmap bitmap = getBitmapFromDiskCache(context, fileName);
					if (bitmap != null) {							
						mMemCache.put(fileName, new SoftReference<Bitmap>(bitmap));
						return bitmap;
					}
				}		
			}
		
		return null;
	}
	

	public void addBitmapToCache(Context context, String fileName, Bitmap bitmap) {
		// disk cache
		File path = context.getCacheDir();
		OutputStream os = null;
		// String hashedURLString = hashURLString(urlString);

		try {
			// NOWORKY File tmpFile = File.createTempFile("wic.", null);
			File file = new File(path, "temp" + fileName);
			os = new FileOutputStream(file.getAbsolutePath());
			bitmap.compress(Bitmap.CompressFormat.JPEG,70, os);
//			bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
			os.flush();
			os.close();
			file.renameTo(new File(file.getParent() + File.separator + fileName));

			// NOWORKY tmpFile.renameTo(file);
		} catch (Exception ex) {
			Log.e(TAG,
					"Could not store " + fileName + " to disk cache: "
							+ ex.toString());
		} finally {
			try {
				os.close();
			} catch (Exception ex) {
			}
		}

	}

	static class FlushedInputStream extends FilterInputStream {
		public FlushedInputStream(InputStream inputStream) {
			super(inputStream);
		}

		@Override
		public long skip(long n) throws IOException {
			long totalBytesSkipped = 0L;

			while (totalBytesSkipped < n) {
				long bytesSkipped = in.skip(n - totalBytesSkipped);

				if (bytesSkipped == 0L) {
					int b = read();

					if (b < 0) {
						break;
					} else {
						bytesSkipped = 1;
					}
				}

				totalBytesSkipped += bytesSkipped;
			}

			return totalBytesSkipped;
		}
	}

	private class DownTask extends AsyncTask<String, Void, Void> {

		@Override
		protected Void doInBackground(String... params) {

			String path = context.getCacheDir()+File.separator+"temp" + params[1];
			saveImage(path, params[0]);
			Bitmap bitmap = getBitmapFromDiskCache(context, "temp" + params[1]);
			if (null != bitmap) {
				File file = new File(path);
				file.renameTo(new File(file.getParent() + File.separator + params[1]));
				addBitmapToMemCache(params[1], bitmap);
			}

				if (bitmap != null) {
					addBitmapToMemCache(params[1], bitmap);
					addBitmapToCache(context, params[1], bitmap);
					// 下载后通知
					Message msg = new Message();
					msg.what = LoginActivity.UPDATECAMIMAGE;
					Bundle mBundle = new Bundle();
					HashMap<String, String> map = new HashMap<String, String>();
					map.put("camId", params[1]);
					mBundle.putSerializable("data", map);
					msg.setData(mBundle);
					SocketFunction.getInstance().getmHandler().sendMessage(msg);
				}


			return null;
		}



	}
	
	
	
	public void saveImage1(String path, String urlString){
		try {
		 // 构造URL   
        URL url = new URL(urlString);   
        // 打开连接   
        URLConnection con = url.openConnection();   
        //设置请求超时为5s   
        con.setConnectTimeout(5*1000);   
        // 输入流   
        InputStream is = con.getInputStream();   
       
        // 1K的数据缓冲   
        byte[] bs = new byte[1024];   
        // 读取到的数据长度   
        int len;   
        // 输出的文件流   
//       File sf1=new File(path);   
//       if(!sf.exists()){   
//           sf.mkdirs();   
//       }   
       OutputStream os;

		os = new FileOutputStream(path);
        // 开始读取   
        while ((len = is.read(bs)) != -1) {   
          os.write(bs, 0, len);   
        }   
        // 完毕，关闭所有链接   
        os.close();   
        is.close(); 
	} catch (FileNotFoundException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}   
  

	}
	
	public void saveImage(String path, String httpUrl) {
		// new一个URL对象
		URL url;
		try {
			url = new URL(httpUrl);
			// 打开链接
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			// 设置请求方式为"GET"
			conn.setRequestMethod("GET");
			// 超时响应时间为5秒
			conn.setConnectTimeout(5 * 1000);
			// 通过输入流获取图片数据
			InputStream inStream = conn.getInputStream();
			// 得到图片的二进制数据，以二进制封装得到数据，具有通用性
			byte[] data = readInputStream(inStream);
			// new一个文件对象用来保存图片，默认保存当前工程根目录
			File imageFile = new File(path);
			// 创建输出流
			FileOutputStream outStream = new FileOutputStream(imageFile);
			// 写入数据
			outStream.write(data);
			// 关闭输出流
			outStream.close();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	private byte[] readInputStream(InputStream inStream) throws Exception {
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		// 创建一个Buffer字符串
		byte[] buffer = new byte[1024];
		// 每次读取的字符串长度，如果为-1，代表全部读取完毕
		int len = 0;
		// 使用一个输入流从buffer里把数据读取出来
		while ((len = inStream.read(buffer)) != -1) {
			// 用输出流往buffer里写入数据，中间参数代表从哪个位置开始读，len代表读取的长度
			outStream.write(buffer, 0, len);
		}
		// 关闭输入流
		inStream.close();
		// 把outStream里的数据写入内存
		return outStream.toByteArray();
	}
}
