package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.ordinary.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.symbol.function.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.variable.UContinuous
import fuookami.ospf.kotlin.example.exampleThresholdSlack
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Position
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*

private val flt64Converter = object : IntoValue<Flt64> {
        override fun intoValue(value: Flt64) = value
        override val zero get() = Flt64.zero
        override val one get() = Flt64.one
        override fun fromValue(value: Flt64) = value
    }

/** Type alias for the MAC decision model. 中文：MAC 决策模型的类型别名。 */
typealias MACDecision = fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac.model.MAC

/**
 * Models the horizontal stabilizer trim and warning slack for a specific stabilizer configuration.
 * 为特定安定面配置建模水平安定面配平和警告松弛。
 *
 * @property key The stabilizer configuration key / 安定面配置键
 * @property points The trim lookup points for this stabilizer / 此安定面的配平查找点
 * @property limit The trim limits including warning boundaries / 包含警告边界的配平限制
 * @property trim The linear intermediate symbol for the stabilizer trim / 安定面配平的线性中间符号
 * @property warnSlack The linear intermediate symbol for the warning slack / 警告松弛的线性中间符号
*/
class HorizontalStabilizer(
    private val aircraftModel: AircraftModel,
    val key: Key,
    val points: List<Point>,
    val limit: Limit,
    private val totalWeight: TotalWeight,
    private val mac: MACDecision
) {

    /**
     * Key identifying a horizontal stabilizer configuration by angle and thrust derate.
     * 通过角度和推力衰减识别水平安定面配置的键。
     *
     * @property angle The horizontal stabilizer angle / 水平安定面角度
     * @property thrustDrate The thrust derate setting, if applicable / 推力衰减设置（如适用）
    */
    data class Key(
        val angle: HorizontalStabilizerAngle,
        val thrustDrate: HorizontalStabilizerThrustDrate?
    ) {
        override fun toString(): String {
            return angle.toString()
        }
    }

    /**
     * A trim lookup point associating takeoff weight, MAC, and trim value.
     * 关联起飞重量、MAC 和配平值的配平查找点。
     *
     * @property tow The takeoff weight at this point / 此点的起飞重量
     * @property mac The MAC value at this point / 此点的 MAC 值
     * @property trim The trim value at this point / 此点的配平值
    */
    data class Point(
        val tow: Quantity<Flt64>,
        val mac: MAC,
        val trim: Flt64
    )

    /**
     * Trim limits including mandatory and warning boundaries.
     * 包括强制和警告边界的配平限制。
     *
     * @property minTrim The minimum allowed trim value / 最小允许配平值
     * @property maxTrim The maximum allowed trim value / 最大允许配平值
     * @property warnMinTrim The warning-level minimum trim value / 警告级最小配平值
     * @property warnMaxTrim The warning-level maximum trim value / 警告级最大配平值
    */
    data class Limit(
        val minTrim: Flt64?,
        val maxTrim: Flt64?,
        val warnMinTrim: Flt64?,
        val warnMaxTrim: Flt64?
    )

    lateinit var trim: LinearIntermediateSymbol<Flt64>
    lateinit var warnSlack: LinearIntermediateSymbol<Flt64>

    operator fun invoke(tow: Quantity<Flt64>, mac: MAC): Flt64 {
        return points
            .minByOrNull { (it.tow.to(aircraftModel.weightUnit)!!.value - tow.to(aircraftModel.weightUnit)!!.value).abs() }
            ?.trim ?: Flt64.zero
    }

    /**
     * Registers the trim and warning slack symbols into the optimization model.
     * 将配平和警告松弛符号注册到优化模型中。
     *
     * @param stowageMode The stowage mode controlling whether warning slack is registered / 控制是否注册警告松弛的装载模式
     * @param model The linear meta-model to register symbols into / 要注册符号的线性元模型
     * @return [Try] indicating success or failure / 表示成功或失败
    */
    fun register(
        stowageMode: StowageMode,
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        if (!::trim.isInitialized) {
            trim = LinearExpressionSymbol(
                Flt64.zero,
                name = "${key}_trim"
            )
        }
        when (val result = model.add(trim)) {
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
        }

        if (stowageMode.withMacOptimization) {
            if (!::warnSlack.isInitialized) {
                warnSlack = if (limit.warnMinTrim != null && limit.warnMaxTrim != null) {
                    // TODO: add upper bound slack for limit.warnMaxTrim
                    LinearFunctionSymbolAdapter(
                        delegate = SlackFunction(
                            x = LinearPolynomial(trim),
                            y = LinearPolynomial(limit.warnMinTrim),
                            type = UContinuous,
                            withNegative = true,
                            withPositive = true,
                            converter = flt64Converter,
                            name = "${key}_trim_warn_slack"
                        ),
                        converter = flt64Converter
                    )
                } else if (limit.warnMinTrim != null) {
                    exampleThresholdSlack(
                        x = trim,
                        threshold = limit.warnMinTrim,
                        withNegative = true,
                        withPositive = false,
                        name = "${key}_trim_warn_slack"
                    )
                } else if (limit.warnMaxTrim != null) {
                    exampleThresholdSlack(
                        x = trim,
                        threshold = limit.warnMaxTrim,
                        withNegative = false,
                        withPositive = true,
                        name = "${key}_trim_warn_slack"
                    )
                } else {
                    LinearExpressionSymbol(
                        Flt64.zero,
                        name = "${key}_trim_warn_slack"
                    )
                }
            }
            when (val result = model.add(warnSlack)) {
                is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

                is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
            }
        }

        return ok
    }

    /**
     * Returns the list of (MAC, trim) pairs for points matching the given takeoff weight.
     * 返回与给定起飞重量匹配的点的 (MAC, 配平) 对列表。
     *
     * @param tow The takeoff weight to look up / 要查找的起飞重量
     * @return List of (MAC, trim) pairs sorted by MAC / 按 MAC 排序的 (MAC, 配平) 对列表
    */
    private fun pointsOf(tow: Quantity<Flt64>): List<Pair<MAC, Flt64>> {
        val eps = Equal<Flt64, Flt64>(Flt64(1e-5))
        val towValue = tow.to(aircraftModel.weightUnit)!!.value
        val sameTowPoints = points.filter { eps(it.tow.to(aircraftModel.weightUnit)!!.value, towValue) }
        val source = if (sameTowPoints.isNotEmpty()) sameTowPoints else points
        return source.map { it.mac to it.trim }.sortedBy { it.first.toString() }
    }
}

