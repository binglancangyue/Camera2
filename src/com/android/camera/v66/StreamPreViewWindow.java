package com.android.camera.v66;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera.CameraInfo;
import android.os.Handler;
import android.os.SystemProperties;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Scroller;
import android.widget.TextView;
import android.widget.Toast;

import com.android.camera.v66.ObserveScrollView.ScrollListener;
import com.android.camera2.R;

public class StreamPreViewWindow implements SurfaceTextureListener {

	private static final String TAG = "StreamPreViewWindow";
	private static final boolean DEBUG = false;
	public static final String ACTION_STREAM_PREVIEW_WIDOW_SHOW = "com.action.stream_preview_window_show";
	public static final String ACTION_STREAM_PREVIEW_WIDOW_HIDE = "com.action.stream_preview_window_hide";
	
	public static final int ACTION_PRE_FRONT = 10;
	public static final int ACTION_PRE_BACK = 11;
	
	private RecordService mRecService;
	private WindowManager.LayoutParams wmParams;
	private WindowManager mWindowManager;
	private LayoutInflater inflater;
	private View rootLayout;
	private ImageView ivPreviewRecord,ivThreeRecord;
	private TextureView streamSurfaceView, streamBackSurfaceView, streamLeftSurfaceView,streamRightSurfaceView;
	private SurfaceHolder streamSurfaceHolder, streamBackSurfaceHolder, streamRightSurfaceHolder;
	private Button btnExit;
	private TextView tvFlag;
	private boolean isShow = false;
	private boolean isMove = false;
	private boolean isBackShow = false;
	private boolean isRecording = false;
	private OnClickListener preOnclick, backOnclick, leftOnclick,rightOnclick;
	private FrameLayout mPreviewContainer;
	private SharedPreferences preferences, mClickPre;
	private ObserveScrollView scrollView;
	private SharedPreferences scrollPref;
	private int yValue;
	private FullCameraBroadReceiver mReceiver;
	private Handler mHandler = new Handler();

	public static int SCREEN_RESOLUTION = SystemProperties.getInt("ro.sys.screen_resolution", 7);// 配置加载布局

	public StreamPreViewWindow(RecordService mRecService) {
		this.mRecService = mRecService;
		init();
	}

	private void init() {
		Log.i(TAG, "init()");
		if (null == mRecService) {
			Log.i(TAG, "mRecService is null !");
			return;
		}
		inflater = LayoutInflater.from(mRecService.getApplication());
		mWindowManager = (WindowManager) mRecService.getApplication().getSystemService(Context.WINDOW_SERVICE);
		wmParams = new WindowManager.LayoutParams();
		wmParams.type = LayoutParams.TYPE_SYSTEM_ERROR;
		// wmParams.type = LayoutParams.TYPE_DISPLAY_OVERLAY;
		wmParams.format = PixelFormat.RGBA_8888;
		wmParams.flags = LayoutParams.FLAG_NOT_FOCUSABLE | LayoutParams.FLAG_HARDWARE_ACCELERATED;
		wmParams.gravity = Gravity.TOP | Gravity.RIGHT;
		wmParams.x = 0;
		wmParams.y = 0;
		wmParams.width = WindowManager.LayoutParams.MATCH_PARENT;
		wmParams.height = WindowManager.LayoutParams.MATCH_PARENT;
		preferences = mRecService.getSharedPreferences("cameraPlug",
				Context.MODE_MULTI_PROCESS | Context.MODE_WORLD_READABLE);
		scrollPref = mRecService.getSharedPreferences("scollView", Context.MODE_PRIVATE);
		mClickPre = mRecService.getSharedPreferences("CameraID", Context.MODE_PRIVATE);
		initThreePreviewWindow();
		initReverseWindow();
//		doRegister();
	}

	public void showWindow() {
		showWindow(ACTION_PRE_FRONT);
	}
	
	public void showWindow(int action) {
		Log.i(TAG, "showWindow()");
		Log.d(TAG, "isBroadcase : " + isBroadcase + " ;mClickPre.getInt(CameraId, 1): "
				+ mClickPre.getInt("CameraId", 1) + " ;curShowCameraID : " + curShowCameraID);
		if (isShow) {
			Log.i(TAG, "stream preview window is already showed!");
			if (!isBroadcase) {
				return;
			}
			if (action == ACTION_PRE_BACK) {
				Log.i(TAG, "zdt --- ACTION_PRE_BACK, curShowCameraID: " + curShowCameraID + ", CAMERA_FACING_BACK: " + CameraInfo.CAMERA_FACING_BACK);
				if (curShowCameraID != CameraInfo.CAMERA_FACING_BACK) {
					showWhichCam(CameraInfo.CAMERA_FACING_BACK);
					curShowCameraID = CameraInfo.CAMERA_FACING_BACK;
				}
			} else {
				Log.i(TAG, "zdt --- ACTION_PRE_FRONT, curShowCameraID: " + curShowCameraID + ", CAMERA_FACING_FRONT: " + CameraInfo.CAMERA_FACING_FRONT);
				if (curShowCameraID != CameraInfo.CAMERA_FACING_FRONT) {
					showWhichCam(CameraInfo.CAMERA_FACING_FRONT);
					curShowCameraID = CameraInfo.CAMERA_FACING_FRONT;
				}
			}
			/*if (curShowCameraID != mClickPre.getInt("CameraId", 1)) {
				showWhichCam(mClickPre.getInt("CameraId", 1));
				curShowCameraID = mClickPre.getInt("CameraId", 1);
			} else {
				switch (curShowCameraID) {
				case 0:
					speakText("您已经打开后路了");
					break;
				case 1:
					speakText("您已经打开前路了");
					break;
				case 2:
					speakText("您已经打开右路了");
					break;
				default:
					break;
				}
			}*/
			//isBroadcase = false;
			return;
		}
		if (null != rootLayout) {
			rootLayout = null;
		}
		if (null == rootLayout) {
			Log.i(TAG, "null == rootLayout, SCREEN_RESOLUTION= " + SCREEN_RESOLUTION);
			rootLayout = inflater.inflate(R.layout.stream_preview_win_700, null);
			btnExit = (Button) rootLayout.findViewById(R.id.bt_pre_exit);
			tvFlag = (TextView) rootLayout.findViewById(R.id.tv_flag);
			ivPreviewRecord = (ImageView) rootLayout.findViewById(R.id.iv_preview_record);
			btnExit.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					Log.i(TAG, "---------------btn----onClick()--exit---");
					hideWindow();
				}
			});

			streamSurfaceView = (TextureView) rootLayout.findViewById(R.id.stream_preview);
			streamBackSurfaceView = (TextureView) rootLayout.findViewById(R.id.stream_backview);
			streamLeftSurfaceView = (TextureView) rootLayout.findViewById(R.id.stream_leftview);
			streamRightSurfaceView = (TextureView) rootLayout.findViewById(R.id.stream_rightview);
			mPreviewContainer = (FrameLayout) rootLayout.findViewById(R.id.framelayout);
			scrollView = (ObserveScrollView) rootLayout.findViewById(R.id.scrollview);
			scrollView.setScrollListener(new ScrollListener() {

				@Override
				public void scrollOritention(int l, int t, int oldl, int oldt) {
					// TODO Auto-generated method stub
					if (DEBUG)
						Log.i(TAG, "scrollView----- scrollOritention----yValue=---" + t);
					scrollPref.edit().putFloat("yValue", t).commit();
					scrollPref.edit().putBoolean("isMove", true).commit();
				}
			});
			switch (SCREEN_RESOLUTION) {
			// 4.5和5寸一样
			// 7和9寸一样
			// 6.86
			//
			//
			case 5://
				FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(854, 480);
				rootLayout.setLayoutParams(p);
				scrollView.setLayoutParams(p);
				mPreviewContainer.setLayoutParams(p);
				streamSurfaceView.setLayoutParams(p);
				streamBackSurfaceView.setLayoutParams(p);

				break;
			case 686://
				FrameLayout.LayoutParams p1 = new FrameLayout.LayoutParams(1280, 480);
				FrameLayout.LayoutParams p12 = new FrameLayout.LayoutParams(1280, 720);
				rootLayout.setLayoutParams(p12);
				scrollView.setLayoutParams(p1);
				mPreviewContainer.setLayoutParams(p12);
				streamSurfaceView.setLayoutParams(p12);
				streamBackSurfaceView.setLayoutParams(p12);
				break;
			case 784://
				FrameLayout.LayoutParams p2 = new FrameLayout.LayoutParams(1280, 400);
				FrameLayout.LayoutParams p21 = new FrameLayout.LayoutParams(1280, 720);
				rootLayout.setLayoutParams(p21);
				scrollView.setLayoutParams(p2);
				mPreviewContainer.setLayoutParams(p21);
				streamSurfaceView.setLayoutParams(p21);
				streamBackSurfaceView.setLayoutParams(p21);
				break;
			case 988://
				FrameLayout.LayoutParams p3 = new FrameLayout.LayoutParams(1600, 400);
				FrameLayout.LayoutParams p31 = new FrameLayout.LayoutParams(1600, 900);
				rootLayout.setLayoutParams(p31);
				scrollView.setLayoutParams(p3);
				mPreviewContainer.setLayoutParams(p31);
				streamSurfaceView.setLayoutParams(p31);
				streamBackSurfaceView.setLayoutParams(p31);
				FrameLayout.LayoutParams btParams = (android.widget.FrameLayout.LayoutParams) btnExit.getLayoutParams();
				btParams.topMargin = 330;
				btParams.leftMargin = 50;
				btnExit.setLayoutParams(btParams);
				break;
			case 7://
				FrameLayout.LayoutParams p4 = new FrameLayout.LayoutParams(1024, 600);
				FrameLayout.LayoutParams p41 = new FrameLayout.LayoutParams(1024, 600);
				rootLayout.setLayoutParams(p41);
				scrollView.setLayoutParams(p4);
				mPreviewContainer.setLayoutParams(p41);
				streamSurfaceView.setLayoutParams(p41);
				streamBackSurfaceView.setLayoutParams(p41);
				break;

			default:
				break;
			}

			preOnclick = new OnClickListener() {

				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					if (DEBUG)
						Log.i(TAG, "pre-----onClick()--ACTION_DOWN---");
					Log.i(TAG, "pre-----isCameraPlugIn" + preferences.getBoolean("isCameraPlugIn", false));

					/*if (preferences.getBoolean("isCameraPlugIn", false)) {
						if (mIsPreviewing && mIsBackPreviewing) {
							android.view.ViewGroup.LayoutParams lp = (android.view.ViewGroup.LayoutParams) streamSurfaceView
									.getLayoutParams();
							streamSurfaceView.setLayoutParams(streamBackSurfaceView.getLayoutParams());
							streamBackSurfaceView.setLayoutParams(lp);
							streamBackSurfaceView.setOnClickListener(backOnclick);
							mPreviewContainer.bringChildToFront(streamBackSurfaceView);
							streamSurfaceView.setOnClickListener(null);
							tvFlag.setText(R.string.back_camera_text);
							curShowCameraID = 1;
						}
					} else {
						btnExit.performClick();
						Toast.makeText(mRecService, mRecService.getString(R.string.please_insert_back_cam),
								Toast.LENGTH_LONG).show();
					}*/
					if (CustomValue.CAMERA_NOT_RECORD||CustomValue.ONLY_ONE_CAMERA) {
						return;
					}
					if (RecorderActivity.CAMERA_COUNT == 2 && mIsPreviewing && mIsBackPreviewing) {
						android.view.ViewGroup.LayoutParams lp = (android.view.ViewGroup.LayoutParams) streamSurfaceView
								.getLayoutParams();
						streamSurfaceView.setLayoutParams(streamBackSurfaceView.getLayoutParams());
						streamBackSurfaceView.setLayoutParams(lp);
						streamBackSurfaceView.setOnClickListener(backOnclick);
						mPreviewContainer.bringChildToFront(streamBackSurfaceView);
						streamSurfaceView.setOnClickListener(null);
						tvFlag.setText(R.string.back_camera_text);
						curShowCameraID = 0;
					}
				}
			};
			backOnclick = new OnClickListener() {

				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					if (DEBUG)
						Log.i(TAG, "back-----onClick()--ACTION_DOWN---");
					/*if (mIsRightPreviewing && mIsBackPreviewing) {
						android.view.ViewGroup.LayoutParams lp = (android.view.ViewGroup.LayoutParams) streamSurfaceView
								.getLayoutParams();
						streamBackSurfaceView.setLayoutParams(streamLeftSurfaceView.getLayoutParams());
						streamLeftSurfaceView.setLayoutParams(lp);
						mPreviewContainer.bringChildToFront(streamLeftSurfaceView);
						streamBackSurfaceView.setOnClickListener(null);
						streamLeftSurfaceView.setOnClickListener(leftOnclick);
						tvFlag.setText(R.string.left_camera_text);
						curShowCameraID = 0;
					}*/
					if (mIsPreviewing && mIsBackPreviewing) {
						android.view.ViewGroup.LayoutParams lp = (android.view.ViewGroup.LayoutParams) streamSurfaceView
								.getLayoutParams();
						streamBackSurfaceView.setLayoutParams(streamSurfaceView.getLayoutParams());
						streamSurfaceView.setLayoutParams(lp);
						mPreviewContainer.bringChildToFront(streamSurfaceView);
						streamBackSurfaceView.setOnClickListener(null);
						streamSurfaceView.setOnClickListener(preOnclick);
						tvFlag.setText(R.string.front_camera_text);
						curShowCameraID = 1;
					}
				}
			};
			
			leftOnclick = new OnClickListener() {

				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					if (DEBUG)
						Log.i(TAG, "left-----onClick()--ACTION_DOWN---");
					if (mIsLeftPreviewing && mIsRightPreviewing) {
						android.view.ViewGroup.LayoutParams lp = (android.view.ViewGroup.LayoutParams) streamSurfaceView
								.getLayoutParams();
						streamLeftSurfaceView.setLayoutParams(streamRightSurfaceView.getLayoutParams());
						streamRightSurfaceView.setLayoutParams(lp);
						mPreviewContainer.bringChildToFront(streamRightSurfaceView);
						streamLeftSurfaceView.setOnClickListener(null);
						streamRightSurfaceView.setOnClickListener(rightOnclick);
						tvFlag.setText(R.string.right_camera_text);
						curShowCameraID = 2;
					}
				}
			};

			rightOnclick = new OnClickListener() {

				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					if (DEBUG)
						Log.i(TAG, "right-----onClick()--ACTION_DOWN---");
					if (mIsRightPreviewing && mIsPreviewing) {
						android.view.ViewGroup.LayoutParams lp = (android.view.ViewGroup.LayoutParams) streamSurfaceView
								.getLayoutParams();
						streamRightSurfaceView.setLayoutParams(streamSurfaceView.getLayoutParams());
						streamSurfaceView.setLayoutParams(lp);
						mPreviewContainer.bringChildToFront(streamSurfaceView);
						streamRightSurfaceView.setOnClickListener(null);
						streamSurfaceView.setOnClickListener(preOnclick);
						tvFlag.setText(R.string.front_camera_text);
						curShowCameraID = 3;
					}
				}
			};

			if (RecorderActivity.CAMERA_COUNT == 2) {
				streamSurfaceView.setOnClickListener(preOnclick);
				streamBackSurfaceView.setOnClickListener(backOnclick);
				streamLeftSurfaceView.setOnClickListener(leftOnclick);
				streamRightSurfaceView.setOnClickListener(rightOnclick);
			}

			streamSurfaceView.setSurfaceTextureListener(this);
			streamBackSurfaceView.setSurfaceTextureListener(this);
			streamLeftSurfaceView.setSurfaceTextureListener(this);
			streamRightSurfaceView.setSurfaceTextureListener(this);
		}
		if (DEBUG)
			Log.i(TAG, "null != rootLayout");

		// if (!mRecService.isPreview(CameraInfo.CAMERA_FACING_FRONT)) {
		// }
		if (mTextureIsUp && !mIsPreviewing) {
			mRecService.setPreviewTexture(CameraInfo.CAMERA_FACING_FRONT, streamSurfaceView.getSurfaceTexture());
			mRecService.startPreview(CameraInfo.CAMERA_FACING_FRONT);
		}
		// if (!mRecService.isPreview(CameraInfo.CAMERA_FACING_BACK)) {
		// }
		if (mTextureBackIsUp && !mIsBackPreviewing) {
			mRecService.setPreviewTexture(CameraInfo.CAMERA_FACING_BACK, streamBackSurfaceView.getSurfaceTexture());
			mRecService.startPreview(CameraInfo.CAMERA_FACING_BACK);
		}
		/*if (mTextureLeftIsUp && !mIsLeftPreviewing) {
			mRecService.setPreviewTexture(TwoFloatWindow.LEFT_CAMERA_ID, streamLeftSurfaceView.getSurfaceTexture());
			mRecService.startPreview(TwoFloatWindow.LEFT_CAMERA_ID);
		}*/

		/*if (mTextureRightIsUp && !mIsRightPreviewing) {
			mRecService.setPreviewTexture(TwoFloatWindow.LEFT_CAMERA_ID, streamRightSurfaceView.getSurfaceTexture());
			mRecService.startPreview(TwoFloatWindow.LEFT_CAMERA_ID);
		}*/
		if (action == ACTION_PRE_BACK) {
			curShowCameraID = CameraInfo.CAMERA_FACING_BACK;
			Log.i(TAG, "mClickPre.getInt(CameraId, 1)--------=" + mClickPre.getInt("CameraId", 1) + " , curShowCameraID: " + curShowCameraID);
		} else {
			//curShowCameraID = mClickPre.getInt("CameraId", 1);
			curShowCameraID = CameraInfo.CAMERA_FACING_FRONT;
			Log.i(TAG, "mClickPre.getInt(CameraId, 1)--------=" + mClickPre.getInt("CameraId", 1));
		}
		showWhichCam(curShowCameraID);
		// tvFlag.setText("前");
		// tvFlag.setText(R.string.front_camera_text);
		mWindowManager.addView(rootLayout, wmParams);
		scrollView.post(new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				if (scrollPref.getBoolean("isMove", false)) {
					yValue = (int) scrollPref.getFloat("yValue", 200);
					scrollView.smoothScrollTo(0, yValue);
					if (DEBUG)
						Log.i(TAG, "-------scrollView  run()-----yValue---=" + yValue);
				} else {
					scrollView.smoothScrollTo(0, 200);
					if (DEBUG)
						Log.i(TAG, "---else----scrollView  run()-----yValue---=" + yValue);
				}
			}
		});
		if (null != ivPreviewRecord) {
			ivPreviewRecord.setVisibility(isRecording ? View.VISIBLE : View.GONE);
		}
		isShow = true;
		mRecService.sendBroadcast(new Intent(ACTION_STREAM_PREVIEW_WIDOW_SHOW));
	}

	public void hideWindow() {
		Log.i(TAG, "hideWindow()");
		Log.d(TAG, "isCloseBroadcase : " + isCloseBroadcase + " ;mClickPre.getInt(CameraId, 1): "
				+ mClickPre.getInt("CameraId", 1) + " ;curShowCameraID : " + curShowCameraID);
		if (!isShow) {
			Log.i(TAG, "stream preview window is already hided!");
			if (isCloseBroadcase) {
				switch (mClickPre.getInt("CameraId", 1)) {
				case 1:
					speakText("您没有打开前路");
					break;

				case 0:
					speakText("您没有打开后路");
					break;
				case 2:
					speakText("您没有打开右路");
					break;

				default:
					break;
				}
				isCloseBroadcase = false;
			}
		} else {
			if (isCloseBroadcase) {
				if (curShowCameraID != mClickPre.getInt("CameraId", 1)) {
					switch (mClickPre.getInt("CameraId", 1)) {
					case 1:
						speakText("您没有打开前路");
						break;

					case 0:
						speakText("您没有打开后路");
						break;
					case 2:
						speakText("您没有打开右路");
						break;

					default:
						break;
					}
					isCloseBroadcase = false;
					return;
				} else {
					switch (mClickPre.getInt("CameraId", 1)) {
					case 1:
						speakText("已为您关闭前路");
						break;

					case 0:
						speakText("已为您关闭后路");
						break;
					case 2:
						speakText("已为您关闭右路");
						break;

					default:
						break;
					}
				}
				isCloseBroadcase = false;
			}
		}
		if (streamSurfaceView != null) {
			Log.i(TAG, "mWindowManager.removeView(rootLayout)()");
			mWindowManager.removeView(rootLayout);
		} else {
			Log.i(TAG, "else stream preview window is already hided!");
		}
		isShow = false;
		streamSurfaceView = null;
		streamBackSurfaceView = null;
		mPreviewContainer = null;
		btnExit = null;
		isMove = false;
		mRecService.sendBroadcast(new Intent(ACTION_STREAM_PREVIEW_WIDOW_HIDE));
		mRecService.sendBroadcast(new Intent("com.zqc.action.HIDE_STREAM_PREVIEW_WINDOW"));
		mRecService.sendBroadcast(new Intent("com.zqc.action.screen.out.close"));
	}

	public void closeWindow() {
		Log.i(TAG, "closeWindow()");
		if (!isShow) {
			Log.i(TAG, "stream preview window is already close!");
		}
		if (streamSurfaceView != null) {
			mWindowManager.removeView(rootLayout);
		}
		isShow = false;
	}

	public boolean isShow() {
		return isShow;
	}

	public void setRecordStatus(boolean isRecord){
		Log.i(TAG, "setRecordStatus() isRecord = "+isRecord);
		this.isRecording = isRecord;
		if (null != ivPreviewRecord) {
			ivPreviewRecord.setVisibility(isRecording ? View.VISIBLE : View.GONE);
		}
		if (null != ivThreeRecord) {
			ivThreeRecord.setVisibility(isRecording ? View.VISIBLE : View.GONE);
		}
	}

	public void destory() {
		Log.i(TAG, "destory()");

	}
	
	private boolean isBackCameraOut = false;
	private boolean isReverse = false;
	private View mReverseLayout;
	private TextureView reversePreview;
	private TextView mReverseHint;
    private ImageView mReverseLine;
	private void initReverseWindow(){
		WindowManager.LayoutParams mReverseParams = new WindowManager.LayoutParams();
		mReverseParams.type = LayoutParams.TYPE_SYSTEM_ERROR;
		mReverseParams.format = PixelFormat.RGBA_8888;
		mReverseParams.flags = LayoutParams.FLAG_NOT_FOCUSABLE | LayoutParams.FLAG_HARDWARE_ACCELERATED;
		mReverseParams.gravity = Gravity.TOP | Gravity.RIGHT;
		mReverseParams.x = 0;
		mReverseParams.y = 0;
		mReverseParams.width = WindowManager.LayoutParams.MATCH_PARENT;
		mReverseParams.height = WindowManager.LayoutParams.MATCH_PARENT;
		mReverseLayout = inflater.inflate(R.layout.reverse_win_988, null);
		reversePreview = (TextureView)mReverseLayout.findViewById(R.id.reverse_preview);
		mReverseHint = (TextView)mReverseLayout.findViewById(R.id.hint_text);
		mReverseLine = (ImageView)mReverseLayout.findViewById(R.id.reverse_lines);
		reversePreview.setSurfaceTextureListener(this);
		mWindowManager.addView(mReverseLayout, mReverseParams);
		mReverseLayout.setVisibility(View.GONE);
	}
	
	public void setCameraPlug(boolean isOut) {
		isBackCameraOut = isOut;
	}
	
	public void onRerverseMode(boolean isReverse, boolean isHideLines) {
		Log.i(TAG, "onRerverseMode() isReverse = "+isReverse +",isHideLines = "+isHideLines);
		MyPreference mPreference = MyPreference.getInstance(mRecService);
		boolean  val = mPreference.getReverseLineResId()==1;
		if (isReverse) {
			
			/*if (isBackCameraOut) {
				mReverseHint.setVisibility(View.VISIBLE);
				reversePreview.setVisibility(View.GONE);
				mReverseLine.setVisibility(View.GONE);
			}else{
				mReverseHint.setVisibility(View.GONE);
				reversePreview.setVisibility(View.VISIBLE);
				mReverseLine.setVisibility(View.VISIBLE);
			}*/
			mReverseHint.setVisibility(View.GONE);
			reversePreview.setVisibility(View.VISIBLE);
			mReverseLayout.setVisibility(View.VISIBLE);
			if(!val){
				mReverseLine.setVisibility(View.GONE);
			}else{
				mReverseLine.setVisibility(View.VISIBLE);
			}
		}else{
			mReverseLayout.setVisibility(View.GONE);
			mReverseHint.setVisibility(View.GONE);
			reversePreview.setVisibility(View.GONE);
			mReverseLine.setVisibility(View.GONE);
		}
	}

	private boolean isThreePreviewAdd = false;
	View threePreviewLayout;
	private FrameLayout threePreviewContainer;
	private TextureView threeLeftTextureView, threeMiddleTextureView, threeRightTextureView;
	private View leftLine,rightLine;
	private int cameraStatus = -1;

	private void initThreePreviewWindow() {
		if (isThreePreviewAdd) {
			return;
		}
		WindowManager.LayoutParams mthreeParams = new WindowManager.LayoutParams();
		mthreeParams.type = LayoutParams.TYPE_SYSTEM_ERROR;
		mthreeParams.format = PixelFormat.RGBA_8888;
		mthreeParams.flags = LayoutParams.FLAG_NOT_FOCUSABLE | LayoutParams.FLAG_HARDWARE_ACCELERATED;
		mthreeParams.gravity = Gravity.TOP | Gravity.RIGHT;
		mthreeParams.x = 0;
		mthreeParams.y = 0;
		mthreeParams.width = WindowManager.LayoutParams.MATCH_PARENT;
		mthreeParams.height = WindowManager.LayoutParams.MATCH_PARENT;
		threePreviewLayout = inflater.inflate(R.layout.three_preview_float_win, null);
		threePreviewContainer = (FrameLayout) threePreviewLayout.findViewById(R.id.three_preview_framely_main);
		threeLeftTextureView = (TextureView) threePreviewLayout.findViewById(R.id.left_preview);
		leftLine = threePreviewLayout.findViewById(R.id.left_line);
		threeMiddleTextureView = (TextureView) threePreviewLayout.findViewById(R.id.middle_preview);
		rightLine = threePreviewLayout.findViewById(R.id.right_line);
		threeRightTextureView = (TextureView) threePreviewLayout.findViewById(R.id.right_preview);
		ivThreeRecord = (ImageView) threePreviewLayout.findViewById(R.id.iv_three_record);
		threeLeftTextureView.setSurfaceTextureListener(this);
		threeMiddleTextureView.setSurfaceTextureListener(this);
		threeRightTextureView.setSurfaceTextureListener(this);
		((Button) threePreviewContainer.findViewById(R.id.bt_three_pre_exit))
				.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						hideThreePreviewWindow();
					}
				});
		mWindowManager.addView(threePreviewLayout, mthreeParams);
		threePreviewLayout.setVisibility(View.GONE);
		isThreePreviewAdd = true;
	}

	private void removeThreePreviewWindow() {
		if (null != mWindowManager) {
			if (!isThreePreviewAdd) {
				return;
			}
			mWindowManager.removeView(threePreviewLayout);
			isThreePreviewAdd = false;
		}
	}

	private void showThreePreviewWindow() {
		Log.i(TAG, "showThreePreviewWindow()");
		if (null != threePreviewContainer) {
			if (null != ivThreeRecord) {
				ivThreeRecord.setVisibility(isRecording ? View.VISIBLE : View.GONE);
			}
			if (threePreviewContainer.getVisibility() == View.GONE) {
				threePreviewContainer.setVisibility(View.VISIBLE);
			} else {
				Log.i(TAG, "threePreviewContainer is already showed!");
			}
		}
	}

	private void hideThreePreviewWindow() {
		Log.i(TAG, "hideThreePreviewWindow()");
		cameraStatus = -1;
		if (null != threePreviewContainer) {
			if (threePreviewContainer.getVisibility() == View.VISIBLE) {
				threePreviewContainer.setVisibility(View.GONE);
			} else {
				Log.i(TAG, "threePreviewContainer is not show!");
			}
		}
	}
	
	private void updateView(int id){
		if (id == 2) {// right
			threeLeftTextureView.setVisibility(View.GONE);
			leftLine.setVisibility(View.GONE);
			LinearLayout.LayoutParams middleParam = (android.widget.LinearLayout.LayoutParams) threeMiddleTextureView.getLayoutParams();
			middleParam.width = 600;
			middleParam.height = 400;
			middleParam.gravity = Gravity.LEFT;
			threeMiddleTextureView.setLayoutParams(middleParam);
			LinearLayout.LayoutParams rightParam = (android.widget.LinearLayout.LayoutParams) threeRightTextureView.getLayoutParams();
			rightParam.width = 1000;
			rightParam.height = 400;
			threeRightTextureView.setLayoutParams(rightParam);
			mHandler.postDelayed(new Runnable() {
				
				@Override
				public void run() {
					threeRightTextureView.setVisibility(View.VISIBLE);
					rightLine.setVisibility(View.VISIBLE);
				}
			}, 500);
		} else if (id == 3) {// left
			threeRightTextureView.setVisibility(View.GONE);
			rightLine.setVisibility(View.GONE);
			LinearLayout.LayoutParams middleParam = (android.widget.LinearLayout.LayoutParams) threeMiddleTextureView.getLayoutParams();
			middleParam.width = 600;
			middleParam.height = 400;
			middleParam.gravity = Gravity.RIGHT;
			threeMiddleTextureView.setLayoutParams(middleParam);
			LinearLayout.LayoutParams leftParam = (android.widget.LinearLayout.LayoutParams) threeLeftTextureView.getLayoutParams();
			leftParam.width = 1000;
			leftParam.height = 400;
			threeLeftTextureView.setLayoutParams(leftParam);
			mHandler.postDelayed(new Runnable() {
				
				@Override
				public void run() {
					threeLeftTextureView.setVisibility(View.VISIBLE);
					leftLine.setVisibility(View.VISIBLE);
				}
			}, 500);
		} else if (id == 4) {//full
			threeLeftTextureView.setVisibility(View.VISIBLE);
			threeRightTextureView.setVisibility(View.VISIBLE);
			leftLine.setVisibility(View.VISIBLE);
			rightLine.setVisibility(View.VISIBLE);
			LinearLayout.LayoutParams leftParam = (android.widget.LinearLayout.LayoutParams) threeLeftTextureView.getLayoutParams();
			leftParam.width = 430;
			leftParam.height = 400;
			threeLeftTextureView.setLayoutParams(leftParam);
			LinearLayout.LayoutParams middleParam = (android.widget.LinearLayout.LayoutParams) threeMiddleTextureView.getLayoutParams();
			middleParam.width = 740;
			middleParam.height = 400;
			middleParam.gravity = Gravity.CENTER_HORIZONTAL;
			threeMiddleTextureView.setLayoutParams(middleParam);
			LinearLayout.LayoutParams rightParam = (android.widget.LinearLayout.LayoutParams) threeRightTextureView.getLayoutParams();
			rightParam.width = 430;
			rightParam.height = 400;
			threeRightTextureView.setLayoutParams(rightParam);
		}
	}

	private void showFullScreenWindow(int id,boolean isVoiceControl) {
		Log.i(TAG, "showFullScreenWindow() id = " + id);
		if (isShow) {
			hideWindow();
		}
		if (null != mRecService) {
			mRecService.hideTwoFloat();
		}
		if (id <=4) {
			updateView(id);
			showThreePreviewWindow();
			if (id == 2) {// right
				if (cameraStatus == 2) {
					if (isVoiceControl) {
						speakText("你已打开右路了");
					}
				}else{
					cameraStatus = 2;
					if (isVoiceControl) {
						speakText("好的");
					}
				}
			} else if (id == 3) {// left
				if (cameraStatus == 3) {
					if (isVoiceControl) {
						speakText("你已打开左路了");
					}
				}else{
					cameraStatus = 3;
					if (isVoiceControl) {
						speakText("好的");
					}
				}
			} else if (id == 4) {
				if (cameraStatus == 4) {
					if (isVoiceControl) {
						speakText("你已打开全屏了");
					}
				}else{
					cameraStatus = 4;
					if (isVoiceControl) {
						speakText("好的");
					}
				}
			}
		}else{
			if (id == 5) {
				hideThreePreviewWindow();
				if (null != mRecService) {
					mRecService.showTwoFloat();
					if (isVoiceControl) {
						speakText("好的");
					}
				}
			}
		}
		
	}

	private void hideFullScreenWindow(boolean isVoiceControl) {
		Log.i(TAG, "hideFullScreenWindow()");
		mRecService.sendBroadcast(new Intent(ACTION_STREAM_PREVIEW_WIDOW_HIDE));
		if (isShow) {
			hideWindow();
		}
		hideThreePreviewWindow();
		if (null != mRecService) {
			mRecService.hideTwoFloat();
		}
		if (isVoiceControl) {
			speakText("好的");
		}
	}

	private boolean mTextureIsUp = false;
	private boolean mTextureBackIsUp = false;
	private boolean mIsPreviewing = false;
	private boolean mIsBackPreviewing = false;
	private boolean mTextureLeftIsUp = false;
	private boolean mTextureRightIsUp = false;
	private boolean mIsLeftPreviewing = false;
	private boolean mIsRightPreviewing = false;

	@Override
	public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
		mTextureIsUp = true;
		mTextureBackIsUp = true;
		mTextureLeftIsUp = true;
		mTextureRightIsUp = true;
		Log.d(TAG, "onSurfaceTextureAvailable");
		Log.d(TAG, "mTextureIsUp=" + mTextureIsUp + ";mIsPreviewing=" + mIsPreviewing);
		if (mRecService != null && null != streamSurfaceView && streamSurfaceView.getSurfaceTexture() == surface) {
			Log.d(TAG, "onSurfaceTextureAvailable---FRONT");
			mRecService.setPreviewTexture(CameraInfo.CAMERA_FACING_FRONT, surface);
			if (!mRecService.isPreview(CameraInfo.CAMERA_FACING_FRONT)) {
				mRecService.startPreview(CameraInfo.CAMERA_FACING_FRONT);
			}
			mRecService.startRender(CameraInfo.CAMERA_FACING_FRONT);
			mIsPreviewing = true;

		} else if (mRecService != null && null != streamBackSurfaceView && streamBackSurfaceView.getSurfaceTexture() == surface) {
			Log.d(TAG, "onSurfaceTextureAvailable---BACK");
			mRecService.setPreviewTexture(CameraInfo.CAMERA_FACING_BACK, surface);
			if (!mRecService.isPreview(CameraInfo.CAMERA_FACING_BACK)) {
				mRecService.startPreview(CameraInfo.CAMERA_FACING_BACK);
			}
			mRecService.startRender(CameraInfo.CAMERA_FACING_BACK);
			mIsBackPreviewing = true;
		} /*else if (mRecService != null && null != streamLeftSurfaceView && streamLeftSurfaceView.getSurfaceTexture() == surface) {
			Log.d(TAG, "onSurfaceTextureAvailable---LEFT");
			mRecService.setPreviewTexture(TwoFloatWindow.LEFT_CAMERA_ID, surface);
			if (!mRecService.isPreview(TwoFloatWindow.LEFT_CAMERA_ID)) {
				mRecService.startPreview(TwoFloatWindow.LEFT_CAMERA_ID);
			}
			mRecService.startRender(TwoFloatWindow.LEFT_CAMERA_ID);
			mIsLeftPreviewing = true;
		}*/ 
		/* remove by zdt
		 * else if (mRecService != null && null != streamRightSurfaceView && streamRightSurfaceView.getSurfaceTexture() == surface) {
			Log.d(TAG, "onSurfaceTextureAvailable---RIGHT");
			mRecService.setPreviewTexture(TwoFloatWindow.RIGHT_CAMERA_ID, surface);
			if (!mRecService.isPreview(TwoFloatWindow.RIGHT_CAMERA_ID)) {
				mRecService.startPreview(TwoFloatWindow.RIGHT_CAMERA_ID);
			}
			mRecService.startRender(TwoFloatWindow.RIGHT_CAMERA_ID);
			mIsRightPreviewing = true;
		}*/
		/*else if(mRecService != null && null != threeLeftTextureView && threeLeftTextureView.getSurfaceTexture() == surface){
			Log.d(TAG, "onSurfaceTextureAvailable---threeLeftTextureView");
			mRecService.setPreviewTexture(TwoFloatWindow.LEFT_CAMERA_ID, surface);
			if (!mRecService.isPreview(TwoFloatWindow.LEFT_CAMERA_ID)) {
				mRecService.startPreview(TwoFloatWindow.LEFT_CAMERA_ID);
			}
			mRecService.startRender(TwoFloatWindow.LEFT_CAMERA_ID);
		}*/
		else if(mRecService != null && null != threeMiddleTextureView && threeMiddleTextureView.getSurfaceTexture() == surface){
			Log.d(TAG, "onSurfaceTextureAvailable---threeMiddleTextureView");
			mRecService.setPreviewTexture(CameraInfo.CAMERA_FACING_BACK, surface);
			if (!mRecService.isPreview(CameraInfo.CAMERA_FACING_BACK)) {
				mRecService.startPreview(CameraInfo.CAMERA_FACING_BACK);
			}
			mRecService.startRender(CameraInfo.CAMERA_FACING_BACK);
		}
		/* remove by zdt
		 * else if(mRecService != null && null != threeRightTextureView && threeRightTextureView.getSurfaceTexture() == surface){
			Log.d(TAG, "onSurfaceTextureAvailable---threeRightTextureView");
			mRecService.setPreviewTexture(TwoFloatWindow.RIGHT_CAMERA_ID, surface);
			if (!mRecService.isPreview(TwoFloatWindow.RIGHT_CAMERA_ID)) {
				mRecService.startPreview(TwoFloatWindow.RIGHT_CAMERA_ID);
			}
			mRecService.startRender(TwoFloatWindow.RIGHT_CAMERA_ID);
		}*/
		else if(mRecService != null && null != reversePreview && reversePreview.getSurfaceTexture() == surface){
			Log.d(TAG, "onSurfaceTextureAvailable---reversePreview");
			//add by lym start
			if (CustomValue.ONLY_ONE_CAMERA || CustomValue.CHANGE_FRONT_BACK_CAMERA
			||CustomValue.CAMERA_NOT_RECORD) {
				Log.d(TAG, "onSurfaceTextureAvailable:CAMERA_FACING_FRONT ");
                mRecService.setPreviewTexture(CameraInfo.CAMERA_FACING_FRONT, surface);
                if (!mRecService.isPreview(CameraInfo.CAMERA_FACING_FRONT)) {
                    mRecService.startPreview(CameraInfo.CAMERA_FACING_FRONT);
                }
                mRecService.startRender(CameraInfo.CAMERA_FACING_FRONT);
                // end
            } else {
				Log.d(TAG, "onSurfaceTextureAvailable:CAMERA_FACING_BACK ");
                mRecService.setPreviewTexture(CameraInfo.CAMERA_FACING_BACK, surface);
                if (!mRecService.isPreview(CameraInfo.CAMERA_FACING_BACK)) {
                    mRecService.startPreview(CameraInfo.CAMERA_FACING_BACK);
                }
                mRecService.startRender(CameraInfo.CAMERA_FACING_BACK);
            }
		}
	}

	@Override
	public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
		Log.d(TAG, "onSurfaceTextureSizeChanged");
	}

	@Override
	public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
		Log.d(TAG, "onSurfaceTextureDestroyed");
		/*if(mRecService != null && null != threeLeftTextureView && threeLeftTextureView.getSurfaceTexture() == surface){
			mRecService.stopRender(TwoFloatWindow.LEFT_CAMERA_ID);
		}else */
		if(mRecService != null && null != threeRightTextureView && threeRightTextureView.getSurfaceTexture() == surface){
//			mRecService.stopRender(TwoFloatWindow.RIGHT_CAMERA_ID);
		}else if(mRecService != null && null != threeMiddleTextureView && threeMiddleTextureView.getSurfaceTexture() == surface){
//			mRecService.stopRender(CameraInfo.CAMERA_FACING_BACK);
		}
		mTextureIsUp = false;
		mTextureBackIsUp = false;
		mIsPreviewing = false;
		mIsBackPreviewing = false;
		mIsLeftPreviewing = false;
		mIsRightPreviewing = false;
		mTextureLeftIsUp = false;
		mTextureRightIsUp = false;
		return false;
	}

	@Override
	public void onSurfaceTextureUpdated(SurfaceTexture surface) {

	}

	public boolean isBroadcase = true;
	public boolean isCloseBroadcase = false;
	public static final String VOICE_ACTION_OPEN_FULL_CAMERA = "com.zqc.action.show.full.camera";
	public static final String VOICE_ACTION_CLOSE_FULL_CAMERA = "com.zqc.action.disShow.full.camera";
	public static final String ACTION_VOICE_RETURN_LAUNCHER = "com.zqc.action.return.launcher";
	public static final String ACTION_SPEAK_TEXT = "com.action.other_Text";
	private int curShowCameraID = 1;

	class FullCameraBroadReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String mAction = intent.getAction();
			Log.d(TAG, "FullCameraBroadReceiver action : " + mAction);
			if (mAction.equals(VOICE_ACTION_OPEN_FULL_CAMERA)) {
				int CameraId = intent.getIntExtra("CameraId", 1);
				Log.d(TAG, "VOICE_ACTION_OPEN_FULL_CAMERA->CameraId: " + CameraId);
				if (CameraId >= 2) {
					showFullScreenWindow(CameraId,true);
					return;
				}
				mClickPre.edit().putInt("CameraId", CameraId).apply();
				isBroadcase = true;
				showWindow();
				mRecService.sendScreenOutBroadCastStatus(true);
			} else if (mAction.equals(VOICE_ACTION_CLOSE_FULL_CAMERA)) {
				int CameraId = intent.getIntExtra("CameraId", 1);
				Log.d(TAG, "VOICE_ACTION_CLOSE_FULL_CAMERA->CameraId: " + CameraId);
				if (CameraId >= 2) {
					hideFullScreenWindow(true);
					return;
				}
				mClickPre.edit().putInt("CameraId", CameraId).apply();
				isCloseBroadcase = true;
				hideWindow();
			}else if (mAction.equals(ACTION_VOICE_RETURN_LAUNCHER)) {
				hideFullScreenWindow(true);
			}else if(FastReverseChecker.INTENT_ACTION_FOUR_CAMERA_TURN_LEFT.equals(mAction)){
				boolean leftStatus = intent.getBooleanExtra(FastReverseChecker.FOUR_CAMERA_TURN_LEFT_STATUS_KEY, false);
				Log.i(TAG, "onReceive() leftStatus = "+leftStatus);
				if (leftStatus) {
					showFullScreenWindow(2,false);
				}else{
					hideThreePreviewWindow();
				}
				
			}else if(FastReverseChecker.INTENT_ACTION_FOUR_CAMERA_TURN_RIGHT.equals(mAction)){
				boolean rightStatus = intent.getBooleanExtra(FastReverseChecker.FOUR_CAMERA_TURN_RIGHT_STATUS_KEY, false);
				Log.i(TAG, "onReceive() rightStatus = "+rightStatus);
				if (rightStatus) {
					showFullScreenWindow(3,false);
				}else{
					hideThreePreviewWindow();
				}
			}
		}

	}

	private void doRegister() {
		if (mReceiver == null) {
			mReceiver = new FullCameraBroadReceiver();
		}
		IntentFilter mFilter = new IntentFilter();
		mFilter.addAction(VOICE_ACTION_OPEN_FULL_CAMERA);
//		mFilter.addAction(VOICE_ACTION_CLOSE_FULL_CAMERA);
//		mFilter.addAction(ACTION_VOICE_RETURN_LAUNCHER);
		mRecService.getApplication().registerReceiver(mReceiver, mFilter);
	}

	private void speakText(String str) {
		Intent mIntent = new Intent();
		mIntent.setAction(ACTION_SPEAK_TEXT);
		mIntent.putExtra("otherText", str);
		mRecService.getApplication().sendBroadcast(mIntent);
	}

	private void showWhichCam(int curShowCameraID) {
		if (curShowCameraID == 1) {// preview
			android.view.ViewGroup.LayoutParams lp = (android.view.ViewGroup.LayoutParams) streamSurfaceView
					.getLayoutParams();
			streamSurfaceView.setLayoutParams(streamBackSurfaceView.getLayoutParams());
			streamBackSurfaceView.setLayoutParams(lp);
			streamBackSurfaceView.setOnClickListener(null);
			mPreviewContainer.bringChildToFront(streamSurfaceView);
			streamSurfaceView.setOnClickListener(preOnclick);
			if(CustomValue.CAMERA_NOT_RECORD||CustomValue.ONLY_ONE_CAMERA){
				tvFlag.setText(R.string.back_camera_text);
			}else{
				tvFlag.setText(R.string.front_camera_text);
			}
			/*if (isBroadcase) {
				speakText("已为您打开前路");
				isBroadcase = false;
			}*/
		} else if (curShowCameraID == 0) {// backview
			Log.d("", "zdt --- show backview, isCameraPlugIn: " + preferences.getBoolean("isCameraPlugIn", false));
			/*if (preferences.getBoolean("isCameraPlugIn", false)) {
				android.view.ViewGroup.LayoutParams lp = (android.view.ViewGroup.LayoutParams) streamBackSurfaceView
						.getLayoutParams();
				streamBackSurfaceView.setLayoutParams(streamSurfaceView.getLayoutParams());
				streamSurfaceView.setLayoutParams(lp);
				streamSurfaceView.setOnClickListener(null);
				mPreviewContainer.bringChildToFront(streamBackSurfaceView);
				streamBackSurfaceView.setOnClickListener(backOnclick);
				tvFlag.setText(R.string.back_camera_text);
				if (isBroadcase) {
					speakText("已为您打开后路");
					isBroadcase = false;
				}
			} else {
				// btnExit.performClick();
				Toast.makeText(mRecService, mRecService.getString(R.string.please_insert_back_cam), Toast.LENGTH_LONG)
						.show();
			}*/
			android.view.ViewGroup.LayoutParams lp = (android.view.ViewGroup.LayoutParams) streamBackSurfaceView
					.getLayoutParams();
			streamBackSurfaceView.setLayoutParams(streamSurfaceView.getLayoutParams());
			streamSurfaceView.setLayoutParams(lp);
			streamSurfaceView.setOnClickListener(null);
			mPreviewContainer.bringChildToFront(streamBackSurfaceView);
			streamBackSurfaceView.setOnClickListener(backOnclick);
			tvFlag.setText(R.string.back_camera_text);
			/*if (isBroadcase) {
				speakText("已为您打开后路");
				isBroadcase = false;
			}*/
		} else if (curShowCameraID == 2) {
			android.view.ViewGroup.LayoutParams lp = (android.view.ViewGroup.LayoutParams) streamSurfaceView
					.getLayoutParams();
			streamRightSurfaceView.setLayoutParams(streamSurfaceView.getLayoutParams());
			streamSurfaceView.setLayoutParams(lp);
			streamSurfaceView.setOnClickListener(null);
			mPreviewContainer.bringChildToFront(streamRightSurfaceView);
			streamRightSurfaceView.setOnClickListener(null);
			streamRightSurfaceView.setOnClickListener(rightOnclick);
			tvFlag.setText(R.string.right_camera_text);
			/*if (isBroadcase) {
				speakText("已为您打开右路");
				isBroadcase = false;
			}*/
		}
	}

	public int getCurShowCameraID () {
		return curShowCameraID;
	}

}
