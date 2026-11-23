package com.delivery.backoffice.report.dto;

import java.util.List;

/** Stable JSON shape instead of serializing Spring's PageImpl. */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
}
