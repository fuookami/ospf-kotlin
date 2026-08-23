package fuookami.ospf.kotlin.framework.network_scheduling.domain.route_compilation.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.value_range.Interval
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.route_compilation.shadow.*
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.networkSchedulingFailure

/**
 * Phase-I 人工覆盖变量。 / Phase-I artificial coverage variables.
 *
 * 为每个客户维护一个非负人工变量 a[i]，使 sum(visit[r, i] * x[r]) + a[i] = 1。
 * Phase I 最小化 sum(a[i])；Phase II 将所有 a[i] 固定为 0。
 *
 * Maintains a non-negative artificial variable a[i] for each customer, so that
 * sum(visit[r, i] * x[r]) + a[i] = 1. Phase I minimizes sum(a[i]);
 * Phase II fixes all a[i] to 0.
 */
class ArtificialCoverage(
    private val customers: List<Customer<*>>
) {
    /** 人工变量 / Artificial variables */
    lateinit var a: URealVariable1

    /** 人工覆盖中间符号 / Artificial coverage intermediate symbols */
    lateinit var artificialCoverage: LinearIntermediateSymbols1<Flt64>

    /** 人工成本中间符号 / Artificial cost intermediate symbol */
    lateinit var artificialCost: LinearIntermediateSymbol<Flt64>

    /** 是否已注册 / Whether registered */
    private var registered = false

    /** 是否已切换到 Phase II / Whether switched to Phase II */
    private var phaseTwo = false

    /**
     * 注册到模型。 / Register to model.
     *
     * @param model 元模型 / Meta model
     * @return 操作结果 / Operation result
     */
    fun register(model: MetaModel<Flt64>): Try {
        if (registered) return ok

        a = URealVariable1("a", Shape1(customers.size))
        for ((index, customer) in customers.withIndex()) {
            a[index].name = "a_${customer.id.value}"
        }
        when (val result = model.add(a)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        artificialCoverage = LinearIntermediateSymbols1<Flt64>(
            name = "artificial_coverage",
            shape = Shape1(customers.size)
        ) { index, _ ->
            LinearExpressionSymbol(
                a[index],
                Flt64,
                name = "artificial_coverage_${customers[index].id.value}"
            )
        }
        when (val result = model.add(artificialCoverage)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        artificialCost = LinearExpressionSymbol(
            Flt64,
            name = "artificial_cost"
        )
        for ((index, _) in customers.withIndex()) {
            (artificialCost as LinearExpressionSymbol<Flt64>).asMutable() +=
                LinearPolynomial(a[index])
        }
        when (val result = model.add(artificialCost)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        registered = true
        return ok
    }

    /**
     * 切换到 Phase II：将所有人工变量固定为 0。 / Switch to Phase II: fix all artificial variables to 0.
     *
     * @param model 元模型 / Meta model
     * @return 操作结果 / Operation result
     */
    fun switchToPhaseTwo(model: AbstractLinearMetaModel<Flt64>): Try {
        if (phaseTwo) return ok
        val zeroRange = ValueRange(Flt64.zero, Flt64.zero, Interval.Closed, Interval.Closed, Flt64).value
            ?: return networkSchedulingFailure(
                "切换到 Phase II 失败：无法创建零值域 / Failed to switch to Phase II: cannot create zero value range"
            )
        for ((index, _) in customers.withIndex()) {
            a[index].range.set(zeroRange)
        }
        phaseTwo = true
        return ok
    }

    /**
     * 检查 Phase I 是否收敛。 / Check if Phase I has converged.
     *
     * @param model 元模型 / Meta model
     * @param feasibilityTolerance 可行性容差 / Feasibility tolerance
     * @return 人工变量总和是否在容差内 / Whether sum of artificial variables is within tolerance
     */
    fun isPhaseOneConverged(model: AbstractLinearMetaModel<Flt64>, feasibilityTolerance: Flt64): Ret<Boolean> {
        var sum = Flt64.zero
        for (token in model.tokens.tokens) {
            if (token.belongsTo(a)) {
                val value = token.result ?: return networkSchedulingFailure(
                    "Phase I 收敛检查失败：人工变量 ${token.name} 无解值 / " +
                            "Phase I convergence check failed: artificial variable ${token.name} has no solution value"
                )
                sum += value
            }
        }
        return Ok(sum leq feasibilityTolerance)
    }

    /** 是否处于 Phase II / Whether in Phase II */
    val isPhaseTwo: Boolean get() = phaseTwo
}
