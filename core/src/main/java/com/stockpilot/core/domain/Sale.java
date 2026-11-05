package com.stockpilot.core.domain;

import com.stockpilot.core.discount.DiscountPolicy;
import com.stockpilot.core.exception.BusinessRuleException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "sale_id", nullable = false)
    private List<SaleItem> items = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Column(precision = 19, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    private SaleStatus status = SaleStatus.COMPLETED;

    private String receiptNumber;

    @Column(precision = 19, scale = 2)
    private BigDecimal amountReceived;

    @Column(name = "change_amount", precision = 19, scale = 2)
    private BigDecimal change;

    private Instant createdAt = Instant.now();

    @Column(name = "created_by", updatable = false)
    private String createdBy;

    private Instant cancelledAt;

    @Version
    private Long version;

    protected Sale() {
    }

    private Sale(PaymentMethod paymentMethod, String receiptNumber) {
        this.paymentMethod = paymentMethod;
        this.receiptNumber = receiptNumber;
    }

    public static Sale open(PaymentMethod paymentMethod) {
        return new Sale(paymentMethod, null);
    }

    public static Sale open(PaymentMethod paymentMethod, String receiptNumber) {
        return new Sale(paymentMethod, receiptNumber);
    }

    public void addItem(Product product, int quantity) {
        if (!product.isActive()) {
            throw new BusinessRuleException("Product " + product.getName() + " is inactive and cannot be sold");
        }
        items.add(new SaleItem(product, quantity));
    }

    public void applyDiscount(DiscountPolicy policy) {
        discountAmount = policy.calculate(getSubtotal());
    }

    public void receivePayment(BigDecimal received) {
        if (received == null) {
            return;
        }
        if (paymentMethod != PaymentMethod.CASH) {
            throw new BusinessRuleException("Amount received is only accepted for cash payments");
        }
        BigDecimal total = getTotal();
        if (received.compareTo(total) < 0) {
            throw new BusinessRuleException("Amount received must cover the sale total of " + total);
        }
        amountReceived = received;
        change = received.subtract(total);
    }

    public void cancel() {
        if (status == SaleStatus.CANCELLED) {
            throw new BusinessRuleException("Sale " + receiptNumber + " is already cancelled");
        }
        status = SaleStatus.CANCELLED;
        cancelledAt = Instant.now();
    }

    public boolean isCancelled() {
        return status == SaleStatus.CANCELLED;
    }

    public BigDecimal getSubtotal() {
        return items.stream().map(SaleItem::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotal() {
        return getSubtotal().subtract(discountAmount);
    }

    public Long getId() {
        return id;
    }

    public List<SaleItem> getItems() {
        return List.copyOf(items);
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public SaleStatus getStatus() {
        return status;
    }

    public String getReceiptNumber() {
        return receiptNumber;
    }

    public BigDecimal getAmountReceived() {
        return amountReceived;
    }

    public BigDecimal getChange() {
        return change;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }
}
