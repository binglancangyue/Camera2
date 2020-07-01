package com.android.camera.home_recorder;

import android.content.Intent;
import android.graphics.drawable.Drawable;

public class AppInfo {
	private String appName;
	private String pkgName;
	private int flags;
    private Intent appintent;
	public Intent getAppintent() {
		return appintent;
	}

	public void setAppintent(Intent appintent) {
		this.appintent = appintent;
	}

	public String getAppName() {
		return appName;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

	public String getPkgName() {
		return pkgName;
	}

	public void setPkgName(String pkgName) {
		this.pkgName = pkgName;
	}

	public Drawable getAppIcon() {
		return appIcon;
	}

	public void setAppIcon(Drawable appIcon) {
		this.appIcon = appIcon;
	}
	
	public int getFlags() {
		return flags;
	}

	public void setFlags(int flags) {
		this.flags = flags;
	}

	private Drawable appIcon;

}
