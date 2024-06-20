package com.example.sharedbikeclient;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private static final int REQUEST_BLUETOOTH_PERMISSION = 2;
    private static final int REQUEST_ENABLE_BLUETOOTH = 3;
    private static final int REQUEST_BLUETOOTH_CONNECT_PERMISSION = 4;
    private String scannedData;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private InputStream inputStream;

    private static final String TAG = "MainActivity111";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 设置Activity为竖屏
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "设备不支持蓝牙", Toast.LENGTH_SHORT).show();
            finish();
        }

        // 设置扫码按钮的点击事件
        Button scanButton = findViewById(R.id.scan_button);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initiateScan();
            }
        });

        // 设置断开连接按钮的点击事件
        Button disconnectButton = findViewById(R.id.disconnect_button);
        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disconnectBluetooth();
            }
        });

        // 检查并请求所需的权限
        checkAndRequestPermissions();
    }

    private void initiateScan() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setOrientationLocked(true); // 锁定屏幕方向为竖屏
        integrator.setPrompt("请扫描二维码"); // 可选：设置提示信息
        integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES); // 可选：设置条码格式
        integrator.setCaptureActivity(PortraitCaptureActivity.class); // 使用自定义的CaptureActivity
        integrator.initiateScan();
    }

    private void checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
            }, REQUEST_BLUETOOTH_CONNECT_PERMISSION);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "取消扫描", Toast.LENGTH_LONG).show();
            } else {
                // 处理扫描结果
                Toast.makeText(this, "扫描结果: " + result.getContents(), Toast.LENGTH_LONG).show();
                Log.d(TAG, "扫描数据: " + result.getContents());
                scannedData = result.getContents();
                if (checkLocationPermission()) {
                    if (checkBluetoothPermission()) {
                        enableBluetoothAndConnect(scannedData);
                    }
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }

        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if (resultCode == RESULT_OK) {
                connectBluetooth(scannedData);
            } else {
                Toast.makeText(this, "需要启用蓝牙才能进行连接", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
            return false;
        }
        return true;
    }

    private boolean checkBluetoothPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN}, REQUEST_BLUETOOTH_PERMISSION);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限被授予，检查蓝牙权限
                if (checkBluetoothPermission()) {
                    enableBluetoothAndConnect(scannedData);
                }
            } else {
                Toast.makeText(this, "需要位置权限才能进行蓝牙连接", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_BLUETOOTH_PERMISSION || requestCode == REQUEST_BLUETOOTH_CONNECT_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableBluetoothAndConnect(scannedData);
            } else {
                Toast.makeText(this, "需要蓝牙权限才能进行蓝牙连接", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void enableBluetoothAndConnect(String scannedData) {
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // 请求蓝牙连接权限
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_ENABLE_BLUETOOTH);
                return;
            }
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH);
        } else {
            connectBluetooth(scannedData);
        }
    }

    private void connectBluetooth(String scannedData) {
        if (scannedData == null || scannedData.isEmpty()) {
            Toast.makeText(this, "扫描数据无效", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "扫描数据: " + scannedData);

        // 分割数据字符串
        String[] parts = scannedData.split(",");
        Log.d(TAG, "分割数据长度: " + parts.length);
        for (int i = 0; i < parts.length; i++) {
            Log.d(TAG, "分割数据 " + i + ": " + parts[i]);
        }

        if (parts.length < 3) {
            Toast.makeText(this, "扫描数据格式错误", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // 获取MAC地址和UUID
            String macAddress = parts[1].split(":", 2)[1].trim();
            String uuid = parts[2].split(":", 2)[1].trim();
            Log.d(TAG, "MAC 地址: " + macAddress);
            Log.d(TAG, "UUID: " + uuid);

            BluetoothDevice device;
            try {
                Log.d(TAG, "尝试获取远程设备");
                device = bluetoothAdapter.getRemoteDevice(macAddress);
                Log.d(TAG, "远程设备获取成功: " + device.getName());
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "无效的蓝牙地址: " + macAddress, e);
                Toast.makeText(this, "无效的蓝牙地址", Toast.LENGTH_SHORT).show();
                return;
            }

            UUID deviceUUID;
            try {
                deviceUUID = UUID.fromString(uuid);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "无效的UUID: " + uuid, e);
                Toast.makeText(this, "无效的UUID", Toast.LENGTH_SHORT).show();
                return;
            }

            bluetoothSocket = device.createRfcommSocketToServiceRecord(deviceUUID);
            bluetoothAdapter.cancelDiscovery(); // 取消搜索以提高连接速度
            Log.d(TAG, "连接蓝牙设备");
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_CONNECT_PERMISSION);
                return;
            }
            bluetoothSocket.connect(); // 尝试连接
            outputStream = bluetoothSocket.getOutputStream();
            inputStream = bluetoothSocket.getInputStream();
            Toast.makeText(this, "蓝牙连接成功", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "蓝牙连接成功");

            // 开始监听输入流
            new Thread(() -> {
                try {
                    byte[] buffer = new byte[1024];
                    int bytes;
                    while ((bytes = inputStream.read(buffer)) != -1) {
                        String receivedData = new String(buffer, 0, bytes);
                        Log.d(TAG, "收到数据: " + receivedData);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "读取输入流时发生错误", e);
                }
            }).start();

        } catch (IOException e) {
            Log.e(TAG, "蓝牙连接失败: " + e.getMessage(), e);
            Toast.makeText(this, "蓝牙连接失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            try {
                if (bluetoothSocket != null) {
                    bluetoothSocket.close();
                }
            } catch (IOException ex) {
                Log.e(TAG, "关闭蓝牙连接时出错: " + ex.getMessage(), ex);
                Toast.makeText(this, "关闭蓝牙连接时出错: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } catch (SecurityException e) {
            Log.e(TAG, "蓝牙连接失败: 权限不足", e);
            Toast.makeText(this, "蓝牙连接失败: 权限不足", Toast.LENGTH_SHORT).show();
        }
    }

    private void disconnectBluetooth() {
        try {
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
                bluetoothSocket = null;
                outputStream = null;
                inputStream = null;
                Toast.makeText(this, "蓝牙连接已断开", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "蓝牙连接已断开");
            }
        } catch (IOException e) {
            Log.e(TAG, "断开蓝牙连接时发生错误", e);
            Toast.makeText(this, "断开蓝牙连接时发生错误: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
