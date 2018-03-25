
package com.myanycamm.ui;

import java.net.URL;
import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.myanycamm.cam.BaseActivity;
import com.myanycamm.cam.R;
import com.myanycamm.utils.ELog;
import com.photostore.zoom.ImageZoomView;
import com.photostore.zoom.SimpleZoomListener;
import com.photostore.zoom.SimpleZoomListener.ControlType;
import com.photostore.zoom.ZoomState;


public class ImageSwitcher extends BaseActivity {
	private final String TAG = "ImageSwitcher";

	private int mIndex;

	private int mItemwidth;
	private int mItemHerght;

	private ArrayList<String> pathes;
	private Drawable netDrawable;

	private ProgressBar mProgressBar;

	
	private ImageZoomView mZoomView;

	
	private ZoomState mZoomState;

	
	private SimpleZoomListener mZoomListener;

	private Bitmap zoomBitmap;

	private ImageView mMovedItem;
	private boolean isMoved;

	private FlingGallery mFlingGallery;

	public int getmIndex() {
		return mIndex;
	}

	public void updateState(int visibility) {
		mProgressBar.setVisibility(visibility);
		mFlingGallery.setCanTouch(View.GONE == visibility);
	}

	private boolean isViewIntent() {
		String action = getIntent().getAction();
		ELog.i(TAG, "action:" + action);
		return Intent.ACTION_VIEW.equals(action);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		mItemwidth = dm.widthPixels;
		mItemHerght = dm.heightPixels;
		// mInflater = LayoutInflater.from(this);
		if (!isViewIntent()) {
			ELog.i(TAG, "pathes");
			pathes = intent.getStringArrayListExtra("pathes");
			mIndex = intent.getIntExtra("index", 0);
		} else {
			ELog.i(TAG, "不是从相册里面来的");
			pathes = new ArrayList<String>();
			// pathes.add(intent.getData().getPath());
			pathes.add(intent.getStringExtra("url"));
			// pathes.add(Environment.getExternalStorageDirectory()+"/myanycam/a.jpg");
			ELog.i(TAG, pathes.get(0));
			mIndex = 0;
		}

		setContentView(R.layout.myhorizontalview);
		mProgressBar = (ProgressBar) findViewById(R.id.progress_circular);
		mMovedItem = (ImageView) findViewById(R.id.removed);
		mFlingGallery = (FlingGallery) findViewById(R.id.horizontalview);
		mZoomView = (ImageZoomView) findViewById(R.id.zoomview);

		mZoomState = new ZoomState();
		mZoomListener = new SimpleZoomListener();
		mZoomListener.setmGestureDetector(new GestureDetector(this,
				new MyGestureListener()));

		mZoomListener.setZoomState(mZoomState);
		mZoomListener.setControlType(ControlType.ZOOM);
		mZoomView.setZoomState(mZoomState);
		mZoomView.setOnTouchListener(mZoomListener);

		// hsv = (MyHorizontalScrollView)
		mFlingGallery.setAdapter(new ArrayAdapter<String>(
				getApplicationContext(), android.R.layout.simple_list_item_1,
				pathes) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				return new GalleryViewItem(getApplicationContext(), position);
			}
		}, mIndex);
	}
	
	@Override
	public void finish() {
		ELog.i(TAG, "finish...");
		super.finish();
	}
	
//	@Override
//	public boolean onKeyDown(int keyCode, KeyEvent event) {
//		if (keyCode == KeyEvent.KEYCODE_BACK) {
//			ELog.i(TAG, "按了返回键");
//			return true;
//		}
//		return super.onKeyDown(keyCode, event);
//	}

	public void goneTempImage() {
	}

	private Bitmap getDrawable(int index, int zoom) {
		if (index >= 0 && index < pathes.size()) {
			String path = pathes.get(index);

			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(path, options);
			int mWidth = options.outWidth;
			int mHeight = options.outHeight;
			int s = 1;
			while ((mWidth / s > mItemwidth * 2 * zoom) || (mHeight / s > mItemHerght * 2 * zoom)) {
				s *= 2;
			}

			options = new BitmapFactory.Options();
			options.inPreferredConfig = Config.ARGB_8888;
			options.inSampleSize = s;
			Bitmap bm = null;
			if (!isViewIntent()) {
				bm = BitmapFactory.decodeFile(path, options);
			}else{

				BitmapDrawable bd = (BitmapDrawable) netDrawable; 
				bm = bd.getBitmap();
			}

			if (bm != null) {
				int h = bm.getHeight();
				int w = bm.getWidth();

				float ft = (float) ((float) w / (float) h);
				float fs = (float) ((float) mItemwidth / (float) mItemHerght);

				int neww = ft >= fs ? mItemwidth * zoom : (int) (mItemHerght * zoom * ft);
				int newh = ft >= fs ? (int) (mItemwidth * zoom / ft) : mItemHerght * zoom;

				float scaleWidth = ((float) neww) / w;
				float scaleHeight = ((float) newh) / h;

				Matrix matrix = new Matrix();
				matrix.postScale(scaleWidth, scaleHeight);
				bm = Bitmap.createBitmap(bm, 0, 0, w, h, matrix, true);
				return bm;
			}
		}
		return null;
	}



	// 用于根据图片的URL，从网络上下载图片
	protected Drawable loadImageFromUrl(String imageUrl) {
		try {
			// 根据图片的URL，下载图片，并生成Drawable对象
			return Drawable.createFromStream(new URL(imageUrl).openStream(),
					"src");
		} catch (Exception e) {
			ELog.i(TAG, "解析错误:"+e.getMessage());
			throw new RuntimeException(e);
		}
	}

	private void resetZoomState() {
		int currentIndex = mFlingGallery.getCurrentPosition();
		if (zoomBitmap != null) {
			zoomBitmap.recycle();
		}
		ELog.i(TAG, "currenIndex:" + currentIndex);
		zoomBitmap = getDrawable(currentIndex, 3);

		mZoomView.setImage(zoomBitmap);

		mZoomListener.setControlType(ControlType.ZOOM);
		mZoomState.setPanX(0.5f);
		mZoomState.setPanY(0.5f);
		mZoomState.setZoom(3f);
		mZoomState.notifyObservers();
	}

	
	
	
	public void goToZoomPage() {
		resetZoomState();
		mFlingGallery.setVisibility(View.GONE);
		isMoved = false;
		mZoomView.setVisibility(View.VISIBLE);
		mMovedItem.setBackgroundColor(0x0000);
		mMovedItem.setVisibility(View.VISIBLE);
	}

	public void goToSwicherPage() {
		mMovedItem.setVisibility(View.GONE);
		mFlingGallery.setVisibility(View.VISIBLE);
		mZoomView.setVisibility(View.GONE);
	}

	private class MyGestureListener extends
			GestureDetector.SimpleOnGestureListener {
		@Override
		public boolean onDoubleTap(MotionEvent e) {
			ELog.i(TAG, "双击了..");
			goToSwicherPage();
			return true;
		}
	}

	public void movedClick(View v) {
		isMoved = !isMoved;
		if (isMoved) {
			mZoomListener.setControlType(ControlType.PAN);
			mMovedItem
					.setBackgroundColor(R.drawable.pressed_application_background);
		} else {
			mZoomListener.setControlType(ControlType.ZOOM);
			mMovedItem.setBackgroundColor(0x0000);
		}
	}

	@Override
	protected void onDestroy() {
		if (zoomBitmap != null) {
			zoomBitmap.recycle();
		}
		super.onDestroy();
	}

	private class GalleryViewItem extends LinearLayout {

		public GalleryViewItem(Context context, int position) {
			super(context);

			this.setOrientation(LinearLayout.VERTICAL);

			this.setLayoutParams(new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT,
					LinearLayout.LayoutParams.MATCH_PARENT));

			ImageView iv = new ImageView(context);
			if (!isViewIntent()) {
				iv.setImageBitmap(getDrawable(position, 1));
			} else {
				ELog.i(TAG, "到了解析网络图片");
				// iv.setImageBitmap(getDrawable(1, 1));
				netDrawable = loadImageFromUrl(pathes.get(0));
				iv.setImageDrawable(netDrawable);
			}

			this.addView(iv, new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT,
					LinearLayout.LayoutParams.MATCH_PARENT));

		}
	}
}
