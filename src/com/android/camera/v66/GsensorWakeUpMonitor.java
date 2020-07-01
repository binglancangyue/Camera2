package com.android.camera.v66;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class GsensorWakeUpMonitor {
    public static final String TAG = "GsensorWakeUpMonitor";
    public static final int CHECK_DELAY = 200;
    public static final int MAX_DEVICES_NUM = 5;
    public static final int MSG_ON_GSENSOR_UP = 1001;
    // only suport 0~255
    public static final int SENSITY_HIGHT = 5;
    public static final int SENSITY_MEDIUM = 10;
    public static final int SENSITY_LOW = 20;
    public static final String INPUT_DEVICE_PATH = "/sys/class/input/input";
    public static final String GSENSOR_DEVICE_NAME = "da380";
    public static final String GSENSOR_NAME_NODE = "/name";
    public static final String GSENSOR_ENABLE_NODE = "/enable";
    public static final String GSENSOR_DELAY_NODE = "/delay";
    public static final String GSENSOR_DATA_NODE = "/axis_data";
    public static final String GSENSOR_IRQ_NODE = "/int2_enable";
    public static final String GSENSOR_IRQ_CLEAR_NODE = "/int2_clear";
    public static final String GSENSOR_SENSITY_NODE = "/threshold";
    public static final String GSENSOR_STATUS_NODE = "/status";
    
    /*
     * slopth values begin----
     * 
     * 0x80: non-latched 0x81: temporary 250ms 0x82: temporary 500ms 0x83:
     * temporary 1s 0x84: temporary 2s 0x85: temporary 4s 0x86: temporary 8s
     * 0x87: latched 0x88: non-latched 0x89: temporary 1ms 0x8a: temporary 1ms
     * 0x8b: temporary 2ms 0x8c: temporary 25ms 0x8d: temporary 50ms 0x8e:
     * temporary 100ms 0x8f: temporary latched
     * 
     * slopth values end----
     */
    public static final String GSENSOR_LOCK_DELAY_NODE = "slopth";

    private static final int STATUS_TRUE = 1;
    
    private String mInputPath;
    private boolean mIsRunning = true;
    private IGsensorWakeListener mWakeListener = null;
    private Context mContext;
    private boolean mIsEnbale = true;
    private boolean mSetEnbale = true;
    private int mSensity;
    private int mSetSensity;
    
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_ON_GSENSOR_UP:
                    if (mWakeListener != null) {
                        mWakeListener.onGsensorWake();
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
                if (mInputPath == null) {
                    mInputPath = getInputDevicePath();
                    if (mInputPath != null) {
                        mIsEnbale = readfileStatus(mInputPath + GSENSOR_ENABLE_NODE);
                        try {
                            mSensity = Integer
                                    .parseInt(readfile(mInputPath + GSENSOR_SENSITY_NODE));
                        } catch (NumberFormatException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
                if (mInputPath != null) {
                    if (mSetEnbale != mIsEnbale) {
                        mIsEnbale = mSetEnbale;
                        writefileStatus(mInputPath + GSENSOR_ENABLE_NODE, mIsEnbale ? 1 : 0);
                    }
                    if (mIsEnbale && readfileStatus(mInputPath + GSENSOR_STATUS_NODE)) {
                        mHandler.sendEmptyMessage(MSG_ON_GSENSOR_UP);
                    }
                    if (mSetSensity != mSensity) {
                        mSensity = mSetSensity;
                        writefileStatus(mInputPath + GSENSOR_SENSITY_NODE, mSensity);
                    }
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
    
    public GsensorWakeUpMonitor(Context context) {
        mContext = context;
        mCheckerThread.start();
    }
    
    private String getInputDevicePath() {
    	File file = null;
        for (int i = 0; i < MAX_DEVICES_NUM; i++) {
            file = new File(INPUT_DEVICE_PATH + i);
            if (file.isDirectory()) {
            	file = new File(INPUT_DEVICE_PATH + i + GSENSOR_NAME_NODE);
                if (file.exists()) {
                    if (readfile(file.getName()).equalsIgnoreCase(GSENSOR_DEVICE_NAME)) {
                        return INPUT_DEVICE_PATH + i;
                    }
                }
            }
            try {
                Thread.sleep(CHECK_DELAY);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return null;
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
    
    private String readfile(String file) {
        String result = null;
        FileReader reader = null;
        try {
            reader = new FileReader(file);
            char[] buf = new char[32];
            int nn = reader.read(buf);
            if (nn > 0) {
                result = new String(buf, 0, nn - 1);
                result.trim();
                return result;
            }
        } catch (IOException ex) {
            // Log.e(TAG, "Couldn't read string from " + file + ": " + ex);
        } catch (NumberFormatException ex) {
            // Log.w(TAG, "Couldn't read string from " + file + ": " + ex);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
        return result;
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
    
    public interface IGsensorWakeListener {
        void onGsensorWake();
    }
    
    public void setGsensorWakeListener(IGsensorWakeListener ls) {
        mWakeListener = ls;
    }
    
    public void enableAccWakeUp(boolean isEnbale) {
        mSetEnbale = isEnbale;
    }
    
    public void setSensity(int sensity) {
        if (sensity == 0) {
            mSetSensity = 8;// 高
        } else if (sensity == 1) {
            mSetSensity = 10;// 中
        } else {
            mSetSensity = 12;// 低
        }
    }
}
