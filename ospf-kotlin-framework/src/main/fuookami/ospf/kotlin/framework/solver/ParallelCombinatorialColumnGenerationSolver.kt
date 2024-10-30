package fuookami.ospf.kotlin.framework.solver

import kotlinx.coroutines.*
import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.core.backend.solver.output.*

class ParallelCombinatorialColumnGenerationSolver(
    private val solvers: List<Lazy<ColumnGenerationSolver>>,
    private val mode: ParallelCombinatorialMode = ParallelCombinatorialMode.Best
): ColumnGenerationSolver {
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
        metaModel: LinearMetaModel,
        toLogModel: Boolean,
        statusCallBack: SolvingStatusCallBack?
    ): Ret<SolverOutput> {
        return when (mode) {
            ParallelCombinatorialMode.First -> {
                var result: SolverOutput? = null
                val lock = Any()
                try {
                    coroutineScope {
                        val promises = solvers.map {
                            launch(Dispatchers.Default) {
                                when (val ret = it.value.solveMILP(name, metaModel, toLogModel)) {
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
                                }
                            }
                        }
                        promises.forEach { it.join() }
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
                            val result = it.value.solveMILP(name, metaModel, toLogModel)
                            when (result) {
                                is Ok -> {
                                    logger.info { "Solver ${it.value.name} found a solution." }
                                }

                                is Failed -> {
                                    logger.warn { "Solver ${it.value.name} failed with error ${result.error.code}: ${result.error.message}" }
                                }
                            }
                            result
                        }
                    }
                    val results = promises.map { it.await() }
                    val successResults = results.mapNotNull {
                        when (it) {
                            is Ok -> {
                                it.value
                            }

                            is Failed -> {
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
        metaModel: LinearMetaModel,
        toLogModel: Boolean,
        statusCallBack: SolvingStatusCallBack?
    ): Ret<ColumnGenerationSolver.LPResult> {
        return when (mode) {
            ParallelCombinatorialMode.First -> {
                var result: ColumnGenerationSolver.LPResult? = null
                val lock = Any()
                try {
                    coroutineScope {
                        val promises = solvers.map {
                            launch(Dispatchers.Default) {
                                when (val ret = it.value.solveLP(name, metaModel, toLogModel)) {
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
                                }
                            }
                        }
                        promises.forEach { it.join() }
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
                            val result = it.value.solveLP(name, metaModel, toLogModel)
                            when (result) {
                                is Ok -> {
                                    logger.info { "Solver ${it.value.name} found a solution." }
                                }

                                is Failed -> {
                                    logger.warn { "Solver ${it.value.name} failed with error ${result.error.code}: ${result.error.message}" }
                                }
                            }
                            result
                        }
                    }
                    val results = promises.map { it.await() }
                    val successResults = results.mapNotNull {
                        when (it) {
                            is Ok -> {
                                it.value
                            }

                            is Failed -> {
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
