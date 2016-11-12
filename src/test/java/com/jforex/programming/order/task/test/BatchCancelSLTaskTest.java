package com.jforex.programming.order.task.test;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import com.dukascopy.api.IOrder;
import com.google.common.collect.Sets;
import com.jforex.programming.order.event.OrderEvent;
import com.jforex.programming.order.task.BatchCancelSLTask;
import com.jforex.programming.order.task.BatchChangeTask;
import com.jforex.programming.order.task.params.ComposeParams;
import com.jforex.programming.order.task.params.TaskParamsUtil;
import com.jforex.programming.order.task.params.position.MergePositionParams;
import com.jforex.programming.test.common.InstrumentUtilForTest;

import de.bechte.junit.runners.context.HierarchicalContextRunner;
import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;

@RunWith(HierarchicalContextRunner.class)
public class BatchCancelSLTaskTest extends InstrumentUtilForTest {

    private BatchCancelSLTask batchCancelSLTask;

    @Mock
    private BatchChangeTask batchChangeTaskMock;
    @Mock
    private MergePositionParams mergePositionParamsMock;
    @Mock
    private TaskParamsUtil taskParamsUtilMock;
    private TestObserver<OrderEvent> testObserver;
    private final Set<IOrder> toCancelSLTPOrders = Sets.newHashSet(buyOrderEURUSD, sellOrderEURUSD);
    private final ComposeParams composeParams = new ComposeParams();
    private final Observable<OrderEvent> observableFromBatch = eventObservable(changedSLEvent);
    private final Observable<OrderEvent> observableFromTaskParamsUtil = eventObservable(changedRejectEvent);

    @Before
    public void setUp() {
        when(batchChangeTaskMock.cancelSL(toCancelSLTPOrders, mergePositionParamsMock))
            .thenReturn(observableFromBatch);
        when(taskParamsUtilMock.composeParams(observableFromBatch, composeParams))
            .thenReturn(observableFromTaskParamsUtil);
        when(mergePositionParamsMock.batchCancelSLComposeParams())
            .thenReturn(composeParams);

        batchCancelSLTask = new BatchCancelSLTask(batchChangeTaskMock, taskParamsUtilMock);
    }

    @Test
    public void observableIsDeferred() {
        batchCancelSLTask.observe(toCancelSLTPOrders, mergePositionParamsMock);

        verifyZeroInteractions(batchChangeTaskMock);
    }

    public class ObserveTaskSetup {

        @Before
        public void setUp() {
            testObserver = batchCancelSLTask
                .observe(toCancelSLTPOrders, mergePositionParamsMock)
                .test();
        }

        @Test
        public void batchChangeTaskIsCalled() {
            verify(batchChangeTaskMock).cancelSL(toCancelSLTPOrders, mergePositionParamsMock);
        }

        @Test
        public void composeOnTaskParamsIsCalled() {
            verify(taskParamsUtilMock).composeParams(any(), eq(composeParams));
        }

        @Test
        public void eventFromTaskParamsUtilIsEmitted() {
            testObserver.assertComplete();
            testObserver.assertValue(changedRejectEvent);
        }
    }
}