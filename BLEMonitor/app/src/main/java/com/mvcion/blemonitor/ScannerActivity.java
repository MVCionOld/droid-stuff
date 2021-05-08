package com.mvcion.blemonitor;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;

import com.mvcion.blemonitor.common.PreferencesFacade;
import com.mvcion.blemonitor.common.ServiceUuis;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ScannerActivity extends Activity {

    private final String TAG = "ScannerActivity";
    private final String SCANNER_ITERATION_PATTERN = "Scanner iteration: {0}";
    private final String UNIQUE_DEVICES_TOTAL_PATTERN = "Unique devices total: {0}";
    private final String UPDATE_FREQUENCY_PATTERN = "Update frequency: {0}s";

    private long processingWindowNanos;
    private long reportDelayMillis;
    private int scannerMode;
    private int callbackType;
    private int matchMode;
    private int numOfMatches;

    private Queue<ScanResult> leDevicesStream = new ConcurrentLinkedQueue<>();

    private BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private BluetoothLeScanner bluetoothLeScanner;

    private final ScanCallback leScanCallback = new ScanCallback() {

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if (result != null) {
                Log.d(TAG, result.toString());
                leDevicesStream.add(result);
            } else {
                Log.w(TAG, "Nullable ScanResult.");
            }
        }
    };

    private void fetchScannerPreferences(Context context) {
        processingWindowNanos = PreferencesFacade.getProcessingWindowNanos(context);
        reportDelayMillis = PreferencesFacade.getReportDelayMillis(context);
        scannerMode = PreferencesFacade.getScannerMode(context);
        callbackType = PreferencesFacade.getCallbackType(context);
        matchMode = PreferencesFacade.getMatchMode(context);
        numOfMatches = PreferencesFacade.getNumOfMatches(context);
    }

    private Thread scanResultsProducer = new Thread(() -> {
        List<ScanFilter> scanFilters = new ArrayList<ScanFilter>(){{
                add(new ScanFilter
                        .Builder()
                        .setServiceUuid(ServiceUuis.getServiceUuid())
                        .build());
        }};
        ScanSettings scanSettings = new ScanSettings
                .Builder()
                .setScanMode(scannerMode)
                .setCallbackType(callbackType)
                .setMatchMode(matchMode)
                .setNumOfMatches(numOfMatches)
                .setReportDelay(reportDelayMillis)
                .build();
        bluetoothLeScanner.startScan(scanFilters, scanSettings, leScanCallback);
    });

    private Thread scanResultsConsumer = new Thread(new Runnable() {

        private int scannerIteration = 0;
        private Set<String> uniqueLeDevices = new HashSet<>();
        private volatile Set<String> nearbyDevices = new HashSet<>();

        private void consumeScanResults() {
            if (nearbyDevices != null) {
                uniqueLeDevices.addAll(nearbyDevices);
            }
            nearbyDevices = new HashSet<>();
            final long startTime = System.nanoTime();
            long currTime = System.nanoTime();
            while (currTime - startTime < processingWindowNanos) {
                if (leDevicesStream.size() > 0) {
                    ScanResult scanResult = leDevicesStream.remove();
                    nearbyDevices.add(scanResult.getDevice().toString());
                }
                currTime = System.nanoTime();
            }
        }

        @Override
        public void run() {
            while (true) {
                consumeScanResults();
                updateLeScannerUi(scannerIteration++, nearbyDevices.size(), uniqueLeDevices.size());
            }
        }
    });

    private void updateLeScannerUi(int scannerIteration, int devicesNearbyNum, int allUniqueDevicesNum) {
        TextView nearbyDevicesCounterTextView = findViewById(R.id.scanner__text_view__nearby_devices_counter);
        TextView scannerIterationTextView = findViewById(R.id.scanner__text_view__scanner_iteration);
        TextView uniqueDevicesTotalTextView = findViewById(R.id.scanner__text_view__unique_devices);
        TextView updateFrequencyTextView = findViewById(R.id.scanner__text_view__update_frequency);

        runOnUiThread(() -> {
            nearbyDevicesCounterTextView.setText(MessageFormat.format("{0}", devicesNearbyNum));
            scannerIterationTextView.setText(MessageFormat.format(
                    SCANNER_ITERATION_PATTERN, scannerIteration
            ));
            uniqueDevicesTotalTextView.setText(MessageFormat.format(
                    UNIQUE_DEVICES_TOTAL_PATTERN, allUniqueDevicesNum
            ));
            updateFrequencyTextView.setText(MessageFormat.format(
                    UPDATE_FREQUENCY_PATTERN, processingWindowNanos / 1_000_000_000.0
            ));
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
                fetchScannerPreferences(this);
                scanResultsProducer.start();
                scanResultsConsumer.start();
            } else {
                Log.e(TAG, "Scanner is not found.");
            }
        }

        setContentView(R.layout.activity_receiver);
    }
}