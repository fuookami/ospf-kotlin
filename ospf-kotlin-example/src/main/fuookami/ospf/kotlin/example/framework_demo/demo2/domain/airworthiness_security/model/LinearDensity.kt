package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.model

import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Position
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * Computes linear density (weight per unit length) for each cargo position and registers it with the model.
 * 计算每个货物位置的线性密度（单位长度重量）并将其注册到模型。
 *
 * @property limitsZones The list of linear density limit zones. / 线性密度限制区域列表
 * @property limitLines The list of linear density limit lines. / 线性密度限制线列表
*/
class LinearDensity(
    private val aircraftModel: AircraftModel,
    val limitsZones: List<LimitZone>,
    val limitLines: List<LimitLine>,
    private val positions: List<Position>,
    private val load: Load
) {

    /**
     * A zone with linear density limits.
     * 具有线性密度限制的区域。
     *
     * @property name The name of the limit zone. / 限制区域名称
     * @property locations The set of deck locations in this zone. / 此区域中的甲板位置集合
     * @property frontArm The front arm of the zone. / 区域的前力臂
     * @property backArm The back arm of the zone. / 区域的后力臂
     * @property maxLinearDensity The maximum allowed linear density. / 最大允许线性密度
    */
    data class LimitZone(
        val name: String,
        val locations: Set<DeckLocation>,
        val frontArm: Quantity<Flt64>,
        val backArm: Quantity<Flt64>,
        val maxLinearDensity: Quantity<Flt64>
    )

    /**
     * A limit line within a linear density zone.
     * 线性密度区域内的限制线。
     *
     * @property zone The parent limit zone. / 父限制区域
     * @property arm The arm position of this line. / 此线的力臂位置
     * @property positions The positions covered by this line. / 此线覆盖的位置
    */
    data class LimitLine(
        val zone: LimitZone,
        val arm: Quantity<Flt64>,
        val positions: List<Position>
    )

    companion object {
        operator fun invoke(
            aircraftModel: AircraftModel,
            limitZones: List<LimitZone>,
            positions: List<Position>,
            load: Load
        ): LinearDensity {
            TODO("not implemented yet")
        }
    }

    lateinit var linearDensity: QuantityLinearIntermediateSymbols1<Flt64>

    /**
     * Registers the linear density symbols with the given model.
     * 将线性密度符号注册到给定模型中。
     *
     * @param model The linear meta model to register with. / 要注册的线性元模型
     * @return Success or failure result. / 成功或失败结果
    */
    fun register(
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        if (!::linearDensity.isInitialized) {
            linearDensity = QuantityLinearIntermediateSymbols1<Flt64>("linear_density", Shape1(positions.size)) { j, _ ->
                val position = positions[j]
                val coefficient = (Flt64.one / position.shape.length.to(aircraftModel.lengthUnit)!!.value)!!
                Quantity(
                    LinearExpressionSymbol(
                        LinearMonomial(coefficient, load.estimateLoadWeight[j].value),
                        name = "linear_density_${position}",
                    ),
                    aircraftModel.linearDensityUnit
                )
            }
        }
        when (val result = model.add(linearDensity)) {
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
