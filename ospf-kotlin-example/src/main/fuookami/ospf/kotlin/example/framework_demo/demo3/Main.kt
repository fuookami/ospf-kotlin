package fuookami.ospf.kotlin.example.framework_demo.demo3

import fuookami.ospf.kotlin.utils.functional.*

import fuookami.ospf.kotlin.math.algebra.number.*

import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.Meter

import fuookami.ospf.kotlin.core.solver.scip.ScipColumnGenerationSolver

import fuookami.ospf.kotlin.framework.csp1d.application.model.Csp1dConfiguration
import fuookami.ospf.kotlin.framework.csp1d.application.model.csp1dProblem
import fuookami.ospf.kotlin.framework.csp1d.application.service.Csp1dColumnGeneration
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.ReducedCostPricingGenerator
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.GenerationConstraints
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.service.FullSumGenerator
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.service.NSameGenerator
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.*

/** One-dimensional Cutting Stock Problem solved via column generation with SCIP. */
class CSP {
    private val rawLength = 1000.0
    private val rawProducts: List<RawProduct> = listOf(
        RawProduct(width = 450.0, demand = 97.0),
        RawProduct(width = 360.0, demand = 610.0),
        RawProduct(width = 310.0, demand = 395.0),
        RawProduct(width = 140.0, demand = 211.0)
    )

    suspend operator fun invoke(): Try {
        val products = rawProducts.mapIndexed { index, raw ->
            Product(
                id = "p-$index",
                name = "product-${raw.width.toInt()}",
                width = listOf(Quantity(Flt64(raw.width), Meter))
            )
        }
        val material = Material(
            id = "m-1000",
            name = "material-1000",
            widthRange = WidthRange(
                width = QuantityRange(
                    lowerBound = Quantity(Flt64.zero, Meter),
                    upperBound = Quantity(Flt64(rawLength), Meter)
                ),
                step = Quantity(Flt64.one, Meter)
            )
        )
        val problem = csp1dProblem<Flt64> {
            products(products)
            material(material)
            demands(
                rawProducts.mapIndexed { index, raw ->
                    ProductDemand.legacyRoll(
                        product = products[index],
                        rollAmount = Flt64(raw.demand)
                    )
                }
            )
            configuration(
                Csp1dConfiguration(
                    maxInitialPlans = Int64(16),
                    maxPricingPlans = Int64(32),
                    iterationLimit = Int64(16)
                )
            )
        }
        val pricingEnumerator = FullSumGenerator(
            constraints = GenerationConstraints(
                maxKnifeCount = UInt64(8UL)
            ),
            arithmetic = DefaultQuantityArithmetic.resolveFor(Flt64.one),
            maxPlans = Int64(256)
        )
        val solver = Csp1dColumnGeneration(
            solver = ScipColumnGenerationSolver(),
            initialGenerator = NSameGenerator(
                arithmetic = DefaultQuantityArithmetic.resolveFor(Flt64.one),
                maxPlans = Int64(16)
            ),
            pricingGenerator = ReducedCostPricingGenerator(pricingEnumerator)
        )
        val result = solver.solveWithTrace(problem)

        println(
            result.solution.produce.cuttingPlans.joinToString(";") { usage ->
                val pattern = usage.plan.slices.joinToString(",") { slice ->
                    "${slice.width.value} * ${slice.amount}"
                }
                "$pattern: ${usage.amount}"
            }
        )
        println("termination=${result.trace.terminationReason}; plans=${result.trace.finalPlanCount}")
        return ok
    }

    private data class RawProduct(
        val width: Double,
        val demand: Double
    )
}
