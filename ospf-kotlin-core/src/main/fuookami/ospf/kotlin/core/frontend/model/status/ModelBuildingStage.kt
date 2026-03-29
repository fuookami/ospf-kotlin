package fuookami.ospf.kotlin.core.frontend.model.status

enum class ModelBuildingStage {
    RegisterTokens,
    RegisterLinearConstraints,
    RegisterQuadraticConstraints,
    RegisterSymbols,
    FlattenLinearModel,
    FlattenQuadraticModel,
    BuildObjective
}

