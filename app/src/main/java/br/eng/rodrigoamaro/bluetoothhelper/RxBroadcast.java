package br.eng.rodrigoamaro.bluetoothhelper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Looper;
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

public class RxBroadcast implements Observable.OnSubscribe<Intent> {

    private static final String TAG = "RxBroadcast";
    private final Context mContext;
    private final IntentFilter mFilters;
    private final Func0<Observable<Intent>> mStartOperation;
    private final Func1<Intent, Boolean> mExitCondition;
    private final boolean mIncludeExitConditionEvent;
    private boolean mRegistered;
    private boolean mCompleted;


    private RxBroadcast(Context context, IntentFilter filters,
                        Func0<Observable<Intent>> startOperation,
                        Func1<Intent, Boolean> exitCondition, boolean includeExitConditionEvent) {
        mContext = context;
        mFilters = filters;
        mStartOperation = startOperation;
        mExitCondition = exitCondition;
        mIncludeExitConditionEvent = includeExitConditionEvent;
    }

    private Subscription unsubscribeInUiThread(final Action0 unsubscribe) {
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

    private boolean hasToIncludeExitConditionEvent() {
        return mIncludeExitConditionEvent;
    }

    private boolean hasCondition() {
        return mExitCondition != null;
    }

    private boolean hasStartingOperation() {
        return mStartOperation != null;
    }

    @Override
    public void call(final Subscriber<? super Intent> subscriber) {
        final BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {
                evaluate(intent, subscriber, this);
            }

        };
        if (hasStartingOperation()) {
            mStartOperation.call()
                    .subscribe(redirectItemTo(subscriber, receiver),
                            redirectErrorTo(subscriber),
                            registerIfNotCompleted(subscriber, receiver));
        } else {
            registerReceiver(subscriber, receiver);
        }
    }

    private Action0 registerIfNotCompleted(final Subscriber<? super Intent> subscriber,
                                           final BroadcastReceiver receiver) {
        return new Action0() {
            @Override
            public void call() {
                if (!mCompleted && !mRegistered && hasCondition()) {
                    registerReceiver(subscriber, receiver);
                }
            }
        };
    }

    private void evaluate(Intent intent, Subscriber<? super Intent> subscriber,
                          BroadcastReceiver receiver) {
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

    private Action1<? super Intent> redirectItemTo(final Subscriber<? super Intent> subscriber,
                                                   final BroadcastReceiver receiver) {
        return new Action1<Intent>() {
            @Override
            public void call(Intent item) {
                if (!hasCondition() || !mExitCondition.call(item)) {
                    subscriber.onNext(item);
                    registerReceiver(subscriber, receiver);
                } else {
                    if (hasToIncludeExitConditionEvent()) {
                        subscriber.onNext(item);
                    }
                    mCompleted = true;
                    subscriber.onCompleted();
                }
            }
        };
    }

    private Action1<Throwable> redirectErrorTo(final Subscriber<?> subscriber) {
        return new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                mCompleted = true;
                subscriber.onError(throwable);
            }
        };
    }

    private void register(BroadcastReceiver receiver) {
        mContext.registerReceiver(receiver, mFilters);
        mRegistered = true;
    }

    private void unregister(BroadcastReceiver receiver) {
        if (mRegistered) {
            mContext.unregisterReceiver(receiver);
            mRegistered = false;
        }
    }


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
            return Observable.create(new RxBroadcast(mContext, filter, mStartOperation,
                    mExitCondition, mIncludeExitConditionEvent));
        }


    }

}