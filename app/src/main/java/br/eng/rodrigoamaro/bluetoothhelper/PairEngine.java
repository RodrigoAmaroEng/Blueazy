package br.eng.rodrigoamaro.bluetoothhelper;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;

import rx.Observable;
import rx.functions.Func1;

public class PairEngine {
    private final PairApi mPairApi;

    public PairEngine(PairApi pairApi) {
        mPairApi = pairApi;
    }

    public Observable<BluetoothDevice> pair(String macAddress) {
        if (!mPairApi.isBluetoothOn()) {
            return mPairApi.turnBluetoothOn()
                    .map(toPairEvent())
                    .concatWith(mPairApi.pair(macAddress))
                    .map(extractDevice());
        } else {
            return mPairApi.turnBluetoothOff()
                    .concatWith(mPairApi.turnBluetoothOn())
                    .map(toPairEvent())
                    .concatWith(mPairApi.pair(macAddress))
                    .map(extractDevice());
        }
    }

    private Func1<? super Intent, PairEvent> toPairEvent() {
        return new Func1<Intent, PairEvent>() {
            @Override
            public PairEvent call(Intent intent) {
                return null;
            }
        };
    }

    private Func1<PairEvent, BluetoothDevice> extractDevice() {
        return new Func1<PairEvent, BluetoothDevice>() {
            @Override
            public BluetoothDevice call(PairEvent event) {
                return event.getDevice();
            }
        };
    }
}
