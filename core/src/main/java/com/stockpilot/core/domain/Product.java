package com.stockpilot.core.domain;

import com.stockpilot.core.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Version;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;
import java.util.Locale;

@Entity
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String sku;

    private String name;

    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    private boolean active = true;

    @Column(precision = 19, scale = 2)
    private BigDecimal price;

    private int quantity;
    private int minimumQuantity;

    @Version
    private long version;

    protected Product() {
    }

    private Product(String sku, String name, String description, BigDecimal price, int quantity,
                    int minimumQuantity, Category category) {
        this.sku = normalizeSku(sku);
        this.name = name;
        this.description = description;
        this.category = category;
        this.price = price;
        this.quantity = quantity;
        this.minimumQuantity = minimumQuantity;
    }

    public static Product create(String sku, String name, BigDecimal price, int quantity, int minimumQuantity) {
        return create(sku, name, null, price, quantity, minimumQuantity, null);
    }

    public static Product create(String sku, String name, String description, BigDecimal price, int quantity,
                                 int minimumQuantity, Category category) {
        requireValidPrice(price);
        requireValidQuantities(quantity, minimumQuantity);
        return new Product(sku, name, description, price, quantity, minimumQuantity, category);
    }

    public static String normalizeSku(String sku) {
        return sku.trim().toUpperCase(Locale.ROOT);
    }

    public void update(String name, String description, BigDecimal price, int minimumQuantity, Category category) {
        requireValidPrice(price);
        requireValidQuantities(0, minimumQuantity);
        this.name = name;
        this.description = description;
        this.price = price;
        this.minimumQuantity = minimumQuantity;
        this.category = category;
    }

    public void deactivate() {
        active = false;
    }

    public void reactivate() {
        active = true;
    }

    private static void requireValidPrice(BigDecimal price) {
        if (price == null || price.signum() < 0) {
            throw new BusinessRuleException("Price cannot be negative");
        }
    }

    private static void requireValidQuantities(int quantity, int minimumQuantity) {
        if (quantity < 0 || minimumQuantity < 0) {
            throw new BusinessRuleException("Quantities cannot be negative");
        }
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

    public void adjustStock(int delta) {
        if (delta == 0) {
            throw new BusinessRuleException("Adjustment must not be zero");
        }
        if (quantity + delta < 0) {
            throw new BusinessRuleException("Adjustment would take stock below zero for " + name
                    + ": current " + quantity + ", adjustment " + delta);
        }
        quantity += delta;
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

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    }

    public boolean isActive() {
        return active;
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

    public long getVersion() {
        return version;
    }
}
