package net.erabbit;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

import net.erabbit.interfaces.BLESearchCallback;

/**
 * Created by ziv on 2017/4/20.
 */

public class BleSearchReceiver extends BroadcastReceiver implements BLESearchCallback {

    public static final String BLE_SEARCH_STARTED = "SearchStarted";
    public static final String BLE_SEARCH_TIME_OUT = "SearchTimeOut";
    public static final String BLE_FOUND_DEVICE = "FoundDevice";
    public static final String BLE_ADVERTISEMENT_UPDATED = "AdvertisementUpdated";
    public static final String BLE_RSSI_UPDATED = "RSSIUpdated";


    public void registerReceiver(LocalBroadcastManager lbm) {

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BLE_SEARCH_STARTED);
        intentFilter.addAction(BLE_SEARCH_TIME_OUT);
        intentFilter.addAction(BLE_FOUND_DEVICE);
        intentFilter.addAction(BLE_ADVERTISEMENT_UPDATED);
        intentFilter.addAction(BLE_RSSI_UPDATED);
        lbm.registerReceiver(this, intentFilter);
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        String deviceID = intent.getStringExtra("deviceID");
        int rssi = intent.getIntExtra("rssi", 0);
        byte[] data = intent.getByteArrayExtra("data");
        String deviceType = intent.getStringExtra("deviceType");
        switch (intent.getAction()) {
            case BLE_SEARCH_STARTED:

                onSearchStarted();
                break;
            case BLE_SEARCH_TIME_OUT:

                onSearchTimeOut();
                break;
            case BLE_FOUND_DEVICE:
                onFoundDevice(deviceID, rssi, data, deviceType);
                break;
            case BLE_ADVERTISEMENT_UPDATED:

                onAdvertisementUpdated();
                break;
            case BLE_RSSI_UPDATED:
                onRSSIUpdated(deviceID, rssi);
                break;
        }
    }

    @Override
    public void onSearchStarted() {

    }

    @Override
    public void onSearchTimeOut() {

    }

    @Override
    public void onFoundDevice(String deviceID, int rssi, byte[] data, String deviceType) {

    }

    @Override
    public void onAdvertisementUpdated() {

    }

    @Override
    public void onRSSIUpdated(String deviceID, int rssi) {

    }
}
