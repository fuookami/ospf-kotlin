package fuookami.ospf.kotlin.framework.gantt_scheduling.application.service.bunch

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.UInt64

class BranchGroupTrackerTest {
    private val tracker = BranchGroupTracker<String, String>(
        executors = listOf("executor"),
        groupOfBunch = { it },
        groupsOfExecutor = { executor -> setOf("$executor/slot-1", "$executor/slot-2") }
    )

    @Test
    fun `slot groups keep executor active until every slot is fixed`() {
        assertEquals(UInt64(2UL), tracker.amount)
        assertEquals(UInt64.one, tracker.notFixedAmount(setOf("executor/slot-1")))
        assertFalse(tracker.allGroupsFixed("executor", setOf("executor/slot-1")))
        assertTrue(tracker.allGroupsFixed(
            executor = "executor",
            fixedBunches = setOf("executor/slot-1", "executor/slot-2")
        ))
    }

    @Test
    fun `only unrestricted pricing over all executors is global`() {
        val allExecutors = setOf("executor-1", "executor-2")

        assertTrue(hasFullPricingDomain(
            allExecutors = allExecutors,
            requestedExecutors = allExecutors,
            hasFixedBunches = false,
            hasKeptBunches = false,
            hasHiddenExecutors = false
        ))
        assertFalse(hasFullPricingDomain(
            allExecutors = allExecutors,
            requestedExecutors = setOf("executor-1"),
            hasFixedBunches = false,
            hasKeptBunches = false,
            hasHiddenExecutors = false
        ))
        assertFalse(hasFullPricingDomain(
            allExecutors = allExecutors,
            requestedExecutors = allExecutors,
            hasFixedBunches = true,
            hasKeptBunches = false,
            hasHiddenExecutors = false
        ))
        assertFalse(hasFullPricingDomain(
            allExecutors = allExecutors,
            requestedExecutors = allExecutors,
            hasFixedBunches = false,
            hasKeptBunches = true,
            hasHiddenExecutors = false
        ))
        assertFalse(hasFullPricingDomain(
            allExecutors = allExecutors,
            requestedExecutors = allExecutors,
            hasFixedBunches = false,
            hasKeptBunches = false,
            hasHiddenExecutors = true
        ))
    }
}
