package net.erabbit.ble;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import net.erabbit.ble.utils.LogUtil;

import org.json.JSONArray;
import org.json.JSONException;
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
        try {
            JSONArray servicesArray = jsonObject.getJSONArray("services");
            for (int s = 0; s < servicesArray.length(); s++) {
                JSONObject serviceObject = servicesArray.getJSONObject(s);
                JSONArray characteristicsArray = serviceObject.getJSONArray("characteristics");
                for (int c=0; c<characteristicsArray.length(); c++) {
                    JSONObject characteristicObject = characteristicsArray.getJSONObject(c);
                    if(characteristicObject.getString("function").equals("feature")) {
                        BLESensorFeature feature = new BLESensorFeature(characteristicObject);
                        features.add(feature);
                    }
                }
            }
        }
        catch(JSONException jsonException) {
            LogUtil.e("sensor", jsonException.getMessage());
        }
    }
}
