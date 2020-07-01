package com.android.camera.v66;

import java.util.Locale;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.Log;
import com.android.camera2.R;

public class MediaPlayerFactory {
	private final static String TAG = "MediaPlayerFactory";
	private final static String ABERRACY_MP = "AberrancyMP";
	private final static String CRASHMP = "CrashMP";

	public static MediaPlayer ChooseMediaPlayer(Context mContext, String str) {
		MediaPlayer mediaPlayer = null;

		String locale = Locale.getDefault().toString();
		
		Log.d(TAG, "The current language is " + locale);
		if (str.equals(ABERRACY_MP)) {

			if (locale.equals("zh_CN")||locale.equals("zh_TW")) {
				
				Log.d(TAG, "The AberrancyMP adas is chooseing " + "zh_CN");
				mediaPlayer = MediaPlayer.create(mContext, R.raw.adas_aberrancy_warning);
				return mediaPlayer;

			} else if (locale.equals("vi_VN")){
				mediaPlayer = MediaPlayer.create(mContext, R.raw.adas_aberrancy_warning_vi);
				return mediaPlayer;
			} else {
				
				Log.d(TAG, "The AberrancyMP adas is chooseing " + "en_GB");
				mediaPlayer = MediaPlayer.create(mContext, R.raw.adas_aberrancy_warning_en);
				return mediaPlayer;
			}

		} else if (str.equals(CRASHMP)) {

			if (locale.equals("zh_CN")||locale.equals("zh_TW")) {

				Log.d(TAG, "The CrashMP adas is chooseing " + "zh_CN");
				mediaPlayer = MediaPlayer.create(mContext, R.raw.adas_crash_warning);
				return mediaPlayer;

			} else if (locale.equals("vi_VN")){
				mediaPlayer = MediaPlayer.create(mContext, R.raw.adas_crash_warning_vi);
				return mediaPlayer;
			} else {

				Log.d(TAG, "The CrashMP adas is chooseing " + "en_GB");
				mediaPlayer = MediaPlayer.create(mContext, R.raw.adas_crash_warning_en);
				return mediaPlayer;
			}
		}
		
		Log.d(TAG, "The adas is chooseing " + "NOTHING");
		
		return mediaPlayer;
	}

}
