/**
 * 远程求解标准化模型类型
 * Remote solve normalized model types
*/
package fuookami.ospf.kotlin.framework.solver.remote.domain

import kotlinx.serialization.Serializable

/**
 * 标准化模型类型。
 * Normalized model type.
*/
@Serializable
enum class NormalizedModelType {
    /** 线性模型 / Linear model */
    LINEAR,

    /** 二次模型 / Quadratic model */
    QUADRATIC,

    /** 未知模型 / Unknown model */
    UNKNOWN
}
