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
    RegisterTokens,
    RegisterLinearConstraints,
    RegisterQuadraticConstraints,
    RegisterSymbols,
    FlattenLinearModel,
    FlattenQuadraticModel,
    BuildObjective
}
