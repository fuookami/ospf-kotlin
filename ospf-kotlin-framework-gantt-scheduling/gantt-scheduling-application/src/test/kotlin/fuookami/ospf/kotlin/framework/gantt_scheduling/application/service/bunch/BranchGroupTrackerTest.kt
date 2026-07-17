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
}
