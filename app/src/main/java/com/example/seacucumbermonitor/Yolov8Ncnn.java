package com.example.seacucumbermonitor;

import android.content.res.AssetManager;
import android.graphics.Bitmap;

public class Yolov8Ncnn {
    // 加载我们编译好的 C++ 动态库 (库名通常在 CMakeLists.txt 里定义为 yolov8ncnn)
    static {
        System.loadLibrary("yolov8ncnn");
    }

    // 初始化模型：传入 assets 管理器，模型 ID，以及是否使用 GPU
    public native boolean loadModel(AssetManager mgr, int modelid, int cpugpu);

    // 执行检测：传入图片，返回识别到的物体信息（坐标、类别、概率）
    public native Obj[] detect(Bitmap bitmap, boolean use_gpu);

    // 定义识别结果的内部类
    public static class Obj {
        public float x;
        public float y;
        public float w;
        public float h;
        public String label;
        public float prob;
    }
}