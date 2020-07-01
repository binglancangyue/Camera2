package com.android.camera.v66;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;

import com.android.camera2.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

class CDRAlertDialog {
    public static CDRAlertDialog mDialogInstance = null;
    public static int mNo = 0;
    private static final String TAG = "CDRAlertDialog";
    private boolean stillShow = false;
    static Context mContext;
    AlertDialog mAd;
    Dialog mDialog;
    ICDRAlertDialogListener mListener;
    
    TextView mTitle;
    View mTitleParting;
    
    TextView mMsg;
    View mMsgParting;
    
    LinearLayout mItems;
    List<LinearLayout> mItemList = new ArrayList<LinearLayout>();
    
    LinearLayout mButtons;
    TextView mPos;
    TextView mNeg;
    
    LinearLayout mTime;
    TimePicker mTimePicker;
    TextView mTimeOK;
    
    LinearLayout mDate;
    DatePicker mDatePicker;
    TextView mDateOK;
    
    TextView mLast;
    
    private IOnCancelClickListener mOnCancelClickListener;
    
    public interface IOnCancelClickListener {
        public void onCancelClicked();
    }
    
    private CDRAlertDialog(Context cc) {
        mContext = cc;
        
        mNo++;
        
        mAd = new AlertDialog.Builder(cc).create();
        mAd.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                mDialogInstance = null;
            }
        });
        
        mAd.show();
        
        Window window = mAd.getWindow();
        window.setContentView(R.layout.alert_dialog);
        WindowManager.LayoutParams params = window.getAttributes();
        //params.width = 472;
        params.width = 370;
        params.y = -40;
        window.setAttributes(params);
        // window.setGravity(Gravity.RIGHT);
        
        mTitle = (TextView) window.findViewById(R.id.alert_dialog_title);
        mTitleParting = (View) window.findViewById(R.id.alert_dialog_title_parting_line);
        
        mMsg = (TextView) window.findViewById(R.id.alert_dialog_message);
        mMsgParting = (View) window.findViewById(R.id.alert_dialog_message_parting_line);
        
        mItems = (LinearLayout) window.findViewById(R.id.alert_dialog_items);
        
        mButtons = (LinearLayout) window.findViewById(R.id.alert_dialog_buttons);
        mPos = (TextView) window.findViewById(R.id.alert_dialog_pos);
        mNeg = (TextView) window.findViewById(R.id.alert_dialog_neg);
        
        mTime = (LinearLayout) window.findViewById(R.id.alert_dialog_time);
        mTimePicker = (TimePicker) window.findViewById(R.id.alert_dialog_timepicker);
        mTimeOK = (TextView) window.findViewById(R.id.alert_dialog_time_ok);
        
        mDate = (LinearLayout) window.findViewById(R.id.alert_dialog_date);
        mDatePicker = (DatePicker) window.findViewById(R.id.alert_dialog_datepicker);
        mDateOK = (TextView) window.findViewById(R.id.alert_dialog_date_ok);
        
        mLast = (TextView) window.findViewById(R.id.alert_dialog_last);
        
    }
    
    public static CDRAlertDialog getInstance(Context cc) {
        if (mDialogInstance == null || mContext != cc) {
            mDialogInstance = new CDRAlertDialog(cc);
            return mDialogInstance;
        } else {
            Log.i("CDRAlertDialog", "########already in mAd");
            return mDialogInstance;
        }
    }
    
    public void setTitle(int titleid) {
        mTitle.setText(titleid);
        mTitle.setVisibility(View.VISIBLE);
        mTitleParting.setVisibility(View.VISIBLE);
    }
    
    public void setMessage(int msgid) {
        mMsg.setText(msgid);
        mMsg.setVisibility(View.VISIBLE);
        mMsgParting.setVisibility(View.VISIBLE);
    }
    
    public void setItems(int itemsid, OnClickListener listener) {
        
    }
    
    public void setCallback(ICDRAlertDialogListener ll) {
        mListener = ll;
    }
    
    public void setOnCancelClickListener(IOnCancelClickListener listener) {
        mOnCancelClickListener = listener;
    }
    
    public void setTouchStillShow(){
    	stillShow = true;
    }
    
    public void addItem(int itemid, final boolean isSelected, boolean isLast) {
        LinearLayout view = new LinearLayout(mContext);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 64); 
                // context.getResource().getDimensionPixelSize(R.dimen.alert_dialog_item_height));
        view.setLayoutParams(lp);
        view.setOrientation(LinearLayout.HORIZONTAL);
        if (!isLast) {
            view.setBackgroundResource(R.drawable.alert_dialog_option_mid);
        } else {
            view.setBackgroundResource(R.drawable.alert_dialog_option_down);
        }
        
        LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        leftParams.gravity = Gravity.CENTER;
        leftParams.setMargins(26, 0, 0, 0);
        TextView tv = new TextView(mContext);
        tv.setLayoutParams(leftParams);
        tv.setGravity(Gravity.LEFT);
        tv.setText(itemid);
        tv.setTextSize(28);
        tv.setTextColor(mContext.getResources().getColorStateList(R.color.more_setting_text));
        view.addView(tv);
        
        LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rightParams.gravity = Gravity.CENTER;
        rightParams.setMargins(0, 0, 26, 0);
        final ImageView iv = new ImageView(mContext);
        iv.setLayoutParams(rightParams);
        iv.setImageResource(R.drawable.item_choice);
        view.addView(iv);
        iv.setTag(isSelected);
        
        view.setOnTouchListener(new android.view.View.OnTouchListener() {
            @Override
            public boolean onTouch(View vv, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        int state = 0;
                        int ii = 0;
                        int cnt = 0;
                        for (LinearLayout view : mItemList) {
                            cnt = view.getChildCount();
                            /*for (int j = 0; j < cnt; j++) {
                                view.getChildAt(j).setSelected(false);
                            }*/
                            view.setSelected(false);
                            if (view == vv) {
                                state = ii;
                            }
                            ii++;
                        }
                        Log.i(TAG, "state " + state);
                        mListener.onClick(state);
                        boolean isSelected = !(Boolean)iv.getTag();
                        iv.setSelected(isSelected);
                        iv.setTag(isSelected);
                        if (!stillShow) {
                        	vv.setSelected(true);
                        	mAd.dismiss();
						}
                        break;
                    case MotionEvent.ACTION_UP:
                        Log.i(TAG, "UP dismiss");
                        if (mAd != null) {
                            // mAd.dismiss();
                            // mAd = null;
                            Log.i(TAG, "UP dismiss over");
                        }
                        
                        break;
                    default:
                        break;
                }
                return true;
            }
        });
        
        if (isSelected) {
            iv.setSelected(true);
        }

        
        mItemList.add(view);
        mItems.addView(view);
        if (!isLast) {
            View partingLine = new View(mContext);
            LinearLayout.LayoutParams vlp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1);
            vlp.setMargins(8, 0, 8, 0);
            partingLine.setLayoutParams(vlp);
            partingLine.setBackgroundResource(R.color.parting_line_dark);
            mItems.addView(partingLine);
        }
    }
    
    public void setPositiveButton(int textid, OnClickListener listener) {
        
    }
    
    public void setNegtiveButton(int textid, OnClickListener listener) {
        
    }
    
    public void setButtons() {
        mButtons.setVisibility(View.VISIBLE);
        mPos.setOnClickListener(new android.view.View.OnClickListener() {
            
            @Override
            public void onClick(View vv) {
            	stillShow = false;
                mListener.onClick(100);
                if (mAd != null) {
                    mAd.dismiss();
                    Log.i(TAG, "UP dismiss over");
                }
            }
        });
        mNeg.setOnClickListener(new android.view.View.OnClickListener() {
            
            @Override
            public void onClick(View vv) {
            	stillShow = false;
                if (mAd != null) {
                    if (mOnCancelClickListener != null) {
                        mOnCancelClickListener.onCancelClicked();
                    }
                    mAd.dismiss();
                    Log.i(TAG, "UP dismiss over");
                }
            }
        });
    }
    
    public void setTime() {
        mTime.setVisibility(View.VISIBLE);
        mTimePicker.setIs24HourView(true);
        mTimeOK.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(View vv) {
                int hour = mTimePicker.getCurrentHour();
                int minute = mTimePicker.getCurrentMinute();
                Calendar cc = Calendar.getInstance();
                
                cc.set(Calendar.HOUR_OF_DAY, hour);
                cc.set(Calendar.MINUTE, minute);
                cc.set(Calendar.SECOND, 0);
                cc.set(Calendar.MILLISECOND, 0);
                long when = cc.getTimeInMillis();
                
                if (when / 1000 < Integer.MAX_VALUE) {
                    ((AlarmManager)mContext.getSystemService(Context.ALARM_SERVICE)).setTime(when);
                }
                
                mListener.onTimeClick(hour, minute);
                
                if (mAd != null) {
                    mAd.dismiss();
                    Log.i(TAG, "UP dismiss over");
                }
            }
        });
    }
    
    public void setDate() {
        mDate.setVisibility(View.VISIBLE);
        mDateOK.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(View vv) {
                vv.setBackgroundResource(R.color.orange);
                
                int year = mDatePicker.getYear();
                int month = mDatePicker.getMonth();
                int day = mDatePicker.getDayOfMonth();
                Calendar cc = Calendar.getInstance();
                
                cc.set(Calendar.YEAR, year);
                cc.set(Calendar.MONTH, month);
                cc.set(Calendar.DAY_OF_MONTH, day);
                long when = cc.getTimeInMillis();
                
                if (when / 1000 < Integer.MAX_VALUE) {
                    ((AlarmManager)mContext.getSystemService(Context.ALARM_SERVICE)).setTime(when);
                }
                
                mListener.onDateClick(year, month, day);
                
                if (mAd != null) {
                    mAd.dismiss();
                    Log.i(TAG, "UP dismiss over");
                }
            }
        });
    }
    
    public void setLast(int lastid) {
        mLast.setText(lastid);
        mLast.setVisibility(View.VISIBLE);
    }
    
    public void dismiss() {
    	if (mAd != null) {
    		mAd.dismiss();
    	}
    }
}
