package fuookami.ospf.kotlin.framework.csp1d.application.service

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.framework.csp1d.application.model.Csp1dProblem
import fuookami.ospf.kotlin.framework.csp1d.application.model.Csp1dSolution

/**
 * CSP1D 排程入口（最小实现）/ CSP1D schedule entry point (minimal implementation)
 *
 * @param V 数值类型 / Numeric value type
 */
class Csp1dSchedule<V : RealNumber<V>>(
    private val columnGeneration: Csp1dColumnGeneration<V> = Csp1dColumnGeneration()
) {
    /**
     * 以列生成作为默认排程求解路径 / Use column generation as the default scheduling path
     *
     * @param problem 问题定义 / Problem definition
     * @return 求解结果 / Solution
     */
    fun solve(problem: Csp1dProblem<V>): Csp1dSolution<V> {
        return columnGeneration.solve(problem)
    }
}
