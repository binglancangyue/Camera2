package com.android.camera.v66;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.camera2.R;

public class ShutdownWindow {
	private static String TAG = "ShutdownWindow";

	private static final int TYPE_DISPLAY_OVERLAY = 2026;
	private final int MSG_UPDATE_TIME = 1;
	private final int MSG_SHOW_WINDOW = 2;
	private final int MSG_HIDE_WINDOW = 3;
	private final int SHUTDOWN_TIME = 10;

	private WindowManager mWm;
	private WindowManager.LayoutParams mParams;
	private View mView = null;
	private Context mContext;

	private int mCurTime = 10;
	private ImageView mShutdownBg;
	private TextView mShutdownTime;

	private boolean mSecurityMode;

	private IShutdownStatusListener mShutdownStatusListener;

	public ShutdownWindow(Context context) {
		this.mContext = context;
		initView();
	}

	private void initView() {
		mWm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
		LayoutInflater inflater = LayoutInflater.from(mContext);
		mView = inflater.inflate(R.layout.shutdown_layout, null);
		mShutdownBg = (ImageView) mView.findViewById(R.id.mShutdownBg);
		mShutdownTime = (TextView) mView.findViewById(R.id.mShutdownTime);
		mShutdownTime.setText(String.valueOf(mCurTime));

		mSecurityMode = SystemProperties.getBoolean("persist.sys.security_mode", true);

		mParams = new WindowManager.LayoutParams();

		mParams.type = TYPE_DISPLAY_OVERLAY;

		mParams.format = PixelFormat.RGBA_8888;
		mParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
				| WindowManager.LayoutParams.FLAG_FULLSCREEN
				| WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
		mParams.x = 0;
		mParams.y = 0;
		mParams.width = WindowManager.LayoutParams.MATCH_PARENT;
		mParams.height = WindowManager.LayoutParams.MATCH_PARENT;
		mWm.addView(mView, mParams);

		onHide();
	}

	Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case MSG_UPDATE_TIME:
				Log.d(TAG, "MSG_UPDATE_TIME shutdownTime ==" + mCurTime);
				if (mCurTime < 0) {
					mShutdownTime.setText(R.string.shutting_down);
					handler.sendEmptyMessageDelayed(MSG_HIDE_WINDOW, 3000);
					if (mShutdownStatusListener != null) {
						mShutdownStatusListener.onShutdown();
					}
				} else {
					mShutdownTime.setText(String.valueOf(mCurTime));
					mCurTime--;
					handler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, 1000);
				}
				break;
			case MSG_SHOW_WINDOW:
				if (mView.getVisibility() == View.GONE) {
					mView.setVisibility(View.VISIBLE);
				}
				if (mSecurityMode) {
					mShutdownBg.setBackgroundResource(R.drawable.security_bg);
				} else {
					mShutdownBg.setBackgroundResource(R.drawable.shutdown_bg);
				}
				mCurTime = SHUTDOWN_TIME;
				Log.d(TAG, "onShow shutdownTime==" + mCurTime);
				handler.removeMessages(MSG_UPDATE_TIME);
				handler.sendEmptyMessage(MSG_UPDATE_TIME);
				break;
			case MSG_HIDE_WINDOW:
				if (mView.getVisibility() == View.VISIBLE) {
					mView.setVisibility(View.GONE);
				}
				handler.removeMessages(MSG_UPDATE_TIME);
				break;
			}
		}
	};

	public void onHide() {
		handler.sendEmptyMessage(MSG_HIDE_WINDOW);
	}

	public void onShow() {
		handler.sendEmptyMessage(MSG_SHOW_WINDOW);
	}

	public boolean isShow() {
		if (mView.getVisibility() == View.VISIBLE) {
			return true;
		}
		return false;
	}

	public interface IShutdownStatusListener {
		public void onShutdown();
	}

	public void setScreenStatusListener(IShutdownStatusListener shutdownStatusListener) {
		mShutdownStatusListener = shutdownStatusListener;
	}

}
