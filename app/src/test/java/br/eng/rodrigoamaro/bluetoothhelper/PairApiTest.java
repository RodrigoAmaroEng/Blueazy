package br.eng.rodrigoamaro.bluetoothhelper;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;

import org.junit.Before;
import org.junit.Ignore;
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

import static android.bluetooth.BluetoothDevice.ACTION_ACL_CONNECTED;
import static android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECTED;
import static android.bluetooth.BluetoothDevice.ACTION_BOND_STATE_CHANGED;
import static android.bluetooth.BluetoothDevice.BOND_BONDED;
import static android.bluetooth.BluetoothDevice.BOND_NONE;
import static br.eng.rodrigoamaro.bluetoothhelper.PairApi.ACTION_FAKE_PAIR_REQUEST;
import static br.eng.rodrigoamaro.bluetoothhelper.PairApi.ACTION_PAIRING_ON_PROGRESS;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Durante o pareamento aguardamos o evento BOND_STATE_CHANGE para valor 12 (Sucesso)
 * Caso recebamos o valor 10 esperamos 3 segundos para enviar um erro
 * Se dentro destes 3 segundos recebermos um valor 11 aguardamos até o recebimento de outro 10 para retornar o erro
 */
@RunWith(RobolectricTestRunner.class)
public class PairApiTest {
    static {
        // redirect the Log.x output to stdout. Stdout will be recorded in the test result report
        ShadowLog.stream = System.out;
    }

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock
    BluetoothAdapter mAdapter;

    @Mock
    PairingSystem mPairingSystem;

    @Mock
    BluetoothDevice mDevice;

    ContextProvider mContextProvider = new ContextProvider() {
        @Override
        public Context getContext() {
            return RuntimeEnvironment.application;
        }
    };

    private static final String MAC_ADDRESS_1 = "00:11:22:33:44:55";
    private static final String MAC_ADDRESS_2 = "00:11:22:33:44:66";

    @Before
    public void setUp() {
        doReturn(BOND_NONE).when(mDevice).getBondState();
        doReturn(MAC_ADDRESS_1).when(mDevice).getAddress();
        doReturn(mDevice).when(mAdapter).getRemoteDevice(eq(MAC_ADDRESS_1));

    }

    @Test
    public void pairMustCallDevicePairSystem() throws Exception {
        PairApi api = new PairApi(mContextProvider, mAdapter, mPairingSystem);
        Observable<PairEvent> observable = api.pair(MAC_ADDRESS_1);
        TestSubscriber<PairEvent> subscriber = new TestSubscriber<>();
        observable.subscribe(subscriber);
        sendDeviceBounded(mDevice, "PAX-12345678", MAC_ADDRESS_1);
        subscriber.awaitTerminalEvent();
        verify(mPairingSystem).pair(eq(mDevice));
    }

    @Test
    public void deviceIsSuccessfullyPaired() throws Exception {
        PairApi api = new PairApi(mContextProvider, mAdapter, mPairingSystem);
        Observable<PairEvent> observable = api.pair(MAC_ADDRESS_1);
        TestSubscriber<PairEvent> subscriber = new TestSubscriber<>();
        observable.subscribe(subscriber);
        
        sendDeviceBounded(mDevice, "PAX-12345678", MAC_ADDRESS_1);
        subscriber.awaitTerminalEvent();
        subscriber.assertValue(new PairEvent(PairApi.ACTION_PAIRING_SUCCEEDED, mDevice));
        subscriber.assertCompleted();
    }

    @Test
    public void pairingSuccessAfterReceiveDeviceNotBounded() throws Exception {
        PairApi api = new PairApi(mContextProvider, mAdapter, mPairingSystem);
        Observable<PairEvent> observable = api.pair(MAC_ADDRESS_1);
        TestSubscriber<PairEvent> subscriber = new TestSubscriber<>();
        observable.subscribe(subscriber);
        
        sendDeviceNotBounded(mDevice, "PAX-12345678", MAC_ADDRESS_1);
        sendDeviceBounded(mDevice, "PAX-12345678", MAC_ADDRESS_1);
        subscriber.awaitTerminalEvent();
        subscriber.assertValues(new PairEvent(PairApi.ACTION_PAIRING_NOT_DONE, mDevice),
                new PairEvent(PairApi.ACTION_PAIRING_SUCCEEDED, mDevice));
        subscriber.assertCompleted();
    }

    @Test
    public void anotherDeviceIsPaired() throws Exception {
        PairApi api = new PairApi(mContextProvider, mAdapter, mPairingSystem);
        Observable<PairEvent> observable = api.pair(MAC_ADDRESS_1);
        TestSubscriber<PairEvent> subscriber = new TestSubscriber<>();
        observable.subscribe(subscriber);
        
        sendDeviceBounded(mDevice, "PAX-12345678", MAC_ADDRESS_2);
        subscriber.assertNoValues();
        subscriber.assertNotCompleted();
    }

    @Test
    public void returnSuccessWhenDeviceAlreadyPaired() throws Exception {
        doReturn(BOND_BONDED).when(mDevice).getBondState();
        PairApi api = new PairApi(mContextProvider, mAdapter, mPairingSystem);
        Observable<PairEvent> observable = api.pair(MAC_ADDRESS_1);
        TestSubscriber<PairEvent> subscriber = new TestSubscriber<>();
        observable.subscribe(subscriber);
        subscriber.awaitTerminalEvent();
        subscriber.assertValue(new PairEvent(PairApi.ACTION_PAIRING_SUCCEEDED, mDevice));
        verify(mPairingSystem, never()).pair(eq(mDevice));
    }

    @Test
    public void pairingFailed() throws Exception {
        PairApi api = new PairApi(mContextProvider, mAdapter, mPairingSystem);
        Observable<PairEvent> observable = api.pair(MAC_ADDRESS_1);
        TestSubscriber<PairEvent> subscriber = new TestSubscriber<>();
        observable.subscribe(subscriber);
        sendPairingFailed(mock(BluetoothDevice.class), MAC_ADDRESS_1);
        subscriber.awaitTerminalEvent();
        subscriber.assertNoValues();
        subscriber.assertError(DevicePairingFailed.class);
    }

    @Test
    public void cancelPairOperationSameAsTimeout() throws Exception {
        PairApi api = new PairApi(mContextProvider, mAdapter, mPairingSystem);
        Observable<PairEvent> observable = api.pair(MAC_ADDRESS_1);
        TestSubscriber<PairEvent> subscriber = new TestSubscriber<>();
        observable.subscribe(subscriber);
        sendTimeout(mock(BluetoothDevice.class), MAC_ADDRESS_1);
        subscriber.awaitTerminalEvent();
        subscriber.assertNoValues();
        subscriber.assertError(DevicePairingTimeout.class);
    }

    @Test
    public void pairingFailedWhenTryToPerform() throws Exception {
        doThrow(new DevicePairingFailed()).when(mPairingSystem).pair(any(BluetoothDevice.class));
        PairApi api = new PairApi(mContextProvider, mAdapter, mPairingSystem);
        Observable<PairEvent> observable = api.pair(MAC_ADDRESS_1);
        TestSubscriber<PairEvent> subscriber = new TestSubscriber<>();
        observable.subscribe(subscriber);
        subscriber.awaitTerminalEvent();
        subscriber.assertNoValues();
        subscriber.assertError(DevicePairingFailed.class);
    }

    @Test
    public void sendBroadcastMessageForTimeout() throws Exception {
        PairApi api = new PairApi(mContextProvider, mAdapter, mPairingSystem);
        Observable<PairEvent> observable = api.pair(MAC_ADDRESS_1);
        TestSubscriber<PairEvent> subscriber = new TestSubscriber<>();
        observable.subscribe(subscriber);
        api.sendTimeoutMessage(MAC_ADDRESS_1);
        subscriber.awaitTerminalEvent();
        subscriber.assertError(DevicePairingTimeout.class);
    }

    @Test
    public void sendBroadcastMessageForError() throws Exception {
        PairApi api = new PairApi(mContextProvider, mAdapter, mPairingSystem);
        Observable<PairEvent> observable = api.pair(MAC_ADDRESS_1);
        TestSubscriber<PairEvent> subscriber = new TestSubscriber<>();
        observable.subscribe(subscriber);
        api.sendErrorMessage(MAC_ADDRESS_1);
        subscriber.awaitTerminalEvent();
        subscriber.assertError(DevicePairingFailed.class);
    }

    @Test
    public void translateDisconnectMessageAsError() throws Exception {
        PairApi api = new PairApi(mContextProvider, mAdapter, mPairingSystem);
        Observable<PairEvent> observable = api.pair(MAC_ADDRESS_1);
        TestSubscriber<PairEvent> subscriber = new TestSubscriber<>();
        observable.subscribe(subscriber);
        sendDisconnectMessage(mock(BluetoothDevice.class), MAC_ADDRESS_1);
        subscriber.assertNoValues();
        subscriber.assertError(DevicePairingFailed.class);
    }

    @Test
    public void translatePairingRequestMessageAsOnProgress() throws Exception {
        PairApi api = new PairApi(mContextProvider, mAdapter, mPairingSystem);
        Observable<PairEvent> observable = api.pair(MAC_ADDRESS_1);
        TestSubscriber<PairEvent> subscriber = new TestSubscriber<>();
        observable.subscribe(subscriber);
        sendPairRequestMessage(mDevice, MAC_ADDRESS_1);
        subscriber.assertValue(new PairEvent(ACTION_PAIRING_ON_PROGRESS, mDevice));
    }

    @Test
    public void translateConnectedMessageAsOnProgress() throws Exception {
        PairApi api = new PairApi(mContextProvider, mAdapter, mPairingSystem);
        Observable<PairEvent> observable = api.pair(MAC_ADDRESS_1);
        TestSubscriber<PairEvent> subscriber = new TestSubscriber<>();
        observable.subscribe(subscriber);
        sendConnectedMessage(mDevice, MAC_ADDRESS_1);
        subscriber.assertValue(new PairEvent(ACTION_PAIRING_ON_PROGRESS, mDevice));
    }

    private void sendConnectedMessage(BluetoothDevice device, String macAddress) {
        sendBroadcastMessage(device, "", macAddress, -1, ACTION_ACL_CONNECTED);
    }

    private void sendPairRequestMessage(BluetoothDevice device, String macAddress) {
        sendBroadcastMessage(device, "", macAddress, -1, ACTION_FAKE_PAIR_REQUEST);
    }

    private void sendDisconnectMessage(BluetoothDevice device, String macAddress) {
        sendBroadcastMessage(device, "", macAddress, -1, ACTION_ACL_DISCONNECTED);
    }

    // TODO: Parar possível busca em andamento antes de parear (Talvez não seja aqui)

    /**
     * O código abaixo parece fornecer uma solução para o problema do pareamento (API 19+)
     * <p>
     * Intent pairingIntent = new Intent(BluetoothDevice.ACTION_PAIRING_REQUEST);
     * pairingIntent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
     * pairingIntent.putExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT,
     * BluetoothDevice.PAIRING_VARIANT_PASSKEY_CONFIRMATION);
     * pairingIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
     * startActivityForResult(pairingIntent, REQUEST_BT_PAIRING);
     */
    private void sendDeviceBounded(BluetoothDevice device, String name, String macAddress) {
        sendBroadcastMessage(device, name, macAddress, BOND_BONDED, ACTION_BOND_STATE_CHANGED);
    }

    private void sendDeviceNoState(BluetoothDevice device, String name, String macAddress) {
        sendBroadcastMessage(device, name, macAddress, -1, ACTION_BOND_STATE_CHANGED);
    }

    private void sendDeviceNotBounded(BluetoothDevice device, String name, String macAddress) {
        sendBroadcastMessage(device, name, macAddress, BOND_NONE, ACTION_BOND_STATE_CHANGED);
    }

    private void sendPairingFailed(BluetoothDevice device, String macAddress) {
        sendBroadcastMessage(device, "", macAddress, -1, PairApi.ACTION_PAIRING_FAILED);
    }

    private void sendTimeout(BluetoothDevice device, String macAddress) {
        sendBroadcastMessage(device, "", macAddress, -1, PairApi.ACTION_PAIRING_TIMEOUT);
    }

    private void sendBroadcastMessage(BluetoothDevice device, String name, String macAddress,
                                      int state, String action) {
        Intent intent = new Intent(action);
        doReturn(macAddress).when(device).getAddress();
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
        intent.putExtra(BluetoothDevice.EXTRA_NAME, name);
        intent.putExtra(BluetoothDevice.EXTRA_BOND_STATE, state);
        RuntimeEnvironment.application.sendBroadcast(intent);
    }


}
