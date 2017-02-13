package com.jforex.programming.order;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.dukascopy.api.Instrument;
import com.google.common.collect.ImmutableMap;
import com.jforex.programming.order.event.OrderEvent;
import com.jforex.programming.order.task.BasicTask;
import com.jforex.programming.order.task.ClosePositionTask;
import com.jforex.programming.order.task.MergePositionTask;
import com.jforex.programming.order.task.params.TaskParams;
import com.jforex.programming.order.task.params.TaskParamsType;
import com.jforex.programming.order.task.params.TaskParamsUtil;
import com.jforex.programming.order.task.params.basic.BatchParams;
import com.jforex.programming.order.task.params.basic.CloseParams;
import com.jforex.programming.order.task.params.basic.MergeParams;
import com.jforex.programming.order.task.params.basic.SetAmountParams;
import com.jforex.programming.order.task.params.basic.SetGTTParams;
import com.jforex.programming.order.task.params.basic.SetLabelParams;
import com.jforex.programming.order.task.params.basic.SetOpenPriceParams;
import com.jforex.programming.order.task.params.basic.SetSLParams;
import com.jforex.programming.order.task.params.basic.SetTPParams;
import com.jforex.programming.order.task.params.basic.SubmitParams;
import com.jforex.programming.order.task.params.position.CloseAllPositionsParams;
import com.jforex.programming.order.task.params.position.ClosePositionParams;
import com.jforex.programming.order.task.params.position.MergeAllPositionsParams;
import com.jforex.programming.order.task.params.position.MergePositionParams;
import com.jforex.programming.position.PositionOrders;
import com.jforex.programming.position.PositionUtil;

import io.reactivex.Observable;

public class OrderUtil {

    private final PositionUtil positionUtil;
    private final TaskParamsUtil taskParamsUtil;
    private final Map<TaskParamsType, Function<TaskParams, Observable<OrderEvent>>> taskParamsMapper;

    public OrderUtil(final BasicTask basicTask,
                     final MergePositionTask mergePositionTask,
                     final ClosePositionTask closePositionTask,
                     final PositionUtil positionUtil,
                     final TaskParamsUtil taskParamsUtil) {
        this.positionUtil = positionUtil;
        this.taskParamsUtil = taskParamsUtil;

        taskParamsMapper = ImmutableMap.<TaskParamsType, Function<TaskParams, Observable<OrderEvent>>> builder()
            .put(TaskParamsType.SUBMIT,
                 params -> basicTask.submitOrder((SubmitParams) params))
            .put(TaskParamsType.MERGE,
                 params -> basicTask.mergeOrders((MergeParams) params))
            .put(TaskParamsType.CLOSE,
                 params -> basicTask.close((CloseParams) params))
            .put(TaskParamsType.SETLABEL,
                 params -> basicTask.setLabel((SetLabelParams) params))
            .put(TaskParamsType.SETGTT,
                 params -> basicTask.setGoodTillTime((SetGTTParams) params))
            .put(TaskParamsType.SETAMOUNT,
                 params -> basicTask.setRequestedAmount((SetAmountParams) params))
            .put(TaskParamsType.SETOPENPRICE,
                 params -> basicTask.setOpenPrice((SetOpenPriceParams) params))
            .put(TaskParamsType.SETSL,
                 params -> basicTask.setStopLossPrice((SetSLParams) params))
            .put(TaskParamsType.SETTP,
                 params -> basicTask.setTakeProfitPrice((SetTPParams) params))
            .put(TaskParamsType.MERGEPOSITION,
                 params -> mergePositionTask.merge((MergePositionParams) params))
            .put(TaskParamsType.MERGEALLPOSITIONS,
                 params -> mergePositionTask.mergeAll((MergeAllPositionsParams) params))
            .put(TaskParamsType.CLOSEPOSITION,
                 params -> closePositionTask.close((ClosePositionParams) params))
            .put(TaskParamsType.CLOSEALLPOSITIONS,
                 params -> closePositionTask.closeAll((CloseAllPositionsParams) params))
            .build();
    }

    public void execute(final TaskParams taskParams) {
        checkNotNull(taskParams);

        final Observable<OrderEvent> observable = taskParamsToObservable(taskParams);
        taskParamsUtil.subscribeToTaskParams(observable, taskParams);
    }

    public void executeBatch(final BatchParams batchParams) {
        checkNotNull(batchParams);

        final List<Observable<OrderEvent>> observables = batchParams
            .taskParams()
            .stream()
            .map(params -> taskParamsUtil.composeParamsWithEvents(taskParamsToObservable(params),
                                                                  params.composeData(),
                                                                  params.consumerForEvent()))
            .collect(Collectors.toList());

        taskParamsUtil.composeAndSubscribe(Observable.merge(observables), batchParams.composeData());
    }

    private final Observable<OrderEvent> taskParamsToObservable(final TaskParams taskParams) {
        return taskParamsMapper
            .get(taskParams.type())
            .apply(taskParams);
    }

    public PositionOrders positionOrders(final Instrument instrument) {
        checkNotNull(instrument);

        return positionUtil.positionOrders(instrument);
    }
}
