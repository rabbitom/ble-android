package net.erabbit.csl;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

public class CSL {

    public static class CSLDecodeReport {
        public Integer length;
    }
    public static Number decodeNumber(byte[] data, int offset, JSONObject config, CSLDecodeReport report) throws JSONException {
        String numberType = config.getString("numberType");
        switch(numberType) {
            case "uint8":
                return data[offset] & 0xFF;
            case "uint16be":
                return ((data[offset] & 0xFF) << 8) | (data[offset+1] & 0xFF);
            case "uint16le":
                return ((data[offset+1] & 0xFF) << 8) | (data[offset] & 0xFF);
            case "int16be":
                return (short)(((data[offset] & 0xFF) << 8) | (data[offset+1] & 0xFF));
            case "int16le":
                return (short)(((data[offset+1] & 0xFF) << 8) | (data[offset] & 0xFF));
            case "uint32be":
                return ((data[offset] & 0xFFL) << 24) | ((data[offset+1] & 0xFFL) << 16) | ((data[offset+2] & 0xFFL) << 8) | (data[offset+3] & 0xFFL);
            case "uint32le":
                return ((data[offset+3] & 0xFFL) << 24) | ((data[offset+2] & 0xFFL) << 16) | ((data[offset+1] & 0xFFL) << 8) | (data[offset] & 0xFFL);
            case "int32be":
                return ((data[offset] & 0xFF) << 24) | ((data[offset+1] & 0xFF) << 16) | ((data[offset+2] & 0xFF) << 8) | (data[offset+3] & 0xFF);
            case "int32le":
                return ((data[offset+3] & 0xFF) << 24) | ((data[offset+2] & 0xFF) << 16) | ((data[offset+1] & 0xFF) << 8) | (data[offset] & 0xFF);
            case "float32le":
                return ByteBuffer.wrap(data, offset, 4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        }
        return null;
    }
    public static Map<String,Object> decodeObject(byte[] data, int offset, JSONObject config, CSLDecodeReport report) throws JSONException {
        Map<String,Object> result = new HashMap<>();
        JSONArray attributes = config.getJSONArray("attributes");
        for(int i=0; i<attributes.length(); i++) {
            JSONObject attribute = attributes.getJSONObject(i);
            CSLDecodeReport attributeReport = new CSLDecodeReport();
            Object attributeValue = decode(data, offset, attribute, attributeReport);
            result.put(attribute.getString("name"), attributeValue);
            offset += attributeReport.length;
        }
        return result;
    }
    public static Object decode(byte[] data, int offset, JSONObject config, CSLDecodeReport report) throws JSONException {
        switch(config.getString("type")) {
            case "number":
                return decodeNumber(data, offset, config, report);
            case "object":
                return decodeObject(data, offset, config, report);
        }
        return null;
    }
}
