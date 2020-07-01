package com.android.camera.v66;

import android.app.ActivityManager;
import android.app.Presentation;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.camera.CameraManager.CameraProxy;
import com.android.camera2.R;

import java.io.IOException;

import android.util.TypedValue;

import android.content.SharedPreferences;

public class DoubleFloatWindow implements Callback {

    private static final String TAG = "DoubleFloatWindow";
    private static boolean isInitFirstWindow = false;
    public static final int DEFAULT_NUM = 2;
    //public static final int CAM_NUM = SystemProperties.getInt("ro.sys.float_camera", DEFAULT_NUM);
    public static final int CAM_NUM = 1;
    public static int LAYOUT_TYPE = SystemProperties.getInt("ro.se.qchome.layouttype", 0);// ���ü��ز���
    public static boolean LAYOUT_TYPE_IOS_988 = SystemProperties.getBoolean("ro.se.qchome.layouttype.ios988", false);// ���ü��ز���

    public static final int ON_LEFT = 1;
    public static final int ON_RIGHT = 2;

    private WindowManager.LayoutParams mWmParams;
    private WindowManager mWindowManager;
    private RelativeLayout[] mFloatWindow = new RelativeLayout[CAM_NUM];
    private RelativeLayout mReverseWindow = null;
    private RecordService mRecService;
    private int mAppIconWidth = 0;
    private int mAppIconHeight = 0;
    private SurfaceView[] mBackSurfaceView = new SurfaceView[CAM_NUM];
    private SurfaceView[] mFrontSurfaceView = new SurfaceView[CAM_NUM];
    private SurfaceView[] mThirdSurfaceView = new SurfaceView[CAM_NUM];
    private SurfaceView mReverseSurfaceView;
    private SurfaceHolder[] mBackHolder = new SurfaceHolder[CAM_NUM];
    private SurfaceHolder[] mFrontHolder = new SurfaceHolder[CAM_NUM];
    private SurfaceHolder[] mThirdHolder = new SurfaceHolder[CAM_NUM];
    private SurfaceHolder mReverseHolder;
    private FrameLayout[] mAppIcon = new FrameLayout[CAM_NUM];
    private RefleshTextView[] mFloatHint = new RefleshTextView[CAM_NUM];
    private TextView mReverseHint;
    private ImageView mReverseLine;
    private ImageView[] mFloatAppIcon = new ImageView[CAM_NUM];
    private int[] mFoatId = new int[CAM_NUM];
    private boolean[] mIsBackHolderReady = new boolean[CAM_NUM];
    private boolean[] mIsFrontHolderReady = new boolean[CAM_NUM];
    private boolean[] mIsThirdHolderReady = new boolean[CAM_NUM];
    private boolean mIsReverseHolderReady = false;
    private boolean mFlipState = true;
    private boolean mIsReverse = false;
    private boolean mIsHideLines = false;
    private WindowManager.LayoutParams mFloatWinParam = null;
    private boolean mIsCameraOut = false;

    // add by chengyuzhou
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    private Boolean isshow = false;

    private Boolean isShowing = false;
    public static final String ACTION_SHOW = "action_show_window";
    public static final String ACTION_HIDE = "action_hide_window";

    // end
    public DoubleFloatWindow(RecordService service, Boolean isshow) {
        if (service == null) {
            Log.d(TAG, "service = null");
            return;
        }
        resetFloatId();
        for (int i = 0; i < CAM_NUM; i++) {
            mFloatAppIcon[i] = new ImageView(service);
        }
        mRecService = service;
        this.isshow = isshow;
        mWindowManager = (WindowManager) mRecService.getApplication().getSystemService(Context.WINDOW_SERVICE);
        // add by chenyuzhou
        preferences = mRecService.getSharedPreferences("isShowThird", Context.MODE_PRIVATE);
        editor = preferences.edit();

        IntentFilter intentFilter = new IntentFilter(StreamMediaWindow.ACTION_STREAM_MEDIA_WIDOW_HIDE);
        intentFilter.addAction(StreamPreViewWindow.ACTION_STREAM_PREVIEW_WIDOW_HIDE);
        intentFilter.addAction(ACTION_SHOW);
        intentFilter.addAction(ACTION_HIDE);
        mRecService.registerReceiver(receiver, intentFilter);
        // end
    }

    public void usbPlugNotify(int state) {

    }

    boolean isHomePressed = false;

    public void setHomePressed() {
        isHomePressed = true;
    }

    public void startFloat() {
        Log.i(TAG, "startFloat");
        int marginLeft = 8;
        int marginTop = 60;
        int screenWidth;
        int screenHeight;
        int foatWinWidth;
        int foatWinHeight;
        int tmpMarginTop = 0;
        DisplayMetrics dm = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getMetrics(dm);

        screenWidth = dm.widthPixels;
        screenHeight = dm.heightPixels;
        Log.d(TAG, "screenWidth=" + screenWidth + ";screenHeight=" + screenHeight);

		/*foatWinWidth = screenWidth * 160 / 1600;
		foatWinHeight = 90 * screenHeight / 480;*/
        foatWinWidth = 300;
        foatWinHeight = 96;
        Log.d(TAG, "foatWinWidth=" + foatWinWidth + ";foatWinHeight=" + foatWinHeight);

        marginLeft = screenWidth * 8 / 1600;
        marginTop = foatWinHeight * (60 + 120) / 480;
        Log.d(TAG, "marginLeft=" + marginLeft + ";marginTop=" + marginTop);

        mAppIconWidth = screenWidth * 90 / 1600;
        mAppIconHeight = 90 * screenHeight / 480;

        marginLeft = 0;
        marginTop = mRecService.getResources().getDimensionPixelSize(com.android.internal.R.dimen.status_bar_height);
        foatWinWidth = (int) mRecService.getResources().getDimension(R.dimen.screen_systemui_width);
        ;
        foatWinHeight = foatWinWidth * 9 / 16;
        Log.d(TAG, "foatWinWidth=" + foatWinWidth + ";foatWinHeight=" + foatWinHeight);
        if (foatWinWidth == 135) {
            if (LAYOUT_TYPE == 6) {
                foatWinWidth = 232;
                marginLeft = 0;
                foatWinHeight = 170;
            } else {
                foatWinWidth = 170;
                marginLeft = 3;
                foatWinHeight = 100;
            }
        }
        mAppIconWidth = screenWidth * 90 / 1600;
        mAppIconHeight = 90 * screenHeight / 480;

        mAppIconWidth = foatWinHeight;
        mAppIconHeight = foatWinHeight;
        if (RecordService.SPLITSCREEN_SEVEN) {
            mAppIconWidth = foatWinWidth = 83;
            mAppIconHeight = foatWinHeight = 83;
        }

        // add by Jenchar
        for (int i = 0; i < CAM_NUM; i++) {
            if (LAYOUT_TYPE == 6) {
                Log.d(TAG, "LAYOUT_TYPE   ==6");
                if (i == 1) {
                    tmpMarginTop = marginTop + foatWinHeight * 9 / 8 + 19;
                } else {
                    tmpMarginTop = marginTop - 25;
                }
                // end
                mFloatWindow[i] = createFloatWindow(R.layout.float_win_double, Gravity.RIGHT | Gravity.BOTTOM,
                        marginLeft, tmpMarginTop, foatWinWidth, foatWinHeight);
            } else {
                Log.d(TAG, "LAYOUT_TYPE  !=6");
                if (SplitUtil.isFullWindow(mRecService)) {
                    int mfoatWinHeight = (int) (mRecService.getResources().getDimension(R.dimen.float_mheight));
                    if (i == 1) {
                        tmpMarginTop = marginTop + mfoatWinHeight * 9 / 8
                                + (int) (mRecService.getResources().getDimension(R.dimen.float_view_margintop));
                    } else {
                        tmpMarginTop = marginTop
                                + (int) (mRecService.getResources().getDimension(R.dimen.float_view_margintop));
                    }

                    if (RecordService.SPLITSCREEN_SEVEN) {
                        marginLeft = 0;
                        tmpMarginTop = 0;
                    }

                    if (RecordService.SPLITSCREEN_SEVEN) {
                        mFloatWindow[i] = createFloatWindow(R.layout.float_win_double, Gravity.LEFT | Gravity.BOTTOM,
                                marginLeft, tmpMarginTop, foatWinWidth, foatWinHeight);
                    } else {
                        mFloatWindow[i] = createFloatWindow(R.layout.float_win_double, Gravity.LEFT | Gravity.TOP,
                                marginLeft, tmpMarginTop, foatWinWidth, foatWinHeight);
                    }
                } else {
                    Log.d(TAG, "not full screen");
                    if (CustomValue.FULL_WINDOW) {
                        mFloatWindow[i] = createFloatWindow(R.layout.float_win_double,
                                Gravity.LEFT | Gravity.TOP,
                                0, 80, 150, 110);
                    } if(CustomValue.CAMERA_NOT_RECORD){
                        mFloatWindow[i] = createFloatWindow(R.layout.float_win_double,
                                Gravity.LEFT | Gravity.TOP,
                                874, 407, 0, 0);
                    }else {
                        mFloatWindow[i] = createFloatWindow(R.layout.float_win_double,
                                Gravity.LEFT | Gravity.TOP,
                                874, 407, 150, 110);
                    }
                }

            }

            mAppIcon[i] = (FrameLayout) mFloatWindow[i].findViewById(R.id.app_icon);
            mBackSurfaceView[i] = (SurfaceView) mFloatWindow[i].findViewById(R.id.back_preview);
            mBackHolder[i] = mBackSurfaceView[i].getHolder();
            mBackHolder[i].addCallback(this);
            mFrontSurfaceView[i] = (SurfaceView) mFloatWindow[i].findViewById(R.id.front_preview);
            mFrontHolder[i] = mFrontSurfaceView[i].getHolder();
            mFrontHolder[i].addCallback(this);
            mThirdSurfaceView[i] = (SurfaceView) mFloatWindow[i].findViewById(R.id.third_preview);
            mThirdHolder[i] = mThirdSurfaceView[i].getHolder();
            mThirdHolder[i].addCallback(this);
            mWmParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM;
            mWindowManager.addView(mFloatWindow[i], mWmParams);
            mFloatWinParam = (WindowManager.LayoutParams) mFloatWindow[i].getLayoutParams();
            mAppIcon[i].setVisibility(View.GONE);
            mBackSurfaceView[i].setVisibility(View.GONE);
            mFrontSurfaceView[i].setVisibility(View.GONE);
            mFloatHint[i] = (RefleshTextView) mFloatWindow[i].findViewById(R.id.hint_text);
            if (LAYOUT_TYPE == 6) {
                if (LAYOUT_TYPE_IOS_988) {
                    mFloatHint[i].setBackgroundColor(mRecService.getResources().getColor(R.color.hint_text));
                }
            }
        }
        if (LAYOUT_TYPE == 6) {
            mReverseWindow = (RelativeLayout) getView(R.layout.reverse_win_988);
        } else {
            mReverseWindow = (RelativeLayout) getView(R.layout.reverse_win);
        }
        mReverseSurfaceView = (SurfaceView) mReverseWindow.findViewById(R.id.reverse_preview);
        mReverseHolder = mReverseSurfaceView.getHolder();
        mReverseHolder.addCallback(this);
        mReverseHint = (TextView) mReverseWindow.findViewById(R.id.hint_text);
        mReverseLine = (ImageView) mReverseWindow.findViewById(R.id.reverse_lines);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams();
        params.type = LayoutParams.TYPE_SYSTEM_ERROR;// LayoutParams.TYPE_TOAST;
        params.format = PixelFormat.RGBA_8888;
        params.flags = LayoutParams.FLAG_NOT_FOCUSABLE | LayoutParams.FLAG_HARDWARE_ACCELERATED;
        // if(LAYOUT_TYPE==6){
        // params.gravity = Gravity.CENTER;
        // }else{
        params.gravity = Gravity.LEFT | Gravity.TOP;
        // }
        params.x = 0;
        params.y = 0;
        // if(LAYOUT_TYPE==6){
        // RelativeLayout.LayoutParams p = (RelativeLayout.LayoutParams)
        // mReverseSurfaceView.getLayoutParams();
        // p.leftMargin=232;
        // p.width=1150;
        // p.height=WindowManager.LayoutParams.MATCH_PARENT;
        // mReverseSurfaceView.setLayoutParams(p);
        // params.format = PixelFormat.TRANSLUCENT;
        // }else{
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        // }

        if (RecordService.SPLITSCREEN_SEVEN) {
            params.gravity = Gravity.LEFT | Gravity.BOTTOM;
        } else {
            params.gravity = Gravity.LEFT | Gravity.TOP;
        }
        params.height = WindowManager.LayoutParams.MATCH_PARENT;
        if (LAYOUT_TYPE != 6) {
            params.windowAnimations = android.R.style.Animation_Translucent;
        }
        mWindowManager.addView(mReverseWindow, params);
        mReverseWindow.setVisibility(View.GONE);
    }

    public void startFloatDisplay() {
        DisplayManager displayManager;
        Display[] displays;
        displayManager = (DisplayManager) mRecService.getSystemService(Context.DISPLAY_SERVICE);
        displays = displayManager.getDisplays();
        Log.d(TAG, "displays.length = " + displays.length);
        DifferentDislay presentation = new DifferentDislay(mRecService, displays[1]);
        presentation.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);

        presentation.show();
    }

    private class DifferentDislay extends Presentation {
        public DifferentDislay(Context outerContext, Display display) {
            super(outerContext, display);
            // TODOAuto-generated constructor stub
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.display_win);
        }

    }

    private RelativeLayout createFloatWindow(int id, int gravity, int x, int y, int w, int h) {
        Log.i(TAG, "createFloatWindow");
        RelativeLayout window = (RelativeLayout) getView(id);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams();
        if (LAYOUT_TYPE == 6) {
            params.type = LayoutParams.TYPE_SYSTEM_DIALOG;// LayoutParams.TYPE_TOAST;
        } else {
            params.type = LayoutParams.TYPE_SYSTEM_ERROR;// LayoutParams.TYPE_TOAST;
        }
        params.format = PixelFormat.RGBA_8888;
        params.flags = LayoutParams.FLAG_NOT_FOCUSABLE | LayoutParams.FLAG_HARDWARE_ACCELERATED;
        params.gravity = gravity;
        if (!RecordService.SPLITSCREEN_SEVEN) {
            params.x = x;
            params.y = y;
        }
        params.width = w;
        params.height = h;
        params.windowAnimations = android.R.style.Animation_Translucent;
        mWmParams = params;
        return window;
    }

    public View getView(int id) {
        View vv;
        LayoutInflater inflater = LayoutInflater.from(mRecService.getApplication());
        vv = (RelativeLayout) inflater.inflate(id, null);
        vv.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        return vv;
    }

    public int getOutCamId() {
        // fix me, temporary solution.
        int maxCamNum = 3;
        for (int i = 0; i < maxCamNum; i++) {
            if (i != mFoatId[0] && i != mFoatId[1]) {
                return i;
            }
        }
        return -1;
    }

    public ImageView[] getAppIconArray() {
        return mFloatAppIcon;
    }

    public void setFloatCameraid(int cameraId, int id) {
        if (id > CAM_NUM || id < 0) {
            Log.d(TAG, "setFloatCameraid id=" + id);
            return;
        }
        mFoatId[id] = cameraId;
    }

    public int getFloatCameraId(int id) {
        if (id > CAM_NUM || id < 0) {
            Log.d(TAG, "getFloatCameraId id=" + id);
            return -1;
        }
        for (int i = 0; i < CAM_NUM; i++) {
            Log.d(TAG, "getFloatCameraId id=" + mFoatId[i]);
        }
        if (mFoatId != null) {
            return mFoatId[id];
        }
        return -1;
    }

    public int getNeedFloat(int cameraId) {
        for (int i = 0; i < CAM_NUM; i++) {
            if (mFoatId[i] == cameraId) {
                return i;
            }
        }
        return -1;
    }

    public void setOnclickListener(View.OnClickListener listener, int id) {
        if (id > CAM_NUM || id < 0) {
            Log.d(TAG, "setOnclickListener id=" + id);
            return;
        }
        if (mFloatWindow[id] != null) {
            mFloatWindow[id].setOnClickListener(listener);
        }
    }

    // add by lym
    public void doPerformClick() {
        mFloatWindow[0].performClick();
    }

    // add by chengyuzhou
    public void setOnLongclickListener(View.OnLongClickListener listener) {
		/*if (SystemProperties.getInt("ro.sys.float_camera", 1) == 2) {
			if (mFloatWindow[1] != null) {
				mFloatWindow[1].setOnLongClickListener(listener);
			}
		}*/
    }
    // end

    // public void setBackFloatWindow(Boolean b){
    // if(b){
    // mFloatWindow[1].setVisibility(View.VISIBLE);
    // }else{
    // mFloatWindow[1].setVisibility(View.INVISIBLE);
    // }
    // }
    public void performClick(int id) {
        if (id > CAM_NUM || id < 0) {
            Log.d(TAG, "performClick id=" + id);
            return;
        }
        if (mFloatWindow[id] != null) {
            mFloatWindow[id].performClick();
        }
    }

    public void resetFloatId() {
        if (mFoatId.length > 0) {
            mFoatId[0] = CameraInfo.CAMERA_FACING_BACK;
        }
        if (mFoatId.length > 1) {
            mFoatId[1] = RecorderActivity.CAMERA_THIRD;
        }
    }

    public void setDisplayId() {
        for (int i = 0; i < CAM_NUM; i++) {
            setDisplayId(mFoatId[i], i);
        }
    }

    // add by chengyuzhou
    public void setThirdImagerIsShow(Boolean isshow) {
		/*if (SystemProperties.getInt("ro.sys.float_camera", 1) == 2) {
			if (isshow) {
				mFloatHint[1].setVisibility(View.VISIBLE);
				mFloatHint[1].setText(R.string.disconnectrecording);
				mFloatHint[1].setTextSize(TypedValue.COMPLEX_UNIT_PX, 16);
				mAppIcon[1].setVisibility(View.GONE);
				mThirdSurfaceView[1].setVisibility(View.GONE);
			} else {
				mFloatHint[1].setVisibility(View.GONE);
				mAppIcon[1].setVisibility(View.GONE);
				mThirdSurfaceView[1].setVisibility(View.VISIBLE);
			}
		}*/
    }

    // end
    public void setDisplayId(int cameraId, int id) {
        if (id > CAM_NUM || id < 0) {
            Log.d(TAG, "setDisplayId id=" + id);
            return;
        }
        Log.d(TAG, "------------go---------cameraId = " + cameraId);
        // add by lym start
        if (CustomValue.CHANGE_FRONT_BACK_CAMERA || CustomValue.FULL_WINDOW) {
            if (cameraId == -1) {
                cameraId = 1;
            }
        }
        // end
        mFoatId[id] = cameraId;
        show(id);
        if (CameraInfo.CAMERA_FACING_BACK == cameraId) {
            if (mIsCameraOut) {
                //by lym start
                if (CustomValue.ONLY_ONE_CAMERA || CustomValue.CAMERA_NOT_RECORD) {
                    mFloatHint[id].setVisibility(View.GONE);
                } else {
                    mFloatHint[id].setVisibility(View.VISIBLE);
                }
                //end
                mAppIcon[id].setVisibility(View.GONE);
                mBackSurfaceView[id].setVisibility(View.GONE);
                mFrontSurfaceView[id].setVisibility(View.GONE);
                mThirdSurfaceView[id].setVisibility(View.GONE);
            } else {
                mAppIcon[id].setVisibility(View.GONE);
                mBackSurfaceView[id].setVisibility(View.VISIBLE);
                mFrontSurfaceView[id].setVisibility(View.GONE);
                mThirdSurfaceView[id].setVisibility(View.GONE);
                mFloatHint[id].setVisibility(View.GONE);
            }
            if (CustomValue.FULL_WINDOW && isHomePressed) {
                mBackSurfaceView[id].setVisibility(View.GONE);
                isHomePressed = false;
            }
            if (mRecService != null && mIsBackHolderReady[id]) {
                /*
                 * if(mRecService.isRender(cameraId)) {
                 * mRecService.stopRender(cameraId); }
                 * if(mRecService.isPreview(cameraId)) {
                 * mRecService.stopPreview(cameraId); }
                 */
                mRecService.setPreviewDisplay(cameraId, mBackHolder[id]);
                if (!mRecService.isPreview(cameraId)) {
                    mRecService.startPreview(cameraId);
                }
                // mRecService.startPreview(cameraId);
                // mRecService.startRender(cameraId);
            }
            setFlip(id);

        } else if (-1 == cameraId) {
            mAppIcon[id].setVisibility(View.VISIBLE);
            mBackSurfaceView[id].setVisibility(View.GONE);
            mFrontSurfaceView[id].setVisibility(View.GONE);
            mThirdSurfaceView[id].setVisibility(View.GONE);
            mFloatHint[id].setVisibility(View.GONE);
        } else if (CameraInfo.CAMERA_FACING_FRONT == cameraId) {
            mAppIcon[id].setVisibility(View.GONE);
            mBackSurfaceView[id].setVisibility(View.GONE);
            mFrontSurfaceView[id].setVisibility(View.VISIBLE);
            mThirdSurfaceView[id].setVisibility(View.GONE);
            mFloatHint[id].setVisibility(View.GONE);
            if (mRecService != null && mIsFrontHolderReady[id]) {
                /*
                 * if(mRecService.isRender(cameraId)) {
                 * mRecService.stopRender(cameraId); }
                 * if(mRecService.isPreview(cameraId)) {
                 * mRecService.stopPreview(cameraId); }
                 */
                mRecService.setPreviewDisplay(cameraId, mFrontHolder[id]);
                if (!mRecService.isPreview(cameraId)) {
                    mRecService.startPreview(cameraId);
                }
                // mRecService.startPreview(cameraId);
                // mRecService.startRender(cameraId);
            }
        } else if (RecorderActivity.CAMERA_THIRD == cameraId) {
            mAppIcon[id].setVisibility(View.GONE);
            mBackSurfaceView[id].setVisibility(View.GONE);
            mFrontSurfaceView[id].setVisibility(View.GONE);
            mThirdSurfaceView[id].setVisibility(View.VISIBLE);
            mFloatHint[id].setVisibility(View.GONE);
            if (mRecService != null && mIsThirdHolderReady[id]) {
                /*
                 * if(mRecService.isRender(cameraId)) {
                 * mRecService.stopRender(cameraId); }
                 * if(mRecService.isPreview(cameraId)) {
                 * mRecService.stopPreview(cameraId); }
                 */
                mRecService.setPreviewDisplay(cameraId, mThirdHolder[id]);
                if (!mRecService.isPreview(cameraId)) {
                    mRecService.startPreview(cameraId);
                }
                // mRecService.startPreview(cameraId);
                // mRecService.startRender(cameraId);
            }
        }
        // add by chenyuzhou
        if (!preferences.getBoolean("isShowThird", true)) {
            setThirdImagerIsShow(true);
        }
        // end
    }

    public void addImageView(ImageView view, int id) {
        if (id > CAM_NUM || id < 0) {
            Log.d(TAG, "addImageView id=" + id);
            return;
        }
        Log.d(TAG, "addImageView");
        if (mAppIcon[id] != null) {
            mAppIcon[id].setVisibility(View.VISIBLE);
            //add by lym start
            if (CustomValue.CHANGE_FRONT_BACK_CAMERA) {
                mAppIcon[id].setVisibility(View.GONE);
            }
            // end
            mAppIcon[id].removeAllViews();
            if (view != null) {
                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(mAppIconWidth, mAppIconHeight);
                view.setLayoutParams(lp);
                mAppIcon[id].addView(view);
                show(id);
            }
        }
    }

    public void show(int id) {
        if (id > CAM_NUM || id < 0) {
            Log.d(TAG, "show id=" + id);
            return;
        }
        if (mFloatWindow[id] != null) {
            //add by chengyuzhou
            if (!SplitUtil.isFullWindow(mRecService)) {
                mFloatWindow[id].setVisibility(View.VISIBLE);
            }
            //	mFloatWindow[id].setVisibility(View.VISIBLE);
        }
    }

    public void hide(int id) {
        if (id > CAM_NUM || id < 0) {
            Log.d(TAG, "hide id=" + id);
            return;
        }
        if (mFloatWindow[id] != null) {
            mFloatWindow[id].setVisibility(View.GONE);
        }
    }

    public void show() {
        isShowing = true;
        for (int i = 0; i < CAM_NUM; i++) {
            show(i);
        }
    }

    public void hide() {
        isShowing = false;
        for (int i = 0; i < CAM_NUM; i++) {
            hide(i);
        }
    }

    public boolean isShow() {
        return isShowing;
    }

    @Override
    public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // TODO Auto-generated method stub
        Log.d(TAG, "surfaceCreated: ");
        for (int i = 0; i < CAM_NUM; i++) {
            if (mBackSurfaceView[i] != null && mBackSurfaceView[i].getHolder() == holder) {
                mIsBackHolderReady[i] = true;
                if (mRecService != null && mFoatId[i] == CameraInfo.CAMERA_FACING_BACK) {
                    /*
                     * if(mRecService.isRender(CameraInfo.CAMERA_FACING_BACK)) {
                     * mRecService.stopRender(CameraInfo.CAMERA_FACING_BACK); }
                     * if(mRecService.isPreview(CameraInfo.CAMERA_FACING_BACK))
                     * { mRecService.stopPreview(CameraInfo.CAMERA_FACING_BACK);
                     * }
                     */
                    mBackHolder[i] = holder;
                    mRecService.setPreviewDisplay(mFoatId[i], mBackHolder[i]);
                    if (!mRecService.isPreview(mFoatId[i])) {
                        mRecService.startPreview(mFoatId[i]);
                    }
                    // mRecService.startPreview(CameraInfo.CAMERA_FACING_BACK);
                    // mRecService.startRender(CameraInfo.CAMERA_FACING_BACK);
                }
                if (mFoatId[i] == CameraInfo.CAMERA_FACING_BACK) {
                    setFlip(i);
                }
            } else if (mFrontSurfaceView[i] != null && mFrontSurfaceView[i].getHolder() == holder) {
                mIsFrontHolderReady[i] = true;
                if (mRecService != null && mFoatId[i] == CameraInfo.CAMERA_FACING_FRONT) {
                    /*
                     * if(mRecService.isRender(CameraInfo.CAMERA_FACING_BACK)) {
                     * mRecService.stopRender(CameraInfo.CAMERA_FACING_BACK); }
                     * if(mRecService.isPreview(CameraInfo.CAMERA_FACING_BACK))
                     * { mRecService.stopPreview(CameraInfo.CAMERA_FACING_BACK);
                     * }
                     */
                    mFrontHolder[i] = holder;
                    mRecService.setPreviewDisplay(mFoatId[i], mFrontHolder[i]);
                    if (!mRecService.isPreview(mFoatId[i])) {
                        mRecService.startPreview(mFoatId[i]);
                    }
                    // mRecService.startPreview(CameraInfo.CAMERA_FACING_BACK);
                    // mRecService.startRender(CameraInfo.CAMERA_FACING_BACK);
                }
            } else if (mThirdSurfaceView[i] != null && mThirdSurfaceView[i].getHolder() == holder) {
                mIsThirdHolderReady[i] = true;
                if (mRecService != null && mFoatId[i] == RecorderActivity.CAMERA_THIRD) {
                    /*
                     * if(mRecService.isRender(CameraInfo.CAMERA_FACING_BACK)) {
                     * mRecService.stopRender(CameraInfo.CAMERA_FACING_BACK); }
                     * if(mRecService.isPreview(CameraInfo.CAMERA_FACING_BACK))
                     * { mRecService.stopPreview(CameraInfo.CAMERA_FACING_BACK);
                     * }
                     */
                    mThirdHolder[i] = holder;
                    mRecService.setPreviewDisplay(mFoatId[i], mThirdHolder[i]);
                    if (!mRecService.isPreview(mFoatId[i])) {
                        // mRecService.startPreview(mFoatId[i]);
                        // Boolean
                        // show=preferences.getBoolean("isShowThird",true);
                        Log.v(TAG, "FloatIsShowThird:" + isshow);
                        if (!isshow) {
                            //by lym start
                            if (CustomValue.ONLY_ONE_CAMERA || CustomValue.CAMERA_NOT_RECORD) {
                                mFloatHint[1].setVisibility(View.GONE);
                            } else {
                                mFloatHint[1].setVisibility(View.VISIBLE);
                            }
                            //end
                            mFloatHint[1].setText(R.string.disconnectrecording);
                            mFloatHint[1].setTextSize(TypedValue.COMPLEX_UNIT_PX, 16);
                            mAppIcon[1].setVisibility(View.GONE);
                            mThirdSurfaceView[1].setVisibility(View.GONE);
                        } else {
                            mRecService.startPreview(mFoatId[i]);
                        }

                    }
                    // mRecService.startPreview(CameraInfo.CAMERA_FACING_BACK);
                    // mRecService.startRender(CameraInfo.CAMERA_FACING_BACK);
                }
            }
        }
        if (mReverseSurfaceView != null && mReverseSurfaceView.getHolder() == holder) {
            mIsReverseHolderReady = true;
            mReverseHolder = holder;
            Log.d(TAG, "surfaceCreated mIsReverse=" + mIsReverse);
            if (mRecService != null && mIsReverse) {
                mRecService.setPreviewDisplay(CameraInfo.CAMERA_FACING_BACK, mReverseHolder);
                Log.d(TAG, "surfaceCreated isPreview=" + mRecService.isPreview(CameraInfo.CAMERA_FACING_BACK));
                if (!mRecService.isPreview(CameraInfo.CAMERA_FACING_BACK)) {
                    mRecService.startPreview(CameraInfo.CAMERA_FACING_BACK);
                }
            }
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // TODO Auto-generated method stub
        for (int i = 0; i < CAM_NUM; i++) {
            if (mBackSurfaceView[i] != null && mBackSurfaceView[i].getHolder() == holder) {
                mIsBackHolderReady[i] = false;
                mBackHolder[i] = null;
            } else if (mFrontSurfaceView[i] != null && mFrontSurfaceView[i].getHolder() == holder) {
                mIsFrontHolderReady[i] = false;
                mFrontHolder[i] = null;
            }
        }
        if (mReverseSurfaceView != null && mReverseSurfaceView.getHolder() == holder) {
            mIsReverseHolderReady = false;
            mReverseHolder = null;
        }
    }

    public void setFlipState(boolean isFlip, int id) {
        if (id > CAM_NUM || id < 0) {
            Log.d(TAG, "setFlipState id=" + id);
            return;
        }
        mFlipState = isFlip;
        setFlip(id);
    }

    private void setFlip(int id) {
        if (mBackSurfaceView[id] != null) {
            /*
             * if (mFlipState) { mBackSurfaceView.setRotationY((float) 180.0); }
             * else { mBackSurfaceView.setRotationY((float) 0.0); }
             */
        }
    }

    public void onRerverseMode(boolean isReverse, boolean isHideLines) {
        Log.d(TAG, "onRerverseMode isReverse=" + isReverse);
        if (mReverseWindow == null) {
            return;
        }
        if (isReverse) {
            mIsReverse = true;
            Log.d(TAG, "onRerverseMode mIsCameraOut=" + mIsCameraOut);
            if (mIsCameraOut) {
                mReverseHint.setVisibility(View.VISIBLE);
                mReverseSurfaceView.setVisibility(View.GONE);
                mReverseLine.setVisibility(View.GONE);
            } else {
                mReverseHint.setVisibility(View.GONE);
                if (isHideLines) {
                    mIsHideLines = true;
                    mReverseLine.setVisibility(View.GONE);
                } else {
                    mIsHideLines = false;
                    mReverseLine.setVisibility(View.VISIBLE);
                }
                mReverseSurfaceView.setVisibility(View.VISIBLE);
                mReverseWindow.setVisibility(View.VISIBLE);
                if (mReverseSurfaceView != null) {
                    /*
                     * if (mFlipState) {
                     * mReverseSurfaceView.setRotationY((float) 180.0); } else {
                     * mReverseSurfaceView.setRotationY((float) 0.0); }
                     */
                }
            }
            Log.d(TAG, "onRerverseMode mIsReverseHolderReady=" + mIsReverseHolderReady);
            if (mRecService != null && mIsReverseHolderReady) {
                mRecService.setPreviewDisplay(CameraInfo.CAMERA_FACING_BACK, mReverseHolder);
                Log.d(TAG, "onRerverseMode isPreview=" + mRecService.isPreview(CameraInfo.CAMERA_FACING_BACK));
                if (!mRecService.isPreview(CameraInfo.CAMERA_FACING_BACK)) {
                    mRecService.startPreview(CameraInfo.CAMERA_FACING_BACK);
                }
            }
        } else {
            for (int i = 0; i < CAM_NUM; i++) {
                if (mFoatId[i] == CameraInfo.CAMERA_FACING_BACK && mIsReverse) {
                    setDisplayId(mFoatId[i], i);
                }
            }
            mIsReverse = false;
            mIsHideLines = false;
            mReverseWindow.setVisibility(View.GONE);

        }
    }

    public void setCameraPlug(boolean isOut) {
        Log.i(TAG, "setCameraPlug isOut = " + isOut);
        mIsCameraOut = isOut;
        if (mIsCameraOut) {
            if (mIsReverse) {
                mReverseHint.setVisibility(View.VISIBLE);
                mReverseSurfaceView.setVisibility(View.GONE);
                mReverseLine.setVisibility(View.GONE);
            } else {
                for (int i = 0; i < CAM_NUM; i++) {
                    if (mFoatId[i] == CameraInfo.CAMERA_FACING_BACK) {
                        //by lym start
                        if (CustomValue.ONLY_ONE_CAMERA || CustomValue.CAMERA_NOT_RECORD) {
                            mFloatHint[i].setVisibility(View.GONE);
                        } else {
                            mFloatHint[i].setVisibility(View.VISIBLE);
                        }
                        //end
                        mAppIcon[i].setVisibility(View.GONE);
                        mBackSurfaceView[i].setVisibility(View.GONE);
                        mFrontSurfaceView[i].setVisibility(View.GONE);
                        mThirdSurfaceView[i].setVisibility(View.GONE);
                    }
                }
            }

        } else {
            if (mIsReverse) {
                mReverseHint.setVisibility(View.GONE);
                mReverseSurfaceView.setVisibility(View.VISIBLE);
                if (mIsHideLines) {
                    mReverseLine.setVisibility(View.GONE);
                } else {
                    mReverseLine.setVisibility(View.VISIBLE);
                }
            } else {
                for (int i = 0; i < CAM_NUM; i++) {
                    if (mFoatId[i] == CameraInfo.CAMERA_FACING_BACK) {
                        mFloatHint[i].setVisibility(View.GONE);
                        mAppIcon[i].setVisibility(View.GONE);
                        mBackSurfaceView[i].setVisibility(View.VISIBLE);
                        mFrontSurfaceView[i].setVisibility(View.GONE);
                        mThirdSurfaceView[i].setVisibility(View.GONE);
                    }
                }
            }
        }
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(TAG, action);
            if (StreamMediaWindow.ACTION_STREAM_MEDIA_WIDOW_HIDE.equals(action)) {
                for (int i = 0; i < CAM_NUM; i++) {
                    if (mFoatId[i] == CameraInfo.CAMERA_FACING_BACK) {
                        if (mBackSurfaceView[i].getVisibility() == View.VISIBLE) {
                            mRecService.setPreviewDisplay(CameraInfo.CAMERA_FACING_BACK, mBackHolder[i]);
                            Log.i(TAG, "back stream isPreview---------=="
                                    + mRecService.isPreview(CameraInfo.CAMERA_FACING_BACK));
                            if (!mRecService.isPreview(CameraInfo.CAMERA_FACING_BACK)) {
                                mRecService.startPreview(CameraInfo.CAMERA_FACING_BACK);
                            }
                            break;
                        }
                    }
                    if (mFoatId[i] == CameraInfo.CAMERA_FACING_FRONT) {
                        if (mFrontSurfaceView[i].getVisibility() == View.VISIBLE) {
                            mRecService.setPreviewDisplay(CameraInfo.CAMERA_FACING_FRONT, mFrontHolder[i]);
                            Log.i(TAG, "front stream isPreview---------=="
                                    + mRecService.isPreview(CameraInfo.CAMERA_FACING_FRONT));

                            if (!mRecService.isPreview(CameraInfo.CAMERA_FACING_FRONT)) {
                                Log.i(TAG, "front preview--startPreview--");
                                mRecService.startPreview(CameraInfo.CAMERA_FACING_FRONT);
                            }
                            break;
                        }
                    }
                }
            } else if (StreamPreViewWindow.ACTION_STREAM_PREVIEW_WIDOW_HIDE.equals(action)) {

                for (int i = 0; i < CAM_NUM; i++) {
                    if (mFoatId[i] == CameraInfo.CAMERA_FACING_BACK) {
                        Log.i(TAG, "----mFoatId[i] == CameraInfo.CAMERA_FACING_BACK------");
                        if (mBackSurfaceView[i].getVisibility() == View.VISIBLE) {
                            Log.i(TAG, "----mBackSurfaceView[i].getVisibility() == View.VISIBLE------");
                            mRecService.setPreviewDisplay(CameraInfo.CAMERA_FACING_BACK, mBackHolder[i]);
                            Log.i(TAG, "front preview isPreview---------=="
                                    + mRecService.isPreview(CameraInfo.CAMERA_FACING_FRONT));
                            if (!mRecService.isPreview(CameraInfo.CAMERA_FACING_BACK)) {
                                Log.i(TAG, "----mRecService.startPreview(CameraInfo.CAMERA_FACING_BACK);------");
                                mRecService.startPreview(CameraInfo.CAMERA_FACING_BACK);
                            }
                            break;
                        }
                    }
                    if (mFoatId[i] == CameraInfo.CAMERA_FACING_FRONT) {
                        Log.i(TAG, "----mFoatId[i] == CameraInfo.CAMERA_FACING_FRONT------");
                        if (mFrontSurfaceView[i].getVisibility() == View.VISIBLE) {
                            Log.i(TAG, "----mFrontSurfaceView[i].getVisibility() == View.VISIBLE------");
                            mRecService.setPreviewDisplay(CameraInfo.CAMERA_FACING_FRONT, mFrontHolder[i]);
                            Log.i(TAG, "front preview isPreview---------=="
                                    + mRecService.isPreview(CameraInfo.CAMERA_FACING_FRONT));
                            if (!mRecService.isPreview(CameraInfo.CAMERA_FACING_FRONT)) {
                                Log.i(TAG, "----mRecService.startPreview(CameraInfo.CAMERA_FACING_FRONT);------");
                                mRecService.startPreview(CameraInfo.CAMERA_FACING_FRONT);
                            }
                            break;
                        }
                    }
                }
            } else if (ACTION_SHOW.equals(action)) {
                show();
            } else if (ACTION_HIDE.equals(action)) {
                hide();
            }
        }
    };

    public void onDestroy() {
        if (receiver != null) {
            mRecService.unregisterReceiver(receiver);
            receiver = null;
        }
        if (mWindowManager != null) {
            for (int i = 0; i < CAM_NUM; i++) {
                if (mFloatWindow[i] != null) {
                    mWindowManager.removeView(mFloatWindow[i]);
                }
            }
            if (mReverseWindow != null) {
                mWindowManager.removeView(mReverseWindow);
            }
        }
        for (int i = 0; i < CAM_NUM; i++) {
            mBackHolder[i] = null;
            mFrontHolder[i] = null;
            mThirdHolder[i] = null;
            mFloatWindow[i] = null;
            mAppIcon[i] = null;
            mBackSurfaceView[i] = null;
            mFrontSurfaceView[i] = null;
            mThirdSurfaceView[i] = null;
            mFloatHint[i] = null;
            mFloatAppIcon[i] = null;
        }
        mFloatAppIcon = null;
        mFoatId = null;
        mIsBackHolderReady = null;
        mIsFrontHolderReady = null;
        mIsThirdHolderReady = null;
        mReverseHolder = null;
        mBackHolder = null;
        mFrontHolder = null;
        mThirdHolder = null;
        mFloatWindow = null;
        mAppIcon = null;
        mBackSurfaceView = null;
        mFrontSurfaceView = null;
        mWindowManager = null;
        mFloatWinParam = null;
        mFloatHint = null;
        mReverseWindow = null;
        mReverseSurfaceView = null;
        mReverseHint = null;
        mReverseLine = null;
    }

    public static boolean getInitFirstWindow() {
        return isInitFirstWindow;
    }

    public static void setInitFirstWindow(boolean is) {
        isInitFirstWindow = is;
    }

    public void updateWindowPosition(int position) {
        Log.d(TAG, "zdt --- updateFloatWindow, " + position);
        if (mWmParams != null) {
            Log.d(TAG, "zdt --- mWmParams != null ");
            if (position == ON_LEFT) {
                mWmParams.x = 0;
                mWmParams.y = 80;
            } else {
                if (CustomValue.FULL_WINDOW) {
                    mWmParams.x = 0;
                    mWmParams.y = 80;
                } else {
                    mWmParams.x = 874;
                    mWmParams.y = 407;
                }

            }
            if (CustomValue.CAMERA_NOT_RECORD) {
                mWmParams.height = 0;
                mWmParams.width = 0;
            }
            for (int i = 0; i < CAM_NUM; i++) {
                mWindowManager.updateViewLayout(mFloatWindow[i], mWmParams);
            }
        }
    }

    public void updateWindowSurface() {
        Log.d(TAG, "zdt --- updateWindowSurface");
        for (int i = 0; i < CAM_NUM; i++) {
//			if (mFoatId[i] == CameraInfo.CAMERA_FACING_BACK) {
//				
//			}
            mFloatHint[i].setVisibility(View.GONE);
            mAppIcon[i].setVisibility(View.GONE);
            mBackSurfaceView[i].setVisibility(View.GONE);
            mFrontSurfaceView[i].setVisibility(View.VISIBLE);
            mThirdSurfaceView[i].setVisibility(View.GONE);
        }
    }

    public void setShow(int VISIBLE) {
        mBackSurfaceView[0].setVisibility(VISIBLE);
        if (VISIBLE != 1) {
            mFloatWindow[0].setVisibility(VISIBLE);
            mFrontSurfaceView[0].setVisibility(VISIBLE);
            mThirdSurfaceView[0].setVisibility(VISIBLE);
        }
    }
}
