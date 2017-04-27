package net.erabbit.ble.entity;

import net.erabbit.ble.entity.Advertisement;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by ziv on 2017/4/19.
 */

public class DeviceObject implements Serializable{

    public String version;
    public Advertisement advertisement;
    public ArrayList<Service> services;
}
