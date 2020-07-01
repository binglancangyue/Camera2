package com.android.camera.v66;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ActivityManager.RecentTaskInfo;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.hardware.Camera.Adas;
import android.hardware.Camera.AdasDetectionListener; // adas
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Size;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.usb.UsbCameraManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.camera.CameraActivity;
import com.android.camera.OnScreenHint;
import com.android.camera.Storage;
import com.android.camera.app.CameraApp;
import com.android.camera.data.CameraDataAdapter;
import com.android.camera.data.LocalData;
import com.android.camera.data.LocalDataAdapter;
import com.android.camera.util.CameraUtil;
import com.android.camera.v66.AccMonitor.IAccDownListener;
import com.android.camera.v66.AccMonitor.IAccWakeListener;
import com.android.camera.v66.CarSpeedMonitor.ISpeedChangeListener;
import com.android.camera.v66.FastReverseChecker.IFastReverseListener;
import com.android.camera.v66.GsensorWakeUpMonitor.IGsensorWakeListener;
import com.android.camera.v66.MyPreference.IParkingCrashSensityChanged;
import com.android.camera.v66.ShutdownWindow.IShutdownStatusListener;
import com.android.camera.v66.WrapedRecorder.IMiniPictureTakenListener;
import com.android.camera.v66.WrapedRecorder.IMiniVideoTakenListener;
import com.android.camera.v66.WrapedRecorder.IRecordCallback;
import com.android.camera2.R;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
//add by chengyuzhou 
import android.content.SharedPreferences;

import com.android.camera.v66.MyPreference.IMiddleVideoChanged;

//end
public class RecordService extends Service implements IFastReverseListener, IAccWakeListener,
        IAccDownListener,
        IGsensorWakeListener, ISpeedChangeListener, IMiniVideoTakenListener,
        IMiniPictureTakenListener {

    public static final String TAG = "RecordService";
    public static final String PACKAGE_NAME = "com.android.camera2";
    public static long mRecordNeedSpace = 800 * 1024 * 1024;// 1G
    public static long mRecordExitSpace = 600 * 1024 * 1024;// 600M
    public static final String IMPACT_SUFFIX = "_impact";
    // public static final String SUBFIX_1_MIN = "_1min";
    // public static final String SUBFIX_2_MIN = "_2min";
    // public static final String SUBFIX_3_MIN = "_3min";
    public static final int FLAG_ACTIVITY_RUN_IN_RIGHT_WINDOW = 0x00000200;
    public static final String POWER_ON_START = "powerOnStart";
    public static final String EXTRA_CAM_TYPE = "REQ_CAM_TYPE";
    public static final String EXTRA_NO_ADAS_FLAG = "REQ_ADAS_FLAG";
    public static final String IS_OPEN_AGAIN = "IS_OPEN_AGAIN";
    public static final String IS_EXIT = "IS_FINISH_ACTIVITY";
    public static final String ACTION_NAVIGATION_CAMERA_CLOSE_NEED_CHANGED = "android.intent" +
            ".action.NAVIGATION_CAMERA_CLOSE_NEED_CHANGED";
    public static final int MW_INVALID_STACK_WINDOW = -1;
    public static final int MW_NORMAL_STACK_WINDOW = 0x0;
    public static final int MW_MAX_STACK_WINDOW = 0x1;
    public static final int MW_MIN_STACK_WINDOW = 0x2;

    public static int ERR_FAIL = 0; // unknow error
    public static int ERR_OK = 1; // sucess
    public static int ERR_NO_TF = 2; // T card uninserted
    public static int ERR_NO_FRONT_CAMERA = 3; // no front camera
    public static int ERR_NO_REAR_CAMERA = 4; // no back camera
    public static int ERR_FILE_TOO_LARGE = 5; // vide file > 4 M
    public static int ERR_OLD_REAR_CAMERA = 6; // rear camera unsupported size

    public static final String ACTION_TAKE_SNAPSHOT = "com.spreadwin.camera.snapshot";
    public static final String ACTION_START_VIDEO = "com.spreadwin.camera.startvideo";
    public static final String ACTION_STOP_VIDEO = "com.spreadwin.camera.stopvideo";
    public static final String ACTION_LOCK_VIDEO = "com.spreadwin.camera.savevideo";
    public static final String ACTION_START_ADAS = "com.spreadwin.camera.startadas";
    public static final String ACTION_STOP_ADAS = "com.spreadwin.camera.stopadas";
    public static final String ACTION_SET_ALARM = "com.spreadwin.camera.set.alarm";
    public static final String ACTION_START_APP = "android.media.action.IMAGE_CAPTURE";
    public static final String ACTION_STOP_APP = "com.spreadwin.camera.finish";
    public static final String ACTION_SNAP_OVER = "com.spreadwin.camera.snapover";
    public static final String ACTION_REQUEST_STATUS = "com.spreadwin.camera.requeststatus";
    public static final String ACTION_REPLY_STATUS = "com.spreadwin.camera.replystatus";
    public static final String ACTION_SHOW_REAR = "com.spreadwin.camera.showrear";
    public static final String ACTION_HOME_PRESS = "android.intent.action.HOME_PRESS";
    public static final String ACTION_RECORD_FILES_UPDATE = "com.spreadwin.camera.file.update";
    public static final String ACTION_TEMP_ACTION = "com.android.action.TEMP_ACTION";
    public static final String ACTION_KILLSELF_ACTION = "com.zqc.action.KILL_MYSELF";
    public static final String ACTION_SHOW_STREAM_MEDIA_WINDOW = "com.zqc.action" +
            ".SHOW_STREAM_MEDIA_WINDOW";
    public static final String ACTION_HIDE_STREAM_MEDIA_WINDOW = "com.zqc.action" +
            ".HIDE_STREAM_MEDIA_WINDOW";
    private static final String ACTION_CLOSE_STREAM_MEDIA_WINDOW = "com.zqc.action" +
            ".CLOSE_STREAM_MEDIA_WINDOW";
    public static final String ACTION_HIDE_STREAM_PREVIEW_WINDOW = "com.zqc.action" +
            ".HIDE_STREAM_PREVIEW_WINDOW";
    public static final String ACTION_SHOW_STREAM_PREVIEW_WINDOW = "com.zqc.action" +
            ".SHOW_STREAM_PREVIEW_WINDOW";
    public static final String ACTION_SHOW_LEFT_RIGHT_PREVIEW = "com.zqc.action" +
            ".SHOW_LEFT_RIGHT_PREVIEW";
    public static final String ACTION_TOUCH_SCREEN = "com.zqc.action.CLEARTIME";
    public static final String ACTION_SHOW_AFTER_SECONDS = "com.zqc.action.show.after.seconds";
    private static final String ACTION_RECORD_SHOW = "com.txznet.txz.record.show";
    private static final String ACTION_RECORD_DISMISS = "com.txznet.txz.record.dismiss";
    private static final String ACTION_VIDEO_LOCK = "com.action.other_Text";
    private static final String ACTION_VOICE_WAKEUP = "com.zqc.action.voice_wakeup";
    private static final String ACTION_SPEED = "android.intent.action.CAMERA_SPEED";
    public static final String PERFORMANCE_TEMP_CUR = "TEMP_CUR";
    public static final String PERFORMANCE_TEMP_ACT = "TEMP_ACT";
    public static final String PERFORMANCE_TEMP_HIGH = "HIGH";
    public static final String PERFORMANCE_TEMP_MIDDLE = "MIDDLE";
    public static final String PERFORMANCE_TEMP_NORMAL = "NORMAL";
    public static final String PERFORMANCE_TEMP_LOW = "LOW";
    public static final String VOICE_ACTION_CLOSE_TAPE = "TXZ_ACTION_CLOSE_TAPING";
    public static final String VOICE_ACTION_OPEN_TAPE = "TXZ_ACTION_OPEN_TAPING";

    public static final String VOICE_ACTION_OPEN_FULL_CAMERA = "com.zqc.action.show.full.camera";
    public static final String VOICE_ACTION_CLOSE_FULL_CAMERA = "com.zqc.action.disShow.full" +
            ".camera";
    public static final String ACTION_VOICE_RETURN_LAUNCHER = "com.zqc.action.return.launcher";
    public static final String ACTION_SPEAK_TEXT = "com.action.other_Text";
    public static final String ACTION_RECORD_STARTED = "com.zqc.action.startCameraRecord";
    public static final String ACTION_RECORD_STOPPED = "com.zqc.action.stopCameraRecord";

    public static final String ACTION_SHOW_FLOAT_WINDOW = "action_SHOW_FLOAT_WINDOW";
    private static final String ACTION_TXZ_SEND = "com.txznet.adapter.send";
    public static String txzLookAhead = "look.ahead";//看前面
    public static String txzLookBehind = "look.behind";//看后面
    public static String txzLookAheadClose = "look.ahead.close";//关闭前录
    public static String txzLookBehindClose = "look.behind.close";//关闭后录
    public static String txzRecordOpen = "record.open";//打开录音
    public static String txzRecordClose = "record.close";//关闭录音

    public static final String ACTION_SHUTDOWN = "android.intent.action.ACTION_SHUTDOWN";
    public static final String ACTION_QUIT_SCREEN_SAVER = "ACTION_SCREENSAVER_CLOSE";
    public static final String SCREEN_SAVER_PROPERTIY = "persist.sys.screen_saver";

    public static final String ACTION_DREAM_STOP = "zqc.action.dream_stop";
    public static final String ACTION_DREAM_START = "zqc.action.dream_start";
    public static final String RESET_CAMERA_NODE = "/sys/class/usb_host_restart/uvc_recovery";

    public static final int CHECK_GSENSOR_DELAY = 20;
    public static final int MAX_DEVICES_NUM = 10;
    public static final int MAX_TRY_NUM = 10;
    public static final int MSG_ON_GSENSOR_UP = 1001;
    public static final int SENSITY_HIGHT = 5;
    public static final int SENSITY_MEDIUM = 10;
    public static final int SENSITY_LOW = 20;
    public static final long GSENSOR_RECORD_DELAY = 15 * 1000;
    public static final String INPUT_DEVICE_PATH = "/sys/devices/virtual/input/input";
    public static final String GSENSOR_DEVICE_NAME = "da380";
    public static final String GSENSOR_NAME_NODE = "/name";
    public static final String GSENSOR_ENABLE_NODE = "/enable";
    public static final String GSENSOR_DELAY_NODE = "/delay";
    public static final String GSENSOR_DATA_NODE = "/axis_data";
    public static final String GSENSOR_IRQ_NODE = "/int2_enable";
    public static final String GSENSOR_IRQ_CLEAR_NODE = "/int2_clear";
    public static final String GSENSOR_SENSITY_NODE = "/threshold";
    public static final String GSENSOR_STATUS_NODE = "/status";

    public static final int CAMERA_ID_FRONT = 100;
    private static final int MSG_CHECK_CAMERA_OPENED = 213;

    private static final String FILE_ACC_STATUS = "/sys/class/gpio_acc/io_status";
    private static final String FILE_ACC_WAKE = "/sys/class/gpio_acc/irq_status";
    private static final int STATUS_TRUE = 1;
    private static int KEEP_DOWN_TIME = 30;

    // private static final String FILE_TVD_STATUS =
    // "/sys/class/switch/tvd_lock0/state";
    private static final String FILE_RIGHT_INSERT_STATUS = "/sys/class/switch/tvd_lock0/state";
    //private static final String FILE_BACK_INSERT_STATUS =
    // "/sys/devices/platform/pr2000/cm_pr2000_lock";
    private static final String FILE_BACK_INSERT_STATUS = "/sys/class/switch/tvd_lock0/state";
    private static final String FILE_TVD_STATUS = "/sys/devices/platform/qc_gpio/cm_detect";
    private static final String FILE_TVD_SYSTEM = "/sys/class/switch/tvd_system0/status";

    private static final int ADAS_LEVEL_1 = 0;
    private static final int ADAS_LEVEL_2 = 1;
    private static final int ADAS_LEVEL_3 = 3;
    private static final int REC_QUA_720P = 0;
    private static final int REC_QUA_1080P = 1;
    private static final int REC_QUA_1296P = 2;

    private static final int MSG_START_SPACE_CHECK = 800;
    private static final int MSG_START_CLEAR_FILE = 801;
    private static final int MSG_START_SWTICH_FILE = 802;
    private static final int MSG_WAITING_ACTIVITY_FINISH = 803;
    private static final int MSG_WAITING_ACTIVITY_UP = 804;
    private static final int MSG_WAITTING_MINI_OVER = 805;
    private static final int MSG_PLAY_SHUTTER_CLICK = 806;
    private static final int MSG_CHECK_CAN_RECORD = 807;
    private static final int MSG_CHECK_ACC_STATUS = 812;
    private static final int MSG_CHECK_ACC_WAKE = 813;
    private static final int MSG_CHECK_GSENSOR_STATUS = 814;
    private static final int MSG_GSENSOR_WAKE = 815;
    private static final int MSG_GSENSOR_ENABLE = 816;
    private static final int MSG_GSENSOR_SET_SENSITY = 817;
    private static final int MSG_GSENSOR_WAKE_OVER = 818;
    private static final int MSG_CHECK_TVD = 819;
    private static final int MSG_CHECK_RIGHT_INSERT = 888;
    private static final int MSG_CHECK_BACK_INSERT = 8888;
    private static final int MSG_ON_TVD_CHANGE = 820;
    private static final int MSG_ON_RIGHT_CHANGE = 889;
    private static final int MSG_ON_BACK_CHANGE = 8899;
    private static final int MSG_RESET_CAMERA = 821;
    private static final int MSG_WAIT_CAMERA_DONE = 822;
    private static final int MSG_WAIT_DONE_TO_START = 823;
    private static final int MSG_UI_HANDLE_BACK_OUT = 824;
    private static final int MSG_BROADCAST_FILE_UPDATE = 825;
    private static final int MSG_RECEIVED_HOME_DELAY = 826;
    private static final int MSG_SD_HANDLE_DELAY = 827;
    private static final long SD_HANDLE_DELAY = 2000;
    private static final long SPACE_CHECK_DELAY = 5000;
    private static final long SWTICH_FILE_DELAY = 500;
    private static final long CHECK_ACC_DELAY = 1000;
    private static final long CHECK_TVD_DELAY = 1000; //500
    private static final long CHECK_SPACK_DELAY = 30 * 1000;
    private static final int MSG_NO_SPACE = 200;
    private static final int MSG_CHECK_DATA_ADAPTER = 201;
    private static final int MSG_SHOW_STREAM_MEDIA_WINDOW = 203;
    private static final int MSG_HIDE_STREAM_MEDIA_WINDOW = 204;
    private static final int MSG_CLOSE_STREAM_MEDIA_WINDOW = 205;
    private static final int MSG_HIDE_STREAM_PREVIEW_WINDOW = 206;
    private static final int MSG_SHOW_STREAM_PREVIEW_WINDOW = 207;
    private static final int MSG_CLOSE_STREAM_PREVIEW_WINDOW = 2008;
    private static final int MAX_TRY_RESET_TIME = 3;
    private static final long TRY_RESET_DELAY = 0;
    private static final long BROADCASE_FILE_DELAY = 3000;

    private final IBinder mBinder = new LocalBinder();
    private final List<WrapedRecorder> mRecorderList =
            Collections.synchronizedList(new ArrayList<WrapedRecorder>());
    private final List<StateKeeper> mStateKeeperList =
            Collections.synchronizedList(new ArrayList<StateKeeper>());
    private final List<StateKeeper> mSdStateKeeperList =
            Collections.synchronizedList(new ArrayList<StateKeeper>());
    private final List<StateKeeper> mSleepStateKeeperList =
            Collections.synchronizedList(new ArrayList<StateKeeper>());
    private final List<ISpeedChangeListener> mSpeedListenerList = Collections
            .synchronizedList(new ArrayList<ISpeedChangeListener>());
    private final List<IServiceListener> mServiceListenerList = Collections
            .synchronizedList(new ArrayList<IServiceListener>());
    private SensorManager mGsensor;
    private CameraDataAdapter mDataAdapter;
    private Handler mRecorderHandler;
    private Handler mMainHandler;
    private boolean mStartSpaceCheck = false;
    private DoubleFloatWindow mFloatWindow;
    private TwoCameraPreviewWin mTwoCamPreWin;
    private TwoFloatWindow twoFloatWindow;
    private TextureView mBackTextureView;
    private TextureView mFrontTextureView;
    private int mFloatId = 0;
    private int mStartId = -1;
    private boolean mIsBackTextureReady = false;
    private boolean mIsFrontTextureReady = false;
    private Intent mLastIntent = null;
    private ComponentName mLastComponent = null;
    private int mLastTaskId = -1;
    private long mStartTime = 0;
    private long mDuartion = 0;
    private boolean mIsChangingFloat = false;
    private boolean mIsHomePressing = false;
    private long mLastHomePressed = 0;
    private long mLastStartActivity = 0;
    private boolean mIsFastRevers = false;
    private FastReverseChecker mFastReverseChecker;
    private PowerManager mPowerManager;
    private PowerManager.WakeLock mWakeLock;
    // private AccMonitor mAccMonitor;
    // private GsensorWakeUpMonitor mGsensorWakeUpMonitor;
    private boolean mFrontOnLeft = false;
    private BroadcastReceiver mReceiver = null;
    private CarSpeedMonitor mCarSpeedMonitor;
    private StreamMediaWindow streamMediaWindow;
    private StreamPreViewWindow streamPreviewWindow;

    private boolean mIsPreRecording = false;
    private boolean mIsMiniVideo = false;
    private boolean mIsMiniMode = false;
    private int mMiniError = -1;
    private boolean mIsMiniFrontStoped = false;
    private boolean mIsMintBackStoped = false;
    private boolean mIsMiniFrontTaken = false;
    private boolean mIsMiniBackTaken = false;
    private boolean mIsMiniAdasOn = false;
    private String mMiniFrontFile;
    private String mMiniBackFile;
    private int mMiniduaration;
    private BroadcastReceiver mSpreadReceiver = null;
    private SoundPool mSoundPool;
    private boolean mIsSoundLoaded = false;
    private int mSoundId = -1;

    private long mLastSnapShot = 0;
    private boolean mIsBackCamOut = false;
    private boolean mIsSDOut = false;
    private boolean mIsLockOnce = false;
    private Toast mToast = null;
    private boolean mIsCanRecord = true;
    private boolean mIsHideLines = false;
    private long mCurTime = 0;

    private boolean mLastAccStatus = false;
    private boolean mSecurityMode = false;
    private int mAccCount = 0;
    private ShutdownWindow mShutdownWindow;
    private boolean mIsSleeping = false;
    private boolean mGsensorWake = false;
    private String mGsensorInputPath = null;
    private int mGsensorFindInputTime = 0;
    private boolean mIsQuickShow = false;
    private boolean mIsAskResetCamera = false;
    private boolean mIsPlugRecording = false;
    public boolean mIsMuteOn = false;
    public boolean mIsHasRevered = false;
    private MyPreference pref;
    public static String mTopActivityPackName = "";
    public static boolean isOnSm = false;

    // add by chengyuzhou
    private SharedPreferences preferences, mpreferences, myClickPre;
    private SharedPreferences.Editor editor;
    private boolean isFirstCheck = true;
    private boolean isFirstCheckRight = true;
    private boolean isFirstCheckBack = true;
    private boolean isSupportStreamMedia = SystemProperties.getBoolean("ro.sys.stream.media",
            false);
    private boolean isSupportBackCameraFullScreen = SystemProperties.getBoolean("ro.sys.back" +
            ".camera.fullscreen", false);
    private boolean isFastTest = SystemProperties.getBoolean("ro.se.settings.fasttest", false);
    private boolean isNeedCheckRear = SystemProperties.getBoolean("ro.se.settings" +
            ".isneedcheckrear", true);
    public static int LAYOUT_TYPE = SystemProperties.getInt("ro.se.qchome.layouttype", 0);
    public static final boolean SPLITSCREEN_SEVEN = SystemProperties.getBoolean("persist.sys" +
            ".splitscreen7", false);
    // end
    // add by Jenchar
    // Function:每次开机
    // ADAS功能都是关闭的，ro.sys.boot_isClose_adas配置需不需要要这个功能，false就是不需要，true就是需要
    public static boolean isBootCloseAdas = SystemProperties.getBoolean("ro.sys.boot_isClose_adas"
            , false);
    public static boolean isFirstBoot = true;

    public static boolean getIsFirstBoot() {
        return isFirstBoot;
    }

    public static void setIsFirstBoot(boolean firstBoot) {
        isFirstBoot = firstBoot;
    }

    // end by Jenchar
    private SharedPreferences myPreSp;
    private SharedPreferences myPreCameraPlug;
    private SensorEventListener mGsensorListener = new SensorEventListener() {
        public void onSensorChanged(SensorEvent sensorEvent) {
            // Log.d(TAG, "GsensorChanger");
            for (int i = 0; i < mRecorderList.size(); i++) {
                mRecorderList.get(i).onSensorChanged(sensorEvent);
            }
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return mBinder;
    }

    public class LocalBinder extends Binder {
        public RecordService getService() {
            return RecordService.this;
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "....onStartCommand...., flag = " + flags + "startId = " + startId);
        return START_STICKY;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        mGsensor = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mGsensor.registerListener(mGsensorListener,
                mGsensor.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_FASTEST);
        mDataAdapter =
                new CameraDataAdapter(new ColorDrawable(getResources().getColor(R.color.photo_placeholder)));
        HandlerThread ht = new HandlerThread("RecordService Handler Thread");
        ht.start();
        mRecorderHandler = new RecorderHandler(ht.getLooper());
        mMainHandler = new MyMainHanlder();
        // mMainHandler.sendEmptyMessage(MSG_CHECK_DATA_ADAPTER);
        Uri uri = Uri.fromFile(new File(Storage.DIRECTORY));
        Intent it = new Intent();
        it.setData(uri);
        it.setAction(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        sendBroadcast(it);
        mCarSpeedMonitor = new CarSpeedMonitor(this);
        mCarSpeedMonitor.setSpeedChangeListener(this);
        mCarSpeedMonitor.startSpeedDection();
        mFastReverseChecker = new FastReverseChecker(this);
        mFastReverseChecker.setFastReverseListener(this);
        mFastReverseChecker.start();
        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        mWakeLock.acquire();
        mSecurityMode = SystemProperties.getBoolean("persist.sys.security_mode", true);
        mShutdownWindow = new ShutdownWindow(this);
        mShutdownWindow.setScreenStatusListener(new IShutdownStatusListener() {
            @Override
            public void onShutdown() {
                Log.d(TAG, "onShutdown");
                if (mSecurityMode) {
                    mShutdownWindow.onHide();
                    onAccSleep();
                } else {
                    shutdown();
                }
            }
        });
        /*
         * if (SystemProperties.getBoolean("ro.sys.GaurdMode", true)) {
         * mRecorderHandler.sendEmptyMessageDelayed(MSG_CHECK_ACC_STATUS,
         * CHECK_ACC_DELAY);
         * mRecorderHandler.sendEmptyMessageDelayed(MSG_CHECK_GSENSOR_STATUS,
         * CHECK_ACC_DELAY); }
         * mRecorderHandler.sendMessageDelayed(mRecorderHandler.obtainMessage(
         * MSG_CHECK_TVD, 1, 0), CHECK_TVD_DELAY);
         */
        /*
         * mRecorderHandler.sendMessageDelayed(mRecorderHandler.obtainMessage(
         * MSG_CHECK_RIGHT_INSERT, 1, 0), 1000);
         * */
        if (RecorderActivity.CAMERA_COUNT == 2) {
            mRecorderHandler.sendMessageDelayed(mRecorderHandler.obtainMessage(
                    MSG_CHECK_BACK_INSERT, 1, 0), 1000);
        }

        pref = MyPreference.getInstance(this);
        myPreSp = getSharedPreferences("PriSP", Context.MODE_PRIVATE);
        myPreSp.edit().putBoolean("isGsonLock", false).commit();
        myPreCameraPlug = getSharedPreferences("cameraPlug",
                Context.MODE_MULTI_PROCESS | Context.MODE_WORLD_READABLE);
        pref.setParkingCrashSensityChangedListener(new IParkingCrashSensityChanged() {

            @Override
            public void onParkingCrashSensityChanged(int value) {
                // TODO Auto-generated method stub
                // mGsensorWakeUpMonitor.setSensity(value);
            }

        });
        /*
         * pref.setMiddleVideoChangerListener(new IMiddleVideoChanged(){
         *
         * public void onMiddleVideoChanged(boolean state){ if(state){
         * mFloatWindow.setThirdImagerIsShow(false);
         * mRecorderList.get(2).startPreview();
         * mRecorderList.get(2).startRender(); editor.putBoolean("isShowThird",
         * true).commit(); if(mpreferences.getBoolean("isVideo", true)){ //
         * mRecorderList.get(2).setLockFlag(true);
         * mRecorderList.get(2).startRecording(); }else{ //
         * mRecorderList.get(2).setLockFlag(false); //
         * mRecorderList.get(2).startRecording(); //
         * mRecorderList.get(2).setLockFlag(false); }
         * mFloatWindow.setOnclickListener(new FloatWinListener(1), 1); }else{
         * mFloatWindow.setThirdImagerIsShow(true); //
         * mRecorderList.get(2).setLockFlag(true);
         * mRecorderList.get(2).stopRender(); //
         * mRecorderList.get(2).stopPreview();
         * mFloatWindow.setOnclickListener(null, 1);
         * editor.putBoolean("isShowThird", false).commit();
         * mRecorderList.get(2).stopRecording(); } } });
         */
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MEDIA_EJECT);
        intentFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        intentFilter.addDataScheme("file");
        mReceiver = new MyBroadcastReceiver();
        registerReceiver(mReceiver, intentFilter);
        IntentFilter usbCameraFilter = new IntentFilter();
        usbCameraFilter.addAction(UsbCameraManager.ACTION_USB_CAMERA_PLUG_IN_OUT);
        usbCameraFilter.addAction(ACTION_TEMP_ACTION);
        registerReceiver(mReceiver, usbCameraFilter);

        IntentFilter spreadFilter = new IntentFilter(ACTION_TAKE_SNAPSHOT);
        spreadFilter.addAction(ACTION_LOCK_VIDEO);
        spreadFilter.addAction(ACTION_START_VIDEO);
        spreadFilter.addAction(ACTION_STOP_VIDEO);
        spreadFilter.addAction(ACTION_SET_ALARM);
        spreadFilter.addAction(ACTION_REQUEST_STATUS);
        spreadFilter.addAction(ACTION_SHOW_REAR);
        spreadFilter.addAction(ACTION_HOME_PRESS);
        spreadFilter.addAction(ACTION_SHUTDOWN);
        spreadFilter.addAction(ACTION_STOP_APP);
        spreadFilter.addAction(ACTION_RECORD_FILES_UPDATE);
        spreadFilter.addAction(ACTION_DREAM_STOP);
        spreadFilter.addAction(ACTION_DREAM_START);
        spreadFilter.addAction(ACTION_KILLSELF_ACTION);
        // spreadFilter.addAction(ACTION_SHOW_STREAM_MEDIA_WINDOW);
        // spreadFilter.addAction(ACTION_HIDE_STREAM_MEDIA_WINDOW);
        // spreadFilter.addAction(ACTION_CLOSE_STREAM_MEDIA_WINDOW);
        spreadFilter.addAction(ACTION_HIDE_STREAM_PREVIEW_WINDOW);
        spreadFilter.addAction(ACTION_SHOW_STREAM_PREVIEW_WINDOW);
        spreadFilter.addAction(VOICE_ACTION_CLOSE_TAPE);
        spreadFilter.addAction(VOICE_ACTION_OPEN_TAPE);
        spreadFilter.addAction(ACTION_TOUCH_SCREEN);
        // spreadFilter.addAction(ACTION_SHOW_AFTER_SECONDS);
        spreadFilter.addAction(ACTION_RECORD_SHOW);
        spreadFilter.addAction(ACTION_RECORD_DISMISS);
        spreadFilter.addAction(ACTION_VIDEO_LOCK);
        spreadFilter.addAction(ACTION_VOICE_WAKEUP);
        spreadFilter.addAction(ACTION_SHOW_LEFT_RIGHT_PREVIEW);
        spreadFilter.addAction(VOICE_ACTION_OPEN_FULL_CAMERA);
        spreadFilter.addAction(VOICE_ACTION_CLOSE_FULL_CAMERA);
        spreadFilter.addAction(ACTION_VOICE_RETURN_LAUNCHER);
        spreadFilter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        mSpreadReceiver = new SpreadActionReceiver();
        registerReceiver(mSpreadReceiver, spreadFilter);
        // add by chenyuzhou
        preferences = getSharedPreferences("isShowThird", Context.MODE_PRIVATE);
        mpreferences = getSharedPreferences("isVideo",
                Context.MODE_MULTI_PROCESS | Context.MODE_WORLD_WRITEABLE);


        editor = preferences.edit();
        myClickPre = getSharedPreferences("CameraID", Context.MODE_PRIVATE);
        mIsMuteOn = pref.isMute();
        Boolean isshow = preferences.getBoolean("isShowThird", true);
        // end
        if (RecorderActivity.CAMERA_COUNT == 2) {
            mFloatWindow = new DoubleFloatWindow(this, isshow);
            mFloatWindow.startFloat();
        }
        // mTwoCamPreWin = new TwoCameraPreviewWin(this);
        // twoFloatWindow = new TwoFloatWindow(this);
        // streamMediaWindow = new StreamMediaWindow(this);
        streamPreviewWindow = new StreamPreViewWindow(this);

        if (RecorderActivity.CAMERA_COUNT == 2) {
            if (preferences.getBoolean("isShowThird", true)) {
                mFloatWindow.setThirdImagerIsShow(false);
                for (int i = 0; i < DoubleFloatWindow.CAM_NUM; i++) {
                    mFloatWindow.hide(i); //
                    //mFloatWindow.startFloatDisplay();
                    if (!CustomValue.ONLY_ONE_CAMERA){
                        mFloatWindow.setOnclickListener(new FloatWinListener(i), i);
                    }
                }
            } else {
                mFloatWindow.setOnclickListener(new FloatWinListener(0), 0); //
                mFloatWindow.setOnclickListener(new FloatWinListener(1), 1);

            } //add by chengyuzhou if
            if (SystemProperties.getInt("ro.sys.float_camera", 1) == 2) {
                mFloatWindow.setOnLongclickListener(new FloatWinLongListener());
            }
        }

        // end
        IntentFilter intent = new IntentFilter();
        // intent.addAction(ACTION_HOME_PRESS);
        intent.addAction("android.intent.action.CAMERA_RECORD");
        intent.addAction("android.intent.action.CAMERA_SNAPSHOT");
        intent.addAction("CLOSE_VIDEO");
        intent.addAction("LOCK_VIDEO");
        intent.addAction("TXZ_START_RECORD");
        intent.addAction("MUTE_WILLBE_OPEN");
        intent.addAction("MUTE_WILLBE_CLOSE");
        intent.addAction(ACTION_SHOW_FLOAT_WINDOW);
        intent.addAction(ACTION_TXZ_SEND);
        // intent.addAction("android.intent.action.MEDIA_MOUNTED");
        // intent.addDataScheme("file");
        intent.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        registerReceiver(previewReceiver, intent);

        mSoundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        mSoundPool.setOnLoadCompleteListener(new OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool sp, int id, int status) {
                // TODO Auto-generated method stub
                Log.d(TAG, "sp=" + sp + ";id=" + id + ";status=" + status);
                mIsSoundLoaded = true;
            }
        });
        mSoundId = mSoundPool.load(this, R.raw.photo, 1);
        if (!isCameraAdd(CameraInfo.CAMERA_FACING_FRONT)) {
            addCamera(CameraInfo.CAMERA_FACING_FRONT);
        }
        if (!isCameraAdd(CameraInfo.CAMERA_FACING_BACK)) {
            addCamera(CameraInfo.CAMERA_FACING_BACK);
        }
        /*
         * if (!isCameraAdd(TwoFloatWindow.LEFT_CAMERA_ID)) {
         * addCamera(TwoFloatWindow.LEFT_CAMERA_ID); }
         */
		/* remove by zdt
		 * if (!isCameraAdd(TwoFloatWindow.RIGHT_CAMERA_ID)) {
			addCamera(TwoFloatWindow.RIGHT_CAMERA_ID);
		}*/
        // showTowFloat();

        // if(LAYOUT_TYPE!=6){
        // Log.i(TAG, "sendBroadcast----" + "showQuickStreamMediaWindow");
        // Intent intent2 = new Intent();
        // intent2.setAction("com.zqc.action.SHOW_STREAM_MEDIA_WINDOW");
        // Bundle bundle = new Bundle();
        // bundle.putBoolean("isClickBtn", true);
        // intent2.putExtras(bundle);
        // //intent.putExtra("isFromQcHome", true);
        // sendBroadcast(intent2);
        //
        // new Thread(new Runnable(){
        // @Override
        // public void run(){
        // try {
        // Thread.sleep(15000);
        // Log.i(TAG, "Runnable----" + "sleep ------> 15000");
        // sendBroadcast(new Intent("com.zqc.action.HIDE_STREAM_MEDIA_WINDOW"));
        // } catch (InterruptedException e) {
        // e.printStackTrace();
        // }
        // }
        //
        // }).start();
        // }

        Intent intentGoHome = new Intent();
        intentGoHome.setAction(Intent.ACTION_MAIN);// "android.intent.action.MAIN"
        intentGoHome.addCategory(Intent.CATEGORY_HOME); //"android.intent.category
        intentGoHome.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        // .HOME"
        CameraApp.cameraApp.startActivity(intentGoHome);

    }
    public DoubleFloatWindow getFloatWindow(){
        return mFloatWindow;
    }

    // add by chengyuzhou
    public void setCanThirdLongClick(Boolean canLongClick) {
        if (RecorderActivity.CAMERA_COUNT == 2 && mFloatWindow != null) {
            if (canLongClick) {
                Log.v(TAG, "---------setLongClick---------");
                if (SystemProperties.getInt("ro.sys.float_camera", 1) == 2) {
                    mFloatWindow.setOnLongclickListener(new FloatWinLongListener());
                }
            } else {
                Log.v(TAG, "---------cancelLongClick---------");
                mFloatWindow.setOnLongclickListener(null);
            }
        }
    }

    class FloatWinLongListener implements View.OnLongClickListener {
        public boolean onLongClick(View v) {
            if (preferences.getBoolean("isShowThird", true)) {

                /*
                 * mFloatWindow.setThirdImagerIsShow(true); //
                 * mRecorderList.get(2).setLockFlag(true);
                 * mRecorderList.get(2).stopRender(); //
                 * mRecorderList.get(2).stopPreview();
                 * mFloatWindow.setOnclickListener(null, 1);
                 * editor.putBoolean("isShowThird", false).commit();
                 * mRecorderList.get(2).stopRecording();
                 */
                pref.saveMiddleVideoState(false);
            } else {
                /*
                 * mFloatWindow.setThirdImagerIsShow(false);
                 * mRecorderList.get(2).startPreview();
                 * mRecorderList.get(2).startRender();
                 * editor.putBoolean("isShowThird", true).commit();
                 * if(mpreferences.getBoolean("isVideo", true)){ //
                 * mRecorderList.get(2).setLockFlag(true);
                 * mRecorderList.get(2).startRecording(); }else{ //
                 * mRecorderList.get(2).setLockFlag(false); //
                 * mRecorderList.get(2).startRecording(); //
                 * mRecorderList.get(2).setLockFlag(false); }
                 * mFloatWindow.setOnclickListener(new FloatWinListener(1), 1);
                 */
                pref.saveMiddleVideoState(true);
            }
            return true;
        }
    }

    // end
    class FloatWinListener implements View.OnClickListener {
        private int mFloatWinId = 0;

        FloatWinListener(int floatWinId) {
            mFloatWinId = floatWinId;
        }

        @Override
        public void onClick(View arg0) {
            // TODO Auto-generated method stub
            Log.d(TAG, "onClick mIsChangingFloat =" + mIsChangingFloat);
            Log.d(TAG, "onClick mStartId =" + mStartId);
            Log.d(TAG, "onClick mFloatId =" + mFloatId);
            Log.d(TAG, "onClick mFloatWinId =" + mFloatWinId);
            if (getFloatCameraId(mFloatWinId) == 2) {
                setCanThirdLongClick(false);
            } else {
                setCanThirdLongClick(true);
            }
            if (mIsChangingFloat || mIsHomePressing || Math.abs(System.currentTimeMillis() - mLastHomePressed) < 600) {
                return;
            }
            mLastStartActivity = System.currentTimeMillis();
            mIsChangingFloat = true;
            Intent intent = null;
            int taskId = -1;
            ActivityManager am =
                    (ActivityManager) RecordService.this.getSystemService(Context.ACTIVITY_SERVICE);
            PackageManager pm = RecordService.this.getPackageManager();
            int leftStack = -1;
            int leftWindowStatus = -1;
            leftStack = SplitUtil.getLeftStackId(RecordService.this);
//            leftStack=2;
            Log.d(TAG, "leftStack=" + leftStack);
            Log.d(TAG, "getFloatCameraId()=" + getFloatCameraId(mFloatWinId));
            ImageView[] appIcon = null;
            if (mFloatWindow != null && mFloatWindow.getAppIconArray() != null) {
                appIcon = mFloatWindow.getAppIconArray();
            }
            if (getFloatCameraId(mFloatWinId) == -1) {
                // setCanThirdLongClick(true);
                mStartId = -1;
                boolean isCameraOn = false;
                RecentTaskInfo ti = SplitUtil.getTopTaskOfStack(RecordService.this, leftStack);
                if (ti != null) {
                    Intent it = ti.baseIntent;
                    ResolveInfo resolveInfo = pm.resolveActivity(it, 0);
                    if ((it.getComponent().getPackageName()).equals(PACKAGE_NAME)) {
                        Log.d(TAG, "camera on left");
                        if (RecorderActivity.CAMERA_COUNT == 2 && mFloatWindow != null) {
                            mFloatWindow.updateWindowSurface();
                        }
                        int leftId = -1;
                        for (int i = mServiceListenerList.size() - 1; i >= 0; i--) {
                            leftId = mServiceListenerList.get(i).onAskLeftCameraId();
                            if (mServiceListenerList.get(i).onAskLeftCameraId() >= 0) {
                                break;
                            }
                        }
                        if (leftId < 0) {
                            Log.d(TAG, "failed to get left camera id");
                        } else {
                            Log.d(TAG, "zdt --- updateWindowSurface");
                            setFloatCameraid(leftId, mFloatWinId);
                            Intent broadcast = new Intent();
                            broadcast.setAction(ACTION_NAVIGATION_CAMERA_CLOSE_NEED_CHANGED);
                            broadcast.putExtra(IS_EXIT, true);
                            RecordService.this.sendBroadcast(broadcast);
                            isCameraOn = true;
                        }
                    }
                }
                if (!isCameraOn) {
                    Log.d(TAG, "must faltal error here isCameraOnt=" + isCameraOn);
                    // to do reset
                    // mFloatId = CameraInfo.CAMERA_FACING_BACK;
                    // startFloat();
                    mIsChangingFloat = false;
                } else {
                    if (mLastIntent != null) {
                        Log.d(TAG, "start pre activity on left");
                        try {
                            mLastIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            RecordService.this.startActivity(mLastIntent);
                        } catch (RuntimeException ex) {
                            ex.printStackTrace();
                        }
                        mLastIntent = null;
                    }
                }

            } else if (getFloatCameraId(mFloatWinId) >= 0) {
                // add by chengyuzhou
                if (getFloatCameraId(mFloatWinId) == 2) {
                    // setCanThirdLongClick(false);

                }
                // end
                mStartId = getFloatCameraId(mFloatWinId);
                Log.d(TAG, "---print mFloatWinId = " + mFloatWinId + " ,mStartId = " + mStartId);
                setFloatCameraid(-1, mFloatWinId);
                if (leftStack <= 0 || (leftStack > 0 && leftWindowStatus == MW_MIN_STACK_WINDOW)) {
                    Log.d(TAG, "launcher on left");
                    if (CustomValue.FULL_WINDOW){
                        test(mFloatWinId);
                    }else{
                        Drawable dw =
                                RecordService.this.getResources().getDrawable(R.drawable.launcher);
                        if (appIcon != null) {
                            appIcon[mFloatWinId].setImageBitmap(null);
                            appIcon[mFloatWinId].setImageDrawable(dw);
                        }
                        setFloatCameraid(-1, mFloatWinId);
                        startFloat(-1, mFloatWinId);
                        if (mFloatWindow != null && appIcon != null) {
                            mFloatWindow.addImageView(appIcon[mFloatWinId], mFloatWinId);
                        }
                        Intent it = new Intent(RecordService.this, RecorderActivity.class);
                        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        // i.putExtra(POWER_ON_START, true);
                        it.putExtra(EXTRA_CAM_TYPE, mStartId);
                        it.putExtra(IS_OPEN_AGAIN, true);
                        RecordService.this.startActivity(it);
                    }
                } else {
                    // setCanThirdLongClick(true);
                    boolean isGetApp = false;
                    RecentTaskInfo ti = SplitUtil.getTopTaskOfStack(RecordService.this, leftStack);
                    if (ti != null) {
                        Intent it = ti.baseIntent;
                        ResolveInfo resolveInfo = pm.resolveActivity(it, 0);
                        Log.d(TAG, "onClick: SplitUtil.getStackId(ti) "+SplitUtil.getStackId(ti));
                        if (SplitUtil.getStackId(ti) == leftStack) {
                            if ((it.getComponent().getPackageName()).equals(PACKAGE_NAME)) {
                                Log.d(TAG, "camera on left");
                                int leftId = -1;
                                for (int i = mServiceListenerList.size() - 1; i >= 0; i--) {
                                    leftId = mServiceListenerList.get(i).onAskLeftCameraId();
                                    if (mServiceListenerList.get(i).onAskLeftCameraId() >= 0) {
                                        break;
                                    }
                                }
                                Log.d(TAG, "camera on left-----leftId=" + leftId);
                                if (leftId < 0) {
                                    Log.d(TAG, "failed to get left camera id");
                                } else if (mFloatWinId == 0 && leftId == RecorderActivity.CAMERA_THIRD
                                        && DoubleFloatWindow.CAM_NUM == DoubleFloatWindow.DEFAULT_NUM) {
                                    Log.d(TAG, "roll third camera");
                                    int bottomId = getFloatCameraId(1);
                                    if (bottomId == -1) {
                                        setFloatCameraid(-1, mFloatWinId);
                                        startFloat(-1, mFloatWinId);
                                        if (mFloatWindow != null && appIcon != null) {
                                            appIcon[mFloatWinId].setImageBitmap(null);
                                            appIcon[mFloatWinId].setImageDrawable(appIcon[1].getDrawable());
                                            mFloatWindow.addImageView(appIcon[mFloatWinId],
                                                    mFloatWinId);
                                        }
                                    } else {
                                        setFloatCameraid(bottomId, mFloatWinId);
                                        startFloat(bottomId, mFloatWinId);
                                    }
                                    setFloatCameraid(leftId, 1);
                                    Intent broadcast = new Intent();
                                    broadcast.setAction(ACTION_NAVIGATION_CAMERA_CLOSE_NEED_CHANGED);
                                    broadcast.putExtra(EXTRA_CAM_TYPE, mStartId);

                                    RecordService.this.sendBroadcast(broadcast);
                                } else if (mFloatWinId == 1 && leftId == CameraInfo.CAMERA_FACING_BACK
                                        && DoubleFloatWindow.CAM_NUM == DoubleFloatWindow.DEFAULT_NUM) {
                                    Log.d(TAG, "roll back camera");
                                    int topId = getFloatCameraId(0);
                                    if (topId == -1) {
                                        setFloatCameraid(-1, mFloatWinId);
                                        startFloat(-1, mFloatWinId);
                                        if (mFloatWindow != null && appIcon != null) {
                                            appIcon[mFloatWinId].setImageBitmap(null);
                                            appIcon[mFloatWinId].setImageDrawable(appIcon[0].getDrawable());
                                            mFloatWindow.addImageView(appIcon[mFloatWinId],
                                                    mFloatWinId);
                                        }
                                    } else {
                                        setFloatCameraid(topId, mFloatWinId);
                                        startFloat(topId, mFloatWinId);
                                    }
                                    setFloatCameraid(leftId, 0);
                                    Intent broadcast = new Intent();
                                    broadcast.setAction(ACTION_NAVIGATION_CAMERA_CLOSE_NEED_CHANGED);
                                    broadcast.putExtra(EXTRA_CAM_TYPE, mStartId);
                                    RecordService.this.sendBroadcast(broadcast);
                                } else {
                                    Log.d(TAG, "launcher on left----else");
                                    setFloatCameraid(leftId, mFloatWinId);
                                    Intent broadcast = new Intent();
                                    broadcast.setAction(ACTION_NAVIGATION_CAMERA_CLOSE_NEED_CHANGED);
                                    broadcast.putExtra(EXTRA_CAM_TYPE, mStartId);
                                    RecordService.this.sendBroadcast(broadcast);
                                    // mLastIntent = null;
                                    if (RecorderActivity.CAMERA_COUNT == 2) {
                                        startFloat();
                                    }
                                }
                            } else if ((it.getComponent().getPackageName()).equals("com" +
                                    ".softwinner.carlet.launcher")) {
                                Log.d(TAG, "launcher on left");
                                Drawable dw =
                                        RecordService.this.getResources().getDrawable(R.drawable.launcher);
                                if (appIcon != null) {
                                    appIcon[mFloatWinId].setImageBitmap(null);
                                    appIcon[mFloatWinId].setImageDrawable(dw);
                                }
                                setFloatCameraid(-1, mFloatWinId);
                                startFloat(-1, mFloatWinId);
                                if (mFloatWindow != null && appIcon != null) {
                                    mFloatWindow.addImageView(appIcon[mFloatWinId], mFloatWinId);
                                }
                                Intent itn = new Intent(RecordService.this, RecorderActivity.class);
                                itn.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                itn.putExtra(POWER_ON_START, true);
                                itn.putExtra(EXTRA_CAM_TYPE, mStartId);
                                itn.putExtra(IS_OPEN_AGAIN, true);
                                RecordService.this.startActivity(itn);
                            } else {
                                Log.d(TAG, "app on left");
                                /*
                                 * Bitmap bitmap =
                                 * SplitUtil.getTaskTopThumbnail(
                                 * RecordService.this, ti.persistentId);
                                 */
                                Drawable drawable =
                                        getAppIconByPackageName(it.getComponent().getPackageName());
                                if (appIcon != null) {
                                    appIcon[mFloatWinId].setImageBitmap(null);
                                    appIcon[mFloatWinId].setImageDrawable(drawable);
                                }
                                mLastIntent = it;
                                setFloatCameraid(-1, mFloatWinId);
                                startFloat(-1, mFloatWinId);
                                if (mFloatWindow != null && appIcon != null) {
                                    mFloatWindow.addImageView(appIcon[mFloatWinId], mFloatWinId);
                                }
                                Intent itn = new Intent(RecordService.this, RecorderActivity.class);
                                itn.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                // itn.putExtra(POWER_ON_START, true);
                                itn.putExtra(EXTRA_CAM_TYPE, mStartId);
                                itn.putExtra(IS_OPEN_AGAIN, true);
                                RecordService.this.startActivity(itn);
                            }
                            isGetApp = true;
                        }
                    } else {
                        Log.d(TAG, "RecentTaskInfo = null ");
                    }
                    if (!isGetApp) {
                        Log.d(TAG, "must faltal error here isGetApp=" + isGetApp);
                        // to do reset
                        // mFloatId = CameraInfo.CAMERA_FACING_BACK;
                        // mStartId = -1;
                        // startFloat();
                        mIsChangingFloat = false;
                    }
                }
            }
        }

    }

    private void test( int mFloatWinId){
        int leftId = -1;
        for (int i = mServiceListenerList.size() - 1; i >= 0; i--) {
            leftId = mServiceListenerList.get(i).onAskLeftCameraId();
            if (mServiceListenerList.get(i).onAskLeftCameraId() >= 0) {
                break;
            }
        }
        Log.d(TAG, "test launcher on left----else");
        setFloatCameraid(leftId, mFloatWinId);
        Intent broadcast = new Intent();
        broadcast.setAction(ACTION_NAVIGATION_CAMERA_CLOSE_NEED_CHANGED);
        broadcast.putExtra(EXTRA_CAM_TYPE, mStartId);
        RecordService.this.sendBroadcast(broadcast);
        // mLastIntent = null;
        if (RecorderActivity.CAMERA_COUNT == 2) {
            startFloat();
        }
    }

    @SuppressLint("NewApi")
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        if (previewReceiver != null) {
            unregisterReceiver(previewReceiver);
            previewReceiver = null;
        }
        if (mFloatWindow != null) {
            mFloatWindow.onDestroy();
            mFloatWindow = null;
        }
        if (mFastReverseChecker != null) {
            mFastReverseChecker.stop();
            mFastReverseChecker = null;
        }
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
            mReceiver = null;
        }
        if (mGsensor != null && mGsensorListener != null) {
            mGsensor.unregisterListener(mGsensorListener);
            mGsensor = null;
            mGsensorListener = null;
        }
        if (mSpreadReceiver != null) {
            unregisterReceiver(mSpreadReceiver);
            mSpreadReceiver = null;
        }
        if (mSoundPool != null) {
            mSoundPool.release();
            mSoundPool = null;
        }
        if (mCarSpeedMonitor != null) {
            mCarSpeedMonitor.stopSpeedDection();
            mCarSpeedMonitor = null;
        }
        Log.d(TAG, "onDestroy destroy Recorder");
        if (mRecorderList != null) {
            for (int i = 0; i < mRecorderList.size(); i++) {
                mRecorderList.get(i).onDestroy();
            }
            mRecorderList.clear();
        }
        Log.d(TAG, "onDestroy destroy Recorder done");
        if (mDataAdapter != null) {
            mDataAdapter.flush();
        }
        if (mRecorderHandler != null) {
            mRecorderHandler.removeCallbacksAndMessages(null);
            if (mRecorderHandler.getLooper() != null) {
                mRecorderHandler.getLooper().quitSafely();
            }
            mRecorderHandler = null;
        }
        if (mWakeLock != null) {
            mWakeLock.release();
            mWakeLock = null;
        }
        mPowerManager = null;
        /*
         * if(mTwoCamPreWin != null) { mTwoCamPreWin.unregisterReceiver(); }
         */
    }

    public synchronized void addCamera(int cameraId) {
        Log.d(TAG, "addCamera cameraId= " + cameraId);
        for (int i = 0; i < mRecorderList.size(); i++) {
            if (cameraId == mRecorderList.get(i).getCameraId()) {
                return;
            }
        }
        WrapedRecorder recorder = new WrapedRecorder(cameraId, this);
        mRecorderList.add(recorder);
        Log.d(TAG, "mRecorderList.size()= " + mRecorderList.size());
        return;
    }

    public synchronized boolean isCameraAdd(int cameraId) {
        for (int i = 0; i < mRecorderList.size(); i++) {
            if (cameraId == mRecorderList.get(i).getCameraId()) {
                return true;
            }
        }
        return false;
    }

    public boolean isCameraInit(int cameraId) {
        for (int i = 0; i < mRecorderList.size(); i++) {
            if (cameraId == mRecorderList.get(i).getCameraId()) {
                return mRecorderList.get(i).isCameraInit();
            }
        }
        return false;
    }

    public boolean isCameraOpened(int cameraId) {
        for (int i = 0; i < mRecorderList.size(); i++) {
            if (cameraId == mRecorderList.get(i).getCameraId()) {
                return mRecorderList.get(i).getIsCameraOpen();
            }
        }
        return false;
    }

    public synchronized void setIsChangingFloat(boolean isChanging, long delay) {
        Log.d(TAG, "setIsChangingFloat isChanging=" + isChanging + ";" + delay);
        if (isChanging) {
            mIsChangingFloat = true;
            if (delay > 0 && mMainHandler != null) {
                mMainHandler.removeMessages(MSG_WAITING_ACTIVITY_FINISH);
                mMainHandler.sendEmptyMessageDelayed(MSG_WAITING_ACTIVITY_FINISH, delay);
            }
        } else {
            if (delay <= 0) {
                mIsChangingFloat = false;
            } else {
                if (mMainHandler != null) {
                    mMainHandler.removeMessages(MSG_WAITING_ACTIVITY_FINISH);
                    mMainHandler.sendEmptyMessageDelayed(MSG_WAITING_ACTIVITY_FINISH, delay);
                }
            }
        }
        Log.d(TAG, "----------------------onShowRear--------------setIsChangingFloat");
        RecordService.this.onShowRear(false);

    }

    public synchronized void showTwoFloat() {
        Log.d(TAG, "showTowFloat()");
        if (twoFloatWindow != null) {
            if (!twoFloatWindow.isShow()) {
                twoFloatWindow.showWindow();
            }
        }
    }

    interface VoiceControlCallBack {
        void onVoiceControl(int id);

        void onHome();
    }

    private VoiceControlCallBack voiceControlCallBack = null;

    public void setVoiceControlCallBack(VoiceControlCallBack voiceControlCallBack) {
        this.voiceControlCallBack = voiceControlCallBack;
    }

    public void showFullscreenCamera(int CameraId) {
        Log.e(TAG, "showFullscreenCamera() CameraId = " + CameraId);
        int key = 1;
        if (CameraId == 5) {
            showTwoFloat();
            speakText("好的");
        } else {
            if (CameraId == 0) {
                key = FullScreenCameraActivity.STATE_FULLSCREEN_SHOW_CAMERA_BACK;
            } else if (CameraId == 1) {
                key = FullScreenCameraActivity.STATE_FULLSCREEN_SHOW_CAMERA_FRONT;
            } else if (CameraId == 2) {
                key = FullScreenCameraActivity.STATE_FULLSCREEN_SHOW_CAMERA_RIGHT_BACK;
            } /*
             * else if(CameraId == 3){ key = FullScreenCameraActivity.
             * STATE_FULLSCREEN_SHOW_CAMERA_LEFT_BACK; }
             */ else if (CameraId == 4) {
                key = FullScreenCameraActivity.STATE_FULLSCREEN_SHOW_CAMERA_LEFT_BACK_RIGHT;
            }
            showFullscreenCameraInner(key, true, false);
        }

    }

    public void showFullscreenCameraInner(int id, boolean isVoiceControl, boolean isReverse) {
        boolean isCameraOpen = false;
        for (int i = 0; i < mRecorderList.size(); i++) {
            if (mRecorderList.get(i).getCameraId() == CameraInfo.CAMERA_FACING_BACK) {
                isCameraOpen = mRecorderList.get(i).getIsCameraOpen();
            }
        }

        Log.e(TAG, "showFullscreenCameraInner" + isCameraOpen);
        if (isCameraOpen) {
            hideTwoFloat();
			/*Intent intent = new Intent(this, FullScreenCameraActivity.class);
			intent.putExtra(FullScreenCameraActivity.KEY_STATUS, id);
			intent.putExtra(FullScreenCameraActivity.KEY_VOICE_CONTROL, isVoiceControl);
			intent.putExtra(FullScreenCameraActivity.KEY_REVERSE, isReverse);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);*/
        } else {
            Toast.makeText(getApplicationContext(), R.string.ri_cam_noopen_tip,
                    Toast.LENGTH_SHORT).show();
        }
    }

    public void hideFullscreenCamera(int CameraId) {
        Log.d(TAG, "hideFullscreenCamera() CameraId = " + CameraId);
        if (CameraId == 5) {
            if (null != twoFloatWindow && twoFloatWindow.isShow()) {
                hideTwoFloat();
                speakText("好的");
            } else {
                speakText("左右预览未打开");
            }
        } else {
            if (voiceControlCallBack == null) {
                if (CameraId == 0) {
                    speakText("你没有打开后路");
                } else if (CameraId == 1) {
                    speakText("你没有打开前路");
                } else if (CameraId == 2) {
                    speakText("你没有打开右路");
                } else if (CameraId == 3) {
                    speakText("你没有打开左路");
                } else if (CameraId == 4) {
                    speakText("你没有打开盲区");
                }
            } else {
                int key = 1;
                if (CameraId == 0) {
                    key = FullScreenCameraActivity.STATE_FULLSCREEN_SHOW_CAMERA_BACK;
                } else if (CameraId == 1) {
                    key = FullScreenCameraActivity.STATE_FULLSCREEN_SHOW_CAMERA_FRONT;
                } else if (CameraId == 2) {
                    key = FullScreenCameraActivity.STATE_FULLSCREEN_SHOW_CAMERA_RIGHT_BACK;
                } /*
                 * else if(CameraId == 3){ key = FullScreenCameraActivity.
                 * STATE_FULLSCREEN_SHOW_CAMERA_LEFT_BACK; }
                 */ else if (CameraId == 4) {
                    key = FullScreenCameraActivity.STATE_FULLSCREEN_SHOW_CAMERA_LEFT_BACK_RIGHT;
                }
                hideFullscreenCameraInner(key);
            }
        }

    }

    public void hideFullscreenCameraInner(int id) {
        if (null != voiceControlCallBack) {
            voiceControlCallBack.onVoiceControl(id);
        }
    }

    public void goToHome() {
        if (null != voiceControlCallBack) {
            voiceControlCallBack.onHome();
        }
        Intent mHomeIntent = new Intent(Intent.ACTION_MAIN);
        mHomeIntent.addCategory(Intent.CATEGORY_HOME);
        mHomeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        startActivity(mHomeIntent);
        speakText("好的");
    }

    private void speakText(String str) {
        Intent mIntent = new Intent();
        mIntent.setAction(ACTION_SPEAK_TEXT);
        mIntent.putExtra("otherText", str);
        sendBroadcast(mIntent);
    }

    public void hideTwoFloat() {
        Log.d(TAG, "hideTwoFloat()");
        if (twoFloatWindow != null) {
            if (twoFloatWindow.isShow()) {
                twoFloatWindow.hideWindow();
            }
        }
    }

    public synchronized void startFloat(int cameraId, int id) {
        if (mFloatWindow != null) {
            mFloatWindow.setDisplayId(cameraId, id);
        }
    }

    public synchronized void startFloat() {
        if (mFloatWindow != null) {
            mFloatWindow.setDisplayId();
        }
    }

    public void setFlip(boolean isFlip) {
        for (int i = 0; i < mRecorderList.size(); i++) {
            if (CameraInfo.CAMERA_FACING_BACK == mRecorderList.get(i).getCameraId()) {
                mRecorderList.get(i).setCameraFlip(isFlip);
            }
        }
    }

    public void setRightFlip(boolean isRightFlip) {
        for (int i = 0; i < mRecorderList.size(); i++) {
            if (RecorderActivity.CAMERA_THIRD == mRecorderList.get(i).getCameraId()) {
                mRecorderList.get(i).setCameraFlip(isRightFlip);
            }
        }
    }

    public synchronized void startSwitchFile() {
        mStartTime = System.currentTimeMillis();
        for (int i = 0; i < mServiceListenerList.size(); i++) {
            if (mServiceListenerList.get(i) != null) {
                mServiceListenerList.get(i).onTimeUpdate(0);
            }
        }
        if (mMainHandler != null) {
            mMainHandler.removeMessages(MSG_START_SWTICH_FILE);
            mMainHandler.sendEmptyMessageDelayed(MSG_START_SWTICH_FILE, SWTICH_FILE_DELAY);
        }
    }

    public synchronized void stopSwitchFile() {
        mStartTime = 0;
        for (int i = 0; i < mServiceListenerList.size(); i++) {
            if (mServiceListenerList.get(i) != null) {
                mServiceListenerList.get(i).onTimeUpdate(0);
            }
        }
        if (mMainHandler != null) {
            mMainHandler.removeMessages(MSG_START_SWTICH_FILE);
        }
    }

    public synchronized int getNeedFloat(int cameraId) {
        if (mFloatWindow != null) {
            return mFloatWindow.getNeedFloat(cameraId);
        } else {
            return -1;
        }
    }

    public synchronized void setFloatCameraid(int cameraId) {
        if (mFloatWindow != null) {
            mFloatWindow.setFloatCameraid(cameraId, mFloatId);
        }
    }

    public int getLastFloatId() {
        return mFloatId;
    }

    public synchronized void setFloatCameraid(int cameraId, int floatId) {
        mFloatId = floatId;
        if (mFloatWindow != null) {
            mFloatWindow.setFloatCameraid(cameraId, floatId);
        }
    }

    public void resetFloatId() {
        if (mFloatWindow != null) {
            mFloatWindow.resetFloatId();
        }
    }

    public void hideFloatWindows() {
        if (RecorderActivity.CAMERA_COUNT == 2 && mFloatWindow != null) {
            mFloatWindow.hide();
        }
    }

    public void showFloatWindows() {
        if (RecorderActivity.CAMERA_COUNT == 2 && mFloatWindow != null) {
            mFloatWindow.show();
        }
    }

    public synchronized void setStartCameraid(int cameraId) {
        mStartId = cameraId;
    }

    public synchronized int getFloatCameraId() {
        if (mFloatWindow != null) {
            return mFloatWindow.getFloatCameraId(mFloatId);
        } else {
            return -1;
        }
    }

    public synchronized int getFloatCameraId(int floatId) {
        if (mFloatWindow != null) {
            return mFloatWindow.getFloatCameraId(floatId);
        } else {
            return -1;
        }
    }

    public synchronized int getRecorderCount() {
        return mRecorderList.size();
    }

    public synchronized void removeCamera(int cameraId) {
        Log.d(TAG, "removeCamera cameraId=" + cameraId);
        int index = -1;
        for (int i = 0; i < mRecorderList.size(); i++) {
            if (cameraId == mRecorderList.get(i).getCameraId()) {
                mRecorderList.get(i).onDestroy();
                index = i;
            }
        }
        if (index > -1 && index < mRecorderList.size()) {
            mRecorderList.remove(index);
        }
        Log.d(TAG, "removeCamera mRecorderList.size()=" + mRecorderList.size());
        return;
    }

    public synchronized boolean isPreview(int cameraId) {
        for (int i = 0; i < mRecorderList.size(); i++) {
            if (cameraId == mRecorderList.get(i).getCameraId()) {
                return mRecorderList.get(i).isPreview();
            }
        }
        return false;
    }

    public synchronized boolean isRender(int cameraId) {
        for (int i = 0; i < mRecorderList.size(); i++) {

            if (cameraId == mRecorderList.get(i).getCameraId()) {
                return mRecorderList.get(i).isRender();
            }
        }
        return false;
    }

    public synchronized void startPreview(int cameraId) {
        for (int i = 0; i < mRecorderList.size(); i++) {

            if (cameraId == mRecorderList.get(i).getCameraId()) {
                mRecorderList.get(i).startPreview();
                Log.v(TAG, "-----startsize:" + i);
                break;
            }
        }
    }

    public synchronized void startRender(int cameraId) {
        for (int i = 0; i < mRecorderList.size(); i++) {
            if (cameraId == mRecorderList.get(i).getCameraId()) {
                mRecorderList.get(i).startRender();
                break;
            }
        }
    }

    public synchronized void stopPreview(int cameraId) {
        for (int i = 0; i < mRecorderList.size(); i++) {

            if (cameraId == mRecorderList.get(i).getCameraId()) {
                mRecorderList.get(i).stopPreview();
                Log.v(TAG, "-----stopsize:" + i);
                break;
            }
        }
    }

    public synchronized void stopRender(int cameraId) {
        for (int i = 0; i < mRecorderList.size(); i++) {

            if (cameraId == mRecorderList.get(i).getCameraId()) {
                mRecorderList.get(i).stopRender();
                break;
            }
        }
    }

    // add by lym start
    private boolean isToLeft = false;
    private SurfaceHolder front;
    private SurfaceHolder back;

    public void setToLeft() {
        isToLeft = true;
    }
    public void doPerformClick(){
        mFloatWindow.doPerformClick();
    }
    public void setToRight() {
        isToLeft = false;
    }
//    public synchronized void setPreviewDisplay(int cameraId, SurfaceHolder sh) {
//        Log.d(TAG, "setPreviewDisplay:cameraId " + cameraId);
//        for (int i = 0; i < mRecorderList.size(); i++) {
//            if (!isToLeft) {
//                if (cameraId == mRecorderList.get(i).getCameraId()) {
//                    mRecorderList.get(i).setPreviewDisplay(sh);
//                    break;
//                }
//            } else {
//                if (cameraId != mRecorderList.get(i).getCameraId()) {
//                    mRecorderList.get(i).setPreviewDisplay(sh);
//                    break;
//                }
//            }
//        }
//    }
    // end

    public synchronized void setPreviewDisplay(int cameraId, SurfaceHolder sh) {
        Log.d(TAG, "setPreviewDisplay:cameraId "+cameraId);
        for (int i = 0; i < mRecorderList.size(); i++) {
            if (cameraId == mRecorderList.get(i).getCameraId()) {
                mRecorderList.get(i).setPreviewDisplay(sh);
                break;
            }
        }
    }

    public synchronized void setPreviewTexture(int cameraId, SurfaceTexture st) {
        for (int i = 0; i < mRecorderList.size(); i++) {
            if (cameraId == mRecorderList.get(i).getCameraId()) {
                mRecorderList.get(i).setPreviewTexture(st);
                break;
            }
        }
    }

    public synchronized void setIntelligentDetect(int cameraId, boolean value) {
        for (int i = 0; i < mRecorderList.size(); i++) {
            if (cameraId == mRecorderList.get(i).getCameraId()) {
                mRecorderList.get(i).setIntelligentDetect(value);
                break;
            }
        }
    }

    public synchronized void setRecQuality(int value) {
        for (int i = 0; i < mRecorderList.size(); i++) {
            mRecorderList.get(i).setRecQuality(value);
        }
    }

    public synchronized void setPicQualtiy(int value) {
        for (int i = 0; i < mRecorderList.size(); i++) {
            mRecorderList.get(i).setPicQualtiy(value);
        }
    }

    public synchronized void setDuration(int value) {
        for (int i = 0; i < mRecorderList.size(); i++) {
            mRecorderList.get(i).setDuration(value);
        }
    }

    public synchronized void setCarLaneAdjust(int cameraId, int value) {
        for (int i = 0; i < mRecorderList.size(); i++) {
            if (cameraId == mRecorderList.get(i).getCameraId()) {
                mRecorderList.get(i).setCarLaneAdjust(value);
                break;
            }
        }
    }

    public synchronized void setCrashSensity(int value) {
        for (int i = 0; i < mRecorderList.size(); i++) {
            mRecorderList.get(i).setCrashSensity(value);
        }
    }

    public synchronized void setLockSensity(int value) {
        for (int i = 0; i < mRecorderList.size(); i++) {
            mRecorderList.get(i).setLockSensity(value);
        }
        Message msg = Message.obtain();
        msg.what = MSG_GSENSOR_SET_SENSITY;
        if (value == 0) {
            msg.arg1 = SENSITY_HIGHT;
        } else if (value == 2) {
            msg.arg1 = SENSITY_LOW;
        } else {
            msg.arg1 = SENSITY_MEDIUM;
        }
        if (mRecorderHandler != null) {
            mRecorderHandler.sendMessage(msg);
        }
    }

    public void setGsensorWake(boolean isEnable) {
        Message msg = Message.obtain();
        msg.what = MSG_GSENSOR_ENABLE;
        if (isEnable) {
            msg.arg1 = 1;
        } else {
            msg.arg1 = 0;
        }
        if (mRecorderHandler != null) {
            mRecorderHandler.sendMessage(msg);
        }
    }

    public synchronized void setParkingCrashSensity(int cameraId, int value) {
        for (int i = 0; i < mRecorderList.size(); i++) {
            if (cameraId == mRecorderList.get(i).getCameraId()) {
                mRecorderList.get(i).setParkingCrashSensity(value);
                break;
            }
        }
    }

    public synchronized void setCarType(int cameraId, int value) {
        for (int i = 0; i < mRecorderList.size(); i++) {
            if (cameraId == mRecorderList.get(i).getCameraId()) {
                mRecorderList.get(i).setCarType(value);
                break;
            }
        }
    }

    public synchronized void setAdasLevel(int cameraId, int value) {
        for (int i = 0; i < mRecorderList.size(); i++) {
            if (cameraId == mRecorderList.get(i).getCameraId()) {
                mRecorderList.get(i).setAdasLevel(value);
                break;
            }
        }
    }

    private boolean isNeedPictureFeedback = false;

    public synchronized void takeSnapShot() {
        if (Math.abs(System.currentTimeMillis() - mLastSnapShot) < 800) {
            return;
        }
        mLastSnapShot = System.currentTimeMillis();
        // add by chengyuzhou
        if (preferences.getBoolean("isShowThird", true)) {
            for (int i = 0; i < mRecorderList.size(); i++) {
                if (isNeedPictureFeedback) {
                    mRecorderList.get(i).needFeedbackPictureFileName();
                }
                if (CustomValue.BACK_CAMERA_NOT_RECORD) {
                    if (mRecorderList.get(i).getCameraId() != CameraInfo.CAMERA_FACING_BACK) {
                        mRecorderList.get(i).takeASnapshot();
                    }
                } else {
                    mRecorderList.get(i).takeASnapshot();
                }
            }
        } else {
            if (isNeedPictureFeedback) {
                mRecorderList.get(0).needFeedbackPictureFileName();
                mRecorderList.get(1).needFeedbackPictureFileName();
            }
            mRecorderList.get(0).takeASnapshot();
            mRecorderList.get(1).takeASnapshot();
        }
        isNeedPictureFeedback = false;
        // end
    }

    public synchronized void setAdasDetecttionCallback(int cameraId,
                                                       AdasDetectionListener listener) {
        for (int i = 0; i < mRecorderList.size(); i++) {
            if (cameraId == mRecorderList.get(i).getCameraId()) {
                mRecorderList.get(i).setAdasDetecttionListener(listener);
                break;
            }
        }
    }

    public synchronized void setLockFlag(boolean isLock) {
        for (int i = 0; i < mRecorderList.size(); i++) {
            mRecorderList.get(i).setLockFlag(isLock);
        }
    }

    public synchronized boolean getLockFlag(int cameraId) {
        for (int i = 0; i < mRecorderList.size(); i++) {
            if (cameraId == mRecorderList.get(i).getCameraId()) {
                return mRecorderList.get(i).getLockFlag();
            }
        }
        return false;
    }

    public synchronized void setLockOnce(boolean isLock) {
        mIsLockOnce = isLock;
    }

    private boolean isNeedFeedbackVideo = false;

    public synchronized int startRecording() {

        if(CustomValue.CAMERA_NOT_RECORD){
            Log.d(TAG, "startRecording CAMERA_NOT_RECORD");
            return -1;
        }
        Log.d(TAG, "startRecording new ");
        if (!checkStorageCanRecord()) {
            // todo more gentlly hint
            if (mToast != null) {
                mToast.cancel();
            }
            mToast = Toast.makeText(this, R.string.space_no_enough, Toast.LENGTH_LONG);
            mToast.show();
            if (mRecorderHandler != null) {
                mRecorderHandler.sendEmptyMessage(MSG_START_CLEAR_FILE);
            }
            return -1;
        } else {
            if (mRecordNeedSpace > Storage.getAvailableSpace()) {
                if (mRecorderHandler != null) {
                    mRecorderHandler.sendEmptyMessage(MSG_START_CLEAR_FILE);
                }
            }
        }
        /*
         * for (int i = 0; i < mRecorderList.size(); i++) { if
         * (mRecorderList.get(i).isRecording()) { Toast.makeText(this,
         * R.string.device_busy, Toast.LENGTH_LONG).show(); return -1; } }
         */
        if (isRecorderBusy()) {
            if (mToast != null) {
                mToast.cancel();
            }
            // mToast = Toast.makeText(this, R.string.operating_camera,
            // Toast.LENGTH_LONG);
            // mToast.show();
            return -1;
        }
        int res = 0;
        mCurTime = System.currentTimeMillis();
        mpreferences.edit().putBoolean("isVideo", true).commit();
        if (null != streamPreviewWindow) {
            streamPreviewWindow.setRecordStatus(true);
        }
        if (null != twoFloatWindow) {
            twoFloatWindow.setRecordStatus(true);
        }
        sendBroadcast(new Intent("com.zqc.action.startCameraRecord"));
        if (null != streamMediaWindow) {
            streamMediaWindow.startRecording();
        }
        // add by chengyuzhou
        if (preferences.getBoolean("isShowThird", true)) {
            for (int i = 0; i < mRecorderList.size(); i++) {
                if (isNeedFeedbackVideo) {
                    mRecorderList.get(i).needFeedbackVideoFileName();
                }
                //add by lym
                if (CustomValue.BACK_CAMERA_NOT_RECORD) {
                    if (mRecorderList.get(i).getCameraId() != CameraInfo.CAMERA_FACING_BACK) {
                        res = mRecorderList.get(i).startRecording();
                    }
                } else {
                    res = mRecorderList.get(i).startRecording();
                }
                //end

//                res = mRecorderList.get(i).startRecording();

                /*
                 * if (mRecorderList.get(i).getCameraId() ==
                 * CameraInfo.CAMERA_FACING_BACK||
                 * mRecorderList.get(i).getCameraId() ==
                 * CameraInfo.CAMERA_FACING_FRONT) { }
                 */
            }
        } else {
            if (isNeedFeedbackVideo) {
                mRecorderList.get(0).needFeedbackVideoFileName();
                mRecorderList.get(1).needFeedbackVideoFileName();
            }
            for (int i = 0; i < mRecorderList.size(); i++) {
                res = mRecorderList.get(i).startRecording();
                /*
                 * if (mRecorderList.get(i).getCameraId() ==
                 * CameraInfo.CAMERA_FACING_BACK||
                 * mRecorderList.get(i).getCameraId() ==
                 * CameraInfo.CAMERA_FACING_FRONT) { }
                 */
            }
        }
        isNeedFeedbackVideo = false;
        // end
        boolean recording = false;
        for (int i = 0; i < mRecorderList.size(); i++) {
            if (mRecorderList.get(i).isRecording()) {
                recording = true;
                break;
            }
        }
        Log.d(TAG, "startRecording recording = " + recording);
        if (mRecorderList.size() > 0 && recording) {
            if (!mStartSpaceCheck) {
                startSpaceCheck();
            }
            startSwitchFile();
        } else {
            if (mStartSpaceCheck) {
                stopSpaceCheck();
            }
            stopSwitchFile();
        }

        return res;
    }

    public synchronized boolean isRecording(int cameraId) {
        for (int i = 0; i < mRecorderList.size(); i++) {
            if (mRecorderList.get(i).getCameraId() == cameraId) {
                return mRecorderList.get(i).isRecording();
            }
        }
        return false;
    }

    public boolean isRecorderBusy() {
        for (int i = 0; i < mRecorderList.size(); i++) {
            if (mRecorderList.get(i).getCameraId() == CameraInfo.CAMERA_FACING_FRONT) {
                if (mRecorderList.get(i).isRecorderBusy()) {
                    return true;
                }
            }
        }
        return false;
    }

    public Camera getCamera(int mCameraID) {
        Camera mCamera = null;
        for (int i = 0; i < mRecorderList.size(); i++) {
            if (mRecorderList.get(i).getCameraId() == mCameraID) {
                mCamera = mRecorderList.get(i).getCamera();
            }
        }
        return mCamera;
    }

    public boolean IsRightCameraOpen() {
        boolean isOpen = false;
        for (int i = 0; i < mRecorderList.size(); i++) {
            if (mRecorderList.get(i).getCameraId() == CameraInfo.CAMERA_FACING_BACK) {
                isOpen = mRecorderList.get(i).getIsCameraOpen();
            }
        }
        return isOpen;
    }

    public synchronized boolean isAdasOn() {
        for (int i = 0; i < mRecorderList.size(); i++) {
            if (mRecorderList.get(i).getCameraId() == CameraInfo.CAMERA_FACING_FRONT) {
                return mRecorderList.get(i).isAdasOn();
            }
        }
        return false;
    }

    public void startRender() {
        Log.d(TAG, "---startPreview size = " + mRecorderList.size());
        if (preferences.getBoolean("isShowThird", true)) {
            for (int i = 0; i < mRecorderList.size(); i++) {
                mRecorderList.get(i).startRender();
            }
        } else {
            mRecorderList.get(0).startRender();
            mRecorderList.get(1).startRender();
        }
    }

    public void stopRender() {
        Log.d(TAG, "---stopPreview size = " + mRecorderList.size());
        for (int i = 0; i < mRecorderList.size(); i++) {
            mRecorderList.get(i).stopRender();
        }
    }

    public synchronized int stopRecording() {
        Log.d(TAG, "stopRecording");
        if (isRecorderBusy()) {
            Log.d(TAG, "stopRecording recorder busy");
            if (mToast != null) {
                mToast.cancel();
            }
            // mToast = Toast.makeText(this, R.string.operating_camera,
            // Toast.LENGTH_LONG);
            // mToast.show();
            return -1;
        }
        Log.e("hy", "isvideo55555555");
        mpreferences.edit().putBoolean("isVideo", false).commit();
        if (null != streamPreviewWindow) {
            streamPreviewWindow.setRecordStatus(false);
        }
        if (null != twoFloatWindow) {
            twoFloatWindow.setRecordStatus(false);
        }
        sendBroadcast(new Intent("com.zqc.action.stopCameraRecord"));
        if (null != streamMediaWindow) {
            streamMediaWindow.stopRecording();
        }
        for (int i = 0; i < mRecorderList.size(); i++) {
            mRecorderList.get(i).stopRecording();
        }
        boolean recording = false;
        for (int i = 0; i < mRecorderList.size(); i++) {
            if (mRecorderList.get(i).isRecording()) {
                recording = true;
                break;
            }
        }
        if (mRecorderList.size() > 0 && recording) {
            if (!mStartSpaceCheck) {
                startSpaceCheck();
            }
            startSwitchFile();
        } else {
            if (mStartSpaceCheck) {
                stopSpaceCheck();
            }
            stopSwitchFile();
        }
        return -1;
    }

    public synchronized int stopRecordingSync() {
        for (int i = 0; i < mRecorderList.size(); i++) {
            mRecorderList.get(i).stopRecording();
            mRecorderList.get(i).waitDone();
        }
        boolean recording = false;
        for (int i = 0; i < mRecorderList.size(); i++) {
            if (mRecorderList.get(i).isRecording()) {
                recording = true;
                break;
            }
        }
        if (mRecorderList.size() > 0 && recording) {
            if (!mStartSpaceCheck) {
                startSpaceCheck();
            }
            startSwitchFile();
        } else {
            if (mStartSpaceCheck) {
                stopSpaceCheck();
            }
            stopSwitchFile();
        }
        return -1;
    }

    public synchronized int release(int cameraId) {
        for (int i = 0; i < mRecorderList.size(); i++) {
            if (cameraId == mRecorderList.get(i).getCameraId()) {
                return mRecorderList.get(i).release();
            }
        }
        return -1;
    }

    public synchronized void setMute(boolean isMute, int cameraId) {
        // Log.v(TAG,"listSize:"+mRecorderList.size());
        for (int i = 0; i < mRecorderList.size(); i++) {

            if (cameraId == mRecorderList.get(i).getCameraId()) {
                mRecorderList.get(i).setMute(isMute);
                break;
            }
        }
    }

    public synchronized void setRecordCallback(int cameraId, WrapedRecorder.IRecordCallback cb) {
        for (int i = 0; i < mRecorderList.size(); i++) {
            if (cameraId == mRecorderList.get(i).getCameraId()) {
                mRecorderList.get(i).setRecordCallback(cb);
                break;
            }
        }
    }

    public WrapedRecorder.IRecordCallback getRecordCallback(int cameraId) {
        for (int i = 0; i < mRecorderList.size(); i++) {
            if (cameraId == mRecorderList.get(i).getCameraId()) {
                return mRecorderList.get(i).getRecordCallback();
            }
        }
        return null;
    }

    public synchronized void setRecorderDestroyedListener(int cameraId,
                                                          WrapedRecorder.IRecorderDestroyedListener ls) {
        for (int i = 0; i < mRecorderList.size(); i++) {
            if (cameraId == mRecorderList.get(i).getCameraId()) {
                mRecorderList.get(i).setRecorderDestroyedListener(ls);
                break;
            }
        }
    }

    public synchronized int switchToNextFile(int cameraId) {
        for (int i = 0; i < mRecorderList.size(); i++) {
            if (cameraId == mRecorderList.get(i).getCameraId()) {
                return mRecorderList.get(i).switchToNextFile();
            }
        }
        return -1;
    }

    public synchronized void startWaterMark(int cameraId) {
        for (int i = 0; i < mRecorderList.size(); i++) {
            if (cameraId == mRecorderList.get(i).getCameraId()) {
                mRecorderList.get(i).startWaterMark();
                break;
            }
        }
        return;
    }

    public synchronized void setWaterMarkMultiple(float speed, double longitude, double latitude) {
        for (int i = 0; i < mRecorderList.size(); i++) {
            if (mRecorderList.get(i).getCameraId() == CameraInfo.CAMERA_FACING_FRONT) {
                mRecorderList.get(i).setWaterMarkMultiple(speed, longitude, latitude);
                break;
            }
        }
        return;
    }

    public synchronized void setAdasSpeed(int cameraId, float speed) {
        for (int i = 0; i < mRecorderList.size(); i++) {
            if (cameraId == mRecorderList.get(i).getCameraId()) {
                mRecorderList.get(i).setAdasSpeed(speed);
                break;
            }
        }
        return;
    }

    public synchronized boolean isWaterMarkRuning(int cameraId) {
        for (int i = 0; i < mRecorderList.size(); i++) {
            if (cameraId == mRecorderList.get(i).getCameraId()) {
                return mRecorderList.get(i).isWaterMarkRuning();
            }
        }
        return false;
    }

    public synchronized void stopWaterMark(int cameraId) {
        for (int i = 0; i < mRecorderList.size(); i++) {
            if (cameraId == mRecorderList.get(i).getCameraId()) {
                mRecorderList.get(i).stopWaterMark();
                break;
            }
        }
        return;
    }

    public boolean isRecordingFile(String name) {
        for (int i = 0; i < mRecorderList.size(); i++) {
            if (mRecorderList.get(i).isRecordingFile(name)) {
                return true;
            }
        }
        return false;
    }

    public void setSaveMediaDelay(boolean isDelay) {
        for (int i = 0; i < mRecorderList.size(); i++) {
            mRecorderList.get(i).setSaveMediaDelay(isDelay);
        }
    }

    public boolean checkStorageNeedRecycle() {
        Log.d(TAG, "=== Storage.getAvailableSpace()=  " + Storage.getAvailableSpace());
        Log.d(TAG, "=== needSpace =  " + mRecordNeedSpace);
        if (mRecordNeedSpace < Storage.getAvailableSpace()) {
            return true;
        }
        return false;
    }

    public boolean checkStorageCanRecord() {
        long needSize = mRecordNeedSpace;
        Log.d(TAG, "=== check storage can record need size " + (needSize / 1024 / 1024) + "m " +
                "=====");
        Log.d(TAG, "=== Storage.getAvailableSpace()=  " + Storage.getAvailableSpace());

        if (needSize < Storage.getAvailableSpace()) {
            Log.d(TAG, "=== return true no need clean  ======");
            return true;
        } /*
         * else if (mDataAdapter.getTotalNumber() > 0) { long canCleanSize =
         * 0; int dataCount = mDataAdapter.getTotalNumber();
         *
         * File file; for (int i = dataCount - 1; i > 0; i--) { LocalData
         * localData = mDataAdapter.getLocalData(i); if
         * (localData.getLocalDataType() == LocalData.LOCAL_VIDEO) { if
         * (isImpactVideo(i)) { continue; }
         *
         * file = new File(localData.getPath()); if (file.exists() &&
         * file.getPath().contains(Storage.DIRECTORY)) { canCleanSize +=
         * file.length(); // Log.d(TAG, "=== localData.getPath()=  " + //
         * localData.getPath() + "getsize=" + file.length()); } } }
         * Log.d(TAG, "getAvailableSpace ==" + Storage.getAvailableSpace() +
         * "; canCleanSize==" + canCleanSize + "; getTotalSpace =" +
         * Storage.getTotalSpace());
         *
         * if (canCleanSize + Storage.getAvailableSpace() >= needSize) {
         * Log.d(TAG, "=== return true from scan data adapter ======"); if
         * (Storage.getAvailableSpace() + canCleanSize <
         * Storage.getTotalSpace() / 2) { // return true; } return true; } }
         */ else {
            long canCleanSize = 0;

            File dirFile = new File(Storage.DIRECTORY);
            File[] subFiles = dirFile.listFiles();

            if (subFiles != null && subFiles.length > 0) {
                for (File f : subFiles) {
                    Log.d(TAG, "checkStorageCanRecord i=" + f.getPath());
                    if ((f.getName().endsWith("mp4") || f.getName().endsWith("ts"))
                            && !f.getName().contains(IMPACT_SUFFIX) && f.getPath().contains(Storage.DIRECTORY)) {
                        canCleanSize += f.length();
                    }

                    if (canCleanSize + Storage.getAvailableSpace() >= needSize) {
                        Log.d(TAG, "=== return true from scan file system ======");
                        return true;
                    }
                }
            }
        }

        Log.d(TAG, "=== check storage no enough space ======");
        return false;
    }

    private boolean isImpactVideo(int dataID) {
        LocalData localData = mDataAdapter.getLocalData(dataID);
        if (localData.getLocalDataType() != LocalData.LOCAL_VIDEO) {
            return false;
        }
        String path = localData.getPath();
        int index = path.lastIndexOf(".");
        if (index < 0) {
            return false;
        }
        String pathSuffix = path.substring((index - IMPACT_SUFFIX.length()) >= 0 ?
                        (index - IMPACT_SUFFIX.length()) : 0,
                index);
        if (pathSuffix.compareTo(IMPACT_SUFFIX) != 0) {
            return false;
        }
        Log.i(TAG, "ImpactVideo, path=" + path + ", index=" + index);
        return true;
    }

    public boolean isImpactVideo(String file) {
        if (file == null) {
            return false;
        }
        int index = file.lastIndexOf(".");
        if (index < 0) {
            return false;
        }
        String pathSuffix = file.substring((index - IMPACT_SUFFIX.length()) >= 0 ?
                        (index - IMPACT_SUFFIX.length()) : 0,
                index);
        if (pathSuffix.compareTo(IMPACT_SUFFIX) != 0) {
            return false;
        }
        Log.i(TAG, "ImpactVideo, path=" + file + ", index=" + index);
        return true;
    }

    private boolean cleanLocalData() {
        long needSize = mRecordNeedSpace - Storage.getAvailableSpace();
        int dataCount = mDataAdapter.getTotalNumber();
        Log.d(TAG, "cleanLocalData dataCount=" + dataCount);
        long recycledSize = 0;

        if (needSize < 0) {
            return true;
        }

        /*
         * if (dataCount > 1) { File file = null; File kml = null; String
         * otherName = null; String otherPath = null; File otherFile = null; for
         * (int i = dataCount - 1; i > 0; i--) { LocalData localData =
         * mDataAdapter.getLocalData(i); if (localData.getLocalDataType() ==
         * LocalData.LOCAL_VIDEO) { Log.d(TAG, "cleanLocalData i=" +
         * localData.getPath()); if (isImpactVideo(i)) { continue; }
         *
         * String filePath = localData.getPath(); file = new File(filePath); if
         * (file.exists() && filePath.contains(Storage.DIRECTORY)) {
         * recycledSize += file.length(); } else { continue; }
         *
         * String fileName = file.getName(); String time = null; if (fileName !=
         * null && fileName.length() > 15) { time = fileName.substring(0, 15); }
         * Log.d(TAG, "time=" + time); kml = new File(Storage.DIRECTORY + "/" +
         * time + "K.kml"); Log.d(TAG, "kml.getPath() =" + kml.getPath()); if
         * (kml.exists()) { kml.delete(); } mDataAdapter.removeData(this, i);
         * mDataAdapter.executeDeletion(this);
         *
         * Log.i(TAG, "oldestNum = " + i + ",need recycle wirte path = " +
         * localData.getPath() + ",  clean_size=" + (localData.getSizeInBytes()
         * / 1024 / 1024) + "m" + ",  real_size=" + (file.length() / 1024 /
         * 1024) + "m");
         *
         * if (recycledSize >= needSize) { break; } } } Log.d(TAG,
         * "recycledSize = " + recycledSize); return recycledSize >= needSize; }
         * else { Log.d(TAG, "cleanLocalData clean directly"); File dirFile =
         * new File(Storage.DIRECTORY); File[] subFiles = dirFile.listFiles();
         * String time = null; File kml = null;
         *
         * if (subFiles != null && subFiles.length > 0) { for (File f :
         * subFiles) { Log.d(TAG, "cleanLocalData i=" + f.getPath()); if
         * (isImpactVideo(f.getName())) { continue; } if
         * ((f.getName().endsWith("mp4") || f.getName().endsWith("ts")) &&
         * !f.getName().contains(IMPACT_SUFFIX) &&
         * f.getPath().contains(Storage.DIRECTORY)) { recycledSize +=
         * f.length(); f.delete(); if (f.getName() != null &&
         * f.getName().length() > 15) { time = f.getName().substring(0, 15); }
         * Log.d(TAG, "time=" + time); kml = new File(Storage.DIRECTORY + "/" +
         * time + "K.kml"); if (kml.exists()) { kml.delete(); } }
         *
         * if (recycledSize >= needSize) { break; } } } Log.d(TAG,
         * "local recycledSize = " + recycledSize); return recycledSize >=
         * needSize; }
         */
        Log.d(TAG, "cleanLocalData clean directly");
        File dirFile = new File(Storage.DIRECTORY);
        File[] subFiles = dirFile.listFiles();
        if (subFiles != null) {
            Arrays.sort(subFiles, new Comparator<File>() {
                @Override
                public int compare(File o1, File o2) {
                    if (o1 == null || o2 == null || o2.getName() == null || o1.getName() == null) {
                        return -1;
                    }
                    return o1.getName().compareTo(o2.getName());
                }
            });
        }
        String time = null;
        // add by chengyuzhou
        // File kml = null;
        // end
        File backFile = null;
        File lastImpactFile = null;

        if (subFiles != null && subFiles.length > 0) {
            for (File f : subFiles) {
                if (f.getName() == null || f.getPath() == null) {
                    continue;
                }
                Log.d(TAG, "cleanLocalData i=" + f.getPath());
                if (isImpactVideo(f.getName())) {
                    lastImpactFile = f;
                    continue;
                }

                if (f.getName().endsWith("kml")) {
                    // add by chengyuzhou
                    // if (f.getName() != null && f.getName().length() > 15) {
                    // time = f.getName().substring(0, 15);
                    // }
                    // Log.d(TAG, "kml time=" + time);

                    // end
                    /*
                     * if (!(new File(Storage.DIRECTORY + "/" + time + "A" +
                     * SUBFIX_1_MIN + ".ts").exists()) && !(new
                     * File(Storage.DIRECTORY + "/" + time + "A" + SUBFIX_1_MIN
                     * + ".mp4").exists()) && !(new File(Storage.DIRECTORY + "/"
                     * + time + "A" + SUBFIX_2_MIN + ".ts").exists()) && !(new
                     * File(Storage.DIRECTORY + "/" + time + "A" + SUBFIX_2_MIN
                     * + ".mp4").exists()) && !(new File(Storage.DIRECTORY + "/"
                     * + time + "A" + SUBFIX_3_MIN + ".ts").exists()) && !(new
                     * File(Storage.DIRECTORY + "/" + time + "A" + SUBFIX_3_MIN
                     * + ".mp4").exists())) { // to do handle IMPACT_SUFFIX
                     * files??? f.delete(); }
                     */

                    // after sort, kml should be after ts or mp4,
                    // now kml is not owned to ts or mp4, should be deleted
                    // add by chengyuzhou
                    /*
                     * if (f.exists() && time != null && (lastImpactFile == null
                     * || (lastImpactFile != null &&
                     * !lastImpactFile.getName().contains(time)))) { Log.d(TAG,
                     * "delete kml=" + f.getPath()); f.delete(); recycledSize +=
                     * f.length(); }
                     */
                    // end
                }
                if (f.getName().endsWith("jpg")) {
                    if (f.exists()) {
                        Log.d(TAG, " jpg=" + f.getPath());
                        f.delete();
                        recycledSize += f.length();
                    }
                }
                if ((f.getName().endsWith("mp4") || f.getName().endsWith("ts")) && !f.getName().contains(IMPACT_SUFFIX)
                        && f.getPath().contains(Storage.DIRECTORY) && !isRecordingFile(f.getPath())) {
                    recycledSize += f.length();
                    f.delete();
                    if (f.getName() != null && f.getName().length() > 15) {
                        time = f.getName().substring(0, 15);
                    }
                    Log.d(TAG, "time=" + time);
                    /*
                     * kml = new File(Storage.DIRECTORY + "/" + time + "K.kml");
                     * if (kml.exists()) { kml.delete(); recycledSize +=
                     * kml.length(); }
                     */
                    if (f.getName().contains("A")) {
                        backFile = new File(Storage.DIRECTORY + "/" + f.getName().replace('A',
                                'B'));
                        Log.d(TAG, "backFile=" + backFile.getPath());
                    } else if (f.getName().contains("B")) {
                        backFile = new File(Storage.DIRECTORY + "/" + f.getName().replace('B',
                                'A'));
                        Log.d(TAG, "backFile=" + backFile.getPath());
                    } else {
                        backFile = null;
                    }
                    if (backFile != null && backFile.exists()) {
                        backFile.delete();
                        recycledSize += backFile.length();
                    }
                    for (int i = dataCount - 1; i > 0; i--) {
                        if (mDataAdapter.getLocalData(i) != null && f.getPath() != null
                                && mDataAdapter.getLocalData(i).getPath() != null
                                && (mDataAdapter.getLocalData(i).getPath().equals(f.getPath()) || (backFile != null
                                && mDataAdapter.getLocalData(i).getPath().equals(backFile.getPath())))) {
                            Log.d(TAG, "cleanLocalData clean dataAdapter=" + f.getPath());
                            mDataAdapter.removeData(this, i);
                            if (mMainHandler != null) {
                                mMainHandler.removeMessages(MSG_BROADCAST_FILE_UPDATE);
                                mMainHandler.sendEmptyMessageDelayed(MSG_BROADCAST_FILE_UPDATE,
                                        BROADCASE_FILE_DELAY);
                            }
                        }
                    }
                }

                if (recycledSize >= needSize) {
                    break;
                }
            }
        }
        Log.d(TAG, "local recycledSize = " + recycledSize);
        return recycledSize >= needSize;
    }

    private void startSpaceCheck() {
        Log.d(TAG, "startSpaceCheck");
        mStartSpaceCheck = true;
        if (mRecorderHandler != null) {
            mRecorderHandler.removeMessages(MSG_START_SPACE_CHECK);
            mRecorderHandler.sendEmptyMessage(MSG_START_SPACE_CHECK);
        }
    }

    private void stopSpaceCheck() {
        mStartSpaceCheck = false;
        if (mRecorderHandler != null) {
            mRecorderHandler.removeMessages(MSG_START_SPACE_CHECK);
        }
    }

    private class MyMainHanlder extends Handler {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_CHECK_CAMERA_OPENED:
                    boolean isRightCameraOpen = isCameraOpened(CameraInfo.CAMERA_FACING_FRONT);
                    Log.e(TAG,
                            "---MSG_CHECK_CAMERA_OPENED " + ",isRightCameraOpen = " + isRightCameraOpen);
                    if (isRightCameraOpen) {
                        showFullscreenCameraInner(FullScreenCameraActivity.STATE_FULLSCREEN_SHOW_CAMERA_BACK, false, true);
                    } else {
                        mMainHandler.sendEmptyMessageDelayed(MSG_CHECK_CAMERA_OPENED, 500);
                    }
                    break;
                case MSG_NO_SPACE:
                    /*
                     * stopSpaceCheck(); for (int i = 0; i < mRecorderList.size();
                     * i++) { mRecorderList.get(i).stopRecording(); }
                     */
                    // mRecorderList.clear();
                    stopRecording();
                    break;
                case MSG_CHECK_DATA_ADAPTER:
                    /*
                     * if(mDataAdapter != null){//just for debug Log.d(TAG,
                     * "MSG_CHECK_DATA_ADAPTER mDataAdapter" +
                     * mDataAdapter.getTotalNumber()); }
                     */
                    if (mMainHandler != null) {
                        mMainHandler.sendEmptyMessageDelayed(MSG_CHECK_DATA_ADAPTER, 1000);
                    }
                    break;
                case MSG_START_SWTICH_FILE:
                    long cur = System.currentTimeMillis();
                    long dura = Math.abs(cur - mStartTime);
                    long recDura = 0;
                    boolean mIsOnce = false;
                    for (int i = 0; i < mRecorderList.size(); i++) {
                        if (mRecorderList.get(i) != null) {
                            recDura = mRecorderList.get(i).getRecDuration();
                            break;
                        }
                    }
                    if (dura > recDura) {
                        mStartTime = System.currentTimeMillis();
                        for (int i = 0; i < mServiceListenerList.size(); i++) {
                            if (mServiceListenerList.get(i) != null) {
                                mServiceListenerList.get(i).onTimeUpdate(0);
                            }
                        }
                        if (mIsLockOnce) {
                            mIsLockOnce = false;
                            mIsOnce = true;
                            stopRecording();
                        } else {
                            mCurTime = System.currentTimeMillis();
                            for (int i = 0; i < mRecorderList.size(); i++) {
                                if (mRecorderList.get(i) != null) {
                                    recDura = mRecorderList.get(i).switchToNextFile();
                                    Log.v(TAG, "-----------switchToNextFile---------------：");
                                }
                            }
                        }
                    } else {
                        for (int i = 0; i < mServiceListenerList.size(); i++) {
                            if (mServiceListenerList.get(i) != null) {
                                mServiceListenerList.get(i).onTimeUpdate(dura);
                            }
                        }
                    }
                    if (!mIsOnce && mMainHandler != null) {
                        mMainHandler.removeMessages(MSG_START_SWTICH_FILE);
                        mMainHandler.sendEmptyMessageDelayed(MSG_START_SWTICH_FILE,
                                SWTICH_FILE_DELAY);
                    }
                    break;
                case MSG_WAITING_ACTIVITY_FINISH:
                    boolean isActivityStillOn = false;
                    ActivityManager am =
                            (ActivityManager) RecordService.this.getSystemService(Context.ACTIVITY_SERVICE);
                    PackageManager pm = RecordService.this.getPackageManager();
                    int leftStack = -1;
                    leftStack = SplitUtil.getLeftStackId(RecordService.this);
                    RecentTaskInfo ti = SplitUtil.getTopTaskOfStack(RecordService.this, leftStack);
                    if (ti != null) {
                        Intent intent = ti.baseIntent;
                        ResolveInfo resolveInfo = pm.resolveActivity(intent, 0);
                        if (SplitUtil.getStackId(ti) == leftStack) {
                            if ((intent.getComponent().getPackageName()).equals(PACKAGE_NAME)) {
                                isActivityStillOn = true;
                            }
                        }
                    }

//				Log.d(TAG, "MSG_WAITING_ACTIVITY_FINISH isActivityStillOn=" + isActivityStillOn);
//				Log.d(TAG, "mFrontOnLeft=" + mFrontOnLeft + ";" + mStartId);
                    if (isActivityStillOn /* && !mFrontOnLeft */ && mMainHandler != null) {
                        // camera2 still finishing,wait
                        mMainHandler.sendEmptyMessageDelayed(MSG_WAITING_ACTIVITY_FINISH, 300);
                    } else {
                        if (mStartId < 0) {
                            // just wait camera2 down
                        } else {
                            // start another camera2
                            startFloat();
                            Intent it = new Intent(RecordService.this, RecorderActivity.class);
                            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            if (mStartId == CameraInfo.CAMERA_FACING_FRONT) {
                                it.putExtra(EXTRA_CAM_TYPE, CameraInfo.CAMERA_FACING_FRONT);
                                it.putExtra(EXTRA_NO_ADAS_FLAG, true);
                            } else if (mStartId == CameraInfo.CAMERA_FACING_BACK
                                    || mStartId == RecorderActivity.CAMERA_THIRD) {
                                // it.putExtra(EXTRA_CAM_TYPE, mStartId);
                                it.putExtra(EXTRA_CAM_TYPE, CameraInfo.CAMERA_FACING_FRONT);
                                it.addFlags(FLAG_ACTIVITY_RUN_IN_RIGHT_WINDOW);
                                it.putExtra(EXTRA_NO_ADAS_FLAG, true);
                            } else if (mStartId == RecordService.CAMERA_ID_FRONT) {
                                // it.putExtra(POWER_ON_START, true);
                                it.addFlags(FLAG_ACTIVITY_RUN_IN_RIGHT_WINDOW);
                                // it.putExtra(EXTRA_CAM_TYPE,
                                // CameraInfo.CAMERA_FACING_FRONT);
                                it.putExtra(EXTRA_CAM_TYPE, CameraInfo.CAMERA_FACING_FRONT);
                                it.putExtra(EXTRA_NO_ADAS_FLAG, true);
                            } else {
                                Log.d(TAG, "error mStartId =" + mStartId);
                                // it.putExtra(EXTRA_CAM_TYPE,
                                // CameraInfo.CAMERA_FACING_BACK);
                                it.putExtra(EXTRA_CAM_TYPE, CameraInfo.CAMERA_FACING_FRONT);
                                it.addFlags(FLAG_ACTIVITY_RUN_IN_RIGHT_WINDOW);
                                it.putExtra(EXTRA_NO_ADAS_FLAG, true);
                            }
                            it.putExtra(IS_OPEN_AGAIN, true);
                            RecordService.this.startActivity(it);
                        }
                        mIsChangingFloat = false;
                    }
                    break;
                case MSG_WAITTING_MINI_OVER:
                    Log.d(TAG, "stop mini record");
                    RecordService.this.stopRecording();
                    break;
                case MSG_RECEIVED_HOME_DELAY:
                    for (int i = 0; i < mServiceListenerList.size(); i++) {
                        if (mServiceListenerList.get(i) != null) {
                            mServiceListenerList.get(i).onHomePressed();
                        }
                    }
                    if (!mIsChangingFloat) {
                        Intent it = new Intent(RecordService.this, RecorderActivity.class);
                        it.putExtra(POWER_ON_START, true);
                        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        it.addFlags(FLAG_ACTIVITY_RUN_IN_RIGHT_WINDOW);
                        if (getFloatCameraId() == CameraInfo.CAMERA_FACING_FRONT) {
                            it.putExtra(EXTRA_CAM_TYPE, CameraInfo.CAMERA_FACING_BACK);
                        } else {
                            it.putExtra(EXTRA_CAM_TYPE, CameraInfo.CAMERA_FACING_FRONT);
                        }
                        startActivity(it);
                    }
                    mIsHomePressing = false;
                    break;
                case MSG_CHECK_ACC_WAKE:
                    Log.d(TAG, "MSG_CHECK_ACC_WAKE");
                    if (mGsensorWake && mMainHandler != null) {
                        mMainHandler.removeMessages(MSG_GSENSOR_WAKE_OVER);
                        RecordService.this.stopRecordingSync();
                    }
                    if (mIsSleeping) {
                        onAccWakeUp();
                    }
                    mGsensorWake = false;
                    mIsMiniMode = false;
                    break;
                case MSG_GSENSOR_WAKE:
                    Log.d(TAG, "MSG_GSENSOR_WAKE");
                    if (mIsSleeping) {
                        onGsensorWakeUp();
                    }
                    break;
                case MSG_GSENSOR_WAKE_OVER:
                    if (mIsSleeping) {
                        onGsensorSleep();
                    } else {
                        Log.d(TAG, "MSG_GSENSOR_WAKE_OVER error");
                    }
                    break;
                case MSG_ON_TVD_CHANGE:
                    if (msg.arg1 == 1) {
                        Log.d(TAG, "tvd plug in");
                        onCameraPlugIn();
                    } else if (msg.arg1 == 0) {
                        Log.d(TAG, "tvd plug out");
                        onCameraPlugOut();
                    }
                    break;
                case MSG_ON_BACK_CHANGE:
                    if (msg.arg1 == 1) {
                        if (mBackInsertImpl != null) {
                            Log.d(TAG, "back plug in");
                            mBackInsertImpl.isBackInsert(true);
                            myPreCameraPlug.edit().putBoolean("isBackCamIn", true).commit();
                        }
                    } else if (msg.arg1 == 0) {
                        if (mBackInsertImpl != null) {
                            Log.d(TAG, "back plug out");
                            mBackInsertImpl.isBackInsert(false);
                            myPreCameraPlug.edit().putBoolean("isBackCamIn", false).commit();
                        }
                    }
                    break;
                case MSG_ON_RIGHT_CHANGE:
                    if (msg.arg1 == 1) {
                        if (mRightInsertImpl != null) {
                            Log.d(TAG, "right plug in");
                            mRightInsertImpl.isRightInsert(true);
                            myPreCameraPlug.edit().putBoolean("isRightCamIn", true).commit();
                        }
                    } else if (msg.arg1 == 0) {
                        if (mRightInsertImpl != null) {
                            Log.d(TAG, "right plug out");
                            mRightInsertImpl.isRightInsert(false);
                            myPreCameraPlug.edit().putBoolean("isRightCamIn", false).commit();
                        }
                    }
                    break;
                case MSG_WAIT_DONE_TO_START:
                    Log.d(TAG, "---MSG_WAIT_DONE_TO_START");
                    handleCameraPlugIn();
                    break;
                case MSG_UI_HANDLE_BACK_OUT:
                    Log.d(TAG, "---MSG_UI_HANDLE_BACK_OUT");
                    if (mIsAskResetCamera) {
                        setCameraPlug(true);
                    }
                    break;
                case MSG_BROADCAST_FILE_UPDATE:
                    sendBroadcast(new Intent(ACTION_RECORD_FILES_UPDATE));
                    break;
                case MSG_SD_HANDLE_DELAY:
                    if (isRecorderBusy()) {
                        if (mMainHandler != null && msg.arg2 < 10) {
                            mMainHandler.removeMessages(MSG_SD_HANDLE_DELAY);
                            Message sdMsg = mMainHandler.obtainMessage(MSG_SD_HANDLE_DELAY,
                                    msg.arg1, msg.arg2 + 1);
                            mMainHandler.sendMessageDelayed(sdMsg, SD_HANDLE_DELAY);
                        }
                    } else {
                        if (msg.arg1 == 0) {
                            Log.d(TAG, "startRecording handleMessage:MSG_SD_HANDLE_DELAY ");
                            RecordService.this.startRecording();
                        } else if (msg.arg1 == 1) {
                            RecordService.this.stopRecording();
                        }
                    }
                    break;
                case MSG_SHOW_STREAM_MEDIA_WINDOW:
                    if (isSupportStreamMedia || isSupportBackCameraFullScreen) {
                        if (mIsQuickShow) {
                            Log.e(TAG, "---quick---stream---media---");
                            showQuickStreamMediaWindow();
                            mIsQuickShow = false;
                        } else {
                            showStreamMediaWindow();
                        }
                    }
                    break;
                case MSG_HIDE_STREAM_PREVIEW_WINDOW:
                    hideStreamPreViewWindow();
                    break;
                case MSG_SHOW_STREAM_PREVIEW_WINDOW:
                    showStreamPreViewWindow();
                    break;
                case MSG_CLOSE_STREAM_PREVIEW_WINDOW:
                    Log.i(TAG, "close ---- preview------");
                    closeStreamPreViewWindow();
                    break;
                case MSG_HIDE_STREAM_MEDIA_WINDOW:
                    if (isSupportStreamMedia || isSupportBackCameraFullScreen) {
                        hideStreamMediaWindow();
                    }
                    break;
                case MSG_CLOSE_STREAM_MEDIA_WINDOW:
                    if (isSupportStreamMedia || isSupportBackCameraFullScreen) {
                        closeStreamMediaWindow();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    public void setFrontCamState(boolean isOnLeft) {
        mFrontOnLeft = isOnLeft;
    }

    private class RecorderHandler extends Handler {
        RecorderHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_START_SPACE_CHECK:
                    Log.d(TAG, "MSG_START_SPACE_CHECK =" + mStartSpaceCheck);
                    if (mStartSpaceCheck) {
                        if (!checkStorageNeedRecycle()) {
                            // cleanTmpFiles();
                            if (!cleanLocalData()) {
                                cleanTmpFiles();
                            }
                            if (!cleanLocalData() && Storage.getAvailableSpace() < mRecordExitSpace) {
                                if (mMainHandler != null) {
                                    mMainHandler.sendEmptyMessage(MSG_NO_SPACE);
                                }
                            }
                        }
                    }
                    if (mStartSpaceCheck && mRecorderHandler != null) {
                        mRecorderHandler.sendEmptyMessageDelayed(MSG_START_SPACE_CHECK,
                                SPACE_CHECK_DELAY);
                    }
                    break;
                case MSG_START_CLEAR_FILE:
                    cleanTmpFiles();
                    cleanLocalData();
                    break;
                case MSG_PLAY_SHUTTER_CLICK:
                    if (mSoundPool == null || !mIsSoundLoaded) {
                        return;
                    }
                    mSoundPool.play(mSoundId, 1.0f, 1.0f, 0, 0, 1.0f);
                    break;
                case MSG_CHECK_ACC_STATUS:
                    boolean accStatus = readfileStatus(FILE_ACC_STATUS);
                    if (accStatus/*
                     * && mPowerManager != null &&
                     * mPowerManager.isScreenOn()
                     */) {
                        mAccCount++;
                    } else {
                        mAccCount = 0;
                    }
                    if (accStatus && !mLastAccStatus) {
                        if (mShutdownWindow != null) {
                            mShutdownWindow.onShow();
                        }
                    } else if (!accStatus && mLastAccStatus) {
                        if (mShutdownWindow != null) {
                            mShutdownWindow.onHide();
                        }
                        if (mIsSleeping && mMainHandler != null) {
                            mMainHandler.removeMessages(MSG_CHECK_ACC_WAKE);
                            mMainHandler.sendEmptyMessage(MSG_CHECK_ACC_WAKE);
                        }
                        if (mPowerManager != null && !mPowerManager.isScreenOn()) {
                            Log.d(TAG, "wakeup screen in screen off");
                            PowerManager.WakeLock wl = mPowerManager.newWakeLock(
                                    PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_DIM_WAKE_LOCK, "bright");
                            if (wl != null) {
                                wl.acquire();
                                wl.release();
                            }
                        }
                    } else if (mAccCount > KEEP_DOWN_TIME) {
                        if (mShutdownWindow != null) {
                            mShutdownWindow.onShow();
                        }
                        mAccCount = 0;
                        if (mPowerManager != null && !mPowerManager.isScreenOn()) {
                            Log.d(TAG, "wakeup screen in screen off");
                            PowerManager.WakeLock wl = mPowerManager.newWakeLock(
                                    PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_DIM_WAKE_LOCK, "bright");
                            if (wl != null) {
                                wl.acquire();
                                wl.release();
                            }
                        }
                    }
                    mLastAccStatus = accStatus;
                    if (mRecorderHandler != null) {
                        mRecorderHandler.sendEmptyMessageDelayed(MSG_CHECK_ACC_STATUS,
                                CHECK_ACC_DELAY);
                    }
                    break;
                case MSG_CHECK_GSENSOR_STATUS:
                    if (mGsensorInputPath == null && mGsensorFindInputTime < MAX_TRY_NUM) {
                        mGsensorInputPath = getInputDevicePath();
                        mGsensorFindInputTime++;
                    }
                    if (mGsensorInputPath != null && mIsSleeping) {
                        boolean status = readfileStatus(mGsensorInputPath + GSENSOR_STATUS_NODE);
                        Log.d(TAG, "MSG_CHECK_GSENSOR_STATUS status=" + status);
                        Log.d(TAG,
                                "GSENSOR_STATUS_NODE=" + mGsensorInputPath + GSENSOR_STATUS_NODE);
                        if (status) {
                            mAccCount = 0;
                            if (mMainHandler != null) {
                                mMainHandler.removeMessages(MSG_GSENSOR_WAKE);
                                mMainHandler.sendEmptyMessage(MSG_GSENSOR_WAKE);
                            }
                            if (mPowerManager != null && !mPowerManager.isScreenOn()) {
                                Log.d(TAG, "wakeup screen in screen off");
                                PowerManager.WakeLock wl = mPowerManager.newWakeLock(
                                        PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_DIM_WAKE_LOCK, "bright");
                                if (wl != null) {
                                    wl.acquire();
                                    wl.release();
                                }
                            }
                        }
                    }
                    if (mGsensorInputPath != null || mGsensorFindInputTime < MAX_TRY_NUM) {
                        if (mRecorderHandler != null) {
                            mRecorderHandler.removeMessages(MSG_CHECK_GSENSOR_STATUS);
                            mRecorderHandler.sendEmptyMessageDelayed(MSG_CHECK_GSENSOR_STATUS,
                                    CHECK_ACC_DELAY);
                        }
                    }
                    break;
                case MSG_GSENSOR_ENABLE:
                    if (mGsensorInputPath == null && mGsensorFindInputTime < MAX_TRY_NUM) {
                        mGsensorInputPath = getInputDevicePath();
                        mGsensorFindInputTime++;
                    }
                    if (mGsensorInputPath != null) {
                        writefileStatus(mGsensorInputPath + GSENSOR_ENABLE_NODE, msg.arg1);
                    }
                    break;
                case MSG_GSENSOR_SET_SENSITY:
                    if (mGsensorInputPath == null && mGsensorFindInputTime < MAX_TRY_NUM) {
                        mGsensorInputPath = getInputDevicePath();
                        mGsensorFindInputTime++;
                    }
                    if (mGsensorInputPath != null) {
                        writefileStatus(mGsensorInputPath + GSENSOR_SENSITY_NODE, msg.arg1);
                    }
                    break;
                case MSG_CHECK_BACK_INSERT:
                    boolean isBackCamIn;
                    isBackCamIn = readfileStatus(FILE_BACK_INSERT_STATUS);
                    boolean isfullWindow = SplitUtil.isFullWindow(RecordService.this);
                    if (isBackCamIn && !mFloatWindow.isShow()) {
//					Log.d(TAG, "MSG_CHECK_BACK_INSERT, showFloatWindows");
//					RecordService.this.sendBroadcast(new Intent(DoubleFloatWindow.ACTION_SHOW));
//					Log.d(TAG, "MSG_CHECK_BACK_INSERT, showFloatWindows");

//					Log.d(TAG,"MSG_CHECK_BACK_INSERT isfullWindow:"+isfullWindow);
                        //add by chengyuzhou
                        if (!isfullWindow) {
                            RecordService.this.sendBroadcast(new Intent(DoubleFloatWindow.ACTION_SHOW));
                        }

                    } else if (!isBackCamIn && mFloatWindow.isShow()) {
                        Log.d(TAG, "MSG_CHECK_BACK_INSERT, hideFloatWindows");
                        //add by chengyuzhou
			/*		if (!SplitUtil.isFullWindow(RecordService.this)) {
						Log.d(TAG,"MSG_CHECK_BACK_INSERT not fullWindow");
                        RecordService.this.sendBroadcast(new Intent(DoubleFloatWindow.ACTION_SHOW));
						}else {
							Log.d(TAG,"MSG_CHECK_BACK_INSERT  fullWindow");
						} */

                        RecordService.this.sendBroadcast(new Intent(DoubleFloatWindow.ACTION_HIDE));
                    }
                    //Log.d(TAG, "isBackCamIn: " + isBackCamIn + " === " + msg.arg1);
                    if (isFirstCheckBack) {
                        myPreCameraPlug.edit().putBoolean("isBackCamIn", isBackCamIn).commit();
                        isFirstCheckBack = false;
                    }
                    if (isBackCamIn && msg.arg1 == 0) {
                        if (mMainHandler != null) {
                            mMainHandler.sendMessage(mMainHandler.obtainMessage(MSG_ON_BACK_CHANGE, 1, 0));
                        }
                    } else if (!isBackCamIn && msg.arg1 == 1) {
                        if (mMainHandler != null) {
                            mMainHandler.sendMessage(mMainHandler.obtainMessage(MSG_ON_BACK_CHANGE, 0, 0));
                        }
                    }
                    if (mRecorderHandler != null) {
                        Message checkTvd = mRecorderHandler.obtainMessage();
                        checkTvd.what = MSG_CHECK_BACK_INSERT;
                        checkTvd.arg1 = isBackCamIn ? 1 : 0;
                        mRecorderHandler.sendMessageDelayed(checkTvd, CHECK_TVD_DELAY);
                    }
                    break;
                case MSG_CHECK_RIGHT_INSERT:
                    boolean isRightCamIn;
                    isRightCamIn = readfileStatus(FILE_RIGHT_INSERT_STATUS);
                    Log.d(TAG, "isRightCamIn: " + isRightCamIn + " === " + msg.arg1);
                    if (isFirstCheckRight) {
                        myPreCameraPlug.edit().putBoolean("isRightCamIn", isRightCamIn).commit();
                        isFirstCheckRight = false;
                    }
                    if (isRightCamIn && msg.arg1 == 0) {
                        if (mMainHandler != null) {
                            mMainHandler.sendMessage(mMainHandler.obtainMessage(MSG_ON_RIGHT_CHANGE, 1, 0));
                        }
                    } else if (!isRightCamIn && msg.arg1 == 1) {
                        if (mMainHandler != null) {
                            mMainHandler.sendMessage(mMainHandler.obtainMessage(MSG_ON_RIGHT_CHANGE, 0, 0));
                        }
                    }
                    if (mRecorderHandler != null) {
                        Message checkTvd = mRecorderHandler.obtainMessage();
                        checkTvd.what = MSG_CHECK_RIGHT_INSERT;
                        checkTvd.arg1 = isRightCamIn ? 1 : 0;
                        mRecorderHandler.sendMessageDelayed(checkTvd, CHECK_TVD_DELAY);
                    }
                    break;
                case MSG_CHECK_TVD:
                    boolean isTvdIn;
                    if (isFastTest || (!isNeedCheckRear)) {
                        isTvdIn = true;
                    } else {
                        isTvdIn = true;// readfileStatus(FILE_TVD_STATUS);
                    }
                    /*
                     * if (readfileStatus(FILE_TVD_SYSTEM)) { }
                     */
                    // Log.v(TAG,"-----------isTvdIn-------:"+isTvdIn);
                    if (isFirstCheck) {
                        isCameraPlugIn = isTvdIn;
                        myPreCameraPlug.edit().putBoolean("isCameraPlugIn", true).commit();
                        if (isTvdIn) {
                            mMainHandler.sendEmptyMessageDelayed(MSG_SHOW_STREAM_MEDIA_WINDOW,
                                    STREAM_MEDIA_WINDOW_DELAY_SHOW);
                        }
                        isFirstCheck = false;
                    }

                    if (isTvdIn && msg.arg1 == 0) {
                        if (mMainHandler != null) {
                            mMainHandler.sendMessage(mMainHandler.obtainMessage(MSG_ON_TVD_CHANGE
                                    , 1, 0));
                        }
                    } else if (!isTvdIn && msg.arg1 == 1) {
                        if (mMainHandler != null) {
                            mMainHandler.sendMessage(mMainHandler.obtainMessage(MSG_ON_TVD_CHANGE
                                    , 0, 0));
                        }
                    }
                    if (mRecorderHandler != null) {
                        Message checkTvd = mRecorderHandler.obtainMessage();
                        checkTvd.what = MSG_CHECK_TVD;
                        checkTvd.arg1 = isTvdIn ? 1 : 0;
                        mRecorderHandler.sendMessageDelayed(checkTvd, CHECK_TVD_DELAY);
                    }
                    break;
                case MSG_RESET_CAMERA:
                    Log.d(TAG, "MSG_RESET_CAMERA try=" + msg.arg1);
                    if (msg.arg1 < MAX_TRY_RESET_TIME && mIsAskResetCamera) {
                        Log.d(TAG, "MSG_RESET_CAMERA try in");
                        writefileStatus(RESET_CAMERA_NODE, 1);
                        if (mRecorderHandler != null) {
                            Message retry = mRecorderHandler.obtainMessage();
                            retry.arg1 = msg.arg1 + 1;
                            retry.what = MSG_RESET_CAMERA;
                            mRecorderHandler.sendMessageDelayed(retry, TRY_RESET_DELAY);
                        }
                    }
                    break;
                case MSG_WAIT_CAMERA_DONE:
                    for (int i = 0; i < mRecorderList.size(); i++) {
                        if (mRecorderList.get(i).getCameraId() == CameraInfo.CAMERA_FACING_BACK) {
                            mRecorderList.get(i).stopRecording();
                            mRecorderList.get(i).waitDone();
                        }
                    }
                    for (int i = 0; i < mRecorderList.size(); i++) {
                        if (mRecorderList.get(i).getCameraId() == CameraInfo.CAMERA_FACING_FRONT) {
                            mRecorderList.get(i).stopRecording();
                            mRecorderList.get(i).waitDone();
                        }
                    }
                    if (mRecorderHandler != null && mMainHandler != null
                            && !mRecorderHandler.hasMessages(MSG_WAIT_CAMERA_DONE)) {
                        mMainHandler.sendEmptyMessage(MSG_WAIT_DONE_TO_START);
                    }
                    break;
                case 1001:
                    RecordService.this.startPreview(2);
                    // mFloatWindow.setBackFloatWindow(true);
                    break;
                default:
                    break;
            }
        }
    }

    private void cleanTmpFiles() {
        CameraDataAdapter.cleanInvalidVideoFile(getContentResolver());
        TmpFileCleaner cleaner = TmpFileCleaner.getInstance();
        TmpFileCleaner.cleanLostFiles();
        cleaner.searchTmpFiles();
        if (mRecorderList.size() > 0) {
            if (cleaner.getTmpFiles().size() > 0) {
                for (File f : new ArrayList<File>(cleaner.getTmpFiles())) {
                    for (int i = 0; i < mRecorderList.size(); i++) {
                        if (!mRecorderList.get(i).isRecordingFile(f.getName())) {
                            cleaner.getTmpFiles().remove(f);
                            f.delete();
                        }
                    }
                }
            }
        } else {
            cleaner.execClean();
        }
    }

    private class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(TAG, "action = " + action);
            if (action.equals(UsbCameraManager.ACTION_USB_CAMERA_PLUG_IN_OUT)) {
                Log.i(TAG, "ACTION_USB_CAMERA_PLUG_IN_OUT");

                Bundle bundle = intent.getExtras();
                if (bundle == null) {
                    Log.i(TAG, "bundle is null");
                    return;
                }

                final String name = bundle.getString(UsbCameraManager.USB_CAMERA_NAME);
                final int state = bundle.getInt(UsbCameraManager.USB_CAMERA_STATE);
                final int totalNum = bundle.getInt(UsbCameraManager.USB_CAMERA_TOTAL_NUMBER);
                final String msg = bundle.getString(UsbCameraManager.EXTRA_MNG);
                Log.i(TAG,
                        "usb camera name = " + name + " totalNum = " + totalNum + " state = " + state);

                if (state == 1) {
                    Log.i(TAG, "uvc camera " + name + " plug in");
                    onCameraPlugIn();
                } else {
                    Log.i(TAG, "uvc camera " + name + " plug out");
                    onCameraPlugOut();
                }
                return;
            } else if (action.equals(ACTION_TEMP_ACTION)) {
                String curTemp = intent.getStringExtra(PERFORMANCE_TEMP_CUR);
                String curAction = intent.getStringExtra(PERFORMANCE_TEMP_ACT);
                Log.i(TAG, "ACTION_TEMP_ACTION " + curTemp + curAction);
                if (curAction.equals(PERFORMANCE_TEMP_HIGH)) {
                    // RecordService.this.setPerformance(ADAS_LEVEL_3,
                    // REC_QUA_720P);
                    setAdasLevel(CameraInfo.CAMERA_FACING_FRONT, ADAS_LEVEL_3);
                    return;
                } else if (curAction.equals(PERFORMANCE_TEMP_MIDDLE)) {
                    setAdasLevel(CameraInfo.CAMERA_FACING_FRONT, ADAS_LEVEL_2);
                    return;
                } else if (curAction.equals(PERFORMANCE_TEMP_NORMAL)) {
                    // MyPreference pref =
                    // MyPreference.getInstance(RecordService.this);
                    // int cur_qua =
                    // pref.getRecQuality(CameraInfo.CAMERA_FACING_FRONT);
                    // Log.i(TAG, "cur_qua= " + cur_qua);
                    // RecordService.this.setPerformance(ADAS_LEVEL_1, cur_qua);
                    setAdasLevel(CameraInfo.CAMERA_FACING_FRONT, ADAS_LEVEL_1);
                    return;
                } else if (curAction.equals(PERFORMANCE_TEMP_LOW)) {
                    return;
                }
                return;
            }

            final Uri uri = intent.getData();
            String path = uri.getPath();

            if (action.equals(Intent.ACTION_MEDIA_EJECT)) {
                Log.i(TAG, "Intent.ACTION_MEDIA_EJECT path = " + path);
                if (path.equals("/mnt/extsd")) {
                    Log.i(TAG, "hide sdcard");
                    if (mToast != null) {
                        mToast.cancel();
                    }
                    // mToast = Toast.makeText(RecordService.this, "SD out",
                    // Toast.LENGTH_LONG);
                    // mToast.show();
                    Intent intent4 = new Intent();
                    intent4.setAction("com.action.other_Text");
                    intent4.putExtra("otherText", "SD卡已卸载");
                    sendBroadcast(intent4);
                    mIsSDOut = true;
                    mSdStateKeeperList.clear();
                    StateKeeper sk = null;
                    for (int i = 0; i < mRecorderList.size(); i++) {
                        WrapedRecorder wr = mRecorderList.get(i);
                        sk = new StateKeeper();
                        sk.mCameraId = wr.getCameraId();
                        sk.mIsSDcardPlugout = true;
                        sk.mIsPreviewing = isPreview(sk.mCameraId);
                        sk.mIsRecording = RecordService.this.isRecording(sk.mCameraId);
                        sk.mIsRending = RecordService.this.isRender(sk.mCameraId);
                        sk.mIsWaterMarkRuning = RecordService.this.isWaterMarkRuning(sk.mCameraId);
                        Log.d(TAG, "stop cameraid=" + sk.mCameraId);
                        /*
                         * if (sk.mIsRecording) {
                         * RecordService.this.release(sk.mCameraId); }
                         */

                        if (sk.mIsWaterMarkRuning) {
                            RecordService.this.stopWaterMark(sk.mCameraId);
                        }
                        /*
                         * if (sk.mIsRending) {
                         * RecordService.this.stopRender(sk.mCameraId); } if
                         * (sk.mIsPreviewing) {
                         * RecordService.this.stopPreview(sk.mCameraId); }
                         */
                        // RecordService.this.closeCamera(sk.mCameraId);
                        mSdStateKeeperList.add(sk);
                    }
                    // RecordService.this.stopRecording();
                    if (mMainHandler != null) {
                        mMainHandler.removeMessages(MSG_SD_HANDLE_DELAY);
                        Message sdMsg = mMainHandler.obtainMessage(MSG_SD_HANDLE_DELAY, 1, 0);
                        mMainHandler.sendMessage(sdMsg);
                    }
                    if (mDataAdapter != null) {
                        mDataAdapter.flush();
                    }
                }
            } else if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
                Log.i(TAG, "Intent.ACTION_MEDIA_MOUNTED = " + path);

                if (path.equals("/mnt/extsd")) {
                    Log.i(TAG, "show sdcard");
                    mIsSDOut = false;
                    if (mToast != null) {
                        mToast.cancel();
                    }
                    // mToast = Toast.makeText(RecordService.this, "SD in",
                    // Toast.LENGTH_LONG);
                    // mToast.show();
                    Intent intent3 = new Intent();
                    intent3.setAction("com.action.other_Text");
                    intent3.putExtra("otherText", "SD卡已加载");
                    sendBroadcast(intent3);
                    boolean isRecording = false;
                    for (int i = 0; i < mSdStateKeeperList.size(); i++) {
                        StateKeeper sk = mSdStateKeeperList.get(i);
                        if (sk.mIsSDcardPlugout) {
                            // openCamera(sk.mCameraId);
                            /*
                             * if (sk.mIsPreviewing) {
                             * RecordService.this.startPreview(sk.mCameraId); }
                             * if (sk.mIsRending) {
                             * RecordService.this.startRender(sk.mCameraId); }
                             * if (sk.mIsRecording) {
                             * RecordService.this.startRecording(); }
                             */
                            if (sk.mIsRecording) {
                                isRecording = true;
                            }
                            if (sk.mIsWaterMarkRuning) {
                                RecordService.this.startWaterMark(sk.mCameraId);
                            }
                        }
                    }
                    if (isRecording) {
                        if (Storage.getTotalSpace() < 0) {
                            // todo more gentlly hint
                            Log.d(TAG, "startRecording sd not mounted");
                            if (mToast != null) {
                                mToast.cancel();
                            }
                            mToast = Toast.makeText(RecordService.this, R.string.sdcard_not_found
                                    , Toast.LENGTH_LONG);
                            mToast.show();
                        } else if (Storage.getSdcardBlockSize() < 64 * 1024) {
                            Log.d(TAG,
                                    "check sdcard failed, sdcard block size " + (Storage.getSdcardBlockSize() / 1024)
                                            + "k");
                            if (mToast != null) {
                                mToast.cancel();
                            }
                            mToast = Toast.makeText(RecordService.this,
                                    R.string.format_sdcard_message,
                                    Toast.LENGTH_LONG);
                            mToast.show();
                        } else if (RecordService.this.isMiniMode()) {
                            if (mToast != null) {
                                mToast.cancel();
                            }
                            mToast = Toast.makeText(RecordService.this, R.string.device_busy,
                                    Toast.LENGTH_LONG);
                            mToast.show();
                        } else {
                            Log.d(TAG, "startRecording my onReceive: ");
                            RecordService.this.startRecording();
                            /*
                             * if (mMainHandler != null) {
                             * mMainHandler.removeMessages(MSG_SD_HANDLE_DELAY);
                             * Message sdMsg =
                             * mMainHandler.obtainMessage(MSG_SD_HANDLE_DELAY,
                             * 0, 0); mMainHandler.sendMessage(sdMsg); }
                             */
                        }
                    }
                    mSdStateKeeperList.clear();
                    Intent it = new Intent();
                    it.setData(uri);
                    it.setAction(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    sendBroadcast(it);
                }
            } else if (action.equals(Intent.ACTION_MEDIA_SCANNER_FINISHED)) {
                String pp = intent.getDataString();
                Log.i(TAG, "Intent.ACTION_MEDIA_SCANNER_FINISHED = " + pp);
                if (mDataAdapter != null && Storage.getTotalSpace() > 0) {
                    // mDataAdapter.requestLoad(getContentResolver());
                }

            } else if (action.equals(Intent.ACTION_MEDIA_SCANNER_STARTED)) {
                String pp = intent.getDataString();
                Log.i(TAG, "Intent.ACTION_MEDIA_SCANNER_STARTED " + pp);

            }
        }
    }

    private void setPerformance(int adasLevel, int recQua) {
        boolean isRecording = RecordService.this.isRecording(CameraInfo.CAMERA_FACING_FRONT);
        if (isRecording) {
            stopRecording();
        }
        Log.i(TAG, "isRecording " + isRecording);
        int i = 0;
        setRecQuality(recQua);
        while (isRecording && RecordService.this.isRecorderBusy()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            i++;
            if (i >= 6) {
                break;
            }
        }
        if (isRecording) {
            Log.d(TAG, "startRecording setPerformance: ");
            startRecording();
        }
        Log.i(TAG, "setAdasLevel ");
        setAdasLevel(CameraInfo.CAMERA_FACING_FRONT, adasLevel);

    }

    public static class StateKeeper {
        public int mCameraId = -1;
        public boolean mIsRecording = false;
        public boolean mIsPreviewing = false;
        public boolean mIsRending = false;
        public boolean mIsWaterMarkRuning = false;
        public boolean mIsCameraPlugout = false;
        public boolean mIsSDcardPlugout = false;
        public boolean mIsFloating = false;
        public boolean mIsAdasOn = false;
    }

    public void sendScreenOutBroadCastStatus(boolean isShow) {
        Intent broadcast = new Intent();
        if (isShow) {
            broadcast.setAction("com.zqc.action.screen.out.show");
            Log.i(TAG, "-------sendScreenOutBroadCastStatus--------- true");
        } else {
            broadcast.setAction("com.zqc.action.screen.out.close");
            Log.i(TAG, "-------sendScreenOutBroadCastStatus--------- false");
        }
        RecordService.this.sendBroadcast(broadcast);
    }

    private void showStreamMediaWindow() {
        try {
            Context ctx = createPackageContext("com.android.settings", CONTEXT_IGNORE_SECURITY);
            SharedPreferences mpref = ctx.getSharedPreferences("isStreamMedia",
                    Context.MODE_MULTI_PROCESS | Context.MODE_WORLD_READABLE);
            Log.i(TAG, "stream media window ====" + mpref.getBoolean("enable_stream_media", false));
            if (mpref.getBoolean("enable_stream_media", false)) {
                if (isCameraPlugIn) {
                    if (null != streamMediaWindow) {
                        if (streamMediaWindow.isShow()) {
                            Log.i(TAG, "stream media window is already showed!");
                        } else {
                            streamMediaWindow.showWindow();
                            sendScreenOutBroadCastStatus(true);
                            // streamPreviewWindow.showWindow();
                        }
                    }
                } else {
                    Log.i(TAG, "back camera is not plugin in!");
                }
            } else {
                Log.i(TAG, "stream media function is disable!");
            }
        } catch (NameNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void showQuickStreamMediaWindow() {
        if (isCameraPlugIn) {
            if (null != streamMediaWindow) {
                if (streamMediaWindow.isShow()) {
                    Log.i(TAG, "qucik stream media window is already showed!");
                } else {
                    Log.i(TAG, "Quick stream media showWindow!");
                    mTopActivityPackName = getTopActivityPackName();
                    Log.d(TAG, "mTopActivityPackName: " + mTopActivityPackName);
                    if (mTopActivityPackName.equals("com.zqc.launcher")
                            || mTopActivityPackName.equals("com.android.camera2")
                            || mTopActivityPackName.equals("com.android.deskclock")) {
                        mTopActivityPackName = "";
                        Log.d(TAG, "Camera and QcHome is on TopActivity,not let them jump to the " +
                                "launcher");
                    } else {
                        launchApp("com.zqc.launcher");
                    }
                    streamMediaWindow.showWindow();
                    sendScreenOutBroadCastStatus(true);
                    // streamPreviewWindow.showWindow();
                }
            }
        } else {
            Toast.makeText(RecordService.this,
                    RecordService.this.getResources().getString(R.string.please_insert_back_cam),
                    Toast.LENGTH_LONG)
                    .show();
            Log.i(TAG, "back camera is not plugin in!");
        }
    }

    private void hideStreamMediaWindow() {
        if (null != streamMediaWindow) {
            if (streamMediaWindow.isShow()) {
                streamMediaWindow.hideWindow();
                sendScreenOutBroadCastStatus(false);
                // streamPreviewWindow.hideWindow();
            } else {
                Log.i(TAG, "stream media window is not show!");
            }
        }
    }

    private void showStreamPreViewWindow() {
        if (null != streamPreviewWindow) {
            if (streamPreviewWindow.isShow()) {
                Log.i(TAG, "qucik stream Preview window is already showed!");
            } else {
                Log.i(TAG, "quick stream Preview showWindow!");
                streamPreviewWindow.showWindow();
                sendScreenOutBroadCastStatus(true);
            }
        }
    }

    private void hideStreamPreViewWindow() {
        if (null != streamPreviewWindow) {
            if (streamPreviewWindow.isShow()) {
                streamPreviewWindow.hideWindow();
                sendScreenOutBroadCastStatus(false);
            } else {
                Log.i(TAG, "stream Preview window is not show!");
            }
        }
    }

    private void closeStreamPreViewWindow() {
        if (null != streamPreviewWindow) {
            if (streamPreviewWindow.isShow()) {
                streamPreviewWindow.closeWindow();
                sendScreenOutBroadCastStatus(false);
            } else {
                Log.i(TAG, "stream Preview window is not show!");
            }
        }
    }

    private void closeStreamMediaWindow() {
        if (null != streamMediaWindow) {
            if (streamMediaWindow.isShow()) {
                streamMediaWindow.closeWindow();
                sendScreenOutBroadCastStatus(false);
                // streamPreviewWindow.closeWindow();
            } else {
                Log.i(TAG, "stream media window is not show!");
            }
        }
    }

    private boolean isCameraPlugIn = false;
    private static final int STREAM_MEDIA_WINDOW_DELAY_SHOW = 10 * 1000;

    private void onCameraPlugIn() {
        Log.i(TAG, "onCameraPlugIn()");
        isCameraPlugIn = true;
        myPreCameraPlug.edit().putBoolean("isCameraPlugIn", true).commit();
        mMainHandler.sendEmptyMessageDelayed(MSG_SHOW_STREAM_MEDIA_WINDOW,
                STREAM_MEDIA_WINDOW_DELAY_SHOW);
        setCameraPlug(false);
        mIsAskResetCamera = false;
        mIsPlugRecording = false;
        if (isRecording(CameraInfo.CAMERA_FACING_FRONT)) {
            mIsPlugRecording = true;
        }
        /*
         * if (mRecorderHandler != null) { //add by chengyuzhou
         * mRecorderHandler.sendEmptyMessage(MSG_WAIT_CAMERA_DONE); //end }
         */
    }

    private void handleCameraPlugIn() {
        boolean recording = false;
        for (int i = 0; i < mRecorderList.size(); i++) {
            if (mRecorderList.get(i).isRecording()) {
                recording = true;
                break;
            }
        }
        if (mRecorderList.size() > 0 && recording) {
            if (!mStartSpaceCheck) {
                startSpaceCheck();
            }
            startSwitchFile();
        } else {
            if (mStartSpaceCheck) {
                stopSpaceCheck();
            }
            stopSwitchFile();
        }
        if (mStateKeeperList.size() <= 0) {
            openCamera(CameraInfo.CAMERA_FACING_BACK);
            // try{
            // Thread.sleep(500);
            // }catch(Exception e){

            // }
            startPreview(CameraInfo.CAMERA_FACING_BACK);
            startWaterMark(CameraInfo.CAMERA_FACING_BACK);
            if (getFloatCameraId() == CameraInfo.CAMERA_FACING_BACK) {
                if (!RecordService.this.getFastReverFlag()) {
                    RecordService.this.startFloat();
                }
            }
            if (RecordService.this.getFastReverFlag()) {
                Log.v(TAG, "---------handleCameraPlugIn--------onRerverseMode");
                onRerverseMode(true);
            }
        } else {
            for (int i = 0; i < mStateKeeperList.size(); i++) {
                if (mStateKeeperList.get(i).mIsCameraPlugout) {
                    StateKeeper sk = mStateKeeperList.get(i);
                    RecordService.this.openCamera(sk.mCameraId);
                    Log.v(TAG, "---------openInCamera--------");
                    if (sk.mIsPreviewing) {
                        Log.v(TAG, "---------InCamerastartPreview--------");
                        // RecordService.this.startPreview(sk.mCameraId);
                        // mFloatWindow.setBackFloatWindow(false);
                        mRecorderHandler.sendEmptyMessageDelayed(1001, 0);
                    }
                    /*
                     * if (sk.mIsRending) {
                     * RecordService.this.startRender(sk.mCameraId); } if
                     * (sk.mIsRecording) { RecordService.this.startRecording();
                     * }
                     */
                    if (sk.mIsWaterMarkRuning) {
                        RecordService.this.startWaterMark(sk.mCameraId);
                    }
                    if (getFloatCameraId() == CameraInfo.CAMERA_FACING_BACK) {
                        if (!RecordService.this.getFastReverFlag()) {
                            RecordService.this.startFloat();
                        }
                    }
                    if (RecordService.this.getFastReverFlag()) {
                        Log.v(TAG, "---------handleCameraPlugIn----else----onRerverseMode");
                        onRerverseMode(true);
                    }
                }
                mStateKeeperList.clear();
            }
        }
        if (mIsPlugRecording) {
            Log.d(TAG, "startRecording handleCameraPlugIn: ");
            RecordService.this.startRecording();
        }
    }

    private void onCameraPlugOut() {
        Log.i(TAG, "onCameraPlugOut()");
        isCameraPlugIn = false;
        myPreCameraPlug.edit().putBoolean("isCameraPlugIn", false).commit();
        mMainHandler.sendEmptyMessage(MSG_HIDE_STREAM_MEDIA_WINDOW);
        sendScreenOutBroadCastStatus(false);
        if (mRecorderList != null && mRecorderList.size() != 0) {
            mRecorderList.get(1).stopRender();
            mRecorderList.get(1).stopPreview();
        }

        if (mMainHandler != null) {
            mMainHandler.sendEmptyMessageDelayed(MSG_UI_HANDLE_BACK_OUT, TRY_RESET_DELAY);
        }
        mIsAskResetCamera = true;
        boolean isRecording = false;
        if (mRecorderHandler != null) {
            Message retry = mRecorderHandler.obtainMessage();
            retry.arg1 = 0;
            retry.what = MSG_RESET_CAMERA;
            mRecorderHandler.sendMessageDelayed(retry, TRY_RESET_DELAY);
        }
        if (isRecording(CameraInfo.CAMERA_FACING_FRONT)) {
            isRecording = true;
            // RecordService.this.stopRecordingSync();
        }
        if (RecordService.this.isCameraAdd(CameraInfo.CAMERA_FACING_BACK)) {
            StateKeeper sk = new StateKeeper();
            sk.mCameraId = CameraInfo.CAMERA_FACING_BACK;
            sk.mIsCameraPlugout = true;
            sk.mIsPreviewing = isPreview(sk.mCameraId);
            sk.mIsRecording = RecordService.this.isRecording(sk.mCameraId);
            sk.mIsRending = RecordService.this.isRender(sk.mCameraId);
            sk.mIsWaterMarkRuning = RecordService.this.isWaterMarkRuning(sk.mCameraId);
            mStateKeeperList.clear();
            mStateKeeperList.add(sk);
            if (sk.mIsRecording) {
                RecordService.this.release(sk.mCameraId);
            }
            if (sk.mIsWaterMarkRuning) {
                RecordService.this.stopWaterMark(sk.mCameraId);
            }
            if (sk.mIsRending) {
                RecordService.this.stopRender(sk.mCameraId);
            }
            if (sk.mIsPreviewing) {
                RecordService.this.stopPreview(sk.mCameraId);
            }
            RecordService.this.closeCamera(sk.mCameraId);
        }
        if (isRecording) {
            // RecordService.this.startRecording();
        }
    }

    /*
     * @Override public void onAWMoveDetection(int value, Camera camera) {
     * Log.d(TAG, "onAWMoveDetection"); if (value != 0) { //to do start
     * recording } else { //more than 30times,stop recording } }
     */

    public synchronized void setAdasListener(int cameraId, Camera.AdasDetectionListener listener) {
        for (int i = 0; i < mRecorderList.size(); i++) {
            if (cameraId == mRecorderList.get(i).getCameraId()) {
                mRecorderList.get(i).setAdasDetecttionListener(listener);
                break;
            }
        }
    }

    public synchronized void openCamera(int cameraId) {
        for (int i = 0; i < mRecorderList.size(); i++) {
            if (cameraId == mRecorderList.get(i).getCameraId()) {
                mRecorderList.get(i).openCamera();
                break;
            }
        }
    }

    public synchronized void openCameraSync(int cameraId) {
        for (int i = 0; i < mRecorderList.size(); i++) {
            if (cameraId == mRecorderList.get(i).getCameraId()) {
                mRecorderList.get(i).openCamera();
                mRecorderList.get(i).waitDone();
                break;
            }
        }
    }

    public synchronized void closeCamera(int cameraId) {
        for (int i = 0; i < mRecorderList.size(); i++) {
            if (cameraId == mRecorderList.get(i).getCameraId()) {
                mRecorderList.get(i).closeCamera();
                break;
            }
        }
    }

    public synchronized void closeCameraSync(int cameraId) {
        for (int i = 0; i < mRecorderList.size(); i++) {
            if (cameraId == mRecorderList.get(i).getCameraId()) {
                mRecorderList.get(i).closeCamera();
                mRecorderList.get(i).waitDone();
                break;
            }
        }
    }

    public synchronized void setMiniMode(int miniMode) {
        for (int i = 0; i < mRecorderList.size(); i++) {
            mRecorderList.get(i).setMiniMode(miniMode);
        }
    }

    public synchronized void setMiniListener() {
        for (int i = 0; i < mRecorderList.size(); i++) {
            mRecorderList.get(i).setMiniVideoTakenListener(this);
            mRecorderList.get(i).setMiniPictureTakenListener(this);
        }
    }

    public synchronized void handleLockOneFile() {
        for (int i = 0; i < mRecorderList.size(); i++) {
            mRecorderList.get(i).handleLockOneFile();
        }
    }

    public synchronized int checkMiniError(int sz, int sz_back) {
        if (mIsMiniMode) {
            return ERR_FAIL;
        }
        if (Storage.getTotalSpace() < 0) {
            return ERR_NO_TF;
        }

        if (mRecorderList.size() <= 0) {
            return ERR_FAIL;
        }
        boolean isFrontExist = false;
        for (int i = 0; i < mRecorderList.size(); i++) {
            if (mRecorderList.get(i).getCameraId() == CameraInfo.CAMERA_FACING_FRONT) {
                if (mRecorderList.get(i).isCanMini()) {
                    isFrontExist = true;
                }
            }
        }
        if (!isFrontExist) {
            return ERR_NO_FRONT_CAMERA;
        }

        boolean isBackExist = false;
        for (int i = 0; i < mRecorderList.size(); i++) {
            if (mRecorderList.get(i).getCameraId() == CameraInfo.CAMERA_FACING_BACK && !mIsBackCamOut) {
                if (mRecorderList.get(i).isCanMini()) {
                    isBackExist = true;
                }
            }
        }
        if (!isBackExist) {
            return ERR_NO_REAR_CAMERA;
        }

        int width = 0;
        int height = 0;
        if (sz_back == 1) {
            // width = 320;
            // height = 240;
            width = 640;
            height = 480;
        } else if (sz_back == 2) {
            width = 640;
            height = 480;
        } else if (sz_back == 3) {
            width = 848;
            height = 480;
        } else if (sz_back == 4) {
            width = 1280;
            height = 720;
        } else if (sz_back == 5) {
            width = 1920;
            height = 1080;
        }
        for (int i = 0; i < mRecorderList.size(); i++) {
            if (mRecorderList.get(i).getCameraId() == CameraInfo.CAMERA_FACING_BACK) {
                if (!mRecorderList.get(i).checkSize(width, height)) {
                    return ERR_OLD_REAR_CAMERA;
                }
            }
        }

        width = 0;
        height = 0;
        if (sz == 1) {
            // width = 320;
            // height = 240;
            width = 640;
            height = 480;
        } else if (sz == 2) {
            width = 640;
            height = 480;
        } else if (sz == 3) {
            width = 848;
            height = 480;
        } else if (sz == 4) {
            width = 1280;
            height = 720;
        } else if (sz == 5) {
            width = 1920;
            height = 1080;
        } else if (sz == 6) {
            width = 2304;
            height = 1296;
        }
        for (int i = 0; i < mRecorderList.size(); i++) {
            if (mRecorderList.get(i).getCameraId() == CameraInfo.CAMERA_FACING_FRONT) {
                if (!mRecorderList.get(i).checkSize(width, height)) {
                    return ERR_FAIL;
                }
            }
        }
        return ERR_OK;
    }

    public synchronized void setMiniSize(int sz, int sz_back) {
        int width = 320;
        int height = 240;
        if (sz == 1) {
            // width = 320;
            // height = 240;
            width = 640;
            height = 480;
        } else if (sz == 2) {
            width = 640;
            height = 480;
        } else if (sz == 3) {
            width = 848;
            height = 480;
        } else if (sz == 4) {
            width = 1280;
            height = 720;
        } else if (sz == 5) {
            width = 1920;
            height = 1080;
        } else if (sz == 6) {
            width = 2304;
            height = 1296;
        }
        for (int i = 0; i < mRecorderList.size(); i++) {
            if (mRecorderList.get(i).getCameraId() == CameraInfo.CAMERA_FACING_FRONT) {
                mRecorderList.get(i).setMiniSize(width, height);
                break;
            }
        }

        width = 320;
        height = 240;
        if (sz_back == 1) {
            // width = 320;
            // height = 240;
            width = 640;
            height = 480;
        } else if (sz_back == 2) {
            width = 640;
            height = 480;
        } else if (sz_back == 3) {
            width = 848;
            height = 480;
        } else if (sz_back == 4) {
            width = 1280;
            height = 720;
        } else if (sz_back == 5) {
            width = 1920;
            height = 1080;
        }
        for (int i = 0; i < mRecorderList.size(); i++) {
            if (mRecorderList.get(i).getCameraId() == CameraInfo.CAMERA_FACING_BACK) {
                mRecorderList.get(i).setMiniSize(width, height);
            }
        }
    }

    public synchronized boolean checkFileSize(String fileName) {
        if (fileName == null) {
            return false;
        }
        boolean res = false;
        File file = new File(fileName);
        if (file.exists() && file.length() < 4 * 1024 * 1024) {
            return true;
        }
        return false;
    }

    public synchronized void setMiniDuaration(int duaration) {
        int duara = 1;
        if (duaration == 1) {
            duara = 5 * 1000;
        } else if (duaration == 2) {
            duara = 10 * 1000;
        } else if (duaration == 3) {
            duara = 15 * 1000;
        } else if (duaration == 4) {
            duara = 20 * 1000;
        }
        for (int i = 0; i < mRecorderList.size(); i++) {
            mRecorderList.get(i).setMiniDuaration(duara);
        }
        mMiniduaration = duara;
    }

    public synchronized void addSpeedListener(ISpeedChangeListener ls) {
        for (int i = 0; i < mSpeedListenerList.size(); i++) {
            if (mSpeedListenerList.get(i) == ls) {
                return;
            }
        }
        mSpeedListenerList.add(ls);
    }

    public synchronized void removeSpeedListener(ISpeedChangeListener ls) {
        int index = -1;
        for (int i = 0; i < mSpeedListenerList.size(); i++) {
            if (mSpeedListenerList.get(i) == ls) {
                index = i;
                break;
            }
        }
        if (index >= 0 && index < mSpeedListenerList.size()) {
            mSpeedListenerList.remove(index);
        }
    }

    @Override
    public void onSpeedChange(float speed, int status, double longitude, double latitude) {
        // TODO Auto-generated method stub
        // Log.d(TAG, "onSpeedChange speed=" + speed);
        for (int i = 0; i < mSpeedListenerList.size(); i++) {
            mSpeedListenerList.get(i).onSpeedChange(speed, status, longitude, latitude);
        }
        setAdasSpeed(CameraInfo.CAMERA_FACING_FRONT, speed);
        setWaterMarkMultiple(speed, longitude, latitude);
        Intent intentSpeed = new Intent();
        intentSpeed.setAction(ACTION_SPEED);
        intentSpeed.putExtra("speed", String.valueOf((int) (speed * 3.6)));
        RecordService.this.sendBroadcast(intentSpeed);

    }

    public CameraDataAdapter getLocalDataAdapter() {
        return mDataAdapter;
    }

    public void notifyNewMedia(Uri uri) {
        ContentResolver cr = getContentResolver();
        String mimeType = cr.getType(uri);

        if (mimeType == null) {
            return;
        }

        if (mimeType.startsWith("video/")) {
            sendBroadcast(new Intent(CameraUtil.ACTION_NEW_VIDEO, uri));
            mDataAdapter.addNewVideo(cr, uri);
            if (mMainHandler != null) {
                mMainHandler.removeMessages(MSG_BROADCAST_FILE_UPDATE);
                mMainHandler.sendEmptyMessageDelayed(MSG_BROADCAST_FILE_UPDATE,
                        BROADCASE_FILE_DELAY);
            }
        } else if (mimeType.startsWith("image/")) {
            CameraUtil.broadcastNewPicture(this, uri);
            mDataAdapter.addNewPhoto(cr, uri);
        } else if (mimeType.startsWith("application/stitching-preview")) {
            mDataAdapter.addNewPhoto(cr, uri);
        } else {
            android.util.Log.w(TAG,
                    "Unknown new media with MIME type:" + mimeType + ", uri:" + uri);
        }
    }

    public boolean getFastReverFlag() {
        return mIsFastRevers;
    }

    public void setFastReverFlag(boolean isReverse) {
        mIsFastRevers = isReverse;
        if (mIsFastRevers) {
            mIsHideLines = false;
        }
        Log.v(TAG, "---------setFastReverFlag--------onRerverseMode");
        onRerverseMode(mIsFastRevers);
    }

    @Override
    public void onFastReverseBoot(boolean isReversing) {
        // TODO Auto-generated method stub
        Log.d(TAG, "onFastReverseBoot getFloatCameraId() =" + getFloatCameraId());
        mIsFastRevers = isReversing;
        if (mIsFastRevers) {
            mIsHideLines = false;
        }
        if (!isCameraAdd(CameraInfo.CAMERA_FACING_BACK) && isCameraAdd(CameraInfo.CAMERA_FACING_FRONT)) {
            stopRecordingSync();
            myPreSp.edit().putBoolean("isGsonLock", false).commit();
            addCamera(CameraInfo.CAMERA_FACING_BACK);
            startFloat();
            /*
             * if (isRecording(CameraInfo.CAMERA_FACING_BACK)) {
             * startRecording(); }
             */
            Log.d(TAG, "startRecording onFastReverseBoot: ");
            startRecording();
        }
        /*
         * if (getFloatCameraId() == CameraInfo.CAMERA_FACING_BACK) {
         * mFloatWindow.performClick(); }
         */
        Log.e(TAG, "---------onFastReverseBoot--------onRerverseMode" + mIsFastRevers);
        onRerverseMode(mIsFastRevers);
        if (mIsFastRevers) {
            wakeUpScreen();
        }
    }

    public void onShowRear(boolean isRear) {
        // TODO Auto-generated method stub
        mIsFastRevers = isRear;
        Log.v(TAG, "---------onShowRear--------onRerverseMode");
        onRerverseMode(mIsFastRevers);
        if (mIsFastRevers) {
            wakeUpScreen();
        }
    }

    public boolean readBootStatus() {
        if (mFastReverseChecker != null) {
            return mFastReverseChecker.readBootStatus();
        }
        return true;
    }

    public void setNeedExit(boolean isNeed) {
        if (mFastReverseChecker != null) {
            mFastReverseChecker.setNeedExit(isNeed);
        }
    }

    @Override
    public void onAccWake() {
        // TODO Auto-generated method stub
        // mWakeLock.acquire();
    }

    @Override
    public void onAccDown() {
        // TODO Auto-generated method stub
        // mWakeLock.release();
    }

    @Override
    public void onGsensorWake() {
        // TODO Auto-generated method stub
        // mWakeLock.acquire();
    }

    public interface IServiceListener {
        public void onTimeUpdate(long curTime);

        public void onHomePressed();

        public int onAskLeftCameraId();
    }

    public synchronized void addServiceListener(IServiceListener ls) {
        for (int i = 0; i < mServiceListenerList.size(); i++) {
            if (mServiceListenerList.get(i) == ls) {
                return;
            }
        }
        mServiceListenerList.add(ls);
    }

    public synchronized void removeServiceListener(IServiceListener ls) {
        int index = -1;
        for (int i = 0; i < mServiceListenerList.size(); i++) {
            if (mServiceListenerList.get(i) == ls) {
                index = i;
                break;
            }
        }
        if (index >= 0 && index < mServiceListenerList.size()) {
            mServiceListenerList.remove(index);
        }
    }

    private class SpreadActionReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            String action = intent.getAction();
            Log.i(TAG, "SpreadActionReceiver action = " + action);
            if (action.equals(RecordService.ACTION_LOCK_VIDEO)) {
                handleLockOneFile();
            } else if (action.equals(RecordService.ACTION_START_VIDEO)) {
                Log.d(TAG, "onReceive:ACTION_START_VIDEO ");
                isNeedFeedbackVideo = true;
                startRecording();
            } else if (action.equals(RecordService.ACTION_STOP_VIDEO)) {
                setSaveMediaDelay(true);// do not save files
                stopRecording();
            } else if (action.equals(RecordService.ACTION_SET_ALARM)) {
                // todo adjust to gsensor
                int level = intent.getIntExtra("level", 3);
                // mGsensorWakeUpMonitor.setSensity(level);
            } else if (action.equals(RecordService.ACTION_TAKE_SNAPSHOT)) {
                if (mIsMiniMode) {
                    Log.d(TAG, "ACTION_TAKE_SNAPSHOT mIsMiniMode =" + mIsMiniMode);
                    return;
                }
                boolean isVideo = intent.getBooleanExtra("video", false);
                int level = intent.getIntExtra("level", 1);
                int size = intent.getIntExtra("size", 3);
                int size_back = intent.getIntExtra("size_back", 3);
                if (size == 0 && !isVideo) {
                    Log.d(TAG, "ACTION_TAKE_SNAPSHOT");
                    if (Storage.getTotalSpace() < 0) {
                        // todo more gentlly hint
                        Log.d(TAG, "startRecording sd not mounted");
                        if (mToast != null) {
                            mToast.cancel();
                        }
                        mToast = Toast.makeText(RecordService.this, R.string.sdcard_not_found,
                                Toast.LENGTH_LONG);
                        mToast.show();
                        return;
                    } else if (Storage.getSdcardBlockSize() < 64 * 1024) {
                        Log.d(TAG,
                                "check sdcard failed, sdcard block size " + (Storage.getSdcardBlockSize() / 1024)
                                        + "k");
                        if (mToast != null) {
                            mToast.cancel();
                        }
                        mToast = Toast.makeText(RecordService.this,
                                R.string.format_sdcard_message, Toast.LENGTH_LONG);
                        mToast.show();
                        return;
                    } else if (RecordService.this.isMiniMode()) {
                        if (mToast != null) {
                            mToast.cancel();
                        }
                        mToast = Toast.makeText(RecordService.this, R.string.device_busy,
                                Toast.LENGTH_LONG);
                        mToast.show();
                        return;
                    } else if (Storage.getAvailableSpace() < 20 * 1024 * 1024) {
                        Log.d(TAG, "Storage.getAvailableSpace() =" + Storage.getAvailableSpace());
                        if (mToast != null) {
                            mToast.cancel();
                        }
                        mToast = Toast.makeText(RecordService.this, R.string.space_no_enough,
                                Toast.LENGTH_LONG);
                        mToast.show();
                        return;
                    }
                    isNeedPictureFeedback = true;
                    RecordService.this.takeSnapShot();
                    return;
                }
                int error = checkMiniError(size, size_back);
                mMiniError = error;
                Log.d(TAG, "ACTION_TAKE_SNAPSHOT error =" + error);
                Log.d(TAG, "level =" + level + ";size =" + size + ";size_back =" + size_back);
                Log.d(TAG, "isVideo =" + isVideo + ";mIsMiniMode=" + mIsMiniMode);
                if (error != ERR_OK && error != ERR_NO_REAR_CAMERA) {
                    Intent it = new Intent(RecordService.ACTION_SNAP_OVER);
                    it.putExtra("error_code", error);
                    RecordService.this.sendBroadcast(it);
                    mIsMiniMode = false;
                } else {
                    mIsMiniMode = true;
                    RecordService.this.setMiniListener();
                    mIsMiniFrontStoped = false;
                    mIsMintBackStoped = false;
                    mIsMiniFrontTaken = false;
                    mIsMiniBackTaken = false;
                    if (isRecording(CameraInfo.CAMERA_FACING_FRONT) || isRecording(CameraInfo.CAMERA_FACING_BACK)) {
                        mIsPreRecording = true;
                    }
                    if (isVideo) {
                        mIsMiniAdasOn = isAdasOn();
                        if (mIsMiniAdasOn) {
                            setIntelligentDetect(CameraInfo.CAMERA_FACING_FRONT, false);
                        }
                        mIsMiniVideo = true;
                        setMiniMode(WrapedRecorder.MINI_MODE_WAITING_STOP);
                        RecordService.this.setMiniDuaration(level);
                        RecordService.this.setMiniSize(size, size_back);
                        /*
                         * if (mIsPreRecording) {
                         * RecordService.this.stopRecording(); }
                         */
                        RecordService.this.stopRecording();
                    } else {
                        mIsMiniVideo = false;
                        setMiniMode(WrapedRecorder.MINI_MODE_WAITING_PHOTO);
                        RecordService.this.setMiniSize(size, size_back);
                        RecordService.this.takeSnapShot();
                    }
                }
            } else if (action.equals(RecordService.ACTION_REQUEST_STATUS)) {
                boolean isRec = false;
                boolean isAdasOn = false;
                Intent it = new Intent(RecordService.ACTION_REPLY_STATUS);
                if (isRecording(CameraInfo.CAMERA_FACING_BACK) || isRecording(CameraInfo.CAMERA_FACING_FRONT)
                        || isRecorderBusy()) {
                    isRec = true;
                }
                if (isAdasOn()) {
                    isAdasOn = true;
                }
                it.putExtra("isrecording", isRec);
                it.putExtra("isadason", isAdasOn);
                RecordService.this.sendBroadcast(it);
            } else if (action.equals(RecordService.ACTION_SHUTDOWN)) {
                stopRecording();
            } else if (action.equals(RecordService.ACTION_STOP_APP)) {
                RecordService.this.stopSelf();
            } else if (action.equals(RecordService.ACTION_SHOW_REAR)) {
                Log.d(TAG, "----------------------onShowRear--------------ACTION_SHOW_REAR");
                if (!mIsFastRevers) {
                    mIsHideLines = true;
                    RecordService.this.onShowRear(true);
                }
            } else if (action.equals(RecordService.ACTION_HOME_PRESS)) {
                mIsHideLines = false;
                mIsFastRevers = false;
                RecordService.this.onShowRear(false);
            } else if (action.equals(RecordService.ACTION_DREAM_START)) {
                stopRender();
            } else if (action.equals(RecordService.ACTION_DREAM_STOP)) {
                startRender();
            } else if (action.equals(ACTION_KILLSELF_ACTION)) {
                System.exit(0);
            } else if (ACTION_SHOW_STREAM_MEDIA_WINDOW.equals(action) || ACTION_SHOW_AFTER_SECONDS.equals(action)
                    || ACTION_RECORD_DISMISS.equals(action)) {
                mMainHandler.removeMessages(MSG_SHOW_STREAM_MEDIA_WINDOW);
                Bundle bundle = new Bundle();
                if (intent.getExtras() != null) {
                    bundle = intent.getExtras();
                    bundle.getBoolean("isClickBtn");
                    // mIsFromQcHome=intent.getBooleanExtra("isFromQcHome",
                    // true);
                    mIsQuickShow = bundle.getBoolean("isClickBtn");
                    Log.d(TAG, "SHOW--isClickBtn=  " + bundle.getBoolean("isClickBtn"));
                    mMainHandler.removeMessages(MSG_SHOW_STREAM_MEDIA_WINDOW);
                    if (bundle.getBoolean("isClickBtn")) {
                        Log.d(TAG, "SHOW--time=  " + 100);
                        mMainHandler.sendEmptyMessageDelayed(MSG_SHOW_STREAM_MEDIA_WINDOW, 100);
                    } else {
                        Log.d(TAG, "SHOW--time=  " + 10 * 500);
                        mMainHandler.sendEmptyMessageDelayed(MSG_SHOW_STREAM_MEDIA_WINDOW,
                                STREAM_MEDIA_WINDOW_DELAY_SHOW);
                    }
                    if (streamPreviewWindow.isShow()) {
                        Log.d(TAG, " ----------------open streammedia  and close " +
                                "preview---------------");
                        mMainHandler.sendEmptyMessage(MSG_HIDE_STREAM_PREVIEW_WINDOW);
                    }
                } else {
                    Log.d(TAG, "SHOW--time= else ====  " + 10 * 500);
                    mMainHandler.sendEmptyMessageDelayed(MSG_SHOW_STREAM_MEDIA_WINDOW,
                            STREAM_MEDIA_WINDOW_DELAY_SHOW);
                }

            } else if (ACTION_HIDE_STREAM_MEDIA_WINDOW.equals(action) || ACTION_VIDEO_LOCK.equals(action)
                    || ACTION_VOICE_WAKEUP.equals(action)) {// ||ACTION_VIDEO_LOCK.equals(action)
                Log.d(TAG, "MSG_HIDE_STREAM_MEDIA_WINDOW--------------------f*****k--------");
                if (intent.getExtras() != null) {
                    try {
                        isOnSm = intent.getExtras().getBoolean("isOnSM");
                    } catch (Exception e) {
                        isOnSm = false;
                    }
                }
                Log.d(TAG, "Whether open the app, the stream media is on : " + isOnSm);
                mMainHandler.sendEmptyMessage(MSG_HIDE_STREAM_MEDIA_WINDOW);
            } else if (ACTION_RECORD_SHOW.equals(action) || ACTION_CLOSE_STREAM_MEDIA_WINDOW.equals(action)) {
                mMainHandler.sendEmptyMessage(MSG_CLOSE_STREAM_MEDIA_WINDOW);
                if (streamPreviewWindow != null) {
                    if (streamPreviewWindow.isShow()) {
                        Log.d(TAG, " ----------------streamPreviewWindow1  ---------1------");
                        mMainHandler.sendEmptyMessage(MSG_CLOSE_STREAM_PREVIEW_WINDOW);
                    }
                }
            } else if (ACTION_TOUCH_SCREEN.equals(action)) {
                mMainHandler.removeMessages(MSG_SHOW_STREAM_MEDIA_WINDOW);
                if (streamMediaWindow != null) {
                    if (streamMediaWindow.isShow()) {
                        // mMainHandler.sendEmptyMessage(MSG_HIDE_STREAM_MEDIA_WINDOW);
                    } else {
                        Log.d(TAG, "ACTION_TOUCH_SCREEN  =======  ");
                        if (mIsQuickShow) {
                            mMainHandler.sendEmptyMessageDelayed(MSG_SHOW_STREAM_MEDIA_WINDOW, 100);
                        } else {
                            mMainHandler.sendEmptyMessageDelayed(MSG_SHOW_STREAM_MEDIA_WINDOW,
                                    STREAM_MEDIA_WINDOW_DELAY_SHOW);
                        }
                    }
                }
            } else if (ACTION_HIDE_STREAM_PREVIEW_WINDOW.equals(action)) {
                Log.d(TAG, " ----------------streamPreviewWindow  ----------hide-----");
                mMainHandler.sendEmptyMessage(MSG_HIDE_STREAM_PREVIEW_WINDOW);
            } else if (ACTION_SHOW_STREAM_PREVIEW_WINDOW.equals(action)) {
                Log.d(TAG, " ----------------streamPreviewWindow  ----------show-----");

                if (intent != null) {
                    int cameraID = intent.getIntExtra("CameraId", 1);
                    Log.d(TAG,
                            " ----------------cameraID  ----------show-----+cameraID=" + cameraID);
                    myClickPre.edit().putInt("CameraId", cameraID).commit();
                }
                mMainHandler.sendEmptyMessage(MSG_SHOW_STREAM_PREVIEW_WINDOW);
            } else if (VOICE_ACTION_OPEN_TAPE.equals(action)) {
                mIsMuteOn = pref.isMute();
                Log.i(TAG, "VOICE_ACTION_OPEN_TAP------mIsMuteOn==" + mIsMuteOn);
                // if(mIsMuteOn){
                Log.v(TAG, "VOICE_ACTION_CLOSE_TAPE------");
                setMute(false, CameraInfo.CAMERA_FACING_FRONT);
                setMute(false, CameraInfo.CAMERA_FACING_BACK);
                setMute(false, RecorderActivity.CAMERA_THIRD);
                Intent intentMute = new Intent();
                intentMute.setAction("com.action.other_Text");
                intentMute.putExtra("otherText", "录音已打开");
                RecordService.this.sendBroadcast(intentMute);
                pref.saveMute(false);
                // }
            } else if (VOICE_ACTION_CLOSE_TAPE.equals(action)) {
                mIsMuteOn = pref.isMute();
                // mIsMuteOn=!mIsMuteOn;
                Log.i(TAG, "VOICE_ACTION_CLOSE_TAPE------mIsMuteOn==" + mIsMuteOn);
                // if(mIsMuteOn){
                Log.v(TAG, "VOICE_ACTION_CLOSE_TAPE------");
                setMute(true, CameraInfo.CAMERA_FACING_FRONT);
                setMute(true, CameraInfo.CAMERA_FACING_BACK);
                setMute(true, RecorderActivity.CAMERA_THIRD);
                Intent intentMute = new Intent();
                intentMute.setAction("com.action.other_Text");
                intentMute.putExtra("otherText", "录音已关闭");
                RecordService.this.sendBroadcast(intentMute);
                pref.saveMute(true);
                // }
            } else if (ACTION_SHOW_LEFT_RIGHT_PREVIEW.equals(action)) {
                // showTwoFloat();
                Log.e(TAG, "showFullScreen！！！！！！！！！");

                if (isCameraOpened(TwoFloatWindow.RIGHT_CAMERA_ID)) {
                    showFullscreenCameraInner(7, false, false);
                } else {
                    Log.e(TAG, "showFullScreen22222222");

                    Toast.makeText(context, R.string.wait_right_camera_init, Toast.LENGTH_SHORT).show();
                }
            } else if (VOICE_ACTION_OPEN_FULL_CAMERA.equals(action)) {
                int CameraId = intent.getIntExtra("CameraId", 1);
                showFullscreenCamera(CameraId);
            } else if (VOICE_ACTION_CLOSE_FULL_CAMERA.equals(action)) {
                int CameraId = intent.getIntExtra("CameraId", 1);
                hideFullscreenCamera(CameraId);
            } else if (ACTION_VOICE_RETURN_LAUNCHER.equals(action)) {
                goToHome();
            } else if (action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
                Log.i(TAG, "Intent.ACTION_CLOSE_SYSTEM_DIALOGS");
                String reason = intent.getStringExtra("reason");
                if (TextUtils.equals(reason, "homekey")) {
                    Log.i(TAG, "press home key !");
                    Intent restoreIntent = new Intent();
                    restoreIntent.setAction(TwoCameraPreviewWin.HOME_TWO_WINDOW_ACTION);
                    sendBroadcast(restoreIntent);
                }
            }
        }

    }

    @Override
    public void onMiniPictureTaken(String fileName, int cameraId) {
        // TODO Auto-generated method stub
        if (cameraId == CameraInfo.CAMERA_FACING_BACK) {
            mMiniBackFile = fileName;
            mIsMiniBackTaken = true;
            Log.d(TAG, "onMiniPictureTaken mMiniBackFile =" + mMiniBackFile);
        }
        if (cameraId == CameraInfo.CAMERA_FACING_FRONT) {
            mMiniFrontFile = fileName;
            mIsMiniFrontTaken = true;
            Log.d(TAG, "onMiniPictureTaken mMiniFrontFile =" + mMiniFrontFile);
        }

        if (mIsMiniFrontTaken && (mIsMiniBackTaken || mMiniError == ERR_NO_REAR_CAMERA)) {
            setMiniMode(WrapedRecorder.MINI_MODE_NONE);
            Intent it = new Intent(RecordService.ACTION_SNAP_OVER);
            if (mIsMiniVideo || mMiniFrontFile == null || (mMiniBackFile == null && mMiniError != ERR_NO_REAR_CAMERA)) {
                Log.d(TAG, "onMiniPictureTaken unknown error");
                it.putExtra("error_code", ERR_FAIL);
                RecordService.this.sendBroadcast(it);
            } else {
                Log.d(TAG, "onMiniPictureTaken sucess");
                it.putExtra("video", mIsMiniVideo);
                it.putExtra("csi_file", mMiniFrontFile);
                it.putExtra("usb_file", mMiniBackFile);
                it.putExtra("usb_ver", "2");
                it.putExtra("error_code", ERR_OK);
                RecordService.this.sendBroadcast(it);
            }
            mIsMiniFrontTaken = false;
            mIsMiniBackTaken = false;
            mIsMiniMode = false;
            mIsMiniVideo = false;
            mMiniFrontFile = null;
            mMiniBackFile = null;
            mMiniError = -1;
            if (mIsMiniAdasOn) {
                setIntelligentDetect(CameraInfo.CAMERA_FACING_FRONT, true);
                mIsMiniAdasOn = false;
            }
        }
    }

    @Override
    public void onMiniVideoTaken(String fileName, int cameraId) {
        // TODO Auto-generated method stub
        if (cameraId == CameraInfo.CAMERA_FACING_BACK) {
            mMiniBackFile = fileName;
            mIsMiniBackTaken = true;
            Log.d(TAG, "onMiniVideoTaken mMiniBackFile =" + mMiniBackFile);
        }
        if (cameraId == CameraInfo.CAMERA_FACING_FRONT) {
            mMiniFrontFile = fileName;
            mIsMiniFrontTaken = true;
            Log.d(TAG, "onMiniVideoTaken mMiniFrontFile =" + mMiniFrontFile);
        }

        if (mIsMiniFrontTaken && mIsMiniBackTaken) {
            // RecordService.this.stopRecording();
            setMiniMode(WrapedRecorder.MINI_MODE_NONE);
            if (mIsPreRecording) {
                Log.d(TAG, "startRecording onMiniVideoTaken: ");
                RecordService.this.startRecording();
                mIsPreRecording = false;
            }
            Intent it = new Intent(RecordService.ACTION_SNAP_OVER);
            if (!mIsMiniVideo || mMiniFrontFile == null
                    || (mMiniBackFile == null && mMiniError != ERR_NO_REAR_CAMERA)) {
                Log.d(TAG, "onMiniVideoTaken unknown error");
                it.putExtra("error_code", ERR_FAIL);
                RecordService.this.sendBroadcast(it);
            } else if (!checkFileSize(mMiniFrontFile)
                    || (!checkFileSize(mMiniBackFile) && mMiniError != ERR_NO_REAR_CAMERA)) {
                it.putExtra("error_code", ERR_FILE_TOO_LARGE);
                RecordService.this.sendBroadcast(it);
            } else {
                Log.d(TAG, "onMiniVideoTaken sucess");
                it.putExtra("video", mIsMiniVideo);
                it.putExtra("csi_file", mMiniFrontFile);
                it.putExtra("usb_file", mMiniBackFile);
                it.putExtra("usb_ver", "2");
                it.putExtra("error_code", ERR_OK);
                RecordService.this.sendBroadcast(it);
            }
            mIsMiniFrontStoped = false;
            mIsMintBackStoped = false;
            mIsMiniFrontTaken = false;
            mIsMiniBackTaken = false;
            mIsMiniMode = false;
            mIsMiniVideo = false;
            mMiniFrontFile = null;
            mMiniBackFile = null;
            mMiniError = -1;
            if (mIsMiniAdasOn) {
                setIntelligentDetect(CameraInfo.CAMERA_FACING_FRONT, true);
                mIsMiniAdasOn = false;
            }
        }
    }

    @Override
    public void onStopForMini(int cameraId) {
        // TODO Auto-generated method stub
        if (cameraId == CameraInfo.CAMERA_FACING_BACK) {
            Log.d(TAG, "onStopForMini mIsMintBackStoped =" + mIsMintBackStoped);
            mIsMintBackStoped = true;
        }
        if (cameraId == CameraInfo.CAMERA_FACING_FRONT) {
            mIsMiniFrontStoped = true;
            Log.d(TAG, "onStopForMini mIsMiniFrontStoped =" + mIsMiniFrontStoped);
        }

        if (mIsMiniFrontStoped && (mIsMintBackStoped || mMiniError == ERR_NO_REAR_CAMERA)) {
            Log.d(TAG, "startRecording onStopForMini started mini record");
            setMiniMode(WrapedRecorder.MINI_MODE_WAITING_TAKEN);
            RecordService.this.startRecording();
            if (mMainHandler != null) {
                mMainHandler.sendEmptyMessageDelayed(MSG_WAITTING_MINI_OVER, mMiniduaration);
            }
        }
    }

    public boolean isMiniMode() {
        Log.d(TAG, "mIsMiniMode =" + mIsMiniMode);
        return mIsMiniMode;
    }

    public synchronized void playShutterClick() {
        if (mRecorderHandler != null) {
            mRecorderHandler.sendEmptyMessage(MSG_PLAY_SHUTTER_CLICK);
        }
    }

    public synchronized void onRerverseMode(boolean isReverse) {
        Log.d(TAG, "RecordService----------------------------------onRerverseMode =");

        if (RecorderActivity.CAMERA_COUNT == 2 && null != streamPreviewWindow) {
            if (streamPreviewWindow.isShow()) {
                streamPreviewWindow.hideWindow();
            }
            streamPreviewWindow.onRerverseMode(isReverse, mIsHideLines);
        }

        if (isReverse) {
            // showFullscreenCameraInner(FullScreenCameraActivity.STATE_FULLSCREEN_SHOW_CAMERA_BACK,
            // false, true);
            mMainHandler.removeMessages(MSG_CHECK_CAMERA_OPENED);
            mMainHandler.sendEmptyMessage(MSG_CHECK_CAMERA_OPENED);
        }
        /*
         * if (mFloatWindow != null) { mFloatWindow.onRerverseMode(isReverse,
         * mIsHideLines); }
         */
        if (isReverse) {
            sendBroadcast(new Intent("com.zqc.action.CLOSE_STREAM_MEDIA_WINDOW"));
            mIsHasRevered = true;
            Log.d(TAG, "onRerverseMode=   true" + "com.zqc.action.CLOSE_STREAM_MEDIA_WINDOW");
        } else {
            mMainHandler.removeMessages(MSG_CHECK_CAMERA_OPENED);
            if (mIsHasRevered) {
                Log.d(TAG, "onRerverseMode=   false" + "ACTION_STREAM_PREVIEW_WIDOW_HIDE");
                sendBroadcast(new Intent("com.action.stream_preview_window_hide"));
                mIsHasRevered = false;
            }
        }
        // else {
        // sendBroadcast(new Intent("com.zqc.action.SHOW_STREAM_MEDIA_WINDOW"));
        // Log.d(TAG, "onRerverseMode= false" +
        // "com.zqc.action.SHOW_STREAM_MEDIA_WINDOW");
        // }
    }

    public synchronized void setCameraPlug(boolean isOut) {
        mIsBackCamOut = isOut;
        if (null != streamPreviewWindow) {
            streamPreviewWindow.setCameraPlug(isOut);
        }
        if (mFloatWindow != null) {
            mFloatWindow.setCameraPlug(isOut);
        }
        for (int i = 0; i < mRecorderList.size(); i++) {
            if (CameraInfo.CAMERA_FACING_BACK == mRecorderList.get(i).getCameraId()) {
                mRecorderList.get(i).setCameraPlug(isOut);
                break;
            }
        }
    }

    public synchronized boolean isBackCameraOut() {
        return mIsBackCamOut;
    }

    public boolean isSDOut() {
        return mIsSDOut;
    }

    public synchronized void setLastIntent() {
        mLastIntent = null;
    }

    public Drawable getAppIconByPackageName(String packageName) {
        if (packageName == null) {
            return null;
        }
        List<PackageInfo> list = getPackageManager().getInstalledPackages(0);
        for (int i = 0; i < list.size(); i++) {
            PackageInfo packageInfo = list.get(i);
            if (packageName.equals(packageInfo.packageName)) {
                return packageInfo.applicationInfo.loadIcon(getPackageManager());
            }
        }
        return null;
    }

    public void wakeUpScreen() {
        boolean isScreenSaverOn = SystemProperties.getBoolean(SCREEN_SAVER_PROPERTIY, false);
        Log.d(TAG, "wakeUpScreen isScreenSaverOn=" + isScreenSaverOn);
        if (isScreenSaverOn) {
            Intent it = new Intent(ACTION_QUIT_SCREEN_SAVER);
            sendBroadcast(it);
        } else {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wl = pm
                    .newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "bright");
            wl.acquire();
            wl.release();
        }
    }

    public long getCurTime() {
        return mCurTime;
    }

    public void onHomePressed() {
        Log.d(TAG, "onHomePressed mIsChangingFloat=" + mIsChangingFloat);
        Log.d(TAG, "onHomePressed mIsHomePressing=" + mIsHomePressing);
        mLastHomePressed = System.currentTimeMillis();
        if (Math.abs(System.currentTimeMillis() - mLastStartActivity) < 300) {
            try {
                Thread.sleep(300);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
        if (mIsChangingFloat || mIsHomePressing) {
            return;
        }
        mIsHomePressing = true;
        if (mMainHandler != null) {
            mMainHandler.removeMessages(MSG_RECEIVED_HOME_DELAY);
            mMainHandler.sendEmptyMessage(MSG_RECEIVED_HOME_DELAY);
            try {
                Thread.sleep(300);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }

    private boolean readfileStatus(String file) {
        boolean isBoot = false;
        FileReader reader = null;
        try {
            reader = new FileReader(file);
            char[] buf = new char[15];
            int nn = reader.read(buf);
            if (nn > 0) {
                isBoot = (STATUS_TRUE == Integer.parseInt(new String(buf, 0, nn - 1)));
            }
        } catch (IOException ex) {
            // Log.e(TAG, "Couldn't read state from " + file + ": " + ex);
        } catch (NumberFormatException ex) {
            // Log.w(TAG, "Couldn't read state from " + file + ": " + ex);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
        return isBoot;
    }

    private void writefileStatus(String file, int flag) {
        FileWriter writer = null;
        try {
            writer = new FileWriter(file);
            writer.write(flag);
        } catch (IOException ex) {
            // Log.e(TAG, "Couldn't write state to " + file + ": " + ex);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
        return;
    }

    protected void onAccSleep() {
        Log.d(TAG, "onAccSleep");
        if (mPowerManager != null) {
            Log.d(TAG, "goToSleep");
            mPowerManager.goToSleep(SystemClock.uptimeMillis());
        }
        if (mCarSpeedMonitor != null) {
            mCarSpeedMonitor.stopSpeedDection();
        }
        mSleepStateKeeperList.clear();
        StateKeeper sk = null;
        for (int i = 0; i < mRecorderList.size(); i++) {
            WrapedRecorder wr = mRecorderList.get(i);
            sk = new StateKeeper();
            sk.mCameraId = wr.getCameraId();
            sk.mIsSDcardPlugout = false;
            sk.mIsPreviewing = isPreview(sk.mCameraId);
            sk.mIsRecording = isRecording(sk.mCameraId);
            sk.mIsRending = isRender(sk.mCameraId);
            sk.mIsWaterMarkRuning = isWaterMarkRuning(sk.mCameraId);
            if (sk.mCameraId == CameraInfo.CAMERA_FACING_FRONT) {
                sk.mIsAdasOn = isAdasOn();
            }
            Log.d(TAG, "stop cameraid=" + sk.mCameraId);
            if (sk.mIsRecording) {
                RecordService.this.release(sk.mCameraId);
            }
            if (sk.mIsAdasOn) {
                setIntelligentDetect(CameraInfo.CAMERA_FACING_FRONT, false);
                setAdasDetecttionCallback(CameraInfo.CAMERA_FACING_FRONT, null);
            }
            if (sk.mIsWaterMarkRuning) {
                RecordService.this.stopWaterMark(sk.mCameraId);
            }
            if (sk.mIsRending) {
                RecordService.this.stopRender(sk.mCameraId);
            }
            if (sk.mIsPreviewing) {
                RecordService.this.stopPreview(sk.mCameraId);
            }
            RecordService.this.closeCamera(sk.mCameraId);
            mSleepStateKeeperList.add(sk);
        }
        for (int i = 0; i < mRecorderList.size(); i++) {
            if (mRecorderList.get(i) != null) {
                mRecorderList.get(i).waitDone();
                if (mRecorderList.get(i).getCameraId() == CameraInfo.CAMERA_FACING_FRONT) {
                    mRecorderList.get(i).removeKmlWriter();
                }
            }
        }
        boolean recording = false;
        for (int i = 0; i < mRecorderList.size(); i++) {
            if (mRecorderList.get(i).isRecording()) {
                recording = true;
                break;
            }
        }
        if (mRecorderList.size() > 0 && recording) {
            if (!mStartSpaceCheck) {
                startSpaceCheck();
            }
            startSwitchFile();
        } else {
            if (mStartSpaceCheck) {
                stopSpaceCheck();
            }
            stopSwitchFile();
        }
        try {
            if (mWakeLock != null) {
                mWakeLock.release();
            }
        } catch (RuntimeException ex) {
            ex.printStackTrace();
        }
        if (mPowerManager != null) {
            Log.d(TAG, "goToSleep");
            mPowerManager.goToSleep(SystemClock.uptimeMillis());
        }
        mIsSleeping = true;
    }

    protected void onAccWakeUp() {
        Log.d(TAG, "onAccWakeUp");
        mIsSleeping = false;
        if (mCarSpeedMonitor != null) {
            mCarSpeedMonitor.startSpeedDection();
        }
        try {
            if (mWakeLock != null) {
                mWakeLock.acquire();
            }
        } catch (RuntimeException ex) {
            ex.printStackTrace();
        }
        for (int i = 0; i < mSleepStateKeeperList.size(); i++) {
            StateKeeper sk = mSleepStateKeeperList.get(i);
            if (!mGsensorWake) {
                openCamera(sk.mCameraId);
                if (sk.mIsPreviewing) {
                    startPreview(sk.mCameraId);
                }
            }
            if (sk.mIsRending) {
                startRender(sk.mCameraId);
            }
            if (sk.mIsRecording && i == mSleepStateKeeperList.size() - 1) {
                Log.d(TAG, "startRecording onAccWakeUp: ");
                startRecording();
            }
            if (sk.mIsWaterMarkRuning) {
                startWaterMark(sk.mCameraId);
            }
            if (sk.mCameraId == CameraInfo.CAMERA_FACING_FRONT && sk.mIsAdasOn) {
                RecordService.this.setIntelligentDetect(sk.mCameraId, true);
            }
        }
        mSleepStateKeeperList.clear();
    }

    protected void onGsensorWakeUp() {
        Log.d(TAG, "onGsensorWakeUp");
        try {
            if (mWakeLock != null) {
                mWakeLock.acquire();
            }
        } catch (RuntimeException ex) {
            ex.printStackTrace();
        }
        mGsensorWake = true;
        mIsMiniMode = true;
        for (int i = 0; i < mSleepStateKeeperList.size(); i++) {
            StateKeeper sk = mSleepStateKeeperList.get(i);
            openCamera(sk.mCameraId);
            if (sk.mIsPreviewing) {
                startPreview(sk.mCameraId);
            }
            if (sk.mIsRecording && i == mSleepStateKeeperList.size() - 1) {
                Log.d(TAG, "startRecording onGsensorWakeUp: ");
                startRecording();
            }
        }
        if (mMainHandler != null) {
            mMainHandler.sendEmptyMessageDelayed(MSG_GSENSOR_WAKE_OVER, GSENSOR_RECORD_DELAY);
        }
    }

    protected void onGsensorSleep() {
        Log.d(TAG, "onGsensorSleep");
        if (mPowerManager != null) {
            mPowerManager.goToSleep(SystemClock.uptimeMillis());
        }
        for (int i = 0; i < mSleepStateKeeperList.size(); i++) {
            StateKeeper sk = mSleepStateKeeperList.get(i);
            if (sk.mIsRecording) {
                release(sk.mCameraId);
            }
            if (sk.mIsPreviewing) {
                stopPreview(sk.mCameraId);
            }
            closeCamera(sk.mCameraId);
        }
        for (int i = 0; i < mRecorderList.size(); i++) {
            if (mRecorderList.get(i) != null) {
                mRecorderList.get(i).waitDone();
                if (mRecorderList.get(i).getCameraId() == CameraInfo.CAMERA_FACING_FRONT) {
                    mRecorderList.get(i).removeKmlWriter();
                }
            }
        }
        mIsMiniMode = false;
        try {
            if (mWakeLock != null) {
                mWakeLock.release();
            }
        } catch (RuntimeException ex) {
            ex.printStackTrace();
        }
        mGsensorWake = false;
    }

    private void shutdown() {
        Log.d(TAG, "shutdown");
        Intent shutdown = new Intent(Intent.ACTION_REQUEST_SHUTDOWN);
        shutdown.putExtra(Intent.EXTRA_KEY_CONFIRM, false);
        shutdown.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(shutdown);
    }

    private String getInputDevicePath() {
        File file = null;
        String name = null;
        for (int i = 0; i < MAX_DEVICES_NUM; i++) {
            file = new File(INPUT_DEVICE_PATH + i);
            if (file.isDirectory()) {
                file = new File(INPUT_DEVICE_PATH + i + GSENSOR_NAME_NODE);
                if (file.exists()) {
                    name = readfile(file.getPath());
                    if (name != null && name.equalsIgnoreCase(GSENSOR_DEVICE_NAME)) {
                        return INPUT_DEVICE_PATH + i;
                    }
                }
            }
            try {
                Thread.sleep(CHECK_GSENSOR_DELAY);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return null;
    }

    private String readfile(String file) {
        String result = null;
        FileReader reader = null;
        try {
            reader = new FileReader(file);
            char[] buf = new char[32];
            int nn = reader.read(buf);
            if (nn > 0) {
                result = new String(buf, 0, nn - 1);
                result.trim();
                return result;
            }
        } catch (IOException ex) {
            // Log.e(TAG, "Couldn't read string from " + file + ": " + ex);
        } catch (NumberFormatException ex) {
            // Log.w(TAG, "Couldn't read string from " + file + ": " + ex);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
        return result;
    }

    private BroadcastReceiver previewReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                Log.i(TAG, "previewReceiver intent is null !");
                return;
            }
            SharedPreferences mpreferences = getSharedPreferences("isVideo",
                    Context.MODE_MULTI_PROCESS | Context.MODE_WORLD_READABLE);
            if (myPreSp.getBoolean("isVisible", true)) {
                Log.i(TAG, "PreviewFragment is visible,we will not do nothing!");
            } else {
                String action = intent.getAction();
                Log.i(TAG, "action = " + action);
                if ("android.intent.action.CAMERA_RECORD".equals(action)) {

                } else if ("android.intent.action.CAMERA_SNAPSHOT".equals(action)) {
                    if (Storage.getTotalSpace() < 0) {
                        // todo more gentlly hint
                        Log.d(TAG, "startRecording sd not mounted");
                        Toast.makeText(RecordService.this, R.string.sdcard_not_found,
                                Toast.LENGTH_LONG).show();
                        Intent intent11 = new Intent();
                        intent11.setAction("com.action.other_Text");
                        intent11.putExtra("otherText", "TF卡不存在");
                        sendBroadcast(intent11);
                        return;
                    } else if (Storage.getSdcardBlockSize() < 64 * 1024) {
                        Log.d(TAG,
                                "check sdcard failed, sdcard block size " + (Storage.getSdcardBlockSize() / 1024)
                                        + "k");
                        // showFormatMsgDialog();
                        return;
                    } else if (Storage.getAvailableSpace() < 20 * 1024 * 1024) {
                        Log.d(TAG, "Storage.getAvailableSpace() =" + Storage.getAvailableSpace());
                        Toast.makeText(RecordService.this, R.string.space_no_enough,
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    takeSnapShot();
                } else if ("CLOSE_VIDEO".equals(action)) {
                    setLockOnce(false);
                    stopRecording();
                    Log.e("hy", "isvideo6666666");
                    mpreferences.edit().putBoolean("isVideo", false).commit();
                } else if ("LOCK_VIDEO".equals(action)) {
                    /*
                     * boolean mIsLocked = false; boolean mIsRecording= false;
                     * Log.i(TAG,"mIsLocked = "+mIsLocked+ " ,mIsRecording = "
                     * +mIsRecording); mIsLocked = !mIsLocked; if (!mIsRecording
                     * && mIsLocked) { Log.d(TAG, "onRecordStart once"); if
                     * (Storage.getTotalSpace() < 0) { // todo more gentlly hint
                     * Log.d(TAG, "startRecording sd not mounted");
                     * Toast.makeText(RecordService.this,
                     * R.string.sdcard_not_found, Toast.LENGTH_LONG).show();
                     * Intent intent6=new Intent();
                     * intent6.setAction("com.action.other_Text");
                     * intent6.putExtra("otherText", "TF卡不存在");
                     * sendBroadcast(intent6); return; } else if
                     * (Storage.getSdcardBlockSize() < 64 * 1024) { Log.d(TAG,
                     * "check sdcard failed, sdcard block size " +
                     * (Storage.getSdcardBlockSize() / 1024) + "k"); return; }
                     * else if (isMiniMode()) {
                     * Toast.makeText(RecordService.this, R.string.device_busy,
                     * Toast.LENGTH_LONG) .show(); return; } if (true) {
                     * setLockOnce(false); setLockFlag(mIsLocked); Intent
                     * intent3=new Intent();
                     * intent3.setAction("CLOSE_VIDEO_APP");
                     * sendBroadcast(intent3);
                     * mpreferences.edit().putBoolean("isVideo", true).commit();
                     * } else { mIsLocked = false; Intent intent3=new Intent();
                     * intent3.setAction("CLOSE_VIDEO_APP");
                     * sendBroadcast(intent3);
                     * mpreferences.edit().putBoolean("isVideo", true).commit();
                     * } } else if (mIsLocked) { setLockFlag(mIsLocked); }
                     * myPreSp.edit().putBoolean("mIsLocked",
                     * mIsLocked).commit();
                     * myPreSp.edit().putBoolean("mIsRecording",
                     * mIsRecording).commit(); Intent intent2 =
                     * RecordService.this.getPackageManager()
                     * .getLaunchIntentForPackage("com.android.camera2");
                     * intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                     * RecordService.this.startActivity(intent2);
                     */
                    myPreSp.edit().putBoolean("isGsonLock", true).commit();
                    Intent it = new Intent(RecordService.this, RecorderActivity.class);
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    it.addFlags(0x00000200);
                    RecordService.this.startActivity(it);
                } else if ("TXZ_START_RECORD".equals(action)) {
                    if (!mpreferences.getBoolean("isVideo", true)) {


                        if (isRecord()) {
                            Log.d(TAG, "startRecording onReceive:TXZ_START_RECORD ");
                            startRecording();
                            mpreferences.edit().putBoolean("isVideo", true).commit();
                        }

                    }
                } else if ("MUTE_WILLBE_OPEN".equals(action)) {
                    boolean mIsMuteOn = false;
                    setMute(mIsMuteOn, CameraInfo.CAMERA_FACING_FRONT);
                    setMute(mIsMuteOn, CameraInfo.CAMERA_FACING_BACK);
                    setMute(mIsMuteOn, RecorderActivity.CAMERA_THIRD);
                    if (!mIsMuteOn) {
                        // Intent intentMute=new Intent();
                        // intentMute.setAction("com.action.other_Text");
                        // intentMute.putExtra("otherText", "录音已打开");
                        // sendBroadcast(intentMute);
                        Log.v(TAG, "muteon");
                    } else {
                        // Intent intentMute=new Intent();
                        // intentMute.setAction("com.action.other_Text");
                        // intentMute.putExtra("otherText", "录音已关闭");
                        // sendBroadcast(intentMute);
                        Log.v(TAG, "muteooff");
                    }
                    myPreSp.edit().putBoolean("mIsMuteOn", mIsMuteOn).commit();
                    pref.saveMute(mIsMuteOn);
                } else if ("MUTE_WILLBE_CLOSE".equals(action)) {
                    boolean mIsMuteOn = true;
                    setMute(mIsMuteOn, CameraInfo.CAMERA_FACING_FRONT);
                    setMute(mIsMuteOn, CameraInfo.CAMERA_FACING_BACK);
                    setMute(mIsMuteOn, RecorderActivity.CAMERA_THIRD);
                    if (!mIsMuteOn) {
                        // Intent intentMute=new Intent();
                        // intentMute.setAction("com.action.other_Text");
                        // intentMute.putExtra("otherText", "录音已打开");
                        // sendBroadcast(intentMute);
                        Log.v(TAG, "muteon");
                    } else {
                        // Intent intentMute=new Intent();
                        // intentMute.setAction("com.action.other_Text");
                        // intentMute.putExtra("otherText", "录音已关闭");
                        // sendBroadcast(intentMute);
                        Log.v(TAG, "muteooff");
                    }
                    myPreSp.edit().putBoolean("mIsMuteOn", mIsMuteOn).commit();
                    pref.saveMute(mIsMuteOn);
                }
            }

            if (ACTION_SHOW_FLOAT_WINDOW.equals(intent.getAction())) {
                if (RecorderActivity.CAMERA_COUNT == 2) {
                    Log.d(TAG, "zdt --- ACTION_SHOW_FLOAT_WINDOW");
                    showFloatWindows();
                }
            } else if (ACTION_TXZ_SEND.equals(intent.getAction())) {
                Log.d(TAG, "zdt --- ACTION_TXZ_SEND");
                Bundle bundle = intent.getExtras();
                if (bundle != null) {
                    int type = bundle.getInt("key_type");
                    String actionStirng = bundle.getString("action");
                    Log.i(TAG, "TXZ_ACTIONtype: " + type + ", actionStirng: " + actionStirng);

                    if (type == 1073) {
                        if (txzLookAhead.equals(actionStirng)) { //看前面
                            if (streamPreviewWindow != null && !mIsHasRevered) {
                                if (CustomValue.CHANGE_FRONT_BACK_CAMERA) {
                                    streamPreviewWindow.showWindow(StreamPreViewWindow.ACTION_PRE_BACK);
                                } else {
                                    streamPreviewWindow.showWindow(StreamPreViewWindow.ACTION_PRE_FRONT);
                                }
                            }
                        } else if (txzLookBehind.equals(actionStirng)) { //看后面
                            if (RecorderActivity.CAMERA_COUNT == 2 && streamPreviewWindow != null && !mIsHasRevered) {
                                if (CustomValue.CHANGE_FRONT_BACK_CAMERA) {
                                    streamPreviewWindow.showWindow(StreamPreViewWindow.ACTION_PRE_FRONT);
                                } else {
                                    streamPreviewWindow.showWindow(StreamPreViewWindow.ACTION_PRE_BACK);
                                }
                            }
                        } else if (txzLookAheadClose.equals(actionStirng)) { //关闭前录
                            if (streamPreviewWindow != null && !mIsHasRevered && streamPreviewWindow.isShow()) {
                                if (CustomValue.CHANGE_FRONT_BACK_CAMERA) {
                                    if (streamPreviewWindow.getCurShowCameraID() == CameraInfo.CAMERA_FACING_BACK) {
                                        streamPreviewWindow.hideWindow();
                                    }
                                }else{
                                    if (streamPreviewWindow.getCurShowCameraID() == CameraInfo.CAMERA_FACING_FRONT) {
                                        streamPreviewWindow.hideWindow();
                                    }
                                }
                            }
                        } else if (txzLookBehindClose.equals(actionStirng)) { //关闭后录
                            if (streamPreviewWindow != null && !mIsHasRevered && streamPreviewWindow.isShow()) {
                                if (CustomValue.CHANGE_FRONT_BACK_CAMERA) {
                                    if (streamPreviewWindow.getCurShowCameraID() == CameraInfo.CAMERA_FACING_FRONT) {
                                        streamPreviewWindow.hideWindow();
                                    }
                                }else{
                                    if (streamPreviewWindow.getCurShowCameraID() == CameraInfo.CAMERA_FACING_BACK) {
                                        streamPreviewWindow.hideWindow();
                                    }
                                }

                            }
                        }
                    } else if (type == 1071) {
                        if (txzRecordOpen.equals(actionStirng)) { //打开录音
                            boolean isMuteOn = false;
                            setMute(isMuteOn, CameraInfo.CAMERA_FACING_FRONT);
                            setMute(isMuteOn, CameraInfo.CAMERA_FACING_BACK);
                            myPreSp.edit().putBoolean("mIsMuteOn", isMuteOn).commit();
                            pref.saveMute(isMuteOn);
                        } else if (txzRecordClose.equals(actionStirng)) { //关闭录音
                            boolean isMuteOn = true;
                            setMute(isMuteOn, CameraInfo.CAMERA_FACING_FRONT);
                            setMute(isMuteOn, CameraInfo.CAMERA_FACING_BACK);
                            myPreSp.edit().putBoolean("mIsMuteOn", isMuteOn).commit();
                            pref.saveMute(isMuteOn);
                        }
                    }
                }
            }
        }

        ;
    };


    public boolean isRecord() {
        if (Storage.getTotalSpace() < 0) {
            // todo more gentlly hint
            Log.d(TAG, "startRecording sd not mounted");

            return false;
        } else if (Storage.getSdcardBlockSize() < 64 * 1024) {
            Log.d(TAG,
                    "check sdcard failed, sdcard block size " + (Storage.getSdcardBlockSize() / 1024)
                            + "k");
            showFormatMsgDialog();
            return false;
        } else if (isMiniMode()) {
            Toast.makeText(getApplicationContext(), R.string.device_busy, Toast.LENGTH_LONG).show();
            return false;
        } else {
            return true;
        }

    }


    public void showFormatMsgDialog() {
        CDRAlertDialog dialog = CDRAlertDialog.getInstance(getApplicationContext());
        if (null == dialog) {
            return;
        }
        dialog.setTitle(R.string.format_sdcard_message);
        dialog.setMessage(R.string.sd1_format_or_not);
        dialog.setCallback(new ICDRAlertDialogListener() {

            @Override
            public void onClick(int state) {
                Storage.formatSDcard(getApplicationContext());
            }

            @Override
            public void onTimeClick(int hour, int minute) {
            }

            @Override
            public void onDateClick(int year, int month, int day) {
            }
        });
        dialog.setButtons();
    }


    public String getTopActivityPackName() {
        String packName = "";
        try {
            ActivityManager am = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
            List<RunningTaskInfo> tasks = am.getRunningTasks(1);
            if (!tasks.isEmpty()) {
                ComponentName topActivity = tasks.get(0).topActivity;
                packName = topActivity.getPackageName();
            }
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
        return packName;
    }

    public void launchApp(String packageName) {

        try {
            Intent launchIntent = this.getPackageManager().getLaunchIntentForPackage(packageName);
            startActivity(launchIntent);
        } catch (Exception e) {
            Log.e(TAG, "can not launch the package = " + packageName);
        }

    }

    public interface RightInsertImpl {
        void isRightInsert(boolean isInsert);
    }

    public interface BackInsertImpl {
        void isBackInsert(boolean isInsert);
    }

    private RightInsertImpl mRightInsertImpl;
    private BackInsertImpl mBackInsertImpl;

    public void setRightInsertImpl(RightInsertImpl mRightInsertImpl) {
        this.mRightInsertImpl = mRightInsertImpl;
    }

    public void setBackInsertImpl(BackInsertImpl mBackInsertImpl) {
        this.mBackInsertImpl = mBackInsertImpl;
    }

    public void setCameraFlip(int cameraId, boolean isFlip) {
        Log.i(TAG, "setCameraFlip() cameraId = " + cameraId + ",isFlip = " + isFlip);
        for (int i = 0; i < mRecorderList.size(); i++) {
            if (cameraId == mRecorderList.get(i).getCameraId()) {
                mRecorderList.get(i).setCameraFlip(isFlip);
            }
        }
    }

//	public void removeFloatWindow() {
//		if (mFloatWindow != null) {
//			mFloatWindow.onDestroy();
//		}
//
//	}

    public void updateFloatWindow(int position) {
        Log.d(TAG, "zdt --- updateFloatWindow, " + position);
        if (RecorderActivity.CAMERA_COUNT == 2 && mFloatWindow != null) {
            mFloatWindow.updateWindowPosition(position);
        }
    }
}
