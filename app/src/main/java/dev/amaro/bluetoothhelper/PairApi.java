package dev.amaro.bluetoothhelper;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.util.Log;

import rx.Observable;
import rx.functions.Func0;
import rx.functions.Func1;

import static android.bluetooth.BluetoothDevice.ACTION_ACL_CONNECTED;
import static android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECTED;
import static android.bluetooth.BluetoothDevice.ACTION_BOND_STATE_CHANGED;
import static android.bluetooth.BluetoothDevice.BOND_BONDED;
import static android.bluetooth.BluetoothDevice.BOND_NONE;
import static android.bluetooth.BluetoothDevice.EXTRA_BOND_STATE;
import static android.bluetooth.BluetoothDevice.EXTRA_DEVICE;

class PairApi extends BluetoothApi {

    private static final String TAG = "PairApi";
    static final String ACTION_FAKE_PAIR_REQUEST = "android.bluetooth.device.action.PAIRING_REQUEST";
    static final String ACTION_PAIRING_SUCCEEDED = "dev.amaro.bluetoothhelper.PAIRING_SUCCEEDED";
    static final String ACTION_PAIRING_STARTED = "dev.amaro.bluetoothhelper.PAIRING_STARTED";
    static final String ACTION_PAIRING_TIMEOUT = "dev.amaro.bluetoothhelper.PAIRING_TIMEOUT";
    static final String ACTION_PAIRING_FAILED = "dev.amaro.bluetoothhelper.PAIRING_FAILED";
    static final String ACTION_PAIRING_NOT_DONE = "dev.amaro.bluetoothhelper.PAIRING_NOT_DONE";
    static final String ACTION_PAIRING_ON_PROGRESS = "android.bluetooth.device.action.PAIRING_ON_PROGRESS";
    private final PairingSystem mPairingSystem;

    public PairApi(ContextProvider context, BluetoothAdapter adapter, PairingSystem pairingSystem) {
        super(context, adapter);
        mPairingSystem = pairingSystem;
    }

    void sendTimeoutMessage(String macAddress) {
        Intent intent = new Intent(ACTION_PAIRING_TIMEOUT);
        intent.putExtra(EXTRA_DEVICE, mAdapter.getRemoteDevice(macAddress));
        mContext.getContext().sendBroadcast(intent);
    }

    void sendErrorMessage(String macAddress) {
        Intent intent = new Intent(ACTION_PAIRING_FAILED);
        intent.putExtra(EXTRA_DEVICE, mAdapter.getRemoteDevice(macAddress));
        mContext.getContext().sendBroadcast(intent);
    }

    Observable<PairEvent> pair(String macAddress) {
        return new RxBroadcast.Builder(mContext.getContext())
                .addFilters(ACTION_BOND_STATE_CHANGED, ACTION_ACL_CONNECTED, ACTION_ACL_DISCONNECTED
                        , ACTION_FAKE_PAIR_REQUEST, ACTION_PAIRING_FAILED, ACTION_PAIRING_TIMEOUT)
                .setIncludeExitConditionEvent()
                .setStartOperation(startPairProcess(macAddress))
                .setExitCondition(detectPairCompleted(macAddress))
                .build()
                .filter(onlyEventsForThisDevice(macAddress))
                .flatMap(detectError())
                .map(extractEvent())
                .filter(RxUtils.discardNulls());
    }

    private Func0<Observable<Intent>> startPairProcess(final String macAddress) {
        return new Func0<Observable<Intent>>() {
            @Override
            public Observable<Intent> call() {
                try {

                    // The method getRemoteDevice will always return a Device even if it doesn't exists
                    // https://developer.android.com/reference/android/bluetooth/BluetoothAdapter.html#getRemoteDevice
                    BluetoothDevice device = mAdapter.getRemoteDevice(macAddress);
                    if (device.getBondState() == BOND_BONDED) {
                        Intent intent = new Intent(ACTION_BOND_STATE_CHANGED);
                        intent.putExtra(EXTRA_DEVICE, device);
                        intent.putExtra(EXTRA_BOND_STATE, BOND_BONDED);
                        return Observable.just(intent);
                    }
                    mPairingSystem.pair(device);
                    Intent intent = new Intent(ACTION_PAIRING_STARTED);
                    intent.putExtra(EXTRA_DEVICE, device);
                    return Observable.just(intent);
                } catch (DevicePairingFailed devicePairingFailed) {
                    return Observable.error(devicePairingFailed);
                }
            }
        };
    }

    private Func1<Intent, Boolean> detectPairCompleted(final String macAddress) {
        return new Func1<Intent, Boolean>() {
            @Override
            public Boolean call(Intent intent) {
                int state = intent.getIntExtra(EXTRA_BOND_STATE, -1);
                BluetoothDevice device = intent.getParcelableExtra(EXTRA_DEVICE);
                return device != null && macAddress.equals(device.getAddress())
                        && state == BOND_BONDED;
            }
        };
    }

    private Func1<? super Intent, Boolean> onlyEventsForThisDevice(final String macAddress) {
        return new Func1<Intent, Boolean>() {
            @Override
            public Boolean call(Intent intent) {
                Log.d(TAG, "Action: " + intent.getAction());
                BluetoothDevice device = intent.getParcelableExtra(EXTRA_DEVICE);
                return macAddress.equals(device.getAddress());
            }
        };
    }

    private Func1<Intent, Observable<Intent>> detectError() {
        return new Func1<Intent, Observable<Intent>>() {
            @Override
            public Observable<Intent> call(Intent intent) {
                final String action = intent.getAction();
                if (ACTION_PAIRING_FAILED.equals(action) || ACTION_ACL_DISCONNECTED.equals(action)) {
                    return Observable.error(new DevicePairingFailed());
                } else if (ACTION_PAIRING_TIMEOUT.equals(action)) {
                    mPairingSystem.cancelPairRequest(mContext.getContext());
                    return Observable.error(new DevicePairingTimeout());
                }
                return Observable.just(intent);
            }
        };
    }

    private Func1<Intent, PairEvent> extractEvent() {
        return new Func1<Intent, PairEvent>() {
            @Override
            public PairEvent call(Intent intent) {
                final int state = intent.getIntExtra(EXTRA_BOND_STATE, -1);
                final String action = intent.getAction();
                BluetoothDevice device = intent.getParcelableExtra(EXTRA_DEVICE);
                if (ACTION_BOND_STATE_CHANGED.equals(action)) {
                    if (state == BOND_BONDED) {
                        return new PairEvent(ACTION_PAIRING_SUCCEEDED, device);
                    } else if (state == BOND_NONE) {
                        return new PairEvent(ACTION_PAIRING_NOT_DONE, device);
                    }
                } else if (ACTION_PAIRING_STARTED.equals(action)) {
                    return new PairEvent(ACTION_PAIRING_STARTED, device);
                } else if (ACTION_FAKE_PAIR_REQUEST.equals(action) ||
                        ACTION_ACL_CONNECTED.equals(action)) {
                    return new PairEvent(ACTION_PAIRING_ON_PROGRESS, device);
                }
                return null;
            }
        };
    }

}
