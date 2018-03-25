package com.myanycamm.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.EditText;

import com.myanycamm.cam.R;

public class FixEditText extends EditText {
	
	private String textNote = null;

	public FixEditText(Context context) {
		super(context);
	}

	public FixEditText(Context context, AttributeSet attrs) {
		super(context, attrs);
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FixEditText);//TypedArray是一个数组容器 
		textNote = a.getString(R.styleable.FixEditText_textNote);
		a.recycle();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		Paint paint = new Paint();
		paint.setTextSize(18);
		paint.setAntiAlias(true);
		paint.setColor(Color.BLACK);
		canvas.drawText(textNote, 10, getHeight() / 2 + 5, paint);
		super.onDraw(canvas);
	}

}
