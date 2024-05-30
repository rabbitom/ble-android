package net.erabbit.ble;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import net.erabbit.ble.interfaces.BLEScanCallback;

import java.util.Map;

/**
 * Created by ziv on 2017/4/20.
 */

public class BLEScanReceiver extends BroadcastReceiver implements BLEScanCallback {

    public BLEScanReceiver() {
    }

    public BLEScanReceiver(Context context) {
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(context);
        registerWithBroadcastManager(lbm);
    }

    public void registerWithBroadcastManager(LocalBroadcastManager lbm) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BLE_SCAN_ERROR);
        intentFilter.addAction(BLE_SCAN_STARTED);
        intentFilter.addAction(BLE_SCAN_STOPPED);
        intentFilter.addAction(BLE_FOUND_DEVICE);
        intentFilter.addAction(BLE_ADVERTISEMENT_UPDATED);
        intentFilter.addAction(BLE_RSSI_UPDATED);
        lbm.registerReceiver(this, intentFilter);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        int errId = intent.getIntExtra("errId", 0);
        String error = intent.getStringExtra("error");
        String deviceID = intent.getStringExtra("deviceId");
        int rssi = intent.getIntExtra("rssi", 0);
        Map<Integer, byte[]> data = (Map<Integer, byte[]>) intent.getSerializableExtra("data");
        switch (intent.getAction()) {
            case BLE_SCAN_ERROR:
                onScanError(errId, error);
                break;
            case BLE_SCAN_STARTED:
                onScanStarted();
                break;
            case BLE_SCAN_STOPPED:
                onScanStopped();
                break;
            case BLE_FOUND_DEVICE:
                onFoundDevice(deviceID);
                break;
            case BLE_ADVERTISEMENT_UPDATED:
                onAdvertisementUpdated(deviceID, data);
                break;
            case BLE_RSSI_UPDATED:
                onRSSIUpdated(deviceID, rssi);
                break;
        }
    }

    @Override
    public void onScanError(int errId, String error) {

    }

    @Override
    public void onScanStarted() {

    }

    @Override
    public void onScanStopped() {

    }

    @Override
    public void onFoundDevice(String deviceID) {

    }

    @Override
    public void onAdvertisementUpdated(String deviceID,Map<Integer, byte[]> data) {

    }

    @Override
    public void onRSSIUpdated(String deviceID, int rssi) {

    }
}
