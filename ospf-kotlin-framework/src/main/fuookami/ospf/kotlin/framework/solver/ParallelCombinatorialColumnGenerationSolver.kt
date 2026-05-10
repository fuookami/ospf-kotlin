package fuookami.ospf.kotlin.framework.solver

import fuookami.ospf.kotlin.core.solver.output.SolvingStatusCallBack
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.basic.RegistrationStatusCallBack
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import kotlinx.coroutines.*
import org.apache.logging.log4j.kotlin.logger
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.solver.output.FeasibleSolverOutput
import fuookami.ospf.kotlin.math.algebra.number.Flt64

class ParallelCombinatorialColumnGenerationSolver(
    private val solvers: List<Lazy<ColumnGenerationSolver>>,
    private val mode: ParallelCombinatorialMode = ParallelCombinatorialMode.Best
) : ColumnGenerationSolver {
    private val logger = logger()

    companion object {
        @JvmName("constructBySolvers")
        operator fun invoke(
            solvers: Iterable<ColumnGenerationSolver>,
            mode: ParallelCombinatorialMode = ParallelCombinatorialMode.Best
        ): ParallelCombinatorialColumnGenerationSolver {
            return ParallelCombinatorialColumnGenerationSolver(solvers.map { lazy { it } }, mode)
        }

        @JvmName("constructBySolverExtractors")
        operator fun invoke(
            solvers: List<() -> ColumnGenerationSolver>,
            mode: ParallelCombinatorialMode = ParallelCombinatorialMode.Best
        ): ParallelCombinatorialColumnGenerationSolver {
            return ParallelCombinatorialColumnGenerationSolver(solvers.map { lazy { it() } }, mode)
        }
    }

    override val name by lazy { "ParallelCombinatorial(${solvers.joinToString(",") { it.value.name }})" }

    override suspend fun solveMILP(
        name: String,
        metaModel: LinearMetaModel<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
        toLogModel: Boolean,
        registrationStatusCallBack: RegistrationStatusCallBack?,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<FeasibleSolverOutput<fuookami.ospf.kotlin.math.algebra.number.Flt64>> {
        return when (mode) {
            ParallelCombinatorialMode.First -> {
                var result: FeasibleSolverOutput<fuookami.ospf.kotlin.math.algebra.number.Flt64>? = null
                val lock = Any()
                try {
                    coroutineScope {
                        val promises = solvers.map {
                            launch(Dispatchers.Default) {
                                when (val ret = it.value.solveMILP(
                                    name = name,
                                    metaModel = metaModel,
                                    toLogModel = toLogModel
                                )) {
                                    is Ok -> {
                                        logger.info { "Solver ${it.value.name} found a solution." }
                                        synchronized(lock) {
                                            result = ret.value
                                            cancel()
                                        }
                                    }

                                    is Failed -> {
                                        logger.warn { "Solver ${it.value.name} failed with error ${ret.error.code}: ${ret.error.message}" }
                                    }

                                    is Fatal -> {
                                        logger.error { "Solver ${it.value.name} fatal: ${ret.errors.joinToString { it.message }}" }
                                    }
                                }
                            }
                        }
                        promises.joinAll()
                        if (result != null) {
                            Ok(result!!)
                        } else {
                            Failed(ErrorCode.SolverNotFound, "No solver valid.")
                        }
                    }
                } catch (e: Exception) {
                    if (result != null) {
                        Ok(result!!)
                    } else {
                        Failed(ErrorCode.OREngineSolvingException)
                    }
                }
            }

            ParallelCombinatorialMode.Best -> {
                coroutineScope {
                    val promises = solvers.map {
                        async(Dispatchers.Default) {
                            val result = it.value.solveMILP(
                                name = name,
                                metaModel = metaModel,
                                toLogModel = toLogModel
                            )
                            when (result) {
                                is Ok -> {
                                    logger.info { "Solver ${it.value.name} found a solution." }
                                }

                                is Failed -> {
                                    logger.warn { "Solver ${it.value.name} failed with error ${result.error.code}: ${result.error.message}" }
                                }

                                is Fatal -> {
                                    logger.error { "Solver ${it.value.name} fatal: ${result.errors.joinToString { it.message }}" }
                                }
                            }
                            result
                        }
                    }
                    val results = promises.awaitAll()
                    val successResults = results.mapNotNull {
                        when (it) {
                            is Ok -> {
                                it.value
                            }

                            is Failed -> {
                                null
                            }

                            is Fatal -> {
                                null
                            }
                        }
                    }
                    if (successResults.isNotEmpty()) {
                        val bestResult = when (metaModel.objectCategory) {
                            ObjectCategory.Minimum -> {
                                successResults.minBy { it.obj }
                            }

                            ObjectCategory.Maximum -> {
                                successResults.maxBy { it.obj }
                            }
                        }
                        Ok(bestResult)
                    } else {
                        Failed(ErrorCode.SolverNotFound, "No solver valid.")
                    }
                }
            }
        }
    }

    override suspend fun solveLP(
        name: String,
        metaModel: LinearMetaModel<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
        toLogModel: Boolean,
        registrationStatusCallBack: RegistrationStatusCallBack?,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<ColumnGenerationSolver.LPResult> {
        return when (mode) {
            ParallelCombinatorialMode.First -> {
                var result: ColumnGenerationSolver.LPResult? = null
                val lock = Any()
                try {
                    coroutineScope {
                        val promises = solvers.map {
                            launch(Dispatchers.Default) {
                                when (val ret = it.value.solveLP(
                                    name = name,
                                    metaModel = metaModel,
                                    toLogModel = toLogModel
                                )) {
                                    is Ok -> {
                                        logger.info { "Solver ${it.value.name} found a solution." }
                                        synchronized(lock) {
                                            result = ret.value
                                            cancel()
                                        }
                                    }

                                    is Failed -> {
                                        logger.warn { "Solver ${it.value.name} failed with error ${ret.error.code}: ${ret.error.message}" }
                                    }

                                    is Fatal -> {
                                        logger.error { "Solver ${it.value.name} fatal: ${ret.errors.joinToString { it.message }}" }
                                    }
                                }
                            }
                        }
                        promises.joinAll()
                        if (result != null) {
                            Ok(result!!)
                        } else {
                            Failed(ErrorCode.SolverNotFound, "No solver valid.")
                        }
                    }
                } catch (e: Exception) {
                    if (result != null) {
                        Ok(result!!)
                    } else {
                        Failed(ErrorCode.OREngineSolvingException)
                    }
                }
            }

            ParallelCombinatorialMode.Best -> {
                coroutineScope {
                    val promises = solvers.map {
                        async(Dispatchers.Default) {
                            val result = it.value.solveLP(
                                name = name,
                                metaModel = metaModel,
                                toLogModel = toLogModel
                            )
                            when (result) {
                                is Ok -> {
                                    logger.info { "Solver ${it.value.name} found a solution." }
                                }

                                is Failed -> {
                                    logger.warn { "Solver ${it.value.name} failed with error ${result.error.code}: ${result.error.message}" }
                                }

                                is Fatal -> {
                                    logger.error { "Solver ${it.value.name} fatal: ${result.errors.joinToString { it.message }}" }
                                }
                            }
                            result
                        }
                    }
                    val results = promises.awaitAll()
                    val successResults = results.mapNotNull {
                        when (it) {
                            is Ok -> {
                                it.value
                            }

                            is Failed -> {
                                null
                            }

                            is Fatal -> {
                                null
                            }
                        }
                    }
                    if (successResults.isNotEmpty()) {
                        val bestResult = when (metaModel.objectCategory) {
                            ObjectCategory.Minimum -> {
                                successResults.minBy { it.obj }
                            }

                            ObjectCategory.Maximum -> {
                                successResults.maxBy { it.obj }
                            }
                        }
                        Ok(bestResult)
                    } else {
                        Failed(ErrorCode.SolverNotFound, "No solver valid.")
                    }
                }
            }
        }
    }
}
