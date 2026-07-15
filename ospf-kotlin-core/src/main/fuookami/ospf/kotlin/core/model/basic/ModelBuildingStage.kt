/**
 * 模型构建阶段
 * Model building stage
*/
package fuookami.ospf.kotlin.core.model.basic

/**
 * 模型构建阶段枚举，标识当前构建进度所处的阶段。
 * Enumeration of model building stages indicating current construction progress.
*/
enum class ModelBuildingStage {
    /** 注册令牌 / Register tokens */
    RegisterTokens,
    /** 注册线性约束 / Register linear constraints */
    RegisterLinearConstraints,
    /** 注册二次约束 / Register quadratic constraints */
    RegisterQuadraticConstraints,
    /** 注册符号 / Register symbols */
    RegisterSymbols,
    /** 展平线性模型 / Flatten linear model */
    FlattenLinearModel,
    /** 展平二次模型 / Flatten quadratic model */
    FlattenQuadraticModel,
    /** 构建目标函数 / Build objective function */
    BuildObjective
}
