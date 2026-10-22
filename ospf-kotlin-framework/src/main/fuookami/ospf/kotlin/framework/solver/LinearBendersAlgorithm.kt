package fuookami.ospf.kotlin.framework.solver

import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.basic.ConstraintRelation
import fuookami.ospf.kotlin.core.model.intermediate.LinearCell
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.toSolverStatus
import fuookami.ospf.kotlin.core.solver.output.SolverStatus
import fuookami.ospf.kotlin.core.solver.report.SolveReport
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.*

/** 线性 Benders 执行预算。 / Linear Benders execution budget.
 * @property iterationLimit 迭代限制 / Iteration limit
 * @property tolerance 相对及绝对间隙容差 / Relative and absolute gap tolerance
 * @property timeLimitMillis 总时间限制 / Total time limit
 * @property stallLimit 无新割迭代限制 / No-new-cut iteration limit
 */
data class LinearBendersConfig(
    val iterationLimit: Int = 64,
    val tolerance: Double = 1e-6,
    val timeLimitMillis: Long = Long.MAX_VALUE,
    val stallLimit: Int = 8
)

/** 已组装的主、子问题，目标统一为最小化。 / Assembled master/sub models, normalized to minimization.
 * @property master 主问题 / Master
 * @property sub 连续子问题，离散变量必须属于绑定 / Continuous subproblem; discrete variables must be bound
 * @property bindings 稳定 ID 到跨模型共享变量身份 / Stable IDs to shared variable identities
 * @property theta 追索目标投影，纯可行性时不进入主目标 / Recourse projection, excluded from objective in feasibility-only mode
 * @property feasibilityOnly 子问题只检查可行性 / Feasibility-only subproblem
 * @property verify 合并解的完整模型复核 / Full-model verification of the merged solution
 */
data class LinearBendersModels(
    val master: LinearMetaModel<Flt64>,
    val sub: LinearMetaModel<Flt64>,
    val bindings: Map<String, AbstractVariableItem<*, *>>,
    val theta: AbstractVariableItem<*, *>,
    val feasibilityOnly: Boolean = false,
    val verify: (Map<AbstractVariableItem<*, *>, Flt64>) -> Try = { ok }
)

/** 一轮轨迹。 / One iteration snapshot.
 * @property lowerBound 全局下界 / Global lower bound
 * @property upperBound 已验证可行上界 / Verified feasible upper bound
 * @property cuts 新增割数 / Newly added cuts
 */
data class LinearBendersIteration(val lowerBound: Double, val upperBound: Double?, val cuts: Int)

/** 已复核的合并解及真实终态。 / Verified merged solution and actual terminal state.
 * @property values 按变量身份合并的结果 / Values merged by variable identity
 * @property objective 完整归一化目标 / Complete normalized objective
 * @property gap 全局上下界间隙 / Global bound gap
 * @property iterations 轨迹 / Trace
 * @property termination 终止原因 / Termination reason
 */
data class LinearBendersResult(
    val values: Map<AbstractVariableItem<*, *>, Flt64>,
    val objective: Flt64,
    val gap: Double,
    val iterations: List<LinearBendersIteration>,
    val status: String = "Optimal",
    val termination: String = "converged"
)

/** 可复用线性 Benders 循环，不放松未固定的整数变量。 / Reusable linear Benders loop with no unfixed integer relaxation.
 * @property solver 后端 / Backend
 */
class LinearBendersAlgorithm(private val solver: LinearBendersDecompositionSolver) {
    /** 执行分解。 / Execute decomposition.
     * @param models 注册结果 / Registered models
     * @param config 执行预算 / Execution budget
     * @return 已证明结果或结构化失败 / Proven result or structured failure
     */
    suspend fun solve(models: LinearBendersModels, config: LinearBendersConfig = LinearBendersConfig()): Ret<LinearBendersResult> {
        fun failed(message: String): Ret<LinearBendersResult> = Failed(ErrorCode.OREngineSolvingException, "Benders 求解失败 / Benders solve failed: $message")
        if (config.iterationLimit < 1 || !config.tolerance.isFinite() || config.tolerance <= 0 || config.timeLimitMillis <= 0 || config.stallLimit < 1) return failed("invalid budget")
        if (models.master.objectCategory != ObjectCategory.Minimum || models.sub.objectCategory != ObjectCategory.Minimum) return failed("models must use minimization")
        if (models.bindings.keys.any { it.isBlank() } || models.bindings.values.toSet().size != models.bindings.size) return failed("invalid binding IDs")
        val fixed = models.bindings.values.toSet()
        val expandedSub = when (val result = LinearMechanismModel(models.sub)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        var subObjective = LinearPolynomial()
        expandedSub.use { expanded ->
            for (part in expanded.objectFunction.subObjects) {
                val sign = if (part.category == ObjectCategory.Minimum) Flt64.one else -Flt64.one
                subObjective += LinearPolynomial(part.linearTerms().map { (coefficient, variable) -> LinearMonomial(sign * coefficient, variable) }, sign * part.constant)
            }
            for (token in expanded.tokens.tokens) {
                if (token.type.isIntegerType && token.variable !in fixed && token.variable.lowerBound != token.variable.upperBound) return failed("unbound expanded integer ${token.name}")
            }
        }
        for (token in models.sub.tokens.tokens) {
            if (token.type.isIntegerType && token.variable !in fixed && token.variable.lowerBound != token.variable.upperBound) return failed("unbound integer ${token.name}")
        }
        val start = System.nanoTime()
        val trace = arrayListOf<LinearBendersIteration>()
        val cutKeys = hashSetOf<String>()
        var lower = Double.NEGATIVE_INFINITY
        var upper = Double.POSITIVE_INFINITY
        var incumbent: Map<AbstractVariableItem<*, *>, Flt64>? = null
        var stalls = 0
        fun stopped(reason: String): Ret<LinearBendersResult> {
            val values = incumbent ?: return failed(reason)
            val gap = if (lower.isFinite()) maxOf(0.0, upper - lower) / maxOf(1.0, abs(upper)) else Double.POSITIVE_INFINITY
            return Ok(LinearBendersResult(values = values, objective = Flt64(upper), gap = gap, iterations = trace, status = "Feasible", termination = reason))
        }
        for (iteration in 1..config.iterationLimit) {
            if ((System.nanoTime() - start) / 1000000 >= config.timeLimitMillis) return stopped("time limit without an incumbent")
            val remaining = config.timeLimitMillis - (System.nanoTime() - start) / 1000000
            val master = when (val result = solver.withTimeLimit(remaining.milliseconds).solveMaster(models.master)) {
                is Ok -> result.value as? SolveReport<*> ?: return failed("missing master incumbent")
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            val masterOptimal = master.toSolverStatus() == SolverStatus.Optimal
            if (master.toSolverStatus() == SolverStatus.Infeasible) {
                if (incumbent != null) return failed("infeasible master contradicts a verified incumbent")
                return Failed(ErrorCode.ORModelInfeasible, "Benders 主问题已证明不可行 / Benders master is proven infeasible")
            }
            if (master.solution == null) return stopped("master has no incumbent: ${master.toSolverStatus()}")
            val masterValues = linkedMapOf<AbstractVariableItem<*, *>, Flt64>()
            for (token in models.master.tokens.tokens) {
                val index = models.master.tokens.indexOf(token.variable) ?: return failed("missing master token ${token.name}")
                val value = master.values.getOrNull(index) as? Flt64 ?: return failed("missing master value ${token.name}")
                if (!value.isFinite()) return failed("nonfinite master value")
                masterValues[token.variable] = value
            }
            val masterObj = (master.solution?.objective as? Flt64)?.toDouble() ?: return failed("missing master objective")
            val masterBound = master.bestBound?.toDouble() ?: if (masterOptimal) masterObj else null
            if (masterBound != null && masterBound.isFinite()) lower = maxOf(lower, masterBound)
            val bindingValues = linkedMapOf<AbstractVariableItem<*, *>, Flt64>()
            for ((id, variable) in models.bindings) {
                bindingValues[variable] = masterValues[variable] ?: return failed("missing binding $id")
            }
            models.sub.tokens.clearSolution()
            val subRemaining = config.timeLimitMillis - (System.nanoTime() - start) / 1000000
            if (subRemaining <= 0) return stopped("time limit before subproblem")
            val sub = when (val result = solver.withTimeLimit(subRemaining.milliseconds).solveSub(
                metaModel = models.sub,
                objectVariable = models.theta,
                fixedVariables = bindingValues
            )) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            val subOptimal = sub !is LinearBendersDecompositionSolver.LinearFeasibleResult || sub.result.toSolverStatus() == SolverStatus.Optimal
            val cuts = if (sub is LinearBendersDecompositionSolver.LinearFeasibleResult) {
                val values = masterValues.toMutableMap()
                val recourseTokens = models.sub.tokens.tokensInSolver.filter { it.variable !in bindingValues }
                for ((index, token) in recourseTokens.withIndex()) {
                    val value = models.sub.tokens.find(token.variable)?.resultFlt64
                        ?: sub.solution.getOrNull(index)
                        ?: return failed("missing recourse value ${token.name}")
                    values[token.variable] = value
                }
                val verification = models.verify(values)
                if (verification is Failed) return Failed(verification.error)
                if (verification is Fatal) return Fatal(verification.errors)
                val thetaValue = masterValues[models.theta]?.toDouble() ?: return failed("missing theta token")
                val objective = if (models.feasibilityOnly) masterObj else masterObj - thetaValue + sub.obj.toDouble()
                if (!objective.isFinite()) return failed("nonfinite objective")
                if (objective < upper) {
                    upper = objective
                    incumbent = values
                }
                if (models.feasibilityOnly || !subOptimal) emptyList() else listOf(LinearBendersCuts.optimal(
                    theta = models.theta,
                    fixed = bindingValues,
                    dual = sub.dualSolution,
                    objective = subObjective,
                    value = sub.obj
                ))
            } else {
                sub as LinearBendersDecompositionSolver.LinearInfeasibleResult
                when (val result = LinearBendersCuts.feasible(bindingValues, sub.farkasDualSolution)) {
                    is Ok -> listOf(result.value)
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            }
            val gap = if (upper.isFinite()) maxOf(0.0, upper - lower) / maxOf(1.0, abs(upper)) else Double.POSITIVE_INFINITY
            if (upper.isFinite() && lower > upper + config.tolerance * maxOf(1.0, abs(upper))) return failed("invalid bounds: lower=$lower upper=$upper")
            var added = 0
            for (cut in cuts) {
                val difference = cut.lhs - cut.rhs
                val combined = difference.monomials.groupBy { it.symbol }.mapValues { (_, terms) -> terms.fold(Flt64.zero) { acc, term -> acc + term.coefficient } }
                if (combined.any { !it.value.isFinite() } || !difference.constant.isFinite()) return failed("nonfinite cut")
                if (combined.keys.any { it !in masterValues }) return failed("cut contains a non-master symbol")
                val scale = (combined.values.map { it.abs() } + difference.constant.abs()).maxOrNull()?.takeIf { it > Flt64.zero } ?: Flt64.one
                val key = cut.comparison.toString() + ":" + (difference.constant / scale).toString() + ":" + combined.entries.filter { it.value != Flt64.zero }.sortedBy { (it.key as AbstractVariableItem<*, *>).key.toString() }.joinToString { "${(it.key as AbstractVariableItem<*, *>).key}=${it.value / scale}" }
                if (!cutKeys.add(key)) continue
                val result = models.master.addConstraint(cut, name = "benders_cut_${cutKeys.size}")
                if (result is Failed) return Failed(result.error)
                if (result is Fatal) return Fatal(result.errors)
                added++
            }
            trace.add(LinearBendersIteration(lower, upper.takeIf { it.isFinite() }, added))
            if (!subOptimal) return stopped("nonoptimal recourse termination")
            if (!masterOptimal) return stopped("nonoptimal master termination")
            if (incumbent != null && gap <= config.tolerance) return Ok(LinearBendersResult(incumbent, Flt64(upper), gap, trace))
            if ((System.nanoTime() - start) / 1000000 >= config.timeLimitMillis) return stopped("time limit after subproblem")
            stalls = if (added == 0) stalls + 1 else 0
            if (stalls >= config.stallLimit) return stopped("stalled; lower=$lower upper=$upper")
        }
        return stopped("iteration limit ${config.iterationLimit}; lower=$lower upper=$upper")
    }
}

/** 扩展变量的分解阶段；整数变量只能属于主问题。 / Extension-variable stage; integer variables must stay in master. */
enum class LinearBendersStage { Master, ContinuousRecourse }

/** 划分完整已展开模型；默认将所有目标变量和整数变量保留在主问题。
 * / Partition an expanded full model, keeping objective and integer variables in master by default.
 */
data object LinearBendersPartition {
    /** 自动安全划分，包含函数生成的辅助整数。 / Safely partition, including function-generated auxiliary integers.
     * @param source 完整模型 / Full model
     * @param stages 扩展显式声明的变量阶段 / Explicit variable stages declared by extensions
     * @return 主子模型与独立完整模型复核器 / Models and independent full-model verifier
     */
    suspend fun build(
        source: LinearMetaModel<Flt64>,
        stages: Map<AbstractVariableItem<*, *>, LinearBendersStage> = emptyMap()
    ): Ret<LinearBendersModels> {
        val expanded = when (val result = LinearMechanismModel(source)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        return expanded.use { mechanism ->
            val master = LinearMetaModel<Flt64>(name = "${source.name}_master", converter = IntoValue.Identity)
            val sub = LinearMetaModel<Flt64>(name = "${source.name}_sub", converter = IntoValue.Identity)
            val variables = mechanism.tokens.tokens.map { it.variable }
            if (stages.keys.any { it !in variables } || stages.any { (variable, stage) -> variable.type.isIntegerType && stage == LinearBendersStage.ContinuousRecourse }) {
                return@use Failed(ErrorCode.IllegalArgument, "分解阶段引用未知变量或整数追索 / Unknown stage variable or integer recourse")
            }
            val objectives = mechanism.objectFunction.subObjects
            var objective = LinearPolynomial()
            for (part in objectives) {
                val sign = if (part.category == ObjectCategory.Minimum) Flt64.one else -Flt64.one
                objective += LinearPolynomial(part.linearTerms().map { (coefficient, variable) -> LinearMonomial(sign * coefficient, variable) }, sign * part.constant)
            }
            val objectiveVariables = objective.monomials.filter { it.coefficient != Flt64.zero }.map { it.symbol }.toSet()
            val decisions = variables.filter {
                it.type.isIntegerType || stages[it] == LinearBendersStage.Master ||
                    (it in objectiveVariables && stages[it] != LinearBendersStage.ContinuousRecourse)
            }.toSet()
            val masterObjective = LinearPolynomial(objective.monomials.filter { it.symbol in decisions }, objective.constant)
            val recourseObjective = LinearPolynomial(objective.monomials.filter { it.symbol !in decisions }, Flt64.zero)
            val feasibilityOnly = recourseObjective.monomials.none { it.coefficient != Flt64.zero }
            var recourseLowerBound = Flt64.zero
            for (term in recourseObjective.monomials) {
                if (term.coefficient == Flt64.zero) continue
                val variable = term.symbol as AbstractVariableItem<*, *>
                val bound = (if (term.coefficient > Flt64.zero) variable.lowerBound else variable.upperBound)?.value?.unwrapOrNull()
                if (bound == null || !bound.isFinite()) return@use Failed(ErrorCode.IllegalArgument, "连续追索目标缺少可证明的有限下界 / Continuous recourse objective has no proven finite lower bound: ${variable.name}")
                recourseLowerBound += term.coefficient * bound
            }
            if (!recourseLowerBound.isFinite()) return@use Failed(ErrorCode.IllegalArgument, "连续追索下界非有限值 / Nonfinite recourse lower bound")
            for (variable in variables) {
                var result = sub.add(variable)
                if (result is Failed) return@use Failed(result.error)
                if (result is Fatal) return@use Fatal(result.errors)
                if (variable in decisions) {
                    result = master.add(variable)
                    if (result is Failed) return@use Failed(result.error)
                    if (result is Fatal) return@use Fatal(result.errors)
                }
                if (source.tokens.find(variable) == null) {
                    val added = source.add(variable)
                    if (added is Failed) return@use Failed(added.error)
                    if (added is Fatal) return@use Fatal(added.errors)
                }
            }
            val rows = arrayListOf<Pair<LinearInequality<Flt64>, String>>()
            for (constraint in mechanism.constraints) {
                val cells = constraint.lhs.map { it as LinearCell<Flt64> }
                val lhs = LinearPolynomial(cells.map { LinearMonomial(it.coefficient, it.token.variable) }, Flt64.zero)
                val relation = when (constraint.sign) {
                    ConstraintRelation.LessEqual -> Comparison.LE
                    ConstraintRelation.Equal -> Comparison.EQ
                    ConstraintRelation.GreaterEqual -> Comparison.GE
                }
                val row = LinearInequality(lhs, LinearPolynomial(constraint.rhs), relation)
                rows.add(row to constraint.name)
                val target = if (cells.all { it.token.variable in decisions }) master else sub
                val result = target.addConstraint(row, name = constraint.name)
                if (result is Failed) return@use Failed(result.error)
                if (result is Fatal) return@use Fatal(result.errors)
            }
            // 显式边界也作为子问题约束，避免对偶/Farkas 割丢失变量边界贡献。
            // Explicit bounds preserve bound contributions in dual/Farkas cuts.
            for (variable in variables.filter { it !in decisions }) {
                variable.lowerBound?.value?.unwrapOrNull()?.takeIf { it.isFinite() }?.let {
                    val result = sub.addConstraint(LinearPolynomial(variable) geq it, name = "bound_lb_${variable.key}")
                    if (result is Failed) return@use Failed(result.error)
                    if (result is Fatal) return@use Fatal(result.errors)
                }
                variable.upperBound?.value?.unwrapOrNull()?.takeIf { it.isFinite() }?.let {
                    val result = sub.addConstraint(LinearPolynomial(variable) leq it, name = "bound_ub_${variable.key}")
                    if (result is Failed) return@use Failed(result.error)
                    if (result is Fatal) return@use Fatal(result.errors)
                }
            }
            val theta = RealVar("benders_recourse_projection")
            if (feasibilityOnly) theta.range.eq(Flt64.zero) else theta.range.geq(recourseLowerBound)
            val completeMasterObjective = if (feasibilityOnly) masterObjective else masterObjective + LinearPolynomial(theta)
            for (result in listOf(master.add(theta), master.minimize(completeMasterObjective, name = "complete_objective"), sub.minimize(recourseObjective, name = "recourse"))) {
                if (result is Failed) return@use Failed(result.error)
                if (result is Fatal) return@use Fatal(result.errors)
            }
            Ok(LinearBendersModels(
                master = master,
                sub = sub,
                bindings = decisions.associateBy { it.key.toString() },
                theta = theta,
                feasibilityOnly = feasibilityOnly,
                verify = { values ->
                    for (variable in variables) {
                        val value = values[variable] ?: return@LinearBendersModels Failed(ErrorCode.DataNotFound, "合并解缺少变量 / Missing merged variable: ${variable.name}")
                        if (!value.isFinite() || variable.lowerBound?.value?.unwrapOrNull()?.let { value < it - Flt64(1e-6) } == true || variable.upperBound?.value?.unwrapOrNull()?.let { value > it + Flt64(1e-6) } == true ||
                            (variable.type.isIntegerType && abs(value.toDouble() - kotlin.math.round(value.toDouble())) > 1e-6)) return@LinearBendersModels Failed(ErrorCode.ApplicationError, "合并解违反变量范围 / Merged solution violates bounds: ${variable.name}")
                    }
                    for ((row, name) in rows) {
                        val lhs = row.lhs.monomials.fold(row.lhs.constant) { acc, term -> acc + term.coefficient * values.getValue(term.symbol as AbstractVariableItem<*, *>) }
                        val rhs = row.rhs.constant
                        val valid = when (row.comparison) {
                            Comparison.LE -> lhs <= rhs + Flt64(1e-6)
                            Comparison.GE -> lhs >= rhs - Flt64(1e-6)
                            else -> (lhs - rhs).abs() <= Flt64(1e-6)
                        }
                        if (!valid) return@LinearBendersModels Failed(ErrorCode.ApplicationError, "合并解违反约束 / Merged solution violates constraint: $name")
                    }
                    ok
                }
            ))
        }
    }
}
