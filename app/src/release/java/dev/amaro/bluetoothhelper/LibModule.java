package dev.amaro.bluetoothhelper;

import android.bluetooth.BluetoothAdapter;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
class LibModule {

    private static Timer sTimer;
    private static PairingSystem sPairingSystem;
    private final ContextProvider mContextProvider;

    public LibModule(ContextProvider context) {
        mContextProvider = context;
    }

    @Provides
    public SearchEngine provideSearchEngine(SearchApi searchApi) {
        return new SearchEngine(searchApi);
    }

    @Provides
    public SearchApi provideSearchApi(BluetoothAdapter bluetoothAdapter) {
        return new SearchApi(mContextProvider, bluetoothAdapter);
    }

    @Provides
    protected BluetoothAdapter provideBluetoothAdapter() {
        return BluetoothAdapter.getDefaultAdapter();
    }

    @Singleton
    @Provides
    protected Timer provideTimer() {
        if (sTimer == null) {
            return new Timer();
        }
        return sTimer;
    }

    @Provides
    protected PairEngine providePairEngine(PairApi pairApi) {
        return new PairEngine(pairApi);
    }

    @Provides
    protected PairApi providePairApi(BluetoothAdapter adapter,
                                     PairingSystem pairingSystem) {
        return new PairApi(mContextProvider, adapter, pairingSystem);
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


