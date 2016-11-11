package br.eng.rodrigoamaro.bluetoothhelper;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import rx.Observable;
import rx.functions.Func1;

import static android.bluetooth.BluetoothAdapter.EXTRA_STATE;
import static android.bluetooth.BluetoothAdapter.STATE_OFF;
import static android.bluetooth.BluetoothAdapter.STATE_ON;

public class BluetoothApi {
    protected final Context mContext;
    protected final BluetoothAdapter mAdapter;

    public BluetoothApi(BluetoothAdapter adapter, Context context) {
        mAdapter = adapter;
        mContext = context;
    }

    public Observable<Intent> turnBluetoothOn() {
        mAdapter.enable();
        IntentFilter intentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        return RxBroadcast.fromShortBroadcast(mContext, intentFilter, filterState(STATE_ON))
                .ignoreElements();
    }

    public Observable<Intent> turnBluetoothOff() {
        mAdapter.disable();
        IntentFilter intentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        return RxBroadcast.fromShortBroadcast(mContext, intentFilter, filterState(STATE_OFF))
                .ignoreElements();
    }

    public boolean isBluetoothOn() {
        return mAdapter.isEnabled();
    }

    private Func1<Intent, Boolean> filterState(final int state) {
        return new Func1<Intent, Boolean>() {
            @Override
            public Boolean call(Intent intent) {
                return intent.getIntExtra(EXTRA_STATE, -1) == state;
            }
        };
    }
}
