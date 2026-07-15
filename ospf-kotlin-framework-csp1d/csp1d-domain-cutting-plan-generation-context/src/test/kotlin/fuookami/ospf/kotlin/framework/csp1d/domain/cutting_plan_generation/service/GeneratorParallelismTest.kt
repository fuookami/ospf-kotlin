package fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.service

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.*
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.*
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

class GeneratorParallelismTest {
    private val arithmetic = (DefaultQuantityArithmetic.resolveFor(Flt64.one) as Ok).value

    @Test
    fun parallelGeneratorsShouldKeepCanonicalForms() {
        val firstProduct = product(
            id = "p1",
            width = 0.3
        )
        val secondProduct = product(
            id = "p2",
            width = 0.5
        )
        val input = CuttingPlanGenerationInput(
            products = listOf(firstProduct, secondProduct),
            materials = listOf(
                material(
                    id = "m1",
                    upperBound = 1.2
                ),
                material(
                    id = "m2",
                    upperBound = 1.5
                )
            ),
            machines = emptyList(),
            demands = listOf(
                ProductDemand.roll(
                    product = firstProduct,
                    quantity = Quantity(Flt64(5.0), RollCountUnit)
                ),
                ProductDemand.roll(
                    product = secondProduct,
                    quantity = Quantity(Flt64(4.0), RollCountUnit)
                )
            )
        )
        val sequentialConstraints = GenerationConstraints<Flt64>(
            maxKnifeCount = UInt64(4UL)
        )
        val parallelConstraints = GenerationConstraints<Flt64>(
            maxKnifeCount = UInt64(4UL),
            parallelism = Int64(2)
        )

        val generators = listOf(
            DFSGenerator(
                constraints = sequentialConstraints,
                arithmetic = arithmetic,
                maxPlans = Int64(256)
            ) to DFSGenerator(
                constraints = parallelConstraints,
                arithmetic = arithmetic,
                maxPlans = Int64(256)
            ),
            NSumGenerator(
                constraints = sequentialConstraints,
                arithmetic = arithmetic,
                maxDepth = UInt64(4UL),
                maxPlans = Int64(256)
            ) to NSumGenerator(
                constraints = parallelConstraints,
                arithmetic = arithmetic,
                maxDepth = UInt64(4UL),
                maxPlans = Int64(256)
            ),
            NSameGenerator(
                constraints = sequentialConstraints,
                arithmetic = arithmetic,
                allAmount = true,
                maxPlans = Int64(256)
            ) to NSameGenerator(
                constraints = parallelConstraints,
                arithmetic = arithmetic,
                allAmount = true,
                maxPlans = Int64(256)
            ),
            FullSumGenerator(
                constraints = sequentialConstraints,
                arithmetic = arithmetic,
                maxPlans = Int64(256)
            ) to FullSumGenerator(
                constraints = parallelConstraints,
                arithmetic = arithmetic,
                maxPlans = Int64(256)
            )
        )

        for ((sequentialGenerator, parallelGenerator) in generators) {
            assertSameCanonicalForms(
                sequentialGenerator = sequentialGenerator,
                parallelGenerator = parallelGenerator,
                input = input
            )
        }
    }

    private fun assertSameCanonicalForms(
        sequentialGenerator: Csp1dInitialCuttingPlanGenerator<Flt64>,
        parallelGenerator: Csp1dInitialCuttingPlanGenerator<Flt64>,
        input: CuttingPlanGenerationInput<Flt64>
    ) {
        val sequentialReport = sequentialGenerator.generateWithReport(input)
        val parallelReport = parallelGenerator.generateWithReport(input)

        assertTrue(sequentialReport.plans.isNotEmpty())
        assertEquals(canonicalForms(sequentialReport.plans), canonicalForms(parallelReport.plans))
        assertEquals(Int64(parallelReport.plans.size.toLong()), parallelReport.statistics.acceptedPlans)
    }

    private fun canonicalForms(plans: List<CuttingPlan<Flt64>>): Set<Any> {
        return plans.map { it.canonicalKey() }.toSet()
    }

    private fun product(
        id: String,
        width: Double
    ): Product<Flt64> {
        return Product(
            id = productIdOf(id),
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
            id = materialIdOf(id),
            name = "material-$id",
            widthRange = WidthRange(
                width = QuantityRange(
                    lowerBound = Quantity(Flt64.zero, Meter),
                    upperBound = Quantity(Flt64(upperBound), Meter)
                ),
                step = Quantity(Flt64(0.1), Meter)
            )
        )
    }
}
