package com.stockpilot.core.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.stockpilot.core.exception.InvalidRequestException;
import java.util.List;
import org.junit.jupiter.api.Test;

class PageQueryTest {

    @Test
    void shouldApplyDefaultsWhenValuesAreMissing() {
        PageQuery query = PageQuery.of(null, null, null, null);

        assertThat(query.page()).isZero();
        assertThat(query.size()).isEqualTo(PageQuery.DEFAULT_SIZE);
        assertThat(query.sortBy()).isNull();
        assertThat(query.direction()).isEqualTo(SortDirection.ASC);
    }

    @Test
    void shouldParseDirectionIgnoringCase() {
        assertThat(PageQuery.of(1, 10, "name", "desc").direction()).isEqualTo(SortDirection.DESC);
    }

    @Test
    void shouldRejectInvalidPagination() {
        assertThatThrownBy(() -> PageQuery.of(-1, 10, null, null)).isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> PageQuery.of(0, 0, null, null)).isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> PageQuery.of(0, PageQuery.MAX_SIZE + 1, null, null))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> PageQuery.of(0, 10, null, "sideways"))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void pageResultShouldComputeTotalPagesAndMapContent() {
        PageResult<Integer> result = new PageResult<>(List.of(1, 2), 0, 2, 5);

        assertThat(result.totalPages()).isEqualTo(3);
        assertThat(result.map(n -> "n" + n).content()).containsExactly("n1", "n2");
        assertThat(result.map(String::valueOf).totalElements()).isEqualTo(5);
    }
}
