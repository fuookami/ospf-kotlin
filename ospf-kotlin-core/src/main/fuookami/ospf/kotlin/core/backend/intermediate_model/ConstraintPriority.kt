package fuookami.ospf.kotlin.core.backend.intermediate_model

fun LinearTriadModelView.nonNullConstraintPriorityAmount(): Int {
    return constraints.priorities.count { it != null }
}

fun QuadraticTetradModelView.nonNullConstraintPriorityAmount(): Int {
    return constraints.priorities.count { it != null }
}

