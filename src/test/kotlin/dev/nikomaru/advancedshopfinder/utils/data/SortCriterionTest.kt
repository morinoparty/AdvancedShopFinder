package dev.nikomaru.advancedshopfinder.utils.data

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SortCriterionTest {
    @Test
    fun everySortTypeBelongsToExactlyOneCriterion() {
        SortType.entries.forEach { type ->
            assertEquals(1, SortCriterion.entries.count { it.asc == type || it.desc == type })
        }
    }

    @Test
    fun toSortEntriesPutsEnabledFirstAndKeepsAllCriteria() {
        val entries = listOf(SortType.ASC_DISTANCE, SortType.DESC_PRICE_PER_ITEM).toSortEntries()

        assertEquals(
            listOf(
                SortEntry(SortCriterion.DISTANCE, descending = false, enabled = true),
                SortEntry(SortCriterion.PRICE_PER_ITEM, descending = true, enabled = true),
                SortEntry(SortCriterion.DISTANCE_NEAREST),
                SortEntry(SortCriterion.PRICE_PER_STACK),
            ),
            entries,
        )
    }

    @Test
    fun toSortEntriesDropsDuplicatedCriterion() {
        // 旧 GUI では同じ基準を重複して登録できたため、最初のものだけを採用する
        val entries = listOf(SortType.ASC_DISTANCE, SortType.DESC_DISTANCE).toSortEntries()

        assertEquals(SortCriterion.entries.size, entries.size)
        assertEquals(listOf(SortType.ASC_DISTANCE), entries.toSortTypes())
    }

    @Test
    fun roundTripPreservesOrderAndDirection() {
        val types = listOf(SortType.DESC_PRICE_PER_STACK, SortType.ASC_DISTANCE_NEAREST, SortType.ASC_PRICE_PER_ITEM)
        assertEquals(types, types.toSortEntries().toSortTypes())
    }

    @Test
    fun emptyListYieldsAllDisabled() {
        val entries = emptyList<SortType>().toSortEntries()
        assertEquals(SortCriterion.entries, entries.map { it.criterion })
        assertEquals(emptyList<SortType>(), entries.toSortTypes())
    }
}
