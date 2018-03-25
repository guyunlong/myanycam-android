package com.myanycamm.process;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.os.Bundle;
import android.os.Message;

import com.myanycam.bean.MainSocket;
import com.myanycam.net.SocketFunction;
import com.myanycamm.cam.AppServer;
import com.myanycamm.ui.DialogFactory;
import com.myanycamm.utils.Constants;
import com.myanycamm.utils.ELog;
import com.myanycamm.utils.FormatTransfer;

public class ReceiveThread extends Thread {

	private static ReceiveThread mReceiveThread;

	private final String TAG = "ReceiveThread";

	private ReceiveThread() {
		// TODO Auto-generated constructor stub
	}

	public static ReceiveThread getInstance() {
		if (mReceiveThread == null) {
			mReceiveThread = new ReceiveThread();
		}
		return mReceiveThread;
	}

	@Override
	public void run() {

		int j = 0;
		AppServer.stop = false;
		new DoCmdThread().start();
		ELog.i(TAG, "run.....");
		while (!AppServer.stop) {
			try {
				ActivityManager am = (ActivityManager) SocketFunction
						.getAppContext().getSystemService(
								Context.ACTIVITY_SERVICE);
				// 判断前后台
				List<RunningAppProcessInfo> list = am.getRunningAppProcesses();
				if (list != null) {
										for (RunningAppProcessInfo temp : list) {
						if (temp.processName.equals(SocketFunction
								.getAppContext().getPackageName())) {
							ELog.i(TAG, "改变后台" + temp.importance);
//							if (temp.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
//								AppServer.isBackgroud = false;
//							}else{
//								AppServer.isBackgroud = true;
//								ScreenManager.getScreenManager().popAllActivity();
//							}
//							AppServer.isBackgroud = temp.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND ? false
//									: true;
						}
					}
				}
				//
				// "cureenActivity:"+ScreenManager.getScreenManager().currentActivity().);
				ELog.i(TAG, "线程跑的上面..." + MainSocket.getInstance());
				if (MainSocket.getInstance() == null
						|| !MainSocket.getInstance().isConnected()) {
					ELog.i(TAG, "j:" + j++);
					try {
						if (j >= 15) {
							// relogin();
							Bundle mBundle = new Bundle();
							mBundle.putString("key", "");
							Message msg = new Message();
							msg.what = 1;
							msg.setData(mBundle);
							DialogFactory.dialogHandler.sendMessage(msg);
//							j = 0;
						}
						ReceiveThread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					continue;
				}

				InputStream in = MainSocket.getInstance().getInputStream();

				ELog.i(TAG, "线程还在跑...."
						+ MainSocket.getInstance().getInetAddress()
								.getHostAddress());

				// byte[] b = new byte[1024];

				byte[] temp = new byte[4];
				ELog.i(TAG, "in:" + in);
				in.read(temp, 0, 4);
				ELog.i(TAG, "read:" + in);
				MainSocket.getInstance().setSoTimeout(Constants.CONNECTION_TIME_OUT);
				// in.read(temp, 0, 4);
				int totalDataLen = FormatTransfer.hBytesToInt(temp);
				temp = null;
				if (totalDataLen == 1) {
					ELog.i(TAG, "收到心跳包");
				
					AppServer.heartCount = 0;
					in.read();
					// in.reset();
					in = null;
					continue;
				}
				ELog.w(TAG, "服务器发来的数据长度:" + totalDataLen);			
				// 过长处理
				if (totalDataLen<0) {
					in = null;
					continue;
				}
				receiveDataParse(in, totalDataLen);
	
			} catch (SocketTimeoutException e) {
				ELog.i(TAG, "socket超时了..");
				Bundle mBundle = new Bundle();
				mBundle.putString("key", "");
				Message msg = new Message();
				msg.what = 1;
				msg.setData(mBundle);
				DialogFactory.dialogHandler.sendMessage(msg);
			} catch (IOException e) {
				if (null != e.getMessage()
						&& e.getMessage().contains("ETIMEDOUT")) {
					ELog.i(TAG, "这里也超时了");
//					Bundle mBundle = new Bundle();
//					mBundle.putString("key", "很久没收到数据了.." + e.getMessage());
//					Message msg = new Message();
//					msg.what = 1;
//					msg.setData(mBundle);
//					DialogFactory.dialogHandler.sendMessage(msg);
					continue;
				} else {
					ELog.w(TAG, "收数据有错误..." + e.getMessage());
					continue;
					// e.printStackTrace();
				}

			}
			ELog.i(TAG, "stop:" + AppServer.stop);
	

		}
	}

	private void receiveDataParse(InputStream in, int total) {
		StringBuffer sb1 = new StringBuffer();
		int i = 0;
		byte[] dataByte = new byte[total];// 根据服务发来的数据决定长度
		if (total == 205) {
			ELog.i(TAG, "本次总长度:" + total);
		}
		int j=0;

		try {
			while ((i = in.read()) != -1) {
				try {
					dataByte[j] = (byte) i;
					j++;
//					if (total == 205) {
					if (j == total) {
						break;
					}
//						ELog.w(TAG, "收到:" + j);
//						break;
//					}
//					if ((i + startPosition) > total) {// 如果这次加上以前有的大于总量,不多收
//						sb1.append(new String(dataByte, startPosition, total
//								- startPosition));
//					} else {
//						sb1.append(new String(dataByte, startPosition, i));
//					}
//					if (sb1.length() >= total) {
////						if (total == 205) {
//							ELog.i(TAG, "本次总共收到了:" + sb1.length());
////						}
//						in = null;
//						break;
//					}
				} catch (StringIndexOutOfBoundsException e) {
					ELog.w(TAG, "溢出" + e.getMessage());
				}
			}
			sb1.append(new String(dataByte, 0, dataByte.length));
			String sd = sb1.substring(0);
			sd = sd.replaceAll("\\s", "");
			ELog.i(TAG, "sd:" + sd);
			Pattern p = Pattern.compile("(?<=<).*?(?=>)");// ?<=匹配后面的,?=匹配前面的
			Matcher m = p.matcher(sd);
			HashMap<String, String> dataMap = new HashMap<String, String>();
			while (m.find()) {
				String[] sarray = m.group().split("=");
				if (sarray.length < 2) {
					dataMap.put(sarray[0], "");
				} else {
					dataMap.put(sarray[0], sarray[1]);

				}
			}
			ELog.i(TAG, "dataMap:" + dataMap.toString());
			if (dataMap != null && dataMap.get("cmd") != null) {
				ELog.i(TAG, dataMap.get("cmd"));// 根据cmd来处理下一步
				DoCmdThread.cmdMaps.add(dataMap);
				return;
			}
		} catch (IOException e) {
			ELog.i(TAG, "收到处理数据错误...");
			e.printStackTrace();
		}
	}

	@Override
	public void interrupt() {
		ELog.i(TAG, "收到中断请求");
		super.interrupt();
		if (mReceiveThread != null) {
			mReceiveThread = null;
		}

	}

}
