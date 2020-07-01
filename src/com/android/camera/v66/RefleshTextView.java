package com.android.camera.v66;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;

public class RefleshTextView extends TextView {
	private static final String TAG = "RefleshTextView";
	private String ANDROIDXML = "http://schemas.android.com/apk/res/android";
	private Context mContext;
	private int textId = 0;

	public RefleshTextView(Context context) {
		super(context);
		mContext = context;
		init(context, null);
		// TODO Auto-generated constructor stub
	}

	public RefleshTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		init(context, attrs);
		// TODO Auto-generated constructor stub
	}

	public RefleshTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context, attrs);
		// TODO Auto-generated constructor stub
	}

	private void doRegister() {
		Log.d(TAG, "doRegister()");
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_LOCALE_CHANGED);
		mContext.registerReceiver(receiver, filter);
	}

	private void doUnRegister() {
		Log.d(TAG, "doUnRegister()");
		if (receiver != null) {
			mContext.unregisterReceiver(receiver);
		}

	}

	private BroadcastReceiver receiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String mAction = intent.getAction();
			Log.i(TAG, "receiver is receiving :" + mAction);
			if (mAction.equals(Intent.ACTION_LOCALE_CHANGED)) {
				//textId = getStringId(mContext,getText().toString());
				Log.d(TAG, "textId:" + textId + "   getText().toString():" + getText().toString());
				if (textId!= 0){
					setText(textId);
				}
			}

		}
	};

	private void init(Context context, AttributeSet attributeSet) {
		if (!DoubleFloatWindow.getInitFirstWindow()) {
			if (attributeSet != null) {
				String textValue = attributeSet.getAttributeValue(ANDROIDXML, "text");
				Log.d(TAG, "textValue:" + textValue);
				if (!(textValue == null || textValue.length() < 2)) {
					textId = string2int(textValue.substring(1, textValue.length()));
				}
			}
			DoubleFloatWindow.setInitFirstWindow(true);
		} else{
			textId = getStringId(mContext,"disconnectrecording");
		}
		
	}

	@Override
	protected void onAttachedToWindow() {
		// TODO Auto-generated method stub
		super.onAttachedToWindow();
		doRegister();
	}

	@Override
	protected void onDetachedFromWindow() {
		// TODO Auto-generated method stub
		super.onDetachedFromWindow();
		doUnRegister();

	}

	private int string2int(String str) {
		return string2int(str, 0);
	}

	private int string2int(String str, int def) {
		try {
			return Integer.valueOf(str);
		} catch (Exception e) {
		}
		return def;
	}
	
	public int getStringId(Context paramContext, String paramString) {
		return paramContext.getResources().getIdentifier(paramString, "string",
				paramContext.getPackageName());
	}

}
