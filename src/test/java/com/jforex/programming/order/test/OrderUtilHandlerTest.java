package com.jforex.programming.order.test;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import com.dukascopy.api.IOrder;
import com.dukascopy.api.JFException;
import com.google.common.collect.Sets;
import com.jforex.programming.order.OrderUtilHandler;
import com.jforex.programming.order.call.OrderCallExecutor;
import com.jforex.programming.order.call.OrderCallExecutorResult;
import com.jforex.programming.order.call.OrderCallRejectException;
import com.jforex.programming.order.call.OrderCallRequest;
import com.jforex.programming.order.call.RunnableWithJFException;
import com.jforex.programming.order.call.OrderSupplier;
import com.jforex.programming.order.event.OrderEvent;
import com.jforex.programming.order.event.OrderEventGateway;
import com.jforex.programming.order.event.OrderEventType;
import com.jforex.programming.order.event.OrderEventTypeData;
import com.jforex.programming.test.common.InstrumentUtilForTest;
import com.jforex.programming.test.fakes.IOrderForTest;

import de.bechte.junit.runners.context.HierarchicalContextRunner;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;

@RunWith(HierarchicalContextRunner.class)
public class OrderUtilHandlerTest extends InstrumentUtilForTest {

    private OrderUtilHandler orderUtilHandler;

    @Mock
    private OrderCallExecutor orderCallExecutorMock;
    @Mock
    private OrderEventGateway orderEventGatewayMock;
    @Captor
    private ArgumentCaptor<OrderSupplier> orderCallCaptor;
    private final IOrderForTest externalOrder = IOrderForTest.orderAUDUSD();
    private final Subject<OrderEvent, OrderEvent> orderEventSubject = PublishSubject.create();
    private OrderCallExecutorResult orderExecutorResultWithJFException;

    @Before
    public void setUp() {
        initCommonTestFramework();

        orderExecutorResultWithJFException =
                new OrderCallExecutorResult(Optional.empty(), Optional.of(jfException));
        setUpMocks();

        orderUtilHandler = new OrderUtilHandler(orderCallExecutorMock, orderEventGatewayMock);
    }

    private void setUpMocks() {
        when(orderEventGatewayMock.observable()).thenReturn(orderEventSubject);
    }

    private void prepareJFException() {
        when(orderCallExecutorMock.run(any(OrderSupplier.class)))
                .thenReturn(orderExecutorResultWithJFException);
    }

    private void captureAndRunOrderCall() throws JFException {
        verify(orderCallExecutorMock).run(orderCallCaptor.capture());
        orderCallCaptor.getValue().get();
    }

    private void assertSubscriberJFError(final TestSubscriber<?> subscriber) {
        subscriber.assertValueCount(0);
        subscriber.assertError(JFException.class);
    }

    private void assertSubscriberRejectError(final TestSubscriber<?> subscriber) {
        subscriber.assertValueCount(0);
        subscriber.assertError(OrderCallRejectException.class);
    }

    private void sendOrderEvent(final IOrder order,
                                final OrderEventType orderEventType) {
        orderEventSubject.onNext(new OrderEvent(order, orderEventType));
    }

    private void assertSubscriberCompleted(final TestSubscriber<?> subscriber) {
        subscriber.assertNoErrors();
        subscriber.assertCompleted();
    }

    public class OrderSupplierCallSetup {

        private final IOrderForTest mergeOrder = IOrderForTest.buyOrderEURUSD();
        private final Set<IOrder> mergeOrders = Sets.newHashSet();
        private final String mergeOrderLabel = "MergeLabel";
        private final OrderSupplier orderCall = () -> engineMock.mergeOrders(mergeOrderLabel, mergeOrders);
        private final TestSubscriber<OrderEvent> callSubscriber = new TestSubscriber<>();
        private final Supplier<Observable<OrderEvent>> runCall =
                () -> orderUtilHandler.runOrderSupplierCall(orderCall, OrderEventTypeData.mergeData);
        private final OrderCallExecutorResult orderExecutorResult =
                new OrderCallExecutorResult(Optional.of(mergeOrder),
                                            Optional.empty());

        @Before
        public void setUp() {
            when(orderCallExecutorMock.run(any(OrderSupplier.class))).thenReturn(orderExecutorResult);
        }

        public class CallWithoutSubscription {

            private Observable<OrderEvent> callObservable;

            @Before
            public void setUp() {
                callObservable = runCall.get();
            }

            @Test
            public void testCallIsExecutedAlsoWhenNotSubscribed() throws JFException {
                captureAndRunOrderCall();

                verify(engineMock).mergeOrders(mergeOrderLabel, mergeOrders);
            }

            @Test
            public void testObservableIsConnectedWithLateSubscriptionPossible() {
                sendOrderEvent(mergeOrder, OrderEventType.MERGE_OK);

                callObservable.subscribe(callSubscriber);

                assertSubscriberCompleted(callSubscriber);
            }
        }

        public class ExecutesWithJFException {

            @Before
            public void setUp() {
                prepareJFException();

                runCall.get().subscribe(callSubscriber);
            }

            @Test
            public void testCorrectCallIsExecuted() throws JFException {
                captureAndRunOrderCall();

                verify(engineMock).mergeOrders(mergeOrderLabel, mergeOrders);
            }

            @Test
            public void testSubscriberCompletesWithJFError() {
                assertSubscriberJFError(callSubscriber);
            }

            @Test
            public void testMergeOrderIsNotRegisteredAtGateway() {
                verify(orderEventGatewayMock, never()).registerOrderRequest(eq(mergeOrder), any());
            }
        }

        public class ExecutesOK {

            @Before
            public void setUp() {
                runCall.get().subscribe(callSubscriber);
            }

            @Test
            public void testCorrectCallIsExecuted() throws JFException {
                captureAndRunOrderCall();

                verify(engineMock).mergeOrders(mergeOrderLabel, mergeOrders);
            }

            @Test
            public void testSubscriberNotYetCompletedWhenNoEventWasSent() {
                callSubscriber.assertNotCompleted();
            }

            @Test
            public void testMergeOrderRegisteredAtGateway() {
                verify(orderEventGatewayMock).registerOrderRequest(mergeOrder, OrderCallRequest.MERGE);
            }

            @Test
            public void testSubscriberCompletesOnDoneEvent() {
                sendOrderEvent(mergeOrder, OrderEventType.MERGE_OK);

                assertSubscriberCompleted(callSubscriber);
            }

            @Test
            public void testSubscriberCompletesWithRejectError() {
                sendOrderEvent(mergeOrder, OrderEventType.MERGE_REJECTED);

                assertSubscriberRejectError(callSubscriber);
            }

            @Test
            public void testOtherOrderEventIsIgnored() {
                sendOrderEvent(externalOrder, OrderEventType.MERGE_OK);

                callSubscriber.assertNotCompleted();
            }

            @Test
            public void testNotCallRelatedEventIsIgnored() {
                sendOrderEvent(mergeOrder, OrderEventType.GTT_CHANGE_OK);

                callSubscriber.assertNotCompleted();
            }
        }
    }

    public class OrderChangeCallSetup {

        private final IOrderForTest orderToChange = IOrderForTest.buyOrderEURUSD();
        private final RunnableWithJFException orderCall = () -> orderToChange.close();
        private final TestSubscriber<OrderEvent> callSubscriber = new TestSubscriber<>();
        private final Supplier<Observable<OrderEvent>> runCall =
                () -> orderUtilHandler.runOrderChangeCall(orderCall, orderToChange, OrderEventTypeData.closeData);
        private final OrderCallExecutorResult orderExecutorResult =
                new OrderCallExecutorResult(Optional.of(orderToChange),
                                            Optional.empty());

        @Before
        public void setUp() {
            when(orderCallExecutorMock.run(any(OrderSupplier.class))).thenReturn(orderExecutorResult);
        }

        public class CallWithoutSubscription {

            private Observable<OrderEvent> callObservable;

            @Before
            public void setUp() {
                callObservable = runCall.get();
            }

            @Test
            public void testCallIsExecutedAlsoWhenNotSubscribed() throws JFException {
                captureAndRunOrderCall();

                verify(orderToChange).close();
            }

            @Test
            public void testObservableIsConnectedWithLateSubscriptionPossible() {
                sendOrderEvent(orderToChange, OrderEventType.CLOSE_OK);

                callObservable.subscribe(callSubscriber);

                assertSubscriberCompleted(callSubscriber);
            }
        }

        public class ExecutesWithJFException {

            @Before
            public void setUp() {
                prepareJFException();

                runCall.get().subscribe(callSubscriber);
            }

            @Test
            public void testCorrectCallIsExecuted() throws JFException {
                captureAndRunOrderCall();

                verify(orderToChange).close();
            }

            @Test
            public void testSubscriberCompletesWithJFError() {
                assertSubscriberJFError(callSubscriber);
            }

            @Test
            public void testMergeOrderIsNotRegisteredAtGateway() {
                verify(orderEventGatewayMock, never()).registerOrderRequest(eq(orderToChange), any());
            }
        }

        public class ExecutesOK {

            @Before
            public void setUp() {
                runCall.get().subscribe(callSubscriber);
            }

            @Test
            public void testCorrectCallIsExecuted() throws JFException {
                captureAndRunOrderCall();

                verify(orderToChange).close();
            }

            @Test
            public void testSubscriberNotYetCompletedWhenNoEventWasSent() {
                callSubscriber.assertNotCompleted();
            }

            @Test
            public void testMergeOrderRegisteredAtGateway() {
                verify(orderEventGatewayMock).registerOrderRequest(orderToChange, OrderCallRequest.CLOSE);
            }

            @Test
            public void testSubscriberCompletesOnDoneEvent() {
                sendOrderEvent(orderToChange, OrderEventType.CLOSE_OK);

                assertSubscriberCompleted(callSubscriber);
            }

            @Test
            public void testSubscriberCompletesWithRejectError() {
                sendOrderEvent(orderToChange, OrderEventType.CLOSE_REJECTED);

                assertSubscriberRejectError(callSubscriber);
            }

            @Test
            public void testOtherOrderEventIsIgnored() {
                sendOrderEvent(externalOrder, OrderEventType.CLOSE_OK);

                callSubscriber.assertNotCompleted();
            }

            @Test
            public void testNotCallRelatedEventIsIgnored() {
                sendOrderEvent(orderToChange, OrderEventType.GTT_CHANGE_OK);

                callSubscriber.assertNotCompleted();
            }
        }
    }
}
