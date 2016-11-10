package br.eng.rodrigoamaro.bluetoothhelper;


public class Timer {

    private java.util.Timer mTimer = new java.util.Timer();

    public TimerOperation countForSeconds(final int seconds, final OnTimeoutListener listener) {
        TimerOperation operation = new TimerOperation(seconds, listener);
        mTimer.schedule(operation, 1000, 1000);
        return operation;
    }

    public void cancel(TimerOperation operation) {
        if (operation != null) {
            operation.cancel();
        }
    }
}
