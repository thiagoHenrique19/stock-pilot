package com.stockpilot.core.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.stockpilot.core.discount.PercentageDiscountPolicy;
import com.stockpilot.core.exception.BusinessRuleException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class SaleTest {

    private final Product coffee = Product.create("C-1", "Coffee", new BigDecimal("20.00"), 50, 5);
    private final Product milk = Product.create("M-1", "Milk", new BigDecimal("5.50"), 50, 5);

    @Test
    void shouldSumItemsIntoSubtotal() {
        Sale sale = Sale.open(PaymentMethod.CASH);

        sale.addItem(coffee, 2);
        sale.addItem(milk, 4);

        assertThat(sale.getSubtotal()).isEqualByComparingTo("62.00");
        assertThat(sale.getTotal()).isEqualByComparingTo("62.00");
    }

    @Test
    void shouldApplyDiscountToTotal() {
        Sale sale = Sale.open(PaymentMethod.CREDIT_CARD);
        sale.addItem(coffee, 5);

        sale.applyDiscount(new PercentageDiscountPolicy(new BigDecimal("10")));

        assertThat(sale.getDiscountAmount()).isEqualByComparingTo("10.00");
        assertThat(sale.getTotal()).isEqualByComparingTo("90.00");
    }

    @Test
    void shouldRejectInactiveProducts() {
        Product inactive = Product.create("I-1", "Old", new BigDecimal("1.00"), 5, 0);
        inactive.deactivate();
        Sale sale = Sale.open(PaymentMethod.CASH);

        assertThatThrownBy(() -> sale.addItem(inactive, 1))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("inactive");
    }

    private Sale saleWithTotal(PaymentMethod method) {
        Sale sale = Sale.open(method, "SALE-000001");
        sale.addItem(coffee, 2);
        return sale;
    }

    @Test
    void shouldStartCompletedWithReceiptNumber() {
        Sale sale = saleWithTotal(PaymentMethod.PIX);

        assertThat(sale.getStatus()).isEqualTo(SaleStatus.COMPLETED);
        assertThat(sale.getReceiptNumber()).isEqualTo("SALE-000001");
        assertThat(sale.getCancelledAt()).isNull();
    }

    @Test
    void shouldCancelOnceOnly() {
        Sale sale = saleWithTotal(PaymentMethod.PIX);

        sale.cancel();

        assertThat(sale.getStatus()).isEqualTo(SaleStatus.CANCELLED);
        assertThat(sale.getCancelledAt()).isNotNull();
        assertThatThrownBy(sale::cancel)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already cancelled");
    }

    @Test
    void shouldCalculateChangeForCashPayments() {
        Sale sale = saleWithTotal(PaymentMethod.CASH);

        sale.receivePayment(new BigDecimal("50.00"));

        assertThat(sale.getAmountReceived()).isEqualByComparingTo("50.00");
        assertThat(sale.getChange()).isEqualByComparingTo("10.00");
    }

    @Test
    void shouldAcceptExactCashAmountWithZeroChange() {
        Sale sale = saleWithTotal(PaymentMethod.CASH);

        sale.receivePayment(new BigDecimal("40.00"));

        assertThat(sale.getChange()).isEqualByComparingTo("0.00");
    }

    @Test
    void shouldComputeChangeAfterDiscount() {
        Sale sale = saleWithTotal(PaymentMethod.CASH);
        sale.applyDiscount(new com.stockpilot.core.discount.FixedAmountDiscountPolicy(new BigDecimal("10")));

        sale.receivePayment(new BigDecimal("50.00"));

        assertThat(sale.getChange()).isEqualByComparingTo("20.00");
    }

    @Test
    void shouldRejectCashAmountThatDoesNotCoverTotal() {
        Sale sale = saleWithTotal(PaymentMethod.CASH);

        assertThatThrownBy(() -> sale.receivePayment(new BigDecimal("39.99")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cover");
    }

    @Test
    void shouldRejectAmountReceivedForNonCashPayments() {
        Sale sale = saleWithTotal(PaymentMethod.PIX);

        assertThatThrownBy(() -> sale.receivePayment(new BigDecimal("50.00")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cash");
    }

    @Test
    void shouldIgnoreMissingAmountReceived() {
        Sale sale = saleWithTotal(PaymentMethod.CASH);

        sale.receivePayment(null);

        assertThat(sale.getAmountReceived()).isNull();
        assertThat(sale.getChange()).isNull();
    }
}
