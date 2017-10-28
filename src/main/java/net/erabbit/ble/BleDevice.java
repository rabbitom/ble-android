package net.erabbit.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import net.erabbit.ble.entity.Advertisement;
import net.erabbit.ble.entity.Characteristic;
import net.erabbit.ble.entity.DeviceObject;
import net.erabbit.ble.entity.Service;
import net.erabbit.ble.interfaces.DeviceStateCallback;
import net.erabbit.ble.utils.BleUtility;
import net.erabbit.ble.utils.LogUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;

/**
 * Created by ziv on 2017/4/18.
 */

public class BleDevice implements DeviceStateCallback, Serializable {

    public static JSONObject loadJsonAsset(Context context, String filename) {
        JSONObject testjson = null;
        try {
            InputStreamReader isr = new InputStreamReader(context.getAssets().open(filename), "UTF-8");
            BufferedReader br = new BufferedReader(isr);
            String line;
            StringBuilder builder = new StringBuilder();
            while ((line = br.readLine()) != null) {
                builder.append(line);
            }
            br.close();
            isr.close();
            testjson = new JSONObject(builder.toString());//builder读取了JSON中的数据。
        } catch (Exception e) {
            e.printStackTrace();
        }
        return testjson;
    }

    private String TAG = "ble ";

    private DeviceObject deviceObject;//JSON文件解析返回的对象
    private HashMap<String, String> uuidToNameMap = new HashMap<>();

    private String deviceKey;//(只读）系统原生蓝牙设备的ID，字符串类型
    private String deviceName;//设备名称，默认使用广播名或设备名，可以修改
    private Map advertisementData;//广播数据，字典类型
    private int deviceRSSI;

    private transient Context context;
    private transient LocalBroadcastManager lbm;

    private transient BluetoothDevice nativeDevice;//系统原生的蓝牙设备对象
    private transient BluetoothGatt btGatt;

    private transient HashMap<String, BluetoothGattCharacteristic> gattCharacteristicMap = new HashMap<>();

    public BleDevice(Context context, BluetoothDevice device, JSONObject jsonObject) {
        this.context = context;
        lbm = LocalBroadcastManager.getInstance(context);
        nativeDevice = device;
        deviceKey = (device != null) ? nativeDevice.getAddress() : "";
        deviceName = (device != null) ? nativeDevice.getName() : "";
        TAG += device.getName();
        try {
            deviceObject = parseJson(jsonObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (deviceObject != null) {
            LogUtil.i(TAG, "===deviceObject.services.size()= " + deviceObject.services.size());
            for (int i = 0; i < deviceObject.services.size(); i++) {
                Service service = deviceObject.services.get(i);
                LogUtil.i(TAG, "===service.characteristics.size()= " + service.characteristics.size());
                for (int j = 0; j < service.characteristics.size(); j++) {
                    Characteristic characteristic = service.characteristics.get(j);
                    LogUtil.i(TAG, "===characteristic.uuid= " + characteristic.uuid);
                    String uuidStr = characteristic.uuid;
                    if(uuidStr.length() == 4)
                        uuidStr = BleUtility.UUIDFromShort(uuidStr).toString();
                    uuidToNameMap.put(uuidStr.toLowerCase(), characteristic.name);
                }
            }
        }

    }

    public void restore(Context context) {
        this.context = context;
        BluetoothManager btManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if((btManager != null) && (deviceKey != null) && BluetoothAdapter.checkBluetoothAddress(deviceKey)) {
            BluetoothAdapter btAdapter = btManager.getAdapter();
            this.nativeDevice = btAdapter.getRemoteDevice(deviceKey);
        }
        gattCharacteristicMap = new HashMap<>();
        lbm = LocalBroadcastManager.getInstance(context);
    }

    //解析JSON
    private DeviceObject parseJson(JSONObject jsonObject) throws JSONException {

        DeviceObject deviceObject = new DeviceObject();
        String version = jsonObject.getString("version");
        deviceObject.version = version;

        Advertisement advertisement = new Advertisement();
        JSONObject object = jsonObject.getJSONObject("advertisement");
        String name = object.getString("name");
        String service = object.getString("service");
        String nameFilterPattern = object.getString("nameFilterPattern");
        advertisement.name = name;
        advertisement.service = service;
        advertisement.nameFilterPattern = nameFilterPattern;
        deviceObject.advertisement = advertisement;

        ArrayList<Service> services = new ArrayList<Service>();
        JSONArray jsonArray = jsonObject.getJSONArray("services");
        for (int i = 0; i < jsonArray.length(); i++) {
            Service serv = new Service();
            JSONObject servObject = jsonArray.getJSONObject(i);
            String sUuid = servObject.getString("uuid");
            String sName = servObject.getString("name");
            ArrayList<Characteristic> characteristics = new ArrayList<>();
            JSONArray charactJsonArray = servObject.getJSONArray("characteristics");
            for (int j = 0; j < charactJsonArray.length(); j++) {
                Characteristic characteristic = new Characteristic();
                JSONObject cObject = charactJsonArray.getJSONObject(j);
                String cUuid = cObject.getString("uuid");
                String cName = cObject.getString("name");
                ArrayList<String> properties = new ArrayList<>();
                JSONArray propertyArray = cObject.getJSONArray("properties");
                for (int k = 0; k < propertyArray.length(); k++) {
                    String property = propertyArray.getString(k);
                    properties.add(property);
                }
                characteristic.name = cName;
                characteristic.uuid = cUuid;
                characteristic.properties = properties;
                characteristics.add(characteristic);
            }
            serv.name = sName;
            serv.uuid = sUuid;
            serv.characteristics = characteristics;
            services.add(serv);
        }
        deviceObject.services = services;
        return deviceObject;
    }


    public String getDeviceKey() {
        return deviceKey;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public boolean getConnected() {
        BluetoothManager btManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        int connectionState = btManager.getConnectionState(nativeDevice, BluetoothProfile.GATT);
        return (connectionState == BluetoothGatt.STATE_CONNECTED);
    }

    public DeviceObject getDeviceObject() {
        return deviceObject;
    }


    public void setAdvertisementData(Map data) {
        this.advertisementData = data;
    }

    public Map getAdvertisementData(){
        return  advertisementData;
    }

    /**
     * 建立连接
     */
    public void connect() {
        if (nativeDevice != null) {
            LogUtil.i(TAG, "connect device: " + deviceKey);
            if (btGatt == null) {
                if (mGattCallback == null)
                    mGattCallback = getGattCallback();
                btGatt = nativeDevice.connectGatt(context, false, mGattCallback);
            } else
                btGatt.connect();
        }
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        if (btGatt != null) {
            btGatt.disconnect();
            btGatt.close();
            btGatt = null;
        }
    }

    /**
     * 发送数据
     * @param name 数据点
     * @param data 数据内容
     */
    public void sendData(String name, byte[] data) {

        BluetoothGattCharacteristic characteristic = gattCharacteristicMap.get(name);
        if (btGatt != null && characteristic != null) {
            Log.i(TAG, "sendData_uuid = " + characteristic.getUuid());
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

        BluetoothGattCharacteristic characteristic = gattCharacteristicMap.get(name);

        if (btGatt != null && characteristic != null) {

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
    public void startReceiveData(String name) {

        BluetoothGattCharacteristic characteristic = gattCharacteristicMap.get(name);
        if (btGatt != null && characteristic != null) {
            Log.i(TAG, "startReceiveData_uuid = " + characteristic.getUuid());
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
    public void stopReceiveData(String name) {
        BluetoothGattCharacteristic characteristic = gattCharacteristicMap.get(name);
        if (btGatt != null && characteristic != null) {
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
        btGatt.readRemoteRssi();
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
                LogUtil.i(TAG, "connection state: " + BleUtility.getConnectionState(newState));
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    onDeviceConnected(deviceKey);
                    //refreshDeviceCache(gatt);
                    if (gattOperationQueue != null)
                        gattOperationQueue.clear();
                    int serviceCount = gatt.getServices().size();
                    if (serviceCount == 0)
                        gatt.discoverServices();
                    else {
                        LogUtil.i(TAG, "device already has services, skip discover");
                        onServicesDiscovered(gatt, BluetoothGatt.GATT_SUCCESS);
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    onDeviceDisconnected(deviceKey);
                }
            }

            //服务发现
            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                //Log.i(TAG, "onServicesDiscovered");

                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.i(TAG, "services discovered success");

                    boolean isMatch = true;
                    List<BluetoothGattService> services = gatt.getServices();

                    ArrayList<String> deviceServUuidList = new ArrayList<>();
                    for (int i = 0; i < services.size(); i++) {
                        deviceServUuidList.add(services.get(i).getUuid().toString());
                    }

                    LogUtil.i(TAG, "services count = " + services.size());

                    ArrayList<String> deviceCharactUuidList = new ArrayList<>();
                    //保存所有服务的特征
                    LogUtil.i(TAG, "====uuidToNameMap=" + uuidToNameMap.toString());
                    for (BluetoothGattService service : services) {
                        Log.i(TAG, "discovered service_uuid = " + service.getUuid());
                        List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                        for (BluetoothGattCharacteristic characteristic : characteristics) {
                            UUID cUuid = characteristic.getUuid();
                            Log.i(TAG, "discovered characteristic_uuid = " + characteristic.getUuid());
                            deviceCharactUuidList.add(cUuid.toString());
                            String cName = uuidToNameMap.get(cUuid.toString());
                            if (cName != null) {
                                Log.i(TAG, "characteristic_cName = " + cName);
                                gattCharacteristicMap.put(cName, characteristic);
                            }
                        }
                    }

                    //验证json文档与设备 服务和特性是否匹配
                    for (int i = 0; i < deviceObject.services.size(); i++) {
                        Service service = deviceObject.services.get(i);
                        if (!deviceServUuidList.contains(service.uuid.toLowerCase())) {
                            isMatch = false;
                            break;
                        }
                        for (int j = 0; j < service.characteristics.size(); j++) {
                            Characteristic characteristic = service.characteristics.get(j);
                            if (!deviceCharactUuidList.contains(characteristic.uuid.toLowerCase())) {
                                isMatch = false;
                                break;
                            }
                        }
                    }

                    if (!isMatch) {
                        onDeviceMismatch(deviceKey);
                    }
                    onDeviceReady(deviceKey);

                } else {
                    LogUtil.i(TAG, "discover services failed");
                    disconnect();
                    onDeviceError(deviceKey, 101, "discover services failed");
                }
            }

            private void onCharacteristicUpdated(BluetoothGattCharacteristic characteristic) {
                UUID uuid = characteristic.getUuid();
                byte[] data = characteristic.getValue();
                if (data != null) {
                    LogUtil.i(TAG, "received data: " + BleUtility.MakeHexString(data));
                    onDeviceReceivedData(deviceKey, uuidToNameMap.get(uuid.toString()), data);
                    // onReceiveData(data);
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
                    onDeviceRSSIUpdated(deviceKey, rssi);
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
    public void onDeviceConnected(String deviceID) {

        Intent intent = new Intent(DeviceStateReceiver.DEVICE_CONNECTED);
        intent.putExtra("deviceID", deviceID);
        lbm.sendBroadcast(intent);
    }

    @Override
    public void onDeviceReady(String deviceID) {
        Intent intent = new Intent(DeviceStateReceiver.DEVICE_READY);
        intent.putExtra("deviceID", deviceID);
        lbm.sendBroadcast(intent);
    }

    @Override
    public void onDeviceMismatch(String deviceID) {
        Intent intent = new Intent(DeviceStateReceiver.DEVICE_MISMATCH);
        intent.putExtra("deviceID", deviceID);
        lbm.sendBroadcast(intent);
    }

    @Override
    public void onDeviceDisconnected(String deviceID) {
        Intent intent = new Intent(DeviceStateReceiver.DEVICE_DISCONNECTED);
        intent.putExtra("deviceID", deviceID);
        lbm.sendBroadcast(intent);
    }

    @Override
    public void onDeviceReceivedData(String deviceID, String name, byte[] data) {
        Intent intent = new Intent(DeviceStateReceiver.DEVICE_RECEIVED_DATA);
        intent.putExtra("deviceID", deviceID);
        intent.putExtra("name", name);
        intent.putExtra("data", data);
        lbm.sendBroadcast(intent);

    }

    @Override
    public void onDeviceValueChanged(String deviceID, int key, Serializable value) {
        Intent intent = new Intent(DeviceStateReceiver.DEVICE_VALUE_CHANGED);
        intent.putExtra("deviceID", deviceID);
        intent.putExtra("key", key);
        intent.putExtra("value", value);
        lbm.sendBroadcast(intent);
    }

    @Override
    public void onDeviceError(String deviceID, int errId, String error) {
        Intent intent = new Intent(DeviceStateReceiver.DEVICE_ERROR);
        intent.putExtra("deviceID", deviceID);
        intent.putExtra("errId", errId);
        intent.putExtra("error", error);
        lbm.sendBroadcast(intent);
    }

    @Override
    public void onDeviceRSSIUpdated(String deviceID, int rssi) {
        Intent intent = new Intent(DeviceStateReceiver.DEVICE_RSSI_UPDATED);
        intent.putExtra("deviceID", deviceID);
        intent.putExtra("rssi", rssi);
        lbm.sendBroadcast(intent);
    }
}
