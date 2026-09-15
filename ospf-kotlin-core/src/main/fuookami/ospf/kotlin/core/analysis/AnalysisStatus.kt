/** Status values shared by critical-constraint analysis stages. / 临界约束分析阶段共享的状态值。 */
package fuookami.ospf.kotlin.core.analysis

import fuookami.ospf.kotlin.core.solver.report.ProblemStatus

/**
 * A conclusion about whether an analysis target can be reached.
 *
 * `Unknown` is used when the backend did not prove either side of the
 * question. `Unsupported` means that the requested analysis is outside the
 * declared capability boundary; it must not be interpreted as unreachable.
 */
enum class AnalysisStatus {
    /** The target was reached or feasibility was proved. / 目标可达或已证明可行。 */
    Reachable,

    /** The target was proved unreachable or infeasible. / 目标不可达或已证明不可行。 */
    Unreachable,

    /** The solver stopped without a proof. / 求解器停止但没有形成证明。 */
    Unknown,

    /** The requested analysis is not supported. / 当前分析能力不受支持。 */
    Unsupported;

    /** Whether this is a solver-proven conclusion. / 是否为求解器证明结论。 */
    val isProven: Boolean
        get() = this == Reachable || this == Unreachable

    /** Map an existing solve conclusion to the analysis vocabulary. / 将现有求解结论映射为分析状态。 */
    companion object {
        /**
         * 将统一求解状态映射到分析状态。
         *
         * 该映射只回答"满足性问题"：找到可行解即证明可达。带预算的求解器可能在未证明最优性的
         * 情况下返回 `Feasible`，因此**当结论依赖最优性时**（目标值、扰动改善、有效性排序），
         * 必须改用 [from] 的证明门控重载。
         *
         * This mapping only answers satisfaction questions: a feasible point proves reachability.
         * A budget-limited backend can report `Feasible` without proving optimality, so any
         * conclusion that depends on optimality (objective values, perturbation improvement,
         * effectiveness ranking) must use the proof-gated [from] overload instead.
         */
        fun from(problemStatus: ProblemStatus): AnalysisStatus {
            return when (problemStatus) {
                ProblemStatus.Feasible -> Reachable
                ProblemStatus.Infeasible -> Unreachable
                ProblemStatus.Unbounded,
                ProblemStatus.InfeasibleOrUnbounded,
                ProblemStatus.Unknown -> Unknown
            }
        }

        /**
         * 带证明门控的映射。
         *
         * 只有在 `proven` 为真时才把 `Feasible`/`Infeasible` 升格为已证明结论；否则一律降为
         * [Unknown]，防止"超时但拿到 incumbent"被读成已证明。
         *
         * 该函数是两端共享的公共语义，由 `analysis-fixtures/analysis-cases.tsv` 的
         * `[status-map]` 段逐例锁定。
         *
         * Proof-gated mapping. Only a proven run may upgrade `Feasible`/`Infeasible` into a proven
         * conclusion; otherwise the result degrades to [Unknown], so "timed out with an incumbent"
         * can never be read as proven. This is shared cross-language semantics, pinned case by case
         * by the `[status-map]` section of `analysis-fixtures/analysis-cases.tsv`.
         */
        fun from(problemStatus: ProblemStatus, proven: Boolean): AnalysisStatus {
            return when (problemStatus) {
                ProblemStatus.Feasible -> if (proven) Reachable else Unknown
                ProblemStatus.Infeasible -> if (proven) Unreachable else Unknown
                ProblemStatus.Unbounded,
                ProblemStatus.InfeasibleOrUnbounded,
                ProblemStatus.Unknown -> Unknown
            }
        }
    }
}
