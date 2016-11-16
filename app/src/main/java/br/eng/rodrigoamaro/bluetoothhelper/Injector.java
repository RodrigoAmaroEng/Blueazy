package br.eng.rodrigoamaro.bluetoothhelper;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = LibModule.class)
public interface Injector {

    SearchEngine createNewSearchEngine();

    Timer timerInstance();

    PairEngine createNewPairEngine();
}
