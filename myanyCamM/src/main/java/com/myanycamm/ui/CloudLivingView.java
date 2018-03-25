package com.myanycamm.ui;

import gyl.cam.SoundPlay;
import gyl.cam.recThread;

import java.io.IOException;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.gesture.GestureOverlayView;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.android.webrtc.audio.MobileAEC;
import com.android.webrtc.audio.MobileAEC.SamplingFrequency;
import com.morlunk.mumbleclient.jni.Native;
import com.myanycam.bean.ActionItem;
import com.myanycam.bean.CameraListInfo;
import com.myanycam.bean.VideoData;
import com.myanycam.net.SocketFunction;
import com.myanycam.net.TcpSocket;
import com.myanycamm.cam.AppServer;
import com.myanycamm.cam.CameraCenterActivity;
import com.myanycamm.cam.R;
import com.myanycamm.model.VideoListener;
import com.myanycamm.process.AdPcm;
import com.myanycamm.utils.ELog;
import com.myanycamm.utils.FileUtils;
import com.myanycamm.utils.Utils;

public class CloudLivingView extends LivingView {

	private static String TAG = "CloudLivingView";
	private ImageView playBtn;
	private static final int SET_Img = 21;
	private static final int SHOW_CONTROLLER = 22;
	public static final int SHWO_HEAD = 29;
	private static final int FADE_OUT = 23;
	private static final int STOP_CAM = 25;
	private static final int SHOW_RATE = 30;

	public static final int NO_VIDEO = 26;
	public static final int NO_MCUSOKET = 27;
	private static final int FADE_OUT_MEDIA = 28;
	private static final int sDefaultTimeout = 3000;
	private ImageButton speak, photo, sound, videRec;
	private SurfaceView mSurfaceView;
	// private GlBufferView mGlBufferView;
	private RelativeLayout mediaControllerLayout;
	private boolean isSound = false;
	private boolean isDrawBitmap = false;
	private boolean isFirstVideo = true;
	public static boolean sdlTAG = false;
	SurfaceHolder surfaceHolder;
	private Button testBtn;
	public static int packagesize = 160;
	public static short[] tmpBuf = new short[packagesize];

	// 音频获取源
	private int audioSource = MediaRecorder.AudioSource.MIC;
	// 设置音频采样率，44100是目前的标准，但是某些设备仍然支持22050，16000，11025
	private static int sampleRateInHz = 8000;
	// private static int sampleRateInHz = 48000;
	// 设置音频的录制的声道CHANNEL_IN_STEREO为双声道，CHANNEL_CONFIGURATION_MONO为单声道
	private static int channelConfig = AudioFormat.CHANNEL_CONFIGURATION_MONO;
	// 音频数据格式:PCM 16位每个样本。保证设备支持。PCM 8位每个样本。不一定能得到设备支持。
	private static int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
	AlertDialog.Builder builder;
	public static boolean isRecording = false;
	boolean isRecVideoing = false;
	int recBufSize;
	SoundPlay soundPlay;
	AudioRecord audioRecord;
	private Paint paint;
	private Bitmap bitmap;
	private Matrix matrix;// 视频放大，移动用
	private Button playBack;
	private TextView settingTitle;
	private SocketFunction sf;
	private TextView rateTextView;
	private Button selectBtn;
	// 视频质量选择
	private static final int ID_BEST = 3;
	private static final int ID_BETTER = 2;
	private static final int ID_GOOD = 1;
	QuickAction mQuickAction;

	// SDL相关
	// Main components
	private static SDLSurface mSurface;
	private static View mTextEdit;
	private static ViewGroup mLayout;
	private static CameraCenterActivity mSingleton;

	// This is what SDL runs in. It invokes SDL_main(), eventually
	private static Thread mSDLThread;

	// Audio
	private static Thread mAudioThread;
	private static AudioTrack mAudioTrack;

	// EGL private objects
	private static EGLContext mEGLContext;
	private static EGLSurface mEGLSurface;
	private static EGLDisplay mEGLDisplay;
	private static EGLConfig mEGLConfig;
	private static int mGLMajor, mGLMinor;

	static final int COMMAND_CHANGE_TITLE = 1;
	static final int COMMAND_UNUSED = 2;
	static final int COMMAND_TEXTEDIT_HIDE = 3;

	private VideoListener mVideoListener = new VideoListener() {

		@Override
		public void noVideoListener(int tag) {
			if (!AppServer.isDisplayVideo) {
				return;
			}
			ELog.i(TAG, "处理监控没有视频");
			switch (tag) {
			case VideoListener.NOTCPVIDEO:
				break;
			case VideoListener.NOUDPVIDEO:
				mHandler.sendEmptyMessage(NO_VIDEO);
				break;
			default:
				break;
			}

		}
	};

	public Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			//

			switch (msg.what) {

			case SET_Img:
				bitmap = (Bitmap) msg.obj;
				if (!AppServer.isDisplayVideo) {
					return;
				}
				//
				if (isFirstVideo) {
					sdlTAG = true;
					mSurfaceView.setBackgroundColor(Color.TRANSPARENT);
					playLayout.setVisibility(View.GONE);
					mActivity
							.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
					mSurfaceView.setOnTouchListener(surfaceOnTouchListener);
					showMedia(sDefaultTimeout);
				}
				isFirstVideo = false;
				if (CameraListInfo.currentCam.isUpnp()) {
					rateTextView.setText(TcpSocket.getInstance().rateLast / 1024 + "KB/s");
				}else{
					rateTextView.setText(sf.mUdpSocket.rateLast / 1024 + "KB/s");
				}
				
				// revImage.setImageBitmap((Bitmap) msg.obj);
				break;
			case SHOW_RATE:
				if (CameraListInfo.currentCam.isUpnp()) {
					rateTextView.setText(TcpSocket.getInstance().rateLast / 1024 + "KB/s");
				}else{
					rateTextView.setText(sf.mUdpSocket.rateLast / 1024 + "KB/s");
				}
				break;
			case SHOW_CONTROLLER:
				headLayout.setVisibility(View.VISIBLE);
				mediaControllerLayout.setVisibility(View.VISIBLE);
				break;
			case SHWO_HEAD:
				headLayout.setVisibility(View.VISIBLE);
				showMedia(sDefaultTimeout);
				break;
			case FADE_OUT:
				if (isRecording) {
					return;
				}
				ELog.i(TAG, "到了fade out..");
				if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
					headLayout.setVisibility(View.VISIBLE);
					return;
				}
				headLayout.setVisibility(View.INVISIBLE);
				mediaControllerLayout.setVisibility(View.INVISIBLE);
				break;
			case STOP_CAM:
				stopCam();
				break;
			case NO_VIDEO:
				// if (sf.getMcuSocket().cameraListInfo.getStatus() == 0) {
				//
				// initStopCam();
				// showOffLineDilalog();
				// return;
				// }
				ELog.i(TAG, "处理视频网络错误");
				ShowRetryDialog();
				break;
			case NO_MCUSOKET:
				ELog.i(TAG, "muc错误");
				ShowRetryDialog();
				break;
			case FADE_OUT_MEDIA:
				if (isRecording) {
					return;
				}
				if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
					return;
				}
				mediaControllerLayout.setVisibility(View.INVISIBLE);
				break;

			case COMMAND_CHANGE_TITLE:
				// setTitle((String) msg.obj);
				break;
			case COMMAND_TEXTEDIT_HIDE:
				// if (mTextEdit != null) {
				// mTextEdit.setVisibility(View.GONE);
				//
				// InputMethodManager imm = (InputMethodManager)
				// getSystemService(Context.INPUT_METHOD_SERVICE);
				// imm.hideSoftInputFromWindow(mTextEdit.getWindowToken(), 0);
				// }
				break;
			default:

				break;
			}
		}

	};

	OnClickListener speakOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			ELog.i(TAG, "对讲...");
			if (isRecording) {
				speak.setImageResource(R.drawable.play_ctr_speak);
				isRecording = false;
				VideoData.audioArraryList.clear();
				sf.mUdpSocket.colseSenAudioSwitch();
			} else {
				speak.setImageResource(R.drawable.play_ctr_speak_on);
				new RecordPlayThread().start();
			}

		}
	};

	OnClickListener recVideoOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			ELog.i(TAG, "对讲...");
			if (isRecVideoing) {
				videRec.setImageResource(R.drawable.play_rec_btn);
				isRecVideoing = false;
				sf.manualRecord(0);
				// VideoData.audioArraryList.clear();
				// sf.mUdpSocket.colseSenAudioSwitch();
			} else {
				sf.manualRecord(1);
				isRecVideoing = true;
				videRec.setImageResource(R.drawable.play_rec_btn_on);
				// new RecordPlayThread().start();
			}

		}
	};
	OnTouchListener speakOnTouchListener = new OnTouchListener() {

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				show(0);
				ELog.i(TAG, "按下了");
				isRecording = true;
				new RecordPlayThread().start();
				break;

			case MotionEvent.ACTION_UP:
				isRecording = false;
				show(sDefaultTimeout);
				sf.mUdpSocket.colseSenAudioSwitch();
				break;

			default:

				break;
			}
			return false;
		}
	};

	OnClickListener photoOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			sf.manualSnap();
			if (!FileUtils.externalMemoryAvailable()) {
				Toast.makeText(mActivity,
						mActivity.getString(R.string.sdcard_invalid),
						Toast.LENGTH_SHORT).show();
				return;
			}

			try {
				FileUtils.saveFile(bitmap,
						"myanycam" + SystemClock.currentThreadTimeMillis()
								+ ".png", PhotoListView.mCardPath);
				Toast.makeText(mActivity,
						mActivity.getString(R.string.save_success),
						Toast.LENGTH_SHORT).show();
			} catch (IOException e) {
				ELog.i(TAG, "保存失败>.." + e.getMessage());
				Toast.makeText(mActivity,
						mActivity.getString(R.string.save_failed),
						Toast.LENGTH_SHORT).show();
				e.printStackTrace();
			}
			// sf.getMcuSocket().modifyCam();
		}
	};

	OnClickListener soundOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			// soundPlay = new SoundPlay();
			// soundPlay.start();
			if (!isSound) {
				soundPlay = new SoundPlay();
				soundPlay.start();
				sf.mUdpSocket.getAudioSwitch();
				sound.setImageResource(R.drawable.play_ctr_sound_on);
			} else if (soundPlay != null) {
				sf.mUdpSocket.closeAudioSwitch();
				soundPlay.is_keep_running = false;
				sound.setImageResource(R.drawable.play_ctr_sound);
				soundPlay = null;
			}
			isSound = !isSound;

		}
	};

	OnClickListener selectClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {

			mQuickAction.show(v);

		}
	};

	OnTouchListener surfaceOnTouchListener = new OnTouchListener() {

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			// 竖屏不处理
			if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
				ELog.i(TAG, "竖屏touch事件");
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					if (mediaControllerLayout.getVisibility() == View.VISIBLE) {
						mHandler.sendEmptyMessage(FADE_OUT_MEDIA);
					} else {
						showMedia(sDefaultTimeout);
					}
					break;
				default:
					break;
				}
			} else {
				ELog.i(TAG, "横屏touch事件");
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:

					if (headLayout.getVisibility() == View.VISIBLE) {
						mHandler.sendEmptyMessage(FADE_OUT);
					} else {
						show(sDefaultTimeout);
					}

					break;

				default:
					break;
				}

			}

			return true;
		}
	};

	private OnClickListener playBtnOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			initCam();
			// surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}
	};

	private OnClickListener testBtnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			byte[] a = testbyte();
			ELog.i(TAG, "abc" + a);
		}
	};

	@Override
	public void stopCam() {
		initStopCam();

	};

	@Override
	public void showHead() {
		mHandler.sendEmptyMessage(SHWO_HEAD);
	}

	public CloudLivingView(CameraCenterActivity activity, TabHost tabHost) {
		super(activity, tabHost);
		super.mHandler = this.mHandler;
		sf = (SocketFunction) mActivity.getApplicationContext();
		mSingleton = activity;
		initView();
	}

	// public void showToastNoSdcard(){
	// Toast.makeText(mActivity,
	// mActivity.getString(R.string.cam_no_sdcard),
	// Toast.LENGTH_SHORT).show();
	// }

	public void initView() {
		SocketFunction.getInstance().deviceStatus();
		LayoutInflater inflater = LayoutInflater.from(mActivity);
		camView = inflater.inflate(R.layout.tab_play,
				mTabHost.getTabContentView());
		headTitle = (TextView) camView
				.findViewById(R.id.settings_head_title_play);
		playLayout = (RelativeLayout) camView.findViewById(R.id.play_layout);
		playBtn = (ImageView) camView.findViewById(R.id.play_btn);
		playBack = (Button) camView.findViewById(R.id.settings_back_play);
		playBack.setOnClickListener(playBackOnclClickListener);
		rateTextView = (TextView) camView.findViewById(R.id.rate);
		playText = (TextView) camView.findViewById(R.id.play_text);
		rotImageView = (ImageView) camView.findViewById(R.id.rotate_play_img);
		speak = (ImageButton) camView.findViewById(R.id.play_speak);
		sound = (ImageButton) camView.findViewById(R.id.play_sound);
		photo = (ImageButton) camView.findViewById(R.id.play_photo);
		videRec = (ImageButton) camView.findViewById(R.id.play_rec_btn);
		videRec.setOnClickListener(recVideoOnClickListener);
		mediaControllerLayout = (RelativeLayout) camView
				.findViewById(R.id.mediacontroll);
		mediaControllerLayout.getBackground().setAlpha(50);
		headLayout = (LinearLayout) camView.findViewById(R.id.head_layout);
		mSurfaceView = (SurfaceView) camView.findViewById(R.id.paly_surf);

		// mGlBufferView = (GlBufferView)
		// camView.findViewById(R.id.glbuffer_view);
		surfaceHolder = mSurfaceView.getHolder();
		mActivity
				.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		mActivity.getWindow().addFlags(
				WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		// addBtn.setVisibility(View.VISIBLE);

		// mainLayout = (LinearLayout)
		// camView.findViewById(R.id.cam_mainlayout);
		setHead();
		initSelectWindow();
		// addCam();
		// addView(camView);
		// initSDLView();

	}

	private void initSDLView() {
		ELog.i(TAG, "初始化sdl窗口");
		System.loadLibrary("SDL");
		System.loadLibrary("ffmpegutils");
		mSurface = (SDLSurface) camView.findViewById(R.id.sdl_surf);
		testBtn = (Button) camView.findViewById(R.id.sdltest_btn);
		testBtn.setOnClickListener(testBtnClickListener);
	}

	// Send a message from the SDLMain thread
	void sendCommand(int command, Object data) {
		Message msg = mHandler.obtainMessage();
		msg.what = command;
		msg.obj = data;
		mHandler.sendMessage(msg);
	}

	// C functions we call
	public native byte[] testbyte();

	public static native String initSDL();

	public static native void onNativeResize(int x, int y, int format);

	public static native String convertBytesToVideo(byte[] PY, byte[] PV,
			byte[] PU);

	public static native String destroySDL();

	// Java functions called from C

	public static boolean createGLContext(int majorVersion, int minorVersion) {
		return initEGL(majorVersion, minorVersion);
	}

	public static void flipBuffers() {
		flipEGL();
	}

	public static void setActivityTitle(String title) {
		// Called from SDLMain() thread and can't directly affect the view
		// mSingleton.sendCommand(COMMAND_CHANGE_TITLE, title);
	}

	public static void sendMessage(int command, int param) {
		// mSingleton.sendCommand(command, Integer.valueOf(param));
	}

	// public static Context getContext() {
	// return mSingleton;
	// }

	public static void startApp() {
		// Start up the C app thread
		if (mSDLThread == null) {
			mSDLThread = new Thread(new SDLMain(), "SDLThread");
			mSDLThread.start();
		}
	}

	// EGL functions
	public static boolean initEGL(int majorVersion, int minorVersion) {
		try {
			if (CloudLivingView.mEGLDisplay == null) {
				Log.v(TAG, "Starting up OpenGL ES " + majorVersion + "."
						+ minorVersion);

				EGL10 egl = (EGL10) EGLContext.getEGL();

				EGLDisplay dpy = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);

				int[] version = new int[2];
				egl.eglInitialize(dpy, version);

				int EGL_OPENGL_ES_BIT = 1;
				int EGL_OPENGL_ES2_BIT = 4;
				int renderableType = 0;
				if (majorVersion == 2) {
					renderableType = EGL_OPENGL_ES2_BIT;
				} else if (majorVersion == 1) {
					renderableType = EGL_OPENGL_ES_BIT;
				}
				int[] configSpec = {
						// EGL10.EGL_DEPTH_SIZE, 16,
						EGL10.EGL_RENDERABLE_TYPE, renderableType,
						EGL10.EGL_NONE };
				EGLConfig[] configs = new EGLConfig[1];
				int[] num_config = new int[1];
				if (!egl.eglChooseConfig(dpy, configSpec, configs, 1,
						num_config) || num_config[0] == 0) {
					Log.e(TAG, "No EGL config available");
					return false;
				}
				EGLConfig config = configs[0];

				CloudLivingView.mEGLDisplay = dpy;
				CloudLivingView.mEGLConfig = config;
				CloudLivingView.mGLMajor = majorVersion;
				CloudLivingView.mGLMinor = minorVersion;
			}
			return CloudLivingView.createEGLSurface();

		} catch (Exception e) {
			Log.v(TAG, e + "");
			for (StackTraceElement s : e.getStackTrace()) {
				Log.v(TAG, s.toString());
			}
			return false;
		}
	}

	public static boolean createEGLContext() {
		EGL10 egl = (EGL10) EGLContext.getEGL();
		int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
		int contextAttrs[] = new int[] { EGL_CONTEXT_CLIENT_VERSION,
				CloudLivingView.mGLMajor, EGL10.EGL_NONE };
		CloudLivingView.mEGLContext = egl.eglCreateContext(
				CloudLivingView.mEGLDisplay, CloudLivingView.mEGLConfig,
				EGL10.EGL_NO_CONTEXT, contextAttrs);
		if (CloudLivingView.mEGLContext == EGL10.EGL_NO_CONTEXT) {
			Log.e(TAG, "Couldn't create context");
			return false;
		}
		return true;
	}

	public static boolean createEGLSurface() {
		if (CloudLivingView.mEGLDisplay != null
				&& CloudLivingView.mEGLConfig != null) {
			EGL10 egl = (EGL10) EGLContext.getEGL();
			if (CloudLivingView.mEGLContext == null)
				createEGLContext();

			Log.v(TAG, "Creating new EGL Surface");
			EGLSurface surface = egl.eglCreateWindowSurface(
					CloudLivingView.mEGLDisplay, CloudLivingView.mEGLConfig,
					CloudLivingView.mSurface, null);
			if (surface == EGL10.EGL_NO_SURFACE) {
				Log.e(TAG, "Couldn't create surface");
				return false;
			}

			if (egl.eglGetCurrentContext() != CloudLivingView.mEGLContext) {
				if (!egl.eglMakeCurrent(CloudLivingView.mEGLDisplay, surface,
						surface, CloudLivingView.mEGLContext)) {
					Log.e(TAG,
							"Old EGL Context doesnt work, trying with a new one");
					// TODO: Notify the user via a message that the old context
					// could not be restored, and that textures need to be
					// manually restored.
					createEGLContext();
					if (!egl.eglMakeCurrent(CloudLivingView.mEGLDisplay,
							surface, surface, CloudLivingView.mEGLContext)) {
						Log.e(TAG, "Failed making EGL Context current");
						return false;
					}
				}
			}
			CloudLivingView.mEGLSurface = surface;
			return true;
		} else {
			Log.e(TAG, "Surface creation failed, display = "
					+ CloudLivingView.mEGLDisplay + ", config = "
					+ CloudLivingView.mEGLConfig);
			return false;
		}
	}

	// EGL buffer flip
	public static void flipEGL() {
		try {
			EGL10 egl = (EGL10) EGLContext.getEGL();

			egl.eglWaitNative(EGL10.EGL_HORIZONTAL_RESOLUTION, null);

			// drawing here

			egl.eglWaitGL();

			egl.eglSwapBuffers(CloudLivingView.mEGLDisplay,
					CloudLivingView.mEGLSurface);

		} catch (Exception e) {
			Log.v(TAG, "flipEGL(): " + e);
			for (StackTraceElement s : e.getStackTrace()) {
				Log.v(TAG, s.toString());
			}
		}
	}

	// Audio

	public static Object audioInit(int sampleRate, boolean is16Bit,
			boolean isStereo, int desiredFrames) {
		return null;
	}

	public static void audioStartThread() {
	}

	public static void audioWriteShortBuffer(short[] buffer) {
	}

	public static void audioWriteByteBuffer(byte[] buffer) {
	}

	public static void audioQuit() {
	}

	// public void addView(View child) {
	// addView(child, -1);
	// }

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		ELog.i(TAG, "布局");
		View v = getChildAt(0);
		v.layout(l, t, r, b);
	}

	public void setHead() {
		playBack.setVisibility(View.VISIBLE);
		mediaControllerLayout.setVisibility(View.INVISIBLE);
		headTitle.setText(CameraListInfo.currentCam.getName());// 默认为摄像头列表
		playBtn.setOnClickListener(playBtnOnClickListener);
		selectBtn = (Button) camView.findViewById(R.id.settings_btn);
		selectBtn.setVisibility(View.VISIBLE);
		selectBtn.setOnClickListener(selectClickListener);
		// selectBtn.setText(mActivity.getResources().getString(
		// R.string.select_quality_better));
		// headLayout.setVisibility(View.VISIBLE);
		ELog.i(TAG, "头部完了..");

	}

	public void show(int timeout) {// 第一次会显示
		mHandler.sendEmptyMessage(SHOW_CONTROLLER);
		if (timeout != 0) {
			mHandler.removeMessages(FADE_OUT);
			mHandler.sendMessageDelayed(mHandler.obtainMessage(FADE_OUT),
					timeout);
		}
	}

	private void showMedia(int timeout) {
		mediaControllerLayout.setVisibility(View.VISIBLE);
		mediaControllerLayout.getBackground().setAlpha(100);
		if (timeout != 0) {
			mHandler.removeMessages(FADE_OUT_MEDIA);
			mHandler.sendMessageDelayed(mHandler.obtainMessage(FADE_OUT_MEDIA),
					timeout);
		}
	}

	private void retryCam() {
		// initStopCam();
		ELog.i(TAG, "重试开始视频...");
		playLayout.setVisibility(View.VISIBLE);
		initCam();
	}

	@Override
	public void initCam() {
		playBtn.setOnClickListener(null);
		playText.setVisibility(View.GONE);
		rotImageView.startAnimation(hyperspaceJumpAnimation);
		if (CameraListInfo.currentCam.isUpnp()) {
			sf.watchCameraTcp();
		} else {
			sf.watchCamera(CameraListInfo.currentCam);
		}

		// ShowRetryDialog();
		// MyTimerTask.getInstance().setVideoListener(true);//监听是否有视频
		ELog.i(TAG, "开始了视频..");
		isFirstVideo = true;
		isDrawBitmap = true;
		new DrawImage(10, Utils.getHeightPixels(mActivity) / 2).start();// 开一条线程绘图
		//
		VideoData.Videolist.clear();// 清空数据
		VideoData.audioArraryList.clear();
		recThread dataRecThread = new recThread(mHandler);
		//
		dataRecThread.start();
		sf.mUdpSocket.setmVideoListener(mVideoListener);
		sound.setOnClickListener(soundOnClickListener);
		photo.setOnClickListener(photoOnClickListener);
		ELog.i(TAG, "sn:" + CameraListInfo.currentCam.getSn());
		// if (mActivity.cam.getSn().substring(0, 4).equals("0101")) {
		//
		// speak.setOnTouchListener(speakOnTouchListener);
		// } else if (mActivity.cam.getSn().substring(0, 4).equals("0102")) {
		//
		speak.setOnClickListener(speakOnClickListener);
		// }
		//

	}

	private void initStopCam() {
		ELog.i(TAG, "停止视频接收");
		playLayout.setVisibility(View.VISIBLE);
		playText.setVisibility(View.VISIBLE);
		// MyTimerTask.getInstance().setVideoListener(false);//不监听没视频了
		// sf.getMcuSocket().setmVideoListener(null);
		// if (isDisplayVideo) {
		sf.mUdpSocket.stopCam();
		TcpSocket.getInstance().stopTcpSocket();
		// }

		isRecording = false;

		isRecVideoing = false;
		if (soundPlay != null) {
			soundPlay.is_keep_running = false;
		}

		if (!AppServer.isBackgroud) {
			speak.setImageResource(R.drawable.play_ctr_speak);
			videRec.setImageResource(R.drawable.play_rec_btn);
			sound.setImageResource(R.drawable.play_ctr_sound);
			rateTextView.setText("0KB/s");
		}

		AppServer.isDisplayVideo = false;
		isDrawBitmap = false;
		sdlTAG = false;
		ELog.i(TAG, "aaaa");
		sf.stopWatchCamer(CameraListInfo.currentCam);

		rotImageView.setAnimation(null);

		mediaControllerLayout.setVisibility(View.INVISIBLE);
		isFirstVideo = true;
		mActivity
				.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		mSurfaceView.setOnTouchListener(null);
		VideoData.Videolist.clear();// 清空数据
		VideoData.audioArraryList.clear();
		ELog.i(TAG, "执行完了...");
	}

	private void ShowRetryDialog() {
		if (null != builder) {
			return;
		}
		initStopCam();
		builder = DialogFactory.creatReTryDialog(mActivity, getResources()
				.getString(R.string.net_error));

		builder.setPositiveButton(getResources().getString(R.string.retry),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						retryCam();
						builder = null;
					}
				});
		builder.setNegativeButton(getResources().getString(R.string.exit),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						// stopCam();
						mActivity.finish();
						builder = null;
					}
				});
		builder.create().show();
	}

	public void showCamMaxDialog() {
		if (null != builder) {
			return;
		}
		initStopCam();
		builder = DialogFactory.creatReTryDialog(mActivity, getResources()
				.getString(R.string.cam_max));
		builder.setPositiveButton(getResources().getString(R.string.confirm),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						mActivity.finish();
						builder = null;
					}
				});
		builder.create().show();
	}

	public void adaptWidthScreen(int imgWidth, int imgHeight) {
		matrix = new Matrix();
		float scaleWidth = ((float) Utils.getWidthPixels(mActivity) / imgWidth);
		float scaleHeight = ((float) Utils.getWidthPixels(mActivity) / imgWidth);
		matrix.postScale(scaleWidth, scaleHeight);
		//
		// "屏幕:"
		// + Utils.getHeightPixels(VideoPlayActivity.this)
		// + "surface:" + mSurfaceView.getHeight()
		// + "图片:" + imgHeight);
		matrix.postTranslate(0, (mSurfaceView.getHeight() - imgHeight
				* scaleHeight) / 2);
	}

	public void adaptHeightScreen(int imgWidth, int imgHeight) {
		matrix = new Matrix();
		float scaleWidth = ((float) Utils.getHeightPixels(mActivity) / imgHeight);
		float scaleHeight = ((float) Utils.getHeightPixels(mActivity) / imgHeight);
		matrix.postScale(scaleWidth, scaleHeight);
		//
		// "屏幕:"
		// + Utils.getHeightPixels(VideoPlayActivity.this)
		// + "surface:" + mSurfaceView.getHeight()
		// + "图片:" + imgHeight);
		matrix.postTranslate(
				(mSurfaceView.getWidth() - imgWidth * scaleWidth) / 2, 0);
	}

	@Override
	protected void onConfigurationChanged(Configuration newConfig) {
		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			// Nothing need to be done here
		} else {
			// Nothing need to be done here
		}
	}

	class RecordPlayThread extends Thread {
		@Override
		public void run() {
			// getVoiceFormFile();
			getVoidceFromRec();
		}
	}

	public void getVoidceFromRec() {
		Log.i(TAG, "准备双向通话");
		sf.mUdpSocket.senAudioSwitch();
		// 获得满足条件的最小缓冲区大小
		// int bufferSizeInBytes = AudioRecord.getMinBufferSize(sampleRateInHz,
		// channelConfig, audioFormat);
		int bufferSizeInBytes = 950;
		// int bufferSizeInBytes = 800;
		ELog.i(TAG, "bufferSizeInBytes:" + bufferSizeInBytes);
		bufferSizeInBytes = AudioRecord.getMinBufferSize(sampleRateInHz,
				channelConfig, audioFormat);
		// int bufferSizeInBytes = 240;
		// 创建AudioRecord对象
		if (bufferSizeInBytes != AudioRecord.ERROR_BAD_VALUE) {
			audioRecord = new AudioRecord(audioSource, sampleRateInHz,
					channelConfig, audioFormat, bufferSizeInBytes);
		}

		try {
			audioRecord.startRecording();
		} catch (IllegalStateException e) {
			ELog.e(TAG, e.getMessage());
		}
		// byte[] buffer = new byte[bufferSizeInBytes];
		short[] buffer = new short[bufferSizeInBytes];
		isRecording = true;

		long begin = System.currentTimeMillis();// 取开始时间 单位是毫秒
		Native.WebRtcAecm_Create();
		Native.WebRtcAecm_Init(8000);
		MobileAEC aecm = new MobileAEC(SamplingFrequency.FS_8000Hz);
		aecm.setAecmMode(MobileAEC.AggressiveMode.MOST_AGGRESSIVE).prepare();
		while (isRecording) {
			int bufferRead = 0;
			long end = System.currentTimeMillis();// 取结束时间

			// int bufferReadResult = audioRecord.read(buffer, 0,
			// bufferSizeInBytes);
			//
			// + bufferReadResult);

			//
			// int bufferReadResult = audioRecord.read(buffer, 0,
			// recBufSize);
			//
			try {
				// System.arraycopy(buffer, 0, tmpBuf, 0, bufferReadResult);
				bufferRead = audioRecord.read(tmpBuf, 0, packagesize);
				// short[] echo = new short[packagesize];
				// mSpeex.process(tmpBuf, echo);

				// aec.putRecordData(tmpBuf, bufferRead);

				// mSpeex.echoplayback(tmpBuf);
				// if (SoundPlay.is_keep_running && null != SoundPlay.outShort)
				// {
				// tmpBuf = mSpeex.process(tmpBuf, SoundPlay.outShort);
				// }
				short[] out = new short[packagesize];
				// Native.WebRtcAecm_Process(tmpBuf, null, out, 8000, 1000);
				if (null != SoundPlay.outShort) {
					aecm.farendBuffer(SoundPlay.outShort, packagesize);
				}

				aecm.echoCancellation(tmpBuf, null, out, (short) packagesize,
						(short) 10);
				byte[] adpcmByte = new byte[tmpBuf.length / 2];
				adpcmByte = AdPcm.encodeAdpcm(out, tmpBuf.length, adpcmByte, 0);
				ELog.i(TAG, "adpcm长度:" + adpcmByte.length);

				// bos.flush();
				if (CameraListInfo.currentCam.isUpnp()) {
					TcpSocket.getInstance().sendVoiceToCam(adpcmByte);
				}else{
					sf.mUdpSocket.sendVoiceToCam(adpcmByte);
				}
		
				// sf.mUdpSocket.sendVoiceToCam(tmpBuf);
				//
			} catch (NegativeArraySizeException e) {
				ELog.i(TAG, "录音错误" + e.getMessage());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		try {
			audioRecord.stop();
		} catch (IllegalStateException e) {
			ELog.i(TAG, "没有初始话录音。。");
		}

	}

	private void initSelectWindow() {
		// ActionItem addItem = new ActionItem(ID_ADD, "高清");
		ActionItem bestItem = new ActionItem(ID_BEST, mActivity.getResources()
				.getString(R.string.select_quality_best));

		ActionItem betterItem = new ActionItem(ID_BETTER, mActivity
				.getResources().getString(R.string.select_quality_better));
		ActionItem goodItem = new ActionItem(ID_GOOD, mActivity.getResources()
				.getString(R.string.select_quality_good));
		mQuickAction = new QuickAction(mActivity);
		ELog.i(TAG, "初始话选择:" + CameraListInfo.currentCam.getVideoSize());
		// mQuickAction.addActionItem(addItem);
		switch (CameraListInfo.currentCam.getVideoSize()) {
		case ID_BEST:
			mQuickAction.addActionItem(bestItem, true);
			mQuickAction.addActionItem(betterItem, false);
			mQuickAction.addActionItem(goodItem, false);
			selectBtn.setText(bestItem.getTitle());
			break;
		case ID_BETTER:
			mQuickAction.addActionItem(bestItem, false);
			mQuickAction.addActionItem(betterItem, true);
			mQuickAction.addActionItem(goodItem, false);
			selectBtn.setText(betterItem.getTitle());
			break;
		case ID_GOOD:
			mQuickAction.addActionItem(bestItem, false);
			mQuickAction.addActionItem(betterItem, false);
			mQuickAction.addActionItem(goodItem, true);
			selectBtn.setText(goodItem.getTitle());
			break;

		default:
			break;
		}

		mQuickAction
				.setOnActionItemClickListener(new QuickAction.OnActionItemClickListener() {

					@Override
					public void onItemClick(QuickAction quickAction, int pos,
							int actionId, View v) {
						ActionItem actionItem = quickAction.getActionItem(pos);
						// View viewCurent = v.findViewById(R.id.select_bg);
						// viewCurent.setBackgroundResource(R.drawable.pop_selected_p);
						// viewCurent.getBackground().setAlpha(255);
						switch (actionId) {
						case ID_BEST:
							// Toast.makeText(mActivity, "Add item selected",
							// Toast.LENGTH_SHORT).show();
							// sf.mod
							// break;
						case ID_BETTER:
						case ID_GOOD:
							// Toast.makeText(mActivity,
							// actionItem.getTitle() + " selected",
							// Toast.LENGTH_SHORT).show();
							CameraListInfo.currentCam.setVideoSize(actionId);
							selectBtn.setText(actionItem.getTitle());
							if (sdlTAG) {
								sf.modifyCamera(CameraListInfo.currentCam,
										sf.mUdpSocket.getChannelId());
							}

							break;

						default:
							break;
						}
						// if (actionId == ID_ADD) {
						// Toast.makeText(mActivity, "Add item selected",
						// Toast.LENGTH_SHORT).show();
						// } else {
						// Toast.makeText(mActivity,
						// actionItem.getTitle() + " selected",
						// Toast.LENGTH_SHORT).show();
						// }
					}
				});

		// mQuickAction.setOnDismissListener(new QuickAction.OnDismissListener()
		// {
		// @Override
		// public void onDismiss() {
		// Toast.makeText(mActivity, "Ups..dismissed", Toast.LENGTH_SHORT)
		// .show();
		// }
		// });
	}

	class DrawImage extends Thread {
		int x, y;

		public DrawImage(int x, int y) {
			this.x = x;
			this.y = y;
		}

		public void run() {
			isDrawBitmap = true;
			int i = 0;
			int iCount =0;
			while (isDrawBitmap) {
				if (bitmap != null) {// 如果图像有效
					// mGlBufferView.setBitmap(bitmap);
					int imgWidth = bitmap.getWidth();
					int imgHeight = bitmap.getHeight();
					Canvas c = surfaceHolder.lockCanvas();
					if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
						// 当前为横屏，在此处添加额外的处理代码
						adaptHeightScreen(imgWidth, imgHeight);

					} else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
						// 当前为竖屏，// 在此处添加额外的处理代码
						adaptWidthScreen(imgWidth, imgHeight);
					}
					// float scaleWidth = ((float) Utils
					// .getWidthPixels(VideoPlayActivity.this) / imgWidth);
					// float scaleHeight = ((float) Utils
					// .getHeightPixels(VideoPlayActivity.this) / imgWidth);
					// c.scale(scaleWidth, scaleHeight);
					// c.setMatrix (matrix);

					// c.drawBitmap(bitmap, this.x, this.y, new Paint());
					Paint paint = new Paint();
					paint.setDither(true);
					 float scaleWidth = ((float) Utils.getWidthPixels(mActivity));
					try {
						// c.drawBitmap(bitmap, matrix, paint);
//						c.drawColor(Color.BLACK);
						c.drawBitmap(bitmap, matrix, paint);
						iCount++;
						if(iCount < 30 && isRecVideoing)
						{
						paint.setColor(Color.RED);
						
						
						float scaleHeight = ((float) Utils .getHeightPixels(mActivity));
						
						if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
							// 当前为横屏，在此处添加额外的处理代码
							c.drawCircle(scaleWidth-50, 50,10, paint);

						} else {
							// 当前为竖屏，// 在此处添加额外的处理代码
							c.drawCircle(scaleWidth-50, 200,10, paint);
						}
					
						Log.i(TAG, "	Color.RED " +iCount );
						//Log.i(TAG, "width ="+scaleWidth+"height = "+scaleHeight);
						}else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
							paint.setColor(Color.BLACK);
							c.drawCircle(scaleWidth-50, 200,10, paint);
						}
						
						if(iCount > 60)
						{
							iCount =0;
						}
						surfaceHolder.unlockCanvasAndPost(c);// 更新屏幕显示内容

					} catch (NullPointerException e) {
						continue;
					}

				} else {
					//
					// try {
					// DrawImage.sleep(1000);
					// } catch (InterruptedException e) {
					//
					// e.printStackTrace();
					// }
					//
					// if (i == 15 && VideoData.audioArraryList.isEmpty()
					// && VideoData.videoArraryList.isEmpty()) {
					// ShowRetryDialog();
					//
					// }
					// i++;
				}
			}
		}
	}

	@Override
	public void changScreenToLand() {
		mHandler.sendEmptyMessage(FADE_OUT);
		mActivity.getWindow().setFlags(
				WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);// 去掉信息栏
		// show(3000);
	}

	@Override
	public void changScreenToPorait() {
		mActivity.getWindow().clearFlags(
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setHead();
		showHead();
		// show(3000);
	}

	@Override
	public void stopRecord() {
		videRec.setImageResource(R.drawable.play_rec_btn);
		isRecVideoing = false;
	}

	@Override
	public void onGesture(GestureOverlayView arg0, MotionEvent arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onGestureCancelled(GestureOverlayView arg0, MotionEvent arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onGestureEnded(GestureOverlayView arg0, MotionEvent arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onGestureStarted(GestureOverlayView arg0, MotionEvent arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setDeviceStatus(int sdcard,int battery) {
				
	}
}

class SDLMain implements Runnable {
	private final String TAG = "SDLMain";

	public void run() {
		// CloudLivingView.initSDL();
		Log.i(TAG, "初始化完成");
		AppServer.isDisplayVideo = true;
		// int width = 352;
		// int height = 288;
		int width = 720;
		int height = 480;
		byte[] PY = new byte[width * height];
		byte[] PV = new byte[(width * height) / 4];
		byte[] PU = new byte[(width * height) / 4];
		// while(true){
		// CloudLivingView.convertBytesToVideo(PY , PU, PV);

		// if (VideoData.yuvArrayList.size()>0) {
		// System.arraycopy(VideoData.yuvArrayList.get(0), 0, PY, 0, width *
		// height);
		// System.arraycopy(VideoData.yuvArrayList.get(0), width * height, PU,
		// 0, width * height/4);
		// System.arraycopy(VideoData.yuvArrayList.get(0), width * height+width
		// * height/4, PV, 0, width * height/4);
		//
		//
		//
		// CloudLivingView.convertBytesToVideo(PY , PU, PV);
		// VideoData.yuvArrayList.remove(0);
		// }

		// }

		// try {
		// BufferedInputStream in = new BufferedInputStream(
		// new FileInputStream("/sdcard/DCIM/show.yuv"));

		// BufferedInputStream in = new BufferedInputStream(
		// new FileInputStream("/sdcard/DCIM/akiyo_qcif.yuv"));
		// int offset = 0;
		// while(true){
		// CloudLivingView.convertBytesToVideo(PY , PV, PU);
		// }
		// if (null != in) {
		// while (-1 != offset) {
		// offset = in.read(PY, 0, width * height);
		// offset = in.read(PV, 0, (width * height) / 4);
		// offset = in.read(PU, 0, (width * height) / 4);
		// Log.i(TAG, "开始转换");
		// CloudLivingView.convertBytesToVideo(PY , PV, PU);
		// Log.i(TAG, "转化完成");
		// }
		// }
		// } catch (FileNotFoundException e) {
		// e.printStackTrace();
		// } catch (IOException e) {
		// e.printStackTrace();
		// }

		// CloudLivingView.destroySDL();

	}
}
