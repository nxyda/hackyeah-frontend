package com.example.saferoute;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.Random;

public class UdostepnianieFragment extends Fragment {

    // Stan aplikacji
    private boolean isBroadcasting = false;
    private boolean isTracking = false;
    private String currentSessionCode = "";

    // Pętle w tle
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable broadcastRunnable;
    private Runnable trackingRunnable;

    public UdostepnianieFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_udostepnianie, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // --- WIDOKI NADAJNIKA ---
        TextView tvBroadcastStatus = view.findViewById(R.id.tv_broadcast_status);
        TextView tvShareCode = view.findViewById(R.id.tv_share_code);
        Button btnStartBroadcast = view.findViewById(R.id.btn_start_broadcast);
        Button btnStopBroadcast = view.findViewById(R.id.btn_stop_broadcast);

        // --- WIDOKI ODBIORNIKA ---
        EditText etReceiveCode = view.findViewById(R.id.et_receive_code);
        Button btnStartTracking = view.findViewById(R.id.btn_start_tracking);
        Button btnStopTracking = view.findViewById(R.id.btn_stop_tracking);
        LinearLayout panelLiveData = view.findViewById(R.id.panel_live_data);
        TextView tvLiveCoordinates = view.findViewById(R.id.tv_live_coordinates);


        // ==========================================
        // 1. LOGIKA NADAJNIKA (Udostępnianie trasy)
        // ==========================================

        broadcastRunnable = new Runnable() {
            @Override
            public void run() {
                if (isBroadcasting) {
                    sendLocationToDatabase(currentSessionCode);
                    handler.postDelayed(this, 5000); // Wykonuj co 5 sekund
                }
            }
        };

        btnStartBroadcast.setOnClickListener(v -> {
            // Generowanie kodu sesji
            currentSessionCode = String.format("%04d", new Random().nextInt(10000));

            // Zmiana UI
            isBroadcasting = true;
            tvShareCode.setText("TWÓJ KOD: " + currentSessionCode);
            tvShareCode.setVisibility(View.VISIBLE);
            tvBroadcastStatus.setText("🟢 Nadawanie na żywo włączone");
            tvBroadcastStatus.setTextColor(0xFF4CAF50);

            btnStartBroadcast.setVisibility(View.GONE);
            btnStopBroadcast.setVisibility(View.VISIBLE);

            // Odpalenie pętli nadającej w tle
            handler.post(broadcastRunnable);

            // Okno wysłania kodu bliskim
            String shareMessage = "Cześć! Śledź moją trasę na żywo w aplikacji SafeRoute. Mój kod sesji to: " + currentSessionCode;
            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
            sendIntent.setType("text/plain");
            startActivity(Intent.createChooser(sendIntent, "Wyślij kod sesji do..."));
        });

        btnStopBroadcast.setOnClickListener(v -> {
            isBroadcasting = false;
            handler.removeCallbacks(broadcastRunnable); // Zatrzymanie pętli

            tvShareCode.setVisibility(View.GONE);
            tvBroadcastStatus.setText("🔴 Nie nadajesz sygnału");
            tvBroadcastStatus.setTextColor(0xFFF44336);

            btnStopBroadcast.setVisibility(View.GONE);
            btnStartBroadcast.setVisibility(View.VISIBLE);
            Toast.makeText(getContext(), "Zakończono nadawanie lokalizacji.", Toast.LENGTH_SHORT).show();
        });


        // ==========================================
        // 2. LOGIKA ODBIORNIKA (Śledzenie bliskiego)
        // ==========================================

        trackingRunnable = new Runnable() {
            @Override
            public void run() {
                if (isTracking) {
                    fetchLocationFromDatabase(etReceiveCode.getText().toString(), tvLiveCoordinates);
                    handler.postDelayed(this, 5000); // Odbieraj co 5 sekund
                }
            }
        };

        btnStartTracking.setOnClickListener(v -> {
            String code = etReceiveCode.getText().toString().trim();
            if (code.length() != 4) {
                Toast.makeText(getContext(), "Wpisz poprawny 4-cyfrowy kod!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Zmiana UI
            isTracking = true;
            etReceiveCode.setEnabled(false);
            btnStartTracking.setVisibility(View.GONE);
            btnStopTracking.setVisibility(View.VISIBLE);
            panelLiveData.setVisibility(View.VISIBLE);

            // Odpalenie pętli odbierającej w tle
            handler.post(trackingRunnable);
            Toast.makeText(getContext(), "Połączono! Nasłuchiwanie bazy danych...", Toast.LENGTH_SHORT).show();
        });

        btnStopTracking.setOnClickListener(v -> {
            isTracking = false;
            handler.removeCallbacks(trackingRunnable); // Zatrzymanie pętli

            etReceiveCode.setEnabled(true);
            etReceiveCode.setText("");
            panelLiveData.setVisibility(View.GONE);
            btnStopTracking.setVisibility(View.GONE);
            btnStartTracking.setVisibility(View.VISIBLE);
            Toast.makeText(getContext(), "Śledzenie zakończone.", Toast.LENGTH_SHORT).show();
        });
    }

    // --- METODY BAZODANOWE (MOCKI) ---

    private void sendLocationToDatabase(String sessionCode) {
        String coords = "50.06143, 19.93658";
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationManager locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
            if (locationManager != null) {
                Location loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (loc == null) loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (loc != null) coords = String.format("%.5f, %.5f", loc.getLatitude(), loc.getLongitude());
            }
        }

        // TODO: SUPABASE - Tutaj wstawiasz: UPDATE live_sessions SET lat=..., lng=... WHERE code = sessionCode
        Log.d("SafeRoute_Live", "NADAJĘ do bazy -> Kod: " + sessionCode + " | Poz: " + coords);
    }

    private void fetchLocationFromDatabase(String sessionCode, TextView tvCoordinates) {
        // TODO: SUPABASE - Tutaj wstawiasz: SELECT lat, lng FROM live_sessions WHERE code = sessionCode
        // Gdyby to była prawdziwa mapa, w tym miejscu przesuwalibyśmy kropkę (znacznik) na mapie.

        // Mockowanie zmiennej lokalizacji dla jury
        double mockLat = 50.06100 + (Math.random() * 0.001);
        double mockLng = 19.93600 + (Math.random() * 0.001);
        String receivedCoords = String.format("📍 %.5f, %.5f", mockLat, mockLng);

        tvCoordinates.setText(receivedCoords + "\n(Ostatnia akt: teraz)");
        Log.d("SafeRoute_Live", "ODBIERAM z bazy -> Kod: " + sessionCode + " | Poz: " + receivedCoords);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Zabezpieczenie przed wyciekiem pamięci
        isBroadcasting = false;
        isTracking = false;
        handler.removeCallbacksAndMessages(null);
    }
}