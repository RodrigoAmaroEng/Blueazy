package dev.amaro.bluetoothhelper;


class Timer {

    private java.util.Timer mTimer = new java.util.Timer();

    TimerOperation countForSeconds(final int seconds, final OnTimeoutListener listener) {
        TimerOperation operation = new TimerOperation(seconds, listener);
        mTimer.schedule(operation, 1000, 1000);
        return operation;
    }

    void cancel(TimerOperation operation) {
        if (operation != null) {
            operation.cancel();
        }
    }
}
