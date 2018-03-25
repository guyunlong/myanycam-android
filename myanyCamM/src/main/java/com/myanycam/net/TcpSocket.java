package com.myanycam.net;

import android.util.Log;

import com.myanycam.bean.CameraListInfo;
import com.myanycam.bean.Vdata;
import com.myanycam.bean.VideoData;
import com.myanycamm.utils.ELog;
import com.myanycamm.utils.FormatTransfer;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import gyl.cam.SoundPlay;

import static com.thSDK.lib.jvideo_decode_frame;
import static com.thSDK.lib.jvideo_decode_init;

public class TcpSocket extends Socket{
	private String TAG = "TcpSocket";
	private static final byte CMD_SEND_DATA = 3;
	private final byte AUDIOADPCM = 2;
	private int videoSize = 0;
	private boolean isTcpRecive = true;
	Socket videoSocket;
	private static TcpSocket instance;
	private static Timer timer = null;
	public int rateLast = 0,tempRate = 0;
	byte[] receivePakage;

	private TcpSocket() {
		// TODO Auto-generated constructor stub
	}
	public static TcpSocket getInstance(){
		if (null == instance) {
			instance = new TcpSocket();
		}
		return instance;
	}
	
	public void connect(final String ip, final int port) {
	 new Thread(new Runnable() {

			@Override
			public void run() {
				ELog.i(TAG, "准备连接视频tcp  ip:"+ ip +"端口:"+port);
				try {
					getInstance().connect(new InetSocketAddress(ip, port), 3000);
					int nType = 1002;
					int nLen = 0;
					byte[] nT = FormatTransfer.toLH(nType);
					byte[] nL = FormatTransfer.toLH(nLen);
					byte[] data3 = new byte[nT.length + nL.length];
					System.arraycopy(nT, 0, data3, 0, nT.length);
					System.arraycopy(nL, 0, data3, nT.length, nL.length);
					OutputStream out = getInstance().getOutputStream();
					out.write(data3);
					out.flush();
					new Timer().scheduleAtFixedRate(new TimerTask() {
						
						@Override
						public void run() {
							rateLast = tempRate;
							tempRate = 0;							
						}
					}, new Date(), 1000);
					while (isTcpRecive) {
						try {
							InputStream in = getInstance().getInputStream();
							// ELog.i(TAG, "read:" + in.read());
							byte[] len = new byte[4];
							in.read(len, 0, 4);
							int iLen = FormatTransfer.hBytesToInt(len);
//						
							if ((iLen < 0)  || (iLen>204800)){
								ELog.i(TAG, "长度 error:" +iLen);
								 in = null;
								 continue;
							}
							receivePakage = new byte[iLen];

							int haveRead = in.read(receivePakage, 0, iLen);
							
							ELog.i(TAG, Thread.currentThread().getName()+"应该长度："+iLen +" 实际长度:"+haveRead);
							while (haveRead <iLen) {
								haveRead += in.read(receivePakage, haveRead, iLen - haveRead);
								ELog.i(TAG, "应该长度："+iLen +" 实际长度:"+haveRead);
							}
							tempRate += haveRead;
							byte[] videoWithCmd = null;
							if (iLen > 5) {
								videoWithCmd = new byte[iLen - 5];// 减去命令类型和通道号
							}

							switch (receivePakage[0]) {
							case CMD_SEND_DATA:
								try {
									System.arraycopy(receivePakage, 5,
											videoWithCmd, 0, iLen - 5);
								} catch (ArrayIndexOutOfBoundsException e) {
									ELog.i(TAG, "拷频数据错误。");
									e.printStackTrace();
									return;
								}
								switch (videoWithCmd[0]) {
								case 0:
									ELog.i(TAG, "收到视频 tcp ");

									// VideoData.videoTreeMap.put(Integer.toString(iTimeStamp),
									// videoWithCmd);
									synchronized(VideoData.Videolist){
										videoSize = videoWithCmd[6];
										Vdata v = new Vdata();
										v.setVideoData(videoWithCmd);
										VideoData.Videolist.add(v);
									}


									//parseFrame(videoWithCmd,videoWithCmd.length);

									//
									break;
								case 1:

									// System.arraycopy(reciverPacke, 16,
									// videoWithCmd, 0, iLen -
									// 5);
									SoundPlay.isAdpcm = videoWithCmd[1] == AUDIOADPCM ? true
											: false;
									VideoData.audioArraryList.add(videoWithCmd);
								default:
									break;
								}

								break;
							default:
								break;
							}
							in = null;
							
							try {
								Thread.sleep(25);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}catch (ArrayIndexOutOfBoundsException e) {
							// TODO: handle exception
						}

					}
				} catch (IOException e1) {
					ELog.i(TAG, "视频tcp连接失败...");
					e1.printStackTrace();
				}

			}
		}).start();

	}
	int recType = -1;
	int type = -1;
	int width;
	int height;

	void parseFrame(byte []buf,int total) {
		// Log.e("gyl", "parse");

		//

		if (buf[6] != recType) {
			recType = buf[6];

			if (buf[1] == 0x00) {
				Log.e("gyl", "mpeg code");
				type = 0;
			}
			if (buf[1] == 0x01) {
				Log.e("gyl", "h264 code");
				type = 1;
			}
			if (buf[6] == 0x00) {

				width = 160;
				height = 120;
			}
			if (buf[6] == 0x01) {
				width = 176;
				height = 144;
			}
			if (buf[6] == 0x02) {
				width = 320;
				height = 240;
			}
			if (buf[6] == 0x03) {
				width = 352;
				height = 288;
			}
			if (buf[6] == 0x04) {
				width = 640;
				height = 480;
			}
			if (buf[6] == 0x05) {
				width = 720;
				height = 480;
			}
			if (buf[6] == 0x06) {
				width = 1280;
				height = 720;
			}
			if (buf[6] == 0x07) {
				width = 1920;
				height = 1080;
			}
			if (buf[6] == 0x08) {
				width = 640;
				height = 360;
			}
			if (buf[6] == 0x09) {
				width = 320;
				height = 180;
			}
			ELog.i(TAG, "width:" + width + "height:" + height);
			jvideo_decode_init(type, width, height);
		}

		byte[] bmp = new byte[total - 7];
		for (int i = 0; i < total - 7; i++) {
			bmp[i] = buf[i + 7];
		}
		//ByteBuffer subbuf = ByteBuffer.wrap(buf,7,total-7);
		long parseTimeBefore = System.currentTimeMillis();
		ELog.i(TAG, "tcp 解码---------0" );
		//synchronized (this)
		{
			int n = jvideo_decode_frame( bmp, total - 7);
			ELog.e(TAG, "tcp 解码n:" + n);
		}

		//
		// FileUtils.saveByteToFile(yuv,
		// Environment.getExternalStorageDirectory().getPath()+"/my.yuv");
		// VideoData.yuvArrayList.add(yuv);
		//long parseTimeAfter = System.currentTimeMillis();
		//parseTime = (int) (parseTimeAfter - parseTimeBefore);
		//


	}
	public void stopTcpSocket(){
		ELog.i(TAG, "停止了TcpSocket...");
		isTcpRecive = false;
		colseTcp();
		Thread.currentThread().interrupt();
	}
	
	public void sendVoiceToCam(byte[] adpcm) {
		ELog.i(TAG, "TCp发送声音");
		// 组包媒体数据
		byte mediaType = 1;
		byte mediaFormat = AUDIOADPCM;
		int time = (int) System.currentTimeMillis();
		byte[] tempTime = FormatTransfer.toLH(time);
		byte mediaSize = 0;
		byte[] mediaHead = new byte[7];
		System.arraycopy(tempTime, 0, mediaHead, 2, 4);
		mediaHead[0] = mediaType;
		mediaHead[1] = mediaFormat;
		mediaHead[6] = mediaSize;
		byte[] voiceDataWithHead = new byte[adpcm.length + 7];
		System.arraycopy(mediaHead, 0, voiceDataWithHead, 0, 7);
		System.arraycopy(adpcm, 0, voiceDataWithHead, 7, adpcm.length);

		byte cmd = CMD_SEND_DATA;
		byte[] channelIdByte = FormatTransfer.toLH(CameraListInfo.currentCam.getId());

		// 组包udp信息数据
		// byte[] messageHead = { '@', '%', '^', '!' };
//		byte[] messageData = new byte[4];
//		System.arraycopy(channelIdByte, 0, messageData, 0, 4);

		// 组包mcu数据
		int totalLen = voiceDataWithHead.length + channelIdByte.length + 1;
		byte[] totalLenByte = FormatTransfer.toLH(totalLen);

		// 总包
		byte[] sendVoiceTotalByte = new byte[voiceDataWithHead.length + 4 + 1 +4];
		sendVoiceTotalByte[4] = cmd;
//		System.arraycopy(channelIdByte, 0, sendVoiceTotalByte, 0, 12);
		System.arraycopy(totalLenByte, 0, sendVoiceTotalByte, 0, 4);
		System.arraycopy(channelIdByte, 0, sendVoiceTotalByte, 5, 4);
		System.arraycopy(voiceDataWithHead, 0, sendVoiceTotalByte, 9,
				voiceDataWithHead.length);
//		dpSend = new DatagramPacket(sendVoiceTotalByte,
//				sendVoiceTotalByte.length, p2pInetAddress, p2pPort);
//		try {
//			dsSend.send(dpSend);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		ByteArrayOutputStream outByte = new ByteArrayOutputStream();
		try {
			outByte.write(sendVoiceTotalByte);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		sendDate(outByte.toByteArray());
		//
	}
	
	public void colseTcp(){
		if (instance != null) {
			try {
				instance.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		instance = null;
	}
	
	public static synchronized void sendDate(final byte[] data){
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					DataOutputStream outData = new DataOutputStream(getInstance().getOutputStream());
					outData.write(data);
					outData.flush();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
				
			}
		}).start();
	
	}
	
}
