package com.myanycamm.cam;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.myanycam.bean.CameraListInfo;
import com.myanycam.bean.MainSocket;
import com.myanycam.net.NetworkManager;
import com.myanycam.net.SocketFunction;
import com.myanycamm.process.HeartBeatThread;
import com.myanycamm.process.ReceiveThread;
import com.myanycamm.ui.DialogFactory;
import com.myanycamm.update.UpdateSoft;
import com.myanycamm.utils.Constants;
import com.myanycamm.utils.ELog;

import java.io.IOException;

import io.fabric.sdk.android.Fabric;

public class WelcomeActivity extends BaseActivity {
	private static String TAG = "WelcomeActivity";
	private LinearLayout image;
	private static final int SPLASH_DISPLAY_LENGHT = 5000;
	private Dialog mDialog = null;
	public static final int NOUSER = 0;
	// public static final int HANDERVERIFYFALSE = 5;
	private static final int NONET = 100;
	private final int LOGINSUCESS = 15;
	private final int USERNAMEERROR = 1;
	public static final int SHOWRETRY = 101;
	private final int NOHEARTBEAT = 111;
	private final int STARTHEAR = 110;
	private final int UPDATESOFT = 123;// 欢迎界面和主界面都要用，不改
	private int timeOutTimes = 0;// 超时次数
	private NetworkManager netManager;
	Intent intentService;
	AlertDialog.Builder builderRetry;
	AppServer mAppServer;
	DialogFactory d = new DialogFactory();

	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case NOUSER:
				ELog.i(TAG, "没有得到保存的用户");
				Intent intentHome = new Intent(WelcomeActivity.this,
						LoginActivity.class);
//				intentHome.putExtra("login", true);
//				Intent intentHome = new Intent(WelcomeActivity.this,
//						LoginActivity.class);
				startActivityForResult(intentHome, 0);
				break;
			// case HANDERVERIFYFALSE:
			//
			// isAP();
			// break;
			case LOGINSUCESS:
				ELog.i(TAG, "登录成功");
				Intent intent = new Intent(WelcomeActivity.this,
						LoginActivity.class);
				intent.putExtra("istoMain", true);
				startActivityForResult(intent, 0);
				break;
			case USERNAMEERROR:
				ELog.i(TAG, "登录失败");
				Toast.makeText(WelcomeActivity.this, R.string.login_failed,
						Toast.LENGTH_SHORT).show();
				Intent intent1 = new Intent(WelcomeActivity.this,
						LoginActivity.class);
				startActivityForResult(intent1, 0);
				break;
			case NONET:
				// Toast.makeText(WelcomeActivity.this,
				// getString(R.string.no_net), Toast.LENGTH_SHORT).show();
				ELog.i(TAG, "收到没有网络..");
				showNoNetDialog();
				// Intent netIntent = new Intent(
				// android.provider.Settings.ACTION_SETTINGS);
				// startActivityForResult(netIntent, 0);
				break;
			case SHOWRETRY:
				showRetryDialog();
				break;
			case UPDATESOFT:
				ELog.i(TAG, "需要更新..");
				UpdateSoft mSoft = new UpdateSoft(WelcomeActivity.this);
				mSoft.update(false);
				break;
			default:
				break;
			}
		};
	};

	private final BroadcastReceiver homePressReceiver = new BroadcastReceiver() {
		final String SYSTEM_DIALOG_REASON_KEY = "reason";
		final String SYSTEM_DIALOG_REASON_HOME_KEY = "homekey";

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
				String reason = intent.getStringExtra(SYSTEM_DIALOG_REASON_KEY);
				if (reason != null
						&& reason.equals(SYSTEM_DIALOG_REASON_HOME_KEY)) {
					// 自己随意控制程序，关闭...
					ELog.i(TAG, "按了home键.....");
					//
					AppServer.stop = true;
					AppServer.heartCount = 0;
					HeartBeatThread.getInstance().closeThread();
					stopService(intentService);
					intentService = null;
					MainSocket.closeMain();

					onDestroy();
				}
			}
		}
	};

	public void onBackPressed() {
		intentService = null;
		super.onBackPressed();
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Fabric.with(this, new Crashlytics());
		setContentView(R.layout.activity_welcome);	
		ELog.i(TAG, "socket:" + MainSocket.getInstance());
		Animation hyperspaceJumpAnimation = AnimationUtils.loadAnimation(WelcomeActivity.this,
				R.anim.loading_animation);
		ImageView animalImageView = (ImageView) findViewById(R.id.animal_imageView);
		animalImageView.setAnimation(hyperspaceJumpAnimation);


	}

	@Override
	protected void onPause() {
		ELog.i(TAG, "到了暂停....");
		if (homePressReceiver != null) {
			try {
				unregisterReceiver(homePressReceiver);
			} catch (Exception e) {
			}
		}
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		ELog.i(TAG, "welcomeActivity...onResume" + getIntent());
		AppServer.isBackgroud = false;
		final IntentFilter homeFilter = new IntentFilter(
				Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
		registerReceiver(homePressReceiver, homeFilter);
		SocketFunction.getInstance().setmHandler(mHandler);
		CameraListInfo.cams.clear();// 全清，重来
		isAP();
	}

	@Override
	public void finish() {
		ELog.i(TAG, "退出了");
		AppServer.isBackgroud = true;
		if (AppServer.isAp) {
			stopSoft();
			super.finish();
		}else {
			// 不是真的退出
//			Intent intent = new Intent(Intent.ACTION_MAIN);
//			intent.addCategory(Intent.CATEGORY_HOME);
//			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//			startActivity(intent);
			super.finish();
		}
		// super.finish();
	}

	// @Override
	// protected void onActivityResult(int requestCode, int resultCode, Intent
	// data) {
	//
	//
	//
	//
	//
	//
	// finish();
	//
	// }

	public void isAP() {
		ELog.i(TAG, "准备设置");
		netManager = new NetworkManager(WelcomeActivity.this);
		intentService = new Intent(this,AppServer.class);
		stopSoft();
		// initService();
		new Thread(new Runnable() {

			@Override
			public void run() {			
				try {
					if (!netManager.isNetworkConnected()) {// 既没连上wifi,也没连上3G，退出
						ELog.i(TAG, "没有网络..");
						mHandler.sendEmptyMessage(NONET);
						return;
					}
					ELog.i(TAG, "ssid:" + netManager.getWifiSSid());
					if (netManager.isWifiConnected()) {
						if (!netManager.getWifiSSid().toLowerCase()
								.contains("myanycam_")) {
							ELog.i(TAG, "不是myanycam wifi");
							connectSeve();
							return;
						} else if (SocketFunction.getInstance().changeSocket(Constants.LOCALSOCKETIP,
								Constants.LOCALSOCKETIPPORT, 2000)) {
							AppServer.isAp = true;
							initService(STARTHEAR);
							toLocalSettingActivity();
						}else{
							connectSeve();
						}
					} else {
						connectSeve();
					}

				} catch (IOException e) {
					ELog.i(TAG, "没有连接上摄像头" + e.getMessage());
					connectSeve();
					e.printStackTrace();
				}
			}
		}).start();

	}

	
	public void initService(int flags) {
		ELog.i(TAG, "初始化service..");

		if (intentService != null) {
			intentService.setFlags(flags);
			SocketFunction.getInstance().startService(intentService);
		}

	}

	private void connectSeve() {

		initService(NOHEARTBEAT);
		ELog.i(TAG, "连接服务器");
//		Constants.SOCKETIP = SocketFunction.getInstance().getIPByDomain();
		SocketFunction.getInstance().getSerIP();
	}

	private void showRetryDialog() {
	
		if (AppServer.isBackgroud || builderRetry != null) {
			return;
		}

		builderRetry = DialogFactory.creatReTryDialog(WelcomeActivity.this,
				getResources().getString(R.string.net_error));

		builderRetry.setPositiveButton(
				getResources().getString(R.string.retry),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						isAP();
						builderRetry = null;
					}
				});
		builderRetry.setNegativeButton(getResources().getString(R.string.exit),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						builderRetry = null;
						finish();
					}
				});
		builderRetry.create().show();

	}

	private void showNoNetDialog() {
		ELog.i(TAG, "弹出没有网络框..."+AppServer.isBackgroud);
		if (AppServer.isBackgroud || builderRetry != null) {
			return;
		}
		builderRetry = DialogFactory.creatReTryDialog(WelcomeActivity.this,
				getResources().getString(R.string.no_net));
		builderRetry.setNegativeButton(
				getResources().getString(R.string.confirm),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						builderRetry = null;
						finish();
					}
				});
		builderRetry.create().show();
	}

	
	private void stopSoft() {
		ELog.i(TAG, "停止软件...");
		ReceiveThread.getInstance().interrupt();
		AppServer.stop = true;
		AppServer.heartCount = 0;
		HeartBeatThread.getInstance().closeThread();
		stopService(intentService);
		// MyTimerTask.getInstance().closeTimer();
		MainSocket.closeMain();
	}

	private void toLocalSettingActivity() {
		Intent intent = new Intent(WelcomeActivity.this, CameraCenterActivity.class);
		startActivityForResult(intent, 0);
	}

}
