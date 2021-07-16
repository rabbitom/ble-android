package net.erabbit.ble.utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

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

    private CSLField(String format) {
        this.format = format;
    }

    public static Number parseArray(byte[] data, String format) {
        CSLField csl = new CSLField(format);
        return csl.parseValue(data, 0);
    }

    public Number parseValue(byte[] data, int offset) {
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
            case "T16":
                return parseT16(data, offset);
            case "P40":
                return parseP40(data, offset);
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
        bb = bb.order(ByteOrder.LITTLE_ENDIAN);
        return bb.getFloat(0);
    }

    //temperature
    private float parseT16(byte[] data, int offset) {
        byte digit = data[offset];
        int remainder = byteToUInt(data[offset+1]);
        return (float)digit + (float)remainder / 100;
    }

    //pressure
    private double parseP40(byte[] data, int offset) {
        double result = (double)parseUInt32LE(data, offset);
        double decimalVal = (double)byteToUInt(data[offset+4]);
        if (decimalVal < 10) {
            result += decimalVal / 10;
        } else if (decimalVal < 100) {
            result += decimalVal / 100;
        } else {
            result += decimalVal / 1000;
        }
        return result;
    }
}
