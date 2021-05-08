package com.mvcion.blemonitor;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.mvcion.blemonitor.common.PreferencesFacade;
import com.mvcion.blemonitor.common.ServiceUuis;

public class AdvertiserActivity extends Activity {

    private final String TAG = "AdvertiserActivity";

    private int advertiserMode;
    private int advertiserTxPower;
    private boolean isConnectable;

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

    private void fetchAdvertiserPreferences(Context context) {
        advertiserMode = PreferencesFacade.getAdvertiserMode(context);
        advertiserTxPower = PreferencesFacade.getAdvertiserTxPower(context);
        isConnectable = PreferencesFacade.getAdvertiserIsConnectable(context);
    }

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

            ParcelUuid serviceUuid = ServiceUuis.getServiceUuid();
            AdvertiseData.Builder advertiseDataBuilder = new AdvertiseData
                    .Builder()
                    .addServiceUuid(serviceUuid);

            AdvertiseSettings.Builder advertiseSettingsBuilder = new AdvertiseSettings
                    .Builder()
                    .setAdvertiseMode(advertiserMode)
                    .setTxPowerLevel(advertiserTxPower)
                    .setConnectable(isConnectable);

            bluetoothLeAdvertiser.startAdvertising(
                    /*settings = */advertiseSettingsBuilder.build(),
                    /*advertiseData = */advertiseDataBuilder.build(),
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

            fetchAdvertiserPreferences(this);
            scanAdvertiser.start();
        }

        setContentView(R.layout.activity_sender);
    }
}