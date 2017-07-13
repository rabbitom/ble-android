package net.erabbit.ble;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import net.erabbit.ble.entity.Advertisement;
import net.erabbit.ble.entity.Characteristic;
import net.erabbit.ble.entity.DeviceObject;
import net.erabbit.ble.entity.FindDeviceData;
import net.erabbit.ble.entity.Service;
import net.erabbit.ble.interfaces.BLESearchCallback;
import net.erabbit.ble.utils.BleUtility;
import net.erabbit.ble.utils.LogUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

/**
 * Created by ziv on 2017/4/18.
 */

public class BleDevicesManager implements BLESearchCallback {

    private static final String TAG = "ble";
    private static final String FRAGMENT_TAG = "BleDeviceScan";

    //BLE广播数据类型，02~07都是服务UUID，参考：https://www.bluetooth.com/specifications/assigned-numbers/generic-access-profile
    private static final int BLE_ADVERTISEMENT_SERVICE_UUID_BEGIN = 0x02;
    private static final int BLE_ADVERTISEMENT_SERVICE_UUID_END = 0x07;
    public static final int BLE_ADVERTISEMENT_MANUFACTURER_SPECIFIC = 0xFF;

    HashMap<String, FindDeviceData> findDeviceHashMap = new HashMap<>();

    ArrayList<String> filterServiceUUIDList = new ArrayList<>();
    private boolean autoSearch;//是否在蓝牙可用时立即开始搜索
    private BleDevice curDevice;//在同一时刻只连接一个设备的应用中设置和获取当前设备
    private int timeSearch = 10000;//10秒
    protected BluetoothAdapter mBluetoothAdapter;
    private ArrayList<BluetoothDevice> devices = new ArrayList<>();//保存所有系统搜索到的设备
    private ArrayList<BleDevice> bleDevices = new ArrayList<>(); //保存所有创建后的BleDevice
    private DeviceObject deviceObject;//JSON文件解析返回的对象

    private boolean isScanning = false;

    private HashMap<String, String> uuidToNameMap = new HashMap<>();

    Timer scanTimer;
    private static BleDevicesManager bleDevicesManager;
    private LocalBroadcastManager lbm;
    private Context context;

    private BluetoothAdapter.LeScanCallback mLeScanCallback;
    private BluetoothLeScanner mBluetoothLeScanner;
    private ScanCallback mScanCallback;

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
        mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                doScanCallback(device, rssi, scanRecord);
            }
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            if (mBluetoothAdapter == null) {
                //初始化蓝牙适配器
                final BluetoothManager bluetoothManager =
                        (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
                mBluetoothAdapter = bluetoothManager.getAdapter();
            }
            mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
            mScanCallback = new ScanCallback() {
                @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);
                    doScanCallback(result.getDevice(), result.getRssi(), result.getScanRecord().getBytes());
                }
            };
        }
    }


    private void doScanCallback(BluetoothDevice device, int rssi, byte[] scanRecord) {
        //更新RSSI
        onRSSIUpdated(device.getAddress(), rssi);

        if(!devices.contains(device)){
            devices.add(device);
        }

        //解析广播数据
        Map<Integer, byte[]> scanRecordMap = parseScanRecord(scanRecord);
        byte[] serviceUUIDBytes = null;
        for(int k = BLE_ADVERTISEMENT_SERVICE_UUID_BEGIN; k <= BLE_ADVERTISEMENT_SERVICE_UUID_END; k++) {
            if (scanRecordMap.containsKey(k)) {
                serviceUUIDBytes = scanRecordMap.get(k);
                break;
            }
        }

        //检查设备信息缓存
        FindDeviceData findDeviceData;
        if (findDeviceHashMap.containsKey(device.getAddress())) {
            //非第一次搜索到此设备
            findDeviceData = findDeviceHashMap.get(device.getAddress());
            Iterator<Integer> iter = scanRecordMap.keySet().iterator();
            boolean advertisementChanged = false;
            while (iter.hasNext() && (!advertisementChanged)) {
                int key = iter.next();
                if (!findDeviceData.scanRecordMap.containsKey(key))
                    advertisementChanged = true;
                else {
                    byte[] value = scanRecordMap.get(key);
                    byte[] oldValue = findDeviceData.scanRecordMap.get(key);
                    advertisementChanged = !Arrays.equals(value, oldValue);
                }
            }
            if (advertisementChanged) {
                findDeviceData.scanRecordMap.putAll(scanRecordMap);
                if (findDeviceData.hasCalledOnFound) {
                    onAdvertisementUpdated(device.getAddress(), scanRecordMap);
                    return;
                }
            } else
                return;
        } else {
            //第一次搜索到
            findDeviceData = new FindDeviceData();
            findDeviceData.hasCalledOnFound = false;
            findDeviceData.id = device.getAddress();
            //findDeviceData.scanRecord = scanRecord;
            findDeviceData.scanRecordMap = scanRecordMap;
            findDeviceHashMap.put(device.getAddress(), findDeviceData);
        }

        //检查过滤条件
        if (filterServiceUUIDList.size() != 0) {
            //需要过滤
            if (serviceUUIDBytes != null) {
                String serviceID = BleUtility.bytesToHexStringReversal(serviceUUIDBytes);
                for (String uuid : filterServiceUUIDList) {
                    if (serviceID.toString().equals(uuid)) {
                        //发现设备
                        LogUtil.i(TAG, String.format("onFoundDevice, name = %s, address = %s", device.getName(), device.getAddress()));
                        onFoundDevice(device.getAddress(), rssi, scanRecordMap, deviceObject.advertisement.name);
                        findDeviceData.hasCalledOnFound = true;
                    }
                }
            }
        } else {
            //没有过滤条件
            LogUtil.i(TAG, String.format("onFoundDevice, name = %s, address = %s", device.getName(), device.getAddress()));
            onFoundDevice(device.getAddress(), rssi, scanRecordMap, device.getName());
            findDeviceData.hasCalledOnFound = true;
        }
    }

    public ArrayList<BluetoothDevice> getDevices() {
        return devices;
    }


    /**
     * 添加设备定义，在搜索时只搜索指定类型的设备，
     * 可以多次调用以支持多个设备类型，若从未调用过则搜索所有设备
     *
     * @param jsonObject
     */

    public void addSearchFilter(JSONObject jsonObject) throws JSONException {

        deviceObject = parseJson(jsonObject);
        filterServiceUUIDList.add(deviceObject.advertisement.service.replace("-",""));

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

    public void setCurDevice(BleDevice bleDevice) {
        this.curDevice = bleDevice;
    }

    public BleDevice getCurDevice() {
        return curDevice;
    }


    /**
     * 搜索（未处理权限问题）
     *
     * @return 是否正常启动搜索
     */
    public boolean startSearch(Context context) {
        if (context == null) {
            return false;
        }

        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            onSearchError(ERROR_NO_BLE, "设备不支持BLE");
            return false;
        }
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
                onSearchError(ERROR_BLUETOOTH_DISABLE, "蓝牙未开启");
                //显示对话框要求用户启用蓝牙
                //Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                //context.startActivityForResult(enableBtIntent, REQUEST_BT_ENABLE);
            }
        }
        return false;
    }


    /**
     * 搜索（内部已处理权限问题）
     *
     * @param activity
     */
    public void startSearch(Activity activity) {

        // Use this check to determine whether BLE is supported on the device. Then
        // you can selectively disable BLE-related features.

        if (activity == null)
            return;

        if (!activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            onSearchError(ERROR_NO_BLE, "设备不支持BLE");
            return;
        }

        if (mBluetoothAdapter == null) {
            //初始化蓝牙适配器
            final BluetoothManager bluetoothManager =
                    (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = bluetoothManager.getAdapter();
        }

        if (mBluetoothAdapter != null) {

            FragmentManager fm = activity.getFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();

            Fragment fragment = fm.findFragmentByTag(FRAGMENT_TAG);
            ScanFragment scanFragment = null;

            if (fragment == null) {
                scanFragment = new ScanFragment();
                ft.add(scanFragment, FRAGMENT_TAG);
                ft.commit();//异步的==!
                fm.executePendingTransactions();//同步执行
                scanFragment.setBluetoothStateCallback(new ScanFragment.BluetoothStateCallback() {
                    @Override
                    public void onBluetoothEnabled() {
                        startScan();
                    }
                });
            } else {
                scanFragment = (ScanFragment) fragment;
            }

            scanFragment.tryScan(mBluetoothAdapter);
        }
    }


    protected void startScan() {
        //数据初值
        findDeviceHashMap.clear();
        devices.clear();
        //开始搜索
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            isScanning = mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            if (mBluetoothLeScanner == null) {
                mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
            }
            mBluetoothLeScanner.startScan(mScanCallback);
            isScanning = true;
        }
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
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            } else {
                mBluetoothLeScanner.stopScan(mScanCallback);
            }
        }
    }

    public boolean isSearching() {
        return isScanning;
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
        LogUtil.i(TAG,"createDevice");
        BleDevice bleDevice = null;
        Class[] paramDef = new Class[]{Context.class, BluetoothDevice.class, JSONObject.class};
        Constructor constructor = null;
        try {
            constructor = clazz.getConstructor(paramDef);
            for (BluetoothDevice device : devices) {
                if (device.getAddress().equals(deviceId)) {
                    bleDevice = (BleDevice) constructor.newInstance(context, device, jsonObject);
                    //设置设备的广播数据
                    bleDevice.setAdvertisementData(findDeviceHashMap.get(deviceId).scanRecordMap);
                    bleDevices.add(bleDevice);
                    break;
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
    public void onSearchError(int errId, String error) {
        Intent intent = new Intent("SearchError");
        intent.putExtra("errId", errId);
        intent.putExtra("error", error);
        lbm.sendBroadcast(intent);
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
    public void onFoundDevice(String deviceID, int rssi, Map<Integer, byte[]> data, String deviceType) {

        Intent intent = new Intent("FoundDevice");
        intent.putExtra("deviceID", deviceID);
        intent.putExtra("rssi", rssi);
        intent.putExtra("data", (Serializable) data);
        intent.putExtra("deviceType", deviceType);

        lbm.sendBroadcast(intent);

    }

    @Override
    public void onAdvertisementUpdated(String deviceID, Map<Integer, byte[]> data) {

        Intent intent = new Intent("FoundDevice");
        intent.putExtra("deviceID", deviceID);
        intent.putExtra("data", (Serializable) data);
        lbm.sendBroadcast(intent);
    }

    @Override
    public void onRSSIUpdated(String deviceID, int rssi) {
        Intent intent = new Intent("RSSIUpdated");
        intent.putExtra("deviceID", deviceID);
        intent.putExtra("rssi", rssi);
        lbm.sendBroadcast(intent);
    }
}
