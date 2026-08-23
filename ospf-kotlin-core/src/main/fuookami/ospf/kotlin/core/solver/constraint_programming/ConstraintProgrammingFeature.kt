/**
 * CP 能力与支持级别。 / CP features and support levels.
 */
package fuookami.ospf.kotlin.core.solver.constraint_programming

/** CP 首版及扩展能力。 / CP capabilities. */
enum class ConstraintProgrammingFeature {
    BooleanLogic,
    Reification,
    SparseDomain,
    AllDifferent,
    Element,
    Table,
    Interval,
    OptionalInterval,
    NoOverlap,
    Cumulative,
    Circuit,
    Automaton,
    Reservoir,
    Assumption,
    ConflictCore,
    SolutionHint,
    IncrementalSolve
}

/** CP 能力支持级别。 / CP capability support level. */
enum class ConstraintProgrammingSupportLevel {
    Native,
    ExactLowering,
    Unsupported
}

/**
 * 获取指定 CP 能力支持级别。 / Get support level for a CP feature.
 *
 * @param feature CP 能力 / CP feature
 * @return 能力支持级别 / Feature support level
 */
fun fuookami.ospf.kotlin.core.solver.report.SolverCapabilities.constraintProgrammingSupport(
    feature: ConstraintProgrammingFeature
): ConstraintProgrammingSupportLevel {
    return constraintProgrammingFeatures[feature] ?: ConstraintProgrammingSupportLevel.Unsupported
}

/**
 * 判断指定 CP 能力是否可用。 / Check whether a CP feature is available.
 *
 * @param feature CP 能力 / CP feature
 * @return 是否支持该能力 / Whether the feature is supported
 */
fun fuookami.ospf.kotlin.core.solver.report.SolverCapabilities.supportsConstraintProgramming(
    feature: ConstraintProgrammingFeature
): Boolean {
    return constraintProgrammingSupport(feature) != ConstraintProgrammingSupportLevel.Unsupported
}
