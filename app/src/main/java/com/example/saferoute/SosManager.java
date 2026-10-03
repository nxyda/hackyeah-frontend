package com.example.saferoute;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;

public class SosManager {

    private final Context context;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable sosRunnable;
    private boolean isSosTriggered = false;

    // Interfejs do komunikacji z mapą (żeby zmienić trasę na Safe Haven)
    public interface SosCallback {
        void onRouteToSafeHavenRequested();
    }
    private SosCallback callback;

    public SosManager(Context context, SosCallback callback) {
        this.context = context;
        this.callback = callback;
    }

    // Metoda, którą osoba od UI podepnie pod swój przycisk
    public void attachToButton(View sosButton) {
        sosButton.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    isSosTriggered = false;
                    // Krótka wibracja informująca, że zaczęto odliczanie
                    vibrate(100);

                    sosRunnable = () -> {
                        isSosTriggered = true;
                        triggerSosActions();
                    };
                    // Uruchom SOS po 3 sekundach (3000 ms)
                    handler.postDelayed(sosRunnable, 3000);
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    // Jeśli użytkownik puścił przycisk przed upływem 3 sekund
                    if (!isSosTriggered && sosRunnable != null) {
                        handler.removeCallbacks(sosRunnable);
                        Toast.makeText(context, "Anulowano SOS", Toast.LENGTH_SHORT).show();
                    }
                    return true;
            }
            return false;
        });
    }

    private void triggerSosActions() {
        // Długa wibracja potwierdzająca aktywację
        vibrate(1000);
        Toast.makeText(context, "SOS AKTYWOWANE! Wzywam pomoc.", Toast.LENGTH_LONG).show();

        // 1. Zadzwoń pod 112 (lub zaufany kontakt)
        //TODO zmienilem numer ze 112 zeby przypadkiem nie zadzwonic xd
        makeEmergencyCall("11222");

        // 3. Poinformuj główny ekran, że ma zmienić nawigację do najbliższego Safe Haven
        if (callback != null) {
            callback.onRouteToSafeHavenRequested();
        }
    }

    private void makeEmergencyCall(String phoneNumber) {
        Intent intent = new Intent(Intent.ACTION_DIAL); // ACTION_DIAL otwiera dialer, ACTION_CALL dzwoni od razu (wymaga uprawnień)
        intent.setData(Uri.parse("tel:" + phoneNumber));
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
        }
    }

    private void startBlackBoxRecording() {
        // Mock funkcji na hackathon
        Toast.makeText(context, "Czarna Skrzynka: Rozpoczęto wysyłanie audio do chmury", Toast.LENGTH_SHORT).show();
    }

    private void vibrate(long milliseconds) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }
}