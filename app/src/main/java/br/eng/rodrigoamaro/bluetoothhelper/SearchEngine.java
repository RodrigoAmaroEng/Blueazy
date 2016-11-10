package br.eng.rodrigoamaro.bluetoothhelper;

import android.content.Intent;

import rx.Observable;
import rx.functions.Func1;

public class SearchEngine {

    private final SearchApi mSearchApi;

    public SearchEngine(SearchApi searchApi) {
        mSearchApi = searchApi;
    }

    public Observable<SearchEvent> search() {
        if (!mSearchApi.isBluetoothOn()) {
            return mSearchApi.turnBluetoothOn()
                    .map(toSearchEvent())
                    .ignoreElements()
                    .takeLast(0)
                    .concatWith(mSearchApi.search());
        } else {
            return mSearchApi.turnBluetoothOff()
                    .takeLast(0)
                    .concatWith(mSearchApi.turnBluetoothOn())
                    .map(toSearchEvent())
                    .ignoreElements()
                    .takeLast(0)
                    .concatWith(mSearchApi.search());
        }
    }

    private Func1<? super Intent, SearchEvent> toSearchEvent() {
        return new Func1<Intent, SearchEvent>() {
            @Override
            public SearchEvent call(Intent intent) {
                return null;
            }
        };
    }

    public void stop() {
        mSearchApi.stop();
    }

}