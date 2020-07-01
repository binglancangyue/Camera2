package com.android.camera.home_recorder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.android.camera.Storage;
import com.android.camera.v66.FullScreenCameraActivity;
import com.android.camera.v66.MyPreference;
import com.android.camera.v66.PreviewFragment;
import com.android.camera.v66.RecordService;
import com.android.camera.v66.RecordService.BackInsertImpl;
import com.android.camera.v66.RecordService.RightInsertImpl;
import com.android.camera.v66.RecorderActivity;
import com.android.camera.v66.TwoFloatWindow;
import com.android.camera.v66.WrapedRecorder.IRecordCallback;

import android.R.id;
import android.R.string;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.Camera.CameraInfo;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.LruCache;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;
import com.android.camera2.R;

import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

public class HomeRecorderActivity extends Activity implements SurfaceHolder.Callback,IRecordCallback{
	private static final String TAG = "HomeRecorderActivity";
	private SurfaceView  frontPreview,backPreview, rightPreview;
	private SurfaceHolder frontPreviewHolder,backPreviewHolder, rightPreviewHolder;
	private boolean mTextureFrontIsUp = false;
	private boolean mTextureBackIsUp = false;
	private boolean mTextureRightIsUp = false;
	public static final int CAMERA_ID_RIGHT = 2;
	public static final int RESULT_CODE = 100;
	private Qc7SpliteGridViewAdapter qc7SpliteGridViewAdapter;
	private GridView qc9ThreeGridView;
	private ImageView mIv9ThreeSettings,mIv9ThreeApplists;
	public static final String ISCLICKRIGHT = "is_click_right";
	private static final String ACTION_LOCATION_PRESS_DOWN = "RESTOR_PREES_DOWN";
	private static final String ACTION_RECORDER_SHOWCAMERA = "com.cywl.recorder.showcamera";
	public static final int MSG_UPDATE_BACK_PREVIEW = 0;
	public static final int MSG_UPDATE_FRONT_PREVIEW = 1;
	public static final int MSG_UPDATE_RIGHT_PREVIEW = 2;
	private MyPreference mPref;
	private Handler mHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_UPDATE_BACK_PREVIEW:
//				Log.i(TAG, "---handleMessage()"+"MSG_UPDATE_BACK_PREVIEW");
				updateBackSurfaceView();
				break;
			case MSG_UPDATE_FRONT_PREVIEW:
//				Log.i(TAG, "---handleMessage()"+"MSG_UPDATE_FRONT_PREVIEW");
				updateFrontSurfaceView();
				break;
			case MSG_UPDATE_RIGHT_PREVIEW:
//				Log.i(TAG, "---handleMessage()"+"MSG_UPDATE_RIGHT_PREVIEW");
				//remove by zdt
				//updateRightSurfaceView();
				break;
			default:
				break;
			}
		};
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		View view = getWindow().getDecorView();
		int visibility = View.STATUS_BAR_TRANSIENT | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
		view.setSystemUiVisibility(visibility);	
		setContentView(R.layout.activity_home_recorder);
		registerBroadCastReceiver();
		mPref = MyPreference.getInstance(HomeRecorderActivity.this);
		Intent intent = new Intent(this, RecordService.class);
        startService(intent);
		bindService(new Intent(this, RecordService.class), mRecConnection, Context.BIND_AUTO_CREATE);
		initView();
		initData();
		// <zhuangdt> <20190516> <增加语音唤醒词修改> begin
		intTXZWakeUpName();
		// <zhuangdt> <20190516> <增加语音唤醒词修改> end
	}
	
	private void registerBroadCastReceiver() {
		// TODO Auto-generated method stub
		IntentFilter filter = new IntentFilter();
		filter.addAction(ACTION_LOCATION_PRESS_DOWN);
		filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
		filter.addAction(RecordService.ACTION_STOP_APP);
		filter.addAction(ACTION_RECORDER_SHOWCAMERA);
		registerReceiver(myReceiver, filter);
	}

	private void initView() {
		frontPreview = (SurfaceView) findViewById(R.id.front_preview);
		backPreview = (SurfaceView) findViewById(R.id.back_preview);
		rightPreview = (SurfaceView) findViewById(R.id.right_preview);
		qc9ThreeGridView = (GridView) findViewById(R.id.gridview_9three_nosplite);
		mIv9ThreeSettings = (ImageView) findViewById(R.id.iv_9three_settings);
		mIv9ThreeApplists = (ImageView) findViewById(R.id.iv_9three_applists);
		frontPreviewHolder = frontPreview.getHolder();
		backPreviewHolder = backPreview.getHolder();
		rightPreviewHolder = rightPreview.getHolder();
		frontPreviewHolder.addCallback(this);
		backPreviewHolder.addCallback(this);
		rightPreviewHolder.addCallback(this);
		frontPreview.setOnClickListener(new FrontListener());
		backPreview.setOnClickListener(new BackListener());
		rightPreview.setOnClickListener(new RightListener());
		rightPreview.setVisibility(View.GONE);

		
	}

	
	
	
	
	
	private void initData() {
		qc7SpliteGridViewAdapter = new Qc7SpliteGridViewAdapter(this, getQc7SpliteItemData());
		qc9ThreeGridView.setAdapter(qc7SpliteGridViewAdapter);
		qc9ThreeGridView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				String packetName = qc7SpliteGridViewAdapter.getItem(position).getmPackage();
				Log.i(TAG, "packetName"+packetName+"position"+position);
				if ("com.android.camera2".equals(packetName)) {
					Intent intent = new Intent(HomeRecorderActivity.this,RecorderActivity.class);
					HomeRecorderActivity.this.startActivity(intent);
				}else {
					launchByPackageName(packetName);
				}
			}
		});
		qc9ThreeGridView.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				if (Constances.isCustomMap) {
					if (position == 0) {
						Intent launchIntent = getPackageManager().getLaunchIntentForPackage(Constances.PACKAGE_NAME_APPLIST);
						if(launchIntent!=null){
							
							launchIntent.putExtra("choose_package", true);
							startActivityForResult(launchIntent, 0);
						}
					}
					return true;
				}
				return false;
			}
		});
		mIv9ThreeSettings.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				launchByPackageName(Constances.PACKAGE_NAME_SETTINGS);
			}
		});
        mIv9ThreeApplists.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
//				launchByPackageName(Constances.PACKAGE_NAME_APPLIST);
				HomeRecorderActivity.this.startActivity(new Intent(HomeRecorderActivity.this,AppListActivity.class));
			}
		});
        addCamera();
        openCamera();
        addWaterMark();
	}
	
	private boolean isCameraAdded = false;
	private void addCamera() {
		if (null != mRecService && !isCameraAdded) {
			Log.i(TAG, "---addCamera()");
			if (!mRecService.isCameraAdd(CameraInfo.CAMERA_FACING_BACK)) {
				mRecService.addCamera(CameraInfo.CAMERA_FACING_BACK);
			}
			if (!mRecService.isCameraAdd(CameraInfo.CAMERA_FACING_FRONT)) {
				mRecService.addCamera(CameraInfo.CAMERA_FACING_FRONT);
			}
			/* remove by zdt
			 * if (!mRecService.isCameraAdd(TwoFloatWindow.RIGHT_CAMERA_ID)) {
				mRecService.addCamera(TwoFloatWindow.RIGHT_CAMERA_ID);
			}*/
			isCameraAdded = true;
		}
	}
	
	private boolean isCameraOpen = false;
	private void openCamera() {
		if (null != mRecService && !isCameraOpen) {
			if (!mRecService.isCameraOpened(CameraInfo.CAMERA_FACING_BACK)) {
				mRecService.openCamera(CameraInfo.CAMERA_FACING_BACK);
//				Log.i(TAG, "---openCamera()======"+"CameraInfo.CAMERA_FACING_BACK");
			}
			if (!mRecService.isCameraOpened(CameraInfo.CAMERA_FACING_FRONT)) {
				mRecService.openCamera(CameraInfo.CAMERA_FACING_FRONT);
//				Log.i(TAG, "---openCamera()======"+"CameraInfo.CAMERA_FACING_FRONT");
			}
			/* remove by zdt
			 * if (!mRecService.isCameraOpened(TwoFloatWindow.RIGHT_CAMERA_ID)) {
				mRecService.openCamera(TwoFloatWindow.RIGHT_CAMERA_ID);
//				Log.i(TAG, "---openCamera()======"+"RIGHT_CAMERA_ID");
			}*/
			isCameraOpen = true;
		}
		
	}
	
	private void addWaterMark() {
		Log.i(TAG, "---addWaterMark()");
		if (null != mRecService) {
			if (!mRecService.isWaterMarkRuning(CameraInfo.CAMERA_FACING_FRONT)) {
				mRecService.startWaterMark(CameraInfo.CAMERA_FACING_FRONT);
			}
			if (!mRecService.isWaterMarkRuning(CameraInfo.CAMERA_FACING_BACK)) {
				mRecService.startWaterMark(CameraInfo.CAMERA_FACING_BACK);
			}
			/* remove by zdt
			 * if (!mRecService.isWaterMarkRuning(TwoFloatWindow.RIGHT_CAMERA_ID)) {
				mRecService.startWaterMark(TwoFloatWindow.RIGHT_CAMERA_ID);
			}*/
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
	}
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		if (mRecService != null) {
			 if (mRecService.getRecordCallback(CameraInfo.CAMERA_FACING_FRONT) == this) {
				 mRecService.setRecordCallback(CameraInfo.CAMERA_FACING_FRONT, null);
	            }
            unbindService(mRecConnection);
            mRecService = null;
        }
		if(myReceiver != null) {
			unregisterReceiver(myReceiver);
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		if(frontPreview != null && frontPreview.getHolder() == holder ) {
			Log.i(TAG, "surfaceCreated()  frontPreview");
			frontPreviewHolder = holder;
			mTextureFrontIsUp = true;
			if(mRecService != null && mRecService.isCameraOpened(CameraInfo.CAMERA_FACING_FRONT)) {
				mRecService.setPreviewDisplay(CameraInfo.CAMERA_FACING_FRONT, frontPreviewHolder);
				if(!mRecService.isPreview(CameraInfo.CAMERA_FACING_FRONT)) {
					mRecService.startPreview(CameraInfo.CAMERA_FACING_FRONT);
				}
				mRecService.startRender(CameraInfo.CAMERA_FACING_FRONT);
			}
		}else if(backPreview != null && backPreview.getHolder() == holder) {
			Log.i(TAG, "surfaceCreated()  backPreview");
            backPreviewHolder = holder;
            mTextureBackIsUp = true;
            if(mRecService != null && mRecService.isCameraOpened(CameraInfo.CAMERA_FACING_BACK)) {
            	mRecService.setPreviewDisplay(CameraInfo.CAMERA_FACING_BACK, backPreviewHolder);
            	if(!mRecService.isPreview(CameraInfo.CAMERA_FACING_BACK)) {
            		mRecService.startPreview(CameraInfo.CAMERA_FACING_BACK);
            	}
            	mRecService.startRender(CameraInfo.CAMERA_FACING_BACK);
            }
		}else if(rightPreview != null && rightPreview.getHolder() == holder) {
			/* remove by zdt
			Log.i(TAG, "surfaceCreated()  rightPreview");
            rightPreviewHolder = holder;
            mTextureRightIsUp = true;
            if(mRecService != null && mRecService.isCameraOpened(CAMERA_ID_RIGHT)) {
            	mRecService.setPreviewDisplay(CAMERA_ID_RIGHT, rightPreviewHolder);
            	if(!mRecService.isPreview(CAMERA_ID_RIGHT)) {
            		mRecService.startPreview(CAMERA_ID_RIGHT);
            	}
            	mRecService.startRender(CAMERA_ID_RIGHT);
            }*/
		}
		if (null == mRecService) {
			Log.i(TAG, "surfaceCreated  mRecService is null");
			isNeedUpdate = true;
		}
		
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		Log.i(TAG, "surfaceDestroyed");
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
	}
	private RecordService mRecService = null;
	private boolean isNeedUpdate = false;
	private boolean mIsMuteOn;
	private ServiceConnection mRecConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			Log.i(TAG, "---onServiceConnected()");
			if (mRecService == null) {
				mRecService = ((RecordService.LocalBinder) service).getService();
			}
			addCamera();
			openCamera();
			addWaterMark();
			
			if (isNeedUpdate) {
				updateFrontSurfaceView();
				updateBackSurfaceView();
				// remove by zdt
				//updateRightSurfaceView();
				isNeedUpdate = false;
			}
			
			if (mRecService != null ) {
				initRecorder();
		    }
		}

		public void onServiceDisconnected(ComponentName className) {
			if (null != mRecService) {
				mRecService = null;
			}
		}
	};

	protected void updateFrontSurfaceView() {
//		Log.i(TAG, "---updateSurfaceView() mRecService = "+mRecService);
		if (mRecService != null) {
			if (!mRecService.isCameraOpened(CameraInfo.CAMERA_FACING_FRONT)) {
				mRecService.openCamera(CameraInfo.CAMERA_FACING_FRONT);
//				Log.i(TAG, "---openCamera()======"+"CameraInfo.CAMERA_FACING_FRONT");
			}
			if(mRecService.isCameraOpened(CameraInfo.CAMERA_FACING_FRONT)) {
//				Log.i(TAG, "---updateSurfaceView() front camera opened!");
				if(mRecService != null && mTextureFrontIsUp 
						&& frontPreviewHolder != null) {
//					Log.i(TAG, "---updateSurfaceView()  setPreviewDisplay  front");
					mRecService.setPreviewDisplay(CameraInfo.CAMERA_FACING_FRONT, frontPreviewHolder);
					if(!mRecService.isPreview(CameraInfo.CAMERA_FACING_FRONT)) {
						mRecService.startPreview(CameraInfo.CAMERA_FACING_FRONT);
					}
					mRecService.startRender(CameraInfo.CAMERA_FACING_FRONT);
				}
			}else {
//				Log.i(TAG, "---updateSurfaceView()  MSG_UPDATE_FRONT_PREVIEW");
				mHandler.removeMessages(MSG_UPDATE_FRONT_PREVIEW);
				mHandler.sendEmptyMessageDelayed(MSG_UPDATE_FRONT_PREVIEW, 1000);	
				
			}
			
		}
	}
	protected void updateBackSurfaceView() {
//		Log.i(TAG, "---updateSurfaceView() mRecService = "+mRecService);
		if (mRecService != null) {
			if (!mRecService.isCameraOpened(CameraInfo.CAMERA_FACING_BACK)) {
				mRecService.openCamera(CameraInfo.CAMERA_FACING_BACK);
//				Log.i(TAG, "---openCamera()======"+"CameraInfo.CAMERA_FACING_BACK");
			}
			if(mRecService.isCameraOpened(CameraInfo.CAMERA_FACING_BACK)){
//				Log.i(TAG, "---updateSurfaceView() back camera opened!");
				if(mRecService != null && mTextureBackIsUp 
						&& backPreviewHolder != null ) {
//					Log.i(TAG, "---updateSurfaceView()  setPreviewDisplay  back");
		        	mRecService.setPreviewDisplay(CameraInfo.CAMERA_FACING_BACK, backPreviewHolder);
		        	if(!mRecService.isPreview(CameraInfo.CAMERA_FACING_BACK)) {
		        		mRecService.startPreview(CameraInfo.CAMERA_FACING_BACK);
		        	}
		        	mRecService.startRender(CameraInfo.CAMERA_FACING_BACK);
		        }
			}else {
				mHandler.removeMessages(MSG_UPDATE_BACK_PREVIEW);
				mHandler.sendEmptyMessageDelayed(MSG_UPDATE_BACK_PREVIEW, 1000);	
			}
		}
	}
	/* remove by zdt
	 * protected void updateRightSurfaceView() {
		if (mRecService != null) {
			if (!mRecService.isCameraOpened(TwoFloatWindow.RIGHT_CAMERA_ID)) {
				mRecService.openCamera(TwoFloatWindow.RIGHT_CAMERA_ID);
//				Log.i(TAG, "---openCamera()======"+"RIGHT_CAMERA_ID");
			}
			if(mRecService.isCameraOpened(CAMERA_ID_RIGHT)) {
//				Log.i(TAG, "---updateSurfaceView() right camera opened!");
				if(mRecService != null && mTextureRightIsUp
						 && rightPreviewHolder != null ) {
//					 Log.i(TAG, "---updateSurfaceView()  setPreviewDisplay  right");
		         	mRecService.setPreviewDisplay(CAMERA_ID_RIGHT, rightPreviewHolder);
		         	if(!mRecService.isPreview(CAMERA_ID_RIGHT)) {
		         		mRecService.startPreview(CAMERA_ID_RIGHT);
		         	}
		         	mRecService.startRender(CAMERA_ID_RIGHT);
		         }
			}else {
//				Log.i(TAG, "---updateSurfaceView()  MSG_UPDATE_PREVIEW");
				mHandler.removeMessages(MSG_UPDATE_RIGHT_PREVIEW);
				mHandler.sendEmptyMessageDelayed(MSG_UPDATE_RIGHT_PREVIEW, 1000);	
			}
		}
		
	}*/
	private class BackListener implements OnClickListener{

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			Intent backIntent = new Intent(HomeRecorderActivity.this,FullScreenCameraActivity.class);
			backIntent.putExtra(FullScreenCameraActivity.KEY_STATUS, FullScreenCameraActivity.STATE_FULLSCREEN_SHOW_CAMERA_BACK);
			startActivity(backIntent);
		}
		
	}
	private class FrontListener implements OnClickListener{

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			Intent frontIntent = new Intent(HomeRecorderActivity.this,FullScreenCameraActivity.class);
			frontIntent.putExtra(FullScreenCameraActivity.KEY_STATUS, FullScreenCameraActivity.STATE_FULLSCREEN_SHOW_CAMERA_FRONT);
			startActivity(frontIntent);
		}
		
	}
	private class RightListener implements OnClickListener{

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			Intent rightIntent = new Intent(HomeRecorderActivity.this,FullScreenCameraActivity.class);
			rightIntent.putExtra(FullScreenCameraActivity.KEY_STATUS, FullScreenCameraActivity.STATE_FULLSCREEN_SHOW_CAMERA_RIGHT);
			rightIntent.putExtra(ISCLICKRIGHT, true);
			startActivity(rightIntent);
		}
		
	}
	public List<ItemData> getQc7SpliteItemData() {
		List<ItemData> itemData = new ArrayList<ItemData>();
		ItemData data = null;
		for (int i = 0; i < Constances.m9ThreePackageNoSplite.length; i++) {
			data = new ItemData(Constances.m9ThreePackageNoSplite[i],
					Constances.img9ThreeImageNoSplite[i]);
			itemData.add(data);
		}
		return itemData;
	}
	private void launchByPackageName(String packageName) {
		if (TextUtils.isEmpty(packageName)) {
			Log.i(TAG, "package name is null!");
			return;
		}
//		if (Constances.PACKAGE_NAME_AUTOLITE.equals(packageName)
//				|| Constances.PACKAGE_NAME_KAILIDE.equals(packageName)) {
//			// FunctionCore handle
//			Intent mIntent = new Intent();
//			mIntent.setAction(Constances.ACTION_NAME_ONLY_ONE_CAN_START);
//			mIntent.putExtra(Constances.KEY_WHICH_NAV,
//					packageName.equals(Constances.PACKAGE_NAME_AUTOLITE) ? Constances.KEY_GAODE
//							: Constances.KEY_KAILIDE);
//			sendBroadcast(mIntent);
//		} else {
		
			try {
				Log.i(TAG, "packageName"+packageName);
				Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
				startActivity(launchIntent);
			} catch (Exception e) {
				// Toast.makeText(HomeRecorderActivity.this,
				// getText(R.string.tv_app_not_install), Toast.LENGTH_SHORT).show();
				
			}
//		}
			
	}
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		return true;
	}
	
	
	BroadcastReceiver myReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			if(ACTION_LOCATION_PRESS_DOWN.equals(intent.getAction())) {
				String packageName = Constances.PACKAGE_NAME_NAVI;
				if (Constances.isCustomMap) {
					packageName = Utils.getUtilInstance(HomeRecorderActivity.this).getString(Constances.KEY_DEFAULT_MAP_PACKAGE_NAME, packageName);
				}
				launchByPackageName(packageName);
			}else if(Intent.ACTION_MEDIA_MOUNTED.equals(intent.getAction())){
				Log.i(TAG, "onReceive  android.intent.action.MEDIA_MOUNTED  开始录像");
//				startRecord();
			}else if(RecordService.ACTION_STOP_APP.equals(intent.getAction())) {
				stopRecord();
			}else if(ACTION_RECORDER_SHOWCAMERA.equals(intent.getAction())) {
				
				boolean showback =(Boolean) intent.getExtra("showback", false);
				Log.e(TAG, "showback:"+showback);
				if(showback){
		
					frontPreview.setVisibility(View.GONE);
					backPreview.setVisibility(View.VISIBLE);
				}else{
					frontPreview.setVisibility(View.VISIBLE);
					backPreview.setVisibility(View.GONE);
				}
				
			}
		}
		
	};
	
	private void stopRecord() {
		if(mIsRecording && mRecService != null) {
			mRecService.stopRecording();
			Intent intent3=new Intent();
            intent3.setAction("CLOSE_VIDEO_APP");
            mRecService.sendBroadcast(intent3);
		}
	}
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_CODE) {
			String packageName = data.getStringExtra("package");
			Utils.getUtilInstance(HomeRecorderActivity.this).putString(Constances.KEY_DEFAULT_MAP_PACKAGE_NAME, packageName);
			if(packageName.equals(Constances.PACKAGE_NAME_TIANDIDA)){
				boolean isAutoLite = false;
				Intent mIntent = new Intent(Constances.ACTION_NAME_IS_AUTOLITE);
				mIntent.putExtra("isAutoLite", isAutoLite);
				sendBroadcast(mIntent);
			} else if (packageName.equals(Constances.PACKAGE_NAME_AUTOLITE)){
				boolean isAutoLite = true;
				Intent mIntent = new Intent(Constances.ACTION_NAME_IS_AUTOLITE);
				mIntent.putExtra("isAutoLite", isAutoLite);
				sendBroadcast(mIntent);
			}
		}
	}
	
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		
		SharedPreferences preferences = getSharedPreferences("isVideo",
				Context.MODE_WORLD_READABLE | Context.MODE_MULTI_PROCESS);
		Log.i("hy", "HomeRecorderActivity::222222:"+preferences.getBoolean("isVideo", false));
	}
	
	
	private void initRecorder() {
		 Log.i(TAG, "initRecorder() :setRecordCallback()");
		 mIsMuteOn = mPref.isMute();
		 mRecService.setRecordCallback(CameraInfo.CAMERA_FACING_FRONT, HomeRecorderActivity.this);
		 mRecService.setMute(true, CameraInfo.CAMERA_FACING_BACK);
		 mRecService.setMute(mIsMuteOn, CameraInfo.CAMERA_FACING_FRONT);
	}
	private void startRecord() {
		 if (mRecService != null) {
			 mRecService.setLockOnce(false);
                 if (Storage.getTotalSpace() < 0) {
                     // todo more gentlly hint
                     Log.d(TAG, "startRecording sd not mounted");
                     Toast.makeText(HomeRecorderActivity.this, R.string.sdcard_not_found,
                             Toast.LENGTH_LONG).show();
                     
                     Intent intent8=new Intent();
                     intent8.setAction("com.action.other_Text");
                     intent8.putExtra("otherText", "TF卡不存在");
                     mRecService.sendBroadcast(intent8);
                     return;
                 } else if (Storage.getSdcardBlockSize() < 64 * 1024) {
                     Log.d(TAG,
                             "check sdcard failed, sdcard block size "
                                     + (Storage.getSdcardBlockSize() / 1024) + "k");
                     
                     Intent intent8=new Intent();
                     intent8.setAction("com.action.other_Text");
                     intent8.putExtra("otherText", "录制不成功，请格式SD卡");
                     mRecService.sendBroadcast(intent8);
                     
//                     showFormatMsgDialog();
                     return;
                 } else if (mRecService.isMiniMode()) {
                     Toast.makeText(HomeRecorderActivity.this, R.string.device_busy, Toast.LENGTH_LONG)
                             .show();
                     return;
                 }
                 mIsRecording = true;
                 Log.e(TAG, "onRecordStart");
                 mRecService.startRecording();
			     Intent intent3=new Intent();
		         intent3.setAction("CLOSE_VIDEO_APP");
		         mRecService.sendBroadcast(intent3);
		         String locale = Locale.getDefault().toString();
		         if (locale.equals("zh_CN") || locale.equals("zh_TW")) {
		                MediaPlayer mediaPlayer = MediaPlayer.create(mRecService, R.raw.start);
		                mediaPlayer.start();
		         }
         }
	}

	private boolean mIsRecording = false;
	@Override
	public void onCameraOpen() {
		// TODO Auto-generated method stub
		Log.e(TAG, "onCameraOpen()");
		boolean cameraOpened = mRecService.isCameraOpened(CameraInfo.CAMERA_FACING_FRONT);
		Log.e(TAG, "onCameraOpen()"+cameraOpened);
		
		if(!cameraOpened){
			return;
		}
		
		if(!mIsRecording) {
			startRecord();
		}
	}

	@Override
	public void onRecordStarted(boolean isStarted) {
		// TODO Auto-generated method stub
		Log.i(TAG, "onRecordStarted()"+"isStarted = "+isStarted);
		if(isStarted) {
			mIsRecording = isStarted;
		}
	}

	@Override
	public void onRecordStoped() {
		// TODO Auto-generated method stub
		Log.i(TAG, "onRecordStoped()");
		mIsRecording = false;
	}

	@Override
	public void onTimeUpdate(long curTime) {
		// TODO Auto-generated method stub
		Log.i(TAG, "onTimeUpdate()");
	}

	@Override
	public void onMute(boolean isMuted) {
		// TODO Auto-generated method stub
		Log.i(TAG, "onMute()"+" isMuted = "+isMuted);
        mIsMuteOn = isMuted;
        if (mPref != null) {
            mPref.saveMute(mIsMuteOn);
        }
	}

	@Override
	public void onLocked(boolean isLocked) {
		// TODO Auto-generated method stub
		Log.i(TAG, "onLocked()"+"isLocked = "+isLocked);
	}

	@Override
	public void onPictureToken() {
		// TODO Auto-generated method stub
		Log.i(TAG, "onPictureToken()");
	}

	@Override
	public void onCameraPlug(boolean isOut) {
		// TODO Auto-generated method stub
		Log.i(TAG, "onCameraPlug()"+"isOut = "+isOut);
	}

	// <zhuangdt> <20190516> <增加语音唤醒词修改> begin
	private void intTXZWakeUpName() {
		String name = "小汇";
		if ("weijia".equals(SystemProperties.get("ro.custom.version"))) {
			name = "为佳";
		}
		Settings.System.putString(getContentResolver(),"cywl_wakeup_keywords",name);
		Log.i("", "version: " + SystemProperties.get("ro.custom.version") + ", name: " + name
				+ ", cywl_wakeup_keywords: " + Settings.System.getString(getContentResolver(), "cywl_wakeup_keywords")
				);
	}
	// <zhuangdt> <20190516> <增加语音唤醒词修改> end
}
