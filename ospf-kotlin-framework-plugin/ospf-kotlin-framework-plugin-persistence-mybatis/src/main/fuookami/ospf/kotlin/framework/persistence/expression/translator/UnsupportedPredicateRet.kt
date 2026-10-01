package fuookami.ospf.kotlin.framework.persistence.expression.translator

import fuookami.ospf.kotlin.framework.persistence.expression.UnsupportedPredicateDetail
import fuookami.ospf.kotlin.framework.persistence.expression.UnsupportedPredicatePolicy
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Ret

internal fun Ret<*>.isAlwaysFalseUnsupported(): Boolean {
    val failure = this as? Failed<*, *, *> ?: return false
    return (failure.error.value as? UnsupportedPredicateDetail)?.policy == UnsupportedPredicatePolicy.AlwaysFalse
}
