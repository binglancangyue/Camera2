package com.android.camera.v66;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.android.camera.data.CameraDataAdapter;
import com.android.camera.data.LocalDataAdapter;
import com.android.camera.ui.FilmStripView;
import com.android.camera.ui.FilmStripView.DataAdapter;
import com.android.camera.ui.FilmStripView.DataAdapter.Listener;
import com.android.camera.ui.FilmStripView.DataAdapter.UpdateReporter;
import com.android.camera.ui.FilmStripView.ImageData;
import com.android.camera.v66.AdjustedListView.IListViewUpListener;
import com.android.camera.v66.RecorderActivity.IServiceBindedListener;
import com.android.camera2.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ReviewFragment extends Fragment
        implements
        IServiceBindedListener,
        Listener,
        OnClickListener,
        IListViewUpListener {
    
    public static final String TAG = "ReviewFragment";
    private LinearLayout mRoot;
    private Button mBackButton;
    private Button mSelector;
    private Button mDeletor;
    private AdjustedListView mList;
    private RecordService mRecordService;
    private CameraDataAdapter mDataAdapter;
    private boolean mAllSelected = false;
    private int mColums = 0;
    private int mRow = 0;
    
    private View.OnClickListener mItemClickListener = new View.OnClickListener() {
        
        @Override
        public void onClick(View arg0) {
            // TODO Auto-generated method stub
            boolean isSelected = !arg0.isSelected();
            ImageView cur = (ImageView) arg0.findViewById(R.id.selector);
            Log.d(TAG, "mItemClickListener cur=" + cur);
            if (cur != null) {
                if (isSelected) {
                    cur.setVisibility(View.VISIBLE);
                } else {
                    cur.setVisibility(View.GONE);
                }
                arg0.setSelected(isSelected);
            }
        }
    };
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // setHasOptionsMenu(true);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, 
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.review_main, container, false);
        mRoot = (LinearLayout) view.findViewById(R.id.root);
        mBackButton = (Button) view.findViewById(R.id.back_button);
        mBackButton.setOnClickListener(this);
        mSelector = (Button) view.findViewById(R.id.seletor);
        mSelector.setOnClickListener(this);
        mDeletor = (Button) view.findViewById(R.id.bt_delete);
        mDeletor.setOnClickListener(this);
        mList = (AdjustedListView) view.findViewById(R.id.review_list);
        mList.setListener(this);
        
        if (getActivity() instanceof RecorderActivity) {
            Log.d(TAG, "onCreateView");
            RecorderActivity activity = ((RecorderActivity) getActivity());
            activity.addServiceBindedListener(this);
            mRecordService = activity.getRecordService();
            if (mRecordService != null) {
                mDataAdapter = mRecordService.getLocalDataAdapter();
                if (mDataAdapter != null) {
                    mDataAdapter.setListener(this);
                    if (mDataAdapter.getTotalNumber() > 0) {
                        int width = (int)getActivity().getResources().getDimension(
                                R.dimen.review_item_width);
                        int height = (int)getActivity().getResources().getDimension(
                                R.dimen.review_item_height);
                        mDataAdapter.suggestViewSizeBound(width, height);
                        mList.setAdapter(new ListAdapter(mColums));
                    }
                }
            }
        }
        return view;
    }
    
    @Override
    public void onDestroyView() {
        // TODO Auto-generated method stub
        super.onDestroyView();
        Log.d(TAG, "onDestroyView");
    }
    
    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        /*
         * if(getActivity() != null){
         * getActivity().unregisterReceiver(mIntentReceiver); }
         */
        Log.d(TAG, "onDestroy");
    }
    
    @Override
    public void onServiceBinded(RecordService service) {
        // TODO Auto-generated method stub
        if (mRecordService != null && mRecordService == service) {
            return;
        }
        Log.d(TAG, "onServiceBinded");
        if (getActivity() instanceof RecorderActivity) {
            ((RecorderActivity) getActivity()).addServiceBindedListener(this);
            mRecordService = ((RecorderActivity) getActivity()).getRecordService();
            if (mRecordService != null) {
                mDataAdapter = mRecordService.getLocalDataAdapter();
                if (mDataAdapter != null) {
                    mDataAdapter.setListener(this);
                    if (mDataAdapter.getTotalNumber() > 0) {
                        int width = (int) getActivity().getResources().getDimension(
                                R.dimen.review_item_width);
                        int height = (int) getActivity().getResources().getDimension(
                                R.dimen.review_item_height);
                        mDataAdapter.suggestViewSizeBound(width, height);
                    }
                }
            }
        }
    }
    
    @Override
    public void onDataLoaded() {
        // TODO Auto-generated method stub
        Log.d(TAG, "onDataLoaded =" + mDataAdapter.getTotalNumber());
        if (mDataAdapter.getTotalNumber() > 0) {
            int width = (int) getActivity().getResources().getDimension(R.dimen.review_item_width);
            int hght = (int) getActivity().getResources().getDimension(R.dimen.review_item_height);
            mDataAdapter.suggestViewSizeBound(width, hght);
            mList.setAdapter(new ListAdapter(mColums));
        }
    }
    
    @Override
    public void onDataUpdated(UpdateReporter reporter) {
        // TODO Auto-generated method stub
        Log.d(TAG, "onDataUpdated");
    }
    
    @Override
    public void onDataInserted(int dataID, ImageData data) {
        // TODO Auto-generated method stub
        Log.d(TAG, "onDataInserted");
    }
    
    @Override
    public void onDataRemoved(int dataID, ImageData data) {
        // TODO Auto-generated method stub
        Log.d(TAG, "onDataRemoved");
    }
    
    private void update(DataAdapter.UpdateReporter reporter) {
        int dataId = 0;
        /*if (reporter.isDataRemoved(dataId)) {
        }
        if (reporter.isDataUpdated(dataId)) {
        }*/
    }
    
    private class ItemSet {
        public int dataId;
        public int viewId;
        public View view;
        
        public ItemSet(int data, int vi, View view) {
            this.dataId = data;
            this.viewId = vi;
            this.view = view;
        }
    }
    
    private class ListAdapter extends BaseAdapter {
        private int mListColums = 0;
        private int mListRow = 0;
        private int mTotalCount = 0;
        private List<ItemSet> mViewList = new ArrayList<ItemSet>();
        
        public ListAdapter(int col) {
            mListColums = col;
        }
        
        public List<ItemSet> getItemList() {
            return mViewList;
        }
        
        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            Log.d(TAG, "ListAdapter");
            if (mDataAdapter != null) {
                mTotalCount = mDataAdapter.getTotalNumber();
                if (mListColums > 0 && mTotalCount % mListColums == 0) {
                    mListRow = mTotalCount / mListColums;
                } else if (mListColums > 0) {
                    mListRow = mTotalCount / mListColums + 1;
                }
            }
            Log.d(TAG, "mTotalCount=" + mTotalCount + ";mListRow=" + mListRow);
            return mListRow;
        }
        
        @Override
        public Object getItem(int arg0) {
            // TODO Auto-generated method stub
            return mViewList.get(arg0);
        }
        
        @Override
        public long getItemId(int arg0) {
            // TODO Auto-generated method stub
            return 0;
        }
        
        @Override
        public View getView(int postion, View view, ViewGroup parent) {
            // TODO Auto-generated method stub
            LinearLayout ll = new LinearLayout(getActivity());
            ll.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.FILL_PARENT,
                    AbsListView.LayoutParams.FILL_PARENT));
            int itemCount = 0;
            if (postion >= 0 && postion < mListRow) {
                itemCount = mListColums;
            } else if (postion >= 0 && postion == mListRow) {
                itemCount = mTotalCount - postion * mListColums;
                if (itemCount < 0) {
                    itemCount = 0;
                }
            }
            Log.d(TAG, "getView postion=" + postion);
            for (int i = 0; i < itemCount; i++) {
                int dataId = postion * mListColums + i;
                int viewId = View.generateViewId();
                boolean isAdded = false;
                View item = LayoutInflater.from(getActivity()).inflate(R.layout.review_item, null);
                FrameLayout itemContain = (FrameLayout) item.findViewById(R.id.item);
                TextView title = (TextView) item.findViewById(R.id.title);
                ImageView selector = (ImageView) item.findViewById(R.id.selector);
                item.setSelected(false);
                item.setOnClickListener(mItemClickListener);
                if (mDataAdapter != null && mDataAdapter.getLocalData(dataId) != null) {
                    mDataAdapter.getLocalData(dataId).prepare();
                    itemContain.addView(mDataAdapter.getView(getActivity(), dataId));
                    title.setText(mDataAdapter.getLocalData(dataId).getTitle());
                    ll.addView(item);
                }
                for (int j = 0; j < mViewList.size(); j++) {
                    if (mViewList.get(j).dataId == dataId) {
                        isAdded = true;
                        break;
                    }
                }
                if (!isAdded) {
                    mViewList.add(new ItemSet(dataId, viewId, item));
                }
            }
            return ll;
        }
        
    }
    
    @Override
    public void onClick(View view) {
        // TODO Auto-generated method stub
        switch (view.getId()) {
            case R.id.back_button:
                FragmentManager fm = getFragmentManager();
                int num = fm.getBackStackEntryCount();
                for (int i = 0; i < num; i++) {
                    try {
                        fm.popBackStack();
                    } catch(IllegalStateException ex) {
                        ex.printStackTrace();
                    }
                }
                Activity act = this.getActivity();
                if (act instanceof RecorderActivity) {
                    ((RecorderActivity)act).resumeState();
                }
                break;
            case R.id.seletor:
                mAllSelected = !mAllSelected;
                if (mAllSelected) {
                    mSelector.setText(R.string.review_unselect_all);
                } else {
                    mSelector.setText(R.string.review_select_all);
                }
                if (mList.getAdapter() instanceof ListAdapter) {
                    List<ItemSet> viewList = ((ListAdapter) mList.getAdapter()).getItemList();
                    for (int i = 0; i < viewList.size(); i++) {
                        viewList.get(i).view.setSelected(mAllSelected);
                        ImageView cur = (ImageView) viewList.get(i).view
                                .findViewById(R.id.selector);
                        if (mAllSelected) {
                            cur.setVisibility(View.VISIBLE);
                        } else {
                            cur.setVisibility(View.GONE);
                        }
                    }
                }
                break;
            case R.id.bt_delete:
                CDRAlertDialog dialog = CDRAlertDialog.getInstance(getActivity());
                if (null == dialog) {
                    return;
                }
                dialog.setTitle(R.string.warning);
                dialog.setMessage(R.string.review_delect_file);
                dialog.setCallback(new ICDRAlertDialogListener() {
                    
                    @Override
                    public void onClick(int state) {
                        if (mAllSelected) {
                            for (int i = 0; i < mDataAdapter.getTotalNumber(); i++) {
                                mDataAdapter.removeData(getActivity(), i);
                            }
                            mDataAdapter.executeDeletion(getActivity());
                        } else {
                            if (mList.getAdapter() instanceof ListAdapter) {
                                List<ItemSet> viewList = ((ListAdapter) mList.getAdapter())
                                        .getItemList();
                                for (int i = 0; i < viewList.size(); i++) {
                                    if (viewList.get(i).view.isSelected()) {
                                        mDataAdapter.removeData(getActivity(),
                                                viewList.get(i).dataId);
                                    }
                                }
                            }
                            mDataAdapter.executeDeletion(getActivity());
                        }
                        mList.setAdapter(new ListAdapter(mColums));
                    }
                    
                    @Override
                    public void onTimeClick(int hour, int minute) {
                    }
                    
                    @Override
                    public void onDateClick(int year, int month, int day) {
                    }
                });
                dialog.setButtons();
                break;
            default:
                break;
        }
    }
    
    @Override
    public void onListViewUp(int ww, int hh) {
        // TODO Auto-generated method stub
        int itemWidth = (int) getActivity().getResources().getDimension(R.dimen.review_item_width);
        int itemHeight = (int) getActivity().getResources()
                .getDimension(R.dimen.review_item_height);
        if (itemWidth > 0 && itemHeight > 0) {
            int col = (ww - 50) / itemWidth;// 50 is margin
            int row = hh / itemHeight + 1;
            Log.d(TAG, "onListViewUp mColums =" + col);
            if (col != mColums || row != mRow) {
                mColums = col;
                mRow = row;
                mList.setAdapter(new ListAdapter(mColums));
            }
        }
    }
    
}
