package com.android.camera.v66;


import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import android.os.SystemProperties;

public class FastReverseChecker {
    
    public static final String TAG = "FastReverseChecker";
    public static final int CHECK_DELAY = 200;
    public static final String INTENT_FAST_REVERSE_BOOTUP
            = "intent.softwinner.carlet.FAST_REVERSE_BOOTUP";
    public static final String INTENT_ACTION_FOUR_CAMERA_TURN_LEFT
    = "intent.action.four_camera_turn_left";
    public static final String INTENT_ACTION_FOUR_CAMERA_TURN_RIGHT
    = "intent.action.four_camera_turn_right";
    public static final int MSG_ON_FASTBOOT = 100;
    
    public static final int MSG_FOUR_CAMERA_TURN_LEFT_ON = 103;
    public static final int MSG_FOUR_CAMERA_TURN_LEFT_OFF = 104;
    
    public static final int MSG_FOUR_CAMERA_TURN_RIGHT_ON = 105;
    public static final int MSG_FOUR_CAMERA_TURN_RIGHT_OFF = 106;
    
    public static final String FOUR_CAMERA_TURN_LEFT_STATUS_KEY = "turn_left_status";
    public static final String FOUR_CAMERA_TURN_RIGHT_STATUS_KEY = "turn_right_status";

    private static final int STATUS_TRUE = 1;
    private static final int STATUS_FALSE = 0;
    private static final int FLAG_NEED_EXIT = 1;
    private static final String FILE_STATUS = "/sys/class/switch/parking-switch/state";
    private static final String FILE_EXIT_HANDLE = "/sys/class/car_reverse/needexit";
	private static final String FILE_REVERSE_RESTATUS = "/sys/class/car_reverse/status";
    private static final String FILE_EXITING_BUSY = "/sys/class/switch/parking-switch/busy";
	private static final String FILE_TVD_LOCK = "/sys/class/switch/tvd_lock0/state";
	private static final String FILE_CHECK_LEFT_CAMERA = "/sys/devices/platform/qc_gpio/left_detect";
	private static final String FILE_CHECK_RIGHT_CAMERA = "/sys/devices/platform/qc_gpio/right_detect";

    private boolean mIsRunning = true;
    private boolean mIsNeedExit = true;
    private IFastReverseListener mFastReverseListener = null;
    private RecordService mService;
    private long mStartTime = 0;
    private boolean mIsReversing = false;
    private int mReverseCount = 0;
    private boolean isSupportStreamMedia = SystemProperties.getBoolean("ro.sys.stream.media", false);
    private boolean isPH7 = SystemProperties.getBoolean("ro.sys.isph7", true);
    
    private static boolean isTurnToLeftOn = false;
    private boolean mIsTurnRight = false;
    private int mRightCheckCount = 0;
    private static boolean isReverse = false;
    private static boolean isTurnRight = false;
    
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_ON_FASTBOOT:
                    if (mFastReverseListener != null) {
                        boolean isReversing = (msg.arg1 == 0 ? true : false);
                        isReverse = isReversing;
//                		Log.d(TAG, "MSG_ON_FASTBOOT"+isReversing);
                        mFastReverseListener.onFastReverseBoot(isReversing);
                    }
                    
                    break;
                /*case MSG_FOUR_CAMERA_TURN_LEFT_ON:
                	Log.i(TAG, "---MSG ---MSG_FOUR_CAMERA_TURN_LEFT_ON");
                	if (null != mService) {
                		mService.showFullscreenCameraInner(FullScreenCameraActivity.STATE_FULLSCREEN_SHOW_CAMERA_LEFT_BACK,false,false);
					}
                	break;*/
                /*case MSG_FOUR_CAMERA_TURN_LEFT_OFF:
                	Log.i(TAG, "---MSG ---MSG_FOUR_CAMERA_TURN_LEFT_OFF");
                	Intent turnLeftOffIntent = new Intent(INTENT_ACTION_FOUR_CAMERA_TURN_LEFT);
                	turnLeftOffIntent.putExtra(FOUR_CAMERA_TURN_LEFT_STATUS_KEY, false);
                	mService.sendBroadcast(turnLeftOffIntent);
                	break;*/
                	
                case MSG_FOUR_CAMERA_TURN_RIGHT_ON://MSG_FOUR_CAMERA_TURN_RIGHT_ON
//                	Log.e(TAG, "---MSG ---MSG_FOUR_CAMERA_TURN_RIGHT_ON");
                	isTurnRight = true;
                	if (null != mService) {
                		boolean isRightCamOpen = mService.IsRightCameraOpen();
                		
                		if(isRightCamOpen) {                			
                			mService.showFullscreenCameraInner(FullScreenCameraActivity.STATE_FULLSCREEN_SHOW_CAMERA_RIGHT_BACK,false,true);
                			Intent it = new Intent(INTENT_FAST_REVERSE_BOOTUP);
                            it.putExtra("isReverseing", true);
                            if (mService != null) {
                            	mService.sendBroadcast(it);
                            }
                		}else {
                			sendEmptyMessageDelayed(MSG_FOUR_CAMERA_TURN_RIGHT_ON, 1000);
                			Log.d(TAG, "MSG_FOUR_CAMERA_TURN_RIGHT_ON-> isRightCamOpen == false");
                		}
					}
                	break;
                case MSG_FOUR_CAMERA_TURN_RIGHT_OFF://MSG_FOUR_CAMERA_TURN_RIGHT_OFF
                	Log.i(TAG, "---MSG ---MSG_FOUR_CAMERA_TURN_RIGHT_OFF");
                	isTurnRight = false;
                	boolean isRightCamOpen = mService.IsRightCameraOpen();
                	if (isRightCamOpen) {
                		Intent turnRightOffIntent = new Intent(INTENT_ACTION_FOUR_CAMERA_TURN_RIGHT);
                		turnRightOffIntent.putExtra(FOUR_CAMERA_TURN_RIGHT_STATUS_KEY, false);
                		mService.sendBroadcast(turnRightOffIntent);
                		Intent it = new Intent(INTENT_FAST_REVERSE_BOOTUP);
                        it.putExtra("isReverseing", false);
                        if (mService != null) {
                        	mService.sendBroadcast(it);
                        }
                	} else {
                		Log.d(TAG, "MSG_FOUR_CAMERA_TURN_RIGHT_OFF-> isRightCamOpen == false");
                	}
                	break;
                default:
                    break;
            }
            super.handleMessage(msg);
        }
    };
    
    public static boolean isTurnRight() {
    	return isTurnRight;
    }
    
    public static boolean isReversing() {
    	return isReverse;
    }
    
    private Thread mCheckerThread = new Thread(new Runnable() {
        @Override
        public void run() {
            // TODO Auto-generated method stub
            mStartTime = System.currentTimeMillis();
            Intent it = null;
            while (mIsRunning) {
                /*
                 * try {//for test Thread.sleep(20*1000); } catch
                 * (InterruptedException e) { // TODO Auto-generated catch block
                 * e.printStackTrace(); }
                 */
                /*if (Math.abs(System.currentTimeMillis() - mStartTime) > 20000) {
                    mIsNeedExit = false;
                }*/
            	
            	/*if (readBootStatus(FILE_CHECK_LEFT_CAMERA)) {
            		if (!isTurnToLeftOn) {
            			mHandler.sendEmptyMessage(MSG_FOUR_CAMERA_TURN_LEFT_ON);
            			isTurnToLeftOn = true;
            		}
            	}else{
            		if (isTurnToLeftOn) {
            			mHandler.sendEmptyMessage(MSG_FOUR_CAMERA_TURN_LEFT_OFF);
            			isTurnToLeftOn = false;
            		}
            	}*/

//                Log.d(TAG, "run:isSupportStreamMedia " + isSupportStreamMedia + " mIsNeedExit " +
//                        mIsNeedExit + " readBootStatus(FILE_STATUS) " + readBootStatus(FILE_STATUS)+
//                        " mIsReversing "+mIsReversing);
                if (mIsNeedExit) {
                    writeExitFlag(FILE_EXIT_HANDLE, FLAG_NEED_EXIT);
                    if(isSupportStreamMedia){
                    	while ((readBootStatus(FILE_STATUS) && readFastReverseStatus()) || readBootStatus(FILE_EXITING_BUSY)) {
                    		try {
                    			Thread.sleep(CHECK_DELAY);
                    		} catch (InterruptedException e) {
                    			// TODO Auto-generated catch block
                    			e.printStackTrace();
                    		}
                    	}
                    } else {
                        Log.d(TAG, "run: ");
                    	while ((readBootStatus(FILE_STATUS) && readFastReverseStatus() && readBootStatus(FILE_TVD_LOCK)) || readBootStatus(FILE_EXITING_BUSY)) {
                    		try {
                    			Thread.sleep(CHECK_DELAY);
                    		} catch (InterruptedException e) {
                    			// TODO Auto-generated catch block
                    			e.printStackTrace();
                    		}
                    	}
                    }
                    Log.d(TAG, "run:go 1 ");
                    mIsNeedExit = false;
                    Message msg = mHandler.obtainMessage();
                    msg.what = MSG_ON_FASTBOOT;
                    msg.arg1 = 1;
                    mHandler.sendMessage(msg);
                    it = new Intent(INTENT_FAST_REVERSE_BOOTUP);
                    it.putExtra("isReverseing", false);
                    if (mService != null) {
                    	mService.sendBroadcast(it);
                    }
                }
                if(isSupportStreamMedia){
                	if ((readBootStatus(FILE_STATUS))) {
                        // mIsRunning = false;
                        if (mReverseCount < 3) {
                            mReverseCount++;
                        } else {
                            if (!mIsReversing) {
                                Log.d(TAG, "run: go2");
                                Message msg = mHandler.obtainMessage();
                                msg.what = MSG_ON_FASTBOOT;
                                msg.arg1 = 0;
                                mHandler.sendMessage(msg);
                                it = new Intent(INTENT_FAST_REVERSE_BOOTUP);
                                it.putExtra("isReverseing", true);
                                if (mService != null) {
                                	mService.sendBroadcast(it);
                                }
                                
                            }
                            mIsReversing = true;
                        }
//                         Log.d(TAG, "readBootStatus111111 =" +
//                         readBootStatus(FILE_EXIT_HANDLE));
                    } else {
                        if (mIsReversing) {
                            Log.d(TAG, "run: go3");
                            Message msg = mHandler.obtainMessage();
                            msg.what = MSG_ON_FASTBOOT;
                            msg.arg1 = 1;
                            mHandler.sendMessage(msg);
                            it = new Intent(INTENT_FAST_REVERSE_BOOTUP);
                            it.putExtra("isReverseing", false);
                            if (mService != null) {
                            	mService.sendBroadcast(it);
                            }
                            
                        }
                        mIsReversing = false;
                        mReverseCount = 0;
                    }
                }else{
                	
//                    Log.e(TAG, "MSG_ON_FASTBOOT7777777=" +readBootStatus(FILE_STATUS) +"readBootStatus(FILE_TVD_LOCK) "+readBootStatus(FILE_TVD_LOCK));
//                    Log.e(TAG, "readFastReverseStatus()=" +readFastReverseStatus() );
                	
//                	if ((readBootStatus(FILE_STATUS) && readBootStatus(FILE_TVD_LOCK))) {
                    if (readBootStatus(FILE_STATUS)) {
                        // mIsRunning = false;
                		
//                		 Log.e(TAG, "MSG_ON_FASTBOOT888888="+mIsReversing );
                        if (mReverseCount < 3) {
                            mReverseCount++;
                        } else {
                            if (!mIsReversing) {
                                Log.d(TAG, "run: go4");
                                Message msg = mHandler.obtainMessage();
                                msg.what = MSG_ON_FASTBOOT;
                                msg.arg1 = 0;
                                mHandler.sendMessage(msg);
                                it = new Intent(INTENT_FAST_REVERSE_BOOTUP);
                                it.putExtra("isReverseing", true);
                                if (mService != null) {
                                	mService.sendBroadcast(it);
                                }
                                
//                                Log.d(TAG, "MSG_ON_FASTBOOT55555=" );
                            }
                            mIsReversing = true;
                        }
                        // Log.d(TAG, "readBootStatus =" +
                        // readBootStatus(FILE_EXIT_HANDLE));
                    } else {
//                   	 Log.e(TAG, "MSG_ON_FASTBOOT99999=" );
//                        Log.d(TAG, "run: go5");
                        if (mIsReversing) {
                            Message msg = mHandler.obtainMessage();
                            msg.what = MSG_ON_FASTBOOT;
                            msg.arg1 = 1;
                            mHandler.sendMessage(msg);
                            it = new Intent(INTENT_FAST_REVERSE_BOOTUP);
                            it.putExtra("isReverseing", false);
                            if (mService != null) {
                            	mService.sendBroadcast(it);
                            }
                            
//                            Log.d(TAG, "MSG_ON_FASTBOOT66666=" );
                        }
                        mIsReversing = false;
                        mReverseCount = 0;
                    }
                }
              //TRUE TO RIGHT DIRECTION FLAG
//				if (readBootStatus(FILE_CHECK_RIGHT_CAMERA)/* &&readBootStatus(FILE_TVD_LOCK) */) {
//					if (!mIsTurnRight)
//						mRightCheckCount = !isPH7 ? mRightCheckCount + 1 : 0;
//					else
//						mRightCheckCount = !isPH7 ? 0 : mRightCheckCount + 1;
//					;
//					boolean isCondit1 = !isPH7 ? !mIsTurnRight : mIsTurnRight;
//					boolean isCondit2 = !isPH7 ? mRightCheckCount >= 2 : mRightCheckCount >= 10;
//					if (isCondit1 && isCondit2) {
//						if (!isPH7) {
//							mHandler.sendEmptyMessage(MSG_FOUR_CAMERA_TURN_RIGHT_ON);
//						} else {
//							mHandler.sendEmptyMessage(MSG_FOUR_CAMERA_TURN_RIGHT_OFF);
//						}
//						mIsTurnRight = !isPH7 ? true : false;
//						mRightCheckCount = 0;
//					}
//				} else {
//					if (!mIsTurnRight)
//						mRightCheckCount = isPH7 ? mRightCheckCount + 1 : 0;
//					else
//						mRightCheckCount = isPH7 ? 0 : mRightCheckCount + 1;
//					boolean isCondit1 = isPH7 ? !mIsTurnRight : mIsTurnRight;
//					boolean isCondit2 = isPH7 ? mRightCheckCount >= 2 : mRightCheckCount >= 10;
//					if (isCondit1 && isCondit2) {
//						if (isPH7) {
//							mHandler.sendEmptyMessage(MSG_FOUR_CAMERA_TURN_RIGHT_ON);
//						} else {
//							mHandler.sendEmptyMessage(MSG_FOUR_CAMERA_TURN_RIGHT_OFF);
//						}
//						mIsTurnRight = isPH7 ? true : false;
//						mRightCheckCount = 0;
//					}
//				}
                
                try {
                    Thread.sleep(CHECK_DELAY);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            mHandler = null;
            mFastReverseListener = null;
        }
    });

    public FastReverseChecker(RecordService mService) {
    	this.mService = mService;
    }
    
    public void setNeedExit(boolean isNeed) {
        mIsNeedExit = isNeed;
    }
    
    // only can start, no stop handler
    public void start() {
        mCheckerThread.start();
    }
    
    public void stop() {
        mIsRunning = false;
        mCheckerThread = null;
    }

    public interface IFastReverseListener {
        void onFastReverseBoot(boolean isReversing);
    }
    
    public void setFastReverseListener(IFastReverseListener ls) {
        mFastReverseListener = ls;
    }

    public boolean readBootStatus() {
    	if(isSupportStreamMedia){
    		return (readBootStatus(FILE_STATUS) && readFastReverseStatus()) || readBootStatus(FILE_EXITING_BUSY);
        }else {
        	return (readBootStatus(FILE_STATUS) && readFastReverseStatus() && readBootStatus(FILE_TVD_LOCK)) || readBootStatus(FILE_EXITING_BUSY);
        }
    }
    
    private boolean readBootStatus(String file) {
        boolean isBoot = false;
        FileReader reader = null;
        try {
            reader = new FileReader(file);
            char[] buf = new char[15];
            int nn = reader.read(buf);
            if (nn > 0) {
                Log.d(TAG, "readBootStatus: isBoot "+isBoot);
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
    
	private boolean readFastReverseStatus() {
        boolean isBoot = false;
        FileReader reader = null;
        try {
            reader = new FileReader(FILE_REVERSE_RESTATUS);
            char[] buf = new char[15];
            int nn = reader.read(buf);
            if (nn > 0) {
                String starOrStop = new String(buf, 0, nn - 1);
				if(starOrStop.equals("start"))
					isBoot = true;
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
    private void writeExitFlag(String file, int flag) {
        FileWriter writer = null;
        try {
            writer = new FileWriter(file);
            writer.write(String.valueOf(flag));
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
}
