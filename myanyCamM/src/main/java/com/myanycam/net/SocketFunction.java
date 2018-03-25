package com.myanycam.net;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;

import org.videolan.vlc.Util;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Environment;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;

import com.myanycam.bean.ActionCmd;
import com.myanycam.bean.CameraListInfo;
import com.myanycam.bean.MainSocket;
import com.myanycam.bean.McuInfo;
import com.myanycam.bean.PicEventInfo;
import com.myanycam.bean.UserInfo;
import com.myanycam.bean.VideoEventInfo;
import com.myanycamm.cam.WelcomeActivity;
import com.myanycamm.utils.Constants;
import com.myanycamm.utils.ELog;
import com.myanycamm.utils.FileUtils;
import com.myanycamm.utils.FormatTransfer;
import com.myanycamm.utils.Utils;

public class SocketFunction extends Application {
	private static String TAG = "SocketFunction";

	private static SocketFunction instance;
	public final static String SLEEP_INTENT = "org.videolan.vlc.SleepIntent";

	public static byte[] data3;
	public static boolean isVerify = false;
	public UserInfo userInfo = new UserInfo();
	private Handler mHandler = null;
	public McuInfo mMcuInfo = new McuInfo();
	public McuInfo mMcuInfo1 = new McuInfo();
	public McuInfo mMcuInfo2 = new McuInfo();
	public McuInfo mMcuInfo3 = new McuInfo();
	private ActionCmd mActionCmd = new ActionCmd();
	public UdpSocket mUdpSocket;
	private int timeOutTimes = 0;// 超时次数
	
	/** OPlayer SD卡缓存路�? */
	public static final String OPLAYER_CACHE_BASE = Environment.getExternalStorageDirectory() + "/myanycam";
	/** 视频截图缓冲路径 */
	public static final String OPLAYER_VIDEO_THUMB = OPLAYER_CACHE_BASE + "/thumb/";
	/** 首次扫描 */
	public static final String PREF_KEY_FIRST = "application_first";


	@Override
	public void onCreate() {
		super.onCreate();
		Log.i(TAG, "oncreate");
		instance = this;
		Util.context = getAppContext();
		Log.i(TAG, "context:" + getAppContext());
		init();
	}
	

	private void init() {
		//创建缓存目录
		com.nmbb.oplayer.util.FileUtils.createIfNoExists(OPLAYER_CACHE_BASE);
		com.nmbb.oplayer.util.FileUtils.createIfNoExists(OPLAYER_VIDEO_THUMB);
	}
	

	public static Context getAppContext() {
		return instance;
	}

	public static SocketFunction getInstance() {
		return instance;
	}

	public static Resources getAppResources() {
		if (instance == null)
			return null;
		return instance.getResources();
	}

	public Handler getmHandler() {
		return mHandler;
	}

	public void setmHandler(Handler mHandler) {
		this.mHandler = mHandler;
	}

	public ActionCmd getmActionCmd() {
		return mActionCmd;
	}

	public void setmActionCmd(ActionCmd mActionCmd) {
		this.mActionCmd = mActionCmd;
	}

	public void sendCmd(final String[] args) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				String cmd = "";
				// OutputStream out;
				try {
					// out = MainSocket.getInstance().getOutputStream();
					ByteArrayOutputStream outByte = new ByteArrayOutputStream();
					int enter = 0;
					for (int i = 0; i < args.length; i++) {
						cmd = cmd + args[i];
					}
					ELog.i(TAG, MainSocket.getInstance().getInetAddress()
							+ "  " + MainSocket.getInstance().getPort());
					ELog.i(TAG, "cmd:" + cmd);
					int len1 = cmd.length() + 1;
					int total_len = len1 + 4;
					byte[] total = FormatTransfer.toLH(total_len);
					byte[] len = FormatTransfer.toLH(len1);
					byte[] outmessage = cmd.getBytes();
					outByte.write(total);
					outByte.write(len);
					outByte.write(outmessage);
					outByte.write(enter);
					MainSocket.sendDate(outByte.toByteArray());
					// outData.write(outByte.toByteArray());
					// out.write(total);
					// out.write(len);
					// out.write(outmessage);
					// out.write(enter);
					// out.flush();
					// outData.flush();
					ELog.i(TAG, "发送成功...");
					MainSocket.getInstance().setSoTimeout(10000);

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}).start();

		// return newSocket;
	}

	public void register(String userName, String password) {
		String[] strings = { "<xns=XNS_CLIENT>", "<cmd=REGISTER>",
				"<account=" + userName + ">",
				"<password=" + FormatTransfer.getMd5Value(password) + ">" };
		sendCmd(strings);
	}


	
	public void senActionCmd(final ActionCmd acmd) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				String cmd = "<xns=" + acmd.getXns() + ">" + "<cmd="
						+ acmd.getCmd() + ">";
				OutputStream out;
				String[] args = acmd.getAppends();
				try {
					out = MainSocket.getInstance().getOutputStream();
					int enter = 0;
					for (int i = 0; i < args.length; i++) {
						cmd = cmd + args[i];
					}
					ELog.i(TAG, MainSocket.getInstance().getInetAddress()
							+ "  " + MainSocket.getInstance().getPort());
					ELog.i(TAG, "cmd:" + cmd);
					int len1 = cmd.length() + 1;
					int total_len = len1 + 4;
					byte[] total = FormatTransfer.toLH(total_len);
					byte[] len = FormatTransfer.toLH(len1);
					byte[] outmessage = cmd.getBytes();
					out.write(total);
					out.write(len);
					out.write(outmessage);
					out.write(enter);
					out.flush();
					MainSocket.getInstance().setSoTimeout(10000);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}).start();

	}

	public void login(int isBackground) {
		// 在线，不需要再登录
		// if (userInfo.getUserId() != 0) {
		// if (mHandler !=null) {
		//
		// mHandler.sendEmptyMessage(15);// 欢迎界面和登录界面都用
		// }
		// return;
		// }
		SharedPreferences sp = getSharedPreferences("passwordFile",
				MODE_PRIVATE);
		if (sp.getAll().size() != 0) {
			String[] allUserName = sp.getAll().keySet().toArray(new String[0]);
			if (sp.getString(allUserName[0], "").equals("")) {
				mHandler.sendEmptyMessage(WelcomeActivity.NOUSER);
			} else {
				loginReal(allUserName[0], sp.getString(allUserName[0], ""),
						isBackground);
			}
		} else if (mHandler != null) {
			ELog.i(TAG, "找不到保存的名字");
			mHandler.sendEmptyMessage(WelcomeActivity.NOUSER);
		}

	}

	public void loginReal(String username, String psw, int isBackground) {
		userInfo.setName(username);
		userInfo.setPassword(psw);
		mActionCmd.setXns("XNS_CLIENT");
		mActionCmd.setCmd("LOGIN");
		ELog.i(TAG, "version:" + Utils.getAppVersionName(getAppContext()));
		String[] strings = {
				"<account=" + userInfo.getName() + ">",
				"<password="
						+ FormatTransfer.getMd5Value(userInfo.getPassword())
						+ ">", "<type=5>",
				"<logintype=" + userInfo.getLoginType() + ">",
				"<logintoken=" + userInfo.getLoginToken() + ">",
				"<version=" + Utils.getAppVersionName(getAppContext()) + ">",
				"<demonic=" + isBackground + ">", "<partner=3>" };
		mActionCmd.setAppends(strings);
		senActionCmd(mActionCmd);

		// String[] strings = { "<xns=XNS_CLIENT>", "<cmd=LOGIN>",
		// "<account=" + userInfo.getName() + ">",
		// "<password=" + FormatTransfer.getMd5Value(userInfo.getPassword()) +
		// ">",
		// "<version=1.0>"};
		//
		// sendCmd(strings);
	}

	public void getSerIP() {
		try {

			if (changeSocket(Constants.SOCKETIP, Constants.SOCKETIPPORT, -1)) {
				String[] strings = { "<xns=XNS_CLIENT>", "<cmd=GET_AGENT_ADDR>" };
				sendCmd(strings);
			} else {
				// 验证失败，重来
				// getSerIP();
				ELog.i(TAG, "验证失败");
			}
		} catch (IOException e) {
			ELog.i(TAG, "连接主Socket超时" + e.getMessage());
			Constants.SOCKETIP = Constants.SOCKET_SECOND_IP;
			timeOutTimes++;
			if (timeOutTimes >= 3) {
				ELog.i(TAG, "超时3次");
				timeOutTimes = 0;
				if (mHandler != null) {
					mHandler.sendEmptyMessage(WelcomeActivity.SHOWRETRY);
				}
			} else {
				getSerIP();
			}

		}

	}

	public String getIPByDomain() {
		ELog.i(TAG, "http://" + Constants.SOCKETIP);
		if (Utils.checkIfURLExists("http://" + Constants.SOCKETIP, 2)) {
			ELog.i(TAG, "成功了..返回域名");
			return Constants.SOCKETIP;
		} else {
			ELog.i(TAG, "失败，返回ip");
			return Constants.SOCKET_SECOND_IP;
		}

	}

	public void ChangeCamDirection(int direction, int step) {
		String[] strings = { "<xns=XNS_CLIENT>", "<cmd=MODIFY_CAMERA_PTZ>",
				"<userid=" + userInfo.getUserId() + ">",
				"<cameraid=" + CameraListInfo.currentCam.getId() + ">",
				"<direction=" + direction + ">", "<step=" + step + ">" };
		sendCmd(strings);
	}

	public boolean changeSocket(String ip, int port, int timeOut)
			throws IOException {
		boolean isVer = false;
		ELog.i(TAG, "新socket,ip:" + ip + "port:" + port);
		// MainSocket.getInstance().c
		MainSocket.closeMain();// 先关闭以前的
		int time = (timeOut == -1) ? Constants.SOCKETTIMEOUT : timeOut;
		MainSocket.getInstance().connect(new InetSocketAddress(ip, port), time);
		isVer = sendVerifyData(MainSocket.getInstance());

		ELog.i(TAG, "连接到了Socket:" + MainSocket.getInstance().hashCode());
		return isVer;
	}

	public boolean sendVerifyData(Socket s) throws IOException {
		isVerify = false;// 换地址都要重新验证
		int nType = 1002;
		int nLen = 0;
		byte[] nT = FormatTransfer.toLH(nType);
		byte[] nL = FormatTransfer.toLH(nLen);
		data3 = new byte[nT.length + nL.length];
		System.arraycopy(nT, 0, data3, 0, nT.length);
		System.arraycopy(nL, 0, data3, nT.length, nL.length);
		OutputStream out = s.getOutputStream();
		out.write(data3);
		out.flush();
		InputStream in = s.getInputStream();
		byte[] b = new byte[8];
		in.read(b, 0, 8);
		String string = new String(b);
		ELog.i(TAG, "得到:" + FormatTransfer.hBytesToInt(b));
		if (Arrays.equals(b, SocketFunction.data3)) {
			ELog.i(TAG, "验证过了");
			isVerify = true;
		} else {
			isVerify = false;
			ELog.i(TAG, "没验证过");
		}
		return isVerify;
	}

	public void callMasterRespon(int ret) {
		String[] strings = { "<xns=XNS_CLIENT>", "<cmd=CALL_MASTER_RESP>",
				"<userid=" + userInfo.getUserId() + ">",
				"<cameraid=" + CameraListInfo.currentCam.getId() + ">",
				"<ret=" + ret + ">" };
		sendCmd(strings);
	}

	public void getCameraNetWorkInfo() {
		String[] strings = { "<xns=XNS_CLIENT>", "<cmd= GET_NETWORK_INFO>" };
		sendCmd(strings);
	}

	public void getCamVersion() {
		String[] strings = { "<xns=XNS_CLIENT>", "<cmd=GET_CAMERA_VERSION>",
				"<userid=" + userInfo.getUserId() + ">",
				"<cameraid=" + CameraListInfo.currentCam.getId() + ">" };
		sendCmd(strings);
	}

	public void restartCam() {
		String[] strings = { "<xns=XNS_CLIENT>", "<cmd=REBOOT_CAMERA>",
				"<userid=" + userInfo.getUserId() + ">",
				"<cameraid=" + CameraListInfo.currentCam.getId() + ">" };
		sendCmd(strings);
	}
	
	public void  deviceStatus(){
		String[] strings = {
				"<xns=XNS_CLIENT>",
				"<cmd=DEVICE_STATUS>",
				"<cameraid=" + CameraListInfo.currentCam.getId() + ">"};
		sendCmd(strings);
	}

	public void updateCamVersion() {
		String[] strings = {
				"<xns=XNS_CLIENT>",
				"<cmd=UPDATE_CAMERA>",
				"<userid=" + userInfo.getUserId() + ">",
				"<cameraid=" + CameraListInfo.currentCam.getId() + ">",
				"<downloadurl=" + CameraListInfo.currentCam.getRomDownloadUrl()
						+ ">" };
		sendCmd(strings);
	}

	public void setRecordConfig(CameraListInfo c) {
		SharedPreferences sp = getSharedPreferences("SP", MODE_PRIVATE);
		String[] strings = {
				"<xns=XNS_CLIENT>",
				"<cmd=RECORD_CONFIG>",
				"<userid=" + userInfo.getUserId() + ">",
				"<cameraid=" + c.getId() + ">",
				"<switch=" + sp.getString("rec_switch", "0") + ">",
				"<repeat=" + sp.getString("rec_repeat", "0111110") + ">",
				"<policy=" + sp.getString("rec_policy", "0") + ">",
				"<beintime1=" + sp.getString("rec_begintime1", "00:00:00")
						+ ">",
				"<beintime2=" + sp.getString("rec_begintime2", "00:00:00")
						+ ">",
				"<beintime3=" + sp.getString("rec_begintime3", "00:00:00")
						+ ">",
				"<beintime4=" + sp.getString("rec_begintime4", "00:00:00")
						+ ">",
				"<endtime1=" + sp.getString("rec_endtime1", "00:00:00") + ">",
				"<endtime2=" + sp.getString("rec_endtime2", "00:00:00") + ">",
				"<endtime3=" + sp.getString("rec_endtime3", "00:00:00") + ">",
				"<endtime4=" + sp.getString("rec_endtime4", "00:00:00") + ">" };
		sendCmd(strings);
	}

	public void getAlarmConfig(CameraListInfo c) {
		SharedPreferences sp = getSharedPreferences("SP", MODE_PRIVATE);
		String[] strings = { "<xns=XNS_CLIENT>", "<cmd=GET_ALARM_CONFIG>",
				"<userid=" + userInfo.getUserId() + ">",
				"<cameraid=" + c.getId() + ">" };

		sendCmd(strings);
	}

	public void setAlarmConfig(CameraListInfo c) {
		SharedPreferences sp = getSharedPreferences("SP", MODE_PRIVATE);
		String[] strings = {
				"<xns=XNS_CLIENT>",
				"<cmd=ALARM_CONFIG>",
				"<userid=" + userInfo.getUserId() + ">",
				"<cameraid=" + c.getId() + ">",
				"<switch=" + sp.getString("ala_switch", "0") + ">",
				"<repeat=" + sp.getString("ala_repeat", "0111110") + ">",
				"<policy=" + sp.getString("ala_policy", "0") + ">",
				"<record=" + sp.getString("ala_record", "0") + ">",
				"<voicealarm=" + sp.getString("ala_voicealarm", "0") + ">",
				"<movealarm=" + sp.getString("ala_movealarm", "0") + ">",
				"<switch1=" + sp.getString("ala_switch1", "0") + ">",
				"<begintime1=" + sp.getString("ala_begintime1", "00:00:00")
						+ ">",
				"<endtime1=" + sp.getString("ala_endtime1", "00:00:00") + ">",
				"<switch2=" + sp.getString("ala_switch2", "0") + ">",
				"<begintime2=" + sp.getString("ala_begintime2", "00:00:00")
						+ ">",
				"<endtime2=" + sp.getString("ala_endtime2", "00:00:00") + ">",
				"<switch3=" + sp.getString("ala_switch3", "0") + ">",
				"<begintime3=" + sp.getString("ala_begintime3", "00:00:00")
						+ ">",

				"<endtime3=" + sp.getString("ala_endtime3", "00:00:00") + ">",
				"<switch4=" + sp.getString("ala_switch4", "0") + ">",
				"<begintime4=" + sp.getString("ala_begintime4", "00:00:00")
						+ ">",

				"<endtime4=" + sp.getString("ala_endtime4", "00:00:00") + ">" };
		sendCmd(strings);
	}

	public void addCamera(String sn, String psw, String name) {
		String[] strings = {
				"<xns=XNS_CLIENT>",
				"<cmd=ADD_CAMERA>",
				"<userid=" + userInfo.getUserId() + ">",
				"<sn=" + sn + ">",
				"<password=" + psw + ">",
				"<name="
						+ Base64.encodeToString(name.getBytes(), Base64.NO_WRAP)
						+ ">", "<type=0>", "<timezone=8>" };
		sendCmd(strings);
	}

	public void getRecordConfig(CameraListInfo c) {
		String[] strings = { "<xns=XNS_CLIENT>", "<cmd=GET_RECORD_CONFIG>",
				"<userid=" + userInfo.getUserId() + ">",
				"<cameraid=" + c.getId() + ">" };
		sendCmd(strings);
	}

	public void getPictureList(CameraListInfo c, int pos) {
		String[] strings = { "<xns=XNS_CLIENT>", "<cmd=GET_PICTURE_LIST>",
				"<userid=" + userInfo.getUserId() + ">",
				"<cameraid=" + c.getId() + ">", "<pos=" + pos + ">" };
		sendCmd(strings);
	}

	public void shareCamSwitch(int arg) {
		String[] strings = { "<xns=XNS_CLIENT>", "<cmd=SHARE_CAMERA>",
				"<userid=" + userInfo.getUserId() + ">",
				"<switch=" + arg + ">",
				"<cameraid=" + CameraListInfo.currentCam.getId() + ">", };
		sendCmd(strings);
	}

	public void deletePic(CameraListInfo c, PicEventInfo p) {
		String[] strings = { "<xns=XNS_CLIENT>", "<cmd=DELETE_PICTURE>",
				"<cameraid=" + c.getId() + ">",
				"<userid=" + userInfo.getUserId() + ">",
				"<filename=" + p.getTotalName() + ">" };
		sendCmd(strings);
	}

	public boolean downloadPic(CameraListInfo c, PicEventInfo p) {
		String path = FileUtils.getSavePath("eventphoto") + "/"
				+ CameraListInfo.currentCam.getId() + p.getTotalName();
		if (FileUtils.isExistFile(path)) {
			ELog.i(TAG, "图片存在..无需下载..");
			return true;
		}
		String[] strings = { "<xns=XNS_CLIENT>", "<cmd=DOWNLOAD_PICTURE>",
				"<userid=" + userInfo.getUserId() + ">",
				"<cameraid=" + c.getId() + ">",
				"<filename=" + p.getTotalName() + ">" };
		sendCmd(strings);
		return false;
	}

	public void getVideoList(CameraListInfo c, int pos) {
		String[] strings = { "<xns=XNS_CLIENT>", "<cmd=GET_VIDEO_LIST>",
				"<userid=" + userInfo.getUserId() + ">",
				"<cameraid=" + c.getId() + ">", "<pos=" + pos + ">" };
		sendCmd(strings);
	}

	public void deleteVideo(CameraListInfo c, VideoEventInfo v) {
		String[] strings = { "<xns=XNS_CLIENT>", "<cmd=DELETE_VIDEO>",
				"<cameraid=" + c.getId() + ">",
				"<userid=" + userInfo.getUserId() + ">",
				"<filename=" + v.getTotalName() + ">" };
		sendCmd(strings);
	}

	public void downLoadVideo(CameraListInfo c, String name) {
		String[] strings = { "<xns=XNS_CLIENT>", "<cmd=DOWNLOAD_VIDEO>",
				"<userid=" + userInfo.getUserId() + ">",
				"<cameraid=" + c.getId() + ">", "<filename=" + name + ">" };
		sendCmd(strings);
	}

	public void manualRecord(int what) {
		String[] strings = { "<xns=XNS_CLIENT>", "<cmd=MANUAL_RECORD>",
				"<userid=" + userInfo.getUserId() + ">",
				"<switch=" + what + ">",
				"<cameraid=" + CameraListInfo.currentCam.getId() + ">" };
		sendCmd(strings);
	}

	public void manualSnap() {
		String[] strings = { "<xns=XNS_CLIENT>", "<cmd=MANUAL_SNAP>",
				"<userid=" + userInfo.getUserId() + ">",
				"<cameraid=" + CameraListInfo.currentCam.getId() + ">" };
		sendCmd(strings);
	}

	public void getDeviceConfig() {
		SharedPreferences sp = getSharedPreferences("SP", MODE_PRIVATE);
		String[] strings = { "<xns=XNS_CLIENT>", "<cmd=GET_DEVICE_INFO>" };
		sendCmd(strings);
	}

	public void setDeviceConfig(String psw) {
		String[] strings = { "<xns=XNS_CLIENT>", "<cmd=CONIFG>",
				"<password=" + psw + ">" };
		sendCmd(strings);
	}

	public void setTimeZone(long time, int timeZone) {
		String[] strings = { "<xns=XNS_CLIENT>", "<cmd=SET_CAMERA_TIME_ZONE>",
				"<time=" + time + ">", "<timezone=" + timeZone + ">" };
		sendCmd(strings);
	}

	public void downloadCamera() {
		CameraListInfo.cams.clear();
		// while (CameraListInfo.cams.size()>1) {
		// CameraListInfo.cams.remove(0);
		// }
		String[] strings = { "<xns=XNS_CLIENT>", "<cmd=DOWNLOAD_CAMERA>",
				"<userid=" + userInfo.getUserId() + ">" };
		sendCmd(strings);
	}

	public void modifyCamInfo(CameraListInfo c) {
		ELog.i(TAG, "memo:" + c.getMemo());
		String[] strings = {
				"<xns=XNS_CLIENT>",
				"<cmd=MODIFY_CAMERA>",
				"<cameraid=" + c.getId() + ">",
				"<sn=" + c.getSn() + ">",
				"<password=" + c.getPassWord() + ">",
				"<type=" + c.getType() + ">",
				"<userid=" + userInfo.getUserId() + ">",
				"<name="
						+ Base64.encodeToString(c.getName().getBytes(),
								Base64.DEFAULT).trim() + ">",
				"<memo="
						+ Base64.encodeToString(c.getMemo().getBytes(),
								Base64.DEFAULT).trim() + ">" };
		sendCmd(strings);
	}

	// 删除摄像头
	public void deleteCam(CameraListInfo c) {
		String[] strings = { "<xns=XNS_CLIENT>", "<cmd=DELETE_CAMERA>",
				"<userid=" + userInfo.getUserId() + ">",
				"<cameraid=" + c.getId() + ">" };
		sendCmd(strings);
	}

	public void audioSwitch(CameraListInfo c, int channelid, int s) {
		String[] strings = { "<xns=XNS_CLIENT>", "<cmd=SPEAKER_SWITCH>",
				"<mcuip=" + mMcuInfo.getIp() + ">",
				"<mcuport=" + mMcuInfo.getPort() + ">",
				"<userid=" + userInfo.getUserId() + ">",
				"<cameraid=" + c.getId() + ">",
				"<channelid=" + channelid + ">", "<switch=" + s + ">" };
		// 0:
		// 关闭
		// 1打开
		sendCmd(strings);
	}

	public void getAudioSwitch(CameraListInfo c, int channelid, int s) {
		String[] strings = { "<xns=XNS_CLIENT>", "<cmd=AUDIO_SWITCH>",
				"<mcuip=" + mMcuInfo.getIp() + ">",
				"<mcuport=" + mMcuInfo.getPort() + ">",
				"<userid=" + userInfo.getUserId() + ">",
				"<cameraid=" + c.getId() + ">",
				"<channelid=" + channelid + ">", "<switch=" + s + ">" };
		// 0:
		// 关闭
		// 1打开
		sendCmd(strings);
	}

	public void sendHeartBeat() {
		// int nType = 1;
		int nLen = 1;
		byte content = 0;
		// byte[] nT = FormatTransfer.toLH(nType);
		byte[] nL = FormatTransfer.toLH(nLen);
		byte[] sendContent = new byte[nL.length + 1];
		// System.arraycopy(nT, 0, sendContent, 0, nT.length);
		System.arraycopy(nL, 0, sendContent, 0, nL.length);
		sendContent[nL.length] = content;
		MainSocket.getInstance().sendDate(sendContent);
		// OutputStream out;
		// try {
		// out = MainSocket.getInstance().getOutputStream();
		// out.write(sendContent);
		// out.flush();
		// } catch (IOException e) {
		//
		// e.printStackTrace();
		// }

	}

	public void watchCamera(CameraListInfo c) {
		String[] strings = { "<xns=XNS_CLIENT>", "<cmd=WATCH_CAMERA>",
				"<userid=" + userInfo.getUserId() + ">",
				"<cameraid=" + c.getId() + ">",
				"<password=" + c.getPassWord() + ">",
				"<natip=" + mUdpSocket.natAddress + ">",
				"<natport=" + mUdpSocket.natPort + ">",
				"<localip=" + mUdpSocket.localIp + ">",
				"<localport=" + mUdpSocket.localIpInt + ">", "<resolution=8>",
				"<videosize=" + c.getVideoSize() + ">" };
		sendCmd(strings);
	}

	public void watchCameraTcp() {
		String[] strings = { "<xns=XNS_CLIENT>", "<cmd=WATCH_CAMERA_TCP>",
				"<userid=" + userInfo.getUserId() + ">",
				"<cameraid=" + CameraListInfo.currentCam.getId() + ">",
				"<password=" + CameraListInfo.currentCam.getPassWord() + ">",
				"<resolution=8>",
				"<videosize=" + CameraListInfo.currentCam.getVideoSize() + ">" };
		sendCmd(strings);
	}

	public void watchCameraLocal() {
		String[] strings = { "<xns=XNS_CLIENT>", "<cmd=WATCH_CAMERA>" };
		sendCmd(strings);
	}

	public void watchDirectCamera(CameraListInfo c) {

		// MainSocket.getInstance().getLocalAddress().getAddress();
		String[] strings = { "<xns=XNS_CLIENT>", "<cmd=WATCH_CAMERA>",
				"<userid=" + userInfo.getUserId() + ">",
				"<cameraid=" + c.getId() + ">",
				"<password=" + c.getPassWord() + ">", "<natip=10.10.10.1>",
				"<natport=5521>", "<localip=192.168.1.121>",
				"<localport=" + mUdpSocket.dsSend.getLocalPort() + ">",
				"<videosize=2>" };
		sendCmd(strings);
	}

	public void stopWatchCamer(CameraListInfo c) {
		String[] strings = { "<xns=XNS_CLIENT>", "<cmd=STOP_WATCH_CAMERA>",
				"<userid=" + userInfo.getUserId() + ">",
				"<cameraid=" + c.getId() + ">",
				"<password=" + c.getPassWord() + ">" };
		sendCmd(strings);
	}

	public void stopWatchCamerLocal() {
		String[] strings = { "<xns=XNS_CLIENT>", "<cmd=STOP_WATCH_CAMERA>" };
		sendCmd(strings);
	}

	public void modifyCamera(CameraListInfo c, int channelid) {
		ELog.i(TAG, "发送修改视频命令");
		String[] strings = { "<xns=XNS_CLIENT>", "<cmd=MODIFY_VIDEO_SIZE>",
				"<userid=" + userInfo.getUserId() + ">",
				"<cameraid=" + c.getId() + ">",
				"<password=" + c.getPassWord() + ">",
				"<mcuip=" + mMcuInfo.getIp() + ">",
				"<mcuport=" + mMcuInfo.getPort() + ">",
				"<channelid=" + channelid + ">",
				"<videosize=" + c.getVideoSize() + ">", "<quality=2>" };
		ELog.i(TAG, "...");
		sendCmd(strings);
	}

	// videosize=
	// 4:VGA640*480 5:720*480 6:720P 7:1080P
	// 本地用
	public void modifyRecVdideoQuality(String quality) {
		String[] strings = { "<xns=XNS_CLIENT>",
				"<cmd=MODIFY_RECORD_VIDEO_QUALITY>",
				"<userid=" + userInfo.getUserId() + ">",
				"<cameraid=" + CameraListInfo.currentCam.getId() + ">",
				"<videosize=" + quality + ">" };
		sendCmd(strings);
	}

	public void modifyLiveVdideoQuality(String quality) {
		String[] strings = { "<xns=XNS_CLIENT>",
				"<cmd=MODIFY_LIVE_VIDEO_QUALITY>",
				"<userid=" + userInfo.getUserId() + ">",
				"<cameraid=" + CameraListInfo.currentCam.getId() + ">",
				"<videosize=" + quality + ">" };
		sendCmd(strings);
	}

	public void setVideoRotate(int vflip) {
		String[] strings = { "<xns=XNS_CLIENT>", "<cmd=SET_VIDEO_ROTATE>",
				"<userid=" + userInfo.getUserId() + ">",
				"<cameraid=" + CameraListInfo.currentCam.getId() + ">",
				"<vflip=" + vflip + ">" };
		sendCmd(strings);
	}

	public void getLiveVdideoQuality() {
		String[] strings = { "<xns=XNS_CLIENT>",
				"<cmd=GET_LIVE_VIDEO_SIZE_QUALITY>" };
		sendCmd(strings);
	}

	public void getRecVdideoQuality() {
		String[] strings = { "<xns=XNS_CLIENT>",
				"<cmd=GET_RECORD_VIDEO_SIZE_QUALITY>" };
		sendCmd(strings);
	}

	public void getCameraWifi() throws IOException {
		if (null != CameraListInfo.currentCam) {
			String[] strings = { "<xns=XNS_CLIENT>", "<cmd=GET_WIFI_INFO>",
					"<userid=" + userInfo.getUserId() + ">",
					"<cameraid=" + CameraListInfo.currentCam.getId() + ">" };
			sendCmd(strings);
		} else {
			String[] strings = { "<xns=XNS_CLIENT>", "<cmd=GET_WIFI_INFO>" };
			sendCmd(strings);
		}

	}

	public void setWifiInfo(String ssid, String safety, String password) {
		String[] strings = { "<xns=XNS_CLIENT>", "<cmd=SET_WIFI_INFO>",
				"<wifi=1>", "<ssid=" + ssid + ">", "<safety=" + safety + ">",
				"<password=" + password + ">",
				"<userid=" + userInfo.getUserId() + ">",
				"<cameraid=" + CameraListInfo.currentCam.getId() + ">" };
		sendCmd(strings);
	}

	public void getNetworkInfo() {
		String[] strings = { "<xns= XNS_CLIENT>", "<cmd=GET_NETWORK_INFO>" };
		sendCmd(strings);
	}

	public void setNetworkInfo(String dhcp, String ip, String mask,
			String netgate, String dns1, String dns2) {
		String[] strings = { "<xns=XNS_CLIENT>", "<cmd=MODIFY_NETWORK_INFO>",
				"<dhcp=" + dhcp + ">", "<ip=" + ip + ">",
				"<mask=" + mask + ">", "<netgate=" + netgate + ">",
				"<dns1=" + dns1 + ">", "<dns2=" + dns2 + ">" };
		sendCmd(strings);
	}

	public void modifyPsw(String newPsw) {
		String[] strings = {
				"<xns=XNS_CLIENT>",
				"<cmd=MODIFY_PWD>",
				"<userid=" + userInfo.getUserId() + ">",
				"<oldpassword="
						+ FormatTransfer.getMd5Value(userInfo.getPassword())
						+ ">",
				"<newpassword=" + FormatTransfer.getMd5Value(newPsw) + ">" };
		sendCmd(strings);
	}

	public void getMcu() {
		String[] strings = { "<xns=XNS_CLIENT>", "<cmd=GET_MCU>",
				"<userid=" + userInfo.getUserId() + ">" };
		sendCmd(strings);

	}

	public void getCamSnap(CameraListInfo c) {
		String[] strings = { "<xns=XNS_CLIENT>", "<cmd=GET_CAMERA_SNAP>",
				"<userid=" + userInfo.getUserId() + ">",
				"<password=" + c.getPassWord() + ">",
				"<cameraid=" + c.getId() + ">" };
		sendCmd(strings);
	}

	public void userCamPswCheck(CameraListInfo c) {
		String[] strings = { "<xns=XNS_CLIENT>", "<cmd=USER_PROVEN>",
				"<userid=" + userInfo.getUserId() + ">",
				"<password=" + c.getPassWord() + ">",
				"<cameraid=" + c.getId() + ">" };
		sendCmd(strings);
	}

	// public void reLogin() {
	//
	//
	// new Thread(new Runnable() {
	//
	// @Override
	// public void run() {
	// try {
	// if (MainSocket.getInstance() != null
	// && MainSocket.getInstance().isConnected()) {
	//
	// MainSocket.getInstance().close();
	// }
	//
	// if (changeSocket(Constants.LOCALSOCKETIP,
	// Constants.LOCALSOCKETIPPORT, 2000) ) {
	// AppServer.isAp = true;
	// if (!AppServer.isBackgroud) {
	// ScreenManager.getScreenManager().toWelcome();
	// }
	//
	//
	//
	//
	//
	//
	// ScreenManager.getScreenManager().currentActivity().startActivity(intent);
	// } else {
	// if (!AppServer.isBackgroud) {
	// ScreenManager.getScreenManager().toWelcome();
	// }else{
	// getSerIP();
	// }
	//
	// }
	//
	//
	//
	// } catch (IOException e) {
	//
	// getSerIP();
	// e.printStackTrace();
	// }
	// }
	// }).start();
	//
	// }

}
