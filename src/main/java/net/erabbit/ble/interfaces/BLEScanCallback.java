package net.erabbit.ble.interfaces;

import java.util.Map;

/**
 * Created by ziv on 2017/4/21.
 */

public interface BLEScanCallback {

    public final int ERROR_NO_BLE = 201;
    public final int ERROR_BLUETOOTH_DISABLE = 202;
    public final int ERROR_NO_BLUETOOTH_PERMISSION = 203;

    void onScanError(int errId, String error);

    void onScanStarted();

    void onScanTimeout();

    void onFoundDevice(String deviceID, int rssi, Map<Integer, byte[]> data, String deviceType);

    void onAdvertisementUpdated(String deviceID,  Map<Integer, byte[]> data);

    void onRSSIUpdated(String deviceID, int rssi);
}
