package com.stockpilot.core.domain;

import com.stockpilot.core.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.Instant;

/**
 * Immutable ledger entry. {@code quantity} is the signed change applied to the stock
 * (positive for entries, negative for exits) and {@code balanceAfter} the resulting balance.
 */
@Entity
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MovementType type;

    private int quantity;

    private String reason;

    private String reference;

    private int balanceAfter;

    private Instant createdAt = Instant.now();

    protected StockMovement() {
    }

    private StockMovement(Product product, MovementType type, int quantity, String reason, String reference) {
        this.product = product;
        this.type = type;
        this.quantity = quantity;
        this.reason = reason;
        this.reference = reference;
        this.balanceAfter = product.getQuantity();
    }

    public static StockMovement record(Product product, MovementType type, int quantity,
                                       String reason, String reference) {
        if (quantity == 0) {
            throw new BusinessRuleException("Movement quantity must not be zero");
        }
        if (type == MovementType.IN && quantity < 0 || type == MovementType.OUT && quantity > 0) {
            throw new BusinessRuleException("Movement quantity sign does not match type " + type);
        }
        if (type == MovementType.ADJUSTMENT && (reason == null || reason.isBlank())) {
            throw new BusinessRuleException("An adjustment requires a reason");
        }
        return new StockMovement(product, type, quantity, reason == null ? null : reason.trim(), reference);
    }

    public Long getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public MovementType getType() {
        return type;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getReason() {
        return reason;
    }

    public String getReference() {
        return reference;
    }

    public int getBalanceAfter() {
        return balanceAfter;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
