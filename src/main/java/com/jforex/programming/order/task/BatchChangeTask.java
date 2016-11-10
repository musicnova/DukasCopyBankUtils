package com.jforex.programming.order.task;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import com.dukascopy.api.IOrder;
import com.jforex.programming.order.event.OrderEvent;
import com.jforex.programming.order.task.params.TaskParamsUtil;
import com.jforex.programming.order.task.params.basic.CancelSLParams;
import com.jforex.programming.order.task.params.basic.CancelTPParams;
import com.jforex.programming.order.task.params.basic.CloseParams;
import com.jforex.programming.order.task.params.basic.SetSLParams;
import com.jforex.programming.order.task.params.basic.SetTPParams;
import com.jforex.programming.order.task.params.position.SimpleClosePositionParams;
import com.jforex.programming.settings.PlatformSettings;
import com.jforex.programming.strategy.StrategyUtil;

import io.reactivex.Observable;

public class BatchChangeTask {

    private final BasicTaskObservable basicTask;
    private final TaskParamsUtil taskParamsUtil;

    private static final PlatformSettings platformSettings = StrategyUtil.platformSettings;

    public BatchChangeTask(final BasicTaskObservable orderBasicTask,
                           final TaskParamsUtil taskParamsUtil) {
        this.basicTask = orderBasicTask;
        this.taskParamsUtil = taskParamsUtil;
    }

    public Observable<OrderEvent> close(final Collection<IOrder> orders,
                                        final SimpleClosePositionParams closePositionParams) {
        final Function<IOrder, Observable<OrderEvent>> taskCall =
                order -> taskParamsUtil.composeTaskWithEventHandling(basicTask.close(CloseParams
                    .withOrder(order)
                    .build()), closePositionParams);
        return forBasicTask(orders,
                            BatchMode.MERGE,
                            taskCall);
    }

    public Observable<OrderEvent> cancelSL(final Collection<IOrder> orders,
                                           final Function<IOrder, CancelSLParams> paramsFactory,
                                           final BatchMode batchMode) {
        final Function<IOrder, Observable<OrderEvent>> taskCall =
                order -> taskParamsUtil.composeTaskWithEventHandling(basicTask.setStopLossPrice(SetSLParams
                    .setSLAtPrice(order, platformSettings.noSLPrice())
                    .build()), paramsFactory.apply(order));
        return forBasicTask(orders,
                            batchMode,
                            taskCall);
    }

    public Observable<OrderEvent> cancelTP(final Collection<IOrder> orders,
                                           final Function<IOrder, CancelTPParams> paramsFactory,
                                           final BatchMode batchMode) {
        final Function<IOrder, Observable<OrderEvent>> taskCall =
                order -> taskParamsUtil.composeTaskWithEventHandling(basicTask.setTakeProfitPrice(SetTPParams
                    .setTPAtPrice(order, platformSettings.noTPPrice())
                    .build()), paramsFactory.apply(order));
        return forBasicTask(orders,
                            batchMode,
                            taskCall);
    }

    private Observable<OrderEvent> forBasicTask(final Collection<IOrder> orders,
                                                final BatchMode batchMode,
                                                final Function<IOrder, Observable<OrderEvent>> basicTask) {
        final List<Observable<OrderEvent>> observables = Observable
            .fromIterable(orders)
            .map(basicTask::apply)
            .toList()
            .blockingGet();

        return batchMode == BatchMode.MERGE
                ? Observable.merge(observables)
                : Observable.concat(observables);
    }
}
