package com.android.camera.v66;

import android.content.Context;
import android.content.Intent;
import android.hardware.Camera.Adas;
import android.media.MediaPlayer;
import android.util.Log;

import com.android.camera2.R;

public class RoadwaySoundPlayer {
    
    private static final String TAG = "RoadwaySoundPlayer";
    
    private final static String ADAS_ROADWAY_ACTION = "com.spreadwin.adas.status";
    private MediaPlayer mCrashMP;
    private MediaPlayer mAberrancyMP;
    private boolean mPlayCrashSound;
    private boolean mPlayAberrancySound;
    private Context mContext;
    private long mLastCrashSound = 0;
    private long mLastAberSound = 0;
    
    public RoadwaySoundPlayer(Context context) {
        mContext = context;
    }
    
    public void checkAdas(Adas adas) {
        
        if (adas == null || mCrashMP == null || mAberrancyMP == null) {
            Log.d(TAG, "check adas is null");
            return;
        }
        
        boolean carWarn = false;
        float carDist = 0;
        boolean adasAction = false;
        int adasInfo = 0;//1为碰车,2为左压线,3为右压线
        for (int i = 0; i < adas.cars.num; ++i) {
            if (adas.cars.carP[i].isWarn != 0) {
                if (!carWarn) {
                    carWarn = true;
                    carDist = adas.cars.carP[i].dist;
                }
            }
        }
        
        if (mCrashMP == null) {
            createCrashMP();
        }
        
        if (mCrashMP != null) {
            if (carWarn) {
                if (!mPlayCrashSound) {
                	adasAction = true;
                	adasInfo = 1;
                    mPlayCrashSound = true;
                    mCrashMP.start();
                    mLastCrashSound = System.currentTimeMillis();
                }
            } else {
                if (mPlayCrashSound && Math.abs(System.currentTimeMillis()
                        - mLastCrashSound) > 2000) {
                    mPlayCrashSound = false;
                    mCrashMP.pause();
                    mCrashMP.seekTo(0);
                }
            }
        }
        
        if (mAberrancyMP == null) {
            createAberrancyMp();
        }
        
        if (mAberrancyMP != null) {
            if ((adas.lane.ltWarn != 0) || (adas.lane.rtWarn != 0)) {
                if (!mPlayAberrancySound) {
                	adasAction = true;
                	if (adas.lane.ltWarn != 0) {
                		adasInfo = 2;
                	}else{
                		adasInfo = 3;
                	}
                    mPlayAberrancySound = true;
                    mAberrancyMP.start();
                    mLastAberSound = System.currentTimeMillis();
                }
            } else {
                if (mPlayAberrancySound && Math.abs(System.currentTimeMillis()
                        - mLastAberSound) > 2000) {
                    mPlayAberrancySound = false;
                    mAberrancyMP.pause();
                    mAberrancyMP.seekTo(0);
                }
            }
        }
        if (adasAction) {
            sendAdasBroadcast(adasInfo,carDist);
        }
    }


    private void sendAdasBroadcast(int adasInfo, float carDist) {
        Log.d(TAG, "sendAdasBroadcast adasInfo =="+adasInfo+"; carDist =="+carDist);
        Intent intent = new Intent(ADAS_ROADWAY_ACTION);
        intent.putExtra("status", adasInfo);
        intent.putExtra("dist", carDist);
        mContext.sendBroadcast(intent);
    }
    
    public void startMediaPlayer() {
        createAberrancyMp();
        createCrashMP();
    }
    
    private void createAberrancyMp() {
        Log.v(TAG, "create abbercy sound player");
        if (mAberrancyMP == null) {
            mAberrancyMP = MediaPlayerFactory.ChooseMediaPlayer(mContext,"AberrancyMP");
            if (mAberrancyMP != null) {
                mAberrancyMP.setLooping(false);
            }
        }
    }
    
    private void createCrashMP() {
        Log.v(TAG, "create crash sound player");
        if (mCrashMP == null) {
            mCrashMP = MediaPlayerFactory.ChooseMediaPlayer(mContext,"CrashMP");
            if (mCrashMP != null) {
                mCrashMP.setLooping(false);
                mPlayCrashSound = false;
                mPlayAberrancySound = false;
            }
        }
    }
    
    public void stopMediaPlayer() {
        if (mAberrancyMP != null) {
            mAberrancyMP.stop();
            mAberrancyMP.reset();
            mAberrancyMP.release();
            mAberrancyMP = null;
        }
        if (mCrashMP != null) {
            mCrashMP.stop();
            mCrashMP.reset();
            mCrashMP.release();
            mCrashMP = null;
        }
    }
}
