package com.myanycamm.update;

import java.io.Serializable;

import org.xmlpull.v1.XmlPullParser;

import com.myanycam.net.SocketFunction;
import com.myanycamm.utils.ELog;
import com.myanycamm.utils.Utils;


public class UpdateInfo implements Serializable {

	public static int UPDATE_TYPE_NOINFO = 0;// 不升级或无升级
	public static int UPDATE_TYPE_TIP = 1;// 提示升级
	public static int UPDATE_TYPE_ENFORE = 2;// 强制升级

	public final static String TAG = "UpdateInfo";
	
	
	private int updateType;
	
	private String updateUrl;
	
	private String updateTip;
	
	private String updateTitle;
	private String googleUrl;
	private String version;

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	
	public boolean isShowSelect = false;

	public int getUpdateType() {
		return updateType;
	}

	public void setUpdateType(int updateType) {
		this.updateType = updateType;
	}

	public String getUpdateUrl() {
		return updateUrl;
	}

	public void setUpdateUrl(String updateUrl) {
		this.updateUrl = updateUrl;
	}

	public String getUpdateTip() {
		return updateTip;
	}

	public void setUpdateTip(String updateTip) {
		this.updateTip = updateTip;
	}

	public String getGoogleUrl() {
		return googleUrl;
	}

	public void setGoogleUrl(String googleUrl) {
		this.googleUrl = googleUrl;
	}

	public String toString() {
		return "updateType=" + updateType + " updateUrl=" + updateUrl
				+ " updateTip=" + updateTip + " updateTitle=" + updateTitle
				+ " googleurl=" + googleUrl;
	}

	public static UpdateInfo parser(XmlPullParser parser) {
		ELog.i(TAG, "升级解析xml");
		try {
			UpdateInfo info = new UpdateInfo();
			int eventType = parser.next();
			String tagName = null;
			while (eventType != XmlPullParser.END_DOCUMENT) {
				if (eventType == XmlPullParser.START_TAG) {
					tagName = parser.getName();
				} else if (eventType == XmlPullParser.END_TAG) {
					tagName = null;
				} else if (eventType == XmlPullParser.TEXT) {
					String text = parser.getText();
					if ("version".equalsIgnoreCase(tagName)) {
						info.setVersion(text);
						ELog.i(TAG,
								"本地:"
										+ Utils.getAppVersionName(SocketFunction
												.getAppContext()) + "网络:"
										+ text);
					} else if ("type".equalsIgnoreCase(tagName)) {
						try {
							int type = Integer.parseInt(text);
							if (info.getVersion().equals(
									Utils.getAppVersionName(SocketFunction
											.getAppContext()))) {
								info.setUpdateType(0);// 如果版本相同，不用升级
							} else {
								info.setUpdateType(type);
							}

						} catch (Exception e) {
						}
					} else if ("url".equalsIgnoreCase(tagName)) {
						info.setUpdateUrl(text);
					} else if ("googleurl".equalsIgnoreCase(tagName)) {
						info.setGoogleUrl(text);
					} else if ("tip".equalsIgnoreCase(tagName)) {
						info.setUpdateTip(text);
					} else if ("tip_title".equals(tagName)) {
						info.setUpdateTitle(text);
					} else if ("isShow_checked".equals(tagName)) {
						if (1 == parserInt(text)) {
							info.setShowSelect(true);
						} else {
							info.setShowSelect(false);
						}
					}
				}
				eventType = parser.next();
			}
			return info;
		} catch (Exception e) {
		}
		return null;
	}

	public String getUpdateTitle() {
		return updateTitle;
	}

	public void setUpdateTitle(String updateTitle) {
		this.updateTitle = updateTitle;
	}

	public boolean isShowSelect() {
		return isShowSelect;
	}

	public void setShowSelect(boolean isShowSelect) {
		this.isShowSelect = isShowSelect;
	}

	public static int parserInt(String str) {
		if (str == null || (str = str.trim()).length() <= 0) {
			return 0;
		}
		try {
			return Integer.parseInt(str);
		} catch (Exception e) {
		}
		return 0;
	}
}
