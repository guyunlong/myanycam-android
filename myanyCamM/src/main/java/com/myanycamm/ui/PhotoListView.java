package com.myanycamm.ui;

import java.io.File;
import java.util.ArrayList;
import net.tsz.afinal.FinalBitmap;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.TextView;

import com.myanycam.net.SocketFunction;
import com.myanycamm.cam.AppServer;
import com.myanycamm.cam.CameraCenterActivity;
import com.myanycamm.cam.PhotoViewActivity;
import com.myanycamm.cam.R;
import com.myanycamm.utils.Constants;
import com.myanycamm.utils.Constants.gridItemEntity;
import com.myanycamm.utils.ELog;


public class PhotoListView extends RelativeLayout {
	private static final String TAG = "PhotoListView";
	private CameraCenterActivity mActivity;
	private SocketFunction sf;
	TabHost mTabHost;
	View photoView;
	TextView photoHeadTitleFile;
	//图片相关
	private LayoutInflater mInflater;
	private int currentConlumID = -1;
	private int currentCount = 1;
	private int displayHeight;
	private int displayWidth;
	private LinearLayout data;

	private int itemh = 150;
	private int itemw = 150;
	public static final String mCardPath = Environment.getExternalStorageDirectory().getPath()+"/myanycam/eventphoto/";
	private ArrayList<String> imagePathes = new ArrayList<String>();
	private boolean exit;
	private boolean isWait;
	private Button backBtn;
	
	private boolean firstRun = true;
//	private Bitmap bm;
	FinalBitmap fb;

	private MThread mThread;

	
	private Handler mHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
				case 0:
					gridItemEntity gie = (gridItemEntity) msg.obj;
					if (gie != null) {
						int num = displayWidth / itemh;
						num = num == 0 ? 1 : num;

						LinearLayout ll;
						if ((currentCount - 1) % num > 0) {
							ll = (LinearLayout) data.findViewWithTag("columnId_" + currentConlumID);
						} else {
							ll = (LinearLayout) mInflater.inflate(R.layout.item_column, null);
							currentConlumID--;
							ll.setTag("columnId_" + currentConlumID);
							for (int j = 0; j < num; j++) {
								LinearLayout child = new LinearLayout(mActivity);
								child.setLayoutParams(new LayoutParams(itemw, itemh));
								child.setTag("item_" + j);
								ll.addView(child);
							}
							data.addView(ll);
						}

						int step = currentCount % num - 1;
						if (step == -1) {
							step = num - 1;
						}
						LinearLayout child = (LinearLayout) ll.findViewWithTag("item_" + step);
						// child.setBackgroundColor(R.color.bright_text_dark_focused);
						child.setBackgroundResource(R.drawable.grid_selector);
						child.setTag(gie);
						child.setOnClickListener(imageClick);
						child.setPadding(10, 10, 10, 10);
						//						
						ImageView v = new ImageView(mActivity);
						// v.setLayoutParams(new LayoutParams(itemw,itemh));
						fb.display(v, gie.path);
//						v.setImageDrawable(gie.image);
						child.addView(v);
						currentCount++;
					}
					break;

				default:
					break;
			}
//			removeMessages(msg.what);
		}
	};


	public PhotoListView(CameraCenterActivity activity, TabHost tabHost) {
		super(activity);
		this.mActivity = activity;
		this.mTabHost = tabHost;
		sf = (SocketFunction) mActivity.getApplicationContext();
		initView();
	}

	private void initView() {
		ELog.i(TAG, "初始化");
		mInflater = LayoutInflater.from(mActivity);
		photoView = mInflater.inflate(R.layout.tab_photo,
				mTabHost.getTabContentView());
		photoHeadTitleFile = (TextView) photoView
				.findViewById(R.id.settings_head_title_photo);
		backBtn = (Button) photoView.findViewById(R.id.settings_back_photo);
		if (AppServer.isAp) {
			backBtn.setVisibility(View.GONE);
		}else{
			backBtn.setVisibility(View.VISIBLE);
		}
	
		backBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mActivity.finish();
			}
		});
		DisplayMetrics dm = new DisplayMetrics();
		mActivity.getWindowManager().getDefaultDisplay().getMetrics(dm);
		displayHeight = dm.heightPixels;
		displayWidth = dm.widthPixels;
		setHead();
	}

	public void setHead() {
		ELog.i(TAG, "到了setHead");
		photoHeadTitleFile.setText(R.string.photos1);
//		if (!FileUtils.externalMemoryAvailable()) {
//			return;
//		}
		imagePathes.clear();
		currentCount = 1;
		getFiles(mCardPath);
		data = (LinearLayout) photoView.findViewById(R.id.layout_webnav);
		ELog.i(TAG, "data:"+data);
		data.removeAllViews();
//		ELog.i(TAG, "thread:"+mThread.isAlive());
		fb = FinalBitmap.create(mActivity);
		mThread = new MThread();
		mThread.start();
//		if(mThread.isAlive()){
//			ELog.i(TAG, "线程还活着..");
//			synchronized(mThread){
//				mThread.notify();
//			}
//		}else{
////			if(firstRun){
////				ELog.i(TAG, "第一次启动");
////				firstRun = !firstRun;
//				mThread.start();
////			}
//		}
	}
	
	private OnClickListener imageClick = new OnClickListener() {

		@Override
		public void onClick(View view) {
			gridItemEntity gie = (gridItemEntity) view.getTag();
//			Intent it = new Intent(mActivity,ImageSwitcher.class);
			Intent it = new Intent(mActivity,PhotoViewActivity.class);
			it.putStringArrayListExtra("pathes", imagePathes);
			it.putExtra("index", gie.index);
			mActivity.startActivityForResult(it, 1);
			if(mThread.isAlive()){
				isWait = true;
			}
		}
	};

	
	private void getFiles(String path) {
		File f = new File(path);
		File[] files = f.listFiles();
		if (files != null) {
			for (int i = 0; i < files.length; i++) {
				final File ff = files[i];
				if (ff.isDirectory()) {
					getFiles(ff.getPath());
				} else {
					String fName = ff.getName();
					if (fName.indexOf(".") > -1) {
						String end = fName.substring(fName.lastIndexOf(".") + 1, fName.length()).toUpperCase();
						if (Constants.getExtens().contains(end)) {
							imagePathes.add(0, ff.getPath());//排序
//							imagePathes.add(ff.getPath());
						}
					}
				}
			}
		}

	}
	
	
//	private Bitmap getDrawable(int index, int zoom) {
//		if (index >= 0 && index < imagePathes.size()) {
//			String path = imagePathes.get(index);
//			BitmapFactory.Options options = new BitmapFactory.Options();
//			options.inJustDecodeBounds = true;
//			BitmapFactory.decodeFile(path, options);
//			int mWidth = options.outWidth;
//			int mHeight = options.outHeight;
//			int s = 1;
//			while ((mWidth / s > itemw * 2 * zoom) || (mHeight / s > itemh * 2 * zoom)) {
//				s *= 2;
//			}
//
//			options = new BitmapFactory.Options();
//			options.inSampleSize = s;
//			options.inPreferredConfig = Config.ARGB_8888;
//			bm = BitmapFactory.decodeFile(path, options);
//
//			if (bm != null) {
//				int h = bm.getHeight();
//				int w = bm.getWidth();
//
//				float ft = (float) ((float) w / (float) h);
//				float fs = (float) ((float) itemw / (float) itemh);
//
//				int neww = ft >= fs ? itemw * zoom : (int) (itemh * zoom * ft);
//				int newh = ft >= fs ? (int) (itemw * zoom / ft) : itemh * zoom;
//
//				float scaleWidth = ((float) neww) / w;
//				float scaleHeight = ((float) newh) / h;
//
//				Matrix matrix = new Matrix();
//				matrix.postScale(scaleWidth, scaleHeight);
//				bm = Bitmap.createBitmap(bm, 0, 0, w, h, matrix, true);
//
//				// Bitmap bm1 = Bitmap.createScaledBitmap(bm, w, h, true);
////				if (!bm.isRecycled()) {// 先判断图片是否已释放了
////					bm.recycle();
////				}
//				return bm;
//			}
//		}
//		return null;
//	}
//	
	
	public void recycleBm(){
//		data.removeAllViews();
//		if ( bm !=null) {
//			bm.recycle();
//			bm = null;
//		}
	}
	
	class MThread extends Thread{

		@Override
		public void run() {	

			for (int i = 0; i < imagePathes.size() && !exit; i++) {

				String path = imagePathes.get(i);
				if (new File(path).exists()) {
					
					gridItemEntity gie = new gridItemEntity();
//					Bitmap bm = getDrawable(i, 2);
					
//						gie.image = new BitmapDrawable(bm);
						gie.path = path;
						gie.index = i;
						android.os.Message msg = new Message();
						msg = new Message();
						msg.what = 0;
						msg.obj = gie;
						mHandler.sendMessage(msg);
				}
			}
		
		}
		
	}
}
