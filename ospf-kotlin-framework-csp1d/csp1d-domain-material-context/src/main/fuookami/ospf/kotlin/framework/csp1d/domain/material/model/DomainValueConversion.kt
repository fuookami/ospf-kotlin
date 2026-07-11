package fuookami.ospf.kotlin.framework.csp1d.domain.material.model

import fuookami.ospf.kotlin.framework.csp1d.domain.material.error.Csp1dCapabilityError
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 转换 solver 值到领域数值类型 / Convert solver value to domain numeric type
 *
 * @param sample 领域数值样本 / Domain value sample
 * @param value solver 边界值 / Solver boundary value
 * @return 与 sample 同类型的领域数值 / Domain value with the same numeric type as sample
*/
@Suppress("UNCHECKED_CAST")
fun <V : RealNumber<V>> convertSolverValue(sample: V, value: Flt64): Ret<V> {
    return when (sample) {
        is Flt64 -> Ok(value as V)
        is FltX -> Ok(value.toFltX() as V)
        else -> Failed(Csp1dCapabilityError("RealNumber type: ${sample::class}"))
    }
}
