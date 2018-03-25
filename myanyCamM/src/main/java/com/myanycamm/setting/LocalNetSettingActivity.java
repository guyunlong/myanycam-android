package com.myanycamm.setting;

import java.net.Socket;
import java.util.HashMap;

import android.app.Dialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.Toast;

import com.myanycam.net.SocketFunction;
import com.myanycamm.cam.BaseActivity;
import com.myanycamm.cam.R;
import com.myanycamm.ui.DialogFactory;
import com.myanycamm.ui.FixEditText;
import com.myanycamm.ui.SlipButton;
import com.myanycamm.utils.ELog;

public class LocalNetSettingActivity extends BaseActivity {

	private static String TAG = "LocalNetSettingActivity";
	public final static int SLIP_BTN_ID_AUTO_NET = 100;
	private ScrollView mScrollView;
	private SlipButton mSlipButton;
	private HashMap<String, String> map;
	private String ret = "";
	private String dhcp;
	private Dialog mDialog;
	private FixEditText ipInfo, mask, netGate, firstDns, secondDns;
	private String ipS = "", maskS = "", netGasteS = "", dns1S = "",
			dns2S = "";
	private SocketFunction sf = null;
	Socket mSocket = null;
	public final static int GETNETINFO = 0;
	public final static int MODIFYRESP = 1;

	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			Bundle bundle = msg.getData();
			map = (HashMap) bundle.getSerializable("data");
			switch (msg.what) {
			case GETNETINFO:
				dhcp = map.get("dhcp");
				if (dhcp.equals("1")) {
					mSlipButton.setState(true);
				} else {
					mSlipButton.setState(false);
				}
				ELog.i(TAG, "得到了网络信息...");
				if (null != map.get("ip")) {
					ipS = map.get("ip");
					ipInfo.setText(ipS);
				}
				if (null != map.get("mask")) {
					maskS = map.get("mask");
					mask.setText(maskS);
				}
				if (null != map.get("netgate")) {
					netGasteS = map.get("netgate");
					ipInfo.setText(netGasteS);
				}
				if (null != map.get("dns1")) {
					dns1S = map.get("dns1");
					firstDns.setText(dns1S);
				}
				if (null != map.get("dns2")) {
					dns2S = map.get("dns2");
					secondDns.setText(dns2S);
				}
				break;
			case MODIFYRESP:
				ret = map.get("ret");
//				if(ret.equals("0")){
//					
//				}else{
//					
//				}
				break;
			default:
				break;
			}
		};
	};

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.local_net_setting);
		ELog.i(TAG, "到了本地网络设置");
		Button settingBack = (Button) findViewById(R.id.settings_back);
		settingBack.setVisibility(View.VISIBLE);
		mSlipButton = (SlipButton) findViewById(R.id.auto_net_slipbtn);
		mSlipButton.SetOnChangedListener(slipBtnChangedListener,
				SLIP_BTN_ID_AUTO_NET);
		mScrollView = (ScrollView) findViewById(R.id.net_setting);
		settingBack.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				// sf.setNetworkInfo(dhcp, ip, mask, netgate, dns1, dns2)
				// finish();
		
				String ipInfoE = ipInfo.getText().toString();
				String maskE = mask.getText().toString();
				String netGateE = netGate.getText().toString();
				String firstDnsE = firstDns.getText().toString();
				String secondDnsE = secondDns.getText().toString();
//				ELog.i(TAG, "测试:"+dhcp.equals(map.get("dhcp")));
				if (map != null) {
					if (!dhcp.equals(map.get("dhcp")) || !ipInfoE.equals(ipS)
							|| !maskE.equals(maskS)
							|| !netGateE.equals(netGasteS)
							|| !firstDnsE.equals(dns1S)
							|| !secondDnsE.equals(dns2S)) {
						sf.setNetworkInfo(dhcp, ipInfoE, maskE, netGateE,
								firstDnsE, secondDnsE);
						refresRotate();
						return;
					}

				}
					finish();
				// sf.setNetworkInfo(dhcp, , mask
				// .getText().toString(), ,
				// firstDns.getText().toString(), secondDns.getText()
				// .toString());
			}
		});
		sf = (SocketFunction) getApplicationContext();
		sf.getNetworkInfo();
		sf.setmHandler(mHandler);
		ipInfo = (FixEditText) findViewById(R.id.ipaddress_edittext);
		mask = (FixEditText) findViewById(R.id.netmask_edittext);
		netGate = (FixEditText) findViewById(R.id.netgate_edittext);
		firstDns = (FixEditText) findViewById(R.id.firstdns_edittext);
		secondDns = (FixEditText) findViewById(R.id.seconddns_edittext);

		// mSocket = localNetSocket();
	}

	private SlipButton.OnChangedListener slipBtnChangedListener = new SlipButton.OnChangedListener() {

		public void OnChanged(boolean CheckState, int id) {
			//
			switch (id) {
			case SLIP_BTN_ID_AUTO_NET:
				ELog.i(TAG, "网络:" + CheckState);
				if (CheckState) {
					mScrollView.setVisibility(View.INVISIBLE);
					dhcp = "1";
				} else {
					mScrollView.setVisibility(View.VISIBLE);
					dhcp = "0";
				}
				break;
			default:
				break;
			}
		}

	};
	
	public void refresRotate() {

		// TODO Auto-generated method stub
		synchronized (this) {
			new AsyncTask<String, String, String>() {
				protected void onPreExecute() {
//					cameraWifiInfos.removeAll(cameraWifiInfos);
//					refreshView.setImageResource(R.drawable.refresh_rotate);
//					refreshView.startRotate();
					mDialog = DialogFactory.createLoadingDialog(LocalNetSettingActivity.this, getResources().getString(R.string.save_network_info));
					mDialog.show();
				};

				@Override
				protected String doInBackground(String... arg0) {
					// 发送手动刷新消息
					ELog.d(TAG, "发送手动刷新消息");
					// mHandler.sendEmptyMessage(Constant.MSG_WEATHER_HANDREFRESH);
					// 假的转圈
					try {
						Thread.sleep(3000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					return null;
				}

				protected void onPostExecute(String result) {
					if(ret.equals("0")){
						Toast.makeText(LocalNetSettingActivity.this, R.string.save_success, Toast.LENGTH_SHORT).show();
					}else if(ret.equals("1")){
						Toast.makeText(LocalNetSettingActivity.this, R.string.save_failed, Toast.LENGTH_SHORT).show();
					}
					mDialog.dismiss();
					LocalNetSettingActivity.this.finish();
					
//					refreshView.stopRotate();
//					refreshView.setImageResource(R.drawable.refresh);
				};
			}.execute("");
		}

	}


}
