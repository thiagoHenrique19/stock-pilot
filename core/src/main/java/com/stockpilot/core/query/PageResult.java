package com.stockpilot.core.query;

import java.util.List;
import java.util.function.Function;

public record PageResult<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public PageResult(List<T> content, int page, int size, long totalElements) {
        this(content, page, size, totalElements, size == 0 ? 0 : (int) Math.ceil((double) totalElements / size));
    }

    public <R> PageResult<R> map(Function<? super T, ? extends R> mapper) {
        return new PageResult<>(content.stream().<R>map(mapper).toList(), page, size, totalElements, totalPages);
    }
}
