package com.android.camera.v66;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.hardware.Camera.Adas;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.os.SystemProperties;

import com.android.camera2.R;

public class Roadway extends AdjustedView {

    public static final int CAR_COLOR_WHITE = 0;
    public static final int CAR_COLOR_YELLOW = 1;
    public static final int CAR_COLOR_RED = 2;
    public static final int RECT_COLOR_BLUE_IDX = 1;
    public static final int RECT_COLOR_GREEN_IDX = 2;
    public static final int[] RECT_COLOR = { 0x600EFDE4, 0x6004B4EE };
    // public static final int RECT_COLOR[] = {0x600000ff, 0x6000ff00};
    public static final int RECT_COLOR_RED = 0xFFFF0000;
    public static final int FLASH_ADAS_COUNT = 3;
    
    private static final String TAG = "CAM_Roadway";
    private static final boolean LOGV = false;
    
    private Paint mPaint;
    private Path mPath;
    private Adas mAdas;
    private Rect mBounds;
    private int mCount = 0;
    // private boolean mPlayCrashSound;
    // private boolean mPlayAberrancySound;
    // private MediaPlayer mCrashMP;
    // private MediaPlayer mAberrancyMP;
    private float[] mCarLines;
    private boolean mInstallCalMode = false;
    private boolean mFirstLaneDisplay = true;
    private boolean mIsHide = SystemProperties.getBoolean("ro.property.hide_adas",false);
    
    private Handler mHandler = new Handler() {
        
        @Override
        public void handleMessage(Message msg) {
            mInstallCalMode = false;
            Log.d(TAG, "first car lane hidde");
            invalidate();
        }
    };
    
    public interface IRoadwayAdasDetectionListener {
        public void startAdasDetection();
        
        public void stopAdasDetection();
    }
    
    public Roadway(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaint = new Paint();
        mPath = new Path();
        mBounds = new Rect();
        mCount = 0;
        // mPlayCrashSound = false;
        // mPlayAberrancySound = false;
        // mAberrancyMP = null;
        // mCrashMP = null;
        mCarLines = new float[32];
    }
    
    public Roadway(Context context) {
        super(context);
        mPaint = new Paint();
        mPath = new Path();
        mBounds = new Rect();
        mCount = 0;
        // mPlayCrashSound = false;
        // mPlayAberrancySound = false;
        // mAberrancyMP = null;
        // mCrashMP = null;
        mCarLines = new float[32];
    }
    
    public void setAdas(Adas adas) {
        if (LOGV) {
            Log.v(TAG, "setAdas");
        }
        
        mCount++;
        if (mCount < FLASH_ADAS_COUNT) {
            return;
        }
        mCount = 0;
        if (adas == null) {
            return;
        }
        
        if ((mAdas == null && adas.cars.num > 0)) {
            Log.v(TAG, "roadway car distance: first display");
        }
        
        if (mAdas != null && adas.cars.num <= 0 && mAdas.cars.num > 0) {
            Log.v(TAG, "roadway car distance: no car display");
        }
        
        if (mAdas != null && adas.cars.num > 0 && mAdas.cars.num <= 0) {
            Log.v(TAG, "roadway car distance: has cars diaplay");
        }
        
        mAdas = adas;
        
        if (mFirstLaneDisplay && adas.lane.isDisp != 0) {
            Log.d(TAG, "first display car lane");
            
            mFirstLaneDisplay = false;
            //mInstallCalMode = true;
            mHandler.sendEmptyMessageDelayed(1, 30 * 1000);
            
            SoundPlayer.getInstance().start(getContext());
        }
        
        invalidate();
        
    }
    
    public void setHide(boolean isHide) {
        mIsHide = isHide;
    }
    public void setInstallAdjustMode(boolean state) {
        Log.d(TAG, "set install adjust mode state " + state);
        mInstallCalMode = state;
        if (mInstallCalMode) {
            mFirstLaneDisplay = true;
        }
        invalidate();
    }
    
    public static float convertCoordinate(float dispVal, float imgVal, float val) {
        return dispVal * val / imgVal;
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mPaint.setAntiAlias(true);
        if (mIsHide) {
            return;
        }
        if (mAdas == null) {
        	if (mInstallCalMode) {
                int width = mWidth;
                int height = mFixedHeight;
                float midRow = (float) 65 * height / 100;
                float midCol = (float) width / 2;
                float vLen = (float) height - midRow - 2;
                float ang = (float) (55.0 / 180 * Math.PI);
                float hLen = (float) ((float) vLen * Math.tan(ang));
                float leftUpX = midCol;
                float leftUpY = midRow - getCurDelta();
                float leftDnX = midCol - hLen;
                float leftDnY = midRow + vLen - getCurDelta();
                float rightUpX = midCol;
                float rightUpY = midRow - getCurDelta();
                float rightDnX = midCol + hLen;
                float rightDnY = midRow + vLen - getCurDelta();
                
                mPaint.setStrokeWidth((float) 6.0);
                mPaint.setColor(Color.GREEN);
                canvas.drawLine(leftUpX, leftUpY, leftDnX, leftDnY, mPaint);
                canvas.drawLine(rightUpX, rightUpY, rightDnX, rightDnY, mPaint);
        	}
            return;
        }
        
        int subW = mAdas.subWidth;
        int subH = mAdas.subHeight;
        //int width = canvas.getWidth();
        //int height = canvas.getHeight();
        int width = mWidth;
        int height = mFixedHeight;
        boolean carWarn = false;
        
        // Log.i(TAG, "adas score=" + mAdas.score + ", version=" + mAdas.version
        // + "mInstallCalMode=" + mInstallCalMode);
        
        // Log.i(TAG, "isDisp=" + mAdas.lane.isDisp + ", colorPointsNum="
        // + mAdas.lane.colorPointsNum + ", carNum=" + mAdas.cars.num);
        
        /* cars information */
        mPaint.setStyle(Style.FILL);
        mPaint.setTextAlign(Align.CENTER);
        // mPaint.setStyle(Style.STROKE);
        
        // draw car distance unit -- (m) character
        for (int i = 0; i < mAdas.cars.num; ++i) {
            if (mAdas.cars.carP[i].idx.x + mAdas.cars.carP[i].idx.width > subW
                    || mAdas.cars.carP[i].idx.y + mAdas.cars.carP[i].idx.height > subH) {
                Log.w(TAG, "Car: some value too large" + ", max(" + subW + "," + subH + ")"
                        + ", x=" + mAdas.cars.carP[i].idx.x + ", width="
                        + mAdas.cars.carP[i].idx.width + ", y=" + mAdas.cars.carP[i].idx.y
                        + ", height=" + mAdas.cars.carP[i].idx.height);
                break;
            }
            
            float x = convertCoordinate(width, subW, mAdas.cars.carP[i].idx.x);
            float w = convertCoordinate(width, subW, mAdas.cars.carP[i].idx.width);
            float y = convertCoordinate(height, subH, mAdas.cars.carP[i].idx.y);
            float h = convertCoordinate(height, subH, mAdas.cars.carP[i].idx.height);
            
            float size = w / 2;
            mPaint.setTextSize((size < 30) ? 30 : size);
            
            /* draw the distance */
            mPaint.setColor(0xFF000000);
            
            mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            mPaint.setStrokeWidth(6.0f);
            mPaint.setSubpixelText(true);// optimize the text display
                                         // performance
            mPaint.setDither(true);
            mPaint.setAntiAlias(true);
            // canvas.drawText((int)mAdas.cars.carP[i].dist + "m", x + w / 2, y
            // + h - 5, mPaint);
            
            int dist = (int) (mAdas.cars.carP[i].dist + 0.5);
            if (dist == 0) {
            	continue;
            }
            
            String distStr = String.valueOf(dist);
            
            float offsetX = mPaint.measureText(distStr);
            
            if (dist < 10) {
                offsetX += 6f;
            }
            
            canvas.drawText("m", offsetX + x + w / 2, y - 5, mPaint);
            
            mPaint.setColor(0xFFFFFF00);
            mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            
            mPaint.setStrokeWidth(0.0f);
            
            offsetX = mPaint.measureText(String.valueOf(dist));
            if (dist < 10) {
                offsetX += 6f;
            }
            canvas.drawText("m", offsetX + x + w / 2, y - 5, mPaint);
            
        }
        
        for (int i = 0; i < mAdas.cars.num; ++i) {
            if (mAdas.cars.carP[i].idx.x + mAdas.cars.carP[i].idx.width > subW
                    || mAdas.cars.carP[i].idx.y + mAdas.cars.carP[i].idx.height > subH) {
                Log.w(TAG, "Car: some value too large" + ", max(" + subW + "," + subH + ")"
                        + ", x=" + mAdas.cars.carP[i].idx.x + ", width="
                        + mAdas.cars.carP[i].idx.width + ", y=" + mAdas.cars.carP[i].idx.y
                        + ", height=" + mAdas.cars.carP[i].idx.height);
                break;
            }
            if (mAdas.cars.carP[i].color == CAR_COLOR_RED) {
                mPaint.setStrokeWidth((float) 4.0);
                // mPaint.setColor(Color.RED);
                mPaint.setColor(0xFFFB3939);
            } else if (mAdas.cars.carP[i].color == CAR_COLOR_YELLOW) {
                mPaint.setStrokeWidth((float) 2.0);
                mPaint.setColor(0xFFFCFF00);
            } else {
                mPaint.setStrokeWidth((float) 2.0);
                mPaint.setColor(0xFFFFFFFF);
            }
            
            float x = convertCoordinate(width, subW, mAdas.cars.carP[i].idx.x);
            float w = convertCoordinate(width, subW, mAdas.cars.carP[i].idx.width);
            float y = convertCoordinate(height, subH, mAdas.cars.carP[i].idx.y);
            float h = convertCoordinate(height, subH, mAdas.cars.carP[i].idx.height);
            float len = w / 6;
            
            // float x1 = x;
            // float y1 = y;
            // canvas.drawLine(x1, y1, x1 + len, y1, mPaint);
            // canvas.drawLine(x1, y1, x1, y1 + len, mPaint);
            mCarLines[0] = x;
            mCarLines[1] = y;
            mCarLines[2] = x + len;
            mCarLines[3] = y;
            mCarLines[4] = x;
            mCarLines[5] = y;
            mCarLines[6] = x;
            mCarLines[7] = y + len;
            
            // x1 = x + w;
            // canvas.drawLine(x1 - len, y1, x1, y1, mPaint);
            // canvas.drawLine(x1, y1, x1, y1 + len, mPaint);
            mCarLines[8] = x + w - len;
            mCarLines[9] = y;
            mCarLines[10] = x + w;
            mCarLines[11] = y;
            mCarLines[12] = x + w;
            mCarLines[13] = y;
            mCarLines[14] = x + w;
            mCarLines[15] = y + len;
            
            // y1 = y + h;
            // canvas.drawLine(x1 - len, y1, x1, y1, mPaint);
            // canvas.drawLine(x1, y1 - len, x1, y1, mPaint);
            mCarLines[16] = x + w - len;
            mCarLines[17] = y + h;
            mCarLines[18] = x + w;
            mCarLines[19] = y + h;
            mCarLines[20] = x + w;
            mCarLines[21] = y + h - len;
            mCarLines[22] = x + w;
            mCarLines[23] = y + h;
            
            // x1 = x;
            // canvas.drawLine(x1, y1, x1 + len, y1, mPaint);
            // canvas.drawLine(x1, y1 - len, x1, y1, mPaint);
            mCarLines[24] = x;
            mCarLines[25] = y + h;
            mCarLines[26] = x + len;
            mCarLines[27] = y + h;
            mCarLines[28] = x;
            mCarLines[29] = y + h - len;
            mCarLines[30] = x;
            mCarLines[31] = y + h;
            
            canvas.drawLines(mCarLines, mPaint);
            
            float size = w / 2;
            mPaint.setTextSize((size < 30) ? 30 : size);
            mPaint.setStrokeWidth(2.0f);
            
            if (mAdas.cars.carP[i].isWarn != 0) {
                if (!carWarn) {
                    carWarn = true;
                }
                /* draw the center red point */
                mPaint.setColor(0xFFFB3939);
                canvas.drawCircle(x + w / 2, y + h / 2, 10, mPaint);
                /*
                 * //mPaint.setColor(0xFF8AFC1B); mPaint.setColor(Color.YELLOW);
                 * // draw time String text = String.format("%.1fs",
                 * mAdas.cars.carP[i].time); mPaint.getTextBounds(text, 0,
                 * text.length(), mBounds); canvas.drawText(text, x + w / 2, y +
                 * (mBounds.bottom - mBounds.top) + 5, mPaint);
                 */
            }
            /* draw the distance */
            // mPaint.setColor(0xFF8AFC1B);
            mPaint.setColor(0xFF000000);
            
            mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            mPaint.setStrokeWidth(6.0f);
            mPaint.setSubpixelText(true);// optimize the text display
                                         // performance
            mPaint.setDither(true);
            mPaint.setAntiAlias(true);
            // canvas.drawText((int)mAdas.cars.carP[i].dist + "m", x + w / 2, y
            // + h - 5, mPaint);
            
            int dist = (int) (mAdas.cars.carP[i].dist + 0.5);
            if (dist == 0) {
            	continue;
            }
            
            canvas.drawText(String.valueOf(dist), x + w / 2, y - 5, mPaint);
            
            mPaint.setColor(0xFFFFFF00);
            mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            
            mPaint.setStrokeWidth(0.0f);
            canvas.drawText(String.valueOf(dist), x + w / 2, y - 5, mPaint);
            
        }
        
        if (mInstallCalMode) {
            float midRow = (float) 65 * subH / 100;
            float midCol = (float) subW / 2;
            float vLen = (float) subH - midRow - 2;
            float ang = (float) (55.0 / 180 * Math.PI);
            float hLen = (float) ((float) vLen * Math.tan(ang));
            float leftUpX = convertCoordinate(width, subW, midCol);
            float leftUpY = convertCoordinate(height, subH, midRow) - getCurDelta();
            float leftDnX = convertCoordinate(width, subW, midCol - hLen);
            float leftDnY = convertCoordinate(height, subH, midRow + vLen) - getCurDelta();
            float rightUpX = convertCoordinate(width, subW, midCol);
            float rightUpY = convertCoordinate(height, subH, midRow) - getCurDelta();
            float rightDnX = convertCoordinate(width, subW, midCol + hLen);
            float rightDnY = convertCoordinate(height, subH, midRow + vLen) - getCurDelta();
            
            mPaint.setStrokeWidth((float) 6.0);
            mPaint.setColor(Color.GREEN);
            canvas.drawLine(leftUpX, leftUpY, leftDnX, leftDnY, mPaint);
            canvas.drawLine(rightUpX, rightUpY, rightDnX, rightDnY, mPaint);
            // return;
        }
    }
    
    // public void startMediaPlayer() {
    // Context context = getContext();
    // mAberrancyMP = MediaPlayer.create(context, R.raw.adas_aberrancy_warning);
    // mAberrancyMP.setLooping(true);
    // mCrashMP = MediaPlayer.create(context, R.raw.adas_crash_warning);
    // mCrashMP.setLooping(true);
    // mPlayCrashSound = false;
    // mPlayAberrancySound = false;
    // }
    //
    // public void stopMediaPlayer() {
    // if (mAberrancyMP != null) {
    // mAberrancyMP.stop();
    // mAberrancyMP.reset();
    // mAberrancyMP.release();
    // mAberrancyMP = null;
    // }
    // if (mCrashMP != null) {
    // mCrashMP.stop();
    // mCrashMP.reset();
    // mCrashMP.release();
    // mCrashMP = null;
    // }
    // }
}
