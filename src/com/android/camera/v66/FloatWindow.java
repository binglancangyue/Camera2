package com.android.camera.v66;

import android.app.ActivityManager;
import android.app.Presentation;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.os.SystemClock;
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

public class FloatWindow implements Callback {
    
    private static final String TAG = "FloatWindow";
    private WindowManager.LayoutParams mWmParams;
    private WindowManager mWindowManager;
    private RelativeLayout mFloatWindow = null;
    private RelativeLayout mReverseWindow = null;
    private RecordService mRecService;
    private int mAppIconWidth = 0;
    private int mAppIconHeight = 0;
    private SurfaceView mBackSurfaceView;
    private SurfaceView mFrontSurfaceView;
    private SurfaceView mReverseSurfaceView;
    private SurfaceHolder mBackHolder;
    private SurfaceHolder mFrontHolder;
    private SurfaceHolder mReverseHolder;
    private FrameLayout mAppIcon;
    private TextView mFloatHint;
    private TextView mReverseHint;
    private ImageView mReverseLine;
    private int mFoatId = CameraInfo.CAMERA_FACING_BACK;
    private boolean mIsBackHolderReady = false;
    private boolean mIsFrontHolderReady = false;
    private boolean mIsReverseHolderReady = false;
    private boolean mFlipState = true;
    private boolean mIsReverse = false;
    private boolean mIsHideLines = false;
    private WindowManager.LayoutParams mFloatWinParam = null;
    private boolean mIsCameraOut = false;
    
    public FloatWindow(RecordService service) {
        mRecService = service;
        mWindowManager = (WindowManager) mRecService.getApplication().getSystemService(
                Context.WINDOW_SERVICE);
    }
    
    public void usbPlugNotify(int state) {
        
    }
    
    public void startFloat() {
        Log.i(TAG, "startFloat");
        int marginLeft = 8;
        int marginTop = 60;
        int screenWidth;
        int screenHeight;
        int foatWinWidth;
        int foatWinHeight;
        DisplayMetrics dm = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getMetrics(dm);
        
        screenWidth = dm.widthPixels;
        screenHeight = dm.heightPixels;
        Log.d(TAG, "screenWidth=" + screenWidth + ";screenHeight=" + screenHeight);
        
        foatWinWidth = screenWidth * 160 / 1600;
        foatWinHeight = 90 * screenHeight / 480;
        Log.d(TAG, "foatWinWidth=" + foatWinWidth + ";foatWinHeight=" + foatWinHeight);
        
        marginLeft = screenWidth * 8 / 1600;
        marginTop = foatWinHeight * (60 + 120) / 480;
        Log.d(TAG, "marginLeft=" + marginLeft + ";marginTop=" + marginTop);
        
        mAppIconWidth = screenWidth * 90 / 1600;
        mAppIconHeight = 90 * screenHeight / 480;
        
        marginLeft = 0;
        marginTop = mRecService.getResources().getDimensionPixelSize(
                com.android.internal.R.dimen.status_bar_height) + 19;
        foatWinWidth = (int) mRecService.getResources().getDimension(R.dimen.screen_systemui_width);
        foatWinHeight = foatWinWidth * 9 /16;
        Log.d(TAG, "foatWinWidth=" + foatWinWidth + ";foatWinHeight=" + foatWinHeight);
		if(foatWinWidth==135){
			foatWinWidth=140;
			marginLeft=20;
		}       
        mAppIconWidth = screenWidth * 90 / 1600;
        mAppIconHeight = 90 * screenHeight / 480;
        
        mAppIconWidth = foatWinHeight;
        mAppIconHeight = foatWinHeight;
        
        mFloatWindow = createFloatWindow(R.layout.float_win, Gravity.LEFT | Gravity.TOP,
                marginLeft, marginTop, foatWinWidth, foatWinHeight);
        mAppIcon = (FrameLayout) mFloatWindow.findViewById(R.id.app_icon);
        mBackSurfaceView = (SurfaceView) mFloatWindow.findViewById(R.id.back_preview);
        mBackHolder = mBackSurfaceView.getHolder();
        mBackHolder.addCallback(this);
        mFrontSurfaceView = (SurfaceView) mFloatWindow.findViewById(R.id.front_preview);
        mFrontHolder = mFrontSurfaceView.getHolder();
        mFrontHolder.addCallback(this);
        mWindowManager.addView(mFloatWindow, mWmParams);
        mFloatWinParam = (WindowManager.LayoutParams) mFloatWindow.getLayoutParams();
        mAppIcon.setVisibility(View.GONE);
        mBackSurfaceView.setVisibility(View.GONE);
        mFrontSurfaceView.setVisibility(View.GONE);
        mFloatHint = (TextView) mFloatWindow.findViewById(R.id.hint_text);
        mReverseWindow = (RelativeLayout) getView(R.layout.reverse_win);
        mReverseSurfaceView = (SurfaceView) mReverseWindow.findViewById(R.id.reverse_preview);
        mReverseHolder = mReverseSurfaceView.getHolder();
        mReverseHolder.addCallback(this);
        mReverseHint = (TextView) mReverseWindow.findViewById(R.id.hint_text);
        mReverseLine = (ImageView) mReverseWindow.findViewById(R.id.reverse_lines);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams();
        params.type = LayoutParams.TYPE_SYSTEM_ERROR;// LayoutParams.TYPE_TOAST;
        params.format = PixelFormat.RGBA_8888;
        params.flags = LayoutParams.FLAG_NOT_FOCUSABLE | LayoutParams.FLAG_HARDWARE_ACCELERATED;
        params.gravity = Gravity.LEFT | Gravity.TOP;
        params.x = 0;
        params.y = 0;
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        params.height = WindowManager.LayoutParams.MATCH_PARENT;
        params.windowAnimations = android.R.style.Animation_Translucent;
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
        params.type = LayoutParams.TYPE_SYSTEM_ERROR;// LayoutParams.TYPE_TOAST;
        params.format = PixelFormat.RGBA_8888;
        params.flags = LayoutParams.FLAG_NOT_FOCUSABLE | LayoutParams.FLAG_HARDWARE_ACCELERATED;
        params.gravity = gravity;
        params.x = x;
        params.y = y;
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
    
    public void setOnclickListener(View.OnClickListener listener) {
        if (mFloatWindow != null) {
            mFloatWindow.setOnClickListener(listener);
        }
    }
    
    public void performClick() {
        if (mFloatWindow != null) {
            mFloatWindow.performClick();
        }
    }
    
    public void setDisplayId(int cameraId) {
        mFoatId = cameraId;
        show();
        if (CameraInfo.CAMERA_FACING_BACK == cameraId) {
            if (mIsCameraOut) {
        		mFloatHint.setVisibility(View.VISIBLE);
                mAppIcon.setVisibility(View.GONE);
                mBackSurfaceView.setVisibility(View.GONE);
                mFrontSurfaceView.setVisibility(View.GONE);
            } else {
                mAppIcon.setVisibility(View.GONE);
                mBackSurfaceView.setVisibility(View.VISIBLE);
                mFrontSurfaceView.setVisibility(View.GONE);
                mFloatHint.setVisibility(View.GONE);
            }
            if (mRecService != null && mIsBackHolderReady) {
                /*
                 * if(mRecService.isRender(cameraId)) {
                 * mRecService.stopRender(cameraId); }
                 * if(mRecService.isPreview(cameraId)) {
                 * mRecService.stopPreview(cameraId); }
                 */
                mRecService.setPreviewDisplay(cameraId, mBackHolder);
                if (!mRecService.isPreview(cameraId)) {
                    mRecService.startPreview(cameraId);
                }
                // mRecService.startPreview(cameraId);
                // mRecService.startRender(cameraId);
            }
            setFlip();
            
        } else if (CameraInfo.CAMERA_FACING_FRONT == cameraId) {
            mAppIcon.setVisibility(View.GONE);
            mBackSurfaceView.setVisibility(View.GONE);
            mFrontSurfaceView.setVisibility(View.VISIBLE);
            mFloatHint.setVisibility(View.GONE);
            if (mRecService != null && mIsFrontHolderReady) {
                /*
                 * if(mRecService.isRender(cameraId)) {
                 * mRecService.stopRender(cameraId); }
                 * if(mRecService.isPreview(cameraId)) {
                 * mRecService.stopPreview(cameraId); }
                 */
                mRecService.setPreviewDisplay(cameraId, mFrontHolder);
                if (!mRecService.isPreview(cameraId)) {
                    mRecService.startPreview(cameraId);
                }
                // mRecService.startPreview(cameraId);
                // mRecService.startRender(cameraId);
            }
        } else {
            mBackSurfaceView.setVisibility(View.GONE);
            mFrontSurfaceView.setVisibility(View.GONE);
            mFloatHint.setVisibility(View.GONE);
            mAppIcon.setVisibility(View.VISIBLE);
        }
    }
    
    public void addImageView(ImageView view) {
        Log.d(TAG, "addImageView");
        if (mAppIcon != null) {
            mAppIcon.setVisibility(View.VISIBLE);
            mAppIcon.removeAllViews();
            if (view != null) {
                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(mAppIconWidth,
                        mAppIconHeight);
                view.setLayoutParams(lp);
                mAppIcon.addView(view);
                show();
            }
        }
    }
    
    public void show() {
        if (mFloatWindow != null) {
            mFloatWindow.setVisibility(View.VISIBLE);
        }
    }
    
    public void hide() {
        if (mFloatWindow != null) {
            mFloatWindow.setVisibility(View.GONE);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {

    }
    
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // TODO Auto-generated method stub
        if (mBackSurfaceView != null && mBackSurfaceView.getHolder() == holder) {
            mIsBackHolderReady = true;
            if (mRecService != null && mFoatId == CameraInfo.CAMERA_FACING_BACK) {
                /*
                 * if(mRecService.isRender(CameraInfo.CAMERA_FACING_BACK)) {
                 * mRecService.stopRender(CameraInfo.CAMERA_FACING_BACK); }
                 * if(mRecService.isPreview(CameraInfo.CAMERA_FACING_BACK)) {
                 * mRecService.stopPreview(CameraInfo.CAMERA_FACING_BACK); }
                 */
                mBackHolder = holder;
                mRecService.setPreviewDisplay(CameraInfo.CAMERA_FACING_BACK,
                        mBackHolder);
                if (!mRecService.isPreview(CameraInfo.CAMERA_FACING_BACK)) {
                    mRecService.startPreview(CameraInfo.CAMERA_FACING_BACK);
                }
                // mRecService.startPreview(CameraInfo.CAMERA_FACING_BACK);
                // mRecService.startRender(CameraInfo.CAMERA_FACING_BACK);
            }
            setFlip();
        } else if (mFrontSurfaceView != null && mFrontSurfaceView.getHolder() == holder) {
            mIsFrontHolderReady = true;
            if (mRecService != null && mFoatId == CameraInfo.CAMERA_FACING_FRONT) {
                /*
                 * if(mRecService.isRender(CameraInfo.CAMERA_FACING_FRONT)) {
                 * mRecService.stopRender(CameraInfo.CAMERA_FACING_FRONT); }
                 * if(mRecService.isPreview(CameraInfo.CAMERA_FACING_FRONT)) {
                 * mRecService.stopPreview(CameraInfo.CAMERA_FACING_FRONT); }
                 */
                mFrontHolder = holder;
                mRecService.setPreviewDisplay(CameraInfo.CAMERA_FACING_FRONT,
                        mFrontHolder);
                if (!mRecService.isPreview(CameraInfo.CAMERA_FACING_FRONT)) {
                    mRecService.startPreview(CameraInfo.CAMERA_FACING_FRONT);
                }
                // mRecService.startPreview(CameraInfo.CAMERA_FACING_FRONT);
                // mRecService.startRender(CameraInfo.CAMERA_FACING_FRONT);
            }
        } else if (mReverseSurfaceView != null
                && mReverseSurfaceView.getHolder() == holder) {
            mIsReverseHolderReady = true;
            mReverseHolder = holder;
            if (mRecService != null && mIsReverse) {
                mRecService.setPreviewDisplay(CameraInfo.CAMERA_FACING_BACK,
                        mReverseHolder);
                if (!mRecService.isPreview(CameraInfo.CAMERA_FACING_BACK)) {
                    mRecService.startPreview(CameraInfo.CAMERA_FACING_BACK);
                }
            }
        }
    }
    
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // TODO Auto-generated method stub
        if (mBackSurfaceView != null && mBackSurfaceView.getHolder() == holder) {
            mIsBackHolderReady = false;
            mBackHolder = null;
        } else if (mFrontSurfaceView != null && mFrontSurfaceView.getHolder() == holder) {
            mIsFrontHolderReady = false;
            mFrontHolder = null;
        } else if (mReverseSurfaceView != null
                && mReverseSurfaceView.getHolder() == holder) {
            mIsReverseHolderReady = false;
            mReverseHolder = null;
        }
    }
    
    public void setFlipState(boolean isFlip) {
        mFlipState = isFlip;
        setFlip();
    }
    
    private void setFlip() {
        if (mBackSurfaceView != null) {
            /*if (mFlipState) {
                mBackSurfaceView.setRotationY((float) 180.0);
            } else {
                mBackSurfaceView.setRotationY((float) 0.0);
            }*/
        }
    }
    
    public void onRerverseMode(boolean isReverse, boolean isHideLines) {
        Log.d(TAG, "onRerverseMode isReverse=" + isReverse);
        if (mReverseWindow == null) {
            return;
        }
        if (isReverse) {
            mIsReverse = true;
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
                    /*if (mFlipState) {
                        mReverseSurfaceView.setRotationY((float) 180.0);
                    } else {
                        mReverseSurfaceView.setRotationY((float) 0.0);
                    }*/
                }
            }
            if (mRecService != null && mIsReverseHolderReady) {
                mRecService.setPreviewDisplay(CameraInfo.CAMERA_FACING_BACK,
                        mReverseHolder);
                if (!mRecService.isPreview(CameraInfo.CAMERA_FACING_BACK)) {
                    mRecService.startPreview(CameraInfo.CAMERA_FACING_BACK);
                }
            }
        } else {
            if (mFoatId == CameraInfo.CAMERA_FACING_BACK && mIsReverse) {
                setDisplayId(mFoatId);
            }
            mIsReverse = false;
            mIsHideLines = false;
            mReverseWindow.setVisibility(View.GONE);
        }
    }
    
    public void setCameraPlug(boolean isOut) {
    	mIsCameraOut = isOut;
    	if (mIsCameraOut) {
    		if (mIsReverse) {
                mReverseHint.setVisibility(View.VISIBLE);
                mReverseSurfaceView.setVisibility(View.GONE);
                mReverseLine.setVisibility(View.GONE);
    		} else {
    			if (mFoatId == CameraInfo.CAMERA_FACING_BACK ) {
            		mFloatHint.setVisibility(View.VISIBLE);
                    mAppIcon.setVisibility(View.GONE);
                    mBackSurfaceView.setVisibility(View.GONE);
                    mFrontSurfaceView.setVisibility(View.GONE);
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
    			if (mFoatId == CameraInfo.CAMERA_FACING_BACK ) {
            		mFloatHint.setVisibility(View.GONE);
                    mAppIcon.setVisibility(View.GONE);
                    mBackSurfaceView.setVisibility(View.VISIBLE);
                    mFrontSurfaceView.setVisibility(View.GONE);
    			}
    		}
    	}
    }
    
    public void onDestroy() {
        if (mWindowManager != null) {
            if (mFloatWindow != null) {
                mWindowManager.removeView(mFloatWindow);
            }
            if (mReverseWindow != null) {
                mWindowManager.removeView(mReverseWindow);
            }
        }
        mReverseHolder = null;
        mBackHolder = null;
        mFrontHolder = null;
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
}
