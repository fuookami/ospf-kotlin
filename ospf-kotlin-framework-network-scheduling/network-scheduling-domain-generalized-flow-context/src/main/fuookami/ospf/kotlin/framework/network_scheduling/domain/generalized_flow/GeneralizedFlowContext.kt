package fuookami.ospf.kotlin.framework.network_scheduling.domain.generalized_flow

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.generalized_flow.model.*

/** 面向模型装配和业务结果提取的广义流上下文。 / Generalized-flow context for model assembly and domain-result extraction.
 *
 * @param V 领域数值类型 / Domain numeric type
 * @property problem 不可变广义流问题 / Immutable generalized-flow problem
 * @property adapter solver 数值转换边界 / Solver numeric-conversion boundary
 * @property name 模型命名空间 / Model namespace
 * @property solverTolerance solver 单位可行性容差 / Solver-unit feasibility tolerance
 * @property aggregation 每模型独立聚合与注册态 / Per-model aggregate and registration state
 * @property cost 已注册总费用表达式 / Registered total-cost expression
 */
class GeneralizedFlowContext<V : RealNumber<V>>(
    val problem: GeneralizedFlowProblem<V>,
    val adapter: NetworkSchedulingSolverValueAdapter<V>,
    val name: String = "generalized_flow",
    extraPipelineFactories: List<GeneralizedFlowPipelineFactory<V>> = emptyList(),
    val solverTolerance: GeneralizedFlowSolverTolerance = GeneralizedFlowSolverTolerance()
) {
    val aggregation = GeneralizedFlowAggregation(
        problem = problem,
        adapter = adapter,
        name = name,
        extensionFactories = extraPipelineFactories,
        solverTolerance = solverTolerance
    )

    /** 已注册的总费用表达式。 / Registered total-cost expression. */
    val cost: LinearPolynomial<Flt64> get() = aggregation.cost

    /** 注册广义流模型。 / Register the generalized-flow model.
     *
     * @param model 元模型 / Meta model
     * @param minimizeCost 是否注册内置费用目标 / Whether to register the built-in cost objective
     * @return 注册结果 / Registration result
     */
    fun register(
        model: AbstractLinearMetaModel<Flt64>,
        minimizeCost: Boolean = true
    ): Try = aggregation.register(model, minimizeCost)

    /** 获取弧投入表达式。 / Get an arc input expression.
     *
     * @param arcId 稳定弧标识 / Stable arc identity
     * @return 起点单位投入表达式 / Origin-unit input expression
     */
    fun input(arcId: NetworkArcId): Ret<LinearPolynomial<Flt64>> = aggregation.input(arcId)

    /** 获取弧到达产出表达式。 / Get an arc arrival-output expression.
     *
     * @param arcId 稳定弧标识 / Stable arc identity
     * @return 终点单位产出表达式 / Destination-unit output expression
     */
    fun output(arcId: NetworkArcId): Ret<LinearPolynomial<Flt64>> = aggregation.output(arcId)

    /** 提取领域结果与节点守恒核验。 / Extract domain results and node-balance checks.
     *
     * @param model 已注册并回填求解结果的模型 / Registered model populated with a solver result
     * @return 广义流领域解 / Generalized-flow domain solution
     */
    fun extractSolution(model: AbstractLinearMetaModel<Flt64>): Ret<GeneralizedFlowSolution<V>> = aggregation.extractSolution(model)
}
