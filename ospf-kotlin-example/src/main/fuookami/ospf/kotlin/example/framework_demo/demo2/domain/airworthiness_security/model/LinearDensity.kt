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
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * Computes linear density (weight per unit length) for each cargo position and registers it with the model.
 * 计算每个货物位置的线性密度（单位长度重量）并将其注册到模型。
 *
 * @property limitsZones 线性密度限制区域列表 / The list of linear density limit zones.
 * @property limitLines 线性密度限制线列表 / The list of linear density limit lines.
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
     * @property name 限制区域名称 / The name of the limit zone.
     * @property locations 此区域中的甲板位置集合 / The set of deck locations in this zone.
     * @property frontArm 区域的前力臂 / The front arm of the zone.
     * @property backArm 区域的后力臂 / The back arm of the zone.
     * @property maxLinearDensity 最大允许线性密度 / The maximum allowed linear density.
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
     * @property zone 父限制区域 / The parent limit zone.
     * @property arm 此线的力臂位置 / The arm position of this line.
     * @property positions 此线覆盖的位置 / The positions covered by this line.
    */
    data class LimitLine(
        val zone: LimitZone,
        val arm: Quantity<Flt64>,
        val positions: List<Position>
    )

    companion object {
        /**
         * Builds linear-density symbols and cross-section limit lines from aircraft positions.
         * 根据飞机位置构建线密度符号和截面限制线。
         *
         * @param aircraftModel 飞机型号 / The aircraft model.
         * @param limitZones 线密度限制区域 / The linear-density limit zones.
         * @param positions 货物位置 / The cargo positions.
         * @param load 载荷模型 / The load model.
         * @return 构建后的线密度模型 / The constructed linear-density model.
         */
        operator fun invoke(
            aircraftModel: AircraftModel,
            limitZones: List<LimitZone>,
            positions: List<Position>,
            load: Load
        ): LinearDensity {
            val limitLines = limitZones.flatMap { zone ->
                val zonePositions = positions.filter { position ->
                    position.location.location in zone.locations
                        && position.coordinate.withIntersectionWith(zone.frontArm, zone.backArm)
                }

                val arms = buildList {
                    add(zone.frontArm)
                    add(zone.backArm)
                    zonePositions.forEach { position ->
                        add(position.coordinate.frontArm)
                        add(position.coordinate.backArm)
                    }
                }.distinct().filter { arm ->
                    if ((zone.backArm ls zone.frontArm)!!) {
                        (arm geq zone.backArm)!! && (arm leq zone.frontArm)!!
                    } else {
                        (arm geq zone.frontArm)!! && (arm leq zone.backArm)!!
                    }
                }

                arms.mapNotNull { arm ->
                    val linePositions = zonePositions.filter { it.coordinate.on(arm) }
                    if (linePositions.isEmpty()) {
                        null
                    } else {
                        LimitLine(zone = zone, arm = arm, positions = linePositions)
                    }
                }
            }

            return LinearDensity(
                aircraftModel = aircraftModel,
                limitsZones = limitZones,
                limitLines = limitLines,
                positions = positions,
                load = load
            )
        }
    }

    lateinit var linearDensity: QuantityLinearIntermediateSymbols1<Flt64>

    /**
     * Registers the linear density symbols with the given model.
     * 将线性密度符号注册到给定模型中。
     *
     * @param model 要注册的线性元模型 / The linear meta model to register with.
     * @return 成功或失败结果 / Success or failure result.
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
                        coefficient * load.estimateLoadWeight[j].value,
                        name = "linear_density_${position}",
                    ),
                    aircraftModel.linearDensityUnit
                )
            }
        }
        when (val result = model.add(linearDensity)) {
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
