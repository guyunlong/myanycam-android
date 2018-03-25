package com.myanycamm.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.myanycamm.cam.R;
import com.myanycamm.utils.ELog;



public class PullAndLoadListView extends PullToRefreshListView {

	public PullAndLoadListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initComponent(context);
	}

	// Listener to process load more items when user reaches the end of the list
	private OnLoadMoreListener mOnLoadMoreListener;
	// To know if the list is loading more items
	private boolean mIsLoadingMore = false;

	// footer
	private RelativeLayout mFooterView;
	// private TextView mLabLoadMore;
	private ProgressBar mProgressBarLoadMore;

	public PullAndLoadListView(Context context) { super(context);
	  initComponent(context); }

	public PullAndLoadListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initComponent(context);
	}

	public void initComponent(Context context) {
		ELog.i(TAG, "加载底部..");

		// footer
		mFooterView = (RelativeLayout) mInflater.inflate(
				R.layout.load_more_footer, this, false);
		
//		mProgressBarLoadMore = (ProgressBar) mFooterView
//				.findViewById(R.id.load_more_progressBar);

		addFooterView(mFooterView);
	}

	

	public void setOnLoadMoreListener(OnLoadMoreListener onLoadMoreListener) {
		mOnLoadMoreListener = onLoadMoreListener;
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		super.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);

		// if need a list to load more items
		if (mOnLoadMoreListener != null) {

			if (visibleItemCount == totalItemCount) {
				mProgressBarLoadMore.setVisibility(View.GONE);
				// mLabLoadMore.setVisibility(View.GONE);
				return;
			}

			boolean loadMore = firstVisibleItem + visibleItemCount >= totalItemCount;

			if (!mIsLoadingMore && loadMore && mRefreshState != REFRESHING
					&& mCurrentScrollState != SCROLL_STATE_IDLE) {
				mProgressBarLoadMore.setVisibility(View.VISIBLE);
				// mLabLoadMore.setVisibility(View.VISIBLE);
				mIsLoadingMore = true;
				onLoadMore();
			}

		}
	}

	public void onLoadMore() {
		Log.d(TAG, "onLoadMore");
		if (mOnLoadMoreListener != null) {
			mOnLoadMoreListener.onLoadMore();
		}
	}

	
	public void onLoadMoreComplete() {
		mIsLoadingMore = false;
	}

	
	public interface OnLoadMoreListener {
		
		public void onLoadMore();
	}
}
