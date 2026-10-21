/**
 * 多目标算子契约测试 / Multi-objective operator contract tests
*/
package fuookami.ospf.kotlin.core.solver.heuristic.operator.multi_objective

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.Flt64

/** 便捷构造目标向量 / Convenience constructor for objective vectors */
private fun vector(vararg values: Double): List<Flt64> = values.map { Flt64(it) }

class MultiObjectiveTest {

    private val dominance = MinimizationDominance(2)

    @Test
    fun paretoDominanceShouldRequireNoWorseAndOneBetter() {
        // 全部维度不差，且至少一维严格更好
        assertTrue(dominance.dominates(vector(1.0, 2.0), vector(2.0, 2.0)))
        assertTrue(dominance.dominates(vector(1.0, 1.0), vector(2.0, 2.0)))
        assertTrue(dominance.dominates(vector(1.0, 2.0), vector(1.0, 3.0)))

        // 一维更好一维更差：互不支配
        assertFalse(dominance.dominates(vector(1.0, 3.0), vector(2.0, 2.0)))
        assertFalse(dominance.dominates(vector(2.0, 2.0), vector(1.0, 3.0)))

        // 完全相同不构成支配（支配要求严格改进）
        assertFalse(dominance.dominates(vector(2.0, 2.0), vector(2.0, 2.0)))
    }

    @Test
    fun maximizeDirectionShouldInvertTheComparison() {
        val maximizing = ParetoDominance(listOf(false, false))
        assertTrue(maximizing.dominates(vector(3.0, 3.0), vector(2.0, 2.0)))
        assertFalse(maximizing.dominates(vector(2.0, 2.0), vector(3.0, 3.0)))
    }

    @Test
    fun nonDominatedSortShouldLayerThePopulation() {
        val points = listOf(
            vector(1.0, 5.0),   // front 1
            vector(2.0, 4.0),   // front 1
            vector(3.0, 3.0),   // front 1
            vector(4.0, 4.0),   // front 2（被 (3,3) 支配）
            vector(5.0, 5.0)    // front 3（被 (4,4) 支配）
        )

        val fronts = FastNonDominatedSort.sort(points, { it }, dominance)

        assertEquals(3, fronts.size)
        assertEquals(3, fronts[0].size)
        assertEquals(listOf(vector(1.0, 5.0), vector(2.0, 4.0), vector(3.0, 3.0)), fronts[0])
        assertEquals(listOf(vector(4.0, 4.0)), fronts[1])
        assertEquals(listOf(vector(5.0, 5.0)), fronts[2])
    }

    @Test
    fun nonDominatedSortShouldHandleEmptyAndSingleInput() {
        assertTrue(FastNonDominatedSort.sort(emptyList<List<Flt64>>(), { it }, dominance).isEmpty())

        val single = FastNonDominatedSort.sort(listOf(vector(1.0, 1.0)), { it }, dominance)
        assertEquals(1, single.size)
        assertEquals(1, single.first().size)
    }

    @Test
    fun crowdingDistanceShouldBeInfiniteAtTheBoundaries() {
        val front = listOf(
            vector(0.0, 4.0),
            vector(1.0, 3.0),
            vector(2.0, 2.0),
            vector(4.0, 0.0)
        )
        val distances = CrowdingDistance.assign(front)

        assertEquals(4, distances.size)
        assertEquals(Double.POSITIVE_INFINITY, distances[0])
        assertEquals(Double.POSITIVE_INFINITY, distances[3])
        assertTrue(distances[1].isFinite() && distances[1] > 0.0)
        assertTrue(distances[2].isFinite() && distances[2] > 0.0)
    }

    @Test
    fun crowdingDistanceShouldBeInfiniteForTinyFronts() {
        assertTrue(CrowdingDistance.assign(emptyList()).isEmpty())
        assertTrue(CrowdingDistance.assign(listOf(vector(1.0, 1.0))).all { it.isInfinite() })
        assertTrue(CrowdingDistance.assign(listOf(vector(1.0, 1.0), vector(2.0, 2.0))).all { it.isInfinite() })
    }

    @Test
    fun paretoArchiveShouldRejectDominatedCandidates() {
        val archive = ParetoArchive<List<Flt64>>(8, { it }, dominance)

        archive.offer(vector(2.0, 2.0))
        archive.offer(vector(3.0, 3.0))   // 被 (2,2) 支配，丢弃
        assertEquals(1, archive.size)

        archive.offer(vector(1.0, 3.0))   // 与 (2,2) 互不支配，保留
        archive.offer(vector(3.0, 1.0))   // 与前面互不支配，保留
        assertEquals(3, archive.size)

        archive.offer(vector(0.5, 0.5))   // 支配全部现有成员
        assertEquals(1, archive.size)
        assertEquals(vector(0.5, 0.5), archive.items.first())
    }

    @Test
    fun paretoArchiveShouldStayBoundedAndMutuallyNonDominated() {
        val archive = ParetoArchive<List<Flt64>>(4, { it }, dominance)
        for (step in 0 until 20) {
            val value = step.toDouble()
            archive.offer(vector(value, 20.0 - value))
        }

        assertEquals(4, archive.size)
        val members = archive.items
        for (left in members.indices) {
            for (right in members.indices) {
                if (left == right) {
                    continue
                }
                assertFalse(
                    dominance.dominates(members[left], members[right]),
                    "archive contains a dominated pair"
                )
            }
        }
    }

    @Test
    fun paretoArchiveShouldDeduplicateIdenticalObjectives() {
        val archive = ParetoArchive<List<Flt64>>(4, { it }, dominance)
        archive.offer(vector(1.0, 1.0))
        archive.offer(vector(1.0, 1.0))
        assertEquals(1, archive.size)
    }
}
