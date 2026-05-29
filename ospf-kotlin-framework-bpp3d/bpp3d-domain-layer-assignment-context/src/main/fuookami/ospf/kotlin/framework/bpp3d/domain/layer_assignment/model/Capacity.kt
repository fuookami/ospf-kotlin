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

interface Capacity {
    val loadWeight: LinearIntermediateSymbols1<InfraNumber>
    val loadVolume: LinearIntermediateSymbols1<InfraNumber>
    val loadDepth: LinearIntermediateSymbols1<InfraNumber>

    val loadingRate: LinearIntermediateSymbols1<InfraNumber>
    val tailLoadingRate: LinearIntermediateSymbols1<InfraNumber>
}

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



