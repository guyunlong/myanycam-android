package com.myanycamm.update;



public interface DownLoadFileListener {
	
	public static final byte STATE_WAITING=0;
	
	public static final byte STATE_DOWNING=1;
	 
	public static final byte STATE_PAUSED=2;
	
	public static final byte STATE_DOWNCOMPLETE=3;
	
	public static final byte STATE_FAILED=4;
	
	
	public static final byte ERROR_ILLEGAL_URL=0;
	
	
	public void downLoadStateChanged(DownLoadFile df,byte state,Object object);
	
	public void downLoadProgressChanged(DownLoadFile df,long maxLen,long currLen);
	
	public void downLoadError(DownLoadFile df,byte error);
	  
	public void downLoadFileNameChanged(DownLoadFile df,String fileName);
	
	public boolean onFileNameExist(DownLoadFile df,String url,String fileName);
	
}
