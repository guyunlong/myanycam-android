package com.myanycamm.zxing.client.android;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.google.zxing.ResultMetadataType;
import com.google.zxing.ResultPoint;
import com.myanycam.zxing.client.android.camera.CameraManager;
import com.myanycamm.cam.AddCameraActivity;
import com.myanycamm.cam.BaseActivity;
import com.myanycamm.cam.LoginActivity;
import com.myanycamm.cam.R;
import com.myanycamm.utils.ELog;
import com.myanycamm.zxing.client.android.result.ResultHandler;
import com.myanycamm.zxing.client.android.result.ResultHandlerFactory;

public final class CaptureActivity extends BaseActivity implements
		SurfaceHolder.Callback {

	private static final String TAG = CaptureActivity.class.getSimpleName();

	private static final long INTENT_RESULT_DURATION = 1500L;
	private static final long BULK_MODE_SCAN_DELAY_MS = 1000L;
	private static final float BEEP_VOLUME = 0.10f;
	private static final long VIBRATE_DURATION = 200L;

	private static final String PACKAGE_NAME = "com.easou.zxing.client.android";
	private static final String PRODUCT_SEARCH_URL_PREFIX = "http://www.google";
	private static final String PRODUCT_SEARCH_URL_SUFFIX = "/m/products/scan";
	private static final String ZXING_URL = "http://zxing.appspot.com/scan";
	private static final String RETURN_CODE_PLACEHOLDER = "{CODE}";
	private static final String RETURN_URL_PARAM = "ret";

	private static final Set<ResultMetadataType> DISPLAYABLE_METADATA_TYPES;
	static {
		DISPLAYABLE_METADATA_TYPES = new HashSet<ResultMetadataType>(5);
		DISPLAYABLE_METADATA_TYPES.add(ResultMetadataType.ISSUE_NUMBER);
		DISPLAYABLE_METADATA_TYPES.add(ResultMetadataType.SUGGESTED_PRICE);
		DISPLAYABLE_METADATA_TYPES
				.add(ResultMetadataType.ERROR_CORRECTION_LEVEL);
		DISPLAYABLE_METADATA_TYPES.add(ResultMetadataType.POSSIBLE_COUNTRY);
	}

	private enum Source {
		NATIVE_APP_INTENT, PRODUCT_SEARCH_LINK, ZXING_LINK, NONE
	}

	private CaptureActivityHandler handler;
	private Button backBtn, manualAddBtn;
	private TextView mTextView;
	private ViewfinderView viewfinderView;
	private TextView statusView;
	private MediaPlayer mediaPlayer;

	private boolean hasSurface;
	private boolean vibrate;
	private boolean copyToClipboard;
	private Source source = Source.NATIVE_APP_INTENT;;
	private String sourceUrl;
	private String returnUrlTemplate;
	private Vector<BarcodeFormat> decodeFormats;
	private String characterSet = "iso-8859-1";
	private String versionName;
	private boolean isLogin = false;

	private final OnCompletionListener beepListener = new OnCompletionListener() {
		public void onCompletion(MediaPlayer mediaPlayer) {
			mediaPlayer.seekTo(0);
		}
	};

	ViewfinderView getViewfinderView() {
		return viewfinderView;
	}

	public Handler getHandler() {
		return handler;
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		Window window = getWindow();
		window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		Intent intent = getIntent();		
		isLogin = intent.getBooleanExtra("login", false);
		ELog.i(TAG, "isLogin:"+isLogin);
		setContentView(R.layout.capture);
		initView();
	}

	private void initView() {
		CameraManager.init(getApplication());
		viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
		statusView = (TextView) findViewById(R.id.status_view);

		backBtn = (Button) findViewById(R.id.settings_back);
		backBtn.setVisibility(View.VISIBLE);
		backBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				finish();
			}
		});
		mTextView = (TextView) findViewById(R.id.settings_head_title);
		mTextView.setText(R.string.qr_code);
		manualAddBtn = (Button) findViewById(R.id.manual_add);
		
		if (isLogin) {
			mTextView.setText(R.string.qr_login);
			manualAddBtn.setText(R.string.manaul_login);
		}else{
			mTextView.setText(R.string.qr_code);
		}
		manualAddBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (isLogin) {
					toLoginActivity(false);
				}else{
					toAddActivity();
				}
			}
		});
		handler = null;
		hasSurface = false;
	}

	private void toLoginActivity(boolean toMain) {
		Intent intent = new Intent(CaptureActivity.this,
				LoginActivity.class);
		if (toMain) {
			intent.putExtra("istoMain", true);
		}
		startActivity(intent);
	}

	OnClickListener backButtonClick = new OnClickListener() {
		@Override
		public void onClick(View v) {
			ELog.i(TAG, "点击返回按钮");
			resetStatusView();
			// initView();
			if (handler != null) {
				handler.sendEmptyMessage(R.id.restart_preview);
			}
			CameraManager.init(getApplication());

		}
	};

	private void toAddActivity() {
		Intent intent = new Intent(CaptureActivity.this,
				AddCameraActivity.class);
		startActivity(intent);
	}

	@Override
	protected void onResume() {
		ELog.i(TAG, "onResume");
		super.onResume();
		resetStatusView();

		SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
		SurfaceHolder surfaceHolder = surfaceView.getHolder();
		if (hasSurface) {
			// The activity was paused but not stopped, so the surface still
			// exists. Therefore
			// surfaceCreated() won't be called, so init the camera here.
			initCamera(surfaceHolder);
		} else {
			// Install the callback and wait for surfaceCreated() to init the
			// camera.
			surfaceHolder.addCallback(this);
			surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}

		Intent intent = getIntent();
		String action = intent == null ? null : intent.getAction();
		String dataString = intent == null ? null : intent.getDataString();
		if (intent != null && action != null) {
			if (action.equals(Intents.Scan.ACTION)) {
				// Scan the formats the intent requested, and return the result
				// to the calling activity.
				source = Source.NATIVE_APP_INTENT;
				decodeFormats = DecodeFormatManager.parseDecodeFormats(intent);
			} else if (dataString != null
					&& dataString.contains(PRODUCT_SEARCH_URL_PREFIX)
					&& dataString.contains(PRODUCT_SEARCH_URL_SUFFIX)) {
				// Scan only products and send the result to mobile Product
				// Search.
				source = Source.PRODUCT_SEARCH_LINK;
				sourceUrl = dataString;
				decodeFormats = DecodeFormatManager.PRODUCT_FORMATS;
			} else if (dataString != null && dataString.startsWith(ZXING_URL)) {
				// Scan formats requested in query string (all formats if none
				// specified).
				// If a return URL is specified, send the results there.
				// Otherwise, handle it ourselves.
				source = Source.ZXING_LINK;
				sourceUrl = dataString;
				Uri inputUri = Uri.parse(sourceUrl);
				returnUrlTemplate = inputUri
						.getQueryParameter(RETURN_URL_PARAM);
				decodeFormats = DecodeFormatManager
						.parseDecodeFormats(inputUri);
			} else {
				// Scan all formats and handle the results ourselves (launched
				// from Home).
				source = Source.NONE;
				decodeFormats = null;
			}
			characterSet = intent.getStringExtra(Intents.Scan.CHARACTER_SET);
		} else {
			source = Source.NONE;
			decodeFormats = null;
			characterSet = null;
		}

	}

	@Override
	protected void onPause() {
		super.onPause();
		if (handler != null) {
			handler.quitSynchronously();
			handler = null;
		}
		CameraManager.get().closeDriver();
	}

	@Override
	protected void onDestroy() {
		// inactivityTimer.shutdown();
		super.onDestroy();
	}

	@Override
	public void onConfigurationChanged(Configuration config) {
		// Do nothing, this is to prevent the activity from being restarted when
		// the keyboard opens.
		super.onConfigurationChanged(config);
	}

	public void surfaceCreated(SurfaceHolder holder) {
		if (!hasSurface) {
			hasSurface = true;
			initCamera(holder);
		}
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		hasSurface = false;
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {

	}

	public void handleDecode(Result rawResult, Bitmap barcode) {
		ELog.i(TAG, "找到结果");
		// inactivityTimer.onActivity();

		if (barcode == null) {
			// This is from history -- no saved barcode
			handleDecodeInternally(rawResult, null);
		} else {
			// playBeepSoundAndVibrate();
			// drawResultPoints(barcode, rawResult);
			Log.i(TAG, source + "");
			switch (source) {
			case NATIVE_APP_INTENT:
			case PRODUCT_SEARCH_LINK:
				handleDecodeExternally(rawResult, barcode);
				break;
			case ZXING_LINK:
				if (returnUrlTemplate == null) {
					handleDecodeInternally(rawResult, barcode);
				} else {
					handleDecodeExternally(rawResult, barcode);
				}
				break;
			case NONE:
				handleDecodeInternally(rawResult, barcode);
				break;
			}
		}
	}

	private static void drawLine(Canvas canvas, Paint paint, ResultPoint a,
			ResultPoint b) {
		canvas.drawLine(a.getX(), a.getY(), b.getX(), b.getY(), paint);
	}

	// Put up our own UI for how to handle the decoded contents.
	private void handleDecodeInternally(Result rawResult, Bitmap barcode) {
		statusView.setVisibility(View.GONE);// 去掉底部提示
		viewfinderView.setVisibility(View.GONE);// 去掉预览框
		ResultHandler resultHandler = ResultHandlerFactory.makeResultHandler(
				this, rawResult);
		//
		String formatText = rawResult.getBarcodeFormat().toString();
		ELog.i(TAG, "结果:" + formatText);
		CharSequence displayContents = resultHandler.getDisplayContents();
		String resultString = displayContents.toString();
		// Log.i(TAG, displayContents.toString());// 这里会有乱码
		if (resultString.contains("#")) {
			Intent data = null;
			if (isLogin) {
				data = new Intent(CaptureActivity.this, LoginActivity.class);
			}else{
				data = new Intent(CaptureActivity.this, AddCameraActivity.class);
			}
	
			data.putExtra("caminfo", displayContents.toString());
			startActivity(data);
//			startActivity(data);
//			setResult(3, data);
//			finish();
		} else {
			ELog.i(TAG, "不是myanycam的二维码");
			resetStatusView();
			// initView();
			if (handler != null) {
				handler.sendEmptyMessage(R.id.restart_preview);
			}
			CameraManager.init(getApplication());
		}

	}

	// Briefly show the contents of the barcode, then handle the result outside
	// Barcode Scanner.
	private void handleDecodeExternally(Result rawResult, Bitmap barcode) {
		viewfinderView.drawResultBitmap(barcode);

		// Since this message will only be shown for a second, just tell the
		// user what kind of
		// barcode was found (e.g. contact info) rather than the full contents,
		// which they won't
		// have time to read.
		ResultHandler resultHandler = ResultHandlerFactory.makeResultHandler(
				this, rawResult);
		statusView.setText(getString(resultHandler.getDisplayTitle()));

		if (source == Source.NATIVE_APP_INTENT) {
			// Hand back whatever action they requested - this can be changed to
			// Intents.Scan.ACTION when
			// the deprecated intent is retired.
			Intent intent = new Intent(getIntent().getAction());
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
			intent.putExtra(Intents.Scan.RESULT, rawResult.toString());
			intent.putExtra(Intents.Scan.RESULT_FORMAT, rawResult
					.getBarcodeFormat().toString());
			Message message = Message.obtain(handler, R.id.return_scan_result);
			message.obj = intent;
			handler.sendMessageDelayed(message, INTENT_RESULT_DURATION);
		} else if (source == Source.PRODUCT_SEARCH_LINK) {
			// Reformulate the URL which triggered us into a query, so that the
			// request goes to the same
			// TLD as the scan URL.
			Message message = Message
					.obtain(handler, R.id.launch_product_query);
			int end = sourceUrl.lastIndexOf("/scan");
			message.obj = sourceUrl.substring(0, end) + "?q="
					+ resultHandler.getDisplayContents().toString()
					+ "&source=zxing";
			handler.sendMessageDelayed(message, INTENT_RESULT_DURATION);
		} else if (source == Source.ZXING_LINK) {
			// Replace each occurrence of RETURN_CODE_PLACEHOLDER in the
			// returnUrlTemplate
			// with the scanned code. This allows both queries and REST-style
			// URLs to work.
			Message message = Message
					.obtain(handler, R.id.launch_product_query);
			message.obj = returnUrlTemplate.replace(RETURN_CODE_PLACEHOLDER,
					resultHandler.getDisplayContents().toString());
			handler.sendMessageDelayed(message, INTENT_RESULT_DURATION);
		}
	}

	private void initCamera(SurfaceHolder surfaceHolder) {
		try {
			CameraManager.get().openDriver(surfaceHolder);
		} catch (IOException ioe) {
			ELog.w(TAG, ioe);
			displayFrameworkBugMessageAndExit();
			return;
		} catch (RuntimeException e) {
			// Barcode Scanner has seen crashes in the wild of this variety:
			// java.?lang.?RuntimeException: Fail to connect to camera service
			ELog.w(TAG, "Unexpected error initializating camera", e);
			displayFrameworkBugMessageAndExit();
			return;
		}
		if (handler == null) {
			handler = new CaptureActivityHandler(this, decodeFormats,
					characterSet);
		}
	}

	private void displayFrameworkBugMessageAndExit() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.app_name));

		builder.setOnCancelListener(new FinishListener(this));
		builder.show();
	}

	private void resetStatusView() {
		ELog.i(TAG, "resetStatusView");
		// statusView.setText(R.string.msg_default_status);
		statusView.setVisibility(View.INVISIBLE);
		viewfinderView.setVisibility(View.VISIBLE);

	}

	public void drawViewfinder() {
		viewfinderView.drawViewfinder();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		ELog.i(TAG, "fanhuile "+ requestCode);
		finish();
		super.onActivityResult(requestCode, resultCode, data);
	}
	
}
