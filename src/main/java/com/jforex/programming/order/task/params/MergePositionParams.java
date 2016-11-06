package com.jforex.programming.order.task.params;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import com.dukascopy.api.Instrument;
import com.jforex.programming.order.event.OrderEventType;

import io.reactivex.functions.Action;

public class MergePositionParams implements RetryParams {

    private final String mergeOrderLabel;
    private final Map<OrderEventType, OrderEventConsumer> consumerForEvent;
    private final BiConsumer<Throwable, Instrument> errorConsumer;
    private final InstrumentConsumer startConsumer;
    private final InstrumentConsumer completeConsumer;
    private final int noOfRetries;
    private final long delayInMillis;

    private MergePositionParams(final Builder builder) {
        mergeOrderLabel = builder.mergeOrderLabel;
        consumerForEvent = builder.consumerForEvent;
        errorConsumer = builder.errorConsumer;
        startConsumer = builder.startConsumer;
        completeConsumer = builder.completeConsumer;
        noOfRetries = builder.noOfRetries;
        delayInMillis = builder.delayInMillis;
    }

    public String mergeOrderLabel() {
        return mergeOrderLabel;
    }

    public Map<OrderEventType, OrderEventConsumer> consumerForEvent() {
        return consumerForEvent;
    }

    public final Action startAction(final Instrument instrument) {
        return () -> startConsumer.accept(instrument);
    }

    public final Action completeAction(final Instrument instrument) {
        return () -> completeConsumer.accept(instrument);
    }

    public final ErrorConsumer errorConsumer(final Instrument instrument) {
        return err -> errorConsumer.accept(err, instrument);
    }

    @Override
    public int noOfRetries() {
        return noOfRetries;
    }

    @Override
    public long delayInMillis() {
        return delayInMillis;
    }

    public static Builder mergeWith(final String mergeOrderLabel) {
        checkNotNull(mergeOrderLabel);

        return new Builder(mergeOrderLabel);
    }

    public static class Builder {

        private final String mergeOrderLabel;
        private final Map<OrderEventType, OrderEventConsumer> consumerForEvent = new HashMap<>();
        private BiConsumer<Throwable, Instrument> errorConsumer;
        private InstrumentConsumer startConsumer;
        private InstrumentConsumer completeConsumer;
        private int noOfRetries;
        private long delayInMillis;

        public Builder(final String mergeOrderLabel) {
            this.mergeOrderLabel = mergeOrderLabel;
        }

        private Builder setEventConsumer(final OrderEventType orderEventType,
                                         final OrderEventConsumer consumer) {
            checkNotNull(consumer);

            consumerForEvent.put(orderEventType, consumer);
            return this;
        }

        public Builder doOnStart(final InstrumentConsumer startConsumer) {
            checkNotNull(startConsumer);

            this.startConsumer = startConsumer;
            return this;
        }

        public Builder doOnException(final BiConsumer<Throwable, Instrument> errorConsumer) {
            checkNotNull(errorConsumer);

            this.errorConsumer = errorConsumer;
            return this;
        }

        public Builder doOnComplete(final InstrumentConsumer completeConsumer) {
            checkNotNull(completeConsumer);

            this.completeConsumer = completeConsumer;
            return this;
        }

        public Builder retryOnReject(final int noOfRetries,
                                     final long delayInMillis) {
            this.noOfRetries = noOfRetries;
            this.delayInMillis = delayInMillis;
            return this;
        }

        public Builder doOnMerge(final OrderEventConsumer mergeConsumer) {
            return setEventConsumer(OrderEventType.MERGE_OK, mergeConsumer);
        }

        public Builder doOnMergeClose(final OrderEventConsumer mergeCloseConsumer) {
            return setEventConsumer(OrderEventType.MERGE_CLOSE_OK, mergeCloseConsumer);
        }

        public Builder doOnReject(final OrderEventConsumer rejectConsumer) {
            return setEventConsumer(OrderEventType.MERGE_REJECTED, rejectConsumer);
        }

        public MergePositionParams build() {
            return new MergePositionParams(this);
        }
    }
}