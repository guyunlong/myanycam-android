package com.myanycamm.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.media.MediaPlayer;
import android.os.Environment;
import android.util.Log;

public class FileUtils {
	private static String TAG = "FileUtils";
	// sdcard路径
	private String SDPath;
	private MediaPlayer mediaPlayer = null;

	public FileUtils() {
		SDPath=Environment.getExternalStorageDirectory()+"";
	}


	public ArrayList<String> getImageFilesByPath(String path) {
		if(path==null){
		 	return null;
		}
		ArrayList<String> mFiles=null;
		try{
			File mainFile = new File(path);
			File[] files = mainFile.listFiles();
			if(files!=null && files.length>0){
				mFiles=new ArrayList<String>();
			}else{
				return null;
			}
			for(File file:files){
				if(file.isFile()){
					Bitmap bitmap=BitmapFactory.decodeFile(file.getAbsolutePath());
					if(bitmap!=null){
						mFiles.add(file.getAbsolutePath());
					}
				}
			}
		}catch (Exception e) {
		}
		return mFiles;
	}
	
	public String getParentPath(String path){
		if(path == null || (path=path.trim()).length()<=0){
			return null;
		}
//		Log.d(tag, "path=" + path);
		if(path!=null && path.equalsIgnoreCase(SDPath)){
			return null;
		}
		try{
			File file = new File(path);
			if(file!=null){
				return file.getParent();
			}
		}catch (Exception e) {
		}
		return null;
	}
	
	public String getCurrPathDirtoryName(String path){
		if(path == null || (path=path.trim()).length()<=0){
			return null;
		}
		String[] strs=path.split("/");
		if(strs!=null && strs.length>0){
			return strs[strs.length-1];
		}
		return null;
	}
	
	public String getSdcardRootPath(){
		return SDPath;
	}
	
	
	public static void deleteFileWithFilter(String path){
		File selfFile = new File(path);
		String dirName = selfFile.getParent();
		if(dirName.endsWith(File.separator)){
			dirName = dirName + File.separator;
		}
		File file = new File(dirName);
		if(!file.exists() || !file.isDirectory()){//不存在或者目录，返回;
			return;
		}
		File[] files = file.listFiles();
		for(int i = 0; i<files.length; i++){
			if(!files[i].getName().equals(selfFile.getName())){
				files[i].delete();
			}		
		}		
	}
	
	
	public static boolean isFileBitmap(String path){
		if(path==null){
			return false;
		}
		Options options=new Options();
		options.inJustDecodeBounds=true;
		Bitmap bm=BitmapFactory.decodeFile(path,options);
//		if(bm==null){
//			ELog.i("isFileBitmap 图片为空");
//			return false;
//		}
		if(options.outWidth>0 && options.outHeight>0){
			return true;
		}
		if(bm!=null){
			bm.recycle();
			bm=null;
		}
		
		return false;
	}
	
	
	public static boolean isExistFile(String path){
		if (new File(path).exists()) {
			return true;
		}else{
			return false;
		}
	}
	
	   
   public static void saveFile(Bitmap bm, String fileName,String dirName) throws IOException {    
       File dirFile = new File(dirName);    
       if(!dirFile.exists()){    
           dirFile.mkdir();    
       }    
       File myCaptureFile = new File(dirName + fileName);    
       BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(myCaptureFile));    
       bm.compress(Bitmap.CompressFormat.JPEG, 80, bos);    
       bos.flush();    
       bos.close();    
   }    

	
	public static String getExternalStoragePath() {
		// 获取SdCard状态
		String state = android.os.Environment.getExternalStorageState();

		// 判断SdCard是否存在并且是可用的
		if (android.os.Environment.MEDIA_MOUNTED.equals(state)) {
			if (android.os.Environment.getExternalStorageDirectory().canWrite()) {
				return android.os.Environment.getExternalStorageDirectory()
						.getPath();
			}
		}
		ELog.w(TAG, "SD can not be used");
		return null;
	}

	
	public static boolean externalMemoryAvailable() {
		if (getExternalStoragePath() != null) {
			return true;
		}
		return false;
	}

	public static String getSavePath(String dir) {
		String path = getExternalStoragePath() + "/myanycam"+"/"+dir;
		File file = new File(path);
		if (!file.exists()) {
			file.mkdirs();
		}
		return path;
	}
	
	public static void saveByteToFile(byte[] bfile, String filePath) {
        BufferedOutputStream bos = null;
        FileOutputStream fos = null;
        File file = null;
        try {
            File dir = new File(filePath);
            if(!dir.exists()&&dir.isDirectory()){//判断文件目录是否存在
                dir.mkdirs();
            }
            file = new File(filePath);
            fos = new FileOutputStream(file);
            bos = new BufferedOutputStream(fos);
            bos.write(bfile);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }
	
	
}
