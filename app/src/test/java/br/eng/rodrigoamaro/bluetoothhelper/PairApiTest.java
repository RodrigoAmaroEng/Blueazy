package br.eng.rodrigoamaro.bluetoothhelper;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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

import static android.bluetooth.BluetoothDevice.ACTION_BOND_STATE_CHANGED;
import static android.bluetooth.BluetoothDevice.BOND_BONDED;
import static android.bluetooth.BluetoothDevice.BOND_NONE;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
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

    private static final String MAC_ADDRESS_1 = "00:11:22:33:44:55";
    private static final String MAC_ADDRESS_2 = "00:11:22:33:44:66";

    @Test
    public void pairMustCallDevicePairSystem() throws Exception {
        BluetoothDevice device = mock(BluetoothDevice.class);
        doReturn(device).when(mAdapter).getRemoteDevice(eq(MAC_ADDRESS_1));
        PairApi api = new PairApi(RuntimeEnvironment.application, mAdapter, mPairingSystem);
        Observable<PairEvent> observable = api.pair(MAC_ADDRESS_1);
        TestSubscriber<PairEvent> subscriber = new TestSubscriber<>();
        observable.subscribe(subscriber);
        verify(mPairingSystem).pair(eq(device));
    }

    @Test
    public void deviceIsSuccessfullyPaired() throws Exception {
        PairApi api = new PairApi(RuntimeEnvironment.application, mAdapter, mPairingSystem);
        Observable<PairEvent> observable = api.pair(MAC_ADDRESS_1);
        TestSubscriber<PairEvent> subscriber = new TestSubscriber<>();
        observable.subscribe(subscriber);
        BluetoothDevice device = mock(BluetoothDevice.class);
        sendDeviceBounded(device, "PAX-12345678", MAC_ADDRESS_1);
        subscriber.awaitTerminalEvent();
        subscriber.assertValue(new PairEvent(PairApi.ACTION_PAIRING_SUCCEEDED, device));
        subscriber.assertCompleted();
    }

    @Test
    public void pairingSuccessAfterReceiveDeviceNotBounded() throws Exception {
        PairApi api = new PairApi(RuntimeEnvironment.application, mAdapter, mPairingSystem);
        Observable<PairEvent> observable = api.pair(MAC_ADDRESS_1);
        TestSubscriber<PairEvent> subscriber = new TestSubscriber<>();
        observable.subscribe(subscriber);
        BluetoothDevice device = mock(BluetoothDevice.class);
        sendDeviceNotBounded(device, "PAX-12345678", MAC_ADDRESS_1);
        sendDeviceBounded(device, "PAX-12345678", MAC_ADDRESS_1);
        subscriber.awaitTerminalEvent();
        subscriber.assertValues(new PairEvent(PairApi.ACTION_PAIRING_NOT_DONE, device),
                new PairEvent(PairApi.ACTION_PAIRING_SUCCEEDED, device));
        subscriber.assertCompleted();
    }

    @Test
    public void anotherDeviceIsPaired() throws Exception {
        PairApi api = new PairApi(RuntimeEnvironment.application, mAdapter, mPairingSystem);
        Observable<PairEvent> observable = api.pair(MAC_ADDRESS_1);
        TestSubscriber<PairEvent> subscriber = new TestSubscriber<>();
        observable.subscribe(subscriber);
        BluetoothDevice device = mock(BluetoothDevice.class);
        sendDeviceBounded(device, "PAX-12345678", MAC_ADDRESS_2);
        subscriber.assertNoValues();
        subscriber.assertNotCompleted();
    }

    @Test
    public void pairingFailed() throws Exception {
        PairApi api = new PairApi(RuntimeEnvironment.application, mAdapter, mPairingSystem);
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
        PairApi api = new PairApi(RuntimeEnvironment.application, mAdapter, mPairingSystem);
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
        PairApi api = new PairApi(RuntimeEnvironment.application, mAdapter, mPairingSystem);
        Observable<PairEvent> observable = api.pair(MAC_ADDRESS_1);
        TestSubscriber<PairEvent> subscriber = new TestSubscriber<>();
        observable.subscribe(subscriber);
        subscriber.awaitTerminalEvent();
        subscriber.assertNoValues();
        subscriber.assertError(DevicePairingFailed.class);
    }

    // TODO: Multiplas tentativas
    // TODO: Timeout (Talvez não seja aqui)
    // TODO: Tipos de pareamento (Outra classe deveria controlar)
    // TODO: Parar possível busca em andamento antes de parear (Talvez não seja aqui)


    /**
     * TODO: O código abaixo parece fornecer uma solução para o problema do pareamento (API 19+)
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
