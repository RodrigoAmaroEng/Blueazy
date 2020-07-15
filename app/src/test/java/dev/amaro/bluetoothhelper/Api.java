package dev.amaro.bluetoothhelper;


import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.annotation.NonNull;

import org.junit.Assert;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.observers.TestSubscriber;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class Api {
    public static final String VALUE = "VALUE";
    private final Application mApplication;
    private Observable<Intent> mObservable;
    private TestSubscriber<Intent> mSubscriber = new TestSubscriber<>();

    public Api(Application application) {
        mApplication = spy(application);
    }

    public void createRegisterFor(String message, final int completeValue, final int firstValue) {
        mObservable = new RxBroadcast.Builder(mApplication)
                .addFilter(message)
                .setExitCondition(checkEqualValue(completeValue))
                .setStartOperation(firstAction(message, firstValue))
                .build()
                .flatMap(generateErrorOnZeroValue());
    }

    public void createRegisterFor(String message, final int completeValue) {
        mObservable = new RxBroadcast.Builder(mApplication)
                .addFilter(message)
                .setExitCondition(checkEqualValue(completeValue))
                .build()
                .flatMap(generateErrorOnZeroValue());
    }

    public void createInclusiveRegisterFor(String message, final int completeValue) {
        mObservable = new RxBroadcast.Builder(mApplication)
                .addFilter(message)
                .setExitCondition(checkEqualValue(completeValue))
                .setIncludeExitConditionEvent()
                .build()
                .flatMap(generateErrorOnZeroValue());
    }

    public void createInclusiveRegisterFor(String message, final int completeValue, final int firstValue) {
        mObservable = new RxBroadcast.Builder(mApplication)
                .addFilter(message)
                .setExitCondition(checkEqualValue(completeValue))
                .setStartOperation(firstAction(message, firstValue))
                .setIncludeExitConditionEvent()
                .build()
                .flatMap(generateErrorOnZeroValue());
    }

    public void createContinuousRegisterFor(String message) {
        mObservable = new RxBroadcast.Builder(mApplication)
                .addFilter(message)
                .build()
                .flatMap(generateErrorOnZeroValue());
    }

    public void createContinuousRegisterFor(String message, int firstValue) {
        mObservable = new RxBroadcast.Builder(mApplication)
                .addFilter(message)
                .setStartOperation(firstAction(message, firstValue))
                .build()
                .flatMap(generateErrorOnZeroValue());
    }

    @NonNull
    private Func0<Observable<Intent>> firstAction(final String message, final int value) {
        return new Func0<Observable<Intent>>() {
            @Override
            public Observable<Intent> call() {
                if (value == 0) {
                    return Observable.error(new Exception());
                } else if (value == -1) {
                    return Observable.empty();
                }
                Intent intent = new Intent(message);
                intent.putExtra(VALUE, value);
                return Observable.just(intent);
            }
        };
    }


    @NonNull
    private Func1<Intent, Boolean> checkEqualValue(final int completeValue) {
        return new Func1<Intent, Boolean>() {
            @Override
            public Boolean call(Intent intent) {
                return completeValue == intent.getIntExtra(VALUE, -1);
            }
        };
    }

    private Func1<? super Intent, ? extends Observable<? extends Intent>> generateErrorOnZeroValue() {
        return new Func1<Intent, Observable<Intent>>() {
            @Override
            public Observable<Intent> call(Intent intent) {
                if (intent.getIntExtra(VALUE, -1) == 0) {
                    return Observable.error(new Exception());
                }
                return Observable.just(intent);
            }
        };
    }

    public void assertCompleted() {
        mSubscriber.awaitTerminalEvent();
        mSubscriber.assertCompleted();
    }

    public void assertNoValueReceived() {
        mSubscriber.assertNoValues();
    }

    public void subscribe() {
        mObservable.subscribe(mSubscriber);
    }

    public void assertValues(int... values) {
        List<Intent> events = mSubscriber.getOnNextEvents();
        List<Integer> eventValues = new ArrayList<>();
        for (Intent i : events) {
            eventValues.add(i.getIntExtra(VALUE, -1));
        }
        for (int v : values) {
            if (!eventValues.contains(v)) {
                Assert.fail("Value '" + v + "' not received");
            }
        }
    }

    public void assertReceiverWasUnregistered() {
        verify(mApplication).unregisterReceiver(any(BroadcastReceiver.class));
    }

    public void assertError() {
        mSubscriber.assertError(Exception.class);
    }

    public void assertNotCompleted() {
        mSubscriber.assertNotCompleted();
    }

    public void assertReceiverWasNotUnregistered() {
        verify(mApplication, never()).unregisterReceiver(any(BroadcastReceiver.class));
    }

    public void assertReceiverWasNotRegistered() {
        verify(mApplication, never()).registerReceiver(any(BroadcastReceiver.class), any(IntentFilter.class));
    }

    public void assertReceiverWasRegistered() {
        verify(mApplication).registerReceiver(any(BroadcastReceiver.class), any(IntentFilter.class));
    }
}
