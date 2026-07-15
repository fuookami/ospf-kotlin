package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.loading_effectiveness.model

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
 * Models trailer-based loading constraints including trailer change and trailer circling.
 * 建模基于拖车的装载约束，包括拖车更换和拖车环绕。
 *
 * @property items The list of cargo items. / 货物项列表
 * @property positions The list of stowage positions. / 配载位置列表
 * @property trailers The list of trailers. / 拖车列表
 * @property orderedItemsInTrailers The list of ordered item pairs within the same trailer. / 同一拖车内的有序货物项对列表
 * @property adjacentPositions The list of adjacent position pairs. / 相邻位置对列表
 * @property orderedTrailers The list of ordered trailer pairs. / 有序拖车对列表
 * @property stowage The stowage assignment model. / 配载分配模型
 * @property load The load model for cargo assignment. / 货物分配的装载模型
*/
class TrailerLoading(
    private val items: List<Item>,
    private val positions: List<Position>,
    private val trailers: List<Trailer>,
    internal val orderedItemsInTrailers: List<ItemPair>,
    private val adjacentPositions: List<PositionPair>,
    internal val orderedTrailers: List<Pair<Trailer, Trailer>>,
    private val stowage: Stowage,
    private val load: Load
) {
    companion object {
        operator fun invoke(
            items: List<Item>,
            positions: List<Position>,
            trailers: List<Trailer>,
            adjacentPositions: List<PositionPair>,
            stowage: Stowage,
            load: Load
        ): TrailerLoading {
            TODO("not implemented yet")
        }
    }

    lateinit var trailerChange: LinearIntermediateSymbols2<Flt64>
    lateinit var trailerCircling: LinearIntermediateSymbols2<Flt64>

    /**
     * Registers the trailer change and trailer circling intermediate symbols into the optimization model.
     * 将拖车更换和拖车环绕中间符号注册到优化模型中。
     *
     * @param model The linear meta model to register into. / 要注册到的线性元模型
     * @return The result of the registration operation. / 注册操作的结果
    */
    fun register(
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        if (!::trailerChange.isInitialized) {
            trailerChange = LinearIntermediateSymbols2<Flt64>("trailer_change", Shape2(orderedTrailers.size, adjacentPositions.size)) { _, v ->
                val (trailer1, trailer2) = orderedTrailers[v[0]]
                val (position1, position2) = adjacentPositions[v[1]]

                val loadAmount1 = load.loadAmountOf(position1) { item ->
                    item in trailer2.items
                }
                val loadAmount2 = load.loadAmountOf(position2) { item ->
                    item in trailer1.items
                }

                LinearFunctionSymbolAdapter(
                    delegate = IfFunction(
                        condition = loadAmount1 + loadAmount2 - Flt64.two,
                        converter = flt64Converter,
                        name = "trailer_change_${trailer1}_${trailer2}_${position1}_${position2}"
                    ),
                    converter = flt64Converter
                )
            }
        }
        when (val result = model.add(trailerChange)) {
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
        }

        if (!::trailerCircling.isInitialized) {
            trailerCircling = LinearIntermediateSymbols2<Flt64>("trailer_circling", Shape2(orderedItemsInTrailers.size, adjacentPositions.size)) { _, v ->
                val (item1, item2) = orderedItemsInTrailers[v[0]]
                val i1 = items.indexOf(item1)
                val i2 = items.indexOf(item2)
                val (position1, position2) = adjacentPositions[v[1]]
                val j1 = positions.indexOf(position1)
                val j2 = positions.indexOf(position2)

                if (Stowage.stowageNeeded(item2, position1) && Stowage.stowageNeeded(item1, position2)) {
                    LinearFunctionSymbolAdapter(
                        delegate = IfFunction(
                            condition = stowage.stowage[i2, j1] + stowage.stowage[i1, j2] - Flt64.two,
                            converter = flt64Converter,
                            name = "trailer_circling_${item1}_${item2}_${position1}_${position2}"
                        ),
                        converter = flt64Converter
                    )
                } else {
                    LinearExpressionSymbol(
                        Flt64.zero,
                        name = "trailer_circling_${item1}_${item2}_${position1}_${position2}"
                    )
                }
            }
        }
        when (val result = model.add(trailerCircling)) {
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

