package fuookami.ospf.kotlin.framework.network_scheduling.application.model

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.model.VrptwSolution

/**
 * VRPTW 求解结果。 / VRPTW solve result.
 *
 * 面向调用方的公开应用模型，与算法内部的 BranchAndPriceResult 解耦。
 * 正常算法终态（Optimal、Feasible、Infeasible、TimeLimit、NodeLimit）通过 Ok 返回；
 * 输入、单位转换、solver 调用或对偶证书失败通过 Failed 返回。 / Public application model for callers, decoupled from the internal BranchAndPriceResult.
 * Normal algorithm terminal states (Optimal, Feasible, Infeasible, TimeLimit, NodeLimit)
 * are returned via Ok; input, unit conversion, solver call, or dual certificate failures
 * are returned via Failed.
 *
 * @property status 求解终止状态 / Solve termination status
 * @property solution 可行解（Infeasible 时为 null） / Feasible solution (null for Infeasible)
 * @property lowerBound 全局下界 / Global lower bound
 * @property upperBound 全局上界（incumbent 成本或 +∞） / Global upper bound (incumbent cost or +∞)
 * @property relativeGap 相对间隙 / Relative gap
 * @property trace 求解过程追踪 / Solve trace
 */
class VrptwSolveResult<V : RealNumber<V>>(
    val status: BranchAndPriceStatus,
    val solution: VrptwSolution<V>?,
    val lowerBound: Flt64,
    val upperBound: Flt64,
    val relativeGap: Flt64,
    val trace: BranchAndPriceTrace
)
