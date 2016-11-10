package br.eng.rodrigoamaro.bluetoothhelper;


import java.util.TimerTask;

public class TimerOperation extends TimerTask {
    private int mDuration;
    private OnTimeoutListener mListener;
    private int mElapsed = 0;
    private TimerTask mTask;

    TimerOperation(final int duration, final OnTimeoutListener listener) {
        mDuration = duration;
        mListener = listener;
    }

    @Override
    public void run() {
        if (++mElapsed == mDuration) {
            cancel();
            mListener.onTimeout();
            mTask = null;
            mListener = null;
        }
    }

    public void incrementBy(int seconds) {
        mDuration += seconds;
    }
}
