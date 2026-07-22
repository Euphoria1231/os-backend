package com.tsy.oa.intelligence.search.dto;

import java.util.List;

public record SearchPageResponse<T>(
        List<T> items,
        long total,
        int page,
        int pageSize
) {
}
