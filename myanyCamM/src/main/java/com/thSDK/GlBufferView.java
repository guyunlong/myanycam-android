package com.thSDK;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by gyl1 on 3/13/18.
 */

public class GlBufferView extends GLSurfaceView {
    public boolean getFirstFrame = false;
    public Context cnx;
    int glwidth;
    int glheight;

    public GlBufferView(Context context) {
        super(context);
        cnx = context;
        setRenderer(new MyRenderer());
        requestFocus();
        setFocusableInTouchMode(true);


    }
    public GlBufferView(Context context, AttributeSet attrs) {
        super(context, attrs);
        cnx = context;
        setRenderer(new MyRenderer());
        requestFocus();
        setFocusableInTouchMode(true);

    }


    class MyRenderer implements GLSurfaceView.Renderer {

        public void onDrawFrame(GL10 gl) {

            int ret = lib.jopengl_Render();
            Log.e("gyl","render ret is "+ret);

        }
        public void onSurfaceChanged(GL10 gl, int w, int h) {
            lib.jopengl_Resize(w, h);
            glwidth = w;
            glheight = h;
            Log.e("gyl","resize");
        }

        public void onSurfaceCreated(GL10 arg0, EGLConfig arg1) {
            // TODO Auto-generated method stub

        }
    }


}