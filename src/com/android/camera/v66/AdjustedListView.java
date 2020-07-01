package com.android.camera.v66;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup.LayoutParams;
import android.widget.ListView;

public class AdjustedListView extends ListView {
    
    private static String TAG = "AdjustedListView";
    private int mParentWidth = 0;
    private int mParentHeight = 0;
    private IListViewUpListener mListener;
    
    public interface IListViewUpListener {
        public void onListViewUp(int ww, int hh);
    }
    
    public AdjustedListView(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
    }
    
    public AdjustedListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub
    }
    
    public void setListener(IListViewUpListener ls) {
        mListener = ls;
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (mParentWidth == 0) {
            mParentWidth = width;
            mParentHeight = height;
            if (mListener != null) {
                mListener.onListViewUp(mParentWidth, mParentHeight);
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
