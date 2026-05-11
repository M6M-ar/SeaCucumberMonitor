package com.example.seacucumbermonitor;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class GalleryActivity extends AppCompatActivity {

    private static final int REQUEST_PICK_IMAGE = 2001;
    private ImageView imgSelected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        imgSelected = findViewById(R.id.imgSelected);

        MaterialButton btnOpenGallery = findViewById(R.id.btnOpenGallery);
        MaterialButton btnBack = findViewById(R.id.btnBackFromGallery);

        btnOpenGallery.setOnClickListener(v -> openGallery());
        btnBack.setOnClickListener(v -> finish());
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            imgSelected.setImageURI(imageUri);
        }
    }
}