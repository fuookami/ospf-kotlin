@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.framework.solver

import fuookami.ospf.kotlin.core.model.basic.RegistrationStatusCallBack
import fuookami.ospf.kotlin.core.model.mechanism.Constraint
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.math.symbol.Quadratic
import fuookami.ospf.kotlin.core.model.mechanism.QuadraticMetaModel
import fuookami.ospf.kotlin.core.solver.output.convertTo
import fuookami.ospf.kotlin.core.solver.output.FeasibleSolverOutput
import fuookami.ospf.kotlin.core.solver.output.SolverOutput
import fuookami.ospf.kotlin.core.solver.output.SolvingStatusCallBack
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import java.util.concurrent.CompletableFuture
import kotlin.time.Duration
import kotlinx.coroutines.future.future

private fun <V> LinearInequality<Flt64>.convertTo(converter: IntoValue<V>): LinearInequality<V>
        where V : RealNumber<V>, V : NumberField<V> {
    val lhs = LinearPolynomial(
        monomials = this.lhs.monomials.map { LinearMonomial(converter.intoValue(it.coefficient), it.symbol) },
        constant = converter.intoValue(this.lhs.constant)
    )
    val rhs = LinearPolynomial(
        monomials = this.rhs.monomials.map { LinearMonomial(converter.intoValue(it.coefficient), it.symbol) },
        constant = converter.intoValue(this.rhs.constant)
    )
    return LinearInequality(lhs, rhs, comparison)
}

private fun <V> QuadraticInequalityOf<Flt64>.convertTo(converter: IntoValue<V>): QuadraticInequalityOf<V>
        where V : RealNumber<V>, V : NumberField<V> {
    val lhs = QuadraticPolynomial(
        monomials = this.lhs.monomials.map { QuadraticMonomial(converter.intoValue(it.coefficient), it.symbol1, it.symbol2) },
        constant = converter.intoValue(this.lhs.constant)
    )
    val rhs = QuadraticPolynomial(
        monomials = this.rhs.monomials.map { QuadraticMonomial(converter.intoValue(it.coefficient), it.symbol1, it.symbol2) },
        constant = converter.intoValue(this.rhs.constant)
    )
    return QuadraticInequalityOf(lhs, rhs, comparison)
}

@Suppress("DEPRECATION")
private fun <V> SolverOutput.convertTo(converter: IntoValue<V>): SolverOutput
        where V : RealNumber<V>, V : NumberField<V> {
    return when (this) {
        is FeasibleSolverOutput<*> -> {
            val converted = solution.map { value ->
                if (value is Flt64) {
                    converter.intoValue(value)
                } else {
                    return this
                }
            }
            FeasibleSolverOutput(
                obj = obj,
                solution = converted,
                time = time,
                possibleBestObj = possibleBestObj,
                gap = gap,
                iterations = iterations,
                nodeCount = nodeCount,
                bestBound = bestBound,
                mipGap = mipGap,
                solveTime = solveTime,
                objValue = converter.intoValue(obj),
                possibleBestObjValue = converter.intoValue(possibleBestObj),
                bestBoundValue = bestBound?.let { converter.intoValue(it) }
            )
        }

        else -> this
    }
}

interface LinearBendersDecompositionSolver {
    val name: String

    suspend fun solveMaster(
        name: String,
        metaModel: LinearMetaModel<Flt64>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<SolverOutput>

    suspend fun solveMaster(
        metaModel: LinearMetaModel<Flt64>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): Ret<SolverOutput> {
        return solveMaster(
            name = options.solveName(metaModel.name),
            metaModel = metaModel,
            toLogModel = options.toLogModel,
            registrationStatusCallBack = options.registrationStatusCallBack,
            solvingStatusCallBack = options.solvingStatusCallBack
        )
    }

        fun solveMasterAsync(
        name: String,
        metaModel: LinearMetaModel<Flt64>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<SolverOutput>> {
        return frameworkAsyncScope.future {
            return@future this@LinearBendersDecompositionSolver.solveMaster(
                name = name,
                metaModel = metaModel,
                toLogModel = toLogModel,
                registrationStatusCallBack = registrationStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack
            )
        }
    }

        fun solveMasterAsync(
        metaModel: LinearMetaModel<Flt64>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): CompletableFuture<Ret<SolverOutput>> {
        return frameworkAsyncScope.future {
            return@future this@LinearBendersDecompositionSolver.solveMaster(
                metaModel = metaModel,
                options = options
            )
        }
    }

    /**
     * Sub Problem Solving Result (with cuts if fixed variables are provided)
     *
     * @property cuts           list of cuts generated by sub problem, if fixed variables are provided.
     * Otherwise, there should not be any fixed variables in the sub problem model. In other words, the fixed variables should be replaced with their values.
     */
    sealed interface LinearSubResult {
        val cuts: List<LinearInequality<Flt64>>?
    }

    data class LinearFeasibleResult(
        val result: FeasibleSolverOutput<Flt64>,
        val dualSolution: kotlin.collections.Map<Constraint<Flt64, Linear>, Flt64>,
        override val cuts: List<LinearInequality<Flt64>>?
    ) : LinearSubResult {
        val obj: Flt64 by result::obj
        val solution: List<Flt64> by result::solution
        val time: Duration by result::time
        val possibleBestObj by result::possibleBestObj
        val gap: Flt64 by result::gap
    }

    data class LinearInfeasibleResult(
        val farkasDualSolution: kotlin.collections.Map<Constraint<Flt64, Linear>, Flt64>,
        override val cuts: List<LinearInequality<Flt64>>?
    ) : LinearSubResult

    suspend fun solveSub(
        name: String,
        metaModel: LinearMetaModel<Flt64>,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<LinearSubResult>

    suspend fun solveSub(
        metaModel: LinearMetaModel<Flt64>,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): Ret<LinearSubResult> {
        return solveSub(
            name = options.solveName(metaModel.name),
            metaModel = metaModel,
            objectVariable = objectVariable,
            fixedVariables = fixedVariables,
            toLogModel = options.toLogModel,
            registrationStatusCallBack = options.registrationStatusCallBack,
            solvingStatusCallBack = options.solvingStatusCallBack
        )
    }

        fun solveSubAsync(
        name: String,
        metaModel: LinearMetaModel<Flt64>,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<LinearSubResult>> {
        return frameworkAsyncScope.future {
            return@future this@LinearBendersDecompositionSolver.solveSub(
                name = name,
                metaModel = metaModel,
                objectVariable = objectVariable,
                fixedVariables = fixedVariables,
                toLogModel = toLogModel,
                registrationStatusCallBack = registrationStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack
            )
        }
    }

        fun solveSubAsync(
        metaModel: LinearMetaModel<Flt64>,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): CompletableFuture<Ret<LinearSubResult>> {
        return frameworkAsyncScope.future {
            return@future this@LinearBendersDecompositionSolver.solveSub(
                metaModel = metaModel,
                objectVariable = objectVariable,
                fixedVariables = fixedVariables,
                options = options
            )
        }
    }

    suspend fun <V> solveMasterV(
        name: String,
        metaModel: LinearMetaModel<Flt64>,
        converter: IntoValue<V>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<SolverOutput> where V : RealNumber<V>, V : NumberField<V> {
        return when (val result = solveMaster(
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

    suspend fun <V> solveMasterV(
        name: String,
        metaModel: LinearMetaModel<V>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<SolverOutput> where V : RealNumber<V>, V : NumberField<V> {
        return solveMasterV(
            name = name,
            metaModel = castLinearMetaModelForSolver(metaModel),
            converter = metaModel.converter,
            toLogModel = toLogModel,
            registrationStatusCallBack = registrationStatusCallBack,
            solvingStatusCallBack = solvingStatusCallBack
        )
    }

    suspend fun <V> solveMasterV(
        metaModel: LinearMetaModel<Flt64>,
        converter: IntoValue<V>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): Ret<SolverOutput> where V : RealNumber<V>, V : NumberField<V> {
        return solveMasterV(
            name = options.solveName(metaModel.name),
            metaModel = metaModel,
            converter = converter,
            toLogModel = options.toLogModel,
            registrationStatusCallBack = options.registrationStatusCallBack,
            solvingStatusCallBack = options.solvingStatusCallBack
        )
    }

    suspend fun <V> solveMasterV(
        metaModel: LinearMetaModel<V>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): Ret<SolverOutput> where V : RealNumber<V>, V : NumberField<V> {
        return solveMasterV(
            name = options.solveName(metaModel.name),
            metaModel = metaModel,
            toLogModel = options.toLogModel,
            registrationStatusCallBack = options.registrationStatusCallBack,
            solvingStatusCallBack = options.solvingStatusCallBack
        )
    }

        fun <V> solveMasterVAsync(
        name: String,
        metaModel: LinearMetaModel<Flt64>,
        converter: IntoValue<V>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<SolverOutput>> where V : RealNumber<V>, V : NumberField<V> {
        return frameworkAsyncScope.future {
            return@future solveMasterV(
                name = name,
                metaModel = metaModel,
                converter = converter,
                toLogModel = toLogModel,
                registrationStatusCallBack = registrationStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack
            )
        }
    }

        fun <V> solveMasterVAsync(
        metaModel: LinearMetaModel<Flt64>,
        converter: IntoValue<V>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): CompletableFuture<Ret<SolverOutput>> where V : RealNumber<V>, V : NumberField<V> {
        return frameworkAsyncScope.future {
            return@future solveMasterV(
                metaModel = metaModel,
                converter = converter,
                options = options
            )
        }
    }

        fun <V> solveMasterVAsync(
        name: String,
        metaModel: LinearMetaModel<V>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<SolverOutput>> where V : RealNumber<V>, V : NumberField<V> {
        return frameworkAsyncScope.future {
            return@future solveMasterV(
                name = name,
                metaModel = metaModel,
                toLogModel = toLogModel,
                registrationStatusCallBack = registrationStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack
            )
        }
    }

        fun <V> solveMasterVAsync(
        metaModel: LinearMetaModel<V>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): CompletableFuture<Ret<SolverOutput>> where V : RealNumber<V>, V : NumberField<V> {
        return frameworkAsyncScope.future {
            return@future solveMasterV(
                metaModel = metaModel,
                options = options
            )
        }
    }

    sealed interface LinearSubResultV<V> where V : RealNumber<V>, V : NumberField<V> {
        val cuts: List<LinearInequality<V>>?
    }

    data class LinearFeasibleResultV<V>(
        val result: FeasibleSolverOutput<V>,
        val dualSolution: kotlin.collections.Map<Constraint<Flt64, Linear>, Flt64>,
        override val cuts: List<LinearInequality<V>>?
    ) : LinearSubResultV<V> where V : RealNumber<V>, V : NumberField<V> {
        val obj: Flt64 by result::obj
        val solution: List<V> by result::solution
        val time: Duration by result::time
        val possibleBestObj by result::possibleBestObj
        val gap: Flt64 by result::gap
    }

    data class LinearInfeasibleResultV<V>(
        val farkasDualSolution: kotlin.collections.Map<Constraint<Flt64, Linear>, Flt64>,
        override val cuts: List<LinearInequality<V>>?
    ) : LinearSubResultV<V> where V : RealNumber<V>, V : NumberField<V>

    suspend fun <V> solveSubV(
        name: String,
        metaModel: LinearMetaModel<Flt64>,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        converter: IntoValue<V>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<LinearSubResultV<V>> where V : RealNumber<V>, V : NumberField<V> {
        return when (val result = solveSub(
            name = name,
            metaModel = metaModel,
            objectVariable = objectVariable,
            fixedVariables = fixedVariables,
            toLogModel = toLogModel,
            registrationStatusCallBack = registrationStatusCallBack,
            solvingStatusCallBack = solvingStatusCallBack
        )) {
            is Ok -> {
                when (val value = result.value) {
                    is LinearFeasibleResult -> {
                        Ok(
                            LinearFeasibleResultV(
                                result = value.result.convertTo(converter),
                                dualSolution = value.dualSolution,
                                cuts = value.cuts?.map { it.convertTo(converter) }
                            )
                        )
                    }

                    is LinearInfeasibleResult -> {
                        Ok(
                            LinearInfeasibleResultV(
                                farkasDualSolution = value.farkasDualSolution,
                                cuts = value.cuts?.map { it.convertTo(converter) }
                            )
                        )
                    }
                }
            }

            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    suspend fun <V> solveSubV(
        name: String,
        metaModel: LinearMetaModel<V>,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<LinearSubResultV<V>> where V : RealNumber<V>, V : NumberField<V> {
        return solveSubV(
            name = name,
            metaModel = castLinearMetaModelForSolver(metaModel),
            objectVariable = objectVariable,
            fixedVariables = fixedVariables,
            converter = metaModel.converter,
            toLogModel = toLogModel,
            registrationStatusCallBack = registrationStatusCallBack,
            solvingStatusCallBack = solvingStatusCallBack
        )
    }

    suspend fun <V> solveSubV(
        metaModel: LinearMetaModel<Flt64>,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        converter: IntoValue<V>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): Ret<LinearSubResultV<V>> where V : RealNumber<V>, V : NumberField<V> {
        return solveSubV(
            name = options.solveName(metaModel.name),
            metaModel = metaModel,
            objectVariable = objectVariable,
            fixedVariables = fixedVariables,
            converter = converter,
            toLogModel = options.toLogModel,
            registrationStatusCallBack = options.registrationStatusCallBack,
            solvingStatusCallBack = options.solvingStatusCallBack
        )
    }

    suspend fun <V> solveSubV(
        metaModel: LinearMetaModel<V>,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): Ret<LinearSubResultV<V>> where V : RealNumber<V>, V : NumberField<V> {
        return solveSubV(
            name = options.solveName(metaModel.name),
            metaModel = metaModel,
            objectVariable = objectVariable,
            fixedVariables = fixedVariables,
            toLogModel = options.toLogModel,
            registrationStatusCallBack = options.registrationStatusCallBack,
            solvingStatusCallBack = options.solvingStatusCallBack
        )
    }

        fun <V> solveSubVAsync(
        name: String,
        metaModel: LinearMetaModel<Flt64>,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        converter: IntoValue<V>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<LinearSubResultV<V>>> where V : RealNumber<V>, V : NumberField<V> {
        return frameworkAsyncScope.future {
            return@future solveSubV(
                name = name,
                metaModel = metaModel,
                objectVariable = objectVariable,
                fixedVariables = fixedVariables,
                converter = converter,
                toLogModel = toLogModel,
                registrationStatusCallBack = registrationStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack
            )
        }
    }

        fun <V> solveSubVAsync(
        metaModel: LinearMetaModel<Flt64>,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        converter: IntoValue<V>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): CompletableFuture<Ret<LinearSubResultV<V>>> where V : RealNumber<V>, V : NumberField<V> {
        return frameworkAsyncScope.future {
            return@future solveSubV(
                metaModel = metaModel,
                objectVariable = objectVariable,
                fixedVariables = fixedVariables,
                converter = converter,
                options = options
            )
        }
    }

        fun <V> solveSubVAsync(
        name: String,
        metaModel: LinearMetaModel<V>,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<LinearSubResultV<V>>> where V : RealNumber<V>, V : NumberField<V> {
        return frameworkAsyncScope.future {
            return@future solveSubV(
                name = name,
                metaModel = metaModel,
                objectVariable = objectVariable,
                fixedVariables = fixedVariables,
                toLogModel = toLogModel,
                registrationStatusCallBack = registrationStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack
            )
        }
    }

        fun <V> solveSubVAsync(
        metaModel: LinearMetaModel<V>,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): CompletableFuture<Ret<LinearSubResultV<V>>> where V : RealNumber<V>, V : NumberField<V> {
        return frameworkAsyncScope.future {
            return@future solveSubV(
                metaModel = metaModel,
                objectVariable = objectVariable,
                fixedVariables = fixedVariables,
                options = options
            )
        }
    }
}

interface QuadraticBendersDecompositionSolver : LinearBendersDecompositionSolver {
    suspend fun solveMaster(
        name: String,
        metaModel: QuadraticMetaModel<Flt64>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<SolverOutput>

    suspend fun solveMaster(
        metaModel: QuadraticMetaModel<Flt64>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): Ret<SolverOutput> {
        return solveMaster(
            name = options.solveName(metaModel.name),
            metaModel = metaModel,
            toLogModel = options.toLogModel,
            registrationStatusCallBack = options.registrationStatusCallBack,
            solvingStatusCallBack = options.solvingStatusCallBack
        )
    }

        fun solveMasterAsync(
        name: String,
        metaModel: QuadraticMetaModel<Flt64>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<SolverOutput>> {
        return frameworkAsyncScope.future {
            return@future this@QuadraticBendersDecompositionSolver.solveMaster(
                name = name,
                metaModel = metaModel,
                toLogModel = toLogModel,
                registrationStatusCallBack = registrationStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack
            )
        }
    }

        fun solveMasterAsync(
        metaModel: QuadraticMetaModel<Flt64>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): CompletableFuture<Ret<SolverOutput>> {
        return frameworkAsyncScope.future {
            return@future this@QuadraticBendersDecompositionSolver.solveMaster(
                metaModel = metaModel,
                options = options
            )
        }
    }

    /**
     * Sub Problem Solving Result (with cuts if fixed variables are provided)
     *
     * @property linearCuts     list of linear cuts generated by sub problem, if fixed variables are provided.
     * @property quadraticCuts  list of quadratic cuts generated by sub problem, if fixed variables are provided.
     * Otherwise, there should not be any fixed variables in the sub problem model. In other words, the fixed variables should be replaced with their values.
     */
    sealed interface QuadraticSubResult {
        val linearCuts: List<LinearInequality<Flt64>>?
        val quadraticCuts: List<QuadraticInequalityOf<Flt64>>?
    }

    data class QuadraticFeasibleResult(
        val result: FeasibleSolverOutput<Flt64>,
        val dualSolution: kotlin.collections.Map<Constraint<Flt64, Quadratic>, Flt64>,
        override val linearCuts: List<LinearInequality<Flt64>>?,
        override val quadraticCuts: List<QuadraticInequalityOf<Flt64>>?,
    ) : QuadraticSubResult {
        val obj: Flt64 by result::obj
        val solution: List<Flt64> by result::solution
        val time: Duration by result::time
        val possibleBestObj by result::possibleBestObj
        val gap: Flt64 by result::gap
    }

    data class QuadraticInfeasibleResult(
        val farkasDualSolution: kotlin.collections.Map<Constraint<Flt64, Quadratic>, Flt64>,
        override val linearCuts: List<LinearInequality<Flt64>>?,
        override val quadraticCuts: List<QuadraticInequalityOf<Flt64>>?,
    ) : QuadraticSubResult

    suspend fun solveSub(
        name: String,
        metaModel: QuadraticMetaModel<Flt64>,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<QuadraticSubResult>

    suspend fun solveSub(
        metaModel: QuadraticMetaModel<Flt64>,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): Ret<QuadraticSubResult> {
        return solveSub(
            name = options.solveName(metaModel.name),
            metaModel = metaModel,
            objectVariable = objectVariable,
            fixedVariables = fixedVariables,
            toLogModel = options.toLogModel,
            registrationStatusCallBack = options.registrationStatusCallBack,
            solvingStatusCallBack = options.solvingStatusCallBack
        )
    }

        fun solveSubAsync(
        name: String,
        metaModel: QuadraticMetaModel<Flt64>,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<QuadraticSubResult>> {
        return frameworkAsyncScope.future {
            return@future this@QuadraticBendersDecompositionSolver.solveSub(
                name = name,
                metaModel = metaModel,
                objectVariable = objectVariable,
                fixedVariables = fixedVariables,
                toLogModel = toLogModel,
                registrationStatusCallBack = registrationStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack
            )
        }
    }

        fun solveSubAsync(
        metaModel: QuadraticMetaModel<Flt64>,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): CompletableFuture<Ret<QuadraticSubResult>> {
        return frameworkAsyncScope.future {
            return@future this@QuadraticBendersDecompositionSolver.solveSub(
                metaModel = metaModel,
                objectVariable = objectVariable,
                fixedVariables = fixedVariables,
                options = options
            )
        }
    }

    suspend fun <V> solveMasterV(
        name: String,
        metaModel: QuadraticMetaModel<Flt64>,
        converter: IntoValue<V>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<SolverOutput> where V : RealNumber<V>, V : NumberField<V> {
        return when (val result = solveMaster(
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

    suspend fun <V> solveMasterV(
        name: String,
        metaModel: QuadraticMetaModel<V>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<SolverOutput> where V : RealNumber<V>, V : NumberField<V> {
        return solveMasterV(
            name = name,
            metaModel = castQuadraticMetaModelForSolver(metaModel),
            converter = metaModel.converter,
            toLogModel = toLogModel,
            registrationStatusCallBack = registrationStatusCallBack,
            solvingStatusCallBack = solvingStatusCallBack
        )
    }

    suspend fun <V> solveMasterV(
        metaModel: QuadraticMetaModel<Flt64>,
        converter: IntoValue<V>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): Ret<SolverOutput> where V : RealNumber<V>, V : NumberField<V> {
        return solveMasterV(
            name = options.solveName(metaModel.name),
            metaModel = metaModel,
            converter = converter,
            toLogModel = options.toLogModel,
            registrationStatusCallBack = options.registrationStatusCallBack,
            solvingStatusCallBack = options.solvingStatusCallBack
        )
    }

    suspend fun <V> solveMasterV(
        metaModel: QuadraticMetaModel<V>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): Ret<SolverOutput> where V : RealNumber<V>, V : NumberField<V> {
        return solveMasterV(
            name = options.solveName(metaModel.name),
            metaModel = metaModel,
            toLogModel = options.toLogModel,
            registrationStatusCallBack = options.registrationStatusCallBack,
            solvingStatusCallBack = options.solvingStatusCallBack
        )
    }

        fun <V> solveMasterVAsync(
        name: String,
        metaModel: QuadraticMetaModel<Flt64>,
        converter: IntoValue<V>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<SolverOutput>> where V : RealNumber<V>, V : NumberField<V> {
        return frameworkAsyncScope.future {
            return@future solveMasterV(
                name = name,
                metaModel = metaModel,
                converter = converter,
                toLogModel = toLogModel,
                registrationStatusCallBack = registrationStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack
            )
        }
    }

        fun <V> solveMasterVAsync(
        metaModel: QuadraticMetaModel<Flt64>,
        converter: IntoValue<V>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): CompletableFuture<Ret<SolverOutput>> where V : RealNumber<V>, V : NumberField<V> {
        return frameworkAsyncScope.future {
            return@future solveMasterV(
                metaModel = metaModel,
                converter = converter,
                options = options
            )
        }
    }

        fun <V> solveMasterVAsync(
        name: String,
        metaModel: QuadraticMetaModel<V>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<SolverOutput>> where V : RealNumber<V>, V : NumberField<V> {
        return frameworkAsyncScope.future {
            return@future solveMasterV(
                name = name,
                metaModel = metaModel,
                toLogModel = toLogModel,
                registrationStatusCallBack = registrationStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack
            )
        }
    }

        fun <V> solveMasterVAsync(
        metaModel: QuadraticMetaModel<V>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): CompletableFuture<Ret<SolverOutput>> where V : RealNumber<V>, V : NumberField<V> {
        return frameworkAsyncScope.future {
            return@future solveMasterV(
                metaModel = metaModel,
                options = options
            )
        }
    }

    sealed interface QuadraticSubResultV<V> where V : RealNumber<V>, V : NumberField<V> {
        val linearCuts: List<LinearInequality<V>>?
        val quadraticCuts: List<QuadraticInequalityOf<V>>?
    }

    data class QuadraticFeasibleResultV<V>(
        val result: FeasibleSolverOutput<V>,
        val dualSolution: kotlin.collections.Map<Constraint<Flt64, Quadratic>, Flt64>,
        override val linearCuts: List<LinearInequality<V>>?,
        override val quadraticCuts: List<QuadraticInequalityOf<V>>?
    ) : QuadraticSubResultV<V> where V : RealNumber<V>, V : NumberField<V> {
        val obj: Flt64 by result::obj
        val solution: List<V> by result::solution
        val time: Duration by result::time
        val possibleBestObj by result::possibleBestObj
        val gap: Flt64 by result::gap
    }

    data class QuadraticInfeasibleResultV<V>(
        val farkasDualSolution: kotlin.collections.Map<Constraint<Flt64, Quadratic>, Flt64>,
        override val linearCuts: List<LinearInequality<V>>?,
        override val quadraticCuts: List<QuadraticInequalityOf<V>>?
    ) : QuadraticSubResultV<V> where V : RealNumber<V>, V : NumberField<V>

    suspend fun <V> solveSubV(
        name: String,
        metaModel: QuadraticMetaModel<Flt64>,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        converter: IntoValue<V>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<QuadraticSubResultV<V>> where V : RealNumber<V>, V : NumberField<V> {
        return when (val result = solveSub(
            name = name,
            metaModel = metaModel,
            objectVariable = objectVariable,
            fixedVariables = fixedVariables,
            toLogModel = toLogModel,
            registrationStatusCallBack = registrationStatusCallBack,
            solvingStatusCallBack = solvingStatusCallBack
        )) {
            is Ok -> {
                when (val value = result.value) {
                    is QuadraticFeasibleResult -> {
                        Ok(
                            QuadraticFeasibleResultV(
                                result = value.result.convertTo(converter),
                                dualSolution = value.dualSolution,
                                linearCuts = value.linearCuts?.map { it.convertTo(converter) },
                                quadraticCuts = value.quadraticCuts?.map { it.convertTo(converter) }
                            )
                        )
                    }

                    is QuadraticInfeasibleResult -> {
                        Ok(
                            QuadraticInfeasibleResultV(
                                farkasDualSolution = value.farkasDualSolution,
                                linearCuts = value.linearCuts?.map { it.convertTo(converter) },
                                quadraticCuts = value.quadraticCuts?.map { it.convertTo(converter) }
                            )
                        )
                    }
                }
            }

            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    suspend fun <V> solveSubV(
        name: String,
        metaModel: QuadraticMetaModel<V>,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<QuadraticSubResultV<V>> where V : RealNumber<V>, V : NumberField<V> {
        return solveSubV(
            name = name,
            metaModel = castQuadraticMetaModelForSolver(metaModel),
            objectVariable = objectVariable,
            fixedVariables = fixedVariables,
            converter = metaModel.converter,
            toLogModel = toLogModel,
            registrationStatusCallBack = registrationStatusCallBack,
            solvingStatusCallBack = solvingStatusCallBack
        )
    }

    suspend fun <V> solveSubV(
        metaModel: QuadraticMetaModel<Flt64>,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        converter: IntoValue<V>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): Ret<QuadraticSubResultV<V>> where V : RealNumber<V>, V : NumberField<V> {
        return solveSubV(
            name = options.solveName(metaModel.name),
            metaModel = metaModel,
            objectVariable = objectVariable,
            fixedVariables = fixedVariables,
            converter = converter,
            toLogModel = options.toLogModel,
            registrationStatusCallBack = options.registrationStatusCallBack,
            solvingStatusCallBack = options.solvingStatusCallBack
        )
    }

    suspend fun <V> solveSubV(
        metaModel: QuadraticMetaModel<V>,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): Ret<QuadraticSubResultV<V>> where V : RealNumber<V>, V : NumberField<V> {
        return solveSubV(
            name = options.solveName(metaModel.name),
            metaModel = metaModel,
            objectVariable = objectVariable,
            fixedVariables = fixedVariables,
            toLogModel = options.toLogModel,
            registrationStatusCallBack = options.registrationStatusCallBack,
            solvingStatusCallBack = options.solvingStatusCallBack
        )
    }

        fun <V> solveSubVAsync(
        name: String,
        metaModel: QuadraticMetaModel<Flt64>,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        converter: IntoValue<V>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<QuadraticSubResultV<V>>> where V : RealNumber<V>, V : NumberField<V> {
        return frameworkAsyncScope.future {
            return@future solveSubV(
                name = name,
                metaModel = metaModel,
                objectVariable = objectVariable,
                fixedVariables = fixedVariables,
                converter = converter,
                toLogModel = toLogModel,
                registrationStatusCallBack = registrationStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack
            )
        }
    }

        fun <V> solveSubVAsync(
        metaModel: QuadraticMetaModel<Flt64>,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        converter: IntoValue<V>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): CompletableFuture<Ret<QuadraticSubResultV<V>>> where V : RealNumber<V>, V : NumberField<V> {
        return frameworkAsyncScope.future {
            return@future solveSubV(
                metaModel = metaModel,
                objectVariable = objectVariable,
                fixedVariables = fixedVariables,
                converter = converter,
                options = options
            )
        }
    }

        fun <V> solveSubVAsync(
        name: String,
        metaModel: QuadraticMetaModel<V>,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        toLogModel: Boolean = false,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): CompletableFuture<Ret<QuadraticSubResultV<V>>> where V : RealNumber<V>, V : NumberField<V> {
        return frameworkAsyncScope.future {
            return@future solveSubV(
                name = name,
                metaModel = metaModel,
                objectVariable = objectVariable,
                fixedVariables = fixedVariables,
                toLogModel = toLogModel,
                registrationStatusCallBack = registrationStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack
            )
        }
    }

        fun <V> solveSubVAsync(
        metaModel: QuadraticMetaModel<V>,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        options: FrameworkSolveOptions = FrameworkSolveOptions()
    ): CompletableFuture<Ret<QuadraticSubResultV<V>>> where V : RealNumber<V>, V : NumberField<V> {
        return frameworkAsyncScope.future {
            return@future solveSubV(
                metaModel = metaModel,
                objectVariable = objectVariable,
                fixedVariables = fixedVariables,
                options = options
            )
        }
    }
}
