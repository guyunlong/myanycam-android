package com.myanycamm.ui;

import java.util.ArrayList;

import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.view.View;
import android.widget.Toast;

import com.external.maxwin.view.XListView;
import com.external.maxwin.view.XListView.IXListViewListener;
import com.myanycam.bean.CameraListInfo;
import com.myanycam.bean.EventInfo;
import com.myanycam.bean.VideoEventInfo;
import com.myanycam.net.SocketFunction;
import com.myanycamm.cam.CameraCenterActivity;
import com.myanycamm.cam.R;
import com.myanycamm.cam.VLCPlayActivity;
import com.myanycamm.cam.VideoDownLoad;
import com.myanycamm.model.EventVideoAdapter;
import com.myanycamm.ui.OneVideoFileListItemCell.IOneVideoFileItemListener;
import com.myanycamm.utils.ELog;
import com.nmbb.oplayer.ui.player.VideoActivity;

public class VideoEvent extends AnyCamEvent implements IXListViewListener {
	private EventVideoAdapter videoFileListAdapter;
	private XListView xlistView;
	public static ArrayList<VideoEventInfo> videoEventList = new ArrayList<VideoEventInfo>();
	public boolean isDownload = false;

	
	
	private IOneVideoFileItemListener iOneFileItemListener = new IOneVideoFileItemListener() {

		@Override
		public void itemOnClickLisenter(VideoEventInfo videoEventinfo,
				int position) {
			itemClick(CameraListInfo.currentCam, videoEventinfo, position);
		}

		@Override
		public void itemLongClickLisenter(int position) {
			showdelelteDilog(position);
		}

		@Override
		public void lastImageItemOnClickLisenter(VideoEventInfo mVideoEventInfo,int position) {
			if (null == mVideoEventInfo.getTotalName()) {
				return;
			}
			mActivity.showRequestDialog(null);
			SocketFunction.getInstance().downLoadVideo(
					CameraListInfo.currentCam, mVideoEventInfo.getTotalName());
			mActivity.mFileListView.setIsDownload(true);
			videoFileListAdapter.setCurrentDownPosition(position);
			ELog.i(TAG, "点击了下载");

		}
	};

	public VideoEvent(View _mView, CameraCenterActivity _activity) {
		super(_mView, _activity);
		displayListView();
	}

	private final String TAG = "VideoEvent";

	public void notDataChange() {
		ELog.i(TAG, "下载成功,改变数据...");
		mActivity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				 videoEventList.get(videoFileListAdapter.getCurrentDownPosition()).setDownLoad(true);
				 videoFileListAdapter.notifyDataSetChanged();
			}
		});

	}

	public boolean isDownload() {
		return isDownload;
	}

	public void setDownload(boolean isDownload) {
		this.isDownload = isDownload;
	}

	@Override
	public void itemClick(CameraListInfo c, EventInfo e, int position) {
		ELog.i(TAG, "视频cell点击");
		setDownload(false);

		VideoEventInfo vei = (VideoEventInfo) e;
		if (vei.isDownLoad()) {
			String url = android.os.Environment.getExternalStorageDirectory()
			.getPath()+"/myanycam/video/"+e.getTotalName();
			ELog.i(TAG, "此视频已经下载了。。。"+url);
			goIntent(url);
		
		}else{
			mActivity.showRequestDialog(null);
			SocketFunction.getInstance().downLoadVideo(c, e.getTotalName());	
		}

	}

	@Override
	public void goIntent(final String url) {
		ELog.i(TAG, "url:"+url);
		mActivity.dimissDialog();
		if (isDownload) {
			mActivity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					try {
						VideoDownLoad.getInstance().downloadApkFile(url, null);
					} catch (Exception e) {
						ELog.i(TAG, "下载视频有错误..." + e.getMessage());
						Toast.makeText(mActivity, e.getMessage(),
								Toast.LENGTH_SHORT).show();
						e.printStackTrace();
					}
				}
			});

		} else {
			if(url.startsWith("http")){
				Intent intent = new Intent(mActivity, VideoActivity.class);
				intent.setData(Uri.parse(url));
				intent.putExtra("displayName", "myanycam");		
				mActivity.startActivity(intent);
			}else if(url.endsWith("MOV") || url.endsWith("mov"))  {
				//用自带播放器播放
				 Intent intent = new Intent(Intent.ACTION_VIEW);
	             intent.setDataAndType(Uri.parse(url), "video/mov");
	             mActivity.startActivity(intent);
			}else{
				Intent intent = new Intent(mActivity, VideoActivity.class);
				intent.setData(Uri.parse(url));
				intent.putExtra("displayName", "myanycam");		
				mActivity.startActivity(intent);
			}

			// 用vlc播放
			// Intent intent = new Intent(mActivity, VLCPlayActivity.class);
			// intent.putExtra("url", url);

			// 用vitamio播放
	

//			intent.setData(Uri.parse(Environment.getExternalStorageDirectory()+"/20150707143448.MOV"));

//			ELog.i(TAG, "去播放视频");

//			
		}

	}

	private void displayListView() {

		// dataAdapter = new VideoEventListAdapter(mActivity,
		// R.layout.event_list_row);
		// // Assign adapter to ListView
		// listView = (LoadMoreListView) mView.findViewById(R.id.listView1);
		// listView.setAdapter(dataAdapter);
		//
		// listView.setOnItemClickListener(new OnItemClickListener() {
		// public void onItemClick(AdapterView<?> parent, View view,
		// int position, long id) {
		// setDownload(false);
		// VideoEventInfo eventInfo = (VideoEventInfo) parent
		// .getItemAtPosition(position);
		//
		// itemClick(CameraListInfo.currentCam,
		// videoEventList.get(position),position);
		//
		//
		// }
		// });
		// listView.setOnItemLongClickListener(new OnItemLongClickListener() {
		//
		// @Override
		// public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
		// int arg2, long arg3) {
		// ELog.i(TAG, "长按了" + arg2);
		// showdelelteDilog(arg2);
		// return false;
		// }
		// });
		// listView.setOnFootClickListener(new OnFootClickListener() {
		//
		// @Override
		// public void onClick() {
		// ELog.i(TAG, "点击了底部");
		// listView.updateLoadMoreViewState(ILoadMoreViewState.LMVS_LOADING);
		// mActivity.sf.getVideoList(CameraListInfo.currentCam,
		// videoEventList.size());
		//
		// }
		// });

		xlistView = (XListView) mView.findViewById(R.id.video_file_list);
		xlistView.setPullLoadEnable(true);
		xlistView.setRefreshTime();
		xlistView.setXListViewListener(this, 1);
		videoFileListAdapter = new EventVideoAdapter(mActivity, videoEventList);
		videoFileListAdapter.setiIOneFileItemListener(iOneFileItemListener);
		xlistView.setAdapter(videoFileListAdapter);
	}

	public void addItem(VideoEventInfo pTemp) {
		ELog.i(TAG, "加一个...");
		videoEventList.add(pTemp);
		videoFileListAdapter.notifyDataSetChanged();
	}

	public void clear() {
		videoEventList.clear();
		videoFileListAdapter.notifyDataSetChanged();
	}

	private void showdelelteDilog(final int position) {
		Builder singleDialog = new Builder(mActivity);
		singleDialog.setMessage(R.string.delete_event_confirm);
		singleDialog.setTitle(mActivity.getString(R.string.note));
		singleDialog.setCancelable(true);
		final VideoEventInfo mEventInfo = videoEventList.get(position);
		singleDialog.setPositiveButton(
				mActivity.getResources().getString(R.string.confirm),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						mActivity.sf.deleteVideo(CameraListInfo.currentCam,
								mEventInfo);

						videoEventList.remove(position);
						videoFileListAdapter.notifyDataSetChanged();
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
	public void allDataFinish() {
		xlistView.setPullLoadEnable(false);
	}

	@Override
	public void onRefresh(int id) {
		videoEventList.clear();
		SocketFunction.getInstance().getVideoList(CameraListInfo.currentCam, 0);

	}

	@Override
	public void onLoadMore(int id) {
		SocketFunction.getInstance().getVideoList(CameraListInfo.currentCam,
				videoEventList.size());

	}
}
