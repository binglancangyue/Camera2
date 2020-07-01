package com.android.camera.v66;

import android.app.ActivityManager;
import android.app.ActivityManager.RecentTaskInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.hardware.Camera.CameraInfo;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.SurfaceHolder.Callback;
import android.view.WindowManager.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.android.camera2.R;

public class TwoCameraPreviewWin implements Callback {

	private static final String TAG = "TwoCameraPreviewWin";
	public static final String ACTION_SWITCH_FRAGMENT = "com.qc.action.switch_fragment";
	private View rootView;
	private FrameLayout firstLayout,secondLayout;
	private SurfaceView firstSurfaceView,secondSurfaceView;
	private SurfaceHolder firstSurfaceHolder,secondSurfaceHolder;
	private ImageView firstImageView,secondImageView;
	
	public static int firstSurfaceShowId = 0;
	public static int secondSurfaceShowId = 2;

	private RecordService mRecService;
	private WindowManager.LayoutParams mWmParams;
	private WindowManager mWindowManager;
	private int width = 159;
	private int height = 110;
//	public static final String SHOW_TWO_WINDOW_ACTION = "show_two_window_action";
//	public static final String HIDE_TWO_WINDOW_ACTION = "hide_two_window_action";
	private static final String ACTION_FULL_CAMERA_SHOW = "com.zqc.action.show_full_camera";
	private static final String ACTION_FULL_CAMERA_HIDE = "com.zqc.action.hide_full_camera";
	public static final String BACK_WINDOW_ACTION = "back_window_action";
	public static final String HOME_TWO_WINDOW_ACTION = "home_two_window_action";
	private boolean isFirstSurfaceReady = false;
	private boolean isSecondSurfaceReady = false;
	private boolean isCameraOn = false;
	private boolean isShowAppIcon = false;
	private boolean isLuncher = false;
	private String packeNames;
	private Drawable drawable;
	private Receiver receiver;
	
	private Handler mHandler = new Handler();

	public TwoCameraPreviewWin(RecordService service) {
		mRecService = service;
		mWindowManager = (WindowManager) mRecService.getApplication().getSystemService(Context.WINDOW_SERVICE);
		receiver = new Receiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(ACTION_FULL_CAMERA_SHOW);
		filter.addAction(ACTION_FULL_CAMERA_HIDE);
		filter.addAction(BACK_WINDOW_ACTION);
		filter.addAction(HOME_TWO_WINDOW_ACTION);
		mRecService.registerReceiver(receiver, filter);
		initWindowParameter();
		init();
	}
	
	private void initWindowParameter() {
		mWmParams = new WindowManager.LayoutParams();
		mWmParams.type = LayoutParams.TYPE_SYSTEM_ERROR;
		mWmParams.format = PixelFormat.RGBA_8888;
		mWmParams.flags = LayoutParams.FLAG_NOT_FOCUSABLE | LayoutParams.FLAG_HARDWARE_ACCELERATED;
		mWmParams.gravity = Gravity.RIGHT | Gravity.TOP;
		mWmParams.x = 0;
		mWmParams.y = 0;
		mWmParams.width = 159;
		mWmParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
		mWmParams.windowAnimations = android.R.style.Animation_Translucent;
	}

	private void init() {
		rootView = LayoutInflater.from(mRecService).inflate(R.layout.two_cam_preview, null);
		firstLayout = (FrameLayout)rootView.findViewById(R.id.layout_first);
		firstSurfaceView  = (SurfaceView)rootView.findViewById(R.id.first_preview);
		firstImageView  = (ImageView)rootView.findViewById(R.id.iv_first);
		firstSurfaceHolder = firstSurfaceView.getHolder();
		
		secondLayout = (FrameLayout)rootView.findViewById(R.id.layout_second);
		secondSurfaceView  = (SurfaceView)rootView.findViewById(R.id.second_preview);
		secondImageView  = (ImageView)rootView.findViewById(R.id.iv_second);
		secondSurfaceHolder = secondSurfaceView.getHolder();
		firstSurfaceHolder.addCallback(this);
		secondSurfaceHolder.addCallback(this);
		firstLayout.setOnClickListener(firstViewClick);
		secondLayout.setOnClickListener(secondViewClick);
		mWindowManager.addView(rootView, mWmParams);
	}



	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
		// TODO Auto-generated method stub

	}

	@Override
	public void surfaceCreated(SurfaceHolder mHolder) {
		if (mRecService != null && firstSurfaceView.getHolder() == mHolder) {
			Log.i(TAG, "surfaceCreated() first preview show");
			isFirstSurfaceReady = true;
			firstSurfaceHolder = mHolder;
			mRecService.setPreviewDisplay(firstSurfaceShowId, firstSurfaceHolder);
			if (!mRecService.isPreview(firstSurfaceShowId)) {
				mRecService.startPreview(firstSurfaceShowId);
			}
		}else if(mRecService != null && secondSurfaceView.getHolder() == mHolder) {
			Log.i(TAG, "surfaceCreated() second preview show");
			isSecondSurfaceReady = true;
			secondSurfaceHolder = mHolder;
			mRecService.setPreviewDisplay(secondSurfaceShowId, secondSurfaceHolder);
			if (!mRecService.isPreview(secondSurfaceShowId)) {
				mRecService.startPreview(secondSurfaceShowId);
			}
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder mHolder) {
		if (firstSurfaceView.getHolder() == mHolder) {
			Log.i(TAG, "surfaceDestroyed() first preview hide");
			isFirstSurfaceReady = false;
			firstSurfaceHolder = null;
		}else if(secondSurfaceView.getHolder() == mHolder) {
			Log.i(TAG, "surfaceDestroyed() second preview hide");
			isSecondSurfaceReady = false;
			secondSurfaceHolder = null;
		}
	}
	
	
	private void updateSurfaceView(int id) {
		Log.i(TAG, "updateSurfaceView()  "+"firstSurfaceShowId :"+firstSurfaceShowId+"    secondSurfaceShowId :"+secondSurfaceShowId);
		Log.i(TAG, "updateSurfaceView()  isFirstSurfaceReady :"+isFirstSurfaceReady+"     isSecondSurfaceReady :"+isSecondSurfaceReady+"   mRecService :"+mRecService);
		if (id == 0 && isFirstSurfaceReady && null != mRecService) {
			mRecService.setPreviewDisplay(firstSurfaceShowId, firstSurfaceHolder);
			Log.i("deng", "first  updateSurfaceView()=========");
			if (!mRecService.isPreview(firstSurfaceShowId)) {
				mRecService.startPreview(firstSurfaceShowId);
			}
			mRecService.startRender(firstSurfaceShowId);
		}else if(id == 1 && isSecondSurfaceReady && null != mRecService) {
			mRecService.setPreviewDisplay(secondSurfaceShowId, secondSurfaceHolder);
			Log.i("deng", "second  updateSurfaceView()=========");
			if (!mRecService.isPreview(secondSurfaceShowId)) {
				mRecService.startPreview(secondSurfaceShowId);
			}
			mRecService.startRender(secondSurfaceShowId);
		}
	}
	long curTime = 0;
	Toast mToast;
	View.OnClickListener firstViewClick = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			handleFirstViewClick();
//			if (Math.abs(System.currentTimeMillis() - curTime) > 300) {
//				handleFirstViewClick();
//			} else {
//				if(mToast == null) {
//					mToast = Toast.makeText(mRecService, "你操作太快了", Toast.LENGTH_SHORT);
//				}else {
//					mToast.cancel();
//				}
//				mToast.show();
//			}
//			curTime = System.currentTimeMillis();
			
		}
	};
	
	View.OnClickListener secondViewClick = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			handleSecondViewClick();
//			if (Math.abs(System.currentTimeMillis() - curTime) > 300) {
//				handleSecondViewClick();
//			} else {
//				if(mToast == null) {
//					mToast = Toast.makeText(mRecService, "你操作太快了", Toast.LENGTH_SHORT);
//				}else {
//					mToast.cancel();
//				}
//				mToast.show();
//			}
//			curTime = System.currentTimeMillis();
			
		}
	};
	private void handleFirstViewClick() {
		getComePackageName();
		if (isCameraOn) {
			if (firstImageView.getVisibility() == View.VISIBLE) {
				firstSurfaceShowId = 0;
				showSurfaceView(0);
				exitCameraPreview();
			}else if(secondImageView.getVisibility() == View.VISIBLE) {
				showAppIcon(0);//上图标
				showSurfaceView(1);
				secondSurfaceShowId = 2;
				showCameraPreview(1);//后
				updateSurfaceView(1);//下右
			}else{
				if (firstSurfaceShowId == 1) {
					firstSurfaceShowId = 0;
					showCameraPreview(0);//显示前路
				}else if(firstSurfaceShowId == 0) {//后
					if (secondSurfaceShowId == 1) {//前
						firstSurfaceShowId = 1;//前
						secondSurfaceShowId = 2;//右
						updateSurfaceView(1);
						showCameraPreview(1);////后
						//显示右路
						//下面悬浮显示前路
					}else {
						firstSurfaceShowId = 1;
						showCameraPreview(1);//显示后路
					}
				}
				showSurfaceView(0);
				updateSurfaceView(0);
			}
			
		}else{
			if (isLuncher) {
				drawable = mRecService.getResources().getDrawable(R.drawable.launcher);
				firstImageView.setImageDrawable(drawable);
				secondImageView.setImageDrawable(drawable);
				if(secondImageView.getVisibility() == View.VISIBLE) {
					secondSurfaceShowId = 2;
					showSurfaceView(1);
					updateSurfaceView(1);
				}
			}else {
				drawable = mRecService.getAppIconByPackageName(packeNames);
				firstImageView.setImageDrawable(drawable);
				secondImageView.setImageDrawable(drawable);
			}
			firstSurfaceShowId = 0;
			showAppIcon(0);
			startRecordActivity(0);
		}
		
	}
	
	private void startRecordActivity(int mStartId) {
		Log.i(TAG, "startRecordActivity()"+"  mStartId = "+mStartId);
		Intent intent = new Intent(mRecService,RecorderActivity.class);
		intent.putExtra(MyBroadcastReceiver.POWER_ON_START, true);
		intent.putExtra(RecorderActivity.EXTRA_CAM_TYPE, mStartId);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		mRecService.startActivity(intent);
	}
	
	private void showCameraPreview(int mStartId) {
		Intent broadcast = new Intent();
		broadcast.setAction(ACTION_SWITCH_FRAGMENT);
		broadcast.putExtra(RecordService.EXTRA_CAM_TYPE, mStartId);
		mRecService.sendBroadcast(broadcast);
	}
	
	private void exitCameraPreview() {
		Intent finishIntent = new Intent();
		finishIntent.setAction(RecorderActivity.ACTION_FINISH);
		mRecService.sendBroadcast(finishIntent);
	}
	
	private void handleSecondViewClick() {
		getComePackageName();
		if (isCameraOn) {
			if (secondImageView.getVisibility() == View.VISIBLE) {
				secondSurfaceShowId = 2;
				showSurfaceView(1);
				exitCameraPreview();
			}else if(firstImageView.getVisibility() == View.VISIBLE) {
				showSurfaceView(0);
				showAppIcon(1);//下图标
				showCameraPreview(10);//左显示右路
				firstSurfaceShowId = 0;
				updateSurfaceView(0);//上显示后路
			}else {
				if (secondSurfaceShowId == 1) {//前路
					secondSurfaceShowId = 2;//右路
					showCameraPreview(0);//前路
					//右边显示前路
					
				}else if(secondSurfaceShowId == 2) {
					if (firstSurfaceShowId == 1) {
						firstSurfaceShowId = 0;//后路
						secondSurfaceShowId = 1;
						//上面显示后路
						updateSurfaceView(0);
						showCameraPreview(10);//右边显示右路
					}else {
						secondSurfaceShowId = 1;
						//右边显示右路
						showCameraPreview(10);
					}
				}
				showSurfaceView(1);
				updateSurfaceView(1);
			}
			
		}else{
			if (isLuncher) {
				drawable = mRecService.getResources().getDrawable(R.drawable.launcher);
				secondImageView.setImageDrawable(drawable);
				firstImageView.setImageDrawable(drawable);
				if(firstImageView.getVisibility() == View.VISIBLE) {
					firstSurfaceShowId = 0;
					showSurfaceView(0);
					updateSurfaceView(0);
				}
			}else {
				drawable = mRecService.getAppIconByPackageName(packeNames);
				secondImageView.setImageDrawable(drawable);
				firstImageView.setImageDrawable(drawable);
			}
			secondSurfaceShowId = 2;
			showAppIcon(1);
			startRecordActivity(2);
		}
		
	}
	private void getComePackageName() {
		RecentTaskInfo taskInfo = SplitUtil.getTopTask(mRecService);
		if (null != taskInfo) {
			packeNames = taskInfo.baseIntent.getComponent().getPackageName();
			Log.i(TAG, "getComePackageName() packeNames = " + packeNames);
			if (packeNames != null) {
				if ("com.zqc.launcher".equals(packeNames)) {
					isLuncher = true;
				} else {
					isLuncher = false;
				}
				if (RecordService.PACKAGE_NAME.equals(packeNames)) {
					isCameraOn = true;
				} else {
					isCameraOn = false;
				}
			}
		}
		Log.d(TAG, "isCameraOn: " + isCameraOn + "  packeNames: " + packeNames + "  isLuncher: " + isLuncher+"  isShowAppIcon :"+isShowAppIcon);
	}

	public void addImageView(ImageView view, int id) {
		
	}


	public void hideTwoWindow() {
		Log.i(TAG, "hideTwoWindow");
		if (null != rootView) {
			rootView.setVisibility(View.GONE);
		}
	}

	public void showTwoWindow() {
		Log.i(TAG, "showTwoWindow");
		if (null != rootView) {
			rootView.setVisibility(View.VISIBLE);
		}
		if(firstImageView != null && firstImageView.getVisibility() != View.VISIBLE) {
			showSurfaceView(0);
			updateSurfaceView(0);
		}
		if(secondImageView != null && secondImageView.getVisibility() != View.VISIBLE)
		   showSurfaceView(1);
		   updateSurfaceView(1);
	}

	public void restoreTwoWindow() {
		Log.i(TAG, "restoreTwoWindow");
		if (null != rootView) {
			rootView.setVisibility(View.VISIBLE);
		}
		firstSurfaceShowId = 0;
		secondSurfaceShowId = 2;
		showSurfaceView(0);
		updateSurfaceView(0);
		showSurfaceView(1);
		updateSurfaceView(1);
//		if(firstSurfaceShowId != 0) {
//			firstSurfaceShowId = 0;
//			showSurfaceView(0);
//			updateSurfaceView(0);
//		}
//		if(secondSurfaceShowId != 2) {
//			secondSurfaceShowId = 2;
//			showSurfaceView(1);
//			updateSurfaceView(1);
//		}
	}
	public void hideSurfaceView(int surfaceId) {
		Log.i(TAG, "hideSurfaceView()"+"surfaceId "+surfaceId);
		if (surfaceId == 0) {
			firstSurfaceView.setVisibility(View.GONE);
		} else if (surfaceId == 1) {
			secondSurfaceView.setVisibility(View.GONE);
		}
	}

	public void showSurfaceView(int surfaceId) {
		Log.i(TAG, "showSurfaceView()"+"surfaceId :"+surfaceId);
		if (surfaceId == 0) {
			firstImageView.setVisibility(View.GONE);
			firstSurfaceView.setVisibility(View.VISIBLE);
		} else if (surfaceId == 1) {
			secondImageView.setVisibility(View.GONE);
			secondSurfaceView.setVisibility(View.VISIBLE);
		}
	}
	
	public void showAppIcon(int surfaceId) {
		Log.i(TAG, "showAppIcon()"+"surfaceId :"+surfaceId);
		if (surfaceId == 0) {
			firstImageView.setVisibility(View.VISIBLE);
			firstSurfaceView.setVisibility(View.GONE);
		} else if (surfaceId == 1) {
			secondImageView.setVisibility(View.VISIBLE);
			secondSurfaceView.setVisibility(View.GONE);
		}
	}

	public void unregisterReceiver() {
		if (receiver != null) {
			mRecService.unregisterReceiver(receiver);
		}
	}
	public void hide(int id) {
		if (firstLayout != null) {
			firstLayout.setVisibility(View.GONE);
		}
	}

	public void show(int id) {
		Log.i(TAG, "show() id = "+id);
		if (secondLayout != null) {
			secondLayout.setVisibility(View.VISIBLE);
		}
	}
	
	private void resetWindow() {
		if (firstImageView != null && firstImageView.getVisibility() != View.VISIBLE && secondImageView != null
				&& secondImageView.getVisibility() != View.VISIBLE) {
			restoreTwoWindow();
		}else {
			if(firstImageView != null && firstImageView.getVisibility() == View.VISIBLE) {
				showSurfaceView(0);
				firstSurfaceShowId = 0;
				updateSurfaceView(0);
			}
			if(secondImageView != null && secondImageView.getVisibility() == View.VISIBLE) {
				Log.i("deng", "second visible");
				showSurfaceView(1);
				secondSurfaceShowId = 2;
				updateSurfaceView(1);
			}
		}
	}
	private class Receiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			if (ACTION_FULL_CAMERA_HIDE.equals(intent.getAction())) {
				showTwoWindow();//全屏预览返回
			} else if (ACTION_FULL_CAMERA_SHOW.equals(intent.getAction())) {
				hideTwoWindow();
			}else if(HOME_TWO_WINDOW_ACTION.equals(intent.getAction())) {
				//home键
				if((firstSurfaceShowId != 0) || (secondSurfaceShowId != 2) 
						&& (firstImageView != null && firstImageView.getVisibility() != View.VISIBLE)
						&& (secondImageView != null&& secondImageView.getVisibility() != View.VISIBLE)) {
					if (null != firstSurfaceView) {
						firstSurfaceView.setVisibility(View.GONE);
					}
					if (null != secondSurfaceView) {
						secondSurfaceView.setVisibility(View.GONE);
					}
				}
				if(((firstSurfaceShowId != 0) || (secondSurfaceShowId != 2)) || ((firstImageView != null && firstImageView.getVisibility() == View.VISIBLE)
						|| (secondImageView != null&& secondImageView.getVisibility() == View.VISIBLE))) {
					
					showCameraPreview(0);//显示前路
					
				}
				resetWindow();
			}else if(BACK_WINDOW_ACTION.equals(intent.getAction())) {
				//RecordActivity返回键
                Log.d(TAG, "onReceive:firstSurfaceShowId "+firstSurfaceShowId
                +" secondSurfaceShowId "+secondSurfaceShowId);
				if (firstSurfaceShowId != 0 || secondSurfaceShowId != 2 && firstImageView != null && firstImageView.getVisibility() != View.VISIBLE && secondImageView != null
						&& secondImageView.getVisibility() != View.VISIBLE) {
					if (null != firstSurfaceView) {
                        firstSurfaceView.setVisibility(View.GONE);
                        Log.d(TAG, "onReceive:firstSurfaceView!=null ");
					}
					if (null != secondSurfaceView) {
						secondSurfaceView.setVisibility(View.GONE);
                        Log.d(TAG, "onReceive:secondSurfaceView!=null ");
					}
				}	
			    resetWindow();
			}
		}
	}
}
