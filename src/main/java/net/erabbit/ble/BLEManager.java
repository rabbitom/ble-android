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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public class BLEManager implements BLEScanCallback {

    private static final String TAG = "[BLE]";

    protected BluetoothAdapter mBluetoothAdapter;
    private final ArrayList<BLEDevice> bleDevices = new ArrayList<>(); //保存所有创建后的BleDevice
    private final ArrayList<JSONObject> deviceClassesMetadata = new ArrayList<>();

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
                if(getDevice(deviceId) == null) {
                    BLEDevice bleDevice = new BLEDevice(result);
                    bleDevices.add(bleDevice);
                }
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

    public void addDeviceClass(JSONObject metadata) {
        deviceClassesMetadata.add(metadata);
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
