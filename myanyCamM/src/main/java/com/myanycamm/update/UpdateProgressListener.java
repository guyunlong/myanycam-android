package com.myanycamm.update;


public interface UpdateProgressListener {
	
	public void downLoadStateChanged(DownLoadFile df,byte state,Object object);
	
	public void downLoadProgressChanged(DownLoadFile df,long maxLen,long currLen);
}
