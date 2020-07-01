package com.android.camera.v66;

import com.android.camera.home_recorder.HomeRecorderActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera.CameraInfo;
import android.os.Bundle;
import android.util.Log;

public class MyBroadcastReceiver extends BroadcastReceiver {
    
    public static final int FLAG_ACTIVITY_RUN_IN_RIGHT_WINDOW = 0x00000200;
    public static final String POWER_ON_START = "powerOnStart";
    public static final String EXTRA_CAM_TYPE = "REQ_CAM_TYPE";
    public static final String ACTION_HOME_PRESS = "android.intent.action.HOME_PRESS";
    private static final String ACTION = "android.intent.action.BOOT_COMPLETED";

    private static final String TAG = "FireEyeBootReceive";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) {
            Log.d(TAG, "context =" + context + ";intent=" + intent);
            return;
        }
        
        Log.i(TAG, "action = " + intent.getAction());
        if (intent.getAction().equals(ACTION)) {
           Log.i(TAG, "Receive BOOT_COMPLETED!");
   /*         if (MyPreference.isBootUp()) {
                Intent it = new Intent(context, RecorderActivity.class);
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                it.putExtra(POWER_ON_START, true);
                it.addFlags(FLAG_ACTIVITY_RUN_IN_RIGHT_WINDOW);
                it.putExtra(EXTRA_CAM_TYPE, CameraInfo.CAMERA_FACING_FRONT);
                context.startActivity(it);
            }     */

        } else if (/*intent.getAction().equals(ACTION_HOME_PRESS)
                || */intent.getAction().equals(RecordService.ACTION_START_APP)) {
            Log.i(TAG, "Receive ACTION_HOME_PRESS!");
            Intent it = new Intent(context, RecorderActivity.class);
            it.putExtra(POWER_ON_START, true);
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            it.addFlags(FLAG_ACTIVITY_RUN_IN_RIGHT_WINDOW);
            it.putExtra(EXTRA_CAM_TYPE, CameraInfo.CAMERA_FACING_FRONT);
            context.startActivity(it);
        }
    }
    
}
