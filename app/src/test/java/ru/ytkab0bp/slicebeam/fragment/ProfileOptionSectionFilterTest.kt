package ru.ytkab0bp.slicebeam.fragment

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.function.Predicate

class ProfileOptionSectionFilterTest {
    private data class Entry(
        val name: String,
        val isHeader: Boolean = false,
        val isGroupHeader: Boolean = false,
    )

    @Test
    fun preservesMatchingRowsAndDropsEmptySections() {
        val firstHeader = Entry("Quality", isHeader = true)
        val firstMatch = Entry("layer_height")
        val rejected = Entry("expert_option")
        val emptyHeader = Entry("Empty", isHeader = true)
        val lastHeader = Entry("Speed", isHeader = true)
        val lastMatch = Entry("outer_wall_speed")
        val source = listOf<Entry?>(
            firstHeader, firstMatch, rejected, emptyHeader, rejected, null, lastHeader, lastMatch,
        )

        val result = ProfileOptionSectionFilter.filter(
            source,
            Predicate { it.isHeader },
            Predicate { it.name != "expert_option" },
        )

        assertEquals(listOf(firstHeader, firstMatch, lastHeader, lastMatch), result)
    }

    @Test
    fun returnsNoOrphanRowsWhenNoSectionHasMatches() {
        val header = Entry("Walls", isHeader = true)
        val rejected = Entry("expert_option")

        val result = ProfileOptionSectionFilter.filter(
            listOf<Entry?>(header, rejected),
            Predicate { it.isHeader },
            Predicate { false },
        )

        assertEquals(emptyList<Entry>(), result)
    }

    @Test
    fun hierarchicalFilterKeepsOnlyMatchingGroupsAndTheirParentCategories() {
        val quality = Entry("Quality", isHeader = true)
        val walls = Entry("Walls", isGroupHeader = true)
        val matchingWall = Entry("wall_loops")
        val infill = Entry("Infill", isGroupHeader = true)
        val rejectedInfill = Entry("sparse_infill_density")
        val speed = Entry("Speed", isHeader = true)
        val travel = Entry("Travel", isGroupHeader = true)
        val matchingTravel = Entry("travel_speed")

        val result = ProfileOptionSectionFilter.filterHierarchical(
            listOf<Entry?>(quality, walls, matchingWall, infill, rejectedInfill, speed, travel, matchingTravel),
            Predicate { it.isHeader },
            Predicate { it.isGroupHeader },
            Predicate { it.name == "wall_loops" || it.name == "travel_speed" },
        )

        assertEquals(listOf(quality, walls, matchingWall, speed, travel, matchingTravel), result)
    }
}
