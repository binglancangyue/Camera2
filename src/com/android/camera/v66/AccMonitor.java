package com.android.camera.v66;


import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class AccMonitor {
    
    public static final String INTENT_ACC_WAKEUP = "intent.softwinner.carlet.ACC_WAKEUP";
    public static final String INTENT_ACC_DWON = "intent.softwinner.carlet.ACC_DOWN";
    public static final int MSG_ON_ACC_DOWN = 1001;
    public static final int MSG_ON_ACC_WAKE = 1002;
    private static final String TAG = "AccMonitor";
    private static final int CHECK_DELAY = 200;
    private static final int STATUS_TRUE = 1;
    private static final String FILE_STATUS = "/sys/class/gpio_acc/io_status";
    private static final String FILE_WAKE_STATUS = "/sys/class/gpio_acc/irq_status";
    private static final String FILE_ENABLE_HANDLE = "/sys/class/gpio_acc/irq_wakeup_enable";
    
    private boolean mIsRunning = true;
    private IAccWakeListener mWakeListener = null;
    private IAccDownListener mAccDownListener = null;
    private Context mContext;
    private boolean mIsAccDownShutdown = true;
    
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_ON_ACC_DOWN:
                    if (mAccDownListener != null) {
                        mAccDownListener.onAccDown();
                    }
                    if (mIsAccDownShutdown) {
                        Intent intent = new Intent(Intent.ACTION_SHUTDOWN);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        // mContext.startActivity(intent);
                    }
                    break;
                case MSG_ON_ACC_WAKE:
                    if (mWakeListener != null) {
                        mWakeListener.onAccWake();
                    }
                    Intent it = new Intent(INTENT_ACC_WAKEUP);
                    if (mContext != null) {
                        mContext.sendBroadcast(it);
                    }
                    break;
                default:
                    break;
            }
            super.handleMessage(msg);
        }
    };
    
    private Thread mCheckerThread = new Thread(new Runnable() {
        @Override
        public void run() {
            // TODO Auto-generated method stub
            while (mIsRunning) {
                if (!readfileStatus(FILE_STATUS)) {
                    mHandler.sendEmptyMessage(MSG_ON_ACC_DOWN);
                }
                if (readfileStatus(FILE_WAKE_STATUS)) {
                    mHandler.sendEmptyMessage(MSG_ON_ACC_WAKE);
                }
                try {
                    Thread.sleep(CHECK_DELAY);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                
            }
            
        }
    });
    
    public AccMonitor(Context context) {
        mContext = context;
        mCheckerThread.start();
    }
    
    private boolean readfileStatus(String file) {
        boolean isBoot = false;
        FileReader reader = null;
        try {
            reader = new FileReader(file);
            char[] buf = new char[15];
            int nn = reader.read(buf);
            if (nn > 0) {
                isBoot = (STATUS_TRUE == Integer.parseInt(new String(buf, 0, nn - 1)));
            }
        } catch (IOException ex) {
            // Log.e(TAG, "Couldn't read state from " + file + ": " + ex);
        } catch (NumberFormatException ex) {
            // Log.w(TAG, "Couldn't read state from " + file + ": " + ex);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
        return isBoot;
    }
    
    private void writefileStatus(String file, int flag) {
        FileWriter writer = null;
        try {
            writer = new FileWriter(file);
            writer.write(flag);
        } catch (IOException ex) {
            // Log.e(TAG, "Couldn't write state to " + file + ": " + ex);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
        return;
    }
    
    public interface IAccWakeListener {
        void onAccWake();
    }
    
    public void setAccWakeListener(IAccWakeListener ls) {
        mWakeListener = ls;
    }
    
    public interface IAccDownListener {
        void onAccDown();
    }
    
    public void setAccDownListener(IAccDownListener ls) {
        mAccDownListener = ls;
    }
    
    public void setAccDownShutdown(boolean isShutdown) {
        mIsAccDownShutdown = isShutdown;
    }
    
    public void enableAccWakeUp(boolean isWakeUp) {
        writefileStatus(FILE_ENABLE_HANDLE, isWakeUp ? 1 : 0);
    }
}
