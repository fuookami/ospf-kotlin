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
 * Computes surface density (weight per unit area) for each cargo position and registers it with the model.
 * 计算每个货物位置的表面密度（单位面积重量）并将其注册到模型。
 *
 * @property limitsZones The list of surface density limit zones. / 表面密度限制区域列表
*/
class SurfaceDensity(
    private val aircraftModel: AircraftModel,
    val limitsZones: List<LimitZone>,
    private val positions: List<Position>,
    private val load: Load
) {

    /**
     * A zone with surface density limits.
     * 具有表面密度限制的区域。
     *
     * @property name The name of the limit zone. / 限制区域名称
     * @property locations The set of deck locations in this zone. / 此区域中的甲板位置集合
     * @property frontArm The front arm of the zone. / 区域的前力臂
     * @property backArm The back arm of the zone. / 区域的后力臂
     * @property maxSurfaceDensity The maximum allowed surface density. / 最大允许表面密度
    */
    data class LimitZone(
        val name: String,
        val locations: Set<DeckLocation>,
        val frontArm: Quantity<Flt64>,
        val backArm: Quantity<Flt64>,
        val maxSurfaceDensity: Quantity<Flt64>
    )

    lateinit var surfaceDensity: QuantityLinearIntermediateSymbols1<Flt64>

    /**
     * Registers the surface density symbols with the given model.
     * 将表面密度符号注册到给定模型中。
     *
     * @param model The linear meta model to register with. / 要注册的线性元模型
     * @return Success or failure result. / 成功或失败结果
    */
    fun register(
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        if (!::surfaceDensity.isInitialized) {
            surfaceDensity = QuantityLinearIntermediateSymbols1<Flt64>("surface_density", Shape1(positions.size)) { j, _ ->
                val position = positions[j]
                val coefficient = (Flt64.one / position.shape.area.to(aircraftModel.areaUnit)!!.value)!!
                Quantity(
                    LinearExpressionSymbol(
                        LinearMonomial(coefficient, load.estimateLoadWeight[j].value),
                        name = "surface_density_${position}",
                    ),
                    aircraftModel.surfaceDensityUnit
                )
            }
        }
        for ((j, position) in positions.withIndex()) {
            if (limitsZones.any { position.coordinate.withIntersectionWith(it.frontArm, it.backArm) }) {
                when (val result = model.add(surfaceDensity[j])) {
                    is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

                    is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
                }
            }
        }

        return ok
    }
}
