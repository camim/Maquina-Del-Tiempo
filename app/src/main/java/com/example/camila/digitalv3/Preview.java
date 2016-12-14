package com.example.camila.digitalv3;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Preview extends AppCompatActivity {

    private ImageView mCameraImage;
    String strname =null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);
        Intent intent = getIntent();
        strname = intent.getStringExtra(CameraActivity.PHOTOPATH);
        Bitmap mybitmap = BitmapFactory.decodeFile(strname);
        mCameraImage = (ImageView) findViewById(R.id.imageView);
        mCameraImage.setImageBitmap(mybitmap);
    }

}

