package com.myanycamm.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.myanycamm.cam.R;



public class LoadMoreListView extends ListView implements OnScrollListener {

	private static final String TAG = "LoadMoreListView";

	
	private OnScrollListener mOnScrollListener;
	private LayoutInflater mInflater;

	// footer view
	private RelativeLayout mFooterView;
	private TextView mLoadMoreTextView;
	private View mLoadMoreView;
	private View mLoadingView;
	// private TextView mLabLoadMore;
	private ProgressBar mProgressBarLoadMore;

	// Listener to process load more items when user reaches the end of the list
	private OnLoadMoreListener mOnLoadMoreListener;
	private OnFootClickListener mOnFootClickListener;
	// To know if the list is loading more items
	private boolean mIsLoadingMore = false;
	private int mCurrentScrollState;

	public LoadMoreListView(Context context) {
		super(context);
		init(context);
	}

	public LoadMoreListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public LoadMoreListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	private void init(Context context) {

		mInflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		// footer
		mFooterView = (RelativeLayout) mInflater.inflate(
				R.layout.load_more_footer, this, false);
		mLoadMoreView = mFooterView.findViewById(R.id.load_more_view);
		mLoadMoreTextView = (TextView) mFooterView.findViewById(R.id.load_more_tv);
		mLoadingView = mFooterView.findViewById(R.id.loading_layout);
		
//		mProgressBarLoadMore = (ProgressBar) mFooterView
//				.findViewById(R.id.load_more_progressBar);

		addFooterView(mFooterView);
//		updateLoadMoreViewState(ILoadMoreViewState.LMVS_NORMAL);
		mLoadMoreView.setOnClickListener(picMoreClickListener);

		super.setOnScrollListener(this);
	}

	@Override
	public void setAdapter(ListAdapter adapter) {
		super.setAdapter(adapter);
	}

	
	@Override
	public void setOnScrollListener(AbsListView.OnScrollListener l) {
		mOnScrollListener = l;
	}

	

	public void setOnLoadMoreListener(OnLoadMoreListener onLoadMoreListener) {
		mOnLoadMoreListener = onLoadMoreListener;
	}
	
	public void setOnFootClickListener(OnFootClickListener onFootClickListener){
		mOnFootClickListener = onFootClickListener;
	}

	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {

		if (mOnScrollListener != null) {
			mOnScrollListener.onScroll(view, firstVisibleItem,
					visibleItemCount, totalItemCount);
		}

		if (mOnLoadMoreListener != null) {

			if (visibleItemCount == totalItemCount) {
				mProgressBarLoadMore.setVisibility(View.GONE);
				// mLabLoadMore.setVisibility(View.GONE);
				return;
			}

			boolean loadMore = firstVisibleItem + visibleItemCount >= totalItemCount;

			if (!mIsLoadingMore && loadMore
					&& mCurrentScrollState != SCROLL_STATE_IDLE) {
				mProgressBarLoadMore.setVisibility(View.VISIBLE);
				// mLabLoadMore.setVisibility(View.VISIBLE);
				mIsLoadingMore = true;
				onLoadMore();
			}

		}

	}

	public void onScrollStateChanged(AbsListView view, int scrollState) {
		mCurrentScrollState = scrollState;

		if (mOnScrollListener != null) {
			mOnScrollListener.onScrollStateChanged(view, scrollState);
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
		mProgressBarLoadMore.setVisibility(View.GONE);
	}

	
	public interface OnLoadMoreListener {
		
		public void onLoadMore();
	}
	
	public interface OnFootClickListener {
		
		public void onClick();
	}
	
	
	public interface ILoadMoreViewState {
		int LMVS_NORMAL = 0;
		int LMVS_LOADING = 1;
		int LMVS_OVER = 2;
	}
	
	// 更新footview视图
	public void updateLoadMoreViewState(int state) {
		switch (state) {
		case ILoadMoreViewState.LMVS_NORMAL:
			mLoadingView.setVisibility(View.GONE);
//			mLoadMoreView.setVisibility(View.GONE);
			mLoadMoreTextView.setVisibility(View.VISIBLE);
			mLoadMoreTextView.setText(R.string.load_more);
			mLoadMoreView.setOnClickListener(picMoreClickListener);
			break;
		case ILoadMoreViewState.LMVS_LOADING:
			mLoadingView.setVisibility(View.VISIBLE);
			mLoadMoreTextView.setVisibility(View.GONE);
			mLoadMoreView.setOnClickListener(null);
			break;
		case ILoadMoreViewState.LMVS_OVER:
			mLoadingView.setVisibility(View.GONE);
			mLoadMoreTextView.setVisibility(View.VISIBLE);
			mLoadMoreTextView.setText(R.string.no_more_event);
			mLoadMoreView.setOnClickListener(null);
			break;
		default:
			break;
		}

//		 mLoadMoreState = state;
	}
	
	OnClickListener picMoreClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			mOnFootClickListener.onClick();			
		}
	};
	
	
	

}