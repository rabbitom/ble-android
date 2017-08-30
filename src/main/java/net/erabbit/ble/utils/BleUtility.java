package net.erabbit.ble.utils;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Created by ziv on 2017/4/18.
 */

public class BleUtility {


    public static UUID UUIDFromShort(String shortStr) {
        return UUID.fromString("0000" + shortStr + "-0000-1000-8000-00805f9b34fb");
    }

    //-------------------------------------------
    private static HashMap<Integer, String> serviceTypes = new HashMap();

    static {
        // Sample Services.
        serviceTypes.put(BluetoothGattService.SERVICE_TYPE_PRIMARY, "PRIMARY");
        serviceTypes.put(BluetoothGattService.SERVICE_TYPE_SECONDARY, "SECONDARY");
    }

    public static String getServiceType(int type) {
        return serviceTypes.get(type);
    }

    //-------------------------------------------
    private static HashMap<UUID, String> serviceNames = new HashMap<>();

    static {
        // https://developer.bluetooth.org/gatt/services/Pages/ServicesHome.aspx
        serviceNames.put(UUIDFromShort("1800"), "Generic Access");
        serviceNames.put(UUIDFromShort("1801"), "Generic Attribute");
        serviceNames.put(UUIDFromShort("1802"), "Immediate Alert");
        serviceNames.put(UUIDFromShort("1803"), "Link Loss");
        serviceNames.put(UUIDFromShort("1804"), "Tx Power");
        serviceNames.put(UUIDFromShort("1805"), "Current Time Service");
        serviceNames.put(UUIDFromShort("1806"), "Reference Time Update Service");
        serviceNames.put(UUIDFromShort("1807"), "Next DST Change Service");
        serviceNames.put(UUIDFromShort("1808"), "Glucose");
        serviceNames.put(UUIDFromShort("1809"), "Health Thermometer");
        serviceNames.put(UUIDFromShort("180A"), "Device Information");
        serviceNames.put(UUIDFromShort("180D"), "Heart Rate");
        serviceNames.put(UUIDFromShort("180E"), "Phone Alert Status Service");
        serviceNames.put(UUIDFromShort("180F"), "Battery Service");
        serviceNames.put(UUIDFromShort("1810"), "Blood Pressure");
        serviceNames.put(UUIDFromShort("1811"), "Alert Notification Service");
        serviceNames.put(UUIDFromShort("1812"), "Human Interface Device");
        serviceNames.put(UUIDFromShort("1813"), "Scan Parameters");
        serviceNames.put(UUIDFromShort("1814"), "Running Speed and Cadence");
        serviceNames.put(UUIDFromShort("1815"), "Automation IO");
        serviceNames.put(UUIDFromShort("1816"), "Cycling Speed and Cadence");
        serviceNames.put(UUIDFromShort("1818"), "Cycling Power");
        serviceNames.put(UUIDFromShort("1819"), "Location and Navigation");
        serviceNames.put(UUIDFromShort("181A"), "Environmental Sensing");
        serviceNames.put(UUIDFromShort("181B"), "Body Composition");
        serviceNames.put(UUIDFromShort("181C"), "User Data");
        serviceNames.put(UUIDFromShort("181D"), "Weight Scale");
        serviceNames.put(UUIDFromShort("181E"), "Bond Management");
        serviceNames.put(UUIDFromShort("181F"), "Continuous Glucose Monitoring");
        serviceNames.put(UUIDFromShort("1820"), "Internet Protocol Support");
        serviceNames.put(UUIDFromShort("1821"), "Indoor Positioning");
        serviceNames.put(UUIDFromShort("1822"), "Pulse Oximeter");
    }

    public static String getServiceName(UUID uuid) {
        String serviceName = serviceNames.get(uuid);
        return (serviceName != null) ? serviceName : "Unknown Service";
    }

    //-------------------------------------------
    private static HashMap<Integer, String> charPermissions = new HashMap();

    static {
        charPermissions.put(0, "UNKNOW");
        charPermissions.put(BluetoothGattCharacteristic.PERMISSION_READ, "READ");
        charPermissions.put(BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED, "READ_ENCRYPTED");
        charPermissions.put(BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM, "READ_ENCRYPTED_MITM");
        charPermissions.put(BluetoothGattCharacteristic.PERMISSION_WRITE, "WRITE");
        charPermissions.put(BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED, "WRITE_ENCRYPTED");
        charPermissions.put(BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM, "WRITE_ENCRYPTED_MITM");
        charPermissions.put(BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED, "WRITE_SIGNED");
        charPermissions.put(BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED_MITM, "WRITE_SIGNED_MITM");
    }

    public static String getCharPermission(int permission) {
        return getHashMapValue(charPermissions, permission);
    }

    //-------------------------------------------
    private static HashMap<Integer, String> charProperties = new HashMap();

    static {

        charProperties.put(BluetoothGattCharacteristic.PROPERTY_BROADCAST, "BROADCAST");
        charProperties.put(BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS, "EXTENDED_PROPS");
        charProperties.put(BluetoothGattCharacteristic.PROPERTY_INDICATE, "INDICATE");
        charProperties.put(BluetoothGattCharacteristic.PROPERTY_NOTIFY, "NOTIFY");
        charProperties.put(BluetoothGattCharacteristic.PROPERTY_READ, "READ");
        charProperties.put(BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE, "SIGNED_WRITE");
        charProperties.put(BluetoothGattCharacteristic.PROPERTY_WRITE, "WRITE");
        charProperties.put(BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE, "WRITE_NO_RESPONSE");
    }

    public static String getCharPropertie(int property) {
        return getHashMapValue(charProperties, property);
    }

    //--------------------------------------------------------------------------
    private static HashMap<Integer, String> descPermissions = new HashMap();

    static {
        descPermissions.put(0, "UNKNOW");
        descPermissions.put(BluetoothGattDescriptor.PERMISSION_READ, "READ");
        descPermissions.put(BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED, "READ_ENCRYPTED");
        descPermissions.put(BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED_MITM, "READ_ENCRYPTED_MITM");
        descPermissions.put(BluetoothGattDescriptor.PERMISSION_WRITE, "WRITE");
        descPermissions.put(BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED, "WRITE_ENCRYPTED");
        descPermissions.put(BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED_MITM, "WRITE_ENCRYPTED_MITM");
        descPermissions.put(BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED, "WRITE_SIGNED");
        descPermissions.put(BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED_MITM, "WRITE_SIGNED_MITM");
    }

    public static String getDescPermission(int property) {
        return getHashMapValue(descPermissions, property);
    }

    //--------------------------------------------------------------------------
    private static HashMap<Integer, String> connStatus = new HashMap();

    static {
        connStatus.put(BluetoothGatt.STATE_CONNECTED, "CONNECTED");
        connStatus.put(BluetoothGatt.STATE_CONNECTING, "CONNECTING");
        connStatus.put(BluetoothGatt.STATE_DISCONNECTED, "DISCONNECTED");
        connStatus.put(BluetoothGatt.STATE_DISCONNECTING, "DISCONNECTING");
    }

    public static String getConnectionState(int state) {
        return getHashMapValue(connStatus, state);
    }

    private static String getHashMapValue(HashMap<Integer, String> hashMap, int number) {
        String result = hashMap.get(number);
        if (TextUtils.isEmpty(result)) {
            List<Integer> numbers = getElement(number);
            result = "";
            for (int i = 0; i < numbers.size(); i++) {
                result += hashMap.get(numbers.get(i)) + "|";
            }
        }
        return result;
    }

    /**
     * 位运算结果的反推函数10 -> 2 | 8;
     */
    static private List<Integer> getElement(int number) {
        List<Integer> result = new ArrayList<Integer>();
        for (int i = 0; i < 32; i++) {
            int b = 1 << i;
            if ((number & b) > 0)
                result.add(b);
        }

        return result;
    }


    public static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

    public static String bytesToHexStringReversal(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = src.length - 1; i >= 0; i--) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }


    public static String MakeHexString(byte[] buffer) {
        if (buffer == null)
            return null;
        return MakeHexString(buffer, 0, buffer.length, " ");
    }

    public static String MakeHexString(byte[] buffer, int start, int len) {
        return MakeHexString(buffer, start, len, " ");
    }

    public static String MakeHexString(byte[] buffer, int start, int len, String gap) {
        String hexStr = "";
        for (int i = 0; i < len; i++)
            hexStr = hexStr.concat(String.format("%1$02X", buffer[start + i]) + gap);
        return hexStr;
    }

    public static byte[] fromHexString(String hexStr) {
        ArrayList<Integer> ints = new ArrayList<Integer>();
        char[] hexChars = hexStr.toCharArray();
        int hValue = -1;
        for (int i = 0; i < hexChars.length; i++) {
            int value = fromHexChar(hexChars[i]);
            if (hValue == -1)
                hValue = value;
            else if (value != -1) {
                ints.add(hValue * 16 + value);
                hValue = -1;
            }
        }
        int intCount = ints.size();
        if (intCount > 0) {
            byte[] bytes = new byte[intCount];
            for (int i = 0; i < intCount; i++)
                bytes[i] = toByte(ints.get(i));
            return bytes;
        } else
            return null;
    }

    protected static int fromHexChar(char ch) {
        if ((ch >= '0') && (ch <= '9'))
            return ch - '0';
        else if ((ch >= 'a') && (ch <= 'f'))
            return ch - 'a' + 10;
        else if ((ch >= 'A') && (ch <= 'F'))
            return ch - 'A' + 10;
        else
            return -1;
    }

    public static byte toByte(int x) {
        return (byte) (x & 0x000000ff);
    }

    public static int toInt(byte b) {
        return 0x000000ff & b;
    }

    public static int toIntLE(byte lByte, byte hByte) {
        return ((lByte & 0x000000ff) | (hByte << 8) & 0x0000ff00);
    }

    public static int toIntBE(byte hByte, byte lByte) {
        return ((lByte & 0x000000ff) | (hByte << 8) & 0x0000ff00);
    }

    public static int toIntLE(byte[] bytes, int offset, int len) {
        int value = 0;
        for (int i = len - 1; i >= 0; i--) {//低位在前
            value = value << 8;
            value = value | toInt(bytes[offset + i]);
        }
        return value;
    }

    public static int toIntBE(byte[] bytes, int offset, int len) {
        int value = 0;
        for (int i = 0; i < len; i++) {
            value = value << 8;//高位在前
            value = value | toInt(bytes[offset + i]);
        }
        return value;
    }

    public static long toLongLE(byte[] bytes, int offset, int len) {
        long value = 0;
        for (int i = len - 1; i >= 0; i--) {
            value = value << 8;
            value = value | toInt(bytes[offset + i]);
        }
        return value;
    }

    public static long toLongBE(byte[] bytes, int offset, int len) {
        long value = 0;
        for (int i = 0; i < len; i++) {
            value = value << 8;//高位在前
            value = value | toInt(bytes[offset + i]);
        }
        return value;
    }

    public static UUID toUUIDBE(byte[] bytes) {
        return toUUIDBE(bytes, 0);
    }

    public static UUID toUUIDBE(byte[] bytes, int offset) {
        if (offset + 16 <= bytes.length) {
            long higherHalf = BleUtility.toLongBE(bytes, offset, 8);
            long lowerHalf = BleUtility.toLongBE(bytes, offset + 8, 8);
            return new UUID(higherHalf, lowerHalf);
        }
        return null;
    }

    public static UUID toUUIDLE(byte[] bytes) {
        return toUUIDLE(bytes, 0);
    }

    public static UUID toUUIDLE(byte[] bytes, int offset) {
        if (offset + 16 <= bytes.length) {
            long lowerHalf = BleUtility.toLongLE(bytes, offset, 8);
            long higherHalf = BleUtility.toLongLE(bytes, offset + 8, 8);
            return new UUID(higherHalf, lowerHalf);
        }
        return null;
    }
/*
    public static int[] toIntArrayLE(byte[] bytes, int offset, int len, int count) {
        int[] array = new int[count];
        for(int i=0; i<count; i++) {
            array[i] = CoolUtility.toIntLE(bytes, offset, len);
            offset += len;
        }
        return array;
    }
*/

}
