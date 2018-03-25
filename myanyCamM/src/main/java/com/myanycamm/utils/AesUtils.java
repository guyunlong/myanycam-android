package com.myanycamm.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class AesUtils {
	/**
	 * @Decription:    字符串aes加密
	 * @param: message 原文
	 * @param:         sKey 密钥
	 */
	public byte[] encrypt(String message, String sKey)  {  
        // 判断Key是否为16位  
        if (sKey.length() != 16) {  
            return null;  
        }  
        
        try {
	        byte[] keyBytes = sKey.getBytes();  
	        SecretKeySpec skeySpec = new SecretKeySpec(keyBytes, "AES");  
	        Cipher cipher = Cipher.getInstance("AES");//"算法/模式/补码方式"  
	        cipher.init(Cipher.ENCRYPT_MODE, skeySpec);  
	        
	        byte[] encrypteds = cipher.doFinal(message.getBytes());  
	  
	        return encrypteds;
        }
        catch (Exception err) {
        	 System.out.println(err.getMessage());
        }

        return null;
    } 
}
