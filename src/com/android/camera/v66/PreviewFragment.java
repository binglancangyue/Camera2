package com.android.camera.v66;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.ActivityManager;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.Service;
import android.app.StatusBarManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.Adas;
import android.hardware.Camera.AdasDetectionListener; // adas
import android.hardware.Camera.CameraInfo;
import android.media.AudioManager;
import android.media.MediaActionSound;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewStub;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.os.SystemProperties;

import java.text.DecimalFormat;

import com.android.camera.AnimationManager;
import com.android.camera.CameraManager;
import com.android.camera.Storage;
import com.android.camera.v66.AdjustedView.IOnScroll;
import com.android.camera.v66.AdjustedView.IWindowsChanged;
import com.android.camera.v66.CarSpeedMonitor.ISpeedChangeListener;
import com.android.camera.v66.MyPreference.IAdasFlagChanged;
import com.android.camera.v66.MyPreference.ICarLaneAdjustChanged;
import com.android.camera.v66.MyPreference.ICarTypeChanged;
import com.android.camera.v66.MyPreference.ICrashSensityChanged;
import com.android.camera.v66.MyPreference.ILockSensityChanged;
import com.android.camera.v66.MyPreference.IPicQualityChanged;
import com.android.camera.v66.MyPreference.IRearFlipChanged;
import com.android.camera.v66.MyPreference.IRecDurationChanged;
import com.android.camera.v66.MyPreference.IRecQualityChanged;
import com.android.camera.v66.MyPreference.IRightFlipChanged;
import com.android.camera.v66.RecorderActivity.IServiceBindedListener;
import com.android.camera.v66.RecordService.IServiceListener;
import com.android.camera.v66.WrapedRecorder.IRecordCallback;
import com.android.camera2.R;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

//add by chengyuzhou//
import android.content.SharedPreferences;
import android.content.ContentResolver;

import java.util.Calendar;

//end           //
public class PreviewFragment extends Fragment implements View.OnClickListener, IServiceBindedListener, IRecordCallback,
        ISpeedChangeListener, Camera.AdasDetectionListener, IWindowsChanged, IServiceListener, Callback {

    public static final String TAG = "PreviewFragment";
    private static final boolean DEBUG = false;
    public static final int MSG_SECOND_TICKY = 0;
    public static final int MSG_RECORD_TICKY = 1;
    public static final int MSG_ADAS_CALLBACK = 2;
    public static final int MSG_CHANGE_PREVIEW = 3;
    public static final int MSG_CHANGE_FLOATWINDOW_RIGHT = 4;
    public static final int MSG_START_RECORD = 5;
    public static final int ACTIVITY_ON_LEFT = 1;
    public static final int ACTIVITY_ON_RIGHT = 0;
    public static final long TICKY_DELAY = 500;
    public static final long ONCLICK_DELAY = 500;
    public static final String NAVIGATION_CAMERA_CLOSE_NEED_CHANGED = "android.intent.action.NAVIGATION_CAMERA_CLOSE_NEED_CHANGED";
    public static final String ACTION_HOME_PRESS = "android.intent.action.HOME_PRESS";
    public static final String INTENT_FAST_REVERSE_BOOTUP = "intent.softwinner.carlet.FAST_REVERSE_BOOTUP";
    public static final String ACTION_SPLIT_WINDOW_HAS_CHANGED = "android.intent.action.SPLIT_WINDOW_HAS_CHANGED";
    public static final int MW_INVALID_STACK_WINDOW = -1;
    public static final int MW_NORMAL_STACK_WINDOW = 0x0;
    public static final int MW_MAX_STACK_WINDOW = 0x1;
    public static final int MW_MIN_STACK_WINDOW = 0x2;
    private static boolean mIsDrawFrame = true;
    public boolean mIsLocked = false;
    public boolean mIsRecording = false;
    public boolean mIsAdasOn = false;
    public boolean mIsMuteOn = false;

    private FrameLayout mStatusBar;
    private FrameLayout mViews;
    private TextView mSpeed;
    private ImageView mRecordIcon;
    private TextView mRecordTime;
    private ImageView mRecordLock;
    private TextView mDate;
    private TextView mTime;
    private ImageView iv_mark;
    private TextView mHintText;
    private LinearLayout layoutOperation;
    private ImageButton mButtonLock;
    private ImageButton mButtonSnapshot;
    private ImageButton mButtonRecord;
    private ImageButton mButtonAdas;
    private ImageButton mButtonMute;
    private ImageView mButtonDivider;
    private ImageButton mButtonSettings;
    private ImageButton mButtonReview;
    private ImageView mReverseLines;

    private FrameLayout mAdasLayout;
    private LinearLayout layoutSpeedBar;
    private RelativeLayout layoutOperationRight;
    private ImageButton mButtonLockRight;
    private ImageButton mButtonSnapshotRight;
    private ImageButton mButtonRecordRight;
    private ImageButton mButtonMuteRight;

    private View mCurrentView = null;
    private int mCameraId;
    private RecordService mService = null;
    private AdjustedSurfaceView mPreview;
    private ImageView mPreviewAnimation;
    private boolean mHolderIsUp = false;
    private boolean mIsPreviewing = false;
    private SurfaceHolder mHolder;
    private long mStartRecordTime = 0;
    private MyPreference mPref;
    private long mDuaration = 0;
    private int mFlag;

    private Roadway mAdasView;
    private AdjustedGlSurfaceView mGlSurfaceView;
    private RoadwayRenderer mRoadwayRenderer;
    private ActivityManager mActivityManager;
    private boolean mIsPowerOn = false;
    private RoadwaySoundPlayer mRoadSoundPlayer;
    private long mAdasLast = 0;
    private boolean mIsOnRight = true;
    private AnimationManager mAnimationManager;
    private CDRAlertDialog mDialog;
    private TextView tv_water_mark;

    // add by chengyuzhou //
    private SharedPreferences mpreferences;
    private SharedPreferences.Editor myEditor;
    private SharedPreferences myPreSp;
    public static Boolean call = true;
    DecimalFormat df = new DecimalFormat("#0.000000");
    public static final boolean SUPPORT_PREVIEW_WATERMARK = SystemProperties.getBoolean("persist.sys.support_watermark",
            false);
    private long[] mHits = new long[2];
    private boolean isMove = false;
    private boolean isCameraBackIn = false;

    // end //
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SECOND_TICKY:
                    SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
//				SimpleDateFormat dateFormatter = new SimpleDateFormat("d MMM yyy");

                    long currentTimeMillis = System.currentTimeMillis();
                    // add by chengyuzhou
                    String date = dateFormatter.format(currentTimeMillis);
                    ContentResolver cv = getActivity().getApplicationContext().getContentResolver();
                    String strTimeFormat = android.provider.Settings.System.getString(cv,
                            android.provider.Settings.System.TIME_12_24);
                    SimpleDateFormat timeFormatter;
                    Calendar mCalendar = Calendar.getInstance();
                    mCalendar.setTimeInMillis(currentTimeMillis);
                    int apm = mCalendar.get(Calendar.AM_PM);
                    if (strTimeFormat.equals("12")) {
                        timeFormatter = new SimpleDateFormat("hh:mm:ss");
                        String time = timeFormatter.format(currentTimeMillis);
                        if (mTime != null && apm == 0) {
                            mTime.setText("AM" + time);
                        } else if (mTime != null && apm == 1) {
                            mTime.setText("PM" + time);
                        }
                    } else {
                        timeFormatter = new SimpleDateFormat("HH:mm:ss");
                        String time = timeFormatter.format(currentTimeMillis);
                        if (mTime != null) {
                            mTime.setText(time);
                        }
                    }
                    // end
                    if (mDate != null) {
                        mDate.setText(date);
                    }

                    mHandler.removeMessages(MSG_SECOND_TICKY);
                    mHandler.sendEmptyMessageDelayed(MSG_SECOND_TICKY, TICKY_DELAY);
                    break;
                case MSG_RECORD_TICKY:
                    // not use now
                    if (msg.arg1 == 9) {
                        mStartRecordTime = System.currentTimeMillis();
                        mHandler.removeMessages(MSG_RECORD_TICKY);
                        mHandler.sendEmptyMessageDelayed(MSG_RECORD_TICKY, TICKY_DELAY);
                    } else if (mIsRecording) {
                        long cur = System.currentTimeMillis();
                        SimpleDateFormat recordFormatter = new SimpleDateFormat("mm:ss");
                        String recordTime = recordFormatter.format(cur - mStartRecordTime);
                        if (mRecordTime != null) {
                            mRecordTime.setText(recordTime);
                        }
                        /*
                         * Log.d(TAG, "mStartRecordTime= " + mStartRecordTime);
                         * Log.d(TAG, "mDuaration= " + mDuaration); Log.d(TAG,
                         * "mCameraId= " + mCameraId); Log.d(TAG,
                         * "MSG_RECORD_TICKY cur - mStartRecordTime= " + (cur -
                         * mStartRecordTime));
                         */
                        if (mDuaration > 3 * 60 * 1000 || mDuaration < 0) {
                            mDuaration = 1 * 60 * 1000;
                        }
                        if (Math.abs(cur - mStartRecordTime) > mDuaration && mService != null) {
                            Log.d(TAG, "cur=" + cur + ";mStartRecordTime =" + mStartRecordTime);
                            Log.d(TAG, "mDuaration=" + mDuaration + ";mCameraId =" + mCameraId);
                            mService.switchToNextFile(mCameraId);
                        } else {
                            mHandler.removeMessages(MSG_RECORD_TICKY);
                            mHandler.sendEmptyMessageDelayed(MSG_RECORD_TICKY, TICKY_DELAY);
                        }
                    }
                    break;
                case MSG_ADAS_CALLBACK:
                    // Log.d(TAG, "MSG_ADAS_CALLBACK coming");
                    if (msg.obj instanceof Adas) {
                        // Log.d(TAG, "MSG_ADAS_CALLBACK");
                        // if(Math.abs(System.currentTimeMillis() - mAdasLast) >
                        // 200) {
                        if (mAdasView != null) {
                            mAdasView.setAdas((Adas) msg.obj);
                        }
                        if (mRoadwayRenderer != null) {
                            mRoadwayRenderer.setAdas((Adas) msg.obj);
                        }
                        if (mGlSurfaceView != null) {
                            mGlSurfaceView.requestRender();
                        }
                        // mAdasLast = System.currentTimeMillis();
                        // }
                    }
                    break;
                case MSG_CHANGE_PREVIEW:
                    Log.d(TAG, "MSG_CHANGE_PREVIEW");
                    changePreview();
                    break;


                case MSG_CHANGE_FLOATWINDOW_RIGHT:
                    if (RecorderActivity.CAMERA_COUNT == 2 && mService != null) {
                        Log.d(TAG, "zdt --- MSG_CHANGE_FLOATWINDOW_RIGHT: mService != null");
                        if (!SplitUtil.isFullWindow(getActivity())) {
                            mService.updateFloatWindow(DoubleFloatWindow.ON_RIGHT);
                            mService.showFloatWindows();
                        }
                    }
                    break;
                case MSG_START_RECORD:
                    mButtonRecord.performClick();
                    break;
                default:
                    break;
            }
            super.handleMessage(msg);
        }
    };
    private BroadcastReceiver mWindowReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                return;
            }
            if (StreamMediaWindow.ACTION_STREAM_MEDIA_WIDOW_HIDE.equals(intent.getAction())) {
                Log.i(TAG, "--------StreamMediaWindow-----------preFragment= ");
                if (null != mService && null != mHolder) {
                    Log.i(TAG, "--------mCameraId= " + mCameraId);
                    if (mHolderIsUp) {
                        mService.setPreviewDisplay(mCameraId, mHolder);
                    }
                }
            } else if (StreamPreViewWindow.ACTION_STREAM_PREVIEW_WIDOW_HIDE.equals(intent.getAction())) {
                Log.i(TAG, "--------StreamPreViewWindow-----------preFragment= ");
                if (null != mService && null != mHolder) {
                    Log.i(TAG, "--------mCameraId= " + mCameraId);
                    if (mHolderIsUp) {
                        mService.setPreviewDisplay(mCameraId, mHolder);
                    }
                }
            }
        }
    };
    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "mIntentReceiver action=" + action);
            SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm:ss");
            long currentTimeMillis = System.currentTimeMillis();
            String date = dateFormatter.format(currentTimeMillis);
            String time = timeFormatter.format(currentTimeMillis);
            if (mDate != null) {
                mDate.setText(date);
            }
            if (mTime != null) {
                mTime.setText(time);
            }
        }
    };
    private BroadcastReceiver mRebootReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                return;
            }

            if (intent.getAction().equals("android.intent.action.MEDIA_MOUNTED")) {
                Log.v(TAG, "MySDcardIn");
                if (mService != null) {
                    if (Storage.getTotalSpace() < 0) {
                        // todo more gentlly hint
                        Log.d(TAG, "startRecording sd not mounted");
                        Toast.makeText(getActivity(), R.string.sdcard_not_found, Toast.LENGTH_LONG).show();
                        Intent intent9 = new Intent();
                        intent9.setAction("com.action.other_Text");
                        intent9.putExtra("otherText", "TF卡不存在");
                        mService.sendBroadcast(intent9);
                        return;
                    } else if (Storage.getSdcardBlockSize() < 64 * 1024) {
                        Log.d(TAG, "check sdcard failed, sdcard block size " + (Storage.getSdcardBlockSize() / 1024)
                                + "k");
                        showFormatMsgDialog();
                        return;
                    } else if (mService.isMiniMode()) {
                        Toast.makeText(getActivity(), R.string.device_busy, Toast.LENGTH_LONG).show();
                        return;
                    }
                    mService.startRecording();
                    Intent intent3 = new Intent();
                    intent3.setAction("CLOSE_VIDEO_APP");
                    mService.sendBroadcast(intent3);
                    myEditor.putBoolean("isVideo", true).commit();
                    // MediaPlayer mediaPlayer=MediaPlayer.create(mService,
                    // R.raw.start);
                    // mediaPlayer.start();
                }
            } else if (intent.getAction().equals("android.intent.action.MEDIA_UNMOUNTED")) {
                if (Storage.getTotalSpace() < 0) {
                    // Intent intent4=new Intent();
                    // intent4.setAction("com.action.other_Text");
                    // intent4.putExtra("otherText", "SD卡已卸载");
                    // context.sendBroadcast(intent4);
                    // Intent intent8 = context.getPackageManager()
                    // .getLaunchIntentForPackage("com.zqc.launcher");
                    // context.startActivity(intent8);
                }
            }
        }
    };

    private BroadcastReceiver mAtionBarReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            if (intent == null) {
                return;
            }
            if (DEBUG)
                Log.d(TAG, "mAtionBarReceiver action=" + intent.getAction());
            if (intent.getAction().equals(NAVIGATION_CAMERA_CLOSE_NEED_CHANGED)) {
                if (mActivityManager != null) {
                    int stackId = SplitUtil.getStackBoxId(getActivity());
                    Log.d(TAG, "stackId =" + stackId);
                    if (stackId > 0 && SplitUtil.getStackPostition(getActivity(), stackId) == ACTIVITY_ON_RIGHT) {
                        // at right do nothing
                        Log.d(TAG, "at right screen");
                    } else {
                        // at left or full screen
                        Log.d(TAG, "at left or full screen");
                        if (mService != null) {
                            boolean isExit = intent.getBooleanExtra(RecordService.IS_EXIT, false);
                            int floatId = intent.getIntExtra(RecordService.EXTRA_CAM_TYPE, -1);
                            Log.d(TAG, "floatid =" + floatId);
                            Log.d(TAG, "mCameraId =" + mCameraId + ";isExit=" + isExit);
                            if (!isExit) {
                                int state = RecorderActivity.STATE_DEFAULT;
                                if (floatId == CameraInfo.CAMERA_FACING_BACK) {
                                    state = RecorderActivity.STATE_BACK_PREVIEW;
                                } else if (floatId == CameraInfo.CAMERA_FACING_FRONT) {
                                    state = RecorderActivity.STATE_FRONT_PREVIEW;
                                } else if (floatId == RecorderActivity.CAMERA_THIRD) {
                                    state = RecorderActivity.STATE_THIRD_PREVIEW;
                                }
                                ((RecorderActivity) getActivity()).switchCameraByState(state);
                            } else {
                                getActivity().finish();
                            }
                        }
                    }
                }
            } else if (intent.getAction().equals(ACTION_HOME_PRESS)) {
                /*
                 * if (mActivityManager != null) { int stackId =
                 * SplitUtil.getStackBoxId(getActivity()); Log.d(TAG,
                 * "stackId =" + stackId); if (stackId > 0 &&
                 * SplitUtil.getStackPostition(getActivity(), stackId) ==
                 * ACTIVITY_ON_LEFT) { // at left if (mService != null) { if
                 * (mCameraId == CameraInfo.CAMERA_FACING_BACK) { Log.d(TAG,
                 * "backcam at left"); Log.d(TAG, "going to float myself");
                 * mService.setFloatCameraid(CameraInfo.CAMERA_FACING_BACK);
                 * mService.startFloat(); getActivity().finish();
                 * mService.setStartCameraid(-1);
                 * mService.setIsChangingFloat(false, 10);
                 * mService.setLastIntent(); } else { Log.d(TAG,
                 * "frontcam at left"); if
                 * (!mService.isCameraAdd(CameraInfo.CAMERA_FACING_BACK)) {
                 * mService.addCamera(CameraInfo.CAMERA_FACING_BACK); }
                 * mService.setFloatCameraid(CameraInfo.CAMERA_FACING_BACK);
                 * mService.startFloat(); mService.setStartCameraid(-1);
                 * mService.setIsChangingFloat(false, 10); } } } else { // at
                 * right or full screen if (mService != null) { Log.d(TAG,
                 * "backcam not running"); if
                 * (!mService.isCameraAdd(CameraInfo.CAMERA_FACING_BACK)) {
                 * mService.addCamera(CameraInfo.CAMERA_FACING_BACK);
                 * mService.setFloatCameraid(CameraInfo.CAMERA_FACING_BACK);
                 * mService.startFloat(); mService.setLastIntent(); } } } }
                 */
            } else if (intent.getAction().equals(INTENT_FAST_REVERSE_BOOTUP)) {
                if (mCameraId == CameraInfo.CAMERA_FACING_BACK) {
                    boolean isReverse = intent.getBooleanExtra("isReverseing", true);
                    if (mService == null) {
                        return;
                    }
                    Log.e(TAG, "onRerverseMode isReverse=" + isReverse);
                    Log.e(TAG, "onRerverseMode mService.getFastReverFlag()=" + mService.getFastReverFlag());
                    if (mService.getFastReverFlag() && isReverse) {
                        // mReverseLines.setVisibility(View.VISIBLE);
                        // mService.setFastReverFlag(false);
                        getActivity().sendBroadcast(new Intent("com.zqc.action.CLOSE_STREAM_MEDIA_WINDOW"));
                        Log.d(TAG, "mAtionBarReceiver sendBroadcast=" + "com.zqc.action.CLOSE_STREAM_MEDIA_WINDOW");
                    } else {
                        // mReverseLines.setVisibility(View.GONE);
                        // mService.setFastReverFlag(false);
                        getActivity().sendBroadcast(new Intent("com.zqc.action.SHOW_STREAM_MEDIA_WINDOW"));
                        Log.d(TAG, "mAtionBarReceiver sendBroadcast=" + "com.zqc.action.SHOW_STREAM_MEDIA_WINDOW");
                        if (mHolderIsUp) {
                            mService.setPreviewDisplay(mCameraId, mHolder);
                        }
                    }
                }
            } else if (intent.getAction().equals(ACTION_SPLIT_WINDOW_HAS_CHANGED)) {
                Log.d(TAG, "windows size change refresh UI");
                int stackId = SplitUtil.getStackBoxId(getActivity());
                int windowSizeStatus = SplitUtil.getWindowSizeStatus(getActivity(), stackId);
                int activityPo = SplitUtil.getStackPostition(getActivity(), stackId);
                Log.d(TAG, "stackId =" + stackId + ", status: " + windowSizeStatus + " ,activityPo: " + activityPo);
                if (mCameraId == CameraInfo.CAMERA_FACING_FRONT) {
                    if (windowSizeStatus == MW_MIN_STACK_WINDOW) {
                        Log.d(TAG, "windows size change, hide carlane");
                        if (mGlSurfaceView != null) {
                            mGlSurfaceView.setVisibility(View.GONE);
                        }
                    } else {
                        Log.d(TAG, "windows size change, show carlane");
                        if (mGlSurfaceView != null) {
                            if (CustomValue.FULL_WINDOW) {
                                mGlSurfaceView.setVisibility(View.GONE);
                            } else {
                                mGlSurfaceView.setVisibility(View.VISIBLE);
                            }
                        }
                    }
                }

                if (RecorderActivity.CAMERA_COUNT == 2 && mService != null) {
                    if (activityPo == ACTIVITY_ON_RIGHT) {
                        if (SplitUtil.isFullWindow(mService)) {
                            mService.hideFloatWindows();
                        }
                    }
                    if (CustomValue.FULL_WINDOW) {
                        mService.hideFloatWindows();
                    }
                }

                // mCurrentView.postInvalidate();
                // mCurrentView.requestLayout();
                // mCurrentView.invalidate();
            } else if (intent.getAction().equals(RecordService.ACTION_START_ADAS)) {
                if (mCameraId == CameraInfo.CAMERA_FACING_FRONT) {
                    if (!mIsAdasOn) {
                        if (MyPreference.isAdasOpen) {
                            mButtonAdas.performClick();
                        }
                    }
                }
            } else if (intent.getAction().equals(RecordService.ACTION_STOP_ADAS)) {
                if (mCameraId == CameraInfo.CAMERA_FACING_FRONT) {
                    if (mIsAdasOn) {
                        if (MyPreference.isAdasOpen) {
                            mButtonAdas.performClick();
                        }
                    }
                }
            } else if (intent.getAction().equals("android.intent.action.CAMERA_RECORD")) {
                if (mService != null) {
                    mService.setLockOnce(false);
                    if (!mIsRecording) {
                        Log.d(TAG, "onRecordStart");
                        if (Storage.getTotalSpace() < 0) {
                            // todo more gentlly hint
                            Log.d(TAG, "startRecording sd not mounted");
                            Toast.makeText(getActivity(), R.string.sdcard_not_found, Toast.LENGTH_LONG).show();
                            Intent intent10 = new Intent();
                            intent10.setAction("com.action.other_Text");
                            intent10.putExtra("otherText", "TF卡不存在");
                            mService.sendBroadcast(intent10);
                            return;
                        } else if (Storage.getSdcardBlockSize() < 64 * 1024) {
                            Log.d(TAG, "check sdcard failed, sdcard block size " + (Storage.getSdcardBlockSize() / 1024)
                                    + "k");
                            showFormatMsgDialog();
                            return;
                        } else if (mService.isMiniMode()) {
                            Toast.makeText(getActivity(), R.string.device_busy, Toast.LENGTH_LONG).show();
                            return;
                        }
                        mService.startRecording();
                        String locale = Locale.getDefault().toString();
                        if (locale.equals("zh_CN") || locale.equals("zh_TW")) {
                            MediaPlayer mediaPlayer = MediaPlayer.create(mService, R.raw.start);
                            mediaPlayer.start();
                        }
                        myEditor.putBoolean("isVideo", true).commit();
                    } else {
                        Log.d(TAG, "onRecordStop");
                        if (mService.isMiniMode()) {
                            Toast.makeText(getActivity(), R.string.device_busy, Toast.LENGTH_LONG).show();
                            return;
                        }
                        mService.stopRecording();
                        String locale = Locale.getDefault().toString();
                        if (locale.equals("zh_CN") || locale.equals("zh_TW")) {
                            MediaPlayer mediaPlayer = MediaPlayer.create(mService, R.raw.stop);
                            mediaPlayer.start();
                        }
                        myEditor.putBoolean("isVideo", false).commit();
                    }
                    // mIsRecording = !mIsRecording;
                }

            } else if (intent.getAction().equals("android.intent.action.CAMERA_SNAPSHOT")) {
                if (Storage.getTotalSpace() < 0) {
                    // todo more gentlly hint
                    Log.d(TAG, "startRecording sd not mounted");
                    Toast.makeText(getActivity(), R.string.sdcard_not_found, Toast.LENGTH_LONG).show();
                    Intent intent11 = new Intent();
                    intent11.setAction("com.action.other_Text");
                    intent11.putExtra("otherText", "TF卡不存在");
                    mService.sendBroadcast(intent11);
                    return;
                } else if (Storage.getSdcardBlockSize() < 64 * 1024) {
                    Log.d(TAG, "check sdcard failed, sdcard block size " + (Storage.getSdcardBlockSize() / 1024) + "k");
                    showFormatMsgDialog();
                    return;
                } else if (Storage.getAvailableSpace() < 20 * 1024 * 1024) {
                    Log.d(TAG, "Storage.getAvailableSpace() =" + Storage.getAvailableSpace());
                    Toast.makeText(getActivity(), R.string.space_no_enough, Toast.LENGTH_LONG).show();
                    return;
                }
                if (mService != null) {
                    mService.takeSnapShot();
                }

            } else if (intent.getAction().equals("android.intent.action.CAMERA_SNAPSHOT")) {
                if (Storage.getTotalSpace() < 0) {
                    // todo more gentlly hint
                    Log.d(TAG, "startRecording sd not mounted");
                    Toast.makeText(getActivity(), R.string.sdcard_not_found, Toast.LENGTH_LONG).show();
                    Intent intent12 = new Intent();
                    intent12.setAction("com.action.other_Text");
                    intent12.putExtra("otherText", "TF卡不存在");
                    mService.sendBroadcast(intent12);
                    return;
                } else if (Storage.getSdcardBlockSize() < 64 * 1024) {
                    Log.d(TAG, "check sdcard failed, sdcard block size " + (Storage.getSdcardBlockSize() / 1024) + "k");
                    showFormatMsgDialog();
                    return;
                } else if (Storage.getAvailableSpace() < 20 * 1024 * 1024) {
                    Log.d(TAG, "Storage.getAvailableSpace() =" + Storage.getAvailableSpace());
                    Toast.makeText(getActivity(), R.string.space_no_enough, Toast.LENGTH_LONG).show();
                    return;
                }
                if (mService != null) {
                    mService.takeSnapShot();
                }
            } else if (intent.getAction().equals("CLOSE_VIDEO")) {
                if (mService != null) {
                    mService.setLockOnce(false);
                    mService.stopRecording();
                    myPreSp.edit().putBoolean("isGsonLock", false).commit();
                    myPreSp.edit().putBoolean("mIsLocked", mIsLocked).commit();

                    Log.e("hy", "isvideo1111111");
                    myEditor.putBoolean("isVideo", false).commit();
                }
            } else if (intent.getAction().equals("TXZ_START_RECORD")) {
                Log.d(TAG, "dakaijiyuyi");
                if (!mpreferences.getBoolean("isVideo", true)) {


                    if (Storage.getTotalSpace() < 0) {
                        // todo more gentlly hint
                        Log.d(TAG, "startRecording sd not mounted");
                        Toast.makeText(getActivity(), R.string.sdcard_not_found, Toast.LENGTH_LONG).show();
                        Intent intent8 = new Intent();
                        intent8.setAction("com.action.other_Text");
                        intent8.putExtra("otherText", "TF卡不存在");
                        mService.sendBroadcast(intent8);
                        return;
                    } else if (Storage.getSdcardBlockSize() < 64 * 1024) {
                        Log.d(TAG, "check sdcard failed, sdcard block size " + (Storage.getSdcardBlockSize() / 1024)
                                + "k");
                        showFormatMsgDialog();
                        return;
                    } else if (mService.isMiniMode()) {
                        Toast.makeText(getActivity(), R.string.device_busy, Toast.LENGTH_LONG).show();
                        return;
                    }


                    mService.startRecording();
                    myEditor.putBoolean("isVideo", true).commit();
                }
            } else if (intent.getAction().equals("MUTE_WILLBE_CLOSE")) {
                if (!mIsMuteOn) {
                    mIsMuteOn = !mIsMuteOn;
                    mService.setMute(mIsMuteOn, CameraInfo.CAMERA_FACING_FRONT);
                    mService.setMute(mIsMuteOn, CameraInfo.CAMERA_FACING_BACK);
                }

            } else if (intent.getAction().equals("MUTE_WILLBE_OPEN")) {
                if (mIsMuteOn) {
                    mIsMuteOn = !mIsMuteOn;
                    mService.setMute(mIsMuteOn, CameraInfo.CAMERA_FACING_FRONT);
                    mService.setMute(mIsMuteOn, CameraInfo.CAMERA_FACING_BACK);
                }

            } else if (intent.getAction().equals("LOCK_VIDEO")) {
                // if (mService != null) {
                // mService.setLockOnce(true);
                // mService.setLockOnce(true);
                // mService.setLockFlag(mIsLocked);
                // mService.startRecording();
                // myEditor.putBoolean("isVideo", true).commit();

                if (mService != null) {
                    mIsLocked = !mIsLocked;
                    if (!mIsRecording && mIsLocked) {
                        Log.d(TAG, "onRecordStart once");
                        if (Storage.getTotalSpace() < 0) {
                            // todo more gentlly hint
                            Log.d(TAG, "startRecording sd not mounted");
                            Toast.makeText(getActivity(), R.string.sdcard_not_found, Toast.LENGTH_LONG).show();
                            Intent intent13 = new Intent();
                            intent13.setAction("com.action.other_Text");
                            intent13.putExtra("otherText", "TF卡不存在");
                            mService.sendBroadcast(intent13);
                            return;
                        } else if (Storage.getSdcardBlockSize() < 64 * 1024) {
                            Log.d(TAG, "check sdcard failed, sdcard block size " + (Storage.getSdcardBlockSize() / 1024)
                                    + "k");
                            showFormatMsgDialog();
                            return;
                        } else if (mService.isMiniMode()) {
                            Toast.makeText(getActivity(), R.string.device_busy, Toast.LENGTH_LONG).show();
                            return;
                        }
                        if (true) {
                            mService.setLockOnce(false);
                            mService.setLockFlag(true);

                        } else {
                            mIsLocked = false;
                            Intent intent3 = new Intent();
                            intent3.setAction("CLOSE_VIDEO_APP");
                            mService.sendBroadcast(intent3);
                            myEditor.putBoolean("isVideo", true).commit();
                        }
                    } else if (mIsLocked) {
                        mService.setLockFlag(mIsLocked);
                    }
                    // }
                }
            } else if (RecorderActivity.ACTION_HIDE_LAYOUT.equals(intent.getAction())) {
                hideLayout();
            } else if (RecorderActivity.ACTION_SHOW_LAYOUT.equals(intent.getAction())) {
                showLayout();
            } else if (TwoCameraPreviewWin.ACTION_SWITCH_FRAGMENT.equals(intent.getAction())) {
                int floatId = intent.getIntExtra(RecordService.EXTRA_CAM_TYPE, -1);
                ((RecorderActivity) getActivity()).switchCameraByState(floatId);
            }
        }

    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate()");
        // setHasOptionsMenu(true);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        filter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
        mpreferences = getActivity().getSharedPreferences("isVideo",
                Context.MODE_MULTI_PROCESS | Context.MODE_WORLD_READABLE);
        myPreSp = getActivity().getSharedPreferences("PriSP", Context.MODE_PRIVATE);

        myEditor = mpreferences.edit();

        /*
         * if(getActivity() != null){
         * getActivity().registerReceiver(mIntentReceiver, filter); }
         */

        if (mHandler != null) {
            mHandler.sendEmptyMessage(MSG_SECOND_TICKY);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView(), CAMERA_COUNT: " + RecorderActivity.CAMERA_COUNT);
        View view = null;
        if (RecordService.SPLITSCREEN_SEVEN) {
            view = inflater.inflate(R.layout.spliteqc7_preview, container, false);
        } else {
            if (RecorderActivity.CAMERA_COUNT == 1) {
                view = inflater.inflate(R.layout.preview_front, container, false);
            } else {
                view = inflater.inflate(R.layout.preview, container, false);
            }
        }
        mStatusBar = (FrameLayout) view.findViewById(R.id.status_bar);
        mStatusBar.getBackground().setAlpha(120);
        mSpeed = (TextView) view.findViewById(R.id.speed_value);
        mRecordIcon = (ImageView) view.findViewById(R.id.record_icon);
        mRecordTime = (TextView) view.findViewById(R.id.record_time);
        mRecordLock = (ImageView) view.findViewById(R.id.record_lock);
        mDate = (TextView) view.findViewById(R.id.date);
        mTime = (TextView) view.findViewById(R.id.time);
        iv_mark = (ImageView) view.findViewById(R.id.iv_mark);
        mHintText = (TextView) view.findViewById(R.id.hint_text);
        layoutOperation = (LinearLayout) view.findViewById(R.id.controll_bar);
        if (CustomValue.CAMERA_NOT_RECORD) {
            layoutOperation.setVisibility(View.GONE);
        }
        mButtonLock = (ImageButton) view.findViewById(R.id.button_lock);
        mButtonSnapshot = (ImageButton) view.findViewById(R.id.button_snapshot);
        mButtonRecord = (ImageButton) view.findViewById(R.id.button_record);
        mButtonAdas = (ImageButton) view.findViewById(R.id.button_adas);
        if (CustomValue.ONLY_ONE_CAMERA){
            mButtonAdas.setVisibility(View.GONE);
        }
        mButtonDivider = (ImageView) view.findViewById(R.id.button_divider);
        mButtonMute = (ImageButton) view.findViewById(R.id.button_mute);
        mButtonSettings = (ImageButton) view.findViewById(R.id.button_settings);
        mButtonReview = (ImageButton) view.findViewById(R.id.button_review);


        layoutSpeedBar = (LinearLayout) view.findViewById(R.id.speed_bar);
        layoutOperationRight = (RelativeLayout) view.findViewById(R.id.controll_bar_right);
        mButtonLockRight = (ImageButton) view.findViewById(R.id.button_lock_right);
        mButtonSnapshotRight = (ImageButton) view.findViewById(R.id.button_snapshot_right);
        mButtonRecordRight = (ImageButton) view.findViewById(R.id.button_record_right);
        mButtonMuteRight = (ImageButton) view.findViewById(R.id.button_mute_right);
        mAdasLayout = (FrameLayout) view.findViewById(R.id.adas_container);


        mPreview = (AdjustedSurfaceView) view.findViewById(R.id.preview_content);
        mHolder = mPreview.getHolder();
        mHolder.addCallback(this);
        mHolder.setFormat(PixelFormat.OPAQUE);
        mPreviewAnimation = (ImageView) view.findViewById(R.id.preview_animation);
        mReverseLines = (ImageView) view.findViewById(R.id.reverse_lines);
        mViews = (FrameLayout) view.findViewById(R.id.views_container);
        tv_water_mark = (TextView) view.findViewById(R.id.tv_water_mark);
        mButtonLock.setOnClickListener(this);
        mButtonSnapshot.setOnClickListener(this);
        mButtonRecord.setOnClickListener(this);
        mButtonMute.setOnClickListener(this);
        mButtonSettings.setOnClickListener(this);
        mButtonReview.setOnClickListener(this);
        mButtonSnapshot.setSoundEffectsEnabled(false);

        mButtonLockRight.setOnClickListener(this);
        mButtonSnapshotRight.setOnClickListener(this);
        mButtonRecordRight.setOnClickListener(this);
        mButtonMuteRight.setOnClickListener(this);
        mButtonSnapshotRight.setSoundEffectsEnabled(false);

        // add by Bryan
        mViews.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // TODO Auto-generated method stub
                switch (event.getAction()) {
                    case MotionEvent.ACTION_UP:
                        if (isMove == false) {
                            if (DEBUG)
                                Log.i(TAG, "mPreview--onTouch--ACTION_UP----");
                        }
                        isMove = false;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (DEBUG)
                            Log.i(TAG, "onTouch()--ACTION_MOVE---");
                        isMove = true;
                        break;
                    case MotionEvent.ACTION_DOWN:
                        if (DEBUG)
                            Log.i(TAG, "mPreview--onTouch--ACTION_DOWN----");
                        if (DEBUG)
                            Log.d(TAG, "-----------------mPreview is onTouch-----------------");
                        // mHandler.removeMessages(MSG_CHANGE_PREVIEW);

                        System.arraycopy(mHits, 1, mHits, 0, mHits.length - 1);
                        mHits[mHits.length - 1] = SystemClock.uptimeMillis(); // 系统开机时间
                        // mHandler.sendEmptyMessageDelayed(MSG_CHANGE_PREVIEW,
                        // 600);
                        if (mHits[0] >= (SystemClock.uptimeMillis() - 500)) {
                            if (mHintText.getVisibility() == View.VISIBLE) {
                                Toast.makeText(getActivity(), R.string.please_insert_back_cam, Toast.LENGTH_SHORT).show();
                                break;
                            }
                            Log.d(TAG, "-----------------mPreview is DoubleTouch-----------------");
                            Log.d(TAG, "-----------------mPreview is DoubleTouch----------------mCameraId==-" + mCameraId);
                            // mHandler.removeMessages(MSG_CHANGE_PREVIEW);
                            doubleClick();
                        }
                        break;

                    default:
                        break;
                }
                return false;
            }
        });
        mCurrentView = view;
        if (MyPreference.isAdasOpen) {
            mButtonAdas.setOnClickListener(this);
        } else {
            mButtonAdas.setVisibility(View.GONE);
        }
        IntentFilter intent = new IntentFilter(NAVIGATION_CAMERA_CLOSE_NEED_CHANGED);
        // intent.addAction(ACTION_HOME_PRESS);
        intent.addAction(INTENT_FAST_REVERSE_BOOTUP);
        intent.addAction(ACTION_SPLIT_WINDOW_HAS_CHANGED);
        intent.addAction(RecordService.ACTION_START_ADAS);
        intent.addAction(RecordService.ACTION_STOP_ADAS);
        intent.addAction("android.intent.action.CAMERA_RECORD");
        intent.addAction("android.intent.action.CAMERA_SNAPSHOT");
        intent.addAction("com.dvr.physical.btn");
        intent.addAction("CLOSE_VIDEO");
        intent.addAction("LOCK_VIDEO");
        intent.addAction("TXZ_START_RECORD");
        intent.addAction("MUTE_WILLBE_OPEN");
        intent.addAction("MUTE_WILLBE_CLOSE");
        intent.addAction(RecorderActivity.ACTION_HIDE_LAYOUT);
        intent.addAction(RecorderActivity.ACTION_SHOW_LAYOUT);
        intent.addAction(TwoCameraPreviewWin.ACTION_SWITCH_FRAGMENT);
        // intent.addAction(StreamMediaWindow.ACTION_STREAM_MEDIA_WIDOW_SHOW);
        // intent.addAction(StreamMediaWindow.ACTION_STREAM_MEDIA_WIDOW_HIDE);
        // intent.addAction(StreamPreViewWindow.ACTION_STREAM_PREVIEW_WIDOW_HIDE);
        // intent.addAction("android.intent.action.MEDIA_MOUNTED");
        // intent.addDataScheme("file");
        getActivity().registerReceiver(mAtionBarReceiver, intent);
        // add by chengyuzhou //
        IntentFilter filter2 = new IntentFilter();
        filter2.addAction("android.intent.action.MEDIA_MOUNTED");
        filter2.addAction("android.intent.action.MEDIA_UNMOUNTED");
        filter2.addDataScheme("file");
        getActivity().registerReceiver(mRebootReceiver, filter2);
        // end //

        IntentFilter filterWindow = new IntentFilter();
        filterWindow.addAction(StreamMediaWindow.ACTION_STREAM_MEDIA_WIDOW_SHOW);
        filterWindow.addAction(StreamMediaWindow.ACTION_STREAM_MEDIA_WIDOW_HIDE);
        filterWindow.addAction(StreamPreViewWindow.ACTION_STREAM_PREVIEW_WIDOW_HIDE);
        getActivity().registerReceiver(mWindowReceiver, filterWindow);

        mRoadSoundPlayer = new RoadwaySoundPlayer(getActivity());
        mAnimationManager = new AnimationManager();

        mActivityManager = (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);
        mPref = MyPreference.getInstance(getActivity());
        ViewStub adasViewStub = (ViewStub) view.findViewById(R.id.adas_view_stub);
        if (adasViewStub != null) {
            adasViewStub.inflate();
            mAdasView = (Roadway) view.findViewById(R.id.adas_view);
            LayoutParams lp = mAdasView.getLayoutParams();
            lp.width = LayoutParams.MATCH_PARENT;
            lp.height = LayoutParams.MATCH_PARENT;
            mAdasView.setLayoutParams(lp);
            mAdasView.setWindowsChanged(this);
            mAdasView.setLongClickable(true);
            mAdasView.setVisibility(View.VISIBLE);
            mAdasView.setHide(false);
            mCurrentView.requestLayout();
            mCurrentView.invalidate();
        }
        mAdasView.setOnScrollListener(new IOnScroll() {

            @Override
            public void onScroll(float deta) {
                // TODO Auto-generated method stub
                if (mGlSurfaceView != null) {
                    mGlSurfaceView.onScroll(deta);
                }
                if (mPreview != null) {
                    mPreview.onScroll(deta);
                }
            }

            @Override
            public void onResume(int tranY) {
                // TODO Auto-generated method stub
                if (mGlSurfaceView != null) {
                    mGlSurfaceView.setTranslationY(tranY);
                }
                if (mPreview != null) {
                    if (DEBUG)
                        Log.d(TAG, "mPreview setTranslationY=" + tranY);
                    mPreview.setTranslationY(tranY);
                    mPreview.requestLayout();
                    mPreview.invalidate();
                }
            }
        });

        if (getActivity() instanceof RecorderActivity) {
            if (mFlag == 0) {
                mFlag = ((RecorderActivity) getActivity()).getFlag();
            }
            if (((mFlag & RecorderActivity.PREVIEW_START_BACK) > 0)
                    || (mFlag == 0 && mCameraId == CameraInfo.CAMERA_FACING_BACK)) {
                mButtonDivider.setVisibility(View.GONE);
                // xiugai
                mButtonSettings.setVisibility(View.GONE);
                mButtonReview.setVisibility(View.GONE);
                mButtonAdas.setVisibility(View.GONE);
                mButtonMute.setVisibility(View.GONE);
                mButtonRecord.setBackgroundResource(R.drawable.bg_button_right);
                mCameraId = CameraInfo.CAMERA_FACING_BACK;
            }
            if (((mFlag & RecorderActivity.PREVIEW_START_FRONT) > 0)
                    || (mFlag == 0 && mCameraId == CameraInfo.CAMERA_FACING_FRONT)) {
                mCameraId = CameraInfo.CAMERA_FACING_FRONT;
                int stackId = SplitUtil.getStackBoxId(getActivity());
                Log.d(TAG, "stackId =" + stackId);
                if (stackId > 0 && SplitUtil.getStackPostition(getActivity(), stackId) == ACTIVITY_ON_RIGHT) {
                    // at right hide settins button
                    Log.d(TAG, "at right screen，hide settings");

                    // xiugai
                    mButtonSettings.setVisibility(View.GONE);
                    if (MyPreference.getChipType() == MyPreference.IC_V66) {
                        mButtonReview.setVisibility(View.GONE);
                        mButtonDivider.setVisibility(View.GONE);
                        mButtonMute.setBackgroundResource(R.drawable.bg_button_right);
                        mButtonMuteRight.setBackgroundResource(R.drawable.bg_button_right);
                    } else {
                        // mButtonMute.setVisibility(View.VISIBLE);
                        // mButtonDivider.setVisibility(View.GONE);
                        mButtonReview.setVisibility(View.GONE);
                        mButtonDivider.setVisibility(View.GONE);
                        mButtonMute.setBackgroundResource(R.drawable.bg_button_right);
                        mButtonMuteRight.setBackgroundResource(R.drawable.bg_button_right);
                        if (mDate != null) {
                            mDate.setVisibility(View.GONE);
                        }
                    }

                    onToRight();
                } else {
                    mButtonSettings.setVisibility(View.VISIBLE);
                    if (MyPreference.getChipType() == MyPreference.IC_V66) {
                        mButtonReview.setVisibility(View.GONE);
                        mButtonDivider.setVisibility(View.GONE);
                        mButtonMute.setBackgroundResource(R.drawable.bg_button_center);
                        mButtonMuteRight.setBackgroundResource(R.drawable.bg_button_center);
                        mButtonSettings.setBackgroundResource(R.drawable.bg_button_right);
                    } else {
                        // mButtonDivider.setVisibility(View.GONE);
                        // mButtonMute.setVisibility(View.GONE);
                        mButtonReview.setVisibility(View.GONE);
                        mButtonDivider.setVisibility(View.GONE);
                        mButtonMute.setBackgroundResource(R.drawable.bg_button_center);
                        mButtonMuteRight.setBackgroundResource(R.drawable.bg_button_center);
                        mButtonSettings.setBackgroundResource(R.drawable.bg_button_right);
                        if (mDate != null) {
                            mDate.setVisibility(View.VISIBLE);
                        }
                    }
                }

                /* adas start */
                FrameLayout layout = (FrameLayout) view.findViewById(R.id.glsurfaceLayout);
                mGlSurfaceView = new AdjustedGlSurfaceView(getActivity().getBaseContext());
                mRoadwayRenderer = new RoadwayRenderer();
                mGlSurfaceView.setBackgroundDrawable(null);
                mGlSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
                mGlSurfaceView.setRenderer(mRoadwayRenderer);
                mGlSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
                mGlSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
                mGlSurfaceView.requestRender();
                layout.addView(mGlSurfaceView, FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT);
                mGlSurfaceView.setZOrderOnTop(true);
                /* adas end */
                mPreview.setCameraId(CameraInfo.CAMERA_FACING_FRONT);
                mAdasView.setCameraId(CameraInfo.CAMERA_FACING_FRONT);
                mGlSurfaceView.setCameraId(CameraInfo.CAMERA_FACING_FRONT);
            }
            if (((mFlag & RecorderActivity.PREVIEW_START_THIRD) > 0)
                    || (mFlag == 0 && mCameraId == RecorderActivity.CAMERA_THIRD)) {
                mButtonDivider.setVisibility(View.GONE);
                mButtonSettings.setVisibility(View.GONE);
                mButtonReview.setVisibility(View.GONE);
                mButtonAdas.setVisibility(View.GONE);
                mButtonMute.setVisibility(View.GONE);
                mButtonRecord.setBackgroundResource(R.drawable.bg_button_right);
                mCameraId = RecorderActivity.CAMERA_THIRD;
            }
            if ((mFlag & RecorderActivity.PREVIEW_START_RECORDING) > 0) {
                mIsPowerOn = true;
                Log.d(TAG, "mIsPowerOn=" + mIsPowerOn);
            }

            Log.d(TAG, "mService=" + mService);
            initSettings();
            ((RecorderActivity) getActivity()).addServiceBindedListener(this);
            mService = ((RecorderActivity) getActivity()).getRecordService();
            if (mService != null) {
                Log.d(TAG, "onCreateView----------------------------initRecorder()");
                initRecorder();
            }
        }
        myPreSp.edit().putBoolean("isVisible", true).commit();
        // myPreSp.edit().putBoolean("mIsLocked", mIsLocked);
        // mService.setMute(true, CameraInfo.CAMERA_FACING_FRONT);
        tv_water_mark.setText("E: 0.000000  N: 0.000000");
        Log.d(TAG, "onCreateView");
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initMarkImageView();
    }

    private void hideLayout() {
        if (null != layoutOperation && layoutOperation.getVisibility() != View.GONE) {
            layoutOperation.setVisibility(View.GONE);
        }

    }

    private void showLayout() {
        if (null != layoutOperation && layoutOperation.getVisibility() != View.VISIBLE) {
            if (CustomValue.CAMERA_NOT_RECORD) {
                layoutOperation.setVisibility(View.GONE);
            } else {
                layoutOperation.setVisibility(View.VISIBLE);
            }
        }
    }

    private void initMarkImageView() {
        if (SUPPORT_PREVIEW_WATERMARK) {
            iv_mark.setVisibility(View.VISIBLE);
            int stackId = SplitUtil.getStackBoxId(getActivity());
            if (stackId > 0 && SplitUtil.getStackPostition(getActivity(), stackId) == ACTIVITY_ON_LEFT) {// left
                iv_mark.setImageResource(R.drawable.iv_mark_blwd);
            } else {// right
                iv_mark.setImageResource(R.drawable.iv_mark_blwd_small);
            }
        } else {
            iv_mark.setVisibility(View.GONE);
        }

    }

    @Override
    public void onDestroyView() {
        // TODO Auto-generated method stub
        super.onDestroyView();
        Log.d(TAG, "onDestroyView");
        // if(mAtionBarReceiver!=null){
        // getActivity().unregisterReceiver(mAtionBarReceiver);
        // mAtionBarReceiver=null;
        // }
        if (mRebootReceiver != null) {
            getActivity().unregisterReceiver(mRebootReceiver);
        }
        if (mWindowReceiver != null) {
            getActivity().unregisterReceiver(mWindowReceiver);
        }
    }

    @Override
    public void onDestroy() {
        if (mService != null) {
            if (mCameraId == CameraInfo.CAMERA_FACING_BACK || mCameraId == RecorderActivity.CAMERA_THIRD) {
                Log.d(TAG, "cam at left");
                Log.d(TAG, "going to float myself");
                mService.resetFloatId();
                mService.startFloat();
                if (CustomValue.FULL_WINDOW) {
                    mService.getFloatWindow().setShow(View.GONE);
                }
                // getActivity().finish();
                mService.setStartCameraid(-1);
                mService.setIsChangingFloat(false, 10);
                mService.setLastIntent();
                if (mCurrentView != null) {
                    mCurrentView.setVisibility(View.GONE);
                }
            }
        }
        // TODO Auto-generated method stub
        myPreSp.edit().putBoolean("isVisible", false).commit();
        // myPreSp.edit().putBoolean("mIsLocked", mIsLocked).commit();
        myPreSp.edit().putBoolean("mIsRecording", mIsRecording).commit();
        myPreSp.edit().putBoolean("isGsonLock", false).commit();
        if (mAtionBarReceiver != null) {
            getActivity().unregisterReceiver(mAtionBarReceiver);
            mAtionBarReceiver = null;
        }
        mHolder = null;
        mGlSurfaceView = null;
        mStatusBar = null;
        mSpeed = null;
        mRecordIcon = null;
        mRecordTime = null;
        mRecordLock = null;
        mDate = null;
        mTime = null;
        mHintText = null;
        tv_water_mark = null;
        mButtonLock = null;
        mButtonSnapshot = null;
        mButtonRecord = null;
        mButtonAdas = null;
        mButtonDivider = null;
        mButtonMute = null;
        mButtonLockRight = null;
        mButtonSnapshotRight = null;
        mButtonRecordRight = null;
        mButtonMuteRight = null;
        mButtonSettings = null;
        mButtonReview = null;
        mPreview = null;
        mPreviewAnimation = null;
        mReverseLines = null;
        mCurrentView = null;
        mPreviewAnimation = null;
        mPreview = null;
        mAdasView = null;
        mRoadwayRenderer = null;
        mActivityManager = null;
        mAnimationManager = null;
        if (mService != null) {
            if (mService.getRecordCallback(mCameraId) == this) {
                mService.setRecordCallback(mCameraId, null);
            }
            mService.removeServiceListener(this);
            mService.removeSpeedListener(this);
        }
        mIntentReceiver = null;
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
        mViews = null;
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // inflater.inflate(R.menu.fragment_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return true;
    }

    @Override
    public void onClick(View view) {
        // TODO Auto-generated method stub

        Log.d(TAG, "onClick ===id " + view.getId());
        if (mService != null && mService.isBackCameraOut() && mCameraId == CameraInfo.CAMERA_FACING_BACK) {
            Log.d(TAG, "onClick, back plug out, miss");
            return;
        }
        onClickAnimation(view);
        switch (view.getId()) {
            case R.id.button_lock:
            case R.id.button_lock_right:
                if (mService != null) {
                    mIsLocked = !mIsLocked;
                    if (!mIsRecording && mIsLocked) {
                        Log.d(TAG, "onRecordStart once");
                        if (Storage.getTotalSpace() < 0) {
                            // todo more gentlly hint
                            Log.d(TAG, "startRecording sd not mounted");
                            Toast.makeText(getActivity(), R.string.sdcard_not_found, Toast.LENGTH_LONG).show();
                            Intent intent6 = new Intent();
                            intent6.setAction("com.action.other_Text");
                            intent6.putExtra("otherText", "TF卡不存在");
                            mService.sendBroadcast(intent6);
                            return;
                        } else if (Storage.getSdcardBlockSize() < 64 * 1024) {
                            Log.d(TAG, "check sdcard failed, sdcard block size " + (Storage.getSdcardBlockSize() / 1024)
                                    + "k");
                            showFormatMsgDialog();
                            return;
                        } else if (mService.isMiniMode()) {
                            Toast.makeText(getActivity(), R.string.device_busy, Toast.LENGTH_LONG).show();
                            return;
                        }
                        if (true) {
                            Log.v(TAG, "---------lock---------");
                            mService.setLockOnce(false);
                            mService.setLockFlag(true);
                            Intent intent3 = new Intent();
                            intent3.setAction("CLOSE_VIDEO_APP");
                            mService.sendBroadcast(intent3);
                            myEditor.putBoolean("isVideo", true).commit();
                            // Intent intent=new Intent();
                            // intent.setAction("com.action.other_Text");
                            // intent.putExtra("otherText", "录像已加锁");
                            // mService.sendBroadcast(intent);

                        } else {
                            mIsLocked = false;
                            Intent intent3 = new Intent();
                            intent3.setAction("CLOSE_VIDEO_APP");
                            mService.sendBroadcast(intent3);
                            myEditor.putBoolean("isVideo", true).commit();
                        }
                    } else if (mIsLocked) {
                        mService.setLockFlag(mIsLocked);
                        // Intent intent=new Intent();
                        // intent.setAction("com.action.other_Text");
                        // intent.putExtra("otherText", "录像已加锁");
                        // mService.sendBroadcast(intent);
                    }
                }
                break;
            case R.id.button_snapshot:
            case R.id.button_snapshot_right:
                if (Storage.getTotalSpace() < 0) {
                    // todo more gentlly hint
                    Log.d(TAG, "startRecording sd not mounted");
                    Toast.makeText(getActivity(), R.string.sdcard_not_found, Toast.LENGTH_LONG).show();
                    Intent intent7 = new Intent();
                    intent7.setAction("com.action.other_Text");
                    intent7.putExtra("otherText", "TF卡不存在");
                    mService.sendBroadcast(intent7);
                    return;
                } else if (Storage.getSdcardBlockSize() < 64 * 1024) {
                    Log.d(TAG, "check sdcard failed, sdcard block size " + (Storage.getSdcardBlockSize() / 1024) + "k");
                    showFormatMsgDialog();
                    return;
                } else if (Storage.getAvailableSpace() < 20 * 1024 * 1024) {
                    Log.d(TAG, "Storage.getAvailableSpace() =" + Storage.getAvailableSpace());
                    Toast.makeText(getActivity(), R.string.space_no_enough, Toast.LENGTH_LONG).show();
                    return;
                }
                if (mService != null) {
                    mService.takeSnapShot();
                }
                break;
            case R.id.button_record:
            case R.id.button_record_right:
                if (mService != null) {
                    mService.setLockOnce(false);
                    if (!mIsRecording) {
                        Log.d(TAG, "onRecordStart");
                        if (Storage.getTotalSpace() < 0) {
                            // todo more gentlly hint
                            Log.d(TAG, "startRecording sd not mounted");
                            Toast.makeText(getActivity(), R.string.sdcard_not_found, Toast.LENGTH_LONG).show();
                            Intent intent8 = new Intent();
                            intent8.setAction("com.action.other_Text");
                            intent8.putExtra("otherText", "TF卡不存在");
                            mService.sendBroadcast(intent8);
                            return;
                        } else if (Storage.getSdcardBlockSize() < 64 * 1024) {
                            Log.d(TAG, "check sdcard failed, sdcard block size " + (Storage.getSdcardBlockSize() / 1024)
                                    + "k");
                            showFormatMsgDialog();
                            return;
                        } else if (mService.isMiniMode()) {
                            Toast.makeText(getActivity(), R.string.device_busy, Toast.LENGTH_LONG).show();
                            return;
                        }
                        mService.startRecording();
                        Intent intent3 = new Intent();
                        intent3.setAction("CLOSE_VIDEO_APP");
                        mService.sendBroadcast(intent3);
                        String locale = Locale.getDefault().toString();
                        if (locale.equals("zh_CN") || locale.equals("zh_TW")) {
                            MediaPlayer mediaPlayer = MediaPlayer.create(mService, R.raw.start);
                            mediaPlayer.start();
                        }
                        myEditor.putBoolean("isVideo", true).commit();
                        mButtonRecord.setEnabled(false);
                        mButtonRecordRight.setEnabled(false);
                    } else {
                        Log.d(TAG, "onRecordStop");
                        if (mService.isMiniMode()) {
                            Toast.makeText(getActivity(), R.string.device_busy, Toast.LENGTH_LONG).show();
                            return;
                        }
                        mService.stopRecording();
                        mService.setLockOnce(false);
                        String locale = Locale.getDefault().toString();
                        if (locale.equals("zh_CN") || locale.equals("zh_TW")) {
                            MediaPlayer mediaPlayer = MediaPlayer.create(mService, R.raw.stop);
                            mediaPlayer.start();
                        }

                        Log.e("hy", "isvideo222222");
                        myEditor.putBoolean("isVideo", false).commit();
                        call = true;
                        mRecordLock.setVisibility(View.GONE);
                        mButtonLock.setImageResource(R.drawable.ic_lock_off);
                        mButtonRecord.setEnabled(false);
                        mButtonLockRight.setImageResource(R.drawable.ic_lock_off);
                        mButtonRecordRight.setEnabled(false);
                    }
                    // mIsRecording = !mIsRecording;
                }
                break;
            case R.id.button_adas:
                if (mService != null) {
                    mIsAdasOn = !mIsAdasOn;
                    if (mIsAdasOn != mService.isAdasOn()) {
                        mService.setIntelligentDetect(CameraInfo.CAMERA_FACING_FRONT, mIsAdasOn);
                    }
                    if (mIsAdasOn) {
                        if (mCameraId == CameraInfo.CAMERA_FACING_FRONT) {
                            Log.d(TAG, "set ADAS callback");
                            mRoadSoundPlayer.startMediaPlayer();
                            mAdasView.setVisibility(View.VISIBLE);
                            mAdasView.setHide(false);
                            mAdasView.setInstallAdjustMode(mPref.getCarLaneAdjust());
                            if (mGlSurfaceView != null) {

                                mGlSurfaceView.setVisibility(View.VISIBLE);
                            }

                            mRoadwayRenderer.setDisplayEnabled(true);
                            mService.setAdasDetecttionCallback(CameraInfo.CAMERA_FACING_FRONT, this);
                        }
                        mButtonAdas.setImageResource(R.drawable.ic_adas_on);
                    } else {
                        mButtonAdas.setImageResource(R.drawable.ic_adas_off);
                        if (mCameraId == CameraInfo.CAMERA_FACING_FRONT) {
                            Log.d(TAG, "set ADAS callback");
                            mRoadSoundPlayer.stopMediaPlayer();
                            // mAdasView.setVisibility(View.GONE);
                            mAdasView.setHide(true);
                            mAdasView.setInstallAdjustMode(false);
                            mAdasView.setHide(true);
                            mAdasView.setInstallAdjustMode(false);
                            if (mGlSurfaceView != null) {
                                mGlSurfaceView.setVisibility(View.GONE);
                            }
                            mRoadwayRenderer.setDisplayEnabled(false);
                            mService.setAdasDetecttionCallback(CameraInfo.CAMERA_FACING_FRONT, null);
                        }
                    }
                    mPref.saveAdasFlag(mIsAdasOn);
                }
                break;
            case R.id.button_mute:
            case R.id.button_mute_right:
                if (mService != null && mCameraId == CameraInfo.CAMERA_FACING_FRONT) {
                    mIsMuteOn = !mIsMuteOn;
                    mService.setMute(mIsMuteOn, CameraInfo.CAMERA_FACING_FRONT);
                    mService.setMute(mIsMuteOn, CameraInfo.CAMERA_FACING_BACK);
                    mService.setMute(mIsMuteOn, RecorderActivity.CAMERA_THIRD);
                    if (!mIsMuteOn) {
                        Intent intent = new Intent();
                        intent.setAction("com.action.other_Text");
                        intent.putExtra("otherText", "录音已打开");
                        mService.sendBroadcast(intent);
                        Log.v(TAG, "muteon");
                    } else {
                        Intent intent = new Intent();
                        intent.setAction("com.action.other_Text");
                        intent.putExtra("otherText", "录音已关闭");
                        mService.sendBroadcast(intent);
                        Log.v(TAG, "muteooff");

                    }
                }
                break;
            case R.id.button_settings:
                Log.i("tang", "button_settings");
                //Intent launchIntent =mService.getPackageManager().getLaunchIntentForPackage("com.zqc.videolisttest");
                //startActivity(launchIntent);

                if (mService != null && mService.isRecorderBusy()) {
                    Toast.makeText(getActivity(), R.string.device_busy, Toast.LENGTH_LONG).show();
                    return;
                }
                if (getActivity() instanceof RecorderActivity) {
                    if (mIsRecording) {
                        Log.i("tang", "mIsRecording-->" + mIsRecording);
                        mDialog = CDRAlertDialog.getInstance(getActivity());
                        if (null == mDialog) {
                            return;
                        }
                        mDialog.setTitle(R.string.warning);
                        mDialog.setMessage(R.string.goto_settings);
                        mDialog.setCallback(new ICDRAlertDialogListener() {

                            @Override
                            public void onClick(int state) {
                                Log.d(TAG, "onRecordStop");
                                if (mService.isMiniMode()) {
                                    Toast.makeText(getActivity(), R.string.device_busy, Toast.LENGTH_LONG).show();
                                    return;
                                }
                                mService.stopRecording();
                                Log.e("hy", "isvideo33333");
                                mpreferences.edit().putBoolean("isVideo", false).commit();
                                // mIsRecording = false;
                                ((RecorderActivity) getActivity())
                                        .loadViewByState(RecorderActivity.STATE_SETTINS_SINGLE_FRAGMENT);
                                mCurrentView.setVisibility(View.GONE);
                                if (mService != null) {
                                    mService.hideFloatWindows();
                                }
                            }

                            @Override
                            public void onTimeClick(int hour, int minute) {
                            }

                            @Override
                            public void onDateClick(int year, int month, int day) {
                            }
                        });
                        mDialog.setButtons();
                    } else {
                        Log.i("tang", "mIsRecording-->else" + mIsRecording);
                        ((RecorderActivity) getActivity()).loadViewByState(RecorderActivity.STATE_SETTINS_SINGLE_FRAGMENT);
                        mCurrentView.setVisibility(View.GONE);
                        if (mService != null) {
                            mService.hideFloatWindows();
                        }
                    }
                }

                break;
            case R.id.button_review:
                if (getActivity() instanceof RecorderActivity) {
                    if (mIsRecording) {
                        mDialog = CDRAlertDialog.getInstance(getActivity());
                        if (null == mDialog) {
                            return;
                        }
                        mDialog.setTitle(R.string.warning);
                        mDialog.setMessage(R.string.goto_record_browser);
                        mDialog.setCallback(new ICDRAlertDialogListener() {

                            @Override
                            public void onClick(int state) {
                                mButtonRecord.performClick();
                                ((RecorderActivity) getActivity()).loadViewByState(RecorderActivity.STATE_REVIEW);
                                mCurrentView.setVisibility(View.GONE);
                                // startPlayBack();
                            }

                            @Override
                            public void onTimeClick(int hour, int minute) {
                            }

                            @Override
                            public void onDateClick(int year, int month, int day) {
                            }
                        });
                        mDialog.setButtons();
                    } else {
                        ((RecorderActivity) getActivity()).loadViewByState(RecorderActivity.STATE_REVIEW);
                        mCurrentView.setVisibility(View.GONE);
                        // startPlayBack();
                    }
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onServiceBinded(RecordService service) {
        // TODO Auto-generated method stub
        if (mService != null && mService == service) {
            return;
        }
        if (getActivity() instanceof RecorderActivity) {
            Log.d(TAG, "onServiceBinded" + mService + "mCameraId=" + mCameraId);
            // ((RecorderActivity)getActivity()).addServiceBindedListener(this);
            mService = service;
            initRecorder();
            // add by chengyuzhou
            // mService.setMute(false, CameraInfo.CAMERA_FACING_FRONT);
            // end
        }
    }

    public void initSettings() {
        if (mPref == null) {
            return;
        }
        int duaration = mPref.getRecDuration();
        if (duaration == 0) {
            mDuaration = 1 * 60 * 1000;
        } else if (duaration == 1) {
            mDuaration = 2 * 60 * 1000;
        } else {
            mDuaration = 3 * 60 * 1000;
        }
        if (mCameraId == CameraInfo.CAMERA_FACING_BACK) {
            /*
             * if (mPref.getRearVisionFlip()) { mPreview.setRotationY((float)
             * 180.0); } else { mPreview.setRotationY((float) 0.0); }
             */
            if (mService != null) {
                mService.setFlip(mPref.getCameraFlipStatus(MyPreference.KEY_BACK_CAMERA_FLIP));
            }
        }
        if (mCameraId == RecorderActivity.CAMERA_THIRD) {
            if (mService != null) {
                mService.setRightFlip(mPref.getCameraFlipStatus(MyPreference.KEY_RIGHT_CAMERA_FLIP));
            }

        }
        if (MyPreference.isAdasOpen) {
            mIsAdasOn = mPref.getAdasFlag();
        } else {
            mIsAdasOn = false;
        }
        if (mCameraId == CameraInfo.CAMERA_FACING_BACK) {
            // back cam, do not register listener
            return;
        }
        mPref.setAdasFlagChangedListener(new IAdasFlagChanged() {
            @Override
            public void onAdasFlagChanged(boolean isOpen) {
                // TODO Auto-generated method stub
                if (mIsAdasOn != isOpen && mButtonAdas != null) {
                    if (MyPreference.isAdasOpen) {
                        mButtonAdas.performClick();
                    }
                }
            }
        });
        mIsMuteOn = mPref.isMute();
        if (mIsMuteOn) {
            mButtonMute.setImageResource(R.drawable.ic_mute_on);
            mButtonMuteRight.setImageResource(R.drawable.ic_mute_on);
        } else {
            mButtonMute.setImageResource(R.drawable.ic_mute_off);
            mButtonMuteRight.setImageResource(R.drawable.ic_mute_off);
        }
        if (mService != null && mCameraId == CameraInfo.CAMERA_FACING_FRONT) {
            mService.setMute(mIsMuteOn, CameraInfo.CAMERA_FACING_BACK);
            mService.setMute(mIsMuteOn, CameraInfo.CAMERA_FACING_FRONT);
        }
        mPref.setRecDurationChangedListener(new IRecDurationChanged() {

            @Override
            public void onRecDurationChanged(int value) {
                // TODO Auto-generated method stub
                if (value == 0) {
                    mDuaration = 1 * 60 * 1000;
                } else if (value == 1) {
                    mDuaration = 3 * 60 * 1000;
                } else {
                    mDuaration = 5 * 60 * 1000;
                }
                if (mService != null) {
                    mService.setDuration(value);
                }
            }

        });

        mPref.setRearFlipChangedListener(new IRearFlipChanged() {

            @Override
            public void onRearFlipChanged(boolean isFlip) {
                // TODO Auto-generated method stub
                /*
                 * if (isFlip) { mPreview.setRotationY((float) 180.0); } else {
                 * mPreview.setRotationY((float) 0.0); }
                 */
                /*
                 * if (mService != null) { mService.setFlip(isFlip); }
                 */
            }

            @Override
            public void onCameraFlipChanged(int cameraId, boolean isFlip) {
                // TODO Auto-generated method stub
                if (mService != null) {
                    mService.setCameraFlip(cameraId, isFlip);
                }
            }

        });

        mPref.setRearFlipChangedListener(new IRightFlipChanged() {

            @Override
            public void onRightFlipChanged(boolean isRightFlip) {
                // TODO Auto-generated method stub
                if (mService != null) {
                    mService.setRightFlip(isRightFlip);
                }
            }
        });
        mPref.setCarLaneAdjustChangedListener(new ICarLaneAdjustChanged() {

            @Override
            public void onCarLaneAdjustChanged(boolean value) {
                // TODO Auto-generated method stub
                mAdasView.setInstallAdjustMode(value);
            }

        });

        mPref.setPicQualityChangedListener(new IPicQualityChanged() {

            @Override
            public void onPicQualityChanged(int value) {
                // TODO Auto-generated method stub
                Log.d(TAG, "onPicQualityChanged value=" + value);
                if (mService != null) {
                    mService.setPicQualtiy(value);
                }

            }

        });
        mPref.setCrashSensityChangedListener(new ICrashSensityChanged() {

            @Override
            public void onCrashSensityChanged(int value) {
                // TODO Auto-generated method stub
                if (mService != null) {
                    mService.setCrashSensity(value);
                }
            }

        });
        mPref.setLockSensityChangedListener(new ILockSensityChanged() {

            @Override
            public void onLockSensityChanged(int value) {
                // TODO Auto-generated method stub
                if (mService != null) {
                    mService.setLockSensity(value);
                }
            }

        });

        mPref.setRecQualityChangedListener(new IRecQualityChanged() {

            @Override
            public void onRecQualityChanged(int value) {
                // TODO Auto-generated method stub
                if (mService != null) {
                    mService.setRecQuality(value);
                }
            }

        });

        mPref.setCarTypeChangedListener(new ICarTypeChanged() {

            @Override
            public void onCarTypeChanged(int value) {
                // TODO Auto-generated method stub
                if (mService != null) {
                    mService.setCarType(mCameraId, value);
                }
            }
        });
    }

    public void initRecorder() {
        if (mService == null) {
            Log.d(TAG, "initRecorder mService=null");
            return;
        }
        // add all camera
        boolean isReversing = mService.readBootStatus();
        if (!mService.isCameraAdd(CameraInfo.CAMERA_FACING_FRONT)) {
            mService.addCamera(CameraInfo.CAMERA_FACING_FRONT);
        }
        if ((!mService.isCameraAdd(CameraInfo.CAMERA_FACING_BACK))) {
            if (isReversing) {
                mFlag = mFlag & (~RecorderActivity.PREVIEW_START_RECORDING);
                mService.setNeedExit(false);
            } else {
                mService.addCamera(CameraInfo.CAMERA_FACING_BACK);
            }
        }
        /*
         * if (!mService.isCameraAdd(RecorderActivity.CAMERA_THIRD)) {
         * mService.addCamera(RecorderActivity.CAMERA_THIRD); }
         */

        /*
         * if (!mService.isCameraAdd(TwoFloatWindow.LEFT_CAMERA_ID)) {
         * mService.addCamera(TwoFloatWindow.LEFT_CAMERA_ID); }
         */

		/* remove by zdt
		 if (!mService.isCameraAdd(TwoFloatWindow.RIGHT_CAMERA_ID)) {
			mService.addCamera(TwoFloatWindow.RIGHT_CAMERA_ID);
		}*/

        mService.openCamera(CameraInfo.CAMERA_FACING_BACK);
        // mService.openCamera(TwoFloatWindow.LEFT_CAMERA_ID);
        // remove by zdt
        //mService.openCamera(TwoFloatWindow.RIGHT_CAMERA_ID);

        // set callback
        mService.setRecordCallback(mCameraId, this);

        Log.d(TAG, "mTextureIsUp=" + mHolderIsUp + ";mIsPreviewing=" + mIsPreviewing);
        Log.d(TAG, "-------------------mService.getNeedFloat(mCameraId)= " + mService.getNeedFloat(mCameraId));
        if (mService.getNeedFloat(mCameraId) >= 0) {
            // handle float
            mService.startFloat();
            if (mIsAdasOn && mService.getFloatCameraId() == CameraInfo.CAMERA_FACING_FRONT) {
                Log.d(TAG, "reset ADAS callback");
                if (mService.isAdasOn()) {
                    mService.setIntelligentDetect(CameraInfo.CAMERA_FACING_FRONT, false);
                    mService.setAdasDetecttionCallback(CameraInfo.CAMERA_FACING_FRONT, null);
                }
                if (mRoadSoundPlayer != null) {
                    mRoadSoundPlayer.stopMediaPlayer();
                }
                if (mAdasView != null) {
                    mAdasView.setVisibility(View.GONE);
                }
                if (mGlSurfaceView != null) {
                    mGlSurfaceView.setVisibility(View.GONE);
                }
                if (mRoadwayRenderer != null) {
                    mRoadwayRenderer.setDisplayEnabled(false);
                }
            }
            if (mCameraId == CameraInfo.CAMERA_FACING_FRONT) {
                Log.d(TAG, "miss front");
            } else {
                getActivity().finish();
                mService.setStartCameraid(-1);
                mService.setIsChangingFloat(false, 10);
            }
            return;
        } else {
            // start preview
            if (mHolderIsUp && !mIsPreviewing) {
                if (!mService.isPreview(mCameraId)) {
                    mService.startPreview(mCameraId);
                }
                mService.setPreviewDisplay(mCameraId, mHolder);
                mIsPreviewing = true;
            }

            if (!mService.isPreview(CameraInfo.CAMERA_FACING_BACK)) {
                mService.startPreview(CameraInfo.CAMERA_FACING_BACK);
            }
            /*
             * if (!mService.isPreview(TwoFloatWindow.LEFT_CAMERA_ID)) {
             * mService.startPreview(TwoFloatWindow.LEFT_CAMERA_ID); }
             */
			/* remove by zdt
			 * if (!mService.isPreview(TwoFloatWindow.RIGHT_CAMERA_ID)) {
				mService.startPreview(TwoFloatWindow.RIGHT_CAMERA_ID);
			}*/
            if (!mService.isWaterMarkRuning(CameraInfo.CAMERA_FACING_FRONT)) {
                mService.startWaterMark(CameraInfo.CAMERA_FACING_FRONT);
            }
            if (!mService.isWaterMarkRuning(CameraInfo.CAMERA_FACING_BACK)) {
                mService.startWaterMark(CameraInfo.CAMERA_FACING_BACK);
            }
            /*
             * if (!mService.isWaterMarkRuning(TwoFloatWindow.LEFT_CAMERA_ID)) {
             * mService.startWaterMark(TwoFloatWindow.LEFT_CAMERA_ID); }
             */
			/* remove by zdt
			 * if (!mService.isWaterMarkRuning(TwoFloatWindow.RIGHT_CAMERA_ID)) {
				mService.startWaterMark(TwoFloatWindow.RIGHT_CAMERA_ID);
			}*/
            Log.d(TAG, "-------------------setIsChangingFloat(0)");
            mService.setIsChangingFloat(false, 0);
        }
        // resume record ui
        if (mService.isRecording(mCameraId)) {
            if (mService.isBackCameraOut() && mCameraId == CameraInfo.CAMERA_FACING_BACK) {
                Log.d(TAG, "back cam is out");
            } else {
                onRecordStarted(true);
            }
        }
        if (mIsPowerOn) {
            Log.d(TAG, "onServiceBinded startFloat");
            int floatId = mService.getNeedFloat(CameraInfo.CAMERA_FACING_BACK);
            mService.setFloatCameraid(CameraInfo.CAMERA_FACING_BACK, floatId);
            if (floatId >= 0 && !isReversing) {
                mService.startFloat();
                mService.setNeedExit(false);
            }
            mIsPowerOn = false;
        }
        if (mCameraId == CameraInfo.CAMERA_FACING_BACK) {
            if (mService.getFastReverFlag()) {
                mReverseLines.setVisibility(View.VISIBLE);
                mService.setFastReverFlag(false);
            }
        }
        if (mCameraId == CameraInfo.CAMERA_FACING_FRONT) {
            mService.setFrontCamState(!mIsOnRight);
        }

        if (mIsAdasOn) {
            if (mCameraId == CameraInfo.CAMERA_FACING_FRONT) {
                Log.d(TAG, "set ADAS callback");
                mService.setIntelligentDetect(CameraInfo.CAMERA_FACING_FRONT, mIsAdasOn);
                mRoadSoundPlayer.startMediaPlayer();
                mAdasView.setVisibility(View.VISIBLE);
                mAdasView.setHide(false);
                mAdasView.setInstallAdjustMode(mPref.getCarLaneAdjust());
                if (mGlSurfaceView != null) {
                    mGlSurfaceView.setVisibility(View.VISIBLE);
                }
                mRoadwayRenderer.setDisplayEnabled(true);
                mService.setAdasDetecttionCallback(CameraInfo.CAMERA_FACING_FRONT, this);
            }
            mButtonAdas.setImageResource(R.drawable.ic_adas_on);
        }
        mService.addServiceListener(this);
        // add by chengyuzhou
        if (!mpreferences.getBoolean("isVideo", true)) {
            // onLocked(mService.getLockFlag(mCameraId));
        } else {
            if (!(mService.isBackCameraOut() && mCameraId == CameraInfo.CAMERA_FACING_BACK)) {
                onLocked(mService.getLockFlag(mCameraId));
                call = false;
            }
        }
        // end
        if (mPref != null) {
            mService.setFlip(mPref.getCameraFlipStatus(MyPreference.KEY_BACK_CAMERA_FLIP));
            mService.setRightFlip(mPref.getCameraFlipStatus(MyPreference.KEY_RIGHT_CAMERA_FLIP));
        }
        if (mCameraId == CameraInfo.CAMERA_FACING_FRONT) {
            // mService.setMute(true, CameraInfo.CAMERA_FACING_BACK);
            mService.setMute(mIsMuteOn, CameraInfo.CAMERA_FACING_BACK);
            mService.setMute(mIsMuteOn, CameraInfo.CAMERA_FACING_FRONT);
            mService.setMute(mIsMuteOn, RecorderActivity.CAMERA_THIRD);
            // add by chengyuzhou
            // mService.startFloat();
            // end
        }
        if (mService.isBackCameraOut() && mCameraId == CameraInfo.CAMERA_FACING_BACK) {
            mHintText.setText(R.string.please_insert_back_cam);
            //by lym start
            if (CustomValue.ONLY_ONE_CAMERA || CustomValue.CAMERA_NOT_RECORD) {
                mHintText.setVisibility(View.GONE);
            } else {
                mHintText.setVisibility(View.VISIBLE);
            }
            //end
            tv_water_mark.setVisibility(View.GONE);
            mButtonLock.setVisibility(View.GONE);
            mButtonSnapshot.setVisibility(View.GONE);
            mButtonRecord.setVisibility(View.GONE);
            if (mPreview != null) {
                mPreview.setVisibility(View.GONE);
            }
        }
        mService.addSpeedListener(this);
        if ((mFlag & RecorderActivity.PREVIEW_START_FROM_LAUNCHER) > 0 && !mIsAdasOn) {
            if (mCameraId == CameraInfo.CAMERA_FACING_FRONT) {
                Log.d(TAG, "initRecorder PREVIEW_START_FROM_LAUNCHER");
//				int stackId = SplitUtil.getStackBoxId(getActivity());
//				if (stackId > 0 && SplitUtil.getStackPostition(getActivity(), stackId) == ACTIVITY_ON_RIGHT) {
//					onToRight();
//				} else {
//					onToLeft();
//				}
                onToLeft();
            }
        }
        // add by chengyuzhou
        Boolean b = myPreSp.getBoolean("isGsonLock", false);

        if (myPreSp.getBoolean("isGsonLock", false)) {
            Log.v(TAG, "---------initRecorder--------:" + b);
            // mIsLocked = !mIsLocked;
            // mIsLocked=true;
            // call=true;
            // mService.setLockOnce(false);
            // mService.setLockFlag(mIsLocked);
            mButtonLock.performClick();
            myPreSp.edit().putBoolean("isGsonLock", false).commit();
        }
        // end
    }

    public void onClickAnimation(View view) {
        PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat("alpha", 1f, 0.8f, 1f);
        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat("scaleX", 1f, 0.92f, 1f);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat("scaleY", 1f, 0.92f, 1f);
        ObjectAnimator.ofPropertyValuesHolder(view, alpha, scaleX, scaleY).setDuration(200).start();
    }

    public void onSnapShotAnimation(View view) {
        /*
         * PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat("alpha",
         * 1f, 0.6f, 1f); PropertyValuesHolder scaleX =
         * PropertyValuesHolder.ofFloat("scaleX", 1f, 0.95f, 1f);
         * PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat("scaleY",
         * 1f, 0.95f, 1f); ObjectAnimator.ofPropertyValuesHolder(view, alpha,
         * scaleX, scaleY).setDuration(200).start();
         */
    }

    public void onWindowsChangeAnimation(final View target, boolean toRight) {
        target.setVisibility(View.GONE);
        PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat("alpha", 0f, 1f);
        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat("scaleX", toRight ? 0.5f : 1.5f, 1f);
        ObjectAnimator oa = ObjectAnimator.ofPropertyValuesHolder(target, alpha).setDuration(380);
        oa.addListener(new Animator.AnimatorListener() {

            @Override
            public void onAnimationCancel(Animator arg0) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onAnimationEnd(Animator arg0) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onAnimationRepeat(Animator arg0) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onAnimationStart(Animator arg0) {
                // TODO Auto-generated method stub
                target.setVisibility(View.VISIBLE);
            }

        });
        oa.start();
    }

    public void onRecordAnimation(View from, View to, final View target) {
        if (from == null || to == null || target == null) {
            return;
        }
        int fromWidth = from.getWidth();
        int fromHeight = from.getHeight();
        float scaleWEnd = 0f;
        float scaleHEnd = 0f;
        if (fromWidth > 0 && fromHeight > 0) {
            scaleWEnd = ((float) to.getWidth()) / fromWidth;
            scaleHEnd = ((float) to.getWidth()) / fromHeight;
        }
        PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat("alpha", 1f, 0.8f);
        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat("scaleX", 1f, scaleWEnd);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat("scaleY", 1f, scaleHEnd);
        PropertyValuesHolder tranX = PropertyValuesHolder.ofFloat("TranslationX", from.getX(), to.getX() - from.getX());
        PropertyValuesHolder tranY = PropertyValuesHolder.ofFloat("TranslationY", from.getY(), to.getY() - from.getY());
        ObjectAnimator oa = ObjectAnimator.ofPropertyValuesHolder(target, alpha, scaleX, scaleY, tranX, tranY)
                .setDuration(380);
        oa.addListener(new Animator.AnimatorListener() {

            @Override
            public void onAnimationCancel(Animator arg0) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onAnimationEnd(Animator arg0) {
                // TODO Auto-generated method stub
                target.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animator arg0) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onAnimationStart(Animator arg0) {
                // TODO Auto-generated method stub

            }

        });
        oa.start();
    }

    @Override
    public void onCameraOpen() {
        // TODO Auto-generated method stub
        Log.d(TAG, "onCameraOpen mCameraId=" + mCameraId);
        if ((mFlag & RecorderActivity.PREVIEW_START_RECORDING) > 0 && !mIsRecording) {
            if (mButtonRecord != null && mCameraId == CameraInfo.CAMERA_FACING_FRONT) {
                mHandler.sendEmptyMessageDelayed(MSG_START_RECORD, 8000);
            }
        }
    }

    @Override
    public void onRecordStarted(boolean isStarted) {
        // TODO Auto-generated method stub
        Log.d(TAG, "onRecordStarted");
        if (isStarted) {
            mIsRecording = isStarted;
            if (mButtonRecord != null && mRecordIcon != null) {
                mRecordIcon.setVisibility(View.VISIBLE);
                mButtonRecord.setImageResource(R.drawable.ic_record_on);
                mButtonRecord.setEnabled(true);
                myEditor.putBoolean("isVideo", true).commit();
            }
            if (mButtonRecordRight != null) {
                mButtonRecordRight.setImageResource(R.drawable.ic_record_on);
                mButtonRecordRight.setEnabled(true);
            }
        }
    }

    @Override
    public void onRecordStoped() {
        // TODO Auto-generated method stub
        Log.d(TAG, "onRecordStoped");
        mIsRecording = false;
        if (mButtonRecord != null && mRecordIcon != null) {
            mRecordIcon.setVisibility(View.GONE);
            mButtonRecord.setImageResource(R.drawable.ic_record_off);
            mButtonRecord.setEnabled(true);
            // add by chengyuzhou
            mRecordLock.setVisibility(View.GONE);
            Log.e("hy", "isvideo4444444");
            myEditor.putBoolean("isVideo", false).commit();
        }
        if (mButtonRecordRight != null) {
            mButtonRecordRight.setImageResource(R.drawable.ic_record_off);
            mButtonRecordRight.setEnabled(true);
        }
        if (mRecordTime != null) {
            mRecordTime.setText(R.string.default_record_time);
        }
        /*
         * if (mPreview != null) { onSnapShotAnimation(mPreview); }
         */
    }

    @Override
    public void onTimeUpdate(long curTime) {
        // TODO Auto-generated method stub
//		Log.i(TAG, "onTimeUpdate() curTime = " + curTime + "  mService= " + mService + "  mService.isBackCameraOut() "
//				+ mService.isBackCameraOut() + "  mCameraId= " + mCameraId);
        if (mService != null && mService.isBackCameraOut() && mCameraId == CameraInfo.CAMERA_FACING_BACK) {
            return;
        }
        SimpleDateFormat recordFormatter = new SimpleDateFormat("mm:ss");
        String recordTime = recordFormatter.format(curTime);
        if (mRecordTime != null && mIsRecording) {
            mRecordTime.setText(recordTime);
        }
    }

    private boolean isHomePress = false;

    @Override
    public void onHomePressed() {
        // TODO Auto-generated method stub
        Log.d(TAG, "onHomePressed mCameraId=" + mCameraId + ";mActivityManager=" + mActivityManager);
        isHomePress = true;
        if (mActivityManager != null) {
            int stackId = SplitUtil.getStackBoxId(getActivity());
            Log.d(TAG, "stackId =" + stackId);
            if (stackId > 0 && SplitUtil.getStackPostition(getActivity(), stackId) == ACTIVITY_ON_LEFT) {
                // at left
                if (mService != null) {
                    if (mCameraId == CameraInfo.CAMERA_FACING_BACK || mCameraId == RecorderActivity.CAMERA_THIRD) {
                        Log.d(TAG, "cam at left");
                        Log.d(TAG, "going to float myself");
                        mService.resetFloatId();
                        mService.startFloat();
                        getActivity().finish();
                        mService.setStartCameraid(-1);
                        mService.setIsChangingFloat(false, 10);
                        mService.setLastIntent();
                        if (mCurrentView != null) {
                            mCurrentView.setVisibility(View.GONE);
                        }
                    } else {
                        Log.d(TAG, "frontcam at left");
                        if (!mService.isCameraAdd(CameraInfo.CAMERA_FACING_BACK)) {
                            mService.addCamera(CameraInfo.CAMERA_FACING_BACK);
                        }
                        mService.resetFloatId();
                        mService.startFloat();
                        getActivity().finish();
                        mService.setStartCameraid(RecordService.CAMERA_ID_FRONT);
                        mService.setIsChangingFloat(true, 30);
                    }
                }
            } else {
                // at right or full screen
                if (mService != null) {
                    Log.d(TAG, "backcam not running");
                    if (!mService.isCameraAdd(CameraInfo.CAMERA_FACING_BACK)) {
                        mService.addCamera(CameraInfo.CAMERA_FACING_BACK);
                        mService.resetFloatId();
                        mService.startFloat();
                        mService.setLastIntent();
                    } else {
                        if (CustomValue.FULL_WINDOW) {
                            mService.getFloatWindow().setHomePressed();
                        }
                        Log.d(TAG, "onHomePressed:mService == null ");
                    }
                }
            }
        }
        if (mDialog != null) {
            mDialog.dismiss();
        }

        if (mService != null) {
            mService.setCanThirdLongClick(true);
        }
    }

    @Override
    public void onMute(boolean isMuted) {
        // TODO Auto-generated method stub
        if (mCameraId == CameraInfo.CAMERA_FACING_BACK) {
            mIsMuteOn = true;
        } else {
            mIsMuteOn = isMuted;
        }
        if (mButtonMute != null) {
            if (isMuted) {
                mButtonMute.setImageResource(R.drawable.ic_mute_on);
            } else {
                mButtonMute.setImageResource(R.drawable.ic_mute_off);
            }
        }
        if (mButtonMuteRight != null) {
            if (isMuted) {
                mButtonMuteRight.setImageResource(R.drawable.ic_mute_on);
            } else {
                mButtonMuteRight.setImageResource(R.drawable.ic_mute_off);
            }
        }
        if (mPref != null) {
            mPref.saveMute(mIsMuteOn);
        }
    }

    @Override
    public void onLocked(boolean isLocked) {
        // TODO Auto-generated method stub
        Log.i(TAG, "isLocked = " + isLocked);
        Log.v(TAG, "iscall = " + call);
        myPreSp.edit().putBoolean("isGsonLock", false).commit();
        // Log.v(TAG, "---------------------test-----------------");
        if (mRecordLock != null) {
            mIsLocked = isLocked;
            if (isLocked) {
                mService.setLockOnce(false);
                mService.setLockFlag(true);
                // add by chengyuzhou
                if (Storage.getTotalSpace() > 0) {
                    // if(!mpreferences.getBoolean("isVideo", true)){
                    //
                    if (!mService.isRecording(CameraInfo.CAMERA_FACING_FRONT)) {


                        if (Storage.getTotalSpace() < 0) {
                            // todo more gentlly hint
                            Log.d(TAG, "startRecording sd not mounted");
                            Toast.makeText(getActivity(), R.string.sdcard_not_found, Toast.LENGTH_LONG).show();
                            Intent intent8 = new Intent();
                            intent8.setAction("com.action.other_Text");
                            intent8.putExtra("otherText", "TF卡不存在");
                            mService.sendBroadcast(intent8);
                            return;
                        } else if (Storage.getSdcardBlockSize() < 64 * 1024) {
                            Log.d(TAG, "check sdcard failed, sdcard block size " + (Storage.getSdcardBlockSize() / 1024)
                                    + "k");
                            showFormatMsgDialog();
                            return;
                        } else if (mService.isMiniMode()) {
                            Toast.makeText(getActivity(), R.string.device_busy, Toast.LENGTH_LONG).show();
                            return;
                        }

                        mService.startRecording();
                        myEditor.putBoolean("isVideo", true).commit();
                        //
                    }
                    if (call) {
                        Intent intent = new Intent();
                        intent.setAction("com.action.other_Text");
                        intent.putExtra("otherText", "录像已加锁");
                        mService.sendBroadcast(intent);
                        Intent intentLock = new Intent();
                        intentLock.setAction("CLOSE_VIDEO_APP");
                        mService.sendBroadcast(intentLock);
                        // mRecordLock.setVisibility(View.VISIBLE);
                        // mButtonLock.setImageResource(R.drawable.ic_lock_on);
                        call = false;
                    }
                    Boolean b = mService.isRecording(CameraInfo.CAMERA_FACING_FRONT);
                    Log.v(TAG, "-------lsockIsRecord-------" + b);
                    if (mService.isRecording(CameraInfo.CAMERA_FACING_FRONT)) {
                        mRecordLock.setVisibility(View.VISIBLE);
                        mButtonLock.setImageResource(R.drawable.ic_lock_on);
                        mButtonLockRight.setImageResource(R.drawable.ic_lock_on);
                        mService.setLockFlag(true);
                    }
                }

            } else {
                call = true;
                mRecordLock.setVisibility(View.GONE);
                mButtonLock.setImageResource(R.drawable.ic_lock_off);
                mButtonLockRight.setImageResource(R.drawable.ic_lock_off);
            }
        }

    }

    @Override
    public void onPictureToken() {
        // TODO Auto-generated method stub
        if (mService != null) {
            mService.playShutterClick();
        }
        if (mPreview != null) {
            onSnapShotAnimation(mPreview);
        }
        /*
         * if (mPreview != null && mPreviewAnimation != null) { Bitmap bm1 =
         * mPreview.getBitmap(); mPreviewAnimation.setImageBitmap(bm1);
         * mPreviewAnimation.setVisibility(View.VISIBLE);
         * onRecordAnimation(mPreview, mButtonRecord, mPreviewAnimation); }
         */
    }

    @Override
    public void onCameraPlug(boolean isOut) {
        // TODO Auto-generated method stub
        Log.d(TAG, "isout:" + isOut);
        if (mCameraId != CameraInfo.CAMERA_FACING_BACK) {
            return;
        }
        if (isOut) {
            if (mHintText != null) {
                mHintText.setText(R.string.please_insert_back_cam);
                //by lym start
                if (CustomValue.ONLY_ONE_CAMERA || CustomValue.CAMERA_NOT_RECORD) {
                    mHintText.setVisibility(View.GONE);
                } else {
                    mHintText.setVisibility(View.VISIBLE);
                }
                //end
                tv_water_mark.setVisibility(View.GONE);
                mButtonLock.setVisibility(View.GONE);
                mButtonSnapshot.setVisibility(View.GONE);
                mButtonRecord.setVisibility(View.GONE);
                mRecordLock.setVisibility(View.GONE);
            }
            if (mPreview != null) {
                mPreview.setVisibility(View.GONE);
            }
        } else {
            if (mHintText != null) {
                mHintText.setVisibility(View.GONE);
                tv_water_mark.setVisibility(View.VISIBLE);
                mButtonLock.setVisibility(View.VISIBLE);
                mButtonSnapshot.setVisibility(View.VISIBLE);
                mButtonRecord.setVisibility(View.VISIBLE);
            }
            if (mPreview != null) {
                mPreview.setVisibility(View.VISIBLE);
            }
            if (mService != null) {
                if (!mService.getFastReverFlag()) {
                    int floatId = mService.getNeedFloat(CameraInfo.CAMERA_FACING_BACK);
                    if (floatId >= 0) {
                        mService.startFloat(CameraInfo.CAMERA_FACING_BACK, floatId);
                    } else {
                        if (mHolderIsUp) {
                            mService.setPreviewDisplay(mCameraId, mHolder);
                        }
                    }
                } else {
                    mService.onRerverseMode(true);
                }
            }

        }
    }

    @Override
    public void onSpeedChange(float speed, int status, double longitude, double latitude) {
        if (mSpeed != null) {
            mSpeed.setText(String.valueOf((int) (speed * 3.6)));
        }
        tv_water_mark.setText("E: " + df.format(longitude) + "  N: " + df.format(latitude));
        cleanHanlder.removeCallbacks(clearGpsRunnable);
        cleanHanlder.postDelayed(clearGpsRunnable, 3000);
    }

    private Handler cleanHanlder = new Handler();

    private ClearGpsRunnable clearGpsRunnable = new ClearGpsRunnable();

    class ClearGpsRunnable implements Runnable {

        @Override
        public void run() {
            if (null != tv_water_mark) {
                tv_water_mark.setText("E: 0.000000  N: 0.000000");
            }

        }

    }

    @Override
    public void onAdasDetection(Adas adas, Camera camera) {
        // Log.d(TAG, "onAdasDetection adas=" + adas);
        if (adas == null) {
            Log.e(TAG, "Adas is null!");
            return;
        }
        if (mRoadSoundPlayer != null) {
            mRoadSoundPlayer.checkAdas(adas);
        }
        /*
         * if (Math.abs(System.currentTimeMillis() - mAdasLast) > 16000) {
         * mRoadSoundPlayer.checkAdas(adas); mAdasLast =
         * System.currentTimeMillis(); }
         */
        Message msg = Message.obtain();
        msg.what = MSG_ADAS_CALLBACK;
        msg.obj = adas;
        if (mHandler != null) {
            mHandler.removeMessages(MSG_ADAS_CALLBACK);
            mHandler.sendMessage(msg);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int arg1, int arg2, int arg3) {
        // TODO Auto-generated method stub

        int stackId = SplitUtil.getStackBoxId(getActivity());
        if (stackId > 0 && SplitUtil.getStackPostition(getActivity(), stackId) == ACTIVITY_ON_RIGHT) {
            onToRight();
        }

        Log.d(TAG, "surfaceChanged, stackId: " + stackId + ", activityState: " + SplitUtil.getStackPostition(getActivity(), stackId));
    }

    @Override
    public void surfaceCreated(SurfaceHolder sh) {
        // TODO Auto-generated method stub
        mHolderIsUp = true;
        Log.d(TAG, "surfaceCreated");
        Log.d(TAG, "mHolderIsUp=" + mHolderIsUp + ";mIsPreviewing=" + mIsPreviewing);
        Log.d(TAG, "mHolder=" + mHolder + ";sh=" + sh + " isHomePress " + isHomePress
                + " mPreview " + mPreview.getVisibility());
        mHolder = sh;

        if (mService != null && mService.getNeedFloat(mCameraId) < 0) {
            mService.setPreviewDisplay(mCameraId, mHolder);
            if (CustomValue.FULL_WINDOW && !isHomePress) {
                mService.getFloatWindow().setShow(View.VISIBLE);
            }
            /*
             * if (mHolder != null) { Canvas cv = mHolder.lockCanvas(); if (cv
             * != null) { cv.drawColor(Color.BLACK);
             * mHolder.unlockCanvasAndPost(cv); } }
             */
            mIsPreviewing = true;
            if (!mService.isPreview(mCameraId)) {
                mService.startPreview(mCameraId);
            }
            mIsPreviewing = true;
            if (!mService.isWaterMarkRuning(mCameraId)) {
                mService.startWaterMark(mCameraId);
            }
            mService.startRender(mCameraId);
        }
        if (mIsDrawFrame) {
            if (mHolder != null) {
                Canvas cv = mHolder.lockCanvas();
                if (cv != null) {
                    cv.drawColor(Color.BLACK);
                    mHolder.unlockCanvasAndPost(cv);
                }
                mIsDrawFrame = false;
            }
        }
        isHomePress = false;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // TODO Auto-generated method stub
        Log.d(TAG, "surfaceDestroyed");
        if (mService != null) {
            if (CustomValue.FULL_WINDOW) {
                mService.getFloatWindow().setShow(View.GONE);
            }
            /*
             * if(mService.isRender(mCameraId)) {
             * mService.stopRender(mCameraId); }
             * if(mService.isPreview(mCameraId)) {
             * mService.stopPreview(mCameraId); }
             */
        }
        /*
         * if (holder != null) { Canvas cv = holder.lockCanvas(); if (cv !=
         * null) { cv.drawColor(Color.BLACK); holder.unlockCanvasAndPost(cv); }
         * }
         */
        mHolderIsUp = false;
        mIsPreviewing = false;
        isHomePress = false;
        return;
    }

    private long time = 0L;

    public void showFormatMsgDialog() {
        long currentTime = System.currentTimeMillis();
        if ((currentTime - time) > 2000) {
            time = currentTime;
        } else {
            time = currentTime;
            return;
        }
        CDRAlertDialog dialog = CDRAlertDialog.getInstance(getActivity());
        if (null == dialog) {
            return;
        }
        dialog.setTitle(R.string.format_sdcard_message);
        dialog.setMessage(R.string.sd1_format_or_not);
        dialog.setCallback(new ICDRAlertDialogListener() {

            @Override
            public void onClick(int state) {
                Storage.formatSDcard(getActivity());
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

    @Override
    public void onWindowsChanged(boolean isOnRight) {
        // TODO Auto-generated method stub
        if (DEBUG)
            Log.d(TAG, "onWindowsChanged isOnRight=" + isOnRight);
        if (mViews != null && mIsOnRight != isOnRight) {
            onWindowsChangeAnimation(mViews, isOnRight);
        }
        if (mService != null && mCameraId == CameraInfo.CAMERA_FACING_FRONT) {
            mService.setFrontCamState(!isOnRight);
        }
        if (mIsOnRight != isOnRight && mCameraId == CameraInfo.CAMERA_FACING_FRONT) {
            mIsOnRight = isOnRight;
            if (isOnRight) {
                if (mButtonSettings != null) {
                    mButtonSettings.setVisibility(View.GONE);
                    mButtonDivider.setVisibility(View.GONE);
                    if (MyPreference.getChipType() == MyPreference.IC_V66) {
                        mButtonReview.setVisibility(View.GONE);
                        mButtonMute.setBackgroundResource(R.drawable.bg_button_right);
                        mButtonMuteRight.setBackgroundResource(R.drawable.bg_button_right);
                    } else {
                        // mButtonMute.setVisibility(View.VISIBLE);
                        mButtonReview.setVisibility(View.GONE);
                        mButtonMute.setBackgroundResource(R.drawable.bg_button_right);
                        mButtonMuteRight.setBackgroundResource(R.drawable.bg_button_right);
                        if (mDate != null) {
                            mDate.setVisibility(View.GONE);
                        }
                    }
                }
            } else {
                if (mButtonSettings != null) {
                    mButtonSettings.setVisibility(View.VISIBLE);
                    mButtonDivider.setVisibility(View.GONE);
                    // mButtonReview.setVisibility(View.VISIBLE);
                    if (MyPreference.getChipType() == MyPreference.IC_V66) {
                        mButtonReview.setVisibility(View.GONE);
                        mButtonMute.setBackgroundResource(R.drawable.bg_button_center);
                        mButtonMuteRight.setBackgroundResource(R.drawable.bg_button_center);
                        // mButtonReview.setBackgroundResource(R.drawable.bg_button_right);
                        mButtonSettings.setBackgroundResource(R.drawable.bg_button_right);
                    } else {
                        // mButtonMute.setVisibility(View.GONE);
                        mButtonReview.setVisibility(View.GONE);
                        mButtonMute.setBackgroundResource(R.drawable.bg_button_center);
                        mButtonMuteRight.setBackgroundResource(R.drawable.bg_button_center);
                        // mButtonReview.setBackgroundResource(R.drawable.bg_button_right);
                        mButtonSettings.setBackgroundResource(R.drawable.bg_button_right);
                        if (mDate != null) {
                            mDate.setVisibility(View.VISIBLE);
                        }

                    }
                    if (CustomValue.CHANGE_FRONT_BACK_CAMERA||CustomValue.ONLY_ONE_CAMERA) {
                        mButtonAdas.setVisibility(View.GONE);
                    }
                } else {
                    Log.d(TAG, "onWindowsChanged: mButtonSettings=null");
                }
            }
        }
//        if (CustomValue.CHANGE_FRONT_BACK_CAMERA && mIsOnRight != isOnRight && mCameraId == CameraInfo.CAMERA_FACING_BACK) {
//            mButtonAdas.setVisibility(View.VISIBLE);
//        }
    }

    public void onLockKeyDown() {
        mButtonLock.performClick();
    }

    public void onSnapShot() {
        mButtonSnapshot.performClick();
    }

    private void changePreview() {
        Log.d(TAG, "changePreview()");
        int state = RecorderActivity.STATE_DEFAULT;
        if (mCameraId == CameraInfo.CAMERA_FACING_BACK) {
            state = RecorderActivity.STATE_FRONT_PREVIEW;
        } else if (mCameraId == CameraInfo.CAMERA_FACING_FRONT) {
            state = RecorderActivity.STATE_BACK_PREVIEW;
        }
        ((RecorderActivity) getActivity()).switchCameraByState(state);
    }

    private void doubleClick() {

        Intent intent2 = new Intent();
        intent2.putExtra("CameraId", mCameraId);
        intent2.setAction("com.zqc.action.SHOW_STREAM_PREVIEW_WINDOW");
        getActivity().sendBroadcast(intent2);
		
		/*Intent intent = new Intent(getActivity(), FullScreenCameraActivity.class);
		int key = 1;
		if (mCameraId == CameraInfo.CAMERA_FACING_FRONT) {
			key = FullScreenCameraActivity.STATE_FULLSCREEN_SHOW_CAMERA_FRONT;
		} else if (mCameraId == CameraInfo.CAMERA_FACING_BACK) {
			key = FullScreenCameraActivity.STATE_FULLSCREEN_SHOW_CAMERA_BACK;
		}
		intent.putExtra(FullScreenCameraActivity.KEY_STATUS, key);
		startActivity(intent);
		if (null != mService) {
			mService.hideTwoFloat();
		}*/
    }

    public void onToLeft() {
        Log.d(TAG, "onToLeft mCameraId " + mCameraId);
        /*
         * int stackId = SplitUtil.getStackBoxId(getActivity()); int rigtStackId
         * = SplitUtil.getRightStackId(getActivity()); Log.d(TAG, "stackId =" +
         * stackId); SplitUtil.setWindowSize(getActivity(), rigtStackId,
         * MW_MIN_STACK_WINDOW); SplitUtil.setWindowSize(getActivity(), stackId,
         * MW_MAX_STACK_WINDOW);
         */

        //by lym start
//		if (CustomValue.CHANGE_FRONT_BACK_CAMERA){
//            mService.setToLeft();
//        }
        // end

        initMarkImageView();
        if (!mIsAdasOn && MyPreference.getChipType() == MyPreference.IC_V66) {
            mIsAdasOn = true;
            if (mService != null && mCameraId == CameraInfo.CAMERA_FACING_FRONT) {
                Log.d(TAG, "set ADAS callback");
                mService.setIntelligentDetect(CameraInfo.CAMERA_FACING_FRONT, mIsAdasOn);
                mRoadSoundPlayer.startMediaPlayer();
                mAdasView.setVisibility(View.VISIBLE);
                mAdasView.setHide(false);
                mAdasView.setInstallAdjustMode(mPref.getCarLaneAdjust());
                if (mGlSurfaceView != null) {
                    mGlSurfaceView.setVisibility(View.VISIBLE);
                }
                mRoadwayRenderer.setDisplayEnabled(true);
                mService.setAdasDetecttionCallback(CameraInfo.CAMERA_FACING_FRONT, this);
            }
            mButtonAdas.setImageResource(R.drawable.ic_adas_on);
            mPref.saveAdasFlag(mIsAdasOn);
        }

        if (mAdasLayout != null) {
            RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) mAdasLayout.getLayoutParams();
            lp.width = RelativeLayout.LayoutParams.MATCH_PARENT;
            lp.height = RelativeLayout.LayoutParams.MATCH_PARENT;
            mAdasLayout.setLayoutParams(lp);
        }

        if (layoutOperation != null) {
            if (CustomValue.CAMERA_NOT_RECORD) {
                layoutOperation.setVisibility(View.GONE);
            } else {
                layoutOperation.setVisibility(View.VISIBLE);
            }
        }
        if (layoutOperationRight != null) {
            layoutOperationRight.setVisibility(View.GONE);
        }
        if (layoutSpeedBar != null) {
            layoutSpeedBar.setVisibility(View.VISIBLE);
        }
        if (tv_water_mark != null) {
            tv_water_mark.setVisibility(View.VISIBLE);
        }

        if (RecorderActivity.CAMERA_COUNT == 2 && mService != null) {
            Log.d(TAG, "zdt --- 11 left mService != null");
            mService.updateFloatWindow(DoubleFloatWindow.ON_LEFT);
            mService.showFloatWindows();
            //add by lym start
//            mService.doPerformClick();
            //end
        }

    }

    public void onToRight() {
        Log.d(TAG, "onToRight");
        if (layoutOperation != null) {
            layoutOperation.setVisibility(View.GONE);
        }
        if (layoutOperationRight != null) {
            if (CustomValue.CAMERA_NOT_RECORD) {
                layoutOperationRight.setVisibility(View.GONE);
            } else {
                layoutOperationRight.setVisibility(View.VISIBLE);
            }
        }
        if (layoutSpeedBar != null) {
            layoutSpeedBar.setVisibility(View.GONE);
        }

        if (mAdasLayout != null) {
            RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) mAdasLayout.getLayoutParams();
            lp.width = RelativeLayout.LayoutParams.MATCH_PARENT;
            lp.height = (RecorderActivity.CAMERA_COUNT == 1) ? 425 : 370;
            mAdasLayout.setLayoutParams(lp);
        }
        if (tv_water_mark != null) {
            tv_water_mark.setVisibility(View.INVISIBLE);
        }

        if (RecorderActivity.CAMERA_COUNT == 2) {
            if (mService != null) {
                Log.d(TAG, "zdt --- 11 right mService != null");
                if (!SplitUtil.isFullWindow(getActivity())) {
                    mService.updateFloatWindow(DoubleFloatWindow.ON_RIGHT);
                    mService.showFloatWindows();
                }
            } else {
                Log.d(TAG, "zdt --- 11 right mService is null");
                mHandler.sendEmptyMessageDelayed(MSG_CHANGE_FLOATWINDOW_RIGHT, 1000);
            }
        }
    }

    private void startPlayBack() {
        Intent it = new Intent();
        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        it.setComponent(
                new ComponentName("com.spreadwin.videoplayback", "com.spreadwin.videoplayback.ui.home.HomeActivity"));
        try {
            getActivity().startActivity(it);
        } catch (ActivityNotFoundException ex) {
            ex.printStackTrace();
        }

    }

    @Override
    public int onAskLeftCameraId() {
        // TODO Auto-generated method stub
        int stackId = SplitUtil.getStackBoxId(getActivity());
        Log.d(TAG, "onAskLeftCameraId stackId =" + stackId);
        if (stackId > 0 && SplitUtil.getStackPostition(getActivity(), stackId) == ACTIVITY_ON_LEFT) {
            // at right do nothing
            Log.d(TAG, "at left screen");
            return mCameraId;
        }
        return -1;
    }

}
