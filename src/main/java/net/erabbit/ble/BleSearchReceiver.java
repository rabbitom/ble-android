package net.erabbit.ble;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

import net.erabbit.ble.interfaces.BLESearchCallback;

import java.util.Map;

/**
 * Created by ziv on 2017/4/20.
 */

public class BleSearchReceiver extends BroadcastReceiver implements BLESearchCallback {

    public static final String BLE_SEARCH_STARTED = "SearchStarted";
    public static final String BLE_SEARCH_TIME_OUT = "SearchTimeOut";
    public static final String BLE_FOUND_DEVICE = "FoundDevice";
    public static final String BLE_ADVERTISEMENT_UPDATED = "AdvertisementUpdated";
    public static final String BLE_RSSI_UPDATED = "RSSIUpdated";
    public static final String BLE_SEARCH_ERROR = "SearchError";

    public BleSearchReceiver() {
    }

    public BleSearchReceiver(Context context) {
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(context);
        registerReceiver(lbm);
    }

    public void registerReceiver(LocalBroadcastManager lbm) {

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BLE_SEARCH_ERROR);
        intentFilter.addAction(BLE_SEARCH_STARTED);
        intentFilter.addAction(BLE_SEARCH_TIME_OUT);
        intentFilter.addAction(BLE_FOUND_DEVICE);
        intentFilter.addAction(BLE_ADVERTISEMENT_UPDATED);
        intentFilter.addAction(BLE_RSSI_UPDATED);
        lbm.registerReceiver(this, intentFilter);
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        int errId = intent.getIntExtra("errId", 0);
        String error = intent.getStringExtra("error");
        String deviceID = intent.getStringExtra("deviceID");
        int rssi = intent.getIntExtra("rssi", 0);
        Map<Integer, byte[]> data = (Map<Integer, byte[]>) intent.getSerializableExtra("data");
        String deviceType = intent.getStringExtra("deviceType");
        switch (intent.getAction()) {
            case BLE_SEARCH_ERROR:

                onSearchError(errId, error);
                break;
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

                onAdvertisementUpdated(deviceID,data);
                break;
            case BLE_RSSI_UPDATED:
                onRSSIUpdated(deviceID, rssi);
                break;
        }
    }

    @Override
    public void onSearchError(int errId, String error) {

    }

    @Override
    public void onSearchStarted() {

    }

    @Override
    public void onSearchTimeOut() {

    }

    @Override
    public void onFoundDevice(String deviceID, int rssi, Map<Integer, byte[]> data, String deviceType) {

    }

    @Override
    public void onAdvertisementUpdated(String deviceID,Map<Integer, byte[]> data) {

    }

    @Override
    public void onRSSIUpdated(String deviceID, int rssi) {

    }
}
