package net.erabbit.ble.interfaces;

import java.io.Serializable;

/**
 * Created by ziv on 2017/4/21.
 */

public interface DeviceStateCallback {

    public static final String DEVICE_CONNECTED = "DeviceConnected";
    public static final String DEVICE_READY = "DeviceReady";
    public static final String DEVICE_MISMATCH = "DeviceMismatch";
    public static final String DEVICE_DISCONNECTED = "DeviceDisconnected";
    public static final String DEVICE_RECEIVED_DATA = "DeviceReceivedData";
    public static final String DEVICE_VALUE_UPDATED = "DeviceValueChanged";
    public static final String DEVICE_ERROR = "DeviceError";
    public static final String DEVICE_RSSI_UPDATED = "DeviceRSSIUpdated";

    public static final int DEVICE_ERROR_DISCOVER_SERVICE_FAILED = 1;

    void onDeviceConnected(String deviceID);//在建立连接后调用

    void onDeviceReady(String deviceID);//发现服务、特征完成后调用

    void onDeviceMismatch(String deviceID);//发现的服务、特征与设备定义不匹配

    void onDeviceDisconnected(String deviceID);//在连接断开后调用

    void onDeviceReceivedData(String deviceID, String name, byte[] data);//在收到数据时调用

    void onDeviceValueUpdated(String deviceID, int key, String name, Serializable value);//在设备解析接收到的数据完成后调用

    void onDeviceError(String deviceID, int errId, String error);//在设备操作出错时回调，比如发现服务和特征失败，发送数据失败等

    void onDeviceRSSIUpdated(String deviceID, int rssi);//在读取到设备的RSSI值时回调
}

