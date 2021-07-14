package net.erabbit.ble;

import net.erabbit.ble.utils.LogUtil;

import org.json.JSONException;
import org.json.JSONObject;

public class BLESensorFeature {

    public BLESensorFeature(JSONObject jsonObject) {
        try {
            this.name = jsonObject.getString("name");
            this.dimension = jsonObject.getInt("dimension");
        }
        catch(JSONException jsonException) {
            LogUtil.e("feature", jsonException.getMessage());
        }
    }

    private boolean enabled = false;

    public boolean isEnabled() {
        return enabled;
    }

    private String name = "";

    public String getName() {
        return name;
    }

    private int dimension = 0;

    public int getDimension() {
        return dimension;
    }
}
