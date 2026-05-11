package com.example.seacucumbermonitor;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;

public class MainActivity extends AppCompatActivity {

    private ViewPager2 bannerPager;
    private LinearLayout indicatorLayout;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final int[] bannerImages = {
            R.drawable.banner_1,
            R.drawable.banner_2,
            R.drawable.banner_3
    };

    private final String[] bannerTitles = {
            "海参养殖环境观察",
            "USB 摄像头实时监测",
            "生长状态图像记录"
    };

    private final Runnable autoScrollRunnable = new Runnable() {
        @Override
        public void run() {
            if (bannerPager != null && bannerImages.length > 0) {
                int next = (bannerPager.getCurrentItem() + 1) % bannerImages.length;
                bannerPager.setCurrentItem(next, true);
                handler.postDelayed(this, 3000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bannerPager = findViewById(R.id.bannerPager);
        indicatorLayout = findViewById(R.id.indicatorLayout);

        MaterialButton btnUsbCamera = findViewById(R.id.btnUsbCamera);
        MaterialButton btnGallery = findViewById(R.id.btnGallery);

        bannerPager.setAdapter(new BannerAdapter());
        bannerPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateIndicator(position);
            }
        });

        createIndicator();
        updateIndicator(0);

        btnUsbCamera.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, UsbCameraActivity.class);
            startActivity(intent);
        });

        btnGallery.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, GalleryActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.postDelayed(autoScrollRunnable, 3000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(autoScrollRunnable);
    }

    private void createIndicator() {
        indicatorLayout.removeAllViews();

        for (int i = 0; i < bannerImages.length; i++) {
            TextView dot = new TextView(this);
            dot.setText("●");
            dot.setTextSize(18);
            dot.setPadding(6, 0, 6, 0);
            indicatorLayout.addView(dot);
        }
    }

    private void updateIndicator(int selectedPosition) {
        for (int i = 0; i < indicatorLayout.getChildCount(); i++) {
            TextView dot = (TextView) indicatorLayout.getChildAt(i);
            if (i == selectedPosition) {
                dot.setTextColor(Color.WHITE);
            } else {
                dot.setTextColor(Color.parseColor("#80FFFFFF"));
            }
        }
    }

    private class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.BannerViewHolder> {

        @Override
        public BannerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            FrameLayout root = new FrameLayout(parent.getContext());
            root.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));

            ImageView imageView = new ImageView(parent.getContext());
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            ));
            root.addView(imageView);

            TextView title = new TextView(parent.getContext());
            title.setTextColor(Color.WHITE);
            title.setTextSize(22);
            title.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            title.setGravity(Gravity.CENTER);
            title.setTypeface(null, android.graphics.Typeface.BOLD);
            title.setBackgroundColor(Color.parseColor("#33000000"));

            FrameLayout.LayoutParams titleParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            );
            root.addView(title, titleParams);

            return new BannerViewHolder(root, imageView, title);
        }

        @Override
        public void onBindViewHolder(BannerViewHolder holder, int position) {
            holder.imageView.setImageResource(bannerImages[position]);
        }

        @Override
        public int getItemCount() {
            return bannerImages.length;
        }

        class BannerViewHolder extends RecyclerView.ViewHolder {

            ImageView imageView;
            TextView titleView;

            BannerViewHolder(View itemView, ImageView imageView, TextView titleView) {
                super(itemView);
                this.imageView = imageView;
                this.titleView = titleView;
            }
        }
    }
}