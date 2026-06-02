package fuookami.ospf.kotlin.framework.csp1d.application.service

import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.eq
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.core.model.basic.RegistrationStatusCallBack
import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.core.solver.output.FeasibleSolverOutput
import fuookami.ospf.kotlin.core.solver.output.SolvingStatusCallBack
import fuookami.ospf.kotlin.framework.solver.ColumnGenerationSolver
import fuookami.ospf.kotlin.framework.solver.Flt64FeasibleSolverOutput
import fuookami.ospf.kotlin.framework.solver.Flt64LinearMetaModel
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Costar
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Machine
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Material
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Product
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.ProductDemand
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.QuantityRange
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.WidthRange
import fuookami.ospf.kotlin.framework.csp1d.application.model.Csp1dConfiguration
import fuookami.ospf.kotlin.framework.csp1d.application.model.Csp1dProblem

/**
 * CSP1D 验收测试使用的 fake solver / Fake solver for CSP1D acceptance tests
 *
 * 按 tokensInSolver.size 生成全 1.0 解向量，使 MILP 求解能产生非空 Produce。
 * Generate an all-1.0 solution vector by tokensInSolver.size so the MILP path can produce non-empty output.
 */
private class Csp1dFakeSolver : ColumnGenerationSolver {
    override val name: String = "csp1d-fake"

    override suspend fun solveMILP(
        name: String,
        metaModel: Flt64LinearMetaModel,
        toLogModel: Boolean,
        registrationStatusCallBack: RegistrationStatusCallBack?,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<Flt64FeasibleSolverOutput> {
        val size = metaModel.tokens.tokensInSolver.size
        val solution: Solution<Flt64> = (0 until size).map { Flt64(1.0) }
        return Ok(
            FeasibleSolverOutput(
                obj = Flt64.zero,
                solution = solution,
                time = Duration.ZERO,
                possibleBestObj = Flt64.zero,
                gap = Flt64.zero
            )
        )
    }

    override suspend fun solveLP(
        name: String,
        metaModel: Flt64LinearMetaModel,
        toLogModel: Boolean,
        registrationStatusCallBack: RegistrationStatusCallBack?,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<ColumnGenerationSolver.LPResult> {
        val size = metaModel.tokens.tokensInSolver.size
        val solution: Solution<Flt64> = (0 until size).map { Flt64(1.0) }
        return Ok(
            ColumnGenerationSolver.LPResult(
                result = FeasibleSolverOutput(
                    obj = Flt64.zero,
                    solution = solution,
                    time = Duration.ZERO,
                    possibleBestObj = Flt64.zero,
                    gap = Flt64.zero
                ),
                dualSolution = emptyMap()
            )
        )
    }
}

class Csp1dApplicationAcceptanceTest {
    private val fakeSolver = Csp1dFakeSolver()

    @Test
    fun milpShouldSolveRollDemandWithoutPoitDependency() = runBlocking {
        val product = product(
            id = "p-roll",
            width = 1.2
        )
        val problem = Csp1dProblem<Flt64>(
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

        val solution = Csp1dMilp<Flt64>(fakeSolver).solve(problem)

        assertTrue(solution.produce.cuttingPlans.isNotEmpty())
        assertTrue(solution.produce.unmetDemands.isEmpty())
        assertEquals("p-roll", solution.produce.cuttingPlans.first().plan.demandContributions.first().product.id)
    }

    @Test
    fun milpShouldCoverCostarRestWidthAndMachineCapacity() = runBlocking {
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
        val problem = Csp1dProblem<Flt64>(
            products = listOf(product),
            materials = listOf(material),
            machines = listOf(machine),
            costars = listOf(
                Costar(
                    id = "c-side",
                    name = "side-coproduct",
                    width = listOf(
                        Quantity(Flt64(0.2), Meter)
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

        val solution = Csp1dMilp<Flt64>(fakeSolver).solve(problem)
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
    fun columnGenerationShouldSolveInitialPlans() = runBlocking {
        val product = product(
            id = "p-cg",
            width = 1.0
        )
        val problem = Csp1dProblem<Flt64>(
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
            solver = fakeSolver
        )

        val (solution, trace) = columnGeneration.solveWithTrace(problem)

        // C3 仅求解初始方案池，C4 再接入 shadow price/pricing / C3 only solves initial plans; C4 adds shadow price/pricing
        assertEquals(UInt64(trace.initialPlanCount.toULong()), trace.finalPlanCount)
        assertTrue(trace.pricedPlanCount.isEmpty())
        assertTrue(solution.generatedPlans.isNotEmpty())
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
