

package com.myanycam.zxing.client.android.camera;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.SurfaceHolder;

import com.myanycamm.zxing.client.android.PlanarYUVLuminanceSource;

import java.io.IOException;


public final class CameraManager {

  private static final String TAG = CameraManager.class.getSimpleName();

//  private static final int MIN_FRAME_WIDTH = 280;
//  private static final int MIN_FRAME_HEIGHT = 200;
//  private static final int MAX_FRAME_WIDTH = 480;
//  private static final int MAX_FRAME_HEIGHT = 360;

  private static CameraManager cameraManager;

  static final int SDK_INT;
  static {
    int sdkInt;
    try {
      sdkInt = Integer.parseInt(Build.VERSION.SDK);
    } catch (NumberFormatException nfe) {
     
      sdkInt = 10000;
    }
    SDK_INT = sdkInt;
  }

  private final Context context;
  private final CameraConfigurationManager configManager;
  private Camera camera;
  private Rect framingRect;
  private Rect framingRectInPreview;
  private boolean initialized;
  private boolean previewing;
  private final boolean useOneShotPreviewCallback;
  
  private final PreviewCallback previewCallback;
  
  private final AutoFocusCallback autoFocusCallback;

  
  public static void init(Context context) {
    if (cameraManager == null) {
      cameraManager = new CameraManager(context);
    }
  }

  
  public static CameraManager get() {
    return cameraManager;
  }

  private CameraManager(Context context) {

    this.context = context;
    this.configManager = new CameraConfigurationManager(context);

   
   
   
   
   
    useOneShotPreviewCallback = Integer.parseInt(Build.VERSION.SDK) > 3;

    previewCallback = new PreviewCallback(configManager, useOneShotPreviewCallback);
    autoFocusCallback = new AutoFocusCallback();
  }

  
  public void openDriver(SurfaceHolder holder) throws IOException {
    if (camera == null) {
      camera = Camera.open();
      if (camera == null) {
        throw new IOException();
      }
      camera.setPreviewDisplay(holder);

      if (!initialized) {
        initialized = true;
        configManager.initFromCameraParameters(camera);
      }
      configManager.setDesiredCameraParameters(camera);
    }
  }

  
  public void closeDriver() {
    if (camera != null) {
      FlashlightManager.disableFlashlight();
      camera.release();
      camera = null;
    }
  }

  
  public void startPreview() {
    if (camera != null && !previewing) {
      camera.startPreview();
      previewing = true;
    }
  }

  
  public void stopPreview() {
    if (camera != null && previewing) {
      if (!useOneShotPreviewCallback) {
        camera.setPreviewCallback(null);
      }
      camera.stopPreview();
      previewCallback.setHandler(null, 0);
      autoFocusCallback.setHandler(null, 0);
      previewing = false;
    }
  }

  
  public void requestPreviewFrame(Handler handler, int message) {
    if (camera != null && previewing) {
      previewCallback.setHandler(handler, message);
      if (useOneShotPreviewCallback) {
        camera.setOneShotPreviewCallback(previewCallback);
      } else {
        camera.setPreviewCallback(previewCallback);
      }
    }
  }

  
  public void requestAutoFocus(Handler handler, int message) {
    if (camera != null && previewing) {
      autoFocusCallback.setHandler(handler, message);
     
      camera.autoFocus(autoFocusCallback);
    }
  }

  
  public Rect getFramingRect() {
    Point screenResolution = configManager.getScreenResolution();
    if (framingRect == null) {
      if (camera == null) {
        return null;
      }
      int width = screenResolution.x * 4 / 5;
      Log.i(TAG, "width:"+width);
//      if (width < MIN_FRAME_WIDTH) {
//        width = width;
//      } else if (width > MAX_FRAME_WIDTH) {
//        width = width;
//      }
      int height = screenResolution.y * 3 / 4;
      	height = width * 5/6;
//      Log.i(TAG, "height:"+height);
//      if (height < MIN_FRAME_HEIGHT) {
//        height = height;
//      } else if (height > MAX_FRAME_HEIGHT) {
//        height = height;
//      }
      int leftOffset = (screenResolution.x - width) / 2;
      int topOffset = (screenResolution.y - height) / 2;
      framingRect = new Rect(leftOffset, topOffset, leftOffset + width, topOffset + height);
      Log.d(TAG, "Calculated framing rect: " + framingRect);
    }
    return framingRect;
  }

  
  public Rect getFramingRectInPreview() {
    if (framingRectInPreview == null) {
      Rect rect = new Rect(getFramingRect());
      Point cameraResolution = configManager.getCameraResolution();
      Point screenResolution = configManager.getScreenResolution();
      
      rect.left = rect.left * cameraResolution.y / screenResolution.x;
      rect.right = rect.right * cameraResolution.y / screenResolution.x;
      rect.top = rect.top * cameraResolution.x / screenResolution.y;
      rect.bottom = rect.bottom * cameraResolution.x / screenResolution.y;
//    rect.left = rect.left * cameraResolution.x / screenResolution.x;
//    rect.right = rect.right * cameraResolution.x / screenResolution.x;
//    rect.top = rect.top * cameraResolution.y / screenResolution.y;
//    rect.bottom = rect.bottom * cameraResolution.y / screenResolution.y;
      

      framingRectInPreview = rect;
    }
    return framingRectInPreview;
  }

  
  

  
  public PlanarYUVLuminanceSource buildLuminanceSource(byte[] data, int width, int height) {
    Rect rect = getFramingRectInPreview();
    int previewFormat = configManager.getPreviewFormat();
    String previewFormatString = configManager.getPreviewFormatString();
    switch (previewFormat) {
     
     
      case PixelFormat.YCbCr_420_SP:
     
     
      case PixelFormat.YCbCr_422_SP:
        return new PlanarYUVLuminanceSource(data, width, height, rect.left, rect.top,
            rect.width(), rect.height());
      default:
       
       
        if ("yuv420p".equals(previewFormatString)) {
          return new PlanarYUVLuminanceSource(data, width, height, rect.left, rect.top,
            rect.width(), rect.height());
        }
    }
    throw new IllegalArgumentException("Unsupported picture format: " +
        previewFormat + '/' + previewFormatString);
  }

}
