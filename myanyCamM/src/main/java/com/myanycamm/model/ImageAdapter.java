package com.myanycamm.model;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

public class ImageAdapter extends BaseAdapter {
	// 定义Context
	private Context mContext;
	private ArrayList<String> dataList;

	public ImageAdapter(Context c, ArrayList<String> _dataList) {
		this.mContext	 = c;
		this.dataList = _dataList;
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return dataList.size();
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		ImageView imageView;
		if (convertView == null) {
			// 给ImageView设置资源
			imageView = new ImageView(mContext);
			// 设置布局 图片120×120显示
			imageView.setLayoutParams(new GridView.LayoutParams(120, 120));
			// 设置显示比例类型
			imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
		} else {
			imageView = (ImageView) convertView;
		}

		imageView.setImageBitmap(BitmapFactory.decodeFile(dataList
				.get(position)));
		return imageView;

	}

}
