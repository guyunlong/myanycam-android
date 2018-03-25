package com.myanycamm.update;

import org.xmlpull.v1.XmlPullParserFactory;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import com.myanycam.net.SocketFunction;
import com.myanycamm.cam.R;
import com.myanycamm.cam.WelcomeActivity;
import com.myanycamm.process.ScreenManager;
import com.myanycamm.utils.ELog;
import com.myanycamm.utils.NotificationUtils;
import com.myanycamm.utils.Utils;

public class UpdateSoft {
	private static String TAG = "UpdateSoft";
	private Activity mContext;
	private boolean isMannul = false;

	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case UpdateManager.MSG_SHOW_UPDATE_DIALOG:
				ELog.d(TAG, "接收显示更新窗口");
				UpdateManager.getInstance(mContext).showUpdateDialog(
						(UpdateInfo) msg.obj, isMannul);
				break;
			case UpdateManager.MSG_REQUEST_UPDATE_TASK:
				ELog.d(TAG, "接收升级更新任务");
				dealWithInstallApk(mContext.getIntent());
				UpdateManager.getInstance(mContext).start(mContext,
						updateListener);
				break;
			default:
				break;
			}
		};
	};

	public UpdateSoft(Activity context) {
		this.mContext = context;
	}

	public void update(boolean isMannul) {
		this.isMannul = isMannul;
		if (!isMannul) {
			SharedPreferences sp = mContext.getSharedPreferences("SP",
					Context.MODE_PRIVATE);
			if (sp.getString("upgradetype", "0").equals("0")) {
				showToast(mContext.getResources().getString(
						R.string.tip_the_newest_version));
				return;
			}
		}
		XmlPullParserFactory factory;
		try {
			executeUpdateTask();
		} catch (Exception e) {
			ELog.i(TAG, "执行错误");
			e.printStackTrace();
		}

	}

	
	
	

	
	private void executeUpdateTask() {
		ELog.d(TAG, "executeUpdateTask请求升级");
		mHandler.sendEmptyMessageDelayed(UpdateManager.MSG_REQUEST_UPDATE_TASK,
				30);

	}

	public void dealWithInstallApk(Intent intent) {
		ELog.d(TAG, "dealWithInstallApk");
		if (intent != null && intent.getExtras() != null) {
			boolean isStartByNotification = intent.getExtras().getBoolean(
					NotificationUtils.START_BY_NOTIFICATION, false);
			ELog.w(TAG, "isStartByNotification=" + isStartByNotification);
			if (isStartByNotification) {
				UpdateManager manager = UpdateManager.getInstance(mContext);
				ELog.w(TAG, "manager.getState()=" + manager.getState());
				if (manager.getState() == UpdateManager.UPDATE_STATE_UPDATE_SUCCESS) {
					if (!UpdateManager.externalMemoryAvailable()) {
						showToast(mContext
								.getString(R.string.tip_sdcard_unavailable));
					} else {
						manager.installApk();
						NotificationUtils.clearNotification(mContext);
						manager.reset();
					}
				} else if (manager.getState() == UpdateManager.UPDATE_STATE_UPDATE_ERROR) {
					NotificationUtils.clearNotification(mContext);
					manager.reset();
				}
			}
		}
		{
			ELog.d(TAG, "intent is null");
		}
	}

	public UpdateListener updateListener = new UpdateListener() {

		@Override
		public void updateResult(UpdateInfo updateInfo) {

			if (updateInfo != null) {
				ELog.w("UpdateManager",
						"updateListener ..." + updateInfo.toString());
			}
			if (updateInfo != null
					&& updateInfo.getUpdateType() != UpdateInfo.UPDATE_TYPE_NOINFO) {
				mHandler.sendMessage(mHandler.obtainMessage(
						UpdateManager.MSG_SHOW_UPDATE_DIALOG, updateInfo));
			} else {
				showToast("version:"+Utils.getAppVersionName(mContext)+" "+mContext.getResources().getString(
						R.string.tip_the_newest_version));
				//
				 if (ScreenManager.getScreenManager().currentActivity().getClass().equals(WelcomeActivity.class)) {
						SocketFunction.getInstance().getmHandler().sendEmptyMessage(15);// 欢迎界面和登录界面都用,不能改
						SocketFunction.getInstance().getMcu();
						SocketFunction.getInstance().downloadCamera();
				}

			}

		}

	};

	private void showToast(final String tip) {
		mContext.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(mContext, tip, Toast.LENGTH_LONG).show();
			}
		});
	}
}
