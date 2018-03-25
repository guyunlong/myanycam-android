package com.myanycamm.update;




public class DownLoadFile {
	
	private String url;
	
	public DownLoadFileListener listener;
	
    private String fileName;
    
    private String suffix;
    
    private String tempFileName;
    
    public static String tempSuffix="tmp";
    
    private byte state;
    
    private long totalSize;
    
    private long currSize;
    
    private String savePath;
    
    private String tempSavePath;
    public DownLoadFile(String url,DownLoadFileListener listener,String fileName){
    	this.url=url;
    	this.listener=listener;
    	this.fileName=fileName;
    	init();
    }
    
    public DownLoadFile(String url){
    	this(url,null,null);
    }
    
    public DownLoadFile(String url,DownLoadFileListener listener){
    	this(url,listener,null);
    }
    
    public DownLoadFile(String url,String fileName){
    	this(url,null,fileName);
    }
    
    
    public void init(){
    	if(url==null || (url=url.trim()).length()<=0){
    		setState(DownLoadFileListener.STATE_FAILED);
    		if(listener!=null){
    			listener.downLoadError(this,DownLoadFileListener.ERROR_ILLEGAL_URL);
    		}
    	} 
    	if(fileName==null || fileName.length()<=0){
    		fileName=FileManager.getFileName(url);
    	}
    	suffix=FileManager.getSuffix(url);
//    	if(suffix!=null){
//    		if(!fileName.endsWith(suffix)){
//    			fileName+="."+suffix;
//    		}
//    	}
//    	tempFileName=fileName;
//    	if(suffix!=null){
//    		tempFileName=fileName.substring(0,fileName.length()-suffix.length());
//    	}
    	//临时文件采用url的MD5值
//    	tempFileName=MD5.toMD5(url);
    	tempFileName=fileName;
//    	tempFileName+=tempSuffix;
    }

	public byte getState() {
		return state;
	}

	public void setState(byte state) {
		//状态无改变
		if(this.state==state){
			return;
		}
		this.state = state;
		if(listener!=null){
			listener.downLoadStateChanged(this,state,savePath+fileName);
		}
	}
	
	public String getUrl() {
		return url;
	}

	public String getFileName() {
		return fileName;
	}
	
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getSuffix() {
		return suffix;
	}
	
	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}

	public String getTempFileName() {
		return tempFileName;
	}

	public String getTempSuffix() {
		return tempSuffix;
	}
	
	public void setTotalSize(long totalSize) {
		this.totalSize = totalSize;
	}

	public void setCurrSize(long currSize) {
		this.currSize = currSize;
	}

	public long getCurrSize() {
		return currSize;
	}

	public long getTotalSize() {
		return totalSize;
	}
	
	public void notifyProgressChanged(long maxLen,long currLen){
		if(listener!=null){
			listener.downLoadProgressChanged(this,maxLen, currLen);
		}
	}
	
	public void notifyDownLoadFileNameChanged(String fileName){
		if(listener!=null){
			listener.downLoadFileNameChanged(this,fileName);
		}
	}
	
	
	public boolean notifyFileNameExist(String url,String fileName){
		if(listener!=null){
			return listener.onFileNameExist(this,url, fileName);
		}
		return true;
	}

	public DownLoadFileListener getListener() {
		return listener;
	}

	public void setListener(DownLoadFileListener listener) {
		this.listener = listener;
	}

	public String getSavePath() {
		return savePath;
	}

	public void setSavePath(String savePath) {
		this.savePath = savePath;
	}

	public boolean equals(DownLoadFile downLoadFile) {
		if(downLoadFile==null){
			return false;
		}
		if(downLoadFile.getUrl()!=null && getUrl()!=null && getUrl().equals(downLoadFile.getUrl())&& getFileName().equals(downLoadFile.getFileName())){
			return true;
		}
		return false;
	}

	public String getTempSavePath() {
		return tempSavePath;
	}

	public void setTempSavePath(String tempSavePath) {
		this.tempSavePath = tempSavePath;
	}
}
