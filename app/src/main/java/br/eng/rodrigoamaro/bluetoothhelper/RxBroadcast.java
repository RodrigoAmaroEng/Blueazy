package br.eng.rodrigoamaro.bluetoothhelper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

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

    public static class Builder {

        private final Context mContext;
        private final Set<String> mFilters = new HashSet<>();
        private boolean mIncludeExitConditionEvent = false;
        private Func1<Intent, Boolean> mExitCondition;
        private Func0<Observable<Intent>> mStartOperation;

        public Builder(Context context) {
            mContext = context;
        }

        public Builder addFilter(String intentAction) {
            mFilters.add(intentAction);
            return this;
        }

        public Builder addFilters(String... intentActions) {
            Collections.addAll(mFilters, intentActions);
            return this;
        }

        public Builder setIncludeExitConditionEvent() {
            mIncludeExitConditionEvent = true;
            return this;
        }

        public Builder setExitCondition(Func1<Intent, Boolean> condition) {
            mExitCondition = condition;
            return this;
        }

        public Builder setStartOperation(Func0<Observable<Intent>> operation) {
            mStartOperation = operation;
            return this;
        }

        public Observable<Intent> build() {
            Iterator<String> iterator = mFilters.iterator();
            final IntentFilter filter = new IntentFilter(iterator.next());
            while (iterator.hasNext()) {
                filter.addAction(iterator.next());
            }
            return Observable.create(new Observable.OnSubscribe<Intent>() {

                @Override
                public void call(final Subscriber<? super Intent> subscriber) {
                    final BroadcastReceiver receiver = new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context c, Intent intent) {
                            evaluate(intent, subscriber, this);
                        }

                    };
                    if (hasStartingOperation()) {
                        Log.d(TAG, "hasStartingOperation");
                        mStartOperation.call()
                                .subscribe(redirectItemTo(subscriber, receiver),
                                        redirectErrorTo(subscriber),
                                        registerIfNotCompleted(subscriber, receiver));
                    } else {
                        Log.d(TAG, "doNotHaveStartingOperation");
                        registerReceiver(subscriber, receiver);
                    }
                }

                private Action0 registerIfNotCompleted(final Subscriber<? super Intent> subscriber,
                                                       final BroadcastReceiver receiver) {
                    return new Action0() {
                        @Override
                        public void call() {
                            if (!completed && !registered && hasCondition()) {
                                registerReceiver(subscriber, receiver);
                            }
                        }
                    };
                }

                private void evaluate(Intent intent, Subscriber<? super Intent> subscriber,
                                      BroadcastReceiver receiver) {
                    Log.d(TAG, "Evaluating: " + intent);
                    if (hasCondition()) {
                        Boolean isCompleted = mExitCondition.call(intent);
                        if (hasToIncludeExitConditionEvent()) {
                            subscriber.onNext(intent);
                        }
                        if (isCompleted) {
                            unregister(receiver);
                            subscriber.onCompleted();
                        } else if (!hasToIncludeExitConditionEvent()) {
                            subscriber.onNext(intent);
                        }
                    } else {
                        subscriber.onNext(intent);
                    }
                }

                @NonNull
                private void registerReceiver(final Subscriber<? super Intent> subscriber,
                                              final BroadcastReceiver receiver) {
                    register(receiver);
                    subscriber.add(unsubscribeInUiThread(new Action0() {
                        @Override
                        public void call() {
                            unregister(receiver);
                        }
                    }));
                }

                boolean registered;
                boolean completed;

                private Action1<? super Intent> redirectItemTo(final Subscriber<? super Intent> subscriber,
                                                               final BroadcastReceiver receiver) {
                    return new Action1<Intent>() {
                        @Override
                        public void call(Intent item) {
                            Log.d(TAG, "Propagation of items");
                            if (!hasCondition() || !mExitCondition.call(item)) {
                                Log.d(TAG, "Propagating and registering");
                                subscriber.onNext(item);
                                registerReceiver(subscriber, receiver);
                            } else {
                                if (hasToIncludeExitConditionEvent()) {
                                    Log.d(TAG, "Propagating");
                                    subscriber.onNext(item);
                                }
                                Log.d(TAG, "Completed");
                                completed = true;
                                subscriber.onCompleted();
                            }
                        }
                    };
                }

                private Action1<Throwable> redirectErrorTo(final Subscriber<?> subscriber) {
                    return new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            Log.d(TAG, "Propagating error");
                            completed = true;
                            subscriber.onError(throwable);
                        }
                    };
                }

                void register(BroadcastReceiver receiver) {
                    mContext.registerReceiver(receiver, filter);
                    registered = true;
                }

                void unregister(BroadcastReceiver receiver) {
                    if (registered) {
                        mContext.unregisterReceiver(receiver);
                        registered = false;
                    }
                }
            });
        }


        private boolean hasToIncludeExitConditionEvent() {
            return mIncludeExitConditionEvent;
        }

        private boolean hasCondition() {
            return mExitCondition != null;
        }

        private boolean hasStartingOperation() {
            return mStartOperation != null;
        }

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

    private static <T> Action1<T> propagateItemTo(final Subscriber<T> subscriber) {
        return new Action1<T>() {
            @Override
            public void call(T item) {
                Log.d(TAG, "Propagation wrong of items");

                subscriber.onNext(item);
            }
        };
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


    private static Action1<Throwable> propagateErrorTo(final Subscriber<?> subscriber) {
        return new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                Log.d(TAG, "Propagating error");
                subscriber.onError(throwable);
            }
        };
    }
}