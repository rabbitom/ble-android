package net.erabbit.ble;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import net.erabbit.ble.interfaces.DeviceStateCallback;
import java.io.Serializable;

/**
 * Created by ziv on 2017/4/21.
 */

public class DeviceStateReceiver extends BroadcastReceiver implements DeviceStateCallback {

    public DeviceStateReceiver() {
    }

    public DeviceStateReceiver(Context context) {
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(context);
        registerReceiver(lbm);
    }

    public void registerReceiver(LocalBroadcastManager lbm) {

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(DEVICE_CONNECTED);
        intentFilter.addAction(DEVICE_READY);
        intentFilter.addAction(DEVICE_MISMATCH);
        intentFilter.addAction(DEVICE_DISCONNECTED);
        intentFilter.addAction(DEVICE_RECEIVED_DATA);
        intentFilter.addAction(DEVICE_VALUE_UPDATED);
        intentFilter.addAction(DEVICE_ERROR);
        intentFilter.addAction(DEVICE_RSSI_UPDATED);
        lbm.registerReceiver(this, intentFilter);
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        String deviceID = intent.getStringExtra("deviceId");
        String name = intent.getStringExtra("name");
        byte[] data = intent.getByteArrayExtra("data");
        int key = intent.getIntExtra("key", 0);
        Serializable value = intent.getSerializableExtra("value");
        int errId = intent.getIntExtra("errId", 0);
        String error = intent.getStringExtra("error");
        int rssi = intent.getIntExtra("rssi", 0);

        switch (intent.getAction()) {
            case DEVICE_CONNECTED:
                onDeviceConnected(deviceID);
                break;
            case DEVICE_READY:
                onDeviceReady(deviceID);
                break;
            case DEVICE_MISMATCH:
                onDeviceMismatch(deviceID);
                break;
            case DEVICE_DISCONNECTED:
                onDeviceDisconnected(deviceID);
                break;
            case DEVICE_RECEIVED_DATA:
                onDeviceReceivedData(deviceID, name, data);
                break;
            case DEVICE_VALUE_UPDATED:
                onDeviceValueUpdated(deviceID, key, name, value);
                break;
            case DEVICE_ERROR:
                onDeviceError(deviceID, errId, error);
                break;
            case DEVICE_RSSI_UPDATED:
                onDeviceRSSIUpdated(deviceID, rssi);
                break;
        }
    }

    @Override
    public void onDeviceConnected(String deviceID) {

    }

    @Override
    public void onDeviceReady(String deviceID) {

    }

    @Override
    public void onDeviceMismatch(String deviceID) {

    }

    @Override
    public void onDeviceDisconnected(String deviceID) {

    }

    @Override
    public void onDeviceReceivedData(String deviceID, String name, byte[] data) {

    }

    @Override
    public void onDeviceValueUpdated(String deviceID, int key, String name, Serializable value) {

    }


    @Override
    public void onDeviceError(String deviceID, int errId, String error) {

    }

    @Override
    public void onDeviceRSSIUpdated(String deviceID, int rssi) {

    }

}
