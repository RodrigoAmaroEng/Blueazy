package dev.amaro.bluetoothhelper

import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito
import org.mockito.Spy
import org.mockito.junit.MockitoJUnit
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@RunWith(JUnit4::class)
class TimerTest {
    @Rule
    var mMockitoRule = MockitoJUnit.rule()
    var mLatch = CountDownLatch(1)
    var mInteger = AtomicInteger()

    @Spy
    var mListener = OnTimeoutListener {
        mInteger.set(mInteger.get() * 2)
        mLatch.countDown()
    }

    @Test
    @Throws(Exception::class)
    fun callListenerAfterTimeout() {
        val timer = Timer()
        timer.countForSeconds(1, mListener)
        Mockito.verify(mListener, Mockito.timeout(1200)).onTimeout()
    }

    @Test
    @Throws(Exception::class)
    fun waitSpecifiedTimeBeforeCallListener() {
        val timer = Timer()
        mInteger.set(1)
        timer.countForSeconds(1, mListener)
        mInteger.set(mInteger.get() + 1)
        mLatch.await()
        Assert.assertEquals(4, mInteger.get().toLong())
    }

    @Test
    @Throws(Exception::class)
    fun incrementTimerDuration() {
        val timer = Timer()
        val operation = timer.countForSeconds(1, mListener)
        operation.incrementBy(1)
        try {
            Mockito.verify(mListener, Mockito.timeout(1200)).onTimeout()
            Assert.fail()
        } catch (e: Throwable) {
            // Garante que não foi chamado antes
        }
        Mockito.verify(mListener, Mockito.timeout(2200)).onTimeout()
    }

    @Test
    @Throws(Exception::class)
    fun resetTimerCounter() {
        val timer = Timer()
        val millis = System.currentTimeMillis()
        val latch = CountDownLatch(1)
        val operation = timer.countForSeconds(2) { latch.countDown() }
        try {
            Mockito.verify(mListener, Mockito.timeout(1200)).onTimeout()
            Assert.fail()
        } catch (e: Throwable) {
            // Garante que não foi chamado antes
        }
        operation.resetTime()
        latch.await()
        val now = System.currentTimeMillis()
        Assert.assertTrue(now - millis > 2500)
    }

    @Test
    @Throws(Exception::class)
    fun stopAvoidListenerToBeCalled() {
        val timer = Timer()
        val operation = timer.countForSeconds(1, mListener)
        timer.cancel(operation)
        mLatch.await(2, TimeUnit.SECONDS)
        Assert.assertEquals(1, mLatch.count)
    }

    @Test
    @Throws(Exception::class)
    fun whenTimerFinishYouCanRunAgain() {
        val timer = Timer()
        timer.countForSeconds(1, mListener)
        mLatch.await()
        mLatch = CountDownLatch(1)
        timer.countForSeconds(1, mListener)
        mLatch.await()
    }
}