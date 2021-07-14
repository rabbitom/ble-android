package net.erabbit.ble.utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;

public class CSLField {

    private String name;
    private String format;
    private int byteLength;

    public int getByteLength() {
        return byteLength;
    }

    public CSLField(JSONObject jsonObject) {
        try {
            name = jsonObject.getString("name");
            format = jsonObject.getString("format");
            byteLength = jsonObject.getInt("byteLength");
        }
        catch(JSONException jsonException) {
            LogUtil.e("csl", jsonException.getMessage());
        }
    }

    public Object parseValue(byte[] data, int offset) {
        switch(format) {
            case "UInt8":
                return parseUInt8(data, offset);
            case "UInt16BE":
                return parseUInt16BE(data, offset);
            case "UInt16LE":
                return parseUInt16LE(data, offset);
            case "UInt32BE":
                return parseUInt32BE(data, offset);
            case "UInt32LE":
                return parseUInt32LE(data, offset);
            case "Int32LE":
                return parseInt32LE(data, offset);
            case "Float32":
                return parseFloat32(data, offset);
            default:
                LogUtil.e("csl", "format not supported: " + format);
                return null;
        }
    }

    private int byteToUInt(byte b) {
        return 0x000000ff & b;
    }

    public int parseUInt8(byte[] data, int offset) {
        return byteToUInt(data[offset]);
    }

    private int parseUInt16BE(byte[] data, int offset) {
        int result = 0;
        for (int i = 0; i < 2; i++) {
            result = result << 8;
            result = result | byteToUInt(data[offset + i]);
        }
        return result;
    }

    private int parseUInt16LE(byte[] data, int offset) {
        int result = 0;
        for (int i = 1; i >= 0; i--) {
            result = result << 8;
            result = result | byteToUInt(data[offset + i]);
        }
        return result;
    }

    private int parseInt32LE(byte[] data, int offset) {
        int result = 0;
        for (int i = 3; i >= 0; i--) {
            result = result << 8;
            result = result | byteToUInt(data[offset + i]);
        }
        return result;
    }

    private long parseUInt32BE(byte[] data, int offset) {
        long result = 0;
        for (int i = 0; i < 4; i++) {
            result = result << 8;
            result = result | byteToUInt(data[offset + i]);
        }
        return result;
    }

    private long parseUInt32LE(byte[] data, int offset) {
        long result = 0;
        for (int i = 3; i >= 0; i--) {
            result = result << 8;
            result = result | byteToUInt(data[offset + i]);
        }
        return result;
    }

    private float parseFloat32(byte[] data, int offset) {
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.put(data, offset, 4);
        return bb.getFloat(0);
    }
}
