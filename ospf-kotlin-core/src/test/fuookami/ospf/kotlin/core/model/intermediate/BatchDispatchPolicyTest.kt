package fuookami.ospf.kotlin.core.model.intermediate

import kotlin.test.*

class BatchDispatchPolicyTest {
    @Test
    fun shouldKeepSegmentSafeForEmptyAndSmallBatches() {
        val emptyPlan = computeBatchDispatchPlan(itemCount = 0, availableProcessors = 1)
        assertEquals(10, emptyPlan.segmentSize)
        assertFalse(emptyPlan.shouldUseParallelPath)
        assertFalse(emptyPlan.shouldSplitIntoSegments)

        val smallPlan = computeBatchDispatchPlan(itemCount = 9, availableProcessors = 2)
        assertEquals(10, smallPlan.segmentSize)
        assertFalse(smallPlan.shouldUseParallelPath)
        assertFalse(smallPlan.shouldSplitIntoSegments)
    }

    @Test
    fun shouldUsePowerOfTenSegmentForLargeBatch() {
        val plan = computeBatchDispatchPlan(itemCount = 10_000, availableProcessors = 8)
        assertTrue(plan.segmentSize >= 100)
        assertTrue(plan.shouldUseParallelPath)
        assertTrue(plan.shouldSplitIntoSegments)
    }

    @Test
    fun shouldBuildNonOverlappingSlicesAndCoverAllItems() {
        val slices = buildBatchSlices(
            itemCount = 25,
            segmentSize = 10
        )
        assertEquals(3, slices.size)
        assertEquals(0, slices[0].fromIndex)
        assertEquals(10, slices[0].toIndexExclusive)
        assertEquals(10, slices[1].fromIndex)
        assertEquals(20, slices[1].toIndexExclusive)
        assertEquals(20, slices[2].fromIndex)
        assertEquals(25, slices[2].toIndexExclusive)
    }
}
