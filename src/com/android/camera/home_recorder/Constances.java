package com.android.camera.home_recorder;

import android.R.string;
import com.android.camera2.R;
import android.os.SystemProperties;

public class Constances {
	public static final int TYPE_LAYOUT_KAKA = 0;
	public static final int TYPE_LAYOUT_BLUESKY = 1;
	public static final int TYPE_LAYOUT_MAIZELONG = 2;
	public static final int TYPE_LAYOUT_TAIWAN = 3;
	public static final int TYPE_LAYOUT_WANFANGMAI = 4;
	public static final int TYPE_LAYOUT_WANFANGMAI_EN = 5;
	public static final int TYPE_LAYOUT_988 = 6;
	public static final int TYPE_LAYOUT_BAOHEIZI = 7;
	public static final int TYPE_LAYOUT_80HOU = 8;
	public static final int TYPE_LAYOUT_QC7_92 = 9;
	public static final int TYPE_LAYOUT_DINGWEITE = 10;
	public static final int TYPE_LAYOUT_QC7 = 20;
	public static final int TYPE_LAYOUT_QC7_BEISITE = 21;
	public static final int TYPE_LAYOUT_QC7_EN = 22;
	public static final int TYPE_LAYOUT_QC7_CYS = 23;
	public static final int TYPE_LAYOUT_QC7_QZ = 24;
	public static final int TYPE_LAYOUT_QC7_SPLITE = 25;
	public static final int TYPE_LAYOUT_QC7_SPLITE_HULIAN = 26;
	public static final int TYPE_LAYOUT_QC7_SPLITE_YISHENGYUAN = 27;
	public static final int TYPE_LAYOUT_QC7_QS = 28;
	public static final int TYPE_LAYOUT_9TRUCK4 = 29;
	public static final int TYPE_LAYOUT_QC7_SPLITE_WEIJIA = 30;
	public static final int TYPE_LAYOUT_FOUR_784 = 40;
	public static final int TYPE_LAYOUT_FOUR_988_COMMON = 41;
	public static final int TYPE_LAYOUT_FOUR_988_KAKA = 42;
	public static final int TYPE_LAYOUT_FOUR_935 = 43;
	public static final int TYPE_LAYOUT_THREE_9_NOSPLITE = 44;
	public static final int TYPE_LAYOUT_T9PLUS_BEI_SI_TE = 45;
	
	public static final int SPLITE_PAGE_COUNT = 3;
	public static final int FULL_PAGE_COUNT = 5;
	public static final String SYSTEM_REASON = "reason";
	public static final String SYSTEM_HOME_KEY = "homekey";
	public static final String SYSTEM_HOME_KEY_LONG = "recentapps";
	public static final String PASSWORD = "start666";


	public static final String KEY_DEFAULT_MAP_PACKAGE_NAME = "default_package";
	public static boolean isCustomMap = SystemProperties.getBoolean("ro.se.qchome.iscustommap", true);// 是否支持长按选择地图
	public static final String ACTION_NAME_ONLY_ONE_CAN_START = "com.zqc.action.only.one.between.gaode.and.kailide"; //凯立德
	public static int LAYOUT_TYPE = SystemProperties.getInt("ro.se.qchome.layouttype", 0);// 配置加载布局
	public static final boolean IS_ENGLISH_APPLIST = SystemProperties.getBoolean("persist.sys.isEn_applist",false);
	public static final boolean IS_NEED_PASSWD_ENTER_STORE = SystemProperties.getBoolean("persist.sys.isneedpasswd",false);

	/*--------------------broadcast action ----------------------------------*/
	public static final String ACTION_NAME_SPLITE = "android.intent.action.SPLIT_WINDOW_HAS_CHANGED";
	public static final String ACTION_NAME_NAVI_CLICK = "RESTOR_PREES_DOWN";
	public static final String ACTION_NAME_CLOSE_APPLIST = "com.zqc.action.close.applist";
	public static final String ACTION_NAME_OPEN_APPLIST = "com.zqc.action.launche.applist";
	public static final String ACTION_NAME_LAUNCH_BROWSER = "com.zqc.action.launch_browser";
	public static final String APPLIST_CLASSNAME = "com.zqc.launcher.AppListActivity";
	public static final String ACTION_NAME_IS_AUTOLITE = "com.zqc.action.isautolite";
	public static final String ACTION_LOCATION = "LRA308";

	/*--------------------package name----------------------------------*/
	public static final String PACKAGE_NAME_DEAULT_EDOG = "com.hdsc.edog";
	public static final String PACKAGE_NAME_ANANEDOG = "com.sxprd.radarspeed";
	public static final String PACKAGE_NAME_KUWO_MUSIC = "cn.kuwo.kwmusiccar";
	public static final String PACKAGE_NAME_TONGTING_MUSIC = "com.txznet.music";
	public static final String PACKAGE_NAME_YOUTOBE_MUSIC = "com.google.android.youtube";
	public static final String PACKAGE_NAME_AUTOLITE = "com.autonavi.amapautolite";
	public static final String PACKAGE_NAME_BLUETOOTH = "com.cywl.bt.activity";
	public static final String PACKAGE_NAME_TIANDIDA = "cn.jyuntech.map";
	public static final String PACKAGE_NAME_VIETMAP = "com.vietmap.s1OBU";
	public static final String PACKAGE_NAME_ANDROID_MUSIC = "com.android.music";
	public static final String PACKAGE_NAME_GOOGLEMAP = "com.google.android.apps.maps";
	public static final String PACKAGE_NAME_GOOGLESTORE = "com.android.vending";
	public static final String PACKAGE_NAME_GPS_TEST = "com.chartcross.gpstestplus";
	public static final String PACKAGE_NAME_DEFAULT_MOBILE_LINK = "com.ligo.appshare_qichen";
	public static final String PACKAGE_NAME_WEIXIN_ASSIT = "com.txznet.webchat";
	public static final String PACKAGE_NAME_PP_VIDEO = "com.zhiyang.connectphone";
	public static final String PACKAGE_NAME_DEFAULT_VIDEO = "com.zqc.videolisttest"; //视频
	public static final String PACKAGE_NAME_BROWSER = "com.android.browser";
	public static final String PACKAGE_NAME_KAILIDE = "cld.navi.STD.mainframe";
	public static final String PACKAGE_NAME_WIFI_HOT = "com.android.settings.Settings$TetherSettingsActivity";
	//万能钥匙
	public static final String PACKAGE_NAME_UNIVERSAL_KEY = "com.snda.wifilocating";
	//云服务
	public static final String PACKAGE_NAME_CLOUND_SERVICE = "com.yichengyuan.uploadgps";
	//网络电视
	public static final String PACKAGE_NAME_INTERNET = "org.fungo.carpad";
	public static final String PACKAGE_NAME_NAVI = SystemProperties.get("ro.se.map_package_name",
			PACKAGE_NAME_AUTOLITE);
	public static final String PACKAGE_NAME_CAMERA = "com.android.camera2";
	public static final String PACKAGE_NAME_MUSIC = SystemProperties.get("ro.se.music_package_name",
			PACKAGE_NAME_KUWO_MUSIC);
	public static final String PACKAGE_NAME_EDOG = SystemProperties.get("ro.se.edog_package_name",
			PACKAGE_NAME_DEAULT_EDOG);
	
	public static final String PACKAGE_NAME_FM = "com.example.administrator.fm";
	public static final String PACKAGE_NAME_SETTINGS = "com.android.settings";
	public static final String PACKAGE_NAME_VIDEO = SystemProperties.get("ro.se.video_package_name",PACKAGE_NAME_DEFAULT_VIDEO);
	public static final String PACKAGE_NAME_MOBILE_LINK = SystemProperties.get("ro.se.pc_package_name",PACKAGE_NAME_DEFAULT_MOBILE_LINK);
	public static final String PACKAGE_NAME_FILE = "com.softwinner.TvdFileManager";
	public static final String PACKAGE_NAME_APPLIST = "com.zqc.applist";
	
	public static final int KEY_GAODE = 1;
	public static final int KEY_KAILIDE = 2;
	public static final String KEY_WHICH_NAV = "key_which_navi";

	
	public static int[] img9ThreeImageNoSplite = new int[] { R.drawable.iv_9three_navi,R.drawable.iv_9three_music,
			R.drawable.iv_9three_bluetooth,R.drawable.iv_9three_edog,R.drawable.iv_9three_record,R.drawable.iv_9three_fm,
			R.drawable.iv_9three_video,R.drawable.iv_9three_file};
	public static String[] m9ThreePackageNoSplite = { PACKAGE_NAME_AUTOLITE, PACKAGE_NAME_MUSIC, PACKAGE_NAME_BLUETOOTH,PACKAGE_NAME_EDOG, 
			PACKAGE_NAME_CAMERA,PACKAGE_NAME_FM,PACKAGE_NAME_VIDEO,PACKAGE_NAME_FILE};
}
