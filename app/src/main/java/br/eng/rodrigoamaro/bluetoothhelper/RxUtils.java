package br.eng.rodrigoamaro.bluetoothhelper;


import rx.functions.Func1;

public class RxUtils {

    public static <O> Func1<O, Boolean> discardNulls() {
        return new Func1<O, Boolean>() {
            @Override
            public Boolean call(O o) {
                return o != null;
            }
        };
    }
}
