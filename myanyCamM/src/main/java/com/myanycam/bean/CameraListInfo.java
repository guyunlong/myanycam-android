package com.myanycam.bean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.SharedPreferences;

import com.myanycamm.utils.ELog;


public class CameraListInfo implements Serializable{

	private static String TAG = "CameraListInfo";
	public static  ArrayList<CameraListInfo> cams = new ArrayList<CameraListInfo>();
	public static CameraListInfo currentCam;

	private String name;
	private int id;
	private String passWord;// 摄像头密码
	private String sn;
	private boolean access = true;//是否有权限
	private boolean isUpnp = false;//是否upnp成功
	byte type;
	int status;// 当前状态 离线 0:在线 1：,升级中:3
	String memo;// 备注
	int count;// 总个数
	int nowcount;//当前是第几个
	private String localIp,trueUrl;//摄像头本地ip,图片地址
	private	int localPort;
	private String natIP;//摄像头natIp
	private int natPort;//摄像头nat端口
	private int alertNum = 0;	//报警信息条数
	private int videoSize = 2;//默认为1,1:流畅,2:清晰
	private String romVersion,romDownloadUrl,newRomVersion;		
	private String accessKey = "";
	private String shareSwitch="0";
	private int vflip = 0;
	
	


	
	public int getVflip() {
		return vflip;
	}

	public void setVflip(int vflip) {
		this.vflip = vflip;
	}

	public String getShareSwitch() {
		return shareSwitch;
	}

	public void setShareSwitch(String shareSwitch) {
		this.shareSwitch = shareSwitch;
	}

	public String getAccessKey() {
		return accessKey;
	}

	public void setAccessKey(String accessKey) {
		this.accessKey = accessKey;
	}

	public boolean isUpnp() {
		return isUpnp;
	}

	public void setUpnp(boolean isUpnp) {
		this.isUpnp = isUpnp;
	}

	public boolean isAccess() {
		return access;
	}

	public void setAccess(boolean access) {
		this.access = access;
	}

	public String getTrueUrl() {
		return trueUrl;
	}

	public void setTrueUrl(String trueUrl) {
		ELog.i(TAG, this.sn + ":"+trueUrl);
		this.trueUrl = trueUrl;
	}



	public int getNowcount() {
		return nowcount;
	}

	public void setNowcount(int nowcount) {
		this.nowcount = nowcount;
	}



	public int getVideoSize() {
		return videoSize;
	}

	public void setVideoSize(int videoSize) {
		this.videoSize = videoSize;
	}

	public String getRomDownloadUrl() {
		return romDownloadUrl;
	}

	public void setRomDownloadUrl(String romDownloadUrl) {
		this.romDownloadUrl = romDownloadUrl;
	}

	public String getNewRomVersion() {
		return newRomVersion;
	}

	public void setNewRomVersion(String newRomVersion) {
		this.newRomVersion = newRomVersion;
	}

	public String getRomVersion() {
		return romVersion;
	}

	public void setRomVersion(String romVersion) {
		this.romVersion = romVersion;
	}

	public static void setCurrentCam(CameraListInfo _currentCam){
		currentCam = _currentCam;
	}
	
	public int getAlertNum() {
		return alertNum;
	}

	public void setAlertNum(int alertNum) {
		this.alertNum = alertNum<99?alertNum:99;
	}
	

	public String getLocalIp() {
		return localIp;
	}

	public void setLocalIp(String localIp) {
		this.localIp = localIp;
	}

	public int getLocalPort() {
		return localPort;
	}

	public void setLocalPort(int localPort) {
		this.localPort = localPort;
	}

	public int getNatPort() {
		return natPort;
	}

	public void setNatPort(int natPort) {
		this.natPort = natPort;
	}

	public String getNatIP() {
		return natIP;
	}

	public void setNatIP(String natIP) {
		this.natIP = natIP;
	}


	public byte getType() {
		return type;
	}

	public void setType(byte type) {
		this.type = type;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String getMemo() {
		return memo;
	}

	public void setMemo(String memo) {
		this.memo = memo;
	}

	public String getSn() {
		return sn;
	}

	public void setSn(String sn) {
		this.sn = sn;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getPassWord() {
		return passWord;
	}

	public void setPassWord(String passWord) {
		this.passWord = passWord;
	}
	

}
