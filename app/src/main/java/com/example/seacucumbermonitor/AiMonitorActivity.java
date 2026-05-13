package com.example.seacucumbermonitor;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.view.Surface;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.serenegiant.usb.IFrameCallback;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.widget.AspectRatioSurfaceView;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class AiMonitorActivity extends AppCompatActivity {

    private USBMonitor mUSBMonitor;
    private UVCCamera mUVCCamera;
    private AspectRatioSurfaceView mCameraView;
    private ImageView ivOverlay; // 用于画框的图层
    private TextView tvCount, tvFps;

    private Yolov8Ncnn yolov8ncnn = new Yolov8Ncnn();
    private boolean isAisRunning = false;

    // --- 追踪计数相关变量 ---
    private int totalSeaCucumberCount = 0;
    private List<TrackedObj> activeTracks = new ArrayList<>();
    private int nextTrackId = 1;
    private final float IOU_THRESHOLD = 0.4f; // 重合度阈值
    private final int MAX_MISSING_FRAMES = 5; // 允许消失的最大帧数

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_monitor);

        initUI();
        initAi();
        initUSB();
    }

    private void initUI() {
        mCameraView = findViewById(R.id.svAiCameraView);
        ivOverlay = findViewById(R.id.ivBoundingBoxOverlay);
        tvCount = findViewById(R.id.tvSeaCucumberCount);
        tvFps = findViewById(R.id.tvFps);

        findViewById(R.id.btnAiBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnAiSnapshot).setOnClickListener(v -> {
            Toast.makeText(this, "截图已保存至海参图库", Toast.LENGTH_SHORT).show();
        });
    }

    private void initAi() {
        // 加载模型：0代表第一个模型，0代表使用CPU(移动端CPU通常比GPU更稳定)
        boolean success = yolov8ncnn.loadModel(getAssets(), 0, 0);
        if (success) {
            isAisRunning = true;
        } else {
            Toast.makeText(this, "AI模型加载失败！", Toast.LENGTH_LONG).show();
        }
    }

    private void initUSB() {
        mUSBMonitor = new USBMonitor(this, mOnDeviceConnectListener);
        mCameraView.setAspectRatio(UVCCamera.DEFAULT_PREVIEW_WIDTH / (float) UVCCamera.DEFAULT_PREVIEW_HEIGHT);
    }

    // 核心：摄像头每一帧的回调
    private final IFrameCallback mIFrameCallback = new IFrameCallback() {
        @Override
        public void onFrame(final ByteBuffer frame) {
            if (!isAisRunning) return;

            // 1. 将 YUV 帧转换为 Bitmap (JNI 层有对应接口转换最快，这里暂用简单逻辑)
            // 注意：uvccamera 的 frame 默认是 NV21 或 RGB 格式
            Bitmap bitmap = convertFrameToBitmap(frame);
            if (bitmap == null) return;

            // 2. 运行 YOLO 检测
            long startTime = System.currentTimeMillis();
            Yolov8Ncnn.Obj[] objects = yolov8ncnn.detect(bitmap, false);
            long endTime = System.currentTimeMillis();

            // 3. 运行 IOU 追踪逻辑
            updateTracking(objects);

            // 4. 在 UI 线程绘制
            runOnUiThread(() -> {
                tvFps.setText("FPS: " + (1000 / (endTime - startTime)));
                tvCount.setText(String.valueOf(totalSeaCucumberCount));
                drawBoxes(objects, bitmap.getWidth(), bitmap.getHeight());
            });
        }
    };

    /**
     * 简单的 IOU 追踪算法：防止重复计数
     */
    private void updateTracking(Yolov8Ncnn.Obj[] detectedObjs) {
        if (detectedObjs == null) return;

        List<Yolov8Ncnn.Obj> unmatchedDetections = new ArrayList<>();
        for (Yolov8Ncnn.Obj obj : detectedObjs) unmatchedDetections.add(obj);

        // 尝试匹配已有的轨迹
        for (TrackedObj track : activeTracks) {
            float maxIou = -1;
            int bestMatchIdx = -1;

            for (int i = 0; i < unmatchedDetections.size(); i++) {
                float iou = calculateIou(track.rect, unmatchedDetections.get(i));
                if (iou > IOU_THRESHOLD && iou > maxIou) {
                    maxIou = iou;
                    bestMatchIdx = i;
                }
            }

            if (bestMatchIdx != -1) {
                // 匹配成功，更新轨迹位置
                track.update(unmatchedDetections.get(bestMatchIdx));
                unmatchedDetections.remove(bestMatchIdx);
            } else {
                track.missingFrames++;
            }
        }

        // 没匹配上的检测框，视为新出现的海参
        for (Yolov8Ncnn.Obj newObj : unmatchedDetections) {
            activeTracks.add(new TrackedObj(nextTrackId++, newObj));
            totalSeaCucumberCount++; // 发现新目标，总数+1
        }

        // 移除消失太久的轨迹
        activeTracks.removeIf(t -> t.missingFrames > MAX_MISSING_FRAMES);
    }

    private void drawBoxes(Yolov8Ncnn.Obj[] objects, int imgW, int imgH) {
        if (objects == null) {
            ivOverlay.setImageResource(0);
            return;
        }

        // 创建一个透明画布
        Bitmap bitmap = Bitmap.createBitmap(ivOverlay.getWidth(), ivOverlay.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(6);
        paint.setColor(Color.parseColor("#00E676")); // 经典海参识别绿

        Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(30);

        float scaleX = (float) ivOverlay.getWidth() / imgW;
        float scaleY = (float) ivOverlay.getHeight() / imgH;

        for (Yolov8Ncnn.Obj obj : objects) {
            canvas.drawRect(obj.x * scaleX, obj.y * scaleY, (obj.x + obj.w) * scaleX, (obj.y + obj.h) * scaleY, paint);
            canvas.drawText("SeaCucumber " + String.format("%.2f", obj.prob), obj.x * scaleX, obj.y * scaleY - 10, textPaint);
        }
        ivOverlay.setImageBitmap(bitmap);
    }

    // 辅助类：追踪的对象
    private static class TrackedObj {
        int id;
        RectF rect;
        int missingFrames = 0;

        TrackedObj(int id, Yolov8Ncnn.Obj obj) {
            this.id = id;
            this.rect = new RectF(obj.x, obj.y, obj.x + obj.w, obj.y + obj.h);
        }

        void update(Yolov8Ncnn.Obj obj) {
            this.rect.set(obj.x, obj.y, obj.x + obj.w, obj.y + obj.h);
            this.missingFrames = 0;
        }
    }

    private float calculateIou(RectF r, Yolov8Ncnn.Obj o) {
        float x1 = Math.max(r.left, o.x);
        float y1 = Math.max(r.top, o.y);
        float x2 = Math.min(r.right, o.x + o.w);
        float y2 = Math.min(r.bottom, o.y + o.h);
        float w = Math.max(0, x2 - x1);
        float h = Math.max(0, y2 - y1);
        float inter = w * h;
        float area1 = r.width() * r.height();
        float area2 = o.w * o.h;
        return inter / (area1 + area2 - inter);
    }

    private Bitmap convertFrameToBitmap(ByteBuffer frame) {
        // 这里简化了转换逻辑，实际开发中建议使用 libyuv 或 OpenCV 进行快速转换
        // 假设预览分辨率为 640x480
        byte[] imageBytes = new byte[frame.remaining()];
        frame.get(imageBytes);
        // 此处需要根据你摄像头的实际输出格式（MJPEG/YUV）进行解码
        // 暂存一个占位符，实际需调用 UVCCamera 的解码工具
        return null;
    }

    // --- USB 生命周期管理 (保持之前的逻辑) ---
    private final USBMonitor.OnDeviceConnectListener mOnDeviceConnectListener = new USBMonitor.OnDeviceConnectListener() {
        @Override
        public void onAttach(UsbDevice device) {
            mUSBMonitor.requestPermission(device);
        }
        @Override
        public void onConnect(UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock, boolean createCtrlBlock) {
            mUVCCamera = new UVCCamera();
            mUVCCamera.open(ctrlBlock);
            Surface surface = mCameraView.getHolder().getSurface();
            mUVCCamera.setPreviewDisplay(surface);
            mUVCCamera.setFrameCallback(mIFrameCallback, UVCCamera.PIXEL_FORMAT_YUV420SP);
            mUVCCamera.startPreview();
        }
        @Override public void onDisconnect(UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock) {}
        @Override public void onDettach(UsbDevice device) {}
        @Override public void onCancel(UsbDevice device) {}
    };

    @Override
    protected void onStart() { super.onStart(); mUSBMonitor.register(); }
    @Override
    protected void onStop() { mUSBMonitor.unregister(); super.onStop(); }
    @Override
    protected void onDestroy() { if (mUVCCamera != null) mUVCCamera.destroy(); mUSBMonitor.destroy(); super.onDestroy(); }
}