package com.jforex.programming.order.task.test;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import com.dukascopy.api.IOrder;
import com.google.common.collect.Sets;
import com.jforex.programming.order.event.OrderEvent;
import com.jforex.programming.order.task.CancelSLTPAndMergeTask;
import com.jforex.programming.order.task.params.MergeParams;
import com.jforex.programming.order.task.params.MergeParamsHandler;
import com.jforex.programming.test.common.InstrumentUtilForTest;

import de.bechte.junit.runners.context.HierarchicalContextRunner;
import io.reactivex.observers.TestObserver;

@RunWith(HierarchicalContextRunner.class)
public class CancelSLTPAndMergeTaskTest extends InstrumentUtilForTest {

    private CancelSLTPAndMergeTask cancelSLTPAndMergeTask;

    @Mock
    private MergeParamsHandler commandHandlerMock;
    @Mock
    private MergeParams mergeCommandMock;
    private final Set<IOrder> toMergeOrders = Sets.newHashSet(buyOrderEURUSD, sellOrderEURUSD);
    private final OrderEvent testEvent = mergeEvent;

    @Before
    public void setUp() {
        setUpMocks();

        cancelSLTPAndMergeTask = new CancelSLTPAndMergeTask(commandHandlerMock);
    }

    private void setUpMocks() {
        when(commandHandlerMock.observeCancelSLTP(toMergeOrders, mergeCommandMock))
            .thenReturn(neverObservable());
        when(commandHandlerMock.observeMerge(toMergeOrders, mergeCommandMock))
            .thenReturn(eventObservable(testEvent));
    }

    private TestObserver<OrderEvent> testSubscribeSplitter() {
        return cancelSLTPAndMergeTask
            .observe(toMergeOrders, mergeCommandMock)
            .test();
    }

    @Test
    public void cancelSLTPAndMergeAreConcatenated() {
        testSubscribeSplitter()
            .assertNotComplete()
            .assertNoValues();
    }

    @Test
    public void cancelSLTPIsCalledOnHandler() {
        testSubscribeSplitter();

        verify(commandHandlerMock).observeCancelSLTP(toMergeOrders, mergeCommandMock);
    }

    @Test
    public void mergeIsCalledOnHandler() {
        testSubscribeSplitter();

        verify(commandHandlerMock).observeMerge(toMergeOrders, mergeCommandMock);
    }
}
