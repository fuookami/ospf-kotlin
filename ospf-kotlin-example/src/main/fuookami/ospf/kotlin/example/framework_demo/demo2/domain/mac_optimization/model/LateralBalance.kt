package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac_optimization.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.symbol.function.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac.model.*

/**
 * Lateral balance slack variable for wide-body aircraft torque optimization.
 * 宽体飞机扭矩优化的横向平衡松弛变量。
 *
 * @property slack The linear intermediate symbol representing lateral balance slack / 表示横向平衡松弛的线性中间符号
*/
class LateralBalance(
    private val aircraftModel: AircraftModel,
    private val torque: Torque
) {
    lateinit var slack: QuantityLinearIntermediateSymbol<Flt64>

    /**
     * Registers the lateral balance slack symbol into the optimization model.
     * 将横向平衡松弛符号注册到优化模型中。
     *
     * @param model The linear meta-model to register the slack into / 要注册松弛符号的线性元模型
     * @return [Try] indicating success or failure / 表示成功或失败
    */
    fun register(
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        if (!::slack.isInitialized) {
            slack = Quantity(
                LinearExpressionSymbol(Flt64, name = "lateral_balance_slack"),
                aircraftModel.torqueUnit
            )
        }
        when (val result = model.add(slack)) {
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                return Failed(result.error)
            }

            is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                return Fatal(result.errors)
            }
        }

        return ok
    }
}
