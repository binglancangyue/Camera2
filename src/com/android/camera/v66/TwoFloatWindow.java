package com.android.camera.v66;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import com.android.camera2.R;

public class TwoFloatWindow implements SurfaceTextureListener {

	private static final String TAG = "TwoFloatWindow";
	private RecordService mRecService;
	private WindowManager mWindowManager;
	private TextureView /*mLeftSurfaceView,*/ mRightSurfaceView;
	private SurfaceTexture /*mLeftSurfaceTexture,*/ mRightSurfaceTexture;
	private RelativeLayout leftWindow, rightWindow;
	private ImageView ivRecordIcon;
	private LayoutInflater layoutInflater;
//	public static final int LEFT_CAMERA_ID = 2;
	//public static final int RIGHT_CAMERA_ID = 3;
	public static final int RIGHT_CAMERA_ID = 2;
    public static final int LEFT_CAMERA_ID = 3;
	public static final String KEY_VOICE_CAMERA_ID = "CameraId";
	private WindowManager.LayoutParams leftParams,rightParams;
	private static final int WINDOW_LESS_WIDTH = 290;
	private static final int WINDOW_MORE_WIDTH = 490;
	private boolean isShow = false;
	private boolean isRecording = false;

	public TwoFloatWindow(RecordService service) {

		if (null == service) {
			return;
		} else {
			this.mRecService = service;
		}
		mWindowManager = (WindowManager) mRecService.getApplication().getSystemService(Context.WINDOW_SERVICE);
		layoutInflater = LayoutInflater.from(mRecService);
		initWindow();
		registerBroadcast();
	}

	public void initWindow() {
		Log.i(TAG, "initWindow()");
		int leftFoatWinWidth = WINDOW_LESS_WIDTH;
		int leftFoatWinHeight = 400;
		int rightFoatWinWidth = WINDOW_LESS_WIDTH;
		int rightFoatWinHeight = 400;
		leftWindow = (RelativeLayout) layoutInflater.inflate(R.layout.left_float_win, null);
		leftParams = new WindowManager.LayoutParams();
		leftParams.type = LayoutParams.TYPE_SYSTEM_ERROR;
		leftParams.format = PixelFormat.RGBA_8888;
		leftParams.flags = LayoutParams.FLAG_NOT_FOCUSABLE | LayoutParams.FLAG_HARDWARE_ACCELERATED;
		leftParams.gravity = Gravity.LEFT;
		leftParams.width = leftFoatWinWidth;
		leftParams.height = leftFoatWinHeight;
		leftParams.windowAnimations = R.style.LeftWindowAnination;
		/*mLeftSurfaceView = (TextureView) leftWindow.findViewById(R.id.left_preview);
		mLeftSurfaceView.setSurfaceTextureListener(this);*/
		ivRecordIcon = (ImageView) leftWindow.findViewById(R.id.iv_record);
		mWindowManager.addView(leftWindow, leftParams);

		rightWindow = (RelativeLayout) layoutInflater.inflate(R.layout.right_float_win, null);
		rightParams = new WindowManager.LayoutParams();
		rightParams.type = LayoutParams.TYPE_SYSTEM_ERROR;
		rightParams.format = PixelFormat.RGBA_8888;
		rightParams.flags = LayoutParams.FLAG_NOT_FOCUSABLE | LayoutParams.FLAG_HARDWARE_ACCELERATED;
		rightParams.gravity = Gravity.RIGHT;
		rightParams.width = rightFoatWinWidth;
		rightParams.height = rightFoatWinHeight;
		rightParams.windowAnimations = R.style.RightWindowAnination;
		mRightSurfaceView = (TextureView) rightWindow.findViewById(R.id.right_preview);
		mRightSurfaceView.setSurfaceTextureListener(this);
		leftWindow.setOnTouchListener(new OnTouchListener() {
			int downX = 0;
			int moveX = 0;
			int upX = 0;
			boolean isShow = false;
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					isShow = true;
					downX = (int) event.getX();
//					tempOrigLeftWidth = leftParams.width;
					break;
				case MotionEvent.ACTION_MOVE:
					moveX = (int) event.getX();
					if (downX - moveX >50) {
						if (isShow) {
							isShow = false;
							if (windowStatus == 2) {
//								showLessWindow();
							}else if(windowStatus == 1){
								hideWindow();
							}
						}
					}
					if (moveX - downX >50) {
						if (isShow) {
							if (isShow) {
								isShow = false;
								if (windowStatus == 1) {
//									showMoreWindow();
									showFullScreen();
								}
							}
						}
					}
//					updateWindowWidth(moveX - downX);
					
					break;
				case MotionEvent.ACTION_UP:
//					upX = (int) event.getX();
//					updateUpWindowWidth(upX);
					break;
				default:
					break;
				}
				return false;
			}
		});
		rightWindow.setOnTouchListener(new OnTouchListener() {
			int downX = 0;
			int moveX = 0;
			int upX = 0;
			boolean isShow = false;
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					isShow = true;
					downX = (int) event.getX();
					tempOrigLeftWidth = leftParams.width;
					break;
				case MotionEvent.ACTION_MOVE:
					moveX = (int) event.getX();
					if (moveX - downX >50) {
						if (isShow) {
							isShow = false;
							if (windowStatus == 2) {
//								showLessWindow();
							}else if(windowStatus == 1){
								hideWindow();
							}
						}
					}
					
					if (downX - moveX >50) {
						if (isShow) {
							isShow = false;
							if (windowStatus == 1) {
//								showMoreWindow();
								showFullScreen();
							}
						}
					}
//					updateWindowWidth(downX - moveX);
					
					break;
				case MotionEvent.ACTION_UP:
//					upX = (int) event.getX();
//					updateUpWindowWidth(upX);
					break;
				default:
					break;
				}
				return false;
			}
		});
		mWindowManager.addView(rightWindow, rightParams);
	}
	
	private static final int MSG_UPDATE_WINDOW = 1;
	private Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_UPDATE_WINDOW:
				if (null != mWindowManager) {
					if (null != leftWindow) {
						mWindowManager.updateViewLayout(leftWindow, leftParams);
					}
					if (null != rightWindow) {
						mWindowManager.updateViewLayout(rightWindow, rightParams);
					}
				}
				break;

			default:
				break;
			}
		};
	};
	
	private int tempLeftWidth = 0;
	private int tempOrigLeftWidth = 0;
	private void updateWindowWidth(int x){
		if (tempOrigLeftWidth >= WINDOW_LESS_WIDTH && tempOrigLeftWidth <= WINDOW_MORE_WIDTH) {
			tempLeftWidth = tempOrigLeftWidth + x;
			if (tempLeftWidth <= WINDOW_LESS_WIDTH) {
				windowStatus = 1;
				tempLeftWidth = WINDOW_LESS_WIDTH;
			}else if(tempLeftWidth >= WINDOW_MORE_WIDTH){
				windowStatus = 2;
				tempLeftWidth = WINDOW_MORE_WIDTH;
			}
			leftParams.width = tempLeftWidth;
			rightParams.width = tempLeftWidth;
			mHandler.sendEmptyMessage(MSG_UPDATE_WINDOW);
		}else{
			
		}
	}
	
	private void updateUpWindowWidth(int x){
		tempLeftWidth = leftParams.width;
		if (tempLeftWidth > WINDOW_LESS_WIDTH) {
			if (tempLeftWidth >= WINDOW_LESS_WIDTH +(WINDOW_MORE_WIDTH-WINDOW_LESS_WIDTH)/2) {
				leftParams.width = WINDOW_MORE_WIDTH;
				rightParams.width = WINDOW_MORE_WIDTH;
				windowStatus = 2;
			}else{
				leftParams.width = WINDOW_LESS_WIDTH;
				rightParams.width = WINDOW_LESS_WIDTH;
				windowStatus = 1;
			}
			mHandler.sendEmptyMessage(MSG_UPDATE_WINDOW);
		}
	}
	
	private int windowStatus = -1;
	
	private void showFullScreen(){
		Log.e(TAG, "showFullScreen");
		
		hideWindow();
		if (null != mRecService) {
			mRecService.showFullscreenCameraInner(FullScreenCameraActivity.STATE_FULLSCREEN_SHOW_CAMERA_LEFT_RIGHT, false, false);
		}
	}
	private void showMoreWindow(){
		windowStatus = 2;
		leftParams.width = WINDOW_MORE_WIDTH;
		rightParams.width = WINDOW_MORE_WIDTH;
		mWindowManager.updateViewLayout(leftWindow, leftParams);
		mWindowManager.updateViewLayout(rightWindow, rightParams);
	}
	
	private void showLessWindow(){
		windowStatus = 1;
		leftParams.width = WINDOW_LESS_WIDTH;
		rightParams.width = WINDOW_LESS_WIDTH;
		mWindowManager.updateViewLayout(leftWindow, leftParams);
		mWindowManager.updateViewLayout(rightWindow, rightParams);
	}

	public void showWindow() {
		Log.i(TAG, "showWindow()");
		isShow = true;
		if (null != leftWindow) {
			if (null != ivRecordIcon) {
				ivRecordIcon.setVisibility(isRecording ? View.VISIBLE : View.GONE);
			}
			leftWindow.setVisibility(View.VISIBLE);
		}
		if (null != rightWindow) {
			rightWindow.setVisibility(View.VISIBLE);
		}
		showLessWindow();
		windowStatus = 1;
	}

	public void hideWindow() {
		isShow = false;
		if (null != leftWindow) {
			leftWindow.setVisibility(View.GONE);
		}
		if (null != rightWindow) {
			rightWindow.setVisibility(View.GONE);
		}
		windowStatus = -1;

	}
	
	public boolean isShow(){
		return isShow;
	}
	
	
	public void setRecordStatus(boolean isRecord){
		Log.i(TAG, "setRecordStatus() isRecord = "+isRecord);
		this.isRecording = isRecord;
		if (null != ivRecordIcon) {
			ivRecordIcon.setVisibility(isRecording ? View.VISIBLE : View.GONE);
		}
	}


	private void registerBroadcast() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(StreamPreViewWindow.ACTION_STREAM_PREVIEW_WIDOW_HIDE);
		mRecService.registerReceiver(receiver, filter);

	}


	private void reloadPreview() {
	/*	if (null != mRecService && null != mLeftSurfaceTexture) {
			mRecService.setPreviewTexture(LEFT_CAMERA_ID, mLeftSurfaceTexture);
			if (!mRecService.isPreview(LEFT_CAMERA_ID)) {
				mRecService.startPreview(LEFT_CAMERA_ID);
			}
		}*/

		/*if (null != mRecService && null != mRightSurfaceTexture) {
			mRecService.setPreviewTexture(RIGHT_CAMERA_ID, mRightSurfaceTexture);
			if (!mRecService.isPreview(RIGHT_CAMERA_ID)) {
				mRecService.startPreview(RIGHT_CAMERA_ID);
			}
		}*/
	}

	private BroadcastReceiver receiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			Log.i(TAG, "onReceive() action = " + action);
			if (StreamPreViewWindow.ACTION_STREAM_PREVIEW_WIDOW_HIDE.equals(action)) {
				reloadPreview();
			}
		}
	};

	@Override
	public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
		/*if (mLeftSurfaceView.getSurfaceTexture() == surface) {
			if (null != mRecService) {
				mLeftSurfaceTexture =  surface;
				mRecService.setPreviewTexture(LEFT_CAMERA_ID, surface);
				if (!mRecService.isPreview(LEFT_CAMERA_ID)) {
					mRecService.startPreview(LEFT_CAMERA_ID);
				}
				mRecService.startRender(LEFT_CAMERA_ID);
			}
		} else */
		/* remove by zdt
		 * if (mRightSurfaceView.getSurfaceTexture() == surface) {
			if (null != mRecService) {
				mRightSurfaceTexture = surface;
				mRecService.setPreviewTexture(RIGHT_CAMERA_ID, surface);
				if (!mRecService.isPreview(RIGHT_CAMERA_ID)) {
					mRecService.startPreview(RIGHT_CAMERA_ID);
				}
				mRecService.startRender(RIGHT_CAMERA_ID);
			}
		}*/
	}

	@Override
	public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
		
	}

	@Override
	public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
		/*if (null != mLeftSurfaceTexture && mLeftSurfaceTexture == surface) {
			mRecService.stopRender(LEFT_CAMERA_ID);
			mLeftSurfaceTexture = null;
		}*/
		/*if (null != mRightSurfaceTexture && mRightSurfaceTexture == surface) {
			mRecService.stopRender(RIGHT_CAMERA_ID);
			mRightSurfaceTexture = null;
		}*/
		return false;
	}

	@Override
	public void onSurfaceTextureUpdated(SurfaceTexture surface) {
		
	}

}
