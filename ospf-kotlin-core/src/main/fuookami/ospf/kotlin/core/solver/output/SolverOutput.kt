@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.core.solver.output

import fuookami.ospf.kotlin.core.model.intermediate.BasicLinearTriadModelView
import fuookami.ospf.kotlin.core.model.intermediate.QuadraticTetradModelView
import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import kotlin.time.Duration

sealed interface SolverOutput {}
sealed interface UnifiedSolverOutput : SolverOutput {
    val iterations: UInt64?
    val nodeCount: UInt64?
    val bestBound: Flt64?
    val mipGap: Flt64?
    val solveTime: Duration?
}
sealed interface LinearSolverOutput : SolverOutput {}
sealed interface QuadraticSolverOutput : SolverOutput {}

data class FeasibleSolverOutput<V>(
    val obj: Flt64,
    val solution: Solution<V>,
    val time: Duration,
    val possibleBestObj: Flt64,
    val gap: Flt64,
    override val iterations: UInt64? = null,
    override val nodeCount: UInt64? = null,
    override val bestBound: Flt64? = null,
    override val mipGap: Flt64 = gap,
    override val solveTime: Duration = time
) : LinearSolverOutput, QuadraticSolverOutput, UnifiedSolverOutput


fun <V> FeasibleSolverOutput<fuookami.ospf.kotlin.math.algebra.number.Flt64>.convertTo(converter: IntoValue<V>): FeasibleSolverOutput<V>
        where V : RealNumber<V>, V : NumberField<V> {
    return FeasibleSolverOutput(
        obj = obj,
        solution = solution.map { converter.intoValue(it) },
        time = time,
        possibleBestObj = possibleBestObj,
        gap = gap,
        iterations = iterations,
        nodeCount = nodeCount,
        bestBound = bestBound,
        mipGap = mipGap,
        solveTime = solveTime
    )
}

data class LinearInfeasibleSolverOutput(
    val iis: BasicLinearTriadModelView,
    override val iterations: UInt64? = null,
    override val nodeCount: UInt64? = null,
    override val bestBound: Flt64? = null,
    override val mipGap: Flt64? = null,
    override val solveTime: Duration? = null
) : LinearSolverOutput, UnifiedSolverOutput

data class QuadraticInfeasibleSolverOutput(
    val iis: QuadraticTetradModelView,
    override val iterations: UInt64? = null,
    override val nodeCount: UInt64? = null,
    override val bestBound: Flt64? = null,
    override val mipGap: Flt64? = null,
    override val solveTime: Duration? = null
) : QuadraticSolverOutput, UnifiedSolverOutput

data class SolverOutputWithIIS<out IIS>(
    val output: SolverOutput,
    val iis: IIS?
)

fun <IIS> SolverOutput.withIIS(iis: IIS?): SolverOutputWithIIS<IIS> {
    return SolverOutputWithIIS(
        output = this,
        iis = iis
    )
}

fun SolverOutput.withoutIIS(): SolverOutputWithIIS<Nothing> {
    return SolverOutputWithIIS(
        output = this,
        iis = null
    )
}

fun LinearInfeasibleSolverOutput.withIIS(): SolverOutputWithIIS<BasicLinearTriadModelView> {
    return SolverOutputWithIIS(
        output = this,
        iis = iis
    )
}

fun QuadraticInfeasibleSolverOutput.withIIS(): SolverOutputWithIIS<QuadraticTetradModelView> {
    return SolverOutputWithIIS(
        output = this,
        iis = iis
    )
}


