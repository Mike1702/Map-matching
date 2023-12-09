package com.example.myapplication;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
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

public class MapsActivity extends AppCompatActivity implements LocationListener {

    private MapView mapView;
    private IMapController mapController;
    private LocationManager locationManager;
    private MyLocationNewOverlay myLocationOverlay;
    private TextView coorTextView;

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
                        System.currentTimeMillis());
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

    }
    private void startRecording() {
        if (!isRecording) {
            String directoryPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath();
            csvFile = new File(directoryPath, "location_data.csv");
            try {
                csvWriter = new FileWriter(csvFile, true);
                csvWriter.append("Latitude,Longitude,Time\n"); // Header
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
    private void writeLocationData(double latitude, double longitude, long timestamp) {
        if (isRecording) {
            try {
                // Ghi dữ liệu vào tệp CSV
                csvWriter.append(String.format(Locale.getDefault(), "%s,%s,%s\n", latitude, longitude, getFormattedTime(timestamp)));
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
            updateTextView(currentLocation.getLatitude(), currentLocation.getLongitude(), System.currentTimeMillis());
            mapView.invalidate(); // Cập nhật lại bản đồ
        } else {
            // Xử lý khi không thể lấy được vị trí
            Toast.makeText(this, "Không thể lấy được vị trí hiện tại.", Toast.LENGTH_SHORT).show();
        }
    }
    private void updateTextView(double latitude, double longitude, long timestamp) {
        String formattedTime = getFormattedTime(timestamp);
        String text = String.format("Latitude: %.7f\nLongitude: %.7f\nTime (UTC): %s", latitude, longitude, formattedTime);
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
}