package com.jforex.programming.order.builder;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.function.Consumer;

import com.dukascopy.api.IOrder;

public class SetLabelBuilder extends OrderBuilder {

    private final IOrder orderToSetLabel;
    private final String newLabel;
    private final Consumer<IOrder> rejectAction;
    private final Consumer<IOrder> okAction;

    public final IOrder orderToSetLabel() {
        return orderToSetLabel;
    }

    public final String newLabel() {
        return newLabel;
    }

    public final Consumer<IOrder> rejectAction() {
        return rejectAction;
    }

    public final Consumer<IOrder> okAction() {
        return okAction;
    }

    public interface SetLabelOption extends CommonOption<SetLabelOption> {
        public SetLabelOption onReject(Consumer<IOrder> closeRejectAction);

        public SetLabelOption onOK(Consumer<IOrder> closeOKAction);

        public SetLabelBuilder build();
    }

    private SetLabelBuilder(final Builder builder) {
        super(builder);
        orderToSetLabel = builder.orderToSetLabel;
        newLabel = builder.newLabel;
        rejectAction = builder.rejectAction;
        okAction = builder.okAction;
    }

    public static final SetLabelOption forParams(final IOrder orderToSetLabel,
                                                 final String newLabel) {
        return new Builder(checkNotNull(orderToSetLabel), checkNotNull(newLabel));
    }

    private static class Builder extends CommonBuilder<Builder> implements SetLabelOption {

        private final IOrder orderToSetLabel;
        private final String newLabel;
        private Consumer<IOrder> rejectAction = o -> {};
        private Consumer<IOrder> okAction = o -> {};

        private Builder(final IOrder orderToSetLabel,
                        final String newLabel) {
            this.orderToSetLabel = orderToSetLabel;
            this.newLabel = newLabel;
        }

        @Override
        public SetLabelOption onReject(final Consumer<IOrder> rejectAction) {
            this.rejectAction = checkNotNull(rejectAction);
            return this;
        }

        @Override
        public SetLabelOption onOK(final Consumer<IOrder> okAction) {
            this.okAction = checkNotNull(okAction);
            return this;
        }

        @Override
        public SetLabelBuilder build() {
            return new SetLabelBuilder(this);
        }
    }
}
