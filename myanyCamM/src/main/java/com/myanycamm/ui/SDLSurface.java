package com.myanycamm.ui;

import android.content.Context;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;


public class SDLSurface extends SurfaceView implements SurfaceHolder.Callback{

	public SDLSurface(Context context, AttributeSet attrs) {
		super(context,attrs);
		getHolder().addCallback(this);
		setFocusable(true);
		setFocusableInTouchMode(true);
		requestFocus();
	}
	
	// Startup
	public SDLSurface(Context context) {
		super(context);
		getHolder().addCallback(this);
		setFocusable(true);
		setFocusableInTouchMode(true);
		requestFocus();
	}

	// Called when we have a valid drawing surface
	public void surfaceCreated(SurfaceHolder holder) {
//		JNITest.startApp();
	}

	// Called when we lose the surface
	public void surfaceDestroyed(SurfaceHolder holder) {
	}

	// Called when the surface is resized
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		Log.v("SDL", "surfaceChanged()");

        int sdlFormat = 0x85151002;
        switch (format) {
        case PixelFormat.A_8:
            Log.v("SDL", "pixel format A_8");
            break;
        case PixelFormat.LA_88:
            Log.v("SDL", "pixel format LA_88");
            break;
        case PixelFormat.L_8:
            Log.v("SDL", "pixel format L_8");
            break;
        case PixelFormat.RGBA_4444:
            Log.v("SDL", "pixel format RGBA_4444");
            sdlFormat = 0x85421002;
            break;
        case PixelFormat.RGBA_5551:
            Log.v("SDL", "pixel format RGBA_5551");
            sdlFormat = 0x85441002;
            break;
        case PixelFormat.RGBA_8888:
            Log.v("SDL", "pix el format RGBA_8888");
            sdlFormat = 0x86462004;
            break;
        case PixelFormat.RGBX_8888:
            Log.v("SDL", "pixel format RGBX_8888");
            sdlFormat = 0x86262004;
            break;
        case PixelFormat.RGB_332:
            Log.v("SDL", "pixel format RGB_332");
            sdlFormat = 0x84110801;
            break;
        case PixelFormat.RGB_565:
            Log.v("SDL", "pixel format RGB_565");
            sdlFormat = 0x85151002;
            break;
        case PixelFormat.RGB_888:
            Log.v("SDL", "pixel format RGB_888");
           
            sdlFormat = 0x86161804;
            break;
        default:
            Log.v("SDL", "pixel format unknown " + format);
            break;
        }
        CloudLivingView.onNativeResize(width, height, sdlFormat);
        Log.v("SDL", "Window size:" + width + "x"+height);
        CloudLivingView.startApp();
	}


}
