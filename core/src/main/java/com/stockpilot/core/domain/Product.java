package com.stockpilot.core.domain;

import com.stockpilot.core.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.math.BigDecimal;

@Entity
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String sku;

    private String name;

    @Column(precision = 19, scale = 2)
    private BigDecimal price;

    private int quantity;
    private int minimumQuantity;

    protected Product() {
    }

    private Product(String sku, String name, BigDecimal price, int quantity, int minimumQuantity) {
        this.sku = sku;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.minimumQuantity = minimumQuantity;
    }

    public static Product create(String sku, String name, BigDecimal price, int quantity, int minimumQuantity) {
        if (price == null || price.signum() < 0) {
            throw new BusinessRuleException("Price cannot be negative");
        }
        if (quantity < 0 || minimumQuantity < 0) {
            throw new BusinessRuleException("Quantities cannot be negative");
        }
        return new Product(sku, name, price, quantity, minimumQuantity);
    }

    public void addStock(int amount) {
        requirePositive(amount);
        quantity += amount;
    }

    public void removeStock(int amount) {
        requirePositive(amount);
        if (amount > quantity) {
            throw new BusinessRuleException("Insufficient stock for " + name + ": requested " + amount
                    + ", available " + quantity);
        }
        quantity -= amount;
    }

    public boolean isLowStock() {
        return quantity <= minimumQuantity;
    }

    private void requirePositive(int amount) {
        if (amount <= 0) {
            throw new BusinessRuleException("Amount must be positive");
        }
    }

    public Long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getMinimumQuantity() {
        return minimumQuantity;
    }
}
