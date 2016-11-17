package br.eng.rodrigoamaro.bluetoothhelper;


import android.app.Application;
import android.content.Intent;

public class Broadcaster {
    private final Application mApplication;

    public Broadcaster(Application application) {
        mApplication = application;
    }

    public void sendMessage(String message, int value) {
        Intent intent = new Intent(message);
        intent.putExtra("VALUE", value);
        mApplication.sendBroadcast(intent);
    }
}
