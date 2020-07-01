package com.android.camera.v66;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.hardware.Camera.CameraInfo;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager.LayoutParams;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.os.SystemProperties;

import com.android.camera.v66.ObserveScrollView.ScrollListener;
import com.android.camera2.R;

public class StreamMediaWindow implements Callback,Runnable {

	private static final String TAG = "StreamMediaWindow";
	private static final boolean DEBUG = false;
	public static final String ACTION_STREAM_MEDIA_WIDOW_SHOW  = "com.action.stream_media_window_show";
	public static final String ACTION_STREAM_MEDIA_WIDOW_HIDE  = "com.action.stream_media_window_hide";
	private static final String AUTONAVI_ICON_ACTION = "AUTONAVI_STANDARD_BROADCAST_SEND";
	private RecordService mRecService;
	private WindowManager.LayoutParams wmParams;
	private WindowManager mWindowManager;
	private LayoutInflater inflater;
	private View rootLayout;
	private SurfaceView streamSurfaceView;
	private SurfaceHolder streamSurfaceHolder;
	private ImageView ivRecordStatus;
	private ImageView ivRecordBrand;
	private ImageView ivBack;
	private boolean isShow = false;
	private boolean isMove = false;
	private Animation alphaAnimation2,alphaAnimation1;
	private boolean isRedCircleShow=false;
	public static int LAYOUT_TYPE = SystemProperties.getInt("ro.se.qchome.layouttype", 0);// 配置加载布局
	public static int BRAND_TYPE = SystemProperties.getInt("ro.se.streammedia.brandtype", 0);// 配置加载布局

	private TextView mTvTimeDate, mTvTimeHour, mTvTimeWeekDay,mTvTopHour;
	private Handler mHandler;
	private String weekDay, Hour;
	
	private LinearLayout ll_right_time;
	private RelativeLayout layout_time;
	private RelativeLayout layout_navi;
	private TextView tv_distance;
	private TextView tv_distance_unit;
	private ImageView iv_icon;
	private TextView tv_since;
	private TextView tv_current_road;
	private TextView tv_final_road;
	private static boolean naviVisible = false;
	private DecimalFormat df = new DecimalFormat("####.0");
	private ObserveScrollView scrollView;
	private SharedPreferences scrollPref; 
	private int yValue;
	
	public StreamMediaWindow(RecordService mRecService) {
		this.mRecService = mRecService;
		init();
		//if(LAYOUT_TYPE==6){
			registerBroadcast();
		//}
	}

	private void init() {
		Log.i(TAG, "init()");
		if (null == mRecService) {
			Log.i(TAG, "mRecService is null !");  
			return;
		}
		inflater = LayoutInflater.from(mRecService.getApplication());
		mWindowManager = (WindowManager) mRecService.getApplication().getSystemService(Context.WINDOW_SERVICE);
		wmParams = new WindowManager.LayoutParams();
		wmParams.type = LayoutParams.TYPE_SYSTEM_ERROR;
		// wmParams.type = LayoutParams.TYPE_DISPLAY_OVERLAY;
		wmParams.format = PixelFormat.RGBA_8888;
		wmParams.flags = LayoutParams.FLAG_NOT_FOCUSABLE | LayoutParams.FLAG_HARDWARE_ACCELERATED;		
		wmParams.gravity = Gravity.TOP;
		wmParams.x = 0;
		wmParams.y = 0;
		wmParams.width = WindowManager.LayoutParams.MATCH_PARENT;
		wmParams.height = WindowManager.LayoutParams.MATCH_PARENT;
		scrollPref	= mRecService.getSharedPreferences("scollViewSM", Context.MODE_PRIVATE);
	}

	public void showWindow() {
		Log.e(TAG, "showWindow()");
		if (isShow) {
			Log.i(TAG, "stream media window is already showed!");
			return;
		}
		if (null == rootLayout) {
			if(LAYOUT_TYPE==6){
				rootLayout = inflater.inflate(R.layout.stream_media_win_988, null);
			}else{
				rootLayout = inflater.inflate(R.layout.stream_media_win, null);
			}
			streamSurfaceView = (SurfaceView)rootLayout.findViewById(R.id.stream_media_preview);
			ivRecordStatus = (ImageView)rootLayout.findViewById(R.id.iv_record_status);
			ivRecordBrand = (ImageView)rootLayout.findViewById(R.id.iv_record_brand);
			ivBack = (ImageView) rootLayout.findViewById(R.id.iv_back);
			//if (LAYOUT_TYPE == 6) {
				mTvTimeDate = (TextView) rootLayout.findViewById(R.id.tv_clock_time_date);
				mTvTimeHour = (TextView) rootLayout.findViewById(R.id.tv_clock_time_hour);
				mTvTimeWeekDay = (TextView) rootLayout.findViewById(R.id.tv_clock_time_weekday);
				mTvTopHour = (TextView) rootLayout.findViewById(R.id.tv_top_clock_time_hour);

				layout_navi = (RelativeLayout) rootLayout.findViewById(R.id.layout_navi);
				tv_distance = (TextView) rootLayout.findViewById(R.id.tv_distance);
				tv_distance_unit = (TextView) rootLayout.findViewById(R.id.tv_distance_unit);
				iv_icon = (ImageView) rootLayout.findViewById(R.id.iv_icon);
				tv_since = (TextView) rootLayout.findViewById(R.id.tv_since);
				tv_current_road = (TextView) rootLayout.findViewById(R.id.tv_current_road);
				tv_final_road = (TextView) rootLayout.findViewById(R.id.tv_final_road);
				
				layout_time = (RelativeLayout) rootLayout.findViewById(R.id.ll_top_time);
				ll_right_time = (LinearLayout) rootLayout.findViewById(R.id.ll_time);
			//}
				scrollView= (ObserveScrollView) rootLayout.findViewById(R.id.scrollview);
				scrollView.setScrollListener(new ScrollListener() {
					
					@Override
					public void scrollOritention(int l, int t, int oldl, int oldt) {
						// TODO Auto-generated method stub
						Log.i(TAG, "scrollView----- scrollOritention----yValue=---"+t);
						scrollPref.edit().putFloat("yValue", t).commit();
						scrollPref.edit().putBoolean("isMove", true).commit();
					}
				});
			streamSurfaceView.setOnTouchListener(new OnTouchListener() {

				@Override
				public boolean onTouch(View v, MotionEvent event) {
					//hideWindow();
					if (DEBUG) Log.i(TAG, "onTouch()");
					switch (event.getAction()) {
					case MotionEvent.ACTION_UP:
						if (isMove == false){
							if (DEBUG) Log.i(TAG, "onClick()--ACTION_UP---");
							hideWindow();
						}
						isMove = false;
						break;
					case MotionEvent.ACTION_MOVE:
						if (DEBUG) Log.i(TAG, "onClick()--ACTION_MOVE---");
						isMove = true;
						break;
					case MotionEvent.ACTION_DOWN:
	
						break;

					default:
						break;
					}
					
					return false;
				}
			});
			streamSurfaceView.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					Log.i(TAG, "streamSurfaceView onClick()");
					hideWindow();
				}
			});
			if (null != ivBack) {
				ivBack.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						Log.i(TAG, "ivBack onClick()");
						hideWindow();
					}
				});
			}
		}
		if (null == streamSurfaceHolder) {
			streamSurfaceHolder = streamSurfaceView.getHolder();
			streamSurfaceHolder.addCallback(this);
		}

		alphaAnimation1 = new AlphaAnimation(1.0f, 0);
		alphaAnimation1.setInterpolator(new DecelerateInterpolator());
        alphaAnimation1.setDuration(500);
        
        alphaAnimation2 = new AlphaAnimation(0, 1.0f);
        alphaAnimation2.setInterpolator(new DecelerateInterpolator());
        alphaAnimation2.setDuration(500);
        
        if(isRedCircleShow){
        	ivRecordStatus.startAnimation(alphaAnimation1);
        	ivRecordStatus.startAnimation(alphaAnimation2);
        }
        alphaAnimation1.setAnimationListener(new AnimationListener() {
			
			@Override
			public void onAnimationStart(Animation animation) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onAnimationRepeat(Animation animation) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onAnimationEnd(Animation animation) {
				// TODO Auto-generated method stub
				if(isRedCircleShow){
					ivRecordStatus.startAnimation(alphaAnimation2);
				}
			}
		});
        	alphaAnimation2.setAnimationListener(new AnimationListener() {
			
			@Override
			public void onAnimationStart(Animation animation) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onAnimationRepeat(Animation animation) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onAnimationEnd(Animation animation) {
				// TODO Auto-generated method stub
				if(isRedCircleShow){
					ivRecordStatus.startAnimation(alphaAnimation1);
				}
			}
		});
        
        
		if (isRecording()) {
			ivRecordStatus.setVisibility(View.VISIBLE);
			ivRecordStatus.startAnimation(alphaAnimation1);
        	ivRecordStatus.startAnimation(alphaAnimation2);
			isRedCircleShow=true;
		}else{
			ivRecordStatus.setVisibility(View.GONE);
			isRedCircleShow=false;
		}
		
		//if(LAYOUT_TYPE==6){
			if(!naviVisible){
				layout_time.setVisibility(View.VISIBLE);
			}
		//}
	        if(null != ivRecordBrand){		
			if (BRAND_TYPE == 0) {
				ivRecordBrand.setVisibility(View.GONE);
			}else if(BRAND_TYPE == 1){
				ivRecordBrand.setVisibility(View.VISIBLE);
				ivRecordBrand.setImageResource(R.drawable.iv_ymjd_brand);
			}
		}
		mWindowManager.addView(rootLayout, wmParams);
		scrollView.post(new  Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				if(scrollPref.getBoolean("isMove", false)){
					yValue=(int) scrollPref.getFloat("yValue", 200);
					scrollView.smoothScrollTo(0, yValue);
					Log.i(TAG, "-------scrollView  run()-----yValue---="+yValue);
				}else{
					scrollView.smoothScrollTo(0, 200);
					Log.i(TAG, "---else----scrollView  run()-----yValue---="+yValue);
				}
			}
		});
		isShow = true;
		mRecService.sendBroadcast(new Intent(ACTION_STREAM_MEDIA_WIDOW_SHOW));
		
		//if(LAYOUT_TYPE==6){
			mHandler = new Handler() {
				public void handleMessage(Message msg) {
					String str = (String) msg.obj;
					
					String[] strArray = str.split("-");
					mTvTimeDate.setText(strArray[0]);
					mTvTimeWeekDay.setText(strArray[1]);
					mTvTimeHour.setText(strArray[2]);
					mTvTopHour.setText(strArray[2]);
				}
			};
			
			new Thread(this).start();
		//}
		
	}

	public void hideWindow() {
		Log.i(TAG, "hideWindow()");
		if (!isShow) {
			Log.i(TAG, "stream media window is already hided!");
		}
		if(RecordService.isOnSm){
			Log.v(TAG,"When the stream media is on,open the app, which not let jump before app after it closes");
		}else{			
			if(!TextUtils.isEmpty(RecordService.mTopActivityPackName)){
				boolean isRunning = isAppRunnning(RecordService.mTopActivityPackName);
				Log.d(TAG, RecordService.mTopActivityPackName + " isRunning : " + isRunning);
				if(isRunning){						
					mRecService.launchApp(RecordService.mTopActivityPackName);
				}
			}
		}
		RecordService.mTopActivityPackName = "";
		RecordService.isOnSm = false;
		//if(LAYOUT_TYPE==6){
			layout_navi.setVisibility(View.GONE);
			layout_time.setVisibility(View.GONE);
			ll_right_time.setVisibility(View.GONE);
		//}
		mWindowManager.removeView(rootLayout);
		isShow = false;
		//if(LAYOUT_TYPE==6){
			naviVisible = false;
		//}
		mRecService.sendBroadcast(new Intent(ACTION_STREAM_MEDIA_WIDOW_HIDE));
		mRecService.sendBroadcast(new Intent("com.zqc.action.HIDE_STREAM_MEDIA_WINDOW"));
		//mRecService.sendBroadcast(new Intent("com.zqc.action.HIDE_STREAM_PREVIEW_WINDOW"));
		mRecService.sendBroadcast(new Intent("com.zqc.action.show.after.seconds"));
		mRecService.sendBroadcast(new Intent("com.zqc.action.screen.out.close"));
	}
	public boolean isAppRunnning(String packeName) {
		boolean isAppRunning = false;
		ActivityManager am = (ActivityManager) mRecService.getSystemService(Context.ACTIVITY_SERVICE);
		List<RunningTaskInfo> list = am.getRunningTasks(100);
		for (RunningTaskInfo info : list) {
			if (info.topActivity.getPackageName().equals(packeName)
					|| info.baseActivity.getPackageName().equals(packeName)) {
				isAppRunning = true;
				// find it, break
				break;
			}
		}
		return isAppRunning;
	}
	
	public void closeWindow(){
		Log.i(TAG, "closeWindow()");
		if (!isShow) {
			Log.i(TAG, "stream media window is already close!");
		}
		mWindowManager.removeView(rootLayout);
		isShow = false;
//		mRecService.sendBroadcast(new Intent(ACTION_STREAM_MEDIA_WIDOW_HIDE));
	}

	public boolean isShow() {
		return isShow;
	}
	private boolean isRecording(){
		if (null != mRecService) {
			return mRecService.isRecording(CameraInfo.CAMERA_FACING_BACK);
		}
		return false;
	}
	
	public void startRecording() {
		if (isShow && null != ivRecordStatus) {
			ivRecordStatus.setVisibility(View.VISIBLE);
			ivRecordStatus.startAnimation(alphaAnimation1);
        	ivRecordStatus.startAnimation(alphaAnimation2);
			isRedCircleShow=true;
		}
	}
	
	public void stopRecording() {
		if (null != ivRecordStatus) {
			ivRecordStatus.setVisibility(View.GONE);
			isRedCircleShow=false;
		}
	}
	
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (DEBUG) Log.i(TAG, "surfaceCreated()");
		if (streamSurfaceView != null && streamSurfaceView.getHolder() == holder) {
			streamSurfaceHolder = holder;
			if (mRecService != null) {
				mRecService.setPreviewDisplay(CameraInfo.CAMERA_FACING_BACK, streamSurfaceHolder);
				if (!mRecService.isPreview(CameraInfo.CAMERA_FACING_BACK)) {
					mRecService.startPreview(CameraInfo.CAMERA_FACING_BACK);
				}
			}
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		if (DEBUG) Log.i(TAG, "surfaceChanged()");
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		if (DEBUG) Log.i(TAG, "surfaceDestroyed()");
		if (streamSurfaceView != null && streamSurfaceView.getHolder() == holder) {
			streamSurfaceHolder = null;
		}
	}

	public void destory() {
		Log.i(TAG, "destory()");

	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		  try {
	            while(true){
	                Calendar c = Calendar.getInstance();
	                SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm");
	                long currentTimeMillis = System.currentTimeMillis();
	                weekDay = String.valueOf(c.get(Calendar.DAY_OF_WEEK));
	                Hour=timeFormatter.format(currentTimeMillis);
	                if("1".equals(weekDay)){
	                    weekDay ="星期日";
	                }else if("2".equals(weekDay)){
	                    weekDay ="星期一";
	                }else if("3".equals(weekDay)){
	                    weekDay ="星期二";
	                }else if("4".equals(weekDay)){
	                    weekDay ="星期三";
	                }else if("5".equals(weekDay)){
	                    weekDay ="星期四";
	                }else if("6".equals(weekDay)){
	                    weekDay ="星期五";
	                }else if("7".equals(weekDay)){
	                    weekDay ="星期六";
	                }
	                
	                
	                SimpleDateFormat sdf=new SimpleDateFormat("yyyy/MM/dd");
	                String str=sdf.format(new Date())+"-"+weekDay+"-"+Hour;
	                mHandler.sendMessage(mHandler.obtainMessage(100,str));
	                Thread.sleep(1000);
	            }
	        } catch (InterruptedException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
	        }
		
	}
	
	private void registerBroadcast() {
		Log.i(TAG, "---registerBroadcast---");
		IntentFilter filter = new IntentFilter();
		filter.addAction(AUTONAVI_ICON_ACTION);
		mRecService.registerReceiver(broadcastReceiver, filter);
	}
	private String action = "";
	private int type = -1;
	private int iconState = -1;
	private int remiandDistance = -1;
	private String currRoadName = "";
	private String nextRoadName = "";

	private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

		public void onReceive(android.content.Context context, Intent intent) {
			if (null != intent) {
				action = intent.getAction();
				Log.i(TAG, "action = "+action);
				 if (AUTONAVI_ICON_ACTION.equals(action)) {
					if (!isShow) {
						Log.i(TAG, "screen is not show!");
						return;
					}
					if (DEBUG) Log.i(TAG, "---AUTONAVI_ICON_ACTION---");
					type = intent.getIntExtra("KEY_TYPE", -1);
					Log.i(TAG, "type = " + type);
					if (10001 == type) {// guide information
						iconState = intent.getIntExtra("ICON", -1);
						// remiandDistance =
						// intent.getIntExtra("ROUTE_REMAIN_DIS", -1);
						remiandDistance = intent.getIntExtra("SEG_REMAIN_DIS", -1);
						currRoadName = intent.getStringExtra("CUR_ROAD_NAME");
						nextRoadName = intent.getStringExtra("NEXT_ROAD_NAME");
						if(TextUtils.isEmpty(currRoadName) || TextUtils.isEmpty(nextRoadName)){
							Log.i(TAG,"null navigation information");
							return;
						}
						if (DEBUG) Log.i(TAG, "iconState = " + iconState);
						if (DEBUG) Log.i(TAG, "remiandDistance = " + remiandDistance);
						if (DEBUG) Log.i(TAG, "currRoadName= " + currRoadName + ", nextRoadName = " + nextRoadName);
						if (!naviVisible) {
							layout_navi.setVisibility(View.VISIBLE);
							layout_time.setVisibility(View.GONE);
							ll_right_time.setVisibility(View.VISIBLE);
							naviVisible = true;
						}
						if (remiandDistance >= 0) {
							if (remiandDistance >= 1000) {
								tv_distance.setText(
										df.format(remiandDistance / 1000.0));
								tv_distance_unit.setText(mRecService.getString(R.string.distance_km_unit));
							} else {
								tv_distance.setText(remiandDistance+"");
								tv_distance_unit.setText(mRecService.getString(R.string.distance_meter_unit));
							}
							if (tv_distance_unit.getVisibility() != View.VISIBLE) {
								tv_distance_unit.setVisibility(View.VISIBLE);
							}
						} else {
							tv_distance.setText(mRecService.getString(R.string.distance_hint));
							tv_distance_unit.setVisibility(View.GONE);
						}

						if (1 == iconState) {// self

						} else if (2 == iconState) {// turn left
							iv_icon.setImageResource(R.drawable.hud_sou2);
						} else if (3 == iconState) {// turn right
							iv_icon.setImageResource(R.drawable.hud_sou3);
						} else if (4 == iconState) {// turn left front
							iv_icon.setImageResource(R.drawable.hud_sou4);
						} else if (5 == iconState) {// turn right front
							iv_icon.setImageResource(R.drawable.hud_sou5);
						} else if (6 == iconState) {// turn left behind
							iv_icon.setImageResource(R.drawable.hud_sou6);
						} else if (7 == iconState) {// turn right behind
							iv_icon.setImageResource(R.drawable.hud_sou7);
						} else if (8 == iconState) {// turn around
							iv_icon.setImageResource(R.drawable.hud_sou8);
						} else if (9 == iconState) {// straight
							iv_icon.setImageResource(R.drawable.hud_sou9);
						} else if (10 == iconState) {// arrive somewhere
							iv_icon.setImageResource(R.drawable.hud_sou10);
						} else if (11 == iconState) {// Into the roundabout
							iv_icon.setImageResource(R.drawable.hud_sou11);
						} else if (12 == iconState) {// out of the roundabout
							iv_icon.setImageResource(R.drawable.hud_sou12);
						} else if (13 == iconState) {// Reach the service area
							iv_icon.setImageResource(R.drawable.hud_sou13);
						} else if (14 == iconState) {// Reach the toll station
							iv_icon.setImageResource(R.drawable.hud_sou14);
						} else if (15 == iconState) {// Reach the destination
							iv_icon.setImageResource(R.drawable.hud_sou15);
						} else if (16 == iconState) {// Enter the tunnel
							iv_icon.setImageResource(R.drawable.hud_sou16);
						}

						if ((!TextUtils.isEmpty(currRoadName)) && (!TextUtils.isEmpty(nextRoadName))) {
							tv_since.setText(mRecService.getString(R.string.since));
							tv_current_road.setText(currRoadName);
							int length  = nextRoadName.length();
							if(length>22){
								nextRoadName ="..."+ nextRoadName.substring(length-21);
							}
							tv_final_road.setText(nextRoadName);
						}
					}
				}
			}
		};
	};
	
}
