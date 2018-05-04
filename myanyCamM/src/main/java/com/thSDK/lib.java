package com.thSDK;


import android.view.Surface;

import io.vov.vitamio.utils.Log;

public class lib
{
    static  String tag = "thSDKLib";
    static
    {
        try
        {
            Log.e(tag,"load lib");
            System.loadLibrary("ffmpegutils");
        }
        catch (UnsatisfiedLinkError e)
        {
            Log.e(tag,"load error:"+e);
            System.out.println("library," + e.getMessage());
        }

    }

    public static native int jlocal_StartRec(String filename);
    public static native int jlocal_StopRec();
    public static native int jlocal_SnapShot(String filename);
    public static native int jopengl_Resize(int w, int h);
    public static native int jopengl_Render();
    public static native int jopenglInit(Surface surface,int width,int height);
    public static native int jvideo_decode_init(int type,int width,int height);
    public static native int jopenglSurfaceChanged(Surface surface,int width,int height);
    public static native int jvideo_decode_frame(byte[]buf,int size);



}

