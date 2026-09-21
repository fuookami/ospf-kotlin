package fuookami.ospf.kotlin.core.solver.gurobi

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.intermediate.AbsStructure
import fuookami.ospf.kotlin.core.model.intermediate.MaxStructure
import fuookami.ospf.kotlin.core.model.intermediate.SemiStructure
import fuookami.ospf.kotlin.core.model.intermediate.LinearTriadModel
import fuookami.ospf.kotlin.core.model.intermediate.MaskingStructure
import fuookami.ospf.kotlin.core.model.intermediate.IndicatorStructure
import fuookami.ospf.kotlin.core.model.intermediate.BinaryLogicStructure
import fuookami.ospf.kotlin.core.model.intermediate.UnivariateLinearPiecewiseStructure
import fuookami.ospf.kotlin.core.token.Token
import fuookami.ospf.kotlin.core.solver.report.SolveReport
import fuookami.ospf.kotlin.core.solver.restoreNativePiecewiseSolution

internal fun restoreGurobiPiecewiseSolution(
    report: SolveReport<Flt64>,
    nativeModel: LinearTriadModel,
    originalTokens: List<Token<Flt64>>,
    structures: List<UnivariateLinearPiecewiseStructure<*>>,
    absStructures: List<AbsStructure<*>> = emptyList(),
    maxStructures: List<MaxStructure<*>> = emptyList(),
    semiStructures: List<SemiStructure<*>> = emptyList(),
    indicatorStructures: List<IndicatorStructure<*>> = emptyList(),
    maskingStructures: List<MaskingStructure<*>> = emptyList(),
    binaryLogicStructures: List<BinaryLogicStructure<*>> = emptyList()
): Ret<SolveReport<Flt64>> {
    return restoreNativePiecewiseSolution(
        report = report,
        nativeModel = nativeModel,
        originalTokens = originalTokens,
        structures = structures,
        backendName = "gurobi",
        absStructures = absStructures,
        maxStructures = maxStructures,
        semiStructures = semiStructures,
        indicatorStructures = indicatorStructures,
        maskingStructures = maskingStructures,
        binaryLogicStructures = binaryLogicStructures
    )
}
