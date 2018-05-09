package com.myanycamm.cam;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.TimeZone;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.R.integer;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.myanycam.bean.CameraListInfo;
import com.myanycam.net.SocketFunction;
import com.myanycam.net.TcpSocket;
import com.myanycamm.model.BitmapCache;
import com.myanycamm.process.ScreenManager;
import com.myanycamm.ui.AnKaiLocalLiving;
import com.myanycamm.ui.CloudLivingView;
import com.myanycamm.ui.DialogFactory;
import com.myanycamm.ui.FileListView;
import com.myanycamm.ui.LivingView;
import com.myanycamm.ui.PhotoListView;
import com.myanycamm.ui.SettingView;
import com.myanycamm.utils.AesUtils;
import com.myanycamm.utils.ELog;
import com.myanycamm.utils.FileUtils;
import com.myanycamm.utils.SharePrefereUtils;

public class CameraCenterActivity extends BaseActivity {
	private static String TAG = "CameraCenterActivity";
	private TabHost mTabHost;
	LayoutInflater inflater_tab;
	LivingView mCameraListView;// 播放界面
	public FileListView mFileListView;// 文件列表
	SettingView mSettingView;// 设置界面列表
	public SocketFunction sf;
	public int position;
	PhotoListView mPhotoListView;
	// public CameraListInfo cam;
	private Dialog mDialog = null;
	public static final int PICTURELIST = 300;
	public static final int DOWNLOADPIC = 301;
	public static final int VIDEOLIST = 302;
	public static final int DOWNLOADVIDEO = 303;
	public static final int MUC_SUCCESS = 304;
	public static final int UPDATEEVENTNUM = 4;
	private final int UPDATECAMRRA = 5;// 更新摄像头状态，和主界面公用，不能随便改
	public static final int CAMMAXRESTRICT = 305;
	public static final int ACCESSPSWERRROR = 306;
	public static final int RECEVIEWATCHCAMERA = 307;
	public static final int ACCESSPSWETRUE = 308;
	public static final int UPDATEROMVERSION = 309;
	public static final int UPDATEROMING = 310;
	public static final int MANUALSNAPRESP = 401;
	public static final int TCPVIDEOSOCKET = 402;
	public static final int MANUALRECORDRESP = 403;
	private static final int TOSHARE = 501;
	public static final int DEVICE_INFO= 701;
	public static final int DEVICE_STATUS = 702;
	private Builder singleDialog = null;

	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			Bundle bundle = msg.getData();
			HashMap<String, String> map = (HashMap) bundle
					.getSerializable("data");
			ELog.i(TAG, "map:" + map);
			switch (msg.what) {
			case DEVICE_STATUS:
				if (AppServer.isAp) {
					int sd = Integer.parseInt(map.get("sdcard"));
					int battery = Integer.parseInt(map.get("battery"));
					mCameraListView.setDeviceStatus(sd,battery);
				}
				break;
			case PICTURELIST:
				mFileListView.doPictureList(map);
				break;
			case DOWNLOADPIC:
				String localUrl = map.get("loaclurl");
				String proxyUrl = map.get("proxyurl");
				mFileListView.doDownLoadPic(map.get("position"));
				// mFileListView.photoAnyCamEvent.goIntent(map.get("position"));
				// mFileListView.ShowRetryDialog(localUrl, proxyUrl);

				break;
			case DEVICE_INFO:
				Context ctx = CameraCenterActivity.this;
				SharePrefereUtils.commitStringData(ctx, "timezone",map.get("timezone"));
				SharePrefereUtils.commitStringData(ctx, "password",map.get("password"));
				CameraListInfo.currentCam.setPassWord(map.get("password"));
				SharePrefereUtils.commitStringData(ctx, "sn",map.get("sn"));
				CameraListInfo.currentCam.setSn(map.get("sn"));
				SharePrefereUtils.commitStringData(ctx, "producter",map.get("producter"));
				if (null != map.get("vflip") && !map.get("vflip").equals("")) {
					CameraListInfo.currentCam.setVflip(Integer.parseInt(map.get("vflip")));
				}
				
				mSettingView.changRotateState(CameraListInfo.currentCam.getVflip());
				mSettingView.fillCamPsw();
				break;
			case VIDEOLIST:
				mFileListView.doVideoList(map);
				break;
			case DOWNLOADVIDEO:
				String localUrlV = map.get("loaclurl");
				String proxyUrlV = map.get("proxyurl");
				mFileListView.doDownloadVideo(localUrlV, proxyUrlV);
				break;
			case UPDATEEVENTNUM:
				ELog.i(TAG, "更新报警条数");
				updateAlertNum();
				break;
			case UPDATECAMRRA:
				switch (CameraListInfo.currentCam.getStatus()) {
				case 0:
					ELog.i(TAG, "摄像头掉线了...");
					mCameraListView.stopCam();
					showOffLineDilalog();
					break;
				case 3:
					showUpdateing();
					break;
				default:
					break;
				}

				break;
			case MUC_SUCCESS:
				//
				// mCameraListView.initCam();
				break;
			case CAMMAXRESTRICT:
				mCameraListView.showCamMaxDialog();
				break;
			case RECEVIEWATCHCAMERA:
				sf.mUdpSocket.setCamIpInfo(map);
				break;
			case ACCESSPSWERRROR:
				if (null != mDialog && mDialog.isShowing()) {
					Toast.makeText(CameraCenterActivity.this,
							R.string.cam_psw_error1, Toast.LENGTH_SHORT).show();
					dimissDialog();
				}
				showAccessPasswordErrorDialog();
				break;
			case ACCESSPSWETRUE:
				CameraListInfo.currentCam.setAccessKey(map.get("accesskey"));
				CameraListInfo.currentCam.setNatIP(map.get("natip"));
				if (null != mDialog && mDialog.isShowing()) {
					CameraListInfo.currentCam.setAccess(true);
					dimissDialog();
					Toast.makeText(CameraCenterActivity.this,
							R.string.cam_psw_true, Toast.LENGTH_SHORT).show();
				}
				if (null != map.get("upnp") && map.get("upnp").equals("1")) {
					CameraListInfo.currentCam.setUpnp(true);
				} else {
					CameraListInfo.currentCam.setUpnp(false);
				}
			
				break;
			case UPDATEROMVERSION:
				mSettingView.updateRomVersion();
				if (null != map.get("vflip") && !map.get("vflip").equals("")) {
					CameraListInfo.currentCam.setVflip(Integer.parseInt(map.get("vflip")));
				}
			
				mSettingView.changRotateState(CameraListInfo.currentCam.getVflip());
				break;
			case MANUALSNAPRESP:
				ELog.i(TAG, "收到拍照回复..");
				if (map.get("ret").equals("1")) {
					Toast.makeText(CameraCenterActivity.this,
							getString(R.string.cam_no_sdcard),
							Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(CameraCenterActivity.this,
							getString(R.string.cam_photo_success),
							Toast.LENGTH_SHORT).show();
				}

				break;
			case MANUALRECORDRESP:
				if (map.get("ret").equals("1")) {
					Toast.makeText(CameraCenterActivity.this,
							getString(R.string.cam_no_sdcard),
							Toast.LENGTH_SHORT).show();
					mCameraListView.stopRecord();
				}
				break;
			case TCPVIDEOSOCKET:
				if (AppServer.isAp) {
					TcpSocket.getInstance().connect(map.get("localip"),
							Integer.parseInt(map.get("localport")));
				} else if (SocketFunction.getInstance().userInfo.getNatIp()
						.equals(CameraListInfo.currentCam.getNatIP()) && null!= map.get("localip")) {
					TcpSocket.getInstance().connect(map.get("localip"),
							Integer.parseInt(map.get("localport")));
				} else {
					ELog.i(TAG, "远程ip");
					TcpSocket.getInstance().connect(map.get("serverip"),
							Integer.parseInt(map.get("serverport")));
				}

				break;
			case TOSHARE:

				break;
			default:
				break;
			}
		};
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		sf = (SocketFunction) getApplicationContext();
		sf.setmHandler(mHandler);
		Bundle bdn = getIntent().getBundleExtra("bdn");
		position = getIntent().getIntExtra("position", -1);
		if (AppServer.isAp) {
			ELog.i(TAG, "本地摄像头");
			int timeZone = TimeZone.getDefault().getRawOffset()/3600000;
			long time = System.currentTimeMillis()/1000;
			SocketFunction.getInstance().setTimeZone(time, timeZone);
			// this.cam = new CameraListInfo();
			CameraListInfo.setCurrentCam(new CameraListInfo());
			SocketFunction.getInstance().getDeviceConfig();
			SocketFunction.getInstance().deviceStatus();
		} else {
			// this.cam = CameraListInfo.cams.get(position);
			ELog.i(TAG, "position:"+position);
			if (position == -1) {
				finish();
			}
			CameraListInfo.setCurrentCam(CameraListInfo.cams.get(position));
			sf.userCamPswCheck(CameraListInfo.currentCam);
			sf.getCamVersion();
		}

		ELog.i(TAG, "得到bdn:" + CameraListInfo.currentCam.getName());
		initTabView();
		// mFileListView.getEvent();
	}

	@Override
	protected void onResume() {
		super.onResume();
		ELog.i(TAG, "cameraCenter resume");
		sf.setmHandler(mHandler);
	}
	
	@Override
	protected void onRestart() {
		ELog.i(TAG, "重新进来...");
		super.onRestart();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			ELog.i(TAG, "横向了..");
			mTabHost.getTabWidget().setVisibility(View.GONE);
			mCameraListView.changScreenToLand();
		} else {
			ELog.i(TAG, "竖屏了..");
			mTabHost.getTabWidget().setVisibility(View.VISIBLE);
			mCameraListView.changScreenToPorait();
			// mCameraListView.mHandler.sendEmptyMessage(CloudLivingView.SHWO_HEAD);
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (AppServer.isAp) {
				ScreenManager.getScreenManager().popAllActivity();
			} else {
				finish();
			}

			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void finish() {
		stop();
		ELog.i(TAG, "finish...");
		mFileListView.clear();
		super.finish();
	}

	private void initTabView() {
		mTabHost = (TabHost) findViewById(R.id.tabhost);
		mTabHost.setup();
		fillTabHost();
	}

	private void updateAlertNum() {
		TextView alarNum = (TextView) mTabHost.getTabWidget()
				.getChildTabViewAt(1).findViewById(R.id.alar_num);
		alarNum.setText(CameraListInfo.currentCam.getAlertNum() + "");
		alarNum.setVisibility(CameraListInfo.currentCam.getAlertNum() > 0 ? View.VISIBLE
				: View.GONE);
	}

	public void stop() {
		mCameraListView.stopCam();
		mPhotoListView.recycleBm();
	}

	private synchronized void fillTabHost() {
		inflater_tab = LayoutInflater.from(this);
		if (AppServer.isAp) {
			mCameraListView = new AnKaiLocalLiving(this, mTabHost);
		} else {
			mCameraListView = new CloudLivingView(this, mTabHost);
			// mCameraListView = new LocalLivingView(this, mTabHost);
		}

		// inflater_tab.
		// inflater_tab.inflate(mCameraListView, mTabHost.getTabContentView());
		mFileListView = new FileListView(this, mTabHost);
		mPhotoListView = new PhotoListView(this, mTabHost);
		// inflater_tab.inflate(R.layout.tab_files,
		// mTabHost.getTabContentView());
		// inflater_tab.inflate(R.layout.tab_user,
		// mTabHost.getTabContentView());
		// inflater_tab
		// .inflate(R.layout.tab_setting, mTabHost.getTabContentView());
		mSettingView = new SettingView(this, mTabHost);
		TabSpec specCam = mTabHost
				.newTabSpec("cam")
				.setIndicator(
						createView(getString(R.string.camera),
								R.drawable.tab_icon_cam, false))
				.setContent(R.id.LinearLayout_cam);
		TabSpec specFiles = mTabHost
				.newTabSpec("event")
				.setIndicator(
						createView(getString(R.string.event),
								R.drawable.tab_icon_file, true))
				.setContent(R.id.LinearLayout_files);
		TabSpec specPhoto = mTabHost
				.newTabSpec("photo")
				.setIndicator(
						createView(getString(R.string.photos1),
								R.drawable.tab_icon_user, false))
				.setContent(R.id.LinearLayout_photo);
		TabSpec specSetting = mTabHost
				.newTabSpec("set")
				.setIndicator(
						createView(getString(R.string.cam_setting),
								R.drawable.tab_icon_setup, false))
				.setContent(R.id.LinearLayout_set);

		mTabHost.addTab(specCam);
		mTabHost.addTab(specFiles);
		mTabHost.addTab(specPhoto);
		mTabHost.addTab(specSetting);
		mTabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {

			public void onTabChanged(String tabId) {
				if (tabId.equals("cam")) {
					ELog.i(TAG, "到了摄像头列表");
					mCameraListView.setHead();
					AppServer.isDisplayVideo = true;
				}
				if (tabId.equals("event")) {
					mCameraListView.stopCam();
					ELog.i(TAG, "到了event");
					setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
					SharedPreferences sp = getSharedPreferences("evenInfo",
							MODE_PRIVATE);
					CameraListInfo.currentCam.setAlertNum(0);
					sp.edit()
							.putInt(String.valueOf(CameraListInfo.currentCam
									.getId()), 0).commit();
					updateAlertNum();
					mFileListView.setHead();
					mFileListView.getEvent();
				}
				if (tabId.equals("photo")) {
					mCameraListView.stopCam();
					ELog.i(TAG, "到了photo");
					setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
					mPhotoListView.setHead();

				}
				if (tabId.equals("set")) {
					mCameraListView.stopCam();
					setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
					mPhotoListView.recycleBm();
					ELog.i(TAG, "到了set");
				}

			}
		});
	}

	// 创建一个自定义布局
	private View createView(String name, int drawable, boolean isShowNum) {
		View view = inflater_tab.inflate(R.layout.tabhost_custom, null);
		TextView tv_name = (TextView) view.findViewById(R.id.tabsText);
		tv_name.setText(name);
		ImageView iv = (ImageView) view.findViewById(R.id.icon);
		iv.setImageResource(drawable);
		TextView alarNum = (TextView) view.findViewById(R.id.alar_num);
		alarNum.setText(CameraListInfo.currentCam.getAlertNum() + "");
		if (CameraListInfo.currentCam.getAlertNum() > 0) {
			alarNum.setVisibility(isShowNum ? View.VISIBLE : View.GONE);
		}
		return view;
	}

	public void showRequestDialog(String note) {
		if (mDialog != null) {
			mDialog.dismiss();
			mDialog = null;
		}
		mDialog = DialogFactory.createLoadingDialog(CameraCenterActivity.this,
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

	private void showUpdateing() {
		if (null == singleDialog) {
			singleDialog = new Builder(this);
			singleDialog.setMessage(CameraListInfo.currentCam.getName()
					+ getString(R.string.confirm_cam_updating));
			singleDialog.setTitle(getString(R.string.note));
			singleDialog.setCancelable(false);
			singleDialog.setPositiveButton(
					getResources().getString(R.string.confirm),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							finish();
							singleDialog = null;
						}
					});
			singleDialog.create().show();
		}

	}

	private void showOffLineDilalog() {
		// if (null == singleDialog) {
		singleDialog = new Builder(this);
		singleDialog.setMessage(CameraListInfo.currentCam.getName()
				+ getString(R.string.off_line));
		singleDialog.setTitle(getString(R.string.note));
		singleDialog.setCancelable(false);
		singleDialog.setPositiveButton(
				getResources().getString(R.string.confirm),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						finish();
						singleDialog = null;
					}
				});
		singleDialog.create().show();
		// }

	}

	public void showUpdateCamRomDialog() {
		if (null == singleDialog) {
			singleDialog = new Builder(this);
			singleDialog.setMessage(getResources().getString(
					R.string.confirm_cam_update));
			singleDialog.setTitle(getString(R.string.note));
			singleDialog.setCancelable(false);
			singleDialog.setPositiveButton(
					getResources().getString(R.string.confirm),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							sf.updateCamVersion();
							singleDialog = null;
						}
					});
			singleDialog.setNegativeButton(
					getResources().getString(R.string.btn_cancel),
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							singleDialog = null;
						}
					});
			singleDialog.create().show();
		}
	}

	public void showRestartCamDialog() {
		if (null == singleDialog) {
			singleDialog = new Builder(this);
			singleDialog.setMessage(getResources().getString(
					R.string.sure_cam_restart));
			singleDialog.setTitle(getString(R.string.note));
			singleDialog.setCancelable(false);
			singleDialog.setPositiveButton(
					getResources().getString(R.string.confirm),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							sf.restartCam();
							CameraListInfo.currentCam.setStatus(0);
							singleDialog = null;
							finish();
						}
					});
			singleDialog.setNegativeButton(
					getResources().getString(R.string.btn_cancel),
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							singleDialog = null;
						}
					});
			singleDialog.create().show();
		}
	}

	public void showAccessPasswordErrorDialog() {
		mCameraListView.stopCam();
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
										sf.modifyCamInfo(CameraListInfo.currentCam);
										sf.userCamPswCheck(CameraListInfo.currentCam);
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

	public void showShareTimeDialog() {
		LayoutInflater inflater = (LayoutInflater) getApplicationContext()
				.getSystemService(LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.share_alert_style, null);

		final EditText timeEdit = (EditText) view.findViewById(R.id.time_edit);

		if (null == singleDialog) {
			singleDialog = new AlertDialog.Builder(this);
			singleDialog.setView(view);
			singleDialog.setIcon(android.R.drawable.ic_dialog_info);
			singleDialog.setTitle(R.string.share_cam_note);
			singleDialog.setCancelable(false);
			singleDialog.setPositiveButton(getString(R.string.btn_cancel),
					new DialogInterface.OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							singleDialog = null;
						}
					});
			singleDialog
					.setNegativeButton(getString(R.string.share_fix),
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									String timelong = "";
									if (!timeEdit.getText().toString()
											.equals("")) {
										timelong = Integer.parseInt(timeEdit
												.getText().toString())
												* 60
												+ "";
									}
									String token = "<cameraid="
											+ CameraListInfo.currentCam.getId()
											+ "><userid="
											+ SocketFunction.getInstance().userInfo
													.getUserId()
											+ "><utc="
											+ new Date().getTime()
											/ 1000
											+ "><validity="
											+ timelong
											+ "><shareName=anycam><type=0><pwd=><accesskey="
											+ CameraListInfo.currentCam
													.getAccessKey() + ">";
									ELog.i(TAG, "token:" + token);
									// new AesUtils().encrypt(token,
									// "010151B446D50000");
									byte[] aes = new AesUtils().encrypt(token,
											"010151B446D50000");
									ELog.i(TAG, "aes长度:" + aes.length);
									String base64 = Base64.encodeToString(aes,
											Base64.NO_WRAP);
									final String shareUrl = "http://myanycam.cn/videolive.php?token="
											+ base64;
									final String shareEncodeUrl = "http://myanycam.cn/videolive.php?token="
											+ URLEncoder.encode(base64);
									ELog.i(TAG, "share:" + shareUrl);
									new Thread(new Runnable() {

										@Override
										public void run() {
											try {

												String shortUrl = shortUrlParse("http://api.t.sina.com.cn/short_url/shorten.xml?source=3213676317&url_long="
														+ shareEncodeUrl);
												FileUtils
														.saveFile(
																cretaeBitmap("http://t.cn/8FTzhfB"),
																"aaa.jpg",
																Environment
																		.getExternalStorageDirectory()
																		.getPath()
																		+ "/myanycam/");
											
												// ShareParams sp = new
												// ShareParams();
												// sp.shareType =
												// Platform.SHARE_WEBPAGE;
												// sp.title= "Myanycam分享";
												// sp.imagePath = Environment
												// .getExternalStorageDirectory()
												// .getPath()
												// + "/myanycam/aaa.jpg";
												// sp.url = shortUrl;
												// sp.text = shortUrl;
												// Platform platform =
												// ShareSDK.getPlatform(CameraCenterActivity.this,
												// Wechat.NAME);
												// platform.share(sp);
											
										
												// mHandler.sendEmptyMessage(TOSHARE);
												// Intent intent = new Intent(
												// Intent.ACTION_SEND);
												// intent.setType("image/*");
												// intent.putExtra(
												// Intent.EXTRA_STREAM,
												// Uri.parse(Environment
												// .getExternalStorageDirectory()
												// .getPath()
												// + "/myanycam/aaa.jpg"));
												// intent.putExtra(
												// Intent.EXTRA_SUBJECT,
												// "直播分享");
												// intent.putExtra(
												// Intent.EXTRA_TEXT,
												// shortUrl);
												// intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
												// startActivity(Intent
												// .createChooser(intent,
												// "直播分享"));

											} catch (MalformedURLException e) {
												// TODO Auto-generated catch
												// block
												e.printStackTrace();
											} catch (WriterException e) {
												// TODO Auto-generated catch
												// block
												e.printStackTrace();
											} catch (IOException e) {
												// TODO Auto-generated catch
												// block
												e.printStackTrace();
											}

										}
									}).start();
									// JsonReader jsonReader = new
									// JarEntry(name)
									// http://api.t.sina.com.cn/short_url/shorten.json?source=2230613802&url_long=%@"
									dialog.dismiss();
									singleDialog = null;
								}
							}).create().show();
		}

	}

	public void dimissDialog() {
		if (mDialog != null) {
			mDialog.dismiss();
			mDialog = null;
		}
	}

	public void sendWatchCam() {
		ELog.i(TAG, "camtercenter发了watch");
		sf.watchCameraLocal();
	}

	public void stopWatchCam() {
		sf.stopWatchCamerLocal();
	}

	private String shortUrlParse(String xmlUri) {
		String shortUrl = "";
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(xmlUri);
			// 获取根节点
			Element element = document.getDocumentElement();
			NodeList nodelist = element.getChildNodes();
			int size = nodelist.getLength();
			ELog.i(TAG, "size:" + size);
			for (int i = 0; i < size; i++) {
				Node element2 = (Node) nodelist.item(i);
				String tagName = element2.getNodeName();
				if (tagName.equals("url")) {
					NodeList urlChildList = element2.getChildNodes();
					int urlChildSize = urlChildList.getLength();
					ELog.i(TAG, "子节点数目:" + urlChildSize);
					for (int j = 0; j < urlChildSize; j++) {
						Node element3 = (Node) urlChildList.item(j);
						String tagNameChild = element3.getNodeName();
						ELog.i(TAG, "子节点:" + tagNameChild);
						if (tagNameChild.equals("url_short")) {
							// ELog.i(TAG, "找到节点："+element3.getTextContent());
							shortUrl = element3.getTextContent();
							return shortUrl;
						}
					}
				}

			}
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return shortUrl;
	}

	/**
	 * 生成二维码
	 * 
	 * @param 字符串
	 * @return Bitmap
	 * @throws WriterException
	 */
	public Bitmap cretaeBitmap(String str) throws WriterException {
		int IMAGE_HALFWIDTH = 30;
		Bitmap logoBitmap = BitmapCache.getBitmapFromMemCache(
				CameraCenterActivity.this, CameraListInfo.currentCam.getId()
						+ "");
		if (null == logoBitmap) {
			logoBitmap = BitmapFactory.decodeResource(getResources(),
					R.drawable.default_image);
		}
		// 缩放图片
		Matrix m = new Matrix();
		float sx = (float) 2 * IMAGE_HALFWIDTH / logoBitmap.getWidth();
		float sy = (float) 2 * IMAGE_HALFWIDTH / logoBitmap.getHeight();
		m.setScale(sx, sy);
		// 重新构造一个40*40的图片
		logoBitmap = Bitmap.createBitmap(logoBitmap, 0, 0,
				logoBitmap.getWidth(), logoBitmap.getHeight(), m, false);
		Hashtable<EncodeHintType, Object> hints = new Hashtable<EncodeHintType, Object>();
		hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
		hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
		// hints.put(EncodeHintType.MARGIN, 1);

		// 生成二维矩阵,编码时指定大小,不要生成了图片以后再进行缩放,这样会模糊导致识别失败
		// BitMatrix matrix= new MultiFormatWriter().encode(str,
		// BarcodeFormat.QR_CODE, 300, 300);
		BitMatrix matrix = new MultiFormatWriter().encode(str,
				BarcodeFormat.QR_CODE, 300, 300, hints);
		int width = matrix.getWidth();
		int height = matrix.getHeight();
		// 二维矩阵转为一维像素数组,也就是一直横着排了
		int halfW = width / 2;
		int halfH = height / 2;
		int[] pixels = new int[width * height];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				if (x > halfW - IMAGE_HALFWIDTH && x < halfW + IMAGE_HALFWIDTH
						&& y > halfH - IMAGE_HALFWIDTH
						&& y < halfH + IMAGE_HALFWIDTH) {
					pixels[y * width + x] = logoBitmap.getPixel(x - halfW
							+ IMAGE_HALFWIDTH, y - halfH + IMAGE_HALFWIDTH);
				} else {
					if (matrix.get(x, y)) {
						pixels[y * width + x] = 0xff000000;
					} else { // 无信息设置像素点为白色
						pixels[y * width + x] = 0xffffffff;
					}
				}

			}
		}
		Bitmap bitmap = Bitmap.createBitmap(width, height,
				Bitmap.Config.ARGB_8888);
		// 通过像素数组生成bitmap
		bitmap.setPixels(pixels, 0, width, 0, 0, width, height);

		return bitmap;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 1) {
			ELog.i(TAG, "从相册回来");
			mPhotoListView.setHead();
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

}
