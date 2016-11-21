package br.eng.rodrigoamaro.bluetoothhelper;


import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

class PairingSystem {

    private static final String TAG = "PairingSystem";

    void pair(BluetoothDevice device) throws DevicePairingFailed {
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

    void cancelPairRequest(Context context) {
        context.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
    }

}
