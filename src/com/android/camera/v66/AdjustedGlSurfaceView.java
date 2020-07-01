package com.android.camera.v66;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.hardware.Camera.CameraInfo;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;

import com.android.camera2.R;

public class AdjustedGlSurfaceView extends GLSurfaceView {
    
    private static String TAG = "AdjustedGlSurfaceView";
    private static final int STATE_LEFT = 0;
    private static final int STATE_RIGHT = 1;
    private static final int STATE_FULL = 2;
    private static final int STATE_UNKNOW = 3;

    public float LEFTSCREEN_RATIO = 800f / 480;
    public float RIGHTSCREEN_RATIO = 625f / 480;
    public float FULLSCREEN_RATIO = 1425f / 480;
    public float STANDARD_RATIO = 16f / 9;
    public float RATIO_SENSITY = ((800f - 625f) / 2) / 480;
    public int mWidth;
    public int mHeight;
    public int mFixedHeight;
    private int mParentWidth = 0;
    private int mParentHeight = 0;
    private int mWindowState = STATE_UNKNOW;
    private int mMainWidth = 0;
    private int mViceWidth = 0;
    private int mFullWidth = 0;
    private int mWitchSensity = 0;
    // private final GestureDetector mGestureDetector;
    private float mPreY = 0;
    private Context mContext;
    private IWindowsChanged mWindowsChanged;
    private BroadcastReceiver mScrollReceiver;
    private int mCameraId = CameraInfo.CAMERA_FACING_BACK;
    private MyPreference mPref;
    
    public AdjustedGlSurfaceView(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
        mContext = context;
        mParentWidth = (int) mContext.getResources().getDimension(R.dimen.windows_width);
        mParentHeight = (int) mContext.getResources().getDimension(R.dimen.windows_height);
        mMainWidth = (int) mContext.getResources().getDimension(R.dimen.screen_main_width);
        mViceWidth = (int) mContext.getResources().getDimension(R.dimen.screen_vice_width);
        mFullWidth = (int) mContext.getResources().getDimension(R.dimen.screen_full_width);
        mWitchSensity = (int) mContext.getResources().getDimension(R.dimen.screen_sensity_width);
        // mGestureDetector = new
        // GestureDetector(context.getApplicationContext(), new
        // MyGestureListener());}
        if (mContext != null) {
            mPref = MyPreference.getInstance(mContext);
        }
    }
    
    public AdjustedGlSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mParentWidth = (int) mContext.getResources().getDimension(R.dimen.windows_width);
        mParentHeight = (int) mContext.getResources().getDimension(R.dimen.windows_height);
        mMainWidth = (int) mContext.getResources().getDimension(R.dimen.screen_main_width);
        mViceWidth = (int) mContext.getResources().getDimension(R.dimen.screen_vice_width);
        mFullWidth = (int) mContext.getResources().getDimension(R.dimen.screen_full_width);
        mWitchSensity = (int) mContext.getResources().getDimension(R.dimen.screen_sensity_width);
        // TODO Auto-generated constructor stub
        // mGestureDetector = new GestureDetector(context, new
        // MyGestureListener());
        if (mContext != null) {
            mPref = MyPreference.getInstance(mContext);
        }
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        LayoutParams lp = getLayoutParams();
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        //Log.d(TAG, "onMeasure  width=" + width + ";height=" + height);
        if (!checkWidthAndHeight(width, height)) {
            mWidth = width;
            mHeight = height;
            mWindowState = getWindowStateBySize(mWidth, mHeight);
            switch (mWindowState) {
	            case STATE_RIGHT:
	                /*mFixedHeight = (int) (mWidth / STANDARD_RATIO);
	                lp.height = mFixedHeight;
	                lp.width = mWidth;
	                setLayoutParams(lp);
	                setMeasuredDimension(width, mFixedHeight);
	                setTranslationY((mParentHeight - mFixedHeight) / 2);*/
	                // requestLayout();
	                // invalidate();
                    mFixedHeight = height;
                    setTranslationY(0);
                    setMeasuredDimension(width, height);// pass through
	                break;
	            case STATE_FULL:
	                mFixedHeight = (int) (mWidth / STANDARD_RATIO);
	                // scrollBy(0, mHeight - mFixedHeight);//to bottom
	                lp.height = mFixedHeight;
	                lp.width = mWidth;
	                setLayoutParams(lp);
	                setMeasuredDimension(width, mFixedHeight);
	                int tranY = mParentHeight - mFixedHeight;
	                if (mPref != null) {
	                    int yy = mPref.getFullTranslationY(mCameraId);
	                    if (yy != Integer.MAX_VALUE) {
	                        tranY = yy;
	                    }
	                }
	                setTranslationY(tranY);// to bottom
	                break;
	            case STATE_LEFT:
	            default:
	                mFixedHeight = height;
	                setTranslationY(0);
	                setMeasuredDimension(width, height);// pass through
	                break;
            }
        } else {
            setMeasuredDimension(width, height);
        }
        // no child,simplly ignore it;
        // super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
    
    private boolean checkWidthAndHeight(int width, int height) {
        if (mWidth != width || mHeight != height) {
            return false;
        }
    	int state = getWindowStateBySize(width, height);
        if (state == STATE_FULL) {
            return height == ((int)(mWidth / STANDARD_RATIO)) ? true : false;
        } else {
            return height == mFixedHeight;
        }
    }
    
    private int getWindowStateBySize(int width, int height) {
        if (height > 0) {
            /*
             * float curRat = ((float)width)/height; if(Math.abs(curRat -
             * LEFTSCREEN_RATIO) < RATIO_SENSITY ) { return STATE_LEFT; }else
             * if(Math.abs(curRat - RIGHTSCREEN_RATIO) < RATIO_SENSITY ){ return
             * STATE_RIGHT; }else if(Math.abs(curRat - FULLSCREEN_RATIO) <
             * RATIO_SENSITY ){ return STATE_FULL; }
             */
        	if (Math.abs(width - mMainWidth) < mWitchSensity) {
                return STATE_LEFT;
        	} else if (Math.abs(width - mViceWidth) < mWitchSensity) {
                return STATE_RIGHT;
        	} else if (Math.abs(width - mFullWidth) < mWitchSensity) {
                return STATE_FULL;
            }
        }
        return STATE_UNKNOW;
    }
    
    private class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float dx, float dy) {
            // todo no callback?
            float curY = getTranslationY();
            Log.d(TAG, "MyGestureListener onScroll=" + curY);
            curY = curY + e2.getY() - e1.getY();
            int delta = mParentHeight - mFixedHeight;
            if (delta > 0) {
                if (curY < 0) {
                    curY = 0;
                } else if (curY > delta) {
                    curY = delta;
                }
                setTranslationY(curY);
                
            } else {
                if (curY > 0) {
                    curY = 0;
                } else if (curY < delta) {
                    curY = delta;
                }
                setTranslationY(curY);
            }
            return true;
        }
        
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            float curY = getTranslationY();
            // todo no callback?
            Log.d(TAG, "MyGestureListener onFling=" + curY);
            float curDelta = e2.getY() - e1.getY();
            int delta = mParentHeight - mFixedHeight;
            if (delta > 0) {
                if (curDelta < 0) {
                    onFlingAnimation(AdjustedGlSurfaceView.this, curY, 0);
                } else if (curDelta > 0) {
                    onFlingAnimation(AdjustedGlSurfaceView.this, curY, delta);
                }
            } else {
                if (curDelta > 0) {
                    onFlingAnimation(AdjustedGlSurfaceView.this, curY, 0);
                } else if (curDelta < 0) {
                    onFlingAnimation(AdjustedGlSurfaceView.this, curY, delta);
                }
            }
            return true;
        }
        
        @Override
        public boolean onDown(MotionEvent ee) {
            Log.d(TAG, "onDown ========");
            return super.onDoubleTap(ee);
        }
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            mPreY = event.getY();
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            float delta = event.getY() - mPreY;
            mPreY = event.getY();
            float sensity = 20;
            if (delta > sensity || delta < -20) { // simplly missing click
                //onScroll(delta / 2);
            }
        }
        return super.onTouchEvent(event);
    }
    
    public void onScroll(float curDelta) {
        if (/*!(mWindowState == STATE_RIGHT) && */!(mWindowState == STATE_FULL)) {
            return;
        }
        float curY = getTranslationY();
        curY = curY + curDelta;
        Log.d(TAG, "onScroll=" + curY);
        int delta = mParentHeight - mFixedHeight;
        if (delta > 0) {
            if (curY < 0) {
                curY = 0;
            } else if (curY > delta) {
                curY = delta;
            }
            setTranslationY(curY);
            
        } else {
            if (curY > 0) {
                curY = 0;
            } else if (curY < delta) {
                curY = delta;
            }
            setTranslationY(curY);
        }
        Log.d(TAG, "onScroll =" + curY + ";" + delta + ";" + curDelta);
    }
    
    public void onFlingAnimation(View view, float begin, float end) {
        PropertyValuesHolder tranY = PropertyValuesHolder.ofFloat("TranslationY", begin, end);
        ObjectAnimator.ofPropertyValuesHolder(view, tranY).setDuration(500).start();
    }
    
    @Override
    protected void onLayout(boolean changed, int ll, int tt, int rr, int bb) {
        // TODO Auto-generated method stub
        super.onLayout(changed, ll, tt, rr, bb);
        // resume the view to match parent,incase view binded to fixed sized
        LayoutParams lp = getLayoutParams();
        lp.width = LayoutParams.MATCH_PARENT;
        lp.height = LayoutParams.MATCH_PARENT;
        setLayoutParams(lp);
    }
    
    public interface IWindowsChanged {
        void onWindowsChanged(boolean isOnRight);
    }
    
    public void setWindowsChanged(IWindowsChanged wc) {
        mWindowsChanged = wc;
    }
    public void setCameraId(int cameraId) {
        mCameraId = cameraId;
    }
}
