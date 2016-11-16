package br.eng.rodrigoamaro.bluetoothhelper;

import android.content.Intent;

import rx.Observable;
import rx.functions.Func1;

public class PairEngine {
    private final PairApi mPairApi;

    public PairEngine(PairApi pairApi) {
        mPairApi = pairApi;
    }

    public Observable<PairEvent> pair(String macAddress) {
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


    public void notifyTimeout(String macAddress) {
        mPairApi.sendTimeoutMessage(macAddress);
    }

    public void notifyError(String macAddress) {
        mPairApi.sendErrorMessage(macAddress);
    }
}
