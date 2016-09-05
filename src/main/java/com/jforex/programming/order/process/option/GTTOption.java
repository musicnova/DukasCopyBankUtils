package com.jforex.programming.order.process.option;

import java.util.function.Consumer;

import com.dukascopy.api.IOrder;

public interface GTTOption<T extends GTTOption<T>> extends CommonOption<T> {

    public T doOnSetGTTReject(Consumer<IOrder> rejectAction);

    public T doOnSetGTT(Consumer<IOrder> doneAction);
}
