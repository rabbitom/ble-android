package net.erabbit.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;

import net.erabbit.ble.interfaces.DeviceStateCallback;
import net.erabbit.ble.utils.BLEUtility;
import net.erabbit.ble.utils.LogUtil;
import net.erabbit.csl.CSL;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;

/**
 * Created by ziv on 2017/4/18.
 */

public class BLEDevice implements DeviceStateCallback, Serializable {

    private static final String TAG = "[BLE]";

    protected static JSONObject metadata;

    public static JSONObject getMetadata() {
        return metadata;
    }

    private String deviceName;//设备名称，默认使用广播名或设备名，可以修改
    private int deviceRSSI;

    private LocalBroadcastManager lbm;

    private BluetoothDevice nativeDevice;//系统原生的蓝牙设备对象
    private BluetoothGatt btGatt;

    private Map<String, BluetoothGattCharacteristic> characteristicsByName = new HashMap<>();
    private Map<UUID, String> characteristicNamesByUUID = new HashMap<>();

    public String getDeviceId() {
        return nativeDevice.getAddress();
    }

    public BLEDevice(ScanResult scanResult) {
        nativeDevice = scanResult.getDevice();
        updateStatus(scanResult);
        lbm = LocalBroadcastManager.getInstance(BLEManager.sharedInstance().getContext());
    }

    public void updateStatus(ScanResult scanResult) {
        try {
            ScanRecord scanRecord = scanResult.getScanRecord();
            if(scanRecord != null)
                deviceName = scanRecord.getDeviceName();
            deviceRSSI = scanResult.getRssi();
        }
        catch(SecurityException exception) {
            LogUtil.e(TAG, "parse scan result failed: " + exception.getMessage());
        }
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public boolean getConnected() {
        BluetoothManager btManager = (BluetoothManager) BLEManager.sharedInstance().getContext().getSystemService(Context.BLUETOOTH_SERVICE);
        int connectionState = BluetoothGatt.STATE_DISCONNECTED;
        try {
            connectionState = btManager.getConnectionState(nativeDevice, BluetoothProfile.GATT);
        }
        catch(SecurityException exception) {
            LogUtil.e(TAG, exception.getMessage());
        }
        return (connectionState == BluetoothGatt.STATE_CONNECTED);
    }

    /**
     * 建立连接
     */
    public void connect() {
        if (nativeDevice != null) {
            LogUtil.i(TAG, "connect device: " + getDeviceId());
            try {
                if (btGatt == null) {
                    if (mGattCallback == null)
                        mGattCallback = getGattCallback();
                    btGatt = nativeDevice.connectGatt(BLEManager.sharedInstance().getContext(), false, mGattCallback);
                } else
                    btGatt.connect();
            }
            catch(SecurityException exception) {
                LogUtil.e(TAG, exception.getMessage());
            }
        }
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        if (btGatt != null) {
            try {
                btGatt.disconnect();
                btGatt.close();
            }
            catch(SecurityException exception) {
                LogUtil.e(TAG, exception.getMessage());
            }
            btGatt = null;
        }
    }

    /**
     * 发送数据
     * @param name 数据点
     * @param data 数据内容
     */
    public void writeData(String name, byte[] data) {
        BluetoothGattCharacteristic characteristic = characteristicsByName.get(name);
        if (btGatt != null && characteristic != null) {
            Log.i(TAG, "write data, characteristic=" + name + ", data=(" + data.length + ")0x" + CSL.formatHexString(data));
            BluetoothGattOperation operation = new BluetoothGattOperation(
                    BluetoothGattOperation.WRITE_CHARACTERISTIC,
                    btGatt,
                    characteristic,
                    data);
            addOperation(operation);
        }
    }

    /**
     * 读取数据
     *
     * @param name 数据点
     */
    public void readData(String name) {
        BluetoothGattCharacteristic characteristic = characteristicsByName.get(name);
        if (btGatt != null && characteristic != null) {
            Log.i(TAG, "read data, characteristic=" + name);
            BluetoothGattOperation operation = new BluetoothGattOperation(
                    BluetoothGattOperation.READ_CHARACTERISTIC,
                    btGatt,
                    characteristic,
                    null);
            addOperation(operation);
        }
    }

    /**
     * 开始接收数据（通知）
     *
     * @param name 数据点
     */
    public void startNotification(String name) {
        BluetoothGattCharacteristic characteristic = characteristicsByName.get(name);
        if (btGatt != null && characteristic != null) {
            Log.i(TAG, "start notification: characteristic=" + name);
            BluetoothGattOperation operation = new BluetoothGattOperation(
                    BluetoothGattOperation.ENABLE_NOTIFICATION,
                    btGatt,
                    characteristic,
                    null);
            addOperation(operation);
        }
    }

    /**
     * 停止接收数据（通知）
     *
     * @param name 数据点
     */
    public void stopNotification(String name) {
        BluetoothGattCharacteristic characteristic = characteristicsByName.get(name);
        if (btGatt != null && characteristic != null) {
            Log.i(TAG, "stop notification: characteristic=" + name);
            BluetoothGattOperation operation = new BluetoothGattOperation(
                    BluetoothGattOperation.DISABLE_NOTIFICATION,
                    btGatt,
                    characteristic,
                    null);
            addOperation(operation);
        }
    }

    /**
     * 读取设备信号强度
     */
    public void readRSSI() {
        if(btGatt != null)
            try {
                btGatt.readRemoteRssi();
            }
            catch(SecurityException exception) {
                LogUtil.e(TAG, "read rssi failed: " + exception.getMessage());
            }
    }

    public int getDeviceRSSI() {
        return deviceRSSI;
    }

    //GATT操作队列
    private transient Queue<BluetoothGattOperation> gattOperationQueue;

    private void addOperation(BluetoothGattOperation operation) {
        if (gattOperationQueue == null)
            gattOperationQueue = new LinkedList<>();
        gattOperationQueue.add(operation);
        if (gattOperationQueue.size() == 1) {
            boolean executeResult = operation.Execute();
            if (!executeResult)
                gattOperationQueue.remove();
        }
    }

    private void executeNextOperation() {
        gattOperationQueue.remove();
        if (gattOperationQueue.size() > 0) {
            boolean executeResult = gattOperationQueue.element().Execute();
            if (!executeResult)
                executeNextOperation();
        }
    }


    //GATT回调函数
    private transient BluetoothGattCallback mGattCallback;

    private BluetoothGattCallback getGattCallback() {
        return new BluetoothGattCallback() {
            //连接状态改变
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                LogUtil.i(TAG, "connection state change: deviceId=" + getDeviceId() + ", state=" + BLEUtility.getConnectionState(newState));
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    onDeviceConnected(getDeviceId());
                    //refreshDeviceCache(gatt);
                    if (gattOperationQueue != null)
                        gattOperationQueue.clear();
                    int serviceCount = gatt.getServices().size();
                    if (serviceCount == 0)
                        try {
                            gatt.discoverServices();
                        }
                        catch(SecurityException exception) {
                            LogUtil.e(TAG, exception.getMessage());
                        }
                    else {
                        LogUtil.i(TAG, "device already has services, skip discover");
                        onServicesDiscovered(gatt, BluetoothGatt.GATT_SUCCESS);
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    onDeviceDisconnected(getDeviceId());
                }
            }

            //服务发现
            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    Log.e(TAG, "discover services failed");
                    disconnect();
                    onDeviceError(getDeviceId(), DEVICE_ERROR_DISCOVER_SERVICE_FAILED, "discover services failed");
                    return;
                }

                Log.i(TAG, "services discovered: deviceId=" + getDeviceId() + ", services=" + gatt.getServices());

                boolean isMatch = true;
                if(metadata.has("services")) {
                    try {
                        JSONArray servicesArray = metadata.getJSONArray("services");
                        for(int s=0; s<servicesArray.length(); s++) {
                            JSONObject serviceObject = servicesArray.getJSONObject(s);
                            String serviceUuidString = serviceObject.getString("uuid");
                            UUID serviceUuid = UUID.fromString(serviceUuidString);
                            BluetoothGattService service = gatt.getService(serviceUuid);
                            if(service == null) {
                                Log.e(TAG, "service not found: deviceId=" + getDeviceId() + ", service=" + serviceUuidString);
                                isMatch = false;
                                continue;
                            }
                            JSONArray characteristicsArray = serviceObject.getJSONArray("characteristics");
                            for (int c = 0; c < characteristicsArray.length(); c++) {
                                JSONObject characteristicObject = characteristicsArray.getJSONObject(c);
                                String characteristicUuidString = characteristicObject.getString("uuid");
                                UUID characteristicUuid = UUID.fromString(characteristicUuidString);
                                BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicUuid);
                                String characteristicName = characteristicObject.getString("name");
                                JSONArray properties = characteristicObject.getJSONArray("properties");
                                if (characteristic != null) {
                                    characteristicsByName.put(characteristicName, characteristic);
                                    characteristicNamesByUUID.put(characteristicUuid, characteristicName);
                                    for(int p=0; p<properties.length(); p++) {
                                        if(properties.getString(p).equals("notify")) {
                                            startNotification(characteristicName);
                                            break;
                                        }
                                    }
                                }
                                else {
                                    Log.e(TAG, "characteristic not found: deviceId=" + getDeviceId() + ", characteristic=" + characteristicName);
                                    isMatch = false;
                                }
                            }
                        }
                    }
                    catch(JSONException exception) {
                        Log.e(TAG, "parse metadata services failed: " + exception.getMessage());
                    }
                }

                if (!isMatch)
                    onDeviceMismatch(getDeviceId());
                onDeviceReady(getDeviceId());
            }

            private void onCharacteristicUpdated(BluetoothGattCharacteristic characteristic) {
                UUID uuid = characteristic.getUuid();
                byte[] data = characteristic.getValue();
                if (data != null) {
                    LogUtil.i(TAG, "received data: " + BLEUtility.MakeHexString(data));
                    String characteristicName = characteristicNamesByUUID.get(uuid);
                    if(characteristicName != null)
                        onDeviceReceivedData(getDeviceId(), characteristicName, data);
                }
            }

            //读特性操作完成
            @Override
            public void onCharacteristicRead(BluetoothGatt gatt,
                                             BluetoothGattCharacteristic characteristic,
                                             int status) {
                if (status == BluetoothGatt.GATT_SUCCESS)
                    onCharacteristicUpdated(characteristic);
                else
                    LogUtil.e(TAG, "read characteristic failed, status: " + status);
                executeNextOperation();
            }

            //写特性操作完成
            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt,
                                              BluetoothGattCharacteristic characteristic,
                                              int status) {
                if(status != BluetoothGatt.GATT_SUCCESS)
                    LogUtil.e(TAG, "write characteristic failed, status: " + status);
                executeNextOperation();
            }

            //写描述符操作完成
            @Override
            public void onDescriptorWrite(BluetoothGatt gatt,
                                          BluetoothGattDescriptor descriptor,
                                          int status) {
                if(status != BluetoothGatt.GATT_SUCCESS)
                    LogUtil.e(TAG, "write descriptor failed, status: " + status);
                executeNextOperation();
            }

            //接收特性通知
            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt,
                                                BluetoothGattCharacteristic characteristic) {
                onCharacteristicUpdated(characteristic);
            }

            @Override
            public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
                //super.onReadRemoteRssi(gatt, rssi, status);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    onDeviceRSSIUpdated(getDeviceId(), rssi);
                    deviceRSSI = rssi;
                }
            }
        };
    }

    //清空GATT缓存
    //http://stackoverflow.com/questions/22596951/how-to-programmatically-force-bluetooth-low-energy-service-discovery-on-android
    private boolean refreshDeviceCache(BluetoothGatt gatt) {
        try {
            Method localMethod = gatt.getClass().getMethod("refresh", new Class[0]);
            if (localMethod != null) {
                boolean bool = (Boolean) localMethod.invoke(gatt, new Object[0]);
                LogUtil.i(TAG, "refresh gatt cache " + (bool ? "succeed" : "failed"));
                return bool;
            }
        } catch (Exception localException) {
            LogUtil.e(TAG, "An exception occured while refreshing device");
        }
        return false;
    }

    @Override
    public void onDeviceConnected(String deviceId) {
        Intent intent = new Intent(DeviceStateReceiver.DEVICE_CONNECTED);
        intent.putExtra("deviceId", deviceId);
        lbm.sendBroadcast(intent);
    }

    @Override
    public void onDeviceReady(String deviceId) {
        Intent intent = new Intent(DeviceStateReceiver.DEVICE_READY);
        intent.putExtra("deviceId", deviceId);
        lbm.sendBroadcast(intent);
    }

    @Override
    public void onDeviceMismatch(String deviceId) {
        Intent intent = new Intent(DeviceStateReceiver.DEVICE_MISMATCH);
        intent.putExtra("deviceId", deviceId);
        lbm.sendBroadcast(intent);
    }

    @Override
    public void onDeviceDisconnected(String deviceId) {
        Intent intent = new Intent(DeviceStateReceiver.DEVICE_DISCONNECTED);
        intent.putExtra("deviceId", deviceId);
        lbm.sendBroadcast(intent);
    }

    @Override
    public void onDeviceReceivedData(String deviceId, String name, byte[] data) {
        Intent intent = new Intent(DeviceStateReceiver.DEVICE_RECEIVED_DATA);
        intent.putExtra("deviceId", deviceId);
        intent.putExtra("name", name);
        intent.putExtra("data", data);
        lbm.sendBroadcast(intent);
    }

    @Override
    public void onDeviceValueUpdated(String deviceId, int key, String name, Serializable value) {
        Intent intent = new Intent(DeviceStateReceiver.DEVICE_VALUE_UPDATED);
        intent.putExtra("deviceId", deviceId);
        intent.putExtra("key", key);
        intent.putExtra("name", name);
        intent.putExtra("value", value);
        lbm.sendBroadcast(intent);
    }

    @Override
    public void onDeviceError(String deviceId, int errId, String error) {
        Intent intent = new Intent(DeviceStateReceiver.DEVICE_ERROR);
        intent.putExtra("deviceId", deviceId);
        intent.putExtra("errId", errId);
        intent.putExtra("error", error);
        lbm.sendBroadcast(intent);
    }

    @Override
    public void onDeviceRSSIUpdated(String deviceId, int rssi) {
        Intent intent = new Intent(DeviceStateReceiver.DEVICE_RSSI_UPDATED);
        intent.putExtra("deviceId", deviceId);
        intent.putExtra("rssi", rssi);
        lbm.sendBroadcast(intent);
    }
}
