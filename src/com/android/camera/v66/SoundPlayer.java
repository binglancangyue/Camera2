package com.android.camera.v66;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.util.Log;

import com.android.camera2.R;

public class SoundPlayer {
    
    private static SoundPlayer instance = new SoundPlayer();
    private MediaPlayer mPlayer;
    private boolean mIsStarted = false;
    private OnCompletionListener mOnCompletionListener = new OnCompletionListener() {
        
        @Override
        public void onCompletion(MediaPlayer arg0) {
            // TODO Auto-generated method stub
            if (mPlayer != null) {
                stop();
            }
        }
    };
    
    private SoundPlayer() {
    }
    
    public static SoundPlayer getInstance() {
        return instance;
    }
    
    public synchronized void start(Context context) {
        
        if (mPlayer != null) {
            stop();
        }
        
        mPlayer = MediaPlayer.create(context, R.raw.adjust_lane);
        mPlayer.setOnCompletionListener(mOnCompletionListener);
        mPlayer.start();
        mIsStarted = true;
    }
    
    private void stop() {
    	try {
            if (mPlayer != null) {
            	Log.d("SoundPlayer", "mIsStarted=" + mIsStarted);
                if (mIsStarted && mPlayer.isPlaying()) {
                    mPlayer.stop();
                    mIsStarted = false;
                }
                mPlayer.release();
            }
    	} catch (IllegalStateException ex) {
    		ex.printStackTrace();
    		mIsStarted = false;
    	}
    }
}
