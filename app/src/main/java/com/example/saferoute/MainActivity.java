package com.example.saferoute;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.mapbox.maps.MapView;
import com.mapbox.maps.Style;

public class MainActivity extends AppCompatActivity {

    private MapView mapView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mapView = findViewById(R.id.mapView);

        mapView.getMapboxMap().loadStyleUri(
                Style.MAPBOX_STREETS
        );
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}