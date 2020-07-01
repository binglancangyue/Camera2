package com.android.camera.home_recorder;

import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.RemoteException;
import android.util.Log;

public class Utils {
	private static final String TAG = "";
	private Context mContext;
	private static Utils utils;
	private ActivityManager activityManager;
	private SharedPreferences preferences;
	private PackageManager pm;

	private Utils(Context mContext) {
		this.mContext = mContext;
		activityManager = (ActivityManager) this.mContext.getSystemService(Context.ACTIVITY_SERVICE);
		pm = this.mContext.getPackageManager();
		preferences = this.mContext.getSharedPreferences("QcHome",Context.MODE_MULTI_PROCESS | Context.MODE_WORLD_READABLE);
	}

	public static Utils getUtilInstance(Context mContext) {
		if (null == utils) {
			utils = new Utils(mContext);
		}
		return utils;
	}

	public int getWindowStatus() {
		int leftid = activityManager.getRightStackId();
		return activityManager.getWindowSizeStatus(leftid);
	}

	public boolean isFullWindow() {
		int status = getWindowStatus();
		if (status == 2) {// full window screen
			return true;
		} else {// half window screen
			return false;
		}
	}
	
	private IActivityManager localIActivityManager = null;
    
    public void doSpliteAction(){
    	if (null == localIActivityManager) {
    		localIActivityManager = ActivityManagerNative.getDefault();
		}
    	Intent localIntent = new Intent();
		try {
		      int leftStack = localIActivityManager.getLeftRightStack(true);
		      int rightStack = localIActivityManager.getLeftRightStack(false);
		      if (leftStack < 0){
		    	  leftStack = 0;
		      }
		      int leftWindowStatus = localIActivityManager.getWindowSizeStatus(leftStack);
		      int rightWindowStatus = localIActivityManager.getWindowSizeStatus(rightStack);
		      if ((leftWindowStatus == 0) && (leftStack >= 0)){
		        if ((rightWindowStatus == 0) && (rightStack > 0)){
		        	localIActivityManager.setWindowSize(rightStack, 2);
		        }
		        localIActivityManager.setWindowSize(leftStack, 1);
		      }else if ((leftWindowStatus == 1) && (leftStack >= 0)){
		        localIActivityManager.setWindowSize(leftStack, 0);
		        if ((rightWindowStatus == 2) && (rightStack > 0)){
		        	localIActivityManager.setWindowSize(rightStack, 0);
		        }
		      }else if ((rightWindowStatus == 0) && (rightStack > 0)){
		        localIActivityManager.setWindowSize(rightStack, 2);
		        if ((leftWindowStatus != 2) && (leftStack >= 0)){
		        	localIActivityManager.setWindowSize(leftStack, 1);
		        }
		      }else if ((rightWindowStatus == 2) && (rightStack > 0)){
		        if ((leftWindowStatus != 2) && (leftStack >= 0)){
		        	localIActivityManager.setWindowSize(leftStack, 0);
		        }
		        localIActivityManager.setWindowSize(rightStack, 0);
		      }
		      localIntent.setAction("android.intent.action.SPLIT_WINDOW_HAS_CHANGED");
		      mContext.sendBroadcast(localIntent);
		  }catch (RemoteException localRemoteException){
		      Log.e(TAG, "GET AMS faile!!!", localRemoteException);
		  }
    }
	
	public void launcherApp(String packageName) {
		
	}

	public boolean isAppInstalled(String packageName) {
		boolean installed = false;
		try {
			pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
			installed = true;
		} catch (PackageManager.NameNotFoundException e) {
			installed = false;
		}
		return installed;
	}

	public void putInt(String key, int value) {
		preferences.edit().putInt(key, value).commit();
	}

	public int getInt(String key, int defValue) {
		return preferences.getInt(key, defValue);
	}

	public void putString(String key, String value) {
		preferences.edit().putString(key, value).commit();
	}

	public String getString(String key, String defValue) {
		return preferences.getString(key, defValue);
	}

	public void putBoolean(String key, boolean value) {
		preferences.edit().putBoolean(key, value).commit();
	}

	public boolean getBoolean(String key, boolean defValue) {
		return preferences.getBoolean(key, defValue);
	}

}
