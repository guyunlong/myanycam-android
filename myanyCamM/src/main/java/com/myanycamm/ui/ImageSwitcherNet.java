
package com.myanycamm.ui;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

import net.tsz.afinal.FinalBitmap;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.myanycam.bean.CameraListInfo;
import com.myanycam.bean.PicEventInfo;
import com.myanycam.net.SocketFunction;
import com.myanycamm.cam.BaseActivity;
import com.myanycamm.cam.R;
import com.myanycamm.cam.VLCPlayActivity;
import com.myanycamm.ui.PhotoViewAttacher.OnPhotoTapListener;
import com.myanycamm.utils.ELog;
import com.myanycamm.utils.FileUtils;

public class ImageSwitcherNet extends BaseActivity {
	private final static String TAG = "ImageSwitcherNet";

	private static final String STATE_POSITION = "STATE_POSITION";
	public static final String EXTRA_IMAGE_INDEX = "image_index";
	public static final String EXTRA_IMAGE_URLS = "image_urls";
	public static final int PICTURELIST = 300;
	public static final int DOWNLOADPIC = 301;
	public static final int DOWNLOADVIDEO = 303;

	private HackyViewPager mPager;
	private int pagerPosition;
	private TextView indicator;
	private RelativeLayout headRelative;
	ImagePagerAdapter mAdapter;
	FinalBitmap fb;
	private Button backBtn, videoBtn;
	private ProgressBar progressBar;
	private Dialog mDialog = null;

	Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Bundle bundle = msg.getData();
			HashMap<String, String> map = (HashMap) bundle
					.getSerializable("data");
			switch (msg.what) {
			case DOWNLOADPIC:
				ELog.i(TAG,
						"收到下载照片完成..." + map.get("position") + ":"
								+ mPager.getCurrentItem());
				if (Integer.parseInt(map.get("position")) == mPager
						.getCurrentItem()) {
					mAdapter.notifyDataSetChanged();
					ELog.i(TAG, "要改变。。图片");
					progressBar.setVisibility(View.GONE);
					// mPager.setCurrentItem(pagerPosition);
				}
				break;
			case PICTURELIST:
				doPictureList(map);
				mAdapter.notifyDataSetChanged();
				break;
			case DOWNLOADVIDEO:
				String localUrlV = map.get("loaclurl");
				String proxyUrlV = map.get("proxyurl");
				doDownloadVideo(localUrlV, proxyUrlV);
				mDialog.dismiss();
				break;
			default:
				break;
			}
			super.handleMessage(msg);
		}
	};

	// private static ArrayList<String> urls = new ArrayList<String>();
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.image_switcher_net);

		pagerPosition = getIntent().getIntExtra(EXTRA_IMAGE_INDEX, 0);
		progressBar = (ProgressBar) findViewById(R.id.loading);
		// String[] paths = getIntent().getStringArrayExtra(EXTRA_IMAGE_URLS);
		// for (int i = 0; i < paths.length; i++) {
		// urls.add(paths[i]);
		// }
		// urls.add(getIntent().getStringExtra("url"));
		mPager = (HackyViewPager) findViewById(R.id.pager);
		mAdapter = new ImagePagerAdapter();
		mPager.setAdapter(mAdapter);
		indicator = (TextView) findViewById(R.id.indicator);
		CharSequence text = getString(R.string.viewpager_indicator, 1, mPager
				.getAdapter().getCount());
		indicator.setText(text);
		fb = FinalBitmap.create(ImageSwitcherNet.this);
		fb.configLoadfailImage(R.drawable.image_loding);
		fb.configLoadfailImage(R.drawable.image_loding);
		backBtn = (Button) findViewById(R.id.settings_back);
		backBtn.setVisibility(View.VISIBLE);
		headRelative = (RelativeLayout) findViewById(R.id.head_box);
		backBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				finish();
			}
		});
		videoBtn = (Button) findViewById(R.id.settings_btn);
		videoBtn.setText(R.string.look_video);

		// 更新下标
		mPager.setOnPageChangeListener(new OnPageChangeListener() {

			@Override
			public void onPageScrollStateChanged(int arg0) {
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
			}

			@Override
			public void onPageSelected(int arg0) {
				//
				// "进入第"+arg0+"页"+PhotoEvent.photoEventList.get(arg0).getVideoName());

				CharSequence text = getString(R.string.viewpager_indicator,
						arg0 + 1, mPager.getAdapter().getCount());
				indicator.setText(text);
				sendDownload(arg0);
				if ((PhotoEvent.photoEventList.size() - arg0) == 5
						&& PhotoEvent.photoEventList.size() >= 20) {
					sendDownloadPicList();
				}
				// if (arg0 != PhotoEvent.photoEventList.size() - 1) {
				// sendDownload(arg0 + 1);
				// }
				// if (arg0 != 0) {
				// sendDownload(arg0 - 1);
				// }

			}

		});

		ELog.d(TAG, "-------" + pagerPosition);
		if (savedInstanceState != null) {
			pagerPosition = savedInstanceState.getInt(STATE_POSITION);
		}

		mPager.setCurrentItem(pagerPosition);
		SocketFunction.getInstance().setmHandler(mHandler);
	}

	
	private void sendDownload(int position) {
		ELog.i(TAG, FileUtils.externalMemoryAvailable() + "");
		ELog.i(TAG, "url:"
				+ PhotoEvent.photoEventList.get(position).getEventUrl());
		String url = PhotoEvent.photoEventList.get(position).getEventUrl();
		if (!FileUtils.externalMemoryAvailable() && null == url) {
			progressBar.setVisibility(View.VISIBLE);
			SocketFunction.getInstance().downloadPic(CameraListInfo.currentCam,
					PhotoEvent.photoEventList.get(position));
			return;
		}
		if (!FileUtils.externalMemoryAvailable()) {
			try {
				if (null == fb.getBitmapFromCache(url)) {
					ELog.i(TAG,
							"缓存:"
									+ fb.getBitmapFromCache(PhotoEvent.photoEventList
											.get(position).getEventUrl()));
					progressBar.setVisibility(View.VISIBLE);
					SocketFunction.getInstance().downloadPic(
							CameraListInfo.currentCam,
							PhotoEvent.photoEventList.get(position));
					return;
				}
			} catch (NullPointerException e) {
				progressBar.setVisibility(View.VISIBLE);
				SocketFunction.getInstance().downloadPic(
						CameraListInfo.currentCam,
						PhotoEvent.photoEventList.get(position));
				return;
			}

		}
		if (FileUtils.externalMemoryAvailable()
				&& !SocketFunction.getInstance().downloadPic(
						CameraListInfo.currentCam,
						PhotoEvent.photoEventList.get(position))) {
			progressBar.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			ELog.i(TAG, "横向了..");
			headRelative.setVisibility(View.GONE);
		} else {
			ELog.i(TAG, "竖屏了..");
			headRelative.setVisibility(View.VISIBLE);
			// mCameraListView.mHandler.sendEmptyMessage(CloudLivingView.SHWO_HEAD);
		}
	}

	private void sendDownloadPicList() {
		// if (PhotoEvent.photoEventList.size() % 20 == 0) {//后面应该还有
		SocketFunction.getInstance().getPictureList(CameraListInfo.currentCam,
				PhotoEvent.photoEventList.size());
		// }
	}

	public void doPictureList(HashMap<String, String> map) {
		int count = Integer.parseInt(map.get("count"));

		for (int i = 0; i < count; i++) {
			PicEventInfo pTemp = new PicEventInfo();
			pTemp.parsePic(map.get("file" + (i + 1)));
			// pictureInfos.add(pTemp);
			PhotoEvent.photoEventList.add(pTemp);// 加载最后一个
			// mainLayout.addView(genItemView(pictureInfos, i));
		}
		
		// if (count >= 20) {
		// photoAnyCamEvent.updateLoadMoreViewState(0);// 0是正常
		// } else {
		// photoAnyCamEvent.updateLoadMoreViewState(2);// 2是全部加载完了
		// }
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putInt(STATE_POSITION, mPager.getCurrentItem());
	}

	private boolean isViewIntent() {
		String action = getIntent().getAction();
		ELog.i(TAG, "action:" + action);
		return Intent.ACTION_VIEW.equals(action);
	}

	class ImagePagerAdapter extends PagerAdapter {

		@Override
		public int getCount() {
			return PhotoEvent.photoEventList.size();
		}

		@Override
		public View instantiateItem(ViewGroup container, int position) {
			ELog.i(TAG, "进入第" + position + "张图片");
			final int arg = position;
			PhotoView photoView = new PhotoView(container.getContext());
			if (null != PhotoEvent.photoEventList.get(position).getVideoName()) {
				videoBtn.setVisibility(View.VISIBLE);
				videoBtn.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						showRequestDialog(null);
						SocketFunction.getInstance().downLoadVideo(
								CameraListInfo.currentCam,
								PhotoEvent.photoEventList.get(arg)
										.getVideoName());
						ELog.i(TAG, "点击了视频..");

					}
				});
			} else {
				videoBtn.setVisibility(View.INVISIBLE);
			}
			// Bitmap bitMap = BitmapFactory.decodeFile(pathes.get(position));
			// photoView.setImageResource(sDrawables[position]);
			// photoView.setImageBitmap(bitMap);
			String path = FileUtils.getSavePath("eventphoto") + "/"
					+ CameraListInfo.currentCam.getId()
					+ PhotoEvent.photoEventList.get(position).getTotalName();
			if (FileUtils.externalMemoryAvailable()) {
				fb.display(photoView, path);
			} else {
				fb.display(photoView, PhotoEvent.photoEventList.get(position)
						.getEventUrl());
			}
			// photoView.setImageResource(R.drawable.myanycam_icon);
			//
			// Bitmap bitMap =
			// mBitmapCache.getBitmapFromMemCache(ImageSwitcherNet.this,
			// urls.get(position).hashCode()+"");
			// photoView.setImageBitmap(bitMap);
			// String path = FileUtils.getSavePath("") + "/myanycam4404.png";
			// FinalBitmap.create(ImageSwitcherNet.this).display(photoView,
			// path);
			// Now just add PhotoView to ViewPager and return it
			container.addView(photoView, LayoutParams.MATCH_PARENT,
					LayoutParams.MATCH_PARENT);
			photoView.setOnPhotoTapListener(new OnPhotoTapListener() {

				@Override
				public void onPhotoTap(View view, float x, float y) {
					ELog.i(TAG, "单击了图片...");
					// mediaControllerLayout.setVisibility(mediaControllerLayout.getVisibility()==View.GONE?View.VISIBLE:View.GONE);
				}
			});

			return photoView;
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			container.removeView((View) object);
		}

		@Override
		public boolean isViewFromObject(View view, Object object) {
			return view == object;
		}

		@Override
		public int getItemPosition(Object object) {
			// TODO Auto-generated method stub
			return POSITION_NONE;
		}

	}

	
	public void showRequestDialog(String note) {
		if (mDialog != null) {
			mDialog.dismiss();
			mDialog = null;
		}
		mDialog = DialogFactory
				.createLoadingDialog(ImageSwitcherNet.this, note);
		mDialog.setCanceledOnTouchOutside(false);
		mDialog.setOnKeyListener(new OnKeyListener() {

			@Override
			public boolean onKey(DialogInterface dialog, int keyCode,
					KeyEvent event) {
				switch (keyCode) {
				case KeyEvent.KEYCODE_BACK:
					// LoginActivity.this.finish();
					break;

				default:
					break;
				}
				return false;
			}
		});
		mDialog.show();
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
						toVideoProxy(localurl);
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

	private void toVideoProxy(String url) {
		if (url.equals("")) {
			ELog.i(TAG, "提示付款");
		} else {
			Intent intent = new Intent(ImageSwitcherNet.this,
					VLCPlayActivity.class);
			// intent.setAction("android.intent.action.VIEW");

			ELog.i(TAG, "去播放视频");
			intent.putExtra("url", url);
			startActivity(intent);
		}
	}

	// private class ImagePagerAdapter extends FragmentStatePagerAdapter {
	//
	// public String[] fileList;
	//
	// public ImagePagerAdapter(FragmentManager fm, String[] fileList) {
	// super(fm);
	// this.fileList = fileList;
	// }
	//
	// @Override
	// public int getCount() {
	// return fileList == null ? 0 : fileList.length;
	// }
	//
	// @Override
	// public Fragment getItem(int position) {
	// String url = fileList[position];
	// return ImageDetailFragment.newInstance(url);
	// }
	//
	// }

}
