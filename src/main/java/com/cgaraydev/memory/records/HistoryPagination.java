package com.cgaraydev.memory.records;

import java.util.List;
import java.util.Objects;

/** Paginación inmutable del historial para no crear cientos de nodos JavaFX. */
public final class HistoryPagination {

    public static final int DEFAULT_PAGE_SIZE = 15;

    private HistoryPagination() { }

    public static <T> Page<T> slice(List<T> items, int requestedPage, int pageSize) {
        Objects.requireNonNull(items, "El historial es obligatorio.");
        if (pageSize <= 0) {
            throw new IllegalArgumentException("El tamaño de página debe ser positivo.");
        }

        int total = items.size();
        int pageCount = (total == 0) ? 1 : 1 + (total - 1) / pageSize;
        int index = Math.max(0, Math.min(requestedPage, pageCount - 1));
        int start = index * pageSize;
        int end = Math.min(start + pageSize, total);
        return new Page<>(List.copyOf(items.subList(start, end)), index,
                pageCount, total, start);
    }

    public record Page<T>(
            List<T> entries,
            int index,
            int pageCount,
            int totalItems,
            int firstItemIndex
    ) {
        public Page {
            entries = List.copyOf(Objects.requireNonNull(entries));
            if (index < 0 || pageCount < 1 || index >= pageCount
                    || totalItems < 0 || firstItemIndex < 0) {
                throw new IllegalArgumentException("Página de historial inválida.");
            }
        }

        public int displayPage() {
            return index + 1;
        }

        public boolean hasPrevious() {
            return index > 0;
        }

        public boolean hasNext() {
            return index < pageCount - 1;
        }
    }
}
