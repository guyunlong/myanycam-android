package com.myanycamm.ui;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

import org.videolan.libvlc.EventHandler;
import org.videolan.libvlc.IVideoPlayer;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.LibVlcException;
import org.videolan.libvlc.LibVlcUtil;
import org.videolan.vlc.AudioServiceController;
import org.videolan.vlc.MediaDatabase;
import org.videolan.vlc.Util;
import org.videolan.vlc.WeakHandler;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.gesture.GestureOverlayView;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings.SettingNotFoundException;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.TextView;

import com.myanycam.net.SocketFunction;
import com.myanycamm.cam.CameraCenterActivity;
import com.myanycamm.cam.R;
import com.myanycamm.utils.ELog;

@SuppressLint("NewApi")
public class LocalLivingView extends LivingView implements IVideoPlayer {
	private final static String TAG = "LocalLivingView";
	private Button playBack;

	// Internal intent identifier to distinguish between internal launch and
	// external intent.
	private final static String PLAY_FROM_VIDEOGRID = "org.videolan.vlc.gui.video.PLAY_FROM_VIDEOGRID";

	private SurfaceView mSurface;
	private SurfaceHolder mSurfaceHolder;
	private FrameLayout mSurfaceFrame;
	private int mSurfaceAlign;
	private LibVLC mLibVLC;
	private String mLocation;

	private static final int SURFACE_BEST_FIT = 0;
	private int mCurrentSize = SURFACE_BEST_FIT;

	
	private View mOverlayHeader;
	private View mOverlayLock;
	private View mOverlayProgress;
	private View mOverlayInterface;
	private static final int OVERLAY_TIMEOUT = 4000;
	private static final int OVERLAY_INFINITE = 3600000;
	private static final int FADE_OUT = 1;
	private static final int SHOW_PROGRESS = 2;
	private static final int SURFACE_SIZE = 3;
	private static final int FADE_OUT_INFO = 4;
	private boolean mDragging;
	private boolean mShowing;
	private int mUiVisibility = -1;
	private TextView mTitle;
	private TextView mSysTime;
	private TextView mBattery;
	private TextView mTime;
	private TextView mInfo;
	private boolean mEnableBrightnessGesture;
	private boolean mDisplayRemainingTime = false;
	private ImageButton mLock;
	private boolean mIsLocked = false;
	private int mLastAudioTrack = -1;
	private int mLastSpuTrack = -2;
	private ImageView playBtn;
	private View content;
	private RelativeLayout mediaControllerLayout;
	private ImageButton speak, photo, sound,videRec;
	boolean isRecVideoing = false;

	private boolean mSwitchingView;
	private boolean mEndReached;
	// Playlist
	private int savedIndexPosition = -1;

	// size of the video
	private int mVideoHeight;
	private int mVideoWidth;
	private int mSarNum;
	private int mSarDen;

	// Volume
	private AudioManager mAudioManager;
	private int mAudioMax;

	// Volume Or Brightness
	private boolean mIsAudioOrBrightnessChanged;
	private int mSurfaceYDisplayRange;
	private float mTouchY, mTouchX, mVol;

	// Brightness
	private boolean mIsFirstBrightnessGesture = true;

	// Tracks & Subtitles
	private Map<Integer, String> mAudioTracksList;
	private Map<Integer, String> mSubtitleTracksList;

	
	private ArrayList<String> mSubtitleSelectedFiles = new ArrayList<String>();
	private OnSystemUiVisibilityChangeListener screenChangeListener = new OnSystemUiVisibilityChangeListener() {

		@Override
		public void onSystemUiVisibilityChange(int visibility) {
			if (visibility == mUiVisibility)
				return;
			setSurfaceSize(mVideoWidth, mVideoHeight, mSarNum, mSarDen);
			if (visibility == View.SYSTEM_UI_FLAG_VISIBLE && !mShowing) {
				ELog.i(TAG, "initVlcView:showOverlay");
				showOverlay();
			}
			mUiVisibility = visibility;
		}
	};
	private OnClickListener playOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			rotImageView.startAnimation(hyperspaceJumpAnimation);
			playBtn.setOnClickListener(null);
			mActivity.sendWatchCam();
			playText.setVisibility(View.GONE);
			playText.startAnimation(AnimationUtils.loadAnimation(
					mActivity, android.R.anim.fade_out));
			
			AudioServiceController c = AudioServiceController.getInstance();
			String s = "rtsp://192.168.42.1/stream2";
//			String s = "http://192.168.1.182/update/aa.mp4";
			c.append(s);		
		}
	};
	
	OnClickListener recVideoOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			ELog.i(TAG, "对讲...");
			if (isRecVideoing) {
				videRec.setImageResource(R.drawable.play_rec_btn);
				isRecVideoing = false;
				SocketFunction.getInstance().manualRecord(0);
//				VideoData.audioArraryList.clear();
//				sf.mUdpSocket.colseSenAudioSwitch();
			} else {
				SocketFunction.getInstance().manualRecord(1);
				isRecVideoing = true;
				videRec.setImageResource(R.drawable.play_rec_btn_on);
//				new RecordPlayThread().start();
			}

		}
	};

	public LocalLivingView(CameraCenterActivity activity, TabHost tabHost) {
		super(activity, tabHost);
		ELog.i(TAG, "LocalLivingView");
		initView();	

	}

	public void initView() {
		LayoutInflater inflater = LayoutInflater.from(mActivity);
		camView = inflater.inflate(R.layout.tab_local_play,
				mTabHost.getTabContentView());
		headTitle = (TextView) camView
				.findViewById(R.id.settings_head_title_play);
		playBack = (Button) camView.findViewById(R.id.settings_back_play);
		playBack.setOnClickListener(playBackOnclClickListener);
		headLayout = (LinearLayout) camView.findViewById(R.id.head_layout);
		rotImageView = (ImageView) camView.findViewById(R.id.rotate_play_img);
		playText = (TextView) camView.findViewById(R.id.play_text);
		playLayout = (RelativeLayout) camView.findViewById(R.id.play_layout);
		mediaControllerLayout = (RelativeLayout) camView
				.findViewById(R.id.mediacontroll);
		speak = (ImageButton) camView.findViewById(R.id.play_speak);
		speak.setVisibility(View.GONE);
		sound = (ImageButton) camView.findViewById(R.id.play_sound);
		sound.setVisibility(View.GONE);
		photo = (ImageButton) camView.findViewById(R.id.play_photo);
		photo.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				SocketFunction.getInstance().manualSnap();
			}
		});
		videRec = (ImageButton) camView.findViewById(R.id.play_rec_btn);
		videRec.setOnClickListener(recVideoOnClickListener);
		mActivity
		.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		initVlcView();
		setHead();
	}

	public void initLoadVlc() {
		AudioServiceController.getInstance().bindAudioService(mActivity);
		// start(VLCApplication.getAppContext(), s, "live",true);
		loadVlcInstance();
		load();

		
		mHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				if (mLibVLC != null && mLibVLC.isPlaying()) {
					KeyguardManager km = (KeyguardManager) mActivity
							.getSystemService(Context.KEYGUARD_SERVICE);
					if (km.inKeyguardRestrictedInputMode())
						mLibVLC.pause();
				}
			}
		}, 500);
//		showOverlay();

		// Add any selected subtitle file from the file picker
		if (mSubtitleSelectedFiles.size() > 0) {
			for (String file : mSubtitleSelectedFiles) {
				Log.i(TAG, "Adding user-selected subtitle " + file);
				mLibVLC.addSubtitleTrack(file);
			}
			
			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					setESTrackLists(true);

					mHandler.postDelayed(new Runnable() {
						@Override
						public void run() {
							setESTrackLists(true);
						}
					}, 1200);
				}
			}, 1000);
		}

	}

	private void loadVlcInstance() {
		if (!LibVlcUtil.hasCompatibleCPU(mActivity)) {
			Log.e(TAG, LibVlcUtil.getErrorMsg());
			// Intent i = new Intent(this, CompatErrorActivity.class);
			// startActivity(i);
			// finish();
			return;
		}

		try {
			// Start LibVLC
			Log.i(TAG, "context:" + SocketFunction.getAppContext());
			Util.getLibVlcInstance();
		} catch (LibVlcException e) {
			e.printStackTrace();
			// Intent i = new Intent(this, CompatErrorActivity.class);
			// i.putExtra("runtimeError", true);
			// i.putExtra("message",
			// "LibVLC failed to initialize (LibVlcException)");
			// startActivity(i);
			// finish();
			return;
		}

	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public void initVlcView() {
		//		if (Util.isICSOrLater()){
//		
//	}		
		content = mActivity
				.getWindow()
				.getDecorView()
				.findViewById(android.R.id.content);			
		content.setOnSystemUiVisibilityChangeListener(null);	

		
		mOverlayHeader = camView.findViewById(R.id.player_overlay_header);
		mOverlayLock = camView.findViewById(R.id.lock_overlay);
		mOverlayLock.setVisibility(View.GONE);
		ELog.i(TAG, "mOverlayLock:" + mOverlayLock);
		mOverlayProgress = camView.findViewById(R.id.progress_overlay);
		mOverlayInterface = camView.findViewById(R.id.interface_overlay);

		
		mTitle = (TextView) camView.findViewById(R.id.player_overlay_title);
		mSysTime = (TextView) camView.findViewById(R.id.player_overlay_systime);
		mBattery = (TextView) camView.findViewById(R.id.player_overlay_battery);
		playBtn =  (ImageView) camView.findViewById(R.id.play_btn);
		playBtn.setOnClickListener(playOnClickListener);
		// Position and remaining time
		mTime = (TextView) camView.findViewById(R.id.player_overlay_time);
//		mTime.setOnClickListener(mRemainingTimeListener);

		// the info textView is not on the overlay
		mInfo = (TextView) camView.findViewById(R.id.player_overlay_info);

		mEnableBrightnessGesture = true;

		mHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				
				setESTrackLists();
			}
		}, 1500);

		mLock = (ImageButton) camView.findViewById(R.id.lock_overlay_button);
		mLock.setOnClickListener(mLockListener);

		mSurface = (SurfaceView) camView.findViewById(R.id.player_surface);
		mSurfaceHolder = mSurface.getHolder();
		mSurfaceFrame = (FrameLayout) camView
				.findViewById(R.id.player_surface_frame);
		int pitch;
		String chroma = "";
		if (Util.isGingerbreadOrLater() && chroma.equals("YV12")) {
			mSurfaceHolder.setFormat(ImageFormat.YV12);
			pitch = ImageFormat.getBitsPerPixel(ImageFormat.YV12) / 8;
		} else if (chroma.equals("RV16")) {
			mSurfaceHolder.setFormat(PixelFormat.RGB_565);
			PixelFormat info = new PixelFormat();
			PixelFormat.getPixelFormatInfo(PixelFormat.RGB_565, info);
			pitch = info.bytesPerPixel;
		} else {
			mSurfaceHolder.setFormat(PixelFormat.RGBX_8888);
			PixelFormat info = new PixelFormat();
			PixelFormat.getPixelFormatInfo(PixelFormat.RGBX_8888, info);
			pitch = info.bytesPerPixel;
		}
		mSurfaceAlign = 16 / pitch - 1;
		mSurfaceHolder.addCallback(mSurfaceCallback);
		mAudioManager = (AudioManager) mActivity
				.getSystemService(Context.AUDIO_SERVICE);
		mAudioMax = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_BATTERY_CHANGED);
		filter.addAction(SocketFunction.SLEEP_INTENT);
		mActivity.registerReceiver(mReceiver, filter);

		try {
			mLibVLC = Util.getLibVlcInstance();
		} catch (LibVlcException e) {
			Log.d(TAG, "LibVLC initialisation failed");
			return;
		}

	}

	public void Playing(){
		content.setOnSystemUiVisibilityChangeListener(screenChangeListener);
		playLayout.setVisibility(View.INVISIBLE);
		playBtn.setOnClickListener(playOnClickListener);
		rotImageView.setAnimation(null);
		showOverlay();
		mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
	}
	
	public static void start(Context context, String location) {
		start(context, location, null, false, false);
	}

	public static void start(Context context, String location, Boolean fromStart) {
		start(context, location, null, false, fromStart);
	}

	public static void start(Context context, String location, String title,
			Boolean dontParse) {
		Log.i(TAG, "location:" + location);
		start(context, location, title, dontParse, false);
	}

	public static void start(Context context, String location, String title,
			Boolean dontParse, Boolean fromStart) {
		Intent intent = new Intent(context, CameraCenterActivity.class);
		intent.setAction(PLAY_FROM_VIDEOGRID);
		intent.putExtra("itemLocation", location);
		intent.putExtra("itemTitle", title);
		intent.putExtra("dontParse", dontParse);
		intent.putExtra("fromStart", fromStart);

		if (dontParse)
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
					| Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
		else {
			// Stop the currently running audio
			AudioServiceController asc = AudioServiceController.getInstance();
			asc.stop();
		}

		context.startActivity(intent);
	}

	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equalsIgnoreCase(Intent.ACTION_BATTERY_CHANGED)) {
				int batteryLevel = intent.getIntExtra("level", 0);
				if (batteryLevel >= 50)
					mBattery.setTextColor(Color.GREEN);
				else if (batteryLevel >= 30)
					mBattery.setTextColor(Color.YELLOW);
				else
					mBattery.setTextColor(Color.RED);
				mBattery.setText(String.format("%d%%", batteryLevel));
			} else if (action.equalsIgnoreCase(SocketFunction.SLEEP_INTENT)) {
				mActivity.finish();
			}
		}
	};

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		setSurfaceSize(mVideoWidth, mVideoHeight, mSarNum, mSarDen);
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public void setSurfaceSize(int width, int height, int sar_num, int sar_den) {
		if (width * height == 0)
			return;

		// store video size
		mVideoHeight = height;
		mVideoWidth = width;
		mSarNum = sar_num;
		mSarDen = sar_den;
		Message msg = mHandler.obtainMessage(SURFACE_SIZE);
		mHandler.sendMessage(msg);
	}

	
	private void lockScreen() {
//		if (mScreenOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR)
			mActivity.setRequestedOrientation(getScreenOrientation());
		showInfo(R.string.locked, 1000);
		mLock.setBackgroundResource(R.drawable.ic_lock_glow);
		mTime.setEnabled(false);
//		hideOverlay(true);
	}

	
	private void unlockScreen() {
//		if (mScreenOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR)
			mActivity
					.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
		showInfo(R.string.unlocked, 1000);
		mLock.setBackgroundResource(R.drawable.ic_lock);
		mTime.setEnabled(true);
		mShowing = false;
		ELog.i(TAG, "unlockScreen:showOverlay");
		showOverlay();
	}

	
	private void showInfo(String text, int duration) {
		Log.i(TAG, "显示:" + text);
		mInfo.setVisibility(View.VISIBLE);
		mInfo.setText(text);
		mHandler.removeMessages(FADE_OUT_INFO);
		mHandler.sendEmptyMessageDelayed(FADE_OUT_INFO, duration);
	}

	private void showInfo(int textid, int duration) {
		mInfo.setVisibility(View.VISIBLE);
		mInfo.setText(textid);
		mHandler.removeMessages(FADE_OUT_INFO);
		mHandler.sendEmptyMessageDelayed(FADE_OUT_INFO, duration);
	}

	
	private void showInfo(String text) {
		mInfo.setVisibility(View.VISIBLE);
		mInfo.setText(text);
		mHandler.removeMessages(FADE_OUT_INFO);
	}

	
	private void hideInfo(int delay) {
		mHandler.sendEmptyMessageDelayed(FADE_OUT_INFO, delay);
	}

	
	private void hideInfo() {
		hideInfo(0);
	}

	private void fadeOutInfo() {
		if (mInfo.getVisibility() == View.VISIBLE)
			mInfo.startAnimation(AnimationUtils.loadAnimation(mActivity,
					android.R.anim.fade_out));
		mInfo.setVisibility(View.INVISIBLE);
	}

	
	public final Handler eventHandler = new VideoPlayerEventHandler(this);

	private static class VideoPlayerEventHandler extends
			WeakHandler<LocalLivingView> {
		public VideoPlayerEventHandler(LocalLivingView owner) {
			super(owner);
		}

		@Override
		public void handleMessage(Message msg) {
			LocalLivingView activity = getOwner();
			if (activity == null)
				return;

			switch (msg.getData().getInt("event")) {
			case EventHandler.MediaPlayerPlaying:
				activity.Playing();
				Log.i(TAG, "MediaPlayerPlaying");
				activity.setESTracks();
				break;
			case EventHandler.MediaPlayerPaused:
				Log.i(TAG, "MediaPlayerPaused");
				break;
			case EventHandler.MediaPlayerStopped:
				Log.i(TAG, "MediaPlayerStopped");
				break;
			case EventHandler.MediaPlayerEndReached:
				Log.i(TAG, "MediaPlayerEndReached");
				activity.endReached();
				break;
			case EventHandler.MediaPlayerVout:
				activity.handleVout(msg);
				break;
			case EventHandler.MediaPlayerPositionChanged:
				// don't spam the logs
				break;
			case EventHandler.MediaPlayerEncounteredError:
				Log.i(TAG, "MediaPlayerEncounteredError");
				break;
			default:
				Log.e(TAG, String.format("Event not handled (0x%x)", msg
						.getData().getInt("event")));
				break;
			}
			activity.updateOverlayPausePlay();
		}
	};

	
	private final Handler mHandler = new VideoPlayerHandler(this);

	private static class VideoPlayerHandler extends
			WeakHandler<LocalLivingView> {
		public VideoPlayerHandler(LocalLivingView owner) {
			super(owner);
		}

		@Override
		public void handleMessage(Message msg) {
			LocalLivingView activity = getOwner();
			if (activity == null)
				return;

			switch (msg.what) {
			case FADE_OUT:
				activity.hideOverlay(false);
				break;
			case SHOW_PROGRESS:
				ELog.i(TAG, "SHOW_PROGRESS...");
				int pos = activity.setOverlayProgress();
				if (activity.canShowProgress()) {
					msg = obtainMessage(SHOW_PROGRESS);
					sendMessageDelayed(msg, 1000 - (pos % 1000));
				}
				break;
			case SURFACE_SIZE:
				activity.changeSurfaceSize();
				break;
			case FADE_OUT_INFO:
				activity.fadeOutInfo();
				break;
			}
		}
	};

	private boolean canShowProgress() {
		return !mDragging && mShowing && mLibVLC.isPlaying();
	}

	private void endReached() {
		
		mEndReached = true;
		mActivity.finish();
	}

	private void handleVout(Message msg) {
		if (msg.getData().getInt("data") == 0 && !mEndReached) {
			
			Log.i(TAG, "Video track lost, switching to audio");
			mSwitchingView = true;
			mActivity.finish();
		}
	}

	private void changeSurfaceSize() {
		// get screen size
		int dw = mActivity.getWindow().getDecorView().getWidth();
		int dh = mActivity.getWindow().getDecorView().getHeight();

		// getWindow().getDecorView() doesn't always take orientation into
		// account, we have to correct the values
		boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
		if (dw > dh && isPortrait || dw < dh && !isPortrait) {
			int d = dw;
			dw = dh;
			dh = d;
		}

		// sanity check
		if (dw * dh == 0 || mVideoWidth * mVideoHeight == 0) {
			Log.e(TAG, "Invalid surface size");
			return;
		}

		// compute the aspect ratio
		double ar, vw;
		double density = (double) mSarNum / (double) mSarDen;
		if (density == 1.0) {
			
			vw = mVideoWidth;
			ar = (double) mVideoWidth / (double) mVideoHeight;
		} else {
			
			vw = mVideoWidth * density;
			ar = vw / mVideoHeight;
		}

		// compute the display aspect ratio
		double dar = (double) dw / (double) dh;

		switch (mCurrentSize) {
		case SURFACE_BEST_FIT:
			if (dar < ar)
				dh = (int) (dw / ar);
			else
				dw = (int) (dh * ar);
			break;

		}

		// align width on 16bytes
		int alignedWidth = (mVideoWidth + mSurfaceAlign) & ~mSurfaceAlign;

		// force surface buffer size
		mSurfaceHolder.setFixedSize(alignedWidth, mVideoHeight);

		// set display size
		android.view.ViewGroup.LayoutParams lp = mSurface.getLayoutParams();
		lp.width = dw * alignedWidth / mVideoWidth;
		lp.height = dh;
		mSurface.setLayoutParams(lp);

		// set frame size (crop if necessary)
		lp = mSurfaceFrame.getLayoutParams();
		lp.width = dw;
		lp.height = dh;
		mSurfaceFrame.setLayoutParams(lp);

		mSurface.invalidate();
	}

	


	private void initBrightnessTouch() {
		float brightnesstemp = 0.01f;
		// Initialize the layoutParams screen brightness
		try {
			brightnesstemp = android.provider.Settings.System.getInt(
					mActivity.getContentResolver(),
					android.provider.Settings.System.SCREEN_BRIGHTNESS) / 255.0f;
		} catch (SettingNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		WindowManager.LayoutParams lp = mActivity.getWindow().getAttributes();
		lp.screenBrightness = brightnesstemp;
		mActivity.getWindow().setAttributes(lp);
		mIsFirstBrightnessGesture = false;
	}


	
	private final OnClickListener mAudioTrackListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			final String[] arrList = new String[mAudioTracksList.size()];
			int i = 0;
			int listPosition = 0;
			for (Map.Entry<Integer, String> entry : mAudioTracksList.entrySet()) {
				arrList[i] = entry.getValue();
				// map the track position to the list position
				if (entry.getKey() == mLibVLC.getAudioTrack())
					listPosition = i;
				i++;
			}
			AlertDialog dialog = new AlertDialog.Builder(mActivity)
					.setSingleChoiceItems(arrList, listPosition,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int listPosition) {
									int trackID = -1;
									// Reverse map search...
									for (Map.Entry<Integer, String> entry : mAudioTracksList
											.entrySet()) {
										if (arrList[listPosition].equals(entry
												.getValue())) {
											trackID = entry.getKey();
											break;
										}
									}
									if (trackID < 0)
										return;

									MediaDatabase
											.getInstance(mActivity)
											.updateMedia(
													mLocation,
													MediaDatabase.mediaColumn.MEDIA_AUDIOTRACK,
													trackID);
									mLibVLC.setAudioTrack(trackID);
									dialog.dismiss();
								}
							}).create();
			dialog.setCanceledOnTouchOutside(true);
			dialog.setOwnerActivity(mActivity);
			dialog.show();
		}
	};

	
	private final OnClickListener mSubtitlesListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			final String[] arrList = new String[mSubtitleTracksList.size()];
			int i = 0;
			int listPosition = 0;
			for (Map.Entry<Integer, String> entry : mSubtitleTracksList
					.entrySet()) {
				arrList[i] = entry.getValue();
				// map the track position to the list position
				if (entry.getKey() == mLibVLC.getSpuTrack())
					listPosition = i;
				i++;
			}

			AlertDialog dialog = new AlertDialog.Builder(mActivity)
					.setSingleChoiceItems(arrList, listPosition,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int listPosition) {
									int trackID = -2;
									// Reverse map search...
									for (Map.Entry<Integer, String> entry : mSubtitleTracksList
											.entrySet()) {
										if (arrList[listPosition].equals(entry
												.getValue())) {
											trackID = entry.getKey();
											break;
										}
									}
									if (trackID < -1)
										return;

									MediaDatabase
											.getInstance(mActivity)
											.updateMedia(
													mLocation,
													MediaDatabase.mediaColumn.MEDIA_SPUTRACK,
													trackID);
									mLibVLC.setSpuTrack(trackID);
									dialog.dismiss();
								}
							}).create();
			dialog.setCanceledOnTouchOutside(true);
			dialog.setOwnerActivity(mActivity);
			dialog.show();
		}
	};

	

	
	private final OnClickListener mLockListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (mIsLocked) {
				mIsLocked = false;
				unlockScreen();
			} else {
				mIsLocked = true;
				lockScreen();
			}
		}
	};

	

	// private final OnClickListener mRemainingTimeListener = new
	// OnClickListener() {
	// @Override
	// public void onClick(View v) {
	// Log.i(TAG, "显示时间");
	// mDisplayRemainingTime = !mDisplayRemainingTime;
	// showOverlay();
	// }
	// };

	
	private final SurfaceHolder.Callback mSurfaceCallback = new Callback() {
		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width,
				int height) {
			if (format == PixelFormat.RGBX_8888)
				Log.d(TAG, "Pixel format is RGBX_8888");
			else if (format == PixelFormat.RGB_565)
				Log.d(TAG, "Pixel format is RGB_565");
			else if (format == ImageFormat.YV12)
				Log.d(TAG, "Pixel format is YV12");
			else
				Log.d(TAG, "Pixel format is other/unknown");
			mLibVLC.attachSurface(holder.getSurface(), LocalLivingView.this,
					width, height);
		}

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			mLibVLC.detachSurface();
		}
	};

	
	private void showOverlay() {
		showOverlay(OVERLAY_TIMEOUT);
	}

	
	private void showOverlay(int timeout) {
		ELog.i(TAG, "showOverlay显示...");
		mHandler.sendEmptyMessage(SHOW_PROGRESS);
		if (!mShowing) {
			mShowing = true;
			mOverlayLock.setVisibility(View.VISIBLE);
//			if (!mIsLocked) {
			if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
				mOverlayHeader.setVisibility(View.VISIBLE);			
			}
			mediaControllerLayout.setVisibility(View.VISIBLE);
//			mOverlayInterface.setVisibility(View.VISIBLE);
//				dimStatusBar(false);
//			}
//			mOverlayProgress.setVisibility(View.VISIBLE);
		}
		Message msg = mHandler.obtainMessage(FADE_OUT);
		if (timeout != 0) {
			mHandler.removeMessages(FADE_OUT);
			mHandler.sendMessageDelayed(msg, timeout);
		}
		updateOverlayPausePlay();
	}

	
	private void hideOverlay(boolean fromUser) {
		if (mShowing) {
			mHandler.removeMessages(SHOW_PROGRESS);
			Log.i(TAG, "remove View!");
			if (!fromUser && !mIsLocked) {
				mOverlayLock.startAnimation(AnimationUtils.loadAnimation(
						mActivity, android.R.anim.fade_out));		

//				mOverlayProgress.startAnimation(AnimationUtils.loadAnimation(
//						mActivity, android.R.anim.fade_out));
//				mOverlayInterface.startAnimation(AnimationUtils.loadAnimation(
//						mActivity, android.R.anim.fade_out));
			}
			if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
				mOverlayHeader.startAnimation(AnimationUtils.loadAnimation(
						mActivity, android.R.anim.fade_out));
				mOverlayHeader.setVisibility(View.INVISIBLE);
//				headLayout.startAnimation(AnimationUtils.loadAnimation(
//						mActivity, android.R.anim.fade_out));
//				headLayout.setVisibility(View.INVISIBLE);	
			}
			mediaControllerLayout.setVisibility(View.INVISIBLE);
			mOverlayLock.setVisibility(View.INVISIBLE);	
			mOverlayProgress.setVisibility(View.INVISIBLE);
			mOverlayInterface.setVisibility(View.INVISIBLE);		
			mShowing = false;
			dimStatusBar(true);
		}
	}

	
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	private void dimStatusBar(boolean dim) {
		if (!Util.isHoneycombOrLater() || !Util.hasNavBar())
			return;
		mSurface.setSystemUiVisibility(dim ? (Util.hasCombBar() ? View.SYSTEM_UI_FLAG_LOW_PROFILE
				: View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
				: View.SYSTEM_UI_FLAG_VISIBLE);
	}

	private void updateOverlayPausePlay() {
		if (mLibVLC == null) {
			return;
		}

	}

	
	private int setOverlayProgress() {
		if (mLibVLC == null) {
			return 0;
		}
		int time = (int) mLibVLC.getTime();
		mSysTime.setText(DateFormat.getTimeFormat(mActivity).format(
				new Date(System.currentTimeMillis())));
		Log.i(TAG, "设置时间:");
		if (time >= 0)
			mTime.setText(Util.millisToString(time));

		return time;
	}

	private void setESTracks() {
		if (mLastAudioTrack >= 0) {
			mLibVLC.setAudioTrack(mLastAudioTrack);
			mLastAudioTrack = -1;
		}
		if (mLastSpuTrack >= -1) {
			mLibVLC.setSpuTrack(mLastSpuTrack);
			mLastSpuTrack = -2;
		}
	}

	private void setESTrackLists() {
		setESTrackLists(false);
	}

	private void setESTrackLists(boolean force) {
		if (mAudioTracksList == null || force) {

		}
		if (mSubtitleTracksList == null || force) {
		}
	}

	
	private void play() {
		mLibVLC.play();
		mSurface.setKeepScreenOn(true);
	}

	
	private void pause() {
		mLibVLC.pause();
		mSurface.setKeepScreenOn(false);
	}

	
	@SuppressWarnings({ "deprecation", "unchecked" })
	private void load() {
		mLocation = null;
		String title = mActivity.getString(R.string.local_title);
		boolean dontParse = false;
		boolean fromStart = false;
		String itemTitle = null;
		long intentPosition = -1;

		mSurface.setKeepScreenOn(true);
		EventHandler em = EventHandler.getInstance();
		em.addHandler(eventHandler);

		mActivity.setVolumeControlStream(AudioManager.STREAM_MUSIC);

		// 100 is the value for screen_orientation_start_lock


		
		if (savedIndexPosition > -1) {
			mLibVLC.playIndex(savedIndexPosition);
		} else if (mLocation != null && mLocation.length() > 0 && !dontParse) {
			savedIndexPosition = mLibVLC.readMedia(mLocation, false);
		}

		if (mLocation != null && mLocation.length() > 0 && !dontParse) {

		} else if (itemTitle != null) {
			title = itemTitle;
		}
		Log.i(TAG, "title:" + title);
		mTitle.setText(title);
	}

	@SuppressWarnings("deprecation")
	private int getScreenRotation() {
		WindowManager wm = (WindowManager) mActivity
				.getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO ) {
			try {
				Method m = display.getClass().getDeclaredMethod("getRotation");
				return (Integer) m.invoke(display);
			} catch (Exception e) {
				return Surface.ROTATION_0;
			}
		} else {
			return display.getOrientation();
		}
	}

	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	private int getScreenOrientation() {
		switch (getScreenRotation()) {
		case Surface.ROTATION_0:
			return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
		case Surface.ROTATION_90:
			return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
		case Surface.ROTATION_180:
			// SCREEN_ORIENTATION_REVERSE_PORTRAIT only available since API
			// Level 9+
			return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO ? ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
					: ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		case Surface.ROTATION_270:
			// SCREEN_ORIENTATION_REVERSE_LANDSCAPE only available since API
			// Level 9+
			return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO ? ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
					: ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		default:
			return 0;
		}
	}

	@Override
	public void showHead() {	
		headLayout.setVisibility(View.VISIBLE);
		mOverlayHeader.setVisibility(View.INVISIBLE);
	}
	public void setChangeScrenn(){
		setSurfaceSize(mVideoWidth, mVideoHeight, mSarNum, mSarDen);
	}

	@Override
	public void stopCam() {
	        if (mLibVLC != null) {
	            mLibVLC.stop();
	        }
	        EventHandler em = EventHandler.getInstance();
	        em.removeHandler(eventHandler);
	        mAudioManager = null;
	        playLayout.setVisibility(View.VISIBLE);
	        content.setOnSystemUiVisibilityChangeListener(null);
	        mActivity.stopWatchCam();
	}

	@Override
	public void initCam() {
		// TODO Auto-generated method stub

	}

	@Override
	public void show(int timeout) {	
		showOverlay(timeout);
	}

	@Override
	public void setHead() {
		playBack.setVisibility(View.VISIBLE);
		headTitle.setText(mActivity.getString(R.string.local_title));
		initLoadVlc();
	}
	
	@Override
	public void changScreenToLand() {
		show(3000);
		headLayout.setVisibility(View.INVISIBLE);
		mOverlayHeader.setVisibility(View.VISIBLE);
		setChangeScrenn();
		mActivity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);//去掉信息栏
	}
	
	@Override
	public void changScreenToPorait() {
		show(3000);
		setChangeScrenn();
		showHead();		
		setHead();
		mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN); 
	}

	@Override
	public void showCamMaxDialog() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void stopRecord() {
		videRec.setImageResource(R.drawable.play_rec_btn);
		isRecVideoing = false;
	}

	@Override
	public void onGesture(GestureOverlayView overlay, MotionEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onGestureCancelled(GestureOverlayView overlay, MotionEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onGestureEnded(GestureOverlayView overlay, MotionEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onGestureStarted(GestureOverlayView overlay, MotionEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setDeviceStatus(int sdcard,int battery) {
		// TODO Auto-generated method stub
		
	}

}
