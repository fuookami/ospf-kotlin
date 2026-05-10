package fuookami.ospf.kotlin.core.model.basic

import fuookami.ospf.kotlin.core.model.intermediate.LinearTriadModelView
import fuookami.ospf.kotlin.core.model.intermediate.QuadraticTetradModelView

fun LinearTriadModelView.nonNullConstraintPriorityAmount(): Int {
    return constraints.priorities.count { it != null }
}

fun QuadraticTetradModelView.nonNullConstraintPriorityAmount(): Int {
    return constraints.priorities.count { it != null }
}
