package com.example.saferoute;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import android.location.Location;

import com.mapbox.bindgen.Expected;
import com.mapbox.bindgen.Value;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.MapView;
import com.mapbox.maps.Style;
import com.mapbox.maps.plugin.Plugin;
import com.mapbox.maps.plugin.gestures.GesturesPlugin;
import com.mapbox.maps.plugin.gestures.GesturesUtils;
import com.mapbox.maps.plugin.gestures.OnMapClickListener;
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin;
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener;
import com.mapbox.maps.RenderedQueryGeometry;
import com.mapbox.maps.RenderedQueryOptions;
import com.mapbox.maps.QueriedRenderedFeature;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class MapaFragment extends Fragment {

    // =========================================================
    // MAPA
    // =========================================================

    private MapView mapView;

    private LocationComponentPlugin locationComponent;

    private GesturesPlugin gesturesPlugin;


    private AnomalyDetector anomalyDetector;

    // =========================================================
    // WYSZUKIWANIE
    // =========================================================

    private EditText searchDestination;
    private Button searchButton;

    // =========================================================
    // TRASY
    // =========================================================

    private Button safeRouteButton;
    private Button fastRouteButton;
    private Button balancedRouteButton;

    // =========================================================
    // ZGŁOSZENIE
    // =========================================================

    private Button addReportButton;

    // =========================================================
    // LOKALIZACJA
    // =========================================================

    private ImageButton locationButton;

    private Point currentLocation;

    private boolean firstLocationReceived = false;

    private boolean locationListenerAdded = false;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    // =========================================================
    // TEKSTY UI
    // =========================================================

    private TextView startPoint;
    private TextView destinationPoint;
    private TextView routeDetails;
    private TextView safetyStatus;

    // =========================================================
    // PANEL INFORMACJI O ZGŁOSZENIU
    // =========================================================

    private LinearLayout reportInfoPanel;

    private TextView reportInfoCategory;
    private TextView reportInfoTime;
    private TextView reportInfoId;

    private Button reportInfoClose;

    // =========================================================
    // TESTOWY CEL
    // =========================================================

    private Point destinationLocation;

    // =========================================================
    // TRASA
    // =========================================================

    private static final String ROUTE_SOURCE_ID =
            "test-route-source";

    private static final String ROUTE_LAYER_ID =
            "test-route-layer";

    // =========================================================
    // ZGŁOSZENIA
    // =========================================================

    private static final String REPORT_SOURCE_ID =
            "reports-source";

    private static final String REPORT_LAYER_ID =
            "reports-layer";

    private final List<Report> reports =
            new ArrayList<>();

    // =========================================================
    // MODEL ZGŁOSZENIA
    // =========================================================

    private static class Report {

        String id;

        double latitude;
        double longitude;

        String category;

        String createdAt;

        Report(
                String id,
                double latitude,
                double longitude,
                String category,
                String createdAt
        ) {

            this.id = id;

            this.latitude = latitude;

            this.longitude = longitude;

            this.category = category;

            this.createdAt = createdAt;
        }
    }

    // =========================================================
    // LISTENER GPS
    // =========================================================

    private final OnIndicatorPositionChangedListener
            onIndicatorPositionChangedListener =
            new OnIndicatorPositionChangedListener() {

                @Override
                public void onIndicatorPositionChanged(
                        Point point
                ) {

                    currentLocation = point;

                    if (anomalyDetector != null) {
                        // Mapbox daje Point, ale AnomalyDetector potrzebuje android.location.Location.
                        // Musimy szybko to zmapować. Jako Provider podajemy "Mapbox".
                        Location androidLoc = new Location("Mapbox");
                        androidLoc.setLatitude(point.latitude());
                        androidLoc.setLongitude(point.longitude());
                        
                        // Uwaga: Mapboxowa metoda OnIndicatorPositionChangedListener nie dostarcza prędkości (speed).
                        // Detektor poradzi sobie z postojem z samego dystansu, ale dla biegu musimy symulować / liczyć.
                        // Zostawiam wywołanie; w pełni produkcyjnej wersji brałbyś to z LocationManager z Androida.
                        anomalyDetector.processNewLocation(androidLoc);
                    }

                    if (!firstLocationReceived) {

                        firstLocationReceived = true;

                        if (mapView != null) {

                            mapView.getMapboxMap().setCamera(
                                    new CameraOptions.Builder()
                                            .center(point)
                                            .zoom(14.0)
                                            .build()
                            );
                        }
                    }
                }
            };

    // =========================================================
    // CLICK NA MAPIE
    // =========================================================

    private final OnMapClickListener
            onMapClickListener =
            point -> {

                if (mapView == null) {
                    return false;
                }

                RenderedQueryGeometry geometry =
                        new RenderedQueryGeometry(
                                mapView
                                        .getMapboxMap()
                                        .pixelForCoordinate(point)
                        );

                RenderedQueryOptions options =
                        new RenderedQueryOptions(
                                Arrays.asList(
                                        REPORT_LAYER_ID
                                ),
                                null
                        );

                mapView
                        .getMapboxMap()
                        .queryRenderedFeatures(
                                geometry,
                                options,
                                expected -> {

                                    if (expected == null
                                            || !expected.isValue()) {
                                        return;
                                    }

                                    List<QueriedRenderedFeature>
                                            features =
                                            expected.getValue();

                                    if (features == null
                                            || features.isEmpty()) {
                                        return;
                                    }

                                    QueriedRenderedFeature
                                            queriedFeature =
                                            features.get(0);

                                    if (queriedFeature == null
                                            || queriedFeature
                                            .getQueriedFeature()
                                            == null) {
                                        return;
                                    }

                                    com.mapbox.geojson.Feature
                                            feature =
                                            queriedFeature
                                                    .getQueriedFeature()
                                                    .getFeature();

                                    if (feature == null) {
                                        return;
                                    }

                                    String reportId =
                                            feature.getStringProperty(
                                                    "id"
                                            );

                                    String category =
                                            feature.getStringProperty(
                                                    "category"
                                            );

                                    String createdAt =
                                            feature.getStringProperty(
                                                    "created_at"
                                            );

                                    showReportInfo(
                                            reportId,
                                            category,
                                            createdAt
                                    );
                                }
                        );

                return true;
            };

    // =========================================================
    // ON CREATE VIEW
    // =========================================================

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {

        View view =
                inflater.inflate(
                        R.layout.fragment_mapa,
                        container,
                        false
                );

        anomalyDetector = new AnomalyDetector(requireContext());

        // =====================================================
        // MAPA
        // =====================================================

        mapView =
                view.findViewById(
                        R.id.mapView
                );

        // =====================================================
        // LOCATION COMPONENT
        // =====================================================

        locationComponent =
                mapView.getPlugin(
                        Plugin.MAPBOX_LOCATION_COMPONENT_PLUGIN_ID
                );

        // =====================================================
        // GESTURES
        // =====================================================

        gesturesPlugin =
                GesturesUtils.getGestures(
                        mapView
                );

        gesturesPlugin.addOnMapClickListener(
                onMapClickListener
        );

        // =====================================================
        // WYSZUKIWANIE
        // =====================================================

        searchDestination =
                view.findViewById(
                        R.id.search_destination
                );

        searchButton =
                view.findViewById(
                        R.id.search_button
                );

        // =====================================================
        // LOKALIZACJA
        // =====================================================

        locationButton =
                view.findViewById(
                        R.id.location_button
                );

        // =====================================================
        // ZGŁOSZENIE
        // =====================================================

        addReportButton =
                view.findViewById(
                        R.id.add_report_button
                );

        // =====================================================
        // TRASY
        // =====================================================

        safeRouteButton =
                view.findViewById(
                        R.id.safe_route_button
                );

        fastRouteButton =
                view.findViewById(
                        R.id.fast_route_button
                );

        balancedRouteButton =
                view.findViewById(
                        R.id.balanced_route_button
                );

        // =====================================================
        // INFORMACJE O TRASIE
        // =====================================================

        startPoint =
                view.findViewById(
                        R.id.start_point
                );

        destinationPoint =
                view.findViewById(
                        R.id.destination_point
                );

        routeDetails =
                view.findViewById(
                        R.id.route_details
                );

        safetyStatus =
                view.findViewById(
                        R.id.safety_status
                );

        // =====================================================
        // PANEL ZGŁOSZENIA
        // =====================================================

        reportInfoPanel =
                view.findViewById(
                        R.id.report_info_panel
                );

        reportInfoCategory =
                view.findViewById(
                        R.id.report_info_category
                );

        reportInfoTime =
                view.findViewById(
                        R.id.report_info_time
                );

        reportInfoId =
                view.findViewById(
                        R.id.report_info_id
                );

        reportInfoClose =
                view.findViewById(
                        R.id.report_info_close
                );

        reportInfoPanel.setVisibility(
                View.GONE
        );

        // =====================================================
        // MAPBOX STYLE
        // =====================================================

        mapView
                .getMapboxMap()
                .loadStyleUri(
                        Style.MAPBOX_STREETS,
                        style -> {

                            // ---------------------------------
                            // GPS
                            // ---------------------------------

                            if (ActivityCompat.checkSelfPermission(
                                    requireContext(),
                                    Manifest.permission
                                            .ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                                    || ActivityCompat.checkSelfPermission(
                                    requireContext(),
                                    Manifest.permission
                                            .ACCESS_COARSE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED) {

                                enableLocation();

                            } else {

                                requestLocationPermission();
                            }

                            // ---------------------------------
                            // ZGŁOSZENIA
                            // ---------------------------------

                            updateReportMarkers();
                        }
                );

        // =====================================================
        // SZUKANIE
        // =====================================================

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

            if (currentLocation == null) {

                Toast.makeText(
                        requireContext(),
                        "Czekam na Twoją lokalizację.",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            double startLng =
                    currentLocation.longitude();

            double startLat =
                    currentLocation.latitude();

            destinationLocation =
                    Point.fromLngLat(
                            startLng + 0.01,
                            startLat + 0.005
                    );

            destinationPoint.setText(
                    "🏁 Cel: " + destination
            );

            routeDetails.setText(
                    "Wyznaczono testową trasę."
            );

            safetyStatus.setText(
                    "🛡 Wybierz rodzaj trasy."
            );

            drawSafeRoute();

            Toast.makeText(
                    requireContext(),
                    "Narysowano testową trasę.",
                    Toast.LENGTH_SHORT
            ).show();
        });

        // =====================================================
        // MOJA LOKALIZACJA
        // =====================================================

        locationButton.setOnClickListener(v -> {

            if (currentLocation == null) {

                Toast.makeText(
                        requireContext(),
                        "Czekam na lokalizację.",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            mapView.getMapboxMap().setCamera(
                    new CameraOptions.Builder()
                            .center(currentLocation)
                            .zoom(15.0)
                            .build()
            );
        });

        // =====================================================
        // DODAJ ZGŁOSZENIE
        // =====================================================

        addReportButton.setOnClickListener(v -> {

            if (currentLocation == null) {

                Toast.makeText(
                        requireContext(),
                        "Czekam na Twoją lokalizację.",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            showReportCategoryDialog();
        });

        // =====================================================
        // ZAMKNIJ INFO
        // =====================================================

        reportInfoClose.setOnClickListener(
                v -> hideReportInfo()
        );

        // =====================================================
        // NAJBEZPIECZNIEJSZA
        // =====================================================

        safeRouteButton.setOnClickListener(v -> {

            if (currentLocation == null
                    || destinationLocation == null) {

                Toast.makeText(
                        requireContext(),
                        "Najpierw ustaw cel.",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            drawSafeRoute();

            routeDetails.setText(
                    "🛡 Wybrano trasę bezpieczną"
            );

            safetyStatus.setText(
                    "🛡 Poziom bezpieczeństwa: Wysoki"
            );
        });

        // =====================================================
        // NAJSZYBSZA
        // =====================================================

        fastRouteButton.setOnClickListener(v -> {

            if (currentLocation == null
                    || destinationLocation == null) {

                Toast.makeText(
                        requireContext(),
                        "Najpierw ustaw cel.",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            drawFastRoute();

            routeDetails.setText(
                    "⚡ Wybrano trasę szybką"
            );

            safetyStatus.setText(
                    "🛡 Poziom bezpieczeństwa: Średni"
            );
        });

        // =====================================================
        // ZBALANSOWANA
        // =====================================================

        balancedRouteButton.setOnClickListener(v -> {

            if (currentLocation == null
                    || destinationLocation == null) {

                Toast.makeText(
                        requireContext(),
                        "Najpierw ustaw cel.",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            drawBalancedRoute();

            routeDetails.setText(
                    "⚖ Wybrano trasę zbalansowaną"
            );

            safetyStatus.setText(
                    "🛡 Poziom bezpieczeństwa: Dobry"
            );
        });

        return view;
    }

    // =========================================================
    // DIALOG KATEGORII
    // =========================================================

    private void showReportCategoryDialog() {

        String[] categoryValues = {

                "danger",
                "harassment",
                "poor_lighting",
                "blocked_path",
                "suspicious_activity",
                "other"
        };

        String[] categoryLabels = {

                "Niebezpieczeństwo",
                "Nękanie",
                "Słabe oświetlenie",
                "Zablokowana droga",
                "Podejrzana aktywność",
                "Inne"
        };

        new AlertDialog.Builder(
                requireContext()
        )
                .setTitle(
                        "Dodaj zgłoszenie"
                )
                .setItems(
                        categoryLabels,
                        (dialog, which) -> {

                            addReport(
                                    categoryValues[which]
                            );
                        }
                )
                .setNegativeButton(
                        "Anuluj",
                        null
                )
                .show();
    }

    // =========================================================
    // DODAJ ZGŁOSZENIE
    // =========================================================

    private void addReport(
            String category
    ) {

        if (currentLocation == null) {

            Toast.makeText(
                    requireContext(),
                    "Brak aktualnej lokalizacji.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String reportId =
                UUID.randomUUID().toString();

        String createdAt =
                new SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss",
                        Locale.getDefault()
                ).format(
                        new Date()
                );

        Report report =
                new Report(
                        reportId,
                        currentLocation.latitude(),
                        currentLocation.longitude(),
                        category,
                        createdAt
                );

        reports.add(report);

        updateReportMarkers();

        Toast.makeText(
                requireContext(),
                "Dodano zgłoszenie",
                Toast.LENGTH_SHORT
        ).show();
    }

    // =========================================================
    // MARKERY ZGŁOSZEŃ
    // =========================================================

    private void updateReportMarkers() {

        if (mapView == null) {
            return;
        }

        mapView.getMapboxMap().getStyle(
                style -> {

                    // -----------------------------------------
                    // USUŃ STARĄ WARSTWĘ
                    // -----------------------------------------

                    try {

                        style.removeStyleLayer(
                                REPORT_LAYER_ID
                        );

                    } catch (Exception ignored) {
                    }

                    // -----------------------------------------
                    // USUŃ STARE ŹRÓDŁO
                    // -----------------------------------------

                    try {

                        style.removeStyleSource(
                                REPORT_SOURCE_ID
                        );

                    } catch (Exception ignored) {
                    }

                    // -----------------------------------------
                    // BRAK ZGŁOSZEŃ
                    // -----------------------------------------

                    if (reports.isEmpty()) {
                        return;
                    }

                    // -----------------------------------------
                    // GEOJSON FEATURES
                    // -----------------------------------------

                    StringBuilder features =
                            new StringBuilder();

                    features.append("[");

                    for (int i = 0;
                         i < reports.size();
                         i++) {

                        Report report =
                                reports.get(i);

                        if (i > 0) {
                            features.append(",");
                        }

                        features.append("{");

                        features.append(
                                "\"type\":\"Feature\","
                        );

                        features.append(
                                "\"properties\":{"
                        );

                        features.append(
                                "\"id\":\""
                        );

                        features.append(
                                escapeJson(
                                        report.id
                                )
                        );

                        features.append(
                                "\","
                        );

                        features.append(
                                "\"category\":\""
                        );

                        features.append(
                                escapeJson(
                                        report.category
                                )
                        );

                        features.append(
                                "\","
                        );

                        features.append(
                                "\"created_at\":\""
                        );

                        features.append(
                                escapeJson(
                                        report.createdAt
                                )
                        );

                        features.append(
                                "\""
                        );

                        features.append(
                                "},"
                        );

                        features.append(
                                "\"geometry\":{"
                        );

                        features.append(
                                "\"type\":\"Point\","
                        );

                        features.append(
                                "\"coordinates\":["
                        );

                        features.append(
                                report.longitude
                        );

                        features.append(",");
                        features.append(
                                report.latitude
                        );

                        features.append(
                                "]"
                        );

                        features.append(
                                "}"
                        );

                        features.append(
                                "}"
                        );
                    }

                    features.append("]");

                    // -----------------------------------------
                    // SOURCE
                    // -----------------------------------------

                    String sourceJson =
                            "{"
                                    + "\"type\":\"geojson\","
                                    + "\"data\":{"
                                    + "\"type\":\"FeatureCollection\","
                                    + "\"features\":"
                                    + features
                                    + "}"
                                    + "}";

                    Expected<String, Value>
                            sourceExpected =
                            Value.fromJson(
                                    sourceJson
                            );

                    if (sourceExpected.isError()) {

                        Toast.makeText(
                                requireContext(),
                                "Błąd tworzenia zgłoszenia.",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }

                    style.addStyleSource(
                            REPORT_SOURCE_ID,
                            sourceExpected.getValue()
                    );

                    // -----------------------------------------
                    // RED CIRCLE
                    // -----------------------------------------

                    String layerJson =
                            "{"
                                    + "\"id\":\""
                                    + REPORT_LAYER_ID
                                    + "\","
                                    + "\"type\":\"circle\","
                                    + "\"source\":\""
                                    + REPORT_SOURCE_ID
                                    + "\","
                                    + "\"paint\":{"
                                    + "\"circle-radius\":12,"
                                    + "\"circle-color\":\"#E53935\","
                                    + "\"circle-opacity\":1,"
                                    + "\"circle-stroke-color\":\"#FFFFFF\","
                                    + "\"circle-stroke-width\":3"
                                    + "}"
                                    + "}";

                    Expected<String, Value>
                            layerExpected =
                            Value.fromJson(
                                    layerJson
                            );

                    if (layerExpected.isError()) {

                        Toast.makeText(
                                requireContext(),
                                "Błąd tworzenia markera.",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }

                    // null = dodaj na wierzchu stylu
                    style.addStyleLayer(
                            layerExpected.getValue(),
                            null
                    );
                }
        );
    }

    // =========================================================
    // CLICK W CZERWONY MARKER
    // =========================================================

    private void showReportInfo(
            String id,
            String category,
            String createdAt
    ) {

        if (reportInfoPanel == null) {
            return;
        }

        reportInfoCategory.setText(
                "Kategoria: " + category
        );

        reportInfoTime.setText(
                "Godzina: " + createdAt
        );

        reportInfoId.setText(
                "ID: " + id
        );

        reportInfoPanel.setVisibility(
                View.VISIBLE
        );
    }

    // =========================================================
    // UKRYJ PANEL
    // =========================================================

    private void hideReportInfo() {

        if (reportInfoPanel != null) {

            reportInfoPanel.setVisibility(
                    View.GONE
            );
        }
    }

    // =========================================================
    // ESCAPE JSON
    // =========================================================

    private String escapeJson(
            String text
    ) {

        if (text == null) {
            return "";
        }

        return text
                .replace(
                        "\\",
                        "\\\\"
                )
                .replace(
                        "\"",
                        "\\\""
                )
                .replace(
                        "\n",
                        "\\n"
                )
                .replace(
                        "\r",
                        "\\r"
                );
    }

    // =========================================================
    // TRASA BEZPIECZNA
    // =========================================================

    private void drawSafeRoute() {

        if (currentLocation == null
                || destinationLocation == null) {
            return;
        }

        double startLng =
                currentLocation.longitude();

        double startLat =
                currentLocation.latitude();

        double endLng =
                destinationLocation.longitude();

        double endLat =
                destinationLocation.latitude();

        List<Point> points =
                new ArrayList<>();

        points.add(
                Point.fromLngLat(
                        startLng,
                        startLat
                )
        );

        points.add(
                Point.fromLngLat(
                        startLng + 0.002,
                        startLat + 0.002
                )
        );

        points.add(
                Point.fromLngLat(
                        startLng + 0.005,
                        startLat + 0.004
                )
        );

        points.add(
                Point.fromLngLat(
                        endLng - 0.002,
                        endLat
                )
        );

        points.add(
                destinationLocation
        );

        drawRoute(points);

        moveCameraToRoute(points);
    }

    // =========================================================
    // TRASA SZYBKA
    // =========================================================

    private void drawFastRoute() {

        if (currentLocation == null
                || destinationLocation == null) {
            return;
        }

        double startLng =
                currentLocation.longitude();

        double startLat =
                currentLocation.latitude();

        double endLng =
                destinationLocation.longitude();

        double endLat =
                destinationLocation.latitude();

        List<Point> points =
                new ArrayList<>();

        points.add(
                Point.fromLngLat(
                        startLng,
                        startLat
                )
        );

        points.add(
                Point.fromLngLat(
                        startLng + 0.004,
                        startLat
                )
        );

        points.add(
                Point.fromLngLat(
                        endLng - 0.003,
                        endLat - 0.002
                )
        );

        points.add(
                destinationLocation
        );

        drawRoute(points);

        moveCameraToRoute(points);
    }

    // =========================================================
    // TRASA ZBALANSOWANA
    // =========================================================

    private void drawBalancedRoute() {

        if (currentLocation == null
                || destinationLocation == null) {
            return;
        }

        double startLng =
                currentLocation.longitude();

        double startLat =
                currentLocation.latitude();

        double endLng =
                destinationLocation.longitude();

        double endLat =
                destinationLocation.latitude();

        List<Point> points =
                new ArrayList<>();

        points.add(
                Point.fromLngLat(
                        startLng,
                        startLat
                )
        );

        points.add(
                Point.fromLngLat(
                        startLng + 0.0025,
                        startLat + 0.001
                )
        );

        points.add(
                Point.fromLngLat(
                        endLng - 0.002,
                        endLat - 0.001
                )
        );

        points.add(
                destinationLocation
        );

        drawRoute(points);

        moveCameraToRoute(points);
    }

    // =========================================================
    // RYSOWANIE TRASY
    // =========================================================

    private void drawRoute(
            List<Point> points
    ) {

        if (mapView == null) {
            return;
        }

        LineString lineString =
                LineString.fromLngLats(
                        points
                );

        String sourceJson =
                "{"
                        + "\"type\":\"geojson\","
                        + "\"data\":"
                        + lineString.toJson()
                        + "}";

        String layerJson =
                "{"
                        + "\"id\":\""
                        + ROUTE_LAYER_ID
                        + "\","
                        + "\"type\":\"line\","
                        + "\"source\":\""
                        + ROUTE_SOURCE_ID
                        + "\","
                        + "\"paint\":{"
                        + "\"line-color\":\"#1976D2\","
                        + "\"line-width\":6"
                        + "}"
                        + "}";

        mapView.getMapboxMap().getStyle(
                style -> {

                    try {

                        style.removeStyleLayer(
                                ROUTE_LAYER_ID
                        );

                    } catch (Exception ignored) {
                    }

                    try {

                        style.removeStyleSource(
                                ROUTE_SOURCE_ID
                        );

                    } catch (Exception ignored) {
                    }

                    Expected<String, Value>
                            sourceExpected =
                            Value.fromJson(
                                    sourceJson
                            );

                    if (sourceExpected.isError()) {
                        return;
                    }

                    style.addStyleSource(
                            ROUTE_SOURCE_ID,
                            sourceExpected.getValue()
                    );

                    Expected<String, Value>
                            layerExpected =
                            Value.fromJson(
                                    layerJson
                            );

                    if (layerExpected.isError()) {
                        return;
                    }

                    style.addStyleLayer(
                            layerExpected.getValue(),
                            null
                    );
                }
        );
    }

    // =========================================================
    // KAMERA NA TRASĘ
    // =========================================================

    private void moveCameraToRoute(
            List<Point> points
    ) {

        if (points == null
                || points.isEmpty()) {
            return;
        }

        Point first =
                points.get(0);

        Point last =
                points.get(
                        points.size() - 1
                );

        double centerLng =
                (
                        first.longitude()
                                + last.longitude()
                ) / 2.0;

        double centerLat =
                (
                        first.latitude()
                                + last.latitude()
                ) / 2.0;

        mapView.getMapboxMap().setCamera(
                new CameraOptions.Builder()
                        .center(
                                Point.fromLngLat(
                                        centerLng,
                                        centerLat
                                )
                        )
                        .zoom(13.0)
                        .build()
        );
    }

    // =========================================================
    // GPS
    // =========================================================

    private void enableLocation() {

        if (mapView == null
                || locationComponent == null) {
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

        locationComponent.updateSettings(
                settings -> {

                    settings.setEnabled(true);

                    return null;
                }
        );

        if (!locationListenerAdded) {

            locationComponent
                    .addOnIndicatorPositionChangedListener(
                            onIndicatorPositionChangedListener
                    );

            locationListenerAdded = true;
        }
    }

    // =========================================================
    // PERMISSION
    // =========================================================

    private void requestLocationPermission() {

        requestPermissions(
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                },
                LOCATION_PERMISSION_REQUEST_CODE
        );
    }

    // =========================================================
    // PERMISSION RESULT
    // =========================================================

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

        if (requestCode ==
                LOCATION_PERMISSION_REQUEST_CODE) {

            boolean granted = false;

            for (int result : grantResults) {

                if (result ==
                        PackageManager.PERMISSION_GRANTED) {

                    granted = true;

                    break;
                }
            }

            if (granted) {

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

    // =========================================================
    // START
    // =========================================================

    @Override
    public void onStart() {

        super.onStart();

        if (mapView != null) {
            mapView.onStart();
        }
    }

    // =========================================================
    // STOP
    // =========================================================

    @Override
    public void onStop() {

        if (locationComponent != null
                && locationListenerAdded) {

            locationComponent
                    .removeOnIndicatorPositionChangedListener(
                            onIndicatorPositionChangedListener
                    );

            locationListenerAdded = false;
        }

        if (gesturesPlugin != null) {

            gesturesPlugin
                    .removeOnMapClickListener(
                            onMapClickListener
                    );
        }

        if (mapView != null) {
            mapView.onStop();
        }

        super.onStop();
    }

    // =========================================================
    // LOW MEMORY
    // =========================================================

    @Override
    public void onLowMemory() {

        super.onLowMemory();

        if (mapView != null) {
            mapView.onLowMemory();
        }
    }

    // =========================================================
    // DESTROY
    // =========================================================

    @Override
    public void onDestroyView() {

        if (locationComponent != null
                && locationListenerAdded) {

            locationComponent
                    .removeOnIndicatorPositionChangedListener(
                            onIndicatorPositionChangedListener
                    );

            locationListenerAdded = false;
        }

        if (gesturesPlugin != null) {

            gesturesPlugin
                    .removeOnMapClickListener(
                            onMapClickListener
                    );
        }

        if (mapView != null) {
            mapView.onDestroy();
        }

        locationComponent = null;

        gesturesPlugin = null;

        mapView = null;

        super.onDestroyView();
    }
}