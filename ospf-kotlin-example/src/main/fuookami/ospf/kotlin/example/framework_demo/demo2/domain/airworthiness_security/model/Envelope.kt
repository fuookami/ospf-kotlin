package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.model


import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import java.util.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.intermediate_symbol.*
import fuookami.ospf.kotlin.core.intermediate_symbol.function.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*

interface AbstractEnvelope {
    data class Point(
        val totalWeight: Quantity<Flt64>,
        val index: Quantity<Flt64>
    )

    enum class SideType {
        Left,
        Right
    }

    class Side(
        private val aircraftModel: AircraftModel,
        val name: String,
        val type: SideType,
        val points: List<Point>
    ) {
        val piecewise by lazy {
            UnivariateLinearPiecewiseFunction(
                // ռλ��ʹ������Ĳ�ֵ����
                x = LinearPolynomial(),
                points = points.map {
                    Point2(
                        it.totalWeight.to(aircraftModel.weightUnit)!!.value,
                        it.index.to(aircraftModel.torqueUnit)!!.value
                    )
                },
                name = "${name}_${type.name.lowercase(Locale.getDefault())}"
            )
        }

        fun piecewise(totalWeight: QuantityLinearIntermediateSymbol): QuantityLinearIntermediateSymbol {
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
    val minIndex: QuantityLinearIntermediateSymbol
    val maxIndex: QuantityLinearIntermediateSymbol

    fun register(model: AbstractLinearMetaModelF64): Try
}

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

    override lateinit var minIndex: QuantityLinearIntermediateSymbol
    override lateinit var maxIndex: QuantityLinearIntermediateSymbol

    override fun register(
        model: AbstractLinearMetaModelF64
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

class ConditionalEnvelope(
    private val aircraftModel: AircraftModel,
    override val phase: FlightPhase,
    override val name: String,
    val lhsSide1: AbstractEnvelope.Side,
    val rhsSide1: AbstractEnvelope.Side,
    val lhsSide2: AbstractEnvelope.Side,
    val rhsSide2: AbstractEnvelope.Side,
    val valueCondition: () -> Boolean?,
    val symbolCondition: (String) -> Either<LinearPolynomial<Flt64>, LinearFunctionSymbolAdapter>,
    private val totalWeight: TotalWeight
) : AbstractEnvelope {
    lateinit var condition: LinearIntermediateSymbolF64
    override lateinit var minIndex: QuantityLinearIntermediateSymbol
    override lateinit var maxIndex: QuantityLinearIntermediateSymbol

    override fun register(
        model: AbstractLinearMetaModelF64
    ): Try {
        when (valueCondition()) {
            true -> {
                // ȡ��һ�ְ���
                if (!::minIndex.isInitialized) {
                    val thisTotalWeight = totalWeight.computedTotalWeight[phase]
                    minIndex = if (thisTotalWeight != null) {
                        // Ԥ���ء�ȫ����ģʽ�£������Ǹ�ȷ��ֵ�����Կ���ͨ�����Բ�ֱֵ�������Сָ��
                        Quantity(
                            LinearExpressionSymbol(
                                LinearPolynomial(lhsSide1(thisTotalWeight).to(aircraftModel.weightUnit)!!.value),
                                name = "min_index_${name}_${phase.name.lowercase(Locale.getDefault())}"
                            ),
                            aircraftModel.torqueUnit
                        )
                    } else {
                        // �������Բ�ֵ��һԪ�������ֶ����Ժ�����ӳ����������Сָ���Ĺ�ϵ
                        lhsSide1.piecewise(totalWeight.estimateTotalWeight[phase]!!)
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
                        // Ԥ���ء�ȫ����ģʽ�£������Ǹ�ȷ��ֵ�����Կ���ͨ�����Բ�ֱֵ��������ָ��
                        Quantity(
                            LinearExpressionSymbol(
                                LinearPolynomial(rhsSide1(thisTotalWeight).to(aircraftModel.weightUnit)!!.value),
                                name = "max_index_${name}_${phase.name.lowercase(Locale.getDefault())}"
                            ),
                            aircraftModel.torqueUnit
                        )
                    } else {
                        // �������Բ�ֵ��һԪ�������ֶ����Ժ�����ӳ�����������ָ���Ĺ�ϵ
                        rhsSide1.piecewise(totalWeight.estimateTotalWeight[phase]!!)
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
                // ȡ�ڶ��ְ���
                if (!::minIndex.isInitialized) {
                    val thisTotalWeight = totalWeight.computedTotalWeight[phase]
                    minIndex = if (thisTotalWeight != null) {
                        // Ԥ���ء�ȫ����ģʽ�£������Ǹ�ȷ��ֵ�����Կ���ͨ�����Բ�ֱֵ�������Сָ��
                        Quantity(
                            LinearExpressionSymbol(
                                LinearPolynomial(lhsSide2(thisTotalWeight).to(aircraftModel.weightUnit)!!.value),
                                name = "min_index_${name}_${phase.name.lowercase(Locale.getDefault())}"
                            ),
                            aircraftModel.torqueUnit
                        )
                    } else {
                        // �������Բ�ֵ��һԪ�������ֶ����Ժ�����ӳ����������Сָ���Ĺ�ϵ
                        lhsSide2.piecewise(totalWeight.estimateTotalWeight[phase]!!)
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
                        // Ԥ���ء�ȫ����ģʽ�£������Ǹ�ȷ��ֵ�����Կ���ͨ�����Բ�ֱֵ��������ָ��
                        Quantity(
                            LinearExpressionSymbol(
                                LinearPolynomial(rhsSide2(thisTotalWeight).to(aircraftModel.weightUnit)!!.value),
                                name = "max_index_${name}_${phase.name.lowercase(Locale.getDefault())}"
                            ),
                            aircraftModel.torqueUnit
                        )
                    } else {
                        // �������Բ�ֵ��һԪ�������ֶ����Ժ�����ӳ�����������ָ���Ĺ�ϵ
                        rhsSide2.piecewise(totalWeight.estimateTotalWeight[phase]!!)
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
                // ʹ����������ʽѡ�����
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
                        // Ԥ���ء�ȫ����ģʽ�£������Ǹ�ȷ��ֵ�����Կ���ͨ�����Բ�ֱֵ�������Сָ��
                        Quantity(
                            LinearExpressionSymbol(
                                LinearPolynomial(lhsSide1(thisTotalWeight).to(aircraftModel.weightUnit)!!.value),
                                name = "min_index_${lhsSide1.name}_${phase.name.lowercase(Locale.getDefault())}"
                            ),
                            aircraftModel.torqueUnit
                        )
                    } else {
                        // �������Բ�ֵ��һԪ�������ֶ����Ժ�����ӳ����������Сָ���Ĺ�ϵ
                        lhsSide1.piecewise(totalWeight.estimateTotalWeight[phase]!!)
                    }
                    val minIndex2 = if (thisTotalWeight != null) {
                        // Ԥ���ء�ȫ����ģʽ�£������Ǹ�ȷ��ֵ�����Կ���ͨ�����Բ�ֱֵ�������Сָ��
                        Quantity(
                            LinearExpressionSymbol(
                                LinearPolynomial(lhsSide2(thisTotalWeight).to(aircraftModel.weightUnit)!!.value),
                                name = "min_index_${lhsSide2.name}_${phase.name.lowercase(Locale.getDefault())}"
                            ),
                            aircraftModel.torqueUnit
                        )
                    } else {
                        // �������Բ�ֵ��һԪ�������ֶ����Ժ�����ӳ����������Сָ���Ĺ�ϵ
                        lhsSide2.piecewise(totalWeight.estimateTotalWeight[phase]!!)
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
                        // Ԥ���ء�ȫ����ģʽ�£������Ǹ�ȷ��ֵ�����Կ���ͨ�����Բ�ֱֵ��������ָ��
                        Quantity(
                            LinearExpressionSymbol(
                                LinearPolynomial(rhsSide1(thisTotalWeight).to(aircraftModel.weightUnit)!!.value),
                                name = "max_index_${rhsSide1.name}_${phase.name.lowercase(Locale.getDefault())}"
                            ),
                            aircraftModel.torqueUnit
                        )
                    } else {
                        // �������Բ�ֵ��һԪ�������ֶ����Ժ�����ӳ�����������ָ���Ĺ�ϵ
                        rhsSide1.piecewise(totalWeight.estimateTotalWeight[phase]!!)
                    }
                    val maxIndex2 = if (thisTotalWeight != null) {
                        // Ԥ���ء�ȫ����ģʽ�£������Ǹ�ȷ��ֵ�����Կ���ͨ�����Բ�ֱֵ��������ָ��
                        Quantity(
                            LinearExpressionSymbol(
                                LinearPolynomial(rhsSide2(thisTotalWeight).to(aircraftModel.weightUnit)!!.value),
                                name = "max_index_${rhsSide2.name}_${phase.name.lowercase(Locale.getDefault())}"
                            ),
                            aircraftModel.torqueUnit
                        )
                    } else {
                        // �������Բ�ֵ��һԪ�������ֶ����Ժ�����ӳ�����������ָ���Ĺ�ϵ
                        rhsSide2.piecewise(totalWeight.estimateTotalWeight[phase]!!)
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














