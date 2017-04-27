package net.erabbit.ble;

import android.Manifest;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.view.View;

import net.erabbit.ble.dialog.GeneralAlertDialog;
import net.erabbit.ble.interfaces.BLESearchCallback;
import net.erabbit.ble.utils.LogUtil;

import static android.app.Activity.RESULT_OK;

/**
 * Created by ziv on 2017/4/21.
 * 扫描方法的一个封装，主要添加权限请求与处理
 */

public class ScanFragment extends Fragment {

    private String dialogtitle = "Permission required";
    private String dialogcontent = "We need permission to access coarse location to use bluetooth on this device, setup now?";

    public final String TAG = this.getClass().getSimpleName();
    private BluetoothStateCallback bluetoothStateCallback;
    private BLESearchCallback bleSearchCallback;

    public void setBluetoothStateCallback(BluetoothStateCallback bluetoothStateCallback) {
        this.bluetoothStateCallback = bluetoothStateCallback;
    }

    interface BluetoothStateCallback {
        void onBluetoothEnabled();
    }


    public void setBleSearchCallback(BLESearchCallback bleSearchCallback) {
        this.bleSearchCallback = bleSearchCallback;
    }

    public void tryScan(BluetoothAdapter bluetoothAdapter) {
        //确认蓝牙是否可用，不可用则弹框请求
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 0);
        } else {
            //蓝牙可用,开始扫描
            if (getActivity() != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                    if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
                    } else {
                        //开始扫描
                        bluetoothStateCallback.onBluetoothEnabled();
                    }
                } else {
                    bluetoothStateCallback.onBluetoothEnabled();
                }
            }
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0) {
            LogUtil.i(TAG, "=====resultCode=" + resultCode);
            if (resultCode == RESULT_OK) {
                //蓝牙可用,开始扫描
                LogUtil.i(TAG, "蓝牙可用,开始扫描");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
                    } else {
                        bluetoothStateCallback.onBluetoothEnabled();
                    }
                } else {
                    bluetoothStateCallback.onBluetoothEnabled();
                }

            } else {
                // ToastUtil.showToast("蓝牙不可用");
                if (bleSearchCallback != null) {
                    bleSearchCallback.onSearchError(BLESearchCallback.ERROR_BLUETOOTH_DISABLE, "蓝牙未开启");
                }
                LogUtil.i(TAG, "蓝牙未开启");
            }
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        LogUtil.i(TAG, "===requestCode=" + requestCode);

        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //toPair();
                    if (bluetoothStateCallback != null) {
                        bluetoothStateCallback.onBluetoothEnabled();
                    }

                } else {

                    if (bleSearchCallback != null) {
                        bleSearchCallback.onSearchError(BLESearchCallback.ERROR_NO_BLUETOOTH_PERMISSION, "未授权使用蓝牙权限");
                    }
                    // 判断用户是否 点击了不再提醒。(检测该权限是否还可以申请)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        boolean b = shouldShowRequestPermissionRationale(permissions[0]);
                        if (!b) {
                            //用户点击拒绝给权限，并且点击了不再询问
                            LogUtil.i(TAG, "===不再询问");
                            requestPermissionsDialog();
                        } else {
                            //用户点击拒绝给权限，但并未点击不再询问
                            //Toast.makeText(context, "You will not be able to use the device without permission to access coarse location to use bluetooth", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                break;
            default:
                break;
        }
    }

    private void requestPermissionsDialog() {
        LogUtil.i(TAG, "===requestPermissionsDialog");
        final GeneralAlertDialog dialog = new GeneralAlertDialog(getActivity());
        dialog.showDialog(dialogtitle, dialogcontent, "Later", "OK", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri packageURI = Uri.parse("package:" + getActivity().getPackageName());
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageURI);
                startActivity(intent);
                dialog.dismiss();
            }
        });
    }

}
