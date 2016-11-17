package br.eng.rodrigoamaro.bluetoothhelper;


import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

public class PairingSystem {
    private static final String BASE_BT_UUID_SUFFIX = "-0000-1000-8000-00805F9B34FB";
    public static final String SDP_UUID_PREFIX = "00000001";
    public static final String RFCOMM_UUID_PREFIX = "00000003";
    public static final String OBEX_UUID_PREFIX = "00000008";
    public static final String HTTP_UUID_PREFIX = "0000000C";
    public static final String BNEP_UUID_PREFIX = "0000000F";
    public static final String L2CAP_UUID_PREFIX = "00000100";
    public static final String SERIAL_UUID_PREFIX = "00001101";
    public static final String HEADSET_UUID_PREFIX = "00001203";

    private static final String TAG = "PairingSystem";

    public void pair(BluetoothDevice device) throws DevicePairingFailed {
        pairBond(device);
    }

    private void pairBond(BluetoothDevice device) throws DevicePairingFailed {
        try {
            device.getClass().getMethod("createBond").invoke(device);
            Log.d(TAG, "Pair call succeeded");
        } catch (Exception e) {
            throw new DevicePairingFailed(e);
        }
    }

    public void cancelPairRequest(Context context) {
        context.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
    }

    private void pairRfComm(BluetoothDevice device) {
        try {
            UUID uuid = UUID.fromString(SERIAL_UUID_PREFIX + BASE_BT_UUID_SUFFIX);
            BluetoothSocket socket = device.createRfcommSocketToServiceRecord(uuid);
            if (socket != null) {
                try {
                    socket.connect();
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    try {
                        socket.close();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
