package br.eng.rodrigoamaro.bluetoothhelper;


import android.bluetooth.BluetoothDevice;

class PairEvent {
    private final String mEvent;
    private final BluetoothDevice mDevice;

    PairEvent(String event, BluetoothDevice device) {
        mEvent = event;
        mDevice = device;
    }

    public BluetoothDevice getDevice() {
        return mDevice;
    }

    public String getEvent() {
        return mEvent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PairEvent pairEvent = (PairEvent) o;

        return mEvent.equals(pairEvent.mEvent) && mDevice.equals(pairEvent.mDevice);
    }

    @Override
    public int hashCode() {
        int result = mEvent.hashCode();
        result = 31 * result + mDevice.hashCode();
        return result;
    }
}
