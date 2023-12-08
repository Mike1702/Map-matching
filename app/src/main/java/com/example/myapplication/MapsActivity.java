package com.example.myapplication;
import static androidx.core.location.LocationManagerCompat.getCurrentLocation;
import androidx.fragment.app.FragmentActivity;
import androidx.core.app.ActivityCompat;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.myapplication.databinding.ActivityMapsBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;
import android.content.Context;

import android.widget.TextView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;
import java.io.FileOutputStream;
import com.opencsv.CSVWriter;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;



public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private static final int LOCATION_REQUEST_CODE = 101;
    private TextView textViewCoordinates;
    private File dataFile;
    private CSVWriter  writer;
    private boolean isRecording = false;
    private Button btnStart, btnStop;
    private SensorManager sensorManager;
    private Sensor accelerometerSensor, gyroscopeSensor, magnetometerSensor;
    private LocationManager locationManager;
    private Handler handler = new Handler();
    private static final long UPDATE_INTERVAL = 10000;
    private Polyline currentPolyline;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps_text);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        textViewCoordinates = findViewById(R.id.textViewCoordinates);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        magnetometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        startLocationUpdates();
        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop);

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startRecording();
                handler.postDelayed(updateRunnable, UPDATE_INTERVAL);
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRecording();
                handler.removeCallbacks(updateRunnable);

            }
        });
    }
    private Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRecording) {
                handler.postDelayed(this, UPDATE_INTERVAL);
            }
        }
    };
    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
            return;
        }
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL)
                .setFastestInterval(UPDATE_INTERVAL);

        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (android.location.Location location : locationResult.getLocations()) {
                    updateCoordinates(location);

                }
            }
        };

        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    private void updateCoordinates(android.location.Location location) {
        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        //mMap.addMarker(new MarkerOptions().position(currentLatLng).title("You are here"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f));

        if (currentPolyline != null) {
            List<LatLng> points = currentPolyline.getPoints();
            points.add(currentLatLng);
            currentPolyline.setPoints(points);
        } else {
            PolylineOptions polylineOptions = new PolylineOptions()
                    .color(Color.BLUE)
                    .width(5)
                    .add(currentLatLng);
            currentPolyline = mMap.addPolyline(polylineOptions);
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String utcTime = sdf.format(new Date(System.currentTimeMillis()));

        textViewCoordinates.setText("Latitude: " + location.getLatitude() + ", Longitude: " + location.getLongitude() +
                "\nLast updated: " + utcTime +
                "\nAccelerometer (X, Y, Z): " + accelerometerX + ", " + accelerometerY + ", " + accelerometerZ +
                "\nGyroscope (X, Y, Z): " + gyroscopeX + ", " + gyroscopeY + ", " + gyroscopeZ +
                "\nMagnetometer (X, Y, Z): " + magnetometerX + ", " + magnetometerY + ", " + magnetometerZ);
        recordLocationData(location);
    }
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }

    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            }
        }
    }

    private void startRecording() {
        isRecording = true;
        // Tạo file để ghi dữ liệu
        String directoryPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath();
        File dataFile = new File(directoryPath, "location_data.csv");

        try {
            writer = new CSVWriter(new FileWriter(dataFile, true));
            if (dataFile.length() == 0) {
                String[] header = {
                        "Latitude",
                        "Longitude",
                        "Time",
                        "AccelerometerX",
                        "AccelerometerY",
                        "AccelerometerZ",
                        "GyroscopeX",
                        "GyroscopeY",
                        "GyroscopeZ",
                        "MagnetometerX",
                        "MagnetometerY",
                        "MagnetometerZ"
                };
                writer.writeNext(header);
            }
            Toast.makeText(this, "Bắt đầu ghi", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void stopRecording() {
        isRecording = false;
        try {
            if (writer != null) {
                writer.close();
                Toast.makeText(this, "Đã ngừng ghi dữ liệu", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void recordLocationData(android.location.Location location) {
        if (isRecording) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            String utcTime = sdf.format(new Date(System.currentTimeMillis()));

            String[] data = {
                    String.valueOf(location.getLatitude()),
                    String.valueOf(location.getLongitude()),
                    utcTime,
                    String.valueOf(accelerometerX),
                    String.valueOf(accelerometerY),
                    String.valueOf(accelerometerZ),
                    String.valueOf(gyroscopeX),
                    String.valueOf(gyroscopeY),
                    String.valueOf(gyroscopeZ),
                    String.valueOf(magnetometerX),
                    String.valueOf(magnetometerY),
                    String.valueOf(magnetometerZ)
            };
            try {
                if (writer != null) {
                    writer.writeNext(data);  // Sửa đổi dòng này
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    private float accelerometerX, accelerometerY, accelerometerZ;
    private float gyroscopeX, gyroscopeY, gyroscopeZ;
    private float magnetometerX, magnetometerY, magnetometerZ;
    private SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            switch (event.sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    accelerometerX = event.values[0];
                    accelerometerY = event.values[1];
                    accelerometerZ = event.values[2];
                    break;
                case Sensor.TYPE_GYROSCOPE:
                    gyroscopeX = event.values[0];
                    gyroscopeY = event.values[1];
                    gyroscopeZ = event.values[2];
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    magnetometerX = event.values[0];
                    magnetometerY = event.values[1];
                    magnetometerZ = event.values[2];
                    break;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Không cần thực hiện gì nếu thay đổi độ chính xác của cảm biến
        }
    };
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(sensorEventListener, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(sensorEventListener, gyroscopeSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(sensorEventListener, magnetometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(sensorEventListener);
    }
}