package com.android.camera.v66;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.IntentFilter;
import android.content.ServiceConnection;

public class MyService extends Service

{
    static final String TAG = "MyService";
    boolean connected = false;
    private RecordService mRecordService = null;
    SpreadActionReceiver mActionReceiver = new SpreadActionReceiver();
    private ServiceConnection mRecConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            if (mRecordService == null) {
                mRecordService = ((RecordService.LocalBinder) service)
                        .getService();
                connected = true;
                if (mRecordService != null) {
                    mRecordService.onHomePressed();
                }
                Log.i(TAG, "RecordService::onServcieConnectd" + mRecordService);
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.i(TAG, "RecordService::onServcieDisconnectd");
            mRecordService = null;
        }
    };

    public class MyServiceImpl extends IMyService.Stub {

        @Override
        public void onHomePress() throws RemoteException {
            Log.d(TAG, "hcl onHomePress");
            if (!connected) {
                Intent intent = new Intent(MyService.this, RecordService.class);
                bindService(intent, mRecConnection, Context.BIND_AUTO_CREATE);
            }
            if (mRecordService != null) {
                Log.d(TAG, "hcl mRecordService.getFloatCameraId()="
                        + mRecordService.getFloatCameraId());
                mRecordService.onHomePressed();
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {

        Log.d(TAG, "hcl onBind");
        IntentFilter spreadFilter = new IntentFilter(RecordService.ACTION_STOP_APP);
        registerReceiver(mActionReceiver, spreadFilter);
        return new MyServiceImpl();

    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "hcl onUnbind");
        unregisterReceiver(mActionReceiver);
        return super.onUnbind(intent);
    }
    @Override
    public void onDestroy() {
        Log.e(TAG, "Release MyService");
        super.onDestroy();
    }

    private class SpreadActionReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            String action = intent.getAction();
            Log.i(TAG, "SpreadActionReceiver action = " + action + ";connected=" + connected);
            if (connected) {
                unbindService(mRecConnection);
                connected = false;
                mRecordService = null;
            }
        }
    }

}