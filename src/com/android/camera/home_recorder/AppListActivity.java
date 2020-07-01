package com.android.camera.home_recorder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.android.camera2.R;
public class AppListActivity extends Activity {
	private static final boolean DEBUG = false;
	private static final String TAG = "AppListActivity";
	private GridView gridView;
	private ArrayList<AppInfo> appList = new ArrayList<AppInfo>();
	private MyAdapter adapter;
	private boolean isSelecteDefault = false;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_applist);
		Intent intent = getIntent();
		if (null != intent) {
			isSelecteDefault = intent.getBooleanExtra("choose_package", false);
		}
		initView();
		initData();
	}

	private void initData() {
		getAppList();
	}

	private void getAppList() {
		if (appList.size() > 0) {
			appList.clear();
		}
		PackageManager pm = getPackageManager();
		
        Intent main = new Intent(Intent.ACTION_MAIN, null);
        main.addCategory(Intent.CATEGORY_LAUNCHER);
        final List<ResolveInfo> apps = pm.queryIntentActivities(main, 0);
        Collections.sort(apps, new ResolveInfo.DisplayNameComparator(pm));
        if (apps != null) {
            for (int i = 0; i < apps.size(); i++) {
            	ResolveInfo resolveinfo = apps.get(i);
               	if (!ignore(this,resolveinfo.activityInfo.packageName)) {
            		AppInfo info = new AppInfo();
            		
            		//cywl
            		if(resolveinfo.activityInfo.packageName.equals("com.ligo.appshare_qichen")){
            			info.setAppName(this.getResources().getString(R.string.mobile_internet));
            		}else {
            			info.setAppName(resolveinfo.loadLabel(pm).toString());
            		}
            		
            		info.setPkgName(resolveinfo.activityInfo.packageName);
            		info.setFlags(resolveinfo.activityInfo.flags);
            		info.setAppIcon(resolveinfo.activityInfo.loadIcon(pm));
            		info.setAppintent(pm.getLaunchIntentForPackage(resolveinfo.activityInfo.packageName));
            		appList.add(info);
               	}
            	
            }
        }
		adapter.notifyDataSetChanged();
	}
	
    public static boolean ignore(Context context,String pkg){
    	boolean ret = false;
//    	if(pkg.equals("com.tencent.qqmusic")
//			|| pkg.equals("com.iflytek.inputmethod")
//			|| pkg.equals("com.android.settings")
//			|| pkg.equals("se.setting")
//			|| pkg.equals("se.fm")
//			|| pkg.equals("com.sohu.inputmethod.sogou")){
//    		ret = true;
//    	}
    	
    	
    	if (pkg.equals("com.zqc.screenout")) {
    		ret = true;
		}
    	return ret;
    }
	
	
	
	
	private void addApp(PackageManager pm, PackageInfo packageInfo) {
		AppInfo info = new AppInfo();
		info.setAppName(packageInfo.applicationInfo.loadLabel(pm).toString());
		info.setPkgName(packageInfo.packageName);
		info.setFlags(packageInfo.applicationInfo.flags);
		info.setAppIcon(packageInfo.applicationInfo.loadIcon(pm));
		info.setAppintent(pm.getLaunchIntentForPackage(packageInfo.packageName));
		appList.add(info);
	}

	private void initView() {
		gridView = (GridView) findViewById(R.id.gv);
		adapter = new MyAdapter();
		gridView.setAdapter(adapter);
		gridView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				if (isSelecteDefault) {
					String packageName = appList.get(arg2).getPkgName();
					Intent intent = new Intent();
					intent.putExtra("package", packageName);
					AppListActivity.this.setResult(HomeRecorderActivity.RESULT_CODE, intent);
					AppListActivity.this.finish();
				} else {
					if (Constances.IS_NEED_PASSWD_ENTER_STORE && Constances.PACKAGE_NAME_GOOGLESTORE.equals(appList.get(arg2).getPkgName())) {
//						enterPassword(appList.get(arg2).getAppintent());
					}else{
						String mPackName = appList.get(arg2).getPkgName();
                       Log.d(TAG, "mPackName :-->else" + mPackName);
						
						if (Constances.PACKAGE_NAME_WIFI_HOT.equals(mPackName)){
							Log.d(TAG, "mPackName :" + mPackName);
							Intent intent = new Intent();
							intent.setComponent(new ComponentName(Constances.PACKAGE_NAME_SETTINGS,
									Constances.PACKAGE_NAME_WIFI_HOT));
							intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							startActivity(intent);
						}else {		
							Log.d(TAG, "mPackName :-->else" + mPackName);
							startActivity(appList.get(arg2).getAppintent());
						}
					}
				}

			}
		});

		gridView.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				AppInfo aInfo = appList.get(arg2);
				if((aInfo.getFlags() & ApplicationInfo.FLAG_SYSTEM) == 0){
					Uri uri = Uri.parse("package:" + aInfo.getPkgName());
					Intent intent = new Intent();
					intent.setAction(Intent.ACTION_DELETE);
					intent.setData(uri);
					startActivity(intent);
				}
				return true;
			}

		});
		registerReceiver();
	}
	

	private void registerReceiver() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(Constances.ACTION_NAME_CLOSE_APPLIST);
		filter.addAction(Intent.ACTION_PACKAGE_ADDED);
		filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
		filter.addDataScheme("package");
		registerReceiver(receiver, filter);
		IntentFilter homeFilter = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
		registerReceiver(homeReceiver, homeFilter);
		IntentFilter closeFilter = new IntentFilter(Constances.ACTION_NAME_CLOSE_APPLIST);
		registerReceiver(closeReceiver, closeFilter);
	}

	private BroadcastReceiver receiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(Intent.ACTION_PACKAGE_ADDED) || action.equals(Intent.ACTION_PACKAGE_REMOVED)) {
				if (DEBUG) Log.i(TAG, "action = " + action);
				getAppList();
			}
		}
	};

	private BroadcastReceiver homeReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (DEBUG) Log.i(TAG, "action = "+action);
			if (action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
				Log.i(TAG, "Intent.ACTION_CLOSE_SYSTEM_DIALOGS");
				String reason = intent.getStringExtra(Constances.SYSTEM_REASON);
				if (TextUtils.equals(reason, Constances.SYSTEM_HOME_KEY)) {
					if (DEBUG) Log.i(TAG, "press home key !");
					AppListActivity.this.finish();
				} else if (TextUtils.equals(reason, Constances.SYSTEM_HOME_KEY_LONG)) {
					if (DEBUG) Log.i(TAG, "long press home key !");
				}
			}

		}
	};
	
	private BroadcastReceiver closeReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if(action.equals(Constances.ACTION_NAME_CLOSE_APPLIST)){
				if (DEBUG) Log.i(TAG, "close applist!");
				AppListActivity.this.finish();
			}
		}
	};

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();

	}

	@Override
	protected void onDestroy() {
		unregisterReceiver(receiver);
		unregisterReceiver(homeReceiver);
		super.onDestroy();
	}

	class MyAdapter extends BaseAdapter {
		@Override
		public int getCount() {
			return appList.size();
		}

		@Override
		public Object getItem(int position) {
			return position;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = View.inflate(AppListActivity.this, R.layout.item_apps, null);
			ImageView imageView = (ImageView) view.findViewById(R.id.img_apps);
			imageView.setBackground(appList.get(position).getAppIcon());
			TextView textView = (TextView) view.findViewById(R.id.tv_apps);
			textView.setText(appList.get(position).getAppName());
			return view;
		}
	}
}
