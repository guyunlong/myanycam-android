
package com.myanycamm.ui;

import com.myanycamm.utils.Configure;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;

public class DragListView extends ListView {
	private final String TAG = "DragGrid";

	private int dragPosition;
	private int dropPosition;
	private int holdPosition;
	private int startPosition;
	private int specialPosition = -1;
	private int leftBottomPosition = -1;

	private int nColumns = 3;

	private int halfItemWidth;

	private ImageView dragImageView = null;
	private ViewGroup dragItemView = null;

	private WindowManager windowManager = null;
	private WindowManager.LayoutParams windowParams = null;

	private int mLastX, xtox;
	private int mLastY, ytoy;
	private int specialItemY;
	private int leftBtmItemY;

	private String LastAnimationID;

	private boolean isCountXY = false;
	private boolean isMoving = false;

	// private ArrayList<ViewGroup> mItemViewList ;

	public DragListView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public DragListView(Context context) {
		super(context);
	}

	boolean flag = true;

	public void setLongFlag(boolean temp) {
		flag = temp;
	}

//	public boolean setOnItemLongClickListener(final MotionEvent ev) {
//		this.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
//			@Override
//			public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
//					int arg2, long arg3) {
//				Log.i(TAG, "长按了");
//				return true;
//			};
//		});
//
//		return super.onInterceptTouchEvent(ev);
//	}

	public void GetItemShadow(int x, int y) {

	}

//	@Override
//	public boolean onInterceptTouchEvent(MotionEvent ev) {
//		if (ev.getAction() == MotionEvent.ACTION_DOWN) {
//			return setOnItemLongClickListener(ev);
//		}
//		return super.onInterceptTouchEvent(ev);
//	}

//	@Override
//	public boolean onTouchEvent(MotionEvent ev) {
//		if (dragImageView != null
//				&& dragPosition != AdapterView.INVALID_POSITION) {
//			int x = (int) ev.getX();
//			int y = (int) ev.getY();
//			switch (ev.getAction()) {
//			case MotionEvent.ACTION_MOVE:
//				if (!isCountXY) {
//					xtox = x - mLastX;
//					ytoy = y - mLastY;
//					isCountXY = true;
//				}
//				onDrag(x, y);
//				if (!isMoving)
//					OnMove(x, y);
//				break;
//			case MotionEvent.ACTION_UP:
//				stopDrag();
//				onDrop(x, y);
//				break;
//			}
//		}
//		return super.onTouchEvent(ev);
//	}

	public void OnMove(int x, int y) {
		int TempPosition = pointToPosition(x, y);
		if (TempPosition == 0) {
			return;
		}
		int sOffsetY = specialItemY == -1 ? y - mLastY : y - specialItemY
				- halfItemWidth;
		int lOffsetY = leftBtmItemY == -1 ? y - mLastY : y - leftBtmItemY
				- halfItemWidth;
		if (TempPosition != AdapterView.INVALID_POSITION
				&& TempPosition != dragPosition) {
			// dropPosition = TempPosition;

			if (getItemAtPosition(TempPosition) != null) {
				dropPosition = TempPosition;
			} else {
				dropPosition = (getCount() - 2);
			}
			// if(dropPosition == getCount() - 1){
			// dropPosition = (getCount() - 2);
			// }
		} else if (specialPosition != -1 && dragPosition == specialPosition
				&& sOffsetY >= halfItemWidth) {
			// dropPosition = (getCount() - 2);//(itemTotalCount - 1);

			if (getItemAtPosition(TempPosition) != null) {
				dropPosition = getCount() - 1;
			} else {
				dropPosition = (getCount() - 2);
			}
		} else if (leftBottomPosition != -1
				&& dragPosition == leftBottomPosition
				&& lOffsetY >= halfItemWidth) {
			// dropPosition = (getCount() - 2);//(itemTotalCount - 1);

			if (getItemAtPosition(TempPosition) != null) {
				dropPosition = getCount() - 1;
			} else {
				dropPosition = (getCount() - 2);
			}
		}
		if (dragPosition != startPosition)
			dragPosition = startPosition;
		int MoveNum = dropPosition - dragPosition;
		if (dragPosition != startPosition && dragPosition == dropPosition)
			MoveNum = 0;
		if (MoveNum != 0) {
			int itemMoveNum = Math.abs(MoveNum);
			float Xoffset, Yoffset;
			for (int i = 0; i < itemMoveNum; i++) {
				if (MoveNum > 0) {
					holdPosition = dragPosition + 1;
					Xoffset = (dragPosition / nColumns == holdPosition
							/ nColumns) ? (-1) : (nColumns - 1);
					Yoffset = (dragPosition / nColumns == holdPosition
							/ nColumns) ? 0 : (-1);
				} else {
					holdPosition = dragPosition - 1;
					Xoffset = (dragPosition / nColumns == holdPosition
							/ nColumns) ? 1 : (-(nColumns - 1));
					Yoffset = (dragPosition / nColumns == holdPosition
							/ nColumns) ? 0 : 1;
				}
				ViewGroup moveView = (ViewGroup) getChildAt(holdPosition);
				Animation animation = getMoveAnimation(Xoffset, Yoffset);
				moveView.startAnimation(animation);
				dragPosition = holdPosition;
				if (dragPosition == dropPosition)
					LastAnimationID = animation.toString();
				animation
						.setAnimationListener(new Animation.AnimationListener() {

							@Override
							public void onAnimationStart(Animation animation) {
								// TODO Auto-generated method stub
								isMoving = true;
							}

							@Override
							public void onAnimationRepeat(Animation animation) {
								// TODO Auto-generated method stub

							}

							@Override
							public void onAnimationEnd(Animation animation) {
								// TODO Auto-generated method stub
								String animaionID = animation.toString();
								if (animaionID
										.equalsIgnoreCase(LastAnimationID)) {
									// adapter.exchange(startPosition,
									// dropPosition);
									startPosition = dropPosition;
									isMoving = false;
								}
							}
						});
			}
		}
	}

	private void onDrop(int x, int y) {
		// final DateAdapter adapter = (DateAdapter) this.getAdapter();
		// adapter.showDropItem(true);
		// adapter.notifyDataSetChanged();
	}

	private void onDrag(int x, int y) {
		if (dragImageView != null) {
			windowParams.alpha = 0.8f;
			windowParams.x = (x - mLastX - xtox) + dragItemView.getLeft() + 8;
			windowParams.y = (y - mLastY - ytoy) + dragItemView.getTop()
					+ (int) (45 * Configure.screenDensity);
			windowManager.updateViewLayout(dragImageView, windowParams);
		}
	}

	private void stopDrag() {
		if (dragImageView != null) {
			windowManager.removeView(dragImageView);
			dragImageView = null;
		}
	}

	public Animation getMoveAnimation(float x, float y) {
		TranslateAnimation go = new TranslateAnimation(
				Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, x,
				Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, y);
		go.setFillAfter(true);
		go.setDuration(300);
		return go;
	}

}
