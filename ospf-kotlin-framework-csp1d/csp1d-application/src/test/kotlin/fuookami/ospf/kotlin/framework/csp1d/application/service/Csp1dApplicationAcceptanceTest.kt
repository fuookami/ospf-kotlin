package fuookami.ospf.kotlin.framework.csp1d.application.service

import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.eq
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.Csp1dInitialCuttingPlanGenerator
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Costar
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Machine
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Material
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Product
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.ProductDemand
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.QuantityRange
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.WidthRange
import fuookami.ospf.kotlin.framework.csp1d.application.model.Csp1dConfiguration
import fuookami.ospf.kotlin.framework.csp1d.application.model.Csp1dProblem

class Csp1dApplicationAcceptanceTest {
    @Test
    fun milpShouldSolveRollDemandWithoutPoitDependency() {
        val product = product(
            id = "p-roll",
            width = 1.2
        )
        val problem = Csp1dProblem(
            products = listOf(product),
            materials = listOf(
                material(
                    id = "m-roll",
                    lowerWidth = 0.8,
                    upperWidth = 2.0,
                    machineId = "machine-roll"
                )
            ),
            machines = listOf(
                machine(
                    id = "machine-roll",
                    capacity = 800.0
                )
            ),
            demands = listOf(
                ProductDemand.legacyRoll(
                    product = product,
                    rollAmount = Flt64(6.0)
                )
            ),
            configuration = Csp1dConfiguration(
                maxInitialPlans = 32,
                maxPricingPlans = 8,
                iterationLimit = 4
            )
        )

        val solution = Csp1dMilp<Flt64>().solve(problem)

        assertEquals(1, solution.produce.cuttingPlans.size)
        assertTrue(solution.produce.unmetDemands.isEmpty())
        assertEquals("p-roll", solution.produce.cuttingPlans.first().plan.demandContributions.first().product.id)
    }

    @Test
    fun milpShouldCoverCostarRestWidthAndMachineCapacity() {
        val product = product(
            id = "p-capacity",
            width = 1.3
        )
        val material = material(
            id = "m-capacity",
            lowerWidth = 1.0,
            upperWidth = 2.0,
            machineId = "machine-capacity"
        )
        val machine = machine(
            id = "machine-capacity",
            capacity = 500.0
        )
        val problem = Csp1dProblem(
            products = listOf(product),
            materials = listOf(material),
            machines = listOf(machine),
            costars = listOf(
                Costar(
                    id = "c-side",
                    name = "side-coproduct",
                    widthRange = widthRange(
                        lower = 0.2,
                        upper = 0.2
                    ),
                    length = Quantity(Flt64(100.0), Meter)
                )
            ),
            demands = listOf(
                ProductDemand.legacyRoll(
                    product = product,
                    rollAmount = Flt64(4.0)
                )
            ),
            configuration = Csp1dConfiguration(
                maxInitialPlans = 32,
                maxPricingPlans = 8,
                iterationLimit = 4
            )
        )

        val solution = Csp1dMilp<Flt64>().solve(problem)
        val selectedPlan = solution.produce.cuttingPlans.first().plan
        val restWidth = selectedPlan.restWidth
        val machineCapacityUsage = solution.produce.machineUsages.firstOrNull {
            it.machine.id == "machine-capacity"
        }?.used

        assertNotNull(restWidth)
        assertTrue(restWidth eq Quantity(Flt64(0.7), Meter))
        assertNotNull(machineCapacityUsage)
        assertTrue(machineCapacityUsage eq Quantity(Flt64(500.0), Kilogram))
    }

    @Test
    fun columnGenerationShouldGenerateNewPlansFromShadowPrice() {
        val product = product(
            id = "p-cg",
            width = 1.0
        )
        val problem = Csp1dProblem(
            products = listOf(product),
            materials = listOf(
                material(
                    id = "m-cg",
                    lowerWidth = 0.8,
                    upperWidth = 1.5
                )
            ),
            machines = emptyList(),
            demands = listOf(
                ProductDemand.legacyRoll(
                    product = product,
                    rollAmount = Flt64(2.0)
                )
            ),
            configuration = Csp1dConfiguration(
                maxInitialPlans = 8,
                maxPricingPlans = 8,
                iterationLimit = 4
            )
        )
        val columnGeneration = Csp1dColumnGeneration<Flt64>(
            initialGenerator = Csp1dInitialCuttingPlanGenerator { emptyList() }
        )

        val (solution, trace) = columnGeneration.solveWithTrace(problem)

        assertTrue(trace.pricedPlanCount.any { it.toULong() > 0UL })
        assertTrue(solution.generatedPlans.isNotEmpty())
        assertTrue(solution.generatedPlans.any { it.id.startsWith("pricing-") })
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
        lowerWidth: Double,
        upperWidth: Double,
        machineId: String? = null
    ): Material<Flt64> {
        return Material(
            id = id,
            name = "material-$id",
            widthRange = widthRange(
                lower = lowerWidth,
                upper = upperWidth
            ),
            machineId = machineId
        )
    }

    private fun machine(
        id: String,
        capacity: Double
    ): Machine<Flt64> {
        return Machine(
            id = id,
            name = "machine-$id",
            capacity = Quantity(Flt64(capacity), Kilogram)
        )
    }

    private fun widthRange(
        lower: Double,
        upper: Double
    ): WidthRange<Flt64> {
        return WidthRange(
            width = QuantityRange(
                lowerBound = Quantity(Flt64(lower), Meter),
                upperBound = Quantity(Flt64(upper), Meter)
            ),
            step = Quantity(Flt64(0.1), Meter)
        )
    }
}
