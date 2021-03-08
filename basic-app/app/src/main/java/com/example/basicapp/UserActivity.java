package com.example.basicapp;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.text.MessageFormat;

public class UserActivity extends AppCompatActivity {

    private static final String TAG = "UserActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);

        final Button button = findViewById(R.id.button);

        EditText editText = findViewById(R.id.edit_text);
        TextView textView = findViewById(R.id.text_view);
        textView.setVisibility(View.INVISIBLE);

        button.setOnClickListener(v -> {
            String input = editText.getText().toString();
            textView.setText(MessageFormat.format("Hello, {0}!", input));
            textView.setVisibility(View.VISIBLE);
        });

        final Button goButton = findViewById(R.id.button_go);
        goButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, e.toString());
            }
        });
    }
}
