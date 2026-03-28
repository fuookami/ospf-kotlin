package fuookami.ospf.kotlin.framework.solver

import fuookami.ospf.kotlin.core.backend.intermediate_model.LinearTriadModelView
import fuookami.ospf.kotlin.core.backend.solver.AbstractLinearSolver
import fuookami.ospf.kotlin.core.backend.solver.output.FeasibleSolverOutput
import fuookami.ospf.kotlin.core.backend.solver.output.SolvingStatus
import fuookami.ospf.kotlin.core.backend.solver.output.SolvingStatusCallBack
import fuookami.ospf.kotlin.core.frontend.model.Solution
import fuookami.ospf.kotlin.core.frontend.model.mechanism.ObjectCategory
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.math.algebra.number.UInt64
import kotlinx.coroutines.*
import org.apache.logging.log4j.kotlin.logger

class ParallelCombinatorialLinearSolver(
    private val solvers: List<Lazy<AbstractLinearSolver>>,
    private val mode: ParallelCombinatorialMode = ParallelCombinatorialMode.Best
) : AbstractLinearSolver {
    private val logger = logger()

    companion object {
        @JvmName("constructBySolvers")
        operator fun invoke(
            solvers: Iterable<AbstractLinearSolver>,
            mode: ParallelCombinatorialMode = ParallelCombinatorialMode.Best
        ): ParallelCombinatorialLinearSolver {
            return ParallelCombinatorialLinearSolver(solvers.map { lazy { it } }, mode)
        }

        @JvmName("constructBySolverExtractors")
        operator fun invoke(
            solvers: Iterable<() -> AbstractLinearSolver>,
            mode: ParallelCombinatorialMode = ParallelCombinatorialMode.Best
        ): ParallelCombinatorialLinearSolver {
            return ParallelCombinatorialLinearSolver(solvers.map { lazy { it() } }, mode)
        }
    }

    override val name by lazy { "ParallelCombinatorial(${solvers.joinToString(",") { it.value.name }})" }

    override suspend fun invoke(
        model: LinearTriadModelView,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<FeasibleSolverOutput> {
        var bestStatus: SolvingStatus? = null
        val lock = Any()

        return when (mode) {
            ParallelCombinatorialMode.First -> {
                var result: FeasibleSolverOutput? = null
                try {
                    coroutineScope {
                        val promises = solvers.mapIndexed { i, solver ->
                            launch(Dispatchers.Default) {
                                when (val ret = solver.value.invoke(model, solvingStatusCallBack?.let {
                                    { status ->
                                        synchronized(lock) {
                                            if (bestStatus == null) {
                                                bestStatus = status.copy(solver = solver.value.name, solverIndex = UInt64(i))
                                                it(bestStatus!!)
                                            } else if (model.objective.category == ObjectCategory.Maximum) {
                                                if (status.obj ls bestStatus!!.obj) {
                                                    bestStatus = status.copy(solver = solver.value.name, solverIndex = UInt64(i))
                                                    it(bestStatus!!)
                                                } else {
                                                    ok
                                                }
                                            } else {
                                                if (status.obj gr bestStatus!!.obj) {
                                                    bestStatus = status.copy(solver = solver.value.name, solverIndex = UInt64(i))
                                                    it(bestStatus!!)
                                                } else {
                                                    ok
                                                }
                                            }
                                        }
                                    }
                                })) {
                                    is Ok -> {
                                        logger.info { "Solver ${solver.value.name} found a solution." }
                                        synchronized(lock) {
                                            result = ret.value
                                            cancel()
                                        }
                                    }

                                    is Failed -> {
                                        logger.warn { "Solver ${solver.value.name} failed with error ${ret.error.code}: ${ret.error.message}" }
                                    }

                                    is Fatal -> {
                                        logger.error { "Solver ${solver.value.name} fatal: ${ret.errors.joinToString { it.message }}" }
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
                    val promises = solvers.mapIndexed { i, solver ->
                        async(Dispatchers.Default) {
                            val result = solver.value.invoke(model, solvingStatusCallBack?.let {
                                { status ->
                                    synchronized(lock) {
                                        if (bestStatus == null) {
                                            bestStatus = status.copy(solver = solver.value.name, solverIndex = UInt64(i))
                                            it(bestStatus!!)
                                        } else if (model.objective.category == ObjectCategory.Maximum) {
                                            if (status.obj ls bestStatus!!.obj) {
                                                bestStatus = status.copy(solver = solver.value.name, solverIndex = UInt64(i))
                                                it(bestStatus!!)
                                            } else {
                                                ok
                                            }
                                        } else {
                                            if (status.obj gr bestStatus!!.obj) {
                                                bestStatus = status.copy(solver = solver.value.name, solverIndex = UInt64(i))
                                                it(bestStatus!!)
                                            } else {
                                                ok
                                            }
                                        }
                                    }
                                }
                            })
                            when (result) {
                                is Ok -> {
                                    logger.info { "Solver ${solver.value.name} found a solution." }
                                }

                                is Failed -> {
                                    logger.warn { "Solver ${solver.value.name} failed with error ${result.error.code}: ${result.error.message}" }
                                }

                                is Fatal -> {
                                    logger.error { "Solver ${solver.value.name} fatal: ${result.errors.joinToString { it.message }}" }
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
                        val bestResult = when (model.objective.category) {
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

    override suspend fun invoke(
        model: LinearTriadModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<Pair<FeasibleSolverOutput, List<Solution>>> {
        var bestStatus: SolvingStatus? = null
        val lock = Any()

        return when (mode) {
            ParallelCombinatorialMode.First -> {
                var result: Pair<FeasibleSolverOutput, List<Solution>>? = null
                try {
                    coroutineScope {
                        val promises = solvers.mapIndexed { i, solver ->
                            launch(Dispatchers.Default) {
                                when (val ret = solver.value.invoke(model, solutionAmount, solvingStatusCallBack?.let {
                                    { status ->
                                        synchronized(lock) {
                                            if (bestStatus == null) {
                                                bestStatus = status.copy(solver = solver.value.name, solverIndex = UInt64(i))
                                                it(bestStatus!!)
                                            } else if (model.objective.category == ObjectCategory.Maximum) {
                                                if (status.obj ls bestStatus!!.obj) {
                                                    bestStatus = status.copy(solver = solver.value.name, solverIndex = UInt64(i))
                                                    it(bestStatus!!)
                                                } else {
                                                    ok
                                                }
                                            } else {
                                                if (status.obj gr bestStatus!!.obj) {
                                                    bestStatus = status.copy(solver = solver.value.name, solverIndex = UInt64(i))
                                                    it(bestStatus!!)
                                                } else {
                                                    ok
                                                }
                                            }
                                        }
                                    }
                                })) {
                                    is Ok -> {
                                        logger.info { "Solver ${solver.value.name} found a solution." }
                                        synchronized(lock) {
                                            result = ret.value
                                            cancel()
                                        }
                                    }

                                    is Failed -> {
                                        logger.warn { "Solver ${solver.value.name} failed with error ${ret.error.code}: ${ret.error.message}" }
                                    }

                                    is Fatal -> {
                                        logger.error { "Solver ${solver.value.name} fatal: ${ret.errors.joinToString { it.message }}" }
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
                    val promises = solvers.mapIndexed { i, solver ->
                        async(Dispatchers.Default) {
                            val result = solver.value.invoke(model, solutionAmount, solvingStatusCallBack?.let {
                                { status ->
                                    synchronized(lock) {
                                        if (bestStatus == null) {
                                            bestStatus = status.copy(solver = solver.value.name, solverIndex = UInt64(i))
                                            it(bestStatus!!)
                                        } else if (model.objective.category == ObjectCategory.Maximum) {
                                            if (status.obj ls bestStatus!!.obj) {
                                                bestStatus = status.copy(solver = solver.value.name, solverIndex = UInt64(i))
                                                it(bestStatus!!)
                                            } else {
                                                ok
                                            }
                                        } else {
                                            if (status.obj gr bestStatus!!.obj) {
                                                bestStatus = status.copy(solver = solver.value.name, solverIndex = UInt64(i))
                                                it(bestStatus!!)
                                            } else {
                                                ok
                                            }
                                        }
                                    }
                                }
                            })
                            when (result) {
                                is Ok -> {
                                    logger.info { "Solver ${solver.value.name} found a solution." }
                                }

                                is Failed -> {
                                    logger.warn { "Solver ${solver.value.name} failed with error ${result.error.code}: ${result.error.message}" }
                                }

                                is Fatal -> {
                                    logger.error { "Solver ${solver.value.name} fatal: ${result.errors.joinToString { it.message }}" }
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
                        val bestResult = when (model.objective.category) {
                            ObjectCategory.Minimum -> {
                                successResults.minBy { it.first.obj }
                            }

                            ObjectCategory.Maximum -> {
                                successResults.maxBy { it.first.obj }
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



