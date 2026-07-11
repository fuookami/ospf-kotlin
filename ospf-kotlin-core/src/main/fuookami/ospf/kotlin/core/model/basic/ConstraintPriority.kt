/**
 * 约束优先级统计
 * Constraint priority statistics
*/
package fuookami.ospf.kotlin.core.model.basic

import fuookami.ospf.kotlin.core.model.intermediate.*

/**
 * 统计线性三元模型视图中非空约束优先级的数量。
 * Count the number of non-null constraint priorities in the linear triad model view.
 *
 * @return 非空优先级的数量 / The count of non-null priorities
*/
fun LinearTriadModelView.nonNullConstraintPriorityAmount(): Int {
    return constraints.priorities.count { it != null }
}

/**
 * 统计二次四元模型视图中非空约束优先级的数量。
 * Count the number of non-null constraint priorities in the quadratic tetrad model view.
 *
 * @return 非空优先级的数量 / The count of non-null priorities
*/
fun QuadraticTetradModelView.nonNullConstraintPriorityAmount(): Int {
    return constraints.priorities.count { it != null }
}
