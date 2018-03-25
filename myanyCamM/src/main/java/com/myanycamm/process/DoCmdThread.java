package com.myanycamm.process;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Base64;
import android.widget.Toast;

import com.myanycam.bean.CameraListInfo;
import com.myanycam.bean.CmdType;
import com.myanycam.bean.MainSocket;
import com.myanycam.net.SocketFunction;
import com.myanycam.net.UdpSocket;
import com.myanycamm.cam.AddCameraActivity;
import com.myanycamm.cam.AppServer;
import com.myanycamm.cam.CallAcceptActivity;
import com.myanycamm.cam.CameraCenterActivity;
import com.myanycamm.cam.ChangPasswordActivity;
import com.myanycamm.cam.LoginActivity;
import com.myanycamm.cam.ModifyCameraInfoActivity;
import com.myanycamm.cam.R;
import com.myanycamm.cam.WelcomeActivity;
import com.myanycamm.model.BitmapCache;
import com.myanycamm.setting.LocalNetSettingActivity;
import com.myanycamm.setting.QualitySettingActivity;
import com.myanycamm.setting.RecSettingActivity;
import com.myanycamm.setting.SysSettingActivity;
import com.myanycamm.setting.WifiSettingActivity;
import com.myanycamm.ui.DialogFactory;
import com.myanycamm.ui.PhotoEvent;
import com.myanycamm.utils.ELog;
import com.myanycamm.utils.FileUtils;
import com.myanycamm.utils.NotificationUtils;
import com.myanycamm.utils.SharePrefereUtils;

public class DoCmdThread extends Thread {
	private String TAG = "DoCmdThread";
	private HashMap<String, String> map;
	public static LinkedList<HashMap<String, String>> cmdMaps = new LinkedList<HashMap<String, String>>();
	private ArrayList<CameraListInfo> tempCams = new ArrayList<CameraListInfo>();
	private int dealy = 200;

	public DoCmdThread() {
		super();
	}

	@Override
	public void run() {
		while (!AppServer.stop) {
			if (cmdMaps.size() > 0) {
				map = cmdMaps.getFirst();
				Bundle mBundle = new Bundle();
				mBundle.putSerializable("data", map);
				Message msg = new Message();
				switch (CmdType.getCmdType(map.get("cmd"))) {
				case DEVICE_STATUS:
					ELog.i(TAG, "收到电量标志："+map + "");
					msg.what = CameraCenterActivity.DEVICE_STATUS;
					msg.setData(mBundle);
					SocketFunction.getInstance().getmHandler()
							.sendMessage(msg);
					break;

				case GET_AGENT_ADDR_RESP:
					AppServer.isAp = false;
					ELog.i(TAG, map + "");
					try {
						if (SocketFunction.getInstance().changeSocket(
								map.get("serverip").toString(),
								Integer.parseInt(map.get("serverport")
										.toString()), -1)) {
							ELog.i(TAG, "连接代理地址成功");
							HeartBeatThread.getInstance().start();
							SocketFunction.getInstance().login(0);
							SocketFunction.getInstance().userInfo.setNatIp(map.get("natip"));
						} else {
							SocketFunction.getInstance().getSerIP();
						}
					} catch (IOException e) {
						// SocketFunction.getInstance().getmHandler().sendEmptyMessage(LoginActivity.RELOGIN);
						if (!AppServer.isBackgroud) {
							SocketFunction
									.getInstance()
									.getmHandler()
									.sendEmptyMessage(WelcomeActivity.SHOWRETRY);
						} else {
							SocketFunction.getInstance().getSerIP();
						}
						ELog.i(TAG, "没有连上服务器" + e.getMessage());
						e.printStackTrace();
					}
					// }else{
					//
					// }

					// SocketFunction.getInstance().registerReal();
					break;
				case LOGIN_RESP:
					ELog.i(TAG, "登录返回");

					if (map.get("ret").equals("0")) {
						ELog.i(TAG, "登录成功"
								+ MainSocket.getInstance().getLocalPort());
						// String ip =
						// MainSocket.getInstance().getLocalAddress().getAddress().toString();
						SocketFunction.getInstance().userInfo.setUserId(Integer
								.parseInt(map.get("userid")));
						SharePrefereUtils.commitStringData(
								SocketFunction.getAppContext(), "upgradetype",
								map.get("upgradetype"));
						SharePrefereUtils.commitStringData(
								SocketFunction.getAppContext(), "newversion",
								map.get("newversion"));
						NotificationUtils.clearNotification(SocketFunction
								.getAppContext().getApplicationContext());
						SharedPreferences sp = SocketFunction.getAppContext()
								.getSharedPreferences("SP",
										Context.MODE_PRIVATE);
						ELog.i(TAG, "是否忽略了:" + sp.contains("1.0.3"));
						if (!map.get("upgradetype").equals("0")
								&& !sp.getBoolean(map.get("newversion"), false)) {// 本地是否忽略
							msg.what = 123;// 主界面和欢迎界面都要用
						} else {
							msg.what = 15;// 欢迎界面和登录界面都用
							SocketFunction.getInstance().getMcu();
							SocketFunction.getInstance().downloadCamera();
						}

					} else if (map.get("ret").equals("3")) {
						ELog.i(TAG, "已经登录");
						msg.what = 3;// 欢迎界面和登录界面都会用到
					} else {
						ELog.i(TAG, "用户名或密码错误" + map.get("ret"));
						msg.what = 1;// 欢迎界面和登录界面都会用到
					}
					msg.setData(mBundle);
					if (null != SocketFunction.getInstance().getmHandler()) {
						SocketFunction.getInstance().getmHandler()
								.sendMessage(msg);
					}

					break;
				case WIFI_INFO:
					ELog.i(TAG, "当前正在使用的wifi");
					msg.what = WifiSettingActivity.CURRENTLINK;
					msg.setData(mBundle);
					SocketFunction.getInstance().getmHandler().sendMessage(msg);
					break;
				case HOT_SPOT:

					ELog.i(TAG, "其他wifi列表");
					msg.what = WifiSettingActivity.OTHERLINK;
					msg.setData(mBundle);
					SocketFunction.getInstance().getmHandler().sendMessage(msg);
					break;
				case DOWNLOAD_CAMERA_RESP:
					ELog.i(TAG, "获取摄像头列表成功");

					if (map.get("count") != null
							&& Integer.parseInt(map.get("count")) == 0) {
						CameraListInfo.cams.clear();
						// CameraListInfo.cams.add(null);
					} else {
						String cName = "";
						String cMemo = "";
						CameraListInfo cTemp = new CameraListInfo();
						try {
							byte[] camNameB = Base64.decode(map.get("name"),
									Base64.DEFAULT);
							byte[] camMemoB = Base64.decode(map.get("memo"),
									Base64.DEFAULT);
							cName = new String(camNameB);
							cMemo = new String(camMemoB);
						} catch (IllegalArgumentException e2) {
							// 如果不是base64,直接显示
							cName = map.get("name");
							cMemo = map.get("memo");
						}
						ELog.w(TAG, "增加:" + map.get("cameraid") + " 名字:"
								+ cName);
						cTemp.setName(cName);
						cTemp.setId(Integer
								.parseInt(map.get("cameraid").trim()));
						cTemp.setPassWord(map.get("password"));
						cTemp.setSn(map.get("sn"));
						cTemp.setMemo(cMemo);
						cTemp.setAlertNum(getAlarmNum(map.get("cameraid")
								.trim()));
						for (int i = 0; i < CameraListInfo.cams.size(); i++) {
							if (CameraListInfo.cams.get(i).getId() == Integer
									.parseInt(map.get("cameraid").trim())) {
								return;
							}
						}
						// CameraListInfo.cams.add(0, cTemp);
						tempCams.add(cTemp);
						CameraListInfo.cams.addAll(tempCams);
						tempCams.clear();
					}

					if (null != SocketFunction.getInstance().getmHandler()
							&& ScreenManager.getScreenManager()
									.currentActivity().getClass()
									.equals(LoginActivity.class)) {
						try {
							if (map.get("nowcount").equals(map.get("count"))) {
								ELog.i(TAG, "有发消息..");
								
								msg.what = LoginActivity.DOWNLOADCAMRRA;
								msg.setData(mBundle);
								SocketFunction.getInstance().getmHandler()
										.sendMessage(msg);
							}
						} catch (NullPointerException e) {
							ELog.i(TAG, "新添加的...");
							msg.what = LoginActivity.DOWNLOADCAMRRA;
							msg.setData(mBundle);
							SocketFunction.getInstance().getmHandler()
									.sendMessage(msg);
						}

					}

					break;
				case CAMERA_STATUS:
					ELog.i(TAG, "更新摄像头状态:" + map);
					for (int i = 0; i < CameraListInfo.cams.size(); i++) {
						if (CameraListInfo.cams.get(i).getId() == Integer
								.parseInt(map.get("cameraid").trim())) {
							ELog.i(TAG, CameraListInfo.cams.get(i).getSn());
							CameraListInfo.cams.get(i).setStatus(
									Integer.parseInt(map.get("status".trim())));
							if (CameraListInfo.cams.get(i).getStatus() == 1) {
								SocketFunction.getInstance().getCamSnap(
										CameraListInfo.cams.get(i));
							} else {
								map.put("position", i + "");
								msg.what = LoginActivity.UPDATECAMRRA;
								msg.setData(mBundle);

								// sendMessage(msg);
								if (null != SocketFunction.getInstance()
										.getmHandler()) {
									SocketFunction.getInstance().getmHandler()
											.sendMessage(msg);
								}
							}

						}
					}
					// msg.what = LoginActivity.UPDATECAMRRA;
					// msg.setData(mBundle);
					//
					// if (null != SocketFunction.getInstance().getmHandler()) {
					// SocketFunction.getInstance().getmHandler()
					// .sendMessage(msg);
					// }
					//
					// for (int i = 0; i <
					// CameraListView.cameraListInfos.size(); i++) {
					//
					// SocketFunction.getInstance().getMcuSocket());
					// if (null != SocketFunction.getInstance().getMcuSocket()
					// &&
					// SocketFunction.getInstance().getMcuSocket().cameraListInfo.getId()
					// == Integer
					// .parseInt(map.get("cameraid").trim())) {
					// SocketFunction.getInstance().getMcuSocket().cameraListInfo.setStatus(Integer
					// .parseInt(map.get("status".trim())));
					// }
					// }

					break;
				case GET_CAMERA_SNAP_RESP:
					ELog.i(TAG, "获取快照..." + map);
					for (int i = 0; i < CameraListInfo.cams.size(); i++) {
						if (CameraListInfo.cams.get(i).getId() == Integer
								.parseInt(map.get("cameraid").trim())) {
							map.put("position", i + "");
							if (map.containsKey("ret")
									&& map.get("ret").equals("1")) {// 没有权限
								CameraListInfo.cams.get(i).setAccess(false);
							}
							try {
								URL url = new URL(map.get("loaclurl"));
								HttpURLConnection con = (HttpURLConnection) url
										.openConnection();
								con.setConnectTimeout(1000);
								con.setReadTimeout(1000);
								int state = con.getResponseCode();
								ELog.i(TAG, "state:" + state);
								if (state == 200) {
									CameraListInfo.cams.get(i).setTrueUrl(
											map.get("loaclurl"));
									// toProxy(proxyurl);
								} else {
									CameraListInfo.cams.get(i).setTrueUrl(
											map.get("proxyurl"));
								}
							} catch (Exception ex) {
								CameraListInfo.cams.get(i).setTrueUrl(
										map.get("proxyurl"));
								ELog.i(TAG, "连接有错误" + ex.getMessage());
								ex.printStackTrace();
							}

							break;
						}
					}

					msg.what = LoginActivity.UPDATECAMRRA;
					msg.setData(mBundle);

					// sendMessage(msg);
					if (null != SocketFunction.getInstance().getmHandler()) {
						SocketFunction.getInstance().getmHandler()
								.sendMessage(msg);
					}
					break;
				case GET_MCU_RESP:
					ELog.i(TAG, "得到MCU地址");
					SocketFunction.getInstance().mMcuInfo.setIp(map
							.get("mcuip"));
					SocketFunction.getInstance().mMcuInfo.setPort(Integer
							.parseInt(map.get("mcuport").trim()));
					SocketFunction.getInstance().mMcuInfo1.setIp(map
							.get("mcuip1"));
					SocketFunction.getInstance().mMcuInfo1.setPort(Integer
							.parseInt(map.get("mcuport1").trim()));
					SocketFunction.getInstance().mMcuInfo2.setIp(map
							.get("mcuip2"));
					SocketFunction.getInstance().mMcuInfo2.setPort(Integer
							.parseInt(map.get("mcuport2").trim()));
					SocketFunction.getInstance().mMcuInfo3.setIp(map
							.get("mcuip3"));
					SocketFunction.getInstance().mMcuInfo3.setPort(Integer
							.parseInt(map.get("mcuport3").trim()));
					ELog.i(TAG,
							"mcu地址:"
									+ SocketFunction.getInstance().mMcuInfo
											.getIp()
									+ " "
									+ SocketFunction.getInstance().mMcuInfo
											.getPort());
					SocketFunction.getInstance().mUdpSocket = new UdpSocket(
							SocketFunction.getInstance());

					break;
				case WATCH_CAMERA_RESP:
					ELog.i(TAG, "得到看视频命令,准备看视频");
					msg.setData(mBundle);
					if (map.get("ret").equals("1")) {
						ELog.i(TAG, "口令错误");
						msg.what = CameraCenterActivity.ACCESSPSWERRROR;
					} else if (map.get("ret").equals("2")) {
						msg.what = CameraCenterActivity.CAMMAXRESTRICT;
						ELog.i(TAG, "同时连接达最大");
					} else {
						msg.what = CameraCenterActivity.RECEVIEWATCHCAMERA;
						// SocketFunction.getInstance().mUdpSocket.setCamIpInfo(map);
					}
					SocketFunction.getInstance().getmHandler().sendMessage(msg);
					break;
				case REGISTER_RESP:
					ELog.i(TAG, "注册返回");
					msg.what = LoginActivity.REGRSPE;
					msg.setData(mBundle);
					SocketFunction.getInstance().getmHandler().sendMessage(msg);
					break;
				case CALL_HANGUP:
					ELog.i(TAG, "要挂断...");
					msg.what = CallAcceptActivity.CALL_HANGUP;
					msg.setData(mBundle);
					SocketFunction.getInstance().getmHandler().sendMessage(msg);
					break;
				case KICK_OFF:
					offLine();
					ELog.w(TAG, "被踢了，要下线 +socket:"
							+ MainSocket.getInstance().hashCode());
					break;
				case MANUAL_SNAP_RESP:				
						msg.what = CameraCenterActivity.MANUALSNAPRESP;
						msg.setData(mBundle);
						SocketFunction.getInstance().getmHandler()
								.sendMessage(msg);
					break;
				case RELOGIN:
					break;
				case NETWORK_INFO:
					ELog.i(TAG, "得到网络信息");
					msg.what = LocalNetSettingActivity.GETNETINFO;
					msg.setData(mBundle);
					SocketFunction.getInstance().getmHandler().sendMessage(msg);
					break;
				case MODIFY_NETWORK_INFO_RESP:
					ELog.i(TAG, "修改网络信息回复..");
					msg.what = LocalNetSettingActivity.MODIFYRESP;
					msg.setData(mBundle);
					SocketFunction.getInstance().getmHandler().sendMessage(msg);
					break;

				case RECORD_CONFIG_RESP:
					ELog.i(TAG, "保存录像配置返回...");
					// 出错后，重新提交
					if (!map.get("ret").equals("0")) {
						SocketFunction.getInstance().setRecordConfig(
								CameraListInfo.currentCam);
					}
					break;
				case RECORD_CONFIG_INFO:
					ELog.i(TAG, "得到录像设置...");
					msg.what = RecSettingActivity.GET_REC_CONFIG;
					msg.setData(mBundle);
					SocketFunction.getInstance().getmHandler().sendMessage(msg);
					break;
				case ALARM_CONFIG_RESP:
					ELog.i(TAG, "保存报警设置返回...");
					break;
				case ALARM_CONFIG_INFO:
					ELog.i(TAG, "得到报警设置...");
					msg.what = RecSettingActivity.GET_ALARM_CONFIG;
					msg.setData(mBundle);
					SocketFunction.getInstance().getmHandler().sendMessage(msg);
					break;
				case DEVICE_INFO:
					ELog.i(TAG, "得到摄像头信息");
					msg.what = SysSettingActivity.DEVICE_INFO;
					msg.setData(mBundle);
					SocketFunction.getInstance().getmHandler().sendMessage(msg);
					break;
				case CONIFG_RESP:
					ELog.i(TAG, "设置摄像头系统信息返回");
					break;
				case MANUAL_RECORD_RESP:
					msg.what = CameraCenterActivity.MANUALRECORDRESP;
					msg.setData(mBundle);
					SocketFunction.getInstance().getmHandler()
							.sendMessage(msg);
					break;
				case ALARM_EVENT:
					SharedPreferences sp = SocketFunction.getAppContext()
							.getSharedPreferences("evenInfo",
									Context.MODE_PRIVATE);
					ELog.i(TAG, "已有信息:" + sp.getInt(map.get("cameraid"), 0));
					// if (map.get("")) {
					//
					// }
					sp.edit()
							.putInt(map.get("cameraid"),
									sp.getInt(map.get("cameraid"), 0) + 1)
							.commit();
					String camName = "";
					for (int i = 0; i < CameraListInfo.cams.size(); i++) {
						ELog.i(TAG, "循环:"+CameraListInfo.cams.get(i).getName());
						if (CameraListInfo.cams.get(i).getId() == Integer
								.parseInt(map.get("cameraid").trim())) {
							CameraListInfo.cams.get(i).setAlertNum(
									sp.getInt(map.get("cameraid"), 0));
							camName = CameraListInfo.cams.get(i).getName();
						}
					}
					// 不能用isbackground，因为要刷新界面条数
					if (null != SocketFunction.getInstance().getmHandler()) {
						SocketFunction.getInstance().getmHandler()
								.sendEmptyMessage(LoginActivity.DOWNLOADCAMRRA);
					}
//					String alarmType = null;
//					if (map.get("picture") == null) {
//						alarmType = map.get("piture");
//					} else {
//						alarmType = map.get("picture");
//					}
					ELog.i(TAG, "alarmType:" + map.get("type"));
					switch (Integer.parseInt(map.get("type").trim())) {
					case 0:
						NotificationUtils
								.showUpdateNotication(
										SocketFunction.getAppContext(),
										camName+SocketFunction
												.getAppContext()
												.getString(
														R.string.notice_move_alarm),
										camName
												+ SocketFunction
														.getAppContext()
														.getString(
																R.string.notice_move_alarm),
										true, WelcomeActivity.class);
						break;
					case 1:
						NotificationUtils
								.showUpdateNotication(
										SocketFunction.getAppContext(),
										SocketFunction
												.getAppContext()
												.getString(
														R.string.notice_voice_alarm),
										camName
												+ SocketFunction
														.getAppContext()
														.getString(
																R.string.notice_voice_alarm),
										true, WelcomeActivity.class);
						break;
					case 4:
						NotificationUtils
								.showUpdateNotication(
										SocketFunction.getAppContext(),
										SocketFunction
												.getAppContext()
												.getString(
														R.string.notice_power_low),
										camName
												+ SocketFunction
														.getAppContext()
														.getString(
																R.string.notice_power_low),
										true, WelcomeActivity.class);
						break;

					default:
						break;
					}

					ELog.i(TAG, "收到报警");

					break;
				case PICTURE_LIST_INFO:
					msg.what = CameraCenterActivity.PICTURELIST;
					msg.setData(mBundle);
					SocketFunction.getInstance().getmHandler().sendMessage(msg);
					break;
				case DOWNLOAD_PICTURE_RESP:
					ELog.i(TAG, "收到下载照片。。");
					// String localUrl = map.get("loaclurl");
					// String proxyUrl = map.get("proxyurl");
					doDownLoadPic(map);
					break;
				case VIDEO_LIST_INFO:
					ELog.i(TAG, "收到视频列表。。");
					msg.what = CameraCenterActivity.VIDEOLIST;
					msg.setData(mBundle);
					SocketFunction.getInstance().getmHandler().sendMessage(msg);
					break;
				case DOWNLOAD_VIDEO_RESP:
					ELog.i(TAG, "收到视频下载路径:");
					msg.what = CameraCenterActivity.DOWNLOADVIDEO;
					msg.setData(mBundle);
					SocketFunction.getInstance().getmHandler().sendMessage(msg);
					break;
				case ADD_CAMERA_RESP:
					ELog.i(TAG, "添加摄像头返回");
					msg.what = AddCameraActivity.ADDRESP;
					msg.setData(mBundle);
					SocketFunction.getInstance().getmHandler().sendMessage(msg);
					break;
				case DELETE_CAMERA_RESP:
					ELog.i(TAG, "删除摄像头返回");
					// if (map.get("ret").equals("0")) {
					//
					// }
					// SocketFunction.getInstance().downloadCamera();
					break;
				case MODIFY_PWD_RESP:
					ELog.i(TAG, "修改密码返回");
					msg.what = ChangPasswordActivity.CHANGRESP;
					msg.setData(mBundle);
					SocketFunction.getInstance().getmHandler().sendMessage(msg);
					break;
				case SET_WIFI_INFO_RESP:
					break;
				case MODIFY_CAMERA_RESP:
					ELog.i(TAG, "设置成功");
					msg.what = ModifyCameraInfoActivity.MODIFYRESP;
					msg.setData(mBundle);
					SocketFunction.getInstance().getmHandler().sendMessage(msg);
					break;
				case GET_LIVE_VIDEO_SIZE_QUALITY_RSP:
					msg.what = QualitySettingActivity.GETLIVEQUALITY;
					msg.setData(mBundle);
					SocketFunction.getInstance().getmHandler().sendMessage(msg);
					break;
				case GET_RECORD_VIDEO_SIZE_QUALITY_RSP:
					msg.what = QualitySettingActivity.GETRECQUALITY;
					msg.setData(mBundle);
					SocketFunction.getInstance().getmHandler().sendMessage(msg);
					break;
				case DELETE_PICTURE_RESP:
					break;
				case USER_PROVEN_RESP:
					ELog.i(TAG, "验证返回...");
					if (map.get("ret").equals("1")) {
						msg.what = CameraCenterActivity.ACCESSPSWERRROR;
						// SocketFunction.getInstance().mUdpSocket.setCamIpInfo(map);

					} else {
						msg.what = CameraCenterActivity.ACCESSPSWETRUE;
						msg.setData(mBundle);
					}
					SocketFunction.getInstance().getmHandler().sendMessage(msg);
					break;
				case START_HEART:
					HeartBeatThread.getInstance().start();
					break;
				case CALL_MASTER:
					ELog.i(TAG, "收到一键通话请求");
					startService(mBundle);
					break;
				case GET_CAMERA_VERSION_RESP:
					
					CameraListInfo.currentCam.setRomVersion(map.get("version"));
					CameraListInfo.currentCam.setNewRomVersion(map
							.get("newversion"));
					CameraListInfo.currentCam.setRomDownloadUrl(map
							.get("downloadurl"));
					msg.setData(mBundle);
					msg.what = CameraCenterActivity.UPDATEROMVERSION;
					SocketFunction.getInstance().getmHandler().sendMessage(msg);
//					SocketFunction
//							.getInstance()
//							.getmHandler()
//							.sendEmptyMessage(
//									CameraCenterActivity.UPDATEROMVERSION);
					break;
				case WATCH_CAMERA_TCP_RESP:
					msg.setData(mBundle);
					if (map.get("ret").equals("1")) {
						ELog.i(TAG, "口令错误");
						msg.what = CameraCenterActivity.ACCESSPSWERRROR;
					} else if (map.get("ret").equals("2")) {
						msg.what = CameraCenterActivity.CAMMAXRESTRICT;
						ELog.i(TAG, "同时连接达最大");
					} else {
						msg.what = CameraCenterActivity.TCPVIDEOSOCKET;
						SocketFunction.getInstance().getmHandler().sendMessage(msg);
					}
		

					break;
				default:
					break;

				}
				cmdMaps.removeFirst();
			} else {
				Thread.yield();
			}
		}

	}

	private int getAlarmNum(String camId) {
		int num = 0;
		SocketFunction.getAppContext();
		SharedPreferences sp = SocketFunction.getAppContext()
				.getSharedPreferences("evenInfo", Context.MODE_PRIVATE);
		num = sp.getInt(camId, 0);
		return num;
	}

	private void offLine() {
		AppServer.stop = true;
		// MyTimerTask.getInstance().cancel();
		MainSocket.closeMain();
		NotificationUtils.showUpdateNotication(SocketFunction.getAppContext(),
				SocketFunction.getAppContext().getString(R.string.note),
				SocketFunction.getAppContext().getString(R.string.other_login),
				true, WelcomeActivity.class);
		if (AppServer.isBackgroud) {
			ScreenManager.getScreenManager().extit();
		} else {
			DialogFactory.dialogHandler.sendEmptyMessage(0);
		}
	}

	public void startService(Bundle bundle) {
		Intent acceptIntent = new Intent(ScreenManager.getScreenManager()
				.currentActivity(), CallAcceptActivity.class);
		acceptIntent.putExtras(bundle);
		startActivitySafely(acceptIntent);
	}

	void startActivitySafely(Intent intent) {
		PowerManager pm = (PowerManager) ScreenManager.getScreenManager()
				.currentActivity().getSystemService(Context.POWER_SERVICE);
		WakeLock wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
				"MyWakeLock");
		wakeLock.acquire();
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_NO_USER_ACTION);
		try {
			ScreenManager.getScreenManager().currentActivity()
					.startActivity(intent);
		} catch (ActivityNotFoundException e) {
			Toast.makeText(ScreenManager.getScreenManager().currentActivity(),
					"无法打开", Toast.LENGTH_SHORT).show();
		} catch (SecurityException e) {
			Toast.makeText(ScreenManager.getScreenManager().currentActivity(),
					"安全问题，无法打开", Toast.LENGTH_SHORT).show();
			ELog.i(TAG,
					"Launcher does not have the permission to launch "
							+ intent
							+ ". Make sure to create a MAIN intent-filter for the corresponding activity "
							+ "or use the exported attribute for this activity.",
					e);
		}
		wakeLock.release();
	}

	public void doDownLoadPic(final HashMap<String, String> map) {
		final int result = -1;
		final String localUrl = map.get("loaclurl");
		final String proxyUrl = map.get("proxyurl");
		ELog.i(TAG, "url:" + localUrl + " proxurl:" + proxyUrl);
		// final String testUrl =
		new Thread(new Runnable() {

			@Override
			public void run() {
				int result = -1;
				try {
					URL url = new URL(localUrl);
					HttpURLConnection con = (HttpURLConnection) url
							.openConnection();
					con.setConnectTimeout(2000);
					con.setReadTimeout(2000);
					int state = con.getResponseCode();
					ELog.i(TAG, "state:" + state);
					if (state == 200) {
						result = addToEventUrl(localUrl);
						// photoAnyCamEvent.goIntent(localurl);
						// toProxy(proxyurl);
					} else {
						result = addToEventUrl(proxyUrl);
						// toProxy(proxyurl);
					}
				} catch (Exception ex) {
					result = addToEventUrl(proxyUrl);
					ELog.i(TAG, "连接有错误" + ex.getMessage());
					ex.printStackTrace();
				} finally {
					if (result == -1) {
						ELog.i(TAG, "出错了.....");
					} else {

						map.put("position", result + "");
						Bundle mBundle = new Bundle();
						mBundle.putSerializable("data", map);
						Message msg = new Message();
						msg.what = CameraCenterActivity.DOWNLOADPIC;
						msg.setData(mBundle);
						if (AppServer.isBackgroud) {
							SocketFunction.getInstance().getmHandler()
									.removeMessages(msg.what);
							return;
						} else {
							SocketFunction.getInstance().getmHandler()
									.sendMessage(msg);
						}

					}
				}
			}
		}).start();

	}

	public synchronized int addToEventUrl(String url) {
		// String
		// if (BitmapCache mBitmapCache = new BitmapCache();) {
		//
		// }
		for (int i = 0; i < PhotoEvent.photoEventList.size(); i++) {
			String[] strs = url.split("/");
			String str = strs[strs.length - 1];
			if (FileUtils.externalMemoryAvailable()) {
				String path = FileUtils.getSavePath("eventphoto") + "/"
						+ CameraListInfo.currentCam.getId() + str;
				if (!FileUtils.isExistFile(path)) {
					BitmapCache mBitmapCache = new BitmapCache();
					mBitmapCache.saveImage1(path, url);
				}
			}
			// str = str.substring(0, str.length() - 4);
			//
			//
			// "totalName:"+PhotoEvent.photoEventList.get(i).getTotalName());
			try {
				if (PhotoEvent.photoEventList.get(i).getTotalName().equals(str)) {
					PhotoEvent.photoEventList.get(i).setEventUrl(url);
					return i;
				}
			} catch (IndexOutOfBoundsException e) {
				e.printStackTrace();
				return -1;
			}

		}
		return -1;

	}

}
