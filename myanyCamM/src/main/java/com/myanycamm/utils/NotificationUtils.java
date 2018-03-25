package com.myanycamm.utils;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.myanycamm.cam.R;
import com.myanycamm.cam.WelcomeActivity;


public class NotificationUtils {

	public static final int NOTIFICATION_ID_UPDATE=100;
	public static final String START_BY_NOTIFICATION="START_BY_NOTIFICATION";
	
	public static void clearNotification(Context mContext) {
		NotificationManager notifyManager = (NotificationManager) mContext.getSystemService(Activity.NOTIFICATION_SERVICE);
		notifyManager.cancel(NOTIFICATION_ID_UPDATE);
	}
	
	public static void showUpdateNotication(Context context,String tip,String content,boolean isClear,Class intentClass){
		clearNotification(context);
		NotificationManager notifyManager = (NotificationManager) context.getSystemService(Activity.NOTIFICATION_SERVICE);
		Notification notification = new Notification(
				R.drawable.notifi,tip, System
						.currentTimeMillis());
		notification.defaults = Notification.DEFAULT_SOUND;
		if(!isClear){
			// 把通知放置在"正在运行"栏目中
			notification.flags |= Notification.FLAG_ONGOING_EVENT;
		}


		Intent intent = new Intent();
//		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK); 
//		intent.setAction(System.currentTimeMillis()+"");
//		Bundle bundle=new Bundle();
//		bundle.putBoolean(START_BY_NOTIFICATION,true);
//		intent.putExtras(bundle);

		PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
				intent, PendingIntent.FLAG_UPDATE_CURRENT);
		// must set this for content view, or will throw a exception


		notification.tickerText = content;
		notification.contentIntent = contentIntent;

		notification.flags = Notification.FLAG_AUTO_CANCEL;
		notifyManager.notify(NOTIFICATION_ID_UPDATE, notification);
	}
	
	
	public static void updateNotication(Notification notification,Context context,String tip,String content){
		if(notification==null){
			NotificationManager notifyManager = (NotificationManager) context.getSystemService(Activity.NOTIFICATION_SERVICE);
			notification = new Notification(
					R.drawable.notifi,tip, System
							.currentTimeMillis());
			// 把通知放置在"正在运行"栏目中
			notification.flags |= Notification.FLAG_ONGOING_EVENT;
		}
		
		NotificationManager notifyManager = (NotificationManager) context.getSystemService(Activity.NOTIFICATION_SERVICE);
		Intent intent = new Intent(context, WelcomeActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK); 
		intent.setAction(System.currentTimeMillis()+"");
		Bundle bundle=new Bundle();
		bundle.putBoolean(START_BY_NOTIFICATION,true);
		intent.putExtras(bundle);
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
				intent, PendingIntent.FLAG_UPDATE_CURRENT);
		// must set this for content view, or will throw a exception


		notification.tickerText = content;
		notification.contentIntent = contentIntent;
		notifyManager.notify(NOTIFICATION_ID_UPDATE, notification);
	}

}
