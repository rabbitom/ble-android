package net.erabbit.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelUuid;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.util.Log;

import net.erabbit.ble.interfaces.BLEScanCallback;
import net.erabbit.ble.utils.BLEUtility;
import net.erabbit.ble.utils.LogUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BLEManager implements BLEScanCallback {

    private static final String TAG = "[BLE]";

    protected BluetoothAdapter mBluetoothAdapter;
    private final ArrayList<BLEDevice> bleDevices = new ArrayList<>(); //保存所有创建后的BleDevice
    private final ArrayList<JSONObject> deviceClassesMetadata = new ArrayList<>();
    private final Map<String,Class<?>> deviceClasses = new HashMap<>();
    private boolean ignoreDevicesOfUnknownClass = false;
    public void setIgnoreDevicesOfUnknownClass(boolean ignore) {
        ignoreDevicesOfUnknownClass = ignore;
    }

    private boolean isScanning = false;

    private static BLEManager bleManager;
    private final LocalBroadcastManager lbm;

    private final BluetoothLeScanner mBluetoothLeScanner;
    private final ScanCallback mScanCallback;

    public static BLEManager getInstance(Context context) {
        if (bleManager == null)
            bleManager = new BLEManager(context);
        return bleManager;
    }

    private BLEManager(Context context) {
        lbm = LocalBroadcastManager.getInstance(context);
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        mScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                Log.i(TAG, "scan result:" + result);
                String deviceId = result.getDevice().getAddress();
                BLEDevice device = getDevice(deviceId);
                if(device == null) {
                    JSONObject deviceClassMetadata = null;
                    ScanRecord scanRecord = result.getScanRecord();
                    for(JSONObject metadata : deviceClassesMetadata) {
                        if(metadata.has("scanFilters")) {
                            try {
                                JSONObject scanFilters = metadata.getJSONObject("scanFilters");
                                if(scanFilters.has("serviceData")) {
                                    JSONObject serviceData = scanFilters.getJSONObject("serviceData");
                                    String uuid = serviceData.getString("uuid");
                                    ParcelUuid parcelUuid = new ParcelUuid(BLEUtility.UUIDFromShort(uuid));
                                    if(scanRecord != null && scanRecord.getServiceData(parcelUuid) != null) {
                                        deviceClassMetadata = metadata;
                                        break;
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "parse scanFilters failed: " + e.getMessage());
                            }
                        }
                    }
                    BLEDevice bleDevice = null;
                    if(deviceClassMetadata != null) {
                        try {
                            String className = deviceClassMetadata.getString("class");
                            Class<?> deviceClass = deviceClasses.get(className);
                            if(deviceClass != null) {
                                Constructor<?> constructor = deviceClass.getConstructor(ScanResult.class);
                                bleDevice = (BLEDevice) constructor.newInstance(result);
                            }
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                    else if(!ignoreDevicesOfUnknownClass)
                        bleDevice = new BLEDevice(result);
                    else
                        return;
                    if(bleDevice != null)
                        bleDevices.add(bleDevice);
                }
                else
                    device.updateStatus(result);
                onFoundDevice(deviceId);
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                Log.i(TAG, "batch scan results: size=" + results.size());
            }

            @Override
            public void onScanFailed(int errorCode) {
                Log.i(TAG, "scan failed: errorCode=" + errorCode);
            }
        };
    }

    public void addDeviceClass(Class<?> deviceClass, JSONObject metadata) {
        try {
            String className = metadata.getString("class");
            deviceClasses.put(className, deviceClass);
            deviceClassesMetadata.add(metadata);
        }
        catch (JSONException exception) {
            Log.e(TAG, "add device class failed: " + exception.getMessage());
        }
    }

    public void startScan() {
        if(mBluetoothAdapter.isEnabled()) {
            try {
                mBluetoothLeScanner.startScan(mScanCallback);
                isScanning = true;
            }
            catch(SecurityException exception) {
                LogUtil.e(TAG, exception.getMessage());
            }
        }
        LogUtil.i(TAG, "start scan: isScanning=" + isScanning);
        if (isScanning)
            onScanStarted();
    }

    /**
     * 停止扫描
     */
    public void stopScan() {
        if (isScanning) {
            //如果停止搜索时蓝牙已经关掉会导致崩溃
            if(mBluetoothAdapter.isEnabled())
                try {
                    mBluetoothLeScanner.stopScan(mScanCallback);
                }
                catch(SecurityException exception) {
                    Log.e(TAG, "stop scan failed: " + exception.getMessage());
                    return;
                }
            isScanning = false;
            Log.i(TAG,"stop scan");
            onScanStopped();
        }
    }

    public boolean isScanning() {
        return isScanning;
    }

    public BLEDevice getDevice(String deviceId) {
        for (BLEDevice device : bleDevices) {
            if (device.getDeviceId().equals(deviceId)) {
                return device;
            }
        }
        return null;
    }

    @Override
    public void onScanError(int errId, String error) {
        Intent intent = new Intent(BLE_SCAN_ERROR);
        intent.putExtra("errId", errId);
        intent.putExtra("error", error);
        lbm.sendBroadcast(intent);
    }

    @Override
    public void onScanStarted() {
        lbm.sendBroadcast(new Intent(BLE_SCAN_STARTED));
    }

    @Override
    public void onScanStopped() {
        lbm.sendBroadcast(new Intent(BLE_SCAN_STOPPED));
    }

    @Override
    public void onFoundDevice(String deviceId) {
        Intent intent = new Intent(BLE_FOUND_DEVICE);
        intent.putExtra("deviceId", deviceId);
        lbm.sendBroadcast(intent);
    }

    @Override
    public void onAdvertisementUpdated(String deviceId, Map<Integer, byte[]> data) {
        Intent intent = new Intent(BLE_ADVERTISEMENT_UPDATED);
        intent.putExtra("deviceId", deviceId);
        intent.putExtra("data", (Serializable) data);
        lbm.sendBroadcast(intent);
    }

    @Override
    public void onRSSIUpdated(String deviceId, int rssi) {
        Intent intent = new Intent(BLE_RSSI_UPDATED);
        intent.putExtra("deviceId", deviceId);
        intent.putExtra("rssi", rssi);
        lbm.sendBroadcast(intent);
    }
}
