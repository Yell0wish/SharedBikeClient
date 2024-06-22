package com.example.sharedbikeclient.ui.ride;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.model.*;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.sharedbikeclient.Bike;
import com.example.sharedbikeclient.PortraitCaptureActivity;
import com.example.sharedbikeclient.R;
import com.example.sharedbikeclient.network.ApiService;
import com.example.sharedbikeclient.network.UploadResponse;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import okhttp3.*;
import org.json.JSONException;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import com.amap.api.maps2d.AMapUtils;
import com.amap.api.maps2d.model.LatLng;

public class RideFragment extends Fragment {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private Uri photoURI;
    private String imageUrl;

    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private static final int REQUEST_BLUETOOTH_PERMISSION = 2;
    private static final int REQUEST_ENABLE_BLUETOOTH = 3;
    private static final int REQUEST_BLUETOOTH_CONNECT_PERMISSION = 4;

    private List<LatLng> ridePathPoints = new ArrayList<>();
    private float totalDistance = 0.0f;

    private String scannedData;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private InputStream inputStream;
    private MapView mMapView;
    private AMap aMap;
    private TextView statusText;

    private PolylineOptions polylineOptions;
    private Polyline polyline;
    private static final String TAG = "RideFragment";

    AMapLocationClient mLocationClient = null;
    AMapLocationClientOption mLocationOption = null;

    private List<LatLng> pathPoints = new ArrayList<>();
    private List<Bike> bikeData = new ArrayList<>();

    private List<LatLng> legalAreaPoints = new ArrayList<>();
    private Polygon legalAreaPolygon;

    public RideFragment() {
        // Required empty public constructor
    }

    private void showRepairDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View view = getLayoutInflater().inflate(R.layout.dialog_repair, null);
        builder.setView(view);

        EditText editFaultTitle = view.findViewById(R.id.edit_fault_title);
        EditText editFaultDescription = view.findViewById(R.id.edit_fault_description);
        Spinner spinnerFaultPart = view.findViewById(R.id.spinner_fault_part);
        Button btnTakePhoto = view.findViewById(R.id.btn_take_photo);

        btnTakePhoto.setOnClickListener(v -> {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                Uri photoUri = createImageUri();
                if (photoUri != null) {
                    photoURI = photoUri;
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                }
            }
        });

        builder.setPositiveButton("提交", (dialog, which) -> {
            String faultTitle = editFaultTitle.getText().toString();
            String faultDescription = editFaultDescription.getText().toString();
            String faultPart = spinnerFaultPart.getSelectedItem().toString();

            // 上传图片并获取 URL
            uploadImageAndSendRepair(faultTitle, faultDescription, faultPart);
        });

        builder.setNegativeButton("取消", (dialog, which) -> dialog.dismiss());

        builder.create().show();
    }

    private Uri createImageUri() {
        ContentResolver resolver = getContext().getContentResolver();
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "IMG_" + System.currentTimeMillis());
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);
        return resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
    }

    private void uploadImage(Uri uri) {
        try {
            InputStream inputStream = getContext().getContentResolver().openInputStream(uri);
            byte[] buffer = new byte[inputStream.available()];
            inputStream.read(buffer);
            File file = new File(getContext().getCacheDir(), "temp_image.jpg");
            FileOutputStream outStream = new FileOutputStream(file);
            outStream.write(buffer);

            RequestBody requestBody = RequestBody.create(MediaType.parse("image/*"), file);
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestBody);

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("http://192.168.1.172:5000/")  // 基础 URL 必须以 / 结尾
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            ApiService apiService = retrofit.create(ApiService.class);
            Call<UploadResponse> call = apiService.uploadImage(body);

            call.enqueue(new Callback<UploadResponse>() {
                @Override
                public void onResponse(Call<UploadResponse> call, Response<UploadResponse> response) {
                    if (response.isSuccessful()) {
                        imageUrl = response.body().getImageUrl();
                        Toast.makeText(getContext(), "图片上传成功", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "图片上传成功", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "图片上传失败: " + response.errorBody().toString());
                    }
                }

                @Override
                public void onFailure(Call<UploadResponse> call, Throwable t) {
                    Toast.makeText(getContext(), "图片上传失败", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "图片上传失败: " + t.getMessage(), t);
                }
            });
        } catch (IOException e) {
            Log.e(TAG, "读取文件时发生错误: " + e.getMessage(), e);
        }
    }


    private void uploadImageAndSendRepair(String faultTitle, String faultDescription, String faultPart) {
        if (imageUrl != null) {
            // 创建 JSON 对象
            JSONObject repairData = new JSONObject();
            try {
                repairData.put("description", faultTitle);
                repairData.put("details", faultDescription);
                repairData.put("faultPart", faultPart);
                repairData.put("imageUrl", imageUrl);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            // 发送 HTTP 请求
            String url = "http://your-server-url.com/repair";
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, repairData,
                    response -> {
                        // 处理响应
                        Toast.makeText(getContext(), "报修成功", Toast.LENGTH_SHORT).show();
                    },
                    error -> {
                        // 处理错误
                        Toast.makeText(getContext(), "报修失败", Toast.LENGTH_SHORT).show();
                    });

            // 添加请求到请求队列
            Volley.newRequestQueue(getContext()).add(jsonObjectRequest);
        } else {
            Toast.makeText(getContext(), "请先拍摄故障部位的照片", Toast.LENGTH_SHORT).show();
        }
    }

    private void initLegalArea() {
        legalAreaPoints.add(new LatLng(39.950, 116.340));
        legalAreaPoints.add(new LatLng(39.950, 116.350));
        legalAreaPoints.add(new LatLng(39.960, 116.350));
        legalAreaPoints.add(new LatLng(39.960, 116.340));
    }

    private boolean isLocationInLegalArea(LatLng point) {
        int intersectCount = 0;
        for (int i = 0; i < legalAreaPoints.size() - 1; i++) {
            if (rayCastIntersect(point, legalAreaPoints.get(i), legalAreaPoints.get(i + 1))) {
                intersectCount++;
            }
        }
        // 处理最后一个顶点和第一个顶点之间的边
        if (rayCastIntersect(point, legalAreaPoints.get(legalAreaPoints.size() - 1), legalAreaPoints.get(0))) {
            intersectCount++;
        }
        return (intersectCount % 2) == 1; // odd = inside, even = outside
    }

    private boolean rayCastIntersect(LatLng point, LatLng vertA, LatLng vertB) {
        double aY = vertA.latitude;
        double bY = vertB.latitude;
        double aX = vertA.longitude;
        double bX = vertB.longitude;
        double pY = point.latitude;
        double pX = point.longitude;

        if ((aY > pY && bY > pY) || (aY < pY && bY < pY) || (aX < pX && bX < pX)) {
            return false; // a and b can't both be above or below the point
        }
        double m = (aY - bY) / (aX - bX); // Slope of the line
        double bee = (-aX) * m + aY; // y-intercept of line
        double x = (pY - bee) / m; // Solve for x when y is pY
        return x > pX;
    }


    private void drawLegalArea() {
        // 添加合法区域的坐标点
        legalAreaPoints.add(new LatLng(39.950, 116.340));
        legalAreaPoints.add(new LatLng(39.950, 116.350));
        legalAreaPoints.add(new LatLng(39.960, 116.350));
        legalAreaPoints.add(new LatLng(39.960, 116.340));

        PolygonOptions polygonOptions = new PolygonOptions();
        polygonOptions.addAll(legalAreaPoints);
        polygonOptions.strokeWidth(5) // 边框宽度
                .strokeColor(Color.BLUE) // 边框颜色
                .fillColor(Color.parseColor("#500084d3")); // 填充颜色（浅蓝色，50%透明度）

        legalAreaPolygon = aMap.addPolygon(polygonOptions);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ride, container, false);

        mMapView = view.findViewById(R.id.map);
        mMapView.onCreate(savedInstanceState);
        if (aMap == null) {
            aMap = mMapView.getMap();
        }

        initBikes();
        addBikeMarkers();
        initMyLocation();
        initLegalArea();
        drawLegalArea();

        try {
            initLocation();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // 初始化 BluetoothAdapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(getContext(), "设备不支持蓝牙", Toast.LENGTH_SHORT).show();
            getActivity().finish();
        }

        // 初始化按钮和文本视图
        statusText = view.findViewById(R.id.status_text);
        statusText.setVisibility(View.GONE);
        Button scanButton = view.findViewById(R.id.scan_button);
        Button disconnectButton = view.findViewById(R.id.disconnect_button);
        Button repairButton = view.findViewById(R.id.repair_button);

        // 设置扫码按钮的点击事件
        scanButton.setOnClickListener(v -> initiateScan());

        // 设置断开连接按钮的点击事件
        disconnectButton.setOnClickListener(v -> disconnectBluetooth());

        // 设置报修按钮的点击事件
        repairButton.setOnClickListener(v -> {
            // 实现报修功能
            showRepairDialog();
        });

        // 检查并请求所需的权限
        checkAndRequestPermissions();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mMapView.onSaveInstanceState(outState);
    }

    private void initiateScan() {
        IntentIntegrator integrator = IntentIntegrator.forSupportFragment(this);
        integrator.setOrientationLocked(true);
        integrator.setPrompt("请扫描二维码");
        integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
        integrator.setCaptureActivity(PortraitCaptureActivity.class);
        integrator.initiateScan();
    }

    private void checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(getContext(), Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(getContext(), Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(getContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(getContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
            }, REQUEST_BLUETOOTH_CONNECT_PERMISSION);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == getActivity().RESULT_OK) {
            // 上传图片并获取 URL
            uploadImage(photoURI);
        }
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(getContext(), "取消扫描", Toast.LENGTH_LONG).show();
                statusText.setText("取消扫描");
            } else {
                // 处理扫描结果
                String resultContents = result.getContents();
                Toast.makeText(getContext(), "扫描结果: " + resultContents, Toast.LENGTH_LONG).show();
                Log.d(TAG, "扫描数据: " + resultContents);
                scannedData = resultContents;
                statusText.setText("扫描结果: " + resultContents);
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
            if (resultCode == getActivity().RESULT_OK) {
                connectBluetooth(scannedData);
            } else {
                Toast.makeText(getContext(), "需要启用蓝牙才能进行连接", Toast.LENGTH_SHORT).show();
                statusText.setText("需要启用蓝牙才能进行连接");
            }
        }
    }

    private boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
            return false;
        }
        return true;
    }

    private boolean checkBluetoothPermission() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(getContext(), Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(getContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(getContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN,
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
                if (checkBluetoothPermission()) {
                    enableBluetoothAndConnect(scannedData);
                }
            } else {
                Toast.makeText(getContext(), "需要位置权限才能进行蓝牙连接", Toast.LENGTH_SHORT).show();
                statusText.setText("需要位置权限才能进行蓝牙连接");
            }
        } else if (requestCode == REQUEST_BLUETOOTH_PERMISSION || requestCode == REQUEST_BLUETOOTH_CONNECT_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableBluetoothAndConnect(scannedData);
            } else {
                Toast.makeText(getContext(), "需要蓝牙权限才能进行蓝牙连接", Toast.LENGTH_SHORT).show();
                statusText.setText("需要蓝牙权限才能进行蓝牙连接");
            }
        }
    }

    private void enableBluetoothAndConnect(String scannedData) {
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_ENABLE_BLUETOOTH);
                return;
            }
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH);
        } else {
            connectBluetooth(scannedData);
        }
    }

    private void connectBluetooth(String scannedData) {
        if (scannedData == null || scannedData.isEmpty()) {
            Toast.makeText(getContext(), "扫描数据无效", Toast.LENGTH_SHORT).show();
            statusText.setText("扫描数据无效");
            return;
        }

        Log.d(TAG, "扫描数据: " + scannedData);
        statusText.setText("连接中...");

        String[] parts = scannedData.split(",");
        Log.d(TAG, "分割数据长度: " + parts.length);
        for (int i = 0; i < parts.length; i++) {
            Log.d(TAG, "分割数据 " + i + ": " + parts[i]);
        }

        if (parts.length < 3) {
            Toast.makeText(getContext(), "扫描数据格式错误", Toast.LENGTH_SHORT).show();
            statusText.setText("扫描数据格式错误");
            return;
        }

        try {
            String macAddress = parts[1].split(":", 2)[1].trim();
            String uuid = parts[2].split(":", 2)[1].trim();
            Log.d(TAG, "MAC 地址: " + macAddress);
            Log.d(TAG, "UUID: " + uuid);

            BluetoothDevice device;
            try {
                device = bluetoothAdapter.getRemoteDevice(macAddress);
                Log.d(TAG, "远程设备获取成功: " + device.getName());
                statusText.setText("远程设备获取成功: " + device.getName());
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "无效的蓝牙地址: " + macAddress, e);
                Toast.makeText(getContext(), "无效的蓝牙地址", Toast.LENGTH_SHORT).show();
                statusText.setText("无效的蓝牙地址");
                return;
            }

            UUID deviceUUID;
            try {
                deviceUUID = UUID.fromString(uuid);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "无效的UUID: " + uuid, e);
                Toast.makeText(getContext(), "无效的UUID", Toast.LENGTH_SHORT).show();
                statusText.setText("无效的UUID");
                return;
            }

            bluetoothSocket = device.createRfcommSocketToServiceRecord(deviceUUID);
            bluetoothAdapter.cancelDiscovery();
            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_CONNECT_PERMISSION);
                return;
            }
            bluetoothSocket.connect();
            outputStream = bluetoothSocket.getOutputStream();
            inputStream = bluetoothSocket.getInputStream();
            Toast.makeText(getContext(), "蓝牙连接成功", Toast.LENGTH_SHORT).show();
            statusText.setText("蓝牙连接成功");
            Log.d(TAG, "蓝牙连接成功");

            new Thread(() -> {
                try {
                    byte[] buffer = new byte[1024];
                    int bytes;
                    while ((bytes = inputStream.read(buffer)) != -1) {
                        String receivedData = new String(buffer, 0, bytes);
                        Log.d(TAG, "收到数据: " + receivedData);
                        getActivity().runOnUiThread(() -> {
                            statusText.setText("收到数据: " + receivedData);
                            parseBluetoothData(receivedData);
                        });
                    }
                } catch (IOException e) {
                    Log.e(TAG, "读取输入流时发生错误", e);
                }
            }).start();


        } catch (IOException e) {
            Log.e(TAG, "蓝牙连接失败: " + e.getMessage(), e);
            Toast.makeText(getContext(), "蓝牙连接失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            statusText.setText("蓝牙连接失败: " + e.getMessage());
            try {
                if (bluetoothSocket != null) {
                    bluetoothSocket.close();
                }
            } catch (IOException ex) {
                Log.e(TAG, "关闭蓝牙连接时出错: " + ex.getMessage(), ex);
                Toast.makeText(getContext(), "关闭蓝牙连接时出错: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
                statusText.setText("关闭蓝牙连接时出错: " + ex.getMessage());
            }
        } catch (SecurityException e) {
            Log.e(TAG, "蓝牙连接失败: 权限不足", e);
            Toast.makeText(getContext(), "蓝牙连接失败: 权限不足", Toast.LENGTH_SHORT).show();
            statusText.setText("蓝牙连接失败: 权限不足");
        }
    }

    private void drawPath() {
        if (pathPoints.size() > 1) {
            PolylineOptions polylineOptions = new PolylineOptions();
            polylineOptions.addAll(pathPoints);
            polylineOptions.width(10).color(Color.BLUE);
            aMap.addPolyline(polylineOptions);
        }
    }

    private void parseBluetoothData(String data) {
        try {
            String[] parts = data.split(",");
            double latitude = Double.parseDouble(parts[0].split(":")[1].trim());
            double longitude = Double.parseDouble(parts[1].split(":")[1].trim());
            LatLng latLng = new LatLng(latitude, longitude);
            pathPoints.add(latLng);

            // 检查位置是否在合法区域内
            if (!isLocationInLegalArea(latLng)) {
                Toast.makeText(getContext(), "警告：超出合法区域！", Toast.LENGTH_LONG).show();
            }

            // 将点添加到 PolylineOptions
            polylineOptions.add(latLng);

            // 在地图上添加 Polyline
            if (polyline != null) {
                polyline.remove();
            }
            polyline = aMap.addPolyline(polylineOptions);

            // 调整地图中心位置到最新位置
            aMap.moveCamera(com.amap.api.maps2d.CameraUpdateFactory.newLatLng(latLng));

            // 计算并更新总距离
            if (ridePathPoints.size() > 1) {
                LatLng previousPoint = ridePathPoints.get(ridePathPoints.size() - 2);
                totalDistance += AMapUtils.calculateLineDistance(previousPoint, latLng);
            }
        } catch (Exception e) {
            Log.e(TAG, "解析蓝牙数据时发生错误: " + e.getMessage());
        }
    }


    private void disconnectBluetooth() {
        try {
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
                bluetoothSocket = null;
                outputStream = null;
                inputStream = null;
                Toast.makeText(getContext(), "蓝牙连接已断开", Toast.LENGTH_SHORT).show();
                statusText.setText("蓝牙连接已断开");
                Log.d(TAG, "蓝牙连接已断开");

                // 展示轨迹弹窗
                showTrackDialog();
            }
        } catch (IOException e) {
            Log.e(TAG, "断开蓝牙连接时发生错误", e);
            Toast.makeText(getContext(), "断开蓝牙连接时发生错误: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            statusText.setText("断开蓝牙连接时发生错误: " + e.getMessage());
        }
    }

    private void showTrackDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("骑行轨迹");
        builder.setMessage("展示骑行轨迹动画");
        builder.setMessage("本次骑行里程: " + totalDistance + " 米\n展示骑行轨迹动画");


        builder.setPositiveButton("确定", (dialog, which) -> {
            // 动画展示轨迹
            animateTrack();
            dialog.dismiss();
        });

        builder.setNegativeButton("取消", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void animateTrack() {
        // 轨迹动画逻辑，可以逐步绘制轨迹线，模拟轨迹生成的过程
        Handler handler = new Handler();
        for (int i = 0; i < pathPoints.size(); i++) {
            final int index = i;
            handler.postDelayed(() -> {
                if (index < pathPoints.size()) {
                    polylineOptions.add(pathPoints.get(index));
                    if (polyline != null) {
                        polyline.remove();
                    }
                    polyline = aMap.addPolyline(polylineOptions);
                }
            }, i * 200); // 每个点之间的延迟时间
        }
    }


    private void initBikes() {
        bikeData.add(new Bike("1", "001", true, false, new LatLng(39.95, 116.16)));
        bikeData.add(new Bike("2", "002", false, true, new LatLng(40.23, 116.47)));
        // 添加更多默认数据
    }

    private void addBikeMarkers() {
        for (Bike bike : bikeData) {
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(bike.position);
            markerOptions.title("Bike ID: " + bike.bikeId);
            markerOptions.snippet("Available: " + bike.available + ", In Use: " + bike.inUse);

            // 获取原始位图
            Bitmap originalBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.location_marker);

            // 调整位图大小
            int width = 100;  // 设置宽度为 50 像素
            int height = 100; // 设置高度为 50 像素
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, width, height, false);

            // 设置标记图标
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(resizedBitmap));
            aMap.addMarker(markerOptions);
        }
    }

    private void initMyLocation() {
        MyLocationStyle myLocationStyle = new MyLocationStyle();
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_FOLLOW);
        myLocationStyle.interval(2000); // 每隔2秒进行一次定位
        aMap.setMyLocationStyle(myLocationStyle);
        aMap.setMyLocationEnabled(true); // 启用定位层

        // 初始化 PolylineOptions
        polylineOptions = new PolylineOptions().width(10).color(Color.BLUE);

        // 放大地图到合适的缩放级别
        aMap.moveCamera(com.amap.api.maps2d.CameraUpdateFactory.zoomTo(17)); // 调整到合适的缩放级别
    }

    private void initLocation() throws Exception {
        // 初始化定位
        mLocationClient = new AMapLocationClient(getContext());
        // 设置定位回调监听
        mLocationClient.setLocationListener(new AMapLocationListener() {
            @Override
            public void onLocationChanged(AMapLocation aMapLocation) {
                if (aMapLocation != null) {
                    if (aMapLocation.getErrorCode() == 0) {
                        // 获取当前坐标并输出
                        double latitude = aMapLocation.getLatitude();
                        double longitude = aMapLocation.getLongitude();
                        Log.d(TAG, "当前坐标: " + latitude + ", " + longitude);
                        LatLng latLng = new LatLng(latitude, longitude);
                        // 同时调整缩放比例
//                        aMap.moveCamera(com.amap.api.maps2d.CameraUpdateFactory.changeLatLng(latLng));
//                        aMap.moveCamera(com.amap.api.maps2d.CameraUpdateFactory.zoomTo(15)); // Adjust this value to your desired zoom level
                    } else {
                        // 显示错误信息ErrCode是错误码，errInfo是错误信息，详见错误码表。
                        Log.e(TAG, "定位失败，错误码: " + aMapLocation.getErrorCode() + ", 错误信息: " + aMapLocation.getErrorInfo());
                    }
                }
            }
        });

        // 初始化定位参数
        mLocationOption = new AMapLocationClientOption();
        // 设置定位模式为高精度模式
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        // 设置定位间隔
        mLocationOption.setInterval(2000);
        // 设置定位参数
        mLocationClient.setLocationOption(mLocationOption);
        // 启动定位
        mLocationClient.startLocation();
    }



}
