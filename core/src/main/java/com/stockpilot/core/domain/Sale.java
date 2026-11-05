package com.stockpilot.core.domain;

import com.stockpilot.core.discount.DiscountPolicy;
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

    private Instant createdAt = Instant.now();

    protected Sale() {
    }

    private Sale(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public static Sale open(PaymentMethod paymentMethod) {
        return new Sale(paymentMethod);
    }

    public void addItem(Product product, int quantity) {
        items.add(new SaleItem(product, quantity));
    }

    public void applyDiscount(DiscountPolicy policy) {
        discountAmount = policy.calculate(getSubtotal());
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

    public Instant getCreatedAt() {
        return createdAt;
    }
}
