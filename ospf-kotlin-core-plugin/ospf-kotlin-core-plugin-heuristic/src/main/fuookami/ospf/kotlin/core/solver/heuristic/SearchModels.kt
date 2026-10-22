package fuookami.ospf.kotlin.core.solver.heuristic

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface

/** 替换目标语义，变量与约束仍来自原模型 / Replace objective semantics while preserving variables and constraints.
 * @param source 原始模型 / Source model
 * @param defaultObjective 默认目标 / Default objective
 * @param objective 目标提取 / Objective extraction
 * @param compare 目标比较 / Objective comparison
 * @return 目标视图 / Objective view
 */
fun <O, V> objectiveModel(
    source: AbstractCallBackModelInterface<*, *, V>,
    defaultObjective: O,
    objective: (Solution<V>) -> O?,
    compare: (O, O) -> Order?
): AbstractCallBackModelInterface<Unit, O, V> where V : RealNumber<V>, V : NumberField<V> {
    val evaluate = objective
    return object : AbstractCallBackModelInterface<Unit, O, V>, Model<V> by source {
        override val defaultObjective = defaultObjective
        override val tokens = source.tokens
        override val constraints = source.constraints
        override val objectiveFunctions = emptyList<Pair<Extractor<Unit?, Solution<V>>, String>>()
        override fun initialSolutions(initialSolutionAmount: fuookami.ospf.kotlin.math.algebra.number.UInt64) = source.initialSolutions(initialSolutionAmount)
        override fun operation(lhs: O, rhs: O): O = lhs
        override fun objectiveValue(): O = defaultObjective
        override fun objectiveValue(obj: Unit): O = defaultObjective
        override fun objective(solution: Solution<V>): O? = evaluate(solution)
        override fun compareObjective(lhs: O, rhs: O): Order? = compare(lhs, rhs)
        override fun constraintSatisfied(solution: Solution<V>): Boolean? = source.constraintSatisfied(solution)
        override fun flush() = source.flush()
    }
}
