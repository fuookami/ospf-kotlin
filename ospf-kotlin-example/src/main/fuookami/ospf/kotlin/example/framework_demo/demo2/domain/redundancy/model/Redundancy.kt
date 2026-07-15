package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.redundancy.model

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
 * @property aircraftModel The aircraft model reference / 飞机模型引用
 * @property flight The flight information / 航班信息
 * @property items The list of cargo items / 货物项目列表
 * @property positions The list of stowage positions / 装载位置列表
 * @property stowage The stowage assignment matrix / 装载分配矩阵
 * @property load The load distribution data / 载荷分布数据
 * @property payload The payload data / 载荷数据
 * @property redundancy The redundancy intermediate symbol / 冗余中间符号
 * @property predicateRedundancy The predicate redundancy intermediate symbol / 预测冗余中间符号
 * @property redundancySlack The redundancy slack variable / 冗余松弛变量
 * @property minRedundancy The minimum redundancy bound / 最小冗余边界
 * @property maxRedundancy The maximum redundancy bound / 最大冗余边界
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
        TODO("not implemented yet")
    }

    val maxRedundancy: LinearPolynomial<Flt64> by lazy {
        TODO("not implemented yet")
    }

    /**
     * Registers redundancy intermediate symbols and slack variables into the optimization model.
     * 将冗余中间符号和松弛变量注册到优化模型中。
     *
     * @param model The linear meta model to register into / 要注册到的线性元模型
     * @return Success or failure result / 成功或失败结果
    */
    fun register(
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        if (!::redundancy.isInitialized) {
            val poly = MutableLinearPolynomial()
            for ((i, item) in items.withIndex()) {
                if (!item.location.main) {
                    continue
                }

                when (item.status) {
                    ItemStatus.Reserved -> {
                        poly -= Flt64.one
                    }

                    ItemStatus.Optional -> {
                        poly -= LinearPolynomial(stowage.loaded[i])
                    }

                    else -> {}
                }
            }
            redundancy = LinearExpressionSymbol(
                poly,
                name = "redundancy"
            )
        }
        when (val result = model.add(redundancy)) {
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

            is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
        }

        if (!::predicateRedundancy.isInitialized) {
            TODO("not implemented yet")
        }
        when (val result = model.add(predicateRedundancy)) {
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

            is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
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
