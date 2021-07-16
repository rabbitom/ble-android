package net.erabbit.ble;

import net.erabbit.ble.utils.CSLField;
import net.erabbit.ble.utils.LogUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class BLESensorFeature {

    public BLESensorFeature(JSONObject jsonObject) {
        try {
            name = jsonObject.getString("name");
        }
        catch(JSONException jsonException) {
            LogUtil.e("feature", "no name");
        }

        try {
            dimension = jsonObject.getInt("dimension");
        }
        catch(JSONException jsonException) {
            dimension = 1;
        }
        values = new Object[dimension];

        try {
            this.unit = jsonObject.getString("unit");
        }
        catch(JSONException jsonException) {
            LogUtil.e("feature", "no unit");
        }

        try {
            JSONArray fieldsArray = jsonObject.getJSONArray("fields");
            fields = new CSLField[fieldsArray.length()];
            for(int i=0; i<fieldsArray.length(); i++) {
                JSONObject fieldObject = fieldsArray.getJSONObject(i);
                fields[i] = new CSLField(fieldObject);
            }
        }
        catch(JSONException jsonException) {
            LogUtil.e("feature", jsonException.getMessage());
        }

        try {
            JSONArray conversionsArray = jsonObject.getJSONArray("conversions");
            conversions = new Conversion[conversionsArray.length()];
            for(int i=0; i<conversionsArray.length(); i++) {
                JSONObject conversionObject = conversionsArray.getJSONObject(i);
                conversions[i] = new Conversion(conversionObject);
            }
        }
        catch(JSONException jsonException) {
            LogUtil.e("feature", jsonException.getMessage());
        }
    }

    private boolean enabled = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean value) {
        enabled = value;
    }

    private String name = "";

    public String getName() {
        return name;
    }

    private int dimension = 0;

    public int getDimension() {
        return dimension;
    }

    private String unit;

    private Object[] values;

    public Object[] getValues() {
        return values;
    }

    public String getValueString() {
        if(values == null)
            return "";
        StringBuilder result = new StringBuilder();
        for (Object value : values) {
            result.append((result.length() == 0) ? "" : ",").append(String.format("%s", value));
        }
        if(unit != null)
            result.append(" ").append(unit);
        return result.toString();
    }

    private CSLField fields[];

    private class Conversion {

        private String operator;
        private String format;
        private byte[] value;

        public Conversion(JSONObject jsonObject) {
            try {
                operator = jsonObject.getString("operator");
                format = jsonObject.getString("format");
                JSONArray valueArray = jsonObject.getJSONArray("value");
                value = new byte[valueArray.length()];
                for(int i=0; i<valueArray.length(); i++)
                    value[i] = (byte)valueArray.getInt(i);
            }
            catch(JSONException jsonException) {
                LogUtil.e("conversion", jsonException.getMessage());
            }
        }

        public Number convert(Number src) {
            if(!"divide".equals(operator)) {
                LogUtil.e("conversion", "not supported: " + operator);
                return src;
            }
            Number divideBy = CSLField.parseArray(value, format);
            return src.floatValue() / divideBy.floatValue();
        }
    }

    private Conversion[] conversions;

    public boolean parseData(byte[] data) {
        if(fields == null)
            return false;
        int v = 0, offset = 0;
        for (CSLField field : fields) {
            Number value = field.parseValue(data, offset);
            if (value != null) {
                if(conversions != null) {
                    for(Conversion conversion : conversions)
                        value = conversion.convert(value);
                }
                values[v++] = value;
            }
            offset += field.getByteLength();
        }
        return v > 0;
    }
}
