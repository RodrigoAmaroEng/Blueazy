package dev.amaro.bluetoothhelper;

import android.bluetooth.BluetoothDevice;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import rx.Observable;
import rx.observers.TestSubscriber;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(JUnit4.class)
public class PairEngineTest {

    private static final String MAC_ADDRESS_1 = "00:11:22:33:44:55";

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock
    PairApi mPairApi;

    @Before
    public void setUp() {
        setSuccessWhenTurnOffBluetooth();
        setSuccessWhenTurnOnBluetooth();
    }

    @Test
    public void turnBluetoothOnBeforeStartPairing() {
        setBluetoothOff();
        PairEngine engine = new PairEngine(mPairApi);
        Observable<PairEvent> observable = engine.pair(MAC_ADDRESS_1);
        TestSubscriber<PairEvent> subscriber = new TestSubscriber<>();
        observable.subscribe(subscriber);
        verify(mPairApi).turnBluetoothOn();
    }

    @Test
    public void ifBluetoothIsOnTurnOffAndOnBeforeStartPairing() {
        setBluetoothOn();
        PairEngine engine = new PairEngine(mPairApi);
        Observable<PairEvent> observable = engine.pair(MAC_ADDRESS_1);
        TestSubscriber<PairEvent> subscriber = new TestSubscriber<>();
        observable.subscribe(subscriber);
        verify(mPairApi).turnBluetoothOff();
        verify(mPairApi).turnBluetoothOn();
    }

    @Test
    public void returnPairedDeviceAfterSucceed() {
        BluetoothDevice device = mock(BluetoothDevice.class);
        setPairRequestSucceeded(device);
        PairEngine engine = new PairEngine(mPairApi);
        Observable<PairEvent> observable = engine.pair(MAC_ADDRESS_1);
        TestSubscriber<PairEvent> subscriber = new TestSubscriber<>();
        observable.subscribe(subscriber);
        subscriber.awaitTerminalEvent();
        subscriber.assertValue(new PairEvent(PairApi.ACTION_PAIRING_SUCCEEDED, device));
        subscriber.assertNoErrors();
    }

    @Test
    public void notifyTimeoutCallsSendTimeoutMessageOnApi() {
        PairEngine engine = new PairEngine(mPairApi);
        engine.notifyTimeout(MAC_ADDRESS_1);
        verify(mPairApi).sendTimeoutMessage(eq(MAC_ADDRESS_1));
    }

    @Test
    public void notifyErrorCallsSendErrorMessageOnApi() {
        PairEngine engine = new PairEngine(mPairApi);
        engine.notifyError(MAC_ADDRESS_1);
        verify(mPairApi).sendErrorMessage(eq(MAC_ADDRESS_1));
    }

    private void setPairRequestSucceeded(BluetoothDevice device) {
        doReturn(Observable.just(new PairEvent(PairApi.ACTION_PAIRING_SUCCEEDED, device)))
                .when(mPairApi)
                .pair(anyString());
    }

    private void setBluetoothOn() {
        doReturn(true).when(mPairApi).isBluetoothOn();
    }

    private void setBluetoothOff() {
        doReturn(false).when(mPairApi).isBluetoothOn();
    }

    private void setSuccessWhenTurnOnBluetooth() {
        doReturn(Observable.empty()).when(mPairApi).turnBluetoothOn();
    }

    private void setSuccessWhenTurnOffBluetooth() {
        doReturn(Observable.empty()).when(mPairApi).turnBluetoothOff();
    }
}
