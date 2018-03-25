package com.myanycamm.ui;

import java.util.ArrayList;

import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.View;

import com.external.maxwin.view.XListView;
import com.external.maxwin.view.XListView.IXListViewListener;
import com.myanycam.bean.CameraListInfo;
import com.myanycam.bean.EventInfo;
import com.myanycam.bean.PicEventInfo;
import com.myanycam.net.SocketFunction;
import com.myanycamm.cam.AppServer;
import com.myanycamm.cam.CameraCenterActivity;
import com.myanycamm.cam.R;
import com.myanycamm.model.EventPhotoAdapter;
import com.myanycamm.process.ScreenManager;
import com.myanycamm.ui.OneFileListItemCell.IOneFileItemListener;
import com.myanycamm.utils.ELog;

public class PhotoEvent extends AnyCamEvent implements IXListViewListener {
	private EventPhotoAdapter photoFileListAdapter;
	private XListView xlistView;

	public static ArrayList<PicEventInfo> photoEventList = new ArrayList<PicEventInfo>();

	public PhotoEvent(View _mView, CameraCenterActivity _activity) {
		super(_mView, _activity);
		displayListView();
	}

	private final String TAG = "PhotoEvent";

	private IOneFileItemListener iOneFileItemListener = new IOneFileItemListener() {

		@Override
		public void itemOnClickLisenter(PicEventInfo picEventinfo, int position) {

			itemClick(CameraListInfo.currentCam, picEventinfo, position);

		}

		@Override
		public void itemLongClickLisenter(int position) {
			ELog.i(TAG, "长按了" + position);
			showdelelteDilog(position);

		}

		@Override
		public void lastImageItemOnClickLisenter(PicEventInfo mPicEventInfo) {

			if (null == mPicEventInfo.getVideoName()) {
				return;
			}
			mActivity.showRequestDialog(null);
			SocketFunction.getInstance().downLoadVideo(
					CameraListInfo.currentCam, mPicEventInfo.getVideoName());

			ELog.i(TAG, "点击了视频..");

		}

	};

	private void displayListView() {

		// dataAdapter = new EventListAdapter(mActivity,
		// R.layout.event_list_row);
		// // Assign adapter to ListView
		// listView = (LoadMoreListView) mView.findViewById(R.id.listView1);
		// listView.setAdapter(dataAdapter);
		//
		// listView.setOnItemClickListener(new OnItemClickListener() {
		// public void onItemClick(AdapterView<?> parent, View view,
		// int position, long id) {
		//
		// PicEventInfo eventInfo = (PicEventInfo) parent
		// .getItemAtPosition(position);
		//
		// itemClick(CameraListInfo.currentCam, eventInfo,position);
		//
		// }
		// });
		// listView.setOnItemLongClickListener(new OnItemLongClickListener() {
		//
		// @Override
		// public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
		// int arg2, long arg3) {

		// }
		// });
		// listView.setOnFootClickListener(new OnFootClickListener() {
		//
		// @Override
		// public void onClick() {
		// ELog.i(TAG, "点击了底部");
		// listView.updateLoadMoreViewState(ILoadMoreViewState.LMVS_LOADING);
		// mActivity.sf.getPictureList(CameraListInfo.currentCam,
		// photoEventList.size());
		//
		// }
		// });

		xlistView = (XListView) mView.findViewById(R.id.photo_file_list);
		xlistView.setPullLoadEnable(true);
		xlistView.setRefreshTime();
		xlistView.setXListViewListener(this, 1);
		photoFileListAdapter = new EventPhotoAdapter(mActivity, photoEventList);
		photoFileListAdapter.setiIOneFileItemListener(iOneFileItemListener);
		xlistView.setAdapter(photoFileListAdapter);

	}

	@Override
	public void itemClick(CameraListInfo c, EventInfo e, int position) {
		ELog.i(TAG, "照片cell点击");
		mActivity.showRequestDialog(null);
		if (mActivity.sf.downloadPic(c, (PicEventInfo) e)) {
			goIntent(position + "");
		}
		;
	}

	@Override
	public void goIntent(String position) {

		if (ScreenManager.getScreenManager().currentActivity().getClass()
				.equals(CameraCenterActivity.class)) {
			Intent intent = new Intent(mActivity, ImageSwitcherNet.class);
			ELog.i(TAG, "要跳转...." + position);
			mActivity.dimissDialog();
			intent.setAction("android.intent.action.VIEW");
			// intent.putExtra(ImageSwitcherNet.EXTRA_IMAGE_URLS, imageUrls);
			intent.putExtra(ImageSwitcherNet.EXTRA_IMAGE_INDEX,
					Integer.parseInt(position));
			// intent.putExtra("url", url);
			mActivity.startActivity(intent);
		}

	}

	private void showdelelteDilog(final int position) {
		Builder singleDialog = new Builder(mActivity);
		singleDialog.setMessage(R.string.delete_event_confirm);
		singleDialog.setTitle(mActivity.getString(R.string.note));
		singleDialog.setCancelable(true);
		final PicEventInfo mEventInfo = photoEventList.get(position);
		singleDialog.setPositiveButton(
				mActivity.getResources().getString(R.string.confirm),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						mActivity.sf.deletePic(CameraListInfo.currentCam,
								mEventInfo);
						photoEventList.remove(position);
						photoFileListAdapter.notifyDataSetChanged();
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

	public void addItem(PicEventInfo pTemp) {
		ELog.i(TAG, "加一个...");
		photoEventList.add(pTemp);
		photoFileListAdapter.notifyDataSetChanged();
	}

	public void clear() {
		photoEventList.clear();
		if (!AppServer.isBackgroud) {
			photoFileListAdapter.notifyDataSetChanged();
		}

	}

	@Override
	public void onRefresh(int id) {
		photoEventList.clear();
		SocketFunction.getInstance().getPictureList(CameraListInfo.currentCam,
				0);

	}

	@Override
	public void onLoadMore(int id) {
		SocketFunction.getInstance().getPictureList(CameraListInfo.currentCam,
				photoEventList.size());

	}

	@Override
	public void allDataFinish() {
		xlistView.setPullLoadEnable(false);

	}

}
