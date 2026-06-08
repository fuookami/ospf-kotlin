package fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.service

import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.Csp1dInitialCuttingPlanGenerator
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.CuttingPlanGenerationInput
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.CuttingPlanGenerationStatistics
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.CuttingPlanGenerationStopReason
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.GenerationConstraints
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Flt64QuantityArithmetic
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Material
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Product
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.ProductDemand
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.QuantityRange
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.RollCountUnit
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.WidthRange

class GeneratorMediumScaleBaselineTest {
    private val arithmetic = Flt64QuantityArithmetic

    @Test
    fun generatorsShouldReportMediumScaleBaselineStatistics() {
        val products = listOf(
            product(id = "p-025", width = 0.25),
            product(id = "p-030", width = 0.30),
            product(id = "p-035", width = 0.35),
            product(id = "p-040", width = 0.40),
            product(id = "p-045", width = 0.45),
            product(id = "p-050", width = 0.50)
        )
        val materials = listOf(
            material(id = "m-120", upperBound = 1.20),
            material(id = "m-140", upperBound = 1.40),
            material(id = "m-160", upperBound = 1.60)
        )
        val input = CuttingPlanGenerationInput(
            products = products,
            materials = materials,
            machines = emptyList(),
            demands = products.mapIndexed { index, product ->
                ProductDemand.roll(
                    product = product,
                    quantity = Quantity(Flt64(10.0 + index), RollCountUnit)
                )
            }
        )
        val constraints = GenerationConstraints<Flt64>(
            maxKnifeCount = UInt64(5UL),
            parallelism = 2,
            enableDominancePruning = true
        )
        val generatorCases = listOf(
            GeneratorCase(
                name = "DFS",
                generator = DFSGenerator(
                    constraints = constraints,
                    arithmetic = arithmetic,
                    maxPlans = 512
                )
            ),
            GeneratorCase(
                name = "NSum",
                generator = NSumGenerator(
                    constraints = constraints,
                    arithmetic = arithmetic,
                    maxDepth = UInt64(5UL),
                    maxPlans = 512
                )
            ),
            GeneratorCase(
                name = "NSame",
                generator = NSameGenerator(
                    constraints = constraints,
                    arithmetic = arithmetic,
                    allAmount = true,
                    maxPlans = 512
                )
            ),
            GeneratorCase(
                name = "FullSum",
                generator = FullSumGenerator(
                    constraints = constraints,
                    arithmetic = arithmetic,
                    maxPlans = 512
                )
            )
        )

        for (case in generatorCases) {
            val report = case.generator.generateWithReport(input)
            val baseline = report.statistics

            assertMediumScaleBaseline(
                name = case.name,
                statistics = baseline
            )
            assertTrue(report.plans.isNotEmpty(), "${case.name} should generate plans")
            assertTrue(report.plans.size <= 512, "${case.name} should respect maxPlans")
        }
    }

    private data class GeneratorCase(
        val name: String,
        val generator: Csp1dInitialCuttingPlanGenerator<Flt64>
    )

    private fun assertMediumScaleBaseline(
        name: String,
        statistics: CuttingPlanGenerationStatistics
    ) {
        assertTrue(statistics.visitedNodes > 0L, "$name should visit nodes")
        assertTrue(statistics.generatedCandidates > 0L, "$name should generate candidates")
        assertTrue(statistics.acceptedPlans > 0, "$name should accept plans")
        assertTrue(
            statistics.generatedCandidates >= statistics.acceptedPlans.toLong(),
            "$name generated candidates should cover accepted plans"
        )
        assertTrue(statistics.duplicateCandidates >= 0L, "$name duplicate count should be non-negative")
        assertTrue(statistics.dominatedCandidates >= 0L, "$name dominance count should be non-negative")
        assertTrue(statistics.elapsedMilliseconds >= 0L, "$name elapsed time should be non-negative")
        assertTrue(
            statistics.stopReason == CuttingPlanGenerationStopReason.Exhausted ||
                    statistics.stopReason == CuttingPlanGenerationStopReason.MaxPlans,
            "$name should stop by exhaustion or max plan limit"
        )
    }

    private fun product(
        id: String,
        width: Double
    ): Product<Flt64> {
        return Product(
            id = id,
            name = "product-$id",
            width = listOf(
                Quantity(Flt64(width), Meter)
            )
        )
    }

    private fun material(
        id: String,
        upperBound: Double
    ): Material<Flt64> {
        return Material(
            id = id,
            name = "material-$id",
            widthRange = WidthRange(
                width = QuantityRange(
                    lowerBound = Quantity(Flt64.zero, Meter),
                    upperBound = Quantity(Flt64(upperBound), Meter)
                ),
                step = Quantity(Flt64(0.05), Meter)
            )
        )
    }
}
