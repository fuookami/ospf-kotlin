package fuookami.ospf.kotlin.core.model.basic

enum class ModelBuildingStage {
    RegisterTokens,
    RegisterLinearConstraints,
    RegisterQuadraticConstraints,
    RegisterSymbols,
    FlattenLinearModel,
    FlattenQuadraticModel,
    BuildObjective
}
