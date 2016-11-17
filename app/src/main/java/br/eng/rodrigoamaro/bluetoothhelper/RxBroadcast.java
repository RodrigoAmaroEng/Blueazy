package br.eng.rodrigoamaro.bluetoothhelper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Looper;
import android.util.Log;

import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.subscriptions.Subscriptions;

public final class RxBroadcast {

    private static final String TAG = "RxBroadcast";

    public static Subscription unsubscribeInUiThread(final Action0 unsubscribe) {
        return Subscriptions.create(new Action0() {
            @Override
            public void call() {
                if (Looper.getMainLooper() == Looper.myLooper()) {
                    unsubscribe.call();
                } else {
                    final Scheduler.Worker inner = AndroidSchedulers.mainThread().createWorker();
                    inner.schedule(new Action0() {
                        @Override
                        public void call() {
                            unsubscribe.call();
                            inner.unsubscribe();
                        }
                    });
                }
            }
        });
    }


    public static Observable<Intent> fromShortBroadcast(
            final Context context,
            final IntentFilter filter,
            final Func1<Intent, Boolean> condition,
            final Func0<Observable<Intent>> startingOperation) {
        return Observable.create(new Observable.OnSubscribe<Intent>() {

            @Override
            public void call(final Subscriber<? super Intent> subscriber) {
                startingOperation.call().subscribe(propagateItemTo(subscriber),
                        propagateErrorTo(subscriber));
                final BroadcastReceiver receiver = new BroadcastReceiver() {

                    @Override
                    public void onReceive(Context c, Intent intent) {
                        if (condition.call(intent)) {
                            Log.d(TAG, "onReceive: Completed");
                            unregister(this);
                            subscriber.onCompleted();
                        } else {
                            Log.d(TAG, "onReceive: Next");
                            subscriber.onNext(intent);
                        }
                    }

                };
                register(receiver);
                subscriber.add(unsubscribeInUiThread(new Action0() {
                    @Override
                    public void call() {
                        unregister(receiver);
                    }
                }));
            }

            boolean registered;

            void register(BroadcastReceiver receiver) {
                Log.d(TAG, "onReceive: Register receiver");
                context.registerReceiver(receiver, filter);
                registered = true;
            }

            void unregister(BroadcastReceiver receiver) {
                Log.d(TAG, "onReceive: Unregister receiver " + registered);
                if (registered) {
                    context.unregisterReceiver(receiver);
                    registered = false;
                }
            }
        });
    }

    public static Observable<Intent> fromShortBroadcastInclusive(
            final Context context,
            final IntentFilter filter,
            final Func1<Intent, Boolean> condition,
            final Func0<Observable<Intent>> startingOperation) {
        return Observable.create(new Observable.OnSubscribe<Intent>() {

            @Override
            public void call(final Subscriber<? super Intent> subscriber) {
                startingOperation.call().subscribe(propagateItemTo(subscriber),
                        propagateErrorTo(subscriber));
                final BroadcastReceiver receiver = new BroadcastReceiver() {

                    @Override
                    public void onReceive(Context c, Intent intent) {
                        Log.d(TAG, "onReceive: Next");
                        subscriber.onNext(intent);
                        if (condition.call(intent)) {
                            Log.d(TAG, "onReceive: Completed");
                            unregister(this);
                            subscriber.onCompleted();
                        }
                    }

                };
                register(receiver);
                subscriber.add(unsubscribeInUiThread(new Action0() {
                    @Override
                    public void call() {
                        unregister(receiver);
                    }
                }));
            }

            boolean registered;

            void register(BroadcastReceiver receiver) {
                Log.d(TAG, "onReceive: Register receiver");
                context.registerReceiver(receiver, filter);
                registered = true;
            }

            void unregister(BroadcastReceiver receiver) {
                Log.d(TAG, "onReceive: Unregister receiver " + registered);
                if (registered) {
                    context.unregisterReceiver(receiver);
                    registered = false;
                }
            }
        });
    }

    public static Observable<Intent> fromBroadcast(final Context context, final IntentFilter filter) {
        return Observable.create(new Observable.OnSubscribe<Intent>() {

            @Override
            public void call(final Subscriber<? super Intent> subscriber) {
                final BroadcastReceiver receiver = new BroadcastReceiver() {

                    @Override
                    public void onReceive(Context context, Intent intent) {
                        subscriber.onNext(intent);
                    }

                };
                context.registerReceiver(receiver, filter);
                subscriber.add(unsubscribeInUiThread(new Action0() {

                    @Override
                    public void call() {
                        context.unregisterReceiver(receiver);
                    }
                }));
            }
        });
    }

    private static <T> Action1<T> emptyAction() {
        return new Action1<T>() {
            @Override
            public void call(T t) {
            }
        };
    }

    private static <T> Action1<T> propagateItemTo(final Subscriber<T> subscriber) {
        return new Action1<T>() {
            @Override
            public void call(T item) {
                subscriber.onNext(item);
            }
        };
    }

    private static Action1<Throwable> propagateErrorTo(final Subscriber<?> subscriber) {
        return new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                subscriber.onError(throwable);
            }
        };
    }
}