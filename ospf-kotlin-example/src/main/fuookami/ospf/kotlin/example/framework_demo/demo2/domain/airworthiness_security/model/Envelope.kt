package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.model

import java.util.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.geometry.*
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
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Position

/**
 * Interface for CG envelope constraints that define min/max index bounds per flight phase.
 * 定义每个飞行阶段最小/最大指数边界的 CG 包络线约束接口。
*/
interface AbstractEnvelope {

    /**
     * A point on the envelope.
     * 包络线上的点。
     *
     * @property totalWeight 此点的总重量 / Total weight at this point.
     * @property index 此点的 CG 指数 / CG index at this point.
    */
    data class Point(
        val totalWeight: Quantity<Flt64>,
        val index: Quantity<Flt64>
    )

    /** The side type of the envelope (left or right). / 包络线的侧边类型（左侧或右侧）。 */
    enum class SideType {
        Left,
        Right
    }

    /**
     * A side of the envelope.
     * 包络线的一侧。
     *
     * @property name 此侧边的名称 / The name of this side.
     * @property type 侧边类型（左侧或右侧） / The side type (left or right).
     * @property points 定义此侧边的点列表 / The list of points defining this side.
    */
    class Side(
        private val aircraftModel: AircraftModel,
        val name: String,
        val type: SideType,
        val points: List<Point>
    ) {
        /**
         * Returns the piecewise linear CG index bound as a quantity symbol for the given total weight.
         * 返回给定总重量的分段线性 CG 指数边界作为量符号。
         *
         * @param totalWeight 要评估包络线边界的总重量符号 / The total weight symbol to evaluate the envelope bound at
         * @return CG 指数边界或单位转换错误 / The CG index bound or a unit-conversion error
         */
        fun piecewise(totalWeight: QuantityLinearIntermediateSymbol<Flt64>): Ret<QuantityLinearIntermediateSymbol<Flt64>> {
            val function = when (val result = UnivariateLinearPiecewiseFunction.fromPointsResult(
                x = totalWeight.value.toLinearPolynomial(),
                points = points.map {
                    val pointWeight = it.totalWeight.to(aircraftModel.weightUnit)
                        ?: return Failed(
                            ErrorCode.IllegalArgument,
                            "包络 ${name} 的点重量单位不兼容 / Envelope ${name} has an incompatible point-weight unit"
                        )
                    val pointIndex = it.index.to(aircraftModel.torqueUnit)
                        ?: return Failed(
                            ErrorCode.IllegalArgument,
                            "包络 ${name} 的点力矩单位不兼容 / Envelope ${name} has an incompatible point-moment unit"
                        )
                    point2(
                        pointWeight.value,
                        pointIndex.value
                    )
                },
                converter = IntoValue.Identity,
                name = "${name}_${type.name.lowercase(Locale.getDefault())}"
            )) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            return Ok(Quantity(
                LinearFunctionSymbolAdapter(
                    delegate = function,
                    converter = IntoValue.Identity
                ),
                aircraftModel.torqueUnit
            ))
        }

        operator fun invoke(totalWeight: Quantity<Flt64>): Quantity<Flt64> {
            val firstPoint = points.firstOrNull()
                ?: return Quantity(Flt64.zero, aircraftModel.torqueUnit)

            // Preserve an incompatible unit in the fallback result so callers can report it.
            // 保留不兼容单位到回退结果中，以便调用方能够报告单位错误。
            val invalidIndex = points.firstOrNull { it.index.to(aircraftModel.torqueUnit) == null }
            if (invalidIndex != null) {
                return invalidIndex.index
            }
            val invalidWeight = points.firstOrNull { it.totalWeight.to(aircraftModel.weightUnit) == null }
            if (invalidWeight != null) {
                return invalidWeight.index
            }

            val targetWeight = totalWeight.to(aircraftModel.weightUnit)?.value
                ?: return firstPoint.index
            val sortedPoints = points.map { point ->
                point.totalWeight.to(aircraftModel.weightUnit)!!.value to
                    point.index.to(aircraftModel.torqueUnit)!!.value
            }.sortedBy { it.first.toDouble() }

            val result = when {
                targetWeight leq sortedPoints.first().first -> sortedPoints.first().second
                targetWeight geq sortedPoints.last().first -> sortedPoints.last().second
                else -> {
                    val upper = sortedPoints.first { targetWeight leq it.first }
                    val upperIndex = sortedPoints.indexOf(upper)
                    val lower = sortedPoints[upperIndex - 1]
                    val deltaWeight = upper.first - lower.first
                    if (deltaWeight eq Flt64.zero) {
                        upper.second
                    } else {
                        lower.second + (upper.second - lower.second) *
                            ((targetWeight - lower.first) / deltaWeight)
                    }
                }
            }
            return Quantity(result, aircraftModel.torqueUnit)
        }
    }

    val phase: FlightPhase
    val name: String
    val minIndex: QuantityLinearIntermediateSymbol<Flt64>
    val maxIndex: QuantityLinearIntermediateSymbol<Flt64>

/**
 * Registers the envelope's min and max index symbols into the linear meta model.
 * 将包络线的最小和最大指数符号注册到线性元模型中。
 * @param model 要注册符号的线性元模型 / The linear meta model to register symbols into
 * @return 注册操作的成功或失败 / Success or failure of the registration
*/
    fun register(model: AbstractLinearMetaModel<Flt64>): Try
}

/**
 * Standard CG envelope with left and right sides defining min/max index bounds.
 * 具有定义最小/最大指数边界的左右两侧的标准 CG 包络线。
 *
 * @property lhsSide 包络线的左侧 / The left-hand side of the envelope.
 * @property rhsSide 包络线的右侧 / The right-hand side of the envelope.
*/
class Envelope(
    private val aircraftModel: AircraftModel,
    override val phase: FlightPhase,
    override val name: String,
    val lhsSide: AbstractEnvelope.Side,
    val rhsSide: AbstractEnvelope.Side,
    private val totalWeight: TotalWeight
) : AbstractEnvelope {

    /**
     * Returns the minimum CG index for the given total weight.
     * 返回给定总重量的最小 CG 指数。
     *
     * @param totalWeight 要评估的总重量 / The total weight to evaluate.
     * @return 最小 CG 指数量 / The minimum CG index quantity.
    */
    fun minIndexOf(totalWeight: Quantity<Flt64>): Quantity<Flt64> {
        return lhsSide(totalWeight)
    }

    /**
     * Returns the maximum CG index for the given total weight.
     * 返回给定总重量的最大 CG 指数。
     *
     * @param totalWeight 要评估的总重量 / The total weight to evaluate.
     * @return 最大 CG 指数量 / The maximum CG index quantity.
    */
    fun maxIndexOf(totalWeight: Quantity<Flt64>): Quantity<Flt64> {
        return rhsSide(totalWeight)
    }

    override lateinit var minIndex: QuantityLinearIntermediateSymbol<Flt64>
    override lateinit var maxIndex: QuantityLinearIntermediateSymbol<Flt64>

    private fun invalidIndexUnit(): Try {
        return Failed(
            ErrorCode.IllegalArgument,
            "包络 ${name} 的纵向力矩单位不兼容 / Envelope ${name} has incompatible longitudinal-moment units"
        )
    }

    private fun piecewiseSide(side: AbstractEnvelope.Side): Ret<QuantityLinearIntermediateSymbol<Flt64>> {
        val estimate = totalWeight.estimateTotalWeight[phase]
            ?: return Failed(
                ErrorCode.IllegalArgument,
                "包络 ${name} 缺少 ${phase.name} 的总重估计 / Envelope ${name} is missing an estimated total weight for ${phase.name}"
            )
        return side.piecewise(estimate)
    }

    override fun register(
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        if (!::minIndex.isInitialized) {
            val thisTotalWeight = totalWeight.computedTotalWeight[phase]
            minIndex = if (thisTotalWeight != null) {
                val index = minIndexOf(thisTotalWeight).to(aircraftModel.torqueUnit)
                    ?: return invalidIndexUnit()
                Quantity(
                    LinearExpressionSymbol(
                        index.value,
                        name = "min_index_${name}_${phase.name.lowercase(Locale.getDefault())}"
                    ),
                    aircraftModel.torqueUnit
                )
            } else {
                when (val result = piecewiseSide(lhsSide)) {
                    is Ok -> result.value ?: return Failed(
                        ErrorCode.ApplicationError,
                        "包络 ${name} 的左侧分段边界为空 / Envelope ${name} returned an empty left piecewise bound"
                    )
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            }
        }
        when (val result = model.add(minIndex)) {
            is Ok -> {}

            is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        if (!::maxIndex.isInitialized) {
            val thisTotalWeight = totalWeight.computedTotalWeight[phase]
            maxIndex = if (thisTotalWeight != null) {
                val index = maxIndexOf(thisTotalWeight).to(aircraftModel.torqueUnit)
                    ?: return invalidIndexUnit()
                Quantity(
                    LinearExpressionSymbol(
                        index.value,
                        name = "max_index_${name}_${phase.name.lowercase(Locale.getDefault())}"
                    ),
                    aircraftModel.torqueUnit
                )
            } else {
                when (val result = piecewiseSide(rhsSide)) {
                    is Ok -> result.value ?: return Failed(
                        ErrorCode.ApplicationError,
                        "包络 ${name} 的右侧分段边界为空 / Envelope ${name} returned an empty right piecewise bound"
                    )
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            }
        }
        when (val result = model.add(maxIndex)) {
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

/**
 * CG envelope with conditional sides that switch based on a runtime condition.
 * 具有基于运行时条件切换的条件边的 CG 包络线。
 *
 * @property lhsSide1 包络线的第一个左侧 / The first left-hand side of the envelope.
 * @property rhsSide1 包络线的第一个右侧 / The first right-hand side of the envelope.
 * @property lhsSide2 包络线的第二个左侧 / The second left-hand side of the envelope.
 * @property rhsSide2 包络线的第二个右侧 / The second right-hand side of the envelope.
 * @property valueCondition 运行时条件函数，返回 true、false 或 null / Runtime condition function returning true, false, or null.
 * @property symbolCondition 模型的符号条件函数 / Symbolic condition function for the model.
*/
class ConditionalEnvelope(
    private val aircraftModel: AircraftModel,
    override val phase: FlightPhase,
    override val name: String,
    val lhsSide1: AbstractEnvelope.Side,
    val rhsSide1: AbstractEnvelope.Side,
    val lhsSide2: AbstractEnvelope.Side,
    val rhsSide2: AbstractEnvelope.Side,
    val valueCondition: () -> Boolean?,
    val symbolCondition: (String) -> Either<LinearPolynomial<Flt64>, LinearFunctionSymbolAdapter<Flt64>>,
    private val totalWeight: TotalWeight
) : AbstractEnvelope {
    lateinit var condition: LinearIntermediateSymbol<Flt64>
    override lateinit var minIndex: QuantityLinearIntermediateSymbol<Flt64>
    override lateinit var maxIndex: QuantityLinearIntermediateSymbol<Flt64>

    private fun invalidIndexUnit(): Try {
        return Failed(
            ErrorCode.IllegalArgument,
            "条件包络 ${name} 的纵向力矩单位不兼容 / Conditional envelope ${name} has incompatible longitudinal-moment units"
        )
    }

    private fun piecewiseSide(side: AbstractEnvelope.Side): Ret<QuantityLinearIntermediateSymbol<Flt64>> {
        if (side.points.size < 2) {
            return Failed(
                ErrorCode.IllegalArgument,
                "条件包络 ${name} 的 ${side.name} 至少需要两个断点，无法建立有限范围 / Conditional envelope ${name} side ${side.name} needs at least two points to establish a finite range"
            )
        }
        val estimate = totalWeight.estimateTotalWeight[phase]
            ?: return Failed(
                ErrorCode.IllegalArgument,
                "条件包络 ${name} 缺少 ${phase.name} 的总重估计 / Conditional envelope ${name} is missing an estimated total weight for ${phase.name}"
            )
        return side.piecewise(estimate)
    }

    private fun Flt64.isFiniteEnvelopeBound(): Boolean {
        if (!isFinite()) {
            return false
        }
        if (constants.nan?.let { this == it } == true) {
            return false
        }

        // Solver full-range sentinels are technically finite doubles but do not prove a usable bound.
        // 求解器的全范围哨兵在浮点层面虽是有限值，但不能证明可用的有限边界。
        return compareTo(Flt64.minimum) > 0 && compareTo(Flt64.maximum) < 0
    }

    private fun sidePointBounds(
        side: AbstractEnvelope.Side,
        label: String
    ): Ret<ConditionBounds<Flt64>> {
        if (side.points.isEmpty()) {
            return Failed(
                ErrorCode.IllegalArgument,
                "条件包络 ${name} 的 ${label} 缺少断点，无法证明有限范围 / Conditional envelope ${name} ${label} has no points from which a finite range can be proven"
            )
        }

        val values = side.points.mapIndexed { index, point ->
            val pointWeight = point.totalWeight.to(aircraftModel.weightUnit)?.value
            val pointIndex = point.index.to(aircraftModel.torqueUnit)?.value
            if (pointWeight == null || pointIndex == null) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "条件包络 ${name} 的 ${label} 第 ${index + 1} 个断点单位不兼容，无法证明有限范围 / Conditional envelope ${name} ${label} point ${index + 1} has incompatible units, so a finite range cannot be proven"
                )
            }
            if (!pointWeight.isFiniteEnvelopeBound() || !pointIndex.isFiniteEnvelopeBound()) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "条件包络 ${name} 的 ${label} 第 ${index + 1} 个断点不是有限值，无法证明有限范围 / Conditional envelope ${name} ${label} point ${index + 1} is non-finite, so a finite range cannot be proven"
                )
            }
            pointIndex
        }

        var lower = values.first()
        var upper = values.first()
        values.drop(1).forEach { value ->
            if (value.compareTo(lower) < 0) {
                lower = value
            }
            if (value.compareTo(upper) > 0) {
                upper = value
            }
        }
        return Ok(ConditionBounds(lower = lower, upper = upper))
    }

    private fun boundaryBounds(
        symbol: QuantityLinearIntermediateSymbol<Flt64>,
        side: AbstractEnvelope.Side,
        label: String
    ): Ret<ConditionBounds<Flt64>> {
        // A piecewise adapter exposes a helper result variable and an unbounded adapter range;
        // its actual finite output range comes from the envelope points.
        // 分段适配器暴露的是辅助结果变量和无界适配器范围，其实际有限输出范围应来自包络断点。
        val inferred = if (symbol.value is LinearFunctionSymbolAdapter<*>) {
            null
        } else {
            symbol.value.toLinearPolynomial().finiteBounds(IntoValue.Identity)
        }
        if (inferred != null &&
            inferred.lower.isFiniteEnvelopeBound() &&
            inferred.upper.isFiniteEnvelopeBound() &&
            inferred.lower.compareTo(inferred.upper) <= 0
        ) {
            return Ok(ConditionBounds(lower = inferred.lower, upper = inferred.upper))
        }
        return sidePointBounds(side, label)
    }

    private fun differenceBounds(
        first: ConditionBounds<Flt64>,
        second: ConditionBounds<Flt64>,
        label: String
    ): Ret<ConditionBounds<Flt64>> {
        val lower = first.lower - second.upper
        val upper = first.upper - second.lower
        if (!lower.isFiniteEnvelopeBound() || !upper.isFiniteEnvelopeBound() || lower.compareTo(upper) > 0) {
            return Failed(
                ErrorCode.IllegalArgument,
                "条件包络 ${name} 的 ${label} 无法得到有限差值范围 / Conditional envelope ${name} cannot derive a finite difference range for ${label}"
            )
        }
        return Ok(ConditionBounds(lower = lower, upper = upper))
    }

    private fun sideAt(side: AbstractEnvelope.Side): Ret<QuantityLinearIntermediateSymbol<Flt64>> {
        val thisTotalWeight = totalWeight.computedTotalWeight[phase]
        return if (thisTotalWeight != null) {
            val index = side(thisTotalWeight).to(aircraftModel.torqueUnit)
                ?: return Failed(
                    ErrorCode.IllegalArgument,
                    "条件包络 ${name} 的纵向力矩单位不兼容 / Conditional envelope ${name} has incompatible longitudinal-moment units"
                )
            Ok(Quantity(
                LinearExpressionSymbol(
                    LinearPolynomial(index.value),
                    name = "${name}_${side.name}_${phase.name.lowercase(Locale.getDefault())}"
                ),
                aircraftModel.torqueUnit
            ))
        } else {
            piecewiseSide(side)
        }
    }

    private fun addSymbol(
        model: AbstractLinearMetaModel<Flt64>,
        symbol: LinearIntermediateSymbol<Flt64>
    ): Try {
        return when (val result = model.add(symbol)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    private fun registerUnknownCondition(
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        if (!::condition.isInitialized) {
            condition = when (val condition = symbolCondition(
                "${name}_${phase.name.lowercase(Locale.getDefault())}_condition"
            )) {
                is Either.Left -> LinearExpressionSymbol(
                    condition.value,
                    name = "${name}_${phase.name.lowercase(Locale.getDefault())}_condition"
                )
                is Either.Right -> condition.value
            }
        }
        when (val result = addSymbol(model, condition)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        if (!::minIndex.isInitialized) {
            val min1 = when (val result = sideAt(lhsSide1)) {
                is Ok -> result.value ?: return Failed(
                    ErrorCode.ApplicationError,
                    "条件包络 ${name} 的第一组左侧边界为空 / Conditional envelope ${name} returned an empty first left bound"
                )
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            val min2 = when (val result = sideAt(lhsSide2)) {
                is Ok -> result.value ?: return Failed(
                    ErrorCode.ApplicationError,
                    "条件包络 ${name} 的第二组左侧边界为空 / Conditional envelope ${name} returned an empty second left bound"
                )
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            val max1 = when (val result = sideAt(rhsSide1)) {
                is Ok -> result.value ?: return Failed(
                    ErrorCode.ApplicationError,
                    "条件包络 ${name} 的第一组右侧边界为空 / Conditional envelope ${name} returned an empty first right bound"
                )
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            val max2 = when (val result = sideAt(rhsSide2)) {
                is Ok -> result.value ?: return Failed(
                    ErrorCode.ApplicationError,
                    "条件包络 ${name} 的第二组右侧边界为空 / Conditional envelope ${name} returned an empty second right bound"
                )
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }

            val min1Bounds = when (val result = boundaryBounds(min1, lhsSide1, "第一组左侧边界")) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            val min2Bounds = when (val result = boundaryBounds(min2, lhsSide2, "第二组左侧边界")) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            val max1Bounds = when (val result = boundaryBounds(max1, rhsSide1, "第一组右侧边界")) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            val max2Bounds = when (val result = boundaryBounds(max2, rhsSide2, "第二组右侧边界")) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            val minSwitchBounds = when (val result = differenceBounds(min1Bounds, min2Bounds, "minSwitch")) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            val maxSwitchBounds = when (val result = differenceBounds(max1Bounds, max2Bounds, "maxSwitch")) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }

            listOf(min1.value, min2.value, max1.value, max2.value).forEach { symbol ->
                when (val result = addSymbol(model, symbol)) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            }

            val minSwitch = LinearFunctionSymbolAdapter(
                IfThenFunction(
                    condition = condition.toLinearPolynomial(),
                    thenPoly = min1.value.toLinearPolynomial() - min2.value.toLinearPolynomial(),
                    converter = IntoValue.Identity,
                    name = "${name}_${phase.name.lowercase(Locale.getDefault())}_min_switch",
                    conditionBounds = ConditionBounds(Flt64.zero, Flt64.one),
                    thenBounds = minSwitchBounds
                ),
                converter = IntoValue.Identity
            )
            val maxSwitch = LinearFunctionSymbolAdapter(
                IfThenFunction(
                    condition = condition.toLinearPolynomial(),
                    thenPoly = max1.value.toLinearPolynomial() - max2.value.toLinearPolynomial(),
                    converter = IntoValue.Identity,
                    name = "${name}_${phase.name.lowercase(Locale.getDefault())}_max_switch",
                    conditionBounds = ConditionBounds(Flt64.zero, Flt64.one),
                    thenBounds = maxSwitchBounds
                ),
                converter = IntoValue.Identity
            )
            when (val result = addSymbol(model, minSwitch)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            when (val result = addSymbol(model, maxSwitch)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }

            minIndex = Quantity(
                LinearExpressionSymbol(
                    min2.value.toLinearPolynomial() + minSwitch.toLinearPolynomial(),
                    name = "min_index_${name}_${phase.name.lowercase(Locale.getDefault())}"
                ),
                aircraftModel.torqueUnit
            )
            maxIndex = Quantity(
                LinearExpressionSymbol(
                    max2.value.toLinearPolynomial() + maxSwitch.toLinearPolynomial(),
                    name = "max_index_${name}_${phase.name.lowercase(Locale.getDefault())}"
                ),
                aircraftModel.torqueUnit
            )
        }

        when (val result = model.add(minIndex)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        return when (val result = model.add(maxIndex)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun register(
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        val conditionValue = valueCondition()
        if (conditionValue == null) {
            return registerUnknownCondition(model)
        }
        when (conditionValue) {
            true -> {
                if (!::minIndex.isInitialized) {
                    val thisTotalWeight = totalWeight.computedTotalWeight[phase]
                    minIndex = if (thisTotalWeight != null) {
                        Quantity(
                            LinearExpressionSymbol(
                                LinearPolynomial(
                                    lhsSide1(thisTotalWeight).to(aircraftModel.torqueUnit)?.value
                                        ?: return invalidIndexUnit()
                                ),
                                name = "min_index_${name}_${phase.name.lowercase(Locale.getDefault())}"
                            ),
                            aircraftModel.torqueUnit
                        )
                    } else {
                        when (val result = piecewiseSide(lhsSide1)) {
                            is Ok -> result.value ?: return Failed(
                                ErrorCode.ApplicationError,
                                "条件包络 ${name} 的左侧分段边界为空 / Conditional envelope ${name} returned an empty left piecewise bound"
                            )
                            is Failed -> return Failed(result.error)
                            is Fatal -> return Fatal(result.errors)
                        }
                    }
                }
                when (val result = model.add(minIndex)) {
                    is Ok -> {}

                    is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
                }

                if (!::maxIndex.isInitialized) {
                    val thisTotalWeight = totalWeight.computedTotalWeight[phase]
                    maxIndex = if (thisTotalWeight != null) {
                        Quantity(
                            LinearExpressionSymbol(
                                LinearPolynomial(
                                    rhsSide1(thisTotalWeight).to(aircraftModel.torqueUnit)?.value
                                        ?: return invalidIndexUnit()
                                ),
                                name = "max_index_${name}_${phase.name.lowercase(Locale.getDefault())}"
                            ),
                            aircraftModel.torqueUnit
                        )
                    } else {
                        when (val result = piecewiseSide(rhsSide1)) {
                            is Ok -> result.value ?: return Failed(
                                ErrorCode.ApplicationError,
                                "条件包络 ${name} 的右侧分段边界为空 / Conditional envelope ${name} returned an empty right piecewise bound"
                            )
                            is Failed -> return Failed(result.error)
                            is Fatal -> return Fatal(result.errors)
                        }
                    }
                }
                when (val result = model.add(maxIndex)) {
                    is Ok -> {}

                    is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
                }
            }

            false -> {
                if (!::minIndex.isInitialized) {
                    val thisTotalWeight = totalWeight.computedTotalWeight[phase]
                    minIndex = if (thisTotalWeight != null) {
                        Quantity(
                            LinearExpressionSymbol(
                                LinearPolynomial(
                                    lhsSide2(thisTotalWeight).to(aircraftModel.torqueUnit)?.value
                                        ?: return invalidIndexUnit()
                                ),
                                name = "min_index_${name}_${phase.name.lowercase(Locale.getDefault())}"
                            ),
                            aircraftModel.torqueUnit
                        )
                    } else {
                        when (val result = piecewiseSide(lhsSide2)) {
                            is Ok -> result.value ?: return Failed(
                                ErrorCode.ApplicationError,
                                "条件包络 ${name} 的左侧分段边界为空 / Conditional envelope ${name} returned an empty left piecewise bound"
                            )
                            is Failed -> return Failed(result.error)
                            is Fatal -> return Fatal(result.errors)
                        }
                    }
                }
                when (val result = model.add(minIndex)) {
                    is Ok -> {}

                    is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
                }

                if (!::maxIndex.isInitialized) {
                    val thisTotalWeight = totalWeight.computedTotalWeight[phase]
                    maxIndex = if (thisTotalWeight != null) {
                        Quantity(
                            LinearExpressionSymbol(
                                LinearPolynomial(
                                    rhsSide2(thisTotalWeight).to(aircraftModel.torqueUnit)?.value
                                        ?: return invalidIndexUnit()
                                ),
                                name = "max_index_${name}_${phase.name.lowercase(Locale.getDefault())}"
                            ),
                            aircraftModel.torqueUnit
                        )
                    } else {
                        when (val result = piecewiseSide(rhsSide2)) {
                            is Ok -> result.value ?: return Failed(
                                ErrorCode.ApplicationError,
                                "条件包络 ${name} 的右侧分段边界为空 / Conditional envelope ${name} returned an empty right piecewise bound"
                            )
                            is Failed -> return Failed(result.error)
                            is Fatal -> return Fatal(result.errors)
                        }
                    }
                }
                when (val result = model.add(maxIndex)) {
                    is Ok -> {}

                    is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
                }
            }

            null -> {
                if (!::condition.isInitialized) {
                    condition = when (val condition = symbolCondition("${name}_${phase.name.lowercase(Locale.getDefault())}_condition")) {
                        is Either.Left -> {
                            LinearExpressionSymbol(
                                condition.value,
                                name = "${name}_${phase.name.lowercase(Locale.getDefault())}_condition"
                            )
                        }

                        is Either.Right -> {
                            condition.value
                        }
                    }
                }
                when (val result = model.add(condition)) {
                    is Ok -> {}

                    is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
                }

                if (!::minIndex.isInitialized) {
                    val thisTotalWeight = totalWeight.computedTotalWeight[phase]
                    val minIndex1 = if (thisTotalWeight != null) {
                        Quantity(
                            LinearExpressionSymbol(
                                LinearPolynomial(
                                    lhsSide1(thisTotalWeight).to(aircraftModel.torqueUnit)?.value
                                        ?: return invalidIndexUnit()
                                ),
                                name = "min_index_${lhsSide1.name}_${phase.name.lowercase(Locale.getDefault())}"
                            ),
                            aircraftModel.torqueUnit
                        )
                    } else {
                        when (val result = piecewiseSide(lhsSide1)) {
                            is Ok -> result.value ?: return Failed(
                                ErrorCode.ApplicationError,
                                "条件包络 ${name} 的左侧分段边界为空 / Conditional envelope ${name} returned an empty left piecewise bound"
                            )
                            is Failed -> return Failed(result.error)
                            is Fatal -> return Fatal(result.errors)
                        }
                    }
                    val minIndex2 = if (thisTotalWeight != null) {
                        Quantity(
                            LinearExpressionSymbol(
                                LinearPolynomial(
                                    lhsSide2(thisTotalWeight).to(aircraftModel.torqueUnit)?.value
                                        ?: return invalidIndexUnit()
                                ),
                                name = "min_index_${lhsSide2.name}_${phase.name.lowercase(Locale.getDefault())}"
                            ),
                            aircraftModel.torqueUnit
                        )
                    } else {
                        when (val result = piecewiseSide(lhsSide2)) {
                            is Ok -> result.value ?: return Failed(
                                ErrorCode.ApplicationError,
                                "条件包络 ${name} 的左侧分段边界为空 / Conditional envelope ${name} returned an empty left piecewise bound"
                            )
                            is Failed -> return Failed(result.error)
                            is Fatal -> return Fatal(result.errors)
                        }
                    }
                    minIndex = minIndex1
                }
                when (val result = model.add(minIndex)) {
                    is Ok -> {}

                    is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
                }

                if (!::maxIndex.isInitialized) {
                    val thisTotalWeight = totalWeight.computedTotalWeight[phase]
                    val maxIndex1 = if (thisTotalWeight != null) {
                        Quantity(
                            LinearExpressionSymbol(
                                LinearPolynomial(
                                    rhsSide1(thisTotalWeight).to(aircraftModel.torqueUnit)?.value
                                        ?: return invalidIndexUnit()
                                ),
                                name = "max_index_${rhsSide1.name}_${phase.name.lowercase(Locale.getDefault())}"
                            ),
                            aircraftModel.torqueUnit
                        )
                    } else {
                        when (val result = piecewiseSide(rhsSide1)) {
                            is Ok -> result.value ?: return Failed(
                                ErrorCode.ApplicationError,
                                "条件包络 ${name} 的右侧分段边界为空 / Conditional envelope ${name} returned an empty right piecewise bound"
                            )
                            is Failed -> return Failed(result.error)
                            is Fatal -> return Fatal(result.errors)
                        }
                    }
                    val maxIndex2 = if (thisTotalWeight != null) {
                        Quantity(
                            LinearExpressionSymbol(
                                LinearPolynomial(
                                    rhsSide2(thisTotalWeight).to(aircraftModel.torqueUnit)?.value
                                        ?: return invalidIndexUnit()
                                ),
                                name = "max_index_${rhsSide2.name}_${phase.name.lowercase(Locale.getDefault())}"
                            ),
                            aircraftModel.torqueUnit
                        )
                    } else {
                        when (val result = piecewiseSide(rhsSide2)) {
                            is Ok -> result.value ?: return Failed(
                                ErrorCode.ApplicationError,
                                "条件包络 ${name} 的右侧分段边界为空 / Conditional envelope ${name} returned an empty right piecewise bound"
                            )
                            is Failed -> return Failed(result.error)
                            is Fatal -> return Fatal(result.errors)
                        }
                    }
                    maxIndex = maxIndex1
                }
                when (val result = model.add(maxIndex)) {
                    is Ok -> {}

                    is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
                }
            }
        }

        return ok
    }
}
