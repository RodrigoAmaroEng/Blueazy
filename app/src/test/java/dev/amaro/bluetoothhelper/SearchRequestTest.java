package dev.amaro.bluetoothhelper;

import android.bluetooth.BluetoothAdapter;

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

import java.util.concurrent.CountDownLatch;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.observers.TestSubscriber;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(JUnit4.class)
public class SearchRequestTest {

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock
    Timer mTimer;

    @Mock
    TimerOperation mOperation;

    @Mock
    SearchEngine mEngine;

    @Mock
    ContextProvider mContextProvider;

    @Before
    public void setUp() {
        doReturn(mOperation).when(mTimer).countForSeconds(anyInt(), any(OnTimeoutListener.class));
        LibModule.setSearchEngine(mEngine);
        LibModule.setTimer(mTimer);
        LibModule.setBluetoothAdapter(mock(BluetoothAdapter.class));
        setupStartEventWhenSearch();
    }

    @Test
    public void timerIsNotStartedIfOnStartIsNotCalled() throws Exception {
        Device device = mock(Device.class);
        setFoundDevicesOnEngine(device);
        SearchRequest request = new SearchRequest.Builder(mContextProvider).create();
        request.perform().subscribe();
        verify(mTimer, never()).countForSeconds(anyInt(), any(OnTimeoutListener.class));
    }

    @Test
    public void whenFindDeviceInsertOnList() throws Exception {
        Device device = mock(Device.class);
        setFoundDevicesOnEngine(device);
        SearchRequest request = new SearchRequest.Builder(mContextProvider).create();
        request.perform().subscribe();
        assertEquals(device, request.getDevices()[0]);
    }

    @Test
    public void whenStartSearchClearsTheDeviceList() throws Exception {
        Device device = mock(Device.class);
        setFoundDevicesOnEngine(device);
        SearchRequest request = new SearchRequest.Builder(mContextProvider).create();
        request.perform().subscribe();
        request.perform().subscribe();
        assertEquals(1, request.getDevices().length);
    }

    @Test
    public void whenTimeEndStopSearch() throws Exception {
        setInstantaneousFinishOnTimer();
        SearchRequest request = new SearchRequest.Builder(mContextProvider).create();
        request.perform().subscribe();
        verify(mEngine).stop();
    }

    @Test
    public void doNotStopSearchBeforeTimeEnds() throws Exception {
        SearchRequest request = new SearchRequest.Builder(mContextProvider).create();
        request.perform().subscribe();
        verify(mEngine, never()).stop();
    }

    @Test
    public void ifRequestToStopMustCallStopOnEngine() throws Exception {
        SearchRequest request = new SearchRequest.Builder(mContextProvider).create();
        request.stop();
        verify(mEngine).stop();
    }

    @Test
    public void ifRequestToStopTurnOffTimer() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        Observable<SearchEvent> observable = Observable.just(new SearchEvent())
                .doOnNext(new Action1<SearchEvent>() {
                    @Override
                    public void call(SearchEvent searchEvent) {
                        latch.countDown();
                    }
                });
        doReturn(observable).when(mEngine).search();
        SearchRequest request = new SearchRequest.Builder(mContextProvider).create();
        request.perform().subscribe();
        latch.await();
        request.stop();
        verify(mTimer).cancel(eq(mOperation));
    }

    @Test
    public void searchDefaultDurationMustBe30Seconds() throws Exception {
        SearchRequest request = new SearchRequest.Builder(mContextProvider).create();
        request.perform().subscribe();
        verify(mTimer).countForSeconds(eq(30), any(OnTimeoutListener.class));
    }

    @Test
    public void filterByName() throws Exception {
        setFoundDevicesOnEngine(newDevice("MP", -12), newDevice("PAX123", -12),
                newDevice("MP2", -12), newDevice("PAX324", -12));
        SearchRequest request = new SearchRequest.Builder(mContextProvider)
                .filterByPrefix("PAX").create();
        Observable<Device> observable = request.perform();
        TestSubscriber<Device> subscriber = new TestSubscriber<>();
        observable.subscribe(subscriber);
        subscriber.assertValueCount(2);
    }

    @Test
    public void capturedResultsWhenFilterByName() throws Exception {
        setFoundDevicesOnEngine(newDevice("MP", -12), newDevice("PAX123", -12),
                newDevice("MP2", -12), newDevice("PAX324", -12));
        SearchRequest request = new SearchRequest.Builder(mContextProvider)
                .filterByPrefix("PAX").create();
        request.perform().subscribe();
        assertEquals(2, request.getDevices().length);
    }

    @Test
    public void filterBySignal() throws Exception {
        setFoundDevicesOnEngine(newDevice("MP", -56), newDevice("PAX123", -43),
                newDevice("MP2", 51), newDevice("PAX324", 12));
        SearchRequest request = new SearchRequest.Builder(mContextProvider)
                .filterBySignal(50).create();
        Observable<Device> observable = request.perform();
        TestSubscriber<Device> subscriber = new TestSubscriber<>();
        observable.subscribe(subscriber);
        subscriber.assertValueCount(2);
    }

    @Test
    public void filterBySignalAndName() throws Exception {
        setFoundDevicesOnEngine(newDevice("MP", -56), newDevice("PAX123", -43),
                newDevice("MP2", 51), newDevice("PAX324", 12));
        SearchRequest request = new SearchRequest.Builder(mContextProvider)
                .filterBySignal(50)
                .filterByPrefix("PAX")
                .create();
        Observable<Device> observable = request.perform();
        TestSubscriber<Device> subscriber = new TestSubscriber<>();
        observable.subscribe(subscriber);
        subscriber.assertValueCount(1);
    }

    private Device newDevice(String name, int signal) {
        Device device = mock(Device.class);
        doReturn(name).when(device).getName();
        doReturn(signal).when(device).getSignal();
        return device;
    }

    private void setFoundDevicesOnEngine(final Device... devices) {
        doReturn(Observable.from(devices).map(new Func1<Device, SearchEvent>() {
            @Override
            public SearchEvent call(Device device) {
                return new SearchEvent(device);
            }
        })).when(mEngine).search();
    }

    private void setupStartEventWhenSearch() {
        doReturn(Observable.just(new SearchEvent())).when(mEngine).search();
    }

    private void setInstantaneousFinishOnTimer() {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                ((OnTimeoutListener) invocationOnMock.getArguments()[1]).onTimeout();
                return mOperation;
            }
        }).when(mTimer).countForSeconds(anyInt(), any(OnTimeoutListener.class));
    }


}