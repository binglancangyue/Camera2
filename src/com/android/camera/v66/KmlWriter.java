package com.android.camera.v66;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import com.android.camera.v66.CarSpeedMonitor.ISpeedChangeListener;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;

public class KmlWriter implements ISpeedChangeListener {
    public static final String TAG = "KmlWriter";
    public static final String KML_XML_TITLE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
    public static final String KML_PROTO_TITLE = "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n";
    public static final String KML_PROTO_END = "</kml>\n";
    public static final String KML_DOC_HEAD = "<Document>\n";
    public static final String KML_DOC_END = "</Document>\n";
    public static final String KML_PLACMARK_HEAD = "<Placemark>\n";
    public static final String KML_PLACMARK_END = "</Placemark>\n";
    public static final String KML_NAME_HEAD = "<name>";
    public static final String KML_NAME_END = "</name>\n";
    public static final String KML_STYLE_HEAD = "<styleUrl>";
    public static final String KML_STYLE_END = "</styleUrl>\n";
    public static final String KML_STYLE_NAME = "#roadStyle";
    public static final String KML_MUTIGEO_HEAD = "<MultiGeometry>\n";
    public static final String KML_MUTIGEO_END = "</MultiGeometry>\n";
    public static final String KML_LINESTRING_HEAD = "<LineString>\n";
    public static final String KML_LINESTRING_END = "</LineString>\n";
    public static final String KML_CRD_HEAD = "<coordinates>\n";
    public static final String KML_CRD_END = "</coordinates>\n";
    private LocationManager mLocationManager;
    private String mFileName;
    private String mEndFileName;
    private String mSwitchFileName;
    private double mLongitude = 0;
    private double mLatitude = 0;
    private double mAltitude = 0;
    private float mSpeed = 0;
    private boolean mIsNeedHeader = true;
    private LocationListener mLocationListener = new LocationListener() {
        
        @Override
        public void onLocationChanged(Location loc) {
            // TODO Auto-generated method stub
            if (loc != null) {
                mLongitude = loc.getLongitude();
                mLatitude = loc.getLatitude();
                mAltitude = loc.getAltitude();
            }
        }
        
        @Override
        public void onProviderDisabled(String arg0) {
            // TODO Auto-generated method stub
            
        }
        
        @Override
        public void onProviderEnabled(String arg0) {
            // TODO Auto-generated method stub
            
        }
        
        @Override
        public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
            // TODO Auto-generated method stub
            
        }
        
    };
    
    public KmlWriter(Context context) {
        Log.d(TAG, "KmlWriter enter");
        if (context != null) {
            mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            try {
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 0,
                        mLocationListener);
            } catch (IllegalArgumentException ex) {
                ex.printStackTrace();
            }
        } else {
            Log.d(TAG, "context = null !!!");
        }
        Log.d(TAG, "KmlWriter leave");
    }
    
    public void writeHeader() {
        Log.d(TAG, "writeHeader enter");
        if (mLocationManager == null || mFileName == null) {
            return;
        }
        try {
            FileWriter writer = new FileWriter(mFileName, true);
            writer.write(getHeader());
            Location loc = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (loc != null) {
                double lgt = loc.getLongitude();
                double ltd = loc.getLatitude();
                double atd = loc.getAltitude();
                writer.write(lgt + "," + ltd + "," + mSpeed + " ");
            }
            writer.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        Log.d(TAG, "writeHeader leave");
    }
    
    public void setCurFileName(String fileName) {
        if (fileName == null) {
            return;
        }
        mFileName = fileName + ".kml";
        mIsNeedHeader = true;
    }
    
    public void writeEnd() {
        if (mLocationManager == null || mFileName == null) {
            return;
        }
        Log.d(TAG, "writeEnd enter");
        try {
            FileWriter writer = new FileWriter(mFileName, true);
            writer.write(mLongitude + "," + mLatitude + "," + mSpeed + " ");
            writer.write(getEnd());
            writer.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        Log.d(TAG, "writeEnd leave");
    }
    
    public void writePoint() {
        // Log.d(TAG, "writePoint enter");
        if (mLocationManager == null || mFileName == null) {
            return;
        }
        try {
            FileWriter writer = new FileWriter(mFileName, true);
            if (mIsNeedHeader) {
                writer.write(getHeader());
                mIsNeedHeader = false;
            }
            writer.write(mLongitude + "," + mLatitude + "," + mSpeed + " ");
            writer.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // Log.d(TAG, "writePoint leave");
    }
    
    public void holdCurFileToEnd() {
        if (mLocationManager == null || mFileName == null) {
            return;
        }
        mEndFileName = new String(mFileName);
    }
    
    public void holdCurFileToSwitch() {
        if (mLocationManager == null || mFileName == null) {
            return;
        }
        mSwitchFileName = new String(mFileName);
    }
    
    public void writeEndFile() {
        if (mLocationManager == null || mEndFileName == null) {
            return;
        }
        Log.d(TAG, "writeEnd enter");
        File endFile = new File(mEndFileName);
        if (endFile == null || !endFile.exists()) {
            Log.d(TAG, "writeEndFile does not extist");
            return;
        }
        try {
            FileWriter writer = new FileWriter(mEndFileName, true);
            writer.write(mLongitude + "," + mLatitude + "," + mSpeed + " ");
            writer.write(getEnd());
            writer.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        Log.d(TAG, "writeEnd leave");
    }
    
    public void writeSwitchFile() {
        if (mLocationManager == null || mSwitchFileName == null) {
            return;
        }
        Log.d(TAG, "writeEnd enter");
        try {
            FileWriter writer = new FileWriter(mSwitchFileName, true);
            writer.write(mLongitude + "," + mLatitude + "," + mSpeed + " ");
            writer.write(getEnd());
            writer.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        Log.d(TAG, "writeEnd leave");
    }
    
    private String getHeader() {
        long time = System.currentTimeMillis();
        SimpleDateFormat recordFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String curTime = recordFormatter.format(time);
        String head = KML_XML_TITLE + KML_PROTO_TITLE + KML_DOC_HEAD + KML_PLACMARK_HEAD;
        head = head + KML_NAME_HEAD + curTime + KML_NAME_END + KML_STYLE_HEAD + KML_STYLE_NAME;
        head = head + KML_STYLE_END + KML_MUTIGEO_HEAD + KML_LINESTRING_HEAD + KML_CRD_HEAD;
        return head;
    }
    
    private String getEnd() {
        String end = KML_CRD_END + KML_LINESTRING_END + KML_MUTIGEO_END + KML_PLACMARK_END;
        end = end + KML_DOC_END + KML_PROTO_END;
        return end;
    }
    
    @Override
    public void onSpeedChange(float speed, int status, double longitude, double latitude) {
        // TODO Auto-generated method stub
        mSpeed = speed;
    }
    
    public void onDestroy() {
    	if (mLocationManager != null) {
        	mLocationManager.removeUpdates(mLocationListener);
        	mLocationManager = null;
    	}
    }
}
