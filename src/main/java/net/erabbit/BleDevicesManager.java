package net.erabbit;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import net.erabbit.entity.Advertisement;
import net.erabbit.entity.Characteristic;
import net.erabbit.entity.DeviceObject;
import net.erabbit.entity.Service;
import net.erabbit.interfaces.BLESearchCallback;
import net.erabbit.utils.BleUtility;
import net.erabbit.utils.LogUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Created by ziv on 2017/4/18.
 */

public class BleDevicesManager implements BluetoothAdapter.LeScanCallback, BLESearchCallback {

    private static final String TAG = "ble";

    private static final int BLE_ADVERTISEMENT_SERVICE_UUID = 0x07;//ble协议

    ArrayList<String> filterServiceUUIDList = new ArrayList<>();
    private boolean autoSearch;//是否在蓝牙可用时立即开始搜索
    private BleDevice curDevice;//在同一时刻只连接一个设备的应用中设置和获取当前设备
    private int timeSearch = 10000;//10秒
    protected BluetoothAdapter mBluetoothAdapter;
    private ArrayList<BluetoothDevice> devices = new ArrayList<>();
    private ArrayList<BleDevice> bleDevices = new ArrayList<>();
    private DeviceObject deviceObject;//JSON文件解析返回的对象

    private boolean isScanning = false;

    private HashMap<String, String> uuidToNameMap = new HashMap<>();

    Timer scanTimer;
    private static BleDevicesManager bleDevicesManager;
    private LocalBroadcastManager lbm;
    private Context context;

    public static BleDevicesManager getInstance(Context context) {

        if (bleDevicesManager == null) {
            synchronized (BleDevicesManager.class) {
                if (bleDevicesManager == null) {
                    bleDevicesManager = new BleDevicesManager(context);
                }
            }
        }
        return bleDevicesManager;
    }

    private BleDevicesManager(Context context) {
        this.context = context;
        lbm = LocalBroadcastManager.getInstance(context);
    }

    public ArrayList<BluetoothDevice> getDevices() {
        return devices;
    }


    public String getNameByUUID2(String uuid) {

        if (uuidToNameMap != null) {
            return uuidToNameMap.get(uuid);
        }
        return null;
    }


    /**
     * 添加设备定义，在搜索时只搜索指定类型的设备，
     * 可以多次调用以支持多个设备类型，若从未调用过则搜索所有设备
     *
     * @param jsonObject
     */
    public void addDeviceFilter(JSONObject jsonObject) throws JSONException {

        deviceObject = parseJson(jsonObject);
        filterServiceUUIDList.add(deviceObject.advertisement.service);

        for (int i = 0; i < deviceObject.services.size(); i++) {
            Service service = deviceObject.services.get(i);
            for (int j = 0; j < service.characteristics.size(); j++) {
                Characteristic characteristic = service.characteristics.get(j);
                uuidToNameMap.put(characteristic.uuid, characteristic.name);
            }
        }
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
            }
            serv.name = sName;
            serv.uuid = sUuid;
            serv.characteristics = characteristics;
            services.add(serv);
        }
        deviceObject.services = services;
        return deviceObject;
    }


    /**
     * 设置搜索设备的超时时间，到时间后自动停止
     *
     * @param timeSearch 单位秒
     */
    public void setSearchTimeout(int timeSearch) {
        this.timeSearch = timeSearch;
    }

    public void setAutoSearch(boolean autoSearch) {
        this.autoSearch = autoSearch;
    }

    public boolean getAutoSearch() {
        return autoSearch;
    }

    /**
     * @return 是否正常启动搜索
     */
    public boolean startSearch(Context context) {

        if (mBluetoothAdapter == null) {
            //初始化蓝牙适配器
            final BluetoothManager bluetoothManager =
                    (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = bluetoothManager.getAdapter();
        }
        if (mBluetoothAdapter != null) {
            //检查蓝牙是否已打开
            if (mBluetoothAdapter.isEnabled()) {
                startScan();
                return true;
            } else {
                //显示对话框要求用户启用蓝牙
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                //context.startActivityForResult(enableBtIntent, REQUEST_BT_ENABLE);
            }
        }

        return false;
    }

    public void startSearch(Activity activity) {
        LogUtil.i(TAG, "===startSearch(activity)");
        boolean search;
        if (mBluetoothAdapter == null) {
            //初始化蓝牙适配器
            final BluetoothManager bluetoothManager =
                    (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = bluetoothManager.getAdapter();
        }

        if (mBluetoothAdapter != null) {
            FragmentManager fm = activity.getFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            ScanFragment scanFragment = new ScanFragment();
            ft.add(scanFragment, "DeviceScan");
            ft.commit();//异步的==!
            fm.executePendingTransactions();//同步执行
            scanFragment.setBluetoothStateCallback(new ScanFragment.BluetoothStateCallback() {
                @Override
                public void onBluetoothEnabled() {
                    startScan();
                }
            });
            scanFragment.tryScan(mBluetoothAdapter);
        }
    }


    protected void startScan() {
        //数据初值
        devices.clear();
        //开始搜索
        isScanning = mBluetoothAdapter.startLeScan(this);
        LogUtil.i(TAG, "===isScanning=" + isScanning);
        if (isScanning) {
            onSearchStarted();
            if (timeSearch > 0) {
                scanTimer = new Timer();
                scanTimer.schedule(new TimerTask() {
                    public void run() {
                        if (isScanning) {
                            stopSearch();
                            onSearchTimeOut();//超时

                        }
                    }
                }, timeSearch);
            }
        }
    }

    /**
     * 停止扫描
     */
    public void stopSearch() {

        if (isScanning) {
            scanTimer.cancel();
            isScanning = false;
            mBluetoothAdapter.stopLeScan(this);
        }
    }

    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {

        onRSSIUpdated(device.getAddress(), rssi);
        //deviceHandler.sendMessage(deviceHandler.obtainMessage(BleDeviceScanHandler.MSG_SCAN_RSSI_UPDATED, rssi, 0, device));
        //扫描到设备以后的动作
        if (!devices.contains(device)) {
            devices.add(device);
            if (filterServiceUUIDList.size() != 0) {
                Map<Integer, byte[]> scanRecords = parseScanRecord(scanRecord);
                if (scanRecords.containsKey(BLE_ADVERTISEMENT_SERVICE_UUID)) {
                    //check service UUID
                    byte[] serviceUUIDBytes = scanRecords.get(BLE_ADVERTISEMENT_SERVICE_UUID);
                    UUID serviceUUID = BleUtility.toUUIDLE(serviceUUIDBytes, 0);
                    if (serviceUUID != null) {
                        LogUtil.i(TAG, "service UUID: " + serviceUUID.toString());
                        for (String uuid : filterServiceUUIDList) {
                            if (serviceUUID.toString().equals(uuid)) {
                                //发现设备
                                LogUtil.i(TAG, String.format("onFoundDevice, name = %s, address = %s", device.getName(), device.getAddress()));
                                onFoundDevice(device.getAddress(), rssi, scanRecord, device.getName());

                            }
                        }
                    }
                }
            } else {
                LogUtil.i(TAG, String.format("onFoundDevice, name = %s, address = %s", device.getName(), device.getAddress()));
                onFoundDevice(device.getAddress(), rssi, scanRecord, device.getName());
            }
        }
    }

    protected Map<Integer, byte[]> parseScanRecord(byte[] scanRecord) {
        Map<Integer, byte[]> scanRecords = new TreeMap<>();
        int offset = 0;
        while (scanRecord.length > offset) {
            int length = BleUtility.toInt(scanRecord[offset]);
            if (length > 0) {
                LogUtil.i(TAG, "scan: " + BleUtility.MakeHexString(scanRecord, offset + 1, length));
                int key = BleUtility.toInt(scanRecord[offset + 1]);
                byte[] valueBytes = new byte[length - 1];
                System.arraycopy(scanRecord, offset + 2, valueBytes, 0, length - 1);
                scanRecords.put(key, valueBytes);
                offset += (length + 1);
            } else
                break;
        }
        return scanRecords;
    }


    /**
     * @return
     */
    public BleDevice findDevice(String deviceId) {

        for (BleDevice device : bleDevices) {
            if (device.getDeviceKey().equals(deviceId)) {
                return device;
            }
        }
        return null;
    }

    public BleDevice createDevice(String deviceId, Context context, Class clazz, JSONObject jsonObject) {

        BleDevice bleDevice = null;
        Class[] paramDef = new Class[]{Context.class, BluetoothDevice.class, JSONObject.class};
        Constructor constructor = null;
        try {
            constructor = clazz.getConstructor(paramDef);
            for (BluetoothDevice device : devices) {
                if (device.getAddress().equals(deviceId)) {
                    bleDevice = (BleDevice) constructor.newInstance(context, device, jsonObject);
                    bleDevices.add(bleDevice);
                }
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return bleDevice;
    }

    @Override
    public void onSearchStarted() {

        lbm.sendBroadcast(new Intent("SearchStarted"));
    }

    @Override
    public void onSearchTimeOut() {
        lbm.sendBroadcast(new Intent("SearchTimeOut"));

    }

    @Override
    public void onFoundDevice(String deviceID, int rssi, byte[] data, String deviceType) {

        Intent intent = new Intent("FoundDevice");
        intent.putExtra("deviceID", deviceID);
        intent.putExtra("rssi", rssi);
        intent.putExtra("data", data);
        intent.putExtra("deviceType", deviceType);

        lbm.sendBroadcast(intent);

    }

    @Override
    public void onAdvertisementUpdated() {
        lbm.sendBroadcast(new Intent("AdvertisementUpdated"));
    }

    @Override
    public void onRSSIUpdated(String deviceID, int rssi) {
        Intent intent = new Intent("RSSIUpdated");
        intent.putExtra("deviceID", deviceID);
        intent.putExtra("rssi", rssi);
        lbm.sendBroadcast(intent);
    }
}
