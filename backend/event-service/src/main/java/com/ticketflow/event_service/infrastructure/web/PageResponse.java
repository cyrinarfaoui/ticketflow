package com.ticketflow.event_service.infrastructure.web;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Format JSON stable pour une page de résultats.
 * On n'expose pas directement Page de Spring : son JSON est verbeux et peut changer d'une version à l'autre.
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages()
        );
    }
}