package com.myanycam.bean;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

import com.myanycamm.utils.Constants;
import com.myanycamm.utils.ELog;

public class MainSocket extends Socket {
	
	private static String TAG = "MainSocket";
	private static MainSocket mMainSocket;
	
	private MainSocket() {
		super();
	}
	
	
	public static MainSocket getInstance(){
		if(mMainSocket == null){
			mMainSocket = new MainSocket();		
		}
		return mMainSocket;
	}
	
	public static void closeMain(){
		if(mMainSocket != null){
			try {
				ELog.i(TAG, "主动关闭...");
				mMainSocket.close();
			} catch (IOException e) {
				ELog.i(TAG, "关闭出错"+e.getMessage());
				e.printStackTrace();
			}
			mMainSocket = null;
		}
		
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
	
	@Override
	public synchronized void close() throws IOException {
		ELog.i(TAG, "被关闭了...");
		super.close();
	}
}
