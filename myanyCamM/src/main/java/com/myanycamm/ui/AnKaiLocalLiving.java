package com.myanycamm.ui;

import gyl.cam.SoundPlay;
import gyl.cam.recThread;

import java.io.IOException;

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
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.myanycam.bean.ActionItem;
import com.myanycam.bean.CameraListInfo;
import com.myanycam.bean.VideoData;
import com.myanycam.net.SocketFunction;
import com.myanycam.net.TcpSocket;
import com.myanycam.net.UdpSocket;
import com.myanycamm.cam.AppServer;
import com.myanycamm.cam.CameraCenterActivity;
import com.myanycamm.cam.R;
import com.myanycamm.model.VideoListener;
import com.myanycamm.utils.ELog;
import com.myanycamm.utils.FileUtils;
import com.myanycamm.utils.Utils;

public class AnKaiLocalLiving extends LivingView {
	private String TAG = "AnKaiLocalLiving";
	boolean isRecVideoing = false;
	private ImageView playBtn,baterryInfo;
	private Button playBack;
	private TextView rateTextView,sdInfo;
	private ImageButton photo, sound, videRec;
	private RelativeLayout mediaControllerLayout;
	private SurfaceView mSurfaceView;
	SurfaceHolder surfaceHolder;

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

	public static int packagesize = 160;
	public static short[] tmpBuf = new short[packagesize];

	private Paint paint;
	private Bitmap bitmap;
	private Matrix matrix;// 视频放大，移动用
	private TextView settingTitle;
	private Button selectBtn;
	// 视频质量选择
	private static final int ID_BEST = 3;
	private static final int ID_BETTER = 2;
	private static final int ID_GOOD = 1;

	public static boolean sdlTAG = false;
	QuickAction mQuickAction;

	AlertDialog.Builder builder;
	private boolean isFirstVideo = true;
	public static boolean isRecording = false;
	private boolean isDrawBitmap = false;
	private boolean isSound = false;
	SoundPlay soundPlay;

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

				// ELog.i(TAG,
				// "码流:"+SocketFunction.getInstance().mUdpSocket.rateLast );

				rateTextView.setText(TcpSocket.getInstance().rateLast / 1024
						+ "KB/s");

				// revImage.setImageBitmap((Bitmap) msg.obj);
				break;
			case SHOW_RATE:
				rateTextView.setText(TcpSocket.getInstance().rateLast / 1024
						+ "KB/s");
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

			default:

				break;
			}
		}

	};

	OnClickListener photoOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			SocketFunction.getInstance().manualSnap();
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

	private OnClickListener playBtnOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			initCam();
			// surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
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
				// VideoData.audioArraryList.clear();
				// sf.mUdpSocket.colseSenAudioSwitch();
			} else {
				SocketFunction.getInstance().manualRecord(1);
				isRecVideoing = true;
				videRec.setImageResource(R.drawable.play_rec_btn_on);
				// new RecordPlayThread().start();
			}

		}
	};

	OnClickListener selectClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {

			mQuickAction.show(v);

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
	};

	public AnKaiLocalLiving(CameraCenterActivity activity, TabHost tabHost) {
		super(activity, tabHost);
		SocketFunction.getInstance().mUdpSocket = new UdpSocket(
				SocketFunction.getInstance());
		super.mHandler = this.mHandler;
		initView();
	}

	public void initView() {
		LayoutInflater inflater = LayoutInflater.from(mActivity);
		camView = inflater.inflate(R.layout.tab_play,
				mTabHost.getTabContentView());
		headTitle = (TextView) camView
				.findViewById(R.id.settings_head_title_play);
		playLayout = (RelativeLayout) camView.findViewById(R.id.play_layout);
		playBtn = (ImageView) camView.findViewById(R.id.play_btn);
		playBack = (Button) camView.findViewById(R.id.settings_back_play);
		playBack.setOnClickListener(playBackOnclClickListener);
		playBack.setVisibility(View.GONE);
		rateTextView = (TextView) camView.findViewById(R.id.rate);
		playText = (TextView) camView.findViewById(R.id.play_text);
		baterryInfo = (ImageView) camView.findViewById(R.id.baterry_info);
		sdInfo = (TextView) camView.findViewById(R.id.sd_info);

		rotImageView = (ImageView) camView.findViewById(R.id.rotate_play_img);
		ImageButton speak = (ImageButton) camView.findViewById(R.id.play_speak);
		speak.setVisibility(View.GONE);
		sound = (ImageButton) camView.findViewById(R.id.play_sound);
		photo = (ImageButton) camView.findViewById(R.id.play_photo);
		videRec = (ImageButton) camView.findViewById(R.id.play_rec_btn);
		videRec.setOnClickListener(recVideoOnClickListener);
		mediaControllerLayout = (RelativeLayout) camView
				.findViewById(R.id.mediacontroll);
		mediaControllerLayout.getBackground().setAlpha(50);
		headLayout = (LinearLayout) camView.findViewById(R.id.head_layout);
		mSurfaceView = (SurfaceView) camView.findViewById(R.id.paly_surf);
		surfaceHolder = mSurfaceView.getHolder();
		mActivity
				.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		mActivity.getWindow().addFlags(
				WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setHead();
		initSelectWindow();
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

	private void showMedia(int timeout) {
		mediaControllerLayout.setVisibility(View.VISIBLE);
		mediaControllerLayout.getBackground().setAlpha(100);
		if (timeout != 0) {
			mHandler.removeMessages(FADE_OUT_MEDIA);
			mHandler.sendMessageDelayed(mHandler.obtainMessage(FADE_OUT_MEDIA),
					timeout);
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
								SocketFunction.getInstance().modifyCamera(
										CameraListInfo.currentCam,
										SocketFunction.getInstance().mUdpSocket
												.getChannelId());
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

	private void initStopCam() {
		ELog.i(TAG, "停止视频接收");
		playLayout.setVisibility(View.VISIBLE);
		playText.setVisibility(View.VISIBLE);
		// MyTimerTask.getInstance().setVideoListener(false);//不监听没视频了
		// sf.getMcuSocket().setmVideoListener(null);
		// if (isDisplayVideo) {
		SocketFunction.getInstance().mUdpSocket.stopCam();
		TcpSocket.getInstance().stopTcpSocket();
		// }

		isRecording = false;
		sdlTAG = false;
		isRecVideoing = false;
		if (soundPlay != null) {
			soundPlay.is_keep_running = false;
		}

		if (!AppServer.isBackgroud) {
			videRec.setImageResource(R.drawable.play_rec_btn);
			sound.setImageResource(R.drawable.play_ctr_sound);
			rateTextView.setText("0KB/s");
		}

		AppServer.isDisplayVideo = false;
		isDrawBitmap = false;
		sdlTAG = false;
		ELog.i(TAG, "aaaa");
		SocketFunction.getInstance().stopWatchCamer(CameraListInfo.currentCam);

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

	private void retryCam() {
		// initStopCam();
		ELog.i(TAG, "重试开始视频...");
		playLayout.setVisibility(View.VISIBLE);
		initCam();
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
	public void stopCam() {
		initStopCam();
		// TODO Auto-generated method stub

	}

	@Override
	public void initCam() {
		SocketFunction.getInstance().deviceStatus();
		playBtn.setOnClickListener(null);
		playText.setVisibility(View.GONE);
		rotImageView.startAnimation(hyperspaceJumpAnimation);
		SocketFunction.getInstance().watchCameraTcp();
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
		SocketFunction.getInstance().mUdpSocket
				.setmVideoListener(mVideoListener);
		sound.setOnClickListener(soundOnClickListener);
		photo.setOnClickListener(photoOnClickListener);
		ELog.i(TAG, "sn:" + CameraListInfo.currentCam.getSn());

	}

	@Override
	public void show(int timeout) {

	}

	@Override
	public void setHead() {
		// playBack.setVisibility(View.VISIBLE);
		mediaControllerLayout.setVisibility(View.INVISIBLE);
		headTitle.setText("Myanycam");// 默认为摄像头列表
		playBtn.setOnClickListener(playBtnOnClickListener);
		selectBtn = (Button) camView.findViewById(R.id.settings_btn);
		selectBtn.setVisibility(View.GONE);
		selectBtn.setOnClickListener(selectClickListener);
		// selectBtn.setText(mActivity.getResources().getString(
		// R.string.select_quality_better));
		// headLayout.setVisibility(View.VISIBLE);
		ELog.i(TAG, "头部完了..");

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
	public void showHead() {
		// TODO Auto-generated method stub

	}

	@Override
	public void changScreenToLand() {
		// TODO Auto-generated method stub

	}

	@Override
	public void changScreenToPorait() {
		// TODO Auto-generated method stub

	}

	@Override
	public void showCamMaxDialog() {
		// TODO Auto-generated method stub

	}

	@Override
	public void stopRecord() {
		// TODO Auto-generated method stub

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
			int iCount = 0;
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
						c.drawColor(Color.BLACK);
						c.drawBitmap(bitmap, matrix, paint);
						iCount++;
						if (iCount < 30 && isRecVideoing) {
							paint.setColor(Color.RED);

							float scaleHeight = ((float) Utils
									.getHeightPixels(mActivity));

							if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
								// 当前为横屏，在此处添加额外的处理代码
								c.drawCircle(scaleWidth - 50, 50, 10, paint);

							} else {
								// 当前为竖屏，// 在此处添加额外的处理代码
								c.drawCircle(scaleWidth - 50, 200, 10, paint);
							}

							Log.i(TAG, "	Color.RED " + iCount);
							// Log.i(TAG,
							// "width ="+scaleWidth+"height = "+scaleHeight);
						} else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
							paint.setColor(Color.BLACK);
							c.drawCircle(scaleWidth - 50, 200, 10, paint);
						}

						if (iCount > 60) {
							iCount = 0;
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
	public void setDeviceStatus(int sdcard,int battery) {
		sdInfo.setVisibility(View.VISIBLE);
		baterryInfo.setVisibility(View.VISIBLE);
		if (sdcard == -1) {
			sdInfo.setText("sdcard:没有sd卡");
		}else{
			sdInfo.setText("sdcard:"+sdcard+"M");
		}
	
		switch (battery) {
		case -1:
		case 0:
			baterryInfo.setImageResource(R.drawable.battery0);
			break;
		case 1:
		case 2:
			baterryInfo.setImageResource(R.drawable.battery1);
			break;

		case 3:
			baterryInfo.setImageResource(R.drawable.battery2);
			break;
		case 4:
			baterryInfo.setImageResource(R.drawable.battery3);
			break;
		default:
			break;
		}
	}

}
