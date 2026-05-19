@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.framework.solver

import fuookami.ospf.kotlin.core.solver.output.SolvingStatusCallBack
import fuookami.ospf.kotlin.core.model.basic.RegistrationStatusCallBack
import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture
import kotlin.time.Duration
import fuookami.ospf.kotlin.core.model.mechanism.Constraint
import fuookami.ospf.kotlin.core.model.mechanism.Linear
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.intermediate_symbol.castLinearMetaModelForSolver
import fuookami.ospf.kotlin.core.solver.output.FeasibleSolverOutput
import fuookami.ospf.kotlin.core.solver.output.convertTo
import fuookami.ospf.kotlin.core.solver.value.IntoValue

typealias Flt64LinearMetaModel = LinearMetaModel<Flt64>
typealias Flt64FeasibleSolverOutput = FeasibleSolverOutput<Flt64>
typealias Flt64SolutionPool = List<Solution<Flt64>>

interface ColumnGenerationSolver {
    val name: String

    suspend fun solveMILP(
        name: String,
        metaModel: Flt64LinearMetaModel,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<Flt64FeasibleSolverOutput>

    suspend fun solveMILP(
        metaModel: Flt64LinearMetaModel,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): Ret<Flt64FeasibleSolverOutput> {
        val solutionAmount = options.solutionAmount
        return if (solutionAmount != null) {
            solveMILP(
                name = options.solveName(metaModel.name),
                metaModel = metaModel,
                amount = solutionAmount,
                toLogModel = options.toLogModel,
                registrationStatusCallBack = options.registrationStatusCallBack,
                solvingStatusCallBack = options.solvingStatusCallBack
            ).map { it.first }
        } else {
            solveMILP(
                name = options.solveName(metaModel.name),
                metaModel = metaModel,
                toLogModel = options.toLogModel,
                registrationStatusCallBack = options.registrationStatusCallBack,
                solvingStatusCallBack = options.solvingStatusCallBack
            )
        }
    }

        fun solveMILPAsync(
        name: String,
        metaModel: Flt64LinearMetaModel,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<Flt64FeasibleSolverOutput>> {
        return frameworkAsyncScope.future {
            return@future this@ColumnGenerationSolver.solveMILP(
                name = name,
                metaModel = metaModel,
                toLogModel = toLogModel,
                registrationStatusCallBack = registrationStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack
            )
        }
    }

        fun solveMILPAsync(
        metaModel: Flt64LinearMetaModel,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): CompletableFuture<Ret<Flt64FeasibleSolverOutput>> {
        return frameworkAsyncScope.future {
            return@future this@ColumnGenerationSolver.solveMILP(
                metaModel = metaModel,
                options = options
            )
        }
    }

    suspend fun solveMILP(
        name: String,
        metaModel: Flt64LinearMetaModel,
        amount: UInt64,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<Pair<Flt64FeasibleSolverOutput, List<List<Flt64>>>> {
        return solveMILP(
            name = name,
            metaModel = metaModel,
            toLogModel = toLogModel,
            registrationStatusCallBack = registrationStatusCallBack,
            solvingStatusCallBack = solvingStatusCallBack
        )
            .map { Pair(it, listOf(it.solution)) }
    }

    suspend fun solveMILPWithSolutionPool(
        metaModel: Flt64LinearMetaModel,
        options: FrameworkSolveOptions
    ): Ret<Pair<Flt64FeasibleSolverOutput, List<List<Flt64>>>> {
        return solveMILP(
            name = options.solveName(metaModel.name),
            metaModel = metaModel,
            amount = options.solutionAmount ?: UInt64.one,
            toLogModel = options.toLogModel,
            registrationStatusCallBack = options.registrationStatusCallBack,
            solvingStatusCallBack = options.solvingStatusCallBack
        )
    }

        fun solveMILPAsync(
        name: String,
        metaModel: Flt64LinearMetaModel,
        amount: UInt64,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<Pair<Flt64FeasibleSolverOutput, List<List<Flt64>>>>> {
        return frameworkAsyncScope.future {
            return@future this@ColumnGenerationSolver.solveMILP(
                name = name,
                metaModel = metaModel,
                amount = amount,
                toLogModel = toLogModel,
                registrationStatusCallBack = registrationStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack
            )
        }
    }

        fun solveMILPWithSolutionPoolAsync(
        metaModel: Flt64LinearMetaModel,
        options: FrameworkSolveOptions
    ): CompletableFuture<Ret<Pair<Flt64FeasibleSolverOutput, List<List<Flt64>>>>> {
        return frameworkAsyncScope.future {
            return@future this@ColumnGenerationSolver.solveMILPWithSolutionPool(
                metaModel = metaModel,
                options = options
            )
        }
    }

    data class LPResult(
        val result: Flt64FeasibleSolverOutput,
        val dualSolution: kotlin.collections.Map<Constraint<Flt64, Linear>, Flt64>
    ) {
        val obj: Flt64 by result::obj
        val solution: List<Flt64> by result::solution
        val time: Duration by result::time
        val possibleBestObj by result::possibleBestObj
        val gap: Flt64 by result::gap
    }

    suspend fun solveLP(
        name: String,
        metaModel: Flt64LinearMetaModel,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<LPResult>

    suspend fun solveLP(
        metaModel: Flt64LinearMetaModel,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): Ret<LPResult> {
        return solveLP(
            name = options.solveName(metaModel.name),
            metaModel = metaModel,
            toLogModel = options.toLogModel,
            registrationStatusCallBack = options.registrationStatusCallBack,
            solvingStatusCallBack = options.solvingStatusCallBack
        )
    }

        fun solveLPAsync(
        name: String,
        metaModel: Flt64LinearMetaModel,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<LPResult>> {
        return frameworkAsyncScope.future {
            return@future this@ColumnGenerationSolver.solveLP(
                name = name,
                metaModel = metaModel,
                toLogModel = toLogModel,
                registrationStatusCallBack = registrationStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack
            )
        }
    }

        fun solveLPAsync(
        metaModel: Flt64LinearMetaModel,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): CompletableFuture<Ret<LPResult>> {
        return frameworkAsyncScope.future {
            return@future this@ColumnGenerationSolver.solveLP(
                metaModel = metaModel,
                options = options
            )
        }
    }

    suspend fun <V> solveMILPV(
        name: String,
        metaModel: Flt64LinearMetaModel,
        converter: IntoValue<V>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<FeasibleSolverOutput<V>> where V : RealNumber<V>, V : NumberField<V> {
        return when (val result = solveMILP(
            name = name,
            metaModel = metaModel,
            toLogModel = toLogModel,
            registrationStatusCallBack = registrationStatusCallBack,
            solvingStatusCallBack = solvingStatusCallBack
        )) {
            is Ok -> Ok(result.value.convertTo(converter))
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    suspend fun <V> solveMILPV(
        name: String,
        metaModel: LinearMetaModel<V>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<FeasibleSolverOutput<V>> where V : RealNumber<V>, V : NumberField<V> {
        return solveMILPV(
            name = name,
            metaModel = castLinearMetaModelForSolver(metaModel),
            converter = metaModel.converter,
            toLogModel = toLogModel,
            registrationStatusCallBack = registrationStatusCallBack,
            solvingStatusCallBack = solvingStatusCallBack
        )
    }

    suspend fun <V> solveMILPV(
        metaModel: Flt64LinearMetaModel,
        converter: IntoValue<V>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): Ret<FeasibleSolverOutput<V>> where V : RealNumber<V>, V : NumberField<V> {
        return solveMILPV(
            name = options.solveName(metaModel.name),
            metaModel = metaModel,
            converter = converter,
            toLogModel = options.toLogModel,
            registrationStatusCallBack = options.registrationStatusCallBack,
            solvingStatusCallBack = options.solvingStatusCallBack
        )
    }

    suspend fun <V> solveMILPV(
        metaModel: LinearMetaModel<V>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): Ret<FeasibleSolverOutput<V>> where V : RealNumber<V>, V : NumberField<V> {
        return solveMILPV(
            name = options.solveName(metaModel.name),
            metaModel = metaModel,
            toLogModel = options.toLogModel,
            registrationStatusCallBack = options.registrationStatusCallBack,
            solvingStatusCallBack = options.solvingStatusCallBack
        )
    }

        fun <V> solveMILPVAsync(
        name: String,
        metaModel: Flt64LinearMetaModel,
        converter: IntoValue<V>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<FeasibleSolverOutput<V>>> where V : RealNumber<V>, V : NumberField<V> {
        return frameworkAsyncScope.future {
            return@future solveMILPV(
                name = name,
                metaModel = metaModel,
                converter = converter,
                toLogModel = toLogModel,
                registrationStatusCallBack = registrationStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack
            )
        }
    }

        fun <V> solveMILPVAsync(
        name: String,
        metaModel: LinearMetaModel<V>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<FeasibleSolverOutput<V>>> where V : RealNumber<V>, V : NumberField<V> {
        return frameworkAsyncScope.future {
            return@future solveMILPV(
                name = name,
                metaModel = metaModel,
                toLogModel = toLogModel,
                registrationStatusCallBack = registrationStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack
            )
        }
    }

        fun <V> solveMILPVAsync(
        metaModel: Flt64LinearMetaModel,
        converter: IntoValue<V>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): CompletableFuture<Ret<FeasibleSolverOutput<V>>> where V : RealNumber<V>, V : NumberField<V> {
        return frameworkAsyncScope.future {
            return@future solveMILPV(
                metaModel = metaModel,
                converter = converter,
                options = options
            )
        }
    }

        fun <V> solveMILPVAsync(
        metaModel: LinearMetaModel<V>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): CompletableFuture<Ret<FeasibleSolverOutput<V>>> where V : RealNumber<V>, V : NumberField<V> {
        return frameworkAsyncScope.future {
            return@future solveMILPV(
                metaModel = metaModel,
                options = options
            )
        }
    }

    suspend fun <V> solveMILPV(
        name: String,
        metaModel: Flt64LinearMetaModel,
        amount: UInt64,
        converter: IntoValue<V>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<Pair<FeasibleSolverOutput<V>, List<List<V>>>> where V : RealNumber<V>, V : NumberField<V> {
        return when (val result = solveMILP(
            name = name,
            metaModel = metaModel,
            amount = amount,
            toLogModel = toLogModel,
            registrationStatusCallBack = registrationStatusCallBack,
            solvingStatusCallBack = solvingStatusCallBack
        )) {
            is Ok -> {
                val (output, pool) = result.value
                Ok(Pair(output.convertTo(converter), pool.map { it.map(converter::intoValue) }))
            }
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    suspend fun <V> solveMILPV(
        name: String,
        metaModel: LinearMetaModel<V>,
        amount: UInt64,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<Pair<FeasibleSolverOutput<V>, List<List<V>>>> where V : RealNumber<V>, V : NumberField<V> {
        return solveMILPV(
            name = name,
            metaModel = castLinearMetaModelForSolver(metaModel),
            amount = amount,
            converter = metaModel.converter,
            toLogModel = toLogModel,
            registrationStatusCallBack = registrationStatusCallBack,
            solvingStatusCallBack = solvingStatusCallBack
        )
    }

        fun <V> solveMILPVAsync(
        name: String,
        metaModel: Flt64LinearMetaModel,
        amount: UInt64,
        converter: IntoValue<V>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<Pair<FeasibleSolverOutput<V>, List<List<V>>>>> where V : RealNumber<V>, V : NumberField<V> {
        return frameworkAsyncScope.future {
            return@future solveMILPV(
                name = name,
                metaModel = metaModel,
                amount = amount,
                converter = converter,
                toLogModel = toLogModel,
                registrationStatusCallBack = registrationStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack
            )
        }
    }

        fun <V> solveMILPVAsync(
        name: String,
        metaModel: LinearMetaModel<V>,
        amount: UInt64,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<Pair<FeasibleSolverOutput<V>, List<List<V>>>>> where V : RealNumber<V>, V : NumberField<V> {
        return frameworkAsyncScope.future {
            return@future solveMILPV(
                name = name,
                metaModel = metaModel,
                amount = amount,
                toLogModel = toLogModel,
                registrationStatusCallBack = registrationStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack
            )
        }
    }

    suspend fun <V> solveMILPVWithSolutionPool(
        metaModel: Flt64LinearMetaModel,
        converter: IntoValue<V>,
        options: FrameworkSolveOptions
    ): Ret<Pair<FeasibleSolverOutput<V>, List<List<V>>>> where V : RealNumber<V>, V : NumberField<V> {
        return solveMILPV(
            name = options.solveName(metaModel.name),
            metaModel = metaModel,
            amount = options.solutionAmount ?: UInt64.one,
            converter = converter,
            toLogModel = options.toLogModel,
            registrationStatusCallBack = options.registrationStatusCallBack,
            solvingStatusCallBack = options.solvingStatusCallBack
        )
    }

    suspend fun <V> solveMILPVWithSolutionPool(
        metaModel: LinearMetaModel<V>,
        options: FrameworkSolveOptions
    ): Ret<Pair<FeasibleSolverOutput<V>, List<List<V>>>> where V : RealNumber<V>, V : NumberField<V> {
        return solveMILPVWithSolutionPool(
            metaModel = castLinearMetaModelForSolver(metaModel),
            converter = metaModel.converter,
            options = options
        )
    }

        fun <V> solveMILPVWithSolutionPoolAsync(
        metaModel: Flt64LinearMetaModel,
        converter: IntoValue<V>,
        options: FrameworkSolveOptions
    ): CompletableFuture<Ret<Pair<FeasibleSolverOutput<V>, List<List<V>>>>> where V : RealNumber<V>, V : NumberField<V> {
        return frameworkAsyncScope.future {
            return@future solveMILPVWithSolutionPool(
                metaModel = metaModel,
                converter = converter,
                options = options
            )
        }
    }

        fun <V> solveMILPVWithSolutionPoolAsync(
        metaModel: LinearMetaModel<V>,
        options: FrameworkSolveOptions
    ): CompletableFuture<Ret<Pair<FeasibleSolverOutput<V>, List<List<V>>>>> where V : RealNumber<V>, V : NumberField<V> {
        return frameworkAsyncScope.future {
            return@future solveMILPVWithSolutionPool(
                metaModel = metaModel,
                options = options
            )
        }
    }

    data class LPResultV<V>(
        val result: FeasibleSolverOutput<V>,
        val dualSolution: kotlin.collections.Map<Constraint<Flt64, Linear>, Flt64>
    ) where V : RealNumber<V>, V : NumberField<V> {
        val obj: Flt64 by result::obj
        val solution: List<V> by result::solution
        val time: Duration by result::time
        val possibleBestObj by result::possibleBestObj
        val gap: Flt64 by result::gap
    }

    suspend fun <V> solveLPV(
        name: String,
        metaModel: Flt64LinearMetaModel,
        converter: IntoValue<V>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<LPResultV<V>> where V : RealNumber<V>, V : NumberField<V> {
        return when (val result = solveLP(
            name = name,
            metaModel = metaModel,
            toLogModel = toLogModel,
            registrationStatusCallBack = registrationStatusCallBack,
            solvingStatusCallBack = solvingStatusCallBack
        )) {
            is Ok -> Ok(LPResultV(result.value.result.convertTo(converter), result.value.dualSolution))
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    suspend fun <V> solveLPV(
        name: String,
        metaModel: LinearMetaModel<V>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<LPResultV<V>> where V : RealNumber<V>, V : NumberField<V> {
        return solveLPV(
            name = name,
            metaModel = castLinearMetaModelForSolver(metaModel),
            converter = metaModel.converter,
            toLogModel = toLogModel,
            registrationStatusCallBack = registrationStatusCallBack,
            solvingStatusCallBack = solvingStatusCallBack
        )
    }

    suspend fun <V> solveLPV(
        metaModel: Flt64LinearMetaModel,
        converter: IntoValue<V>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): Ret<LPResultV<V>> where V : RealNumber<V>, V : NumberField<V> {
        return solveLPV(
            name = options.solveName(metaModel.name),
            metaModel = metaModel,
            converter = converter,
            toLogModel = options.toLogModel,
            registrationStatusCallBack = options.registrationStatusCallBack,
            solvingStatusCallBack = options.solvingStatusCallBack
        )
    }

    suspend fun <V> solveLPV(
        metaModel: LinearMetaModel<V>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): Ret<LPResultV<V>> where V : RealNumber<V>, V : NumberField<V> {
        return solveLPV(
            name = options.solveName(metaModel.name),
            metaModel = metaModel,
            toLogModel = options.toLogModel,
            registrationStatusCallBack = options.registrationStatusCallBack,
            solvingStatusCallBack = options.solvingStatusCallBack
        )
    }

        fun <V> solveLPVAsync(
        name: String,
        metaModel: Flt64LinearMetaModel,
        converter: IntoValue<V>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<LPResultV<V>>> where V : RealNumber<V>, V : NumberField<V> {
        return frameworkAsyncScope.future {
            return@future solveLPV(
                name = name,
                metaModel = metaModel,
                converter = converter,
                toLogModel = toLogModel,
                registrationStatusCallBack = registrationStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack
            )
        }
    }

        fun <V> solveLPVAsync(
        name: String,
        metaModel: LinearMetaModel<V>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<LPResultV<V>>> where V : RealNumber<V>, V : NumberField<V> {
        return frameworkAsyncScope.future {
            return@future solveLPV(
                name = name,
                metaModel = metaModel,
                toLogModel = toLogModel,
                registrationStatusCallBack = registrationStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack
            )
        }
    }

        fun <V> solveLPVAsync(
        metaModel: Flt64LinearMetaModel,
        converter: IntoValue<V>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): CompletableFuture<Ret<LPResultV<V>>> where V : RealNumber<V>, V : NumberField<V> {
        return frameworkAsyncScope.future {
            return@future solveLPV(
                metaModel = metaModel,
                converter = converter,
                options = options
            )
        }
    }

        fun <V> solveLPVAsync(
        metaModel: LinearMetaModel<V>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): CompletableFuture<Ret<LPResultV<V>>> where V : RealNumber<V>, V : NumberField<V> {
        return frameworkAsyncScope.future {
            return@future solveLPV(
                metaModel = metaModel,
                options = options
            )
        }
    }
}