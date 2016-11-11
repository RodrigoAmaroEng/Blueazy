package br.eng.rodrigoamaro.bluetoothhelper;


import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import rx.Observable;
import rx.functions.Func1;

import static android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_FINISHED;
import static android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_STARTED;
import static android.bluetooth.BluetoothDevice.ACTION_FOUND;

public class SearchApi extends BluetoothApi {

    private boolean mStopRequested;

    public SearchApi(Context context, BluetoothAdapter adapter) {
        super(adapter, context);
    }

    public void stop() {
        mAdapter.cancelDiscovery();
        mStopRequested = true;
    }

    public Observable<SearchEvent> search() {
        mStopRequested = false;
        mAdapter.startDiscovery();
        IntentFilter intentFilter = new IntentFilter(ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(ACTION_DISCOVERY_FINISHED);
        intentFilter.addAction(ACTION_FOUND);
        return RxBroadcast.fromShortBroadcast(mContext, intentFilter, detectEndOfSearch())
                .map(extractEvent())
                .filter(RxUtils.discardNulls());
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
