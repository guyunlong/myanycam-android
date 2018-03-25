package com.myanycamm.cam;

import java.lang.reflect.Method;
import java.util.ArrayList;
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
import org.videolan.vlc.interfaces.IPlayerControl;
import org.videolan.vlc.interfaces.OnPlayerControlListener;
import org.videolan.vlc.widget.PlayerControlClassic;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnSystemUiVisibilityChangeListener;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.myanycam.net.SocketFunction;
import com.myanycamm.utils.ELog;

public class VLCPlayActivity extends BaseActivity implements IVideoPlayer {

	private final static String TAG = "VLCPlayActivity";
	private Button playBack;

	// Internal intent identifier to distinguish between internal launch and
	// external intent.
	private final static String PLAY_FROM_VIDEOGRID = "org.videolan.vlc.gui.video.PLAY_FROM_VIDEOGRID";
	ImageView rotImageView;
	Animation hyperspaceJumpAnimation;
	RelativeLayout playLayout;

	private SurfaceView mSurface;
	private SurfaceHolder mSurfaceHolder;
	private FrameLayout mSurfaceFrame;
	private int mSurfaceAlign;
	private LibVLC mLibVLC;
	private String mLocation;
	private IPlayerControl mControls;

	private static final int SURFACE_BEST_FIT = 0;
	private int mCurrentSize = SURFACE_BEST_FIT;
	private SeekBar mSeekbar;

	
	private View mOverlayHeader;
	private View mOverlayInterface;
	private static final int OVERLAY_TIMEOUT = 4000;
	private static final int OVERLAY_INFINITE = 3600000;
	private static final int FADE_OUT = 1;
	private static final int SHOW_PROGRESS = 2;
	private static final int SURFACE_SIZE = 3;
	private static final int FADE_OUT_INFO = 4;
	private static final int PLAY = 5;
	
	private boolean mDragging;
	private boolean mShowing;
	private int mUiVisibility = -1;
	private TextView mTime;
	private TextView mInfo;
	private TextView mLength;
	private boolean mEnableBrightnessGesture;
	private boolean mDisplayRemainingTime = false;
	private int mLastAudioTrack = -1;
	private int mLastSpuTrack = -2;
	private ImageView playBtn;
	private View content;
	static String url;

	
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
	private ImageButton downVideo;
	@SuppressLint("NewApi")
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

	
	private final OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			mDragging = true;
			showOverlay(OVERLAY_INFINITE);
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			mDragging = false;
			showOverlay();
			hideInfo();
		}

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			if (fromUser) {
				mLibVLC.setTime(progress);
				setOverlayProgress();
				mTime.setText(Util.millisToString(progress));
				showInfo(Util.millisToString(progress));
			}

		}
	};

	
	private final OnPlayerControlListener mPlayerControlListener = new OnPlayerControlListener() {
		@Override
		public void onPlayPause() {
			if (mLibVLC.isPlaying())
				pause();
			else
				play();
			showOverlay();
		}

		@Override
		public void onSeek(int delta) {
			// unseekable stream
			if (mLibVLC.getLength() <= 0)
				return;

			long position = mLibVLC.getTime() + delta;
			if (position < 0)
				position = 0;
			mLibVLC.setTime(position);
			showOverlay();
		}

		@Override
		public void onSeekTo(long position) {
			// unseekable stream
			if (mLibVLC.getLength() <= 0)
				return;
			mLibVLC.setTime(position);
			mTime.setText(Util.millisToString(position));
		}

		@Override
		public long onWheelStart() {
			showOverlay(OVERLAY_INFINITE);
			return mLibVLC.getTime();
		}

		@Override
		public void onShowInfo(String info) {
			if (info != null)
				showInfo(info);
			else {
				hideInfo();
				showOverlay();
			}
		}
	};
	OnClickListener playBackOnclClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			finish();
		}
	};

	private final OnClickListener mRemainingTimeListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			mDisplayRemainingTime = !mDisplayRemainingTime;
			showOverlay();
		}
	};

	private OnClickListener playOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			rotImageView.startAnimation(hyperspaceJumpAnimation);
			playBtn.setOnClickListener(null);
			AudioServiceController c = AudioServiceController.getInstance();
			// String s = "rtsp://192.168.42.1/stream2";
			String s = "http://192.168.1.105/update/aa.mp4";
			c.append(s);
		}
	};

	private OnClickListener downLoadClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			ELog.i(TAG, "点击了下载按钮");
			try {
				VideoDownLoad.getInstance().downloadApkFile(url, null);
			} catch (Exception e) {
				ELog.i(TAG, "下载视频有错误..."+e.getMessage());
				e.printStackTrace();
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_vlc_play);
		url = getIntent().getStringExtra("url");
		ELog.i(TAG, "创建url：" + url);
		// Uri uri = Uri.parse(url);
		initView();
		mHandler.sendEmptyMessageDelayed(5, 1000);
		rotImageView.startAnimation(hyperspaceJumpAnimation);
		// AudioServiceController c = AudioServiceController.getInstance();
		// String s = "rtsp://192.168.42.1/stream2";
		// String s = "http://192.168.1.105/update/aa.mp4";
		// c.append(s);
	}

	@Override
	public void finish() {
		// stopCam();
		AudioServiceController asc = AudioServiceController.getInstance();
		asc.stop();
		mLibVLC.stop();
		super.finish();
	}

	public void initView() {
		hyperspaceJumpAnimation = AnimationUtils.loadAnimation(
				VLCPlayActivity.this, R.anim.loading_animation);
		playBack = (Button) findViewById(R.id.settings_back_play);
		playBack.setOnClickListener(playBackOnclClickListener);
		rotImageView = (ImageView) findViewById(R.id.rotate_play_img);
		playLayout = (RelativeLayout) findViewById(R.id.play_layout);
		mSeekbar = (SeekBar) findViewById(R.id.player_overlay_seekbar);
		mSeekbar.setOnSeekBarChangeListener(mSeekListener);
		mControls = new PlayerControlClassic(this);
		mControls.setOnPlayerControlListener(mPlayerControlListener);
//		downVideo = (ImageButton) findViewById(R.id.down_video);
//		downVideo.setVisibility(View.GONE);
//		downVideo.setOnClickListener(downLoadClickListener);
	
		FrameLayout mControlContainer = (FrameLayout) findViewById(R.id.player_control);
		mControlContainer.addView((View) mControls);
		// setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		initVlcView();
		setHead();

	}

	public void initLoadVlc() {
		AudioServiceController.getInstance().bindAudioService(
				VLCPlayActivity.this);
		// start(VLCApplication.getAppContext(), s, "live",true);
		loadVlcInstance();
		load();

		
		mHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				if (mLibVLC != null && mLibVLC.isPlaying()) {
					KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
					if (km.inKeyguardRestrictedInputMode())
						mLibVLC.pause();
				}
			}
		}, 500);
		// showOverlay();

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
		if (!LibVlcUtil.hasCompatibleCPU(VLCPlayActivity.this)) {
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
		// if (Util.isICSOrLater()){
		//
		// }
		try {
			mLibVLC = Util.getLibVlcInstance();
		} catch (LibVlcException e) {
			Log.d(TAG, "LibVLC initialisation failed");
			return;
		}
		content = getWindow().getDecorView().findViewById(android.R.id.content);
		content.setOnSystemUiVisibilityChangeListener(null);

		
		mOverlayHeader = findViewById(R.id.player_overlay_header);
		mOverlayInterface = findViewById(R.id.interface_overlay);

		

		playBtn = (ImageView) findViewById(R.id.play_btn);
		// playBtn.setOnClickListener(playOnClickListener);
		// Position and remaining time
		mTime = (TextView) findViewById(R.id.player_overlay_time);
		mTime.setOnClickListener(mRemainingTimeListener);
		mLength = (TextView) findViewById(R.id.player_overlay_length);
		mLength.setOnClickListener(mRemainingTimeListener);

		// the info textView is not on the overlay
		mInfo = (TextView) findViewById(R.id.player_overlay_info);

		mEnableBrightnessGesture = true;

		mHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				
				setESTrackLists();
			}
		}, 1500);

		mSurface = (SurfaceView) findViewById(R.id.player_surface);
		mSurfaceHolder = mSurface.getHolder();
		mSurfaceFrame = (FrameLayout) findViewById(R.id.player_surface_frame);
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
		mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		mAudioMax = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

	}

	@SuppressLint("NewApi")
	public void Playing() {
		content.setOnSystemUiVisibilityChangeListener(screenChangeListener);
		playLayout.setVisibility(View.INVISIBLE);
		rotImageView.setAnimation(null);
		showOverlay();

		// setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
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
			mInfo.startAnimation(AnimationUtils.loadAnimation(
					VLCPlayActivity.this, android.R.anim.fade_out));
		mInfo.setVisibility(View.INVISIBLE);
	}

	
	public final Handler eventHandler = new VideoPlayerEventHandler(this);

	private static class VideoPlayerEventHandler extends
			WeakHandler<VLCPlayActivity> {
		public VideoPlayerEventHandler(VLCPlayActivity owner) {
			super(owner);
		}

		@Override
		public void handleMessage(Message msg) {
			VLCPlayActivity activity = getOwner();
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
			WeakHandler<VLCPlayActivity> {
		public VideoPlayerHandler(VLCPlayActivity owner) {
			super(owner);
		}

		@Override
		public void handleMessage(Message msg) {
			VLCPlayActivity activity = getOwner();
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
			case PLAY:
				AudioServiceController c = AudioServiceController.getInstance();
				// String s = "rtsp://192.168.42.1/stream2";
				// String s = "http://192.168.1.105/update/aa.mp4";
				ELog.i(TAG, "播放:" + url);
				c.append(url);
				break;
	
			}
		}
	};

	private boolean canShowProgress() {
		return !mDragging && mShowing && mLibVLC.isPlaying();
	}

	private void endReached() {
		
		mEndReached = true;
		finish();
	}

	private void handleVout(Message msg) {
		if (msg.getData().getInt("data") == 0 && !mEndReached) {
			
			Log.i(TAG, "Video track lost, switching to audio");
			mSwitchingView = true;
			finish();
		}
	}

	private void changeSurfaceSize() {
		// get screen size
		int dw = getWindow().getDecorView().getWidth();
		int dh = getWindow().getDecorView().getHeight();

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
					getContentResolver(),
					android.provider.Settings.System.SCREEN_BRIGHTNESS) / 255.0f;
		} catch (SettingNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		WindowManager.LayoutParams lp = getWindow().getAttributes();
		lp.screenBrightness = brightnesstemp;
		getWindow().setAttributes(lp);
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
			AlertDialog dialog = new AlertDialog.Builder(VLCPlayActivity.this)
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
											.getInstance(VLCPlayActivity.this)
											.updateMedia(
													mLocation,
													MediaDatabase.mediaColumn.MEDIA_AUDIOTRACK,
													trackID);
									mLibVLC.setAudioTrack(trackID);
									dialog.dismiss();
								}
							}).create();
			dialog.setCanceledOnTouchOutside(true);
			dialog.setOwnerActivity(VLCPlayActivity.this);
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

			AlertDialog dialog = new AlertDialog.Builder(VLCPlayActivity.this)
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
											.getInstance(VLCPlayActivity.this)
											.updateMedia(
													mLocation,
													MediaDatabase.mediaColumn.MEDIA_SPUTRACK,
													trackID);
									mLibVLC.setSpuTrack(trackID);
									dialog.dismiss();
								}
							}).create();
			dialog.setCanceledOnTouchOutside(true);
			dialog.setOwnerActivity(VLCPlayActivity.this);
			dialog.show();
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
			mLibVLC.attachSurface(holder.getSurface(), VLCPlayActivity.this,
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
			// if (!mIsLocked) {
			if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
				mOverlayHeader.setVisibility(View.VISIBLE);
			}
			mOverlayInterface.setVisibility(View.VISIBLE);
			dimStatusBar(false);
			// }
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
			if (!fromUser) {

				mOverlayInterface.startAnimation(AnimationUtils.loadAnimation(
						VLCPlayActivity.this, android.R.anim.fade_out));
			}
			if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
				mOverlayHeader.startAnimation(AnimationUtils.loadAnimation(
						VLCPlayActivity.this, android.R.anim.fade_out));
				mOverlayHeader.setVisibility(View.INVISIBLE);
				// headLayout.startAnimation(AnimationUtils.loadAnimation(
				// mActivity, android.R.anim.fade_out));
				// headLayout.setVisibility(View.INVISIBLE);
			}
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
		mControls.setState(mLibVLC.isPlaying());// 设置播放或者暂停图标
	}

	
	private int setOverlayProgress() {
		if (mLibVLC == null) {
			return 0;
		}
		int time = (int) mLibVLC.getTime();
		int length = (int) mLibVLC.getLength();

		// Update all view elements
		mControls.setSeekable(length > 0);
		mSeekbar.setMax(length);
		mSeekbar.setProgress(time);

		if (time >= 0)
			mTime.setText(Util.millisToString(time));
		if (length >= 0)
			mLength.setText(mDisplayRemainingTime && length > 0 ? "- "
					+ Util.millisToString(length - time) : Util
					.millisToString(length));

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
		String title = getString(R.string.local_title);
		boolean dontParse = false;
		boolean fromStart = false;
		String itemTitle = null;
		long intentPosition = -1;

		mSurface.setKeepScreenOn(true);
		EventHandler em = EventHandler.getInstance();
		em.addHandler(eventHandler);

		setVolumeControlStream(AudioManager.STREAM_MUSIC);

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
	}

	@SuppressWarnings("deprecation")
	private int getScreenRotation() {
		WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
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

	public void showHead() {
		mOverlayHeader.setVisibility(View.INVISIBLE);
	}

	public void setChangeScrenn() {
		setSurfaceSize(mVideoWidth, mVideoHeight, mSarNum, mSarDen);
	}

	@SuppressLint("NewApi")
	public void stopCam() {
		if (mLibVLC != null && !mSwitchingView) {
			mLibVLC.stop();
		}
		EventHandler em = EventHandler.getInstance();
		em.removeHandler(eventHandler);
		mAudioManager = null;
		playLayout.setVisibility(View.VISIBLE);
		content.setOnSystemUiVisibilityChangeListener(null);
	}

	public void initCam() {
		// TODO Auto-generated method stub

	}

	public void show(int timeout) {
		showOverlay(timeout);
	}

	public void setHead() {
		playBack.setVisibility(View.VISIBLE);
		initLoadVlc();
	}

	public void changScreenToLand() {
		show(3000);
		mOverlayHeader.setVisibility(View.VISIBLE);
		setChangeScrenn();
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);// 去掉信息栏
	}

	public void changScreenToPorait() {
		show(3000);
		setChangeScrenn();
		showHead();
		setHead();
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
	}

	public void showCamMaxDialog() {
		// TODO Auto-generated method stub

	}




}
