package com.example.saferoute;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.MapView;
import com.mapbox.maps.Style;
import com.mapbox.maps.plugin.Plugin;
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin;
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener;

public class MapaFragment extends Fragment {

    private MapView mapView;

    private LocationComponentPlugin locationComponent;

    private EditText searchDestination;
    private Button searchButton;

    private Button safeRouteButton;
    private Button fastRouteButton;
    private Button balancedRouteButton;

    private ImageButton locationButton;

    private TextView startPoint;
    private TextView destinationPoint;
    private TextView routeDetails;
    private TextView safetyStatus;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    // Czy mapa została już wycentrowana na użytkowniku
    private boolean firstLocationReceived = false;


    // =========================
    // OTRZYMANIE LOKALIZACJI
    // =========================

    private final OnIndicatorPositionChangedListener
            onIndicatorPositionChangedListener =
            new OnIndicatorPositionChangedListener() {

                @Override
                public void onIndicatorPositionChanged(Point point) {

                    if (!firstLocationReceived) {

                        firstLocationReceived = true;

                        mapView.getMapboxMap().setCamera(
                                new CameraOptions.Builder()
                                        .center(point)
                                        .zoom(14.0)
                                        .build()
                        );
                    }
                }
            };


    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {

        View view = inflater.inflate(
                R.layout.fragment_mapa,
                container,
                false
        );


        // =========================
        // MAPA
        // =========================

        mapView = view.findViewById(R.id.mapView);


        // =========================
        // LOCATION COMPONENT
        // =========================

        locationComponent =
                mapView.getPlugin(Plugin.MAPBOX_LOCATION_COMPONENT_PLUGIN_ID);


        // =========================
        // WYSZUKIWANIE
        // =========================

        searchDestination =
                view.findViewById(R.id.search_destination);

        searchButton =
                view.findViewById(R.id.search_button);


        // =========================
        // PRZYCISK LOKALIZACJI
        // =========================

        locationButton =
                view.findViewById(R.id.location_button);


        // =========================
        // WYBÓR RODZAJU TRASY
        // =========================

        safeRouteButton =
                view.findViewById(R.id.safe_route_button);

        fastRouteButton =
                view.findViewById(R.id.fast_route_button);

        balancedRouteButton =
                view.findViewById(R.id.balanced_route_button);


        // =========================
        // PANEL INFORMACJI
        // =========================

        startPoint =
                view.findViewById(R.id.start_point);

        destinationPoint =
                view.findViewById(R.id.destination_point);

        routeDetails =
                view.findViewById(R.id.route_details);

        safetyStatus =
                view.findViewById(R.id.safety_status);


        // =========================
        // MAPBOX
        // =========================

        mapView.getMapboxMap().loadStyleUri(
                Style.MAPBOX_STREETS,
                style -> {

                    if (ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                            || ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED) {

                        enableLocation();

                    } else {

                        requestLocationPermission();
                    }
                }
        );


        // =========================
        // SZUKANIE CELU
        // =========================

        searchButton.setOnClickListener(v -> {

            String destination =
                    searchDestination
                            .getText()
                            .toString()
                            .trim();

            if (destination.isEmpty()) {

                Toast.makeText(
                        requireContext(),
                        "Wpisz miejsce docelowe.",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            destinationPoint.setText(
                    "🏁 Cel: " + destination
            );

            routeDetails.setText(
                    "Start i cel zostały wybrane. Gotowe do wyznaczenia trasy."
            );

            safetyStatus.setText(
                    "🛡 Poziom bezpieczeństwa: oczekiwanie na trasę"
            );

            Toast.makeText(
                    requireContext(),
                    "Cel ustawiony: " + destination,
                    Toast.LENGTH_SHORT
            ).show();

        });


        // =========================
        // PRZYCISK MOJEJ LOKALIZACJI
        // =========================

        locationButton.setOnClickListener(v -> {

            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {

                requestLocationPermission();

                return;
            }

            firstLocationReceived = false;

            Toast.makeText(
                    requireContext(),
                    "Ustawiam Twoją lokalizację.",
                    Toast.LENGTH_SHORT
            ).show();

        });


        // =========================
        // NAJBEZPIECZNIEJSZA
        // =========================

        safeRouteButton.setOnClickListener(v -> {

            routeDetails.setText(
                    "Trasa z priorytetem bezpieczeństwa"
            );

            safetyStatus.setText(
                    "🛡 Poziom bezpieczeństwa: Wysoki"
            );

        });


        // =========================
        // NAJSZYBSZA
        // =========================

        fastRouteButton.setOnClickListener(v -> {

            routeDetails.setText(
                    "Trasa z priorytetem czasu przejścia"
            );

            safetyStatus.setText(
                    "🛡 Poziom bezpieczeństwa: Średni"
            );

        });


        // =========================
        // ZBALANSOWANA
        // =========================

        balancedRouteButton.setOnClickListener(v -> {

            routeDetails.setText(
                    "Trasa łącząca bezpieczeństwo i czas"
            );

            safetyStatus.setText(
                    "🛡 Poziom bezpieczeństwa: Dobry"
            );

        });


        return view;
    }


    // =========================
    // WŁĄCZENIE LOKALIZACJI
    // =========================

    private void enableLocation() {

        if (mapView == null || locationComponent == null) {
            return;
        }

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {

            return;
        }

        // Włączamy widoczność lokalizacji.
        // W Mapbox 11.x LocationComponentPlugin posiada
        // ustawienie "enabled".
        locationComponent.updateSettings(
                settings -> {
                    settings.setEnabled(true);
                    return null;
                }
        );

        locationComponent.addOnIndicatorPositionChangedListener(
                onIndicatorPositionChangedListener
        );
    }


    // =========================
    // UPRAWNIENIA LOKALIZACJI
    // =========================

    private void requestLocationPermission() {

        requestPermissions(
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                },
                LOCATION_PERMISSION_REQUEST_CODE
        );
    }


    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {

            boolean granted = false;

            for (int result : grantResults) {

                if (result == PackageManager.PERMISSION_GRANTED) {
                    granted = true;
                    break;
                }
            }

            if (granted) {

                Toast.makeText(
                        requireContext(),
                        "Lokalizacja została włączona.",
                        Toast.LENGTH_SHORT
                ).show();

                firstLocationReceived = false;

                enableLocation();

            } else {

                Toast.makeText(
                        requireContext(),
                        "Brak dostępu do lokalizacji.",
                        Toast.LENGTH_SHORT
                ).show();
            }
        }
    }


    // =========================
    // CYKL ŻYCIA MAPY
    // =========================

    @Override
    public void onStart() {

        super.onStart();

        if (mapView != null) {
            mapView.onStart();
        }
    }


    @Override
    public void onStop() {

        if (locationComponent != null) {

            locationComponent
                    .removeOnIndicatorPositionChangedListener(
                            onIndicatorPositionChangedListener
                    );
        }

        if (mapView != null) {
            mapView.onStop();
        }

        super.onStop();
    }


    @Override
    public void onLowMemory() {

        super.onLowMemory();

        if (mapView != null) {
            mapView.onLowMemory();
        }
    }


    @Override
    public void onDestroyView() {

        if (locationComponent != null) {

            locationComponent
                    .removeOnIndicatorPositionChangedListener(
                            onIndicatorPositionChangedListener
                    );
        }

        if (mapView != null) {
            mapView.onDestroy();
        }

        locationComponent = null;
        mapView = null;

        super.onDestroyView();
    }
}