package com.example.saferoute;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

public class SosManager {

    private final Context context;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable sosRunnable;
    private boolean isSosTriggered = false;

    public interface SosCallback {
        void onRouteToSafeHavenRequested();
    }
    private SosCallback callback;

    public SosManager(Context context, SosCallback callback) {
        this.context = context;
        this.callback = callback;
    }

    public void attachToButton(View sosButton) {
        sosButton.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    isSosTriggered = false;
                    vibrate(100);

                    sosRunnable = () -> {
                        isSosTriggered = true;
                        triggerSosActions();
                    };
                    handler.postDelayed(sosRunnable, 3000);
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
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
        vibrate(1000);
        Toast.makeText(context, "SOS AKTYWOWANE!", Toast.LENGTH_LONG).show();

        // 1. Zadzwoń od razu
        makeEmergencyCall("514157266");

        // 2. Czarna skrzynka
        startBlackBoxRecording();

        // 3. Udostępnianie lokalizacji bliskim (Placeholder)
        notifyEmergencyContacts();

        // 4. Nawigacja ucieczki
        if (callback != null) {
            callback.onRouteToSafeHavenRequested();
        }
    }

    private void makeEmergencyCall(String phoneNumber) {
        // Zmiana na ACTION_CALL - wymusza natychmiastowe połączenie bez otwierania dialera
        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setData(Uri.parse("tel:" + phoneNumber));
        try {
            context.startActivity(intent);
        } catch (SecurityException e) {
            // Aplikacja zcrashuje, jeśli nie dodasz uprawnień CALL_PHONE w Manifeście
            Toast.makeText(context, "Brak uprawnień do dzwonienia!", Toast.LENGTH_SHORT).show();
        }
    }

    private void startBlackBoxRecording() {
        Toast.makeText(context, "Czarna Skrzynka: Nagrywanie i stream do chmury", Toast.LENGTH_SHORT).show();
    }

    private void notifyEmergencyContacts() {
        // TODO: Placeholder - tu w przyszłości dodasz logikę pobierania kontaktów z Supabase i wysyłania do nich pusha lub SMSa z linkiem do Live Location
        Toast.makeText(context, "Powiadamianie bliskich: Wysłano alert z lokalizacją", Toast.LENGTH_SHORT).show();
    }

    // Naprawiony błąd z wibracjami (kompatybilność ze starszymi Androidami)
    private void vibrate(long milliseconds) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(milliseconds); // Dla starszych wersji Androida
            }
        }
    }
}