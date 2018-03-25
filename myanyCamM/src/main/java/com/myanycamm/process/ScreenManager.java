package com.myanycamm.process;

import java.util.Stack;

import com.myanycamm.cam.WelcomeActivity;
import com.myanycamm.utils.ELog;

import android.app.Activity;

public class ScreenManager {
	private final String TAG = "ScreenManager";
	private Stack activityStack;
	private static ScreenManager instance;

	private ScreenManager() {
	}

	public static ScreenManager getScreenManager() {
		if (instance == null) {
			instance = new ScreenManager();
		}
		return instance;
	}

	public void toWelcome() {
		popAllActivityExceptOne(WelcomeActivity.class);
		// popAllActivity();
	}

	public void popActivity() {
		//
		if (!activityStack.isEmpty()) {
			activityStack.pop();
		}
		//
	}

	public void popActivity(Activity activity) {
		if (activity != null) {
			activity.finish();
			activityStack.remove(activity);
			activity = null;
		}
	}

	public Activity currentActivity() {
		if (!activityStack.isEmpty()) {
			Activity activity = (Activity) activityStack.lastElement();
			return activity;
		}
		return null;
	}

	public void pushActivity(Activity activity) {
		ELog.i(TAG, "push:" + activity.getClass().getSimpleName());
		if (activityStack == null) {
			activityStack = new Stack();
		}
		if (activityStack.contains(activity)) {
			ELog.i(TAG, "栈中已经有了。。");
			return;
		}
		activityStack.add(activity);
	}

	public void popAllActivityExceptOne(Class cls) {
		while (true) {
			Activity activity = currentActivity();
			if (activity == null) {
				break;
			}
			if (activity.getClass().equals(cls)) {
				break;
			}
			popActivity(activity);
		}

	}

	public void popAllActivity() {
		while (true) {
			Activity activity = currentActivity();
			if (activity == null) {
				break;
			}

			ELog.i(TAG, "弹出" + activity.getClass().getSimpleName());
			if (activity.getClass().equals(WelcomeActivity.class)) {
				activity.finish();
				break;
			} else {
				popActivity(activity);
			}

		}
		System.exit(0);
	}

	public void extit() {
		while (true) {
			Activity activity = currentActivity();
			if (activity == null) {
				System.exit(0);
			} else {
				popActivity(activity);
				activity.finish();
			}

		}
	}
}
