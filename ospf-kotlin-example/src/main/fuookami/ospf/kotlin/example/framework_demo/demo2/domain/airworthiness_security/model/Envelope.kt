package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.model

import java.util.*
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
     * @property totalWeight Total weight at this point. / 此点的总重量
     * @property index CG index at this point. / 此点的 CG 指数
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
     * @property name The name of this side. / 此侧边的名称
     * @property type The side type (left or right). / 侧边类型（左侧或右侧）
     * @property points The list of points defining this side. / 定义此侧边的点列表
    */
    class Side(
        private val aircraftModel: AircraftModel,
        val name: String,
        val type: SideType,
        val points: List<Point>
    ) {
        val piecewise by lazy {
            UnivariateLinearPiecewiseFunction.fromPoints(
                x = LinearPolynomial(),
                points = points.map {
                    point2(
                        it.totalWeight.to(aircraftModel.weightUnit)!!.value,
                        it.index.to(aircraftModel.torqueUnit)!!.value
                    )
                },
                converter = IntoValue.Identity,
                name = "${name}_${type.name.lowercase(Locale.getDefault())}"
            )
        }
/**
 * Returns the piecewise linear CG index bound as a quantity symbol for the given total weight.
 * 返回给定总重量的分段线性 CG 指数边界作为量符号。
 * @param totalWeight The total weight symbol to evaluate the envelope bound at / 要评估包络线边界的总重量符号
 * @return The CG index bound as a quantity linear intermediate symbol / CG 指数边界作为量线性中间符号
*/
        fun piecewise(totalWeight: QuantityLinearIntermediateSymbol<Flt64>): QuantityLinearIntermediateSymbol<Flt64> {
            return Quantity(
                LinearExpressionSymbol(
                    Flt64.zero,
                    name = when (type) {
                        SideType.Left -> "min_index_${name}_${type.name.lowercase(Locale.getDefault())}"
                        SideType.Right -> "max_index_${name}_${type.name.lowercase(Locale.getDefault())}"
                    }
                ),
                aircraftModel.torqueUnit
            )
        }

        operator fun invoke(totalWeight: Quantity<Flt64>): Quantity<Flt64> {
            return points.firstOrNull()?.index ?: Quantity(Flt64.zero, aircraftModel.torqueUnit)
        }
    }

    val phase: FlightPhase
    val name: String
    val minIndex: QuantityLinearIntermediateSymbol<Flt64>
    val maxIndex: QuantityLinearIntermediateSymbol<Flt64>

/**
 * Registers the envelope's min and max index symbols into the linear meta model.
 * 将包络线的最小和最大指数符号注册到线性元模型中。
 * @param model The linear meta model to register symbols into / 要注册符号的线性元模型
 * @return Success or failure of the registration / 注册操作的成功或失败
*/
    fun register(model: AbstractLinearMetaModel<Flt64>): Try
}

/**
 * Standard CG envelope with left and right sides defining min/max index bounds.
 * 具有定义最小/最大指数边界的左右两侧的标准 CG 包络线。
 *
 * @property lhsSide The left-hand side of the envelope. / 包络线的左侧
 * @property rhsSide The right-hand side of the envelope. / 包络线的右侧
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
     * @param totalWeight The total weight to evaluate. / 要评估的总重量
     * @return The minimum CG index quantity. / 最小 CG 指数量
    */
    fun minIndexOf(totalWeight: Quantity<Flt64>): Quantity<Flt64> {
        return lhsSide(totalWeight)
    }

    /**
     * Returns the maximum CG index for the given total weight.
     * 返回给定总重量的最大 CG 指数。
     *
     * @param totalWeight The total weight to evaluate. / 要评估的总重量
     * @return The maximum CG index quantity. / 最大 CG 指数量
    */
    fun maxIndexOf(totalWeight: Quantity<Flt64>): Quantity<Flt64> {
        return rhsSide(totalWeight)
    }

    override lateinit var minIndex: QuantityLinearIntermediateSymbol<Flt64>
    override lateinit var maxIndex: QuantityLinearIntermediateSymbol<Flt64>

    override fun register(
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        if (!::minIndex.isInitialized) {
            val thisTotalWeight = totalWeight.computedTotalWeight[phase]
            minIndex = if (thisTotalWeight != null) {
                Quantity(
                    LinearExpressionSymbol(
                        minIndexOf(thisTotalWeight).to(aircraftModel.torqueUnit)!!.value,
                        name = "min_index_${name}_${phase.name.lowercase(Locale.getDefault())}"
                    ),
                    aircraftModel.torqueUnit
                )
            } else {
                lhsSide.piecewise(totalWeight.estimateTotalWeight[phase]!!)
            }
        }
        when (val result = model.add(minIndex)) {
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
        }

        if (!::maxIndex.isInitialized) {
            val thisTotalWeight = totalWeight.computedTotalWeight[phase]
            maxIndex = if (thisTotalWeight != null) {
                Quantity(
                    LinearExpressionSymbol(
                        maxIndexOf(thisTotalWeight).to(aircraftModel.torqueUnit)!!.value,
                        name = "max_index_${name}_${phase.name.lowercase(Locale.getDefault())}"
                    ),
                    aircraftModel.torqueUnit
                )
            } else {
                rhsSide.piecewise(totalWeight.estimateTotalWeight[phase]!!)
            }
        }
        when (val result = model.add(maxIndex)) {
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

/**
 * CG envelope with conditional sides that switch based on a runtime condition.
 * 具有基于运行时条件切换的条件边的 CG 包络线。
 *
 * @property lhsSide1 The first left-hand side of the envelope. / 包络线的第一个左侧
 * @property rhsSide1 The first right-hand side of the envelope. / 包络线的第一个右侧
 * @property lhsSide2 The second left-hand side of the envelope. / 包络线的第二个左侧
 * @property rhsSide2 The second right-hand side of the envelope. / 包络线的第二个右侧
 * @property valueCondition Runtime condition function returning true, false, or null. / 运行时条件函数，返回 true、false 或 null
 * @property symbolCondition Symbolic condition function for the model. / 模型的符号条件函数
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

    override fun register(
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        when (valueCondition()) {
            true -> {
                if (!::minIndex.isInitialized) {
                    val thisTotalWeight = totalWeight.computedTotalWeight[phase]
                    minIndex = if (thisTotalWeight != null) {
                        Quantity(
                            LinearExpressionSymbol(
                                LinearPolynomial(lhsSide1(thisTotalWeight).to(aircraftModel.weightUnit)!!.value),
                                name = "min_index_${name}_${phase.name.lowercase(Locale.getDefault())}"
                            ),
                            aircraftModel.torqueUnit
                        )
                    } else {
                        lhsSide1.piecewise(totalWeight.estimateTotalWeight[phase]!!)
                    }
                }
                when (val result = model.add(minIndex)) {
                    is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

                    is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
                }

                if (!::maxIndex.isInitialized) {
                    val thisTotalWeight = totalWeight.computedTotalWeight[phase]
                    maxIndex = if (thisTotalWeight != null) {
                        Quantity(
                            LinearExpressionSymbol(
                                LinearPolynomial(rhsSide1(thisTotalWeight).to(aircraftModel.weightUnit)!!.value),
                                name = "max_index_${name}_${phase.name.lowercase(Locale.getDefault())}"
                            ),
                            aircraftModel.torqueUnit
                        )
                    } else {
                        rhsSide1.piecewise(totalWeight.estimateTotalWeight[phase]!!)
                    }
                }
                when (val result = model.add(maxIndex)) {
                    is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

                    is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
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
                                LinearPolynomial(lhsSide2(thisTotalWeight).to(aircraftModel.weightUnit)!!.value),
                                name = "min_index_${name}_${phase.name.lowercase(Locale.getDefault())}"
                            ),
                            aircraftModel.torqueUnit
                        )
                    } else {
                        lhsSide2.piecewise(totalWeight.estimateTotalWeight[phase]!!)
                    }
                }
                when (val result = model.add(minIndex)) {
                    is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

                    is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
                }

                if (!::maxIndex.isInitialized) {
                    val thisTotalWeight = totalWeight.computedTotalWeight[phase]
                    maxIndex = if (thisTotalWeight != null) {
                        Quantity(
                            LinearExpressionSymbol(
                                LinearPolynomial(rhsSide2(thisTotalWeight).to(aircraftModel.weightUnit)!!.value),
                                name = "max_index_${name}_${phase.name.lowercase(Locale.getDefault())}"
                            ),
                            aircraftModel.torqueUnit
                        )
                    } else {
                        rhsSide2.piecewise(totalWeight.estimateTotalWeight[phase]!!)
                    }
                }
                when (val result = model.add(maxIndex)) {
                    is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

                    is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
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
                    is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

                    is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
                }

                if (!::minIndex.isInitialized) {
                    val thisTotalWeight = totalWeight.computedTotalWeight[phase]
                    val minIndex1 = if (thisTotalWeight != null) {
                        Quantity(
                            LinearExpressionSymbol(
                                LinearPolynomial(lhsSide1(thisTotalWeight).to(aircraftModel.weightUnit)!!.value),
                                name = "min_index_${lhsSide1.name}_${phase.name.lowercase(Locale.getDefault())}"
                            ),
                            aircraftModel.torqueUnit
                        )
                    } else {
                        lhsSide1.piecewise(totalWeight.estimateTotalWeight[phase]!!)
                    }
                    val minIndex2 = if (thisTotalWeight != null) {
                        Quantity(
                            LinearExpressionSymbol(
                                LinearPolynomial(lhsSide2(thisTotalWeight).to(aircraftModel.weightUnit)!!.value),
                                name = "min_index_${lhsSide2.name}_${phase.name.lowercase(Locale.getDefault())}"
                            ),
                            aircraftModel.torqueUnit
                        )
                    } else {
                        lhsSide2.piecewise(totalWeight.estimateTotalWeight[phase]!!)
                    }
                    minIndex = minIndex1
                }
                when (val result = model.add(minIndex)) {
                    is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

                    is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
                }

                if (!::maxIndex.isInitialized) {
                    val thisTotalWeight = totalWeight.computedTotalWeight[phase]
                    val maxIndex1 = if (thisTotalWeight != null) {
                        Quantity(
                            LinearExpressionSymbol(
                                LinearPolynomial(rhsSide1(thisTotalWeight).to(aircraftModel.weightUnit)!!.value),
                                name = "max_index_${rhsSide1.name}_${phase.name.lowercase(Locale.getDefault())}"
                            ),
                            aircraftModel.torqueUnit
                        )
                    } else {
                        rhsSide1.piecewise(totalWeight.estimateTotalWeight[phase]!!)
                    }
                    val maxIndex2 = if (thisTotalWeight != null) {
                        Quantity(
                            LinearExpressionSymbol(
                                LinearPolynomial(rhsSide2(thisTotalWeight).to(aircraftModel.weightUnit)!!.value),
                                name = "max_index_${rhsSide2.name}_${phase.name.lowercase(Locale.getDefault())}"
                            ),
                            aircraftModel.torqueUnit
                        )
                    } else {
                        rhsSide2.piecewise(totalWeight.estimateTotalWeight[phase]!!)
                    }
                    maxIndex = maxIndex1
                }
                when (val result = model.add(maxIndex)) {
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
