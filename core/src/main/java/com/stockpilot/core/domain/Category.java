package com.stockpilot.core.domain;

import com.stockpilot.core.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Category extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    private String description;

    protected Category() {
    }

    private Category(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public static Category create(String name, String description) {
        if (name == null || name.isBlank()) {
            throw new BusinessRuleException("Category name must not be blank");
        }
        return new Category(name.trim(), description);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
