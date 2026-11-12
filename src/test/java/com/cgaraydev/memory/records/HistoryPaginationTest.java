package com.cgaraydev.memory.records;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class HistoryPaginationTest {

    private static List<Integer> numbers(int count) {
        return IntStream.range(0, count).boxed().toList();
    }

    @Test
    void emptyHistoryShouldReturnFirstEmptyPage() {
        var page = HistoryPagination.slice(List.<Integer>of(), 0, 15);
        assertTrue(page.entries().isEmpty());
        assertEquals(1, page.pageCount());
        assertEquals(1, page.displayPage());
        assertEquals(0, page.totalItems());
        assertFalse(page.hasPrevious());
        assertFalse(page.hasNext());
    }

    @Test
    void shortHistoryShouldFitInOnePage() {
        var page = HistoryPagination.slice(numbers(8), 0, 15);
        assertEquals(numbers(8), page.entries());
        assertEquals(1, page.pageCount());
        assertEquals(0, page.firstItemIndex());
    }

    @Test
    void fullPageShouldNotCreateEmptyNextPage() {
        var page = HistoryPagination.slice(numbers(15), 0, 15);
        assertEquals(15, page.entries().size());
        assertFalse(page.hasNext());
    }

    @Test
    void twoPagesShouldHaveCorrectSlices() {
        var first = HistoryPagination.slice(numbers(19), 0, 15);
        var second = HistoryPagination.slice(numbers(19), 1, 15);
        assertEquals(numbers(15), first.entries());
        assertEquals(List.of(15, 16, 17, 18), second.entries());
        assertEquals(15, second.firstItemIndex());
        assertEquals(19, second.totalItems());
        assertTrue(first.hasNext());
        assertTrue(second.hasPrevious());
        assertFalse(second.hasNext());
    }

    @Test
    void outOfRangePageShouldClampToLast() {
        var page = HistoryPagination.slice(numbers(31), 99, 15);
        assertEquals(2, page.index());
        assertEquals(3, page.displayPage());
        assertEquals(List.of(30), page.entries());
    }

    @Test
    void negativePageShouldClampToFirst() {
        var page = HistoryPagination.slice(numbers(40), -5, 15);
        assertEquals(0, page.index());
        assertFalse(page.hasPrevious());
    }

    @Test
    void invalidPageSizeShouldBeRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> HistoryPagination.slice(numbers(5), 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> HistoryPagination.slice(numbers(5), 0, -1));
    }

    @Test
    void nullListShouldBeRejected() {
        assertThrows(NullPointerException.class,
                () -> HistoryPagination.slice(null, 0, 15));
    }

    @Test
    void returnedEntriesShouldBeImmutableAndIndependent() {
        var mutable = new java.util.ArrayList<>(numbers(5));
        var page = HistoryPagination.slice(mutable, 0, 15);
        mutable.clear();
        assertEquals(numbers(5), page.entries());
        assertThrows(UnsupportedOperationException.class,
                () -> page.entries().add(5));
    }
}
