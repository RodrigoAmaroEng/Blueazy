package br.eng.rodrigoamaro.bluetoothhelper;


import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.IntentFilter;

import rx.Observable;
import rx.functions.Func0;
import rx.functions.Func1;

import static android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_FINISHED;
import static android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_STARTED;
import static android.bluetooth.BluetoothDevice.ACTION_FOUND;

class SearchApi extends BluetoothApi {

    private boolean mStopRequested;

    SearchApi(ContextProvider context, BluetoothAdapter adapter) {
        super(context, adapter);
    }

    void stop() {
        mAdapter.cancelDiscovery();
        mStopRequested = true;
    }

    Observable<SearchEvent> search() {
        mStopRequested = false;
        return new RxBroadcast.Builder(mContext.getContext())
                .addFilters(ACTION_DISCOVERY_STARTED, ACTION_DISCOVERY_FINISHED, ACTION_FOUND)
                .setExitCondition(detectEndOfSearch())
                .setStartOperation(startSearch())
                .build()
                .map(extractEvent())
                .filter(RxUtils.discardNulls());
    }

    private Func0<Observable<Intent>> startSearch() {
        return new Func0<Observable<Intent>>() {
            @Override
            public Observable<Intent> call() {
                mAdapter.startDiscovery();
                return Observable.empty();
            }
        };
    }


    private Func1<Intent, Boolean> detectEndOfSearch() {
        return new Func1<Intent, Boolean>() {
            @Override
            public Boolean call(Intent intent) {
                return ACTION_DISCOVERY_FINISHED.equals(intent.getAction()) && mStopRequested;
            }
        };
    }

    private Func1<Intent, SearchEvent> extractEvent() {
        return new Func1<Intent, SearchEvent>() {
            @Override
            public SearchEvent call(Intent intent) {
                String action = intent.getAction();
                if (ACTION_DISCOVERY_STARTED.equals(action)) {
                    return new SearchEvent();
                } else if (ACTION_FOUND.equals(action)) {
                    return new SearchEvent(new Device(intent));
                } else if (ACTION_DISCOVERY_FINISHED.equals(action)) {
                    mAdapter.startDiscovery();
                }
                return null;
            }
        };
    }


}
