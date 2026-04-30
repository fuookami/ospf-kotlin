package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac.model

import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.intermediate_symbol.*
import fuookami.ospf.kotlin.core.intermediate_symbol.function.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*

class MAC(
    private val aircraftModel: AircraftModel,
    private val formula: Formula,
    private val totalWeight: TotalWeight,
    private val torque: Torque
) {
    lateinit var mac: LinearIntermediateSymbolF64

    fun register(
        model: AbstractLinearMetaModelF64
    ): Try {
        if (!::mac.isInitialized) {
            val tow = totalWeight.computedTotalWeight[FlightPhase.TakeOff]
            mac = if (tow != null) {
                // 预配载、全配载模式下，起飞总重是个确定值，所以重心与起飞指数是线性相关，可以通过重心指数计算公式计算得到重心的表达式
                val index = torque.index[FlightPhase.TakeOff]!!
                LinearExpressionSymbol(
                    formula.mac(
                        Quantity(
                            LinearPolynomial(
                                monomials = listOf(LinearMonomial(Flt64.one, index.value)),
                                constant = Flt64.zero
                            ),
                            index.unit
                        ),
                        tow
                    ),
                    name = "mac"
                )
            } else {
                // TODO: 恢复基于分段线性函数的重心估计
                LinearExpressionSymbol(
                    Flt64.zero,
                    name = "mac"
                )
            }
        }
        when (val result = model.add(mac)) {
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











