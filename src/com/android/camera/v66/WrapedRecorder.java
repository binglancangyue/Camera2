package com.android.camera.v66;

import com.softwinner.recorder.AWRecorder;
import com.softwinner.recorder.AWRecorder.MicStatu;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.AdasDetectionListener;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.hardware.Camera.WaterMarkDispMode;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.location.Location;
import android.media.CamcorderProfile;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.MediaStore.MediaColumns;
import android.provider.MediaStore.Video;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.widget.Toast;

import com.android.camera.CameraManager;
import com.android.camera.CameraManager.CameraOpenErrorCallback;
import com.android.camera.CameraManager.CameraPictureCallback;
import com.android.camera.CameraManager.CameraProxy;
import com.android.camera.Exif;
import com.android.camera.LocationManager;
import com.android.camera.MediaSaveService;
import com.android.camera.Storage;
import com.android.camera.exif.ExifInterface;
import com.android.camera.util.CameraUtil;
import com.android.camera.v66.MyPreference.ICrashSensityChanged;
import com.android.camera.v66.MyPreference.ILockSensityChanged;
import com.android.camera.v66.MyPreference.IRecQualityChanged;
import com.android.camera2.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Queue;

//add by chengyuzhou
import android.content.SharedPreferences;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.pm.PackageManager;
import android.app.ActivityManager.RecentTaskInfo;
import android.content.pm.ResolveInfo;
import android.content.Intent;

//end
public class WrapedRecorder implements SensorEventListener, AWRecorder.OnErrorListener,
        AWRecorder.OnInfoListener {
    public static final String TAG = "WrapedRecorder";
    public static final int STATE_IDLE = 0;
    public static final int STATE_STARTING = 1;
    public static final int STATE_STARTED = 2;
    public static final int STATE_STOPPING = 3;
    public static final int STATE_STOPPED = 4;
    public static final int STATE_SWITCHING_FILE = 5;

    public static final int MSG_OPEN_CAMERA_CB = 100;
    public static final int MSG_START_RECORD_CB = 101;
    public static final int MSG_STOP_RECORD_CB = 102;
    public static final int MSG_GSENSOR_LOCK_UP = 103;
    public static final int MSG_START_PREVIEW = 104;
    public static final int MSG_FILE_SWITCHED = 105;
    public static final int MSG_RELEASE_RECORD_CB = 106;
    public static final int MSG_UNLOCKED_CB = 107;
    public static final int MSG_RECORD_TICKY = 108;
    public static final int MSG_START_PREVIEW_CB = 109;
    public static final int MSG_OPEN_CAMERA = 1000;
    public static final int MSG_START_RECORD = 1001;
    public static final int MSG_STOP_RECORD = 1002;
    public static final int MSG_RELEASE_RECORD = 1003;
    public static final int MSG_SET_MUTE = 1004;
    public static final int MSG_SWITCH_NEXT_FILE = 1005;
    public static final int MSG_STOP_PREVIEW = 1006;
    public static final int MSG_START_RENDER = 1007;
    public static final int MSG_STOP_RENDER = 1008;
    public static final int MSG_RELEASE = 1009;
    public static final int MSG_SET_PREVIEW_TEXTURE = 1010;
    public static final int MSG_SET_SURFACE_HOLDER = 1011;
    public static final int MSG_SET_WATER_MARK = 1012;
    public static final int MSG_STOP_WATER_MARK = 1013;
    public static final int MSG_START_WATER_MARK = 1014;
    public static final int MSG_CLOSE_CAMERA = 1015;
    public static final int MSG_WRITE_KML = 1016;
    public static final int MSG_WRITE_KML_END = 1017;
    public static final int MSG_WRITE_KML_SWITCH = 1018;
    public static final int MSG_TAKE_SNAPSHOT = 1019;
    public static final int MSG_START_ADAS = 1020;
    public static final int MSG_STOP_ADAS = 1021;
    public static final int MSG_SET_ADAS_LISENER = 1022;
    public static final int MSG_SET_ADAS_SPEED = 1023;
    public static final int MSG_SET_ADAS_CAR_TYPE = 1024;
    public static final int MSG_SET_ADAS_SENSITY = 1025;
    public static final int MSG_SET_RECQUALITY = 1026;
    public static final int MSG_SET_ADAS_LEVEL = 1027;
    public static final int MSG_SET_FLIP_STATUS = 1028;

    public static final int LOCK_FLAG_NONE = 0;
    public static final int LOCK_FLAG_1MIN = 1;
    public static final int LOCK_FLAG_2MIN = 2;
    public static final int LOCK_FLAG_PRE_1MIN = 3;

    public static final String IMPACT_SUFFIX = "_impact";
    public static final String SUBFIX_1_MIN = "_1min";
    public static final String SUBFIX_2_MIN = "_3min";
    public static final String SUBFIX_3_MIN = "_5min";

    private static final String ADAS_CAR_HEIGHT = "adas-car-height";
    private static final String ADAS_CPU_LEVEL = "adas-cpu-level";

    public static final int MINI_MODE_NONE = -1;
    public static final int MINI_MODE_WAITING_STOP = 2000;
    public static final int MINI_MODE_WAITING_TAKEN = 2001;
    public static final int MINI_MODE_WAITING_PHOTO = 2002;

    private static final int SEND_MSG_DELAY = 2000;
    private static final int TRY_MAX_TIME = 5;
    private static final int TICKY_DELAY = 1000;

    public static final int LOCK_SENSITY_HIGH = 28;// 20;
    public static final int LOCK_SENSITY_MIDIUM = 38;// 30;
    public static final int LOCK_SENSITY_LOW = 55;// 47;
    public static final int LOCK_SENSITY_XLOW = 68;
    private int mCameraId = -1;
    private Camera mCamera;
    private AWRecorder mRecorder;
    private CamcorderProfile mProfile;
    private RecorderHandler mRecorderHandler;
    private int mState = STATE_IDLE;

    private int mVideoQuality;
    private int mDistanceDetectLevel;
    private Camera.Parameters mParameters;
    private int mGsensorLockLevel;
    private int mImageQuality;
    // The video duration limit. 0 menas no limit.
    private int mMaxVideoDurationInMs;
    private int mVideoDuration;
    private boolean mIsMute;
    private boolean mAdasDetect = false;
    private int mDesiredPreviewWidth;
    private int mDesiredPreviewHeight;
    private int mDesiredPreviewWidth_src;
    private int mDesiredPreviewHeight_src;
    private int mMotionDetect;

    private int mLockFlag = LOCK_FLAG_NONE;
    private boolean mForceLock = false;
    private RecordService mRecordService;
    private ContentValues mPreVideoValues;
    private ContentValues mCurVideoValues;
    private ContentValues mNextVideoValues;
    private LocationManager mLocationManager;
    private boolean mSnapshotInProgress = false;
    private ContentResolver mContentResolver;
    private String mPreFile;
    private String mCurFile;
    private String mNextFile;
    private List<Size> mSupportedSizes;
    private boolean mAskedPreview = false;
    private boolean mAskedWaterMark = false;
    private boolean mIsPreview = false;
    private boolean mIsRender = false;
    private SurfaceTexture mTexture;
    private SurfaceHolder mSurfaceHolder;
    private IRecordCallback mRecordCallback;
    private IRecorderDestroyedListener mDestroyedListener;
    private int mTryTime = 0;
    private boolean mAskAdasDetection = false;
    private boolean mSuspending = false;
    private boolean mIsWaterMarkRuning = false;
    private boolean mAskRecorder = false;
    private long mStartRecordTime;

    private int mMiniMode = MINI_MODE_NONE;
    private int mMiniWidth;
    private int mMiniHeight;
    private IMiniPictureTakenListener mMiniPictureTakenListener;
    private IMiniVideoTakenListener mMiniVideoTakenListener;
    private int mMiniduaration;

    private KmlWriter mKmlWriter;

    private long mLastDate;
    private int mSameSecondCount;
    private boolean mIsFirstAdasUp = true;
    private AdasDetectionListener mAdasListener = null;
    private int mCarType;
    private boolean mIsSaveMediaDelay = false;
    private int mLevel;
    // add by chengyuzhou
    private boolean tape = true;
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;

    private boolean isCameraOpen = false;
    // end
    private static final boolean IS_BACK_RIGHT_ON = SystemProperties.getBoolean("ro.sys.back" +
            ".right", true);
    private static final int FORNT_CAMERA_KO = SystemProperties.getInt("ro.front.camera.ko", 2363);

    // private FileUtil mFileUtil = new FileUtil();
    public static final long PRESIZE_1296P_1MIN = 100 * 1024 * 1024;
    public static final long PRESIZE_1080P_1MIN = 80 * 1024 * 1024;
    public static final long PRESIZE_1440_1152_1MIN = 35 * 1024 * 1024; // 4*CVBS
    public static final long PRESIZE_720P_1MIN = 45 * 1024 * 1024;
    public static final long PRESIZE_576P_1MIN = 30 * 1024 * 1024;
    public static final long PRESIZE_480P_1MIN = 30 * 1024 * 1024;
    public static long mRecordSwitchSpace = 1300 * 1024 * 1024;// 600M

    DecimalFormat df = new DecimalFormat("#0.000000");
    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_OPEN_CAMERA_CB:
                    Log.e(TAG, "MSG_OPEN_CAMERA_CB msg.arg1=" + msg.arg1);
                    if (msg.arg1 == 1) {
                        /*
                         * Toast.makeText(mRecordService,
                         * R.string.open_camera_failed, Toast.LENGTH_LONG).show();
                         */
                        mTryTime++;
                        if (mTryTime < TRY_MAX_TIME) {
                            if (mRecorderHandler != null) {
                                Log.i(TAG, "MSG_OPEN_CAMERA_CB");
                                mRecorderHandler.sendEmptyMessageDelayed(MSG_OPEN_CAMERA,
                                        SEND_MSG_DELAY);
                            }
                        } else {
                            mTryTime = 0;
                            if (mRecordService != null && mRecordService.getFloatCameraId() == CameraInfo.CAMERA_FACING_BACK
                                    && mCameraId == CameraInfo.CAMERA_FACING_BACK) {
                                mRecordService.setCameraPlug(true);
                                mAskRecorder = false;
                                mState = STATE_IDLE;
                            }
                        }
                    } else {
                        Log.e(TAG, "MSG_OPEN_CAMERA_CB mCamera=" + mCamera);
                        if (mAskedPreview || mTexture != null || mSurfaceHolder != null) {
                            startPreview();
                            // no need delay?
                            // mHandler.sendEmptyMessageDelayed(MSG_START_PREVIEW,
                            // SEND_MSG_DELAY);
                            if (mTexture != null) {
                                setPreviewTexture(mTexture);
                            } else if (mSurfaceHolder != null) {
                                setPreviewDisplay(mSurfaceHolder);
                            }
                        }
                        if (mRecordCallback != null) {
                            Log.e("HomeRecorderActivity", "onCameraOpen mCamera=" + mCamera);
                            mRecordCallback.onCameraOpen();
                        }
                        isCameraOpen = true;
                    }
                    break;
                case MSG_START_PREVIEW_CB:
                    Log.d(TAG, "MSG_START_PREVIEW_CB mAskAdasDetection=" + mAskAdasDetection);
                    if (mAskAdasDetection) {
                        setIntelligentDetect(true);
                        setAdasDetecttionListener(mAdasListener);
                    }
                    if (mAskedWaterMark) {
                        startWaterMark();
                    }
                    Log.d(TAG, "MSG_START_PREVIEW_CB mAskRecorder=" + mAskRecorder);
                    if (mAskRecorder) {
                        if (mRecorderHandler != null) {
                            mRecorderHandler.sendEmptyMessage(MSG_START_RECORD);
                        }
                        mState = STATE_STARTING;
                        mAskRecorder = false;
                    }
                    break;
                case MSG_START_RECORD_CB:
                    int sucess = 0;
                    int failed = -1;
                    boolean isStart = false;
                    if (msg.arg1 == sucess) {
                        isStart = true;
                        mState = STATE_STARTED;
                    } else if (msg.arg1 == failed) {
                        mState = STATE_IDLE;
                        isStart = false;
                    }
                    if (mRecordCallback != null) {
                        mRecordCallback.onRecordStarted(isStart);
                    }
                    break;
                case MSG_STOP_RECORD_CB:
                    Log.d(TAG, "MSG_STOP_RECORD_CB");
                    if (mRecordCallback != null) {
                        mRecordCallback.onRecordStoped();
                    }
                    if (mMiniMode == MINI_MODE_WAITING_STOP && mMiniVideoTakenListener != null) {
                        mMiniVideoTakenListener.onStopForMini(mCameraId);
                    } else if (mMiniMode == MINI_MODE_WAITING_TAKEN && mMiniVideoTakenListener != null) {
                        mMiniVideoTakenListener.onMiniVideoTaken(mCurVideoValues.getAsString(Video.Media.DATA), mCameraId);
                    }
                    break;
                case MSG_FILE_SWITCHED:
                    boolean isSwitched = false;
                    /*
                     * if (mRecordCallback != null) {
                     * mRecordCallback.onTimeUpdate(0); }
                     */
                    if (msg.arg1 == 0) {
                        isSwitched = true;
                        mState = STATE_STARTED;
                        mStartRecordTime = System.currentTimeMillis();
                        // mHandler.removeMessages(MSG_RECORD_TICKY);
                        // mHandler.sendEmptyMessageDelayed(MSG_RECORD_TICKY,
                        // TICKY_DELAY);
                    } else if (msg.arg1 == -1) {
                        mState = STATE_IDLE;
                        isSwitched = false;
                        if (mRecordCallback != null) {
                            mRecordCallback.onRecordStoped();
                        }
                        // mHandler.removeMessages(MSG_RECORD_TICKY);
                    }
                    break;
                case MSG_GSENSOR_LOCK_UP:
                    /*
                     * long duration = 0; if (mCurVideoValues != null) { duration =
                     * mCurVideoValues.getAsLong(Video.Media.DATE_TAKEN) -
                     * System.currentTimeMillis(); } if (duration < 10 * 1000 &&
                     * mPreVideoValues != null) { // current recorder take less than
                     * 10S,should save pre // and // cur recorder file ContentValues
                     * tmp = new ContentValues(mPreVideoValues);
                     * updatVideoName(mPreVideoValues,
                     * tmp.getAsString(Video.Media.DATA)); mLockFlag =
                     * LOCK_FLAG_2MIN; } else if (mCurVideoValues != null) {
                     * mLockFlag = LOCK_FLAG_1MIN; } else { mLockFlag =
                     * LOCK_FLAG_NONE; }
                     */
                    ActivityManager manager =
                            (ActivityManager) mRecordService.getSystemService(Context.ACTIVITY_SERVICE);
                    PackageManager pm = mRecordService.getPackageManager();
                    int leftStack = -1;
                    int rightStack = -1;
                    leftStack = SplitUtil.getLeftStackId(mRecordService);
                    rightStack = SplitUtil.getRightStackId(mRecordService);
                    RecentTaskInfo til = SplitUtil.getTopTaskOfStack(mRecordService, leftStack);
                    RecentTaskInfo tir = SplitUtil.getTopTaskOfStack(mRecordService, rightStack);
                    String namel = "test";
                    String namer = "test";
                    if (til != null) {
                        Intent itnl = til.baseIntent;
                        ResolveInfo resolveInfol = pm.resolveActivity(itnl, 0);
                        namel = itnl.getComponent().getPackageName();

                    }

                    if (tir != null) {
                        Intent itnr = tir.baseIntent;
                        ResolveInfo resolveInfor = pm.resolveActivity(itnr, 0);
                        namer = itnr.getComponent().getPackageName();

                    }
                    Log.v(TAG, "---------name-------" + namel);
                    if (!namel.equals("com.android.camera2") && !namer.equals("com.android" +
                            ".camera2")) {
                        Intent it = new Intent(mRecordService, RecorderActivity.class);
                        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        // it.putExtra("REQ_CAM_TYPE", 2);
                        it.addFlags(0x00000200);
                        // it.putExtra("IS_OPEN_AGAIN", true);
                        mRecordService.startActivity(it);
                        editor.putBoolean("isGsonLock", true).commit();
                    }

                    mLockFlag = LOCK_FLAG_1MIN;
                    // for(int i=0;i<100;i++){
                    // Log.v(TAG,"-----------FOR----------:"+i);
                    if (mRecordCallback != null && mCurVideoValues != null) {
                        Log.v(TAG, "-----------Go----------:");
                        mRecordCallback.onLocked(true);
                    }
                    // }
                    break;
                case MSG_RELEASE_RECORD_CB:
                    // standard method
                    if (mDestroyedListener != null) {
                        mDestroyedListener.onRecorderDestroyed(mCameraId);
                    }
                    break;
                case MSG_UNLOCKED_CB:
                    if (mRecordCallback != null) {
                        mRecordCallback.onLocked(false);
                    }
                    break;
                case MSG_RECORD_TICKY:
                    // Log.d(TAG, "MSG_RECORD_TICKY getRecDuration() =" +
                    // getRecDuration());
                    long cur = System.currentTimeMillis();
                    long dura = Math.abs(cur - mStartRecordTime);
                    if (dura > getRecDuration()) {
                        switchToNextFile();
                    } else {
                        // mHandler.removeMessages(MSG_RECORD_TICKY);
                        // mHandler.sendEmptyMessageDelayed(MSG_RECORD_TICKY,
                        // TICKY_DELAY);
                        if (mRecordCallback != null) {
                            mRecordCallback.onTimeUpdate(dura);
                        }
                    }
                    break;
                default:
                    break;
            }
            super.handleMessage(msg);
        }
    };

    public boolean getIsCameraOpen() {
        return isCameraOpen;
    }

    private final MediaSaveService.OnMediaSavedListener mOnVideoSavedListener =
            new MediaSaveService.OnMediaSavedListener() {
                @Override
                public void onMediaSaved(Uri uri) {
                    if (uri != null) {
                        Log.d(TAG, "onMediaSaved video");
                        if (mRecordService != null) {
                            mRecordService.notifyNewMedia(uri);
                        }
                    }
                }
            };

    private final MediaSaveService.OnMediaSavedListener mOnPhotoSavedListener =
            new MediaSaveService.OnMediaSavedListener() {
                @Override
                public void onMediaSaved(Uri uri) {
                    if (uri != null) {
                        Log.d(TAG, "onMediaSaved picutre");
                        if (mRecordService != null) {
                            mRecordService.notifyNewMedia(uri);
                        }
                    }
                }
            };

    private MediaSaveService mMediaSaveService;
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder bb) {
            mMediaSaveService = ((MediaSaveService.LocalBinder) bb).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            if (mMediaSaveService != null) {
                mMediaSaveService.setListener(null);
                mMediaSaveService = null;
            }
        }
    };

    private CameraOpenErrorCallback mCameraOpenErrorCallback = new CameraOpenErrorCallback() {
        @Override
        public void onCameraDisabled(int cameraId) {

        }

        @Override
        public void onDeviceOpenFailure(int cameraId) {

        }

        @Override
        public void onReconnectionFailure(CameraManager mgr) {

        }
    };

    public boolean jugeFileIsExit(String fileName) {
        File f = new File(fileName);
        if (f.exists()) {
            return true;
        } else {
            return false;
        }
    }

    public synchronized String getCanRepalceFiles() {
        String backOrFront = "_Front";
        String tempName = null;
        int count_front = 0;
        int count_back = 0;
        int count_third = 0;
        File file_test = new File("/mnt/extsd/DCIM/Camera");
        if (file_test.exists()) {
            List files = Arrays.asList(new File("/mnt/extsd/DCIM/Camera").listFiles());
            Collections.sort(files, new Comparator<File>() {
                @Override
                public int compare(File o1, File o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });

            for (int i = 0; i < files.size(); i++) {
                if (((File) files.get(i)).getName().contains("_Front")
                        && !((File) files.get(i)).getName().contains("impact")) {
                    count_front++;
                } else if (((File) files.get(i)).getName().contains("_Back")
                        && !((File) files.get(i)).getName().contains("impact")) {
                    count_back++;
                } else if (((File) files.get(i)).getName().contains("_Third")
                        && !((File) files.get(i)).getName().contains("impact")) {
                    count_third++;
                }
            }

            Log.d(TAG,
                    "count_front = " + count_front + " count_back = " + count_back + " " +
                            "count_third = " + count_third);

            if (mCameraId == CameraInfo.CAMERA_FACING_FRONT) {
                backOrFront = "_Front";
                if (!(count_front >= count_back && count_front >= count_third)) // 平衡各路的文件数量
                    return null;
            } else if (mCameraId == CameraInfo.CAMERA_FACING_BACK) {
                backOrFront = "_Back";
                if (!(count_back >= count_front && count_back >= count_third)) // 平衡各路的文件数量
                    return null;
            } else if (mCameraId == RecorderActivity.CAMERA_THIRD) {
                backOrFront = "_Third";
                if (!(count_third >= count_front && count_third >= count_back))// 平衡各路的文件数量
                    return null;
            }
            for (int i = 0; i < files.size(); i++) {
                if (((File) files.get(i)).getName().contains(backOrFront)
                        && !((File) files.get(i)).getName().contains("impact")) {
                    tempName = ((File) files.get(i)).getName();
                    break;
                }
            }
            if (tempName == null)
                return null;

            String shortCutSuffix = null;

            if (mVideoDuration == 0) {
                shortCutSuffix = SUBFIX_1_MIN;
            } else if (mVideoDuration == 1) {
                shortCutSuffix = SUBFIX_2_MIN;
            } else {
                shortCutSuffix = SUBFIX_3_MIN;
            }

            if (tempName.contains(shortCutSuffix))
                return "/mnt/extsd/DCIM/Camera/" + tempName;
            else
                return null;
        } else
            return null;
    }

    public long getFallocateSize() {
        long size = 0;

        if (mDesiredPreviewHeight <= 480) {

            size = PRESIZE_480P_1MIN;
        } else if (mDesiredPreviewHeight > 480 && mDesiredPreviewHeight <= 576) {
            size = PRESIZE_576P_1MIN;
        } else if (mDesiredPreviewHeight > 576 && mDesiredPreviewHeight <= 720) {
            size = PRESIZE_720P_1MIN;
        } else if (mDesiredPreviewHeight > 720 && mDesiredPreviewHeight <= 1080) {
            size = PRESIZE_1080P_1MIN;
        } else if (mDesiredPreviewHeight > 1080 && mDesiredPreviewHeight <= 1296) {
            size = PRESIZE_1296P_1MIN;
        } else {
            size = PRESIZE_1296P_1MIN;
        }
        Log.d(TAG, "mCameraId = " + mCameraId + "  getFallocateSize size=  " + size);

        if (mVideoDuration == 1) {
            size *= 3; // 3min
        } else if (mVideoDuration == 2) {
            size *= 5; // 5min
        }

        Log.d(TAG, "mCameraId = " + mCameraId + "  getFallocateSize real size=  " + size);
        return size;
    }

    public boolean checkNeedSwitchNow() {
        if (Storage.getAvailableSpace() < mRecordSwitchSpace) {
            Log.i(TAG, "Free storge not enough! Size byte:" + Storage.getAvailableSpace());
            return true;
        } else {
            Log.d(TAG, "Free storge enough! Size byte:" + Storage.getAvailableSpace());
            return false;
        }
    }

    public void exeAllocateOrTruncate(String videoPath) {
        Log.d(TAG, "mCameraId = " + mCameraId + "  videoPath=" + videoPath);

        int result = -1;
        if (!checkNeedSwitchNow()) {
            result = mRecorder.pre_qc_fallocate(videoPath, getFallocateSize());
            Log.d(TAG, "mCameraId = " + mCameraId + "  fallocate result =" + result);
        } else {
            // TODO rename 重写ftruncate
            // TODO getCanRepalceFiles
            String old = getCanRepalceFiles();
            Log.d(TAG, "mCameraId = " + mCameraId + "  old = " + old);
            if (old == null) {
                // 预分配
                result = mRecorder.pre_qc_fallocate(videoPath, getFallocateSize());
                Log.d(TAG, "mCameraId = " + mCameraId + "  fallocate pre result =" + result);
                return;
            }

            result = -1;
            if (new File(old).exists()) {
                Log.d(TAG, "mCameraId = " + mCameraId + "  Old File existes!");
                new File(old).renameTo(new File(videoPath));
                if (new File(videoPath).exists()) {
                    result = mRecorder.pre_qc_ftruncate(videoPath, 0);
                    Log.d(TAG, "mCameraId = " + mCameraId + "  new File existes ftruncate result " +
                            "=" + result);
                }
            }

            if (result < 0 || !(new File(videoPath).exists())) {
                result = mRecorder.pre_qc_fallocate(videoPath, getFallocateSize());
                Log.d(TAG, "mCameraId = " + mCameraId + "  ftruncate fail todo fallocate pre " +
                        "result =" + result);
            }
        }
    }

    private class RecorderHandler extends Handler {
        RecorderHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            Log.d(TAG, "in handler mCameraId=" + mCameraId + ";msg = " + msg.what);
            switch (msg.what) {
                case MSG_OPEN_CAMERA:
                    Log.d(TAG, "CameraInfo.CAMERA_FACING_BACK =" + CameraInfo.CAMERA_FACING_BACK);
                    Log.d(TAG, "CameraInfo.CAMERA_FACING_FRONT =" + CameraInfo.CAMERA_FACING_FRONT);
                    Log.e(TAG, "MSG_OPEN_CAMERA mCameraId =" + mCameraId);
                    Log.d(TAG, "mRecorderHandler =" + mRecorderHandler);
                    Log.d(TAG, "WrapedRecorder =" + WrapedRecorder.this);
                    Log.d(TAG,
                            "mHandler=" + mHandler + ";mCameraOpenErrorCallback=" + mCameraOpenErrorCallback);
                    int cameraIndex =  findCameraIndex(mCameraId);
                    Log.d(TAG, "-----------------------cameraIndex =" + cameraIndex);
                    if (cameraIndex >= 0) {
                        try {
                            if (mCamera == null) {
//							mCamera = Camera.open(cameraIndex);

//                                if (CustomValue.ONLY_ONE_CAMERA && cameraIndex != 0) {
//                                    mCamera = Camera.open(cameraIndex);
//                                }
//                                if (CustomValue.ONLY_ONE_CAMERA){
//                                  if (cameraIndex != 0) {
//                                      mCamera = Camera.open(cameraIndex);
//                                  }
//                                }else{
                                    mCamera = Camera.open(0);
//                                }
                                if (cameraIndex == RecorderActivity.CAMERA_THIRD) {
                                    Log.d(TAG, "onlyone");
                                    Parameters mParameters = mCamera.getParameters();
                                    mParameters.setZoom(3);
                                    mCamera.setParameters(mParameters);
                                }
                            }
                        } catch (RuntimeException ex) {
                            Log.d(TAG,
                                    "handleMessage RuntimeException = mCamera---=" + mCamera +
                                            "----cameraIndex=   "
                                            + cameraIndex);
                            ex.printStackTrace();
                        }
                    }
                    mParameters = null;
//				Log.e("HomeRecorderActivity", "MSG_OPEN_CAMERA mCamera=" + mCamera);
                    Message mes = Message.obtain();
                    mes.what = MSG_OPEN_CAMERA_CB;
                    if (mCamera != null) {
                        mes.arg1 = 0;
                        try {
                            loadParameters();
                        } catch (RuntimeException e) {
                            Log.e(TAG, "Fail to getParameters.");
                            e.printStackTrace();
                        }
                    } else {
                        mes.arg1 = 1;
                    }
                    mHandler.sendMessage(mes);
                    ;
                    break;
                case MSG_CLOSE_CAMERA:
                    try {
                        if (mCamera != null) {
                            mCamera.release();
                        }
                    } catch (RuntimeException ex) {
                        ex.printStackTrace();
                    } finally {
                        mCamera = null;
                    }
                    break;
                case MSG_START_RECORD:
                    if (mIsSaveMediaDelay) {
                        Log.d(TAG, "saveMediaDelay save 1");
                        if (mCurVideoValues != null) {
                            mPreVideoValues = new ContentValues(mCurVideoValues);
                            // saveVideo(mCurVideoValues);
                        }
                    }
                    if (mRecorder == null) {
                        mRecorder = new AWRecorder();
                    } else {
                        mRecorder.reset();
                    }

                    if (mRecorder != null && mCamera != null) {
                        mLockFlag = LOCK_FLAG_NONE;
                        try {
                            // Unlock the camera object before passing it to
                            // media
                            // recorder.
                            mCamera.unlock();
                            Log.v(TAG, "---------unlock()-----------");
                            // for MediaRecorder
                            // mRecorder.setCamera(mCamera.getCamera());
                            // for v66
                            Log.d(TAG, "setCamera mCameraId222 =" + mCameraId);
                            mRecorder.setCamera(mCamera, mCameraId);
                            mRecorder.setVideoSource(AWRecorder.VideoSource.CAMERA);
                            if (mMiniMode == MINI_MODE_WAITING_TAKEN && mMiniVideoTakenListener != null) {
                                mRecorder.setOutputFormat(AWRecorder.OutputFormat.MPEG_4);
                                mRecorder.setVideoEncoder(AWRecorder.VideoEncoder.H264);
                                mRecorder.setVideoSize(mMiniWidth, mMiniHeight);
                                Log.d("cdong",
                                        "mMiniWidth = " + mMiniWidth + "mMiniHeight = " + mMiniHeight);
                                mCurVideoValues =
                                        generateVideoFilename(AWRecorder.OutputFormat.MPEG_4);
                            } else {
                                // mRecorder.setOutputFormat(mProfile.fileFormat);
                                // mRecorder.setOutputFormat(AWRecorder.OutputFormat.MPEG_4);
                                mRecorder.setOutputFormat(AWRecorder.OutputFormat.OUTPUT_FORMAT_MPEG2TS);
                                // mRecorder.setVideoFrameRate(mProfile.videoFrameRate);
                                // mRecorder.setVideoEncoder(mProfile.videoCodec);
                                mRecorder.setVideoEncoder(AWRecorder.VideoEncoder.H264);
                                /*
                                 * if(mCameraId == CameraInfo.CAMERA_FACING_BACK) {
                                 * mRecorder.setVideoSize(1920, 1080); }else {
                                 * mRecorder.setVideoSize(1280, 720); }
                                 */
                                Log.e("hy", "mDesiredPreviewWidth_src = " + mDesiredPreviewWidth_src
                                        + " mDesiredPreviewHeight_src = " + mDesiredPreviewHeight_src);
                                Log.e("hy", "mDesiredPreviewWidth = " + mDesiredPreviewWidth + " " +
                                        "mDesiredPreviewHeight = "
                                        + mDesiredPreviewHeight);
                                if (mCameraId == CameraInfo.CAMERA_FACING_FRONT && mDesiredPreviewHeight_src == 720
                                        && mDesiredPreviewHeight == 1080) {
                                    // mRecorder.setVideoSize(1920, 1080, 1280,
                                    // 720);
                                    mRecorder.setVideoSize(mDesiredPreviewWidth_src,
                                            mDesiredPreviewHeight_src,
                                            mDesiredPreviewWidth, mDesiredPreviewHeight);
                                } else {
                                    mRecorder.setVideoSize(mDesiredPreviewWidth,
                                            mDesiredPreviewHeight);
                                }
                                // mRecorder.setVideoSize(mProfile.videoFrameWidth,
                                // mProfile.videoFrameHeight);
                                // mRecorder.setVideoEncodingBitRate(mProfile.videoBitRate);

                                if (mCameraId == CameraInfo.CAMERA_FACING_FRONT
                                        || mCameraId == CameraInfo.CAMERA_FACING_BACK) {
                                    mRecorder.setVideoEncodingBitRate(8000000);
                                } else {
                                    mRecorder.setVideoEncodingBitRate(2000000);
                                }

                                // mRecorder.setMaxDuration(mMaxVideoDurationInMs);
                                // mCurVideoValues =
                                // generateVideoFilename(mProfile.fileFormat);
                                // mCurVideoValues =
                                // generateVideoFilename(AWRecorder.OutputFormat.MPEG_4);
                                mCurVideoValues =
                                        generateVideoFilename(AWRecorder.OutputFormat.OUTPUT_FORMAT_MPEG2TS);
                                // todo
                                // mRecorder = getTmpFilePath(true);
                            }

                            mRecorder.setOutputFile(mCurVideoValues.getAsString(Video.Media.DATA));
                            // todo
                            mRecorder.setOnErrorListener(WrapedRecorder.this);
                            mRecorder.setOnInfoListener(WrapedRecorder.this);
                            /*
                             * if(mCameraId == CameraInfo.CAMERA_FACING_BACK){
                             * mRecorder.setMicMute(tape? 1 : 0); }else if(mCameraId
                             * == TwoFloatWindow.LEFT_CAMERA_ID || mCameraId ==
                             * TwoFloatWindow.RIGHT_CAMERA_ID){
                             * mRecorder.setMicMute(tape? 1 : 0); }else{
                             * mRecorder.setMicMute(mIsMute ? 1 : 0); }
                             */

                            // <zhuangdt> <20190618> 后录录音 begin
                            mRecorder.setMicMute(mIsMute ? 1 : 0);
						/*if (mCameraId == CameraInfo.CAMERA_FACING_FRONT) {
							mRecorder.setMicMute(mIsMute ? 1 : 0);
						} else {
							mRecorder.setMicMute(MicStatu.DISABLE_AUDIO_TRACK);
						}*/
                            // <zhuangdt> <20190618> 后录录音 end

                            // exeAllocateOrTruncate(mCurVideoValues.getAsString(Video.Media.DATA));
                            mRecorder.prepare();
                            // pauseAudioPlayback();
                            mRecorder.start();
                        } catch (IllegalStateException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                            mRecorder.release();
                            mRecorder = null;
                            mCamera.lock();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                            mRecorder.release();
                            mRecorder = null;
                            mCamera.lock();
                        } catch (RuntimeException e) {
                            mCamera.lock();
                            e.printStackTrace();
                        } finally {
                            int failed = -1;
                            int sucess = 0;
                            Message msg1 = Message.obtain();
                            msg1.what = MSG_START_RECORD_CB;
                            if (mRecorder == null) {
                                msg1.arg1 = failed;
                            } else {
                                msg1.arg1 = sucess;
                                if (mIsSaveMediaDelay) {
                                    Log.d(TAG, "saveMediaDelay save 2");
                                    mIsSaveMediaDelay = false;
                                    if (mPreVideoValues != null) {
                                        saveVideo(mPreVideoValues);
                                    }
                                }
                            }
                            mHandler.sendMessage(msg1);
                        }

                    }
                    break;
                case MSG_STOP_RECORD:
                    Log.d(TAG, "MSG_STOP_RECORD");
                    try {
                        if (mRecordService.isSDOut()) {
                            if (mRecorderHandler != null) {
                                mRecorderHandler.removeMessages(MSG_WRITE_KML);
                            }
                            Log.d(TAG, "missing kml when sd out");
                        }
                        if (mRecorder != null && mCamera != null) {
                            mRecorder.setOnErrorListener(null);
                            mRecorder.setOnInfoListener(null);
                            mRecorder.stop();
                            mRecorder.release();
                            mState = STATE_STOPPED;
                            mCamera.lock();
                            mRecorder = null;
                        }
                    } catch (RuntimeException e) {
                        Log.e(TAG, "Fail to stop the mRecorder.");
                        if (mRecorder != null) {
                            try {
                                mRecorder.release();
                            } catch (Exception ex) {
                                Log.e(TAG, "Fail to release the mRecorder.");
                            }
                        }
                        mRecorder = null;
                        mState = STATE_IDLE;
                    }
                    if (mLockFlag != LOCK_FLAG_NONE || mForceLock) {
                        mForceLock = false;
                        generateImpactVideoFilename(mCurVideoValues);
                        /*
                         * if (mLockFlag != LOCK_FLAG_NONE) {
                         * mHandler.sendEmptyMessage(MSG_UNLOCKED_CB); }
                         */
                        mHandler.sendEmptyMessage(MSG_UNLOCKED_CB);
                        mLockFlag = LOCK_FLAG_NONE;
                    }
                    if (mRecordService.isSDOut()) {
                        Log.d(TAG, "MSG_STOP_RECORD sd out, not save file");
                    } else {
                        if (!mIsSaveMediaDelay) {
                            saveVideo(mCurVideoValues);
                        } else {
                            Log.d(TAG, "saveMediaDelay delay");
                        }
                    }
                    if (mCameraId == CameraInfo.CAMERA_FACING_FRONT) {
                        if (mRecordService.isSDOut()) {
                            if (mRecorderHandler != null) {
                                mRecorderHandler.removeMessages(MSG_WRITE_KML);
                            }
                            Log.d(TAG, "missing kml when sd out");
                        } else {
                            if (mKmlWriter != null) {
                                mKmlWriter.holdCurFileToEnd();
                            }
                            if (mRecorderHandler != null) {
                                mRecorderHandler.removeMessages(MSG_WRITE_KML);
                                mRecorderHandler.sendEmptyMessageDelayed(MSG_WRITE_KML_END,
                                        TICKY_DELAY);
                            }
                        }
                    }
                    mLockFlag = LOCK_FLAG_NONE;
                    Log.d(TAG, "MSG_STOP_RECORD 2");
                    mHandler.sendEmptyMessage(MSG_STOP_RECORD_CB);
                    break;

                case MSG_RELEASE_RECORD:
                    try {
                        if (mRecorder != null && mCamera != null) {
                            mRecorder.release();
                            mState = STATE_IDLE;
                        }
                    } catch (RuntimeException e) {
                        Log.e(TAG, "Fail to stop the mRecorder.");
                    } finally {
                        mRecorder = null;
                        mState = STATE_IDLE;
                        mHandler.sendEmptyMessage(MSG_RELEASE_RECORD_CB);
                    }
                    break;
                case MSG_SWITCH_NEXT_FILE:
                    if (mRecorder != null && mCamera != null) {

                        if (mForceLock) {
                            mForceLock = false;
                            Log.d(TAG,
                                    "MSG_SWITCH_NEXT_FILE " + mCurVideoValues.getAsString(Video.Media.DATA));
                            generateImpactVideoFilename(mCurVideoValues);
                            Log.d(TAG,
                                    "MSG_SWITCH_NEXT_FILE " + mCurVideoValues.getAsString(Video.Media.DATA));
                            if (mLockFlag == LOCK_FLAG_1MIN) {
                                // add by chengyuzhou
                                // mLockFlag = LOCK_FLAG_2MIN;
                                mLockFlag = LOCK_FLAG_NONE;
                                Log.v(TAG, "---------lockChanger---------");
                                // end
                            } else if (mLockFlag == LOCK_FLAG_2MIN) {
                                mLockFlag = LOCK_FLAG_NONE;
                            } else {
                                mLockFlag = LOCK_FLAG_NONE;
                            }
                            if (mLockFlag == LOCK_FLAG_NONE) {
                                mHandler.sendEmptyMessage(MSG_UNLOCKED_CB);
                            }
                        } else if (mLockFlag == LOCK_FLAG_1MIN) {
                            // add by chengyuzhou
                            // generateImpactVideoFilename(mCurVideoValues);
                            // mLockFlag = LOCK_FLAG_2MIN;
                            mLockFlag = LOCK_FLAG_NONE;
                            generateImpactVideoFilename(mCurVideoValues);
                            if (mLockFlag == LOCK_FLAG_NONE) {
                                mHandler.sendEmptyMessage(MSG_UNLOCKED_CB);
                            }
                            // end
                        } else if (mLockFlag == LOCK_FLAG_2MIN) {
                            mLockFlag = LOCK_FLAG_NONE;
                            generateImpactVideoFilename(mCurVideoValues);
                            if (mLockFlag == LOCK_FLAG_NONE) {
                                mHandler.sendEmptyMessage(MSG_UNLOCKED_CB);
                            }
                        } else {
                            mLockFlag = LOCK_FLAG_NONE;
                        }
                        mPreVideoValues = new ContentValues(mCurVideoValues);
                        try {
                            // for MediaRecorder
                            /*
                             * mRecorder.stop(); mRecorder.reset();
                             * mCamera.unlock();
                             * mRecorder.setCamera(mCamera.getCamera());
                             * mRecorder.setVideoSource
                             * (AWRecorder.VideoSource.CAMERA);
                             * mRecorder.setOutputFormat(mProfile.fileFormat);
                             * mRecorder
                             * .setVideoFrameRate(mProfile.videoFrameRate);
                             * mRecorder.setVideoEncoder(mProfile.videoCodec);
                             * mRecorder.setVideoSize(mProfile.videoFrameWidth,
                             * mProfile.videoFrameHeight);
                             * mRecorder.setVideoEncodingBitRate
                             * (mProfile.videoBitRate);
                             *
                             * mRecorder.setMaxDuration(mMaxVideoDurationInMs);
                             * mCurVideoValues =
                             * generateVideoFilename(mProfile.fileFormat); //todo
                             * //mRecorder = getTmpFilePath(true);
                             *
                             * mRecorder.setOutputFile(mCurVideoValues.getAsString
                             * (Video .Media.DATA));
                             *
                             * mRecorder.prepare(); pauseAudioPlayback();
                             * mRecorder.start();
                             */
                            if (mCameraId == CameraInfo.CAMERA_FACING_FRONT) {
                                if (mRecordService.isSDOut()) {
                                    if (mRecorderHandler != null) {
                                        mRecorderHandler.removeMessages(MSG_WRITE_KML);
                                    }
                                    Log.d(TAG, "missing kml when sd out");
                                } else {
                                    if (mKmlWriter != null) {
                                        mKmlWriter.holdCurFileToSwitch();
                                    }
                                    if (mRecorderHandler != null) {
                                        mRecorderHandler.removeMessages(MSG_WRITE_KML);
                                        mRecorderHandler.sendEmptyMessageDelayed(MSG_WRITE_KML_SWITCH, TICKY_DELAY);
                                    }
                                }
                            }

                            /* for AWRecorder */
                            // mCurVideoValues =
                            // generateVideoFilename(AWRecorder.OutputFormat.MPEG_4);
                            mCurVideoValues =
                                    generateVideoFilename(AWRecorder.OutputFormat.OUTPUT_FORMAT_MPEG2TS);

                            // exeAllocateOrTruncate(mCurVideoValues.getAsString(Video.Media.DATA));
                            mRecorder.outputToNextFileStart(mCurVideoValues.getAsString(Video.Media.DATA));
                        } catch (IllegalStateException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                            mRecorder.release();
                            mRecorder = null;
                        } /*
                         * catch (IOException e) {// for MediaRecorder // TODO
                         * Auto-generated catch block e.printStackTrace();
                         * mRecorder.release(); mRecorder = null; }
                         */ finally {
                            int failed = -1;
                            int sucess = 0;
                            Message msg3 = Message.obtain();
                            msg3.what = MSG_FILE_SWITCHED;
                            if (mRecorder == null) {
                                msg3.arg1 = failed;
                            } else {
                                msg3.arg1 = sucess;
                            }
                            mHandler.sendMessage(msg3);
                            if (mRecordService.isSDOut()) {
                                Log.d(TAG, "MSG_SWITCH_NEXT_FILE sd out, not save file");
                            } else {
                                saveVideo(mPreVideoValues);
                            }
                        }

                    }
                    break;
                case MSG_SET_MUTE:
                    if (mRecorder != null) {
                        // <zhuangdt> <20190618> 后录录音 begin
                        Log.d(TAG,
                                "setMicMute msg.arg1=" + msg.arg1 + ",  mCameraId: " + mCameraId);
                        mRecorder.setMicMute(msg.arg1);
					/*if (mCameraId == CameraInfo.CAMERA_FACING_FRONT) {
						Log.d(TAG, "setMicMute, front Camera...");
						mRecorder.setMicMute(msg.arg1);
					} else if (mCameraId == CameraInfo.CAMERA_FACING_BACK) {
						Log.d(TAG, "setMicMute, back Camera...");
						mRecorder.setMicMute(1);
					}*/
                        // <zhuangdt> <20190618> 后录录音 end

                    }
                    break;
                case MSG_START_PREVIEW:
                    if (mCamera != null) {
                        if (mParameters == null) {
                            mParameters = mCamera.getParameters();
                        }
                        /*
                         * Size size = mParameters.getPreviewSize(); List<Size>
                         * sizes = mParameters.getSupportedPreviewSizes(); for(int
                         * i=0;i<sizes.size();i++){ Size s = sizes.get(i);
                         * Log.d(TAG, i +"s width=" + s.width + ";height" +
                         * s.height); } Log.d(TAG, "size width=" + size.width +
                         * ";height" + size.height);
                         * mParameters.setPreviewSize(sizes.get(0).width,
                         * sizes.get(0).height);
                         */
                        Log.e("hy", "mDesiredPreviewWidth =" + mDesiredPreviewWidth +
                                "mDesiredPreviewHeight=" + mDesiredPreviewHeight + " mDesiredPreviewWidth_src " + mDesiredPreviewWidth_src +
                                " mDesiredPreviewHeight_src " + mDesiredPreviewHeight_src);
                        if (mDesiredPreviewWidth_src != 0 && mDesiredPreviewHeight_src != 0)
                            mParameters.setPreviewSize(mDesiredPreviewWidth_src,
                                    mDesiredPreviewHeight_src);
                        else
                            mParameters.setPreviewSize(mDesiredPreviewWidth, mDesiredPreviewHeight);
                        try {
                            mCamera.setParameters(mParameters);
                        } catch (RuntimeException ex) {
                            ex.printStackTrace();
                        }
                    }
                    if (mCamera != null && msg.arg1 < TRY_MAX_TIME) {
                        try {
                            // amend
                            if (mCameraId == CameraInfo.CAMERA_FACING_BACK
                                    && MyPreference.getChipType() == MyPreference.IC_V40) {
                                mCamera.stopRender();
                            }
                            // end
                            mCamera.startPreview();
                            // amend
                            if (mCameraId == CameraInfo.CAMERA_FACING_BACK
                                    && MyPreference.getChipType() == MyPreference.IC_V40) {
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException ex) {
                                    ex.printStackTrace();
                                }
                                mCamera.startRender();
                            }
                            // end
                            mIsPreview = true;
                        } catch (RuntimeException ex) {
                            ex.printStackTrace();
                            Message pv = Message.obtain();
                            pv.what = MSG_START_PREVIEW;
                            pv.arg1 = msg.arg1 + 1;
                            if (mRecorderHandler != null) {
                                Log.i(TAG, "MSG_START_PREVIEW" + ex.getMessage());
                                mRecorderHandler.sendMessageDelayed(pv, SEND_MSG_DELAY);
                            }
                        }
                        if (mIsPreview) {
                            mHandler.sendEmptyMessage(MSG_START_PREVIEW_CB);
                        }
                    }
                    break;
                case MSG_STOP_PREVIEW:
                    if (mCamera != null) {
                        try {
                            mCamera.stopPreview();
                            mIsPreview = false;
                        } catch (RuntimeException ex) {
                            ex.printStackTrace();
                        }
                    }
                    break;
                case MSG_START_RENDER:
                    if (mCamera != null) {
                        mCamera.startRender();
                        mIsRender = true;
                    }
                    Log.d(TAG, "startRender mCamera=" + mCamera + ";mIsRender=" + mIsRender);
                    break;
                case MSG_STOP_RENDER:
                    if (mCamera != null) {
                        mCamera.stopRender();
                        mIsRender = false;
                    }
                    Log.d(TAG, "stopRender mCamera=" + mCamera + ";mIsRender=" + mIsRender);
                    break;
                case MSG_RELEASE:
                    if (mCamera != null) {
                        try {
                            mCamera.release();
                        } catch (RuntimeException ex) {
                            ex.printStackTrace();
                        }
                        mCamera = null;
                    }
                    break;
                case MSG_SET_PREVIEW_TEXTURE:
                    mTexture = (SurfaceTexture) msg.obj;
                    if (mCamera != null && msg.arg1 < TRY_MAX_TIME) {
                        try {
                            Log.i(TAG, "mCamera.setPreviewTexture");
                            mCamera.setPreviewTexture((SurfaceTexture) msg.obj);
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                            Message st = Message.obtain();
                            st.what = MSG_SET_PREVIEW_TEXTURE;
                            st.arg1 = msg.arg1 + 1;
                            st.obj = msg.obj;
                            if (mRecorderHandler != null) {
                                Log.i(TAG, "MSG_SET_PREVIEW_TEXTURE" + e.getMessage());
                                mRecorderHandler.sendMessageDelayed(st, SEND_MSG_DELAY);
                            }
                        }
                    }
                    break;
                case MSG_SET_SURFACE_HOLDER:
                    mSurfaceHolder = (SurfaceHolder) msg.obj;
                    Log.d(TAG, "mCamera =" + mCamera + "mSurfaceHolder=" + mSurfaceHolder);
                    if (mCamera != null && msg.arg1 < TRY_MAX_TIME) {
                        Surface surface = mSurfaceHolder.getSurface();
                        Log.d(TAG, "msg.arg1 =" + msg.arg1 + ", surface = " + surface + "   " +
                                "isValid: " + surface.isValid());
                        try {
                            mCamera.setPreviewDisplay((SurfaceHolder) msg.obj);
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                            Message st = Message.obtain();
                            st.what = MSG_SET_SURFACE_HOLDER;
                            st.arg1 = msg.arg1 + 1;
                            st.obj = msg.obj;
                            if (mRecorderHandler != null) {
                                Log.i(TAG, "IOException_MSG_SET_SURFACE_HOLDER: " + e.getMessage());
                                mRecorderHandler.sendMessageDelayed(st, 200);
                            }
                        } catch (IllegalArgumentException ex) {
                            ex.printStackTrace();
                            Message st = Message.obtain();
                            st.what = MSG_SET_PREVIEW_TEXTURE;
                            st.arg1 = msg.arg1 + 1;
                            st.obj = msg.obj;
                            if (mRecorderHandler != null) {
                                Log.i(TAG, "_MSG_SET_SURFACE_HOLDER: " + ex.getMessage());
                                mRecorderHandler.sendMessageDelayed(st, SEND_MSG_DELAY);
                            }
                        }
                    }
                    break;
                case MSG_SET_WATER_MARK:
                    if (mCamera != null) {
                        Bundle bd = msg.getData();
                        Float speed = bd.getFloat("speed");
                        if (speed < 0) {
                            speed = 0f;
                        }
                        String str = getWaterMarkString() + (int) (speed * 3.6) + " KM/H";
                        Log.d(TAG, "MSG_SET_WATER_MARK str=" + str);
                        try {
                            mCamera.setWaterMarkMultiple(str, WaterMarkDispMode.VIDEO_ONLY);
                        } catch (RuntimeException ex) {
                            ex.printStackTrace();
                        }
                    }
                    break;
                case MSG_START_WATER_MARK:
                    if (mCamera != null) {
                        Log.d(TAG, "startWaterMark = ");
                        try {
                            if (!mIsWaterMarkRuning) {
                                mCamera.startWaterMark();
                                mIsWaterMarkRuning = true;
                            }
                        } catch (RuntimeException ex) {
                            ex.printStackTrace();
                        }
                        mAskedWaterMark = false;
                        String str = getWaterMarkString() + 0 + " KM/H";
                        Log.d(TAG, "MSG_START_WATER_MARK str=" + str);
                        try {
                            Log.d(TAG, "addWaterMark==");
                            mCamera.setWaterMarkMultiple(str, WaterMarkDispMode.VIDEO_ONLY);
                        } catch (RuntimeException ex) {
                            ex.printStackTrace();
                        }
                    }
                    break;
                case MSG_STOP_WATER_MARK:
                    if (mCamera != null) {
                        Log.d(TAG, "stopWaterMark = ");
                        try {
                            if (mIsWaterMarkRuning) {
                                mCamera.stopWaterMark();
                                mIsWaterMarkRuning = false;
                            }
                        } catch (RuntimeException ex) {
                            ex.printStackTrace();
                        }
                    }
                    break;

                case MSG_START_ADAS:
                    Log.d(TAG, "MSG_START_ADAS mAdasDetect=" + mAdasDetect);
                    if (mCamera != null) {
                        try {
                            if (!mAdasDetect) {
                                mCamera.startAdasDetection();
                                mAdasDetect = true;
                                mAskAdasDetection = false;
                            }
                        } catch (RuntimeException ex) {
                            ex.printStackTrace();
                        }
                    }
                    break;
                case MSG_STOP_ADAS:
                    Log.d(TAG, "MSG_STOP_ADAS mAdasDetect=" + mAdasDetect);
                    if (mCamera != null) {
                        try {
                            if (mAdasDetect) {
                                mCamera.stopAdasDetection();
                                mAdasDetect = false;
                                mAskAdasDetection = false;
                            }
                        } catch (RuntimeException ex) {
                            ex.printStackTrace();
                        }
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                    }
                    break;
                case MSG_SET_ADAS_LISENER:
                    if (mCamera != null) {
                        mCamera.setAdasDetectionListener((AdasDetectionListener) msg.obj);
                    }
                    break;
                case MSG_SET_ADAS_SPEED:
                    if (mCamera != null) {
                        Bundle bd = msg.getData();
                        Float speed = bd.getFloat("speed");
                        if (speed < 0) {
                            speed = 0f;
                        }
                        try {
                            mCamera.setADASCarSpeed(speed * 3.6f);
                        } catch (RuntimeException ex) {
                            ex.printStackTrace();
                        }
                    }
                    break;
                case MSG_SET_ADAS_CAR_TYPE:
                    if (mCamera != null) {
                        Log.d(TAG, "MSG_SET_ADAS_CAR_TYPE =" + msg.arg1);
                        mCarType = msg.arg1;
                        int hight = 1300;
                        Camera.Parameters mPara;
                        if (mCarType == 0) {
                            hight = 1300;
                        } else if (mCarType == 1) {
                            hight = 1500;
                        } else {
                            hight = 2000;
                        }
                        try {
                            mPara = mCamera.getParameters();
                            mPara.set(ADAS_CAR_HEIGHT, hight);
                            mCamera.setParameters(mPara);
                        } catch (RuntimeException ex) {
                            ex.printStackTrace();
                        }

                    }
                    break;
                case MSG_SET_ADAS_LEVEL:
                    if (mCamera != null) {
                        Log.d(TAG, "MSG_SET_ADAS_LEVEL =" + msg.arg1);
                        mLevel = msg.arg1;
                        Camera.Parameters mPara;
                        try {
                            mPara = mCamera.getParameters();
                            mPara.set(ADAS_CPU_LEVEL, mLevel);
                            mCamera.setParameters(mPara);
                        } catch (RuntimeException ex) {
                            ex.printStackTrace();
                        }
                    }
                    break;
                case MSG_SET_ADAS_SENSITY:
                    if (mCamera != null) {
                        Log.d(TAG, "MSG_SET_ADAS_SENSITY crash =" + msg.arg1);
                        mParameters = mCamera.getParameters();
                        if (mParameters != null) {
                            Log.d(TAG, String.format("--------  onDistanceDetectChange state " +
                                    "%d------- ", msg.arg1));
                            mParameters.setAdasDistanceDetectLevel(mDistanceDetectLevel);
                            if (mCamera != null) {
                                try {
                                    mCamera.setParameters(mParameters);
                                } catch (RuntimeException ex) {
                                    ex.printStackTrace();
                                }
                            }
                        }
                        Log.d(TAG, "MSG_SET_ADAS_SENSITY carlane =" + (msg.arg1 + 1));
                        try {
                            mCamera.setLaneLineOffsetSensity(msg.arg1 + 1);
                        } catch (RuntimeException ex) {
                            ex.printStackTrace();
                        }
                    }
                    break;
                case MSG_WRITE_KML:
                    if (mCameraId == CameraInfo.CAMERA_FACING_FRONT) {
                        if (mKmlWriter != null) {
                            mKmlWriter.writePoint();
                        }
                        if (mRecorderHandler != null) {
                            mRecorderHandler.sendEmptyMessageDelayed(MSG_WRITE_KML, TICKY_DELAY);
                        }
                    }
                    break;
                case MSG_WRITE_KML_SWITCH:
                    if (mCameraId == CameraInfo.CAMERA_FACING_FRONT) {
                        if (mKmlWriter != null) {
                            mKmlWriter.writeSwitchFile();
                        }
                    }
                    break;
                case MSG_WRITE_KML_END:
                    if (mCameraId == CameraInfo.CAMERA_FACING_FRONT) {
                        if (mKmlWriter != null) {
                            mKmlWriter.writeEndFile();
                        }
                    }
                    break;
                case MSG_TAKE_SNAPSHOT:
                    if (mParameters == null || mCamera == null) {
                        return;
                    }

                    if (CameraUtil.isVideoSnapshotSupported(mParameters)) {

                        if (mSnapshotInProgress) {
                            return;
                        }

                        if (mMediaSaveService == null || mMediaSaveService.isQueueFull()) {
                            return;
                        }

                        Location loc = mLocationManager.getCurrentLocation();
                        CameraUtil.setGpsParameters(mParameters, loc);
                        try {
                            if (mMiniMode == MINI_MODE_WAITING_PHOTO && mMiniPictureTakenListener != null) {
                                mParameters.setPictureSize(mMiniWidth, mMiniHeight);
                            } else {
                                setPicQualtiy(mImageQuality);
                            }
                            mCamera.setParameters(mParameters);
                        } catch (RuntimeException ex) {
                            ex.printStackTrace();
                        }
                        Log.v(TAG, "Video snapshot start");
                        try {
                            mCamera.takePicture(null, null, null, new MyPictureCallback());
                            mSnapshotInProgress = true;
                        } catch (RuntimeException ex) {
                            ex.printStackTrace();
                            mSnapshotInProgress = false;
                        }
                    }
                    Log.d(TAG, "takeASnapshot done =" + mParameters + ";" + mCamera);
                    break;
                case MSG_SET_RECQUALITY:
                    if (mCamera != null) {
                        try {
                            if (mAdasDetect) {
                                mCamera.stopAdasDetection();
                            }
                            Thread.sleep(200);
                            if (mIsWaterMarkRuning) {
                                mCamera.stopWaterMark();
                            }
                            if (mIsPreview) {
                                mCamera.stopPreview();
                                mCamera.setParameters((Camera.Parameters) msg.obj);
                                mCamera.startPreview();
                            }
                            if (mIsWaterMarkRuning) {
                                mCamera.startWaterMark();
                            }
                            if (mAdasDetect) {
                                mCamera.startAdasDetection();
                            }
                        } catch (RuntimeException ex) {
                            ex.printStackTrace();
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                    }
                    break;
                case MSG_SET_FLIP_STATUS:
                    if (mCamera != null) {
                        try {
                            mCamera.setCameraFlipStatus(msg.arg1);
                        } catch (RuntimeException ex) {
                            ex.printStackTrace();
                        }
                    }
                    break;
                default:
                    break;
            }
            Log.d(TAG, "out handler mCameraId=" + mCameraId + ";msg = " + msg.what);
            super.handleMessage(msg);
        }

        public boolean waitDone() {
            Log.v(TAG, "waitDone 1");
            final Object waitDoneLock = new Object();
            final Runnable unlockRunnable = new Runnable() {
                @Override
                public void run() {
                    synchronized (waitDoneLock) {
                        Log.v(TAG, "waitDone 2");
                        waitDoneLock.notifyAll();
                        Log.v(TAG, "waitDone 3");
                    }
                }
            };
            Log.v(TAG, "waitDone 4");
            synchronized (waitDoneLock) {
                Log.v(TAG, "waitDone 5");
                if (mRecorderHandler != null) {
                    mRecorderHandler.post(unlockRunnable);
                }
                Log.v(TAG, "waitDone 6");
                try {
                    Log.v(TAG, "waitDone 7");
                    waitDoneLock.wait();
                    Log.v(TAG, "waitDone 8");
                } catch (InterruptedException ex) {
                    Log.v(TAG, "waitDone interrupted");
                    return false;
                }
                Log.v(TAG, "waitDone 9");
            }
            return true;
        }

    }

    ;

    public WrapedRecorder(int cameraId, RecordService service) {
        mRecordService = service;
        mCameraId = cameraId;
        mLocationManager = new LocationManager(mRecordService, null);
        mContentResolver = mRecordService.getContentResolver();
        HandlerThread ht = new HandlerThread("recorder Handler Thread cameraid" + mCameraId);
        ht.start();
        mRecorderHandler = new RecorderHandler(ht.getLooper());
        if (mRecorderHandler != null) {
            mRecorderHandler.sendEmptyMessage(MSG_OPEN_CAMERA);
        }
        if (mCameraId == CameraInfo.CAMERA_FACING_FRONT) {
            mKmlWriter = new KmlWriter(mRecordService);
            mRecordService.addSpeedListener(mKmlWriter);
        }
        bindMediaSaveService();
        // add by chengyuzhou
        preferences = mRecordService.getSharedPreferences("PriSP", Context.MODE_PRIVATE);
        editor = preferences.edit();
        // end
    }

    @SuppressLint("NewApi")
    public void onDestroy() {
        if (isAdasOn()) {
            setIntelligentDetect(false);
        }
        if (isWaterMarkRuning()) {
            stopWaterMark();
        }
        this.release();
        unbindMediaSaveService();
        if (mCameraId == CameraInfo.CAMERA_FACING_FRONT && mKmlWriter != null) {
            mRecordService.removeSpeedListener(mKmlWriter);
            mKmlWriter.onDestroy();
        }
        if (isPreview()) {
            stopPreview();
        }
        closeCamera();
        waitDone();
        if (mRecorderHandler != null) {
            mRecorderHandler.removeCallbacksAndMessages(null);
            if (mRecorderHandler.getLooper() != null) {
                mRecorderHandler.getLooper().quitSafely();
            }
            mRecorderHandler = null;
        }
    }

    public void removeKmlWriter() {
        if (mCameraId == CameraInfo.CAMERA_FACING_FRONT && mKmlWriter != null) {
            mRecordService.removeSpeedListener(mKmlWriter);
            mKmlWriter.onDestroy();
            mKmlWriter = null;
        }
    }

    public int getCameraId() {
        return mCameraId;
    }

    public boolean isCameraInit() {
        return mCamera == null ? false : true;
    }

    public Camera getCamera() {
        return mCamera;
    }

    public void openCamera() {
        if (null == mCamera) {
            if (mRecorderHandler != null) {
                mRecorderHandler.sendEmptyMessage(MSG_OPEN_CAMERA);
            }
        }
    }

    public void closeCamera() {
        if (mRecorderHandler != null) {
            mRecorderHandler.sendEmptyMessage(MSG_CLOSE_CAMERA);
        }
    }

    public void loadParameters() {
        mMaxVideoDurationInMs = 120000;
        MyPreference pref = MyPreference.getInstance(mRecordService);

        if (pref != null) {
            mVideoQuality = pref.getRecQuality(CameraInfo.CAMERA_FACING_FRONT);
            setRecQualityDefault(mVideoQuality);
            mImageQuality = pref.getPicQualtiy(CameraInfo.CAMERA_FACING_FRONT);
            setPicQualtiy(mImageQuality);
            setCrashSensity(pref.getCrashSensity());
            if (mCamera != null) {
                mParameters = mCamera.getParameters();
            }
            mGsensorLockLevel = pref.getLockSensity();
            mVideoDuration = pref.getRecDuration();

            // <zhuangdt> <20190618> 后录录音 begin
            mIsMute = pref.isMute();
			/*if (mCameraId == CameraInfo.CAMERA_FACING_FRONT) {
				mIsMute = pref.isMute();
			} else {
				mIsMute = true;
			}*/
            // <zhuangdt> <20190618> 后录录音 end

            // mAdasDetect = pref.getAdasFlag();
            if (mCameraId == CameraInfo.CAMERA_FACING_FRONT) {
                setCarType(pref.getCarType());
            }
            /*
             * if (mCameraId == CameraInfo.CAMERA_FACING_BACK) {
             * setCameraFlip(pref.getRearVisionFlip()); }
             */

            if (mCameraId == CameraInfo.CAMERA_FACING_BACK) {
                // setCameraFlip(pref.getRearVisionFlip());
                setCameraFlip(pref.getCameraFlipStatus(MyPreference.KEY_BACK_CAMERA_FLIP));
            }
            if (mCameraId == TwoCameraPreviewWin.secondSurfaceShowId) {
                setCameraFlip(pref.getCameraFlipStatus(MyPreference.KEY_RIGHT_CAMERA_FLIP));
            }
        }
    }

    public long getRecDuration() {
        long dura = 0;
        if (mVideoDuration == 2) {
            dura = 5 * 60 * 1000;
        } else if (mVideoDuration == 1) {
            dura = 3 * 60 * 1000;
        } else {
            dura = 1 * 60 * 1000;
        }
        return dura;
    }

    private void getDesiredPreviewSize(boolean isRestartPreview) {
        mDesiredPreviewWidth = 1280;
        mDesiredPreviewHeight = 720;
        if (mVideoQuality == 0) {
            mDesiredPreviewWidth = 1280;
            mDesiredPreviewHeight = 720;
        } else if (mVideoQuality == 1) {
            Log.i(TAG, "QUALITY_1080P, ko: " + FORNT_CAMERA_KO);
            if (FORNT_CAMERA_KO == 2363) {
                mDesiredPreviewWidth = 1280;
                mDesiredPreviewHeight = 720;
            } else {
                mDesiredPreviewWidth = 1920;
                mDesiredPreviewHeight = 1080;
            }
        } else {
            Log.i(TAG, "QUALITY_1296P");
            mDesiredPreviewWidth = 2304;
            mDesiredPreviewHeight = 1296;
        }
        if (mCamera == null) {
            Log.d(TAG, "getDesiredPreviewSize mCamera == null");
            return;
        }
        if (mParameters == null) {
            mParameters = mCamera.getParameters();
        }
        Size size = mParameters.getPreviewSize();
        List<Size> sizes = mParameters.getSupportedPreviewSizes();
        boolean isSupport = false;
        int max = -1;
        int maxIndex = -1;
        for (int i = 0; i < sizes.size(); i++) {
            if (i == 0) {
                max = sizes.get(i).width;
                maxIndex = 0;
            } else if (max < sizes.get(i).width) {
                max = sizes.get(i).width;
                maxIndex = i;
            }
            Log.d(TAG, "max = " + max + ";maxIndex = " + maxIndex);
            Size sz = sizes.get(i);
            Log.e("hy", i + "s width=" + sz.width + ";height" + sz.height);
            if (mDesiredPreviewWidth == sizes.get(i).width && mDesiredPreviewHeight == sizes.get(i).height) {
                isSupport = true;
            }
        }
        Log.e("hy", "isSupport = " + isSupport + ";maxIndex = " + maxIndex);
        Log.d("cdong",
                "mCameraId = " + mCameraId + " CameraInfo.CAMERA_FACING_FRONT = " + CameraInfo.CAMERA_FACING_FRONT);

        mDesiredPreviewWidth_src = 0;
        mDesiredPreviewHeight_src = 0;
        if (!isSupport && maxIndex >= 0 && maxIndex < sizes.size()) {
            Log.d(TAG, " get default max size mDesiredPreviewWidth =" + mDesiredPreviewWidth + ";" +
                    "mDesiredPreviewHeight"
                    + mDesiredPreviewHeight);
            if (mCameraId == CameraInfo.CAMERA_FACING_FRONT) {
                mDesiredPreviewWidth_src = sizes.get(maxIndex).width;
                mDesiredPreviewHeight_src = sizes.get(maxIndex).height;
                if (mDesiredPreviewWidth_src != 1280 || mDesiredPreviewHeight_src != 720) {
                    mDesiredPreviewWidth = mDesiredPreviewWidth_src;
                    mDesiredPreviewHeight = mDesiredPreviewHeight_src;
                    mDesiredPreviewWidth_src = 0;
                    mDesiredPreviewHeight_src = 0;
                }
            } else {
                mDesiredPreviewWidth = sizes.get(maxIndex).width;
                mDesiredPreviewHeight = sizes.get(maxIndex).height;
            }
        }
        if (mParameters != null) {
            if (mDesiredPreviewWidth_src != 0 && mDesiredPreviewHeight_src != 0)
                mParameters.setPreviewSize(mDesiredPreviewWidth_src, mDesiredPreviewHeight_src);
            else
                mParameters.setPreviewSize(mDesiredPreviewWidth, mDesiredPreviewHeight);
            if (isRestartPreview && mCameraId == CameraInfo.CAMERA_FACING_FRONT) {
                if (mRecorderHandler != null) {
                    Message msg = mRecorderHandler.obtainMessage();
                    msg.what = MSG_SET_RECQUALITY;
                    msg.obj = mParameters;
                    mRecorderHandler.sendMessage(msg);
                }
            } else {
                try {
                    mCamera.setParameters(mParameters);
                } catch (RuntimeException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        // TODO Auto-generated method stub
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            // by chengyuzhou 澧炲姞浜嗗湪鏈綍鍍忔儏鍐典笅锛屾憞鏅冨綍鍍忓拰鍔犳墍鍔熻兘
            // if (mState == STATE_STARTED) {
            float threshold = LOCK_SENSITY_MIDIUM;
            float xlateral = sensorEvent.values[0];
            float ylongitudinal = sensorEvent.values[1];
            float zvertical = sensorEvent.values[2];
            if (mGsensorLockLevel == 0) {
                threshold = LOCK_SENSITY_HIGH;
            } else if (mGsensorLockLevel == 1) {
                threshold = LOCK_SENSITY_MIDIUM;
            } else if (mGsensorLockLevel == 2) {
                threshold = LOCK_SENSITY_LOW;
            } else {
                // add by chengyuzhou
                threshold = 100;
                // end
            }
            if ((xlateral > threshold) || (ylongitudinal > threshold) || (zvertical > threshold)) {
                Log.v(TAG,
                        "heading=" + xlateral + ", pitch=" + ylongitudinal + ", roll=" + zvertical + ", " +
                                "threshold="
                                + threshold);
                mHandler.sendEmptyMessage(MSG_GSENSOR_LOCK_UP);
            }
            // }
        }

    }

    public int startRecording() {
        Log.d(TAG, "startRecording mState=" + mState);
        if (mState == STATE_STARTED) {
            return 0;
        } else if (mState == STATE_STARTING || mState == STATE_STOPPING) {
            return -1;
        } else {
            // mStartRecordTime = System.currentTimeMillis();
            // mHandler.removeMessages(MSG_RECORD_TICKY);
            // mHandler.sendEmptyMessage(MSG_RECORD_TICKY);
            Log.d(TAG, "startRecording mCameraId=" + mCameraId);
            Log.d(TAG, "startRecording isBackCameraOut=" + mRecordService.isBackCameraOut());
            if (mCameraId == CameraInfo.CAMERA_FACING_BACK && mRecordService.isBackCameraOut()) {
                return -1;
            } else if (!mIsPreview || mCamera == null) {
                mAskRecorder = true;
                mState = STATE_STARTING;
            } else {
                if (mRecorderHandler != null) {
                    mRecorderHandler.sendEmptyMessage(MSG_START_RECORD);
                }
                mState = STATE_STARTING;
                mAskRecorder = false;
            }
            Log.d(TAG, "startRecording mAskRecorder=" + mAskRecorder);
            return 0;
        }
    }

    public boolean isRecording() {
        if (mCamera == null) {
            return false;
        }
        if (mState == STATE_STARTING || mState == STATE_STARTED || mState == STATE_SWITCHING_FILE) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isRecorderBusy() {
        if (mCamera == null) {
            Log.d(TAG, "isRecorderBusy mCamera == null");
            return false;
        }
        Log.d(TAG, "isRecorderBusy mState = " + mState + ",mCameraId = " + mCameraId);
        if (mState == STATE_STARTING || mState == STATE_STOPPING || mState == STATE_SWITCHING_FILE) {
            return true;
        } else {
            return false;
        }
    }

    public int stopRecording() {
        Log.d(TAG, "stopRecording mState=" + mState);
        isVideoNeed = false;
        int res = -1;
        if (mState == STATE_IDLE || mState == STATE_STOPPED) {
            res = -1;
        } else if (mState == STATE_STARTING || mState == STATE_STOPPING) {
            res = -1;
        } else {
            // mHandler.removeMessages(MSG_RECORD_TICKY);
            if (mRecorderHandler != null) {
                mRecorderHandler.sendEmptyMessage(MSG_STOP_RECORD);
            }
            mAskRecorder = false;
            mState = STATE_STOPPING;
            res = 0;
        }
        if (mMiniMode == MINI_MODE_WAITING_STOP && mMiniVideoTakenListener != null) {
            if (res != 0) {
                mMiniVideoTakenListener.onStopForMini(mCameraId);
            }
        } else if (mMiniMode == MINI_MODE_WAITING_TAKEN && mMiniVideoTakenListener != null) {
            if (res != 0) {
                mMiniVideoTakenListener.onMiniVideoTaken(null, mCameraId);
            }
        }
        return res;
    }

    public int switchToNextFile() {
        Log.d(TAG, "switchToNextFile");
        if (mState == STATE_STARTED) {
            if (mRecorderHandler != null) {
                mRecorderHandler.removeMessages(MSG_SWITCH_NEXT_FILE);
                mRecorderHandler.sendEmptyMessage(MSG_SWITCH_NEXT_FILE);
                // add by chengyuzhou
                Log.v(TAG, "------------toNextFile-----------");
                if (mRecordCallback != null) {
                    mRecordCallback.onLocked(false);
                }
                // end
            }
            mState = STATE_SWITCHING_FILE;
            return 0;
        } else {
            return -1;
        }
    }

    public int release() {
        if (mState == STATE_IDLE || mState == STATE_STOPPING || mState == STATE_STOPPED) {
            if (mRecorderHandler != null) {
                mRecorderHandler.sendEmptyMessage(MSG_RELEASE_RECORD);
            }
        } else {
            mIsSaveMediaDelay = true;
            if (mRecorderHandler != null) {
                mRecorderHandler.sendEmptyMessage(MSG_STOP_RECORD);
                mRecorderHandler.sendEmptyMessage(MSG_RELEASE_RECORD);
            }
        }
        /*
         * if (mCamera != null) {
         * mRecorderHandler.sendEmptyMessage(MSG_RELEASE); }
         */
        return 0;
    }

    public boolean isAdasOn() {
        return mAdasDetect;
    }

    public void setIntelligentDetect(boolean isOn) {

        Log.d(TAG, "setIntelligentDetect isOn=" + isOn + "mCamera=" + mCamera);
        if (!mIsPreview || mCamera == null) {
            mAskAdasDetection = true;
        } else {
            mAskAdasDetection = false;
            if (isOn && mCamera != null) {
                if (mRecorderHandler != null) {
                    mRecorderHandler.sendEmptyMessage(MSG_START_ADAS);
                }
                if (mIsFirstAdasUp) {
                    mIsFirstAdasUp = false;
                }
            } else {
                if (mRecorderHandler != null) {
                    mRecorderHandler.sendEmptyMessage(MSG_STOP_ADAS);
                }
            }
        }
        Log.d(TAG, "setIntelligentDetect mAskAdasDetection=" + mAskAdasDetection);
        return;
    }

    public boolean setRecQualityDefault(int value) {
        mVideoQuality = value;
        getDesiredPreviewSize(false);
        if (!mIsPreview || mCamera == null) {
            mAskedWaterMark = true;
        } else {
            if (isWaterMarkRuning()) {
                stopWaterMark();
            }
            if (mRecorderHandler != null) {
                mRecorderHandler.sendEmptyMessage(MSG_START_WATER_MARK);
            }
        }
        return false;
    }

    public boolean setRecQuality(int value) {
        mVideoQuality = value;
        getDesiredPreviewSize(true);
        if (!mIsPreview || mCamera == null) {
            mAskedWaterMark = true;
        }
        return false;
    }

    public boolean setPicQualtiy(int value) {
        Log.d(TAG, "setPicQualtiy");
        if (mCamera == null) {
            return false;
        }
        if (mParameters == null) {
            mParameters = mCamera.getParameters();
        }
        if (mParameters == null) {
            return false;
        }
        mImageQuality = value;
        int width = 0;
        int height = 0;
        // add by chengyuzhou
        /*
         * if (mImageQuality == 0) { // 2M width = 1280; height = 720; } else if
         * (mImageQuality == 1) { // 5M width = 1920; height = 1080; } else if
         * (mImageQuality == 2) { // 8M width = 2304; height = 1296; }
         */

        if (mImageQuality == 0) { // 2M
            width = 840;
            height = 480;
        } else if (mImageQuality == 1) { // 4M
            width = 1080;
            height = 720;
        } else if (mImageQuality == 2) { // 6M
            width = 1920;
            height = 1080;
        }
        // end
        List<Size> sizes = mParameters.getSupportedPreviewSizes();
        boolean isSupport = false;
        int max = -1;
        int maxIndex = -1;
        for (int i = 0; i < sizes.size(); i++) {
            if (i == 0) {
                max = sizes.get(i).width;
                maxIndex = 0;
            } else if (max < sizes.get(i).width) {
                max = sizes.get(i).width;
                maxIndex = i;
            }
            Log.d(TAG, "max = " + max + ";maxIndex = " + maxIndex);
            Size sz = sizes.get(i);
            Log.d(TAG, i + "s width=" + sz.width + ";height" + sz.height);
            if (height == sizes.get(i).height) {
                isSupport = true;
                width = sizes.get(i).width;
            }
        }
        Log.d(TAG, "isSupport = " + isSupport + ";maxIndex = " + maxIndex);
        if (!isSupport && maxIndex >= 0 && maxIndex < sizes.size()) {
            Log.d(TAG, " get default max size with =" + width + ";height" + height);
            width = sizes.get(maxIndex).width;
            height = sizes.get(maxIndex).height;
        }
        if (mCameraId == RecorderActivity.CAMERA_THIRD) {
            // width = 1280;
            // height = 720;
            width = mDesiredPreviewWidth;
            height = mDesiredPreviewHeight;
        }
        mParameters.setPictureSize(width, height);
        try {
            mCamera.setParameters(mParameters);
        } catch (RuntimeException ex) {
            ex.printStackTrace();
        }
        return true;
    }

    public void setDuration(int value) {
        Log.d(TAG, "setDuration value=" + value);
        mVideoDuration = value;
        if (mVideoDuration == 0) {
            mMaxVideoDurationInMs = 30000; // 1sec
        }
        if (mVideoDuration == 1) {
            mMaxVideoDurationInMs = 12000; // 2sec
            /* need decrease falloc time */
            mMaxVideoDurationInMs = mMaxVideoDurationInMs - 500;
        }
        if (mVideoDuration == 2) {
            mMaxVideoDurationInMs = 300000; // 5sec
            mMaxVideoDurationInMs = mMaxVideoDurationInMs - 1500;
        }
        return;
    }

    public void setCarLaneAdjust(int value) {
        // todo
    }

    public void setLockFlag(boolean isLock) {
        if (mForceLock) {
            return;
        }
        mForceLock = isLock;
        if (mRecordCallback == null) {
            return;
        }
        if (mForceLock) {
            mRecordCallback.onLocked(true);
        } else if (mLockFlag != LOCK_FLAG_NONE) {
            mRecordCallback.onLocked(true);
        } else {
            mRecordCallback.onLocked(false);
        }
    }

    public boolean getLockFlag() {
        return mForceLock || mLockFlag != LOCK_FLAG_NONE;
    }

    public void setCrashSensity(int value) {
        if (mCamera == null || mCameraId != CameraInfo.CAMERA_FACING_FRONT) {
            return;
        }

        if (value == 0) {
            mDistanceDetectLevel = 2;
        } else if (value == 2) {
            mDistanceDetectLevel = 0;
        } else {
            mDistanceDetectLevel = 1;
        }

        Message msg = Message.obtain();
        msg.what = MSG_SET_ADAS_SENSITY;
        msg.arg1 = mDistanceDetectLevel;
        if (mRecorderHandler != null) {
            mRecorderHandler.sendMessage(msg);
        }
    }

    public void setLockSensity(int value) {
        mGsensorLockLevel = value;
    }

    public void setParkingCrashSensity(int value) {
        if (mCamera == null || mParameters == null) {
            return;
        }
        Log.d(TAG, "onMotionDetectChange " + value);
        mMotionDetect = value;
        // todo
        // mCamera.setMotionDetect(value);
    }

    public void setCarType(int value) {
        if (mCamera == null) {
            return;
        }
        Message msg = Message.obtain();
        msg.what = MSG_SET_ADAS_CAR_TYPE;
        msg.arg1 = value;
        if (mRecorderHandler != null) {
            mRecorderHandler.sendMessage(msg);
        }
    }

    public void setAdasLevel(int value) {
        if (mCamera == null) {
            return;
        }
        Message msg = Message.obtain();
        msg.what = MSG_SET_ADAS_LEVEL;
        msg.arg1 = value;
        mRecorderHandler.sendMessage(msg);

    }

    public void setAdasDetecttionListener(AdasDetectionListener listener) {
        // todo
        // onAdasDetection
        Log.d(TAG, "setAdasDetecttionListener =" + listener);
        Message msg = Message.obtain();
        msg.what = MSG_SET_ADAS_LISENER;
        msg.obj = listener;
        if (mRecorderHandler != null) {
            mRecorderHandler.sendMessage(msg);
        }
        mAdasListener = listener;
    }

    public void setAdasSpeed(float speed) {
        if (isRecorderBusy()) {
            return;
        }
        Message msg = Message.obtain();
        msg.what = MSG_SET_ADAS_SPEED;
        Bundle bd = new Bundle();
        bd.putFloat("speed", speed);
        msg.setData(bd);
        if (mRecorderHandler != null) {
            mRecorderHandler.removeMessages(MSG_SET_ADAS_SPEED);
            mRecorderHandler.sendMessage(msg);
        }
    }

    public void setWaterMarkMultiple(float speed, double longitude, double latitude) {
        if (isRecorderBusy()) {
            return;
        }
        Message msg = Message.obtain();
        msg.what = MSG_SET_WATER_MARK;
        Bundle bd = new Bundle();
        bd.putFloat("speed", speed);
        msg.setData(bd);
        if (mRecorderHandler != null) {
            mRecorderHandler.removeMessages(MSG_SET_WATER_MARK);
            mRecorderHandler.sendMessage(msg);
        }
    }

    public void setCameraFlip(boolean isFlip) {
        Log.i(TAG, "setCameraFlip() cameraId = " + mCameraId + ",isFlip = " + isFlip);
        Message msg = Message.obtain();
        msg.what = MSG_SET_FLIP_STATUS;
        msg.arg1 = isFlip ? 1 : 0;
        if (mRecorderHandler != null) {
            mRecorderHandler.sendMessage(msg);
        }
    }

    public void startWaterMark() {
        if (!mIsPreview || mCamera == null) {
            mAskedWaterMark = true;
        } else {
            if (mCameraId == CameraInfo.CAMERA_FACING_FRONT) {
                if (mRecorderHandler != null) {
                    mRecorderHandler.sendEmptyMessage(MSG_START_WATER_MARK);
                }
            }
        }
    }

    public void stopWaterMark() {
        mAskedWaterMark = false;
        if (mRecorderHandler != null) {
            mRecorderHandler.sendEmptyMessage(MSG_STOP_WATER_MARK);
        }
    }

    public boolean isWaterMarkRuning() {
        return mIsWaterMarkRuning;
    }

    public void startPreview() {
        Log.d(TAG, "startPreview mCameraId =" + mCameraId);
        mAskedPreview = true;
        Log.d(TAG, "mParameters =" + mParameters + "mCamera=" + mCamera);
        if (mCamera != null) {
            // mCamera.stopPreview();
            if (mRecorderHandler != null) {
                mRecorderHandler.sendEmptyMessage(MSG_START_PREVIEW);
            }
            mAskedPreview = false;
        }
    }

    public void startRender() {
        if (mRecorderHandler != null) {
            mRecorderHandler.removeMessages(MSG_START_RENDER);
            mRecorderHandler.sendEmptyMessage(MSG_START_RENDER);
        }
    }

    public void stopPreview() {
        if (mRecorderHandler != null) {
            mRecorderHandler.removeMessages(MSG_STOP_PREVIEW);
            mRecorderHandler.sendEmptyMessage(MSG_STOP_PREVIEW);
        }
    }

    public void stopRender() {
        if (mRecorderHandler != null) {
            mRecorderHandler.removeMessages(MSG_STOP_RENDER);
            mRecorderHandler.sendEmptyMessage(MSG_STOP_RENDER);
        }
    }

    public boolean isPreview() {
        return mIsPreview;
    }

    public boolean isRender() {
        return mIsRender;
    }

    public void setPreviewDisplay(SurfaceHolder sh) {
        if (sh == null || sh.getSurface() == null) {
            Log.d(TAG, "setPreviewDisplay surface not ready");
            return;
        }
        Message msg = Message.obtain();
        msg.what = MSG_SET_SURFACE_HOLDER;
        msg.arg1 = 0;
        msg.obj = sh;
        if (mRecorderHandler != null) {
            mRecorderHandler.removeMessages(MSG_SET_SURFACE_HOLDER);
            mRecorderHandler.sendMessage(msg);
        }
    }

    public void setPreviewTexture(SurfaceTexture st) {
        Log.d(TAG, "setPreviewTexture=" + st + ";mCameraId=" + mCameraId);
        if (st == null) {
            return;
        }
        Message msg = Message.obtain();
        msg.what = MSG_SET_PREVIEW_TEXTURE;
        msg.arg1 = 0;
        msg.obj = st;
        if (mRecorderHandler != null) {
            mRecorderHandler.removeMessages(MSG_SET_PREVIEW_TEXTURE);
            mRecorderHandler.sendMessage(msg);
        }
    }

    public void setMute(boolean isMute) {
        Log.v(TAG, "---------daodaodeisMute:" + isMute);
        mIsMute = isMute;
        tape = isMute;
        if (mRecorder != null) {
            Message ms = Message.obtain();
            ms.what = MSG_SET_MUTE;
            ms.arg1 = mIsMute ? 1 : 0;
            if (mRecorderHandler != null) {
                mRecorderHandler.sendMessage(ms);
            }
        }
        if (mRecordCallback != null) {
            mRecordCallback.onMute(mIsMute);
        }
    }

    private boolean isPictureNeed = false;
    private boolean isVideoNeed = false;

    public void needFeedbackPictureFileName() {
        isPictureNeed = true;
    }

    public void needFeedbackVideoFileName() {
        isVideoNeed = true;
    }

    private ContentValues generateVideoFilename(int outputFileFormat) {
        long dateTaken = 0;
        if (mRecordService != null) {
            dateTaken = mRecordService.getCurTime();
        } else {
            dateTaken = System.currentTimeMillis();
        }
        String title = createFileName(dateTaken);
        // Used when emailing.
        String filename = title + convertOutputFormatToFileExt(outputFileFormat);
        String mime = convertOutputFormatToMimeType(outputFileFormat);
        File file = new File(Storage.DIRECTORY);
        if (!file.exists()) {
            file.mkdirs();
        }
        String tempFile = "";
        /*
         * if(mCameraId < 2){ tempFile = Storage.DIRECTORY + '/' + filename;
         * }else{ File file2 = new File("/sdcard/DCIM/Camera"); if
         * (!file2.exists()) { file2.mkdir(); } tempFile =
         * "/sdcard/DCIM/Camera/" + filename; }
         */
        tempFile = Storage.DIRECTORY + '/' + filename;
        final String path = tempFile;// "/dev/null";//
        String tmpPath = path + ".tmp";

        ContentValues value = new ContentValues(9);
        value.put(Video.Media.TITLE, title);
        value.put(Video.Media.DISPLAY_NAME, filename);
        value.put(Video.Media.DATE_TAKEN, dateTaken);
        value.put(MediaColumns.DATE_MODIFIED, dateTaken / 1000);
        value.put(Video.Media.MIME_TYPE, mime);
        value.put(Video.Media.DATA, path);
        // todo: fix solution
        value.put(Video.Media.RESOLUTION, Integer.toString(1280) + "x" + Integer.toString(720));
        value.put(Video.Media.WIDTH, 1280);
        value.put(Video.Media.HEIGHT, 720);

        Location loc = mLocationManager.getCurrentLocation();
        if (loc != null) {
            value.put(Video.Media.LATITUDE, loc.getLatitude());
            value.put(Video.Media.LONGITUDE, loc.getLongitude());
        }
        Log.d(TAG, "generateVideoFilename file=" + path);
        if (isVideoNeed) {
            isVideoNeed = false;
            mHandler.postDelayed(new Runnable() {

                @Override
                public void run() {
                    Intent videoIntent = new Intent("com.action.create_video_file");
                    videoIntent.putExtra("videoFile", path);
                    mRecordService.sendBroadcast(videoIntent);
                }
            }, 1000);
        }
        // kml
        if (mCameraId == CameraInfo.CAMERA_FACING_FRONT) {
            // add by chengyizhou
            /*
             * if (mKmlWriter == null) { mKmlWriter = new
             * KmlWriter(mRecordService);
             * mRecordService.addSpeedListener(mKmlWriter); }
             * mKmlWriter.setCurFileName(createKmlFileName(dateTaken)); if
             * (mRecorderHandler != null) {
             * mRecorderHandler.sendEmptyMessageDelayed(MSG_WRITE_KML,
             * TICKY_DELAY); }
             */
            // end
        }
        return value;
    }

    private void generateImpactVideoFilename(ContentValues values) {
        if (values != null) {
            String oldPath = values.getAsString(Video.Media.DATA);
            int index = oldPath.lastIndexOf(".");
            Log.v(TAG, "index=" + index);
            if (!oldPath.contains(IMPACT_SUFFIX)) {
                String path =
                        oldPath.substring(0, index) + IMPACT_SUFFIX + oldPath.substring(index);
                // mCurCsiVideoValues.remove(Video.Media.DATA);
                values.put(Video.Media.DATA, path);
                Log.v(TAG, "New impact video filename: " + path);
            }
        }
        // add by chengyuzhou
        // if (mRecordCallback != null) {
        // mRecordCallback.onLocked(false);
        // }
        // end

    }

    private String createFileName(long dateTaken) {
        Date date = new Date(dateTaken);
        String backOrFront = null;

        SimpleDateFormat dateFormat = null;
        dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");

        if (mCameraId == CameraInfo.CAMERA_FACING_FRONT) {
            if (CustomValue.CHANGE_FRONT_BACK_CAMERA) {
                backOrFront = "_Back";
            } else {
                backOrFront = "_Front";
            }
        } else if (mCameraId == CameraInfo.CAMERA_FACING_BACK) {
            if (CustomValue.CHANGE_FRONT_BACK_CAMERA) {
                backOrFront = "_Front";
            } else {
                backOrFront = "_Back";
            }
        } /*
         * else if (mCameraId == TwoFloatWindow.LEFT_CAMERA_ID) {
         * backOrFront = "_Left"; }
         */ else if (mCameraId == TwoFloatWindow.RIGHT_CAMERA_ID) {
            backOrFront = "_Right";
        }
        //by lym start
        if (CustomValue.ONLY_ONE_CAMERA) {
            backOrFront = "";
        }
        //end
        String shortCutSuffix = null;
        Log.d(TAG, "mVideoDuration =" + mVideoDuration);
        if (mVideoDuration == 0) {
            shortCutSuffix = SUBFIX_1_MIN;
        } else if (mVideoDuration == 1) {
            shortCutSuffix = SUBFIX_2_MIN;
        } else {
            shortCutSuffix = SUBFIX_3_MIN;
        }

        return dateFormat.format(date) + backOrFront + shortCutSuffix;
    }

    private String createPictureFileName(long dateTaken) {
        String result = null;
        Date date = new Date(dateTaken);

        SimpleDateFormat dateFormat = null;
        if (mCameraId == CameraInfo.CAMERA_FACING_FRONT) {
            if (CustomValue.CHANGE_FRONT_BACK_CAMERA) {
                dateFormat = new SimpleDateFormat("'IMG_'yyyyMMdd_HHmmss'Back'");

            } else {
                dateFormat = new SimpleDateFormat("'IMG_'yyyyMMdd_HHmmss'Front'");
            }
        } else if (mCameraId == CameraInfo.CAMERA_FACING_BACK) {
            if (CustomValue.CHANGE_FRONT_BACK_CAMERA) {
                dateFormat = new SimpleDateFormat("'IMG_'yyyyMMdd_HHmmss'Front'");
            } else {
                dateFormat = new SimpleDateFormat("'IMG_'yyyyMMdd_HHmmss'Back'");
            }
        } /*
         * else if (mCameraId == TwoFloatWindow.LEFT_CAMERA_ID) { dateFormat
         * = new SimpleDateFormat("'IMG_'yyyyMMdd_HHmmss'Left'"); }
         */ else if (mCameraId == TwoFloatWindow.RIGHT_CAMERA_ID) {
            dateFormat = new SimpleDateFormat("'IMG_'yyyyMMdd_HHmmss'Right'");
        }
        //by lym start
        if (CustomValue.ONLY_ONE_CAMERA) {
            dateFormat = new SimpleDateFormat("'IMG_'yyyyMMdd_HHmmss");
        }
        //end
        result = dateFormat.format(date);
        if (dateTaken / 1000 == mLastDate / 1000) {
            mSameSecondCount++;
            result += "_" + mSameSecondCount;
        } else {
            mLastDate = dateTaken;
            mSameSecondCount = 0;
        }
        return result;
    }

    private String createKmlFileName(long dateTaken) {
        Date date = new Date(dateTaken);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss'K'");
        return Storage.DIRECTORY + "/" + dateFormat.format(date);
    }

    private String convertOutputFormatToMimeType(int outputFileFormat) {
        if (outputFileFormat == AWRecorder.OutputFormat.MPEG_4) {
            return "video/mp4";
        } else if (outputFileFormat == AWRecorder.OutputFormat.OUTPUT_FORMAT_MPEG2TS) {
            return "video/mp2ts";
        }
        return "video/3gpp";
    }

    private String convertOutputFormatToFileExt(int outputFileFormat) {
        if (outputFileFormat == AWRecorder.OutputFormat.MPEG_4) {
            return ".mp4";
        } else if (outputFileFormat == AWRecorder.OutputFormat.OUTPUT_FORMAT_MPEG2TS) {
            return ".ts";
        }
        return ".3gp";
    }

    private void pauseAudioPlayback() {
        // Shamelessly copied from MediaPlaybackService.java, which
        // should be public, but isn't.
        Intent it = new Intent("com.android.music.musicservicecommand");
        it.putExtra("command", "pause");

        mRecordService.sendBroadcast(it);
    }

    public void takeASnapshot() {
        Log.d(TAG, "takeASnapshot =" + mParameters + ";" + mCamera);
        if (mRecordCallback != null) {
            mRecordCallback.onPictureToken();
        }
        if (mRecorderHandler != null) {
            mRecorderHandler.sendEmptyMessage(MSG_TAKE_SNAPSHOT);
        }
    }

    private class MyPictureCallback implements PictureCallback {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            // TODO Auto-generated method stub
            Log.v(TAG, "onPictureTaken");
            Location loc = mLocationManager.getCurrentLocation();
            storeImage(data, loc);
            mSnapshotInProgress = false;

        }
    }

    public void saveVideo(ContentValues conVal) {
        Log.d(TAG, "saveVideo conVal=" + conVal);
        if (conVal == null) {
            return;
        }
        long duration = System.currentTimeMillis() - conVal.getAsLong(Video.Media.DATE_TAKEN);
        duration = Math.abs(duration);
        String file = conVal.getAsString(Video.Media.DATA);
        if (file == null || duration < 0) {
            return;
        }
        String finalFile = file;
        if (file.contains(IMPACT_SUFFIX)) {
            finalFile = file.replaceAll(IMPACT_SUFFIX, "");
        }
        final String filePath = finalFile;
        Log.d(TAG, "saveVideo file=" + finalFile);
        mMediaSaveService.addVideo(finalFile, duration, conVal, mOnVideoSavedListener,
                mContentResolver);
    }

    public void updatVideoName(ContentValues conVal, String fileName) {
        if (conVal == null || fileName == null) {
            return;
        }
        long duration = conVal.getAsLong(Video.Media.DATE_TAKEN);

        if (duration < 0) {
            return;
        }
        mMediaSaveService.addVideo(fileName, duration, conVal, mOnVideoSavedListener,
                mContentResolver, true);

    }

    private void sendImagePathBroadcast(String imageName) {
        Log.i(TAG, "sendImagePathBroadcast() imageName = " + imageName);
        Intent pictureIntent = new Intent("com.action.passFullImageName");
        String path = Storage.DIRECTORY + "/" + imageName + ".jpg";
        pictureIntent.putExtra("imagePath", path);
        mRecordService.sendBroadcast(pictureIntent);
    }

    private void storeImage(final byte[] data, Location loc) {
        long dateTaken = System.currentTimeMillis();
        // String title = CameraUtil.createJpegName(dateTaken);
        final String title = createPictureFileName(dateTaken);
        Log.i(TAG, "title = " + title);
        if (isPictureNeed) {
            isPictureNeed = false;
            mHandler.postDelayed(new Runnable() {

                @Override
                public void run() {
                    Intent pictureIntent = new Intent("com.action.passImageName");
                    if (title.contains("Front")) {
                        pictureIntent.putExtra("imageNameFront", title);
                        pictureIntent.putExtra("imageName", title);
                    }
                    if (title.contains("Back")) {
                        pictureIntent.putExtra("imageNameBack", title);
                    }
                    mRecordService.sendBroadcast(pictureIntent);
                    sendImagePathBroadcast(title);
                }
            }, 3000);
        }

        ExifInterface exif = Exif.getExif(data);
        int orientation = Exif.getOrientation(exif);
        if (Storage.getTotalSpace() > 0) {
            mMediaSaveService.addImage(data, title, dateTaken, loc, orientation, exif,
                    mOnPhotoSavedListener,
                    mContentResolver);
        }
        if (mMiniMode == MINI_MODE_WAITING_PHOTO && mMiniPictureTakenListener != null) {
            mMiniPictureTakenListener.onMiniPictureTaken(Storage.generateFilepath(title),
                    mCameraId);
        }
    }

    private void bindMediaSaveService() {
        Intent intent = new Intent(mRecordService, MediaSaveService.class);
        mRecordService.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    private void unbindMediaSaveService() {
        if (mConnection != null) {
            mRecordService.unbindService(mConnection);
        }
    }

    @Override
    public void onError(AWRecorder mr, int arg1, int arg2) {
        // TODO Auto-generated method stub
        Toast.makeText(mRecordService, "onError", Toast.LENGTH_LONG).show();

    }

    @Override
    public void onInfo(AWRecorder mr, int arg1, int arg2) {
        // TODO Auto-generated method stub

    }

    public interface IRecordCallback {
        void onCameraOpen();

        void onRecordStarted(boolean isStarted);

        void onRecordStoped();

        void onTimeUpdate(long curTime);

        void onMute(boolean isMuted);

        void onLocked(boolean isLocked);

        void onPictureToken();

        void onCameraPlug(boolean isOut);
    }

    public void setRecordCallback(IRecordCallback cb) {
        mRecordCallback = cb;
    }

    public IRecordCallback getRecordCallback() {
        return mRecordCallback;
    }

    public boolean isRecordingFile(String name) {
        if (mCurVideoValues != null && name != null && mCurVideoValues.getAsString(Video.Media.DATA).equals(name)) {
            Log.d(TAG, "curVideo =" + mCurVideoValues.getAsString(Video.Media.DATA));
            return true;
        } else {
            return false;
        }
    }

    public void setRecorderDestroyedListener(IRecorderDestroyedListener ls) {
        mDestroyedListener = ls;
    }

    public interface IRecorderDestroyedListener {
        public void onRecorderDestroyed(int cameraId);
    }

    private int findCameraIndex(int cameraFacing) {
        int cameraCount = 0;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras();
        Log.d(TAG, "---------findCameraIndex ---------cameraCount----" + cameraCount);
        if (SystemProperties.getInt("ro.sys.float_camera", -1) == 1) {
            if (CameraInfo.CAMERA_FACING_BACK == cameraFacing /**
             * &&
             * cameraCount>=
             * 4
             */
            ) {
                if (jugeFileIsExit("/dev/video1") || jugeFileIsExit("/dev/video4")) {
                    return 1;
                } else {
                    return -1;
                }
            } else if (CameraInfo.CAMERA_FACING_FRONT == cameraFacing) {/// dev/video0
                if (jugeFileIsExit("/dev/video0")) {
                    Log.d(TAG, "--------jugeFileIsExit---------" + true);
                    return 0;
                } else {
                    Log.d(TAG, "---------jugeFileIsExit---------" + false);
                    return -1;
                }
            }
        } else {
            if (CustomValue.CHANGE_FRONT_BACK_CAMERA) {
                Log.d(TAG, "findCameraIndex: true cameraFacing " + cameraFacing);

                if (CameraInfo.CAMERA_FACING_BACK == cameraFacing) {
                    return 0;
                } else if (RecorderActivity.CAMERA_THIRD == cameraFacing) {
                    return 2;
                } else if (CameraInfo.CAMERA_FACING_FRONT == cameraFacing) {
                    return 1;
                }
            } else if (IS_BACK_RIGHT_ON) {
                if (CameraInfo.CAMERA_FACING_BACK == cameraFacing) {
                    if (true/* jugeFileIsExit("/dev/video1") */) {
                        return 1;
                    } else {
                        return -1;
                    }
                } else if (RecorderActivity.CAMERA_THIRD == cameraFacing) {
                    if (true/* jugeFileIsExit("/dev/video4") */) {
                        return 2;
                    } else {
                        return -1;
                    }
                } else if (CameraInfo.CAMERA_FACING_FRONT == cameraFacing) {
                    if (true/* jugeFileIsExit("/dev/video0") */) {
                        return 0;
                    } else {
                        return -1;
                    }
                }
            } else {
                if (RecorderActivity.CAMERA_THIRD == cameraFacing) {
                    if (jugeFileIsExit("/dev/video1")) {
                        return 1;
                    } else {
                        return -1;
                    }
                } else if (CameraInfo.CAMERA_FACING_BACK == cameraFacing) {
                    if (jugeFileIsExit("/dev/video4")) {
                        return 2;
                    } else {
                        return -1;
                    }
                } else if (CameraInfo.CAMERA_FACING_FRONT == cameraFacing) {
                    if (jugeFileIsExit("/dev/video0")) {
                        return 0;
                    } else {
                        return -1;
                    }
                }
            }
        }
        return -1;
    }

    public void setMiniMode(int isMiniMode) {
        mMiniMode = isMiniMode;
        if (mMiniMode == MINI_MODE_NONE) {
            setRecQualityDefault(mVideoQuality);
            setPicQualtiy(mImageQuality);
        }
    }

    public interface IMiniPictureTakenListener {
        public void onMiniPictureTaken(String fileName, int cameraId);
    }

    public void setMiniPictureTakenListener(IMiniPictureTakenListener ls) {
        mMiniPictureTakenListener = ls;
    }

    public interface IMiniVideoTakenListener {
        public void onMiniVideoTaken(String fileName, int cameraId);

        public void onStopForMini(int cameraId);
    }

    public void setMiniVideoTakenListener(IMiniVideoTakenListener ls) {
        mMiniVideoTakenListener = ls;
    }

    public void setMiniSize(int width, int height) {
        mMiniWidth = width;
        mMiniHeight = height;
    }

    public void setMiniDuaration(int duaration) {
        mMiniduaration = duaration;
    }

    public void handleLockOneFile() {
        mLockFlag = LOCK_FLAG_2MIN;
        if (mRecordCallback != null) {
            mRecordCallback.onLocked(true);
        }
    }

    public boolean checkSize(int width, int height) {
        Log.d(TAG, "checkSize width =" + width + ";height" + height);
        if (mCamera == null) {
            return false;
        }

        if (mParameters == null) {
            mParameters = mCamera.getParameters();
        }
        List<Size> sizes = mParameters.getSupportedPreviewSizes();
        for (int i = 0; i < sizes.size(); i++) {
            Size sz = sizes.get(i);
            Log.d(TAG, i + "s width=" + sz.width + ";height" + sz.height);
            if (height == sizes.get(i).height && width == sizes.get(i).width) {
                return true;
            }
        }
        return false;
    }

    public boolean waitDone() {
        if (mRecorderHandler == null) {
            return false;
        }
        return mRecorderHandler.waitDone();
    }

    private String getWaterMarkString() {
        String waterMark = "";
        Log.d("cdong",
                "mCameraId = " + mCameraId + " CameraInfo.CAMERA_FACING_FRONT = " + CameraInfo.CAMERA_FACING_FRONT);
        if (mDesiredPreviewWidth == 1280) {
            Log.i("cdong", "getWaterMarkString QUALITY_720P");
            waterMark = (mDesiredPreviewWidth / 3) + ",15,0," + (mDesiredPreviewWidth / 8) + ", " +
                    "15, ";
        } else if (mDesiredPreviewWidth == 1920) {
            Log.i("cdong", "getWaterMarkString QUALITY_1080P");
            waterMark = (mDesiredPreviewWidth / 2) + ",20,0," + (mDesiredPreviewWidth / 4) + ", " +
                    "20, ";
        } else {
            Log.i("cdong", "getWaterMarkString QUALITY_1296P");
            waterMark = (mDesiredPreviewWidth / 2) + ",15,0," + (mDesiredPreviewWidth / 4) + ", " +
                    "15, ";
        }

        if (mCameraId == CameraInfo.CAMERA_FACING_FRONT && mDesiredPreviewWidth_src == 1280
                && mDesiredPreviewWidth == 1920) {
            Log.i("cdong", "getWaterMarkString change to QUALITY_720P");
            waterMark =
                    (mDesiredPreviewWidth_src / 3) + ",15,0," + (mDesiredPreviewWidth_src / 8) + ", 15, ";
        }

        return waterMark;
    }

    private String getGPStWaterMarkString() {
        String waterMark = "";
        if (mDesiredPreviewWidth == 1280) {
            Log.i(TAG, "geGPStWaterMarkString QUALITY_720P");
            waterMark = (mDesiredPreviewWidth / 3) + "," + (mDesiredPreviewHeight / 15) + ",";
        } else if (mDesiredPreviewWidth == 1920) {
            Log.i(TAG, "geGPStWaterMarkString QUALITY_1080P");
            waterMark = (mDesiredPreviewWidth / 3) + "," + (mDesiredPreviewHeight / 15) + ",";
        } else {
            Log.i(TAG, "geGPStWaterMarkString QUALITY_1296P");
            waterMark = (mDesiredPreviewWidth / 3) + "," + (mDesiredPreviewHeight / 15) + ",";
        }
        if (mCameraId == CameraInfo.CAMERA_FACING_FRONT && mDesiredPreviewWidth_src == 1280
                && mDesiredPreviewWidth == 1920) {
            Log.i("cdong", "getWaterMarkString change to QUALITY_720P");
            waterMark =
                    (mDesiredPreviewWidth_src / 3) + ",0,0," + (mDesiredPreviewWidth_src / 8) + ", 0, ";
        }
        return waterMark;
    }

    public void setCameraPlug(boolean isOut) {
        if (mRecordCallback != null) {
            mRecordCallback.onCameraPlug(isOut);
        }
    }

    public boolean isCanMini() {
        if (mCamera != null && mIsPreview) {
            return true;
        } else {
            return false;
        }
    }

    public void setSaveMediaDelay(boolean isDelay) {
        mIsSaveMediaDelay = isDelay;
    }
}
