package net.erabbit;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.util.Log;

import net.erabbit.utils.BleUtility;

import java.util.UUID;

//import cn.cooltools.lifetrack.LifeTrackApp;

public class BluetoothGattOperation {

	public static final int READ_CHARACTERISTIC = 1;
	public static final int WRITE_CHARACTERISTIC = 2;
	public static final int ENABLE_NOTIFICATION = 3;
	public static final int DISABLE_NOTIFICATION = 4;

	public final static UUID UUID_CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

	public int operation;
	public BluetoothGatt gatt;
	public byte[] value;
	BluetoothGattCharacteristic characteristic;

	//构造函数
	public BluetoothGattOperation(
			int operation,
			BluetoothGatt gatt,
			BluetoothGattCharacteristic characteristic,
			byte[] value) {
		this.operation = operation;
		this.gatt = gatt;
		this.characteristic = characteristic;
		this.value = value;
	}
	
	//执行操作
	public boolean Execute() {
		if(characteristic != null) {
			switch(operation)
			{
			case READ_CHARACTERISTIC:
				return gatt.readCharacteristic(characteristic);
			case WRITE_CHARACTERISTIC:
				Log.i("Ble", "write data: " + BleUtility.MakeHexString(value));
		   		characteristic.setValue(value);
		   		return gatt.writeCharacteristic(characteristic);
			case ENABLE_NOTIFICATION:
			{
		   		gatt.setCharacteristicNotification(characteristic, true);
		   		BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID_CLIENT_CHARACTERISTIC_CONFIG);
		   		if(descriptor != null) {
			   		descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
			   		return gatt.writeDescriptor(descriptor);
		   		}
			}
		   		break;
			case DISABLE_NOTIFICATION:
			{
		   		gatt.setCharacteristicNotification(characteristic, false);
		   		BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID_CLIENT_CHARACTERISTIC_CONFIG);
		   		if(descriptor != null) {
			   		descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
			   		return gatt.writeDescriptor(descriptor);
		   		}
			}
				break;
			}
		}
		Log.i("Ble", "operation not executed, type = " + operation);
		return false;
	}
}
