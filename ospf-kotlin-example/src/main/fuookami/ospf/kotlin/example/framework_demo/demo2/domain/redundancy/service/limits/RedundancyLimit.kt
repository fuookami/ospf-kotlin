package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.redundancy.service.limits

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.redundancy.model.*

/**
 * Minimizes the redundancy slack weighted by a coefficient.
 * 最小化按系数加权的冗余松弛。
 *
 * @property redundancy 包含松弛变量的冗余模型 / The redundancy model containing slack variables
 * @property coefficient 目标函数的权重系数函数 / The weight coefficient function for the objective
*/
class RedundancyLimit(
    private val redundancy: Redundancy,
    private val coefficient: () -> Flt64,
    override val name: String = "redundancy"
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        when (val result = model.minimize(
            coefficient() * redundancy.redundancySlack,
            "redundancy"
        )) {
            is Ok -> {}

            is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        return ok
    }
}
