package com.jforex.programming.position.task;

import static com.jforex.programming.order.OrderStaticUtil.isClosed;
import static com.jforex.programming.order.OrderStaticUtil.isSLSetTo;
import static com.jforex.programming.order.OrderStaticUtil.isTPSetTo;

import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.jforex.programming.order.OrderChangeUtil;
import com.jforex.programming.order.OrderCreateUtil;
import com.jforex.programming.order.event.OrderEvent;

import com.dukascopy.api.IOrder;
import com.dukascopy.api.Instrument;

import rx.Observable;

public class PositionSingleTask {

    private final OrderCreateUtil orderCreateUtil;
    private final OrderChangeUtil orderChangeUtil;

    private static final Logger logger = LogManager.getLogger(PositionSingleTask.class);

    public PositionSingleTask(final OrderCreateUtil orderCreateUtil,
                              final OrderChangeUtil orderChangeUtil) {
        this.orderCreateUtil = orderCreateUtil;
        this.orderChangeUtil = orderChangeUtil;
    }

    public Observable<OrderEvent> setSLObservable(final IOrder orderToChangeSL,
                                                  final double newSL) {
        final double currentSL = orderToChangeSL.getStopLossPrice();
        final Observable<OrderEvent> setSLObservable =
                Observable.just(orderToChangeSL)
                        .filter(order -> !isSLSetTo(newSL).test(order))
                        .doOnNext(order -> logger.debug("Start to change SL from " + currentSL + " to "
                                + newSL + " for order " + orderToChangeSL.getLabel() + " and position "
                                + orderToChangeSL.getInstrument()))
                        .flatMap(order -> orderChangeUtil.setStopLossPrice(order, newSL))
                        .retryWhen(PositionTaskUtil::shouldRetry)
                        .doOnError(e -> logger.debug("Failed to change SL from " + currentSL + " to " + newSL +
                                " for order " + orderToChangeSL.getLabel() + " and position "
                                + orderToChangeSL.getInstrument() + ".Excpetion: " + e.getMessage()))
                        .doOnNext(order -> logger.debug("Changed SL from " + currentSL + " to " + newSL +
                                " for order " + orderToChangeSL.getLabel() + " and position "
                                + orderToChangeSL.getInstrument()));

        return PositionTaskUtil.connectObservable(setSLObservable);
    }

    public Observable<OrderEvent> setTPObservable(final IOrder orderToChangeTP,
                                                  final double newTP) {
        final double currentTP = orderToChangeTP.getTakeProfitPrice();
        final Observable<OrderEvent> setTPObservable =
                Observable.just(orderToChangeTP)
                        .filter(order -> !isTPSetTo(newTP).test(order))
                        .doOnSubscribe(() -> logger.debug("Start to change TP from " + currentTP + " to "
                                + newTP + " for order " + orderToChangeTP.getLabel() + " and position "
                                + orderToChangeTP.getInstrument()))
                        .flatMap(order -> orderChangeUtil.setTakeProfitPrice(order, newTP))
                        .retryWhen(PositionTaskUtil::shouldRetry)
                        .doOnError(e -> logger.debug("Failed to change TP from " + currentTP + " to " + newTP +
                                " for order " + orderToChangeTP.getLabel() + " and position "
                                + orderToChangeTP.getInstrument() + ".Excpetion: " + e.getMessage()))
                        .doOnCompleted(() -> logger.debug("Changed TP from " + currentTP + " to " + newTP +
                                " for order " + orderToChangeTP.getLabel() + " and position " + newTP
                                + orderToChangeTP.getInstrument()));

        return PositionTaskUtil.connectObservable(setTPObservable);
    }

    public Observable<OrderEvent> mergeObservable(final String mergeOrderLabel,
                                                  final Collection<IOrder> toMergeOrders) {
        final Instrument instrument = toMergeOrders.iterator().next().getInstrument();
        final Observable<OrderEvent> mergeObservable =
                Observable.just(mergeOrderLabel)
                        .doOnSubscribe(() -> logger.debug("Starting to merge with label " + mergeOrderLabel
                                + " for " + instrument + " position."))
                        .flatMap(order -> orderCreateUtil.mergeOrders(mergeOrderLabel, toMergeOrders))
                        .retryWhen(PositionTaskUtil::shouldRetry)
                        .doOnError(e -> logger.error("Merging with label " + mergeOrderLabel
                                + " for " + instrument + " failed! Exception: " + e.getMessage()))
                        .doOnCompleted(() -> logger.debug("Merging with label " + mergeOrderLabel
                                + " for " + instrument + " position was successful."));

        return PositionTaskUtil.connectObservable(mergeObservable);
    }

    public Observable<OrderEvent> closeObservable(final IOrder orderToClose) {
        final Observable<OrderEvent> closeObservable =
                Observable.just(orderToClose)
                        .filter(order -> !isClosed.test(order))
                        .doOnSubscribe(() -> logger.debug("Starting to close order " + orderToClose.getLabel()
                                + " for " + orderToClose.getInstrument() + " position."))
                        .flatMap(order -> orderChangeUtil.close(orderToClose))
                        .retryWhen(PositionTaskUtil::shouldRetry)
                        .doOnError(e -> logger.error("Closing position " + orderToClose.getInstrument()
                                + " failed! Exception: " + e.getMessage()))
                        .doOnCompleted(() -> logger.debug("Closing position "
                                + orderToClose.getInstrument() + " was successful."));

        return PositionTaskUtil.connectObservable(closeObservable);
    }
}