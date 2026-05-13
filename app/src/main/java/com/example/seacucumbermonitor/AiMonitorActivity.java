package com.example.seacucumbermonitor;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.usb.UsbDevice;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Surface;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.serenegiant.usb.IFrameCallback;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.widget.AspectRatioSurfaceView;
import com.serenegiant.usb.Size;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class AiMonitorActivity extends AppCompatActivity {

    private static final int DEFAULT_PREVIEW_WIDTH = 640;
    private static final int DEFAULT_PREVIEW_HEIGHT = 480;

    private static final String TAG = "AiMonitorActivity";

    private static final int DETECT_WIDTH = 640;
    private static final int DETECT_HEIGHT = 360;
    private static final int DETECT_INTERVAL = 5;

    private int frameIndex = 0;


    private volatile int previewWidth = DEFAULT_PREVIEW_WIDTH;
    private volatile int previewHeight = DEFAULT_PREVIEW_HEIGHT;

    // 当前 yolov8.param / yolov8.bin 按 640 输入处理，速度不够时再改成 320。
    private static final int MODEL_ID = 0;
    private static final int CPU_GPU = 0; // 0 = CPU, 1 = GPU。先用 CPU 更稳定。

    private USBMonitor mUSBMonitor;
    private UVCCamera mUVCCamera;
    private AspectRatioSurfaceView mCameraView;
    private ImageView ivOverlay;
    private TextView tvCount;
    private TextView tvFps;

    private final Yolov8Ncnn yolov8ncnn = new Yolov8Ncnn();

    private final AtomicBoolean detecting = new AtomicBoolean(false);
    private final ExecutorService detectExecutor = Executors.newSingleThreadExecutor();

    private volatile boolean isAiRunning = false;
    private volatile boolean isCameraPreviewing = false;
    private volatile Bitmap lastFrameBitmap;

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

        mCameraView.setAspectRatio(DEFAULT_PREVIEW_WIDTH / (float) DEFAULT_PREVIEW_HEIGHT);

        findViewById(R.id.btnAiBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnAiSnapshot).setOnClickListener(v -> saveSnapshot());
    }

    private void initAi() {
        boolean success = yolov8ncnn.loadModel(getAssets(), MODEL_ID, CPU_GPU);

        if (success) {
            isAiRunning = true;
            Toast.makeText(this, "AI模型加载成功", Toast.LENGTH_SHORT).show();
        } else {
            isAiRunning = false;
            Toast.makeText(this, "AI模型加载失败，请检查 yolov8.param / yolov8.bin", Toast.LENGTH_LONG).show();
        }
    }

    private void initUSB() {
        mUSBMonitor = new USBMonitor(this, mOnDeviceConnectListener);
    }

    private final IFrameCallback mIFrameCallback = new IFrameCallback() {
        @Override
        public void onFrame(final ByteBuffer frame) {
            if (!isAiRunning || !isCameraPreviewing) {
                return;
            }

            frameIndex++;
            if (frameIndex % DETECT_INTERVAL != 0) {
                return;
            }

            if (!detecting.compareAndSet(false, true)) {
                return;
            }

            final int width = previewWidth;
            final int height = previewHeight;
            final byte[] frameBytes = copyFrameBytes(frame, width, height);

            detectExecutor.execute(() -> {
                Bitmap previewBitmap = null;
                Bitmap detectBitmap = null;

                try {
                    previewBitmap = convertFrameToBitmap(frameBytes, width, height);
                    if (previewBitmap == null) {
                        return;
                    }

                    Bitmap oldBitmap = lastFrameBitmap;
                    lastFrameBitmap = previewBitmap.copy(Bitmap.Config.ARGB_8888, false);
                    if (oldBitmap != null && !oldBitmap.isRecycled()) {
                        oldBitmap.recycle();
                    }

                    detectBitmap = Bitmap.createScaledBitmap(
                            previewBitmap,
                            DETECT_WIDTH,
                            DETECT_HEIGHT,
                            true
                    );

                    long startTime = System.currentTimeMillis();

                    Yolov8Ncnn.Obj[] objects = new Yolov8Ncnn.Obj[0];
                    long cost = Math.max(1, System.currentTimeMillis() - startTime);

                    runOnUiThread(() -> {
                        int count = objects == null ? 0 : objects.length;
                        tvCount.setText(String.valueOf(count));
                        tvFps.setText(String.format(Locale.US, "FPS: %.1f", 1000f / cost));

                        drawBoxes(objects, DETECT_WIDTH, DETECT_HEIGHT);
                    });

                } catch (Throwable t) {
                    Log.e(TAG, "AI frame detect failed", t);
                } finally {
                    if (previewBitmap != null && !previewBitmap.isRecycled()) {
                        previewBitmap.recycle();
                    }

                    if (detectBitmap != null && !detectBitmap.isRecycled()) {
                        detectBitmap.recycle();
                    }

                    detecting.set(false);
                }
            });
        }
    };

    private byte[] copyFrameBytes(ByteBuffer frame, int width, int height) {
        try {
            ByteBuffer buffer = frame.duplicate();
            buffer.rewind();

            int expectedSize = width * height * 3 / 2;
            int length = Math.min(buffer.remaining(), expectedSize);

            byte[] data = new byte[length];
            buffer.get(data, 0, length);

            return data;
        } catch (Exception e) {
            Log.e(TAG, "copyFrameBytes failed", e);
            return new byte[0];
        }
    }

    private Bitmap convertFrameToBitmap(byte[] nv21Data, int width, int height) {
        int expectedSize = width * height * 3 / 2;

        if (nv21Data == null || nv21Data.length < expectedSize) {
            Log.e(TAG, "NV21 data invalid, length=" + (nv21Data == null ? 0 : nv21Data.length)
                    + ", expected=" + expectedSize
                    + ", size=" + width + "x" + height);
            return null;
        }

        try {
            YuvImage yuvImage = new YuvImage(
                    nv21Data,
                    ImageFormat.NV21,
                    width,
                    height,
                    null
            );

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            boolean success = yuvImage.compressToJpeg(
                    new Rect(0, 0, width, height),
                    80,
                    outputStream
            );

            if (!success) {
                return null;
            }

            byte[] jpegBytes = outputStream.toByteArray();

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;

            Bitmap bitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.length, options);

            if (bitmap == null) {
                return null;
            }

            if (bitmap.getConfig() != Bitmap.Config.ARGB_8888) {
                bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false);
            }

            return bitmap;

        } catch (Throwable t) {
            Log.e(TAG, "convertFrameToBitmap failed", t);
            return null;
        }
    }

    private void drawBoxes(Yolov8Ncnn.Obj[] objects, int imageWidth, int imageHeight) {
        int overlayWidth = ivOverlay.getWidth();
        int overlayHeight = ivOverlay.getHeight();

        if (overlayWidth <= 0 || overlayHeight <= 0 || imageWidth <= 0 || imageHeight <= 0) {
            return;
        }

        Bitmap overlayBitmap = Bitmap.createBitmap(overlayWidth, overlayHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(overlayBitmap);
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        if (objects != null) {
            Paint boxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            boxPaint.setStyle(Paint.Style.STROKE);
            boxPaint.setStrokeWidth(5f);
            boxPaint.setColor(Color.parseColor("#00E676"));

            Paint textBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            textBgPaint.setStyle(Paint.Style.FILL);
            textBgPaint.setColor(Color.parseColor("#AA00C853"));

            Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(32f);
            textPaint.setFakeBoldText(true);

            float scaleX = overlayWidth / (float) imageWidth;
            float scaleY = overlayHeight / (float) imageHeight;

            for (Yolov8Ncnn.Obj obj : objects) {
                if (obj == null || obj.prob <= 0f) {
                    continue;
                }

                float left = obj.x * scaleX;
                float top = obj.y * scaleY;
                float right = (obj.x + obj.w) * scaleX;
                float bottom = (obj.y + obj.h) * scaleY;

                left = clamp(left, 0, overlayWidth);
                top = clamp(top, 0, overlayHeight);
                right = clamp(right, 0, overlayWidth);
                bottom = clamp(bottom, 0, overlayHeight);

                canvas.drawRect(left, top, right, bottom, boxPaint);

                String label = obj.label == null || obj.label.length() == 0
                        ? "sea_cucumber"
                        : obj.label;

                String text = String.format(Locale.US, "%s %.2f", label, obj.prob);

                float textWidth = textPaint.measureText(text);
                float textHeight = 40f;

                float textLeft = left;
                float textTop = Math.max(0, top - textHeight);

                canvas.drawRect(
                        textLeft,
                        textTop,
                        Math.min(textLeft + textWidth + 18f, overlayWidth),
                        textTop + textHeight,
                        textBgPaint
                );

                canvas.drawText(text, textLeft + 8f, textTop + 29f, textPaint);
            }
        }

        ivOverlay.setImageBitmap(overlayBitmap);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private void saveSnapshot() {
        Bitmap frame = lastFrameBitmap;

        if (frame == null) {
            Toast.makeText(this, "当前还没有可保存的画面", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Bitmap saveBitmap = frame.copy(Bitmap.Config.ARGB_8888, true);
            Canvas canvas = new Canvas(saveBitmap);

            Yolov8Ncnn.Obj[] objects = yolov8ncnn.detect(frame, false);
            drawSnapshotBoxes(canvas, objects, saveBitmap.getWidth(), saveBitmap.getHeight());

            String fileName = "SeaCucumber_" + System.currentTimeMillis() + ".jpg";

            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/SeaCucumberMonitor");
                values.put(MediaStore.Images.Media.IS_PENDING, 1);
            }

            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            if (uri == null) {
                Toast.makeText(this, "保存失败：无法创建图片文件", Toast.LENGTH_SHORT).show();
                return;
            }

            OutputStream outputStream = getContentResolver().openOutputStream(uri);
            if (outputStream == null) {
                Toast.makeText(this, "保存失败：无法写入图片", Toast.LENGTH_SHORT).show();
                return;
            }

            saveBitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream);
            outputStream.flush();
            outputStream.close();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear();
                values.put(MediaStore.Images.Media.IS_PENDING, 0);
                getContentResolver().update(uri, values, null, null);
            }

            Toast.makeText(this, "截图已保存到相册", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "截图保存失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void drawSnapshotBoxes(Canvas canvas, Yolov8Ncnn.Obj[] objects, int imageWidth, int imageHeight) {
        if (objects == null) {
            return;
        }

        Paint boxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(5f);
        boxPaint.setColor(Color.parseColor("#00E676"));

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(28f);
        textPaint.setFakeBoldText(true);

        Paint textBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textBgPaint.setStyle(Paint.Style.FILL);
        textBgPaint.setColor(Color.parseColor("#AA00C853"));

        for (Yolov8Ncnn.Obj obj : objects) {
            if (obj == null) {
                continue;
            }

            float left = clamp(obj.x, 0, imageWidth);
            float top = clamp(obj.y, 0, imageHeight);
            float right = clamp(obj.x + obj.w, 0, imageWidth);
            float bottom = clamp(obj.y + obj.h, 0, imageHeight);

            canvas.drawRect(left, top, right, bottom, boxPaint);

            String label = obj.label == null || obj.label.length() == 0
                    ? "sea_cucumber"
                    : obj.label;

            String text = String.format(Locale.US, "%s %.2f", label, obj.prob);

            float textWidth = textPaint.measureText(text);
            float textTop = Math.max(0, top - 36f);

            canvas.drawRect(left, textTop, Math.min(left + textWidth + 16f, imageWidth), textTop + 36f, textBgPaint);
            canvas.drawText(text, left + 8f, textTop + 26f, textPaint);
        }
    }

    private void openCamera(UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock) {
        try {
            closeCamera();

            mUVCCamera = new UVCCamera(null);

            int openResult = mUVCCamera.open(ctrlBlock);
            if (openResult != 0) {
                throw new RuntimeException("open camera failed, result=" + openResult);
            }

            Size realSize = mUVCCamera.getPreviewSize();
            if (realSize != null) {
                previewWidth = realSize.width;
                previewHeight = realSize.height;

                runOnUiThread(() -> {
                    mCameraView.setAspectRatio(previewWidth / (float) previewHeight);
                    Toast.makeText(
                            this,
                            "预览尺寸：" + previewWidth + "×" + previewHeight,
                            Toast.LENGTH_SHORT
                    ).show();
                });
            } else {
                previewWidth = DEFAULT_PREVIEW_WIDTH;
                previewHeight = DEFAULT_PREVIEW_HEIGHT;
            }

            Surface surface = mCameraView.getHolder().getSurface();
            mUVCCamera.setPreviewDisplay(surface);

            mUVCCamera.setFrameCallback(mIFrameCallback, UVCCamera.PIXEL_FORMAT_NV21);

            mUVCCamera.startPreview();
            isCameraPreviewing = true;

            runOnUiThread(() ->
                    Toast.makeText(this, "USB摄像头已连接", Toast.LENGTH_SHORT).show()
            );
        } catch (Exception e) {
            e.printStackTrace();
            isCameraPreviewing = false;

            runOnUiThread(() ->
                    Toast.makeText(this, "USB摄像头打开失败：" + e.getMessage(), Toast.LENGTH_LONG).show()
            );
        }
    }

    private void closeCamera() {
        isCameraPreviewing = false;

        if (mUVCCamera != null) {
            try {
                mUVCCamera.setFrameCallback(null, 0);
            } catch (Exception ignored) {
            }

            try {
                mUVCCamera.stopPreview();
            } catch (Exception ignored) {
            }

            try {
                mUVCCamera.destroy();
            } catch (Exception ignored) {
            }

            mUVCCamera = null;
        }
    }

    private final USBMonitor.OnDeviceConnectListener mOnDeviceConnectListener =
            new USBMonitor.OnDeviceConnectListener() {
                @Override
                public void onAttach(UsbDevice device) {
                    if (mUSBMonitor != null) {
                        mUSBMonitor.requestPermission(device);
                    }
                }

                @Override
                public void onDeviceOpen(UsbDevice device,
                                         USBMonitor.UsbControlBlock ctrlBlock,
                                         boolean createNew) {
                    openCamera(device, ctrlBlock);
                }

                @Override
                public void onDeviceClose(UsbDevice device,
                                          USBMonitor.UsbControlBlock ctrlBlock) {
                    closeCamera();
                }

                @Override
                public void onDetach(UsbDevice device) {
                    closeCamera();
                }

                @Override
                public void onCancel(UsbDevice device) {
                    runOnUiThread(() ->
                            Toast.makeText(AiMonitorActivity.this, "USB摄像头授权已取消", Toast.LENGTH_SHORT).show()
                    );
                }
            };

    @Override
    protected void onStart() {
        super.onStart();
        if (mUSBMonitor != null) {
            mUSBMonitor.register();
        }
    }

    @Override
    protected void onStop() {
        closeCamera();

        if (mUSBMonitor != null) {
            try {
                mUSBMonitor.unregister();
            } catch (Exception ignored) {
            }
        }

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        closeCamera();

        if (mUSBMonitor != null) {
            try {
                mUSBMonitor.destroy();
            } catch (Exception ignored) {
            }
            mUSBMonitor = null;
        }

        detectExecutor.shutdownNow();

        if (lastFrameBitmap != null && !lastFrameBitmap.isRecycled()) {
            lastFrameBitmap.recycle();
            lastFrameBitmap = null;
        }

        super.onDestroy();
    }
}