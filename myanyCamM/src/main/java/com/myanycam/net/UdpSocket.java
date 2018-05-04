package com.myanycam.net;

import android.util.Log;

import com.myanycam.bean.CameraListInfo;
import com.myanycam.bean.MainSocket;
import com.myanycam.bean.Vdata;
import com.myanycam.bean.VideoData;
import com.myanycamm.model.VideoListener;
import com.myanycamm.ui.CloudLivingView;
import com.myanycamm.utils.ELog;
import com.myanycamm.utils.FormatTransfer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

import gyl.cam.SoundPlay;

import static com.thSDK.lib.jvideo_decode_frame;
import static com.thSDK.lib.jvideo_decode_init;

public class UdpSocket {

	private static String TAG = "UdpSocket";
	private SocketFunction sf;
	public String localIp;
	public int localIpInt;
	public int localPort = 0;// 传0会随机分配端口
	byte[] localportByte;
	byte[] localIpv4Byte;
	private byte[] tempVideoArray = new byte[12];
	public DatagramSocket dsSend = null;
	private int videoSize = 0;
	private boolean isUdpRecive = false;
	DatagramPacket dpSend = null;
	byte[] senData = null;
	private byte num = '1';
	public static HashMap<Integer, Vdata> vMap = new HashMap<Integer, Vdata>();

	public static TreeMap<Integer, Set> dropVideoMap = new TreeMap<Integer, Set>();
	private byte[] tempPackageVerify = new byte[4];
	public byte[] natinfo;
	public String natAddress;
	public int natPort;
	private static final byte CMD_GET_UDP_ADDR = 9;
	private static final byte CMD_SEND_DATA = 3;
	private static final byte CMD_JOIN_DATA_CHANNEL = 2;
	private int tempNatPort = 0;// 检测赛门铁克型用
	public boolean isSaimMen = false;
	InetAddress p2pInetAddress;
	int p2pPort;
	int changFlag = 0;
	int changRate = 0;
	int rate = 0;
	int rateTemp = 0;
	public int rateLast = 0;
	int senNo1Tiem = 0;
	private VideoListener mVideoListener;

	private final byte AUDIONONE = -1;
	private final byte AUDIOARMNB = 0;
	private final byte AUDIOILIB = 1;
	private final byte AUDIOADPCM = 2;
	private final byte AUDIOAAC = 3;

	private boolean isP2pSuccess = false;
	private static boolean isSendModify = false;
	private int noVideoInt = 0;
	private boolean isNoVideoListen = false;

	private int channelId;
	private SendP2PThread sendP2pThread;
	private UdpThread udpReciverThread;
	String camMcuIp;
	int camMcuPort;

	private final static int SCHEDULEVALUE = 100;// 100毫秒监听一次
	private static Timer timer = null;
	static TimerTask mTimerTask = null;

	public void setCamIpInfo(HashMap<String, String> map) {
		mTimerTask = new TimerTask() {

			public void run() {
				listenVideo();
			}

		};
		timer = new Timer();
		timer.scheduleAtFixedRate(mTimerTask, new Date(), SCHEDULEVALUE);
		ELog.i(TAG, "设置摄像头信息...");
		isSendModify = false;
		isNoVideoListen = true;
		noVideoInt = 0;
		channelId = Integer.parseInt(map.get("channelid"));
		String camLocalIp = map.get("localip");
		int camLocalPort = Integer.parseInt(map.get("localport"));
		camMcuIp = map.get("mcuip");
		camMcuPort = Integer.parseInt(map.get("mcuport"));
		String cmaNatIp = map.get("natip");
		int camNatport = Integer.parseInt(map.get("natport"));
		CameraListInfo.currentCam.setLocalIp(camLocalIp);
		CameraListInfo.currentCam.setLocalPort(camLocalPort);
		CameraListInfo.currentCam.setNatIP(cmaNatIp);

		CameraListInfo.currentCam.setNatPort(camNatport);
		sendP2pThread = new SendP2PThread();
		sendP2pThread.start();

		// try {
		//
		// dsSend.setSoTimeout(Constants.VIDEOTIMEOUT);
		// } catch (SocketException e) {
		//
		//
		// mVideoListener.noVideoListener(VideoListener.NOUDPVIDEO);
		// }
	}

	public int getChannelId() {
		return channelId;
	}

	public void setChannelId(int channelId) {
		this.channelId = channelId;
	}

	public VideoListener getmVideoListener() {
		return mVideoListener;
	}

	public void setmVideoListener(VideoListener mVideoListener) {
		this.mVideoListener = mVideoListener;
	}

	public UdpSocket(SocketFunction s) {
		sf = s;
		ELog.i(TAG, "进来UdpSocket");
		// MyTimerTask.getInstance().setmVideoDropListener(videoDropListener);
		// MyTimerTask.getInstance().setIsVideoDropListener(true);
		new Thread(new Runnable() {

			@Override
			public void run() {
				localPort = 0;
				// try {
				try {
					dsSend = new DatagramSocket(localPort);

					// TODO Auto-generated catch block

					// localPort = (short) dsSend.getLocalPort();
					localportByte = FormatTransfer.toLH(localPort);
					senData = getPackageData();
					dpSend = new DatagramPacket(senData, senData.length,
							InetAddress.getByName(sf.mMcuInfo.getIp()),
							sf.mMcuInfo.getPort());
					DatagramPacket dpSend2 = new DatagramPacket(senData,
							senData.length, InetAddress.getByName(sf.mMcuInfo2
									.getIp()), sf.mMcuInfo2.getPort());
					DatagramPacket dpSend3 = new DatagramPacket(senData,
							senData.length, InetAddress.getByName(sf.mMcuInfo3
									.getIp()), sf.mMcuInfo3.getPort());

					// dpSend = new DatagramPacket(senData, senData.length,
					// InetAddress.getByName("183.49.47.90"),
					// 5203);
					//
					// DatagramPacket dpSend1 = new DatagramPacket(senData,
					// senData.length,
					// InetAddress.getByName("23.20.125.3"),
					// 5203);
					localIpInt = dsSend.getLocalPort();
					ELog.i(TAG, "本地端口:" + dsSend.getLocalPort());
					dsSend.send(dpSend);
					dsSend.send(dpSend2);
					dsSend.send(dpSend3);
					// dsSend.send(dpSend1);
					udpReciverThread = new UdpThread();
					udpReciverThread.start();
				} catch (SocketException e) {
					e.printStackTrace();
				}
				// } catch (Exception e) {
				//
				// e.printStackTrace();
				// }
				catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}).start();
	}

	public byte[] getPackageData() {
		// int dataLen = 15;
		// byte[] dataLenByte = FormatTransfer.toLH(dataLen);
		byte[] messageHead = { '@', '%', '^', '!' };
		// System.arraycopy(messageHead, 0, messageData, 0, 4);
		byte[] cmd = new byte[1];
		cmd[0] = CMD_GET_UDP_ADDR;
		int channelId = 0;
		byte[] channelIdByte = FormatTransfer.toLH(channelId);
		byte[] data = new byte[6];
		// byte cmd = 9;
		byte[] packageData = new byte[messageHead.length + 1 + 4 + data.length];
		localIpv4Byte = getLocalIpAddress();
		System.arraycopy(localIpv4Byte, 0, data, 0, 4);
		System.arraycopy(localportByte, 0, data, 4, 2);
		System.arraycopy(messageHead, 0, packageData, 0, 4);
		System.arraycopy(cmd, 0, packageData, 4, 1);
		System.arraycopy(channelIdByte, 0, packageData, 5, 4);
		System.arraycopy(data, 0, packageData, 9, 6);
		return packageData;
	}

	public void senAudioSwitch() {
		sf.audioSwitch(CameraListInfo.currentCam, channelId, 1);
	}

	public void colseSenAudioSwitch() {
		sf.audioSwitch(CameraListInfo.currentCam, channelId, 0);
	}

	public void getAudioSwitch() {
		sf.getAudioSwitch(CameraListInfo.currentCam, channelId, 1);
	}

	public void closeAudioSwitch() {
		sf.getAudioSwitch(CameraListInfo.currentCam, channelId, 0);
	}

	public void sendVoiceToCam(byte[] adpcm) {
		ELog.i(TAG, "udp发送声音");
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
		byte[] channelIdByte = FormatTransfer.toLH(channelId);

		// 组包udp信息数据
		// byte[] messageHead = { '@', '%', '^', '!' };
		Random rd = new Random();
		int messageId = rd.nextInt();
		byte[] messageIdByte = FormatTransfer.toLH(messageId);
		short totalPackageNum = 1;
		byte[] totalPackageNumByte = FormatTransfer.toLH(totalPackageNum);
		short currenPackageNum = 0;
		byte[] currenPackageNumByte = FormatTransfer.toLH(currenPackageNum);
		byte[] messageData = new byte[12];
		System.arraycopy(channelIdByte, 0, messageData, 0, 4);
		System.arraycopy(messageIdByte, 0, messageData, 4, 4);
		System.arraycopy(totalPackageNumByte, 0, messageData, 8, 2);
		System.arraycopy(currenPackageNumByte, 0, messageData, 10, 2);

		// 组包mcu数据
		int totalLen = voiceDataWithHead.length + messageData.length + 5;
		byte[] totalLenByte = FormatTransfer.toLH(totalLen);

		// 总包
		byte[] sendVoiceTotalByte = new byte[voiceDataWithHead.length + 12 + 9];
		sendVoiceTotalByte[16] = cmd;
		System.arraycopy(messageData, 0, sendVoiceTotalByte, 0, 12);
		System.arraycopy(totalLenByte, 0, sendVoiceTotalByte, 12, 4);
		System.arraycopy(channelIdByte, 0, sendVoiceTotalByte, 17, 4);
		System.arraycopy(voiceDataWithHead, 0, sendVoiceTotalByte, 21,
				voiceDataWithHead.length);
		dpSend = new DatagramPacket(sendVoiceTotalByte,
				sendVoiceTotalByte.length, p2pInetAddress, p2pPort);
		try {
			dsSend.send(dpSend);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//
	}

	public void keepLive() {
		//
		byte[] dataP2p = new byte[1];
		dataP2p[0] = (byte) num;
		try {
			dpSend = new DatagramPacket(dataP2p, 1,
					InetAddress.getByName("111.111.111.111"), 6066);
			dsSend.send(dpSend);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void sendP2pLocal() {
		byte[] dataP2p = new byte[1];
		dataP2p[0] = (byte) num;
		InetAddress iad = null;
		try {
			iad = InetAddress.getByName(CameraListInfo.currentCam.getLocalIp());
			dpSend = new DatagramPacket(dataP2p, 1,
					InetAddress.getByName(CameraListInfo.currentCam
							.getLocalIp()),
					CameraListInfo.currentCam.getLocalPort());
			dsSend.send(dpSend);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//
		// "摄像头本地Ip:" + iad.getHostAddress() + "摄像头本地端口:"
		// + cam.getLocalPort());

	}

	public void sendP2pToCam() {
		byte[] dataP2p = new byte[1];
		dataP2p[0] = (byte) num;
		InetAddress iad = null;
		try {
			iad = InetAddress.getByName(CameraListInfo.currentCam.getNatIP());
			dpSend = new DatagramPacket(
					dataP2p,
					1,
					InetAddress.getByName(CameraListInfo.currentCam.getNatIP()),
					CameraListInfo.currentCam.getNatPort());
			dsSend.send(dpSend);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		ELog.d(TAG, "摄像头NAT Ip:" + iad.getHostAddress() + "摄像头NAT端口:"
				+ CameraListInfo.currentCam.getNatPort());

		// reciveCamP2p();
		// udpReciverThread.start();
	}

	//
	// public void reciveCamP2p() {
	// byte[] buf = new byte[10];
	// DatagramPacket dp = null;
	// dp = new DatagramPacket(buf, 10);
	//
	// try {
	// dsSend.receive(dp);
	//
	// } catch (IOException e) {
	//
	// e.printStackTrace();
	// }
	// int len = dp.getLength();// 接收数据长度
	//
	// if (buf[0] == '1') {
	//
	// cam.setLocalPort(dp.getPort());
	// sendP2pToCam(cam)
	// }
	//
	// }

	public void stopCam() {
		// isUdpRecive = false;
		ELog.i(TAG, "udpsoket stop");
		isSendModify = false;
		sendP2pThread = null;
		isNoVideoListen = false;
		closeTimer();
		// udpReciverThread = null;
		// MyTimerTask.getInstance().setIsVideoDropListener(false);
		// MyTimerTask.getInstance().setmVideoDropListener(null);
		// sf.stopWatchCamer(sf.getMcuSocket().cameraListInfo);
		// sendVoiceThread.stop();
	}

	class UdpThread extends Thread {
		@Override
		public void run() {
			isUdpRecive = true;
			while (isUdpRecive) {
				byte[] buf = new byte[2048];
				DatagramPacket dp = null;
				dp = new DatagramPacket(buf, 2048);
				//
				//
				// Syste.out.println("等待接收数据..");
				try {
					dsSend.receive(dp);
					// dsSend.setSoTimeout(Constants.VIDEOTIMEOUT);
					// dsSend.setSoTimeout(300000);
				} catch (IOException e) {
					ELog.e(TAG, "udp超时了");
					// mVideoListener.noVideoListener(VideoListener.NOUDPVIDEO);
					e.printStackTrace();
				}
				int len = dp.getLength();// 接收数据长度
				p2pInetAddress = dp.getAddress();
				p2pPort = dp.getPort();
				//
				// 监听有没有超时
				noVideoInt = 0;
				if ((dp.getLength() == 1) && (buf[0] == '1')) {
					ELog.i(TAG, "接收到摄像头发来的1,p2p成功");
					changRate = 6 * 60 * 1024;// 刚开始不降码流
					senNo1Tiem++;
					if (senNo1Tiem >= 3) {
						senNo1Tiem = 0;
						continue;
					}
					ELog.i(TAG, "补发1了...");
					isP2pSuccess = true;
					// sf.getMcuSocket().setP2pSuccess(true);

					//
					// sf.getMcuSocket().cameraListInfo.getLocalPort()
					// + "收到1的端口:" + dp.getPort() + "收到1的地址:"
					// + dp.getAddress().getHostAddress());
					// 补发1
					byte[] dataP2p = new byte[1];
					dataP2p[0] = (byte) num;
					try {
						dpSend = new DatagramPacket(dataP2p, 1, p2pInetAddress,
								p2pPort);
						dsSend.send(dpSend);
					} catch (UnknownHostException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
					// sendP2pLocal(sf.getMcuSocket().cameraListInfo);
					// sendP2pToCam(sf.getMcuSocket().cameraListInfo);
				} else if ((buf[0] == '@') && (buf[1] == '%')
						&& (buf[2] == '^') && (buf[3] == '!')) {

					ELog.i(TAG, "长度:" + dp.getLength());
					buf = dp.getData();
					byte[] data = new byte[len];
					System.arraycopy(buf, 0, data, 0, len);
					// byte[] recLen = new byte[4];// 保存数据长度，包括自己
					switch (buf[4]) {
					case CMD_GET_UDP_ADDR:
						ELog.i(TAG, "获得NAT地址成功");
						byte[] myNatIp = new byte[4];
						byte[] myNatPort = new byte[4];
						System.arraycopy(data, 9, myNatIp, 0, 4);
						System.arraycopy(data, 13, myNatPort, 2, 2);
						int myIntNatPort = FormatTransfer
								.hBytesToInt(myNatPort);
						if (tempNatPort != 0 && tempNatPort != myIntNatPort) {
							ELog.i(TAG, "赛门铁克型");
							isSaimMen = true;
							localPort = 0;
						} else {
							tempNatPort = myIntNatPort;
						}

						byte[] myNatinfo = new byte[6];

						// natPort = new byte[2];
						try {
							InetAddress myNatAddress = InetAddress
									.getByAddress(myNatIp);
							ELog.i(TAG,
									"得到myNatIp:"
											+ myNatAddress.getHostAddress()
											+ "myIntNatPort:" + myIntNatPort);
							natAddress = myNatAddress.getHostAddress();
							natPort = myIntNatPort;
						} catch (UnknownHostException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						System.arraycopy(data, 9, myNatinfo, 0, 6);// 存natIP地址
						natinfo = myNatinfo;
						if (isSaimMen) {
							natinfo[4] = 0;
							natinfo[5] = 0;
						}
						// System.arraycopy(data, 13, natPort, 0, 2);
						break;
					case CMD_JOIN_DATA_CHANNEL:
						ELog.i(TAG, "加入数据通道成功");
						break;

					default:
						break;
					}

				} else {

					ELog.d(TAG, "接收到数据" + dp.getLength());
					byte[] videoPackageTotal = new byte[4];
					buf = dp.getData();
					byte[] packageVideoData = new byte[len];
					System.arraycopy(buf, 0, packageVideoData, 0, len);
					// byte[] first12 = new byte[12];
					// System.arraycopy(buf, 0, first12, 0, 12);
					// dpSend = new DatagramPacket(first12, 12, p2pInetAddress,
					// p2pPort);
					// try {
					// dsSend.send(dpSend);
					//
					// } catch (IOException e) {
					//
					// e.printStackTrace();
					// }
					// if (!Arrays.equals(first12, tempVideoArray)) {
					packageVideo(packageVideoData);
					// }else {
					//
					// }
					// tempVideoArray = first12;

					// System.arraycopy(src, srcPos, dst, dstPos, length)
				}

			}

			super.run();
		}
	}

	public void packageVideo(byte[] pack) {
		rateTemp += pack.length;
		//
		byte[] packMessage = new byte[4];
		System.arraycopy(pack, 4, packMessage, 0, 4);
		int iPackMessage = FormatTransfer.lBytesToInt(packMessage);
		//
		byte[] packTotal = new byte[4];
		System.arraycopy(pack, 8, packTotal, 2, 2);
		int ipackTotal = FormatTransfer.hBytesToInt(packTotal);

		byte[] packCurrent = new byte[4];
		System.arraycopy(pack, 10, packCurrent, 2, 2);
		int iPackCurrent = FormatTransfer.hBytesToInt(packCurrent);
		// if (ipackTotal>10) {

		// }
		//
		if (!Arrays.equals(tempPackageVerify, packMessage)) {
			tempPackageVerify = packMessage;
			//
			if (vMap.get(iPackMessage) == null) {
				Vdata vData = new Vdata();
				vData.setMcuData(new byte[ipackTotal * 1024]);
				vData.totalPackage = ipackTotal;
				HashSet<String> tempSet = new HashSet<String>();
				for (int i = 0; i < ipackTotal; i++) {
					tempSet.add(Integer.toString(i));
					dropVideoMap.put(iPackMessage, tempSet);
				}
				// byte[] videoMcuData = new byte[ipackTotal * 1024];
				vMap.put(iPackMessage, vData);
			}
		}
		byte[] mcuData = new byte[1024];
		int currentPackMessage = iPackMessage;

		//
		try {
			if (dropVideoMap.containsKey(currentPackMessage)) {
				dropVideoMap.get(currentPackMessage).remove(
						Integer.toString(iPackCurrent));
				if (dropVideoMap.get(currentPackMessage).isEmpty()) {
					dropVideoMap.remove(currentPackMessage);
				}
			}
			Vdata vd = vMap.get(currentPackMessage);
			System.arraycopy(pack, 12, vd.getMcuData(), iPackCurrent * 1024,
					(pack.length - 12));
			vd.reciverPackeNum++;
			vd.recivePackeLen += pack.length - 12;// 每一包减去头
			if (vd.reciverPackeNum == ipackTotal) {
				//
				packageToRealVideo(currentPackMessage);
				// videoMap.remove(currentPackMessage);
				// TreeMap<String, String> mTreeMap = new TreeMap<String,
				// String>();
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			return;
		} catch (NullPointerException e) {
			ELog.i(TAG, "接收的空信息");
			return;
		}

	}

	private void packageToRealVideo(int packageId) {
		Vdata v = vMap.get(packageId);
		byte[] len = new byte[4];
		byte[] reciverPacke = v.getMcuData();
		System.arraycopy(v.getMcuData(), 0, len, 0, 4);
		int iLen = FormatTransfer.hBytesToInt(len);
		// iLen = (iLen == reciverPacke.length-4)?iLen:reciverPacke.length-4;
		//
		byte[] videoWithCmd = null;
		if (iLen > 5) {
			videoWithCmd = new byte[iLen - 5];// 减去命令类型和通道号
		}

		// byte[] audioNoCmd = new byte[iLen - 12];
		switch (reciverPacke[4]) {
		case CMD_SEND_DATA:
			byte[] timeStamp = new byte[4];
			int iTimeStamp;
			// System.arraycopy(reciverPacke, 9, videoWithCmd, 0, iLen - 5);
			//
			try {
				System.arraycopy(reciverPacke, 9, videoWithCmd, 0, iLen - 5);
				System.arraycopy(reciverPacke, 11, timeStamp, 0,
						timeStamp.length);
				iTimeStamp = FormatTransfer.hBytesToInt(timeStamp);
				v.timeStamp = iTimeStamp;
				//
			} catch (ArrayIndexOutOfBoundsException e) {
				ELog.i(TAG, "拷频数据错误。");
				vMap.remove(packageId);
				e.printStackTrace();
				return;
			}

			//
			switch (videoWithCmd[0]) {
			case 0:
				ELog.i(TAG, "收到视频,packid is "+packageId);

				synchronized(VideoData.Videolist){
					videoSize = videoWithCmd[6];
					Vdata video = new Vdata();
					video.setVideoData(videoWithCmd);
					VideoData.Videolist.add(video);
				}

				// VideoData.videoTreeMap.put(Integer.toString(iTimeStamp),
				// videoWithCmd);
//				videoSize = videoWithCmd[6];
//				v.setVideoData(videoWithCmd);
//				VideoData.Videolist.add(v);

//				parse(v.getVideoData(),v.getVideoData().length);
//				vMap.remove(packageId);
				//
				break;
			case 1:

				// System.arraycopy(reciverPacke, 16, videoWithCmd, 0, iLen -
				// 5);
				SoundPlay.isAdpcm = videoWithCmd[1] == AUDIOADPCM ? true
						: false;
				VideoData.audioArraryList.add(videoWithCmd);
				vMap.remove(packageId);
			default:
				break;
			}

			break;

		default:
			break;
		}
	}
	int recType = -1;
	int type = -1;
	int width;
	int height;
	void parse(byte []buf,int total) {
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
		ELog.i(TAG, "解码---------0" );
		//synchronized (this)
		{
			int n = jvideo_decode_frame( bmp, total - 7);
			ELog.e(TAG, "解码n:" + n);
		}

		//
		// FileUtils.saveByteToFile(yuv,
		// Environment.getExternalStorageDirectory().getPath()+"/my.yuv");
		// VideoData.yuvArrayList.add(yuv);
		//long parseTimeAfter = System.currentTimeMillis();
		//parseTime = (int) (parseTimeAfter - parseTimeBefore);
		//


	}

	private byte[] getLocalIpAddress() {
		byte[] b = null;
		ELog.i(TAG, "本地:"
				+ MainSocket.getInstance().getLocalAddress().getHostAddress());
		localIp = MainSocket.getInstance().getLocalAddress().getHostAddress();
		b = MainSocket.getInstance().getLocalAddress().getAddress();
		// String hostName;
		// try {
		// hostName = InetAddress.getLocalHost().getHostName();
		//
		// InetAddress[] hosts = InetAddress.getAllByName(hostName);
		// for (int i = 0; i < hosts.length; i++) {
		//
		// if (hosts[i] instanceof Inet4Address) {
		// String ip = hosts[i].getHostAddress().toString();
		// b = hosts[i].getAddress();
		//
		// }
		// }
		// } catch (UnknownHostException e) {
		//
		// e.printStackTrace();
		// }
		return b;
	}

	public void sendJoinChannel(String mcuIp, int mucPort) {
		ELog.i(TAG, "sendJoinChannel");
		try {
			byte[] messageHead = { '@', '%', '^', '!' };
			byte[] packageData = new byte[messageHead.length + 1 + 4];
			byte[] channelIdByte = FormatTransfer.toLH(channelId);
			byte[] cmd = new byte[1];
			cmd[0] = CMD_JOIN_DATA_CHANNEL;
			localIpv4Byte = getLocalIpAddress();
			System.arraycopy(messageHead, 0, packageData, 0, 4);
			System.arraycopy(cmd, 0, packageData, 4, 1);
			System.arraycopy(channelIdByte, 0, packageData, 5, 4);
			dpSend = new DatagramPacket(packageData, packageData.length,
					InetAddress.getByName(mcuIp), mucPort);
			dsSend.send(dpSend);
			// sf.mUdpSocket.reciveCamP2p();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void modifyCam() {
		ELog.i(TAG, "降码流:" + isSendModify);
		if (isSendModify) {
			return;
		}
		isSendModify = true;
		// sf.modifyCamera(CameraListInfo.currentCam, channelId);
	}

	public static void closeTimer() {
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
		if (mTimerTask != null) {
			mTimerTask.cancel();
			mTimerTask = null;
		}

	}

	public void listenVideo() {
		rate++;
		changFlag++;
		if (rate == 10) {
			rate = 0;
			changRate += rateLast;
			noVideoInt++;
			rateLast = rateTemp;
			rateTemp = 0;
			ELog.i(TAG, "码流:" + rateLast / 1024);
			ELog.i(TAG, "Videolist:" + VideoData.Videolist.size());
			ELog.i(TAG, "音频:" + VideoData.audioArraryList.size());
			// keepLive();
			if (VideoData.Videolist.size() > 50) {
				ELog.i(TAG, "积累变码流了");
				modifyCam();
			}

			ELog.i(TAG, "isNoVideoListen:" + isNoVideoListen);
			if (noVideoInt > 30 && isNoVideoListen) {
				ELog.i(TAG, "超时了");
				mVideoListener.noVideoListener(VideoListener.NOUDPVIDEO);
			}
		}

		if (changFlag == 50) {
			changFlag = 0;
			ELog.i(TAG, "sdlTAG :" + CloudLivingView.sdlTAG + " changRate:"
					+ changRate);
			// 变码流
			if (changRate < 5 * 60 * 1024 && isP2pSuccess) {
				ELog.i(TAG, "变码流了");
				modifyCam();
			}
			ELog.i(TAG, "丢包:" + dropVideoMap.size());
			if (dropVideoMap.size() > 10) {
				ELog.i(TAG, "丢包将码流");
				modifyCam();
			}
			dropVideoMap.clear();
			changRate = 0;
		}

		//
		// try {
		// if (dropVideoMap.size() > 0) {
		//
		//
		// dropVideoMap.size());
		// float percent = dropVideoMap.get(dropVideoMap.firstKey())
		// .size()
		// / (vMap.get(dropVideoMap.firstKey()).totalPackage);
		//
		// (vMap.get(dropVideoMap.firstKey()).totalPackage));
		// 超过90%没有完成,丢
		// dropVideoMap.remove(dropVideoMap.firstKey());
		// vMap.remove(dropVideoMap.firstKey());
		// if(percent>0.1){
		// dropVideoMap.remove(dropVideoMap.firstKey());
		// vMap.remove(dropVideoMap.firstKey());
		// }else{
		// packageToRealVideo(dropVideoMap.firstKey());
		// }

		// }
		// } catch (Exception e) {
		//
		//
		// }

	}

	class SendP2PThread extends Thread {

		@Override
		public void run() {
			sendJoinChannel(camMcuIp, camMcuPort);
			// 摄像头nat端口为0，即摄像头是赛门铁克型
			if (CameraListInfo.currentCam.getNatPort() == 0) {
				sendP2pLocal();
				for (int i = 0; i < 32256; i++) {
					if (isP2pSuccess) {
						break;
					}
					// 65536-32256 = 33280;
					// 1024+32256 = 33280
					CameraListInfo.currentCam.setNatPort(33280 - i);
					sendP2pToCam();
					CameraListInfo.currentCam.setNatPort(33280 + i);
					sendP2pToCam();
					try {
						this.sleep(2);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if (isP2pSuccess) {
						break;
					}
				}
			} else {
				for (int i = 0; i < 100; i++) {
					sendP2pLocal();
					sendP2pToCam();
					try {
						sendP2pThread.sleep(100);
					} catch (InterruptedException e) {
						ELog.i(TAG, "发p2p出错");
						e.printStackTrace();
					}
					if (isP2pSuccess) {
						break;
					}
				}
			}

		}

	}

}
