package net.erabbit.ble.entity;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by ziv on 2017/4/19.
 */

public class Characteristic implements Serializable{
    public String uuid;
    public String name;
    public ArrayList<String> properties;
}
