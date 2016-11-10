package br.eng.rodrigoamaro.bluetoothhelper;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.util.Log;

import rx.Observable;
import rx.functions.Func1;

import static android.bluetooth.BluetoothDevice.ACTION_ACL_CONNECTED;
import static android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECTED;
import static android.bluetooth.BluetoothDevice.ACTION_BOND_STATE_CHANGED;
import static android.bluetooth.BluetoothDevice.BOND_BONDED;
import static android.bluetooth.BluetoothDevice.BOND_NONE;
import static android.bluetooth.BluetoothDevice.EXTRA_BOND_STATE;
import static android.bluetooth.BluetoothDevice.EXTRA_DEVICE;

public class PairApi {

    private static final String TAG = "PairApi";
    private static final String FAKE_ACTION_PAIR_REQUEST = "android.bluetooth.device.action.PAIRING_REQUEST";
    public static final String ACTION_PAIRING_SUCCEEDED = "br.eng.rodrigoamaro.bluetoothhelper.PAIRING_SUCCEEDED";
    public static final String ACTION_PAIRING_TIMEOUT = "br.eng.rodrigoamaro.bluetoothhelper.PAIRING_TIMEOUT";
    public static final String ACTION_PAIRING_FAILED = "br.eng.rodrigoamaro.bluetoothhelper.PAIRING_FAILED";
    public static final String ACTION_PAIRING_NOT_DONE = "br.eng.rodrigoamaro.bluetoothhelper.PAIRING_NOT_DONE";
    private final Context mContext;
    private final BluetoothAdapter mAdapter;
    private final PairingSystem mPairingSystem;

    public PairApi(Context context, BluetoothAdapter adapter, PairingSystem pairingSystem) {
        mContext = context;
        mAdapter = adapter;
        mPairingSystem = pairingSystem;
    }

    public Observable<PairEvent> pair(String macAddress) {
        try {
            // The method getRemoteDevice will always return a Device even if it doesn't exists
            // https://developer.android.com/reference/android/bluetooth/BluetoothAdapter.html#getRemoteDevice
            mPairingSystem.pair(mAdapter.getRemoteDevice(macAddress));
            IntentFilter intentFilter = new IntentFilter(ACTION_BOND_STATE_CHANGED);
            intentFilter.addAction(ACTION_ACL_CONNECTED);
            intentFilter.addAction(ACTION_ACL_DISCONNECTED);
            intentFilter.addAction(FAKE_ACTION_PAIR_REQUEST);
            intentFilter.addAction(ACTION_PAIRING_FAILED);
            intentFilter.addAction(ACTION_PAIRING_TIMEOUT);
            return RxBroadcast.fromShortBroadcastInclusive(mContext, intentFilter, detectPairCompleted(macAddress))
                    .flatMap(detectError(macAddress))
                    .map(extractEvent(macAddress))
                    .filter(RxUtils.discardNulls());
        } catch (DevicePairingFailed devicePairingFailed) {
            return Observable.error(devicePairingFailed);
        }

    }

    private Func1<Intent, Observable<Intent>> detectError(final String macAddress) {
        return new Func1<Intent, Observable<Intent>>() {
            @Override
            public Observable<Intent> call(Intent intent) {
                final String action = intent.getAction();
                BluetoothDevice device = intent.getParcelableExtra(EXTRA_DEVICE);
                if (macAddress.equals(device.getAddress())) {
                    if (ACTION_PAIRING_FAILED.equals(action)) {
                        return Observable.error(new DevicePairingFailed());
                    } else if (ACTION_PAIRING_TIMEOUT.equals(action)) {
                        return Observable.error(new DevicePairingTimeout());
                    }
                }
                return Observable.just(intent);
            }
        };
    }

    private Func1<Intent, PairEvent> extractEvent(final String macAddress) {
        return new Func1<Intent, PairEvent>() {
            @Override
            public PairEvent call(Intent intent) {
                final int state = intent.getIntExtra(EXTRA_BOND_STATE, -1);
                final String action = intent.getAction();
                BluetoothDevice device = intent.getParcelableExtra(EXTRA_DEVICE);
                log(state, action, device);
                if (macAddress.equals(device.getAddress())) {
                    if (state == BOND_BONDED) {
                        return new PairEvent(ACTION_PAIRING_SUCCEEDED, device);
                    } else if (state == BOND_NONE) {
                        return new PairEvent(ACTION_PAIRING_NOT_DONE, device);
                    }
                }
                return null;
            }
        };
    }

    private void log(int state, String action, BluetoothDevice device) {
        Log.d(TAG, "Action: " + action + " Bond State: " + state +
                " MacAddress: " + device.getAddress() +
                " DeviceClass: " + getDeviceClass(device));
    }

    @NonNull
    private String getDeviceClass(BluetoothDevice device) {
        if (device.getBluetoothClass() != null) {
            return device.getBluetoothClass().getMajorDeviceClass() + " - " +
                    device.getBluetoothClass().getDeviceClass();
        }
        return "No information";
    }

    private Func1<Intent, Boolean> detectPairCompleted(final String macAddress) {
        return new Func1<Intent, Boolean>() {
            @Override
            public Boolean call(Intent intent) {
                int state = intent.getIntExtra(EXTRA_BOND_STATE, -1);
                BluetoothDevice device = intent.getParcelableExtra(EXTRA_DEVICE);
                return macAddress.equals(device.getAddress()) && state == BOND_BONDED;
            }
        };
    }
}
