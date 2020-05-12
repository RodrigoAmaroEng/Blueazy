package dev.amaro.bluetoothhelper;

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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(JUnit4.class)
public class SearchEngineTest {

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock
    SearchApi mSearchApi;

    @Before
    public void setUp() {
        setSuccessfulSearchStart();
        setSuccessWhenTurnOnBluetooth();
        setSuccessWhenTurnOffBluetooth();
        setBluetoothOn();
    }

    @Test
    public void whenAskToStopMustCallStopOnApi() throws Exception {
        SearchEngine engine = new SearchEngine(mSearchApi);
        engine.stop();
        verify(mSearchApi).stop();
    }

    @Test
    public void whenSearchFindDeviceMustNotify() throws Exception {
        Device device = mock(Device.class);
        setDeviceToDeliverAsFound(device);
        SearchEngine engine = new SearchEngine(mSearchApi);
        Observable<SearchEvent> observable = engine.search();
        TestSubscriber<SearchEvent> subscriber = new TestSubscriber<>();
        observable.subscribe(subscriber);
        assertEquals(device, subscriber.getOnNextEvents().get(0).getDevice());
    }

    @Test
    public void whenSearchFinishMustNotify() throws Exception {
        setSuccessfulSearchStop();
        SearchEngine engine = new SearchEngine(mSearchApi);
        Observable<SearchEvent> observable = engine.search();
        TestSubscriber<SearchEvent> subscriber = new TestSubscriber<>();
        observable.subscribe(subscriber);
        subscriber.assertValueCount(0);
    }


    @Test
    public void whenSearchStartMustNotify() throws Exception {
        SearchEngine engine = new SearchEngine(mSearchApi);
        Observable<SearchEvent> observable = engine.search();
        TestSubscriber<SearchEvent> subscriber = new TestSubscriber<>();
        observable.subscribe(subscriber);
        subscriber.assertValue(new SearchEvent());
    }

    @Test
    public void ifBluetoothIsOffMustTurnItOn() throws Exception {
        setBluetoothOff();
        SearchEngine engine = new SearchEngine(mSearchApi);
        Observable<SearchEvent> observable = engine.search();
        TestSubscriber<SearchEvent> subscriber = new TestSubscriber<>();
        observable.subscribe(subscriber);
        verify(mSearchApi).turnBluetoothOn();
    }

    @Test
    public void searchMustStartAfterBluetoothIsTurnedOn() throws Exception {
        setBluetoothOff();
        SearchEngine engine = new SearchEngine(mSearchApi);
        Observable<SearchEvent> observable = engine.search();
        TestSubscriber<SearchEvent> subscriber = new TestSubscriber<>();
        observable.subscribe(subscriber);
        subscriber.assertValue(new SearchEvent());
    }

    @Test
    public void searchMustResetBluetoothIfItsOn() throws Exception {
        setBluetoothOn();
        SearchEngine engine = new SearchEngine(mSearchApi);
        Observable<SearchEvent> observable = engine.search();
        TestSubscriber<SearchEvent> subscriber = new TestSubscriber<>();
        observable.subscribe(subscriber);
        verify(mSearchApi).turnBluetoothOff();
        verify(mSearchApi).turnBluetoothOn();
    }

    private void setDeviceToDeliverAsFound(Device device) {
        doReturn(Observable.just(new SearchEvent(device))).when(mSearchApi).search();
    }

    private void setSuccessWhenTurnOnBluetooth() {
        doReturn(Observable.empty()).when(mSearchApi).turnBluetoothOn();
    }

    private void setSuccessWhenTurnOffBluetooth() {
        doReturn(Observable.empty()).when(mSearchApi).turnBluetoothOff();
    }

    private void setSuccessfulSearchStart() {
        doReturn(Observable.just(new SearchEvent())).when(mSearchApi).search();
    }

    private void setSuccessfulSearchStop() {
        doReturn(Observable.empty()).when(mSearchApi).search();
    }

    private void setBluetoothOn() {
        doReturn(true).when(mSearchApi).isBluetoothOn();
    }

    private void setBluetoothOff() {
        doReturn(false).when(mSearchApi).isBluetoothOn();
    }
}
