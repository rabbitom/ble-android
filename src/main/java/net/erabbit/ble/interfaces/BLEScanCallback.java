package net.erabbit.ble.interfaces;

import net.erabbit.ble.BLEDevice;

import java.util.Map;

/**
 * Created by ziv on 2017/4/21.
 */

public interface BLEScanCallback {

    public final int ERROR_NO_BLE = 201;
    public final int ERROR_BLUETOOTH_DISABLE = 202;
    public final int ERROR_NO_BLUETOOTH_PERMISSION = 203;

    public static final String BLE_SCAN_STARTED = "ScanStarted";
    public static final String BLE_SCAN_STOPPED = "ScanStopped";
    public static final String BLE_FOUND_DEVICE = "FoundDevice";
    public static final String BLE_ADVERTISEMENT_UPDATED = "AdvertisementUpdated";
    public static final String BLE_RSSI_UPDATED = "RSSIUpdated";
    public static final String BLE_SCAN_ERROR = "ScanError";

    void onScanError(int errId, String error);

    void onScanStarted();

    void onScanStopped();

    void onFoundDevice(String deviceId);

    void onAdvertisementUpdated(String deviceID,  Map<Integer, byte[]> data);

    void onRSSIUpdated(String deviceID, int rssi);
}
