package com.myanycam.bean;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeMap;

public class VideoData {
	// private int id;
	public int reciverPackeNum=0;// 实际的包数
	public int recivePackeLen = 0;// 实际收到的包长度
	private byte[] mcuData;
	private int timeStamp;//时间
	private byte[] videoByte;
//	public static TreeMap<String, byte[]> videoTreeMap = new TreeMap<String, byte[]>();
//	public static ArrayList<byte[]> videoArraryList = new ArrayList<byte[]>();//存真正的视频数据
	public static ArrayList<Vdata> Videolist = new ArrayList<Vdata>();
	public static ArrayList<byte[]> audioArraryList = new ArrayList<byte[]>();//存音频数据
	public static ArrayList<byte[]> yuvArrayList = new ArrayList<byte[]>();//存yuv数据
//	public static TreeMap<String, Set> dropVideoMap = new TreeMap<String, Set>();
	
	
	
	public byte[] getVideoByte() {
		return videoByte;
	}

	public void setVideoByte(byte[] videoByte) {
		this.videoByte = videoByte;
	}

	public int getReciverPackeNum() {
		return reciverPackeNum;
	}

	public void setReciverPackeNum(int reciverPackeNum) {
		this.reciverPackeNum = reciverPackeNum;
	}

	public int getRecivePackeLen() {
		return recivePackeLen;
	}

	public void setRecivePackeLen(int recivePackeLen) {
		this.recivePackeLen = recivePackeLen;
	}

	public byte[] getMcuData() {
		return mcuData;
	}

	public void setMcuData(byte[] mcuData) {
		this.mcuData = mcuData;
	}
	
	public static void clear(){
		Videolist.clear();
		audioArraryList.clear();
	}

}
