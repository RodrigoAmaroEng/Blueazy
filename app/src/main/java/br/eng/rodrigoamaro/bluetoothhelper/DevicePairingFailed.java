package br.eng.rodrigoamaro.bluetoothhelper;


public class DevicePairingFailed extends Exception {
    public DevicePairingFailed() {
        super();
    }

    public DevicePairingFailed(Exception e) {
        super(e);
    }
}
