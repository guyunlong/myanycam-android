package com.myanycamm.ui;

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

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.myanycam.bean.VideoEventInfo;
import com.myanycamm.cam.R;
import com.myanycamm.utils.ELog;

public class OneVideoFileListItemCell extends LinearLayout {
	private String TAG = "OneVideoFileListItemCell";
	Context mContext;
	private ImageView aler_type;
	private ImageView event_video;
	private TextView code;
	private LinearLayout good_cell_one;
	private IOneVideoFileItemListener iOneFileItemListener;

	public OneVideoFileListItemCell(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		ELog.i(TAG, "初始化OneFileListItemCell");
	}

	public IOneVideoFileItemListener getIOneFileItemListener() {
		return iOneFileItemListener;
	}

	public void setIOneFileItemListener(
			IOneVideoFileItemListener oneFileItemListener) {
		this.iOneFileItemListener = oneFileItemListener;
	}

	void init() {
		if (null == good_cell_one) {
			good_cell_one = (LinearLayout) findViewById(R.id.good_item_one);
		}

		if (null == aler_type) {
			aler_type = (ImageView) good_cell_one.findViewById(R.id.alert_type);
		}

		if (null == event_video) {
			event_video = (ImageView) good_cell_one
					.findViewById(R.id.event_video);
		}

		if (null == code) {
			code = (TextView) good_cell_one.findViewById(R.id.code);
		}

	}

	public void bindData(final VideoEventInfo videoEventinfo,final int position) {
		init();
		code.setText(videoEventinfo.getTime());
		
		if (videoEventinfo.isDownLoad()) {
			event_video.setImageResource(R.drawable.downloaded_video);
			event_video.setClickable(false);
		}else{
			event_video.setImageResource(R.drawable.down_load_video);
			event_video.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					iOneFileItemListener.lastImageItemOnClickLisenter(videoEventinfo,position);
				}
			});
		}
		event_video.setVisibility(View.VISIBLE);

		good_cell_one.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				iOneFileItemListener.itemOnClickLisenter(videoEventinfo,position);
				
			}
		});
		
		good_cell_one.setOnLongClickListener(new OnLongClickListener() {
			
			@Override
			public boolean onLongClick(View v) {
				iOneFileItemListener.itemLongClickLisenter(position);
				return false;
			}
		});

			aler_type.setImageResource(R.drawable.record_video_time);

	}





	/**
	 * 长按和单击事件
	 */
	public interface IOneVideoFileItemListener {
		public void itemOnClickLisenter(VideoEventInfo videoEventinfo,int position);

		public void itemLongClickLisenter(int position);
		
		public void lastImageItemOnClickLisenter(VideoEventInfo mVideoEventInfo,int position);
	}

}
