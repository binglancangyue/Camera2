package com.android.camera.v66;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.android.camera2.R;

public class LEDView extends LinearLayout {
	private static final String TAG = "LEDView";
	private static final boolean DEBUG = true;
	private TextView tv_am_pm;
	private TextView timeView;
	private TextView tv_date;
	private static final String FONT_DIGITAL_7 = "fonts" + File.separator + "digital-7.ttf";
	private Context mContext;
	private static final String DATE_FORMAT = "%02d:%02d";
	private static final int REFRESH_DELAY = 1000;
	private Calendar calendar = Calendar.getInstance();
	private static boolean is24HourFormat = true;
	private static boolean isAM = true;
	private int year = -1;
	private static String textAM = "";
	private static String textPM = "";
	SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd ");
	ContentResolver cv = null;
	private final Handler mHandler = new Handler();
	private final Runnable mTimeRefresher = new Runnable() {

		@Override
		public void run(){ 
			calendar.setTimeInMillis(System.currentTimeMillis());
			reflashData();
			if (is24HourFormat) {
				timeView.setText(
						String.format(DATE_FORMAT, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE)));
			} else {
				timeView.setText(
						String.format(DATE_FORMAT, calendar.get(Calendar.HOUR), calendar.get(Calendar.MINUTE)));
			}
//			mHandler.postDelayed(this, REFRESH_DELAY);
		}
	};

	@SuppressLint("NewApi")
	public LEDView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	public LEDView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public LEDView(Context context) {
		super(context);
		init(context);
	}



	public int getCurrentHour() {
		calendar.setTimeInMillis(System.currentTimeMillis());
		return calendar.get(Calendar.HOUR_OF_DAY);
	}

	private void init(Context context) {
		mContext = context;
		cv = mContext.getContentResolver();
		textAM = getResources().getString(R.string.am);
		textPM = getResources().getString(R.string.pm);
		LayoutInflater layoutInflater = LayoutInflater.from(context);
		View view = layoutInflater.inflate(R.layout.ledview, this);
		tv_am_pm = (TextView) view.findViewById(R.id.tv_am_pm);
		timeView = (TextView) view.findViewById(R.id.ledview_clock_time);
		tv_date = (TextView) view.findViewById(R.id.tv_date);
		AssetManager assets = context.getAssets();
		final Typeface font = Typeface.createFromAsset(assets, FONT_DIGITAL_7);
		timeView.setTypeface(font);
		mHandler.post(mTimeRefresher);
	}

	private void reflashData() {
		if (DEBUG) Log.i(TAG, "reflashData()");
		String strTimeFormat = android.provider.Settings.System.getString(cv,
				android.provider.Settings.System.TIME_12_24);
		if (DEBUG) Log.i(TAG, "strTimeFormat = " + strTimeFormat);
		if (strTimeFormat.equals("12")) {
			if (is24HourFormat) {
				tv_am_pm.setVisibility(View.VISIBLE);
				is24HourFormat = false;
			}
			int apm = calendar.get(Calendar.AM_PM);
			if (DEBUG) Log.i(TAG, "apm = " + apm);
			if (apm == Calendar.AM) {
				if (!isAM) {
					tv_am_pm.setText(textAM);
					isAM = true;
				}
			} else if (apm == Calendar.PM) {
				if (isAM) {
					tv_am_pm.setText(textPM);
					isAM = false;
				}
			}
		} else {
			if (!is24HourFormat) {
				tv_am_pm.setVisibility(View.GONE);
				is24HourFormat = true;
			}

		}
		if (calendar.get(Calendar.DAY_OF_YEAR) != year) {
			year = calendar.get(Calendar.DAY_OF_YEAR);
			if (DEBUG) Log.i(TAG, "year = " + year);
			initDate();
		}
	}

	private void initDate() {
		String strWeek = "";
		int weekDay = calendar.get(Calendar.DAY_OF_WEEK);
		if (DEBUG) Log.i(TAG, "weekDay = " + weekDay);
		if (weekDay == Calendar.MONDAY) {
			strWeek = getResources().getString(R.string.monday);
		} else if (weekDay == Calendar.TUESDAY) {
			strWeek = getResources().getString(R.string.tuesday);
		} else if (weekDay == Calendar.WEDNESDAY) {
			strWeek = getResources().getString(R.string.wednesday);
		} else if (weekDay == Calendar.THURSDAY) {
			strWeek = getResources().getString(R.string.thursday);
		} else if (weekDay == Calendar.FRIDAY) {
			strWeek = getResources().getString(R.string.friday);
		} else if (weekDay == Calendar.SATURDAY) {
			strWeek = getResources().getString(R.string.saturday);
		} else if (weekDay == Calendar.SUNDAY) {
			strWeek = getResources().getString(R.string.sunday);
		}
		tv_date.setText(format.format(calendar.getTime()) + strWeek);
		if (DEBUG) Log.i(TAG, "weekDay = " + weekDay);
	}

    
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		calendar = Calendar.getInstance();
		textAM = getResources().getString(R.string.am);
		textPM = getResources().getString(R.string.pm);
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_TIME_TICK);
		filter.addAction(Intent.ACTION_TIME_CHANGED);
		filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
		filter.addAction(Intent.ACTION_LOCALE_CHANGED);
		mContext.registerReceiver(mIntentReceiver, filter, null, null);
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		mContext.unregisterReceiver(mIntentReceiver);
	}
	
	private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if (Intent.ACTION_TIME_TICK.equals(action) || Intent.ACTION_TIME_CHANGED.equals(action)
					|| Intent.ACTION_TIMEZONE_CHANGED.equals(action) || Intent.ACTION_LOCALE_CHANGED.equals(action)) {
				if (!Intent.ACTION_TIME_TICK.equals(action)) {
					calendar = Calendar.getInstance();
				}
				if (DEBUG) Log.d(TAG,"The BroadcastReceiver is receiving action :" +action );
				mHandler.post(mTimeRefresher);
			}
		}
	};

}
