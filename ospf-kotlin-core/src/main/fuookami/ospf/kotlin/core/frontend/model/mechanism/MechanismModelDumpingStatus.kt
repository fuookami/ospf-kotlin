package fuookami.ospf.kotlin.core.frontend.model.mechanism

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*

data class MechanismModelDumpingStatus(
    val readyConstraintAmount: UInt64,
    val totalConstraintAmount: UInt64,
    val readySymbolAmount: UInt64,
    val totalSymbolAmount: UInt64
) {
    val constraintProgress: Flt64 get() = if (totalConstraintAmount neq UInt64.zero) {
        readyConstraintAmount.toFlt64() / totalConstraintAmount.toFlt64()
    } else {
        Flt64.one
    }

    val symbolProgress: Flt64 get() = if (totalSymbolAmount != UInt64.zero) {
        readySymbolAmount.toFlt64() / totalSymbolAmount.toFlt64()
    } else {
        Flt64.one
    }

    companion object {
        fun dumpingConstrains(ready: UInt64, model: MetaModel): MechanismModelDumpingStatus {
            return MechanismModelDumpingStatus(
                readyConstraintAmount = ready,
                totalConstraintAmount = model.constraints.usize,
                readySymbolAmount = UInt64.zero,
                totalSymbolAmount = model.tokens.symbols.usize
            )
        }

        fun dumpingSymbols(ready: UInt64, model: MetaModel): MechanismModelDumpingStatus {
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
