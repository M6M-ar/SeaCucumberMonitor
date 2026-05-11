package com.example.seacucumbermonitor;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.herohan.uvcapp.CameraHelper;
import com.herohan.uvcapp.ICameraHelper;
import com.herohan.uvcapp.ImageCapture;
import com.herohan.uvcapp.VideoCapture;
import com.serenegiant.usb.Size;
import com.serenegiant.widget.AspectRatioSurfaceView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class UsbCameraActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "UsbCameraActivity";
    private static final boolean DEBUG = true;

    private static final int DEFAULT_WIDTH = 640;
    private static final int DEFAULT_HEIGHT = 480;
    private static final int REQUEST_PERMISSIONS = 1001;

    private ICameraHelper mCameraHelper;
    private AspectRatioSurfaceView mCameraViewMain;

    private TextView tvStatus;
    private TextView tvDeviceInfo;
    private TextView tvRecordState;

    private MaterialButton btnOpenCamera;
    private MaterialButton btnCaptureImage;
    private MaterialButton btnRecordVideo;
    private MaterialButton btnCloseCamera;

    private UsbDevice currentDevice;
    private boolean isCameraOpened = false;
    private boolean isSelectingDevice = false;
    private boolean isFullScreen = false; // 记录当前是否为全屏

    private final ICameraHelper.StateCallback mStateCallback = new ICameraHelper.StateCallback() {
        @Override
        public void onAttach(UsbDevice device) {
            if (DEBUG) Log.d(TAG, "onAttach: " + device);
            currentDevice = device;

            runOnUiThread(() -> {
                setStatus("检测到 USB 摄像头，正在准备打开...");
                tvDeviceInfo.setText(getDeviceSimpleInfo(device));
            });

            selectDevice(device);
        }

        @Override
        public void onDeviceOpen(UsbDevice device, boolean isFirstOpen) {
            if (DEBUG) Log.d(TAG, "onDeviceOpen: " + device);
            currentDevice = device;
            isSelectingDevice = false;

            runOnUiThread(() -> setStatus("USB 设备已连接，正在打开摄像头..."));

            if (mCameraHelper != null) {
                mCameraHelper.openCamera();
            }
        }

        @Override
        public void onCameraOpen(UsbDevice device) {
            if (DEBUG) Log.d(TAG, "onCameraOpen: " + device);

            isCameraOpened = true;
            currentDevice = device;

            if (mCameraHelper != null) {
                mCameraHelper.startPreview();

                Size size = mCameraHelper.getPreviewSize();
                if (size != null) {
                    mCameraViewMain.setAspectRatio(size.width, size.height);
                } else {
                    mCameraViewMain.setAspectRatio(DEFAULT_WIDTH, DEFAULT_HEIGHT);
                }

                SurfaceHolder holder = mCameraViewMain.getHolder();
                if (holder != null && holder.getSurface() != null) {
                    mCameraHelper.addSurface(holder.getSurface(), false);
                }
            }

            runOnUiThread(() -> {
                setStatus("USB 摄像头已打开");
                tvDeviceInfo.setText(getDeviceSimpleInfo(device));
            });
        }

        @Override
        public void onCameraClose(UsbDevice device) {
            if (DEBUG) Log.d(TAG, "onCameraClose: " + device);

            isCameraOpened = false;
            updateRecordUi(false);

            if (mCameraHelper != null) {
                SurfaceHolder holder = mCameraViewMain.getHolder();
                if (holder != null && holder.getSurface() != null) {
                    mCameraHelper.removeSurface(holder.getSurface());
                }
            }

            runOnUiThread(() -> setStatus("摄像头已关闭"));
        }

        @Override
        public void onDeviceClose(UsbDevice device) {
            if (DEBUG) Log.d(TAG, "onDeviceClose: " + device);
            isCameraOpened = false;
            isSelectingDevice = false;

            runOnUiThread(() -> setStatus("USB 设备已关闭"));
        }

        @Override
        public void onDetach(UsbDevice device) {
            if (DEBUG) Log.d(TAG, "onDetach: " + device);

            isCameraOpened = false;
            isSelectingDevice = false;
            updateRecordUi(false);

            runOnUiThread(() -> {
                setStatus("USB 摄像头已断开");
                tvDeviceInfo.setText("未连接设备");
            });
        }

        @Override
        public void onCancel(UsbDevice device) {
            if (DEBUG) Log.d(TAG, "onCancel: " + device);

            isSelectingDevice = false;

            runOnUiThread(() -> {
                setStatus("USB 授权被取消或打开失败");
                if (device != null) {
                    tvDeviceInfo.setText(getDeviceSimpleInfo(device));
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usb_camera);
        initViews();
        checkAndRequestPermissions();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (hasAllPermissions()) {
            initCameraHelper();

            mCameraViewMain.postDelayed(this::openFirstUsbDeviceIfExists, 600);
        }
    }

    @Override
    protected void onStop() {
        clearCameraHelper();
        super.onStop();
    }

    private void initViews() {
        tvStatus = findViewById(R.id.tvStatus);
        tvDeviceInfo = findViewById(R.id.tvDeviceInfo);
        tvRecordState = findViewById(R.id.tvRecordState);

        btnOpenCamera = findViewById(R.id.btnOpenCamera);
        btnCaptureImage = findViewById(R.id.btnCaptureImage);
        btnRecordVideo = findViewById(R.id.btnRecordVideo);
        btnCloseCamera = findViewById(R.id.btnCloseCamera);

        btnOpenCamera.setOnClickListener(this);
        btnCaptureImage.setOnClickListener(this);
        btnRecordVideo.setOnClickListener(this);
        btnCloseCamera.setOnClickListener(this);

        mCameraViewMain = findViewById(R.id.svCameraViewMain);
        mCameraViewMain.setAspectRatio(DEFAULT_WIDTH, DEFAULT_HEIGHT);

        if (mCameraViewMain != null) {
            mCameraViewMain.setOnClickListener(v -> toggleFullScreen());
        }
        mCameraViewMain.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                if (mCameraHelper != null && isCameraOpened) {
                    mCameraHelper.addSurface(holder.getSurface(), false);
                }
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                if (mCameraHelper != null) {
                    mCameraHelper.removeSurface(holder.getSurface());
                }
            }
        });

        setStatus("等待连接 USB 摄像头...");
        tvDeviceInfo.setText("未连接设备");
    }

    private void initCameraHelper() {
        if (mCameraHelper == null) {
            mCameraHelper = new CameraHelper();
            mCameraHelper.setStateCallback(mStateCallback);
        }
    }

    private void clearCameraHelper() {
        if (mCameraHelper != null) {
            if (mCameraHelper.isRecording()) {
                mCameraHelper.stopRecording();
            }
            mCameraHelper.release();
            mCameraHelper = null;
        }
        isCameraOpened = false;
        isSelectingDevice = false;
        updateRecordUi(false);
    }

    private boolean hasAllPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void checkAndRequestPermissions() {
        if (!hasAllPermissions()) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.CAMERA,
                            Manifest.permission.RECORD_AUDIO
                    },
                    REQUEST_PERMISSIONS
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSIONS) {
            if (hasAllPermissions()) {
                setStatus("权限已允许，现在可以连接 USB 摄像头");
                initCameraHelper();
                mCameraViewMain.postDelayed(this::openFirstUsbDeviceIfExists, 600);
            } else {
                setStatus("请先允许相机和麦克风权限，否则无法拍照/录像");
            }
        }
    }

    private void openFirstUsbDeviceIfExists() {
        if (!hasAllPermissions()) {
            checkAndRequestPermissions();
            return;
        }

        if (mCameraHelper == null) {
            initCameraHelper();
        }

        if (isCameraOpened) {
            setStatus("摄像头已经打开");
            return;
        }

        List<?> deviceList = mCameraHelper.getDeviceList();
        if (deviceList == null || deviceList.isEmpty()) {
            setStatus("未检测到 USB 摄像头，请检查 OTG 和摄像头连接");
            return;
        }

        Object first = deviceList.get(0);
        if (first instanceof UsbDevice) {
            selectDevice((UsbDevice) first);
        } else {
            setStatus("检测到设备，但设备类型异常");
        }
    }

    private void selectDevice(UsbDevice device) {
        if (device == null || mCameraHelper == null) {
            setStatus("USB 设备为空或初始化失败");
            return;
        }

        if (isSelectingDevice) {
            setStatus("正在连接 USB 摄像头，请稍等...");
            return;
        }

        isSelectingDevice = true;
        currentDevice = device;
        setStatus("正在请求 USB 授权，请在系统弹窗中点击“确定”");
        tvDeviceInfo.setText(getDeviceSimpleInfo(device));
        mCameraHelper.selectDevice(device);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.btnOpenCamera) {
            openFirstUsbDeviceIfExists();

        } else if (id == R.id.btnCaptureImage) {
            takePicture();

        } else if (id == R.id.btnRecordVideo) {
            if (mCameraHelper == null || !isCameraOpened) {
                Toast.makeText(this, "请先打开摄像头", Toast.LENGTH_SHORT).show();
                return;
            }

            if (mCameraHelper.isRecording()) {
                stopRecord();
            } else {
                startRecord();
            }

        } else if (id == R.id.btnCloseCamera) {
            if (mCameraHelper != null) {
                if (mCameraHelper.isRecording()) {
                    mCameraHelper.stopRecording();
                }
                mCameraHelper.closeCamera();
            }
        }
    }

    private void takePicture() {
        if (mCameraHelper == null || !isCameraOpened) {
            Toast.makeText(this, "请先打开摄像头", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileName = "SCM_IMG_" + getTimeString() + ".jpg";

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_DCIM + "/SeaCucumberMonitor");
        }

        ImageCapture.OutputFileOptions options =
                new ImageCapture.OutputFileOptions.Builder(
                        getContentResolver(),
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues
                ).build();

        mCameraHelper.takePicture(options, new ImageCapture.OnImageCaptureCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                runOnUiThread(() -> {
                    Toast.makeText(UsbCameraActivity.this, "拍照成功，已保存到相册", Toast.LENGTH_SHORT).show();
                    setStatus("拍照成功");
                });
            }

            @Override
            public void onError(int imageCaptureError, @NonNull String message, @Nullable Throwable cause) {
                runOnUiThread(() -> Toast.makeText(UsbCameraActivity.this, "拍照失败：" + message, Toast.LENGTH_LONG).show());
            }
        });
    }

    private void startRecord() {
        String fileName = "SCM_VIDEO_" + getTimeString() + ".mp4";

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_MOVIES + "/SeaCucumberMonitor");
        }

        VideoCapture.OutputFileOptions options =
                new VideoCapture.OutputFileOptions.Builder(
                        getContentResolver(),
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        contentValues
                ).build();

        mCameraHelper.startRecording(options, new VideoCapture.OnVideoCaptureCallback() {
            @Override
            public void onStart() {
                runOnUiThread(() -> {
                    updateRecordUi(true);
                    setStatus("正在录像...");
                    Toast.makeText(UsbCameraActivity.this, "开始录像", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onVideoSaved(@NonNull VideoCapture.OutputFileResults outputFileResults) {
                runOnUiThread(() -> {
                    updateRecordUi(false);
                    setStatus("录像已保存到相册");
                    Toast.makeText(UsbCameraActivity.this, "录像保存成功", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(int videoCaptureError, @NonNull String message, @Nullable Throwable cause) {
                runOnUiThread(() -> {
                    updateRecordUi(false);
                    Toast.makeText(UsbCameraActivity.this, "录像失败：" + message, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void stopRecord() {
        if (mCameraHelper != null) {
            mCameraHelper.stopRecording();
        }
    }

    private void updateRecordUi(boolean isRecording) {
        runOnUiThread(() -> {
            tvRecordState.setVisibility(isRecording ? View.VISIBLE : View.GONE);
            btnRecordVideo.setText(isRecording ? "停止录像" : "开始录像");
        });
    }

    private void setStatus(String text) {
        tvStatus.setText(text);
    }

    private String getDeviceSimpleInfo(UsbDevice device) {
        if (device == null) {
            return "未连接设备";
        }
        return "deviceName=" + device.getDeviceName()
                + "\nvendorId=" + device.getVendorId()
                + "\nproductId=" + device.getProductId();
    }

    private String getTimeString() {
        return new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
    }
    // --- 新增的全屏切换逻辑 ---
    private void toggleFullScreen() {
        if (mCameraViewMain == null) return;
        isFullScreen = !isFullScreen;

        // 获取需要隐藏/显示的 UI 控件
        View pageTitle = findViewById(R.id.tvPageTitle);
        View status = findViewById(R.id.tvStatus);
        View controls = findViewById(R.id.cardControls);
        View videoCard = findViewById(R.id.cardPreview);

        if (isFullScreen) {
            // 【1. 切换到全屏横屏】
            if (pageTitle != null) pageTitle.setVisibility(View.GONE);
            if (status != null) status.setVisibility(View.GONE);
            if (controls != null) controls.setVisibility(View.GONE);

            // 改变屏幕方向为横屏
            setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

            // 让包裹视频的卡片填满全屏
            if (videoCard != null) {
                androidx.constraintlayout.widget.ConstraintLayout.LayoutParams params =
                        (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) videoCard.getLayoutParams();
                params.setMargins(0, 0, 0, 0); // 去掉四周边距
                params.matchConstraintPercentHeight = 1.0f; // 高度撑满 100%
                videoCard.setLayoutParams(params);

                if (videoCard instanceof com.google.android.material.card.MaterialCardView) {
                    ((com.google.android.material.card.MaterialCardView) videoCard).setRadius(0f); // 去掉圆角
                }
            }

            // 隐藏系统状态栏和导航栏 (沉浸式)
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

            Toast.makeText(this, "已进入全屏，再次点击画面退出", Toast.LENGTH_SHORT).show();

        } else {
            // 【2. 恢复到正常竖屏】
            if (pageTitle != null) pageTitle.setVisibility(View.VISIBLE);
            if (status != null) status.setVisibility(View.VISIBLE);
            if (controls != null) controls.setVisibility(View.VISIBLE);

            // 改变屏幕方向为竖屏
            setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

            // 恢复视频卡片的布局属性
            if (videoCard != null) {
                float density = getResources().getDisplayMetrics().density;
                int margin = (int) (20 * density); // 恢复 20dp 的边距

                androidx.constraintlayout.widget.ConstraintLayout.LayoutParams params =
                        (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) videoCard.getLayoutParams();
                params.setMargins(margin, margin, margin, margin);
                params.matchConstraintPercentHeight = 0.55f; // 恢复原来的 55% 高度比例
                videoCard.setLayoutParams(params);

                if (videoCard instanceof com.google.android.material.card.MaterialCardView) {
                    ((com.google.android.material.card.MaterialCardView) videoCard).setRadius(20 * density); // 恢复圆角
                }
            }

            // 恢复显示系统 UI
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }
    }
}