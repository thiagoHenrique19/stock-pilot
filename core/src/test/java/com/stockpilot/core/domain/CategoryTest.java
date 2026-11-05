package com.stockpilot.core.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.stockpilot.core.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

class CategoryTest {

    @Test
    void shouldTrimName() {
        Category category = Category.create("  Beverages ", "Drinks");

        assertThat(category.getName()).isEqualTo("Beverages");
        assertThat(category.getDescription()).isEqualTo("Drinks");
    }

    @Test
    void shouldRejectBlankName() {
        assertThatThrownBy(() -> Category.create("  ", null)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> Category.create(null, null)).isInstanceOf(BusinessRuleException.class);
    }
}
