package com.mvcion.blemonitor;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.mvcion.blemonitor.common.ServiceUuis;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ContactTracingActivity extends Activity {

    private final String TAG = "ContactTracingActivity";
    private BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    private Thread activityScanner = new Thread(new Runnable() {

        private final long CONSUMING_PERIOD_NANOS = 5_000_000_000L;
        private int scannerIteration = 0;
        private Set<String> allUniqueDevices = new HashSet<>();
        private Queue<ScanResult> scanResultsQueue = new ConcurrentLinkedQueue<>();
        private BluetoothLeScanner bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();;

        private final ScanCallback leScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                if (result != null) {
                    Log.d(TAG, result.toString());
                    scanResultsQueue.add(result);
                } else {
                    Log.w(TAG, "Nullable ScanResult.");
                }
            }
        };

        private Thread scanResultsProducer = new Thread(() -> {
            List<ScanFilter> scanFilters = new ArrayList<ScanFilter>(){{
                add(new ScanFilter
                        .Builder()
                        .setServiceUuid(ServiceUuis.getServiceUuid())
                        .build());
            }};
            ScanSettings scanSettings = new ScanSettings
                    .Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                    .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                    .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
                    .setReportDelay(0L)
                    .build();
            bluetoothLeScanner.startScan(scanFilters, scanSettings, leScanCallback);
        });

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
                    if (scanResultsQueue.size() > 0) {
                        ScanResult scanResult = scanResultsQueue.remove();
                        set.add(scanResult.getDevice().toString());
                    }
                    currTime = System.nanoTime();
                }
            }

            private void dumpLog() {
                Log.d(TAG, MessageFormat.format(
                        "Receiver iteration: {0}\n"
                        + "Total unique devices: {1}\n"
                        + "Nearby devices: {2}\n"
                        + "Frequency, sec: {3}\n",
                        scannerIteration,
                        allUniqueDevices.size(),
                        set.size(),
                        CONSUMING_PERIOD_NANOS / 1_000_000_000.0
                ));
            }

            @Override
            public void run() {
                while (true) {
                    consumeScanResults();
                    dumpLog();
                    scannerIteration++;
                }
            }
        });

        @Override
        public void run() {
            if (bluetoothLeScanner != null) {
                Log.v(TAG, "BluetoothLeScanner is found.");
                scanResultsProducer.start();
                scanResultsConsumer.start();
            } else {
                Log.e(TAG, "BluetoothLeScanner is not found.");
            }
        }
    });

    private Thread activityAdvertiser = new Thread(new Runnable() {

        private BluetoothLeAdvertiser bluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();

        private final AdvertiseCallback leAdvertiseCallback = new AdvertiseCallback() {

            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                super.onStartSuccess(settingsInEffect);
                Log.v(TAG, settingsInEffect.toString());
            }

            @Override
            public void onStartFailure(int errorCode) {
                super.onStartFailure(errorCode);
                Log.e(TAG, String.valueOf(errorCode));
            }
        };

        @Override
        public void run() {
            if (bluetoothLeAdvertiser != null) {
                Log.v(TAG, "BluetoothLeScanner is found.");
            } else {
                Log.e(TAG, "BluetoothLeScanner is not found.");
                return;
            }

            ParcelUuid serviceUuid = ServiceUuis.getServiceUuid();
            AdvertiseData.Builder advertiseDataBuilder = new AdvertiseData
                    .Builder()
                    .addServiceUuid(serviceUuid);

            AdvertiseSettings.Builder advertiseSettingsBuilder = new AdvertiseSettings
                    .Builder()
                    .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                    .setConnectable(true);

            bluetoothLeAdvertiser.startAdvertising(
                    /*settings = */advertiseSettingsBuilder.build(),
                    /*advertiseData = */advertiseDataBuilder.build(),
                    /*scanResponse = */null,
                    /*callback = */leAdvertiseCallback);
        }
    });

    private void setUpTracing() {
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
            activityAdvertiser.start();
            activityScanner.start();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setUpTracing();
        setContentView(R.layout.activity_contact_tracing);
    }
}