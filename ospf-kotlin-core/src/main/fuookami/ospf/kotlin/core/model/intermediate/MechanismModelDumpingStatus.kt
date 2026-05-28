/**
 * 机制模型转储状态
 * Mechanism model dumping status
 */
package fuookami.ospf.kotlin.core.model.intermediate

import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.math.usize
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.MetaModel

/**
 * 机制模型转储进度状态，跟踪约束和符号的准备进度。
 * Mechanism model dumping progress status tracking constraint and symbol readiness.
 *
 * @property readyConstraintAmount 已就绪约束数量 / Number of ready constraints
 * @property totalConstraintAmount 约束总数 / Total number of constraints
 * @property readySymbolAmount     已就绪符号数量 / Number of ready symbols
 * @property totalSymbolAmount     符号总数 / Total number of symbols
 */
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
        fun <V> dumpingConstrains(ready: UInt64, model: MetaModel<V>): MechanismModelDumpingStatus where V : RealNumber<V>, V : NumberField<V> {
            return MechanismModelDumpingStatus(
                readyConstraintAmount = ready,
                totalConstraintAmount = model.constraints.usize,
                readySymbolAmount = UInt64.zero,
                totalSymbolAmount = model.tokens.symbols.usize
            )
        }

        fun <V> dumpingSymbols(ready: UInt64, model: MetaModel<V>): MechanismModelDumpingStatus where V : RealNumber<V>, V : NumberField<V> {
            return MechanismModelDumpingStatus(
                readyConstraintAmount = model.constraints.usize,
                totalConstraintAmount = model.constraints.usize,
                readySymbolAmount = ready,
                totalSymbolAmount = model.tokens.symbols.usize
            )
        }
    }
}

/** 机制模型转储状态回调 / Mechanism model dumping status callback */
typealias MechanismModelDumpingStatusCallBack = (MechanismModelDumpingStatus) -> Try

/**
 * 将机制模型转储状态转换为模型构建状态。
 * Convert mechanism model dumping status to model building status.
 */
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
