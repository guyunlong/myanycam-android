
package com.myanycamm.cam;

import java.io.File;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import com.myanycamm.ui.DialogFactory;
import com.myanycamm.ui.PhotoView;
import com.myanycamm.ui.PhotoViewAttacher.OnPhotoTapListener;
import com.myanycamm.utils.ELog;

public class PhotoViewActivity extends BaseActivity {
	private final static String TAG = "PhotoViewActivity";

	private ViewPager mViewPager;
	
	private static ArrayList<String> pathes;
	private static int mIndex;
	private static RelativeLayout mediaControllerLayout;
	private ImageButton back, share, delete,rec;
	private SamplePagerAdapter mSamplePagerAdapter;
	
	private OnClickListener backClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			finish();			
		}
	};

	private OnClickListener shareClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {}
	};
	
	private OnClickListener deClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			showDeleteDialog();
		}
	};
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		
		setContentView(R.layout.activity_photo_view);
		mViewPager = (ViewPager) findViewById(R.id.view_pager);
		mediaControllerLayout = (RelativeLayout) findViewById(R.id.mediacontroll);
		mediaControllerLayout.getBackground().setAlpha(80);
		mediaControllerLayout.setVisibility(View.VISIBLE);
		share = (ImageButton) findViewById(R.id.play_speak);
		share.setImageResource(R.drawable.browser_menu_share);
		share.setOnClickListener(shareClickListener);
		back = (ImageButton) findViewById(R.id.play_sound);
		back.setImageResource(R.drawable.browser_menu_gallery);
		back.setOnClickListener(backClickListener);
		delete = (ImageButton) findViewById(R.id.play_photo);
		delete.setImageResource(R.drawable.browser_menu_delete);
		delete.setOnClickListener(deClickListener);
		rec = (ImageButton) findViewById(R.id.play_rec_btn);
		rec.setVisibility(View.GONE);
		Intent intent = getIntent();
		pathes = intent.getStringArrayListExtra("pathes");
		mIndex = intent.getIntExtra("index", 0);
		pathes.get(mIndex);
	mSamplePagerAdapter = new SamplePagerAdapter();
		mViewPager.setAdapter(mSamplePagerAdapter);
		mViewPager.setCurrentItem(mIndex);
	}
	
	@Override
	protected void onStop() {
		super.onStop();
	}	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// TODO Auto-generated method stub
		super.onConfigurationChanged(newConfig);
	}
	
	@Override
	public void onBackPressed() {
		ELog.i(TAG, "按了返回键..");
		super.onBackPressed();
	}

	static class SamplePagerAdapter extends PagerAdapter {


		@Override
		public int getCount() {
			return pathes.size();
		}

		@Override
		public View instantiateItem(ViewGroup container, int position) {
			ELog.i(TAG, "进入第"+position+"张图片");
			PhotoView photoView = new PhotoView(container.getContext());
//			Bitmap bitMap = BitmapFactory.decodeFile(pathes.get(position));
//			photoView.setImageResource(sDrawables[position]);
//			photoView.setImageBitmap(bitMap);
			photoView.setFbImageView(pathes.get(position));

			// Now just add PhotoView to ViewPager and return it
			container.addView(photoView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
			photoView.setOnPhotoTapListener(new OnPhotoTapListener() {
				
				@Override
				public void onPhotoTap(View view, float x, float y) {
					ELog.i(TAG, "单击了图片...");
					mediaControllerLayout.setVisibility(mediaControllerLayout.getVisibility()==View.GONE?View.VISIBLE:View.GONE);
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
	

	public void showDeleteDialog() {
		AlertDialog.Builder builder = DialogFactory.creatReTryDialog(
				PhotoViewActivity.this, getResources()
						.getString(R.string.delete_photo));

		builder.setPositiveButton(getResources().getString(R.string.confirm),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						
						int i = mViewPager.getCurrentItem();						
						ELog.i(TAG, "移除"+i);
//						mViewPager.removeViewAt(i);
						if (i == pathes.size() && pathes.size() != 0) {
							mViewPager.setCurrentItem(i-1);
						}
						File selfFile = new File(pathes.get(i));
						pathes.remove(i);	
						mSamplePagerAdapter.notifyDataSetChanged();					
						if (selfFile.exists()){
							selfFile.delete();
						}
						if (pathes.size() == 0) {
							finish();
						}				
						dialog.dismiss();	
					}
				});
		builder.setNegativeButton(getResources().getString(R.string.btn_cancel),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						ELog.i(TAG, "要删除"+mViewPager.getCurrentItem());
						dialog.dismiss();		
						// stopCam();
					}
				});
		builder.setCancelable(true);
		builder.create().show();
	}

}
