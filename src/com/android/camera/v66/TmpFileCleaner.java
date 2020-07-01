package com.android.camera.v66;

import android.text.TextUtils;
import android.util.Log;

import com.android.camera.Storage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TmpFileCleaner {
    
    private static final String TAG = "CAM_TMP_FILE_CLEAN";
    private static final String CAM_VIDEO_DIRECTORY = Storage.DIRECTORY;// "/mnt/extsd/DCIM/Camera";
    
    private ArrayList<File> mFiles;
    
    public TmpFileCleaner() {
        mFiles = new ArrayList<File>();
    }
    
    public static TmpFileCleaner getInstance() {
        return new TmpFileCleaner();
    }
    
    public void searchTmpFiles() {
        Log.d(TAG, "======== start search tmp file====");
        if (mFiles.size() > 0) {
            mFiles.clear();
        }
        File dirFile = new File(CAM_VIDEO_DIRECTORY);
        
        if (dirFile.exists() && dirFile.isDirectory()) {
            String[] subFiles = dirFile.list();
            
            for (String path : subFiles) {
                if (!TextUtils.isEmpty(path) && path.endsWith(".tmp")) {
                    Log.d(TAG, " PATH = " + path);
                    mFiles.add(new File(CAM_VIDEO_DIRECTORY + "/" + path));
                }
            }
        }
        
        Log.d(TAG, String.format("======== search end, tmp file num is %d ====", mFiles.size()));
    }
    
    public void execClean() {
        if (mFiles.size() > 0) {
            for (File f : mFiles) {
                if (f.exists()) {
                    Log.d(TAG, String.format("======== delete tmp file %s  ====", f.getParent()));
                    f.delete();
                }
            }
        }
        
        mFiles.clear();
    }
    
    public List<File> getTmpFiles() {
        return mFiles;
    }
    
    public static void cleanLostFiles() {
        Log.d(TAG, "cleanLostFiles");
        File garbage = new File("/mnt/extsd/LOST.DIR");
        if (garbage != null) {
            File[] subs = garbage.listFiles();
            if (subs != null && subs.length > 0) {
                for (File f : subs) {
                    Log.d(TAG, "Deleted garbage file:" + f.getName());
                    f.delete();
                }
            }
        }
    }
}
