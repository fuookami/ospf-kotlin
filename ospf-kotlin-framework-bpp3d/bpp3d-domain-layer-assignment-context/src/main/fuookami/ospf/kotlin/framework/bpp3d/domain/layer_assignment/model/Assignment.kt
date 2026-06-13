/**
 * 层分配赋值模型。
 * Layer assignment model.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model

import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.core.symbol.LinearExpressionSymbol
import fuookami.ospf.kotlin.core.symbol.LinearIntermediateSymbols1
import fuookami.ospf.kotlin.core.symbol.LinearIntermediateSymbols2
import fuookami.ospf.kotlin.core.symbol.function.BinaryzationFunction
import fuookami.ospf.kotlin.core.symbol.function.LinearFunctionSymbolAdapter
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.BinVariable1
import fuookami.ospf.kotlin.core.variable.UIntVariable1
import fuookami.ospf.kotlin.core.variable.UIntVariable2
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Item
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bin
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.sum
import fuookami.ospf.kotlin.math.symbol.polynomial.plusAssign
import fuookami.ospf.kotlin.multiarray.Shape1
import fuookami.ospf.kotlin.multiarray.Shape2
import fuookami.ospf.kotlin.multiarray._a
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.MetaModel

private val flt64Converter: IntoValue<FltX> = IntoValue.fromConverter(FltX)

/**
 * 不精确赋值，用于列生成 RMP 阶段。
 * Imprecise assignment, used for column generation RMP phase.
 *
 * @property items 货物需求映射 / item demand map
 * @property aggregation 层聚合 / layer aggregation
 * @property solverValueAdapter 求解器值适配器 / solver value adapter
 */
class ImpreciseAssignment(
    private val items: Map<Item, UInt64>,
    private val aggregation: LayerAggregation,
    private val solverValueAdapter: Bpp3dSolverValueAdapter = DefaultBpp3dSolverValueAdapter
) {
    val layers: List<BinLayer> by aggregation::layers
    val lastIterationLayers: List<BinLayer> by aggregation::lastIterationLayers

    private val _x = ArrayList<UIntVariable1>()
    val x: List<UIntVariable1> by ::_x

    /** 体积符号 / volume symbol */
    lateinit var volume: LinearExpressionSymbol<FltX>

    /**
     * 注册赋值符号到模型。
     * Register assignment symbols to model.
     *
     * @param model 元模型 / meta model
     * @return 注册结果 / registration result
     */
    fun register(model: MetaModel<FltX>): Try {
        if (!::volume.isInitialized) {
            volume = LinearExpressionSymbol(FltX.zero, name = "volume")
        }
        when (val result = model.add(volume)) {
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

    /**
     * 添加新列到赋值模型。
     * Add new columns to assignment model.
     *
     * @param iteration 迭代次数 / iteration count
     * @param newLayers 新层列表 / new layer list
     * @param model 线性元模型 / linear meta model
     * @return 去重后新增的层 / deduplicated newly added layers
     */
    suspend fun addColumns(
        iteration: UInt64,
        newLayers: List<BinLayer>,
        model: AbstractLinearMetaModel<FltX>
    ): Ret<List<BinLayer>> {
        val unduplicatedLayers = aggregation.addColumns(newLayers)

        val xi = UIntVariable1("x_$iteration", Shape1(unduplicatedLayers.size))
        for (layer in unduplicatedLayers) {
            xi[layer].name = "${xi.name}_${layer.index}"
            val maxAmount = items.minOfOrNull {
                val amount = layer.amount(it.key)
                if (amount == UInt64.zero) {
                    UInt64.maximum
                } else {
                    it.value / amount + UInt64.one
                }
            }
            maxAmount?.let { xi[layer].range.leq(it) }
        }
        _x.add(xi)
        when (val result = model.add(xi)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        if (unduplicatedLayers.isEmpty()) {
            return Ok(emptyList())
        }

        volume.flush()
        volume.asMutable() += sum(unduplicatedLayers.map {
            LinearMonomial(solverValueAdapter.volumeToSolver(it.volume), xi[it])
        })

        return Ok(unduplicatedLayers)
    }
}

/**
 * 精确赋值，用于最终 MILP 求解阶段。
 * Precise assignment, used for final MILP solving phase.
 *
 * @property bins 箱子列表 / bin list
 * @property layers 层列表 / layer list
 */
class PreciseAssignment(
    private val bins: List<Bin<BinLayer, FltX>>,
    private val layers: List<BinLayer>
) {
    /** 赋值变量矩阵 / assignment variable matrix */
    lateinit var x: UIntVariable2

    /** 二值化中间符号 / binary intermediate symbols */
    lateinit var u: LinearIntermediateSymbols2<FltX>
    /** 箱子使用符号 / bin usage symbols */
    lateinit var v: LinearIntermediateSymbols1<FltX>
    /** 尾箱标记变量 / tail bin marker variable */
    lateinit var tail: BinVariable1

    /**
     * 注册赋值符号到模型。
     * Register assignment symbols to model.
     *
     * @param model 元模型 / meta model
     * @return 注册结果 / registration result
     */
    fun register(model: MetaModel<FltX>): Try {
        if (!::x.isInitialized) {
            x = UIntVariable2(
                "x",
                Shape2(bins.size, layers.size)
            )
            for ((i, bin) in bins.withIndex()) {
                for ((j, layer) in layers.withIndex()) {
                    if (layer.bin != bin.type || !bin.enabled(layer)) {
                        x[i, j].range.eq(UInt64.zero)
                    } else {
                        x[i, j].range.leq((bin.depth / layer.depth).ceil().toUInt64())
                    }
                }
            }
        }
        for ((i, bin) in bins.withIndex()) {
            for ((j, layer) in layers.withIndex()) {
                if (layer.bin == bin.type && bin.enabled(layer)) {
                    when (val result = model.add(x[i, j])) {
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
        }

        if (!::u.isInitialized) {
            u = LinearIntermediateSymbols2<FltX>(
                name = "u",
                shape = Shape2(bins.size, layers.size)
            ) { _, v ->
                LinearFunctionSymbolAdapter(
                    delegate = BinaryzationFunction(
                        polynomial = LinearMonomial(FltX.one, x[v[0], v[1]]).toLinearPolynomial(),
                        converter = flt64Converter,
                        name = "u_$v",
                    ),
                    converter = flt64Converter
                )
            }
        }
        when (val result = model.add(u)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        if (!::v.isInitialized) {
            v = LinearIntermediateSymbols1<FltX>(
                name = "v",
                shape = Shape1(bins.size)
            ) { i, _ ->
                LinearFunctionSymbolAdapter(
                    delegate = BinaryzationFunction(
                        polynomial = sum(x[i, _a].map { LinearMonomial(FltX.one, it) }),
                        converter = flt64Converter,
                        name = "v_$i",
                    ),
                    converter = flt64Converter
                )
            }
        }
        when (val result = model.add(v)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        if (!::tail.isInitialized) {
            tail = BinVariable1(
                "tail",
                Shape1(bins.size)
            )
        }
        when (val result = model.add(tail)) {
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



