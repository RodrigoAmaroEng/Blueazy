package br.eng.rodrigoamaro.bluetoothhelper;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Build;
import android.os.Looper;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * TODO: Para gerar diversos exemplos podemos testar cenários variando
 * - O Celular
 * - O Leitor
 * - O método de pareamento (CreateBond,CreateRfCommSerial,CreateRfCommX)
 * Durante pareamento devemos checar como o celular identifica o leitor
 * TODO: Parece que o Broadcast receiver não é removido em casos onde o Rx é interrompido...
 * Como fica quando receber uma solicitação de cancelamento?
 **/
@RunWith(AndroidJUnit4.class)
public class RealPairTest {

    private static final String MAC_D180 = "8C:DE:52:C1:F0:13";
    private static final String MAC_D200 = "00:07:80:7A:C6:CB";
    private static final String MAC_MP10 = "D4:F5:13:5D:D2:06";
    private static final String TAG = "RealPairTest";

    // RealPairTest|PairApi|PairingSystem|System.err
    @Test
    public void testRealPair() throws Exception {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            Looper.prepare();
        }
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        PairApi api = new PairApi(appContext, adapter, new PairingSystem());
        final CountDownLatch semaphore = new CountDownLatch(1);
        api.pair(MAC_MP10)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread())
                .subscribe(new Subscriber<PairEvent>() {
                    @Override
                    public void onCompleted() {
                        Log.d(TAG, "Pair completed");
                        semaphore.countDown();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "Pair failed", e);
                        semaphore.countDown();
                    }

                    @Override
                    public void onNext(PairEvent device) {

                    }
                });
        semaphore.await();
    }
}
