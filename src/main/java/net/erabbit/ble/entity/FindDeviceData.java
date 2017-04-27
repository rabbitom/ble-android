package net.erabbit.ble.entity;

import android.content.Intent;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ziv on 2017/4/27.
 */

public class FindDeviceData implements Serializable {
    public String id;
    public byte[] scanRecord;
    public Map<Integer, byte[]> scanRecordMap;
    public boolean hasCalledOnFound;
}
