package net.erabbit.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelUuid;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.util.Log;

import net.erabbit.ble.entity.Characteristic;
import net.erabbit.ble.entity.DeviceObject;
import net.erabbit.ble.entity.FindDeviceData;
import net.erabbit.ble.entity.Service;
import net.erabbit.ble.interfaces.BLEScanCallback;
import net.erabbit.ble.utils.BLEUtility;
import net.erabbit.ble.utils.LogUtil;

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
import java.util.TreeMap;
import java.util.UUID;

/**
 * Created by ziv on 2017/4/18.
 */

public class BLEManager implements BLEScanCallback {

    private static final String TAG = "[BLE]";
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
    private final ArrayList<BluetoothDevice> devices = new ArrayList<>();//保存所有系统搜索到的设备
    private ArrayList<BleDevice> bleDevices = new ArrayList<>(); //保存所有创建后的BleDevice
    private DeviceObject deviceObject;//JSON文件解析返回的对象
    private ArrayList<JSONObject> deviceClassesMetadata = new ArrayList<>();

    private boolean isScanning = false;

    private HashMap<String, String> uuidToNameMap = new HashMap<>();

    Timer scanTimer;
    private static BLEManager bleManager;
    private LocalBroadcastManager lbm;
    private Context context;

    private BluetoothAdapter.LeScanCallback mLeScanCallback;
    private BluetoothLeScanner mBluetoothLeScanner;
    private ScanCallback mScanCallback;

    private boolean scanFilterByServiceUUID = true;

    public boolean isScanFilterByServiceUUID() {
        return scanFilterByServiceUUID;
    }

    public void setScanFilterByServiceUUID(boolean scanFilterByServiceUUID) {
        this.scanFilterByServiceUUID = scanFilterByServiceUUID;
    }

    public static BLEManager getInstance(Context context) {

        if (bleManager == null) {
            synchronized (BLEManager.class) {
                if (bleManager == null) {
                    bleManager = new BLEManager(context);
                }
            }
        }
        return bleManager;
    }

    private BLEManager(Context context) {
        this.context = context;
        lbm = LocalBroadcastManager.getInstance(context);
        mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                doScanCallback(device, rssi, scanRecord);
            }
        };

        if (mBluetoothAdapter == null) {
            //初始化蓝牙适配器
            final BluetoothManager bluetoothManager =
                    (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = bluetoothManager.getAdapter();
        }
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        mScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                doScanCallback(result.getDevice(), result.getRssi(), result.getScanRecord().getBytes());
            }
        };
    }


    private void doScanCallback(BluetoothDevice device, int rssi, byte[] scanRecord) {
        //更新RSSI
        onRSSIUpdated(device.getAddress(), rssi);

        synchronized (devices) {
            if (!devices.contains(device)) {
                devices.add(device);
            }
        }

        //解析广播数据
        Map<Integer, byte[]> scanRecordMap = parseScanRecord(scanRecord);
        byte[] serviceUUIDBytes = null;
        for (int k = BLE_ADVERTISEMENT_SERVICE_UUID_BEGIN; k <= BLE_ADVERTISEMENT_SERVICE_UUID_END; k++) {
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

        try {
            LogUtil.i(TAG, String.format("found device, name = %s, address = %s", device.getName(), device.getAddress()));
            onFoundDevice(device.getAddress(), rssi, scanRecordMap, device.getName());
        }
        catch(SecurityException exception) {
            LogUtil.e(TAG, exception.getMessage());
        }
        findDeviceData.hasCalledOnFound = true;
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
    // to deprecate
    public void addSearchFilter(JSONObject jsonObject) throws JSONException {

        deviceObject = BleDevice.parseJson(jsonObject);
        filterServiceUUIDList.add(deviceObject.advertisement.service.replace("-",""));
        Log.i(TAG,"FilterUUID="+deviceObject.advertisement.service.replace("-",""));
        for (int i = 0; i < deviceObject.services.size(); i++) {
            Service service = deviceObject.services.get(i);
            for (int j = 0; j < service.characteristics.size(); j++) {
                Characteristic characteristic = service.characteristics.get(j);
                uuidToNameMap.put(characteristic.uuid, characteristic.name);
            }
        }
    }

    public void addDeviceClass(JSONObject metadata) {
        deviceClassesMetadata.add(metadata);
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

    public void startScan() {
        //数据初值
        findDeviceHashMap.clear();
        synchronized (devices) {
            devices.clear();
        }
        //开始搜索
        UUID mainServiceUUID = null;
        if((deviceObject != null) && scanFilterByServiceUUID) {
            String UUIDString = deviceObject.advertisement.service;
            if(UUIDString != null)
                try {
                    if (UUIDString.length() == 4)
                        mainServiceUUID = BLEUtility.UUIDFromShort(UUIDString);
                    else {
                        if (UUIDString.length() == 32)
                            UUIDString = UUIDString.substring(0, 8) + "-" +
                                    UUIDString.substring(8, 4) + "-" +
                                    UUIDString.substring(12, 4) + "-" +
                                    UUIDString.substring(16, 4) + "-" +
                                    UUIDString.substring(20);
                        mainServiceUUID = UUID.fromString(UUIDString);
                    }
                } catch (IllegalArgumentException e) {
                    LogUtil.i(TAG, "can't make uuid from service uuid string: " + e.getMessage());
                }
        }
        if (mBluetoothLeScanner == null) {
            mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        }
        try {
            if(mainServiceUUID != null) {
                ScanFilter filter = new ScanFilter.Builder().setServiceUuid(new ParcelUuid(mainServiceUUID)).build();
                ArrayList<ScanFilter> filters = new ArrayList<>();
                filters.add(filter);
                mBluetoothLeScanner.startScan(filters, new ScanSettings.Builder().build(), mScanCallback);
            }
            else
                mBluetoothLeScanner.startScan(mScanCallback);
            isScanning = true;
        }
        catch(SecurityException exception) {
            LogUtil.e(TAG, exception.getMessage());
        }
        LogUtil.i(TAG, "scanning: " + isScanning);
        if (isScanning)
            onScanStarted();
    }

    /**
     * 停止扫描
     */
    public void stopScan() {

        if (isScanning) {
            scanTimer.cancel();
            isScanning = false;
            //如果停止搜索时蓝牙已经关掉会导致崩溃
            if(mBluetoothAdapter.isEnabled())
                try {
                    mBluetoothLeScanner.stopScan(mScanCallback);
                }
                catch(SecurityException exception) {
                    Log.e(TAG, exception.getMessage());
                }
        }
    }

    public boolean isScanning() {
        return isScanning;
    }

    protected Map<Integer, byte[]> parseScanRecord(byte[] scanRecord) {
        Map<Integer, byte[]> scanRecords = new TreeMap<>();
        int offset = 0;
        while (scanRecord.length > offset) {
            int length = BLEUtility.toInt(scanRecord[offset]);
            if (length > 0) {
                LogUtil.i(TAG, "scan: " + BLEUtility.MakeHexString(scanRecord, offset + 1, length));
                int key = BLEUtility.toInt(scanRecord[offset + 1]);
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
            synchronized (devices) {
                for (BluetoothDevice device : devices) {
                    if (device.getAddress().equals(deviceId)) {
                        bleDevice = (BleDevice) constructor.newInstance(context, device, jsonObject);
                        //设置设备的广播数据
                        bleDevice.setAdvertisementData(findDeviceHashMap.get(deviceId).scanRecordMap);
                        bleDevices.add(bleDevice);
                        break;
                    }
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
    public void onScanError(int errId, String error) {
        Intent intent = new Intent("SearchError");
        intent.putExtra("errId", errId);
        intent.putExtra("error", error);
        lbm.sendBroadcast(intent);
    }

    @Override
    public void onScanStarted() {
        lbm.sendBroadcast(new Intent("SearchStarted"));
    }

    @Override
    public void onScanTimeout() {
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
