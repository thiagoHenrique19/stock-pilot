package com.stockpilot.infrastructure.persistence;

import com.stockpilot.core.exception.InvalidRequestException;
import com.stockpilot.core.query.PageQuery;
import com.stockpilot.core.query.PageResult;
import com.stockpilot.core.query.SortDirection;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

final class PageableMapper {

    private PageableMapper() {
    }

    static Pageable toPageable(PageQuery query, Set<String> sortableFields, String defaultSortField) {
        String field = query.sortBy() == null ? defaultSortField : query.sortBy();
        if (!sortableFields.contains(field)) {
            throw new InvalidRequestException("Cannot sort by '" + field + "'. Allowed: "
                    + sortableFields.stream().sorted().toList());
        }
        Sort sort = query.direction() == SortDirection.DESC ? Sort.by(field).descending() : Sort.by(field);
        return PageRequest.of(query.page(), query.size(), sort.and(Sort.by("id")));
    }

    static <T> PageResult<T> toPageResult(Page<T> page) {
        return new PageResult<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
    }
}
