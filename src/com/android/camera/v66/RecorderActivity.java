package com.android.camera.v66;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentManager.BackStackEntry;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.view.animation.Animation;
import com.android.camera.v66.RecordService.IServiceListener;
import com.android.camera2.R;
import android.os.SystemProperties;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class RecorderActivity extends Activity implements IServiceListener {
    
    public static final String TAG = "RecorderActivity";
    public static final String TITLE_FRAGMENT = "TitleFragment";
    public static final String LEFT_PREFERENCE = "LeftPreference";
    public static final String RIGHT_PREFERENCE = "RightPreference";
    public static final String FRONT_PREVIEW_FRAGMENT = "FrontPreviewFragment";
    public static final String BACK_PREVIEW_FRAGMENT = "BackPreviewFragment";
    public static final String THIRD_PREVIEW_FRAGMENT = "ThirdPreviewFragment";
    public static final String REVIEW_FRAGMENT = "ReviewFragment";
    public static final String SETTINGS_FRAGMENT = "SettinsFragment";
    public static final String DOUBLE_PREVIEW_FRAGMENT = "DoublePreviewFragment";
    public static final String SAVE_STATE = "SavedInstanceState";
    public static final String SAVE_CAMERA_ID = "SavedCurrentCameraId";
    public static final String PREVIEW_START_FLAG = "PreviewStartFlag";
    public static final String EXTRA_CAM_TYPE = "REQ_CAM_TYPE";
    public static final String ACTION_HOME_PRESS = "android.intent.action.HOME_PRESS";
    public static final String ACTION_HIDE_LAYOUT = "com.zqc.action.hide_layout";
    public static final String ACTION_SHOW_LAYOUT = "com.zqc.action.show_layout";
    public static final String ACTION_FINISH = "action_finish";
    public static final int STATE_DEFAULT = -1;
    public static final int STATE_FRONT_PREVIEW = 0;
    public static final int STATE_BACK_PREVIEW = 1;
    public static final int STATE_SETTINGS = 2;
    public static final int STATE_REVIEW = 3;
    public static final int STATE_FRONT_RECORD = 4;
    public static final int STATE_BACK_RECORD = 5;
    public static final int STATE_SETTINS_SINGLE_FRAGMENT = 6;
    public static final int STATE_FLOAT_BACK_PREVIEW = 7;// not used
    public static final int STATE_FLOAT_BACK_RECORD = 8;// not used
    public static final int STATE_DOUBLE_PREVIEW = 9;
    public static final int STATE_THIRD_PREVIEW = 10;
    public static final int STATE_THIRD_RECORD = 11;
    public static final int PREVIEW_START_FRONT = 1 << 1;
    public static final int PREVIEW_START_BACK = 1 << 2;
    public static final int PREVIEW_START_RECORDING = 1 << 3;
    public static final int PREVIEW_START_FROM_LAUNCHER = 1 << 4;
    public static final int PREVIEW_START_THIRD = 1 << 5;
    public static final int MSG_RECONNECT_SERVICE = 0;
    public static final int MSG_DELAY_TO_FINISH = 1;
    public static final long RECONNECT_DELAY = 3000;
    public static final int ACTIVITY_ON_LEFT = 1;
    public static final int ACTIVITY_ON_RIGHT = 0;
    public static final int CAMERA_THIRD = 2;
    
    private int mActivityState = STATE_FRONT_PREVIEW;
    private int mCameraId = CameraInfo.CAMERA_FACING_FRONT;
    private FrameLayout mContainer = null;
    private Fragment mCurFragment = null;
    private List<IServiceBindedListener> mListeners = new ArrayList<IServiceBindedListener>();
    private boolean mIsFirtUp = true;
    private boolean mIsPaused = false;
    private int mFlag = 0;
    private int mPreState = STATE_FRONT_PREVIEW;
    private boolean mIsFromLauncher = false;
    private int mInitState = STATE_FRONT_PREVIEW;
    private static boolean isvalidate = SystemProperties.getBoolean("ro.se.settings.iscamerahide", false);

    //获取摄像头数量，控制单录或者双录，默认双录
  	public static int CAMERA_COUNT = SystemProperties.getInt("ro.camera.count", 2);
//    public static int CAMERA_COUNT = 1;
    
    /*start   add by huqiucheng*/
    private static final int MSG_TOUCH =1;
    private static final int DELAY_TIME = 30*1000;
    private Handler timeHandler = new Handler(){
    	public void handleMessage(Message msg) {
    		switch (msg.what) {
			case MSG_TOUCH:
				Log.i(TAG, "delay 2min!");
				if (isvalidate) {
					sendBroadcast(new Intent(ACTION_HIDE_LAYOUT));
				}
				break;

			default:
				break;
			}
    		
    	};
    };

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		if (isvalidate) {
			startCounterTime();
		}
		return super.dispatchTouchEvent(ev);
	}
	
	private void startCounterTime(){
		Log.i(TAG, "startCounterTime");
		sendBroadcast(new Intent(ACTION_SHOW_LAYOUT));
		timeHandler.removeMessages(MSG_TOUCH);
		timeHandler.sendEmptyMessageDelayed(MSG_TOUCH, DELAY_TIME);
	}
	
	
	
    /*end   add by huqiucheng*/
    
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_RECONNECT_SERVICE:
                    startRecordService();
                    bindRecordService();
                    break;
                case MSG_DELAY_TO_FINISH:
                    PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                    if (pm != null && pm.isScreenOn()) {
                        finish();
                    }
                    break;
                default:
                    break;
            }
            super.handleMessage(msg);
        }
    };
    
    private RecordService mRecordService = null;
    private ServiceConnection mRecConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            if (mRecordService == null) {
                mRecordService = ((RecordService.LocalBinder) service).getService();
                Log.i(TAG, "RecordService::onServcieConnectd" + mRecordService);
            }
            
            if (mRecordService != null) {
                for (int i = 0; i < mListeners.size(); i++) {
                    if (mListeners.get(i) != null) {
                        mListeners.get(i).onServiceBinded(mRecordService);
                    }
                }
                mRecordService.addServiceListener(RecorderActivity.this);
            } else {
                mHandler.sendEmptyMessageDelayed(MSG_RECONNECT_SERVICE, RECONNECT_DELAY);
            }
        }
        
        public void onServiceDisconnected(ComponentName className) {
            Log.i(TAG, "RecordService::onServcieDisconnectd");
            mRecordService = null;
        }
    };
    
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        
        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            Log.d(TAG, "mAtionBarReceiver action=" + intent.getAction());
            if (intent.getAction().equals(ACTION_HOME_PRESS)) {
                /*if (mActivityState == STATE_SETTINS_SINGLE_FRAGMENT
                        || mActivityState == STATE_REVIEW) {
                    if (mCameraId == CameraInfo.CAMERA_FACING_FRONT) {
                        Log.d(TAG, "ACTION_HOME_PRESS mPreState=" + mPreState);
                        FragmentManager fm = getFragmentManager();
                        int num = fm.getBackStackEntryCount();
                        Log.d(TAG, "num =" + num);
                        for (int i = 0; i < num; i++) {
                            fm.popBackStack();
                        }
                        // fm.popBackStackImmediate(PreviewFragment.class.getName(),
                        // FragmentManager.POP_BACK_STACK_INCLUSIVE);
                        mActivityState = mPreState;
                        resumeState();
                    }
                }*/
            } else if (intent.getAction().equals(RecordService.ACTION_STOP_APP)) {
                Intent it = new Intent(RecorderActivity.this, RecordService.class);
                stopService(it);
                finish();
            }else if(intent.getAction().equals(ACTION_FINISH)) {
            	RecorderActivity.this.finish();
            }
        }
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate this = " + this);
		overridePendingTransition(Animation.INFINITE, Animation.INFINITE);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        /*View decorView = getWindow().getDecorView();  
	    // Hide both the navigation bar and the status bar.  
	    // SYSTEM_UI_FLAG_FULLSCREEN is only available on Android 4.1 and higher, but as  
	    // a general rule, you should design your app to hide the status bar whenever you  
	    // hide the navigation bar.  
	    int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY ;
	    decorView.setSystemUiVisibility(uiOptions);*/
        setContentView(R.layout.recorder_main);
        
        mContainer = (FrameLayout) findViewById(R.id.view_content);
        if (getIntent().getExtras() != null) {
            boolean isPowerUp = getIntent().getExtras().getBoolean(
                    MyBroadcastReceiver.POWER_ON_START, false);
            int cameraId = getIntent().getExtras().getInt(EXTRA_CAM_TYPE, -1);
            int preState = getIntent().getExtras().getInt(SAVE_STATE, STATE_FRONT_PREVIEW);
            Log.d(TAG, "isPowerUp =" + isPowerUp + ";cameraId" + cameraId);
            Log.d(TAG, "preState =" + preState);
            if (isPowerUp && cameraId == CameraInfo.CAMERA_FACING_BACK) {
                mActivityState = STATE_BACK_RECORD;
            } else if (isPowerUp && cameraId == CameraInfo.CAMERA_FACING_FRONT) {
                mActivityState = STATE_FRONT_RECORD;
            } else if (cameraId == CameraInfo.CAMERA_FACING_BACK) {
                mActivityState = STATE_BACK_PREVIEW;
            } else if (cameraId == CameraInfo.CAMERA_FACING_FRONT) {
                mActivityState = STATE_FRONT_PREVIEW;
            } else if (isPowerUp && cameraId == CAMERA_THIRD) {
                mActivityState = STATE_THIRD_RECORD;
            } else if (cameraId == CAMERA_THIRD) {
                mActivityState = STATE_THIRD_PREVIEW;
            } else {
                mActivityState = preState;
            }
            mCameraId = cameraId;
        }
		Log.i("dddeng", "onCreate()");
        //if (SplitUtil.getStackBoxId(this) < -1) {
        Log.d(TAG, "MyPreference.isSplitMode() =" + MyPreference.isSplitMode());
        if (!MyPreference.isSplitMode()) {
            //Log.d(TAG, "getStackBoxId() =" + SplitUtil.getStackBoxId(this));
            //mActivityState = STATE_DOUBLE_PREVIEW;
        }
        Intent intent = getIntent();
        if ((intent != null) && !((intent.getFlags() 
        		& RecordService.FLAG_ACTIVITY_RUN_IN_RIGHT_WINDOW) > 0)
        		&& !intent.getBooleanExtra(RecordService.EXTRA_NO_ADAS_FLAG, false)) {
        	mIsFromLauncher = true;
        }
        Log.d(TAG, "onCreate mIsFromLauncher= " + mIsFromLauncher);
        mListeners.clear();
        startRecordService();
        bindRecordService();
        mInitState = mActivityState;
        if (isvalidate) {
			startCounterTime();
		}
    }
    
    @Override
    protected void onResume() {
        super.onResume();
		overridePendingTransition(Animation.INFINITE, Animation.INFINITE);
        mIsPaused = false;
        loadViewByState(mActivityState);
        IntentFilter intent = new IntentFilter(ACTION_HOME_PRESS);
        intent.addAction(RecordService.ACTION_STOP_APP);
        intent.addAction(Intent.ACTION_SCREEN_OFF);
        intent.addAction(ACTION_FINISH);
        registerReceiver(mReceiver, intent);
        if (mHandler != null) {
            mHandler.removeMessages(MSG_DELAY_TO_FINISH);
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
		overridePendingTransition(Animation.INFINITE, Animation.INFINITE);
        mIsPaused = true;
        Log.d(TAG, "onPause mCameraId=" + mCameraId);
        if ((mCameraId == CameraInfo.CAMERA_FACING_BACK
                || mCameraId == RecorderActivity.CAMERA_THIRD) && mRecordService != null
                && (mRecordService.getNeedFloat(CameraInfo.CAMERA_FACING_BACK) < 0
                || mRecordService.getNeedFloat(RecorderActivity.CAMERA_THIRD) < 0)) {
            Log.d(TAG, "onPause going to float");
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                int stackId = SplitUtil.getStackBoxId(this);
                Log.d(TAG, "onPause stackId =" + stackId);
                if ((stackId > 0 && (SplitUtil.getStackPostition(this, stackId)
                        == ACTIVITY_ON_LEFT))) {
                    /*if (!mRecordService.isCameraAdd(CameraInfo.CAMERA_FACING_BACK)) {
                        mRecordService.addCamera(CameraInfo.CAMERA_FACING_BACK);
                    }
                    mRecordService.setFloatCameraid(CameraInfo.CAMERA_FACING_BACK);
                    mRecordService.startFloat(CameraInfo.CAMERA_FACING_BACK,
                            mRecordService.getLastFloatId());*/
                    mRecordService.setStartCameraid(-1);
                    mRecordService.resetFloatId();
                    mRecordService.startFloat();
                    mRecordService.setIsChangingFloat(false, 300);
                    finish();
                }
            }
        } else if (mCameraId == CameraInfo.CAMERA_FACING_FRONT && mRecordService != null) {
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                int stackId = SplitUtil.getStackBoxId(this);
                Log.d(TAG, "onPause stackId =" + stackId);
                if ((stackId > 0 && (SplitUtil.getStackPostition(this, stackId)
                        == ACTIVITY_ON_LEFT))) {
                    if (mHandler != null) {
                        mHandler.removeMessages(MSG_DELAY_TO_FINISH);
                        mHandler.sendEmptyMessageDelayed(MSG_DELAY_TO_FINISH, 800);
                    }
                }
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != mRecordService) {
//        	mRecordService.stopRender();
		}
        unbindRecordService();
        mListeners.clear();
        mRecordService = null;
        unregisterReceiver(mReceiver);
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }


        int stackId = SplitUtil.getStackBoxId(RecorderActivity.this);
    	int getStackPostition = SplitUtil.getStackPostition(this, stackId);
        Log.d("", "zdt --- stackId: " + stackId + ", getStackPostition: " + getStackPostition);
    	if (stackId > 0 && getStackPostition == ACTIVITY_ON_LEFT) {
    		if (RecorderActivity.CAMERA_COUNT == 2 && mRecordService != null) {
        		mRecordService.hideFloatWindows();
        		mRecordService.updateFloatWindow(DoubleFloatWindow.ON_RIGHT);
        	}

    		Log.d("", "zdt --- start activity on right");
        	Intent i = new Intent(RecorderActivity.this, RecorderActivity.class);
        	i.addFlags(0x00000200);
        	startActivity(i);

        }
    }
    
    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        //super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt(SAVE_STATE, mActivityState);
        savedInstanceState.putInt(SAVE_CAMERA_ID, mCameraId);
        return;
    }
    
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mActivityState = savedInstanceState.getInt(SAVE_STATE, STATE_FRONT_PREVIEW);
        mActivityState = savedInstanceState.getInt(SAVE_CAMERA_ID, mCameraId);
        return;
    }
    
    public void setCurState(int state) {
        mActivityState = state;
    }
    
    public void resumeState() {
        mActivityState = mInitState;
    }
    public void loadViewByState(int state) {
        mActivityState = state;
        Log.e(TAG, "mActivityState =" + mActivityState);
        mFlag = 0;
        if (mIsFromLauncher) {
        	mFlag = mFlag | PREVIEW_START_FROM_LAUNCHER;
        }
        FragmentManager fm = getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        switch (state) {
            case STATE_FRONT_RECORD:
                mFlag = mFlag | PREVIEW_START_RECORDING;
                mPreState = STATE_FRONT_RECORD;
                mContainer.setVisibility(View.VISIBLE);
                mCurFragment = fm.findFragmentByTag(FRONT_PREVIEW_FRAGMENT);
                if (mCurFragment == null) {
                    mCurFragment = new PreviewFragment();
                }
                mFlag = mFlag | PREVIEW_START_FRONT;
                ft.replace(R.id.view_content, mCurFragment, FRONT_PREVIEW_FRAGMENT);
                break;
            case STATE_DEFAULT:
                mPreState = STATE_FRONT_PREVIEW;
                mContainer.setVisibility(View.VISIBLE);
                mCurFragment = fm.findFragmentByTag(FRONT_PREVIEW_FRAGMENT);
                if (mCurFragment == null) {
                    mCurFragment = new PreviewFragment();
                }
                mFlag = mFlag | PREVIEW_START_FRONT;
                ft.replace(R.id.view_content, mCurFragment, FRONT_PREVIEW_FRAGMENT);
                break;
            case STATE_FRONT_PREVIEW:
                mPreState = STATE_FRONT_PREVIEW;
                mContainer.setVisibility(View.VISIBLE);
                mCurFragment = fm.findFragmentByTag(FRONT_PREVIEW_FRAGMENT);
                if (mCurFragment == null) {
                    mCurFragment = new PreviewFragment();
                }
                mFlag = mFlag | PREVIEW_START_FRONT;
                ft.replace(R.id.view_content, mCurFragment, FRONT_PREVIEW_FRAGMENT);
                break;
            case STATE_BACK_RECORD:
                mFlag = mFlag | PREVIEW_START_RECORDING;
                mContainer.setVisibility(View.VISIBLE);
                mCurFragment = fm.findFragmentByTag(BACK_PREVIEW_FRAGMENT);
                if (mCurFragment == null) {
                    mCurFragment = new PreviewFragment();
                }
                mFlag = mFlag | PREVIEW_START_BACK;
                ft.replace(R.id.view_content, mCurFragment, BACK_PREVIEW_FRAGMENT);
                break;
            case STATE_BACK_PREVIEW:
                mContainer.setVisibility(View.VISIBLE);
                mCurFragment = fm.findFragmentByTag(BACK_PREVIEW_FRAGMENT);
                if (mCurFragment == null) {
                    mCurFragment = new PreviewFragment();
                }
                mFlag = mFlag | PREVIEW_START_BACK;
                ft.replace(R.id.view_content, mCurFragment, BACK_PREVIEW_FRAGMENT);
                break;
            case STATE_THIRD_RECORD:
                mFlag = mFlag | PREVIEW_START_RECORDING;
                mContainer.setVisibility(View.VISIBLE);
                mCurFragment = fm.findFragmentByTag(THIRD_PREVIEW_FRAGMENT);
                if (mCurFragment == null) {
                    mCurFragment = new PreviewFragment();
                }
                mFlag = mFlag | PREVIEW_START_THIRD;
                ft.replace(R.id.view_content, mCurFragment, THIRD_PREVIEW_FRAGMENT);
                break;
            case STATE_THIRD_PREVIEW:
                mContainer.setVisibility(View.VISIBLE);
                mCurFragment = fm.findFragmentByTag(THIRD_PREVIEW_FRAGMENT);
                if (mCurFragment == null) {
                    mCurFragment = new PreviewFragment();
                }
                mFlag = mFlag | PREVIEW_START_THIRD;
                ft.replace(R.id.view_content, mCurFragment, THIRD_PREVIEW_FRAGMENT);
                break;
            case STATE_REVIEW:
                mContainer.setVisibility(View.VISIBLE);
                mCurFragment = fm.findFragmentByTag(REVIEW_FRAGMENT);
                if (mCurFragment == null) {
                    mCurFragment = new ReviewFragment();
                }
                ft.replace(R.id.view_content, mCurFragment, REVIEW_FRAGMENT);
                break;
            case STATE_SETTINS_SINGLE_FRAGMENT:
			Log.i("tang","STATE_SETTINS_SINGLE_FRAGMENT");
                mContainer.setVisibility(View.VISIBLE);
                mCurFragment = fm.findFragmentByTag(SETTINGS_FRAGMENT);
                if (mCurFragment == null) {
                    mCurFragment = new SettingsFragment();
                }
                ft.replace(R.id.view_content, mCurFragment, SETTINGS_FRAGMENT);
                break;
            case STATE_DOUBLE_PREVIEW:
            	mFlag = mFlag | PREVIEW_START_RECORDING;
                mContainer.setVisibility(View.VISIBLE);
                mCurFragment = fm.findFragmentByTag(DOUBLE_PREVIEW_FRAGMENT);
                if (mCurFragment == null) {
                    mCurFragment = new DoublePreviewFragment();
                }
                ft.replace(R.id.view_content, mCurFragment, DOUBLE_PREVIEW_FRAGMENT);
                break;
            default:
                break;
        }
        if (mIsFirtUp) {
            mIsFirtUp = false;
        } else {
            ft.addToBackStack(null);
        }
        ft.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_in_right);
        ft.commit();
    }
    
    public int getCurrentState() {
        return mActivityState;
    }
    
    public void switchCameraByState(int state) {
        Log.d(TAG, "switchCameraByState state=" + state);
        mIsFirtUp = true;
        mIsFromLauncher = false;
        mInitState = mActivityState = state;
        FragmentManager fm = getFragmentManager();
        int num = fm.getBackStackEntryCount();
        for (int i = 0; i < num; i++) {
            try {
                fm.popBackStack();
            } catch(IllegalStateException ex) {
                ex.printStackTrace();
            }
        }
        loadViewByState(mActivityState);
    }

    private void startRecordService() {
        Intent intent = new Intent(this, RecordService.class);
        startService(intent);
    }
    
    public void bindRecordService() {
        // mWakeLock.acquire();
        Log.i(TAG, "#####bindRecordService");
        Intent intent = new Intent(this, RecordService.class);
        bindService(intent, mRecConnection, Context.BIND_AUTO_CREATE);
    }
    
    private void unbindRecordService() {
        Log.i(TAG, "#####unbindRecordService");
        if (mRecordService != null) {
            mRecordService.removeServiceListener(this);
        }
        if (mRecConnection != null) {
            unbindService(mRecConnection);
            mRecConnection = null;
        }
        // mWakeLock.release();
    }
    
    public RecordService getRecordService() {
        return mRecordService;
    }
    
    public interface IServiceBindedListener {
        void onServiceBinded(RecordService service);
    }
    
    public void addServiceBindedListener(IServiceBindedListener ls) {
        mListeners.add(ls);
    }
    
    public int getFlag() {
        return mFlag;
    }
    
    protected void onActivityMove(boolean isToLeft) {
    	Log.d(TAG, "onActivityMove isToLeft=" + isToLeft);
    	if (isToLeft) {
            if (mCurFragment instanceof PreviewFragment) {
                PreviewFragment pf = ((PreviewFragment) mCurFragment);
                pf.onToLeft();
                return;
            }
            FragmentManager fm = getFragmentManager();
            if (fm.getBackStackEntryCount() == 0 && mCameraId == CameraInfo.CAMERA_FACING_FRONT) {
                mCurFragment = fm.findFragmentByTag(FRONT_PREVIEW_FRAGMENT);
                if (mCurFragment != null && mCurFragment instanceof PreviewFragment) {
                    PreviewFragment pf = ((PreviewFragment) mCurFragment);
                    pf.onToLeft();
                }
            }

    	}
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyDown keyCode=" + keyCode + ";" + KeyEvent.KEYCODE_FILE_LOCK);
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            FragmentManager fm1 = getFragmentManager();
            int num1 = fm1.getBackStackEntryCount();
            Log.d(TAG, "keyEvent.KEYCODE_BACK, getBackStackEntryCount= " + num1);
            int stackId0 = SplitUtil.getStackBoxId(RecorderActivity.this);
        	int getStackPostition = SplitUtil.getStackPostition(this, stackId0);
        	if (getStackPostition == ACTIVITY_ON_RIGHT) {
        		Log.d(TAG, "keyEvent.KEYCODE_BACK, ACTIVITY_ON_LEFT return");
        		//return super.onKeyDown(keyCode, event);
			}
            if (num1 == 0 || CustomValue.FULL_WINDOW) {
            	Intent restoreIntent = new Intent();
            	restoreIntent.setAction(TwoCameraPreviewWin.BACK_WINDOW_ACTION);
            	this.sendBroadcast(restoreIntent);


            	finish();
            	if (RecorderActivity.CAMERA_COUNT == 2 && getStackPostition == ACTIVITY_ON_LEFT) {
            		if (mRecordService != null) {
            			Log.d(TAG, "zdt --- onKeyDown hide, mRecordService != null, StackPostition: " + getStackPostition);
            			mRecordService.hideFloatWindows();
            			mRecordService.updateFloatWindow(DoubleFloatWindow.ON_RIGHT);
            		}

            	}

            	return true;
			} else {
				if (RecorderActivity.CAMERA_COUNT == 2 && getStackPostition == ACTIVITY_ON_LEFT) {
					if (mRecordService != null) {
						Log.d(TAG, "zdt --- onKeyDown show, mRecordService != null, StackPostition: " + getStackPostition);
						mRecordService.updateFloatWindow(DoubleFloatWindow.ON_LEFT);
	            		mRecordService.showFloatWindows();
	            	}
            	}
			}


            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
            	int stackId = SplitUtil.getStackBoxId(RecorderActivity.this);
                Log.d(TAG, "stackId =" + stackId);
                if ((stackId > 0 && (SplitUtil.getStackPostition(this, stackId)
                		== ACTIVITY_ON_LEFT)) && (mRecordService != null)
                        && (mRecordService.getNeedFloat(CameraInfo.CAMERA_FACING_BACK) < 0
                        || mRecordService.getNeedFloat(RecorderActivity.CAMERA_THIRD) < 0)) {
                    Log.d(TAG, "onleft floating");
                    if (RecorderActivity.CAMERA_COUNT == 2) {
                    	if (!mRecordService.isCameraAdd(CameraInfo.CAMERA_FACING_BACK)) {
                            mRecordService.addCamera(CameraInfo.CAMERA_FACING_BACK);
                        }
                        mRecordService.setFloatCameraid(CameraInfo.CAMERA_FACING_BACK);
                        mRecordService.startFloat(CameraInfo.CAMERA_FACING_BACK,
                                mRecordService.getLastFloatId());
                    }
                    mRecordService.setStartCameraid(-1);
                    mRecordService.resetFloatId();
                    mRecordService.startFloat();
                    mRecordService.setIsChangingFloat(false, 300);
                } else if ((stackId > 0 && (SplitUtil.getStackPostition(this, stackId)
                        == ACTIVITY_ON_LEFT)) && (mRecordService != null)
                        && (mCameraId == CameraInfo.CAMERA_FACING_FRONT)
                        && STATE_SETTINS_SINGLE_FRAGMENT != mActivityState
                        && STATE_REVIEW != mActivityState) {
                    Log.d(TAG, "exit front cam");
                    FragmentManager fm = getFragmentManager();
                    int num = fm.getBackStackEntryCount();
                    Log.d(TAG, "num =" + num);
                    for (int i = 0; i < num; i++) {
                        try {
                            fm.popBackStack();
                        } catch(IllegalStateException ex) {
                            ex.printStackTrace();
                        }
                    }
                    finish();
                }
            }
            
        } else if (keyCode == KeyEvent.KEYCODE_FILE_LOCK) {
            if (mCurFragment instanceof PreviewFragment) {
                PreviewFragment pf = ((PreviewFragment) mCurFragment);
                pf.onLockKeyDown();
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_CAMERA) {
            if (mCurFragment instanceof PreviewFragment) {
                PreviewFragment pf = ((PreviewFragment) mCurFragment);
                pf.onSnapShot();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    
    @Override
    public void onTimeUpdate(long curTime) {
        // TODO Auto-generated method stub
        //nothing to do
    }

    @Override
    public void onHomePressed() {
        // TODO Auto-generated method stub
        Log.d(TAG, "onHomePressed mCameraId=" + mCameraId);
        //add by lym start
//        if (CustomValue.CHANGE_FRONT_BACK_CAMERA){
//            mRecordService.setToRight();
//        }
        //end
        if (mActivityState == STATE_SETTINS_SINGLE_FRAGMENT
                || mActivityState == STATE_REVIEW) {
            if (mCameraId == CameraInfo.CAMERA_FACING_FRONT) {
                Log.d(TAG, "ACTION_HOME_PRESS mPreState=" + mPreState);
                FragmentManager fm = getFragmentManager();
                int num = fm.getBackStackEntryCount();
                Log.d(TAG, "num =" + num);
                for (int i = 0; i < num; i++) {
                    try {
                        fm.popBackStack();
                    } catch(IllegalStateException ex) {
                        ex.printStackTrace();
                    }
                }
                // fm.popBackStackImmediate(PreviewFragment.class.getName(),
                // FragmentManager.POP_BACK_STACK_INCLUSIVE);
                mActivityState = mPreState;
                resumeState();
            }
        }
//        mRecordService.getFloatWindow().setHomePressed();
        int stackId = SplitUtil.getStackBoxId(RecorderActivity.this);
        Log.d(TAG, "stackId =" + stackId);
        if (RecorderActivity.CAMERA_COUNT == 2 && stackId > 0 && (SplitUtil.getStackPostition(this, stackId) == ACTIVITY_ON_LEFT)){
        	if (mRecordService != null) {
    			Log.d(TAG, "zdt --- onHomePressed hide, mRecordService != null");
    			mRecordService.hideFloatWindows();
    			mRecordService.updateFloatWindow(DoubleFloatWindow.ON_RIGHT);
    		}
        }

    }

    @Override
	public int onAskLeftCameraId() {
		// TODO Auto-generated method stub
		return -1;
	}
	@Override
	protected void onNewIntent(Intent intent) {
		// TODO Auto-generated method stub
		super.onNewIntent(intent);
		if(intent != null) {
			  int cameraId = intent.getIntExtra(EXTRA_CAM_TYPE, -1);
			  Log.i(TAG, "onNewIntent() "+   "cameraId = "+ cameraId );
			  if(cameraId == 2) {
				  switchCameraByState(10);
			  }else if(cameraId == 0) {
				  switchCameraByState(1);
			  }
		}
	 }

	private void startPackageByName(String pkgName) {
		PackageManager pm = getPackageManager();
		Intent i = pm.getLaunchIntentForPackage(pkgName);
		startActivity(i);
	}
}
