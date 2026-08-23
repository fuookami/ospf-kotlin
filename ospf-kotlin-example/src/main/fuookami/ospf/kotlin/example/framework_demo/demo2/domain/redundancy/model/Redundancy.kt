package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.redundancy.model

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.symbol.function.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.variable.UContinuous
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Position

private val flt64Converter = object : IntoValue<Flt64> {
    override fun intoValue(value: Flt64) = value
    override val zero get() = Flt64.zero
    override val one get() = Flt64.one
    override fun fromValue(value: Flt64) = value
}

/**
 * Computes redundancy (spare capacity) on the main deck and registers slack variables for optimization.
 * 计算主甲板上的冗余（备用容量）并注册松弛变量用于优化。
 *
 * @property aircraftModel 飞机模型引用 / The aircraft model reference
 * @property flight 航班信息 / The flight information
 * @property items 货物项目列表 / The list of cargo items
 * @property positions 装载位置列表 / The list of stowage positions
 * @property stowage 装载分配矩阵 / The stowage assignment matrix
 * @property load 载荷分布数据 / The load distribution data
 * @property payload 载荷数据 / The payload data
 * @property redundancy 冗余中间符号 / The redundancy intermediate symbol
 * @property predicateRedundancy 预测冗余中间符号 / The predicate redundancy intermediate symbol
 * @property redundancySlack 冗余松弛变量 / The redundancy slack variable
 * @property minRedundancy 最小冗余边界 / The minimum redundancy bound
 * @property maxRedundancy 最大冗余边界 / The maximum redundancy bound
*/
class Redundancy(
    private val aircraftModel: AircraftModel,
    private val flight: Flight,
    private val items: List<Item>,
    private val positions: List<Position>,
    private val stowage: Stowage,
    private val load: Load,
    private val payload: Payload
) {
    lateinit var redundancy: LinearIntermediateSymbol<Flt64>
    lateinit var predicateRedundancy: LinearIntermediateSymbol<Flt64>
    lateinit var redundancySlack: LinearIntermediateSymbol<Flt64>

    val minRedundancy: LinearPolynomial<Flt64> by lazy {
        LinearPolynomial(Flt64.zero)
    }

    val maxRedundancy: LinearPolynomial<Flt64> by lazy {
        LinearPolynomial(mainDeckCapacity)
    }

    private val mainDeckCapacity: Flt64 by lazy {
        positions
            .asSequence()
            .filter { it.location.main }
            .map { position: Position -> position.mlw.mlw.value }
            .fold(Flt64.zero) { total, capacity -> total + capacity }
    }

    private fun loadPolynomial(
        estimate: Boolean
    ): LinearPolynomial<Flt64> {
        var poly = LinearPolynomial(mainDeckCapacity)
        for ((j, position) in positions.withIndex()) {
            if (position.location.main) {
                val loadWeight = if (estimate) {
                    load.estimateLoadWeight[j]
                } else {
                    load.actualLoadWeight[j]
                }
                poly -= loadWeight.to(aircraftModel.weightUnit)!!.value.toLinearPolynomial()
            }
        }
        return poly
    }

    /**
     * Registers redundancy intermediate symbols and slack variables into the optimization model.
     * 将冗余中间符号和松弛变量注册到优化模型中。
     *
     * @param model 要注册到的线性元模型 / The linear meta model to register into
     * @return 成功或失败结果 / Success or failure result
    */
    fun register(
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        if (!::redundancy.isInitialized) {
            redundancy = LinearExpressionSymbol(
                loadPolynomial(estimate = false),
                name = "redundancy"
            )
        }
        when (val result = model.add(redundancy)) {
            is Ok -> {}

            is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        if (!::predicateRedundancy.isInitialized) {
            predicateRedundancy = LinearExpressionSymbol(
                loadPolynomial(estimate = true),
                name = "predicate_redundancy"
            )
        }
        when (val result = model.add(predicateRedundancy)) {
            is Ok -> {}

            is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        if (!::redundancySlack.isInitialized) {
            redundancySlack = LinearFunctionSymbolAdapter(
                delegate = SlackFunction(
                    x = LinearPolynomial(redundancy),
                    y = minRedundancy,
                    type = UContinuous,
                    withNegative = true,
                    withPositive = true,
                    converter = flt64Converter,
                    name = "redundancy_slack"
                ),
                converter = flt64Converter
            )
        }
        when (val result = model.add(redundancySlack)) {
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
