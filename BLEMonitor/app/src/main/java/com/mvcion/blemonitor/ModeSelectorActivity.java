package com.mvcion.blemonitor;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class ModeSelectorActivity extends Activity {

    final String TAG = "ModeSelectorActivity";

    private void setTextViewOnClickListener(TextView textView, Intent intent) {
        if (textView == null) {
            Log.e(TAG, "No TextView");
        } else if (intent == null) {
            Log.e(TAG, "No Intent");
        } else {
            textView.setOnClickListener(v -> {
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Log.e(TAG, e.toString());
                }
            });
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mode_selector);

        setTextViewOnClickListener(
                /*textView = */findViewById(R.id.mode_selector__text_view__receiver),
                /*intent = */new Intent(this, ReceiverActivity.class)
        );
        setTextViewOnClickListener(
                /*textView = */findViewById(R.id.mode_selector__text_view__sender),
                /*intent = */ new Intent(this, SenderActivity.class)
        );
    }
}
