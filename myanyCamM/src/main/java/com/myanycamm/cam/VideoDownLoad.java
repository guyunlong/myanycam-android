package com.myanycamm.cam;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;

import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StatFs;
import android.widget.Toast;

import com.myanycam.net.SocketFunction;
import com.myanycam.update.net.NetWorkManager;
import com.myanycamm.process.ScreenManager;
import com.myanycamm.update.DownLoadFile;
import com.myanycamm.update.DownLoadFileListener;
import com.myanycamm.update.UpdateProgressListener;
import com.myanycamm.update.UpdateService;
import com.myanycamm.utils.ELog;
import com.myanycamm.utils.FileUtils;
import com.myanycamm.utils.NotificationUtils;
import com.myanycamm.utils.Utils;

public class VideoDownLoad {
	
	private final static String TAG = "VideoDownLoad";
	private static final int MSG_UPDATE_DOWNLOAD_PERCENT = 301;
	
	// 下载相关

	
	public static final byte UPDATE_STATE_CHECK = 0;
	
	public static final byte UPDATE_STATE_UPDATING = 1;
	
	public static final byte UPDATE_STATE_UPDATE_ERROR = 2;
	
	public static final byte UPDATE_STATE_UPDATE_SUCCESS = 3;
	

	
	private byte state;
	private ArrayList<UpdateProgressListener> listeners;
	
	private boolean isStartUpdate = false;
	
	private DownLoadFile dlFile;
	
	private String savePath;

	
	private long downFileMaxLen;
	
	private long downFileCurrLen;

	private int downloadPercent = 0;

	public long getMaxLen() {
		return downFileMaxLen;
	}

	public void setMaxLen(long maxLen) {
		this.downFileMaxLen = maxLen;
	}

	public long getCurrLen() {
		return downFileCurrLen;
	}

	public void setCurrLen(long currLen) {
		this.downFileCurrLen = currLen;
	}

	private static VideoDownLoad mVideoDownLoad;
	
	private Handler mHandler = new Handler(){
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_UPDATE_DOWNLOAD_PERCENT:
				int percent = (Integer) msg.obj;
				ELog.w(TAG, "percent:" + percent);
				NotificationUtils.updateNotication(null, SocketFunction.getAppContext(),
						SocketFunction.getAppContext().getString(R.string.downloaded_video),
						percent + "%");
				break;

			default:
				break;
			}
		};
	};

	private VideoDownLoad() {
		listeners = new ArrayList<UpdateProgressListener>();
	}

	public static VideoDownLoad getInstance() {
		if (mVideoDownLoad == null) {
			mVideoDownLoad = new VideoDownLoad();
		}
		return mVideoDownLoad;
	}

	
	public synchronized void downloadApkFile(String url,
			UpdateProgressListener listener) throws Exception {
		addUpdateProgressListener(listener);
		ELog.i(TAG, "下载..+"+url);
		if (isStartUpdate) {
			showToast(SocketFunction.getAppContext().getString(R.string.tip_pre_video_downing));
			return;
		}
		// 检查是否有存储卡，没有则不下载并提示
		if (!FileUtils.externalMemoryAvailable()) {
			throw new Exception(
					SocketFunction.getAppContext().getString(R.string.sdcard_invalid));
		}
		// if (!externalMemoryAvailable()) {
		// throw new Exception(mContext.getString(R.string.tip_no_sdcard));
		// }
		// 判断下载地址是否为空，为空则不下载并提示
		if (url == null) {
			throw new Exception(
					SocketFunction.getAppContext().getString(R.string.tip_url_illegal));
		}
		ELog.v(TAG, "addDownLoadFile url=" + url);

		// 判断网络是否连接正常，没有连接则不下载并提示
		if (!Utils.isNetworkAvailable(SocketFunction.getAppContext())) {
			throw new Exception(
					SocketFunction.getAppContext()
							.getString(R.string.newwork_unavailable));
		}

		// 形成下载文件，设定下载文件名
		dlFile = new DownLoadFile(url);

		new Thread(new DownApkRunnable(dlFile)).start();
	}

	public void addUpdateProgressListener(UpdateProgressListener listener) {
		if (listener == null) {
			return;
		}
		for (UpdateProgressListener l : listeners) {
			if (l == listener) {
				return;
			}
		}
		listeners.add(listener);
	}
	
	public static void showToast(final String tip) {
		if (ScreenManager.getScreenManager().currentActivity() == null) {
			return;
		}
		ScreenManager.getScreenManager().currentActivity() .runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(ScreenManager.getScreenManager().currentActivity(), tip, Toast.LENGTH_LONG).show();
			}
		});
	}



	public static String getExternalStoragePath() {
		// 获取SdCard状态
		String state = android.os.Environment.getExternalStorageState();

		// 判断SdCard是否存在并且是可用的
		if (android.os.Environment.MEDIA_MOUNTED.equals(state)) {
			if (android.os.Environment.getExternalStorageDirectory().canWrite()) {
				return android.os.Environment.getExternalStorageDirectory()
						.getPath();
			}
		}
		ELog.w(TAG, "SD can not be used");
		return null;
	}

	private void updateFailed(String tip, String content) {
		isStartUpdate = false;
		NotificationUtils.showUpdateNotication(SocketFunction.getAppContext(), tip,
				content, false,WelcomeActivity.class);
		stopUpdateService();
		if (content != null) {
			tip += "," + content;
		}
		showToast(SocketFunction.getAppContext().getString(R.string.down_load_video_failed));
		// showFailedActivity(tip);
	}
	
	private void updateSuccess(String tip, String content){
		isStartUpdate = false;
		NotificationUtils.showUpdateNotication(SocketFunction.getAppContext(), tip,
				content, false,WelcomeActivity.class);
		stopUpdateService();
		if (content != null) {
			tip += "," + content;
		}
		showToast(SocketFunction.getAppContext().getString(R.string.down_load_video_success));
		if (ScreenManager.getScreenManager().currentActivity().getClass().equals(CameraCenterActivity.class)) {
			CameraCenterActivity activity = (CameraCenterActivity) ScreenManager.getScreenManager().currentActivity();
			activity.mFileListView.dataChange();
		}
	}

	private void stopUpdateService() {
		Intent intent = new Intent();
		intent.setClass(SocketFunction.getAppContext(), UpdateService.class);
		SocketFunction.getAppContext().stopService(intent);
	}

	public static long getTotalExternalMemorySize() {
		File path = Environment.getExternalStorageDirectory();
		StatFs stat = new StatFs(path.getPath());
		// long blockSize = stat.getBlockSize();
		// long totalBlocks = stat.getBlockCount();
		// return totalBlocks * blockSize;

		long blockSize = stat.getBlockSize();
		long blocks = stat.getAvailableBlocks();
		long availableSpare = (blocks * blockSize);
		return availableSpare;
	}

	public void downLoadProgressChanged(DownLoadFile df, long maxLen,
			long currLen) {
		downFileMaxLen = maxLen;
		downFileCurrLen = currLen;
		int percent = 0;
		if (downFileMaxLen != 0) {
			percent = (int) (downFileCurrLen * 100 / downFileMaxLen);
		}else{
			percent = 100;
		}
		if (downloadPercent != percent) {
			downloadPercent = percent;
			Message msg = new Message();
			msg.what = MSG_UPDATE_DOWNLOAD_PERCENT;
			msg.obj = downloadPercent;
			mHandler.removeMessages(MSG_UPDATE_DOWNLOAD_PERCENT);
			mHandler.sendMessage(msg);
		}
		for (UpdateProgressListener listener : listeners) {
			listener.downLoadProgressChanged(df, maxLen, currLen);
		}
	}

	public void downLoadStateChanged(DownLoadFile df, byte state, Object object) {
		this.state = state;
		for (UpdateProgressListener listener : listeners) {
			listener.downLoadStateChanged(df, state, object);
		}
	}

	
	class DownApkRunnable implements Runnable {
		private DownLoadFile dlFile;

		public DownApkRunnable(DownLoadFile dlFile) {
			this.dlFile = dlFile;
		}

		@Override
		public synchronized void run() {
			// 开始下载前先提示
			showToast(SocketFunction.getAppContext().getString(R.string.tip_download_begin));

			isStartUpdate = true;

			// 初始化下载路径，确保下载路径存在并可用
			savePath = getExternalStoragePath() + "/myanycam/" + "video/";
			isStartUpdate = true;
			// setState(UPDATE_STATE_UPDATING);
			ELog.d(TAG, "start to download update info...");
//			FileManager.deleteDirectory(savePath);
			ELog.d(TAG, "updateDataSavePath=" + savePath);
			File file = new File(savePath);
			if (!file.exists()) {
				file.mkdirs();
			}
			dlFile.setSavePath(savePath);

			// 初始化文件路径及临时存储路径
			final String filePath = dlFile.getSavePath() + dlFile.getFileName();
			String tempFilePath = savePath + dlFile.getTempFileName();
			ELog.i(TAG, "filePath=" + filePath);
			ELog.i(TAG, "tempFilePath=" + tempFilePath);
			if (tempFilePath == null
					|| (tempFilePath = tempFilePath.trim()).length() <= 0) {
				updateFailed(SocketFunction.getAppContext().getString(R.string.down_load_video_failed),
						null);
				return;
			}
			long currLen = 0;
			long maxLen = 0;
			InputStream is = null;
			// 下载失败重试次数
			int retryCount = 2;
			boolean isCreateFileSuccess = false;
			for (int i = 0; i <= retryCount; i++) {
				try {
					ELog.e("第" + i + "次连接网络...");
					final File tempfile = new File(tempFilePath);
					if (tempfile.exists()) {
						// 第1次连接时，确保临时存放的文件是空的，存在的
						if (i == 0) {
							tempfile.delete();
							isCreateFileSuccess = tempfile.createNewFile();
						} else {// 重试时，读取临时文件的大小，如支持断点就继续下载，否则从头下载
							currLen = tempfile.length();
							ELog.v("file has download portion :currLen="
									+ currLen);
							isCreateFileSuccess = true;
						}
					} else {
						isCreateFileSuccess = tempfile.createNewFile();
					}
					NetWorkManager netManager = new NetWorkManager();
					Header[] mHeaders = null;

					if (currLen > 0) {
						mHeaders = new Header[1];
						final long tempLen = currLen;
						Header header = new Header() {
							@Override
							public String getValue() {
								return "bytes=" + tempLen + "-";
							}

							@Override
							public String getName() {
								return "RANGE";
							}

							@Override
							public HeaderElement[] getElements()
									throws ParseException {
								return null;
							}
						};
						mHeaders[0] = header;
					}

					HttpResponse response = netManager
							.sendGetRequestAndWaitHttpEntityResponse(
									SocketFunction.getAppContext(), dlFile.getUrl(),
									mHeaders);
					if (response == null) {
						ELog.i(TAG, "下载没反应");
						isStartUpdate = false;
						// throw new Exception();
						// downLoadStateChanged(dlFile,
						// UPDATE_STATE_UPDATE_ERROR,
						// null);
						 updateFailed(
						 SocketFunction.getAppContext().getString(R.string.down_load_video_failed),
						 SocketFunction.getAppContext().getString(R.string.notification_update_error4));
						return;
					}
					int respCode = response.getStatusLine().getStatusCode();
					ELog.i("下载视频，respCode:" + respCode);
					HttpEntity httpEntiy;
					if (respCode == HttpStatus.SC_OK
							|| respCode == HttpStatus.SC_PARTIAL_CONTENT) {// 206表示支持断点续传){
						httpEntiy = response.getEntity();
					} else {
						throw new Exception();
					}

					if (httpEntiy == null) {
						throw new Exception();
					}

					FileOutputStream fos = null;

					// 判断是否支持断点续传
					Header[] allHeaders = response.getAllHeaders();
					if (allHeaders != null) {
						for (Header header : allHeaders) {
							ELog.w("升级包head： key=" + header.getName()
									+ "  value=" + header.getValue());
							if (header.getName().equals("Accept-Ranges")) {
								if ((header.getValue() != null && header
										.getValue().equalsIgnoreCase("bytes"))
										|| respCode == HttpStatus.SC_PARTIAL_CONTENT) {
									ELog.i("升级包，支持断点续传的...");
									fos = new FileOutputStream(tempfile, true);
								} else {
									ELog.i("升级包，不..支持断点续传的...");
									fos = new FileOutputStream(tempfile);
									currLen = 0;
								}
							}
						}
					}

					// 长度取第一次联网获取的长度
					if (i == 0 && maxLen <= 0) {
						maxLen = httpEntiy.getContentLength();
					}

					ELog.i("下载，文件大小：" + maxLen + "  sd卡剩余空间："
							+ getTotalExternalMemorySize());

					if (maxLen > getTotalExternalMemorySize()) {
						ELog.i("文件下载前比较，空间不足，取消下载...");
						// downLoadStateChanged(dlFile,
						// UPDATE_STATE_UPDATE_ERROR,
						// null);
						// updateFailed(
						// mContext.getString(R.string.notification_update_error_title),
						// mContext.getString(R.string.notification_update_error2));
						return;
					}
					setMaxLen(maxLen);
					setCurrLen(currLen);

					downLoadProgressChanged(dlFile, maxLen, currLen);
					is = httpEntiy.getContent();
					byte[] data = new byte[1024];
					int len;
					downLoadStateChanged(dlFile, UPDATE_STATE_UPDATING, null);
					// 流读取完或是文件被暂停后停止读取数据
					while ((len = is.read(data)) != -1) {
						fos.write(data, 0, len);
						currLen += len;
						ELog.v(TAG, "down....currLen=" + currLen);
						downLoadProgressChanged(dlFile, maxLen, currLen);
						dlFile.setCurrSize(currLen);
					}
					fos.flush();
					fos.close();
					is.close();
					// 当下载完后才重命名
					isStartUpdate = false;
					updateSuccess(SocketFunction.getAppContext().getString(R.string.down_load_video_success), "100%");
					ELog.v(TAG, dlFile.getTempFileName() + "  rename to :"
							+ dlFile.getFileName());
					file.renameTo(new File(savePath + dlFile.getFileName()));
					// downLoadFileCompleted(dlFile);
					downLoadStateChanged(dlFile, UPDATE_STATE_UPDATE_SUCCESS,
							null);
					dlFile.setState(DownLoadFileListener.STATE_DOWNCOMPLETE);
					break;
				} catch (Exception e) {
					ELog.e("Error:" + e.getMessage());
					e.printStackTrace();
					if (!isCreateFileSuccess) {
						downLoadStateChanged(dlFile, UPDATE_STATE_UPDATE_ERROR,
								null);
						ELog.i(TAG, "创建文件失败");
						// updateFailed(
						// mContext.getString(R.string.notification_update_error_title),
						// mContext.getString(R.string.notification_update_error5));
						break;
					} else if (getTotalExternalMemorySize() <= 0) {
						downLoadStateChanged(dlFile, UPDATE_STATE_UPDATE_ERROR,
								null);
						ELog.i(TAG, "空间不足");
						// updateFailed(
						// mContext.getString(R.string.notification_update_error_title),
						// mContext.getString(R.string.notification_update_error2));
						break;
					} else if (i >= retryCount) {
						isStartUpdate  = false;
						showToast(SocketFunction.getAppContext().getString(R.string.tip_url_illegal));
						if (!Utils.isNetworkAvailable(SocketFunction.getAppContext())) {
							downLoadStateChanged(dlFile,
									UPDATE_STATE_UPDATE_ERROR, null);
							ELog.i(TAG, "网络问题..");
							// updateFailed(
							// mContext.getString(R.string.notification_update_error_title),
							// mContext.getString(R.string.notification_update_error3));
						} else if (e instanceof SocketTimeoutException) {
							downLoadStateChanged(dlFile,
									UPDATE_STATE_UPDATE_ERROR, null);
							ELog.i(TAG, "SOKET网络问题..");
							// updateFailed(
							// mContext.getString(R.string.notification_update_error_title),
							// mContext.getString(R.string.notification_update_error4));
						} else {
							downLoadStateChanged(dlFile,
									UPDATE_STATE_UPDATE_ERROR, null);
							ELog.i(TAG, "其他错误..");
							// updateFailed(
							// mContext.getString(R.string.notification_update_error_title),
							// mContext.getString(R.string.notification_update_error6));
						}
					}
				} finally {
					try {
						if (is != null) {
							is.close();
							is = null;
						}
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			}
		}
	}
}
