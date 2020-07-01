package com.android.camera.v66;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.android.camera2.R;

public class SettingsTitle extends Fragment {
    
    public static final String TAG = "LeftScreenPreference";
    private Button mReturnButton = null;
    
    @Override
    public View onCreateView(LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.settings_title, container, false);
        mReturnButton = (Button) view.findViewById(R.id.return_icon);
        mReturnButton.setOnClickListener(new View.OnClickListener() {
            
            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                Activity act = SettingsTitle.this.getActivity();
                if (act instanceof RecorderActivity) {
                    RecorderActivity ract = ((RecorderActivity) act);
                    ract.loadViewByState(RecorderActivity.STATE_FRONT_PREVIEW);
                }
            }
        });
        return view;
    }
    
}
