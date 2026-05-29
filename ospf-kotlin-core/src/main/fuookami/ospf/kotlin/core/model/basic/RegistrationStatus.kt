/**
 * 注册状态
 * Registration status
 */
package fuookami.ospf.kotlin.core.model.basic

import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.math.algebra.number.*

/**
 * 符号注册状态，跟踪符号注册进度。
 * Symbol registration status tracking registration progress.
 *
 * @property emptySymbolAmount  空符号数量 / Number of empty symbols
 * @property readySymbolAmount  已就绪符号数量 / Number of ready symbols
 * @property totalSymbolAmount  符号总数 / Total number of symbols
 */
data class RegistrationStatus(
    val emptySymbolAmount: UInt64,
    val readySymbolAmount: UInt64,
    val totalSymbolAmount: UInt64
) {
    /** 非空符号数量 / Non-empty symbol count */
    val notEmptySymbolAmount: UInt64 get() = totalSymbolAmount - emptySymbolAmount
    /** 已就绪的非空符号数量 / Ready non-empty symbol count */
    val readyNotEmptySymbolAmount: UInt64 get() = readySymbolAmount - emptySymbolAmount

    /** 总进度 / Total progress */
    val totalProgress: Flt64
        get() = if (totalSymbolAmount neq UInt64.zero) {
            readySymbolAmount.toFlt64() / totalSymbolAmount.toFlt64()
        } else {
            Flt64.one
        }

    /** 非空进度 / Non-empty progress */
    val notEmptyProgress: Flt64
        get() = if (notEmptySymbolAmount neq UInt64.zero) {
            (readyNotEmptySymbolAmount.toFlt64() / notEmptySymbolAmount.toFlt64())
        } else {
            Flt64.one
        }
}

/** 注册状态回调 / Registration status callback */
typealias RegistrationStatusCallBack = (RegistrationStatus) -> Try

/**
 * 将注册状态转换为模型构建状态。
 * Convert registration status to model building status.
 *
 * @param modelName 模型名称 / The model name
 * @return 模型构建状态 / The model building status
 */
fun RegistrationStatus.toModelBuildingStatus(modelName: String): ModelBuildingStatus {
    return ModelBuildingStatus(
        modelName = modelName,
        stage = ModelBuildingStage.RegisterTokens,
        ready = readySymbolAmount,
        total = totalSymbolAmount
    )
}
