package fuookami.ospf.kotlin.core.model.intermediate

import fuookami.ospf.kotlin.core.model.basic.ModelBuildingStage
import fuookami.ospf.kotlin.core.model.basic.ModelBuildingStatus
import fuookami.ospf.kotlin.core.model.mechanism.MetaModelFlt64
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.usize

data class MechanismModelDumpingStatus(
    val readyConstraintAmount: UInt64,
    val totalConstraintAmount: UInt64,
    val readySymbolAmount: UInt64,
    val totalSymbolAmount: UInt64
) {
    val constraintProgress: Flt64
        get() = if (totalConstraintAmount neq UInt64.zero) {
            readyConstraintAmount.toFlt64() / totalConstraintAmount.toFlt64()
        } else {
            Flt64.one
        }

    val symbolProgress: Flt64
        get() = if (totalSymbolAmount != UInt64.zero) {
            readySymbolAmount.toFlt64() / totalSymbolAmount.toFlt64()
        } else {
            Flt64.one
        }

    companion object {
        fun dumpingConstrains(ready: UInt64, model: MetaModelFlt64): MechanismModelDumpingStatus {
            return MechanismModelDumpingStatus(
                readyConstraintAmount = ready,
                totalConstraintAmount = model.constraints.usize,
                readySymbolAmount = UInt64.zero,
                totalSymbolAmount = model.tokens.symbols.usize
            )
        }

        fun dumpingSymbols(ready: UInt64, model: MetaModelFlt64): MechanismModelDumpingStatus {
            return MechanismModelDumpingStatus(
                readyConstraintAmount = model.constraints.usize,
                totalConstraintAmount = model.constraints.usize,
                readySymbolAmount = ready,
                totalSymbolAmount = model.tokens.symbols.usize
            )
        }
    }
}

typealias MechanismModelDumpingStatusCallBack = (MechanismModelDumpingStatus) -> Try

fun MechanismModelDumpingStatus.toModelBuildingStatus(
    modelName: String,
    quadratic: Boolean = false
): ModelBuildingStatus {
    val constraintStage = if (quadratic) {
        ModelBuildingStage.RegisterQuadraticConstraints
    } else {
        ModelBuildingStage.RegisterLinearConstraints
    }
    return if (readyConstraintAmount ls totalConstraintAmount) {
        ModelBuildingStatus(
            modelName = modelName,
            stage = constraintStage,
            ready = readyConstraintAmount,
            total = totalConstraintAmount
        )
    } else {
        ModelBuildingStatus(
            modelName = modelName,
            stage = ModelBuildingStage.RegisterSymbols,
            ready = readySymbolAmount,
            total = totalSymbolAmount
        )
    }
}



