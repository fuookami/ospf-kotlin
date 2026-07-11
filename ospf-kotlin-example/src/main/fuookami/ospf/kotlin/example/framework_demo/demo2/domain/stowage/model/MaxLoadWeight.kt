package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*

/**
 * Maximum load weight model for each position, supporting both constant
 * and piecewise-linear maximum load weight limits based on zero-fuel weight.
 * 每个位置的最大装载重量模型，支持基于零油重的常量和分段线性最大装载重量限制。
*/
class MaxLoadWeight(
    private val aircraftModel: AircraftModel,
    private val fuselage: Fuselage,
    private val items: List<Item>,
    private val positions: List<Position>,
    private val totalWeight: TotalWeight
) {
    lateinit var maxLoadWeight: QuantityLinearIntermediateSymbols1<Flt64>

    /**
     * Registers the maximum load weight symbols into the model.
     * 将最大装载重量符号注册到模型中。
     *
     * @param model the linear meta-model to register into / 要注册到的线性元模型
     * @return success or failure / 成功或失败
    */
    fun register(
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        if (!::maxLoadWeight.isInitialized) {
            maxLoadWeight = QuantityLinearIntermediateSymbols1<Flt64>("max_load_weight", Shape1(positions.size)) { j, _ ->
                val position = positions[j]
                val symbol: LinearIntermediateSymbol<Flt64> = if (position.mlw.segments.isNotEmpty()) {
                    val zfw = totalWeight.computedTotalWeight[FlightPhase.ZeroFuel]
                    if (zfw != null) {
                        LinearExpressionSymbol(
                            LinearPolynomial(position.mlw(zfw).to(aircraftModel.weightUnit)!!.value),
                            name = "max_load_weight_${position}"
                        )
                    } else {
                        LinearExpressionSymbol(
                            LinearPolynomial(position.mlw.mlw.to(aircraftModel.weightUnit)!!.value),
                            name = "max_load_weight_${position}",
                        )
                    }
                } else {
                    LinearExpressionSymbol(
                        LinearPolynomial(position.mlw.mlw.to(aircraftModel.weightUnit)!!.value),
                        name = "max_load_weight_${position}"
                    )
                }
                Quantity(
                    symbol,
                    aircraftModel.weightUnit
                )
            }
        }
        when (val result = model.add(maxLoadWeight)) {
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}
            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> return Failed(result.error)
            is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> return Fatal(result.errors)
        }

        return ok
    }
}