package com.tails.workdaytime;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class ImageActivity extends AppCompatActivity {

    ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);

        imageView = findViewById(R.id.imageViewInImageActivity);

        Intent intent = getIntent();
        Bitmap bmImg = BitmapFactory.decodeFile(intent.getStringExtra("path"));
        imageView.setImageBitmap(bmImg);
    }

}
