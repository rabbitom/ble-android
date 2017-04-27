package net.erabbit.ble.entity;

import net.erabbit.ble.entity.Characteristic;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by ziv on 2017/4/19.
 */

public class Service implements Serializable {
    public String uuid;
    public String name;
    public ArrayList<Characteristic> characteristics;
}
