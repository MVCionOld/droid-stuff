package com.mvcion.blemonitor;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;


public class ReceiverActivity extends Activity {

    private final String TAG = "ReceiverActivity";
    private final String RECEIVER_ITERATION_PATTERN = "Receiver iteration: {0}";
    private final String UNIQUE_DEVICES_TOTAL_PATTERN = "Unique devices total: {0}";
    private final String UPDATE_FREQUENCY_PATTERN = "Update frequency: {0}s";

    private final long CONSUMING_PERIOD_NANOS = 5_000_000_000L;
    private int receiverIteration = 0;
    private Set<String> allUniqueDevices = new HashSet<>();
    private Queue<ScanResult> queue = new ConcurrentLinkedQueue<>();

    private BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private BluetoothLeScanner bluetoothLeScanner;


    private final ScanCallback leScanCallback = new ScanCallback() {

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if (result != null) {
                Log.d(TAG, String.valueOf(result.getRssi()));
                queue.add(result);
            } else {
                Log.w(TAG, "Nullable ScanResult.");
            }
        }
    };

    private Thread scanResultsProducer = new Thread(() -> bluetoothLeScanner.startScan(leScanCallback));

    private Thread scanResultsConsumer = new Thread(new Runnable() {

        private volatile Set<String> set = new HashSet<>();

        private void consumeScanResults() {
            if (set != null) {
                allUniqueDevices.addAll(set);
            }
            set = new HashSet<>();
            final long startTime = System.nanoTime();
            long currTime = System.nanoTime();
            while (currTime - startTime < CONSUMING_PERIOD_NANOS) {
                if (queue.size() > 0) {
                    ScanResult scanResult = queue.remove();
                    if (scanResult.getRssi() > -70) {
                        set.add(scanResult.getDevice().toString());
                    }
                }
                currTime = System.nanoTime();
            }
        }

        @Override
        public void run() {
            while (true) {
                consumeScanResults();
                updateLeReceiverUi(set.size());
                receiverIteration++;
            }
        }
    });

    private void updateLeReceiverUi(int newCounterValue) {
        TextView counterTextView = findViewById(R.id.receiver__text_view__counter);
        TextView receiverIterationTextView = findViewById(R.id.receiver__text_view__receiver_iteration);
        TextView uniqueDevicesTotalTextView = findViewById(R.id.receiver__text_view__unique_devices);
        TextView updateFrequencyTextView = findViewById(R.id.receiver__text_view__update_frequency);

        runOnUiThread(() -> {
            counterTextView.setText(MessageFormat.format("{0}", newCounterValue));
            receiverIterationTextView.setText(MessageFormat.format(
                    RECEIVER_ITERATION_PATTERN, receiverIteration));
            uniqueDevicesTotalTextView.setText(MessageFormat.format(
                    UNIQUE_DEVICES_TOTAL_PATTERN, allUniqueDevices.size()));
            updateFrequencyTextView.setText(MessageFormat.format(
                    UPDATE_FREQUENCY_PATTERN, CONSUMING_PERIOD_NANOS / 1_000_000_000.0));
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receiver);

        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        }, 1001);

        if (bluetoothAdapter == null) {
            Log.e(TAG, "BluetoothAdapter is not found.");
        } else {
            if (!bluetoothAdapter.isEnabled()) {
                bluetoothAdapter.enable();
            }
            bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
            if (bluetoothLeScanner != null) {
                Log.v(TAG, "Scanner is found.");
                scanResultsProducer.start();
                scanResultsConsumer.start();
            } else {
                Log.e(TAG, "Scanner is not found.");
            }
        }
    }
}