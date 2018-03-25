package com.myanycamm.ui;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.TextView;

import com.myanycam.bean.ActionItem;
import com.myanycamm.cam.R;
import com.myanycamm.utils.ELog;


public class QuickAction extends PopupWindows implements OnDismissListener {

	private static String TAG = "QuickAction";
	private LayoutInflater inflater;
	private ViewGroup mTrack;
	private OnActionItemClickListener mItemClickListener;
	private OnDismissListener mDismissListener;

	private List<ActionItem> mActionItemList = new ArrayList<ActionItem>();

	private boolean mDidAction;

	private int mChildPos;

	public static final int ANIM_GROW_FROM_LEFT = 1;
	public static final int ANIM_GROW_FROM_RIGHT = 2;
	public static final int ANIM_GROW_FROM_CENTER = 3;
	public static final int ANIM_AUTO = 4;

	
	public QuickAction(Context context) {
		super(context);

		inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		setRootViewId(R.layout.quickaction);

		mChildPos = 0;
	}

	
	public ActionItem getActionItem(int index) {
		return mActionItemList.get(index);
	}

	
	public void setRootViewId(int id) {
		mRootView = (ViewGroup) inflater.inflate(id, null);
		mTrack = (ViewGroup) mRootView.findViewById(R.id.tracks);

		mRootView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT));

		setContentView(mRootView);
	}

	
	public void addActionItem(ActionItem action, boolean isSelect) {
		mActionItemList.add(action);
		String title = action.getTitle();

		final View container = (View) inflater.inflate(R.layout.action_item,
				null);
		final TextView text = (TextView) container.findViewById(R.id.tv_title);

		if (title != null) {
			text.setText(title);
		} else {
			text.setVisibility(View.GONE);
		}

		final int pos = mChildPos;
		final int actionId = action.getActionId();

		container.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mItemClickListener != null) {
					updateItemBg(pos);
					mItemClickListener.onItemClick(QuickAction.this, pos,
							actionId, container);
				}

				if (!getActionItem(pos).isSticky()) {
					mDidAction = true;

					// workaround for transparent background bug
					// thx to Roman Wozniak <roman.wozniak@gmail.com>
					v.post(new Runnable() {
						@Override
						public void run() {
							dismiss();
						}
					});
				}
			}
		});

		container.setFocusable(true);
		container.setClickable(true);

		// if (mChildPos != 0) {
		container.setBackgroundResource(R.drawable.pop_middle_p);
		if (isSelect) {
			ELog.i(TAG, "当前选择的:" + pos + " actionTitle" + title);
			// updateItemBg(mChildPos);
			// mTrack.getChildAt(mChildPos).findViewById(R.id.select_bg)
			// .setBackgroundResource(R.drawable.pop_selected_p);
			container.findViewById(R.id.select_bg).setBackgroundResource(
					R.drawable.pop_selected_p);
			((TextView) container.findViewById(R.id.tv_title)).setTextColor(Color.GREEN);
			// mTrack.getChildAt(mChildPos).setBackgroundResource(
			// R.drawable.pop_selected_p);
		}
		//
		// }

		mTrack.addView(container, mChildPos + 1);
		mChildPos++;
	}

	
	public void updateItemBg(int position) {
		for (int i = 1; i < mTrack.getChildCount(); i++) {
			if (i == position + 1) {
				mTrack.getChildAt(i).findViewById(R.id.select_bg)
						.setBackgroundResource(R.drawable.pop_selected_p);
				((TextView) mTrack.getChildAt(i).findViewById(R.id.tv_title))
						.setTextColor(Color.GREEN);
			} else {
				mTrack.getChildAt(i).findViewById(R.id.select_bg)
						.setBackgroundDrawable(null);
				((TextView) mTrack.getChildAt(i).findViewById(R.id.tv_title))
						.setTextColor(Color.WHITE);
			}

		}
		ELog.i(TAG, "点击:" + position);

	}

	public void setOnActionItemClickListener(OnActionItemClickListener listener) {
		mItemClickListener = listener;
	}

	
	public void show(View anchor) {
		preShow();

		int[] location = new int[2];

		mDidAction = false;

		anchor.getLocationOnScreen(location);

		Rect anchorRect = new Rect(location[0], location[1], location[0]
				+ anchor.getWidth(), location[1] + anchor.getHeight());

		// mRootView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
		// LayoutParams.WRAP_CONTENT));
		mRootView.measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

		int rootWidth = mRootView.getMeasuredWidth();
		int rootHeight = mRootView.getMeasuredHeight();

		int screenWidth = mWindowManager.getDefaultDisplay().getWidth();
		// int screenHeight = mWindowManager.getDefaultDisplay().getHeight();

		int xPos = screenWidth - rootWidth;
		// int yPos = anchorRect.top - rootHeight;
		// int xPos = (int)anchor.getScrollX();
		int yPos = 110;

		setAnimationStyle(screenWidth, anchorRect.centerX());

		mWindow.showAtLocation(anchor, Gravity.NO_GRAVITY, xPos, yPos);

	}

	
	private void setAnimationStyle(int screenWidth, int requestedX) {

		mWindow.setAnimationStyle(R.style.Animations_PopDownMenu_Center);

	}

	
	public void setOnDismissListener(QuickAction.OnDismissListener listener) {
		setOnDismissListener(this);

		mDismissListener = listener;
	}

	@Override
	public void onDismiss() {
		if (!mDidAction && mDismissListener != null) {
			mDismissListener.onDismiss();
		}
	}

	
	public interface OnActionItemClickListener {
		public abstract void onItemClick(QuickAction source, int pos,
				int actionId, View v);
	}

	
	public interface OnDismissListener {
		public abstract void onDismiss();
	}
}