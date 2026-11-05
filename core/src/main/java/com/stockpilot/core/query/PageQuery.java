package com.stockpilot.core.query;

import com.stockpilot.core.exception.InvalidRequestException;
import java.util.Locale;

public record PageQuery(int page, int size, String sortBy, SortDirection direction) {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    public PageQuery {
        if (page < 0) {
            throw new InvalidRequestException("Page index must not be negative");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new InvalidRequestException("Page size must be between 1 and " + MAX_SIZE);
        }
        if (direction == null) {
            direction = SortDirection.ASC;
        }
    }

    public static PageQuery of(Integer page, Integer size, String sortBy, String direction) {
        return new PageQuery(
                page == null ? 0 : page,
                size == null ? DEFAULT_SIZE : size,
                sortBy == null || sortBy.isBlank() ? null : sortBy,
                parseDirection(direction));
    }

    public static PageQuery firstPage() {
        return of(null, null, null, null);
    }

    private static SortDirection parseDirection(String direction) {
        if (direction == null || direction.isBlank()) {
            return SortDirection.ASC;
        }
        try {
            return SortDirection.valueOf(direction.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("Sort direction must be ASC or DESC");
        }
    }
}
