package br.eng.rodrigoamaro.bluetoothhelper;


import java.util.TimerTask;

class TimerOperation extends TimerTask {
    private static final String TAG = "TimerOperation";
    private int mDuration;
    private OnTimeoutListener mListener;
    private int mElapsed = 0;
    private final Object mLock = new Object();

    TimerOperation(final int duration, final OnTimeoutListener listener) {
        mDuration = duration;
        mListener = listener;
    }

    @Override
    public void run() {
        synchronized (mLock) {
            mElapsed++;
            if (mElapsed >= mDuration) {
                cancel();
                mListener.onTimeout();
                mListener = null;
            }
        }
    }

    void incrementBy(int seconds) {
        synchronized (mLock) {
            mDuration += seconds;
        }
    }

    void resetTime() {
        synchronized (mLock) {
            mElapsed = 0;
        }
    }
}
