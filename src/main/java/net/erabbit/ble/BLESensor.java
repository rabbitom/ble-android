package net.erabbit.ble;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class BLESensor extends BleDevice {

    private ArrayList<BLESensorFeature> features;

    public int getFeatureCount() {
        return features.size();
    }

    public BLESensorFeature getFeature(int index) {
        return features.get(index);
    }

    public BLESensor(Context context, BluetoothDevice device, JSONObject jsonObject) {
        super(context, device, jsonObject);
        features = new ArrayList<>();
    }
}
