package dev.amaro.bluetoothhelper;

import android.bluetooth.BluetoothAdapter;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
class LibModule {

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
            return new SearchApi(mContextProvider, bluetoothAdapter);
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

    private static PairEngine sPairEngine;

    public static void setPairEngine(PairEngine pairEngine) {
        sPairEngine = pairEngine;
    }

    @Provides
    protected PairEngine providePairEngine(PairApi pairApi) {
        if (sPairEngine == null) {
            return new PairEngine(pairApi);
        }
        return sPairEngine;
    }

    private static PairApi sPairApi;

    public static void setPairApi(PairApi pairApi) {
        sPairApi = pairApi;
    }

    @Provides
    protected PairApi providePairApi(BluetoothAdapter adapter,
                                     PairingSystem pairingSystem) {
        if (sPairApi == null) {
            return new PairApi(mContextProvider, adapter, pairingSystem);
        }
        return sPairApi;
    }

    private static PairingSystem sPairingSystem;

    public static void setPairingSystem(PairingSystem pairingSystem) {
        sPairingSystem = pairingSystem;
    }

    @Singleton
    @Provides
    protected PairingSystem providePairingSystem() {
        if (sPairingSystem == null) {
            return new PairingSystem();
        }
        return sPairingSystem;
    }
}
