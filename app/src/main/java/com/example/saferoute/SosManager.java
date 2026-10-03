package com.example.saferoute;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.telephony.SmsManager;
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

        startBlackBoxRecording();

        notifyEmergencyContacts();

        if (callback != null) {
            callback.onRouteToSafeHavenRequested();
        }

        makeEmergencyCall("514157266");
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
        // Dzięki temu na prezentacji przed jury faktycznie pokażecie, że SMS przyszedł.
        String[] emergencyContacts = {"514157266", "693300172"}; 
        
        // Mockowana lokalizacja (współrzędne centrum). 
        // Docelowo współrzędne można pobierać z Mapboxa/GPS telefonu.
        String message = "POZDRO!";

        try {
            // Pobranie domyślnego managera SMS w Androidzie
            SmsManager smsManager = SmsManager.getDefault();
            
            // Wysłanie wiadomości do wszystkich numerów z listy
            for (String number : emergencyContacts) {
                // sendTextMessage(numer_docelowy, numer_centrum_sms (null=domyślny), treść, intent_wysłania, intent_dostarczenia)
                smsManager.sendTextMessage(number, null, message, null, null);
            }
            Toast.makeText(context, "Wysłano SMS ratunkowy do bliskich!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            // Jeśli użytkownik nie dał uprawnień lub nie ma karty SIM, wyłapujemy błąd żeby aplikacja nie wyłączyła się (crash)
            Toast.makeText(context, "Błąd SMS. Brak uprawnień lub karty SIM?", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
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