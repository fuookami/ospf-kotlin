package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.soft_security.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.math.*
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
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*

private val flt64Converter = object : IntoValue<Flt64> {
        override fun intoValue(value: Flt64) = value
        override val zero get() = Flt64.zero
        override val one get() = Flt64.one
        override fun fromValue(value: Flt64) = value
    }

/**
 * Models the divide-empty-loading pattern for adjacent positions to minimize empty cargo gaps.
 * 对相邻位置之间的空载分割模式建模以最小化空载间隙。
 *
 * @property positions The list of stowage positions / 装载位置列表
 * @property adjacentPositions The list of adjacent position pairs / 相邻位置对列表
 * @property load The load distribution data / 载荷分布数据
 * @property emptyBetweenCargo Intermediate symbols for empty slots between cargo / 货物之间空位的中间符号
 * @property emptyCargoBetweenCargo Intermediate symbols for empty cargo between cargo / 货物之间空货的中间符号
 * @property emptyBetweenEmptyCargo Intermediate symbols for empty slots between empty cargo / 空货之间空位的中间符号
*/
class DivideEmptyLoading(
    private val positions: List<Position>,
    internal val adjacentPositions: List<PositionPair>,
    private val load: Load
) {
    companion object {
        operator fun invoke(
            positions: List<Position>,
            load: Load
        ): DivideEmptyLoading {
            TODO("not implemented yet")
        }
    }

    lateinit var emptyBetweenCargo: LinearIntermediateSymbols1<Flt64>
    lateinit var emptyCargoBetweenCargo: LinearIntermediateSymbols1<Flt64>
    lateinit var emptyBetweenEmptyCargo: LinearIntermediateSymbols1<Flt64>

    /**
     * Registers divide-empty-loading intermediate symbols into the optimization model.
     * 将空载分割中间符号注册到优化模型中。
     *
     * @param model The linear meta model to register into / 要注册到的线性元模型
     * @return Success or failure result / 成功或失败结果
    */
    fun register(
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        if (!::emptyBetweenCargo.isInitialized) {
            emptyBetweenCargo = LinearIntermediateSymbols1<Flt64>("empty_between_cargo", Shape1(adjacentPositions.size)) { p, _ ->
                val position1 = adjacentPositions[p].first
                val position2 = adjacentPositions[p].second
                val j2 = positions.indexOf(position2)
                val loadAmount1 = load.loadAmountOf(position1) { item ->
                    !item.cargo.contains(CargoCode.Empty)
                }
                val loadAmount2 = load.loadAmount[j2]
                if (loadAmount1.range.fixedValue?.let { it eq Flt64.zero } == true) {
                    LinearExpressionSymbol(
                        Flt64.zero,
                        name = "empty_between_cargo_${position1}_${position2}"
                    )
                } else if (position2.status.stowageNeeded || position2.status.adjustmentNeeded) {
                    LinearFunctionSymbolAdapter(
                        delegate = IfFunction(
                            condition = LinearPolynomial(loadAmount1) - (LinearPolynomial(loadAmount2) + Flt64.one),
                            converter = flt64Converter,
                            name = "empty_between_cargo_${position1}_${position2}"
                        ),
                        converter = flt64Converter
                    )
                } else if (loadAmount2.range.fixedValue?.let { it eq Flt64.zero } == true) {
                    LinearExpressionSymbol(
                        loadAmount1,
                        Flt64,
                        name = "empty_between_cargo_${position1}_${position2}"
                    )
                } else {
                    LinearExpressionSymbol(
                        Flt64.zero,
                        name = "empty_between_cargo_${position1}_${position2}"
                    )
                }
            }
        }
        when (val result = model.add(emptyBetweenCargo)) {
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
        }

        if (!::emptyCargoBetweenCargo.isInitialized) {
            emptyCargoBetweenCargo = LinearIntermediateSymbols1<Flt64>("empty_cargo_between_cargo", Shape1(adjacentPositions.size)) { p, _ ->
                val position1 = adjacentPositions[p].first
                val position2 = adjacentPositions[p].second

                val loadAmount1 = load.loadAmountOf(position1) { item ->
                    item.cargo.contains(CargoCode.Empty)
                }
                val loadAmount2 = load.loadAmountOf(position2) { item ->
                    !item.cargo.contains(CargoCode.Empty)
                }

                if (loadAmount1.range.fixedValue?.let { it eq Flt64.zero } == true) {
                    LinearExpressionSymbol(
                        Flt64.zero,
                        name = "empty_cargo_between_cargo_${position1}_${position2}"
                    )
                } else if (position2.status.stowageNeeded || position2.status.adjustmentNeeded) {
                    LinearFunctionSymbolAdapter(
                        delegate = IfFunction(
                            condition = (LinearPolynomial(loadAmount1) + LinearPolynomial(loadAmount2)) - Flt64.two,
                            converter = flt64Converter,
                            name = "empty_cargo_between_cargo_${position1}_${position2}"
                        ),
                        converter = flt64Converter
                    )
                } else if (loadAmount2.range.fixedValue?.let { it eq Flt64.zero } == true) {
                    LinearExpressionSymbol(
                        loadAmount1,
                        Flt64,
                        name = "empty_cargo_between_cargo_${position1}_${position2}"
                    )
                } else {
                    LinearExpressionSymbol(
                        Flt64.zero,
                        name = "empty_cargo_between_cargo_${position1}_${position2}"
                    )
                }
            }
        }
        when (val result = model.add(emptyCargoBetweenCargo)) {
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
        }

        if (!::emptyBetweenEmptyCargo.isInitialized) {
            emptyBetweenEmptyCargo = LinearIntermediateSymbols1<Flt64>("empty_between_empty_cargo", Shape1(adjacentPositions.size)) { p, _ ->
                val position1 = adjacentPositions[p].first
                val j1 = positions.indexOf(position1)
                val position2 = adjacentPositions[p].second
                val j2 = positions.indexOf(position2)
                val loadAmount1 = load.loadAmountOf(position1) { item ->
                    item.cargo.contains(CargoCode.Empty)
                }
                val loadAmount2 = load.loadAmount[j2]
                if (loadAmount1.range.fixedValue?.let { it eq Flt64.zero } == true) {
                    LinearExpressionSymbol(
                        Flt64.zero,
                        name = "empty_between_empty_cargo_${position1}_${position2}"
                    )
                } else if (position2.status.stowageNeeded || position2.status.adjustmentNeeded) {
                    LinearFunctionSymbolAdapter(
                        delegate = IfFunction(
                            condition = LinearPolynomial(loadAmount1) - (LinearPolynomial(loadAmount2) + Flt64.one),
                            converter = flt64Converter,
                            name = "empty_between_empty_cargo_${position1}_${position2}"
                        ),
                        converter = flt64Converter
                    )
                } else if (loadAmount2.range.fixedValue?.let { it eq Flt64.zero } == true) {
                    LinearExpressionSymbol(
                        loadAmount1,
                        Flt64,
                        name = "empty_between_empty_cargo_${position1}_${position2}"
                    )
                } else {
                    LinearExpressionSymbol(
                        Flt64.zero,
                        name = "empty_between_empty_cargo_${position1}_${position2}"
                    )
                }
            }
        }
        when (val result = model.add(emptyBetweenEmptyCargo)) {
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

