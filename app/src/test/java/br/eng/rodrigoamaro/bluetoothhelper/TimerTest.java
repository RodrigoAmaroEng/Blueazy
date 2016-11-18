package br.eng.rodrigoamaro.bluetoothhelper;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@RunWith(JUnit4.class)
public class TimerTest {

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    CountDownLatch mLatch = new CountDownLatch(1);

    AtomicInteger mInteger = new AtomicInteger();

    @Spy
    OnTimeoutListener mListener = new OnTimeoutListener() {
        @Override
        public void onTimeout() {
            mInteger.set(mInteger.get() * 2);
            mLatch.countDown();
        }
    };

    @Test
    public void callListenerAfterTimeout() throws Exception {
        Timer timer = new Timer();
        timer.countForSeconds(1, mListener);
        verify(mListener, timeout(1200)).onTimeout();
    }

    @Test
    public void waitSpecifiedTimeBeforeCallListener() throws Exception {
        Timer timer = new Timer();
        mInteger.set(1);
        timer.countForSeconds(1, mListener);
        mInteger.set(mInteger.get() + 1);
        mLatch.await();
        assertEquals(4, mInteger.get());
    }

    @Test
    public void incrementTimerDuration() throws Exception {
        Timer timer = new Timer();
        TimerOperation operation = timer.countForSeconds(1, mListener);
        operation.incrementBy(1);
        try {
            verify(mListener, timeout(1200)).onTimeout();
            fail();
        } catch (Throwable e) {
            // Garante que não foi chamado antes
        }
        verify(mListener, timeout(2200)).onTimeout();
    }

    @Test
    public void resetTimerCounter() throws Exception {
        Timer timer = new Timer();
        long millis = System.currentTimeMillis();
        final CountDownLatch latch = new CountDownLatch(1);
        TimerOperation operation = timer.countForSeconds(2, new OnTimeoutListener() {
            @Override
            public void onTimeout() {
                latch.countDown();
            }
        });
        try {
            verify(mListener, timeout(1200)).onTimeout();
            fail();
        } catch (Throwable e) {
            // Garante que não foi chamado antes
        }
        operation.resetTime();
        latch.await();
        long now = System.currentTimeMillis();
        assertTrue((now - millis) > 2500);
    }

    @Test
    public void stopAvoidListenerToBeCalled() throws Exception {
        Timer timer = new Timer();
        TimerOperation operation = timer.countForSeconds(1, mListener);
        timer.cancel(operation);
        mLatch.await(2, TimeUnit.SECONDS);
        assertEquals(1, mLatch.getCount());
    }

    @Test
    public void whenTimerFinishYouCanRunAgain() throws Exception {
        Timer timer = new Timer();
        timer.countForSeconds(1, mListener);
        mLatch.await();
        mLatch = new CountDownLatch(1);
        timer.countForSeconds(1, mListener);
        mLatch.await();
    }

}
