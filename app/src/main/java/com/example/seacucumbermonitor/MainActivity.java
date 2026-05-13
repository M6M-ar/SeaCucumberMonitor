package com.example.seacucumbermonitor;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.card.MaterialCardView;

public class MainActivity extends AppCompatActivity {

    private ViewPager2 viewPagerBanner;

    // ⚠️ 注意：确保这里的图片名称与你 drawable 文件夹中的图片名称完全一致
    // 如果你存的名字是别的，请在这里修改
    private final int[] bannerImages = {
            R.drawable.banner_1,
            R.drawable.banner_2,
            R.drawable.banner_3
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. 初始化轮播图 (Banner)
        viewPagerBanner = findViewById(R.id.viewPagerBanner);
        BannerAdapter adapter = new BannerAdapter(bannerImages);
        viewPagerBanner.setAdapter(adapter);

        // 开启自动轮播 (3秒切换一次)
        setupAutoScroll();

        // 2. 初始化下方的四个功能卡片
        MaterialCardView cardAi = findViewById(R.id.cardAiMonitor);
        MaterialCardView cardUsb = findViewById(R.id.cardUsbPreview);
        MaterialCardView cardGallery = findViewById(R.id.cardGallery);
        MaterialCardView cardSettings = findViewById(R.id.cardSettings);

        // 3. 设置点击跳转事件

        // 点击 [AI 智能监测]（进入我们新设计的 YOLO 识别界面）
        cardAi.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AiMonitorActivity.class);
            startActivity(intent);
        });

        // 点击 [USB 预览]（进入你原有的纯净摄像头界面）
        cardUsb.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, UsbCameraActivity.class);
            startActivity(intent);
        });

        // 点击 [海参图库]
        cardGallery.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, GalleryActivity.class);
            startActivity(intent);
        });

        // 点击 [工程设置]（目前先弹窗提示）
        cardSettings.setOnClickListener(v -> {
            Toast.makeText(MainActivity.this, "参数设置功能开发中...", Toast.LENGTH_SHORT).show();
        });
    }

    // 轮播图自动滚动的核心逻辑
    private void setupAutoScroll() {
        viewPagerBanner.postDelayed(new Runnable() {
            @Override
            public void run() {
                int current = viewPagerBanner.getCurrentItem();
                // 循环切换到下一张
                viewPagerBanner.setCurrentItem((current + 1) % bannerImages.length, true);
                // 重新延时3秒执行
                viewPagerBanner.postDelayed(this, 3000);
            }
        }, 3000);
    }
}