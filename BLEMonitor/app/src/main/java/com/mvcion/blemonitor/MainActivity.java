package com.mvcion.blemonitor;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

public class MainActivity extends Activity {

    final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageView imageView = findViewById(R.id.main__image_view__bluetooth);
        if (imageView != null) {
            imageView.setOnClickListener(v -> {
                Intent intent = new Intent(this, ModeSelectorActivity.class);
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Log.e(TAG, e.toString());
                }
            });
        } else {
            Log.e(TAG, "No start image.");
        }
    }
}