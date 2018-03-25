package com.myanycamm.model;

/*
 *
 *       _/_/_/                      _/        _/_/_/_/_/
 *    _/          _/_/      _/_/    _/  _/          _/      _/_/      _/_/
 *   _/  _/_/  _/_/_/_/  _/_/_/_/  _/_/          _/      _/    _/  _/    _/
 *  _/    _/  _/        _/        _/  _/      _/        _/    _/  _/    _/
 *   _/_/_/    _/_/_/    _/_/_/  _/    _/  _/_/_/_/_/    _/_/      _/_/
 *
 *
 *  Copyright 2013-2014, Geek Zoo Studio
 *  http://www.ecmobile.cn/license.html
 *
 *  HQ China:
 *    2319 Est.Tower Van Palace
 *    No.2 Guandongdian South Street
 *    Beijing , China
 *
 *  U.S. Office:
 *    One Park Place, Elmira College, NY, 14901, USA
 *
 *  QQ Group:   329673575
 *  BBS:        bbs.ecmobile.cn
 *  Fax:        +86-10-6561-5510
 *  Mail:       info@geek-zoo.com
 */

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.myanycam.bean.VideoEventInfo;
import com.myanycamm.cam.R;
import com.myanycamm.ui.OneVideoFileListItemCell;
import com.myanycamm.ui.OneVideoFileListItemCell.IOneVideoFileItemListener;
import com.myanycamm.utils.ELog;

/**
 * @author mc374
 * 
 */
public class EventVideoAdapter extends BeeBaseAdapter {

	private String TAG = "EventVideoAdapter";
	private IOneVideoFileItemListener iOneFileItemListener;
	
	private int currentDownPosition;

	public EventVideoAdapter(Context c, ArrayList dataList) {
		super(c, dataList);
		ELog.i(TAG, "初始�?..GoodDescImageAdapter:" + dataList.size());
		// TODO Auto-generated constructor stub
	}
	

	public int getCurrentDownPosition() {
		return currentDownPosition;
	}

	public void setCurrentDownPosition(int currentDownPosition) {
		this.currentDownPosition = currentDownPosition;
	}


	public IOneVideoFileItemListener getiIOneFileItemListener() {
		return iOneFileItemListener;
	}

	public void setiIOneFileItemListener(
			IOneVideoFileItemListener iOneFileItemListener) {
		this.iOneFileItemListener = iOneFileItemListener;
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.Adapter#getCount()
	 */
	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		ELog.i(TAG, "getCount..");
		return dataList.size();

		// return dataList.size()/2;
	}

	public int getItemViewType(int position) {
		return 1;
	}

	@Override
	public int getViewTypeCount() {
		return 1;
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.Adapter#getItemId(int)
	 */
	@Override
	public long getItemId(int position) {
		ELog.i(TAG, "getItemId..");
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected View bindData(int position, View cellView, ViewGroup parent,
			BeeCellHolder h) {
		ELog.i(TAG, "bindData.."+position);
		List<String> itemList = null;

		int distance = dataList.size() - position;
		int cellCount = distance;

		itemList = dataList.subList(position, position + cellCount);

		OneVideoFileListItemCell oneFileListItemCell = (OneVideoFileListItemCell) cellView;
		oneFileListItemCell.setIOneFileItemListener(iOneFileItemListener);
		((OneVideoFileListItemCell) cellView).bindData((VideoEventInfo) dataList.get(position), position);
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public View createCellView() {
		ELog.i(TAG, "createCellView..");
		// TODO Auto-generated method stub
		return mInflater.inflate(R.layout.one_video_file_item, null);
	}

	@Override
	protected BeeCellHolder createCellHolder(View cellView) {
		BeeCellHolder holder = new BeeCellHolder();

		return holder;
	}

	public View getView(int position, View cellView, ViewGroup parent) {
		ELog.i(TAG, "获取view...");
		BeeCellHolder holder = null;
		if (cellView == null) {
			cellView = createCellView();
			holder = createCellHolder(cellView);
			if (null != holder) {
				cellView.setTag(holder);
			}

		} else {
			holder = (BeeCellHolder) cellView.getTag();
			if (holder == null) {
				ELog.v("lottery", "error");
			} else {
				ELog.d("ecmobile", "last position" + holder.position
						+ "    new position" + position + "\n");
			}

		}

		if (null != holder) {
			holder.position = position;
		}

		bindData(position, cellView, parent, holder);
		return cellView;
	}

}
