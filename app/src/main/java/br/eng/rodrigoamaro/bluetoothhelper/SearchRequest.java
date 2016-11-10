package br.eng.rodrigoamaro.bluetoothhelper;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;

public class SearchRequest {
    private TimerOperation mOperation;
    private final Timer mTimer;
    private final SearchEngine mEngine;
    private final String mPrefix;
    private final int mSignal;
    private final List<Device> mDevices = new ArrayList<>();

    private final OnTimeoutListener mTimeoutListener = new OnTimeoutListener() {
        @Override
        public void onTimeout() {
            mEngine.stop();
        }
    };

    private SearchRequest(Timer timer, SearchEngine engine, String prefix, int signal) {
        mTimer = timer;
        mEngine = engine;
        mPrefix = prefix;
        mSignal = signal;
    }

    public Observable<Device> perform() {
        return mEngine.search()
                .doOnSubscribe(clearDeviceList())
                .doOnNext(detectStart())
                .map(extractDevice())
                .filter(RxUtils.discardNulls())
                .filter(discardByFilter())
                .doOnNext(collect());
    }

    private Action1<? super Device> collect() {
        return new Action1<Device>() {
            @Override
            public void call(Device device) {
                mDevices.add(device);
            }
        };
    }

    private Func1<? super Device, Boolean> discardByFilter() {
        return new Func1<Device, Boolean>() {
            @Override
            public Boolean call(Device device) {
                boolean filter = filterByPrefix(device);
                filter &= filterBySignalStrength(device);
                return filter;
            }
        };
    }

    private boolean filterBySignalStrength(Device device) {
        if (mSignal > 0) {
            int signal = device.getSignal() < 0 ? 100 + device.getSignal() :
                    device.getSignal();
            return signal > mSignal;
        }
        return true;
    }

    private boolean filterByPrefix(Device device) {
        if (mPrefix != null) {
            return device.getName().startsWith(mPrefix);
        }
        return true;
    }

    private Action0 clearDeviceList() {
        return new Action0() {
            @Override
            public void call() {
                mDevices.clear();
            }
        };
    }

    private Func1<? super SearchEvent, Device> extractDevice() {
        return new Func1<SearchEvent, Device>() {
            @Override
            public Device call(SearchEvent searchEvent) {
                if (BluetoothDevice.ACTION_FOUND.equals(searchEvent.getEventType())) {
                    return searchEvent.getDevice();
                }
                return null;
            }
        };
    }

    private Action1<? super SearchEvent> detectStart() {
        return new Action1<SearchEvent>() {
            @Override
            public void call(SearchEvent searchEvent) {
                if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(searchEvent.getEventType())) {
                    mOperation = mTimer.countForSeconds(30, mTimeoutListener);
                }
            }
        };
    }


    public Device[] getDevices() {
        return mDevices.toArray(new Device[mDevices.size()]);
    }

    public void stop() {
        mTimer.cancel(mOperation);
        mEngine.stop();
    }

    public static class Builder {

        private Timer mTimer;
        private SearchEngine mEngine;
        private String mPrefix;
        private int mSignal;

        public Builder w(Timer timer) {
            this.mTimer = timer;
            return this;
        }

        public Builder w(SearchEngine engine) {
            this.mEngine = engine;
            return this;
        }

        public SearchRequest create() {
            return new SearchRequest(mTimer, mEngine, mPrefix, mSignal);
        }

        public Builder filterByPrefix(String prefix) {
            this.mPrefix = prefix;
            return this;
        }

        public Builder filterBySignal(int signal) {
            this.mSignal = signal;
            return this;
        }
    }
}
