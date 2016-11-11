package br.eng.rodrigoamaro.bluetoothhelper;

import android.bluetooth.BluetoothAdapter;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class LibModule {

    private final ContextProvider mContextProvider;

    public LibModule(ContextProvider context) {
        mContextProvider = context;
    }

    private static SearchEngine sSearchEngine;

    public static void setSearchEngine(SearchEngine searchEngine) {
        sSearchEngine = searchEngine;
    }

    @Provides
    public SearchEngine provideSearchEngine(SearchApi searchApi) {
        if (sSearchEngine == null) {
            return new SearchEngine(searchApi);
        }
        return sSearchEngine;
    }

    private static SearchApi sSearchApi;

    public static void setSearchApi(SearchApi searchApi) {
        sSearchApi = searchApi;
    }

    @Provides
    public SearchApi provideSearchApi(BluetoothAdapter bluetoothAdapter) {
        if (sSearchApi == null) {
            return new SearchApi(mContextProvider.getContext(), bluetoothAdapter);
        }
        return sSearchApi;
    }

    private static BluetoothAdapter sBluetoothAdapter;

    public static void setBluetoothAdapter(BluetoothAdapter bluetoothAdapter) {
        sBluetoothAdapter = bluetoothAdapter;
    }

    @Provides
    protected BluetoothAdapter provideBluetoothAdapter() {
        if (sBluetoothAdapter == null) {
            return BluetoothAdapter.getDefaultAdapter();
        }
        return sBluetoothAdapter;
    }

    private static Timer sTimer;

    public static void setTimer(Timer timer) {
        sTimer = timer;
    }

    @Singleton
    @Provides
    protected Timer provideTimer() {
        if (sTimer == null) {
            return new Timer();
        }
        return sTimer;
    }
}
