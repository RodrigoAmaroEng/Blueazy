package br.eng.rodrigoamaro.bluetoothhelper;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.stubbing.Answer;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.observers.TestSubscriber;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(JUnit4.class)
public class PairRequestTest {

    private static final String MAC_ADDRESS_1 = "00:11:22:33:44:55";

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock
    public PairEngine mPairEngine;

    @Mock
    public Timer mTimer;

    @Mock
    public BluetoothDevice mDevice;

    @Mock
    public BluetoothAdapter mAdapter;

    @Mock
    public TimerOperation mOperation;

    @Before
    public void setUp() {
        LibModule.setPairEngine(mPairEngine);
        LibModule.setTimer(mTimer);
        LibModule.setBluetoothAdapter(mAdapter);
        doReturn(mOperation).when(mTimer).countForSeconds(anyInt(), any(OnTimeoutListener.class));
    }

    @Test
    public void pairRequestSuccessfullyCompleted() throws Exception {
        PairRequest request = new PairRequest(MAC_ADDRESS_1, mock(ContextProvider.class));
        setDevicePairedMessage(mDevice);
        TestSubscriber<BluetoothDevice> subscriber = new TestSubscriber<>();
        request.perform().subscribe(subscriber);
        subscriber.awaitTerminalEvent();
        subscriber.assertNoErrors();
        subscriber.assertValue(mDevice);
    }

    @Test
    public void sendTimeoutMessageWhenTimerTimeout() throws Exception {
        PairRequest request = new PairRequest(MAC_ADDRESS_1, mock(ContextProvider.class));
        TestSubscriber<BluetoothDevice> subscriber = new TestSubscriber<>();
        setLongWaitToDeliverMessage(mDevice);
        setTimerEndImmediately();
        request.perform().subscribe(subscriber);
        subscriber.awaitTerminalEvent();
        verify(mPairEngine).notifyTimeout(eq(MAC_ADDRESS_1));
    }

    @Test
    public void detectErrorCaseWhenPairingRequestWasReceivedButTheStateReceivedAfterWasNotBound() {
        PairRequest request = new PairRequest(MAC_ADDRESS_1, mock(ContextProvider.class));
        setPairNotDoneAfterRequest(mDevice);
        TestSubscriber<BluetoothDevice> subscriber = new TestSubscriber<>();
        request.perform().subscribe(subscriber);
        verify(mPairEngine).notifyError(eq(MAC_ADDRESS_1));
    }

    @Test
    public void doNotThreatAsErrorWhenReceiveNotBondMessageBeforePairingRequestMessage() {
        PairRequest request = new PairRequest(MAC_ADDRESS_1, mock(ContextProvider.class));
        setDevicePairedMessage(mDevice);
        TestSubscriber<BluetoothDevice> subscriber = new TestSubscriber<>();
        request.perform().subscribe(subscriber);
        subscriber.awaitTerminalEvent();
        verify(mPairEngine, never()).notifyError(anyString());
    }

    @Test
    public void stopTimerWhenComplete() {
        PairRequest request = new PairRequest(MAC_ADDRESS_1, mock(ContextProvider.class));
        setPairNotDoneBeforeRequest(mDevice);
        TestSubscriber<BluetoothDevice> subscriber = new TestSubscriber<>();
        request.perform().subscribe(subscriber);
        verify(mTimer).cancel(eq(mOperation));
    }

    @Test
    public void whenReceiveMessageInformingAboutPairOnProgressIncrementTimerBy5Seconds() {
        PairRequest request = new PairRequest(MAC_ADDRESS_1, mock(ContextProvider.class));
        setPairNotDoneAfterRequest(mDevice);
        TestSubscriber<BluetoothDevice> subscriber = new TestSubscriber<>();
        request.perform().subscribe(subscriber);
        verify(mOperation).resetTime();
    }

    @Test
    public void doNotStartTimerBeforeReceiveStartEvent() {
        PairRequest request = new PairRequest(MAC_ADDRESS_1, mock(ContextProvider.class));
        TestSubscriber<BluetoothDevice> subscriber = new TestSubscriber<>();
        setNoStartMessage();
        request.perform().subscribe(subscriber);
        verify(mTimer, never()).countForSeconds(anyInt(), any(OnTimeoutListener.class));
    }

    private void setTimerEndImmediately() {
        doAnswer(
                new Answer() {
                    @Override
                    public Object answer(InvocationOnMock invocation) throws Throwable {
                        OnTimeoutListener listener = (OnTimeoutListener) invocation.getArguments()[1];
                        listener.onTimeout();
                        return mOperation;
                    }
                }
        ).when(mTimer).countForSeconds(anyInt(), any(OnTimeoutListener.class));
    }

    private void setDevicePairedMessage(BluetoothDevice device) {
        doReturn(Observable.just(new PairEvent(PairApi.ACTION_PAIRING_SUCCEEDED, device)))
                .when(mPairEngine).pair(anyString());
    }

    private void setNoStartMessage() {
        doReturn(Observable.empty()).when(mPairEngine).pair(anyString());
    }

    private void setLongWaitToDeliverMessage(BluetoothDevice device) {
        doReturn(Observable.just(new PairEvent(PairApi.ACTION_PAIRING_STARTED, device))
                .delay(500, TimeUnit.MILLISECONDS)).when(mPairEngine).pair(anyString());
    }

    private void setPairNotDoneBeforeRequest(BluetoothDevice device) {
        doReturn(Observable.just(
                new PairEvent(PairApi.ACTION_PAIRING_STARTED, device),
                new PairEvent(PairApi.ACTION_PAIRING_NOT_DONE, device),
                new PairEvent(PairApi.ACTION_PAIRING_ON_PROGRESS, device)
        )).when(mPairEngine).pair(anyString());
    }

    private void setPairNotDoneAfterRequest(BluetoothDevice device) {
        doReturn(Observable.just(
                new PairEvent(PairApi.ACTION_PAIRING_STARTED, device),
                new PairEvent(PairApi.ACTION_PAIRING_ON_PROGRESS, device),
                new PairEvent(PairApi.ACTION_PAIRING_NOT_DONE, device)
        )).when(mPairEngine).pair(anyString());
    }
}
