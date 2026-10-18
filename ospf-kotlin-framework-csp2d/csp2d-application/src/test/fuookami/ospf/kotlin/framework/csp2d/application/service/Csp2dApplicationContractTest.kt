/**
 * 应用层装配与恢复契约测试。 / Application assembly and recovery contract test.
 *
 * 本测试不接求解器：只验证问题定义、候选方案生成、主问题装配与「由使用次数恢复解」这几段纯逻辑。
 * 求解路径由 Gurobi 集成测试覆盖。
 * / This test does not use a solver: it only covers the problem definition, candidate generation, master problem
 * assembly and the pure "recover the solution from usage counts" logic. The solving paths are covered by the Gurobi
 * integration test.
*/
package fuookami.ospf.kotlin.framework.csp2d.application.service

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.framework.geometry2d.*
import fuookami.ospf.kotlin.framework.csp2d.application.model.*
import fuookami.ospf.kotlin.framework.csp2d.domain.material.model.*
import fuookami.ospf.kotlin.framework.csp2d.domain.sheet_assignment.Csp2dSheetAssignmentConfig
import fuookami.ospf.kotlin.framework.csp2d.infrastructure.CuttingProcess2

class Csp2dApplicationContractTest {

    private fun meter(value: Double): Quantity<FltX> = FltX(value) * Meter

    private fun sheet(): Material2<FltX> {
        return Material2(
            id = materialId2Of("SHEET"),
            name = "SHEET",
            width = meter(100.0),
            height = meter(60.0)
        )
    }

    private fun coil(): Material2<FltX> {
        return Material2(id = materialId2Of("COIL"), name = "COIL", width = meter(100.0))
    }

    private fun product(id: String, width: Double, height: Double): Product2<FltX> {
        return Product2(
            id = productId2Of(id),
            name = id,
            width = meter(width),
            height = meter(height)
        )
    }

    private fun demand(product: Product2<FltX>, count: UInt64): ProductDemand2<FltX> {
        return ProductDemand2(product = product, demand = DemandValue2.Amount(count))
    }

    private fun sheetProblem(
        process: CuttingProcess2 = CuttingProcess2.Freeform,
        config: Csp2dConfiguration = Csp2dConfiguration()
    ): Csp2dProblem {
        val p1 = product("P1", 40.0, 30.0)
        return Csp2dProblem(
            materials = listOf(sheet()),
            products = listOf(p1),
            demands = listOf(demand(p1, UInt64(3))),
            kerf = meter(0.0),
            process = process,
            config = config
        )
    }

    @Test
    fun problemValidationRejectsEmptyInputs() {
        val p1 = product("P1", 40.0, 30.0)
        assertTrue(
            Csp2dProblem(
                materials = emptyList(),
                products = listOf(p1),
                demands = listOf(demand(p1, UInt64.one)),
                kerf = meter(0.0)
            ).validate() is Failed
        )
        assertTrue(
            Csp2dProblem(
                materials = listOf(sheet()),
                products = listOf(p1),
                demands = emptyList(),
                kerf = meter(0.0)
            ).validate() is Failed
        )
        assertTrue(sheetProblem().validate() is Ok)
    }

    @Test
    fun primaryObjectiveFollowsMaterialMode() {
        assertEquals(Csp2dObjectiveKind.SheetCount, sheetProblem().primaryObjective())
        val coilProblem = Csp2dProblem(
            materials = listOf(coil()),
            products = listOf(product("P1", 40.0, 30.0)),
            demands = listOf(demand(product("P1", 40.0, 30.0), UInt64.one)),
            kerf = meter(0.0)
        )
        assertEquals(Csp2dObjectiveKind.ConsumedLength, coilProblem.primaryObjective())
        // 显式配置优先。 / An explicit configuration wins.
        val explicit = sheetProblem(config = Csp2dConfiguration(primary = Csp2dObjectiveKind.Waste))
        assertEquals(Csp2dObjectiveKind.Waste, explicit.primaryObjective())
    }

    @Test
    fun totalPieceDemandSumsDemands() {
        val p1 = product("P1", 40.0, 30.0)
        val p2 = product("P2", 20.0, 10.0)
        val problem = Csp2dProblem(
            materials = listOf(sheet()),
            products = listOf(p1, p2),
            demands = listOf(demand(p1, UInt64(3)), demand(p2, UInt64(4))),
            kerf = meter(0.0)
        )
        assertEquals(7, problem.totalPieceDemand().toInt())
    }

    @Test
    fun candidatePlansAreGeneratedAndValid() {
        val problem = sheetProblem()
        val plans = buildCsp2dPlans(
            problem = problem,
            products = problem.demands.map { it.product to (it.piecesOrNull() ?: UInt64.one) }
        ).valueOrFail()
        assertTrue(plans.isNotEmpty())
        for (plan in plans) {
            assertTrue(plan.validate(problem.kerf) is Ok)
            assertTrue(plan.contributions.isNotEmpty())
        }
    }

    @Test
    fun masterProblemAssembles() {
        val problem = sheetProblem()
        val plans = buildCsp2dPlans(
            problem = problem,
            products = problem.demands.map { it.product to (it.piecesOrNull() ?: UInt64.one) }
        ).valueOrFail()
        val assembly = assembleCsp2dMaster(name = "test", problem = problem, plans = plans).valueOrFail()
        assertEquals(plans.size, assembly.second.plans.size)
    }

    @Test
    fun solutionRecoveryComputesAreasAndCoverage() {
        val problem = sheetProblem()
        val plans = buildCsp2dPlans(
            problem = problem,
            products = problem.demands.map { it.product to (it.piecesOrNull() ?: UInt64.one) }
        ).valueOrFail()
        // 只使用第一个方案两次。 / Use only the first plan twice.
        val usages = plans.indices.map { if (it == 0) 2 else 0 }
        val solution = csp2dSolutionOf(
            problem = problem,
            plans = plans,
            usages = usages,
            iterations = 1
        ).valueOrFail()

        assertEquals(1, solution.usages.size)
        assertEquals(2, solution.usages.first().usage)
        assertEquals(0.0, solution.consumedLength.toDouble(), 1e-9)
        assertEquals(2, solution.sheetCount)
        val pieces = plans.first().contributions.map { it.amount.toInt() }.sum()
        assertEquals(pieces * 2, solution.coverage["P1"])
        assertTrue(solution.usedArea.toDouble() > 0.0)
        assertTrue(solution.utilization.toDouble() > 0.0)
        assertEquals("2", solution.kpi["sheet_count"])
        assertTrue(solution.covers(problem))
    }

    @Test
    fun solutionCoverageDetectsShortfall() {
        val problem = sheetProblem()
        val plans = buildCsp2dPlans(
            problem = problem,
            products = problem.demands.map { it.product to (it.piecesOrNull() ?: UInt64.one) }
        ).valueOrFail()
        // 一个方案都不使用 → 无法覆盖需求。 / Using no plan cannot cover the demand.
        val empty = csp2dSolutionOf(
            problem = problem,
            plans = plans,
            usages = plans.indices.map { 0 },
            iterations = 1
        ).valueOrFail()
        assertTrue(empty.usages.isEmpty())
        assertTrue(!empty.covers(problem))
    }

    @Test
    fun coilProblemRecoversConsumedLength() {
        val p1 = product("P1", 40.0, 30.0)
        val problem = Csp2dProblem(
            materials = listOf(coil()),
            products = listOf(p1),
            demands = listOf(demand(p1, UInt64(2))),
            kerf = meter(0.0),
            sheetConfig = Csp2dSheetAssignmentConfig()
        )
        val plans = buildCsp2dPlans(
            problem = problem,
            products = problem.demands.map { it.product to (it.piecesOrNull() ?: UInt64.one) }
        ).valueOrFail()
        val solution = csp2dSolutionOf(
            problem = problem,
            plans = plans,
            usages = plans.indices.map { if (it == 0) 1 else 0 },
            iterations = 1
        ).valueOrFail()
        // 卷材解必须给出消耗段高。 / A coil solution must report the consumed segment height.
        assertTrue(solution.consumedLength.toDouble() > 0.0)
        assertEquals(1, solution.sheetCount)
    }
}

/**
 * 解包 Ret 结果。 / Unwrap a Ret result.
 *
 * @param T 结果类型 / result type
 * @param message 失败信息 / failure message
 * @return 成功值 / the successful value
*/
private fun <T> Ret<T>.valueOrFail(message: String = "result should succeed"): T {
    return when (this) {
        is Ok -> value
        is Failed -> kotlin.test.fail("$message: ${error.message}")
        is Fatal -> kotlin.test.fail("$message: ${errors.joinToString { it.message }}")
    }
}
