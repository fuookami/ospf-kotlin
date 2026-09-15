/** Evidence captured while evaluating activity. / 活动性求值时保留的证据。 */
package fuookami.ospf.kotlin.core.analysis

import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingComparison

/** Evidence kind used at the original-model report boundary. / 原始模型报告边界的证据类型。 */
enum class ActivityEvidenceKind {
    /** An integer comparison with a scalar slack metric. / 带标量松弛的整数比较。 */
    Comparison,

    /** A variable lower or upper bound. / 变量下界或上界。 */
    VariableBound,

    /** A sparse integer domain without a scalar slack metric. / 无标量松弛的稀疏整数值域。 */
    SparseDomain,

    /** A semantic CP constraint without a scalar slack metric. / 无标量松弛的 CP 语义约束。 */
    Semantic,

    /** Evaluation was not possible. / 无法完成求值。 */
    Unknown
}

/**
 * Original-model evidence for one activity record.
 *
 * Numeric values are exposed as `Double` for report interoperability. Exact
 * integer spellings are retained as strings when an Int64 value was available,
 * so consumers do not need to use a rounded floating value as a CP input.
 */
data class ActivityEvidence(
    /** Original stable source. / 原始稳定来源。 */
    val source: DiagnosticSource,
    /** Evidence kind. / 证据类型。 */
    val kind: ActivityEvidenceKind,
    /** Evaluated left-hand side, when applicable. / 可用时的左侧求值。 */
    val lhs: Double? = null,
    /** Right-hand side or bound, when applicable. / 可用时的右侧或边界值。 */
    val rhs: Double? = null,
    /** Comparison relation, when applicable. / 可用时的比较关系。 */
    val relation: ConstraintProgrammingComparison? = null,
    /** Evaluated variable value, when applicable. / 可用时的变量值。 */
    val value: Double? = null,
    /** Variable bound or domain boundary, when applicable. / 可用时的变量边界或值域边界。 */
    val boundary: Double? = null,
    /** Exact left-hand side spelling. / 精确左侧值字符串。 */
    val exactLhs: String? = null,
    /** Exact right-hand side spelling. / 精确右侧值字符串。 */
    val exactRhs: String? = null,
    /** Exact variable value spelling. / 精确变量值字符串。 */
    val exactValue: String? = null,
    /** Exact boundary spelling. / 精确边界值字符串。 */
    val exactBoundary: String? = null,
    /** Exact semantic satisfaction result, when evaluation completed. / 求值完成时的精确语义结果。 */
    val satisfied: Boolean? = null,
    /** Bilingual diagnostic detail, normally used for Unknown. / 双语诊断详情，通常用于 Unknown。 */
    val message: String? = null
)
