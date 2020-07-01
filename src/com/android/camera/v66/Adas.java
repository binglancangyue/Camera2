package com.android.camera.v66;

public class Adas {
    
    public Lane lane;
    public Cars cars;
    public int score;
    // public String version;
    public int subWidth;
    public int subHeight;
    
    public static class Cars {
        public int num;
        public Car[] carP;
    }
    
    public static class Lane {
        public byte isDisp;
        public byte ltWarn;
        public byte rtWarn;
        public PointC[] ltIdxs;
        public PointC[] rtIdxs;
        public int colorPointsNum;
        public byte dnColor;
        public int[] rows;
        public int[] ltCols;
        public int[] mdCols;
        public int[] rtCols;
    }
    
    public static class RectC {
        public int x;
        public int y;
        public int width;
        public int height;
    }
    
    public static class PointC {
        public int x;
        public int y;
    }
    
    public static class Car {
        public byte isWarn;
        public byte color;
        public float dist;
        public float time;
        public RectC idx;
    }
}
