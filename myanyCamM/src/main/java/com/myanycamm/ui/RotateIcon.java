

package com.myanycamm.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.myanycamm.cam.R;


public class RotateIcon extends ImageView
{
	Animation mAnim_Roat;
	
	public RotateIcon(Context context)
	{
		super(context);
		initView();
		initData();
	}

	public RotateIcon(Context context, AttributeSet attrs)
	{
		super(context, attrs);		
		initView();
		initData();
	}

	public void startRotate()
	{
		this.startAnimation(mAnim_Roat);
	}
	
	public void stopRotate()
	{
		this.clearAnimation();
	}

	public void initView()
	{
		setImageResource(R.drawable.refresh_normal);		
	}

	public void initData()
	{
		mAnim_Roat = AnimationUtils.loadAnimation(getContext(), R.anim.rote);
	}
}
