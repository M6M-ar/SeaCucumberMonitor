package com.example.seacucumbermonitor;

import android.annotation.SuppressLint;
import android.content.res.AssetManager;
import android.graphics.Bitmap;

@SuppressLint("JniMissingFunction")
public class Yolov8Ncnn {

    static {
        System.loadLibrary("yolov8ncnn");
    }

    public native boolean loadModel(AssetManager mgr, int modelid, int cpugpu);

    public native Obj[] detect(Bitmap bitmap, boolean useGpu);

    public static class Obj {
        public float x;
        public float y;
        public float w;
        public float h;
        public String label;
        public float prob;
    }
}