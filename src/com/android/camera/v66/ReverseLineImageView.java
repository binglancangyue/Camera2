package com.android.camera.v66;

import android.content.Context;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageView;
import com.android.camera2.R;

public class ReverseLineImageView extends ImageView {

	private static final String TAG = "ReverseLineImageView";
	private Context mContext;
	private MyPreference myPreference;
	private int mode = Mode.NONE;
	private int current_x;
	private int current_y;
	private int start_x;
	private int start_y;

	private int beforeLenght;
	private int afterLenght;
	private int imageStatus = Mode.NORMAL;

	public ReverseLineImageView(Context context) {
		super(context);
		init(context);
	}

	public ReverseLineImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public ReverseLineImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	private void init(Context context) {
		mContext = context;
		myPreference = MyPreference.getInstance(mContext);
		imageStatus = myPreference.getReverseMode();
		if (imageStatus == Mode.LOW) {
			setImageResource(R.drawable.reverse_lines_low);
		} else if (imageStatus == Mode.WIDTH_LOW) {
			setImageResource(R.drawable.reverse_lines_width_low);
		}else if (imageStatus == Mode.NORMAL) {
			setImageResource(R.drawable.reverse_lines);
		} else if (imageStatus == Mode.WIDTH) {
			setImageResource(R.drawable.reverse_lines_width);
		}
		
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		/** 处理单点、多点触摸 **/
		switch (event.getAction() & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN:
			onTouchDown(event);
			break;
		// 多点触摸
		case MotionEvent.ACTION_POINTER_DOWN:
			onPointerDown(event);
			break;

		case MotionEvent.ACTION_MOVE:
			onTouchMove(event);
			break;
		case MotionEvent.ACTION_UP:
			mode = Mode.NONE;
			break;

		// 多点松开
		case MotionEvent.ACTION_POINTER_UP:
			mode = Mode.NONE;
			break;
		}

		return true;
	}

	/** 按下 **/
	void onTouchDown(MotionEvent event) {
		mode = Mode.DRAG;
		start_x = current_x = (int) event.getRawX();
		start_y = current_y = (int) event.getRawY();
		Log.i(TAG, "--onTouchDown  current_x = " + current_x + " , current_y = " + current_y);
	}

	/** 两个手指 只能放大缩小 **/
	void onPointerDown(MotionEvent event) {
		if (event.getPointerCount() == 2) {
			mode = Mode.ZOOM;
			beforeLenght = getDistance(event);// 获取两点的距离
			Log.i(TAG, "--onPointerDown double point beforeLenght " + beforeLenght);
		}
	}

	/** 移动的处理 **/
	void onTouchMove(MotionEvent event) {
		int dragX = 0, dragY = 0;
		/** 处理拖动 **/
		if (mode == Mode.DRAG) {
			current_x = (int) event.getRawX();
			current_y = (int) event.getRawY();
			dragX = current_x - start_x;
			dragY = current_y - start_y;
			Log.i(TAG, "--onTouchMove dragX = " + dragX + " , dragY = " + dragY);
			if (dragY >= 100) {
				Log.i(TAG, "--onTouchMove ACTION_DOWN");
				updateImageStatus(Mode.ACTION_DOWN);
			} else if (dragY <= -100) {
				Log.i(TAG, "--onTouchMove ACTION_UP");
				updateImageStatus(Mode.ACTION_UP);
			}
		} else if (mode == Mode.ZOOM) {/** 处理缩放 **/
			afterLenght = getDistance(event);// 获取两点的距离
			float gapLenght = afterLenght - beforeLenght;// 变化的长度
			Log.i(TAG, "--onTouchMove zoom  gapLenght= " + gapLenght);
			if (gapLenght >= 200) {
				Log.i(TAG, "--onTouchMove ACTION_ZOOM");
				updateImageStatus(Mode.ACTION_ZOOM);
			} else if (gapLenght <= -200) {
				Log.i(TAG, "--onTouchMove ACTION_NARROW");
				updateImageStatus(Mode.ACTION_NARROW);
			}
		}

	}

	private void updateImageStatus(int status) {
		if (status == Mode.ACTION_DOWN) {
			if (imageStatus == Mode.NORMAL) {
				imageStatus = Mode.LOW;
				setImageResource(R.drawable.reverse_lines_low);
			} else if (imageStatus == Mode.WIDTH) {
				imageStatus = Mode.WIDTH_LOW;
				setImageResource(R.drawable.reverse_lines_width_low);
			}
		} else if (status == Mode.ACTION_UP) {
			if (imageStatus == Mode.LOW) {
				imageStatus = Mode.NORMAL;
				setImageResource(R.drawable.reverse_lines);
			} else if (imageStatus == Mode.WIDTH_LOW) {
				imageStatus = Mode.WIDTH;
				setImageResource(R.drawable.reverse_lines_width);
			}
		} else if (status == Mode.ACTION_NARROW) {
			if (imageStatus == Mode.WIDTH) {
				imageStatus = Mode.NORMAL;
				setImageResource(R.drawable.reverse_lines);
			} else if (imageStatus == Mode.WIDTH_LOW) {
				imageStatus = Mode.LOW;
				setImageResource(R.drawable.reverse_lines_low);
			}
		} else if (status == Mode.ACTION_ZOOM) {
			if (imageStatus == Mode.NORMAL) {
				imageStatus = Mode.WIDTH;
				setImageResource(R.drawable.reverse_lines_width);
			} else if (imageStatus == Mode.LOW) {
				imageStatus = Mode.WIDTH_LOW;
				setImageResource(R.drawable.reverse_lines_width_low);
			}
		}
		myPreference.saveReverseMode(imageStatus);
	}

	private int getDistance(MotionEvent event) {
		float x = event.getX(0) - event.getX(1);
		float y = event.getY(0) - event.getY(1);
		return (int) FloatMath.sqrt(x * x + y * y);
	}

	static class Mode {
		public static final int NONE = 0;
		public static final int MOVE = 1;
		public static final int DRAG = 2;
		public static final int ZOOM = 3;

		public static final int ACTION_UP = 5;
		public static final int ACTION_DOWN = 6;
		public static final int ACTION_NARROW = 7;
		public static final int ACTION_ZOOM = 8;

		public static final int NORMAL = 9;
		public static final int LOW = 10;
		public static final int WIDTH = 11;
		public static final int WIDTH_LOW = 12;
	}

}
