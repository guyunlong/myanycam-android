package com.myanycamm.ui;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.myanycam.net.SocketFunction;
import com.myanycamm.cam.AppServer;
import com.myanycamm.cam.R;
import com.myanycamm.cam.WelcomeActivity;
import com.myanycamm.process.ScreenManager;
import com.myanycamm.utils.ELog;

public class DialogFactory {
	private final static String TAG = "DialogFactory";
	public static final int GLOBAL = 0;
	public static final int SOCKETIMEOUT = 1;
	static AlertDialog.Builder gobalBuilder = null;
	static AlertDialog.Builder gobalTimeOutBuilder = null;
	public static Handler dialogHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case GLOBAL:
				ELog.i(TAG, "收到GLOBAL");
				showGlobalDialog();
				break;
			case SOCKETIMEOUT:
				AppServer.stop = true;
				Bundle mBundle = msg.getData();
				if (ScreenManager.getScreenManager().currentActivity().getClass().equals(WelcomeActivity.class)) {
					SocketFunction.getInstance().getmHandler().sendEmptyMessage(101);
				}else{
					showGlobalTimeOutDialog(mBundle.getString("key"));
				}
		
				break;

			default:
				break;
			}
		}
	};



	
	public static Dialog createLoadingDialog(Context context, String msg) {

		LayoutInflater inflater = LayoutInflater.from(context);
		View v = inflater.inflate(R.layout.loading_process_dialog_icon, null);// 得到加载view
		LinearLayout layout = (LinearLayout) v.findViewById(R.id.dialog_view);// 加载布局
		// main.xml中的ImageView
		ImageView spaceshipImage = (ImageView) v.findViewById(R.id.img);
		TextView tipTextView = (TextView) v.findViewById(R.id.tipTextView);// 提示文字
		// 加载动画
		Animation hyperspaceJumpAnimation = AnimationUtils.loadAnimation(
				context, R.anim.loading_animation);
		// 使用ImageView显示动画
		spaceshipImage.startAnimation(hyperspaceJumpAnimation);
		if (msg == null || msg.length() == 0) {
			tipTextView.setText(R.string.sending_request);
		} else {
			tipTextView.setText(msg);
		}
//		tipTextView.setText(msg);// 设置加载信息

		Dialog loadingDialog = new Dialog(context, R.style.loading_dialog);// 创建自定义样式dialog

//		loadingDialog.setCancelable(false);// 不可以用“返回键”取消
		loadingDialog.setContentView(layout, new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.FILL_PARENT,
				LinearLayout.LayoutParams.FILL_PARENT));// 设置布局
		return loadingDialog;

	}

	public static Builder creatReTryDialog(final Context context, String tip) {
		AlertDialog.Builder builder = new Builder(context);
		builder.setMessage(tip);
		builder.setTitle(context.getResources().getString(
				R.string.note));
		builder.setCancelable(false);
		return builder;
	}

	public static void showGlobalDialog() {
		if (AppServer.isBackgroud) {
			return;
		}
		if (gobalBuilder!=null) {
			return;
		}
		AppServer.stop = true;
		ELog.i(TAG, "当前Activity:"+ScreenManager.getScreenManager().currentActivity().getClass().getSimpleName());
		Context context = ScreenManager.getScreenManager().currentActivity()
				.getWindow().getContext();
		gobalBuilder = new Builder(context);
		gobalBuilder.setMessage(context.getResources().getString(
				R.string.other_login));
		gobalBuilder.setTitle(context.getResources().getString(R.string.note));
		gobalBuilder.setCancelable(false);
		gobalBuilder.setPositiveButton(
				context.getResources().getString(R.string.relogin),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						gobalBuilder = null;
						ScreenManager.getScreenManager().toWelcome();
						// login();
						// retryCam();
					}
				});
		gobalBuilder.setNegativeButton(
				context.getResources().getString(R.string.exit),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						gobalBuilder = null;					
						ScreenManager.getScreenManager().popAllActivity();
					}
				});
		gobalBuilder.create().show();
	}
	
	public static void showGlobalTimeOutDialog(String content) {
	
		if (AppServer.isBackgroud) {
			return;
		}
		if (gobalTimeOutBuilder!=null) {
			return;
		}
		try {
			ELog.i(TAG, "当前Activity:"+ScreenManager.getScreenManager().currentActivity().getClass().getSimpleName());
			Context context = ScreenManager.getScreenManager().currentActivity()
					.getWindow().getContext();
			gobalTimeOutBuilder = new Builder(context);
			gobalTimeOutBuilder.setMessage(context.getResources().getString(
					R.string.main_socket_timeout)+content);
			gobalTimeOutBuilder.setTitle(context.getResources().getString(R.string.note));
			gobalTimeOutBuilder.setCancelable(false);
			gobalTimeOutBuilder.setNegativeButton(
					context.getResources().getString(R.string.confirm),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							gobalTimeOutBuilder = null;	
							if (AppServer.isAp) {
								ScreenManager.getScreenManager().popAllActivity();
							}else{
								ScreenManager.getScreenManager().toWelcome();
							}
													
						}
					});
			gobalTimeOutBuilder.create().show();
		} catch (NullPointerException e) {
			ELog.i(TAG, "在后台...空错误");
		}
	
	}


}
