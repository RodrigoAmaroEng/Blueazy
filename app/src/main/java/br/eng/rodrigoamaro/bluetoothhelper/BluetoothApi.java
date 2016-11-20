package br.eng.rodrigoamaro.bluetoothhelper;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.util.Log;

import rx.Observable;
import rx.functions.Func0;
import rx.functions.Func1;

import static android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED;
import static android.bluetooth.BluetoothAdapter.EXTRA_STATE;
import static android.bluetooth.BluetoothAdapter.STATE_OFF;
import static android.bluetooth.BluetoothAdapter.STATE_ON;

public class BluetoothApi {
    private static final String TAG = "BluetoothApi";
    protected final ContextProvider mContext;
    protected final BluetoothAdapter mAdapter;

    public BluetoothApi(ContextProvider context, BluetoothAdapter adapter) {
        mAdapter = adapter;
        mContext = context;
    }

    public Observable<Intent> turnBluetoothOn() {
        return new RxBroadcast.Builder(mContext.getContext())
                .addFilter(ACTION_STATE_CHANGED)
                .setExitCondition(filterStateIs(STATE_ON))
                .setStartOperation(turnOn())
                .build()
                .ignoreElements();
    }

    public Observable<Intent> turnBluetoothOff() {
        return new RxBroadcast.Builder(mContext.getContext())
                .addFilter(ACTION_STATE_CHANGED)
                .setExitCondition(filterStateIs(STATE_OFF))
                .setStartOperation(turnOff())
                .build()
                .ignoreElements();
    }

    public boolean isBluetoothOn() {
        return mAdapter.isEnabled();
    }

    private Func0<Observable<Intent>> turnOn() {
        return new Func0<Observable<Intent>>() {
            @Override
            public Observable<Intent> call() {
                mAdapter.enable();
                return Observable.empty();
            }
        };
    }

    private Func0<Observable<Intent>> turnOff() {
        return new Func0<Observable<Intent>>() {
            @Override
            public Observable<Intent> call() {
                mAdapter.disable();
                return Observable.empty();
            }
        };
    }

    private Func1<Intent, Boolean> filterStateIs(final int state) {
        return new Func1<Intent, Boolean>() {
            @Override
            public Boolean call(Intent intent) {
                Log.d(TAG, "filterStateIs: " + intent.getIntExtra(EXTRA_STATE, -1));
                return intent.getIntExtra(EXTRA_STATE, -1) == state;
            }
        };
    }
}
