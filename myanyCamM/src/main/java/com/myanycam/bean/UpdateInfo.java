
package com.myanycam.bean;

import com.myanycamm.utils.ELog;


public class UpdateInfo {
	private static String TAG = "UpdateInfo";
    private String version;
    private String url;
    private String description;

    public String getVersion() {
    	ELog.i(TAG, "version:"+version);
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
