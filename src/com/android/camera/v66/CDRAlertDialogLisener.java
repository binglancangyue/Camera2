package com.android.camera.v66;

interface ICDRAlertDialogListener {
    public void onClick(int state);
    
    public void onTimeClick(int hour, int minute);
    
    public void onDateClick(int year, int month, int day);
}
