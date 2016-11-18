package br.eng.rodrigoamaro.bluetoothhelper;


import android.bluetooth.BluetoothDevice;

import java.util.concurrent.atomic.AtomicReference;

import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;

import static br.eng.rodrigoamaro.bluetoothhelper.PairApi.ACTION_PAIRING_NOT_DONE;
import static br.eng.rodrigoamaro.bluetoothhelper.PairApi.ACTION_PAIRING_ON_PROGRESS;
import static br.eng.rodrigoamaro.bluetoothhelper.PairApi.ACTION_PAIRING_STARTED;

public class PairRequest {
    private final PairEngine mEngine;
    private final Timer mTimer;
    private final String mMacAddress;
    private final OnTimeoutListener mTimeoutListener = new OnTimeoutListener() {
        @Override
        public void onTimeout() {
            mEngine.notifyTimeout(mMacAddress);
        }
    };

    public PairRequest(String macAddress, ContextProvider contextProvider) {
        mMacAddress = macAddress;
        Injector injector = DaggerInjector.builder()
                .libModule(new LibModule(contextProvider)).build();
        mEngine = injector.createNewPairEngine();
        mTimer = injector.timerInstance();
    }

    public Observable<BluetoothDevice> perform() {
        AtomicReference<TimerOperation> opReference = new AtomicReference<>();
        return mEngine.pair(mMacAddress)
                .doOnNext(detectStart(opReference))
                .filter(detectError(opReference))
                .map(extractDevice())
                .doOnCompleted(stopTimer(opReference));
    }

    private Action1<? super PairEvent> detectStart(final AtomicReference<TimerOperation> opReference) {
        return new Action1<PairEvent>() {
            @Override
            public void call(PairEvent pairEvent) {
                if (ACTION_PAIRING_STARTED.equals(pairEvent.getEvent())) {
                    opReference.set(mTimer.countForSeconds(20, mTimeoutListener));
                }
            }
        };
    }


    private Action0 stopTimer(final AtomicReference<TimerOperation> opReference) {
        return new Action0() {
            @Override
            public void call() {
                mTimer.cancel(opReference.get());
            }
        };
    }


    private Func1<PairEvent, Boolean> detectError(final AtomicReference<TimerOperation> operation) {
        return new Func1<PairEvent, Boolean>() {
            boolean mReceivedPairingRequest = false;

            @Override
            public Boolean call(PairEvent pairEvent) {
                if (mReceivedPairingRequest && ACTION_PAIRING_NOT_DONE.equals(pairEvent.getEvent())) {
                    mEngine.notifyError(mMacAddress);
                    return false;
                } else if (ACTION_PAIRING_ON_PROGRESS.equals(pairEvent.getEvent())) {
                    operation.get().resetTime();
                    mReceivedPairingRequest = true;
                }
                return true;
            }
        };
    }

    private Func1<PairEvent, BluetoothDevice> extractDevice() {
        return new Func1<PairEvent, BluetoothDevice>() {
            @Override
            public BluetoothDevice call(PairEvent pairEvent) {
                return pairEvent.getDevice();
            }
        };
    }
}
