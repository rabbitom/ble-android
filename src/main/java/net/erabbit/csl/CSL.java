package net.erabbit.csl;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CSL {

    public static class CSLDecodeReport {
        public Integer length;
        CSLDecodeReport() {
            this.length = 0;
        }
        public void checkLength(byte[] data, int offset, int requiredLength) throws Exception {
            if(data.length >= offset + requiredLength)
                this.length = requiredLength;
            else
                throw new Exception("Data length is not enough");
        }
    }
    public static Number decodeNumber(byte[] data, int offset, JSONObject config, CSLDecodeReport report) throws Exception {
        String numberType = config.getString("numberType");
        Number number;
        switch(numberType) {
            case "uint8":
                if(report != null)
                    report.checkLength(data, offset, 1);
                number = data[offset] & 0xFF;
                break;
            case "uint16be":
                if(report != null)
                    report.checkLength(data, offset, 2);
                number = ((data[offset] & 0xFF) << 8) | (data[offset+1] & 0xFF);
                break;
            case "uint16le":
                if(report != null)
                    report.checkLength(data, offset, 2);
                number = ((data[offset+1] & 0xFF) << 8) | (data[offset] & 0xFF);
                break;
            case "int16be":
                if(report != null)
                    report.checkLength(data, offset, 2);
                number = (short)(((data[offset] & 0xFF) << 8) | (data[offset+1] & 0xFF));
                break;
            case "int16le":
                if(report != null)
                    report.checkLength(data, offset, 2);
                number = (short)(((data[offset+1] & 0xFF) << 8) | (data[offset] & 0xFF));
                break;
            case "uint32be":
                if(report != null)
                    report.checkLength(data, offset, 4);
                number = ((data[offset] & 0xFFL) << 24) | ((data[offset+1] & 0xFFL) << 16) | ((data[offset+2] & 0xFFL) << 8) | (data[offset+3] & 0xFFL);
                break;
            case "uint32le":
                if(report != null)
                    report.checkLength(data, offset, 4);
                number = ((data[offset+3] & 0xFFL) << 24) | ((data[offset+2] & 0xFFL) << 16) | ((data[offset+1] & 0xFFL) << 8) | (data[offset] & 0xFFL);
                break;
            case "int32be":
                if(report != null)
                    report.checkLength(data, offset, 4);
                number = ((data[offset] & 0xFF) << 24) | ((data[offset+1] & 0xFF) << 16) | ((data[offset+2] & 0xFF) << 8) | (data[offset+3] & 0xFF);
                break;
            case "int32le":
                if(report != null)
                    report.checkLength(data, offset, 4);
                number = ((data[offset+3] & 0xFF) << 24) | ((data[offset+2] & 0xFF) << 16) | ((data[offset+1] & 0xFF) << 8) | (data[offset] & 0xFF);
                break;
            case "float32le":
                if(report != null)
                    report.checkLength(data, offset, 4);
                number = ByteBuffer.wrap(data, offset, 4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
                break;
            default:
                throw new Exception("Unknown number type: " + numberType);
        }
        if(config.has("scale"))
            return number.doubleValue() * config.getDouble("scale");
        else
            return number;
    }
    public static byte[] encodeNumber(Number value, JSONObject config) throws Exception {
        Number number = value;
        if(config.has("scale")) {
            double scale = config.getDouble("scale");
            number = value.doubleValue() / scale;
        }
        String numberType = config.getString("numberType");
        switch (numberType) {
            case "uint8":
                return new byte[]{number.byteValue()};
            case "uint16be":
                return new byte[]{(byte) ((number.intValue() >> 8) & 0xFF), (byte) (number.intValue() & 0xFF)};
            case "uint16le":
                return new byte[]{(byte) (number.intValue() & 0xFF), (byte) ((number.intValue() >> 8) & 0xFF)};
            case "int16be":
                return new byte[]{(byte) ((number.shortValue() >> 8) & 0xFF), (byte) (number.shortValue() & 0xFF)};
            case "int16le":
                return new byte[]{(byte) (number.shortValue() & 0xFF), (byte) ((number.shortValue() >> 8) & 0xFF)};
            case "uint32be":
                return new byte[]{(byte) ((number.longValue() >> 24) & 0xFF), (byte) ((number.longValue() >> 16) & 0xFF), (byte) ((number.longValue() >> 8) & 0xFF), (byte) (number.longValue() & 0xFF)};
            case "uint32le":
                return new byte[]{(byte) (number.longValue() & 0xFF), (byte) ((number.longValue() >> 8) & 0xFF), (byte) ((number.longValue() >> 16) & 0xFF), (byte) ((number.longValue() >> 24) & 0xFF)};
            case "int32be":
                return new byte[]{(byte) ((number.intValue() >> 24) & 0xFF), (byte) ((number.intValue() >> 16) & 0xFF), (byte) ((number.intValue() >> 8) & 0xFF), (byte) (number.intValue() & 0xFF)};
            case "int32le":
                return new byte[]{(byte) (number.intValue() & 0xFF), (byte) ((number.intValue() >> 8) & 0xFF), (byte) ((number.intValue() >> 16) & 0xFF), (byte) ((number.intValue() >> 24) & 0xFF)};
            case "float32le": {
                ByteBuffer byteBuffer = ByteBuffer.allocate(4);
                byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
                byteBuffer.putFloat(number.floatValue());
                return byteBuffer.array();
            }
            default:
                throw new Exception("Unknown number type: " + numberType);
        }
    }
    public static String decodeString(byte[] data, int offset, JSONObject config, CSLDecodeReport report) throws Exception {
        if(!config.has("byteLength"))
            throw new Exception("String config must have byteLength");
        int byteLength = config.getInt("byteLength");
        if(report != null)
            report.checkLength(data, offset, byteLength);
        if(config.has("stringEncoding") && config.getString("stringEncoding").equals("hex")) {
            String hexByteConnector = "";
            if(config.has("hexByteConnector"))
                hexByteConnector = config.getString("hexByteConnector");
            StringBuilder stringBuilder = new StringBuilder();
            for(int i=0; i<byteLength; i++) {
                stringBuilder.append(String.format("%02X", data[offset+i] & 0xFF));
                if(i<byteLength-1)
                    stringBuilder.append(hexByteConnector);
            }
            return stringBuilder.toString();
        }
        else {
            while(byteLength > 0 && data[offset + byteLength - 1] == 0)
                byteLength--;
            return new String(data, offset, byteLength);
        }
    }
    public static byte[] decodeBytes(byte[] data, int offset, JSONObject config, CSLDecodeReport report) throws Exception {
        int byteLength;
        if(config.has("byteLength")) {
            byteLength = config.getInt("byteLength");
            if(report != null)
                report.checkLength(data, offset, byteLength);
        }
        else {
            byteLength = data.length - offset;
            if(report != null)
                report.length = byteLength;
        }
        byte[] result = new byte[byteLength];
        System.arraycopy(data, offset, result, 0, byteLength);
        return result;
    }
    public static Boolean decodeBoolean(byte[] data, int offset, CSLDecodeReport report) throws Exception {
        if(report != null)
            report.checkLength(data, offset, 1);
        return data[offset] != 0;
    }
    public static Map<String,Object> decodeObject(byte[] data, int offset, JSONObject config, CSLDecodeReport report) throws Exception {
        if(config.has("byteLength")) {
            int byteLength = config.getInt("byteLength");
            if(report != null)
                report.checkLength(data, offset, byteLength);
        }
        Map<String,Object> result = new HashMap<>();
        JSONArray attributes = config.getJSONArray("attributes");
        int totalLength = 0;
        for(int i=0; i<attributes.length(); i++) {
            JSONObject attribute = attributes.getJSONObject(i);
            String attributeName = attribute.getString("name");
            if(attribute.getString("type").equals("variable")) {
                String typeIndexKey = attribute.getString("typeIndex");
                Object typeIndexValue = result.get(typeIndexKey);
                JSONArray types = attribute.getJSONArray("types");
                for(int t=0; t<types.length(); t++) {
                    JSONObject type = types.getJSONObject(t);
                    if(type.get("index") == typeIndexValue) {
                        attribute = type;
                        break;
                    }
                }
            }
            CSLDecodeReport attributeReport = new CSLDecodeReport();
            Object attributeValue = decode(data, offset + totalLength, attribute, attributeReport);
            result.put(attributeName, attributeValue);
            totalLength += attributeReport.length;
        }
        if(report != null)
            report.length = totalLength;
        return result;
    }
    public static Map<String,Object> decodeBitmask(byte[] data, int offset, JSONObject config, CSLDecodeReport report) throws Exception {
        if(report != null)
            report.checkLength(data, offset, 1);
        Map<String,Object> result = new HashMap<>();
        JSONArray attributes = config.getJSONArray("attributes");
        for(int i=0; i<attributes.length(); i++) {
            JSONObject attribute = attributes.getJSONObject(i);
            String name = attribute.getString("name");
            int mask = attribute.getInt("mask");
            int maskShift = 0;
            while(((1 << maskShift) & mask) == 0)
                maskShift++;
            int attrByte = (data[offset] & mask) >> maskShift;
            Object attrValue = decode(new byte[]{(byte)attrByte}, 0, attribute, null);
            result.put(name, attrValue);
        }
        return result;
    }
    public static Object[] decodeArray(byte[] data, int offset, JSONObject config, CSLDecodeReport report) throws Exception {
        if (!config.has("byteLength"))
            throw new Exception("Array config must have byteLength");
        int byteLength = config.getInt("byteLength");
        if (report != null)
            report.checkLength(data, offset, byteLength);
        ArrayList<Object> result = new ArrayList<>();
        JSONObject arrayItem = config.getJSONObject("arrayItem");
        CSLDecodeReport itemDecodeReport = new CSLDecodeReport();
        int totalLength = 0;
        while (totalLength < byteLength) {
            itemDecodeReport.length = 0;
            Object itemValue = decode(data, offset + totalLength, arrayItem, itemDecodeReport);
            result.add(itemValue);
            totalLength += itemDecodeReport.length;
        }
        return result.toArray();
    }
    public static Object decode(byte[] data, int offset, JSONObject config, CSLDecodeReport report) throws Exception {
        if(offset >= data.length)
            throw new Exception("offset is larger than data length");
        switch(config.getString("type")) {
            case "number":
                return decodeNumber(data, offset, config, report);
            case "string":
                return decodeString(data, offset, config, report);
            case "bytes":
                return decodeBytes(data, offset, config, report);
            case "boolean":
                return decodeBoolean(data, offset, report);
            case "object":
                if(config.has("objectType") && config.getString("objectType").equals("bitmask"))
                    return decodeBitmask(data, offset, config, report);
                else
                    return decodeObject(data, offset, config, report);
            case "array":
                return decodeArray(data, offset, config, report);
            default:
                throw new Exception("Unknown type: " + config.getString("type"));
        }
    }
    public static byte[] encode(Object value, JSONObject config) throws Exception {
        switch(config.getString("type")) {
            case "number":
                return encodeNumber((Number)value, config);
            default:
                throw new Exception("Unknown type: " + config.getString("type"));
        }
    }
}
