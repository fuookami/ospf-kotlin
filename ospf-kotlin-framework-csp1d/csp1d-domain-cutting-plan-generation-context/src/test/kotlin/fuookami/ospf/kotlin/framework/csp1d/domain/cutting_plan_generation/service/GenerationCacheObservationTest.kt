package fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.service

import kotlin.test.*
import kotlin.time.Duration
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.*
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.*
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.*

class GenerationCacheObservationTest {
    @Test
    fun disabledTemplateCachesPreserveResultsAndBudgetsForEveryChangedGenerator() {
        val input = input("custom")
        val arithmetic = DefaultQuantityArithmetic.resolveFor(Flt64.zero).value!!
        for (kind in listOf("dfs", "nsum", "fullsum")) {
            fun generator(
                workers: Long,
                maxPlans: Long,
                timeout: Duration? = null
            ): Csp1dInitialCuttingPlanGenerator<Flt64> {
                val constraints = listOf(
                    MaxKnifeCountConstraint<Flt64>(UInt64(5)),
                    object : CuttingPlanConstraint<Flt64> {
                        override fun isSatisfied(context: CuttingPlanConstraintContext<Flt64>): Boolean = true
                    }
                )
                return when (kind) {
                    "nsum" -> NSumGenerator(
                        constraints = constraints,
                        arithmetic = arithmetic,
                        maxDepth = UInt64(5),
                        maxPlans = Int64(maxPlans),
                        timeout = timeout,
                        parallelism = Int64(workers)
                    )
                    "fullsum" -> FullSumGenerator(
                        constraints = constraints,
                        arithmetic = arithmetic,
                        maxPlans = Int64(maxPlans),
                        timeout = timeout,
                        parallelism = Int64(workers)
                    )
                    else -> DFSGenerator(
                        constraints = constraints,
                        arithmetic = arithmetic,
                        maxPlans = Int64(maxPlans),
                        timeout = timeout,
                        parallelism = Int64(workers)
                    )
                }
            }
            val sequential = generator(1, 100_000).generateWithReport(input)
            val parallel = generator(4, 100_000).generateWithReport(input)
            assertTrue(sequential.plans.isNotEmpty(), kind)
            assertEquals(
                expected = sequential.plans.map { it.canonicalKey() }.toSet(),
                actual = parallel.plans.map { it.canonicalKey() }.toSet(),
                message = kind
            )
            assertEquals(Int64.zero, parallel.statistics.materialSliceTemplateCacheMisses)
            assertEquals(Int64.zero, parallel.statistics.materialSliceTemplateCacheHits)
            val bounded = generator(4, 7).generateWithReport(input)
            assertTrue(bounded.plans.size <= 7, kind)
            val expired = generator(
                workers = 4,
                maxPlans = 100_000,
                timeout = Duration.ZERO
            ).generateWithReport(input)
            assertTrue(expired.plans.isEmpty(), kind)
            assertEquals(
                expected = CuttingPlanGenerationStopReason.Timeout,
                actual = expired.statistics.stopReason,
                message = kind
            )
        }
    }

    @Test
    fun realDfsReportsEquivalentDistinctAndDisabledCacheConsumption() {
        for (scenario in listOf("equivalent", "distinct", "custom")) {
            val input = input(scenario)
            val custom = scenario == "custom"
            val reports = listOf(1, 4).map { workers ->
                val constraints = mutableListOf<CuttingPlanConstraint<Flt64>>(MaxKnifeCountConstraint(UInt64(5)))
                if (custom) constraints.add(object : CuttingPlanConstraint<Flt64> {
                    override fun isSatisfied(context: CuttingPlanConstraintContext<Flt64>): Boolean = true
                })
                val report = DFSGenerator(
                    constraints = constraints,
                    arithmetic = DefaultQuantityArithmetic.resolveFor(Flt64.zero).value!!,
                    maxPlans = Int64(100_000),
                    parallelism = Int64(workers.toLong())
                ).generateWithReport(input)
                val stats = report.statistics
                println("CACHE_OBSERVATION scenario=$scenario workers=$workers plans=${report.plans.size} visited=${stats.visitedNodes} widthHits=${stats.materialWidthIndexCacheHits} templateHits=${stats.materialSliceTemplateCacheHits} templateMisses=${stats.materialSliceTemplateCacheMisses}")
                assertTrue(report.plans.isNotEmpty())
                if (custom) {
                    assertEquals(Int64.zero, stats.materialSliceTemplateCacheHits)
                    assertEquals(Int64.zero, stats.materialSliceTemplateCacheMisses)
                } else {
                    assertEquals(Int64(12), stats.materialSliceTemplateCacheHits + stats.materialSliceTemplateCacheMisses)
                    if (scenario == "distinct") assertEquals(Int64(12), stats.materialSliceTemplateCacheMisses)
                    if (scenario == "equivalent" && workers == 1) {
                        assertEquals(Int64.one, stats.materialSliceTemplateCacheMisses)
                        assertEquals(Int64(11), stats.materialSliceTemplateCacheHits)
                    }
                }
                report.plans.map { it.canonicalKey() }.toSet()
            }
            assertEquals(reports[0], reports[1], "scenario=$scenario")
        }
    }

    private fun input(scenario: String): CuttingPlanGenerationInput<Flt64> {
        val products = List(8) { index -> Product(
            id = productIdOf("p$index"),
            name = "p$index",
            width = listOf(Quantity(Flt64(0.17 + index * 0.03), Meter))
        ) }
        return CuttingPlanGenerationInput(
            products = products,
            materials = List(12) { index -> Material(
                id = materialIdOf("m$index"),
                name = "m$index",
                widthRange = WidthRange(
                    width = QuantityRange(
                        lowerBound = Quantity(Flt64.zero, Meter),
                        upperBound = Quantity(Flt64(if (scenario == "distinct") 1.2 + index * 0.01 else 1.2), Meter)
                    ),
                    step = Quantity(Flt64(0.01), Meter)
                )
            ) },
            machines = emptyList(),
            demands = products.map { ProductDemand.roll(it, Quantity(Flt64(8), RollCountUnit)) }
        )
    }
}
