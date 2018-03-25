package com.myanycamm.cam;

import java.util.HashMap;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.myanycam.bean.CameraListInfo;
import com.myanycam.net.SocketFunction;
import com.myanycamm.model.BitmapCache;
import com.myanycamm.model.DateAdapter;
import com.myanycamm.model.TwitterLogin;
import com.myanycamm.process.ScreenManager;
import com.myanycamm.ui.DialogFactory;
import com.myanycamm.ui.DragListView;
import com.myanycamm.ui.FileListView;
import com.myanycamm.ui.LivingView;
import com.myanycamm.ui.PhotoListView;
import com.myanycamm.ui.SettingView;
import com.myanycamm.update.UpdateSoft;
import com.myanycamm.utils.Configure;
import com.myanycamm.utils.ELog;
import com.myanycamm.utils.Utils;
import com.myanycamm.zxing.client.android.CaptureActivity;
import com.nmbb.oplayer.ui.player.VideoActivity;

public class LoginActivity extends BaseActivity {

	private static String TAG = "LoginActivity";

	private DragListView listView;
	DateAdapter adapter;
	public static boolean isGettingDate = false;

	private View loginView;
	private View mainView;
	private View registerView;
	private TextView forgotPsw, headTitle;// 忘记密码
	private EditText username, password;// 用户名,密码
	private BitmapCache mBitmapCache;
	private Button loginBtn, setButton;// 登录按钮
	private Button registerBtn;// 注册按钮
	private Button mBtnRegister;// 注册页面的注册按钮
	private Button addButton;
	private Button reg_backBtn;
	private EditText emailEt, regPasswordEt, regPasswordConfirm;
	// Intent intentService;
	SocketFunction sf;
	SharedPreferences sp;
	Button testBtn;
	TwitterLogin mTwitterLogin;

	private HashMap<String, String> testCamMap = new HashMap<String, String>();
	private Dialog mDialog = null;
	private final int LOGINSUCCESS = 15;// 登录成功标志
	public static final int USERNAMEERROR = 1;
	public static final int ONLINE = 3;
	public static final int REGRSPE = 6;
	public static final int RELOGIN = 10;

	// mainView中的变量
	LayoutInflater inflater_tab;
	LivingView mCameraListView;// 摄像头列表界面
	FileListView mFileListView;// 文件列表
	SettingView mSettingView;// 设置界面列表
	PhotoListView mPhotoListView;
	public static final int DOWNLOADCAMRRA = 4;
	public static final int UPDATECAMRRA = 5;
	public static final int UPDATESOFT = 123;
	public static final int UPDATECAMIMAGE = 124;

	Handler mHandler = new Handler() {

		public void handleMessage(Message msg) {
			Bundle bundle = msg.getData();
			HashMap<String, String> map = (HashMap) bundle
					.getSerializable("data");
			switch (msg.what) {
			case LOGINSUCCESS:
				// 登录成功
				ELog.i(TAG, "登录成功");
				showToast(getString(R.string.logn_succeed));
				dimissDialog();
				showMainView();
				// sf.downloadCamera();
				// sf.getMcu();
				break;
			case USERNAMEERROR:
				// 用户名或密码错误
				ELog.i(TAG, "用户名或密码错误");
				mDialog.dismiss();
				Toast.makeText(sf, sf.getString(R.string.username_error),
						Toast.LENGTH_SHORT).show();
				break;
			case ONLINE:
				ELog.i(TAG, "已经在线");
				break;
			case DOWNLOADCAMRRA:
				ELog.i(TAG, "摄像头信息:" + map);
				adapter.notifyDataSetChanged();
				// camListAdapter.notifyDataSetChanged();
				break;
			case UPDATECAMRRA:
				ELog.i(TAG, "收到更新通知" + map.get("cameraid"));
				upadateOneList(Integer.parseInt(map.get("position")));
				// adapter.notifyDataSetChanged();
				// camListAdapter.notifyDataSetChanged();
				break;
			case UPDATECAMIMAGE:
				ELog.i(TAG, "收到更新图片通知");
				updateCamImg(Integer.parseInt(map.get("camId")));
				break;
			case REGRSPE:
				if (map.get("ret").equals("1")) {
					Toast.makeText(sf,
							sf.getString(R.string.reg_fail_username_exit),
							Toast.LENGTH_SHORT).show();
					dimissDialog();
				}
				if (map.get("ret").equals("0")) {
					dimissDialog();
					Toast.makeText(sf, sf.getString(R.string.reg_success),
							Toast.LENGTH_SHORT).show();
					rememberUser(emailEt.getText().toString(), regPasswordEt
							.getText().toString());
					sf.login(0);
					// login();
					showRequestDialog(getString(R.string.dialog_verify_cou));
					// showLoginView();
				}

				break;
			case RELOGIN:
				ELog.i(TAG, "处理重新登录");
				showReLoginDialog();
				break;
			case UPDATESOFT:
				// 升级软件
				ELog.i(TAG, "需要更新..");
				UpdateSoft mSoft = new UpdateSoft(LoginActivity.this);
				mSoft.update(false);
				break;
			default:
				break;
			}
		};
	};

	OnClickListener loginBtnOnclickListener = new OnClickListener() {
		public void onClick(View v) {
			login();
		}

	};
	
	private void login(){
		realLogin(username.getText().toString(), password.getText()
				.toString());
		sf.userInfo.setLoginToken("");
		sf.userInfo.setLoginType(0);
		// testLogin();
	}

	public void showReLoginDialog() {
		AlertDialog.Builder builder = DialogFactory.creatReTryDialog(
				LoginActivity.this, getResources()
						.getString(R.string.net_error));

		builder.setPositiveButton(getResources().getString(R.string.retry),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						realLogin(username.getText().toString(), password
								.getText().toString());
						// retryCam();
					}
				});
		builder.setNegativeButton(getResources().getString(R.string.exit),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						finish();
						// stopCam();
					}
				});
		builder.create().show();
	}

	public void facebookLogin(String _name, String _psw, String _loginToken,
			int loginType) {
		sf.userInfo.setLoginToken(_loginToken);
		sf.userInfo.setLoginType(1);
		// realLogin(_name, _psw);
	}

	public void realLogin(String _name, String _psw) {
//		if (!Utils.isEmail(_name)) {
//			Toast.makeText(LoginActivity.this,
//					getResources().getString(R.string.reg_email_invalid),
//					Toast.LENGTH_SHORT).show();
//			return;
//		}

		if (_psw.equals("")) {
			Toast.makeText(sf, sf.getString(R.string.password_null),
					Toast.LENGTH_SHORT).show();
			return;
		}

		rememberUser(_name, _psw);
		showRequestDialog(getString(R.string.dialog_verify_cou));
		sf.login(0);
	}

	OnClickListener registerBtnOnClickListener = new OnClickListener() {

		public void onClick(View v) {
			showRegisterView();
		}
	};

	OnClickListener backBtnOnClickListener = new OnClickListener() {

		public void onClick(View v) {
			showLoginView();
		}
	};
	OnClickListener mRegisterBtnOnClickListener = new OnClickListener() {

		public void onClick(View v) {
			// Toast.makeText(LoginActivity.this,
			// "点击..."+emailEt.getText().toString(), Toast.LENGTH_SHORT).show();
			// if (emailEt.getText().toString().equals("")) {
			// Toast.makeText(LoginActivity.this,
			// getResources().getString(R.string.email_cannot_bank),
			// Toast.LENGTH_SHORT).show();
			// return;
			// }
//			if (!Utils.isEmail(emailEt.getText().toString())) {
//				Toast.makeText(LoginActivity.this,
//						getResources().getString(R.string.reg_email_invalid),
//						Toast.LENGTH_SHORT).show();
//				return;
//			}
			if (regPasswordEt.getText().length() < 8) {
				Toast.makeText(LoginActivity.this,
						getResources().getString(R.string.psw_too_short),
						Toast.LENGTH_SHORT).show();
				return;
			}
			if (!regPasswordEt.getText().toString()
					.equals(regPasswordConfirm.getText().toString())) {
				Toast.makeText(LoginActivity.this,
						getString(R.string.password_no_match),
						Toast.LENGTH_SHORT).show();
				return;
			}
			showRequestDialog(getString(R.string.dialog_reg));
			// initService();
			new Thread(new Runnable() {

				@Override
				public void run() {
					sf.register(emailEt.getText().toString(), regPasswordEt
							.getText().toString());
				}
			}).start();

		}
	};

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		sp = getSharedPreferences("passwordFile", MODE_PRIVATE);
		// intentService = new Intent("com.myanycam.cam.AppServer");
		initView();
		initMainView();
		Intent intent = getIntent();
		if (intent.getBooleanExtra("istoMain", false)) {
			showMainView();
		}
		mBitmapCache = new BitmapCache(LoginActivity.this);
		String camInfo = getIntent().getStringExtra("caminfo");
		ELog.i(TAG, "camInfo:"+camInfo);
		if (camInfo != null) {
			String[] camInfos = camInfo.split("#");
			username.setText(camInfos[0]);
			password.setText(camInfos[1]);
			login();
		}
	}

	@Override
	protected void onRestart() {
		ELog.i(TAG, "又启动了");
		if (sf.userInfo.getUserId() == 0) {
			finish();
		}
		super.onRestart();
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}

	public void initView() {
		loginView = findViewById(R.id.loginpage);
		mainView = findViewById(R.id.main_view);
		registerView = findViewById(R.id.register_view);
		username = (EditText) findViewById(R.id.username);
		// username.addTextChangedListener(mWatcher);// 字符改变监听
		password = (EditText) findViewById(R.id.password);
		// password.addTextChangedListener(mWatcher);
		loginBtn = (Button) findViewById(R.id.loginbtn);
		loginBtn.setOnClickListener(loginBtnOnclickListener);
		registerBtn = (Button) findViewById(R.id.registerbtn);
		registerBtn.setOnClickListener(registerBtnOnClickListener);
		reg_backBtn = (Button) findViewById(R.id.settings_back);
		reg_backBtn.setOnClickListener(backBtnOnClickListener);
		// String htmlLinkText =
		// "<a href=\"http://student.csdn.net/?232885\"><u>forgot password?</u></a>";
		// forgotPsw.setText(Html.fromHtml(htmlLinkText));
		// forgotPsw.setMovementMethod(LinkMovementMethod.getInstance());
		// rememberMe.setOnCheckedChangeListener(rememberOnCheckedChangeListener);
		if (sp.getAll().size() != 0) {
			String[] allUserName = sp.getAll().keySet().toArray(new String[0]);
			//
			username.setText(allUserName[0]);
			password.setText(sp.getString(allUserName[0], ""));
		}
		mBtnRegister = (Button) findViewById(R.id.register_btn);
		mBtnRegister.setOnClickListener(mRegisterBtnOnClickListener);
		emailEt = (EditText) findViewById(R.id.email);
		regPasswordEt = (EditText) findViewById(R.id.reg_password);
		regPasswordConfirm = (EditText) findViewById(R.id.password_confirm);
	}

	private void showRequestDialog(String note) {
		if (mDialog != null) {
			mDialog.dismiss();
			mDialog = null;
		}
		mDialog = DialogFactory.createLoadingDialog(LoginActivity.this, note);
		mDialog.setCanceledOnTouchOutside(false);
		mDialog.show();
	}

	private void dimissDialog() {
		if (mDialog != null) {
			mDialog.dismiss();
			mDialog = null;
		}
	}

	public void showMainView() {
		loginView.setVisibility(View.GONE);
		registerView.setVisibility(View.GONE);
		mainView.setVisibility(View.VISIBLE);
	}

	public void showRegisterView() {
		loginView.setVisibility(View.GONE);
		registerView.setVisibility(View.VISIBLE);
		mainView.setVisibility(View.GONE);
	}

	public void showLoginView() {
		loginView.setVisibility(View.VISIBLE);
		registerView.setVisibility(View.GONE);
		mainView.setVisibility(View.GONE);
	}

	public void rememberUser(String nam, String psw) {
		sp.edit().clear().commit();
		sp.edit().putString(nam, psw).commit();

	}

	// mainView

	public void initMainView() {
		setButton = (Button) findViewById(R.id.main_setting_btn);
		setButton.setVisibility(View.VISIBLE);
		setButton.setBackgroundResource(R.drawable.button_main_setting);
		// setButton.setText(getResources().getString(R.string.sys_setting));
		setButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				Intent intent = new Intent(LoginActivity.this,
						SystemActivity.class);

				startActivity(intent);
			}
		});
		addButton = (Button) findViewById(R.id.add_cam_btn);
		// addButton.setBackgroundResource(R.drawable.title_button);
		addButton.setBackgroundResource(R.drawable.button_add_cam);
		addButton.setVisibility(View.VISIBLE);
		// addButton.setText(R.string.add_camera);
		addButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(LoginActivity.this,
						CaptureActivity.class);
				// Intent intent = new Intent(LoginActivity.this,
				// JNITest1.class);
				startActivity(intent);

			}
		});
		headTitle = (TextView) findViewById(R.id.settings_head_title);
		headTitle.setText(R.string.camera_list);
		isGettingDate = false;
		DateAdapter.isExchanged = false;
		DateAdapter.isDeleted = false;
		Configure.init(this);

		listView = (DragListView) findViewById(R.id.listview);
		adapter = new DateAdapter(this);
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(new GridView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				ELog.i(TAG, "点击了" + CameraListInfo.cams.get(arg2).getName());
				switch (CameraListInfo.cams.get(arg2).getStatus()) {
				case 1:
				case 2:
					Intent intent = new Intent(LoginActivity.this,
							CameraCenterActivity.class);
					intent.putExtra("position", arg2);
					startActivityForResult(intent, 0);
					break;
				default:
					break;
				}

			}

		});

		listView.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				ELog.i(TAG, "长按了.." + arg2);
				showdelelteDilog(arg2);
				return true;
			}
		});

	}

	private void showdelelteDilog(final int arg2) {
		Builder singleDialog = new Builder(LoginActivity.this);
		singleDialog.setMessage(R.string.sure_delete_cam);
		singleDialog.setTitle(getString(R.string.note));
		singleDialog.setCancelable(true);
		singleDialog.setPositiveButton(
				getResources().getString(R.string.confirm),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						sf.deleteCam(CameraListInfo.cams.get(arg2));
						CameraListInfo.cams.remove(arg2);
						adapter.notifyDataSetChanged();
						dialog.dismiss();
					}
				});
		singleDialog.setNegativeButton(R.string.btn_cancel,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
		singleDialog.create().show();

	}

	@Override
	public void onBackPressed() {
		ELog.i(TAG, "按了返回键");
		if (registerView.getVisibility() == View.VISIBLE) {
			showLoginView();
		} else {
			ELog.i(TAG, "弹出所有");
			ScreenManager.getScreenManager().popAllActivity();
			// ScreenManager.getScreenManager().toWelcome();
			// finish();
			//
			//
			// AppServer.isBackgroud = true;
			// Intent intent = new Intent(Intent.ACTION_MAIN);
			// intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);// 注意
			// intent.addCategory(Intent.CATEGORY_HOME);
			// this.startActivity(intent);
			//
			// return true;
		}
		;
	}

	// @Override
	// public boolean onKeyDown(int keyCode, KeyEvent event) {
	// if (keyCode == KeyEvent.KEYCODE_BACK) {
	//
	// }
	// return super.onKeyDown(keyCode, event);
	// }

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case 0:
			if (CameraListInfo.cams.size() == 0) {
				ELog.i(TAG, "准备退出。。");
				ScreenManager.getScreenManager().toWelcome();
				break;
			}
			ELog.i(TAG, "从别的界面回来..." + CameraListInfo.cams + ":"
					+ SocketFunction.getInstance().userInfo);
			if ((CameraListInfo.cams.size() == 0)
					&& SocketFunction.getInstance().userInfo.getUserId() == 0) {
				ScreenManager.getScreenManager().toWelcome();
			} else {
				adapter.notifyDataSetChanged();
			}

			break;

		default:
			// Session.getActiveSession().onActivityResult(this, requestCode,
			// resultCode, data);
			break;
		}

	}

	@Override
	protected void onResume() {
		ELog.i(TAG, "loginactivity resume");
		AppServer.isBackgroud = false;
		// if (CameraListInfo.cams.size() == 0) {
		// ScreenManager.getScreenManager().toWelcome();
		// }
		sf = (SocketFunction) getApplicationContext();
		// 有时错误,就到这里来了
		ELog.i(TAG,
				"cam:" + CameraListInfo.cams.size() + "user:"
						+ sf.userInfo.getUserId());

		adapter.notifyDataSetChanged();
		sf.setmHandler(mHandler);
		// Uri uri = getIntent().getData();
		// if (uri != null && uri.toString().startsWith(Constants.CALLBACK_URL))
		// {
		// mTwitterLogin = new TwitterLogin(LoginActivity.this);
		// mTwitterLogin.restoreToken(uri);
		// }
		super.onResume();
	}

	private void showToast(String text) {
		if (AppServer.isBackgroud) {
			return;
		}
		Toast.makeText(sf, text, Toast.LENGTH_SHORT).show();
	}

	private int getAlarmNum(String camId) {
		int num = 0;
		SocketFunction.getAppContext();
		SharedPreferences sp = SocketFunction.getAppContext()
				.getSharedPreferences("evenInfo", Context.MODE_PRIVATE);
		num = sp.getInt(camId, 0);
		return num;
	}

	private void upadateOneList(int position) {
		CameraListInfo cam = CameraListInfo.cams.get(position);// 要更新的cam
		// new
		// WebImageView(LoginActivity.this).downLoadImage(LoginActivity.this,
		// CameraListInfo.cams.get(position).getTrueUrl(),
		// CameraListInfo.cams.get(position), -1);
		ELog.i(TAG, "下载..." + CameraListInfo.cams.get(position).getId());
		mBitmapCache.downImage(CameraListInfo.cams.get(position).getTrueUrl(),
				CameraListInfo.cams.get(position).getId() + "");
		ELog.i(TAG, "更新" + position + " 总:" + listView.getCount() + "状态:"
				+ CameraListInfo.cams.get(position).getStatus());
		for (int i = 0; i < listView.getCount(); i++) {
			try {
				View viewTemp = listView.getChildAt(i);
				TextView camName = (TextView) viewTemp
						.findViewById(R.id.highlowtemp);
				if (CameraListInfo.cams.get(position).getName()
						.equals(camName.getText().toString())) {
					ELog.i(TAG, "找到.." + i);
					position = i;
					break;
				}
			} catch (NullPointerException e) {
				ELog.i(TAG, "还没显示出来...");
				return;
			}
		}

		View view = listView.getChildAt(position);

		//
		ImageView camIngImageView = (ImageView) view.findViewById(R.id.cam_ing);
		// ImageView camImage = (ImageView) view
		// .findViewById(R.id.good_cell_photo_one);
		// camImage.setImageWithURL(LoginActivity.this,
		// cam.getTrueUrl(),cam);
		// Bitmap bitmap =
		// BitmapCache.getBitmapFromDiskCache(LoginActivity.this, cam.getId());
		// if (bitmap!=null ) {
		// camImage.setImageBitmap(bitmap);
		// }

		TextView online = (TextView) view.findViewById(R.id.online_text);
		ImageView camLineType = (ImageView) view
				.findViewById(R.id.cam_line_type);
		switch (cam.getStatus()) {
		case 0:
			online.setText(R.string.cam_offline);
			camLineType.setImageResource(R.drawable.cam_offlin_type);
			camIngImageView.setVisibility(View.INVISIBLE);
			break;
		case 1:
			online.setText(R.string.cam_online);
			camLineType.setImageResource(R.drawable.cam_online_type);
			camIngImageView.setVisibility(View.INVISIBLE);
			break;
		case 2:
			online.setText(R.string.cam_online);
			camLineType.setImageResource(R.drawable.cam_online_type);
			camIngImageView.setVisibility(View.VISIBLE);
			// camIcon.setImageResource(R.drawable.cam_online_ico_1);
			break;
		case 3:
			online.setText(R.string.cam_updating);
			camIngImageView.setVisibility(View.INVISIBLE);
			break;

		default:
			break;
		}

	}

	private void updateCamImg(int camId) {
		int position = -1;
		for (int i = 0; i < CameraListInfo.cams.size(); i++) {
			if (CameraListInfo.cams.get(i).getId() == camId) {
				for (int j = 0; j < listView.getCount(); j++) {
					try {
						ELog.i(TAG, "找..." + j);
						View viewTemp = listView.getChildAt(j);
						TextView camName = (TextView) viewTemp
								.findViewById(R.id.highlowtemp);
						if (CameraListInfo.cams.get(i).getName()
								.equals(camName.getText().toString())) {
							ELog.i(TAG, "找到.." + i);
							position = j;
							break;
						}
					} catch (NullPointerException e) {
						ELog.i(TAG, "还没显示出来...");
						return;
					}
				}
			}
		}
		if (position == -1) {
			return;
		}
		View view = listView.getChildAt(position);

		//
		ImageView camImage = (ImageView) view
				.findViewById(R.id.good_cell_photo_one);
		Bitmap bitmap = BitmapCache.getBitmapFromMemCache(LoginActivity.this,
				camId + "");
		if (bitmap != null) {
			camImage.setImageBitmap(bitmap);
		}
	}

	private void exitApplication() {
		ScreenManager.getScreenManager().extit();
		// Builder builder = DialogFactory.creatReTryDialog(LoginActivity.this,
		// getString(R.string.dialog_exit_content));
		// builder.setTitle(getString(R.string.dialog_title));
		// builder.setPositiveButton(getString(R.string.dialog_exit_posi),
		// new DialogInterface.OnClickListener() {
		// @Override
		// public void onClick(DialogInterface dialog, int which) {
		// dialog.dismiss();
		// ScreenManager.getScreenManager().extit();
		// }
		// });
		// builder.setNegativeButton(getString(R.string.dialog_exit_netg),
		// new DialogInterface.OnClickListener() {
		// @Override
		// public void onClick(DialogInterface dialog, int which) {
		// dialog.dismiss();
		// }
		// });
		// builder.create().show();
	}

}
