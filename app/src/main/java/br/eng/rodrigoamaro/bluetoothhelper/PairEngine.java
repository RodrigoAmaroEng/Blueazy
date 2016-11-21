package br.eng.rodrigoamaro.bluetoothhelper;

import android.content.Intent;

import rx.Observable;
import rx.functions.Func1;

class PairEngine {
    private final PairApi mPairApi;

    PairEngine(PairApi pairApi) {
        mPairApi = pairApi;
    }

    Observable<PairEvent> pair(String macAddress) {
        Observable<Intent> observable = mPairApi.turnBluetoothOn();
        if (mPairApi.isBluetoothOn()) {
            observable = mPairApi.turnBluetoothOff().concatWith(observable);
        }
        return observable
                .map(toPairEvent())
                .concatWith(mPairApi.pair(macAddress));
    }

    private Func1<? super Intent, PairEvent> toPairEvent() {
        return new Func1<Intent, PairEvent>() {
            @Override
            public PairEvent call(Intent intent) {
                return null;
            }
        };
    }


    void notifyTimeout(String macAddress) {
        mPairApi.sendTimeoutMessage(macAddress);
    }

    void notifyError(String macAddress) {
        mPairApi.sendErrorMessage(macAddress);
    }
}
