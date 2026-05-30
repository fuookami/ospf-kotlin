/**
 * 层分配容量模型。
 * Layer assignment capacity model.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model

import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraNumber
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.div
import fuookami.ospf.kotlin.math.symbol.polynomial.sum
import fuookami.ospf.kotlin.core.symbol.LinearExpressionSymbol
import fuookami.ospf.kotlin.core.symbol.LinearIntermediateSymbols1
import fuookami.ospf.kotlin.core.symbol.function.LinearFunctionSymbolAdapter
import fuookami.ospf.kotlin.core.symbol.function.MaskingFunction
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bin
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayer
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.multiarray.Shape1
import fuookami.ospf.kotlin.core.model.mechanism.MetaModel

private val flt64Converter: IntoValue<InfraNumber> = IntoValue.fromConverter(InfraNumber)

/**
 * 容量接口，提供装载重量、体积、深度及装载率的符号表达。
 * Capacity interface, provides symbolic expressions for load weight, volume, depth and loading rate.
 */
interface Capacity {
    /** 装载重量符号 / load weight symbols */
    val loadWeight: LinearIntermediateSymbols1<InfraNumber>
    /** 装载体积符号 / load volume symbols */
    val loadVolume: LinearIntermediateSymbols1<InfraNumber>
    /** 装载深度符号 / load depth symbols */
    val loadDepth: LinearIntermediateSymbols1<InfraNumber>

    /** 装载率符号 / loading rate symbols */
    val loadingRate: LinearIntermediateSymbols1<InfraNumber>
    /** 尾箱装载率符号 / tail loading rate symbols */
    val tailLoadingRate: LinearIntermediateSymbols1<InfraNumber>
}

/**
 * 精确装载容量，基于精确分配计算各箱子的容量指标。
 * Precise load capacity, computes capacity metrics per bin based on precise assignment.
 *
 * @property bins 箱子列表 / bin list
 * @property layers 层列表 / layer list
 * @property assignment 精确赋值 / precise assignment
 * @property solverValueAdapter 求解器值适配器 / solver value adapter
 */
class PreciseLoadCapacity(
    private val bins: List<Bin<BinLayer>>,
    private val layers: List<BinLayer>,
    private val assignment: PreciseAssignment,
    private val solverValueAdapter: Bpp3dSolverValueAdapter = DefaultBpp3dSolverValueAdapter
) : Capacity {
    override lateinit var loadWeight: LinearIntermediateSymbols1<InfraNumber>
    override lateinit var loadVolume: LinearIntermediateSymbols1<InfraNumber>
    override lateinit var loadDepth: LinearIntermediateSymbols1<InfraNumber>

    override lateinit var loadingRate: LinearIntermediateSymbols1<InfraNumber>
    override lateinit var tailLoadingRate: LinearIntermediateSymbols1<InfraNumber>

    /**
     * 注册容量符号到模型。
     * Register capacity symbols to model.
     *
     * @param model 元模型 / meta model
     * @return 注册结果 / registration result
     */
    fun register(model: MetaModel<InfraNumber>): Try {
        if (!::loadWeight.isInitialized) {
            loadWeight = LinearIntermediateSymbols1<InfraNumber>(
                name = "load_weight",
                shape = Shape1(bins.size)
            ) { i, _ ->
                LinearExpressionSymbol(
                    polynomial = sum(layers.mapIndexed { j, layer ->
                        LinearPolynomial(listOf(LinearMonomial(solverValueAdapter.weightToSolver(layer.weight), assignment.x[i, j])), InfraNumber.zero)
                    }),
                    name = "load_weight_${i}"
                )
            }
        }
        when (val result = model.add(loadWeight)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        if (!::loadVolume.isInitialized) {
            loadVolume = LinearIntermediateSymbols1<InfraNumber>(
                name = "load_volume",
                shape = Shape1(bins.size)
            ) { i, _ ->
                LinearExpressionSymbol(
                    polynomial = sum(layers.mapIndexed { j, layer ->
                        LinearPolynomial(listOf(LinearMonomial(solverValueAdapter.volumeToSolver(layer.volume), assignment.x[i, j])), InfraNumber.zero)
                    }),
                    name = "load_volume_${i}"
                )
            }
        }
        when (val result = model.add(loadVolume)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        if (!::loadDepth.isInitialized) {
            loadDepth = LinearIntermediateSymbols1<InfraNumber>(
                name = "load_depth",
                shape = Shape1(bins.size)
            ) { i, _ ->
                LinearExpressionSymbol(
                    polynomial = sum(layers.mapIndexed { j, layer ->
                        LinearPolynomial(listOf(LinearMonomial(solverValueAdapter.depthToSolver(layer.depth), assignment.x[i, j])), InfraNumber.zero)
                    }),
                    name = "load_depth_${i}"
                )
            }
        }
        when (val result = model.add(loadDepth)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        if (!::loadingRate.isInitialized) {
            loadingRate = LinearIntermediateSymbols1<InfraNumber>(
                name = "loading_rate",
                shape = Shape1(bins.size)
            ) { i, _ ->
                LinearExpressionSymbol(
                    polynomial = sum(layers.mapIndexed { j, layer ->
                        LinearPolynomial(listOf(LinearMonomial(solverValueAdapter.volumeToSolver(layer.volume), assignment.x[i, j])), InfraNumber.zero)
                    }) / solverValueAdapter.volumeToSolver(bins[i].volume),
                    name = "loading_rate_${i}"
                )
            }
        }
        when (val result = model.add(loadingRate)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        if (!::tailLoadingRate.isInitialized) {
            tailLoadingRate = LinearIntermediateSymbols1<InfraNumber>(
                name = "tail_loading_rate",
                shape = Shape1(bins.size)
            ) { i, _ ->
                LinearFunctionSymbolAdapter(
                    delegate = MaskingFunction(
                        input = loadingRate[i].toLinearPolynomial(),
                        mask = assignment.tail[i],
                        converter = flt64Converter,
                        name = "tail_loading_rate_${i}"
                    ),
                    converter = flt64Converter
                )
            }
        }
        when (val result = model.add(tailLoadingRate)) {
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



