package com.android.camera.v66;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Camera.CameraInfo;
import android.os.Bundle;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.android.camera.Storage;
import com.android.camera2.R;
import com.android.internal.app.LocalePicker;
import com.android.internal.os.storage.ExternalStorageFormatter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
//add by chengyuzhou
import android.os.SystemProperties;
import android.widget.Toast;
import com.android.camera.v66.MyPreference.IMiddleVideoChanged;

//end
public class SettingsFragment extends Fragment implements OnClickListener {

	public static final String TAG = "SettingsFragment";
	public static final int MW_INVALID_STACK_WINDOW = -1;
	public static final int MW_NORMAL_STACK_WINDOW = 0x0;
	public static final int MW_MAX_STACK_WINDOW = 0x1;
	public static final int MW_MIN_STACK_WINDOW = 0x2;

	private static final int REC_QUA = 0;
	private static final int PIC_QUA = 1;
	private static final int REC_DUA = 2;
	private static final int REAR_FL = 3;
	private static final int CARLN_AD = 0;
	private static final int CRH_SEN = 1;
	private static final int LCK_SEN = 2;
	// private static final int PRK_SEN = 3;
	private static final int CAR_TYPE = 3;
	private static final int SD_TYPE = 4;
	private static final int LIST_ITEM_NUM = 4;
//    private static final int LIST_ITEM_NUM = 3;

	private Button mBackButton;
	private FrameLayout intelligent_detect;
	private ImageView mIntDetect;
	private ImageView mRearFlip, mMiddleState, mRightFlip;
	private ImageView mCarlaneAdjust;
	private TextView mRecQuality;
	private TextView mPicQuality;
	private TextView mRecDuration;
	private TextView mCrashDetect;
	private TextView mLockSensity;
	private TextView mParkSensity;
	private TextView mCarType;
	private View mFormatSd;
	private View viewSpace;
	private ListView mLeftList;
	private BaseAdapter mLeftAdapter;
	private ListView mRightList;
	private BaseAdapter mRightAdapter;
	private Activity mActivity;
	private MyPreference mPreference;
	private ActivityManager mActivityManager;
	private int mOldWindowsStatus;
	private boolean mIsPopuping = false;
	private long mLaseClick = 0;
//    private int cameraNumber=SystemProperties.getInt("ro.sys.float_camera", 1);
	private int cameraNumber = 1;
	public static int LAYOUT_TYPE = SystemProperties.getInt("ro.se.qchome.layouttype", 0);
	private static int[] flipChoice = new int[] { R.string.right_camera_flip, R.string.back_camera_flip };

//    private Handler mHandler = new Handler();
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view;
		Log.d("aaaa", "LAYOUT_TYPE "+LAYOUT_TYPE+" "+RecordService.SPLITSCREEN_SEVEN);
		if (RecordService.SPLITSCREEN_SEVEN) {
			view = inflater.inflate(R.layout.settings_spliteqc7_main, container, false);
		} else {
			if (cameraNumber == 2) {
				view = inflater.inflate(R.layout.settings_qc, container, false);
			} else if (cameraNumber == 1) {
				view = inflater.inflate(R.layout.settings_main, container, false);
				if (LAYOUT_TYPE == 6) {
					Log.d(TAG, "LAYOUT_TYPE==6");
					LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(1136,
							LinearLayout.LayoutParams.MATCH_PARENT);
					view.setLayoutParams(p);
				}
			} else {
				view = inflater.inflate(R.layout.settings_main, container, false);
			}
		}

		mBackButton = (Button) view.findViewById(R.id.back_button);
		intelligent_detect = (FrameLayout) view.findViewById(R.id.intelligent_detect);
		mIntDetect = (ImageView) view.findViewById(R.id.intelligent_detect_switch);
		mFormatSd = view.findViewById(R.id.format_sd);
		mFormatSd.setOnClickListener(this);
		viewSpace = view.findViewById(R.id.view_space);
		mLeftList = (ListView) view.findViewById(R.id.left_list);
		mLeftList.setOnItemClickListener(new LeftListListener());
		mLeftAdapter = new LeftListAdapter();
		mLeftList.setAdapter(mLeftAdapter);
		mRightList = (ListView) view.findViewById(R.id.right_list);
		mRightList.setOnItemClickListener(new RightListListener());
		mRightAdapter = new RightListAdapter();
		mRightList.setAdapter(mRightAdapter);
		mActivity = this.getActivity();
		mPreference = MyPreference.getInstance(mActivity);
		if (MyPreference.isAdasOpen) {
			intelligent_detect.setVisibility(View.VISIBLE);
			viewSpace.setVisibility(View.VISIBLE);
			if (mActivity != null) {
				mIntDetect.setOnClickListener(this);
				if (mPreference.getAdasFlag()) {
					mIntDetect.setImageResource(R.drawable.settings_item_button_on);
				} else {
					mIntDetect.setImageResource(R.drawable.settings_item_button_off);
				}
			}
		} else {
			intelligent_detect.setVisibility(View.GONE);
			viewSpace.setVisibility(View.GONE);
		}
		mPreference.setSettingStateChangerListener(new IMiddleVideoChanged() {

			public void onMiddleVideoChanged(boolean state) {
				if (state) {
					mMiddleState.setImageResource(R.drawable.settings_item_button_on);
				} else {
					mMiddleState.setImageResource(R.drawable.settings_item_button_off);
				}

			}
		});
		int stackId = SplitUtil.getStackBoxId(getActivity());
		int rigtStackId = SplitUtil.getRightStackId(getActivity());
		Log.d(TAG, "stackId =" + stackId);
		mActivityManager = (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);
		if (stackId < -1) {
			mOldWindowsStatus = -40;
		} else if (LAYOUT_TYPE != 6) {
			mOldWindowsStatus = SplitUtil.getWindowSizeStatus(getActivity(), stackId);
			// ActivityManager.MW_MAX_STACK_WINDOW
			// ActivityManager.MW_MIN_STACK_WINDOW
			// ActivityManager.MW_NORMAL_STACK_WINDOW
			SplitUtil.setWindowSize(getActivity(), rigtStackId, MW_MIN_STACK_WINDOW);
			SplitUtil.setWindowSize(getActivity(), stackId, MW_MAX_STACK_WINDOW);
			SplitUtil.setSplitButtonVisibility(getActivity(), false);

		}
		mBackButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				FragmentManager fm = mActivity.getFragmentManager();
				int num = fm.getBackStackEntryCount();
				for (int i = 0; i < num; i++) {
					try {
						fm.popBackStack();
					} catch (IllegalStateException ex) {
						ex.printStackTrace();
					}
				}
				if (mActivity instanceof RecorderActivity) {
					((RecorderActivity) mActivity).resumeState();
				}
				mActivity.sendBroadcast(new Intent(RecordService.ACTION_SHOW_FLOAT_WINDOW));
			}
		});
		return view;
	}

	@Override
	public void onDestroyView() {
		// TODO Auto-generated method stub
		CDRAlertDialog dialog = CDRAlertDialog.getInstance(mActivity);
		if (null != dialog) {
			dialog.dismiss();
		}
		SplitUtil.setSplitButtonVisibility(getActivity(), true);
		super.onDestroyView();
		Log.d(TAG, "onDestroyView");
		/*
		 * if (mOldWindowsStatus > -1) { int stackId =
		 * SplitUtil.getStackBoxId(getActivity());
		 * SplitUtil.setWindowSize(getActivity(), stackId, mOldWindowsStatus); }
		 */
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (!((RecorderActivity) getActivity()).getRecordService().isRecording(CameraInfo.CAMERA_FACING_FRONT)) {
			if (Storage.getTotalSpace() < 0) {
				Toast.makeText(getActivity(), R.string.sdcard_not_found, Toast.LENGTH_LONG).show();
			} else {
				((RecorderActivity) getActivity()).getRecordService().startRecording();
			}
		}

		/*
		 * if(getActivity() != null){ getActivity().unregisterReceiver(mIntentReceiver);
		 * }
		 */
		Log.d(TAG, "onDestroy");
	}

	@Override
	public void onClick(View view) {
		// TODO Auto-generated method stub
		Log.d(TAG, "onClick view=" + view);
		switch (view.getId()) {
		case R.id.intelligent_detect_switch:
			boolean value = mPreference.getAdasFlag();
			value = !value;
			mPreference.saveAdasFlag(value);
			if (value) {
				mIntDetect.setImageResource(R.drawable.settings_item_button_on);
			} else {
				mIntDetect.setImageResource(R.drawable.settings_item_button_off);
			}
			break;
		case R.id.format_sd:
			onSD1Format();
			break;
		default:
			break;
		}

	}

	private class LeftListAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			// add by chengyuzhou
			if (MyPreference.isAdasOpen) {
				if (cameraNumber == 2) {
					// return 4;
					return 3;
				} else {
					// end
					return LIST_ITEM_NUM;
				}
			} else {
				// return 4;
				return 3;
			}

		}

		@Override
		public Object getItem(int arg0) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public long getItemId(int arg0) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public View getView(int position, View itView, ViewGroup parent) {
			// TODO Auto-generated method stub
			View itemView = itView;
			itemView = LayoutInflater.from(getActivity()).inflate(R.layout.settings_item, null);
			TextView title = (TextView) itemView.findViewById(R.id.title);
			ImageView btSwitch = (ImageView) itemView.findViewById(R.id.bt_switch);
			TextView value = (TextView) itemView.findViewById(R.id.value);
			ImageView arrow = (ImageView) itemView.findViewById(R.id.arrow);
			ImageView divider = (ImageView) itemView.findViewById(R.id.divider);
			switch (position) {
			case REC_QUA:
				title.setText(R.string.video_quality);
				value.setText(mPreference.getRecResId(CameraInfo.CAMERA_FACING_FRONT));
				mRecQuality = value;
				break;
			case PIC_QUA:
                	//add by chengyuzhou
               //     title.setText(R.string.picture_quality);
               //     value.setText(mPreference.getPicResId(CameraInfo.CAMERA_FACING_FRONT));
                	title.setText(R.string.reverse_lines);
                	value.setText(mPreference.getReverseLine());
				mPicQuality = value;
				break;
			case REC_DUA:
				title.setText(R.string.video_time);
				value.setText(mPreference.getRecDurationResId());
				mRecDuration = value;
				break;
			case REAR_FL:
				if (cameraNumber == 1) {
//                    title.setText(R.string.camera_flip);
					title.setText(R.string.text_flip);
					btSwitch.setVisibility(View.GONE);
//                    mRearFlip = btSwitch;
//                    if (mPreference.getRearVisionFlip()) {
//                        btSwitch.setImageResource(R.drawable.settings_item_button_on);
//                    } else {
//                        btSwitch.setImageResource(R.drawable.settings_item_button_off);
//                    }
					value.setVisibility(View.GONE);
//                    arrow.setVisibility(View.GONE);
					arrow.setVisibility(View.VISIBLE);
					divider.setVisibility(View.GONE);
				} else if (cameraNumber == 2) {
					title.setText(R.string.middlevideo);
					btSwitch.setVisibility(View.VISIBLE);
					mMiddleState = btSwitch;
					if (mPreference.getMiddleVideoState()) {
						btSwitch.setImageResource(R.drawable.settings_item_button_on);
					} else {
						btSwitch.setImageResource(R.drawable.settings_item_button_off);
					}
					value.setVisibility(View.GONE);
					arrow.setVisibility(View.GONE);
					divider.setVisibility(View.GONE);
				}
				break;
			default:
				break;
			}
			return itemView;
		}

	}

	private class LeftListListener implements OnItemClickListener {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			// TODO Auto-generated method stub
			Log.d(TAG, "onClick view=" + view);
			long curTime = System.currentTimeMillis();
			if (mIsPopuping && Math.abs(mLaseClick - curTime) < 500) {
				return;
			} else if (mIsPopuping) {
				mIsPopuping = false;
			}
			mLaseClick = curTime;
			CDRAlertDialog dialog;
			int res[];
			int cur;
			switch (position) {
			case REC_QUA:
				dialog = CDRAlertDialog.getInstance(mActivity);
				if (null == dialog) {
					return;
				}
				mIsPopuping = true;
				res = mPreference.getResources(MyPreference.RES_REC_QUA);
				cur = mPreference.getRecQuality(CameraInfo.CAMERA_FACING_FRONT);
				dialog.setTitle(R.string.video_quality);
				for (int i = 0; i < res.length; i++) {
					dialog.addItem(res[i], cur == i, false);
				}

				dialog.setCallback(new ICDRAlertDialogListener() {
					@Override
					public void onClick(int state) {
						mPreference.saveRecQuality(CameraInfo.CAMERA_FACING_FRONT, state);
						Log.d(TAG, "onClick value =" + mPreference.getRecResId(CameraInfo.CAMERA_FACING_FRONT));
						mRecQuality.setText(mPreference.getRecResId(CameraInfo.CAMERA_FACING_FRONT));
						mIsPopuping = false;
					}

					@Override
					public void onTimeClick(int hour, int minute) {
					}

					@Override
					public void onDateClick(int year, int month, int day) {
					}
				});
				break;
			case PIC_QUA:
				dialog = CDRAlertDialog.getInstance(mActivity);
				if (null == dialog) {
					return;
				}
				mIsPopuping = true;
                    //add by chengyuzhou
                /*    res = mPreference.getResources(MyPreference.RES_PIC_QUA);
                    cur = mPreference.getPicQualtiy(CameraInfo.CAMERA_FACING_FRONT);
                    dialog.setTitle(R.string.picture_quality);
                    for (int i = 0; i < res.length; i++) {
                        dialog.addItem(res[i], cur == i, false);
                    } */
                    
                    res = mPreference.getResources(MyPreference.RES_REVERSE_LINES);
                    cur = mPreference.getReverseLineResId();
                    dialog.setTitle(R.string.reverse_lines);
				for (int i = 0; i < res.length; i++) {
					dialog.addItem(res[i], cur == i, false);
				}

				dialog.setCallback(new ICDRAlertDialogListener() {
					@Override
					public void onClick(int state) {
                      //add by chengyuzhou
                      /*      mPreference.savePicQuality(CameraInfo.CAMERA_FACING_FRONT, state);
                            mPicQuality.setText(mPreference
                                    .getPicResId(CameraInfo.CAMERA_FACING_FRONT)); */
                        	mPreference.saveReverseLine(state);
                        	mPicQuality.setText(mPreference.getReverseLine());
						mIsPopuping = false;
					}

					@Override
					public void onTimeClick(int hour, int minute) {
					}

					@Override
					public void onDateClick(int year, int month, int day) {
					}
				});
				break;
			case REC_DUA:
				dialog = CDRAlertDialog.getInstance(mActivity);
				if (null == dialog) {
					return;
				}
				mIsPopuping = true;
				res = mPreference.getResources(MyPreference.RES_REC_TIME);
				cur = mPreference.getRecDuration();
				dialog.setTitle(R.string.video_time);
				for (int i = 0; i < res.length; i++) {
					dialog.addItem(res[i], cur == i, false);
				}

				dialog.setCallback(new ICDRAlertDialogListener() {
					@Override
					public void onClick(int state) {
						mPreference.saveRecDuration(state);
						mRecDuration.setText(mPreference.getRecDurationResId());
						mIsPopuping = false;
					}

					@Override
					public void onTimeClick(int hour, int minute) {
					}

					@Override
					public void onDateClick(int year, int month, int day) {
					}
				});
				break;
			case REAR_FL:
				/*
				 * if(cameraNumber==1){ boolean value = mPreference.getRearVisionFlip(); value =
				 * !value; mPreference.saveRearVisionFlip(value); if (value) {
				 * mRearFlip.setImageResource(R.drawable.settings_item_button_on); } else {
				 * mRearFlip.setImageResource(R.drawable.settings_item_button_off); } }else
				 * if(cameraNumber==2){ boolean value = mPreference.getMiddleVideoState(); value
				 * = !value; mPreference.saveMiddleVideoState(value); if (value) {
				 * mMiddleState.setImageResource(R.drawable.settings_item_button_on); } else {
				 * mMiddleState.setImageResource(R.drawable.settings_item_button_off); } }
				 */
				setFlip();
				break;
			default:
				break;
			}
		}
	}

	private class RightListAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			// add by chengyuzhou
			if (MyPreference.isAdasOpen) {
				if (cameraNumber == 2) {
					// return 3;
					return 4;
				} else {
					return LIST_ITEM_NUM;
				}
			} else {
				if (cameraNumber == 2) {
					// return 3;
					return 4;
				} else {
					return 1;
				}
			}

		}

		@Override
		public Object getItem(int arg0) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public long getItemId(int arg0) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public View getView(int position, View itView, ViewGroup parent) {
			// TODO Auto-generated method stub
			View itemView = itView;
			itemView = LayoutInflater.from(getActivity()).inflate(R.layout.settings_item, null);
			TextView title = (TextView) itemView.findViewById(R.id.title);
			ImageView btSwitch = (ImageView) itemView.findViewById(R.id.bt_switch);
			TextView value = (TextView) itemView.findViewById(R.id.value);
			ImageView arrow = (ImageView) itemView.findViewById(R.id.arrow);
			ImageView divider = (ImageView) itemView.findViewById(R.id.divider);
			if (MyPreference.isAdasOpen) {
				switch (position) {
				case CARLN_AD:
					if (cameraNumber == 1) {
						title.setText(R.string.install_adjust);
						btSwitch.setVisibility(View.VISIBLE);
						if (mPreference.getCarLaneAdjust()) {
							btSwitch.setImageResource(R.drawable.settings_item_button_on);
						} else {
							btSwitch.setImageResource(R.drawable.settings_item_button_off);
						}
						mCarlaneAdjust = btSwitch;
						value.setVisibility(View.GONE);
						arrow.setVisibility(View.GONE);
					} else if (cameraNumber == 2) {
						title.setText(R.string.gsensor_lock_level);
						value.setText(mPreference.getLockSensityResId());
						mLockSensity = value;
					}

					break;
				case CRH_SEN:
					if (cameraNumber == 1) {
						title.setText(R.string.distance_detect_level);
						value.setText(mPreference.getCrashSensityResId());
						mCrashDetect = value;
					} else if (cameraNumber == 2) {
//							title.setText(R.string.camera_flip);
						title.setText(R.string.text_flip);
						btSwitch.setVisibility(View.VISIBLE);
						mRearFlip = btSwitch;
						if (mPreference.getRearVisionFlip()) {
							btSwitch.setImageResource(R.drawable.settings_item_button_on);
						} else {
							btSwitch.setImageResource(R.drawable.settings_item_button_off);
						}
						value.setVisibility(View.GONE);
						arrow.setVisibility(View.GONE);
						// divider.setVisibility(View.GONE);
					}
					break;
				case LCK_SEN:
					if (cameraNumber == 1) {
						title.setText(R.string.gsensor_lock_level);
						value.setText(mPreference.getLockSensityResId());
						mLockSensity = value;
					} else if (cameraNumber == 2) {
						// title.setText(R.string.sd1_format);
						title.setText(R.string.camera_right_flip);
						btSwitch.setVisibility(View.VISIBLE);
						mRightFlip = btSwitch;
						if (!mPreference.getRightVisionFlip()) {
							btSwitch.setImageResource(R.drawable.settings_item_button_on);
						} else {
							btSwitch.setImageResource(R.drawable.settings_item_button_off);
						}
						value.setVisibility(View.GONE);
						arrow.setVisibility(View.GONE);
					}
					break;
				/*
				 * case PRK_SEN: title.setText(R.string.gsensor_alarm_level);
				 * value.setText(mPreference.getParkingCrashSensityResId());
				 * divider.setVisibility(View.GONE); mParkSensity = value; break;
				 */
				case SD_TYPE:
					title.setText(R.string.sd1_format);
					break;
				case CAR_TYPE:
					title.setText(R.string.car_type);
					value.setText(mPreference.getCarTypeResId());
					divider.setVisibility(View.GONE);
					mCarType = value;
					break;
				default:
					break;
				}
			} else {
				if (cameraNumber == 2) {
					switch (position) {
					case CARLN_AD:
						title.setText(R.string.gsensor_lock_level);
						value.setText(mPreference.getLockSensityResId());
						mLockSensity = value;
						break;
					case CRH_SEN:
//							title.setText(R.string.camera_flip);
						title.setText(R.string.text_flip);
						btSwitch.setVisibility(View.VISIBLE);
						mRearFlip = btSwitch;
						if (mPreference.getRearVisionFlip()) {
							btSwitch.setImageResource(R.drawable.settings_item_button_on);
						} else {
							btSwitch.setImageResource(R.drawable.settings_item_button_off);
						}
						value.setVisibility(View.GONE);
						arrow.setVisibility(View.GONE);
						break;
					case 2:
						title.setText(R.string.camera_right_flip);
						btSwitch.setVisibility(View.VISIBLE);
						mRightFlip = btSwitch;
						if (!mPreference.getRightVisionFlip()) {
							btSwitch.setImageResource(R.drawable.settings_item_button_on);
						} else {
							btSwitch.setImageResource(R.drawable.settings_item_button_off);
						}
						value.setVisibility(View.GONE);
						arrow.setVisibility(View.GONE);
						break;
					case 3:
						title.setText(R.string.sd1_format);
						break;
					}
				} else {
					title.setText(R.string.gsensor_lock_level);
					value.setText(mPreference.getLockSensityResId());
					divider.setVisibility(View.GONE);
					mLockSensity = value;
				}
			}
			return itemView;
		}

	}

	private class RightListListener implements OnItemClickListener {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			// TODO Auto-generated method stub
			CDRAlertDialog dialog;
			int res[];
			int cur;
			long curTime = System.currentTimeMillis();
			if (mIsPopuping && Math.abs(mLaseClick - curTime) < 500) {
				return;
			} else if (mIsPopuping) {
				mIsPopuping = false;
			}
			mLaseClick = curTime;
			Log.v(TAG, "right.onItemClick-------------------:" + position);
			Log.v(TAG, "MyPreference.isAdasOpen-----------------:" + MyPreference.isAdasOpen);
			if (MyPreference.isAdasOpen) {
				switch (position) {
				case CARLN_AD:
					Log.v(TAG, "CARLN_AD---------------------:");
					if (cameraNumber == 1) {
						boolean value = mPreference.getCarLaneAdjust();
						value = !value;
						mPreference.saveCarLaneAdjust(value);
						Log.v(TAG, "value---------------------:" + value);
						if (value) {
							mCarlaneAdjust.setImageResource(R.drawable.settings_item_button_on);
						} else {
							mCarlaneAdjust.setImageResource(R.drawable.settings_item_button_off);
						}
					} else if (cameraNumber == 2) {
						dialog = CDRAlertDialog.getInstance(mActivity);
						if (null == dialog) {
							return;
						}
						mIsPopuping = true;
						res = mPreference.getResources(MyPreference.RES_LCK_LEV);
						cur = mPreference.getLockSensity();
						dialog.setTitle(R.string.gsensor_lock_level);
						Log.v(TAG, "res.length:" + res.length);
						for (int i = 0; i < res.length; i++) {
							dialog.addItem(res[i], cur == i, false);
						}

						dialog.setCallback(new ICDRAlertDialogListener() {
							@Override
							public void onClick(int state) {
								mPreference.saveLockSensity(state);
								mLockSensity.setText(mPreference.getLockSensityResId());
								mIsPopuping = false;
							}

							@Override
							public void onTimeClick(int hour, int minute) {
							}

							@Override
							public void onDateClick(int year, int month, int day) {
							}
						});
					}
					break;
				case CRH_SEN:
					if (cameraNumber == 1) {
						dialog = CDRAlertDialog.getInstance(mActivity);
						if (null == dialog) {
							return;
						}
						mIsPopuping = true;
						res = mPreference.getResources(MyPreference.RES_DIS_LEV);
						cur = mPreference.getCrashSensity();
						dialog.setTitle(R.string.distance_detect_level);
						for (int i = 0; i < res.length; i++) {
							dialog.addItem(res[i], cur == i, false);
						}

						dialog.setCallback(new ICDRAlertDialogListener() {
							@Override
							public void onClick(int state) {
								Log.v(TAG, "CRH_SEN--------------:setCallback---------state=  " + state);
								mPreference.saveCrashSensity(state);
								mCrashDetect.setText(mPreference.getCrashSensityResId());
								mIsPopuping = false;
							}

							@Override
							public void onTimeClick(int hour, int minute) {
							}

							@Override
							public void onDateClick(int year, int month, int day) {
							}
						});
					} else if (cameraNumber == 2) {
						/*
						 * boolean value = mPreference.getRearVisionFlip(); value = !value;
						 * mPreference.saveRearVisionFlip(value); if (value) {
						 * mRearFlip.setImageResource(R.drawable.settings_item_button_on); } else {
						 * mRearFlip.setImageResource(R.drawable.settings_item_button_off); }
						 */
						setFlip();
					}
					break;
				case LCK_SEN:
					if (cameraNumber == 1) {
						dialog = CDRAlertDialog.getInstance(mActivity);
						if (null == dialog) {
							return;
						}
						mIsPopuping = true;
						res = mPreference.getResources(MyPreference.RES_LCK_LEV);
						cur = mPreference.getLockSensity();
						dialog.setTitle(R.string.gsensor_lock_level);
						for (int i = 0; i < res.length; i++) {
							dialog.addItem(res[i], cur == i, false);
						}

						dialog.setCallback(new ICDRAlertDialogListener() {
							@Override
							public void onClick(int state) {
								mPreference.saveLockSensity(state);
								mLockSensity.setText(mPreference.getLockSensityResId());
								mIsPopuping = false;
							}

							@Override
							public void onTimeClick(int hour, int minute) {
							}

							@Override
							public void onDateClick(int year, int month, int day) {
							}
						});
					} else if (cameraNumber == 2) {
						// onSD1Format();
						boolean valueRight = mPreference.getRightVisionFlip();
						Log.d(TAG, "value = mPreference.getRightVisionFlip(): " + valueRight);
						valueRight = !valueRight;
						mPreference.saveRightVisionFlip(valueRight);
						if (!valueRight) {
							mRightFlip.setImageResource(R.drawable.settings_item_button_on);
						} else {
							mRightFlip.setImageResource(R.drawable.settings_item_button_off);
						}
					}

					break;
				/*
				 * case PRK_SEN: dialog = CDRAlertDialog.getInstance(mActivity); if (null ==
				 * dialog) { return; } res = mPreference.getResources(MyPreference.RES_PRK_LEV);
				 * cur = mPreference.getParkingCrashSensity();
				 * dialog.setTitle(R.string.gsensor_alarm_level); for (int i = 0; i <
				 * res.length; i++) { dialog.addItem(res[i], cur == i, false); }
				 * 
				 * dialog.setCallback(new ICDRAlertDialogListener() {
				 * 
				 * @Override public void onClick(int state) {
				 * mPreference.saveParkingCrashSensity(state);
				 * mParkSensity.setText(mPreference.getParkingCrashSensityResId()); }
				 * 
				 * @Override public void onTimeClick(int hour, int minute) { }
				 * 
				 * @Override public void onDateClick(int year, int month, int day) { } });
				 * break;
				 */
				case SD_TYPE:
					onSD1Format();
					break;
				case CAR_TYPE:
					dialog = CDRAlertDialog.getInstance(mActivity);
					if (null == dialog) {
						return;
					}
					mIsPopuping = true;
					res = mPreference.getResources(MyPreference.RES_CAR_TYPE);
					cur = mPreference.getCarType();
					dialog.setTitle(R.string.select_car_type);
					for (int i = 0; i < res.length; i++) {
						dialog.addItem(res[i], cur == i, false);
					}

					dialog.setCallback(new ICDRAlertDialogListener() {
						@Override
						public void onClick(int state) {
							mPreference.saveCarType(state);
							mCarType.setText(mPreference.getCarTypeResId());
							mIsPopuping = false;
						}

						@Override
						public void onTimeClick(int hour, int minute) {
						}

						@Override
						public void onDateClick(int year, int month, int day) {
						}
					});
					break;
				default:
					break;
				}
			} else {
				if (cameraNumber == 1) {
					if (position == 0) {
						dialog = CDRAlertDialog.getInstance(mActivity);
						if (null == dialog) {
							return;
						}
						mIsPopuping = true;
						res = mPreference.getResources(MyPreference.RES_LCK_LEV);
						cur = mPreference.getLockSensity();
						dialog.setTitle(R.string.gsensor_lock_level);
						for (int i = 0; i < res.length; i++) {
							dialog.addItem(res[i], cur == i, false);
						}

						dialog.setCallback(new ICDRAlertDialogListener() {
							@Override
							public void onClick(int state) {
								mPreference.saveLockSensity(state);
								mLockSensity.setText(mPreference.getLockSensityResId());
								mIsPopuping = false;
							}

							@Override
							public void onTimeClick(int hour, int minute) {
							}

							@Override
							public void onDateClick(int year, int month, int day) {
							}
						});
					}
				} else {
					switch (position) {
					case CARLN_AD:
						dialog = CDRAlertDialog.getInstance(mActivity);
						if (null == dialog) {
							return;
						}
						mIsPopuping = true;
						res = mPreference.getResources(MyPreference.RES_LCK_LEV);
						cur = mPreference.getLockSensity();
						dialog.setTitle(R.string.gsensor_lock_level);
						Log.v(TAG, "res.length:" + res.length);
						for (int i = 0; i < res.length; i++) {
							dialog.addItem(res[i], cur == i, false);
						}

						dialog.setCallback(new ICDRAlertDialogListener() {
							@Override
							public void onClick(int state) {
								mPreference.saveLockSensity(state);
								mLockSensity.setText(mPreference.getLockSensityResId());
								mIsPopuping = false;
							}

							@Override
							public void onTimeClick(int hour, int minute) {
							}

							@Override
							public void onDateClick(int year, int month, int day) {
							}
						});
						break;
					case CRH_SEN:
						boolean value = mPreference.getRearVisionFlip();
						value = !value;
						Log.i(TAG, "value = " + value);
						mPreference.saveRearVisionFlip(value);
						if (value) {
							mRearFlip.setImageResource(R.drawable.settings_item_button_on);
						} else {
							mRearFlip.setImageResource(R.drawable.settings_item_button_off);
						}
						break;
					case 2:
						boolean valueRight = mPreference.getRightVisionFlip();
						Log.d(TAG, "value = mPreference.getRightVisionFlip(): " + valueRight);
						valueRight = !valueRight;
						mPreference.saveRightVisionFlip(valueRight);
						if (!valueRight) {
							mRightFlip.setImageResource(R.drawable.settings_item_button_on);
						} else {
							mRightFlip.setImageResource(R.drawable.settings_item_button_off);
						}
						break;
					case 3:
						onSD1Format();
						break;
					}
				}
			}
			mRightAdapter.notifyDataSetChanged();
		}

	}

	private void setFlip() {
		CDRAlertDialog dialog = CDRAlertDialog.getInstance(mActivity);
		if (null == dialog) {
			return;
		}
		mIsPopuping = true;
		dialog.setTouchStillShow();
		dialog.setTitle(R.string.text_flip);
//    	dialog.addItem(flipChoice[0], mPreference.getCameraFlipStatus(MyPreference.KEY_LEFT_CAMERA_FLIP), false);
//    	dialog.addItem(flipChoice[0], mPreference.getCameraFlipStatus(MyPreference.KEY_RIGHT_CAMERA_FLIP), false);
		dialog.addItem(flipChoice[1], mPreference.getCameraFlipStatus(MyPreference.KEY_BACK_CAMERA_FLIP), false);
		dialog.setCallback(new ICDRAlertDialogListener() {
//    		boolean leftFlip = mPreference.getCameraFlipStatus(MyPreference.KEY_LEFT_CAMERA_FLIP);
//    		boolean rightFlip = mPreference.getCameraFlipStatus(MyPreference.KEY_RIGHT_CAMERA_FLIP);
			boolean backFlip = mPreference.getCameraFlipStatus(MyPreference.KEY_BACK_CAMERA_FLIP);

			@Override
			public void onClick(int state) {
//	            if (state == 0) {
//	            	leftFlip = !leftFlip;
//				}else 
//					if(state == 0){
//					rightFlip = !rightFlip;
//				}else 
				if (state == 0) {
					backFlip = !backFlip;
				} else if (state == 100) {
//					mPreference.saveCameraFlipStatus(MyPreference.KEY_LEFT_CAMERA_FLIP, leftFlip);
//					mPreference.saveCameraFlipStatus(MyPreference.KEY_RIGHT_CAMERA_FLIP, rightFlip);
					mPreference.saveCameraFlipStatus(MyPreference.KEY_BACK_CAMERA_FLIP, backFlip);
					mIsPopuping = false;
				}
			}

			@Override
			public void onTimeClick(int hour, int minute) {
			}

			@Override
			public void onDateClick(int year, int month, int day) {
			}
		});
		dialog.setButtons();
	}

	private void onSD1Format() {
		// CDRAlertDialog dialog = new CDRAlertDialog(mContext);
		CDRAlertDialog dialog = CDRAlertDialog.getInstance(mActivity);
		if (null == dialog) {
			return;
		}
		if (Storage.getTotalSpace() > 0) {
			dialog.setTitle(R.string.sd1_format);
			dialog.setMessage(R.string.sd1_format_or_not);

			dialog.setCallback(new ICDRAlertDialogListener() {

				@Override
				public void onClick(int state) {
					Storage.formatSDcard(mActivity);
				}

				@Override
				public void onTimeClick(int hour, int minute) {
				}

				@Override
				public void onDateClick(int year, int month, int day) {
				}
			});
			dialog.setButtons();
		} else {
			Toast.makeText(getActivity(), R.string.sdcard_not_found, Toast.LENGTH_LONG).show();
		}
	}
}
