package com.myanycamm.update;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StatFs;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.myanycam.net.SocketFunction;
import com.myanycam.update.net.NetWorkManager;
import com.myanycamm.cam.R;
import com.myanycamm.cam.WelcomeActivity;
import com.myanycamm.process.ScreenManager;
import com.myanycamm.utils.Constants;
import com.myanycamm.utils.ELog;
import com.myanycamm.utils.NotificationUtils;
import com.myanycamm.utils.Utils;


public class UpdateManager {
	private static final String TAG = "UpdateManager";
	public static final int MSG_SHOW_UPDATE_DIALOG = 57575;
	public static final int MSG_REQUEST_UPDATE_TASK = 57576;
	
	private static UpdateManager instance;

	
	public static final byte UPDATE_STATE_CHECK = 0;
	
	public static final byte UPDATE_STATE_UPDATING = 1;
	
	public static final byte UPDATE_STATE_UPDATE_ERROR = 2;
	
	public static final byte UPDATE_STATE_UPDATE_SUCCESS = 3;

	
	private byte state;
	
	private long downFileMaxLen;
	
	private long downFileCurrLen;

	
	private UpdateInfo updateInfo;
	
	private boolean isStartUpdate = false;
	
	private Context mContext;
	
	private DownLoadFile dlFile;
	
	private ArrayList<UpdateProgressListener> listeners;
	
	private String UPDATE_FILE_NAME = "update_data";
	
	private UpdateListener listener;
	
	private String savePath;

	private int downloadPercent = 0;

	private UpdateManager(Context mContext) {
		this.mContext = mContext;
		listeners = new ArrayList<UpdateProgressListener>();
		if (!isStartUpdate) {
			NotificationUtils.clearNotification(mContext);
		}
	}

	
	public static UpdateManager getInstance(Context mContext) {
		if (instance == null) {
			instance = new UpdateManager(mContext);
		}
		return instance;
	}

	private final int MSG_UPDATE_DOWNLOAD_PERCENT = 301;
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case MSG_UPDATE_DOWNLOAD_PERCENT:
				int percent = (Integer) msg.obj;
				ELog.w(TAG, "percent:" + percent);
				NotificationUtils.updateNotication(null, mContext,
						mContext.getString(R.string.notification_updating),
						percent + "%");
				break;

			default:
				break;
			}
		}

	};

	
	public void retry(String url, UpdateProgressListener listener) {
		isStartUpdate = false;
		try {
			downloadApkFile(url, listener);
		} catch (Exception e) {
			showFailedActivity(e.getMessage());
			e.printStackTrace();
		}
	}

	public UpdateListener getListener() {
		return listener;
	}

	
	public void setListener(UpdateListener listener) {
		this.listener = listener;
	}

	
	public void start(final Activity activity, final UpdateListener listener) {
		ELog.w(TAG, "to get update info....isStartUpdate=" + isStartUpdate);
		if (isStartUpdate) {
			return;
		}
		NotificationUtils.clearNotification(activity);
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				// String url = mergeUrlWithPhoneInfoAndUid(activity, uid);
				String url = Constants.updateUrl;
				checkUpdate(listener, url);
			}
		}).start();
	}

	
	public static String getApkPath(Context context) {
		PackageManager pm = context.getPackageManager();
		ApplicationInfo appInf = null;
		try {
			appInf = pm.getApplicationInfo(context.getPackageName(), 0);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		String path = "";
		if (appInf != null && appInf.sourceDir != null) {
			path = appInf.sourceDir.toString();
		}
		ELog.i("APK", "path:" + path);
		return path;
	}

	
	public void checkUpdate(final UpdateListener listener, String url) {
		setState(UPDATE_STATE_CHECK);
		int resultStatus = NetWorkManager.NET_UNKNOW_ERROR;
		NetWorkManager netManager = new NetWorkManager();
		HttpEntity data = netManager.sendGetRequestAndWaitHttpEntityResponse(
				mContext, url);

		if (data != null) {
			resultStatus = NetWorkManager.NET_SUCCESS;
		} else {
			if (NetWorkManager.SSL_ERROR == netManager.getErrorType()) {
				resultStatus = NetWorkManager.SSL_ERROR;
			} else if (NetWorkManager.IO_ERROR == netManager.getErrorType()) {
				resultStatus = NetWorkManager.IO_ERROR;
			}
		}
		ELog.w(TAG, "checkUpdate data=" + data + " resultStatus="
				+ resultStatus);
		UpdateInfo updateInfo = null;
		InputStream is = null;
		if (resultStatus == NetWorkManager.NET_SUCCESS) {
			XmlPullParserFactory factory;
			try {
				factory = XmlPullParserFactory.newInstance();
				factory.setNamespaceAware(true);
				XmlPullParser parser = factory.newPullParser();
				is = data.getContent();
				parser.setInput(new InputStreamReader(is));
				updateInfo = UpdateInfo.parser(parser);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		this.updateInfo = updateInfo;
		if (listener != null) {
			listener.updateResult(updateInfo);
		}
	}

	
	public synchronized void downloadApkFile(String url) throws Exception {
		downloadApkFile(url, null);
	}

	
	public synchronized void downloadApkFile(String url,
			UpdateProgressListener listener) throws Exception {
		addUpdateProgressListener(listener);
		if (isStartUpdate) {
			return;
		}
		// 检查是否有存储卡，没有则不下载并提示
		if (!externalMemoryAvailable()) {
			throw new Exception(mContext.getString(R.string.tip_no_sdcard));
		}
		// 判断下载地址是否为空，为空则不下载并提示
		if (url == null) {
			throw new Exception(mContext.getString(R.string.tip_url_illegal));
		}
		ELog.v(TAG, "addDownLoadFile url=" + url);

		// 判断网络是否连接正常，没有连接则不下载并提示
		if (!Utils.isNetworkAvailable(mContext)) {
			throw new Exception(
					mContext.getString(R.string.newwork_unavailable));
		}

		// 形成下载文件，设定下载文件名
		dlFile = new DownLoadFile(url, null, UPDATE_FILE_NAME);

		new Thread(new DownApkRunnable(dlFile)).start();
	}

	public static void showToast(Activity activity, final String tip) {
		if (activity == null) {
			return;
		}
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(SocketFunction.getAppContext(), tip,
						Toast.LENGTH_LONG).show();
			}
		});
	}

	
	class DownApkRunnable implements Runnable {
		private DownLoadFile dlFile;

		public DownApkRunnable(DownLoadFile dlFile) {
			this.dlFile = dlFile;
		}

		@Override
		public synchronized void run() {
			// 开始下载前先提示
			showToast(
					ScreenManager.getScreenManager().currentActivity(),
					SocketFunction.getAppContext().getString(
							R.string.tip_update_begin));

			isStartUpdate = true;

			// 初始化下载路径，确保下载路径存在并可用
			savePath = getSavePath() + "update/";
			setState(UPDATE_STATE_UPDATING);
			ELog.d(TAG, "start to download update info...");
			FileManager.deleteDirectory(savePath);
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
				updateFailed(
						mContext.getString(R.string.notification_update_error_title),
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
							.sendGetRequestAndWaitHttpEntityResponse(mContext,
									dlFile.getUrl(), mHeaders);
					if (response == null) {
						// throw new Exception();
						downLoadStateChanged(dlFile, UPDATE_STATE_UPDATE_ERROR,
								null);
						updateFailed(
								mContext.getString(R.string.notification_update_error_title),
								mContext.getString(R.string.notification_update_error4));
						return;
					}
					int respCode = response.getStatusLine().getStatusCode();
					ELog.i("升级下载，respCode:" + respCode);
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
						downLoadStateChanged(dlFile, UPDATE_STATE_UPDATE_ERROR,
								null);
						updateFailed(
								mContext.getString(R.string.notification_update_error_title),
								mContext.getString(R.string.notification_update_error2));
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
					ELog.v(TAG, dlFile.getTempFileName() + "  rename to :"
							+ dlFile.getFileName());
					file.renameTo(new File(savePath + dlFile.getFileName()));
					downLoadFileCompleted(dlFile);
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
						updateFailed(
								mContext.getString(R.string.notification_update_error_title),
								mContext.getString(R.string.notification_update_error5));
						break;
					} else if (getTotalExternalMemorySize() <= 0) {
						downLoadStateChanged(dlFile, UPDATE_STATE_UPDATE_ERROR,
								null);
						updateFailed(
								mContext.getString(R.string.notification_update_error_title),
								mContext.getString(R.string.notification_update_error2));
						break;
					} else if (i >= retryCount) {
						if (!Utils.isNetworkAvailable(mContext)) {
							downLoadStateChanged(dlFile,
									UPDATE_STATE_UPDATE_ERROR, null);
							updateFailed(
									mContext.getString(R.string.notification_update_error_title),
									mContext.getString(R.string.notification_update_error3));
						} else if (e instanceof SocketTimeoutException) {
							downLoadStateChanged(dlFile,
									UPDATE_STATE_UPDATE_ERROR, null);
							updateFailed(
									mContext.getString(R.string.notification_update_error_title),
									mContext.getString(R.string.notification_update_error4));
						} else {
							downLoadStateChanged(dlFile,
									UPDATE_STATE_UPDATE_ERROR, null);
							updateFailed(
									mContext.getString(R.string.notification_update_error_title),
									mContext.getString(R.string.notification_update_error6));
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

	private void updateFailed(String tip, String content) {
		isStartUpdate = false;
		setState(UPDATE_STATE_UPDATE_ERROR);
		dlFile.setState(DownLoadFileListener.STATE_FAILED);
		NotificationUtils.showUpdateNotication(mContext, tip, content, true,WelcomeActivity.class);
		stopUpdateService();
		if (content != null) {
			tip += "," + content;
		}
		showFailedActivity(tip);
	}

	public void showFailedActivity(String tip) {
		if (updateInfo != null
				&& updateInfo.getUpdateType() == UpdateInfo.UPDATE_TYPE_ENFORE) {
			if (ScreenManager.getScreenManager().currentActivity() == null) {
				return;
			}
			if (tip == null || tip.length() <= 0) {
				tip = SocketFunction.getAppContext().getString(
						R.string.notification_update_error);
			}
			final String tempTip = tip;
			if (ScreenManager.getScreenManager().currentActivity() == null) {
				return;
			}
			ScreenManager.getScreenManager().currentActivity()
					.runOnUiThread(new Runnable() {

						@Override
						public void run() {
							TextView textView = (TextView) LayoutInflater.from(
									SocketFunction.getAppContext()).inflate(
									R.layout.update_tip_dialog_view, null);
							textView.setText(tempTip);
							new AlertDialog.Builder(ScreenManager
									.getScreenManager().currentActivity())
									.setView(textView)
									.setTitle(
											updateInfo.getUpdateTitle() == null ? SocketFunction
													.getAppContext()
													.getString(
															R.string.update_title)
													: updateInfo
															.getUpdateTitle())
									.setPositiveButton(
											SocketFunction.getAppContext()
													.getString(
															R.string.btn_retry),
											new DialogInterface.OnClickListener() {
												@Override
												public void onClick(
														DialogInterface dialog,
														int which) {
													startUpdateService(updateInfo);
												}
											})
									.setNegativeButton(
											SocketFunction.getAppContext()
													.getString(
															R.string.btn_quit),
											new DialogInterface.OnClickListener() {
												@Override
												public void onClick(
														DialogInterface dialog,
														int which) {
													dialog.dismiss();
													ScreenManager
															.getScreenManager()
															.popAllActivity();
												}
											}).setOnKeyListener(keyListener)
									.setCancelable(false).create().show();
						}
					});

		}
	}

	private void stopUpdateService() {
		Intent intent = new Intent();
		intent.setClass(mContext, UpdateService.class);
		mContext.stopService(intent);
	}

	private Object object = new Object();

	private void downLoadFileCompleted(DownLoadFile dlFile) {
		ELog.w(TAG, "downLoadFileCompleted...");
		NotificationUtils.showUpdateNotication(mContext,
				mContext.getString(R.string.notification_update_success_title),
				mContext.getString(R.string.notification_update_success), true,WelcomeActivity.class);
		installApk(dlFile);
	}

	public void installApk() {
		installApk(dlFile);
	}

	private void installApk(DownLoadFile dlFile) {
		if (dlFile != null) {
			ELog.i(TAG, "installApk:" + savePath + dlFile.getFileName());
			synchronized (object) {
				openInstallApkFile(mContext,
						new File(savePath + dlFile.getFileName()));
				if (updateInfo.getUpdateType() == UpdateInfo.UPDATE_TYPE_ENFORE) {
					NotificationUtils.clearNotification(mContext);
					reset();
					ScreenManager.getScreenManager().popAllActivity();
				}
			}
		}
	}

	public UpdateInfo getUpdateInfo() {
		return updateInfo;
	}

	public void setUpdateInfo(UpdateInfo updateInfo) {
		this.updateInfo = updateInfo;
	}

	public boolean isStartUpdate() {
		return isStartUpdate;
	}

	public void setStartUpdate(boolean isStartUpdate) {
		this.isStartUpdate = isStartUpdate;
	}

	public byte getState() {
		return state;
	}

	public void setState(byte state) {
		this.state = state;
	}

	public void reset() {
		isStartUpdate = false;
		setState(UPDATE_STATE_CHECK);
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

	public void downLoadStateChanged(DownLoadFile df, byte state, Object object) {
		this.state = state;
		for (UpdateProgressListener listener : listeners) {
			listener.downLoadStateChanged(df, state, object);
		}
	}

	public void downLoadProgressChanged(DownLoadFile df, long maxLen,
			long currLen) {
		downFileMaxLen = maxLen;
		downFileCurrLen = currLen;
		int percent = 0;
		if (downFileMaxLen != 0) {
			percent = (int) (downFileCurrLen * 100 / downFileMaxLen);
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

	
	public void showUpdateDialog(final UpdateInfo updateInfo,
			final boolean isManual) {
		final SharedPreferences sp = mContext.getSharedPreferences("SP",
				Context.MODE_PRIVATE);
		if (updateInfo == null) {
			return;
		}
		View view = (View) LayoutInflater.from(SocketFunction.getAppContext())
				.inflate(R.layout.update_dialog_view, null);
		TextView textView = (TextView) view.findViewById(R.id.textview);
		textView.setText(updateInfo.getUpdateTip() == null ? SocketFunction
				.getAppContext().getString(R.string.default_update_content)
				: updateInfo.getUpdateTip());
		CheckBox checkBox = (CheckBox) view.findViewById(R.id.checkbox);
		checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				ELog.i(TAG,"..........isChecked=" + isChecked+"verion:"+updateInfo.getVersion());
				if (isChecked) {		
					sp.edit().putBoolean(updateInfo.getVersion(), true).commit();//是否忽略
				} else {
				
					sp.edit().putBoolean(updateInfo.getVersion(), false).commit();//是否忽略
				}
				ELog.i(TAG, "成功保存:"+sp.contains("1.0.3"));
			}
		});
//
//		boolean isShowFromLocal = UserInfo.getIntance().loadRemindUpdate(
//				mContext, updateInfo.getUpdateUrl());// 从本地保存的数据中读取是否要显示不再提示的框

//		
//		if (!isManual
//				&& updateInfo.getUpdateType() == UpdateInfo.UPDATE_TYPE_TIP) {
//			ELog.e(TAG, "不显示升级框噢.....");
//			return;
//		}

		
		if (!isManual
				&& updateInfo.getUpdateType() == UpdateInfo.UPDATE_TYPE_TIP
				&& updateInfo.isShowSelect) {
			checkBox.setVisibility(View.VISIBLE);
		} else {
			checkBox.setVisibility(View.GONE);
		}
		ELog.i(TAG, "UpdateType:" + updateInfo.getUpdateType());
		ELog.i(TAG, "提示升级:"
				+ ScreenManager.getScreenManager().currentActivity());
		ELog.i(TAG, "updateTip:" + updateInfo.getUpdateTip());

		try {
			if (updateInfo.getUpdateType() == UpdateInfo.UPDATE_TYPE_TIP) {
				ELog.i(TAG, "提示升级:"
						+ ScreenManager.getScreenManager().currentActivity());
				new AlertDialog.Builder(ScreenManager.getScreenManager()
						.currentActivity())
						.setView(view)
						.setTitle(
								updateInfo.getUpdateTitle() == null ? SocketFunction
										.getAppContext().getString(
												R.string.update_title)
										: updateInfo.getUpdateTitle())
						.setPositiveButton(
								SocketFunction.getAppContext().getString(
										R.string.btn_update_ok),
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										ELog.i(TAG, "是否手动:" + isManual);
										if (!isManual) {// 如果不是手动，就登录
											SocketFunction.getInstance()
													.getmHandler()
													.sendEmptyMessage(15);// 欢迎界面和登录界面都用,不能改
											SocketFunction.getInstance()
													.getMcu();
											SocketFunction.getInstance()
													.downloadCamera();
										}

										startUpdateService(updateInfo);
									}
								})
						.setNegativeButton(
								SocketFunction.getAppContext().getString(
										R.string.btn_cancel),
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										dialog.dismiss();
										if (!isManual) {// 如果不是手动，就登录
											SocketFunction.getInstance()
													.getmHandler()
													.sendEmptyMessage(15);// 欢迎界面和登录界面都用,不能改
											SocketFunction.getInstance()
													.getMcu();
											SocketFunction.getInstance()
													.downloadCamera();
										}
									}
								}).setCancelable(false).create().show();
			} else if (updateInfo.getUpdateType() == UpdateInfo.UPDATE_TYPE_ENFORE) {
				new AlertDialog.Builder(ScreenManager.getScreenManager()
						.currentActivity())
						.setView(view)
						.setTitle(
								updateInfo.getUpdateTitle() == null ? SocketFunction
										.getAppContext().getString(
												R.string.update_title)
										: updateInfo.getUpdateTitle())
						.setPositiveButton(
								SocketFunction.getAppContext().getString(
										R.string.btn_update_ok),
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										startUpdateService(updateInfo);
									}
								}).setOnKeyListener(keyListener)
						.setCancelable(false).create().show();
			}
		} catch (Exception e) {
			ELog.i(TAG, "升级有错:" + e);
		}
	}

	private OnKeyListener keyListener = new OnKeyListener() {
		@Override
		public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
			if (keyCode == KeyEvent.KEYCODE_SEARCH) {
				return true;
			}
			return false;
		}
	};

	private void startUpdateService(UpdateInfo updateInfo) {
		Intent intent = new Intent();
	
		try {			
			intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.myanycamm.cam"));
			intent.setClassName("com.android.vending", "com.android.vending.AssetBrowserActivity");
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			ScreenManager.getScreenManager().currentActivity().startActivity(intent);
		} catch (ActivityNotFoundException e) {
			intent.setClass(SocketFunction.getAppContext(), UpdateService.class);
			intent.putExtra("URL", updateInfo.getUpdateUrl());
			SocketFunction.getAppContext().startService(intent);
		}

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

	
	public static boolean externalMemoryAvailable() {
		if (getExternalStoragePath() != null) {
			return true;
		}
		return false;
	}

	public static String getSavePath() {
		return getExternalStoragePath() + "/myanycam/update/";
	}

	
	public static void openInstallApkFile(Context activity, File file) {
		ELog.e(TAG, "openInstallApkFile," + file.getName());
		Intent intent = new Intent();
		// Intent respondIntent = new Intent(activity, activity.getClass());
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.setAction(android.content.Intent.ACTION_VIEW);
		intent.setDataAndType(Uri.fromFile(file),
				"application/vnd.android.package-archive");
		activity.startActivity(intent);
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
}
