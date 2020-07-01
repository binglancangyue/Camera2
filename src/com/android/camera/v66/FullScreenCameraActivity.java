package com.android.camera.v66;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.SurfaceTexture;
import android.hardware.Camera.CameraInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter.ViewBinder;

import java.text.DecimalFormat;

import com.android.camera.home_recorder.HomeRecorderActivity;
import com.android.camera.v66.ObserveScrollView.ScrollListener;
import com.android.camera.v66.RecordService.BackInsertImpl;
import com.android.camera.v66.RecordService.RightInsertImpl;
import com.android.camera2.R;

public class FullScreenCameraActivity extends Activity implements View.OnClickListener, SurfaceHolder.Callback {

	private static final String TAG = "FullScreenCameraActivity";
	public static final String KEY_STATUS = "camera_status";
	public static final String KEY_REVERSE = "isReverseing";
	public static final String KEY_VOICE_CONTROL = "isVoiceControl";
	public static final int STATE_FULLSCREEN_SHOW_CAMERA_FRONT = 1;
	public static final int STATE_FULLSCREEN_SHOW_CAMERA_BACK = 2;
	//public static final int STATE_FULLSCREEN_SHOW_CAMERA_LEFT = 3;
	public static final int STATE_FULLSCREEN_SHOW_CAMERA_RIGHT = 4;
	//public static final int STATE_FULLSCREEN_SHOW_CAMERA_LEFT_BACK = 5;
	public static final int STATE_FULLSCREEN_SHOW_CAMERA_RIGHT_BACK = 6;
	public static final int STATE_FULLSCREEN_SHOW_CAMERA_LEFT_BACK_RIGHT = 7;
	public static final int STATE_FULLSCREEN_SHOW_CAMERA_LEFT_RIGHT = 8;
	public static final String ACTION_SPEAK_TEXT = "com.action.other_Text";
	private static final String AUTONAVI_ICON_ACTION = "AUTONAVI_STANDARD_BROADCAST_SEND";
	private static final String ACTION_FULL_CAMERA_SHOW = "com.zqc.action.show_full_camera";
	private static final String ACTION_FULL_CAMERA_HIDE = "com.zqc.action.hide_full_camera";
	private static final int FULLSCREEN_WIDTH = 1024;
//	private static final int FULLSCREEN_HEIGHT = 400;
	private static final int FULLSCREEN_HEIGHT = 600;
	//public static final int CAMERA_ID_LEFT = 2;
	//public static final int CAMERA_ID_RIGHT = 3;
	public static final int CAMERA_ID_RIGHT = 2;
	private boolean mTextureLeftIsUp = false;
	private boolean mTextureFrontIsUp = false;
	private boolean mTextureBackIsUp = false;
	private boolean mTextureRightIsUp = false;
	private boolean isClickRight = false;
	private boolean isCurrentActClick = false;
	private int currentStatus = 0;
	private int type = -1;
	private int iconState = -1;
	private int remiandDistance = -1;
	private String currRoadName = "";
	private String nextRoadName = "";
	private SharedPreferences scrollPref;
	private int yValue;
	private boolean isMove = false;

	private DecimalFormat roadDF = new DecimalFormat("####.0");
	private SharedPreferences sharedPreferences,mCamInsertSp;;
	private LinearLayout previewFramelayout;
	private FrameLayout layoutNaviInfo;
	private RelativeLayout layoutNavi;
	private SurfaceView /*leftPreview,*/ frontPreview,backPreview, rightPreview;
	private SurfaceHolder frontPreviewHolder,backPreviewHolder, rightPreviewHolder;
	private View /*leftLine,*/ rightLine;
	private TextView tvDistance,tvDistanceUnit,tvSince,tvCurrentRoad,tvFinalRoad;
	private LEDView ledView;
	private ImageView iv_icon;
	private ImageView ivReverseLine;
	private ImageView ivRecord;
	private TextView tvFlag,tvTipInsertRight,tvTipInsertBack;
	private Button btExit;
//	private ObserveScrollView scrollView;
	private Handler mHandler = new Handler();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "onCreate()");
		sendBroadcast(new Intent(ACTION_FULL_CAMERA_SHOW));
		requestWindowFeature(Window.FEATURE_NO_TITLE); 
	//	View decorView = getWindow().getDecorView();    //设置点击左侧录像框进去时全屏
    //    int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
	//		| View.SYSTEM_UI_FLAG_IMMERSIVE | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
	//	decorView.setSystemUiVisibility(uiOptions);
	     hideNavigationBar();

		setContentView(R.layout.activity_fullscreen_camera);
		sharedPreferences = getSharedPreferences("isVideo", Context.MODE_PRIVATE);
		mCamInsertSp = getSharedPreferences("cameraPlug", Context.MODE_MULTI_PROCESS | Context.MODE_WORLD_READABLE);
		int status = getIntent().getIntExtra(KEY_STATUS, 0);
		isClickRight = getIntent().getBooleanExtra(HomeRecorderActivity.ISCLICKRIGHT, false);
		currentClickState = isClickRight;
		boolean isVoiceControl = getIntent().getBooleanExtra(KEY_VOICE_CONTROL, false);
		boolean isReverse = getIntent().getBooleanExtra(KEY_REVERSE, false);
		bindService(new Intent(this, RecordService.class), mRecConnection, Context.BIND_AUTO_CREATE);
		initView();
		initData(status,isVoiceControl,isReverse);
		registerBroadcast();

	}

	private void initView() {
		Log.i(TAG, "initView()");
		scrollPref = this.getSharedPreferences("scollView", Context.MODE_PRIVATE);
		previewFramelayout = (LinearLayout) findViewById(R.id.preview_framelayout);
		//leftPreview = (TextureView) findViewById(R.id.left_preview);
		//leftLine = findViewById(R.id.left_line);
//		scrollView = (ObserveScrollView) findViewById(R.id.scrollview);
		frontPreview = (SurfaceView) findViewById(R.id.front_preview);
		backPreview = (SurfaceView) findViewById(R.id.back_preview);
		layoutNaviInfo = (FrameLayout) findViewById(R.id.layout_naviinfo);
		layoutNavi = (RelativeLayout) findViewById(R.id.layout_navi);
		tvDistance = (TextView) findViewById(R.id.tv_distance);
		tvDistanceUnit = (TextView) findViewById(R.id.tv_distance_unit);
		iv_icon = (ImageView) findViewById(R.id.iv_icon);
		tvSince = (TextView) findViewById(R.id.tv_since);
		tvCurrentRoad = (TextView) findViewById(R.id.tv_current_road);
		tvFinalRoad = (TextView) findViewById(R.id.tv_final_road);
		ledView = (LEDView) findViewById(R.id.ledview);
		rightLine = findViewById(R.id.right_line);
		rightPreview = (SurfaceView) findViewById(R.id.right_preview);
		ivReverseLine = (ImageView) findViewById(R.id.reverse_lines);
		ivRecord = (ImageView) findViewById(R.id.iv_record);
		tvFlag = (TextView) findViewById(R.id.tv_flag);
		btExit = (Button) findViewById(R.id.bt_exit);
		tvTipInsertRight = (TextView) findViewById(R.id.tv_insert_right);
		tvTipInsertBack = (TextView) findViewById(R.id.tv_insert_back);
		//leftPreview.setOnClickListener(this);
		frontPreview.setOnClickListener(this);
		backPreview.setOnClickListener(this);
		rightPreview.setOnClickListener(this);
		btExit.setOnClickListener(this);
		//leftPreview.setSurfaceTextureListener(this);
		frontPreviewHolder = frontPreview.getHolder();
		backPreviewHolder = backPreview.getHolder();
		rightPreviewHolder = rightPreview.getHolder();
		frontPreviewHolder.addCallback(this);
		backPreviewHolder.addCallback(this);
		rightPreviewHolder.addCallback(this);
		
		/*scrollView.setScrollListener(new ScrollListener() {

			@Override
			public void scrollOritention(int l, int t, int oldl, int oldt) {
				// TODO Auto-generated method stub
				if(scrollPref != null) {
					scrollPref.edit().putFloat("yValue", t).commit();
					scrollPref.edit().putBoolean("isMove", true).commit();
				}
			}
		});*/
	}

	private void initData(int status,boolean isVoiceControl,boolean isReverse) {
		Log.i(TAG, "initData() status = " + status+"，isVoiceControl = "+isVoiceControl+",isReverse = "+isReverse);
		if (isVoiceControl) {
			voiceOpenFeedBack(status);
		}
		if(true/*mCamInsertSp.getBoolean("isRightCamIn", false)*/) {
			tvTipInsertRight.setVisibility(View.GONE);
		}else {
			tvTipInsertRight.setVisibility(View.VISIBLE);
		}
		if(true/*mCamInsertSp.getBoolean("isBackCamIn", false)*/) {
			tvTipInsertBack.setVisibility(View.GONE);
		}else {
			tvTipInsertBack.setVisibility(View.VISIBLE);
		}
//		currentStatus = status;
		if (status == STATE_FULLSCREEN_SHOW_CAMERA_FRONT) {
			currentStatus = status;
			//leftPreview.setVisibility(View.GONE);
			backPreview.setVisibility(View.GONE);
			rightPreview.setVisibility(View.GONE);
			layoutNaviInfo.setVisibility(View.GONE);
			//leftLine.setVisibility(View.GONE);
			rightLine.setVisibility(View.GONE);
			ivReverseLine.setVisibility(View.GONE);
			btExit.setVisibility(View.VISIBLE);
			tvFlag.setVisibility(View.VISIBLE);
			tvFlag.setText(R.string.front_camera_text);
			LinearLayout.LayoutParams frontParm = (LayoutParams) frontPreview.getLayoutParams();
			frontParm.width = FULLSCREEN_WIDTH;
			frontParm.height = FULLSCREEN_HEIGHT;
			frontPreview.setLayoutParams(frontParm);
			frontPreview.setVisibility(View.VISIBLE);
			frontPreview.setClickable(true);
			startCameraRender(CameraInfo.CAMERA_FACING_FRONT);
//			stopCameraRender(CameraInfo.CAMERA_FACING_BACK);
			//stopCameraRender(CAMERA_ID_LEFT); 
//			stopCameraRender(CAMERA_ID_RIGHT);
		} else if (status == STATE_FULLSCREEN_SHOW_CAMERA_BACK) {
			//leftPreview.setVisibility(View.GONE);
			frontPreview.setVisibility(View.GONE);
			rightPreview.setVisibility(View.GONE);
			layoutNaviInfo.setVisibility(View.GONE);
			ivReverseLine.setVisibility(View.GONE);
			//leftLine.setVisibility(View.GONE);
			rightLine.setVisibility(View.GONE);
			btExit.setVisibility(View.VISIBLE);
			tvFlag.setVisibility(View.VISIBLE);
			tvFlag.setText(R.string.back_camera_text);
			RelativeLayout.LayoutParams backParm = (android.widget.RelativeLayout.LayoutParams) backPreview.getLayoutParams();
			backParm.width = FULLSCREEN_WIDTH;
			backParm.height = FULLSCREEN_HEIGHT;
			backPreview.setLayoutParams(backParm);
			backPreview.setVisibility(View.VISIBLE);
			
			if (isReverse) {
				mHandler.postDelayed(new Runnable() {
					
					@Override
					public void run() {
						ivReverseLine.setVisibility(View.VISIBLE);
					}
				}, 500);
				btExit.setVisibility(View.GONE);
				tvFlag.setVisibility(View.GONE);
				backPreview.setClickable(false);
			}else{
				ivReverseLine.setVisibility(View.GONE);
				btExit.setVisibility(View.VISIBLE);
				tvFlag.setVisibility(View.VISIBLE);
				backPreview.setClickable(true);
				currentStatus = status;
			}
			startCameraRender(CameraInfo.CAMERA_FACING_BACK);
//			stopCameraRender(CameraInfo.CAMERA_FACING_FRONT);
			//stopCameraRender(CAMERA_ID_LEFT); 
//			stopCameraRender(CAMERA_ID_RIGHT);
		} /*else if (status == STATE_FULLSCREEN_SHOW_CAMERA_LEFT) {
			frontPreview.setVisibility(View.GONE);
			backPreview.setVisibility(View.GONE);
			rightPreview.setVisibility(View.GONE);
			layoutNaviInfo.setVisibility(View.GONE);
			ivReverseLine.setVisibility(View.GONE);
			leftLine.setVisibility(View.GONE);
			rightLine.setVisibility(View.GONE);
			btExit.setVisibility(View.VISIBLE);
			tvFlag.setVisibility(View.VISIBLE);
			tvFlag.setText(R.string.left_camera_text);
			LinearLayout.LayoutParams leftParm = (LayoutParams) leftPreview.getLayoutParams();
			leftParm.width = FULLSCREEN_WIDTH;
			leftParm.height = FULLSCREEN_HEIGHT;
			leftPreview.setLayoutParams(leftParm);
			leftPreview.setVisibility(View.VISIBLE);
			leftPreview.setClickable(true);
			startCameraRender(CAMERA_ID_LEFT);
			stopCameraRender(CameraInfo.CAMERA_FACING_FRONT);
			stopCameraRender(CameraInfo.CAMERA_FACING_BACK);
			stopCameraRender(CAMERA_ID_RIGHT);
		}*/ 
		else if (status == STATE_FULLSCREEN_SHOW_CAMERA_RIGHT_BACK || status == STATE_FULLSCREEN_SHOW_CAMERA_RIGHT ) {
			if(isClickRight) {
				frontPreview.setVisibility(View.GONE);
				backPreview.setVisibility(View.GONE);
				//leftPreview.setVisibility(View.GONE);
				layoutNaviInfo.setVisibility(View.GONE);
				ivReverseLine.setVisibility(View.GONE);
				tvTipInsertBack.setVisibility(View.GONE);
				//leftLine.setVisibility(View.GONE);
				rightLine.setVisibility(View.GONE);
				if(isReverse) {
					btExit.setVisibility(View.GONE);
					tvFlag.setVisibility(View.GONE);
				}else {	
					currentStatus = status;
					btExit.setVisibility(View.VISIBLE);
					tvFlag.setVisibility(View.VISIBLE);
				}
				Log.i(TAG, "right : currentStatus = "+currentStatus+"    isReverse = "+isReverse);
				tvFlag.setText(R.string.right_camera_text);
				RelativeLayout.LayoutParams rightParm = (android.widget.RelativeLayout.LayoutParams) rightPreview.getLayoutParams();
				rightParm.width = FULLSCREEN_WIDTH;
				rightParm.height = FULLSCREEN_HEIGHT;
				rightPreview.setLayoutParams(rightParm);
				rightPreview.setVisibility(View.VISIBLE);
				rightPreview.setClickable(true);
				startCameraRender(CAMERA_ID_RIGHT);
//				stopCameraRender(CameraInfo.CAMERA_FACING_FRONT);
//				stopCameraRender(CameraInfo.CAMERA_FACING_BACK);
				//stopCameraRender(CAMERA_ID_LEFT);
			}else {
				frontPreview.setVisibility(View.GONE);
				//leftPreview.setVisibility(View.GONE);
				layoutNaviInfo.setVisibility(View.GONE);
				ivReverseLine.setVisibility(View.GONE);
				//leftLine.setVisibility(View.GONE);
				if(isReverse) {
					btExit.setVisibility(View.GONE);
				}else {
					currentStatus = status;
					btExit.setVisibility(View.VISIBLE);
				}
				tvFlag.setVisibility(View.GONE);
				RelativeLayout.LayoutParams backParm = (android.widget.RelativeLayout.LayoutParams) backPreview.getLayoutParams();
				backParm.width = 341;
				backParm.height = FULLSCREEN_HEIGHT;
				backPreview.setLayoutParams(backParm);
				backPreview.setVisibility(View.VISIBLE);
				backPreview.setClickable(false);
				RelativeLayout.LayoutParams rightParm = (android.widget.RelativeLayout.LayoutParams) rightPreview.getLayoutParams();
				rightParm.width = 680;
				rightParm.height = FULLSCREEN_HEIGHT;
				rightPreview.setLayoutParams(rightParm);
				rightPreview.setVisibility(View.VISIBLE);
				rightPreview.setClickable(false);
				rightLine.setVisibility(View.VISIBLE);
//				stopCameraRender(CameraInfo.CAMERA_FACING_FRONT);
				//stopCameraRender(CAMERA_ID_LEFT);
				startCameraRender(CameraInfo.CAMERA_FACING_BACK);
				startCameraRender(CAMERA_ID_RIGHT);
			}
		} /*else if (status == STATE_FULLSCREEN_SHOW_CAMERA_LEFT_BACK) {
			frontPreview.setVisibility(View.GONE);
			rightPreview.setVisibility(View.GONE);
			layoutNaviInfo.setVisibility(View.GONE);
			ivReverseLine.setVisibility(View.GONE);
			rightLine.setVisibility(View.GONE);
			btExit.setVisibility(View.VISIBLE);
			tvFlag.setVisibility(View.GONE);
			LinearLayout.LayoutParams leftParm = (LayoutParams) leftPreview.getLayoutParams();
			leftParm.width = 1000;
			leftParm.height = FULLSCREEN_HEIGHT;
			leftPreview.setLayoutParams(leftParm);
			leftPreview.setVisibility(View.VISIBLE);
			leftPreview.setClickable(false);
			LinearLayout.LayoutParams backParm = (LayoutParams) backPreview.getLayoutParams();
			backParm.width = 600;
			backParm.height = FULLSCREEN_HEIGHT;
			backPreview.setLayoutParams(backParm);
			backPreview.setVisibility(View.VISIBLE);
			backPreview.setClickable(false);
			leftLine.setVisibility(View.VISIBLE);
			stopCameraRender(CameraInfo.CAMERA_FACING_FRONT);
			stopCameraRender(CAMERA_ID_RIGHT);
			startCameraRender(CameraInfo.CAMERA_FACING_BACK);
			startCameraRender(CAMERA_ID_LEFT);
		} */
		else if (status == STATE_FULLSCREEN_SHOW_CAMERA_LEFT_BACK_RIGHT) {
			frontPreview.setVisibility(View.GONE);
			//leftPreview.setVisibility(View.GONE);
			layoutNaviInfo.setVisibility(View.GONE);
			ivReverseLine.setVisibility(View.GONE);
			//leftLine.setVisibility(View.GONE);
			if(isReverse) {
				btExit.setVisibility(View.GONE);
			}else {
				currentStatus = status;
				btExit.setVisibility(View.VISIBLE);
			}
			tvFlag.setVisibility(View.GONE);
			RelativeLayout.LayoutParams backParm = (android.widget.RelativeLayout.LayoutParams) backPreview.getLayoutParams();
			backParm.width = 341;
			backParm.height = FULLSCREEN_HEIGHT;
			backPreview.setLayoutParams(backParm);
			backPreview.setVisibility(View.VISIBLE);
			backPreview.setClickable(false);
			RelativeLayout.LayoutParams rightParm = (android.widget.RelativeLayout.LayoutParams) rightPreview.getLayoutParams();
			rightParm.width = 680;
			rightParm.height = FULLSCREEN_HEIGHT;
			rightPreview.setLayoutParams(rightParm);
			rightPreview.setVisibility(View.VISIBLE);
			rightPreview.setClickable(false);
			rightLine.setVisibility(View.VISIBLE);
//			stopCameraRender(CameraInfo.CAMERA_FACING_FRONT);
			//stopCameraRender(CAMERA_ID_LEFT);
			startCameraRender(CameraInfo.CAMERA_FACING_BACK);
			startCameraRender(CAMERA_ID_RIGHT);
		} /*else if (status == STATE_FULLSCREEN_SHOW_CAMERA_LEFT_BACK_RIGHT) {
			frontPreview.setVisibility(View.GONE);
			layoutNaviInfo.setVisibility(View.GONE);
			tvFlag.setVisibility(View.GONE);
			LinearLayout.LayoutParams leftParm = (LayoutParams) leftPreview.getLayoutParams();
			leftParm.width = 430;
			leftParm.height = FULLSCREEN_HEIGHT;
			leftPreview.setLayoutParams(leftParm);
			leftPreview.setVisibility(View.VISIBLE);
			leftPreview.setClickable(false);
			LinearLayout.LayoutParams backParm = (LayoutParams) backPreview.getLayoutParams();
			backParm.width = 740;
			backParm.height = FULLSCREEN_HEIGHT;
			backPreview.setLayoutParams(backParm);
			backPreview.setVisibility(View.VISIBLE);
			backPreview.setClickable(false);
			LinearLayout.LayoutParams rightParm = (LayoutParams) rightPreview.getLayoutParams();
			rightParm.width = 430;
			rightParm.height = FULLSCREEN_HEIGHT;
			rightPreview.setLayoutParams(rightParm);
			rightPreview.setVisibility(View.VISIBLE);
			rightPreview.setClickable(false);
			leftLine.setVisibility(View.VISIBLE);
			rightLine.setVisibility(View.VISIBLE);
			if (isReverse) {
				mHandler.postDelayed(new Runnable() {
					
					@Override
					public void run() {
						ivReverseLine.setVisibility(View.VISIBLE);
					}
				}, 500);
				btExit.setVisibility(View.GONE);
			}else{
				ivReverseLine.setVisibility(View.GONE);
				btExit.setVisibility(View.VISIBLE);
			}
			stopCameraRender(CameraInfo.CAMERA_FACING_FRONT);
			startCameraRender(CameraInfo.CAMERA_FACING_BACK);
			startCameraRender(CAMERA_ID_LEFT);
			startCameraRender(CAMERA_ID_RIGHT);
		}else if(status == STATE_FULLSCREEN_SHOW_CAMERA_LEFT_RIGHT){
			frontPreview.setVisibility(View.GONE);
			backPreview.setVisibility(View.GONE);
			ivReverseLine.setVisibility(View.GONE);
			btExit.setVisibility(View.VISIBLE);
			tvFlag.setVisibility(View.GONE);
			LinearLayout.LayoutParams leftParm = (LayoutParams) leftPreview.getLayoutParams();
			leftParm.width = 600;
			leftParm.height = FULLSCREEN_HEIGHT;
			leftPreview.setLayoutParams(leftParm);
			leftPreview.setVisibility(View.VISIBLE);
			leftPreview.setClickable(false);
			LinearLayout.LayoutParams rightParm = (LayoutParams) rightPreview.getLayoutParams();
			rightParm.width = 600;
			rightParm.height = FULLSCREEN_HEIGHT;
			rightPreview.setLayoutParams(rightParm);
			rightPreview.setVisibility(View.VISIBLE);
			rightPreview.setClickable(false);
			leftLine.setVisibility(View.VISIBLE);
			rightLine.setVisibility(View.VISIBLE);
			layoutNaviInfo.setVisibility(View.VISIBLE);
			stopCameraRender(CameraInfo.CAMERA_FACING_FRONT);
			stopCameraRender(CameraInfo.CAMERA_FACING_BACK);
			startCameraRender(CAMERA_ID_LEFT);
			startCameraRender(CAMERA_ID_RIGHT);
		}*/
		initRecordStatus();
//		updateSurfaceTexture();
		
		
		/*scrollView.post(new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				if (scrollPref != null && scrollPref.getBoolean("isMove", false)) {
					yValue = (int) scrollPref.getFloat("yValue", 200);
					scrollView.smoothScrollTo(0, yValue);
				} else {
					scrollView.smoothScrollTo(0, 200);
				}
			}
		});*/
	}

	private boolean currentClickState =false;
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		if (null != intent) {
			int status = intent.getIntExtra(KEY_STATUS, 0);
			boolean isVoiceControl = intent.getBooleanExtra(KEY_VOICE_CONTROL, false);
			boolean isReverse = intent.getBooleanExtra(KEY_REVERSE, false);
			Log.i(TAG, "onNewIntent() status = "+status+",isVoiceControl = "+isVoiceControl+",isReverse = "+isReverse);
//			if (FastReverseChecker.isReversing() || FastReverseChecker.isTurnRight()) {
//				if (currentStatus == 0) {
//					currentStatus = status;
//				}
//			}
			currentClickState = isClickRight;
			if((isReverse && status == STATE_FULLSCREEN_SHOW_CAMERA_RIGHT_BACK)
					|| (isVoiceControl && status == STATE_FULLSCREEN_SHOW_CAMERA_RIGHT_BACK)) {
				isClickRight = false;
			}
			initData(status,isVoiceControl,isReverse);
			updateSurfaceTexture();
		}
	}
	
	private void registerBroadcast(){
		IntentFilter filter = new IntentFilter();
		filter.addAction(RecordService.ACTION_RECORD_STARTED);
		filter.addAction(RecordService.ACTION_RECORD_STOPPED);
		filter.addAction(FastReverseChecker.INTENT_FAST_REVERSE_BOOTUP);
		filter.addAction(FastReverseChecker.INTENT_ACTION_FOUR_CAMERA_TURN_LEFT);
		filter.addAction(FastReverseChecker.INTENT_ACTION_FOUR_CAMERA_TURN_RIGHT);
		filter.addAction(AUTONAVI_ICON_ACTION);
		registerReceiver(receiver, filter);
	}
	
	private void startCameraRender(int cameraId){
		if (null != mRecService) {
			mRecService.startRender(cameraId);
		}
	}
	
	private void stopCameraRender(int cameraId){
		if (null != mRecService) {
			mRecService.stopRender(cameraId);
		}
	}

	@Override
	protected void onDestroy() {
		unregisterReceiver(receiver);
		super.onDestroy();
		currentStatus = 0;
		if (mRecService != null) {
            unbindService(mRecConnection);
            mRecService = null;
        }
		this.sendBroadcast(new Intent(ACTION_FULL_CAMERA_HIDE));
	}
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}
	
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if (hasFocus) {
			hideNavigationBar();
		}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (KeyEvent.KEYCODE_BACK == keyCode) {
			// return false;
		}
		// return super.onKeyDown(keyCode, event);
		if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
			hideNavigationBar();
		}
		return false;
	}
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		/*case R.id.left_preview:
			initData(STATE_FULLSCREEN_SHOW_CAMERA_RIGHT,false,false);
			break;*/
		case R.id.front_preview:
			initData(STATE_FULLSCREEN_SHOW_CAMERA_BACK,false,false);
			break;
		case R.id.back_preview:
//			isClickRight = true;
//			isCurrentActClick = true;
			initData(STATE_FULLSCREEN_SHOW_CAMERA_FRONT,false,false);
//			initData(STATE_FULLSCREEN_SHOW_CAMERA_RIGHT,false,false);
			break;
		case R.id.right_preview:
			isClickRight = false;
			isCurrentActClick = false;
			initData(STATE_FULLSCREEN_SHOW_CAMERA_FRONT,false,false);
			break;
		case R.id.bt_exit:
			FullScreenCameraActivity.this.finish();
			break;
		default:
			break;
		}
	}
	/**
	 * 隐藏导航栏
	 */
	public void hideNavigationBar() {
		View decorView = getWindow().getDecorView();    //设置点击左侧录像框进去时全屏
	       int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
			| View.SYSTEM_UI_FLAG_IMMERSIVE | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
		decorView.setSystemUiVisibility(uiOptions);
	}
	
	private void initRecordStatus(){
		Log.e("hy", "initRecordStatus"+sharedPreferences.getBoolean("isVideo", false));
		if (null != ivRecord) {
			if (sharedPreferences.getBoolean("isVideo", false)) {
				ivRecord.setVisibility(View.VISIBLE);
			}else{
				ivRecord.setVisibility(View.GONE);
			}
		}
	}
	
	private void showNaviInfo(Intent intent){
		if (null != intent) {
			if (layoutNaviInfo.getVisibility() == View.VISIBLE) {
				type = intent.getIntExtra("KEY_TYPE", -1);
				if (10001 == type) {// guide information
					iconState = intent.getIntExtra("ICON", -1);
					remiandDistance = intent.getIntExtra("SEG_REMAIN_DIS", -1);
					currRoadName = intent.getStringExtra("CUR_ROAD_NAME");
					nextRoadName = intent.getStringExtra("NEXT_ROAD_NAME");
					if (TextUtils.isEmpty(currRoadName) || TextUtils.isEmpty(nextRoadName)) {
						Log.i(TAG, "null navigation information!");
						return;
					}
					if (ledView.getVisibility() == View.VISIBLE) {
						ledView.setVisibility(View.GONE);
						layoutNavi.setVisibility(View.VISIBLE);
					}
					if (remiandDistance >= 0) {
						if (remiandDistance >= 1000) {
							tvDistance.setText(
									roadDF.format(remiandDistance / 1000.0));
							tvDistanceUnit.setText(getString(R.string.distance_km_unit));
						} else {
							tvDistance.setText(remiandDistance+"");
							tvDistanceUnit.setText(getString(R.string.distance_meter_unit));
						}
						if (tvDistanceUnit.getVisibility() != View.VISIBLE) {
							tvDistanceUnit.setVisibility(View.VISIBLE);
						}
					} else {
						tvDistance.setText(getString(R.string.distance_hint));
						tvDistanceUnit.setVisibility(View.GONE);
					}

					if (1 == iconState) {// self

					} else if (2 == iconState) {// turn left
						iv_icon.setImageResource(R.drawable.hud_sou2);
					} else if (3 == iconState) {// turn right
						iv_icon.setImageResource(R.drawable.hud_sou3);
					} else if (4 == iconState) {// turn left front
						iv_icon.setImageResource(R.drawable.hud_sou4);
					} else if (5 == iconState) {// turn right front
						iv_icon.setImageResource(R.drawable.hud_sou5);
					} else if (6 == iconState) {// turn left behind
						iv_icon.setImageResource(R.drawable.hud_sou6);
					} else if (7 == iconState) {// turn right behind
						iv_icon.setImageResource(R.drawable.hud_sou7);
					} else if (8 == iconState) {// turn around
						iv_icon.setImageResource(R.drawable.hud_sou8);
					} else if (9 == iconState) {// straight
						iv_icon.setImageResource(R.drawable.hud_sou9);
					} else if (10 == iconState) {// arrive somewhere
						iv_icon.setImageResource(R.drawable.hud_sou10);
					} else if (11 == iconState) {// Into the roundabout
						iv_icon.setImageResource(R.drawable.hud_sou11);
					} else if (12 == iconState) {// out of the roundabout
						iv_icon.setImageResource(R.drawable.hud_sou12);
					} else if (13 == iconState) {// Reach the service area
						iv_icon.setImageResource(R.drawable.hud_sou13);
					} else if (14 == iconState) {// Reach the toll station
						iv_icon.setImageResource(R.drawable.hud_sou14);
					} else if (15 == iconState) {// Reach the destination
						iv_icon.setImageResource(R.drawable.hud_sou15);
					} else if (16 == iconState) {// Enter the tunnel
						iv_icon.setImageResource(R.drawable.hud_sou16);
					}

					if ((!TextUtils.isEmpty(currRoadName)) && (!TextUtils.isEmpty(nextRoadName))) {
						tvSince.setText(getString(R.string.since));
						tvCurrentRoad.setText(currRoadName);
						int length  = nextRoadName.length();
						if(length>22){
							nextRoadName ="..."+ nextRoadName.substring(length-21);
						}
						tvFinalRoad.setText(nextRoadName);
					}
				}
			}
		}
	}
	
	private void updateSurfaceTexture(){
		int status = getIntent().getIntExtra(KEY_STATUS, 0);
		if(!isCurrentActClick && getIntent() != null) {
			isClickRight = getIntent().getBooleanExtra(HomeRecorderActivity.ISCLICKRIGHT, false);
		}
		Log.i(TAG, "updateSurfaceTexture  status = "+status+"  isClickRight = "+isClickRight+"   isCurrentActClick = "+isCurrentActClick );
		if (status == STATE_FULLSCREEN_SHOW_CAMERA_FRONT) {
			if (mTextureFrontIsUp) {
				mRecService.setPreviewDisplay(CameraInfo.CAMERA_FACING_FRONT, frontPreviewHolder);
				if (!mRecService.isPreview(CameraInfo.CAMERA_FACING_FRONT)) {
					mRecService.startPreview(CameraInfo.CAMERA_FACING_FRONT);
				}
				mRecService.startRender(CameraInfo.CAMERA_FACING_FRONT);
			}
		}else if(status == STATE_FULLSCREEN_SHOW_CAMERA_BACK){
			if (mTextureBackIsUp) {
				mRecService.setPreviewDisplay(CameraInfo.CAMERA_FACING_BACK, backPreviewHolder);
				if (!mRecService.isPreview(CameraInfo.CAMERA_FACING_BACK)) {
					mRecService.startPreview(CameraInfo.CAMERA_FACING_BACK);
				}
				mRecService.startRender(CameraInfo.CAMERA_FACING_BACK);
			}
		}/*else if(currentStatus == STATE_FULLSCREEN_SHOW_CAMERA_LEFT){
			if (mTextureLeftIsUp) {
				mRecService.setPreviewTexture(CAMERA_ID_LEFT, leftPreview.getSurfaceTexture());
				if (!mRecService.isPreview(CAMERA_ID_LEFT)) {
					mRecService.startPreview(CAMERA_ID_LEFT);
				}
				mRecService.startRender(CAMERA_ID_LEFT);
			}
		}*/
		else if(status == STATE_FULLSCREEN_SHOW_CAMERA_RIGHT_BACK || status == STATE_FULLSCREEN_SHOW_CAMERA_RIGHT){
			/*if (mTextureRightIsUp) {
				mRecService.setPreviewDisplay(CAMERA_ID_RIGHT, rightPreviewHolder);
				if (!mRecService.isPreview(CAMERA_ID_RIGHT)) {
					mRecService.startPreview(CAMERA_ID_RIGHT);
				}
				mRecService.startRender(CAMERA_ID_RIGHT);
			}*/
			if (mTextureBackIsUp) {
				mRecService.setPreviewDisplay(CameraInfo.CAMERA_FACING_BACK, backPreviewHolder);
				if (!mRecService.isPreview(CameraInfo.CAMERA_FACING_BACK)) {
					mRecService.startPreview(CameraInfo.CAMERA_FACING_BACK);
				}
				mRecService.startRender(CameraInfo.CAMERA_FACING_BACK);
			}
			if (mTextureRightIsUp) {
				mRecService.setPreviewDisplay(CAMERA_ID_RIGHT, rightPreviewHolder);
				if (!mRecService.isPreview(CAMERA_ID_RIGHT)) {
					mRecService.startPreview(CAMERA_ID_RIGHT);
				}
				mRecService.startRender(CAMERA_ID_RIGHT);
			}
			
		}else if(status == STATE_FULLSCREEN_SHOW_CAMERA_LEFT_BACK_RIGHT) {
			if (mTextureBackIsUp) {
				mRecService.setPreviewDisplay(CameraInfo.CAMERA_FACING_BACK, backPreviewHolder);
				if (!mRecService.isPreview(CameraInfo.CAMERA_FACING_BACK)) {
					mRecService.startPreview(CameraInfo.CAMERA_FACING_BACK);
				}
				mRecService.startRender(CameraInfo.CAMERA_FACING_BACK);
			}
			if (mTextureRightIsUp) {
				mRecService.setPreviewDisplay(CAMERA_ID_RIGHT, rightPreviewHolder);
				if (!mRecService.isPreview(CAMERA_ID_RIGHT)) {
					mRecService.startPreview(CAMERA_ID_RIGHT);
				}
				mRecService.startRender(CAMERA_ID_RIGHT);
			}
		}
		/*else if(currentStatus == STATE_FULLSCREEN_SHOW_CAMERA_LEFT_RIGHT){
			if (mTextureLeftIsUp) {
				mRecService.setPreviewTexture(CAMERA_ID_LEFT, leftPreview.getSurfaceTexture());
				if (!mRecService.isPreview(CAMERA_ID_LEFT)) {
					mRecService.startPreview(CAMERA_ID_LEFT);
				}
				mRecService.startRender(CAMERA_ID_LEFT);
			}
			if (mTextureRightIsUp) {
				mRecService.setPreviewTexture(CAMERA_ID_RIGHT, rightPreview.getSurfaceTexture());
				if (!mRecService.isPreview(CAMERA_ID_RIGHT)) {
					mRecService.startPreview(CAMERA_ID_RIGHT);
				}
				mRecService.startRender(CAMERA_ID_RIGHT);
			}
		}*/
	
	}
	
	
	private void voiceOpenFeedBack(int status){
		if (status == currentStatus) {
			if (status == STATE_FULLSCREEN_SHOW_CAMERA_FRONT) {
				speakText("你已经打开前路了");
			}else if(status == STATE_FULLSCREEN_SHOW_CAMERA_BACK){
				speakText("你已经打开后路了");
			}/*else if(status == STATE_FULLSCREEN_SHOW_CAMERA_LEFT){
				
			}*/
			else if(status == STATE_FULLSCREEN_SHOW_CAMERA_RIGHT){
				
			}/*else if(status == STATE_FULLSCREEN_SHOW_CAMERA_LEFT_BACK){
				speakText("你已经打开左路了");
			}*/
			else if(status == STATE_FULLSCREEN_SHOW_CAMERA_RIGHT_BACK){
				speakText("你已经打开右路了");
			}else if(status == STATE_FULLSCREEN_SHOW_CAMERA_LEFT_BACK_RIGHT){
				speakText("你已经打开盲区了");
			}
		}else{
			speakText("好的");
		}
	}
	
	private void voiceCloseFeedBack(int status){
		if (status == currentStatus) {
			speakText("好的");
			FullScreenCameraActivity.this.finish();
		}else{
			if (status == STATE_FULLSCREEN_SHOW_CAMERA_FRONT) {
				speakText("你没有打开前路");
			}else if(status == STATE_FULLSCREEN_SHOW_CAMERA_BACK){
				speakText("你没有打开后路");
			}/*else if(status == STATE_FULLSCREEN_SHOW_CAMERA_LEFT){
				
			}*/
			else if(status == STATE_FULLSCREEN_SHOW_CAMERA_RIGHT){
				
			}/*else if(status == STATE_FULLSCREEN_SHOW_CAMERA_LEFT_BACK){
				speakText("你没有打开左路");
			}*/
			else if(status == STATE_FULLSCREEN_SHOW_CAMERA_RIGHT_BACK){
				speakText("你没有打开右路");
			}else if(status == STATE_FULLSCREEN_SHOW_CAMERA_LEFT_BACK_RIGHT){
				speakText("你没有打开盲区");
			}
		}
	}
	
	private void speakText(String str) {
		Intent mIntent = new Intent();
		mIntent.setAction(ACTION_SPEAK_TEXT);
		mIntent.putExtra("otherText", str);
		sendBroadcast(mIntent);
	}

	private RecordService mRecService = null;
	private ServiceConnection mRecConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			Log.i(TAG, "onServiceConnected()");
			if (mRecService == null) {
				mRecService = ((RecordService.LocalBinder) service).getService();
			}
			mRecService.setVoiceControlCallBack(new RecordService.VoiceControlCallBack() {
				
				@Override
				public void onVoiceControl(int id) {
					Log.i(TAG, "onVoiceControl() id = "+id);
					voiceCloseFeedBack(id);
				}
				
				@Override
				public void onHome() {
					FullScreenCameraActivity.this.finish();
				};
			});
			mRecService.setRightInsertImpl(new RightInsertImpl() {
				
				@Override
				public void isRightInsert(boolean isInsert) {
					// TODO Auto-generated method stub
					if(tvTipInsertRight != null) {
						if (isInsert) {							
							tvTipInsertRight.setVisibility(View.GONE);
						} else {
							tvTipInsertRight.setVisibility(View.VISIBLE);
						}
					}
				}
			});
			mRecService.setBackInsertImpl(new BackInsertImpl() {
				
				@Override
				public void isBackInsert(boolean isInsert) {
					// TODO Auto-generated method stub
					if(tvTipInsertBack != null) {
						if (isInsert) {							
							tvTipInsertBack.setVisibility(View.GONE);
						} else {
							tvTipInsertBack.setVisibility(View.VISIBLE);
						}
					}
				}
			});
			if (isNeenUpdate) {
				updateSurfaceTexture();
				isNeenUpdate = false;
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			Log.i(TAG, "onServiceDisconnected()");
			if (null != mRecService) {
				mRecService.setVoiceControlCallBack(null);
				mRecService.setRightInsertImpl(null);
				mRecService = null;
			}
		}
	};
	
	private boolean isNeenUpdate = false;
	
	
	private BroadcastReceiver receiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (RecordService.ACTION_RECORD_STARTED.equals(action) 
					|| RecordService.ACTION_RECORD_STOPPED.equals(action)) {
				initRecordStatus();
			}else if(FastReverseChecker.INTENT_ACTION_FOUR_CAMERA_TURN_LEFT.equals(action)){
				if (!intent.getBooleanExtra(FastReverseChecker.FOUR_CAMERA_TURN_LEFT_STATUS_KEY, false)) {
					FullScreenCameraActivity.this.finish();
				}
			}else if(FastReverseChecker.INTENT_ACTION_FOUR_CAMERA_TURN_RIGHT.equals(action)){
				if (!intent.getBooleanExtra(FastReverseChecker.FOUR_CAMERA_TURN_RIGHT_STATUS_KEY, false)) {
					if(currentStatus != 0) {
						mHandler.postDelayed(new Runnable() {
							
							@Override
							public void run() {
								// TODO Auto-generated method stub
								Log.i("deng", "onReceive  currentClickState="+currentClickState);
								isClickRight = currentClickState;
								initData(currentStatus,false,false);
							}
						}, 300);
					}else {						
						FullScreenCameraActivity.this.finish();
					}
				}
			}else if(FastReverseChecker.INTENT_FAST_REVERSE_BOOTUP.equals(action)){
				if (!intent.getBooleanExtra("isReverseing", false)) {
					if(currentStatus != 0) {
						mHandler.postDelayed(new Runnable() {
							
							@Override
							public void run() {
								// TODO Auto-generated method stub	
								initData(currentStatus,false,false);
							}
						}, 300);
					}else {	
						FullScreenCameraActivity.this.finish();
					}
				}
			}else if(AUTONAVI_ICON_ACTION.equals(action)){
				showNaviInfo(intent);
			}
		}
	};

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.i(TAG, "surfaceCreated() mRecService = "+mRecService);
		/*if (null != mRecService && null != leftPreview && leftPreview.getSurfaceTexture() == surface) {
			mTextureLeftIsUp = true;
			mRecService.setPreviewTexture(CAMERA_ID_LEFT, surface);
			if (!mRecService.isPreview(CAMERA_ID_LEFT)) {
				mRecService.startPreview(CAMERA_ID_LEFT);
			}
			mRecService.startRender(CAMERA_ID_LEFT);
		} else */
		if (null != frontPreview && frontPreview.getHolder() == holder) {
			Log.i(TAG, "frontPreview");
			frontPreviewHolder = holder;
			mTextureFrontIsUp = true;
			if (null != mRecService) {
				mRecService.setPreviewDisplay(CameraInfo.CAMERA_FACING_FRONT, frontPreviewHolder);
				if (!mRecService.isPreview(CameraInfo.CAMERA_FACING_FRONT)) {
					mRecService.startPreview(CameraInfo.CAMERA_FACING_FRONT);
				}
				mRecService.startRender(CameraInfo.CAMERA_FACING_FRONT);
			}
		}else if (null != backPreview && backPreview.getHolder() == holder) {
			Log.i(TAG, "backPreview");
			backPreviewHolder = holder;
			mTextureBackIsUp = true;
			if (null != mRecService) {
				mRecService.setPreviewDisplay(CameraInfo.CAMERA_FACING_BACK, backPreviewHolder);
				if (!mRecService.isPreview(CameraInfo.CAMERA_FACING_BACK)) {
					mRecService.startPreview(CameraInfo.CAMERA_FACING_BACK);
				}
				mRecService.startRender(CameraInfo.CAMERA_FACING_BACK);
			}
		} else if (null != rightPreview && rightPreview.getHolder() == holder) {
			Log.i(TAG, "rightPreview");
			rightPreviewHolder = holder;
			mTextureRightIsUp = true;
			if (null != mRecService) {
				mRecService.setPreviewDisplay(CAMERA_ID_RIGHT, rightPreviewHolder);
				if (!mRecService.isPreview(CAMERA_ID_RIGHT)) {
					mRecService.startPreview(CAMERA_ID_RIGHT);
				}
				mRecService.startRender(CAMERA_ID_RIGHT);
			}
		}
		if (null == mRecService) {
			Log.i(TAG, "surfaceCreated  mRecService is null");
			isNeenUpdate = true;
		}
		
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.i(TAG, "onSurfaceTextureDestroyed()");
		/*if (null != mRecService && null != leftPreview && leftPreview.getSurfaceTexture() == surface) {
			mTextureLeftIsUp = false;
			mRecService.stopRender(CAMERA_ID_LEFT);
		} else */
		if (null != frontPreview && frontPreview.getHolder() == holder) {
			mTextureFrontIsUp = false;
//			mRecService.stopRender(CameraInfo.CAMERA_FACING_FRONT);
		} else if (null != backPreview && backPreview.getHolder() == holder) {
			mTextureBackIsUp = false;
//			mRecService.stopRender(CameraInfo.CAMERA_FACING_BACK);
		}  else if (null != rightPreview && rightPreview.getHolder() == holder) {
			mTextureRightIsUp = false;
//			mRecService.stopRender(CAMERA_ID_RIGHT);
		}
		isMove = false;
		
	}

}
