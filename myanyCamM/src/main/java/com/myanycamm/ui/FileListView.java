package com.myanycamm.ui;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.TextView;

import com.myanycam.bean.CameraListInfo;
import com.myanycam.bean.PicEventInfo;
import com.myanycam.bean.VideoEventInfo;
import com.myanycam.net.SocketFunction;
import com.myanycamm.cam.AppServer;
import com.myanycamm.cam.CameraCenterActivity;
import com.myanycamm.cam.R;
import com.myanycamm.utils.ELog;

public class FileListView extends RelativeLayout {
	private static String TAG = "FileListView";
	private CameraCenterActivity mActivity;
	public SocketFunction sf;
	TabHost mTabHost;
	View fileView;
	TextView headTitleFile;
	private Button bacBtn;

	private ViewPager mPager;// 页卡内容
	private List<View> listViews;
	private ImageView cursor;// 动画图片
	private TextView t1, t2;// 页卡头标
	private int offset = 0;// 动画图片偏移量
	private int currIndex = 0;// 当前页卡编号
	private int bmpW;// 动画图片宽度
	private LinearLayout tabPhoto, tabVideo;
	public static final int PICTURELIST = 0;
	public static final int DOWNLOADPIC = 1;
	public static final int VIDEOLIST = 2;
	public static final int DOWNLOADVIDEO = 3;
	AlertDialog.Builder builder;

//	public ArrayList<PicEventInfo> pictureInfos = new ArrayList<PicEventInfo>();
	private AnyCamEvent currentEvent;
	private PhotoEvent photoAnyCamEvent;
	private VideoEvent videoAnyCamEvent;

	public FileListView(CameraCenterActivity activity, TabHost tabHost) {
		super(activity);
		this.mActivity = activity;
		this.mTabHost = tabHost;
		sf = mActivity.sf;
		initView();
	}
	
	public void dataChange(){
		videoAnyCamEvent.notDataChange();
	}
	
	public void  setIsDownload(boolean isDown){
		videoAnyCamEvent.isDownload = true;
	}

	private void initView() {
		ELog.i(TAG, "初始化");
		LayoutInflater inflater = LayoutInflater.from(mActivity);
		fileView = inflater.inflate(R.layout.tab_files,
				mTabHost.getTabContentView());
		headTitleFile = (TextView) fileView
				.findViewById(R.id.settings_head_title_file);
		bacBtn = (Button) fileView.findViewById(R.id.settings_back_file);
		if (AppServer.isAp) {
			bacBtn.setVisibility(View.GONE);
		}else{
			bacBtn.setVisibility(View.VISIBLE);
		}
		bacBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mActivity.finish();
			}
		});
		// mainLayout = (LinearLayout)
		// fileView.findViewById(R.id.cam_mainlayout);
		setHead();
		initImageView();
		initTextView();
		initViewPager();
	}

	public void setHead() {
		ELog.i(TAG, "到了setHead");
		headTitleFile.setText(R.string.event);
	}

	
	private void initImageView() {
		cursor = (ImageView) fileView.findViewById(R.id.cursor);
		bmpW = BitmapFactory.decodeResource(getResources(),
				R.drawable.slide_current).getWidth();// 获取图片宽度
		DisplayMetrics dm = new DisplayMetrics();
		mActivity.getWindowManager().getDefaultDisplay().getMetrics(dm);
		int screenW = dm.widthPixels;// 获取分辨率宽度
		offset = (screenW / 2 - bmpW) / 2;// 计算偏移量
		Matrix matrix = new Matrix();
		matrix.postTranslate(offset, 0);
		cursor.setImageMatrix(matrix);// 设置动画初始位置
	}

	
	private void initTextView() {
		tabPhoto = (LinearLayout) fileView.findViewById(R.id.tab_photo);
//		tabPhoto.setBackgroundResource(R.drawable.slidetab_bg_press);
		tabPhoto.setOnClickListener(new MyOnClickListener(0));
		tabVideo = (LinearLayout) fileView.findViewById(R.id.tab_video);
		tabVideo.setOnClickListener(new MyOnClickListener(1));

	}

	
	public void ShowRetryDialog(final String localurl, final String proxyurl) {
		if (null != builder) {
			return;
		}
		builder = DialogFactory.creatReTryDialog(mActivity, localurl);

		builder.setPositiveButton(getResources().getString(R.string.confirm),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
//						doDownLoadPic(localurl, proxyurl);
						builder = null;
					}
				});
		builder.setNegativeButton(
				getResources().getString(R.string.btn_cancel),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						mActivity.dimissDialog();
						builder = null;
					}
				});
		builder.create().show();
	}

	
	private void initViewPager() {
		mPager = (ViewPager) fileView.findViewById(R.id.vPager);
		listViews = new ArrayList<View>();
		LayoutInflater mInflater = LayoutInflater.from(mActivity);
		listViews.add(mInflater.inflate(R.layout.photo_files, null));
		listViews.add(mInflater.inflate(R.layout.video_files, null));
		// 初始话event界面
		photoAnyCamEvent = new PhotoEvent(listViews.get(0), mActivity);
		videoAnyCamEvent = new VideoEvent(listViews.get(1), mActivity);
		currentEvent = photoAnyCamEvent;
//		tabPhoto.setBackgroundResource(R.color.vpager_hover);
//		tabVideo.setBackgroundResource(R.color.vpager_bg);
		mPager.setAdapter(new MyPagerAdapter(listViews));
		mPager.setCurrentItem(0);
		mPager.setOnPageChangeListener(new MyOnPageChangeListener());
	}

	
	public class MyOnPageChangeListener implements OnPageChangeListener {

		int one = offset * 2 + bmpW;// 页卡1 -> 页卡2 偏移量
		int two = one * 2;// 页卡1 -> 页卡3 偏移量

		@Override
		public void onPageSelected(int arg0) {
			ELog.i(TAG, "页卡切换了..." + arg0);
			Animation animation = null;
			switch (arg0) {
			case 0:
				currentEvent = photoAnyCamEvent;
//				tabPhoto.setBackgroundResource(R.color.vpager_hover);
//				tabVideo.setBackgroundResource(R.color.vpager_bg);
				if (currIndex == 1) {
					animation = new TranslateAnimation(one, 0, 0, 0);
				} else if (currIndex == 2) {
					animation = new TranslateAnimation(two, 0, 0, 0);
				}
				break;
			case 1:
				currentEvent = videoAnyCamEvent;
//				tabPhoto.setBackgroundResource(R.color.vpager_bg);
//				tabVideo.setBackgroundResource(R.color.vpager_hover);
				if (VideoEvent.videoEventList.size() == 0) {
					sf.getVideoList(CameraListInfo.currentCam, 0);
//					videoAnyCamEvent.updateLoadMoreViewState(1);// 1是全部加载中
				}
			
				if (currIndex == 0) {
					animation = new TranslateAnimation(offset, one, 0, 0);
				} else if (currIndex == 2) {
					animation = new TranslateAnimation(two, one, 0, 0);
				}
				break;
			case 2:
				if (currIndex == 0) {
					animation = new TranslateAnimation(offset, two, 0, 0);
				} else if (currIndex == 1) {
					animation = new TranslateAnimation(one, two, 0, 0);
				}
				break;
			}
			currIndex = arg0;
			animation.setFillAfter(true);// True:图片停在动画结束位置
			animation.setDuration(300);
			cursor.startAnimation(animation);
		}

		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2) {
		}

		@Override
		public void onPageScrollStateChanged(int arg0) {
		}
	}

	
	public class MyPagerAdapter extends PagerAdapter {
		public List<View> mListViews;

		public MyPagerAdapter(List<View> mListViews) {
			this.mListViews = mListViews;
		}

		@Override
		public void destroyItem(View arg0, int arg1, Object arg2) {
			((ViewPager) arg0).removeView(mListViews.get(arg1));
		}

		@Override
		public void finishUpdate(View arg0) {
		}

		@Override
		public int getCount() {
			return mListViews.size();
		}

		@Override
		public Object instantiateItem(View arg0, int arg1) {
			((ViewPager) arg0).addView(mListViews.get(arg1), 0);
			return mListViews.get(arg1);
		}

		@Override
		public boolean isViewFromObject(View arg0, Object arg1) {
			return arg0 == (arg1);
		}

		@Override
		public void restoreState(Parcelable arg0, ClassLoader arg1) {
		}

		@Override
		public Parcelable saveState() {
			return null;
		}

		@Override
		public void startUpdate(View arg0) {
		}
	}

	
	public class MyOnClickListener implements View.OnClickListener {
		private int index = 0;

		public MyOnClickListener(int i) {
			index = i;
		}

		@Override
		public void onClick(View v) {
			mPager.setCurrentItem(index);
		}
	};

	// 当AdapterView被单击(触摸屏或者键盘)，则返回的Item单击事件

	public void getEvent() {
		if (PhotoEvent.photoEventList.size() == 0) {
			sf.getPictureList(CameraListInfo.currentCam, 0);
		}	
	}

	public void doPictureList(HashMap<String, String> map) {
		int count = Integer.parseInt(map.get("count"));
		for (int i = 0; i < count; i++) {
			PicEventInfo pTemp = new PicEventInfo();
			pTemp.parsePic(map.get("file" + (i + 1)));
//			pictureInfos.add(pTemp);
			photoAnyCamEvent.addItem(pTemp);// 加载最后一个
			// mainLayout.addView(genItemView(pictureInfos, i));
		}
		
		if (count < 20) {
			photoAnyCamEvent.allDataFinish();
		}
		
	}

	public void doDownLoadPic(String position) {
		ELog.i(TAG, "position:" + position);
		photoAnyCamEvent.goIntent(position);
//		// final String testUrl =
//		new Thread(new Runnable() {
//
//			@Override
//			public void run() {
//				try {
//					URL url = new URL(localurl);
//					HttpURLConnection con = (HttpURLConnection) url
//							.openConnection();
//					con.setConnectTimeout(2000);
//					con.setReadTimeout(2000);
//					int state = con.getResponseCode();
//					ELog.i(TAG, "state:" + state);
//					if (state == 200) {				
//						photoAnyCamEvent.goIntent(localurl);
//						// toProxy(proxyurl);
//					} else {
//						toProxy(proxyurl);
//					}
//				} catch (Exception ex) {
//					toProxy(proxyurl);
//					ELog.i(TAG, "连接有错误" + ex.getMessage());
//					ex.printStackTrace();
//				}
//			}
//		}).start();

	}

	public void doVideoList(HashMap<String, String> map) {
		int countV = Integer.parseInt(map.get("count"));
		ELog.i(TAG, "count" + countV);
		for (int i = 0; i < countV; i++) {
			VideoEventInfo vTemp = new VideoEventInfo();
			vTemp.parsePic(map.get("file" + (i + 1)));
			videoAnyCamEvent.addItem(vTemp);
//			videoAnyCamEvent.addItem(videoEventInfos.size() - 1);
		}
		if (countV<20) {
			videoAnyCamEvent.allDataFinish();
		}
//		if (countV >= 20) {
//			videoAnyCamEvent.updateLoadMoreViewState(0);// 0是正常
//		} else {
//			videoAnyCamEvent.updateLoadMoreViewState(2);// 2是全部加载完了
//		}
	}

	public void doDownloadVideo(final String localurl, final String proxyurl) {
		ELog.i(TAG, "url:" + localurl + " proxurl:" + proxyurl);
		// videoAnyCamEvent.goIntent(localurl);
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					URL url = new URL(localurl);
					HttpURLConnection con = (HttpURLConnection) url
							.openConnection();
					con.setConnectTimeout(2000);
					con.setReadTimeout(2000);
					int state = con.getResponseCode();
					ELog.i(TAG, "state:" + state);
					if (state == 200) {
						videoAnyCamEvent.goIntent(localurl);
						// toVideoProxy(proxyurl);
					} else {
						toVideoProxy(proxyurl);
					}
				} catch (Exception ex) {
					toVideoProxy(proxyurl);
					ELog.i(TAG, "连接有错误" + ex.getMessage());
					ex.printStackTrace();
				}
			}
		}).start();
	}

	private void toProxy(String url) {
		if (url.equals("")) {
			ELog.i(TAG, "提示付款");
		} else {
			photoAnyCamEvent.goIntent(url);
		}
	}

	private void toVideoProxy(String url) {
		if (url.equals("")) {
			ELog.i(TAG, "提示付款");
		} else {
			videoAnyCamEvent.goIntent(url);
		}
	}
	
	public void clear(){
		photoAnyCamEvent.clear();
		videoAnyCamEvent.clear();
	}

}
