package com.example.myapplication;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.SensorListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapAdapter;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class MapsActivity extends AppCompatActivity implements LocationListener, SensorEventListener  {

    private MapView mapView;
    private IMapController mapController;
    private LocationManager locationManager;
    private MyLocationNewOverlay myLocationOverlay;
    private TextView coorTextView;
    private SensorManager sensorManager;
    private Sensor accelerometerSensor;
    private Sensor gyroscopeSensor;
    private Sensor magnetometerSensor;
    private float[] accelerometerValues = new float[3];
    private float[] gyroscopeValues = new float[3];
    private float[] magnetometerValues = new float[3];
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private boolean isRecording = false;
    private File csvFile;
    private FileWriter csvWriter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Thiết lập thư viện OSMDroid
        Configuration.getInstance().load(this, getPreferences(Context.MODE_PRIVATE));
        setContentView(R.layout.activity_maps);
        Button startButton = findViewById(R.id.btnStart);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startRecording();
            }
        });
        Button stopButton = findViewById(R.id.btnStop);
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRecording();
            }
        });
        Button updateLocationButton = findViewById(R.id.updateLocationButton);
        updateLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateLocationAndMoveCamera();
                writeLocationData(myLocationOverlay.getMyLocation().getLatitude(),
                        myLocationOverlay.getMyLocation().getLongitude(),
                        System.currentTimeMillis(),
                        accelerometerValues[0], accelerometerValues[1], accelerometerValues[2],
                        gyroscopeValues[0], gyroscopeValues[1], gyroscopeValues[2],
                        magnetometerValues[0], magnetometerValues[1], magnetometerValues[2]);
            }

        });
        // Thiết lập bản đồ
        mapView = findViewById(R.id.map);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        mapController = mapView.getController();
        mapController.setZoom(15.0);

        // Thiết lập đánh dấu vị trí người dùng
        myLocationOverlay = new MyLocationNewOverlay(mapView);
        myLocationOverlay.enableMyLocation();
        myLocationOverlay.enableFollowLocation();
        mapView.getOverlays().add(myLocationOverlay);

        // Thiết lập sự kiện scroll và zoom để ngừng theo dõi vị trí người dùng khi người dùng thao tác trên bản đồ
        mapView.addMapListener(new MapAdapter() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                myLocationOverlay.disableFollowLocation();
                return super.onScroll(event);
            }

            @Override
            public boolean onZoom(ZoomEvent event) {
                myLocationOverlay.disableFollowLocation();
                return super.onZoom(event);
            }
        });

        // Kiểm tra và yêu cầu quyền truy cập vị trí
        if (checkLocationPermission()) {
        }
        coorTextView = findViewById(R.id.textViewCoordinates);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        magnetometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        if (accelerometerSensor != null) {
            sensorManager.registerListener((SensorEventListener) this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (gyroscopeSensor != null) {
            sensorManager.registerListener((SensorEventListener) this, gyroscopeSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (magnetometerSensor != null) {
            sensorManager.registerListener((SensorEventListener) this, magnetometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }
    private void startRecording() {
        if (!isRecording) {
            String directoryPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath();
            csvFile = new File(directoryPath, "location_data.csv");
            try {
                csvWriter = new FileWriter(csvFile, true);
                csvWriter.append("Latitude,Longitude,Time," +
                        "AccelerometerX,AccelerometerY,AccelerometerZ," +
                        "GyroscopeX,GyroscopeY,GyroscopeZ," +
                        "MagnetometerX,MagnetometerY,MagnetometerZ\n"); // Header
                isRecording = true;

            } catch (Exception e) {
                e.printStackTrace();
                // Xử lý lỗi khi không thể khởi tạo tệp
                Toast.makeText(this, "Không thể khởi tạo tệp ghi.", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Ghi thông báo nếu đang trong quá trình ghi
            Toast.makeText(this, "Đang ghi dữ liệu...", Toast.LENGTH_SHORT).show();
        }
    }
    private void stopRecording() {
        if (isRecording) {
            try {
                csvWriter.close();
                isRecording = false;
                Toast.makeText(this, "Ngừng ghi dữ liệu.", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
                // Xử lý lỗi khi không thể đóng tệp
                Toast.makeText(this, "Không thể đóng tệp ghi.", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Ghi thông báo nếu không trong quá trình ghi
            Toast.makeText(this, "Không có dữ liệu ghi.", Toast.LENGTH_SHORT).show();
        }
    }
    private void writeLocationData(double latitude, double longitude, long timestamp,
                                   float accelerometerX, float accelerometerY, float accelerometerZ,
                                   float gyroscopeX, float gyroscopeY, float gyroscopeZ,
                                   float magnetometerX, float magnetometerY, float magnetometerZ) {
        if (isRecording) {
            try {
                // Ghi dữ liệu vào tệp CSV
                csvWriter.append(String.format(Locale.getDefault(),
                        "%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
                        latitude, longitude, getFormattedTime(timestamp),
                        accelerometerX, accelerometerY, accelerometerZ,
                        gyroscopeX, gyroscopeY, gyroscopeZ,
                        magnetometerX, magnetometerY, magnetometerZ));
            } catch (IOException e) {
                e.printStackTrace();
                // Xử lý lỗi khi không thể ghi dữ liệu
                Toast.makeText(this, "Không thể ghi dữ liệu vào tệp.", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Ghi thông báo nếu không trong quá trình ghi
            Toast.makeText(this, "Chưa bắt đầu ghi dữ liệu.", Toast.LENGTH_SHORT).show();
        }
    }
    private boolean checkLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            // Nếu quyền chưa được cấp, yêu cầu quyền từ người dùng
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    // Yêu cầu cập nhật vị trí từ LocationManager

    private void updateLocationAndMoveCamera() {
        if (myLocationOverlay != null && myLocationOverlay.getMyLocation() != null) {
            GeoPoint currentLocation = myLocationOverlay.getMyLocation();
            mapController.setCenter(currentLocation);

            // Đánh dấu vị trí mới trên bản đồ
            Marker marker = new Marker(mapView);
            marker.setPosition(currentLocation);
            mapView.getOverlays().add(marker);
            updateTextView(myLocationOverlay.getMyLocation().getLatitude(),
                    myLocationOverlay.getMyLocation().getLongitude(),
                    System.currentTimeMillis(),
                    accelerometerValues[0], accelerometerValues[1], accelerometerValues[2],
                    gyroscopeValues[0], gyroscopeValues[1], gyroscopeValues[2],
                    magnetometerValues[0], magnetometerValues[1], magnetometerValues[2]);
            mapView.invalidate(); // Cập nhật lại bản đồ
        } else {
            // Xử lý khi không thể lấy được vị trí
            Toast.makeText(this, "Không thể lấy được vị trí hiện tại.", Toast.LENGTH_SHORT).show();
        }
    }
    private void updateTextView(double latitude, double longitude, long timestamp,
                                float accelerometerX, float accelerometerY, float accelerometerZ,
                                float gyroscopeX, float gyroscopeY, float gyroscopeZ,
                                float magnetometerX, float magnetometerY, float magnetometerZ) {
        String formattedTime = getFormattedTime(timestamp);
        String text = String.format(Locale.getDefault(),
                "Latitude: %.7f\nLongitude: %.7f\nTime (UTC): %s\n" +
                        "Accelerometer: X=%.2f, Y=%.2f, Z=%.2f\n" +
                        "Gyroscope: X=%.2f, Y=%.2f, Z=%.2f\n" +
                        "Magnetometer: X=%.2f, Y=%.2f, Z=%.2f",
                latitude, longitude, formattedTime,
                accelerometerX, accelerometerY, accelerometerZ,
                gyroscopeX, gyroscopeY, gyroscopeZ,
                magnetometerX, magnetometerY, magnetometerZ);
        coorTextView.setText(text);
    }

    private String getFormattedTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date resultDate = new Date(timestamp);
        return sdf.format(resultDate);
    }
    public void onLocationChanged(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        GeoPoint geoPoint = new GeoPoint(latitude, longitude);
        mapController.setCenter(geoPoint);

        // Đánh dấu vị trí mới trên bản đồ
        Marker marker = new Marker(mapView);
        marker.setPosition(geoPoint);
        mapView.getOverlays().add(marker);

        mapView.invalidate(); // Cập nhật lại bản đồ
    }
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelerometerValues, 0, 3);
        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            System.arraycopy(event.values, 0, gyroscopeValues, 0, 3);
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magnetometerValues, 0, 3);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {
                Toast.makeText(this, "Ứng dụng cần quyền truy cập vị trí để hoạt động.", Toast.LENGTH_SHORT).show();
            }
        }
    }
    protected void onResume() {
        super.onResume();
        if (sensorManager != null) {
            if (accelerometerSensor != null) {
                sensorManager.registerListener((SensorEventListener) this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
            if (gyroscopeSensor != null) {
                sensorManager.registerListener((SensorEventListener) this, gyroscopeSensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
            if (magnetometerSensor != null) {
                sensorManager.registerListener((SensorEventListener) this, magnetometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }
    }
    protected void onPause() {
        super.onPause();
        unregisterSensorListeners();
    }
    private void unregisterSensorListeners() {
        if (sensorManager != null) {
            if (accelerometerSensor != null) {
                sensorManager.unregisterListener(this, accelerometerSensor);
            }
            if (gyroscopeSensor != null) {
                sensorManager.unregisterListener(this, gyroscopeSensor);
            }
            if (magnetometerSensor != null) {
                sensorManager.unregisterListener(this, magnetometerSensor);
            }
        }
    }
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Xử lý sự kiện khi độ chính xác của cảm biến thay đổi (nếu cần)
    }
}