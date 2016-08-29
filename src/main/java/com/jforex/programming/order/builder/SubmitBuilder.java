package com.jforex.programming.order.builder;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.function.Consumer;

import com.dukascopy.api.IOrder;
import com.jforex.programming.order.OrderParams;

public class SubmitBuilder extends OrderBuilder {

    private final OrderParams orderParams;
    private final Consumer<IOrder> submitRejectAction;
    private final Consumer<IOrder> fillRejectAction;
    private final Consumer<IOrder> submitOKAction;
    private final Consumer<IOrder> partialFillAction;
    private final Consumer<IOrder> fillAction;

    public final OrderParams orderParams() {
        return orderParams;
    }

    public final Consumer<IOrder> submitRejectAction() {
        return submitRejectAction;
    }

    public final Consumer<IOrder> fillRejectAction() {
        return fillRejectAction;
    }

    public final Consumer<IOrder> submitOKAction() {
        return submitOKAction;
    }

    public final Consumer<IOrder> partialFillAction() {
        return partialFillAction;
    }

    public final Consumer<IOrder> fillAction() {
        return fillAction;
    }

    public interface SubmitOption extends CommonOption<SubmitOption> {
        public SubmitOption onSubmitReject(Consumer<IOrder> submitRejectAction);

        public SubmitOption onFillReject(Consumer<IOrder> fillRejectAction);

        public SubmitOption onSubmitOK(Consumer<IOrder> submitOKAction);

        public SubmitOption onPartialFill(Consumer<IOrder> partialFillAction);

        public SubmitOption onFill(Consumer<IOrder> fillAction);

        public SubmitBuilder build();
    }

    private SubmitBuilder(final Builder builder) {
        super(builder);
        orderParams = builder.orderParams;
        submitRejectAction = builder.submitRejectAction;
        fillRejectAction = builder.fillRejectAction;
        submitOKAction = builder.submitOKAction;
        partialFillAction = builder.partialFillAction;
        fillAction = builder.fillAction;
    }

    public static final SubmitOption forOrderParams(final OrderParams orderParams) {
        return new Builder(checkNotNull(orderParams));
    }

    private static class Builder extends CommonBuilder<Builder> implements SubmitOption {

        private final OrderParams orderParams;
        private Consumer<IOrder> submitRejectAction = o -> {};
        private Consumer<IOrder> fillRejectAction = o -> {};
        private Consumer<IOrder> submitOKAction = o -> {};
        private Consumer<IOrder> partialFillAction = o -> {};
        private Consumer<IOrder> fillAction = o -> {};

        private Builder(final OrderParams orderParams) {
            this.orderParams = orderParams;
        }

        @Override
        public SubmitOption onSubmitReject(final Consumer<IOrder> submitRejectAction) {
            this.submitRejectAction = checkNotNull(submitRejectAction);
            return this;
        }

        @Override
        public SubmitOption onFillReject(final Consumer<IOrder> fillRejectAction) {
            this.fillRejectAction = checkNotNull(fillRejectAction);
            return this;
        }

        @Override
        public SubmitOption onSubmitOK(final Consumer<IOrder> submitOKAction) {
            this.submitOKAction = checkNotNull(submitOKAction);
            return this;
        }

        @Override
        public SubmitOption onPartialFill(final Consumer<IOrder> partialFillAction) {
            this.partialFillAction = checkNotNull(partialFillAction);
            return this;
        }

        @Override
        public SubmitOption onFill(final Consumer<IOrder> fillAction) {
            this.fillAction = checkNotNull(fillAction);
            return this;
        }

        @Override
        public SubmitBuilder build() {
            return new SubmitBuilder(this);
        }
    }
}
