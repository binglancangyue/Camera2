package com.android.camera.v66;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.util.Log;

import com.android.camera2.R;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class MyPreference {
    
    public static final String TAG = "MyPreference";
    public static final String IC_INFO_FILE = "/sys/class/sunxi_info/sys_info";
    public static final String CHIP_ID_NODE = "chipid";
    public static final String PLATFORM_NODE = "platform";
    public static final String CHIP_ID_V66 = "Sun8iw6p1";
    public static final String CHIP_ID_V40 = "sun8iw11p1";
    public static final boolean DEFAULT_DETECT = false;
    public static final boolean DEFAULT_DETECT_V40 = false;
    public static final int DEFAULT_FRONT_REC_Q = 1;
    public static final int DEFAULT_FRONT_PIC_Q = 2;
    //add by chengyuzhou
 //   public static final int DEFAULT_FRONT_PIC_Q_V40 = 1;
    public static final int DEFAULT_FRONT_PIC_Q_V40 = 2;
    public static final int DEFAULT_BACK_REC_Q = 0;
    public static final int DEFAULT_BACK_PIC_Q = 0;
    public static final int DEFAULT_REC_D = 0;
	//add by chengyuzhou
	public static final boolean DEFAULT_MIDDLE_STATE=true;
	//end
    public static final boolean DEFAULT_REAR_FLIP = false;
    public static boolean DEFAULT_REAR_FLIP_V40 = false;
    public static boolean DEFAULT_RIGHT_FLIP_V40 = SystemProperties.getBoolean("ro.sys.right.flip",false);
    public static final boolean DEFAULT_CL_A = true;
    public static int DEFAULT_CRASH_SEN = 1;
    public static final int DEFAULT_LOCK_SEN = SystemProperties.getInt("ro.sys.float_gravity", 2);
    public static final int DEFAULT_P_CRASH_SEN = 1;
    public static final int DEFAULT_CAR_TYPE = SystemProperties.getInt("ro.sys.default_car_type", 0);
    private static final boolean REAR_FLIP = SystemProperties.getBoolean("ro.sys.rear.flip", false);
    private static final boolean LEFT_FLIP = SystemProperties.getBoolean("ro.sys.left.flip", false);
    private static final boolean RIGHT_FLIP = SystemProperties.getBoolean("ro.sys.right.flip", false);
    public static final boolean DEFAULT_MUTE = false;
    public static final boolean DEFAULT_MUTE_V40 = false;
    public static final int DEFAULT_FULL_TRANY = Integer.MAX_VALUE;
    public static final boolean IS_BOOT_UP = true;
    public static final int RES_REC_QUA = 0;
    public static final int RES_PIC_QUA = 1;
    public static final int RES_REC_TIME = 2;
    public static final int RES_DIS_LEV = 3;
    public static final int RES_LCK_LEV = 4;
    public static final int RES_PRK_LEV = 5;
    public static final int RES_CAR_TYPE = 6;
    //add by chengyzuhou
    public static final int RES_REVERSE_LINES=7;
    
    public static final int IC_DEFAULT = 0;
    public static final int IC_V66 = 1;
    public static final int IC_V40 = 2;
    public static final int MAX_LINES = 100;
    
    public static final boolean isAdasOpen = SystemProperties.getBoolean("ro.sys.control_adas_open", true); 
    public static final boolean isAdasFunctionOpen = SystemProperties.getBoolean("ro.sys.adas_function_open", true); 
    
    private static final int[] REC_QUA_ARRY = { R.string.video_quality_720p,
            R.string.video_quality_1080p, R.string.video_quality_1296p};
    private static final int[] PIC_QUA_ARRY = { R.string.picture_quality_2m,
            R.string.picture_quality_4m , R.string.picture_quality_6m};
    private static final int[] REC_TIME = { R.string.video_time_1min, R.string.video_time_2min,
            R.string.video_time_3min };
    private static final int[] DIS_LEV = { R.string.distance_leve_1, R.string.distance_leve_2,
            R.string.distance_leve_3 };
    private static final int[] LCK_LEV = { R.string.gsensor_level_1, R.string.gsensor_level_2,
            R.string.gsensor_level_3,R.string.gsensor_level_4 };
    private static final int[] PRK_LEV = { R.string.alarm_level_1, R.string.alarm_level_2,
            R.string.alarm_level_3 };
    private static final int[] CAR_TYPE = { R.string.car_type_car, R.string.car_type_suv,
        R.string.car_type_truck };
    private static final int[] EMPTY = { R.string.dialog_empty };
    
    private static final int[] DIS_LEV_V40 = { R.string.distance_leve_1_40, R.string.distance_leve_2,
        R.string.distance_leve_3 };
    private static final int[] REC_QUA_ARRY_V40 = { R.string.video_quality_720p,
        R.string.video_quality_1080p};
    private static final int[] PIC_QUA_ARRY_V40 = { R.string.picture_quality_2m,
        R.string.picture_quality_4m,R.string.picture_quality_6m};
    //add by chengyuzhou
    private static final int[] RES_REVERSE_ARRY = { R.string.reverse_lines_off,R.string.reverse_lines_on};
    private static final int[] REAR_FL = { R.string.car_type_car, R.string.car_type_suv,
            R.string.car_type_truck };
    public static final String KEY_LEFT_CAMERA_FLIP = "left_flip";
    public static final String KEY_RIGHT_CAMERA_FLIP = "right_flip";
    public static final String KEY_BACK_CAMERA_FLIP = "back_flip";
    private static MyPreference mMyPreference;
    
    public SharedPreferences shp = null;
    
    private IAdasFlagChanged mAdasFlagChanged;
    private IRecQualityChanged mRecQualityChanged;
    private IPicQualityChanged mPicQualityChanged;
    private IRecDurationChanged mRecDurationChanged;
    private IRearFlipChanged mRearFlipChanged;
    private IRightFlipChanged mRightFlipChanged;
	//add by chengyuzhou 
	private IMiddleVideoChanged mMiddleVideoChanged,mSettingStateChanged;
	//end 
    private ICarLaneAdjustChanged mCarLaneAdjustChanged;
    private ILockSensityChanged mLockSensityChanged;
    private ICrashSensityChanged mCrashSensityChanged;
    private IParkingCrashSensityChanged mParkingCrashSensityChanged;
    private ICarTypeChanged mCarTypeChanged;
    private IMuteChanged mMuteChanged;
    
    private static int mIcType = IC_DEFAULT;
    private static boolean mIsSplit = false;
    private static boolean mIsCheckedSplit = false;
    
    public MyPreference(Context context) {
		if (context != null) {
			shp = context.getSharedPreferences("com.android.camera2_preferences",
					Context.MODE_MULTI_PROCESS | Context.MODE_WORLD_READABLE);
		}
    }
    
    public static MyPreference getInstance(Context context) {
        if (mMyPreference == null) {
            mMyPreference = new MyPreference(context);
        }
        return mMyPreference;
    }
    
    public int[] getResources(int index) {
        switch (index) {
            case RES_REC_QUA:
            	if (getChipType() == IC_V40) {
            		return REC_QUA_ARRY_V40;
            	} else {
                    return REC_QUA_ARRY;
            	}
            case RES_PIC_QUA:
            	if (getChipType() == IC_V40) {
            		return PIC_QUA_ARRY_V40;
            	} else {
                    return PIC_QUA_ARRY;
            	}
            case RES_REC_TIME:
                return REC_TIME;
            case RES_DIS_LEV:
            	if (getChipType() == IC_V40) {
            		return DIS_LEV_V40;
            	} else {
                    return DIS_LEV;
            	}
            case RES_LCK_LEV:
                return LCK_LEV;
            case RES_PRK_LEV:
                return PRK_LEV;
            case RES_CAR_TYPE:
                return CAR_TYPE;
            //add by chengyuzhou
            case RES_REVERSE_LINES:
            	return RES_REVERSE_ARRY;
            default:
                break;
        }
        return EMPTY;
    }
    
    public boolean getAdasFlag() {
    	if (isAdasOpen) {
    		boolean default_adas_v40 = false;
    		if (SystemProperties.getInt("ro.sys.float_camera", 1) == 1){
    			default_adas_v40 = isAdasFunctionOpen;
    		}
    		if(RecordService.isBootCloseAdas && RecordService.isFirstBoot ){
    			Log.d(TAG,"Before boot, RecordService.isFirstBoot:->" + RecordService.isFirstBoot);
    			saveAdasFlag(false);
    			RecordService.setIsFirstBoot(false);
    			Log.d(TAG,"After boot, RecordService.isFirstBoot:->" + RecordService.isFirstBoot);
    		}
            if (getChipType() == IC_V40) {
                if (shp != null) {
    				return shp.getBoolean("key_intelligent_detect", default_adas_v40);
                }
                return default_adas_v40;
            } else {
                if (shp != null) {
                    return shp.getBoolean("key_intelligent_detect", DEFAULT_DETECT);
                }
                return DEFAULT_DETECT;
            }
		}else{
			return DEFAULT_DETECT;
		}
		
    }
    public void saveCameraFlipStatus(String key,boolean value){
    	if (shp != null) {
            Editor editor = shp.edit();
            editor.putBoolean(key, value);
            editor.commit();
        }
    	if (mRearFlipChanged != null) {
    		if (KEY_RIGHT_CAMERA_FLIP.equals(key) ) {
				mRearFlipChanged.onCameraFlipChanged(TwoCameraPreviewWin.secondSurfaceShowId, value);
			}else if(KEY_BACK_CAMERA_FLIP.equals(key)){
    		    if (CustomValue.ONLY_ONE_CAMERA){
                    mRearFlipChanged.onCameraFlipChanged(CameraInfo.CAMERA_FACING_FRONT, value);
                }else{
                    mRearFlipChanged.onCameraFlipChanged(CameraInfo.CAMERA_FACING_BACK, value);
                }
			}
        }
    }
    
    public boolean getCameraFlipStatus(String key){
    	boolean defaultValue = false;
        if (KEY_RIGHT_CAMERA_FLIP.equals(key)) {
			defaultValue = RIGHT_FLIP;
		}else if(KEY_BACK_CAMERA_FLIP.equals(key)){
			defaultValue = REAR_FLIP;
		}
    	return shp.getBoolean(key, defaultValue);
    }
	

    
    public boolean saveAdasFlag(boolean isOpen) {
        if (shp != null) {
            Editor editor = shp.edit();
            editor.putBoolean("key_intelligent_detect", isOpen);
            editor.commit();
        }
        if (mAdasFlagChanged != null) {
            mAdasFlagChanged.onAdasFlagChanged(isOpen);
        }
        return false;
    }
	
	public int getReverseMode() {
    	return shp.getInt("key_reverse_mode", ReverseLineImageView.Mode.NORMAL);
    }
	
	public void saveReverseMode(int mode) {
        if (shp != null) {
            Editor editor = shp.edit();
            editor.putInt("key_reverse_mode", mode);
            editor.commit();
        }
    }
	
	
    
    public interface IAdasFlagChanged {
        void onAdasFlagChanged(boolean isOpen);
    }
    
    public void setAdasFlagChangedListener(IAdasFlagChanged listener) {
        mAdasFlagChanged = listener;
    }
    
    public int getRecQuality(int cameraId) {
        if (cameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            if (shp != null) {
                return shp.getInt("key_front_rec_quality", DEFAULT_FRONT_REC_Q);
            }
        } else {
            if (shp != null) {
                return shp.getInt("key_back_rec_quality", DEFAULT_BACK_REC_Q);
            }
        }
        return DEFAULT_FRONT_REC_Q;
    }
    
    public int getRecResId(int cameraId) {
        int value = getRecQuality(cameraId);
        if (value < 0 && value >= REC_QUA_ARRY.length) {
            return REC_QUA_ARRY[DEFAULT_FRONT_REC_Q];
        } else {
            return REC_QUA_ARRY[value];
        }
    }
    
    public boolean saveRecQuality(int cameraId, int value) {
        if (value < 0 && value >= REC_QUA_ARRY.length) {
            return false;
        }
        if (shp != null) {
            Editor editor = shp.edit();
            if (cameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                editor.putInt("key_front_rec_quality", value);
            } else {
                editor.putInt("key_back_rec_quality", value);
            }
            editor.apply();
        }
        if (cameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            if (mRecQualityChanged != null) {
                mRecQualityChanged.onRecQualityChanged(value);
            }
        }
        return true;
    }
    
    public interface IRecQualityChanged {
        void onRecQualityChanged(int value);
    }
    
    public void setRecQualityChangedListener(IRecQualityChanged listener) {
        mRecQualityChanged = listener;
    }
    
    public int getPicQualtiy(int cameraId) {
        if (cameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            if (shp != null) {
                if (getChipType() == IC_V40) {
                    return shp.getInt("key_front_pic_quality", DEFAULT_FRONT_PIC_Q_V40);
                } else {
                    return shp.getInt("key_front_pic_quality", DEFAULT_FRONT_PIC_Q);
                }
            }
        } else {
            if (shp != null) {
                return shp.getInt("key_back_pic_quality", DEFAULT_BACK_REC_Q);
            }
        }
        if (getChipType() == IC_V40) {
            return DEFAULT_FRONT_PIC_Q_V40;
        } else {
            return DEFAULT_FRONT_PIC_Q;
        }
    }
    
    //add by chengyuzhou
    public int getReverseLine(){
    	return RES_REVERSE_ARRY[getReverseLineResId()];
    }
    
    public int getReverseLineResId(){
    	return shp.getInt("key_reverse_lines", 1);
    }
    
    public void saveReverseLine(int val){
    	 Editor editor = shp.edit();
    	 editor.putInt("key_reverse_lines", val).commit();
    }
    
    public int getPicResId(int cameraId) {
        int value = getPicQualtiy(cameraId);
        if (value < 0 && value >= PIC_QUA_ARRY.length) {
            if (getChipType() == IC_V40) {
                return PIC_QUA_ARRY[DEFAULT_FRONT_PIC_Q_V40];
            } else {
                return PIC_QUA_ARRY[DEFAULT_FRONT_PIC_Q];
            }
        } else {
            return PIC_QUA_ARRY[value];
        }
    }
    
    public boolean savePicQuality(int cameraId, int value) {
        Log.d(TAG, "savePicQuality ls=" + mPicQualityChanged);
        if (value < 0 && value >= PIC_QUA_ARRY.length) {
            return false;
        }
        if (shp != null) {
            Editor editor = shp.edit();
            if (cameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                editor.putInt("key_front_pic_quality", value);
            } else {
                editor.putInt("key_back_pic_quality", value);
            }
            editor.commit();
        }
        if (mPicQualityChanged != null) {
            mPicQualityChanged.onPicQualityChanged(value);
        }
        return true;
    }
    
    public interface IPicQualityChanged {
        void onPicQualityChanged(int value);
    }
    
    public void setPicQualityChangedListener(IPicQualityChanged listener) {
        mPicQualityChanged = listener;
    }
    
    public int getRecDuration() {
        if (shp != null) {
            return shp.getInt("key_rec_duration", DEFAULT_REC_D);
        }
        return DEFAULT_REC_D;
    }
    
    public int getRecDurationResId() {
        int value = getRecDuration();
        if (value < 0 && value >= REC_TIME.length) {
            return REC_TIME[DEFAULT_REC_D];
        } else {
            return REC_TIME[value];
        }
    }
    
    public boolean saveRecDuration(int value) {
        if (value < 0 && value >= REC_TIME.length) {
            return false;
        }
        if (shp != null) {
            Editor editor = shp.edit();
            editor.putInt("key_rec_duration", value);
            editor.commit();
        }
        if (mRecDurationChanged != null) {
            mRecDurationChanged.onRecDurationChanged(value);
        }
        return true;
    }
    
    public interface IRecDurationChanged {
        void onRecDurationChanged(int value);
    }
    
    public void setRecDurationChangedListener(IRecDurationChanged listener) {
        mRecDurationChanged = listener;
    }
    //add by chengyuzhou 
	public boolean saveMiddleVideoState(boolean value) {
        if (shp != null) {
            Editor editor = shp.edit();
            editor.putBoolean("key_middle_state", value);
            editor.commit();
        }
        if (mMiddleVideoChanged != null) {
            mMiddleVideoChanged.onMiddleVideoChanged(value);
        }
		if (mSettingStateChanged != null) {
		mSettingStateChanged.onMiddleVideoChanged(value);
		}
        return true;
    }
	
	public void setMiddleVideoChangerListener(IMiddleVideoChanged listener){
		mMiddleVideoChanged=listener;
	}
	public void setSettingStateChangerListener(IMiddleVideoChanged listener){
		mSettingStateChanged=listener;
	}
	public interface IMiddleVideoChanged{
		void onMiddleVideoChanged(boolean state);
	}
	public boolean getMiddleVideoState(){
		if (shp != null) {
             return shp.getBoolean("key_middle_state", DEFAULT_MIDDLE_STATE);
           }
		return	DEFAULT_MIDDLE_STATE;
	}
	//end 
    public boolean getRearVisionFlip() {
        if (getChipType() == IC_V40) {
			DEFAULT_REAR_FLIP_V40 = SystemProperties.getBoolean("ro.sys.rear.flip", false);
            if (shp != null) {
                return shp.getBoolean("key_rear_vision_flip", DEFAULT_REAR_FLIP_V40);
            }
            return DEFAULT_REAR_FLIP_V40;
        } else {
            if (shp != null) {
                return shp.getBoolean("key_rear_vision_flip", DEFAULT_REAR_FLIP);
            }
            return DEFAULT_REAR_FLIP;
        }
    }

	public boolean getRightVisionFlip() {
		if (shp != null) {
			return shp.getBoolean("key_right_vision_flip", DEFAULT_RIGHT_FLIP_V40);
		} else {
			return DEFAULT_RIGHT_FLIP_V40;
		}
	}

    public boolean saveRearVisionFlip(boolean value) {
        if (shp != null) {
            Editor editor = shp.edit();
            editor.putBoolean("key_rear_vision_flip", value);
            editor.commit();
        }
        if (mRearFlipChanged != null) {
            mRearFlipChanged.onRearFlipChanged(value);
        }
        return true;
    }
	
	public void saveScrollPreviewY(String key,int scrollY){
    	Log.i(TAG, "saveScrollPreviewY() key = "+key+",scrollY = "+scrollY);
    	if (null != shp) {
            shp.edit().putInt(key, scrollY).commit();
        }
    }
    
    public int getScrollPreviewY(String key){
    	if (null != shp) {
    		return shp.getInt(key, 0);
		}
    	return 0;
    }
    
    public boolean saveRightVisionFlip(boolean value) {
    	if (shp != null) {
            Editor editor = shp.edit();
            editor.putBoolean("key_right_vision_flip", value);
            editor.commit();
        }
        if (mRightFlipChanged != null) {
        	mRightFlipChanged.onRightFlipChanged(value);
        }
        return true;
    }
    
    public interface IRightFlipChanged {
        void onRightFlipChanged(boolean isFlip);
        
    }
    public void setRearFlipChangedListener(IRightFlipChanged listener) {
        mRightFlipChanged = listener;
    }
    
    public interface IRearFlipChanged {
        void onRearFlipChanged(boolean isFlip);
        
        void onCameraFlipChanged(int cameraId,boolean isFlip);
    }
    
    public void setRearFlipChangedListener(IRearFlipChanged listener) {
        mRearFlipChanged = listener;
    }
    
    public boolean getCarLaneAdjust() {
        if (shp != null) {
            return shp.getBoolean("key_carlane_adjust", DEFAULT_CL_A);
        }
        return DEFAULT_CL_A;
    }
    
    public boolean saveCarLaneAdjust(boolean value) {
        if (shp != null) {
            Editor editor = shp.edit();
            editor.putBoolean("key_carlane_adjust", value);
            editor.commit();
        }
        if (mCarLaneAdjustChanged != null) {
            mCarLaneAdjustChanged.onCarLaneAdjustChanged(value);
        }
        return true;
    }
    
    public interface ICarLaneAdjustChanged {
        void onCarLaneAdjustChanged(boolean value);
    }
    
    public void setCarLaneAdjustChangedListener(ICarLaneAdjustChanged listener) {
        mCarLaneAdjustChanged = listener;
    }
    
    public int getCrashSensity() {
		DEFAULT_CRASH_SEN = SystemProperties.getInt("ro.sys.crash.sen", 1);
        if (shp != null) {
            return shp.getInt("key_crash_sensity", DEFAULT_CRASH_SEN);
        }
        return DEFAULT_CRASH_SEN;
    }
    
    public int getCrashSensityResId() {
        int value = getCrashSensity();
        if (getChipType() == IC_V40) {
            if (value < 0 && value >= DIS_LEV_V40.length) {
                return DIS_LEV_V40[DEFAULT_CRASH_SEN];
            } else {
                return DIS_LEV_V40[value];
            }
        } else {
            if (value < 0 && value >= DIS_LEV.length) {
                return DIS_LEV[DEFAULT_CRASH_SEN];
            } else {
                return DIS_LEV[value];
            }
        }
    }
    
    public interface ICrashSensityChanged {
        void onCrashSensityChanged(int value);
    }
    
    public void setCrashSensityChangedListener(ICrashSensityChanged listener) {
        mCrashSensityChanged = listener;
    }
    
    public boolean saveCrashSensity(int value) {
    	if (getChipType() == IC_V40) {
            if (value < 0 && value >= DIS_LEV_V40.length) {
                return false;
            }
            if (shp != null) {
                Editor editor = shp.edit();
                editor.putInt("key_crash_sensity", value);
                editor.commit();
            }
            if (mCrashSensityChanged != null) {
                mCrashSensityChanged.onCrashSensityChanged(value);
            }
            return true;
    	} else {
            if (value < 0 && value >= DIS_LEV.length) {
                return false;
            }
            if (shp != null) {
                Editor editor = shp.edit();
                editor.putInt("key_crash_sensity", value);
                editor.commit();
            }
            if (mCrashSensityChanged != null) {
                mCrashSensityChanged.onCrashSensityChanged(value);
            }
            return true;
    	}
    }
    
    public int getLockSensity() {
        if (shp != null) {
            return shp.getInt("key_1lock_sensity", DEFAULT_LOCK_SEN);
        }
        return DEFAULT_LOCK_SEN;
    }
    
    public int getLockSensityResId() {
        int value = getLockSensity();
        if (value < 0 && value >= LCK_LEV.length) {
            return LCK_LEV[DEFAULT_LOCK_SEN];
        } else {
            return LCK_LEV[value];
        }
    }
    
    public boolean saveLockSensity(int value) {
        if (value < 0 && value >= LCK_LEV.length) {
            return false;
        }
        if (shp != null) {
            Editor editor = shp.edit();
            editor.putInt("key_1lock_sensity", value);
            editor.commit();
        }
        if (mLockSensityChanged != null) {
            mLockSensityChanged.onLockSensityChanged(value);
        }
        return true;
    }
    
    public interface ILockSensityChanged {
        void onLockSensityChanged(int value);
    }
    
    public void setLockSensityChangedListener(ILockSensityChanged listener) {
        mLockSensityChanged = listener;
    }
    
    public int getParkingCrashSensity() {
        if (shp != null) {
            return shp.getInt("key_parking_crash_sensity", DEFAULT_P_CRASH_SEN);
        }
        return DEFAULT_P_CRASH_SEN;
    }
    
    public int getParkingCrashSensityResId() {
        int value = getParkingCrashSensity();
        if (value < 0 && value >= PRK_LEV.length) {
            return PRK_LEV[DEFAULT_P_CRASH_SEN];
        } else {
            return PRK_LEV[value];
        }
    }
    
    public boolean saveParkingCrashSensity(int value) {
        if (value < 0 && value >= PRK_LEV.length) {
            return false;
        }
        if (shp != null) {
            Editor editor = shp.edit();
            editor.putInt("key_parking_crash_sensity", value);
            editor.commit();
        }
        if (mParkingCrashSensityChanged != null) {
            mParkingCrashSensityChanged.onParkingCrashSensityChanged(value);
        }
        return true;
    }
    
    public interface IParkingCrashSensityChanged {
        void onParkingCrashSensityChanged(int value);
    }
    
    public void setParkingCrashSensityChangedListener(IParkingCrashSensityChanged listener) {
        mParkingCrashSensityChanged = listener;
    }
    
    public int getCarType() {
        if (shp != null) {
            return shp.getInt("key_car_type", DEFAULT_CAR_TYPE);
        }
        return DEFAULT_CAR_TYPE;
    }
    
    public int getCarTypeResId() {
        int value = getCarType();
        if (value < 0 && value >= CAR_TYPE.length) {
            return CAR_TYPE[DEFAULT_CAR_TYPE];
        } else {
            return CAR_TYPE[value];
        }
    }
    
    public boolean saveCarType(int value) {
        if (value < 0 && value >= CAR_TYPE.length) {
            return false;
        }
        if (shp != null) {
            Editor editor = shp.edit();
            editor.putInt("key_car_type", value);
            editor.commit();
        }
        if (mCarTypeChanged != null) {
        	mCarTypeChanged.onCarTypeChanged(value);
        }
        return true;
    }
    
    public interface ICarTypeChanged {
        void onCarTypeChanged(int value);
    }
    
    public void setCarTypeChangedListener(ICarTypeChanged listener) {
    	mCarTypeChanged = listener;
    }
    
    public boolean isMute() {
        if (getChipType() == IC_V40) {
            if (shp != null) {
                return shp.getBoolean("key_is_mute", DEFAULT_MUTE_V40);
            }
            return DEFAULT_MUTE_V40;
        } else {
            if (shp != null) {
                return shp.getBoolean("key_is_mute", DEFAULT_MUTE);
            }
            return DEFAULT_MUTE;
        }
    }
    
    public boolean saveMute(boolean isMute) {
        if (shp != null) {
            Editor editor = shp.edit();
            editor.putBoolean("key_is_mute", isMute);
            editor.commit();
        }
        if (mMuteChanged != null) {
            mMuteChanged.onMuteChanged(isMute);
        }
        return false;
    }
    
    public interface IMuteChanged {
        void onMuteChanged(boolean isMute);
    }
    
    public void setMuteChangedListener(IMuteChanged listener) {
        mMuteChanged = listener;
    }
    
    public int getFullTranslationY(int cameraId) {
        if (shp != null) {
            if (cameraId == CameraInfo.CAMERA_FACING_FRONT) {
                return shp.getInt("key_full_trany_front", DEFAULT_FULL_TRANY);
            } else {
                return shp.getInt("key_full_trany_back", DEFAULT_FULL_TRANY);
            }
        }
        return DEFAULT_FULL_TRANY;
    }
    
    public boolean saveFullTranslationY(int cameraId, int value) {
        if (shp != null) {
            Editor editor = shp.edit();
            if (cameraId == CameraInfo.CAMERA_FACING_FRONT) {
                editor.putInt("key_full_trany_front", value);
            } else {
                editor.putInt("key_full_trany_back", value);
            }
            editor.commit();
        }
        return true;
    }
    
    public static int getChipType() {
    	if (mIcType != IC_DEFAULT) {
    		return mIcType;
    	} else {
        	String platform = null;
        	String chipId = null;
        	try {
    			BufferedReader br = new BufferedReader(new FileReader(IC_INFO_FILE));
    			String data = br.readLine();
    			int count = 0;
    			while (data != null) {
    			    Log.d(TAG,"get online = " + data);
    				if (data.contains(CHIP_ID_NODE)) {
    					chipId = data.substring(data.indexOf(":") + 2);
    					if (chipId != null) {
        					chipId.replaceAll(" ", "");
        					chipId.trim();
        					Log.d(TAG,"get chipid = " + chipId);
    					}
    			    } else if (data.contains(PLATFORM_NODE)) {
    					platform = data.substring(data.indexOf(":") + 2);
    					if (platform != null) {
        					platform.replaceAll(" ", "");
        					platform.trim();
        					Log.d(TAG,"get platform = " + platform);
    					}
    			    }
    	            data = br.readLine(); 
    			    count++;
    			    if(count > MAX_LINES){
    			    	break;
    			    }
    			}
            } catch (FileNotFoundException ex) {
            	ex.printStackTrace();
            } catch (IOException ex) {
            	ex.printStackTrace();
            }
        	
        	mIcType = IC_V66;// default
        	if (chipId != null && platform != null) {
        		if (platform.equalsIgnoreCase(CHIP_ID_V66)) {
        			mIcType = IC_V66;
        		} else if (platform.equalsIgnoreCase(CHIP_ID_V40)) {
        			mIcType = IC_V40;
        		}
        	}
        	return mIcType;
    	}
    }
    
    public static boolean isSplitMode () {
    	if (mIsCheckedSplit) {
    		return mIsSplit;
    	} else {
    		mIsSplit = SystemProperties.getBoolean("persist.sys.splitscreen", true);
    		mIsCheckedSplit = true;
    		return mIsSplit;
    	}
    }
    
    public static boolean isBootUp () {
    	return IS_BOOT_UP;
    }
}
