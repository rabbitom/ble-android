package net.erabbit.interfaces;

/**
 * Created by ziv on 2017/4/21.
 */

public interface BLESearchCallback {
    void onSearchStarted();

    void onSearchTimeOut();

    void onFoundDevice(String deviceID, int rssi, byte[] data, String deviceType);

    void onAdvertisementUpdated();

    void onRSSIUpdated(String deviceID, int rssi);
}
