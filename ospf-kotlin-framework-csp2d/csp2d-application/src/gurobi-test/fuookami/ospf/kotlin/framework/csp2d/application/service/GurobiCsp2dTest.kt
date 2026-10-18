/**
 * 真实 Gurobi 集成测试：双模式 × 三工艺。 / Real Gurobi integration test: two modes by three processes.
 *
 * 这组用例回答两个问题：
 * 1. csp2d 能否真的跑出解（主问题、目标、恢复链路都通）；
 * 2. 断头台工艺给出的方案是否**可制造**——图案必须通过 `Pattern2.validate`，且切分树级数不超过工艺上限。
 *
 * 只有带 `csp2d.gurobi.test.enabled=true` 时才运行（由 `gurobi-cg-test` profile 注入）。
 * / These cases answer two questions: whether csp2d really produces a solution (master problem, objective and recovery
 * all wired), and whether a guillotine plan is **manufacturable** — the pattern must pass `Pattern2.validate` and the
 * cut tree must stay within the process stage limit. They only run when `csp2d.gurobi.test.enabled=true`, injected by the
 * `gurobi-cg-test` profile.
*/
package fuookami.ospf.kotlin.framework.csp2d.application.service

import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.framework.csp2d.application.model.*
import fuookami.ospf.kotlin.framework.csp2d.domain.material.model.*
import fuookami.ospf.kotlin.framework.csp2d.infrastructure.CuttingProcess2

@EnabledIfSystemProperty(named = "csp2d.gurobi.test.enabled", matches = "true")
class GurobiCsp2dTest {

    /**
     * 构造米制量值。 / Build a metric quantity.
     *
     * @param value 数值 / value
     * @return 量值 / quantity
    */
    private fun meter(value: Double): Quantity<FltX> = FltX(value) * Meter

    /**
     * 构造 Gurobi 求解器。 / Build a Gurobi solver.
     *
     * @return 求解器 / solver
    */
    private fun solver(): GurobiDelegatingCsp2dSolver {
        return GurobiDelegatingCsp2dSolver(config = SolverConfig(time = 10.seconds))
    }

    /**
     * 构造定尺板材。 / Build a fixed-size sheet material.
     *
     * @return 物料 / material
    */
    private fun sheet(): Material2<FltX> {
        return Material2(
            id = materialId2Of("SHEET"),
            name = "SHEET",
            width = meter(100.0),
            height = meter(60.0)
        )
    }

    /**
     * 构造卷材。 / Build a coil material.
     *
     * @return 物料 / material
    */
    private fun coil(): Material2<FltX> {
        return Material2(id = materialId2Of("COIL"), name = "COIL", width = meter(100.0))
    }

    /**
     * 构造产品。 / Build a product.
     *
     * @param id 编号 / identifier
     * @param width 宽度 / width
     * @param height 高度 / height
     * @return 产品 / product
    */
    private fun product(id: String, width: Double, height: Double): Product2<FltX> {
        return Product2(
            id = productId2Of(id),
            name = id,
            width = meter(width),
            height = meter(height)
        )
    }

    /**
     * 构造问题。 / Build a problem.
     *
     * @param material 物料 / material
     * @param demands 产品与件数 / products and counts
     * @param process 工艺 / process
     * @return 问题 / problem
    */
    private fun problem(
        material: Material2<FltX>,
        demands: List<Pair<Product2<FltX>, UInt64>>,
        process: CuttingProcess2
    ): Csp2dProblem {
        return Csp2dProblem(
            materials = listOf(material),
            products = demands.map { it.first },
            demands = demands.map { (product, count) ->
                ProductDemand2(product = product, demand = DemandValue2.Amount(count))
            },
            kerf = meter(0.0),
            process = process,
            config = Csp2dConfiguration(maxCandidatesPerIteration = 12, iterationLimit = 4)
        )
    }

    /**
     * 断言解覆盖需求且图案可制造。 / Assert that the solution covers the demand and every pattern is manufacturable.
     *
     * @param problem 问题 / problem
     * @param solution 解 / solution
    */
    private fun assertManufacturable(problem: Csp2dProblem, solution: Csp2dSolution) {
        assertTrue(solution.usages.isNotEmpty(), "解不应为空 / the solution must not be empty")
        assertTrue(solution.covers(problem), "解必须覆盖全部需求 / the solution must cover every demand")
        assertTrue(solution.sheetCount > 0, "板材张数应为正 / the sheet count must be positive")
        for (usage in solution.usages) {
            assertTrue(
                usage.plan.validate(problem.kerf) is Ok,
                "图案必须通过几何校验 / the pattern must pass the geometry validation"
            )
            val tree = usage.plan.pattern.cutTree
            if (problem.process.requiresCutTree) {
                assertTrue(tree != null, "断头台工艺必须带切分树 / a guillotine process must carry a cut tree")
                assertTrue(
                    tree!!.maxStage() <= problem.process.maxStage,
                    "切分树级数不得超过工艺上限 / the cut tree must stay within the process stage limit"
                )
            } else {
                assertTrue(tree == null, "任意排样不应输出切分树 / arbitrary nesting must not carry a cut tree")
            }
        }
    }

    @Test
    fun sheetFreeform() = runBlocking {
        val p1 = product("P1", 40.0, 30.0)
        val subject = problem(sheet(), listOf(p1 to UInt64(3)), CuttingProcess2.Freeform)
        val solution = Csp2dMilp(solver()).solve(subject).valueOrFail()
        assertManufacturable(subject, solution)
    }

    @Test
    fun sheetGuillotineTwoStage() = runBlocking {
        // 等高件才能组成两级断头台行。 / Only pieces of equal height form a two-stage guillotine row.
        val p1 = product("P1", 40.0, 30.0)
        val p2 = product("P2", 30.0, 30.0)
        val subject = problem(
            sheet(),
            listOf(p1 to UInt64(2), p2 to UInt64(2)),
            CuttingProcess2.GuillotineTwoStage
        )
        val solution = Csp2dMilp(solver()).solve(subject).valueOrFail()
        assertManufacturable(subject, solution)
    }

    @Test
    fun sheetGuillotineThreeStage() = runBlocking {
        // 混高件需要三级断头台。 / Mixed heights need a three-stage guillotine.
        val p1 = product("P1", 60.0, 60.0)
        val p2 = product("P2", 40.0, 30.0)
        val subject = problem(
            sheet(),
            listOf(p1 to UInt64(1), p2 to UInt64(1)),
            CuttingProcess2.GuillotineThreeStage
        )
        val solution = Csp2dMilp(solver()).solve(subject).valueOrFail()
        assertManufacturable(subject, solution)
    }

    @Test
    fun coilFreeformReportsConsumedLength() = runBlocking {
        val p1 = product("P1", 40.0, 30.0)
        val subject = problem(coil(), listOf(p1 to UInt64(3)), CuttingProcess2.Freeform)
        val solution = Csp2dMilp(solver()).solve(subject).valueOrFail()
        assertManufacturable(subject, solution)
        // 卷材模式必须给出消耗段高。 / Coil mode must report the consumed segment height.
        assertTrue(solution.consumedLength.toDouble() > 0.0, "卷材解必须给出消耗段高 / a coil solution reports consumed length")
    }

    @Test
    fun coilGuillotineTwoStage() = runBlocking {
        val p1 = product("P1", 40.0, 30.0)
        val p2 = product("P2", 30.0, 30.0)
        val subject = problem(
            coil(),
            listOf(p1 to UInt64(2), p2 to UInt64(2)),
            CuttingProcess2.GuillotineTwoStage
        )
        val solution = Csp2dMilp(solver()).solve(subject).valueOrFail()
        assertManufacturable(subject, solution)
        assertTrue(solution.consumedLength.toDouble() > 0.0)
    }

    @Test
    fun coilGuillotineThreeStage() = runBlocking {
        val p1 = product("P1", 60.0, 60.0)
        val p2 = product("P2", 40.0, 30.0)
        val subject = problem(
            coil(),
            listOf(p1 to UInt64(1), p2 to UInt64(1)),
            CuttingProcess2.GuillotineThreeStage
        )
        val solution = Csp2dMilp(solver()).solve(subject).valueOrFail()
        assertManufacturable(subject, solution)
        assertTrue(solution.consumedLength.toDouble() > 0.0)
    }

    @Test
    fun columnGenerationPathSolvesAndCovers() = runBlocking {
        val p1 = product("P1", 40.0, 30.0)
        val subject = problem(sheet(), listOf(p1 to UInt64(5)), CuttingProcess2.Freeform)
        val solution = Csp2dColumnGeneration(solver()).solve(subject).valueOrFail()
        assertManufacturable(subject, solution)
        assertTrue(solution.iterations >= 1, "迭代次数应为正 / the iteration count must be positive")
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
