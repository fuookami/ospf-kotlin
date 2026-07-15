package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.redundancy.model

import fuookami.ospf.kotlin.utils.functional.*
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
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Position

/**
 * Experimental longitudinal balance model for analyzing main-deck torque distribution.
 * 用于分析主甲板扭矩分布的实验纵向平衡模型。
 *
 * @property aircraftModel The aircraft model reference / 飞机模型引用
 * @property positions The list of stowage positions / 装载位置列表
 * @property load The load distribution data / 载荷分布数据
 * @property payload The payload data / 载荷数据
 * @property redundancy The redundancy model reference / 冗余模型引用
 * @property mainActualLongitudinalTorque The main deck actual longitudinal torque symbol / 主甲板实际纵向扭矩符号
 * @property predicateLongitudinalTorque The predicate longitudinal torque symbol / 预测纵向扭矩符号
 * @property longitudinalTorqueSlack The longitudinal torque slack variable / 纵向扭矩松弛变量
 * @property minLongitudinalTorque The minimum longitudinal torque bound / 最小纵向扭矩边界
 * @property maxLongitudinalTorque The maximum longitudinal torque bound / 最大纵向扭矩边界
*/
class ExperimentalLongitudinalBalance(
    private val aircraftModel: AircraftModel,
    private val positions: List<Position>,
    private val load: Load,
    private val payload: Payload,
    private val redundancy: Redundancy
) {
    lateinit var mainActualLongitudinalTorque: QuantityLinearIntermediateSymbol<Flt64>
    lateinit var predicateLongitudinalTorque: QuantityLinearIntermediateSymbol<Flt64>
    lateinit var longitudinalTorqueSlack: QuantityLinearIntermediateSymbol<Flt64>

    val minLongitudinalTorque: QuantityLinearIntermediateSymbol<Flt64> by lazy {
        TODO("not implemented yet")
    }

    val maxLongitudinalTorque: QuantityLinearIntermediateSymbol<Flt64> by lazy {
        TODO("not implemented yet")
    }

    /**
     * Registers longitudinal balance intermediate symbols into the optimization model.
     * 将纵向平衡中间符号注册到优化模型中。
     *
     * @param model The linear meta model to register into / 要注册到的线性元模型
     * @return Success or failure result / 成功或失败结果
    */
    fun register(model: AbstractLinearMetaModel<Flt64>): Try {
        if (!::mainActualLongitudinalTorque.isInitialized) {
            val poly = MutableLinearPolynomial()
            for ((j, position) in positions.withIndex()) {
                if (position.location.main) {
                    poly += LinearMonomial(
                        Flt64.one,
                        load.loadActualLongitudinalTorque[j].to(aircraftModel.torqueUnit)!!.value
                    )
                }
            }
            mainActualLongitudinalTorque = Quantity(
                LinearExpressionSymbol(
                    poly,
                    name = "main_actual_longitudinal_torque"
                ),
                aircraftModel.torqueUnit
            )
        }
        when (val result = model.add(mainActualLongitudinalTorque)) {
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                return Failed(result.error)
            }

            is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                return Fatal(result.errors)
            }
        }

        if (!::predicateLongitudinalTorque.isInitialized) {
            TODO("not implemented yet")
        }
        when (val result = model.add(predicateLongitudinalTorque)) {
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                return Failed(result.error)
            }

            is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                return Fatal(result.errors)
            }
        }

        if (!::longitudinalTorqueSlack.isInitialized) {
            longitudinalTorqueSlack = Quantity(
                LinearExpressionSymbol(Flt64, name = "longitudinal_torque_slack"),
                aircraftModel.torqueUnit
            )
        }
        when (val result = model.add(longitudinalTorqueSlack)) {
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
