package com.example.myapplication;

import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DisplayData extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private static final int LOCATION_REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_displaydata);

        // Load Google Map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Upload CSV and display points
        Button btnUpload = findViewById(R.id.btnUpload);

        // Set OnClickListener for the Upload button
        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Call the method to upload and display points
                uploadAndDisplayPoints();
            }
        });
    }

    private void uploadAndDisplayPoints() {
        // Replace "your_csv_file.csv" with the actual CSV file name
        File csvFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "location_data.csv");

        try {
            BufferedReader reader = new BufferedReader(new FileReader(csvFile));
            String line;
            List<LatLng> points = new ArrayList<>();

            // Skip header line if it exists
            reader.readLine();

            while ((line = reader.readLine()) != null) {
                String[] data = line.split(",");

                // Check if data array has at least 2 elements (latitude and longitude)
                if (data.length >= 2) {
                    // Remove double quotes from latitude and longitude strings
                    String latitudeString = data[0].trim().replaceAll("\"", "");
                    String longitudeString = data[1].trim().replaceAll("\"", "");

                    double latitude;
                    double longitude;

                    try {
                        // Parse latitude and longitude to double
                        latitude = Double.parseDouble(latitudeString);
                        longitude = Double.parseDouble(longitudeString);
                    } catch (NumberFormatException e) {
                        // Handle the case where parsing to double fails
                        e.printStackTrace(); // Log the exception for debugging
                        continue; // Skip to the next iteration of the loop
                    }

                    // Create LatLng and add to list
                    LatLng point = new LatLng(latitude, longitude);
                    points.add(point);
                }
            }

            // Display points on the map
            displayPoints(points);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void displayPoints(List<LatLng> points) {
        if (mMap != null && !points.isEmpty()) {
            LatLng previousPoint = null;

            for (LatLng point : points) {
                mMap.addMarker(new MarkerOptions().position(point));

                if (previousPoint != null) {
                    // Draw a polyline between the current point and the previous point
                    PolylineOptions polylineOptions = new PolylineOptions()
                            .add(previousPoint, point)
                            .width(5)
                            .color(Color.RED);
                    mMap.addPolyline(polylineOptions);
                }

                previousPoint = point;
            }

            // Move camera to the first point
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(points.get(0), 15f));
        }
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15f));
                        }
                    });
        } else {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }
    }
}
