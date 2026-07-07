package dev.nikomaru.advancedshopfinder.utils.data

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SortTypeTest {
    @Test
    fun nextCyclesForwardAndWraps() {
        assertEquals(SortType.DESC_PRICE_PER_STACK, SortType.ASC_PRICE_PER_STACK.next())
        // 末尾は先頭へ循環する
        assertEquals(SortType.entries.first(), SortType.entries.last().next())
    }

    @Test
    fun previousCyclesBackwardAndWraps() {
        assertEquals(SortType.ASC_PRICE_PER_STACK, SortType.DESC_PRICE_PER_STACK.previous())
        // 先頭は末尾へ循環する
        assertEquals(SortType.entries.last(), SortType.entries.first().previous())
    }

    @Test
    fun nextThenPreviousIsIdentity() {
        SortType.entries.forEach { sortType ->
            assertEquals(sortType, sortType.next().previous())
        }
    }
}
