package com.myanycamm.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import com.myanycamm.cam.R;

public class SlipButton extends View implements OnTouchListener {
	private Bitmap bg, slip_btn,bgOff;
	private boolean NowChoose = true;
	private boolean OnSlip = false;
	private float DownX, NowX;
	private Rect Btn_On, Btn_Off;

	private boolean isChgLsnOn = false;
	private OnChangedListener ChgLsn;

	public static final int ERROR_SOURCE_ID = -1;

	private int mButtonBackgroundSourceId = ERROR_SOURCE_ID;
	private int mButtonBackgroundSourceIdOff = ERROR_SOURCE_ID;
	private int mSlipBackgroundSourceId = ERROR_SOURCE_ID;

	private int flags;
	

	public boolean isNowChoose() {
		return NowChoose;
	}

	public SlipButton(Context context) {
		super(context);
		init();
		setOnTouchListener(this);
	}

	public SlipButton(Context context, AttributeSet attrs) {
		super(context, attrs);

		TypedArray resArray = context.obtainStyledAttributes(attrs,
				R.styleable.SlipButton);

		mButtonBackgroundSourceId = resArray.getResourceId(
				R.styleable.SlipButton_background, R.drawable.switch_default);
		mButtonBackgroundSourceIdOff = resArray.getResourceId(
				R.styleable.SlipButton_background, R.drawable.switch_default_off);

		mSlipBackgroundSourceId = resArray.getResourceId(
				R.styleable.SlipButton_slip, R.drawable.slip);
		NowChoose = resArray.getBoolean(R.styleable.SlipButton_choosed_status,
				true);
		resArray.recycle();

		init();
		setOnTouchListener(this);
	}

	
	private void init() {
		
		bg = BitmapFactory.decodeResource(getResources(),
				mButtonBackgroundSourceId);
		bgOff = BitmapFactory.decodeResource(getResources(),
				mButtonBackgroundSourceIdOff);
		slip_btn = BitmapFactory.decodeResource(getResources(),
				mSlipBackgroundSourceId);
		
		Btn_Off = new Rect(0, 0, slip_btn.getWidth(), slip_btn.getHeight());
		Btn_On = new Rect(bg.getWidth() - slip_btn.getWidth(), 0,
				bg.getWidth(), slip_btn.getHeight());
	}

	@Override
	protected void onDraw(Canvas canvas) {
		
		super.onDraw(canvas);
		Matrix matrix = new Matrix();
		Paint paint = new Paint();
		float x;	
		canvas.drawBitmap(bgOff, matrix, paint);

		if (OnSlip)
		{
			
			if (NowX >= bg.getWidth()) { 
				x = bg.getWidth() - slip_btn.getWidth() / 2;
			} else {
				x = NowX - slip_btn.getWidth() / 2;
			}
		} else {
			if (NowChoose) {
				x = Btn_On.left;
				canvas.drawBitmap(bg, matrix, paint);
			} else {
				x = Btn_Off.left;
			}
		}
		if (x < 0) {
			x = 0;
		} else if (x > bg.getWidth() - slip_btn.getWidth()) {
			x = bg.getWidth() - slip_btn.getWidth();
		}
	
		canvas.drawBitmap(slip_btn, x, 0, paint);
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch (event.getAction())
		{
		case MotionEvent.ACTION_MOVE: {
			NowX = event.getX();
		}
			break;
		case MotionEvent.ACTION_DOWN: {
			if (event.getX() > bg.getWidth() || event.getY() > bg.getHeight()) {
				return false;
			}
			OnSlip = true;
			DownX = event.getX();
			NowX = DownX;
		}
			break;
		case MotionEvent.ACTION_UP: {
			OnSlip = false;
			boolean LastChoose = NowChoose;
			
			
			
			
			

			if (event.getX() >= (bg.getWidth() / 2)) {
				if (NowChoose) {
					NowChoose = false;
				} else {
					NowChoose = true;
				}
			} else {
				if (NowChoose) {
					NowChoose = false;
				} else {
					NowChoose = true;
				}
			}

			if (isChgLsnOn && (LastChoose != NowChoose)) {
				ChgLsn.OnChanged(NowChoose, flags);
			}
		}
			break;
		case MotionEvent.ACTION_CANCEL:
			boolean LastChoose = NowChoose;
			OnSlip = false;
			if (event.getX() >= (bg.getWidth() / 2)) {
				if (NowChoose) {
					NowChoose = false;
				} else {
					NowChoose = true;
				}
			} else {
				if (NowChoose) {
					NowChoose = false;
				} else {
					NowChoose = true;
				}
			}
			if (isChgLsnOn && (LastChoose != NowChoose)) {
				ChgLsn.OnChanged(NowChoose, flags);
			}
			break;
		default:

		}

		invalidate();

		return true;
	}

	public void onBtnClick() {
		OnSlip = false;
		boolean LastChoose = NowChoose;
		if (NowChoose) {
			NowChoose = false;
		} else {
			NowChoose = true;
		}
		if (isChgLsnOn && (LastChoose != NowChoose)) {
			ChgLsn.OnChanged(NowChoose, flags);
		}
		invalidate();
	}

	
	public void setState(boolean isOpen) {
		NowChoose = isOpen;
		if (isChgLsnOn) {
			ChgLsn.OnChanged(NowChoose, flags);
		}
		invalidate();
	}

	public void setStateWithOutChangListern(boolean isOpen) {
		NowChoose = isOpen;
		invalidate();
	}
	
	public void SetOnChangedListener(OnChangedListener listener, int id) {
		isChgLsnOn = true;
		ChgLsn = listener;
		flags = id;
	}

	
	public void setButtonBackgroundSourceId(int sourceId) {
		mButtonBackgroundSourceId = sourceId;
		init();
	}

	
	public void setSlipSourceId(int sourceId) {
		mSlipBackgroundSourceId = sourceId;
		init();
	}

	
	public void setButtonDefault(boolean isOpen) {
		NowChoose = isOpen;
	}

	
	public interface OnChangedListener {
		abstract void OnChanged(boolean CheckState, int id);
	}

}
