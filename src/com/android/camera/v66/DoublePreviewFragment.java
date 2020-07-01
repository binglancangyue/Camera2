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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.Adas;
import android.hardware.Camera.AdasDetectionListener; // adas
import android.hardware.Camera.CameraInfo;
import android.media.MediaActionSound;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewStub;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.camera.AnimationManager;
import com.android.camera.CameraManager;
import com.android.camera.Storage;
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
import com.android.camera.v66.RecorderActivity.IServiceBindedListener;
import com.android.camera.v66.RecordService.IServiceListener;
import com.android.camera.v66.WrapedRecorder.IRecordCallback;
import com.android.camera2.R;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class DoublePreviewFragment extends Fragment implements View.OnClickListener, IServiceBindedListener,
		IRecordCallback, ISpeedChangeListener, SurfaceTextureListener, Camera.AdasDetectionListener, IServiceListener {

	public static final String TAG = "DoublePreviewFragment";
	public static final int MSG_SECOND_TICKY = 0;
	public static final int MSG_RECORD_TICKY = 1;
	public static final int MSG_ADAS_CALLBACK = 2;
	public static final long TICKY_DELAY = 1000;
	public static final long ONCLICK_DELAY = 500;
	public static final String NAVIGATION_CAMERA_CLOSE_NEED_CHANGED = "android.intent.action.NAVIGATION_CAMERA_CLOSE_NEED_CHANGED";
	public static final String ACTION_HOME_PRESS = "android.intent.action.HOME_PRESS";
	public static final String INTENT_FAST_REVERSE_BOOTUP = "intent.softwinner.carlet.FAST_REVERSE_BOOTUP";
	public static final String ACTION_SPLIT_WINDOW_HAS_CHANGED = "android.intent.action.SPLIT_WINDOW_HAS_CHANGED";
	private TextView mSpeed;
	private ImageView mRecordIcon;
	private TextView mRecordTime;
	private ImageView mRecordLock;
	private TextView mDate;
	private TextView mTime;
	private TextView mHintText;
	private ImageButton mButtonLock;
	private ImageButton mButtonSnapshot;
	private ImageButton mButtonRecord;
	private ImageButton mButtonAdas;
	private ImageButton mButtonMute;
	private ImageView mButtonDivider;
	private ImageButton mButtonSettings;
	private ImageButton mButtonReview;
	private ImageView mReverseLines;
	private boolean mIsLocked = false;
	private boolean mIsRecording = false;
	private boolean mIsAdasOn = false;
	private boolean mIsMuteOn = false;
	private View mCurrentView = null;
	private RecordService mService = null;
	private TextureView mPreview;
	private TextureView mBackPreview;
	private ImageView mPreviewAnimation;
	private boolean mTextureIsUp = false;
	private boolean mIsPreviewing = false;
	private boolean mTextureBackIsUp = false;
	private boolean mIsBackPreviewing = false;
	private SurfaceTexture mTexture;
	private SurfaceTexture mBackTexture;
	private long mStartRecordTime = 0;
	private MyPreference mPref;
	private int mDuaration = 0;
	private CarSpeedMonitor mCarSpeedMonitor;
	private int mFlag;
	private ActivityManager mActivityManager;
	private boolean mIsPowerOn = false;
	private RoadwaySoundPlayer mRoadSoundPlayer;
	private long mAdasLast = 0;
	private boolean mIsOnRight = true;
	private AnimationManager mAnimationManager;
	private MediaActionSound mCameraSound;
	private RoadwayRaw mAdasView;
	private GLSurfaceView mGlSurfaceView;
	private RoadwayRenderer mRoadwayRenderer;
	private boolean mIsBackInFront = true;
	private FrameLayout mPreviewContainer;

	private View.OnClickListener mPreviewClickListener = new View.OnClickListener() {

		@Override
		public void onClick(View arg0) {
			// TODO Auto-generated method stub
			if (mIsBackInFront) {
				LayoutParams lp = mPreview.getLayoutParams();
				mPreview.setLayoutParams(mBackPreview.getLayoutParams());
				mBackPreview.setLayoutParams(lp);
				mBackPreview.setOnClickListener(null);
				mPreviewContainer.bringChildToFront(mPreview);
				mPreview.setOnClickListener(mPreviewClickListener);
			} else {
				LayoutParams lp = mPreview.getLayoutParams();
				mPreview.setLayoutParams(mBackPreview.getLayoutParams());
				mBackPreview.setLayoutParams(lp);
				mBackPreview.setOnClickListener(mPreviewClickListener);
				mPreviewContainer.bringChildToFront(mBackPreview);
				mPreview.setOnClickListener(null);
			}
			mIsBackInFront = !mIsBackInFront;
		}

	};

	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_SECOND_TICKY:
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
				mHandler.sendEmptyMessageDelayed(MSG_SECOND_TICKY, TICKY_DELAY);
				break;
			case MSG_RECORD_TICKY:
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
					if (mDuaration > 3 * 60 * 1000 || mDuaration < 0) {
						mDuaration = 1 * 60 * 1000;
					}
					if (Math.abs(cur - mStartRecordTime) > mDuaration && mService != null) {
						Log.d(TAG, "cur=" + cur + ";mStartRecordTime =" + mStartRecordTime);
						Log.d(TAG, "mDuaration=" + mDuaration);
						mService.switchToNextFile(CameraInfo.CAMERA_FACING_FRONT);
						mService.switchToNextFile(CameraInfo.CAMERA_FACING_BACK);
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
					mAdasView.setAdas((Adas) msg.obj);
					mRoadwayRenderer.setAdas((Adas) msg.obj);
					mGlSurfaceView.requestRender();
					mAdasLast = System.currentTimeMillis();
					// }
				}
				break;
			default:
				break;
			}
			super.handleMessage(msg);
		}
	};

	private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
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

	private BroadcastReceiver mAtionBarReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			if (intent == null || mService == null) {
				return;
			}

			if (RecorderActivity.ACTION_HIDE_LAYOUT.equals(intent.getAction())) {
				hideLayout();
				return;
			} else if (RecorderActivity.ACTION_SHOW_LAYOUT.equals(intent.getAction())) {
				showLayout();
				return;
			} else if (StreamMediaWindow.ACTION_STREAM_MEDIA_WIDOW_SHOW.equals(intent.getAction())) {
				Log.d(TAG, "mAtionBarReceiver sendBroadcast=" + "StreamMediaWindow.ACTION_STREAM_MEDIA_WIDOW_SHOW");
				getActivity().sendBroadcast(new Intent("com.zqc.action.SHOW_STREAM_MEDIA_WINDOW"));
				return;
			} else if (StreamMediaWindow.ACTION_STREAM_MEDIA_WIDOW_HIDE.equals(intent.getAction())) {
				Log.d(TAG, "mAtionBarReceiver sendBroadcast=" + "StreamMediaWindow.ACTION_STREAM_MEDIA_WIDOW_HIDE");
				getActivity().sendBroadcast(new Intent("com.zqc.action.CLOSE_STREAM_MEDIA_WINDOW"));
				return;
			} else if (StreamPreViewWindow.ACTION_STREAM_PREVIEW_WIDOW_SHOW.equals(intent.getAction())) {
				return;
			} else if (StreamPreViewWindow.ACTION_STREAM_PREVIEW_WIDOW_HIDE.equals(intent.getAction())) {
				return;
			}

			Log.d(TAG, "mAtionBarReceiver action=" + intent.getAction());
			boolean isReverse = intent.getBooleanExtra("isReverseing", true);
			Log.d(TAG, "mAtionBarReceiver isReverse =" + isReverse);
			Log.d(TAG, "mAtionBarReceiver mService.getFastReverFlag() =" + mService.getFastReverFlag());
			if (mService.getFastReverFlag() && isReverse) {
				getActivity().sendBroadcast(new Intent("com.zqc.action.CLOSE_STREAM_MEDIA_WINDOW"));
				Log.d(TAG, "mAtionBarReceiver sendBroadcast=" + "com.zqc.action.CLOSE_STREAM_MEDIA_WINDOW");
				mReverseLines.setVisibility(View.VISIBLE);
				if (!mIsBackInFront && mBackPreview != null) {
					mBackPreview.performClick();
				}
			} else {
				getActivity().sendBroadcast(new Intent("com.zqc.action.SHOW_STREAM_MEDIA_WINDOW"));
				Log.d(TAG, "mAtionBarReceiver sendBroadcast=" + "com.zqc.action.SHOW_STREAM_MEDIA_WINDOW");
				mReverseLines.setVisibility(View.GONE);
				mService.setFloatCameraid(-1);
				mService.startFloat();
				mService.hideFloatWindows();
				mService.setPreviewTexture(CameraInfo.CAMERA_FACING_BACK, mBackPreview.getSurfaceTexture());
			}
		}

	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// setHasOptionsMenu(true);
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_TIME_TICK);
		filter.addAction(Intent.ACTION_TIME_CHANGED);
		filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
		filter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);

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
		View view = inflater.inflate(R.layout.double_preview, container, false);
		mSpeed = (TextView) view.findViewById(R.id.speed_value);
		mRecordIcon = (ImageView) view.findViewById(R.id.record_icon);
		mRecordTime = (TextView) view.findViewById(R.id.record_time);
		mRecordLock = (ImageView) view.findViewById(R.id.record_lock);
		mDate = (TextView) view.findViewById(R.id.date);
		mTime = (TextView) view.findViewById(R.id.time);
		mHintText = (TextView) view.findViewById(R.id.hint_text);
		mButtonLock = (ImageButton) view.findViewById(R.id.button_lock);
		mButtonSnapshot = (ImageButton) view.findViewById(R.id.button_snapshot);
		mButtonRecord = (ImageButton) view.findViewById(R.id.button_record);
		mButtonAdas = (ImageButton) view.findViewById(R.id.button_adas);
		mButtonDivider = (ImageView) view.findViewById(R.id.button_divider);
		mButtonMute = (ImageButton) view.findViewById(R.id.button_mute);
		mButtonSettings = (ImageButton) view.findViewById(R.id.button_settings);
		mButtonReview = (ImageButton) view.findViewById(R.id.button_review);
		mPreview = (TextureView) view.findViewById(R.id.front_preview);
		mPreview.setSurfaceTextureListener(this);
		mBackPreview = (TextureView) view.findViewById(R.id.back_preview);
		mBackPreview.setSurfaceTextureListener(this);
		mPreviewAnimation = (ImageView) view.findViewById(R.id.preview_animation);
		mReverseLines = (ImageView) view.findViewById(R.id.reverse_lines);
		mPreviewContainer = (FrameLayout) view.findViewById(R.id.previw_container);
		mButtonLock.setOnClickListener(this);
		mButtonSnapshot.setOnClickListener(this);
		mButtonRecord.setOnClickListener(this);
		mButtonAdas.setOnClickListener(this);
		mButtonMute.setOnClickListener(this);
		mButtonSettings.setOnClickListener(this);
		mButtonReview.setOnClickListener(this);
		mBackPreview.setOnClickListener(mPreviewClickListener);
		mCurrentView = view;
		IntentFilter intent = new IntentFilter(NAVIGATION_CAMERA_CLOSE_NEED_CHANGED);
		intent.addAction(ACTION_HOME_PRESS);
		intent.addAction(INTENT_FAST_REVERSE_BOOTUP);
		intent.addAction(ACTION_SPLIT_WINDOW_HAS_CHANGED);
		intent.addAction(RecorderActivity.ACTION_HIDE_LAYOUT);
		intent.addAction(RecorderActivity.ACTION_SHOW_LAYOUT);
		intent.addAction(StreamMediaWindow.ACTION_STREAM_MEDIA_WIDOW_SHOW);
		intent.addAction(StreamMediaWindow.ACTION_STREAM_MEDIA_WIDOW_HIDE);
		intent.addAction(StreamPreViewWindow.ACTION_STREAM_PREVIEW_WIDOW_SHOW);
		intent.addAction(StreamPreViewWindow.ACTION_STREAM_PREVIEW_WIDOW_HIDE);
		getActivity().registerReceiver(mAtionBarReceiver, intent);
		mRoadSoundPlayer = new RoadwaySoundPlayer(getActivity());
		mAnimationManager = new AnimationManager();
		mCameraSound = new MediaActionSound();
		mCameraSound.load(MediaActionSound.SHUTTER_CLICK);

		mActivityManager = (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);

		mCarSpeedMonitor = new CarSpeedMonitor(getActivity());
		mCarSpeedMonitor.setSpeedChangeListener(this);
		if (getActivity() != null) {
			mPref = MyPreference.getInstance(getActivity());
		}

		if (mFlag == 0) {
			mFlag = ((RecorderActivity) getActivity()).getFlag();
		}

		if ((mFlag & RecorderActivity.PREVIEW_START_RECORDING) > 0) {
			mIsPowerOn = true;
			Log.d(TAG, "mIsPowerOn=" + mIsPowerOn);
		}

		if (getActivity() instanceof RecorderActivity) {
			((RecorderActivity) getActivity()).addServiceBindedListener(this);
			mService = ((RecorderActivity) getActivity()).getRecordService();
			initRecorder();
		}

		/* adas start */
		ViewStub adasViewStub = (ViewStub) view.findViewById(R.id.adas_view_stub);
		if (adasViewStub != null) {
			adasViewStub.inflate();
			mAdasView = (RoadwayRaw) view.findViewById(R.id.adas_view);
		}

		FrameLayout layout = (FrameLayout) view.findViewById(R.id.glsurfaceLayout);

		mGlSurfaceView = new GLSurfaceView(getActivity().getBaseContext());

		mRoadwayRenderer = new RoadwayRenderer();
		mGlSurfaceView.setBackgroundDrawable(null);

		mGlSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
		mGlSurfaceView.setRenderer(mRoadwayRenderer);
		mGlSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
		mGlSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);

		mGlSurfaceView.requestRender();

		layout.addView(mGlSurfaceView, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
		mGlSurfaceView.setZOrderOnTop(true);
		/* adas end */

		initSettings();
		Log.d(TAG, "onCreateView");
		return view;
	}

	private void showLayout() {

	}

	private void hideLayout() {

	}

	@Override
	public void onDestroyView() {
		// TODO Auto-generated method stub
		super.onDestroyView();
		if (mService != null) {
			mService.removeServiceListener(this);
		}
		Log.d(TAG, "onDestroyView");
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		if (getActivity() != null) {
			getActivity().unregisterReceiver(mAtionBarReceiver);
		}
		mSpeed = null;
		mRecordIcon = null;
		mRecordTime = null;
		mRecordLock = null;
		mDate = null;
		mTime = null;
		mHintText = null;
		mButtonLock = null;
		mButtonSnapshot = null;
		mButtonRecord = null;
		mButtonAdas = null;
		mButtonDivider = null;
		mButtonMute = null;
		mButtonSettings = null;
		mButtonReview = null;
		mPreview = null;
		mBackPreview = null;
		mPreviewAnimation = null;
		mReverseLines = null;
		mPreviewContainer = null;
		mCurrentView = null;
		mRoadSoundPlayer.stopMediaPlayer();
		mRoadSoundPlayer = null;
		mAnimationManager.cancelAnimations();
		mAnimationManager = null;
		mCameraSound.release();
		mCameraSound = null;
		Log.d(TAG, "onDestroy");
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		// inflater.inflate(R.menu.fragment_menu, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		/*
		 * switch (item.getItemId()) { case R.id.id_menu_fra_test:
		 * Toast.makeText(getActivity(), "FragmentMenuItem1",
		 * Toast.LENGTH_SHORT).show(); break; default: break; }
		 */
		return true;
	}

	@Override
	public void onClick(View view) {
		// TODO Auto-generated method stub

		Log.d(TAG, "onClick ===");
		switch (view.getId()) {
		case R.id.button_lock:
			if (mService != null) {
				mIsLocked = !mIsLocked;
				if (!mIsRecording && mIsLocked) {
					Log.d(TAG, "onRecordStart once");
					if (Storage.getTotalSpace() < 0) {
						// todo more gentlly hint
						Log.d(TAG, "startRecording sd not mounted");
						Toast.makeText(getActivity(), R.string.sdcard_not_found, Toast.LENGTH_LONG).show();
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
					if (0 == mService.startRecording()) {
						mService.setLockOnce(true);
						mService.setLockFlag(mIsLocked);
					} else {
						mIsLocked = false;
					}
				} else if (mIsLocked) {
					mService.setLockFlag(mIsLocked);
				}
			}
			break;
		case R.id.button_snapshot:
			if (Storage.getTotalSpace() < 0) {
				// todo more gentlly hint
				Log.d(TAG, "startRecording sd not mounted");
				Toast.makeText(getActivity(), R.string.sdcard_not_found, Toast.LENGTH_LONG).show();
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
			if (mService != null) {
				mService.setLockOnce(false);
				if (!mIsRecording) {
					Log.d(TAG, "onRecordStart");
					if (Storage.getTotalSpace() < 0) {
						// todo more gentlly hint
						Log.d(TAG, "startRecording sd not mounted");
						Toast.makeText(getActivity(), R.string.sdcard_not_found, Toast.LENGTH_LONG).show();
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
				} else {
					Log.d(TAG, "onRecordStop");
					if (mService.isMiniMode()) {
						Toast.makeText(getActivity(), R.string.device_busy, Toast.LENGTH_LONG).show();
						return;
					}
					mService.stopRecording();
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
					Log.d(TAG, "set ADAS callback");
					mRoadSoundPlayer.startMediaPlayer();
					mAdasView.setVisibility(View.VISIBLE);
					mAdasView.setInstallAdjustMode(mPref.getCarLaneAdjust());
					if (mGlSurfaceView != null) {
						mGlSurfaceView.setVisibility(View.VISIBLE);
					}
					mRoadwayRenderer.setDisplayEnabled(true);
					mService.setAdasDetecttionCallback(CameraInfo.CAMERA_FACING_FRONT, this);
					mButtonAdas.setImageResource(R.drawable.ic_adas_on);
				} else {
					mButtonAdas.setImageResource(R.drawable.ic_adas_off);
					Log.d(TAG, "set ADAS callback");
					mRoadSoundPlayer.stopMediaPlayer();
					mAdasView.setVisibility(View.GONE);
					if (mGlSurfaceView != null) {
						mGlSurfaceView.setVisibility(View.GONE);
					}
					mRoadwayRenderer.setDisplayEnabled(false);
					mService.setAdasDetecttionCallback(CameraInfo.CAMERA_FACING_FRONT, null);
				}
				mPref.saveAdasFlag(mIsAdasOn);
			}
			break;
		case R.id.button_mute:
			if (mService != null) {
				mIsMuteOn = !mIsMuteOn;
				mService.setMute(mIsMuteOn, CameraInfo.CAMERA_FACING_BACK);
				mService.setMute(mIsMuteOn, CameraInfo.CAMERA_FACING_FRONT);
			}
			break;
		case R.id.button_settings:
			if (getActivity() instanceof RecorderActivity) {
				if (mIsRecording) {
					CDRAlertDialog dialog = CDRAlertDialog.getInstance(getActivity());
					if (null == dialog) {
						return;
					}
					dialog.setTitle(R.string.warning);
					dialog.setMessage(R.string.goto_settings);
					dialog.setCallback(new ICDRAlertDialogListener() {

						@Override
						public void onClick(int state) {
							Log.d(TAG, "onRecordStop");
							if (mService.isMiniMode()) {
								Toast.makeText(getActivity(), R.string.device_busy, Toast.LENGTH_LONG).show();
								return;
							}
							mService.stopRecording();
							// mIsRecording = false;
							((RecorderActivity) getActivity())
									.loadViewByState(RecorderActivity.STATE_SETTINS_SINGLE_FRAGMENT);
							mCurrentView.setVisibility(View.GONE);
						}

						@Override
						public void onTimeClick(int hour, int minute) {
						}

						@Override
						public void onDateClick(int year, int month, int day) {
						}
					});
					dialog.setButtons();
				} else {
					((RecorderActivity) getActivity()).loadViewByState(RecorderActivity.STATE_SETTINS_SINGLE_FRAGMENT);
					mCurrentView.setVisibility(View.GONE);
				}
			}
			break;
		case R.id.button_review:
			if (getActivity() instanceof RecorderActivity) {
				if (mIsRecording) {
					CDRAlertDialog dialog = CDRAlertDialog.getInstance(getActivity());
					if (null == dialog) {
						return;
					}
					dialog.setTitle(R.string.warning);
					dialog.setMessage(R.string.goto_record_browser);
					dialog.setCallback(new ICDRAlertDialogListener() {

						@Override
						public void onClick(int state) {
							mButtonRecord.performClick();
							((RecorderActivity) getActivity()).loadViewByState(RecorderActivity.STATE_REVIEW);
							mCurrentView.setVisibility(View.GONE);
						}

						@Override
						public void onTimeClick(int hour, int minute) {
						}

						@Override
						public void onDateClick(int year, int month, int day) {
						}
					});
					dialog.setButtons();
				} else {
					((RecorderActivity) getActivity()).loadViewByState(RecorderActivity.STATE_REVIEW);
					mCurrentView.setVisibility(View.GONE);
				}
			}
			break;
		default:
			break;
		}
		onClickAnimation(view);
	}

	@Override
	public void onServiceBinded(RecordService service) {
		// TODO Auto-generated method stub
		if (mService != null && mService == service) {
			return;
		}
		if (getActivity() instanceof RecorderActivity) {
			mService = service;
			initRecorder();
		}
	}

	public void initSettings() {
		if (mPref == null) {
			return;
		}
		mIsMuteOn = mPref.isMute();
		if (mIsMuteOn) {
			mButtonMute.setImageResource(R.drawable.ic_mute_on);
		} else {
			mButtonMute.setImageResource(R.drawable.ic_mute_off);
		}
		if (mService != null) {
			mService.setMute(mIsMuteOn, CameraInfo.CAMERA_FACING_BACK);
			mService.setMute(mIsMuteOn, CameraInfo.CAMERA_FACING_FRONT);
		}
		mDuaration = mPref.getRecDuration();
		if (mPref != null) {
			/*
			 * if (mPref.getRearVisionFlip()) {
			 * mBackPreview.setRotationY((float) 180.0); } else {
			 * mBackPreview.setRotationY((float) 0.0); }
			 */
			if (mService != null) {
				mService.setFlip(mPref.getRearVisionFlip());
			}
		}

		mIsAdasOn = mPref.getAdasFlag();
		mPref.setAdasFlagChangedListener(new IAdasFlagChanged() {
			@Override
			public void onAdasFlagChanged(boolean isOpen) {
				// TODO Auto-generated method stub
				if (mIsAdasOn != isOpen) {
					mButtonAdas.performClick();
				}
			}
		});
		mPref.setRecDurationChangedListener(new IRecDurationChanged() {

			@Override
			public void onRecDurationChanged(int value) {
				// TODO Auto-generated method stub
				if (mService != null) {
					mService.setDuration(value);
				}
			}

		});

		mPref.setRearFlipChangedListener(new IRearFlipChanged() {

			@Override
			public void onRearFlipChanged(boolean isFlip) {
				// TODO Auto-generated method stub
				Log.d(TAG, "onRearFlipChanged");
				/*
				 * if (isFlip) { mBackPreview.setRotationY((float) 180.0); }
				 * else { mBackPreview.setRotationY((float) 0.0); }
				 */
				if (mService != null) {
					mService.setFlip(isFlip);
				}
			}

			@Override
			public void onCameraFlipChanged(int cameraId, boolean isFlip) {
				// TODO Auto-generated method stub

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
					mService.setCarType(CameraInfo.CAMERA_FACING_FRONT, value);
				}
			}
		});
	}

	private void initRecorder() {
		if (mService != null) {
			boolean isReversing = mService.readBootStatus();
			if (!mService.isCameraAdd(CameraInfo.CAMERA_FACING_FRONT)) {
				mService.addCamera(CameraInfo.CAMERA_FACING_FRONT);
			}
			if (!mService.isCameraAdd(CameraInfo.CAMERA_FACING_BACK)) {
				if (isReversing) {
					mFlag = mFlag & (~RecorderActivity.PREVIEW_START_RECORDING);
					mService.setNeedExit(false);
				} else {
					mService.addCamera(CameraInfo.CAMERA_FACING_BACK);
				}
			}

			/*
			 * if (!mService.isCameraAdd(TwoFloatWindow.LEFT_CAMERA_ID)) {
			 * mService.addCamera(TwoFloatWindow.LEFT_CAMERA_ID); }
			 */
			if (!mService.isCameraAdd(TwoFloatWindow.RIGHT_CAMERA_ID)) {
				mService.addCamera(TwoFloatWindow.RIGHT_CAMERA_ID);
			}
			// mService.openCamera(TwoFloatWindow.LEFT_CAMERA_ID);
			mService.openCamera(TwoFloatWindow.RIGHT_CAMERA_ID);
			mService.setRecordCallback(CameraInfo.CAMERA_FACING_FRONT, this);
			if (mTextureIsUp && !mIsPreviewing) {
				if (!mService.isPreview(CameraInfo.CAMERA_FACING_FRONT)) {
					mService.startPreview(CameraInfo.CAMERA_FACING_FRONT);
				}
				mService.setPreviewTexture(CameraInfo.CAMERA_FACING_FRONT, mPreview.getSurfaceTexture());
				mIsPreviewing = true;
				if (!mService.isWaterMarkRuning(CameraInfo.CAMERA_FACING_FRONT)) {
					mService.startWaterMark(CameraInfo.CAMERA_FACING_FRONT);
				}
			}
			if (mTextureBackIsUp && !mIsBackPreviewing) {
				if (!mService.isPreview(CameraInfo.CAMERA_FACING_BACK)) {
					mService.startPreview(CameraInfo.CAMERA_FACING_BACK);
				}
				mService.setPreviewTexture(CameraInfo.CAMERA_FACING_BACK, mBackPreview.getSurfaceTexture());
				mIsBackPreviewing = true;
				if (!mService.isWaterMarkRuning(CameraInfo.CAMERA_FACING_BACK)) {
					mService.startWaterMark(CameraInfo.CAMERA_FACING_BACK);
				}
			}

			if (mService.isRecording(CameraInfo.CAMERA_FACING_FRONT)) {
				onRecordStarted(true);
			}

			/*
			 * if (!mService.isPreview(TwoFloatWindow.LEFT_CAMERA_ID)) {
			 * mService.startPreview(TwoFloatWindow.LEFT_CAMERA_ID); }
			 */
			if (!mService.isPreview(TwoFloatWindow.RIGHT_CAMERA_ID)) {
				mService.startPreview(TwoFloatWindow.RIGHT_CAMERA_ID);
			}
			/*
			 * if (!mService.isWaterMarkRuning(TwoFloatWindow.LEFT_CAMERA_ID)) {
			 * mService.startWaterMark(TwoFloatWindow.LEFT_CAMERA_ID); }
			 */
			if (!mService.isWaterMarkRuning(TwoFloatWindow.RIGHT_CAMERA_ID)) {
				mService.startWaterMark(TwoFloatWindow.RIGHT_CAMERA_ID);
			}

			Log.d(TAG, "set ADAS callback");
			if (mIsAdasOn) {
				mService.setIntelligentDetect(CameraInfo.CAMERA_FACING_FRONT, mIsAdasOn);
				mRoadSoundPlayer.startMediaPlayer();
				mAdasView.setVisibility(View.VISIBLE);
				mRoadwayRenderer.setDisplayEnabled(true);
				mAdasView.setInstallAdjustMode(mPref.getCarLaneAdjust());
				if (mGlSurfaceView != null) {
					mGlSurfaceView.setVisibility(View.VISIBLE);
				}
				mService.setAdasDetecttionCallback(CameraInfo.CAMERA_FACING_FRONT, this);
				mButtonAdas.setImageResource(R.drawable.ic_adas_on);
			}
			Log.d(TAG, "mService.getFastReverFlag()=" + mService.getFastReverFlag());
			if (mService.getFastReverFlag()) {
				mReverseLines.setVisibility(View.VISIBLE);
				if (!mIsBackInFront && mBackPreview != null) {
					mBackPreview.performClick();
				}
			}
			mService.addServiceListener(this);
			if (mPref != null) {
				mService.setFlip(mPref.getRearVisionFlip());
			}

			mHintText.setText(R.string.please_insert_back_cam);
			mHintText.setVisibility(View.VISIBLE);

			onLocked(mService.getLockFlag(CameraInfo.CAMERA_FACING_FRONT));
			mService.setMute(mIsMuteOn, CameraInfo.CAMERA_FACING_FRONT);
			mService.setMute(mIsMuteOn, CameraInfo.CAMERA_FACING_BACK);
			mService.setDuration(mPref.getRecDuration());
			mService.addSpeedListener(this);
		}
	}

	public void onClickAnimation(View view) {
		PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat("alpha", 1f, 0.8f, 1f);
		PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat("scaleX", 1f, 0.92f, 1f);
		PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat("scaleY", 1f, 0.92f, 1f);
		ObjectAnimator.ofPropertyValuesHolder(view, alpha, scaleX, scaleY).setDuration(200).start();
	}

	public void onSnapShotAnimation(View view) {
		PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat("alpha", 1f, 0.6f, 1f);
		PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat("scaleX", 1f, 0.95f, 1f);
		PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat("scaleY", 1f, 0.95f, 1f);
		ObjectAnimator.ofPropertyValuesHolder(view, alpha, scaleX, scaleY).setDuration(200).start();
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
				.setDuration(600);
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
		if ((mFlag & RecorderActivity.PREVIEW_START_RECORDING) > 0 && !mIsRecording) {
			mButtonRecord.performClick();
		}
	}

	@Override
	public void onRecordStarted(boolean isStarted) {
		// TODO Auto-generated method stub
		Log.d(TAG, "onRecordStarted");
		if (isStarted) {
			mIsRecording = isStarted;
			mRecordIcon.setVisibility(View.GONE);
			mButtonRecord.setImageResource(R.drawable.ic_record_on);
		}
	}

	@Override
	public void onRecordStoped() {
		// TODO Auto-generated method stub
		Log.d(TAG, "onRecordStoped");
		mIsRecording = false;
		mRecordIcon.setVisibility(View.GONE);
		mButtonRecord.setImageResource(R.drawable.ic_record_off);
		if (mRecordTime != null) {
			mRecordTime.setText(R.string.default_record_time);
		}
	}

	@Override
	public void onCameraPlug(boolean isOut) {
		// TODO Auto-generated method stub
		if (isOut) {
			mHintText.setText(R.string.please_insert_back_cam);
			mHintText.setVisibility(View.VISIBLE);
		} else {
			mHintText.setVisibility(View.GONE);
			if (mService != null) {
				mService.setPreviewTexture(CameraInfo.CAMERA_FACING_BACK, mBackPreview.getSurfaceTexture());
			}
		}
	}

	/*
	 * @Override public void onFileSwitch(boolean isSwitched) { // TODO
	 * Auto-generated method stub Log.d(TAG, "onFileSwitch=" + isSwitched); if
	 * (isSwitched) { mStartRecordTime = System.currentTimeMillis();
	 * mHandler.removeMessages(MSG_RECORD_TICKY);
	 * mHandler.sendEmptyMessage(MSG_RECORD_TICKY); } else { mIsRecording =
	 * false; mHandler.removeMessages(MSG_RECORD_TICKY);
	 * mRecordIcon.setVisibility(View.INVISIBLE);
	 * mButtonRecord.setImageResource(R.drawable.ic_record_off); if (mRecordTime
	 * != null) { mRecordTime.setText(R.string.default_record_time); } } }
	 */

	@Override
	public void onSpeedChange(float speed, int status, double longitude, double latitude) {
		// TODO Auto-generated method stub
		if (mSpeed != null) {
			mSpeed.setText(String.valueOf((int) (speed * 3.6)));
		}
	}

	@Override
	public void onAdasDetection(Adas adas, Camera camera) {
		// Log.d(TAG, "onAdasDetection adas=" + adas);
		if (adas == null) {
			Log.e(TAG, "Adas is null!");
			return;
		}
		mRoadSoundPlayer.checkAdas(adas);
		Message msg = Message.obtain();
		msg.what = MSG_ADAS_CALLBACK;
		msg.obj = adas;
		// mHandler.removeMessages(MSG_ADAS_CALLBACK);
		mHandler.sendMessage(msg);
	}

	@Override
	public void onSurfaceTextureAvailable(SurfaceTexture st, int arg1, int arg2) {
		// TODO Auto-generated method stub
		mTextureIsUp = true;
		Log.d(TAG, "onSurfaceTextureAvailable");
		Log.d(TAG, "mTextureIsUp=" + mTextureIsUp + ";mIsPreviewing=" + mIsPreviewing);
		if (mService != null && mPreview.getSurfaceTexture() == st) {
			mService.setPreviewTexture(CameraInfo.CAMERA_FACING_FRONT, st);
			if (!mService.isPreview(CameraInfo.CAMERA_FACING_FRONT)) {
				mService.startPreview(CameraInfo.CAMERA_FACING_FRONT);
			}
			mIsPreviewing = true;
			if (!mService.isWaterMarkRuning(CameraInfo.CAMERA_FACING_FRONT)) {
				mService.startWaterMark(CameraInfo.CAMERA_FACING_FRONT);
			}
			mService.startRender(CameraInfo.CAMERA_FACING_FRONT);
		} else if (mService != null && mBackPreview.getSurfaceTexture() == st) {
			mService.setPreviewTexture(CameraInfo.CAMERA_FACING_BACK, st);
			if (!mService.isPreview(CameraInfo.CAMERA_FACING_BACK)) {
				mService.startPreview(CameraInfo.CAMERA_FACING_BACK);
			}
			mIsBackPreviewing = true;
			if (!mService.isWaterMarkRuning(CameraInfo.CAMERA_FACING_BACK)) {
				mService.startWaterMark(CameraInfo.CAMERA_FACING_BACK);
			}
			mService.startRender(CameraInfo.CAMERA_FACING_BACK);
		}
	}

	@Override
	public boolean onSurfaceTextureDestroyed(SurfaceTexture st) {
		if (mService != null && mPreview.getSurfaceTexture() == st) {
			mService.stopRender(CameraInfo.CAMERA_FACING_FRONT);
		} else if (mService != null && mBackPreview.getSurfaceTexture() == st) {
			mService.stopRender(CameraInfo.CAMERA_FACING_BACK);
		}
		mTextureIsUp = false;
		mIsPreviewing = false;
		mTextureBackIsUp = false;
		mIsBackPreviewing = false;
		return false;
	}

	@Override
	public void onSurfaceTextureSizeChanged(SurfaceTexture arg0, int arg1, int arg2) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSurfaceTextureUpdated(SurfaceTexture arg0) {
		// TODO Auto-generated method stub

	}

	public void showFormatMsgDialog() {
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

	public void onLockKeyDown() {
		mButtonLock.performClick();
	}

	@Override
	public void onTimeUpdate(long curTime) {
		// TODO Auto-generated method stub
		Log.d(TAG, "onTimeUpdate curTime=" + curTime);
		SimpleDateFormat recordFormatter = new SimpleDateFormat("mm:ss");
		String recordTime = recordFormatter.format(curTime);
		if (mRecordTime != null && mIsRecording) {
			mRecordTime.setText(recordTime);
		}
	}

	@Override
	public void onHomePressed() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onMute(boolean isMuted) {
		// TODO Auto-generated method stub
		mIsMuteOn = isMuted;
		if (isMuted) {
			mButtonMute.setImageResource(R.drawable.ic_mute_on);
		} else {
			mButtonMute.setImageResource(R.drawable.ic_mute_off);
		}
		if (mPref != null) {
			mPref.saveMute(mIsMuteOn);
		}
	}

	@Override
	public void onLocked(boolean isLocked) {
		// TODO Auto-generated method stub
		mIsLocked = isLocked;
		if (isLocked) {
			mRecordLock.setVisibility(View.VISIBLE);
			mButtonLock.setImageResource(R.drawable.ic_lock_on);
		} else {
			mRecordLock.setVisibility(View.INVISIBLE);
			mButtonLock.setImageResource(R.drawable.ic_lock_off);
		}
	}

	@Override
	public void onPictureToken() {
		// TODO Auto-generated method stub
		onSnapShotAnimation(mPreview);
		mCameraSound.play(MediaActionSound.SHUTTER_CLICK);
	}

	public void onSnapShot() {
		mButtonSnapshot.performClick();
	}

	@Override
	public int onAskLeftCameraId() {
		// TODO Auto-generated method stub
		return -1;
	}

}
