package com.myanycamm.process;

import java.text.DecimalFormat;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

import com.myanycamm.cam.R;


public class PorterDuffView extends ImageView {
	private static final String TAG = "PorterDuffView";
	
	public static final int FG_HEIGHT = 1;
	
	// public static final int FOREGROUND_COLOR = 0x77123456;
	public static final int FOREGROUND_COLOR = 0x77ff0000;
	
	public static final int TEXT_COLOR = 0xff7fff00;
	
	public static final int FONT_SIZE = 30;
	private Bitmap bitmapBg, bitmapFg;
	private Paint paint;
	
	private float progress;
	
	private int width, height;
	
	private DecimalFormat decFormat;
	
	private float txtBaseY;
	
	private boolean porterduffMode;
	
	private boolean loading;

	public PorterDuffView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs);
	}

	
	private static Bitmap createForegroundBitmap(int w) {
		Bitmap bm = Bitmap.createBitmap(w, FG_HEIGHT, Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(bm);
		Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
		p.setColor(FOREGROUND_COLOR);
		c.drawRect(0, 0, w, FG_HEIGHT, p);
		return bm;
	}

	private void init(Context context, AttributeSet attrs) {
		if (attrs != null) {
			TypedArray typedArr = context.obtainStyledAttributes(attrs,
					R.styleable.PorterDuffView);
			porterduffMode = typedArr.getBoolean(
					R.styleable.PorterDuffView_porterduffMode, false);
		}
		Drawable drawable = getDrawable();
		if (porterduffMode && drawable != null
				&& drawable instanceof BitmapDrawable) {
			bitmapBg = ((BitmapDrawable) drawable).getBitmap();
			width = bitmapBg.getWidth();
			height = bitmapBg.getHeight();
			bitmapFg = createForegroundBitmap(width);//得到高度为1的红色前景色
		} else {
			// 不符合要求，自动设置为false。
			porterduffMode = false;
		}

		paint = new Paint();
		paint.setFilterBitmap(false);
		paint.setAntiAlias(true);
		paint.setTextSize(FONT_SIZE);
		Paint.FontMetrics fontMetrics = paint.getFontMetrics();
		// 注意观察本输出：
		// ascent:单个字符基线以上的推荐间距，为负数
		Log.i(TAG, "ascent:" + fontMetrics.ascent//
				// descent:单个字符基线以下的推荐间距，为正数
				+ " descent:" + fontMetrics.descent
				// 单个字符基线以上的最大间距，为负数
				+ " top:" + fontMetrics.top
				// 单个字符基线以下的最大间距，为正数
				+ " bottom:" + fontMetrics.bottom//
				// 文本行与行之间的推荐间距
				+ " leading:" + fontMetrics.leading);
		// 在此处直接计算出来，避免了在onDraw()处的重复计算
		txtBaseY = (height - fontMetrics.bottom - fontMetrics.top) / 2;

		decFormat = new DecimalFormat("0.0%");
	}

	public void onDraw(Canvas canvas) {
		if (porterduffMode) {
			int tmpW = (getWidth() - width) / 2, tmpH = (getHeight() - height) / 2;
			// 画出背景图
			canvas.drawBitmap(bitmapBg, tmpW, tmpH, paint);
			// 设置PorterDuff模式
			paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DARKEN));
			// canvas.drawBitmap(bitmapFg, tmpW, tmpH - progress * height,
			// paint);
			int tH = height - (int) (progress * height);
			for (int i = 0; i < tH; i++) {
				canvas.drawBitmap(bitmapFg, tmpW, tmpH + i, paint);
			}

			// 立即取消xfermode
			paint.setXfermode(null);
			int oriColor = paint.getColor();
			paint.setColor(TEXT_COLOR);
			paint.setTextSize(FONT_SIZE);
			String tmp = decFormat.format(progress);
			float tmpWidth = paint.measureText(tmp);
			canvas.drawText(decFormat.format(progress), tmpW
					+ (width - tmpWidth) / 2, tmpH + txtBaseY, paint);
			// 恢复为初始值时的颜色
			paint.setColor(oriColor);
		} else {
			Log.i(TAG, "onDraw super");
			super.onDraw(canvas);
		}
	}

	public void setProgress(float progress) {
		if (porterduffMode) {
			this.progress = progress;
			// 刷新自身。
			invalidate();
		}
	}


	public boolean isLoading() {
		return loading;
	}

	public void setLoading(boolean loading) {
		this.loading = loading;
	}

	public void setPorterDuffMode(boolean bool) {
		porterduffMode = bool;
	}
}
