package com.myanycamm.cam;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.myanycam.bean.CameraListInfo;
import com.myanycam.bean.VideoData;
import com.myanycam.net.SocketFunction;
import com.myanycamm.model.VideoListener;
import com.myanycamm.process.AdPcm;
import com.myanycamm.process.ScreenManager;
import com.myanycamm.ui.DialogFactory;
import com.myanycamm.ui.PhotoListView;
import com.myanycamm.utils.ELog;
import com.myanycamm.utils.FileUtils;
import com.thSDK.VideoSurfaceView;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;

import gyl.cam.SoundPlay;
import gyl.cam.recThread;

public class CallAcceptActivity extends Activity {
	private final String TAG = "CallAcceptActivity";

	private static final int SET_Img = 21;
	private static final int SHOW_CONTROLLER = 22;
	public static final int SHWO_HEAD = 29;
	private static final int FADE_OUT = 23;
	private static final int STOP_CAM = 25;
	private static final int SHOW_RATE = 30;
	public static final int CALL_HANGUP = 31;

	public static final int NO_VIDEO = 26;
	public static final int NO_MCUSOKET = 27;
	private static final int FADE_OUT_MEDIA = 28;
	private static final int sDefaultTimeout = 3000;
	

	// 和camercenteractivity公用，不能改
	public static final int CAMMAXRESTRICT = 305;
	public static final int ACCESSPSWERRROR = 306;
	public static final int RECEVIEWATCHCAMERA = 307;
	public static final int ACCESSPSWETRUE = 308;
	private boolean isDrawBitmap = false;
	private Button accpet, refuse;
	private RelativeLayout accpetView, cloudView, mediaControllerLayout;
	TextView headTitle,topText;
	LinearLayout headLayout;
	View camView;
	ImageView rotImageView;
	Animation hyperspaceJumpAnimation;
	TextView playText;
	RelativeLayout playLayout;
	private ImageView playBtn;
	private Button playBack;
	private TextView rateTextView;
	private ImageButton speak, photo, sound;
	VideoSurfaceView glView;


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
	boolean isRecording = false;
	int recBufSize;
	SoundPlay soundPlay;
	AudioRecord audioRecord;
	private Paint paint;
	private Bitmap bitmap;
	private Matrix matrix;// 视频放大，移动用
	private boolean isSound = false;
	private boolean isFirstVideo = true;
	private Dialog mDialog = null;
	private Builder singleDialog = null;
	MediaPlayer mMediaPlayer = new MediaPlayer();
	HashMap<String, String> map;
	private OnClickListener refuseOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			refuse();
		}
	};

	private OnClickListener accpetOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			mMediaPlayer.stop();
			SocketFunction.getInstance().callMasterRespon(0);
			if(ScreenManager.getScreenManager().currentActivity().getClass().equals(CameraCenterActivity.class)){
				ScreenManager.getScreenManager().currentActivity().finish();
			}
			showCloudView();
			connectCam();
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
						callHandler.sendEmptyMessage(FADE_OUT_MEDIA);
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
						callHandler.sendEmptyMessage(FADE_OUT);
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
				callHandler.sendEmptyMessage(NO_VIDEO);
				break;
			default:
				break;
			}

		}
	};
	OnClickListener speakOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			speakSwitch();
		}
	};

	OnClickListener soundOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			soundSwitch();
		}
	};

	OnClickListener photoOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (!FileUtils.externalMemoryAvailable()) {
				Toast.makeText(CallAcceptActivity.this,
						getString(R.string.sdcard_invalid), Toast.LENGTH_SHORT)
						.show();
				return;
			}

			try {
				FileUtils.saveFile(bitmap,
						"myanycam" + SystemClock.currentThreadTimeMillis()
								+ ".png", PhotoListView.mCardPath);
				Toast.makeText(CallAcceptActivity.this,
						getString(R.string.save_success), Toast.LENGTH_SHORT)
						.show();
			} catch (IOException e) {
				ELog.i(TAG, "保存失败>.." + e.getMessage());
				Toast.makeText(CallAcceptActivity.this,
						getString(R.string.save_failed), Toast.LENGTH_SHORT)
						.show();
				e.printStackTrace();
			}
			// sf.getMcuSocket().modifyCam();
		}
	};

	public Handler callHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Bundle bundle = msg.getData();
			HashMap<String, String> map = (HashMap) bundle
					.getSerializable("data");
			super.handleMessage(msg);
			ELog.i(TAG, "收到图片数据,处理");

			switch (msg.what) {

			case SET_Img:
				bitmap = (Bitmap) msg.obj;
//				if (!AppServer.isDisplayVideo) {
//					return;
//				}
//				ELog.i(TAG, "来了图片");
//				if (isFirstVideo) {
//					playLayout.setVisibility(View.GONE);
//					mSurfaceView.setBackgroundColor(Color.TRANSPARENT);
//					setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
//					mSurfaceView.setOnTouchListener(surfaceOnTouchListener);
//					showMedia(sDefaultTimeout);
//					speakSwitch();
//					soundSwitch();
//				}
				isFirstVideo = false;
				rateTextView
						.setText(SocketFunction.getInstance().mUdpSocket.rateLast
								/ 1024 + "KB/s");
				// revImage.setImageBitmap((Bitmap) msg.obj);
				break;
			case SHOW_RATE:
				rateTextView
						.setText(SocketFunction.getInstance().mUdpSocket.rateLast
								/ 1024 + "KB/s");
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
				initStopCam();
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
			case CAMMAXRESTRICT:
				showCamMaxDialog();
				break;
			case RECEVIEWATCHCAMERA:
				SocketFunction.getInstance().mUdpSocket.setCamIpInfo(map);
				break;
			case ACCESSPSWERRROR:
				if (null != mDialog && mDialog.isShowing()) {
					Toast.makeText(CallAcceptActivity.this,
							R.string.cam_psw_error1, Toast.LENGTH_SHORT).show();
					dimissDialog();
				}
				showAccessPasswordErrorDialog();
				break;
			case ACCESSPSWETRUE:
				if (null != mDialog && mDialog.isShowing()) {
					dimissDialog();
					Toast.makeText(CallAcceptActivity.this,
							R.string.cam_psw_true, Toast.LENGTH_SHORT).show();
				}
				break;
			case CALL_HANGUP:
				initStopCam();
				showCallHangupDialog();
				break;

			default:

				break;
			}
		}

	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		final Window win = getWindow();
		win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
				| WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
		win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
				| WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
		setContentView(R.layout.activity_call_accept);
		initView();
		playRingStone();
		Intent intent = getIntent();
		Bundle bundle = (Bundle) intent.getExtras();
		map = (HashMap) bundle.getSerializable("data");
		for (int i = 0; i < CameraListInfo.cams.size(); i++) {
			if (CameraListInfo.cams.get(i).getId() == Integer.parseInt(map
					.get("cameraid"))) {
				ELog.i(TAG, "设置当前摄像头:"+CameraListInfo.cams.get(i).getSn());
				CameraListInfo.setCurrentCam(CameraListInfo.cams.get(i));
				topText.setText(CameraListInfo.currentCam.getName()+" "+getResources().getString(R.string.top_alert_text));
				break;
			}
		}
	}

	@Override
	public void onBackPressed() {
		finish();
		return;
	}

	@Override
	public void finish() {
		initStopCam();
		super.finish();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			ELog.i(TAG, "横向了..");
			changScreenToLand();
		} else {
			ELog.i(TAG, "竖屏了..");
			changScreenToPorait();
			// mCameraListView.mHandler.sendEmptyMessage(CloudLivingView.SHWO_HEAD);
		}
	}

	private void speakSwitch() {

		ELog.i(TAG, "对讲...");
		if (isRecording) {
			speak.setImageResource(R.drawable.play_ctr_speak);
			isRecording = false;
			SocketFunction.getInstance().mUdpSocket.colseSenAudioSwitch();
		} else {
			speak.setImageResource(R.drawable.play_ctr_speak_on);
			new RecordPlayThread().start();
		}

	}

	private void soundSwitch() {

		// soundPlay = new SoundPlay();
		// soundPlay.start();
		if (!isSound) {
			soundPlay = new SoundPlay();
			soundPlay.start();
			SocketFunction.getInstance().mUdpSocket.getAudioSwitch();
			sound.setImageResource(R.drawable.play_ctr_sound_on);
		} else if (soundPlay != null) {
			SocketFunction.getInstance().mUdpSocket.closeAudioSwitch();
			soundPlay.is_keep_running = false;
			sound.setImageResource(R.drawable.play_ctr_sound);
			soundPlay = null;
		}
		isSound = !isSound;

	}

	private void playRingStone() {

		Uri alert = RingtoneManager
				.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
		try {

			mMediaPlayer.setDataSource(CallAcceptActivity.this, alert);
			final AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
			if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
				mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
				mMediaPlayer.setLooping(true);
				mMediaPlayer.prepare();
			}
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		mMediaPlayer.start();

	}

	private void refuse() {
		mMediaPlayer.stop();
		SocketFunction.getInstance().callMasterRespon(1);
		super.finish();
	}

	private void initView() {
		accpetView = (RelativeLayout) findViewById(R.id.accpet_view);
		cloudView = (RelativeLayout) findViewById(R.id.cloud_view);
		accpet = (Button) findViewById(R.id.accept);
		accpet.setOnClickListener(accpetOnClickListener);
		refuse = (Button) findViewById(R.id.refuse);
		refuse.setOnClickListener(refuseOnClickListener);
		headTitle = (TextView) findViewById(R.id.settings_head_title_play);
		topText = (TextView) findViewById(R.id.top_text);
		playLayout = (RelativeLayout) findViewById(R.id.play_layout);
		playBtn = (ImageView) findViewById(R.id.play_btn);
		playBack = (Button) findViewById(R.id.settings_back_play);
		// playBack.setOnClickListener(playBackOnclClickListener);
		
		rateTextView = (TextView) findViewById(R.id.rate);
		playText = (TextView) findViewById(R.id.play_text);
		rotImageView = (ImageView) findViewById(R.id.rotate_play_img);
		speak = (ImageButton) findViewById(R.id.play_speak);
		sound = (ImageButton) findViewById(R.id.play_sound);
		photo = (ImageButton) findViewById(R.id.play_photo);
		mediaControllerLayout = (RelativeLayout) findViewById(R.id.mediacontroll);
		mediaControllerLayout.getBackground().setAlpha(80);
		headLayout = (LinearLayout) findViewById(R.id.head_layout);
		glView = (VideoSurfaceView) findViewById(R.id.paly_surf);

		// mGlBufferView = (GlBufferView)
		// camView.findViewById(R.id.glbuffer_view);

		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		// addBtn.setVisibility(View.VISIBLE);

		// mainLayout = (LinearLayout)
		// camView.findViewById(R.id.cam_mainlayout);
		setHead();
	}

	private void showCloudView() {
		accpetView.setVisibility(View.GONE);
		cloudView.setVisibility(View.VISIBLE);
	}

	public void setHead() {
		playBack.setVisibility(View.VISIBLE);
		playBack.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				finish();
			}
		});
		mediaControllerLayout.setVisibility(View.INVISIBLE);
		// playBtn.setOnClickListener(playBtnOnClickListener);
		// headLayout.setVisibility(View.VISIBLE);

	}

	public void changScreenToLand() {
		callHandler.sendEmptyMessage(FADE_OUT);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);// 去掉信息栏
		// show(3000);
	}

	public void changScreenToPorait() {
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setHead();
		callHandler.sendEmptyMessage(SHWO_HEAD);
		// show(3000);
	}

	private void connectCam() {
		SocketFunction.getInstance().setmHandler(callHandler);
		SocketFunction.getInstance().userCamPswCheck(CameraListInfo.currentCam);
		headTitle.setText(CameraListInfo.currentCam.getName());// 默认为摄像头列表
		initCam();
	}

	
	public void show(int timeout) {// 第一次会显示
		callHandler.sendEmptyMessage(SHOW_CONTROLLER);
		if (timeout != 0) {
			callHandler.removeMessages(FADE_OUT);
			callHandler.sendMessageDelayed(callHandler.obtainMessage(FADE_OUT),
					timeout);
		}
	}

	private void showMedia(int timeout) {
		mediaControllerLayout.setVisibility(View.VISIBLE);
		mediaControllerLayout.getBackground().setAlpha(100);
		if (timeout != 0) {
			callHandler.removeMessages(FADE_OUT_MEDIA);
			callHandler.sendMessageDelayed(
					callHandler.obtainMessage(FADE_OUT_MEDIA), timeout);
		}
	}

	private void initStopCam() {
		ELog.i(TAG, "停止视频接收");
		playLayout.setVisibility(View.VISIBLE);
		playText.setVisibility(View.VISIBLE);
		// MyTimerTask.getInstance().setVideoListener(false);//不监听没视频了
		// sf.getMcuSocket().setmVideoListener(null);
		// if (isDisplayVideo) {
		SocketFunction.getInstance().mUdpSocket.stopCam();
		// }
		isRecording = false;
		AppServer.isDisplayVideo = false;
		isDrawBitmap = false;
		ELog.i(TAG, "aaaa");

		SocketFunction.getInstance().stopWatchCamer(CameraListInfo.currentCam);
		if (soundPlay != null) {
			soundPlay.is_keep_running = false;
		}
		rotImageView.setAnimation(null);
		rateTextView.setText("0KB/s");
		mediaControllerLayout.setVisibility(View.INVISIBLE);
		isFirstVideo = true;
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		glView.setOnTouchListener(null);
		VideoData.Videolist.clear();// 清空数据
		ELog.i(TAG, "执行完了...");
	}

	
	private void ShowRetryDialog() {
		if (null != builder) {
			return;
		}
		initStopCam();
		builder = DialogFactory.creatReTryDialog(CallAcceptActivity.this,
				getResources().getString(R.string.net_error));

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
						initStopCam();
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
		builder = DialogFactory.creatReTryDialog(CallAcceptActivity.this,
				getResources().getString(R.string.cam_max));
		builder.setPositiveButton(getResources().getString(R.string.confirm),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						finish();
						builder = null;
					}
				});
		builder.create().show();
	}
	
	public void showCallHangupDialog(){
		if (null != builder) {
			return;
		}
		initStopCam();
		builder = DialogFactory.creatReTryDialog(CallAcceptActivity.this,
				getResources().getString(R.string.cam_finish_call));
		builder.setPositiveButton(getResources().getString(R.string.confirm),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						finish();
						builder = null;
					}
				});
		builder.create().show();
	}

	public void dimissDialog() {
		if (mDialog != null) {
			mDialog.dismiss();
			mDialog = null;
		}
	}

	
	private void retryCam() {
		// initStopCam();
		ELog.i(TAG, "重试开始视频...");
		playLayout.setVisibility(View.VISIBLE);
		initCam();
	}

	public void initCam() {
		playBtn.setOnClickListener(null);
		playText.setVisibility(View.GONE);
		hyperspaceJumpAnimation = AnimationUtils.loadAnimation(
				CallAcceptActivity.this, R.anim.loading_animation);
		rotImageView.startAnimation(hyperspaceJumpAnimation);
		SocketFunction.getInstance().watchCamera(CameraListInfo.currentCam);
		// ShowRetryDialog();
		// MyTimerTask.getInstance().setVideoListener(true);//监听是否有视频
		ELog.i(TAG, "开始了视频..");
		isFirstVideo = true;
		isDrawBitmap = true;

		//
		VideoData.Videolist.clear();// 清空数据
		recThread dataRecThread = new recThread(callHandler);
		//
		dataRecThread.start();
		SocketFunction.getInstance().mUdpSocket
				.setmVideoListener(mVideoListener);
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
	}

	


	private void showAccessPasswordErrorDialog() {
		initStopCam();
		LayoutInflater inflater = (LayoutInflater) getApplicationContext()
				.getSystemService(LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.wifi_alert_style, null);
		TextView mTextView = (TextView) view.findViewById(R.id.safety_info);
		mTextView.setText(getString(R.string.cam_psw_error));
		final EditText pswEditText = (EditText) view
				.findViewById(R.id.psw_edit);
		TextView mNote = (TextView) view.findViewById(R.id.wifi_psw_note);
		mNote.setVisibility(View.GONE);
		// pswEditText.setText(cwi.getPassword());
		CheckBox mCheckBox = (CheckBox) view.findViewById(R.id.is_remember);
		mCheckBox.setVisibility(View.GONE);
		if (null == singleDialog) {
			singleDialog = new AlertDialog.Builder(this);
			singleDialog.setView(view);
			singleDialog.setIcon(android.R.drawable.ic_dialog_info);
			singleDialog.setTitle(R.string.note);
			singleDialog.setCancelable(false);
			singleDialog.setPositiveButton(getString(R.string.btn_cancel),
					new DialogInterface.OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {
							try {
								Field field = dialog.getClass().getSuperclass()
										.getDeclaredField("mShowing");
								field.setAccessible(true);
								// 将mShowing变量设为false，表示对话框已关闭
								field.set(dialog, true);
								dialog.dismiss();
								finish();
							} catch (Exception e) {

							}

						}
					});
			singleDialog
					.setNegativeButton(getString(R.string.connect),
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									if (pswEditText.getText().length() > 7) {
										// cameraWifiInfos.get(rp).setIsCurrenLink(false);
										// cameraWifiInfos.get(position).setIsCurrenLink(true);
										// cwi.setPassword(pswEditText.getText().toString());
										// sf.setWifiInfo(cameraWifiInfos.get(position)
										// .getSsid(),
										// cameraWifiInfos.get(position)
										// .getSafe(), pswEditText.getText()
										// .toString());
										// updateWifiList();
										CameraListInfo.currentCam
												.setPassWord(pswEditText
														.getText().toString());
										SocketFunction
												.getInstance()
												.modifyCamInfo(
														CameraListInfo.currentCam);
										SocketFunction
												.getInstance()
												.userCamPswCheck(
														CameraListInfo.currentCam);
										showRequestDialog(null);
										singleDialog = null;
									} else {
										// 不关闭
										try {
											Field field = dialog
													.getClass()
													.getSuperclass()
													.getDeclaredField(
															"mShowing");
											field.setAccessible(true);
											// 将mShowing变量设为false，表示对话框已关闭
											field.set(dialog, false);
											dialog.dismiss();

										} catch (Exception e) {

										}

									}

								}
							}).create().show();
		}

	}

	
	public void showRequestDialog(String note) {
		if (mDialog != null) {
			mDialog.dismiss();
			mDialog = null;
		}
		mDialog = DialogFactory.createLoadingDialog(CallAcceptActivity.this,
				note);
		mDialog.setCanceledOnTouchOutside(false);
		mDialog.setOnKeyListener(new OnKeyListener() {

			@Override
			public boolean onKey(DialogInterface dialog, int keyCode,
					KeyEvent event) {
				switch (keyCode) {
				case KeyEvent.KEYCODE_BACK:
					// LoginActivity.this.finish();
					break;

				default:
					break;
				}
				return false;
			}
		});
		mDialog.show();
	}

	public void getVoidceFromRec() {

		Log.i(TAG, "准备双向通话");
		SocketFunction.getInstance().mUdpSocket.senAudioSwitch();
		// 获得满足条件的最小缓冲区大小
		int bufferSizeInBytes = AudioRecord.getMinBufferSize(sampleRateInHz,
				channelConfig, audioFormat);
		// int bufferSizeInBytes = 1920;
		// int bufferSizeInBytes = 800;
		ELog.i(TAG, "bufferSizeInBytes:" + bufferSizeInBytes);
		// int bufferSizeInBytes = 240;
		// 创建AudioRecord对象
		AudioRecord audioRecord = new AudioRecord(audioSource, sampleRateInHz,
				channelConfig, audioFormat, bufferSizeInBytes);
		try {
			audioRecord.startRecording();
		} catch (IllegalStateException e) {
			ELog.e(TAG, e.getMessage());
		}
		// byte[] buffer = new byte[bufferSizeInBytes];
		short[] buffer = new short[bufferSizeInBytes];
		isRecording = true;
		while (isRecording) {
			int bufferReadResult = audioRecord.read(buffer, 0,
					bufferSizeInBytes);
			ELog.i(TAG, "buffer:" + buffer.length + "bufferReadResult:"
					+ bufferReadResult);

			//
			// int bufferReadResult = audioRecord.read(buffer, 0,
			// recBufSize);
			//
			try {
				short[] tmpBuf = new short[bufferReadResult];
				System.arraycopy(buffer, 0, tmpBuf, 0, bufferReadResult);
				byte[] adpcmByte = new byte[tmpBuf.length / 2];
				adpcmByte = AdPcm.encodeAdpcm(tmpBuf, tmpBuf.length, adpcmByte,
						0);
				ELog.i(TAG, "adpcm长度:" + adpcmByte.length);
				SocketFunction.getInstance().mUdpSocket
						.sendVoiceToCam(adpcmByte);
				// sf.mUdpSocket.sendVoiceToCam(tmpBuf);
				//
			} catch (NegativeArraySizeException e) {
				ELog.i(TAG, "录音错误" + e.getMessage());
			}
		}

	}

	


	class RecordPlayThread extends Thread {
		@Override
		public void run() {
			// getVoiceFormFile();
			getVoidceFromRec();
		}
	}

}
