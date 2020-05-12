package dev.amaro.bluetoothhelper;


import android.bluetooth.BluetoothDevice;
import android.content.Intent;

import static android.bluetooth.BluetoothDevice.EXTRA_DEVICE;
import static android.bluetooth.BluetoothDevice.EXTRA_NAME;
import static android.bluetooth.BluetoothDevice.EXTRA_RSSI;

public class Device {
    private BluetoothDevice mDetails;
    private final String mName;
    private final int mSignal;

    public Device(Intent intent) {
        this((BluetoothDevice) intent.getParcelableExtra(EXTRA_DEVICE),
                intent.getStringExtra(EXTRA_NAME), intent.getIntExtra(EXTRA_RSSI, -99));
    }

    public Device(BluetoothDevice details, String name, int signal) {
        mDetails = details;
        mName = name;
        mSignal = signal;
    }

    public BluetoothDevice getDetails() {
        return mDetails;
    }

    public int getSignal() {
        return mSignal;
    }

    public String getName() {
        return mName;
    }
}
