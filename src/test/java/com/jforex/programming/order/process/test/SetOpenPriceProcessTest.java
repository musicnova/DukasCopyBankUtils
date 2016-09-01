package com.jforex.programming.order.process.test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.Map;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.dukascopy.api.IOrder;
import com.jforex.programming.order.event.OrderEventType;
import com.jforex.programming.order.process.SetOpenPriceProcess;
import com.jforex.programming.test.common.CommonUtilForTest;

public class SetOpenPriceProcessTest extends CommonUtilForTest {

    private SetOpenPriceProcess process;

    @Mock
    private Consumer<Throwable> errorActionMock;
    @Mock
    private Consumer<IOrder> doneActionMock;
    @Mock
    private Consumer<IOrder> rejectedActionMock;
    private Map<OrderEventType, Consumer<IOrder>> eventHandlerForType;
    private final double newOpenPrice = 1.1234;

    @Before
    public void SetOpenPriceProcess() {
        process = SetOpenPriceProcess
            .forParams(buyOrderEURUSD, newOpenPrice)
            .onError(errorActionMock)
            .onOpenPriceChange(doneActionMock)
            .onOpenPriceReject(rejectedActionMock)
            .doRetries(3, 1500L)
            .build();

        eventHandlerForType = process.eventHandlerForType();
    }

    @Test
    public void processValuesAreCorrect() {
        assertThat(process.errorAction(), equalTo(errorActionMock));
        assertThat(process.order(), equalTo(buyOrderEURUSD));
        assertThat(process.newOpenPrice(), equalTo(newOpenPrice));
        assertThat(process.noOfRetries(), equalTo(3));
        assertThat(process.delayInMillis(), equalTo(1500L));
        assertThat(eventHandlerForType.size(), equalTo(2));
    }

    @Test
    public void actionsAreCorrectMapped() {
        eventHandlerForType.get(OrderEventType.CHANGE_PRICE_REJECTED).accept(buyOrderEURUSD);
        eventHandlerForType.get(OrderEventType.CHANGED_PRICE).accept(buyOrderEURUSD);

        verify(doneActionMock).accept(buyOrderEURUSD);
        verify(rejectedActionMock).accept(buyOrderEURUSD);
    }
}
