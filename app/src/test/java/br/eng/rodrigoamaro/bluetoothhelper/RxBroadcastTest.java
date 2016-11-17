package br.eng.rodrigoamaro.bluetoothhelper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class RxBroadcastTest {

    Api mApi = new Api(RuntimeEnvironment.application);

    Broadcaster mBroadcaster = new Broadcaster(RuntimeEnvironment.application);

    @Test
    public void registerWaitAndCompleteFlow() throws Exception {
        mApi.createRegisterFor("MESSAGE_1", 4);
        mApi.subscribe();
        mBroadcaster.sendMessage("MESSAGE_1", 4);

        mApi.assertNoValueReceived();
        mApi.assertCompleted();
        mApi.assertReceiverWasUnregistered();
    }

    @Test
    public void registerExecuteSomethingFirstThatFails() throws Exception {
        mApi.createRegisterFor("MESSAGE_1", 4, 0);
        mApi.subscribe();

        mApi.assertNoValueReceived();
        mApi.assertError();
        mApi.assertReceiverWasNotRegistered();
    }

    @Test
    public void registerExecuteSomethingFirstThatReturnsValue() throws Exception {
        mApi.createRegisterFor("MESSAGE_1", 4, 1);
        mApi.subscribe();

        mApi.assertValues(1);
        mApi.assertNotCompleted();
        mApi.assertReceiverWasRegistered();

    }

    @Test
    public void registerWaitReceiveOtherEventsAndCompleteFlow() throws Exception {
        mApi.createRegisterFor("MESSAGE_1", 4);
        mApi.subscribe();
        mBroadcaster.sendMessage("MESSAGE_1", 3);
        mBroadcaster.sendMessage("MESSAGE_1", 4);

        mApi.assertValues(3);
        mApi.assertCompleted();
        mApi.assertReceiverWasUnregistered();
    }

    @Test
    public void registerWaitAndThrowError() throws Exception {
        mApi.createRegisterFor("MESSAGE_1", 4);
        mApi.subscribe();
        mBroadcaster.sendMessage("MESSAGE_1", 0);

        mApi.assertNoValueReceived();
        mApi.assertError();
        mApi.assertReceiverWasUnregistered();
    }

    @Test
    public void registerWaitAndCompleteFlowIncludingTheClosingEvent() throws Exception {
        mApi.createInclusiveRegisterFor("MESSAGE_1", 4);
        mApi.subscribe();
        mBroadcaster.sendMessage("MESSAGE_1", 4);

        mApi.assertValues(4);
        mApi.assertCompleted();
        mApi.assertReceiverWasUnregistered();
    }

    @Test
    public void registerExecuteSomethingFirstThatReturnsValueIncludingTheClosingEvent() throws Exception {
        mApi.createInclusiveRegisterFor("MESSAGE_1", 4, 4);
        mApi.subscribe();

        mApi.assertValues(4);
        mApi.assertCompleted();
        mApi.assertReceiverWasNotRegistered();
    }

    @Test
    public void registerExecuteSomethingFirstThatFailsIncludingMode() throws Exception {
        mApi.createInclusiveRegisterFor("MESSAGE_1", 4, 0);
        mApi.subscribe();

        mApi.assertNoValueReceived();
        mApi.assertError();
        mApi.assertReceiverWasNotRegistered();
    }

    @Test
    public void registerWaitAndThrowErrorIncludingTheClosingEvent() throws Exception {
        mApi.createInclusiveRegisterFor("MESSAGE_1", 4);
        mApi.subscribe();
        mBroadcaster.sendMessage("MESSAGE_1", 0);

        mApi.assertNoValueReceived();
        mApi.assertError();
        mApi.assertReceiverWasUnregistered();
    }

    @Test
    public void registerWaitReceiveOtherEventsAndCompleteFlowIncludingTheClosingEvent() throws Exception {
        mApi.createInclusiveRegisterFor("MESSAGE_1", 4);
        mApi.subscribe();
        mBroadcaster.sendMessage("MESSAGE_1", 3);
        mBroadcaster.sendMessage("MESSAGE_1", 4);

        mApi.assertValues(3, 4);
        mApi.assertCompleted();
        mApi.assertReceiverWasUnregistered();
    }

    @Test
    public void registerWaitReceiveAndDoNotComplete() throws Exception {
        mApi.createContinuousRegisterFor("MESSAGE_1");
        mApi.subscribe();
        mBroadcaster.sendMessage("MESSAGE_1", 3);
        mBroadcaster.sendMessage("MESSAGE_1", 4);

        mApi.assertValues(3, 4);
        mApi.assertNotCompleted();
        mApi.assertReceiverWasNotUnregistered();
    }

    @Test
    public void registerWaitReceiveAndFail() throws Exception {
        mApi.createContinuousRegisterFor("MESSAGE_1");
        mApi.subscribe();
        mBroadcaster.sendMessage("MESSAGE_1", 0);

        mApi.assertNoValueReceived();
        mApi.assertError();
        mApi.assertReceiverWasUnregistered();
    }

    @Test
    public void registerDoSomethingThatReturnsValueFirstWaitAndDoNotComplete() throws Exception {
        mApi.createContinuousRegisterFor("MESSAGE_1", 4);
        mApi.subscribe();

        mApi.assertValues(4);
        mApi.assertNotCompleted();
        mApi.assertReceiverWasRegistered();
        mApi.assertReceiverWasNotUnregistered();
    }

    @Test
    public void registerDoSomethingThatFailsFirst() throws Exception {
        mApi.createContinuousRegisterFor("MESSAGE_1", 0);
        mApi.subscribe();

        mApi.assertNoValueReceived();
        mApi.assertError();
        mApi.assertReceiverWasNotRegistered();
    }


}
