package fuookami.ospf.kotlin.core.solver.gurobi11

import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.Token
import fuookami.ospf.kotlin.core.solver.report.SolveReport
import fuookami.ospf.kotlin.core.solver.restoreNativePiecewiseSolution

/**
 * 使用 core 报告恢复逻辑，并标记 Gurobi 11 backend。 / Delegate report restoration to core and mark the Gurobi 11 backend.
 *
 * @param report 原生求解报告 / Native solve report
 * @param nativeModel 原生中间模型 / Native intermediate model
 * @param originalTokens 原始 token 顺序 / Original token order
 * @param structures 原生处理的 PWL 结构 / Natively handled PWL structures
 * @param absStructures 原生处理的 ABS 结构 / Natively handled ABS structures
 * @return 恢复后的报告或错误 / Restored report or an error
 */
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
        backendName = "gurobi11",
        absStructures = absStructures,
        maxStructures = maxStructures,
        semiStructures = semiStructures,
        indicatorStructures = indicatorStructures,
        maskingStructures = maskingStructures,
        binaryLogicStructures = binaryLogicStructures
    )
}
