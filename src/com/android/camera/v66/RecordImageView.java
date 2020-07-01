package com.android.camera.v66;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;

public class RecordImageView extends ImageView {
	private static final String TAG = "RecordImageView";
	private Context mContext;
	private AlphaAnimation alphaAnimation;

	public RecordImageView(Context context) {
		super(context);
		init(context);
	}

	public RecordImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public RecordImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	private void init(Context context) {
		mContext = context;
		alphaAnimation = new AlphaAnimation(1.0f, 0.0f);
		alphaAnimation.setDuration(1000);
		alphaAnimation.setFillAfter(true);
		alphaAnimation.setFillBefore(true);
		alphaAnimation.setRepeatMode(AlphaAnimation.REVERSE);
		alphaAnimation.setRepeatCount(AlphaAnimation.INFINITE);
	}

	private boolean isStartAnimation = false;

	private void startAlphaAni() {
		Log.i(TAG, "startAlphaAni()");
		if (!isStartAnimation) {
			startAnimation(alphaAnimation);
			isStartAnimation = true;
		}
	}

	private void stopAlphaAni() {
		Log.i(TAG, "stopAlphaAni()");
		if (isStartAnimation) {
			clearAnimation();
			isStartAnimation = false;
		}
	}
	
	@Override
	protected void onVisibilityChanged(View changedView, int visibility) {
		super.onVisibilityChanged(changedView, visibility);
		Log.i(TAG, "onVisibilityChanged() visibility = "+visibility);
		if (visibility == View.VISIBLE) {
			startAlphaAni();
		} else {
			stopAlphaAni();
		}
	}
	
}
