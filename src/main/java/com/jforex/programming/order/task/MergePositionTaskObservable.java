package com.jforex.programming.order.task;

import java.util.Collection;

import com.dukascopy.api.IOrder;
import com.dukascopy.api.Instrument;
import com.google.common.base.Supplier;
import com.jforex.programming.order.event.OrderEvent;
import com.jforex.programming.order.task.params.position.MergeAllPositionsParams;
import com.jforex.programming.order.task.params.position.MergePositionParams;
import com.jforex.programming.position.PositionUtil;

import io.reactivex.Observable;
import io.reactivex.functions.Function;

public class MergePositionTaskObservable {

    private final CancelSLTPAndMergeTask cancelSLTPAndMergeTask;
    private final PositionUtil positionUtil;

    public MergePositionTaskObservable(final CancelSLTPAndMergeTask cancelSLTPAndMergeTask,
                                       final PositionUtil positionUtil) {
        this.cancelSLTPAndMergeTask = cancelSLTPAndMergeTask;
        this.positionUtil = positionUtil;
    }

    public Observable<OrderEvent> merge(final Collection<IOrder> toMergeOrders,
                                        final MergePositionParams mergePositionParams) {
        return observeSplit(() -> toMergeOrders, mergePositionParams);
    }

    public Observable<OrderEvent> merge(final Instrument instrument,
                                        final MergePositionParams mergePositionParams) {
        return observeSplit(() -> positionUtil.filledOrders(instrument), mergePositionParams);
    }

    private final Observable<OrderEvent> observeSplit(final Supplier<Collection<IOrder>> toMergeOrders,
                                                      final MergePositionParams mergePositionParams) {
        return Observable.defer(() -> cancelSLTPAndMergeTask.observe(toMergeOrders.get(), mergePositionParams));
    }

    public Observable<OrderEvent> mergeAll(final MergeAllPositionsParams mergeAllPositionParams) {
        return Observable.defer(() -> {
            final Function<Instrument, Observable<OrderEvent>> observablesFromFactory =
                    instrument -> merge(instrument, mergeAllPositionParams.mergePositionParams());
            return Observable.merge(positionUtil.observablesFromFactory(observablesFromFactory));
        });
    }
}
