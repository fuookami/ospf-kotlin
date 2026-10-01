/**
 * 检查结果是否携带 AlwaysFalse 不支持谓词标记 / Check whether a result carries an AlwaysFalse unsupported-predicate marker.
 */
package fuookami.ospf.kotlin.framework.persistence.expression.translator

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.framework.persistence.expression.UnsupportedPredicateDetail
import fuookami.ospf.kotlin.framework.persistence.expression.UnsupportedPredicatePolicy

internal fun Ret<*>.isAlwaysFalseUnsupported(): Boolean {
    val failure = this as? Failed<*, *, *> ?: return false
    return (failure.error.value as? UnsupportedPredicateDetail)?.policy == UnsupportedPredicatePolicy.AlwaysFalse
}
