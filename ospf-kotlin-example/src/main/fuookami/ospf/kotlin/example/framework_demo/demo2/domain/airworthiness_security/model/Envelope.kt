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

/** 定义每个飞行阶段最小/最大指数边界的 CG 包络线约束接口。Interface for CG envelope constraints that define min/max index bounds per flight phase. */
interface AbstractEnvelope {
    /**
     * 包络线上的点。
     * A point on the envelope.
     *
     * @property totalWeight 总重量。
     * @property index 指数。
     */
    data class Point(
        val totalWeight: Quantity<Flt64>,
        val index: Quantity<Flt64>
    )

    enum class SideType {
        Left,
        Right
    }

    /**
     * 包络线的一侧。
     * A side of the envelope.
     *
     * @property name 名称。
     * @property type 类型。
     * @property points 点列表。
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

    fun register(model: AbstractLinearMetaModel<Flt64>): Try
}

/**
 * 具有定义最小/最大指数边界的左右两侧的标准 CG 包络线。Standard CG envelope with left and right sides defining min/max index bounds.
 *
 * @property private val aircraftModel 参数。
 * @property override val phase 参数。
 * @property override val name 参数。
 * @property lhsSide 参数。
 * @property rhsSide 参数。
 * @property private val totalWeight 参数。
 */
class Envelope(
    private val aircraftModel: AircraftModel,
    override val phase: FlightPhase,
    override val name: String,
    val lhsSide: AbstractEnvelope.Side,
    val rhsSide: AbstractEnvelope.Side,
    private val totalWeight: TotalWeight
) : AbstractEnvelope {
    fun minIndexOf(totalWeight: Quantity<Flt64>): Quantity<Flt64> {
        return lhsSide(totalWeight)
    }

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
 * 具有基于运行时条件切换的条件边的 CG 包络线。CG envelope with conditional sides that switch based on a runtime condition.
 *
 * @property private val aircraftModel 参数。
 * @property override val phase 参数。
 * @property override val name 参数。
 * @property lhsSide1 参数。
 * @property rhsSide1 参数。
 * @property lhsSide2 参数。
 * @property rhsSide2 参数。
 * @property valueCondition 参数。
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
