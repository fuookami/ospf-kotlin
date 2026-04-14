package fuookami.ospf.kotlin.core.model.status

enum class ModelBuildingStage {
    RegisterTokens,
    RegisterLinearConstraints,
    RegisterQuadraticConstraints,
    RegisterSymbols,
    FlattenLinearModel,
    FlattenQuadraticModel,
    BuildObjective
}

