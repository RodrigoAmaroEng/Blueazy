package br.eng.rodrigoamaro.bluetoothhelper;

import android.bluetooth.BluetoothDevice;

import static android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_STARTED;

public class SearchEvent {

    private final String mEventType;
    private final Device mDevice;

    public SearchEvent() {
        this(ACTION_DISCOVERY_STARTED);
    }

    public SearchEvent(String eventType) {
        this(eventType, null);
    }

    public SearchEvent(Device device) {
        this(BluetoothDevice.ACTION_FOUND, device);
    }

    private SearchEvent(String eventType, Device device) {
        mEventType = eventType;
        mDevice = device;
    }

    public String getEventType() {
        return mEventType;
    }

    public Device getDevice() {
        return mDevice;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SearchEvent)) {
            return false;
        }

        SearchEvent that = (SearchEvent) o;

        if (!mEventType.equals(that.mEventType)) {
            return false;
        }
        return mDevice != null ? mDevice.equals(that.mDevice) : that.mDevice == null;

    }

    @Override
    public int hashCode() {
        int result = mEventType.hashCode();
        result = 31 * result + (mDevice != null ? mDevice.hashCode() : 0);
        return result;
    }
}
