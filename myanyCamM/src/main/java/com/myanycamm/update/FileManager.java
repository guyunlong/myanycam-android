package com.myanycamm.update;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;

import com.myanycamm.utils.ELog;



public class FileManager {

	
	public static String getFileName(String url){
		if(url==null || (url=url.trim()).length()<=0){
			return null;
		}
		int pos=url.indexOf("?");
		if(pos>0){
			url=url.substring(0,pos);
		}
		if(url.endsWith("/") && url.length()>2){
			url=url.substring(0,url.length()-1);
		}
		pos=url.lastIndexOf("/");
		if(pos>=0){
			 return url.substring(pos+1);
		}
		return null;
	}
	
	
	public static String getSuffix(String url){
		return getSuffixByFileName(getFileName(url));
	}
	
	
	public static String getSuffixByFileName(String fileName){
		if(fileName==null || (fileName=fileName.trim()).length()<=0){
			return null;
		}
		int pos=fileName.lastIndexOf(".");
		if(pos>=0 && fileName.length()-1>pos){
		   return fileName.substring(pos+1);	
		}  
		return null;
	} 
	
	
	public static String getNewFileName(String filePath,String fileName,int index){
		if(fileName==null || (fileName=fileName.trim()).length()<=0){
			return null;
		}
		String suffix=getSuffixByFileName(fileName);
		String newFileName;
		if(suffix==null){ 
			newFileName=fileName+"("+index+")";
		}else{
			newFileName=fileName.substring(0,fileName.length()-suffix.length()-1)+"("+index+")."+suffix;
		}
		if(isFileExist(filePath+newFileName)){
			return getNewFileName(filePath,fileName,index+1);
		}else{
			return newFileName;
		}
	}
	
	
	public static String getNewFileName(String filePath,String fileName,int index,ArrayList<String> shieldStrs){
		String newFileName=getNewFileName(filePath, fileName, index);
		if(shieldStrs==null){
			return newFileName;
		}
		while (true) {
			for(String name:shieldStrs){
				if(newFileName.equals(name)){
					newFileName=increaseFileNameSuffix(newFileName);
					continue;
				}
			}
			return newFileName;
		}
	}
	
	
	public static String getNewFileName(String fileName,ArrayList<String> shieldStrs){
		String newFileName=fileName;
		while (true) {
			for(String name:shieldStrs){
				if(newFileName.equals(name)){
					newFileName=increaseFileNameSuffix(newFileName);
//					ELog.i(MusicApplication.tag,"......newFileName="+newFileName);
					continue;
				}
			}
			return newFileName;
		}
		
	}
	
	
	public static String increaseFileNameSuffix(String fileName){
		if(fileName==null || (fileName=fileName.trim()).length()<=0){
			return null;
		}
		String suffix=getSuffixByFileName(fileName);
		String newFileName;
		int index=0;
		if(suffix!=null){
			fileName=fileName.substring(0,fileName.length()-suffix.length()-1);
		}
		int pos1=fileName.lastIndexOf("(");
		int pos2=fileName.lastIndexOf(")");
		if(pos1>0 && pos2>0 && pos2>=pos1){
			String temp=fileName.substring(pos1+1,pos2);
			if(temp!=null){
				try {
					index=Integer.parseInt(temp);
					fileName=fileName.substring(0,pos1);
				} catch (Exception e) {
				}
			}
		}
		index++;
		if(suffix==null){ 
			newFileName=fileName+"("+index+")";
		}else{
			newFileName=fileName+"("+index+")."+suffix;
		}
		return newFileName;
	}
	
    
	public static boolean isFileExist(String filePath){
		if(filePath==null){
			return false;
		}
		File file=new File(filePath);
		if(file.exists()){
			return true;
		}
		return false;
	}
	
	
	public static long getFileSize(String filePath){
		if(filePath==null){
			return 0;
		}
		File file=new File(filePath);
		if(file.exists()){
			return file.length();
		}
		return 0;
	}
	
	
	public static void deleteFile(String filePath){
		ELog.e("DownLoadManager","deleteFile:filePath="+filePath);
		if(filePath==null){
			return;
		}
		File file=new File(filePath);
		if(file.exists()){
			file.delete();
		}
	}
	
	
	public static String[] getDirectoryFiles(String path){
		if(path==null){
			return null;
		}
		File file=new File(path);
		if(!file.exists()){
			return null;
		}
		String[] files=file.list();
		if(files==null || files.length<=0){
			return null;
		}
		return files;
	}
	
	
	public static void deleteDirectory(String path){
		ELog.e("DownLoadManager","deleteDirectory.."+path);
		File file=new File(path);
		if(!file.exists()){
			return;
		}
		String fPath=file.getAbsolutePath();
		if(file.isDirectory()){
			String[] files=getDirectoryFiles(path);
			if(files==null){
				deleteFile(path);
				return;
			}
			for(String str:files){
				str=fPath+"/"+str;
			    file=new File(str);
				if(file.isDirectory()){
					deleteDirectory(str);
				}else if(file.isFile()){
					deleteFile(str);
				}
			}
			deleteFile(path);
		}else if(file.isFile()){
			deleteFile(path);
		}
	}
	
	
	public static void moveOneFile(String src, String dest) throws Exception {
		copyOneFile(src, dest);
		deleteFile(src);
	}
	
	public static boolean renameFile(String src,String dest){
		if(src==null || dest==null){
//			throw new Exception("源文件目录或是目标文件目录为空");
			return false;
		}
		File srcFile=new File(src);//TODO 文件存在时 另外处理
		if(!srcFile.exists()){
//			throw new Exception("源文件不存在");
			return false;
		}
		File destFile=new File(dest);//TODO 文件存在时 另外处理
		if(destFile.exists()){
			return false;
		}
		return srcFile.renameTo(destFile);
	}
	
	
	public static void copyOneFile(String src, String dest) throws Exception {
		if(src==null || dest==null){
			throw new Exception("源文件目录或是目标文件目录为空");
		}
		ELog.v("DownLoadManager","src="+src+" dest="+dest);
		File srcFile=new File(src);//TODO 文件存在时 另外处理
		if(!srcFile.exists()){
			throw new Exception("源文件不存在");
		}
		File destFile=new File(dest);//TODO 文件存在时 另外处理
		if(!destFile.exists()){
			destFile.createNewFile();
		}
		FileInputStream fis=new FileInputStream(srcFile);
		FileOutputStream fos=new FileOutputStream(destFile);
		byte[] data = new byte[1024];
		int len;
		// 流读取完或是文件被暂停后停止读取数据
		while ((len = fis.read(data)) != -1) {
			fos.write(data, 0, len);
		}
		fos.flush();
		fos.close();
		fis.close();
	}
	
	
	public static String fileSizeFormate(long size) {
		if (size <= 0) {
			return "0kB";
		}
		float kb = (float) size / 1024;
		if (kb < 1024) {
			return floatToString(kb) + "KB";
		} else {
			kb = kb / 1024;
			return floatToString(kb) + "MB";
		}
	}
	
	
	public static String floatToString(float size) {
		String str = "" + size;
		int index = str.indexOf(".");
		if (index > 0 && index + 3 < str.length()) {
			str = str.substring(0, index + 3);
		}
		return str;
	}
}
