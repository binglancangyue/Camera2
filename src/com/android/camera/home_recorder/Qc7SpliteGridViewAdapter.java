package com.android.camera.home_recorder;

import java.util.List;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;
import com.android.camera2.R;

public class Qc7SpliteGridViewAdapter extends BaseAdapter {
	private Context mContext;
	private List<ItemData> datas;
	private LayoutInflater inflater;

	public Qc7SpliteGridViewAdapter(Context mContext, List<ItemData> datas) {
		this.mContext = mContext;
		this.datas = datas;
		inflater = LayoutInflater.from(this.mContext);
	}

	@Override
	public int getCount() {
		return datas.size();
	}

	@Override
	public ItemData getItem(int position) {
		return datas.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder viewHolder;
		if (null == convertView) {
			viewHolder = new ViewHolder();
			convertView = inflater.inflate(R.layout.item_view_qc7_splite, null);
			viewHolder.imageView = (ImageView) convertView.findViewById(R.id.image);
			convertView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}

		ItemData data = datas.get(position);
		viewHolder.imageView.setImageResource(data.getmImage());
		return convertView;
	}

	private static class ViewHolder {
		ImageView imageView;
	}

}
