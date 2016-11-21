package br.eng.rodrigoamaro.bluetoothhelper;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = LibModule.class)
interface Injector {

    SearchEngine createNewSearchEngine();

    Timer timerInstance();

    PairEngine createNewPairEngine();
}
