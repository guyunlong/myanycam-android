package com.myanycamm.cam;

import java.util.LinkedList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue.IdleHandler;
import android.provider.MediaStore;
import android.view.Display;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.myanycamm.ui.SoundView;
import com.myanycamm.ui.VideoView;
import com.myanycamm.ui.SoundView.OnVolumeChangedListener;
import com.myanycamm.ui.VideoView.MySizeChangeLinstener;
import com.myanycamm.utils.ELog;

public class EventVideoPlayActivity extends BaseActivity {

	private final static String TAG = "EventVideoPlayActivity";
	private boolean isOnline = false;
	private boolean isChangedVideo = false;
	private RelativeLayout playLayout;
	private ImageView rotImageView;

	public static LinkedList<MovieInfo> playList = new LinkedList<MovieInfo>();

	public class MovieInfo {
		String displayName;
		String path;
	}

	private Uri videoListUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
	private static int position;
	private static boolean backFromAD = false;
	private int playedTime;
	private VideoView vv = null;
	private SeekBar seekBar = null;
	private TextView durationTextView = null;
	private TextView playedTextView = null;
	private AudioManager mAudioManager = null;

	private int maxVolume = 0;
	private int currentVolume = 0;

	private ImageButton playPauseBtn = null;
	private ImageButton SoundBtn = null;

	private View controlView = null;
	private PopupWindow controler = null;

	private SoundView mSoundView = null;
	private PopupWindow mSoundWindow = null;

	private View extralView = null;
	private PopupWindow extralWindow = null;

	private static int screenWidth = 0;
	private static int screenHeight = 0;
	private static int controlHeight = 0;

	private final static int TIME = 6868;

	private boolean isControllerShow = true;
	private boolean isPaused = false;
	private boolean isFullScreen = false;
	private boolean isSilent = false;
	private boolean isSoundShow = false;

	private OnErrorListener playErrorListener = new OnErrorListener() {

		@Override
		public boolean onError(MediaPlayer mp, int what, int extra) {

			ELog.i(TAG, "error:" + extra);
			switch (extra) {
			case -1004:
				showErrorDialog(getString(R.string.net_error));
				break;
			case -111:
				showErrorDialog(getString(R.string.cannot_connect_server));
			break;
			case -110:
				showErrorDialog(getString(R.string.video_time_out));
				break;
			case -2147483648:
				showErrorDialog(getString(R.string.no_coder));
				break;
			default:
				showErrorDialog(getString(R.string.video_error));
				break;
			}
			vv.stopPlayback();
			isOnline = false;
			return false;
		}

	};

	private MySizeChangeLinstener mySizeChangeLinstener = new MySizeChangeLinstener() {

		@Override
		public void doMyThings() {
			// TODO Auto-generated method stub
			ELog.i(TAG, "尺寸改变了");
			playLayout.setVisibility(View.GONE);
			showMediaContoller();
			setVideoScale(SCREEN_FULL);
		}
	};

	private OnClickListener playPauseClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {

			// TODO Auto-generated method stub
			cancelDelayHide();
			if (isPaused) {
				vv.start();
				playPauseBtn.setImageResource(R.drawable.pause);
				hideControllerDelay();
			} else {
				vv.pause();
				playPauseBtn.setImageResource(R.drawable.play);
			}
			isPaused = !isPaused;

		}
	};

	private OnClickListener soundClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {

			// TODO Auto-generated method stub
			cancelDelayHide();
			if (isSoundShow) {
				mSoundWindow.dismiss();
			} else {
				if (mSoundWindow.isShowing()) {
					mSoundWindow.update(15, 40, SoundView.ALL_WIDTH,
							SoundView.ALL_HEIGHT);
				} else {
					mSoundWindow.showAtLocation(vv, Gravity.RIGHT
							| Gravity.CENTER_VERTICAL, 15, 40);
					mSoundWindow.update(15, 40, SoundView.ALL_WIDTH,
							SoundView.ALL_HEIGHT);
				}
			}
			isSoundShow = !isSoundShow;
			hideControllerDelay();

		}
	};

	private OnLongClickListener soundOnLongClickListener = new OnLongClickListener() {

		@Override
		public boolean onLongClick(View arg0) {
			// TODO Auto-generated method stub
			if (isSilent) {
				SoundBtn.setImageResource(R.drawable.soundenable);
			} else {
				SoundBtn.setImageResource(R.drawable.sounddisable);
			}
			isSilent = !isSilent;
			updateVolume(currentVolume);
			cancelDelayHide();
			hideControllerDelay();
			return true;
		}

	};
	private OnVolumeChangedListener soundOnvChangedListener = new OnVolumeChangedListener() {

		@Override
		public void setYourVolume(int index) {
			cancelDelayHide();
			updateVolume(index);
			hideControllerDelay();
		}
	};

	private OnSeekBarChangeListener seekBarChangeListener = new OnSeekBarChangeListener() {

		@Override
		public void onProgressChanged(SeekBar seekbar, int progress,
				boolean fromUser) {
			// TODO Auto-generated method stub

			if (fromUser) {
				ELog.i(TAG, "移动了seekbar");
				// if(!isOnline){
				ELog.i(TAG, "准备seekToseekTo:" + progress);
				vv.seekTo(progress);
				// }

			}

		}

		@Override
		public void onStartTrackingTouch(SeekBar arg0) {
			// TODO Auto-generated method stub
			myHandler.removeMessages(HIDE_CONTROLER);
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			// TODO Auto-generated method stub
			myHandler.sendEmptyMessageDelayed(HIDE_CONTROLER, TIME);
		}
	};

	private GestureDetector mGestureDetector = new GestureDetector(
			new SimpleOnGestureListener() {

				@Override
				public boolean onDoubleTap(MotionEvent e) {
					// TODO Auto-generated method stub
					if (isFullScreen) {
						setVideoScale(SCREEN_DEFAULT);
					} else {
						setVideoScale(SCREEN_FULL);
					}
					isFullScreen = !isFullScreen;
					ELog.d(TAG, "onDoubleTap");

					if (isControllerShow) {
						showController();
					}
					// return super.onDoubleTap(e);
					return true;
				}

				@Override
				public boolean onSingleTapConfirmed(MotionEvent e) {
					// TODO Auto-generated method stub
					if (!isControllerShow) {
						showController();
						hideControllerDelay();
					} else {
						cancelDelayHide();
						hideController();
					}
					// return super.onSingleTapConfirmed(e);
					return true;
				}

				@Override
				public void onLongPress(MotionEvent e) {
					// TODO Auto-generated method stub
					if (isPaused) {
						vv.start();
						playPauseBtn.setImageResource(R.drawable.pause);
						cancelDelayHide();
						hideControllerDelay();
					} else {
						vv.pause();
						playPauseBtn.setImageResource(R.drawable.play);
						cancelDelayHide();
						showController();
					}
					isPaused = !isPaused;
					// super.onLongPress(e);
				}
			});

	private OnPreparedListener mOnPreparedListener = new OnPreparedListener() {

		@Override
		public void onPrepared(MediaPlayer arg0) {
			// TODO Auto-generated method stub

			setVideoScale(SCREEN_FULL);
			isFullScreen = false;
			if (isControllerShow) {
				showController();
			}

			int i = vv.getDuration();
			ELog.d("onCompletion", "" + i);
			seekBar.setMax(i);
			i /= 1000;
			int minute = i / 60;
			int hour = minute / 60;
			int second = i % 60;
			minute %= 60;
			durationTextView.setText(String.format("%02d:%02d", hour,
					hour*60+minute, second));

			

			vv.start();
			playPauseBtn.setImageResource(R.drawable.pause);
			hideControllerDelay();
			myHandler.sendEmptyMessage(PROGRESS_CHANGED);
		}
	};

	private OnCompletionListener mOnCompletionListener = new OnCompletionListener() {

		@Override
		public void onCompletion(MediaPlayer arg0) {
			// TODO Auto-generated method stub
			int n = playList.size();
			isOnline = false;
			if (++position < n) {
				vv.setVideoPath(playList.get(position).path);
			} else {
				vv.stopPlayback();
				EventVideoPlayActivity.this.finish();
			}
		}
	};

	private IdleHandler mIdleHandler = new IdleHandler() {

		@Override
		public boolean queueIdle() {

			// TODO Auto-generated method stub
			if (controler != null && vv.isShown()) {
				controler.showAtLocation(vv, Gravity.BOTTOM, 0, 0);
				// controler.update(screenWidth, controlHeight);
				controler.update(0, 0, screenWidth, controlHeight);
			}

			if (extralWindow != null && vv.isShown()) {
				extralWindow.showAtLocation(vv, Gravity.TOP, 0, 0);
				extralWindow.update(0, 25, screenWidth, 60);
			}

			// myHandler.sendEmptyMessageDelayed(HIDE_CONTROLER, TIME);
			return false;
		}

	};

	
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.event_play);
		ELog.i(TAG, "到了..回看视频界面...");
		initView();
//		showMediaContoller();
		String url = getIntent().getStringExtra("url");
		Uri uri = Uri.parse(url);
		ELog.i(TAG, "播放url:" + uri);
		if (uri != null) {
			vv.stopPlayback();
			vv.setVideoURI(uri);
			isOnline = true;
			playPauseBtn.setImageResource(R.drawable.pause);
		} else {
			playPauseBtn.setImageResource(R.drawable.play);
		}
		mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		currentVolume = mAudioManager
				.getStreamVolume(AudioManager.STREAM_MUSIC);
		getScreenSize();
	}
	
	@Override
	public void onBackPressed() {
		ELog.i(TAG, "按了返回键...");
		this.finish();
		super.onBackPressed();
	}

	private void initView() {
		playLayout = (RelativeLayout) findViewById(R.id.play_layout);
		rotImageView = (ImageView) findViewById(R.id.rotate_play_img);
		// 加载动画
		Animation hyperspaceJumpAnimation = AnimationUtils.loadAnimation(
				EventVideoPlayActivity.this, R.anim.loading_animation);
		// 使用ImageView显示动画
		rotImageView.startAnimation(hyperspaceJumpAnimation);
//		playLayout.setVisibility(View.GONE);
		vv = (VideoView) findViewById(R.id.vv);
		vv.setOnErrorListener(playErrorListener);
		
		vv.setMySizeChangeLinstener(mySizeChangeLinstener);
		vv.setOnPreparedListener(mOnPreparedListener);
		vv.setOnCompletionListener(mOnCompletionListener);
		controlView = getLayoutInflater().inflate(R.layout.controler, null);
		controler = new PopupWindow(controlView);
		playPauseBtn = (ImageButton) controlView.findViewById(R.id.play_pause);
		playPauseBtn.setAlpha(0xBB);
		playPauseBtn.setOnClickListener(playPauseClickListener);
		SoundBtn = (ImageButton) controlView.findViewById(R.id.sound_btn);
		SoundBtn.setAlpha(findAlphaFromSound());
		SoundBtn.setOnClickListener(soundClickListener);
		SoundBtn.setOnLongClickListener(soundOnLongClickListener);
		seekBar = (SeekBar) controlView.findViewById(R.id.seekbar);
		seekBar.setOnSeekBarChangeListener(seekBarChangeListener);
		durationTextView = (TextView) controlView.findViewById(R.id.duration);
		playedTextView = (TextView) controlView.findViewById(R.id.has_played);
		mSoundView = new SoundView(this);
		mSoundWindow = new PopupWindow(mSoundView);
		mSoundView.setOnVolumeChangeListener(soundOnvChangedListener);
		// 上面条条
		extralView = getLayoutInflater().inflate(R.layout.extral, null);
		extralWindow = new PopupWindow(extralView);
		ImageButton backButton = (ImageButton) extralView
				.findViewById(R.id.back);

		backButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				EventVideoPlayActivity.this.finish();
			}

		});
		position = -1;

	}

	private void showMediaContoller() {
		Looper.myQueue().addIdleHandler(mIdleHandler);
	}

	private void showErrorDialog(String note) {
		new AlertDialog.Builder(EventVideoPlayActivity.this)
				.setTitle(getString(R.string.note))
				.setMessage(note)
				.setPositiveButton(getString(R.string.confirm),
						new AlertDialog.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {

								// vv.stopPlayback();
								finish();

							}

						}).setCancelable(false).show();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		if (requestCode == 0 && resultCode == Activity.RESULT_OK) {

			vv.stopPlayback();

			int result = data.getIntExtra("CHOOSE", -1);
			ELog.d("RESULT", "" + result);
			if (result != -1) {
				isOnline = false;
				isChangedVideo = true;
				vv.setVideoPath(playList.get(result).path);
				position = result;
			} else {
				String url = data.getStringExtra("CHOOSE_URL");
				if (url != null) {
					vv.setVideoPath(url);
					isOnline = true;
					isChangedVideo = true;
				}
			}

			return;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	private final static int PROGRESS_CHANGED = 0;
	private final static int HIDE_CONTROLER = 1;

	Handler myHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub

			switch (msg.what) {

			case PROGRESS_CHANGED:
				//

				int i = vv.getCurrentPosition();
				seekBar.setProgress(i);

				if (isOnline) {
					int j = vv.getBufferPercentage();
					seekBar.setSecondaryProgress(j * seekBar.getMax() / 100);
				} else {
					seekBar.setSecondaryProgress(0);
				}

				i /= 1000;
				int minute = i / 60;
				int hour = minute / 60;
				int second = i % 60;
				minute %= 60;
				playedTextView.setText(String.format("%02d:%02d",
						hour *60+minute, second));

				sendEmptyMessageDelayed(PROGRESS_CHANGED, 100);
				break;

			case HIDE_CONTROLER:
				hideController();
				break;
			}

			super.handleMessage(msg);
		}
	};

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub

		boolean result = mGestureDetector.onTouchEvent(event);

		if (!result) {
			if (event.getAction() == MotionEvent.ACTION_UP) {

				
			}
			result = super.onTouchEvent(event);
		}

		return result;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// TODO Auto-generated method stub

		getScreenSize();
		if (isControllerShow) {
			cancelDelayHide();
			hideController();
			showController();
			hideControllerDelay();
		}

		super.onConfigurationChanged(newConfig);
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		playedTime = vv.getCurrentPosition();
		vv.pause();
		playPauseBtn.setImageResource(R.drawable.play);
		super.onPause();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		if (!isChangedVideo) {
			vv.seekTo(playedTime);
			vv.start();
		} else {
			isChangedVideo = false;
		}

		// if(vv.getVideoHeight()!=0){
		if (vv.isPlaying()) {
			playPauseBtn.setImageResource(R.drawable.pause);
			hideControllerDelay();
		}
		ELog.d("REQUEST", "NEW AD !");

//		if (getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
//			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
//		}

		super.onResume();
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub

		if (controler.isShowing()) {
			controler.dismiss();
			extralWindow.dismiss();
		}
		if (mSoundWindow.isShowing()) {
			mSoundWindow.dismiss();
		}

		myHandler.removeMessages(PROGRESS_CHANGED);
		myHandler.removeMessages(HIDE_CONTROLER);

		if (vv.isPlaying()) {
			vv.stopPlayback();
		}

		playList.clear();

		super.onDestroy();
	}

	private void getScreenSize() {
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);//去掉信息栏
		Display display = getWindowManager().getDefaultDisplay();
		screenHeight = display.getHeight();
		screenWidth = display.getWidth();
		controlHeight = screenHeight / 5;
	}

	private void hideController() {
		if (controler.isShowing()) {
			controler.update(0, 0, 0, 0);
			extralWindow.update(0, 0, screenWidth, 0);
			isControllerShow = false;
		}
		if (mSoundWindow.isShowing()) {
			mSoundWindow.dismiss();
			isSoundShow = false;
		}
	}

	private void hideControllerDelay() {
		myHandler.sendEmptyMessageDelayed(HIDE_CONTROLER, TIME);
	}

	private void showController() {
		controler.update(0, 0, screenWidth, controlHeight);
		if (isFullScreen) {
			extralWindow.update(0, 0, screenWidth, 60);
		} else {
			extralWindow.update(0, 25, screenWidth, 60);
		}

		isControllerShow = true;
	}

	private void cancelDelayHide() {
		myHandler.removeMessages(HIDE_CONTROLER);
	}

	private final static int SCREEN_FULL = 0;
	private final static int SCREEN_DEFAULT = 1;

	private void setVideoScale(int flag) {

		LayoutParams lp = vv.getLayoutParams();

		switch (flag) {
		case SCREEN_FULL:

			ELog.d(TAG, "screenWidth: " + screenWidth + " screenHeight: "
					+ screenHeight);
			vv.setVideoScale(screenWidth, screenHeight);
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

			break;

		case SCREEN_DEFAULT:

			int videoWidth = vv.getVideoWidth();
			int videoHeight = vv.getVideoHeight();
			int mWidth = screenWidth;
			int mHeight = screenHeight - 25;

			if (videoWidth > 0 && videoHeight > 0) {
				if (videoWidth * mHeight > mWidth * videoHeight) {
					//
					mHeight = mWidth * videoHeight / videoWidth;
				} else if (videoWidth * mHeight < mWidth * videoHeight) {
					//
					mWidth = mHeight * videoWidth / videoHeight;
				} else {

				}
			}

			vv.setVideoScale(mWidth, mHeight);

			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

			break;
		}
	}

	private int findAlphaFromSound() {
		if (mAudioManager != null) {
			// int currentVolume =
			// mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
			int alpha = currentVolume * (0xCC - 0x55) / maxVolume + 0x55;
			return alpha;
		} else {
			return 0xCC;
		}
	}

	private void updateVolume(int index) {
		if (mAudioManager != null) {
			if (isSilent) {
				mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
			} else {
				mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, index,
						0);
			}
			currentVolume = index;
			SoundBtn.setAlpha(findAlphaFromSound());
		}
	}

}
