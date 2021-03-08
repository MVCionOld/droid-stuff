package com.mvcion.blemonitor;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.mvcion.blemonitor.common.ServiceUuis;

public class SenderActivity extends Activity {

    private final String TAG = "SenderActivity";

    private BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private BluetoothLeAdvertiser bluetoothLeAdvertiser;

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

    private Thread scanAdvertiser = new Thread(new Runnable() {
        @Override
        public void run() {
            bluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();

            if (bluetoothLeAdvertiser != null) {
                Log.v(TAG, "Scanner is found.");
            } else {
                Log.e(TAG, "Scanner is not found.");
                return;
            }

            AdvertiseData.Builder advertiseDataBuilder = new AdvertiseData.Builder();
            AdvertiseSettings.Builder advertiseSettingsBuilder = new AdvertiseSettings.Builder();

            ParcelUuid serviceUuid = ServiceUuis.getServiceUuid();
            advertiseDataBuilder.addServiceUuid(serviceUuid);
            advertiseSettingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
            advertiseSettingsBuilder.setConnectable(true);

            AdvertiseData advertiseData = advertiseDataBuilder.build();
            bluetoothLeAdvertiser.startAdvertising(
                    /*settings = */advertiseSettingsBuilder.build(),
                    /*advertiseData = */advertiseData,
                    /*scanResponse = */null,
                    /*callback = */leAdvertiseCallback);
        }
    });

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
                Log.v(TAG, "Enabling Bluetooth Adapter.");
                bluetoothAdapter.enable();
            }
            scanAdvertiser.start();
        }

        setContentView(R.layout.activity_sender);
    }
}