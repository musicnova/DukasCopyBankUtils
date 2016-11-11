package com.jforex.programming.order.task.params.position.test;

//@RunWith(HierarchicalContextRunner.class)
//public class MergePositionParamsHandlerTest extends InstrumentUtilForTest {
//
//    private MergePositionParamsHandler mergePositionParamsHandler;
//
//    @Mock
//    private CancelSLTPTask cancelSLTPTaskMock;
//    @Mock
//    private BasicTaskObservable basicTaskMock;
//    @Mock
//    private TaskParamsUtil taskParamsUtilMock;
//    @Mock
//    private MergePositionParams mergePositionParamsMock;
//    private TestObserver<OrderEvent> testObserver;
//    private final String mergeOrderLabel = "mergeOrderLabel";
//    private final Set<IOrder> toMergeOrders = Sets.newHashSet(buyOrderEURUSD, sellOrderEURUSD);
//    private final OrderEvent testEvent = mergeEvent;
//
//    @Before
//    public void setUp() {
//        setUpMocks();
//
//        mergePositionParamsHandler = new MergePositionParamsHandler(cancelSLTPTaskMock,
//                                                                    basicTaskMock,
//                                                                    taskParamsUtilMock);
//    }
//
//    private void setUpMocks() {
//        when(mergePositionParamsMock.simpleMergePositionParams())
//            .thenReturn(simpleMergePositionParamsMock);
//
//        when(simpleMergePositionParamsMock.mergeOrderLabel())
//            .thenReturn(mergeOrderLabel);
//
//        when(cancelSLTPTaskMock.observe(toMergeOrders, mergePositionParamsMock))
//            .thenReturn(eventObservable(testEvent));
//    }
//
//    @Test
//    public void observeCancelSLTPDelegatesToCancelSLTPMock() {
//        testObserver = mergePositionParamsHandler
//            .observeCancelSLTP(toMergeOrders, mergePositionParamsMock)
//            .test();
//
//        testObserver.assertComplete();
//        testObserver.assertValue(testEvent);
//    }
//
//    public class ObserveMerge {
//
//        private Observable<OrderEvent> returnedObservable;
//
//        @Before
//        public void setUp() {
//            returnedObservable = eventObservable(testEvent);
//
//            when(taskParamsUtilMock.composeTask(any(), eq(simpleMergePositionParamsMock)))
//                .thenReturn(returnedObservable);
//
//            when(basicTaskMock.mergeOrders(mergeOrderLabel, toMergeOrders))
//                .thenReturn(eventObservable(testEvent));
//
//            testObserver = mergePositionParamsHandler
//                .observeMerge(toMergeOrders, simpleMergePositionParamsMock)
//                .test();
//        }
//
//        @Test
//        public void observeMergeCallsBasicTaskMockCorrect() {
//            verify(basicTaskMock).mergeOrders(mergeOrderLabel, toMergeOrders);
//        }
//
//        @Test
//        public void returnedObservableIsCorrectComposed() {
//            testObserver.assertComplete();
//            testObserver.assertValue(testEvent);
//        }
//    }
//}
