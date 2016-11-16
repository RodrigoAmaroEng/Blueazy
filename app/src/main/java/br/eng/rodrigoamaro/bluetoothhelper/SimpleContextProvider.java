package br.eng.rodrigoamaro.bluetoothhelper;

import android.content.Context;

public class SimpleContextProvider implements ContextProvider {

    private static Context sContext;

    public SimpleContextProvider(Context context) {
        sContext = context;
    }

    @Override
    public Context getContext() {
        return sContext;
    }
}
