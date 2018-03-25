package com.myanycam.bean;


public class Vdata implements Comparable {
	public int packageId;
	public int reciverPackeNum = 0;// 实际的包数
	public int recivePackeLen = 0;// 实际收到的包长度
	public int totalPackage = 10000;//总包数，初始为1万，以防分母为0
	public int timeStamp;
	private byte[] videoData;
	private byte[] mcuData;

	public byte[] getVideoData() {
		return videoData;
	}

	public void setVideoData(byte[] videoData) {
		this.videoData = videoData;
	}

	public byte[] getMcuData() {
		return mcuData;
	}

	public void setMcuData(byte[] mcuData) {
		this.mcuData = mcuData;
	}

	@Override
	public boolean equals(Object obj) {

		if (!(obj instanceof Vdata))
			return false;
		Vdata v = (Vdata) obj;
		return this.packageId == v.packageId;

	}

	@Override
	public int hashCode() {
		return timeStamp;
	}

	//根据时间排序
	@Override
	public int compareTo(Object another) {
		Vdata v = (Vdata) another;
		if (this.timeStamp > v.timeStamp) {
			return 1;
		} else {
			return -1;
		}

	}
}
