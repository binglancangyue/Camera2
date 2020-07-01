package com.android.camera.home_recorder;

public class ItemData {
	private String mPackage;// 包名
	private int mName;// 名称id
	private int mImage;// 图片id

	public ItemData() {
	}

	public ItemData(String mPackage, int mName, int mImage) {
		this.mPackage = mPackage;
		this.mName = mName;
		this.mImage = mImage;
	}

	public ItemData(String mPackage, int mImage) {
		this.mPackage = mPackage;
		this.mImage = mImage;
	}

	public String getmPackage() {
		return mPackage;
	}

	public void setmPackage(String mPackage) {
		this.mPackage = mPackage;
	}

	public int getmName() {
		return mName;
	}

	public void setmName(int mName) {
		this.mName = mName;
	}

	public int getmImage() {
		return mImage;
	}

	public void setmImage(int mImage) {
		this.mImage = mImage;
	}

}
