package br.eng.rodrigoamaro.bluetoothhelper;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowLog;

import rx.Observable;
import rx.observers.TestSubscriber;

import static android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_FINISHED;
import static android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_STARTED;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
public class SearchApiTest {
    static {
        // redirect the Log.x output to stdout. Stdout will be recorded in the test result report
        ShadowLog.stream = System.out;
    }

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock
    BluetoothAdapter mAdapter;

    ContextProvider mContextProvider = new ContextProvider() {
        @Override
        public Context getContext() {
            return RuntimeEnvironment.application;
        }
    };

    @Test
    public void checkBluetoothStatusOnAdapter() {
        SearchApi eventHandler = new SearchApi(mContextProvider, mAdapter);
        doReturn(true).when(mAdapter).isEnabled();
        assertEquals(true, eventHandler.isBluetoothOn());
    }

    @Test
    public void stopMethodMustCallCancelDiscovery() {
        SearchApi eventHandler = new SearchApi(mContextProvider, mAdapter);
        eventHandler.stop();
        verify(mAdapter).cancelDiscovery();
    }

    @Test
    public void doNotStartNewSearchCycleAfterSearchFinishesIfStopRequestHasBeenSent() {
        SearchApi eventHandler = new SearchApi(mContextProvider, mAdapter);
        Observable<SearchEvent> observable = eventHandler.search();
        TestSubscriber<SearchEvent> subscriber = new TestSubscriber<>();
        observable.subscribe(subscriber);
        eventHandler.stop();
        sendSearchFinishedMessage();
        verify(mAdapter, times(1)).startDiscovery();
        subscriber.assertCompleted();
    }

    @Test
    public void keepSearchingAfterSearchFinishesIfNoStopRequestHasBeenSent() {
        SearchApi eventHandler = new SearchApi(mContextProvider, mAdapter);
        Observable<SearchEvent> observable = eventHandler.search();
        TestSubscriber<SearchEvent> subscriber = new TestSubscriber<>();
        observable.subscribe(subscriber);
        sendSearchFinishedMessage();
        verify(mAdapter, times(2)).startDiscovery();
    }

    @Test
    public void sendNotificationWhenSearchStart() {
        SearchApi eventHandler = new SearchApi(mContextProvider, mAdapter);
        Observable<SearchEvent> observable = eventHandler.search();
        TestSubscriber<SearchEvent> subscriber = new TestSubscriber<>();
        observable.subscribe(subscriber);
        sendSearchStartedMessage();
        subscriber.assertValues(new SearchEvent());
        subscriber.assertNotCompleted();
    }

    @Test
    public void sendNotificationWhenFindDevice() {
        SearchApi eventHandler = new SearchApi(mContextProvider, mAdapter);
        Observable<SearchEvent> observable = eventHandler.search();
        TestSubscriber<SearchEvent> subscriber = new TestSubscriber<>();
        BluetoothDevice device = mock(BluetoothDevice.class);
        observable.subscribe(subscriber);
        sendSearchStartedMessage();
        sendDeviceFoundMessage(device);
        subscriber.assertValueCount(2);
        assertEquals(device, subscriber.getOnNextEvents().get(1).getDevice().getDetails());
        subscriber.assertNotCompleted();
    }

    @Test
    public void retrieveDeviceNameAndSignal() {
        SearchApi eventHandler = new SearchApi(mContextProvider, mAdapter);
        Observable<SearchEvent> observable = eventHandler.search();
        TestSubscriber<SearchEvent> subscriber = new TestSubscriber<>();
        BluetoothDevice device = mock(BluetoothDevice.class);
        observable.subscribe(subscriber);
        sendSearchStartedMessage();
        sendDeviceFoundMessage(device, "Name", -34);
        assertEquals("Name", subscriber.getOnNextEvents().get(1).getDevice().getName());
        assertEquals(-34, subscriber.getOnNextEvents().get(1).getDevice().getSignal());
    }

    @Test
    public void doNotSendNotificationWhenFinishSearchWithoutStopRequest() {
        SearchApi eventHandler = new SearchApi(mContextProvider, mAdapter);
        Observable<SearchEvent> observable = eventHandler.search();
        TestSubscriber<SearchEvent> subscriber = new TestSubscriber<>();
        observable.subscribe(subscriber);
        sendSearchStartedMessage();
        sendSearchFinishedMessage();
        subscriber.assertValues(new SearchEvent());
        subscriber.assertNotCompleted();
    }

    @Test
    public void sendNotificationOnlyWhenFinishSearchAfterStopRequest() {
        SearchApi eventHandler = new SearchApi(mContextProvider, mAdapter);
        Observable<SearchEvent> observable = eventHandler.search();
        TestSubscriber<SearchEvent> subscriber = new TestSubscriber<>();
        observable.subscribe(subscriber);
        sendSearchStartedMessage();
        eventHandler.stop();
        sendSearchFinishedMessage();
        subscriber.assertValues(new SearchEvent());
        subscriber.assertCompleted();
    }

    @Test
    public void turnBluetoothOnMustCallItOnAdapter() {
        SearchApi eventHandler = new SearchApi(mContextProvider, mAdapter);
        eventHandler.turnBluetoothOn().subscribe();
        verify(mAdapter).enable();
    }

    @Test
    public void sendNotificationWhenBluetoothIsOn() {
        SearchApi eventHandler = new SearchApi(mContextProvider, mAdapter);
        Observable<Intent> observable = eventHandler.turnBluetoothOn();
        TestSubscriber<Intent> subscriber = new TestSubscriber<>();
        observable.subscribe(subscriber);
        sendBluetoothIsOnMessage();
        subscriber.assertValueCount(0);
        subscriber.assertCompleted();
    }

    @Test
    public void sendDoNotPropagateDifferentEventsWhenTurningBluetoothOn() {
        SearchApi eventHandler = new SearchApi(mContextProvider, mAdapter);
        Observable<Intent> observable = eventHandler.turnBluetoothOn();
        TestSubscriber<Intent> subscriber = new TestSubscriber<>();
        observable.subscribe(subscriber);
        sendBluetoothIsOffMessage();
        subscriber.assertValueCount(0);
        subscriber.assertNotCompleted();
    }

    @Test
    public void turnBluetoothOffMustCallItOnAdapter() {
        SearchApi eventHandler = new SearchApi(mContextProvider, mAdapter);
        eventHandler.turnBluetoothOff().subscribe();
        verify(mAdapter).disable();
    }


    @Test
    public void sendNotificationWhenBluetoothIsOff() {
        SearchApi eventHandler = new SearchApi(mContextProvider, mAdapter);
        Observable<Intent> observable = eventHandler.turnBluetoothOff();
        TestSubscriber<Intent> subscriber = new TestSubscriber<>();
        observable.subscribe(subscriber);
        sendBluetoothIsOffMessage();
        subscriber.assertValueCount(0);
        subscriber.assertCompleted();
    }

    @Test
    public void sendDoNotPropagateDifferentEventsWhenTurningBluetoothOff() {
        SearchApi eventHandler = new SearchApi(mContextProvider,
                mAdapter);
        Observable<Intent> observable = eventHandler.turnBluetoothOff();
        TestSubscriber<Intent> subscriber = new TestSubscriber<>();
        observable.subscribe(subscriber);
        sendBluetoothIsOnMessage();
        subscriber.assertValueCount(0);
        subscriber.assertNotCompleted();
    }

    private void sendDeviceFoundMessage(BluetoothDevice device) {
        Intent intent = new Intent(BluetoothDevice.ACTION_FOUND);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
        RuntimeEnvironment.application.sendBroadcast(intent);
    }

    private void sendDeviceFoundMessage(BluetoothDevice device, String name, int signal) {
        Intent intent = new Intent(BluetoothDevice.ACTION_FOUND);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
        intent.putExtra(BluetoothDevice.EXTRA_NAME, name);
        intent.putExtra(BluetoothDevice.EXTRA_RSSI, signal);
        RuntimeEnvironment.application.sendBroadcast(intent);
    }

    private void sendSearchStartedMessage() {
        Intent intent = new Intent(ACTION_DISCOVERY_STARTED);
        RuntimeEnvironment.application.sendBroadcast(intent);
    }

    private void sendSearchFinishedMessage() {
        Intent intent = new Intent(ACTION_DISCOVERY_FINISHED);
        RuntimeEnvironment.application.sendBroadcast(intent);
    }

    private void sendBluetoothIsOffMessage() {
        Intent intent = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
        intent.putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
        RuntimeEnvironment.application.sendBroadcast(intent);
    }

    private void sendBluetoothIsOnMessage() {
        Intent intent = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
        intent.putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_ON);
        RuntimeEnvironment.application.sendBroadcast(intent);
    }

}
