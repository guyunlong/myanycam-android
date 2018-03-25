package com.myanycamm.model;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.myanycam.bean.CameraListInfo;
import com.myanycamm.cam.AddCameraActivity;
import com.myanycamm.cam.R;
import com.myanycamm.utils.ELog;

@SuppressLint("ResourceAsColor")
public class DateAdapter extends BaseAdapter {

	private Activity mActivity;
	// private List<String> lstDate;
	private TextView highlowtemp,alarNum;

	private int realSize;
	private TextView online;
	private ImageView camImage;
	private ImageView camIngImageView,camLineType;

	private static String TAG = "DateAdapter";
	private final int TABLE_MAX_COUNT = 90;
	public static boolean isExchanged = false;
	public static boolean isDeleted = false;

	public DateAdapter(Activity _activity) {

		this.mActivity = _activity;
		realSize = CameraListInfo.cams.size();
//		if (CameraListInfo.cams.size() < TABLE_MAX_COUNT) {
//			CameraListInfo.cams.add(null);
//		}

	}

	@Override
	public int getCount() {
		return CameraListInfo.cams.size();
	}

	@Override
	public Object getItem(int position) {
		return CameraListInfo.cams.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}


	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {

			convertView = LayoutInflater.from(mActivity).inflate(
					R.layout.main_cam_item, null);
			ELog.i(TAG, CameraListInfo.cams.get(position).getSn() + " "+ CameraListInfo.cams.get(position).getStatus());
			camImage = (ImageView) convertView.findViewById(R.id.good_cell_photo_one);
			camIngImageView = (ImageView) convertView.findViewById(R.id.cam_ing);
			camLineType = (ImageView) convertView.findViewById(R.id.cam_line_type);
			if (CameraListInfo.cams.get(position).isAccess()) {
				Bitmap bitmap = BitmapCache.getBitmapFromMemCache(mActivity, CameraListInfo.cams.get(position).getId()+"");
				if (bitmap!=null ) {
					camImage.setImageBitmap(bitmap);
				}
			}else{
				camImage.setImageResource(R.drawable.cam_lock);
			}
			
			highlowtemp = (TextView) convertView.findViewById(R.id.highlowtemp);
			alarNum = (TextView) convertView.findViewById(R.id.alar_num);
			online = (TextView) convertView.findViewById(R.id.online_text);

				highlowtemp.setText(CameraListInfo.cams.get(position).getName());
				switch (CameraListInfo.cams.get(position).getStatus()) {
				case 0:
					online.setText(R.string.cam_offline);
					camLineType.setImageResource(R.drawable.cam_offlin_type);
					break;
				case 1:
					online.setText(R.string.cam_online);
					camLineType.setImageResource(R.drawable.cam_online_type);
					break;
				case 2:
					online.setText(R.string.cam_online);
					camLineType.setImageResource(R.drawable.cam_online_type);
					camIngImageView.setVisibility(View.VISIBLE);
//					camIcon.setImageResource(R.drawable.cam_online_ico_1);
					break;		
				case 3:
					online.setText(R.string.cam_updating);
					break;

				default:
					break;
				}
//				camIcon.setImageResource((CameraListInfo.cams.get(position).getStatus()==1)?R.drawable.cam_online_ico:R.drawable.cam_offline_icon);
				if (CameraListInfo.cams.get(position).getAlertNum() != 0) {
					alarNum.setText(CameraListInfo.cams.get(position).getAlertNum()+"");
					alarNum.setVisibility(View.VISIBLE);
				}
		
		return convertView;
	}



	private void toAddCam(){
		Intent intent = new Intent(mActivity, AddCameraActivity.class);
		mActivity.startActivity(intent);
	}
	
	private void showDeleteDialog(final int position) {
	}


}
