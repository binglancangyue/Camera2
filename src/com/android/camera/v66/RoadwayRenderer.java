package com.android.camera.v66;

import android.hardware.Camera.Adas;
import android.opengl.GLSurfaceView.Renderer;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class RoadwayRenderer implements Renderer {
    public static final int NO_DRAWABLE_ID_SMALL_VIDEO = 1;
    public static final int NO_DRAWABLE_ID_CONTROL_VIDE = 2;
    public static final int NO_DRAWABLE_ID_SETTING = 3;
    public static final int[] RECT_COLOR = { 0x600EFDE4, 0x6004B4EE };
    // public static final int RECT_COLOR[] = {0x600000ff, 0x6000ff00};
    public static final int RECT_COLOR_RED = 0xFFFF0000;
    
    private static final String TAG = "RoadwayRenderer";
    private static final int ONE = 0x10000;
    private static final float[] COLOR_0 = { 0x04 * 1f / 0xFF, 0xB4 * 1f / 0xFF, 0xEE * 1f / 0xFF,
            0.5f, };
    private static final float[] COLOR_1 = { 0x0E * 1f / 0xFF, 0xFD * 1f / 0xFF, 0xE4 * 1f / 0xFF,
            0x60 * 1f / 0xFF, };
    private static final float[] COLOR_RED = { 0xFB * 1f / 0xFF, 0x39 * 1f / 0xFF,
            0x39 * 1f / 0xFF, 0xFF * 1f / 0xFF, };
    
    private static final float[] COLOR_GREEN = { 0x00 * 1f / 0xFF, 0xCD * 1f / 0xFF,
            0x00 * 1f / 0xFF, 0xFF * 1f / 0xFF, };
    
    private static final float[] COLOR_YELLOW = { 0xEE * 1f / 0xFF, 0xEE * 1f / 0xFF,
            0x00 * 1f / 0xFF, 0xFF * 1f / 0xFF, };
    
    private static final float[] COLOR_BLUE = { 0x00 * 1f / 0xFF, 0x00 * 1f / 0xFF,
            0xCC * 1f / 0xFF, 0x77 * 1f / 0xFF, };
    
    private static final long WARN_DELAY = 500;
    
    private float mRatio = 1;
    private boolean mDisplayEnabled = true;
    private Adas mAdas;
    private Object mLock = new Object();
    private boolean mSmallVideoEnable = false;
    private boolean mControlViewEnable = false;
    private boolean mSettingViewEnable = false;
    private float[] mArgs = new float[14];
    private int[] mVertexts1 = new int[13];
    private boolean mIsLeftWarn = false;
    private boolean mIsRightWarn = false;
    private long mLastLeftWarn = 0;
    private long mLastRightWarn = 0;
    
    // private static final float[] COLOR_0 = {
    // 0x00 * 1f / 0xFF,
    // 0x00 * 1f / 0xFF,
    // 0xFF * 1f / 0xFF,
    // 0x60 * 1f / 0xFF,
    // };
    //
    // private static final float[] COLOR_1 = {
    // 0x00 * 1f / 0xFF,
    // 0xFF * 1f / 0xFF,
    // 0x00 * 1f / 0xFF,
    // 0x60 * 1f / 0xFF,
    // };
    
    public RoadwayRenderer() {
    }
    
    public void setAdas(Adas adas) {
        
        if ((mAdas == null && adas.lane.isDisp != 1)) {
            Log.v(TAG, "roadway car lane: first display");
        }
        
        if (mAdas != null && adas.lane.isDisp == 0 && mAdas.lane.isDisp != 0) {
            Log.v(TAG, "roadway car lane: not display");
        }
        
        if (mAdas != null && adas.lane.isDisp != 0 && mAdas.lane.isDisp == 0) {
            Log.v(TAG, "roadway car lane: diaplay");
        }
        
        synchronized (mLock) {
            mAdas = adas;
        }
        
    }
    
    public void setDisplayEnabled(boolean enable) {
        mDisplayEnabled = enable;
    }
    
    public void setNoDrawableArea(int noDrawId, boolean enable) {
        switch (noDrawId) {
            case NO_DRAWABLE_ID_SMALL_VIDEO:
                mSmallVideoEnable = enable;
                break;
            case NO_DRAWABLE_ID_CONTROL_VIDE:
                mControlViewEnable = enable;
                Log.v(TAG, " control ui displayed = " + !enable);
                break;
            case NO_DRAWABLE_ID_SETTING:
                mSettingViewEnable = enable;
                break;
            default:
                break;
        }
    }
    
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mRatio = (float) width / height;
        
        Log.d(TAG, "onSurfaceChanged ratio = " + mRatio + ", width=" + width + ",  height="
                + height);
        
        // 设置OpenGL场景
        gl.glViewport(0, 0, width, height);
        
        // 设置投影矩阵
        gl.glMatrixMode(GL10.GL_PROJECTION);
        // 重置投影矩阵
        gl.glLoadIdentity();
        // 设置视口大小
        gl.glFrustumf(-mRatio, mRatio, -1, 1, 1, 10);
        
        // 选择模型观察矩阵
        gl.glMatrixMode(GL10.GL_MODELVIEW);
        // 重置模型观察矩阵
        gl.glLoadIdentity();
        
    }
    
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // 启用阴影平滑
        gl.glShadeModel(GL10.GL_SMOOTH);
        
        gl.glClearColor(0, 0, 0, 0);
        
        // 设置深度缓存
        gl.glClearDepthf(1.0f);
        
        // 启用深度测试
        gl.glEnable(GL10.GL_DEPTH_TEST);
        
        // 所做深度测试类型
        gl.glDepthFunc(GL10.GL_LEQUAL);
        
        // 告诉系统对透视进行修正
        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_FASTEST);
    }
    
    @Override
    public void onDrawFrame(GL10 gl) {
        
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
        gl.glClearColor(0, 0, 0, 0);
        gl.glEnable(GL10.GL_BLEND);
        gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
        
        if (!mDisplayEnabled) {
            return;
        }
        
        synchronized (mLock) {
            if (mAdas == null || mAdas.lane.isDisp == 0) {
                return;
            }
            
            if (mSettingViewEnable) {
                return;
            }
            
            gl.glLoadIdentity();
            gl.glTranslatef(-0.0f, -0.0f, -2f);
            
            gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
            
            int width = (int) (4 * ONE * mRatio);
            int height = 4 * ONE;
            
            float subW = mAdas.subWidth;
            float subH = mAdas.subHeight;
            boolean isLeftWarn = false;
            boolean isRightWarn = false;
            if (mAdas.lane.ltWarn != 0 ) {
                mLastLeftWarn = System.currentTimeMillis();
                isLeftWarn = true;
                mIsLeftWarn = true;
            } else if (mIsLeftWarn) {
                if (Math.abs(System.currentTimeMillis() - mLastLeftWarn) < WARN_DELAY) {
                    isLeftWarn = true;
                } else {
                    isLeftWarn = false;
                    mLastLeftWarn = 0;
                    mIsLeftWarn = false;
                }
            } else {
                isLeftWarn = false;
                mLastLeftWarn = 0;
                mIsLeftWarn = false;
            }
            
            if (mAdas.lane.rtWarn != 0 ) {
                mLastRightWarn = System.currentTimeMillis();
                isRightWarn = true;
                mIsRightWarn = true;
            } else if (mIsRightWarn) {
                if (Math.abs(System.currentTimeMillis() - mLastRightWarn) < WARN_DELAY) {
                    isRightWarn = true;
                } else {
                    isRightWarn = false;
                    mLastRightWarn = 0;
                    mIsRightWarn = false;
                }
            } else {
                isRightWarn = false;
                mLastRightWarn = 0;
                mIsRightWarn = false;
            }
            Log.w(TAG, "mAdas.lane.colorPointsNum - 1=" + (mAdas.lane.colorPointsNum - 1));
            for (int i = 0; i < mAdas.lane.colorPointsNum - 1; i++) {
                
                if (mAdas.lane.ltCols[i] > subW || mAdas.lane.ltCols[i + 1] > subW
                        || mAdas.lane.rtCols[i] > subW || mAdas.lane.rtCols[i + 1] > subW
                        || mAdas.lane.rows[i] > subH || mAdas.lane.rows[i + 1] > subH) {
                    Log.w(TAG, "Track: some value too large" + ", max(" + subW + "," + subH + ")"
                            + ", p0(" + mAdas.lane.ltCols[i] + "," + mAdas.lane.rows[i] + ")"
                            + ", p1(" + mAdas.lane.rtCols[i] + "," + mAdas.lane.rows[i] + ")"
                            + ", p2(" + mAdas.lane.rtCols[i + 1] + "," + mAdas.lane.rows[i + 1]
                            + ")" + ", p3(" + mAdas.lane.ltCols[i + 1] + ","
                            + mAdas.lane.rows[i + 1] + ")");
                    break;
                }
            }
            if (mAdas.lane.colorPointsNum < 3) {
                return;
            }
            // gl.glDisable(GL10.GL_LIGHTING);
            int divider = mAdas.lane.colorPointsNum / 3;
            int rate = 28;
            int heightRate = 10;
            int random = (int)(Math.random()*10);
            heightRate = heightRate + random - 5;
            float[] args = mArgs;
            
            float laderAllWith = mAdas.lane.rtCols[0] - mAdas.lane.ltCols[0];
            float laderAllBottomWith = mAdas.lane.rtCols[mAdas.lane.colorPointsNum - 1]
                    - mAdas.lane.ltCols[mAdas.lane.colorPointsNum - 1];
            float laderAllHeight = mAdas.lane.rows[mAdas.lane.colorPointsNum - 1]
                    - mAdas.lane.rows[0];
            args[0] = mAdas.lane.ltCols[0] + laderAllWith / rate;
            args[1] = mAdas.lane.rows[0];
            args[2] = mAdas.lane.rtCols[0] - laderAllWith / rate;
            args[3] = mAdas.lane.rows[0];
            
            args[4] = mAdas.lane.ltCols[mAdas.lane.colorPointsNum - 1] + laderAllBottomWith / rate;
            args[5] = mAdas.lane.rows[mAdas.lane.colorPointsNum - 1];
            args[6] = mAdas.lane.rtCols[mAdas.lane.colorPointsNum - 1] - laderAllBottomWith / rate;
            args[7] = mAdas.lane.rows[mAdas.lane.colorPointsNum - 1];
            
            args[8] = subW;
            args[9] = subH;
            
            args[10] = COLOR_BLUE[0];
            args[11] = COLOR_BLUE[1];
            args[12] = COLOR_BLUE[2];
            args[13] = COLOR_BLUE[3];
            drawLadder(gl, args);
            
            int botIndex = divider - divider / heightRate;
            float laderTopWith = mAdas.lane.rtCols[0] - mAdas.lane.ltCols[0];
            float laderBottomWith = mAdas.lane.rtCols[divider] - mAdas.lane.ltCols[divider];
            float laderHeight = mAdas.lane.rows[divider] - mAdas.lane.rows[0];
            float laderBottomX = mAdas.lane.ltCols[0] / heightRate + mAdas.lane.ltCols[divider]
                    * (heightRate - 1) / heightRate;
            float laderBottomY = mAdas.lane.rows[0] / heightRate + mAdas.lane.rows[divider]
                    * (heightRate - 1) / heightRate;
            float bottomDivider = laderBottomWith / rate * (heightRate - 1) / heightRate;
            
            args[0] = mAdas.lane.ltCols[0] - laderTopWith / rate;
            args[1] = mAdas.lane.rows[0];
            args[2] = mAdas.lane.ltCols[0] + laderTopWith / rate;
            args[3] = mAdas.lane.rows[0];
            
            args[4] = laderBottomX - bottomDivider;
            args[5] = laderBottomY;
            args[6] = laderBottomX + bottomDivider;
            args[7] = laderBottomY;
            
            args[8] = subW;
            args[9] = subH;
            
            if (!isLeftWarn) {
                args[10] = COLOR_GREEN[0];
                args[11] = COLOR_GREEN[1];
                args[12] = COLOR_GREEN[2];
                args[13] = COLOR_GREEN[3];
            } else {
                args[10] = COLOR_RED[0];
                args[11] = COLOR_RED[1];
                args[12] = COLOR_RED[2];
                args[13] = COLOR_RED[3];
            }
            drawLadder(gl, args);
            
            args[0] = mAdas.lane.rtCols[0] - laderTopWith / rate;
            args[1] = mAdas.lane.rows[0];
            args[2] = mAdas.lane.rtCols[0] + laderTopWith / rate;
            args[3] = mAdas.lane.rows[0];
            
            laderBottomX = mAdas.lane.rtCols[0] / heightRate + mAdas.lane.rtCols[divider]
                    * (heightRate - 1) / heightRate;
            
            args[4] = laderBottomX - bottomDivider;
            args[5] = laderBottomY;
            args[6] = laderBottomX + bottomDivider;
            args[7] = laderBottomY;
            
            args[8] = subW;
            args[9] = subH;
            
            if (!isRightWarn) {
                args[10] = COLOR_GREEN[0];
                args[11] = COLOR_GREEN[1];
                args[12] = COLOR_GREEN[2];
                args[13] = COLOR_GREEN[3];
            } else {
                args[10] = COLOR_RED[0];
                args[11] = COLOR_RED[1];
                args[12] = COLOR_RED[2];
                args[13] = COLOR_RED[3];
            }
            drawLadder(gl, args);
            
            float laderMidWith = mAdas.lane.rtCols[divider] - mAdas.lane.ltCols[divider];
            float laderMidBottomWith = mAdas.lane.rtCols[divider * 2]
                    - mAdas.lane.ltCols[divider * 2];
            float laderMidHeight = mAdas.lane.rows[divider * 2] - mAdas.lane.rows[divider];
            args[0] = mAdas.lane.ltCols[divider] - laderMidWith / rate;
            args[1] = mAdas.lane.rows[divider];
            args[2] = mAdas.lane.ltCols[divider] + laderMidWith / rate;
            args[3] = mAdas.lane.rows[divider];
            
            laderBottomWith = mAdas.lane.rtCols[divider * 2] - mAdas.lane.ltCols[divider * 2];
            laderBottomX = mAdas.lane.ltCols[divider] / heightRate + mAdas.lane.ltCols[divider * 2]
                    * (heightRate - 1) / heightRate;
            laderBottomY = mAdas.lane.rows[divider] / heightRate + mAdas.lane.rows[divider * 2]
                    * (heightRate - 1) / heightRate;
            bottomDivider = laderBottomWith / rate * (heightRate - 1) / heightRate;
            
            args[4] = laderBottomX - bottomDivider;
            args[5] = laderBottomY;
            args[6] = laderBottomX + bottomDivider;
            args[7] = laderBottomY;
            
            args[8] = subW;
            args[9] = subH;
            
            if (!isLeftWarn) {
                args[10] = COLOR_YELLOW[0];
                args[11] = COLOR_YELLOW[1];
                args[12] = COLOR_YELLOW[2];
                args[13] = COLOR_YELLOW[3];
            } else {
                args[10] = COLOR_RED[0];
                args[11] = COLOR_RED[1];
                args[12] = COLOR_RED[2];
                args[13] = COLOR_RED[3];
            }
            drawLadder(gl, args);
            
            args[0] = mAdas.lane.rtCols[divider] - laderMidWith / rate;
            args[1] = mAdas.lane.rows[divider];
            args[2] = mAdas.lane.rtCols[divider] + laderMidWith / rate;
            args[3] = mAdas.lane.rows[divider];
            
            laderBottomX = mAdas.lane.rtCols[divider] / heightRate + mAdas.lane.rtCols[divider * 2]
                    * (heightRate - 1) / heightRate;
            
            args[4] = laderBottomX - bottomDivider;
            args[5] = laderBottomY;
            args[6] = laderBottomX + bottomDivider;
            args[7] = laderBottomY;
            
            args[8] = subW;
            args[9] = subH;
            
            if (!isRightWarn) {
                args[10] = COLOR_YELLOW[0];
                args[11] = COLOR_YELLOW[1];
                args[12] = COLOR_YELLOW[2];
                args[13] = COLOR_YELLOW[3];
            } else {
                args[10] = COLOR_RED[0];
                args[11] = COLOR_RED[1];
                args[12] = COLOR_RED[2];
                args[13] = COLOR_RED[3];
            }
            drawLadder(gl, args);
            
            float laderBotWith = mAdas.lane.rtCols[divider * 2] - mAdas.lane.ltCols[divider * 2];
            float laderBotBottomWith = mAdas.lane.rtCols[mAdas.lane.colorPointsNum - 1]
                    - mAdas.lane.ltCols[mAdas.lane.colorPointsNum - 1];
            float laderBotHeight = mAdas.lane.rows[mAdas.lane.colorPointsNum - 1]
                    - mAdas.lane.rows[divider * 2];
            args[0] = mAdas.lane.ltCols[divider * 2] - laderBotWith / rate;
            args[1] = mAdas.lane.rows[divider * 2];
            args[2] = mAdas.lane.ltCols[divider * 2] + laderBotWith / rate;
            args[3] = mAdas.lane.rows[divider * 2];
            
            laderBottomWith = mAdas.lane.rtCols[mAdas.lane.colorPointsNum - 1]
                    - mAdas.lane.ltCols[mAdas.lane.colorPointsNum - 1];
            laderBottomX = mAdas.lane.ltCols[divider * 2] / heightRate
                    + mAdas.lane.ltCols[mAdas.lane.colorPointsNum - 1] * (heightRate - 1)
                    / heightRate;
            laderBottomY = mAdas.lane.rows[divider * 2] / heightRate
                    + mAdas.lane.rows[mAdas.lane.colorPointsNum - 1] * (heightRate - 1)
                    / heightRate;
            bottomDivider = laderBottomWith / rate * (heightRate - 1) / heightRate;
            
            args[4] = laderBottomX - bottomDivider;
            args[5] = laderBottomY;
            args[6] = laderBottomX + bottomDivider;
            args[7] = laderBottomY;
            
            args[8] = subW;
            args[9] = subH;
            
            args[10] = COLOR_RED[0];
            args[11] = COLOR_RED[1];
            args[12] = COLOR_RED[2];
            args[13] = COLOR_RED[3];
            
            drawLadder(gl, args);
            
            args[0] = mAdas.lane.rtCols[divider * 2] - laderBotWith / rate;
            args[1] = mAdas.lane.rows[divider * 2];
            args[2] = mAdas.lane.rtCols[divider * 2] + laderBotWith / rate;
            args[3] = mAdas.lane.rows[divider * 2];
            
            laderBottomX = mAdas.lane.rtCols[divider * 2] / heightRate
                    + mAdas.lane.rtCols[mAdas.lane.colorPointsNum - 1] * (heightRate - 1)
                    / heightRate;
            
            args[4] = laderBottomX - bottomDivider;
            args[5] = laderBottomY;
            args[6] = laderBottomX + bottomDivider;
            args[7] = laderBottomY;
            
            args[8] = subW;
            args[9] = subH;
            
            args[10] = COLOR_RED[0];
            args[11] = COLOR_RED[1];
            args[12] = COLOR_RED[2];
            args[13] = COLOR_RED[3];
            
            drawLadder(gl, args);
            gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
            gl.glDisable(GL10.GL_BLEND);
            gl.glFlush();
        }
        
    }
    
    private void drawLadder(GL10 gl, float[] agrs) {
        if (gl == null || agrs.length != 14) {
            Log.d(TAG, "drawLadder gl=" + gl + ";agrs.length" + agrs.length);
            return;
        }
        
        int leftTopX = 0;
        int reftTopY = 0;
        
        int rightTopX = 0;
        int rightTopY = 0;
        
        int leftBottomX = 0;
        int leftBottomY = 0;
        
        int rightBottomX = 0;
        int rightBottomY = 0;
        
        int width = (int) (4 * ONE * mRatio);
        int height = 4 * ONE;
        
        float subW = agrs[8];
        float subH = agrs[9];
        
        float red = agrs[10];
        float green = agrs[11];
        float blue = agrs[12];
        float alpha = agrs[13];
        
        leftTopX = (int) convertCoordinate(width, subW, agrs[0]) - width / 2;
        reftTopY = height / 2 - (int) convertCoordinate(height, subH, agrs[1]);
        
        rightTopX = (int) convertCoordinate(width, subW, agrs[2]) - width / 2;
        rightTopY = height / 2 - (int) convertCoordinate(height, subH, agrs[3]);
        
        leftBottomX = (int) convertCoordinate(width, subW, agrs[4]) - width / 2;
        leftBottomY = height / 2 - (int) convertCoordinate(height, subH, agrs[5]);
        
        rightBottomX = (int) convertCoordinate(width, subW, agrs[6]) - width / 2;
        rightBottomY = height / 2 - (int) convertCoordinate(height, subH, agrs[7]);
        
        int[] vertexts1 = mVertexts1;
        vertexts1[0] = leftTopX;
        vertexts1[1] = reftTopY;
        vertexts1[2] = 0;
        vertexts1[3] = rightTopX;
        vertexts1[4] = rightTopY;
        vertexts1[5] = 0;
        vertexts1[6] = leftBottomX;
        vertexts1[7] = leftBottomY;
        vertexts1[8] = 0;
        vertexts1[9] = rightBottomX;
        vertexts1[10] = rightBottomY;
        vertexts1[11] = 0;
        
        gl.glColor4f(red, green, blue, alpha);
        
        ByteBuffer vertextByteBuffer = ByteBuffer.allocateDirect(vertexts1.length * 4);
        
        vertextByteBuffer.order(ByteOrder.nativeOrder());
        IntBuffer vertextBuffer1 = vertextByteBuffer.asIntBuffer();
        vertextBuffer1.put(vertexts1);
        vertextBuffer1.position(0);
        
        gl.glVertexPointer(3, GL10.GL_FIXED, 0, vertextBuffer1);
        gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4);
    }
    
    private float convertCoordinate(float dispVal, float imgVal, float val) {
        return (float) (val * 1.0 / imgVal * dispVal);
    }
    
}
