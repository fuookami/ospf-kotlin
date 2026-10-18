/**
 * 二维套裁求解入口：MILP 路径与列生成路径。 / 2D cutting stock entry points: the MILP path and the column generation
 * path.
 *
 * 两条路径共用同一套装配与恢复逻辑，差别只在「候选方案怎么来」：
 * 1. **MILP 路径**：一次生成全部候选图案，解一次主问题。
 * 2. **列生成路径**：先解一次松弛主问题，按**剩余需求**再生成图案并追加列，重复到没有新列或到达迭代上限，
 *    最后解一次最终整数主问题。
 *
 * 这里刻意说明清楚：本阶段的列追加由**剩余需求**驱动，而不是由检验数驱动。两者都属于列生成思想，
 * 但后者需要从 LP 对偶里取出需求约束的影子价格；对偶提取留作后续，不在此阶段声称已实现。
 * / Both paths share the same assembly and recovery logic and differ only in where the candidate plans come from: the
 * MILP path generates every candidate once and solves the master problem once, while the column generation path solves
 * the relaxed master first, regenerates patterns against the **residual demand** and appends columns, repeating until no
 * new column appears or the iteration limit is reached, then solves the final integer master problem.
 *
 * This is stated explicitly: at this stage the column addition is driven by the **residual demand**, not by reduced
 * costs. Both belong to the column generation idea, but the latter needs the demand shadow prices from the LP duals;
 * dual extraction is left for a later stage and is not claimed here.
*/
package fuookami.ospf.kotlin.framework.csp2d.application.service

import kotlin.math.floor
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.MetaModelConfiguration
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.framework.solver.ColumnGenerationSolver
import fuookami.ospf.kotlin.framework.csp2d.application.model.*
import fuookami.ospf.kotlin.framework.csp2d.domain.material.model.*
import fuookami.ospf.kotlin.framework.csp2d.domain.pattern_generation.*
import fuookami.ospf.kotlin.framework.csp2d.domain.produce.*
import fuookami.ospf.kotlin.framework.csp2d.domain.sheet_assignment.*
import fuookami.ospf.kotlin.framework.csp2d.domain.wasting_minimization.Csp2dWasteMinimization
import fuookami.ospf.kotlin.framework.csp2d.domain.yield.Csp2dOverProductionBoundConstraint
import fuookami.ospf.kotlin.framework.csp2d.infrastructure.Csp2dSolvingError

/**
 * 创建线性元模型。 / Create a linear meta model.
 *
 * @param name 模型名称 / model name
 * @return 元模型 / meta model
*/
internal fun newCsp2dModel(name: String): LinearMetaModel<FltX> {
    return LinearMetaModel(
        name = name,
        objectCategory = ObjectCategory.Minimum,
        configuration = MetaModelConfiguration(),
        converter = IntoValue.fromConverter(FltX)
    )
}

/**
 * 把求解结果规整成 `FltX` 列表。 / Normalise the solver result into a `FltX` list.
 *
 * @param values 原始解值 / raw solution values
 * @return 规整结果 / normalised values
*/
internal fun normalizeCsp2dSolution(values: List<*>): Ret<List<FltX>> {
    val normalized = ArrayList<FltX>(values.size)
    for ((index, value) in values.withIndex()) {
        when (value) {
            is FltX -> normalized.add(value)
            is Number -> normalized.add(FltX(value.toDouble()))
            else -> return Failed(
                Csp2dSolvingError(
                    detail = "不支持的解值类型 / unsupported solution value at $index: ${value?.javaClass?.name}"
                )
            )
        }
    }
    return ok(normalized)
}

/**
 * 读取变量的解值。 / Read the solution value of a variable.
 *
 * @param model 元模型 / meta model
 * @param variable 变量 / variable
 * @return 解值 / solution value
*/
internal fun csp2dTokenValue(
    model: LinearMetaModel<FltX>,
    variable: fuookami.ospf.kotlin.core.variable.AbstractVariableItem<*, *>
): FltX {
    val token = model.tokens.find(variable) ?: return FltX.zero
    return token.doubleResult?.let { FltX(it) } ?: FltX.zero
}

/**
 * 由问题生成初始候选方案。 / Build the initial candidate plans of a problem.
 *
 * 每个物料按各自的供给模式生成图案：卷材按段高上界排样，定尺按整块板材排样。
 * / Patterns are generated per material according to its supply mode: coils are laid out against the segment height
 * bound and fixed sheets against the whole sheet.
 *
 * @param problem 问题 / problem
 * @param products 参与生成的产品及件数 / products and counts taking part
 * @param iteration 迭代序号 / iteration index
 * @return 候选方案 / candidate plans
*/
internal fun buildCsp2dPlans(
    problem: Csp2dProblem,
    products: List<Pair<Product2<FltX>, UInt64>>,
    iteration: Int = 0
): Ret<List<CuttingPlan2<FltX>>> {
    val context = PatternGenerationContext2<FltX>()
    val catalogue = problem.productsById()
    val plans = ArrayList<CuttingPlan2<FltX>>()
    for ((materialIndex, material) in problem.materials.withIndex()) {
        val request = PatternGenerationRequest2(
            material = material,
            products = products,
            kerf = problem.kerf,
            process = problem.process,
            costars = problem.costars,
            maxCandidates = problem.config.maxCandidatesPerIteration,
            iteration = iteration
        )
        val patterns = when (val generated = context.generate(request)) {
            is Ok -> generated.value
            is Failed -> return Failed(generated.error)
            is Fatal -> return Fatal(generated.errors)
        }
        for ((patternIndex, result) in patterns.withIndex()) {
            val plan = when (
                val built = cuttingPlan2Of(
                    id = planId2Of("P${materialIndex}_${iteration}_$patternIndex"),
                    pattern = result.pattern,
                    products = catalogue
                )
            ) {
                is Ok -> built.value
                is Failed -> return Failed(built.error)
                is Fatal -> return Fatal(built.errors)
            }
            plans.add(plan)
        }
    }
    if (plans.isEmpty()) {
        return Failed(
            Csp2dSolvingError(detail = "没有生成任何候选方案 / No candidate plan was generated")
        )
    }
    return ok(plans)
}

/**
 * 装配主问题。 / Assemble the master problem.
 *
 * @param name 模型名称 / model name
 * @param problem 问题 / problem
 * @param plans 候选方案 / candidate plans
 * @return 元模型与使用量变量 / the meta model and the usage variables
*/
internal fun assembleCsp2dMaster(
    name: String,
    problem: Csp2dProblem,
    plans: List<CuttingPlan2<FltX>>
): Ret<Pair<LinearMetaModel<FltX>, Csp2dPatternAssignment>> {
    val model = newCsp2dModel(name)
    val assignment = Csp2dPatternAssignment(plans)
    when (val result = assignment.register(model = model, demandUpperBound = problem.totalPieceDemand())) {
        is Ok -> {}
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }

    when (
        val result = Csp2dDemandConstraint(
            plans = plans,
            assignment = assignment,
            demands = problem.demands
        ).invoke(model)
    ) {
        is Ok -> {}
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }

    when (
        val result = Csp2dMaterialAvailabilityConstraint(
            plans = plans,
            assignment = assignment,
            materials = problem.materials
        ).invoke(model)
    ) {
        is Ok -> {}
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }

    when (
        val result = Csp2dMachineCapacityConstraint(
            plans = plans,
            assignment = assignment,
            machines = problem.machines
        ).invoke(model)
    ) {
        is Ok -> {}
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }

    when (
        val result = Csp2dOverProductionBoundConstraint(
            plans = plans,
            assignment = assignment,
            demands = problem.demands,
            config = problem.yieldConfig
        ).invoke(model)
    ) {
        is Ok -> {}
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }

    when (
        val result = Csp2dConsumedLengthConstraint(
            plans = plans,
            assignment = assignment,
            config = problem.sheetConfig
        ).invoke(model)
    ) {
        is Ok -> {}
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }

    // 主目标按模式选择，次目标保持极小权重以避免在等价解之间多开料。
    // The primary objective follows the mode while the secondary terms keep tiny weights so that equivalent solutions do
    // not open extra material.
    val primaryRegistered = when (problem.primaryObjective()) {
        Csp2dObjectiveKind.ConsumedLength -> Csp2dConsumedLengthObjective(
            plans = plans,
            assignment = assignment
        ).invoke(model)

        Csp2dObjectiveKind.SheetCount -> Csp2dSheetCountObjective(
            plans = plans,
            assignment = assignment
        ).invoke(model)

        Csp2dObjectiveKind.Waste -> Csp2dWasteMinimization(
            plans = plans,
            assignment = assignment,
            weight = problem.config.primaryWeight
        ).invoke(model)
    }
    when (primaryRegistered) {
        is Ok -> {}
        is Failed -> return Failed(primaryRegistered.error)
        is Fatal -> return Fatal(primaryRegistered.errors)
    }

    if (problem.config.wasteWeight != FltX.zero && problem.primaryObjective() != Csp2dObjectiveKind.Waste) {
        when (
            val result = Csp2dWasteMinimization(
                plans = plans,
                assignment = assignment,
                weight = problem.config.wasteWeight
            ).invoke(model)
        ) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
    }

    if (problem.config.sheetCountWeight != FltX.zero && problem.primaryObjective() != Csp2dObjectiveKind.SheetCount) {
        when (
            val result = Csp2dSheetCountObjective(
                plans = plans,
                assignment = assignment
            ).invoke(model)
        ) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
    }

    return ok(model to assignment)
}

/**
 * 读取方案使用次数。 / Read the plan usage counts.
 *
 * @param model 元模型 / meta model
 * @param assignment 使用量变量 / usage variables
 * @return 使用次数，向下取整 / usage counts, floored
*/
internal fun readCsp2dUsages(
    model: LinearMetaModel<FltX>,
    assignment: Csp2dPatternAssignment
): List<Int> {
    return assignment.plans.indices.map { index ->
        val value = csp2dTokenValue(model, assignment.usage[index])
        floor(value.toDouble()).toInt().coerceAtLeast(0)
    }
}

/**
 * 由使用次数构造解。 / Build the solution from the usage counts.
 *
 * @param problem 问题 / problem
 * @param plans 候选方案 / candidate plans
 * @param usages 使用次数 / usage counts
 * @param iterations 迭代次数 / iteration count
 * @return 解 / solution
*/
internal fun csp2dSolutionOf(
    problem: Csp2dProblem,
    plans: List<CuttingPlan2<FltX>>,
    usages: List<Int>,
    iterations: Int
): Ret<Csp2dSolution> {
    val assignmentResult = csp2dSheetAssignmentOf(
        plans = plans,
        usages = usages,
        config = problem.sheetConfig
    )
    val sheetAssignment = when (assignmentResult) {
        is Ok -> assignmentResult.value
        is Failed -> return Failed(assignmentResult.error)
        is Fatal -> return Fatal(assignmentResult.errors)
    }

    val kept = ArrayList<Csp2dPlanUsage>()
    val coverage = LinkedHashMap<String, Int>()
    var usedArea = FltX.zero
    var wasteArea = FltX.zero
    var consumedArea = FltX.zero
    for ((index, plan) in plans.withIndex()) {
        val usage = usages[index]
        if (usage <= 0) {
            continue
        }
        kept.add(Csp2dPlanUsage(plan = plan, usage = usage))
        usedArea += plan.usedArea.value * FltX(usage.toDouble())
        wasteArea += plan.wasteArea.value * FltX(usage.toDouble())
        consumedArea += plan.pattern.region.area.value * FltX(usage.toDouble())
        for (contribution in plan.contributions) {
            val productId = contribution.product.id.value
            coverage[productId] = (coverage[productId] ?: 0) + contribution.amount.toInt() * usage
        }
    }

    val utilization = if (consumedArea > FltX.zero) usedArea / consumedArea else FltX.zero
    return Ok(
        Csp2dSolution(
            usages = kept,
            consumedLength = sheetAssignment.consumedLength,
            sheetCount = sheetAssignment.sheetCount,
            usedArea = usedArea,
            wasteArea = wasteArea,
            utilization = utilization,
            coverage = coverage,
            iterations = iterations,
            kpi = mapOf(
                "sheet_count" to sheetAssignment.sheetCount.toString(),
                "consumed_length" to sheetAssignment.consumedLength.toString(),
                "used_area" to usedArea.toString(),
                "waste_area" to wasteArea.toString(),
                "utilization" to utilization.toString(),
                "over_length" to sheetAssignment.overLength.toString(),
                "over_length_detected" to sheetAssignment.overLengthDetected.toString(),
                "iteration_count" to iterations.toString()
            )
        )
    )
}

/**
 * MILP 求解器：一次生成全部候选图案，解一次整数主问题。 / The MILP solver: generate every candidate once and solve one
 * integer master problem.
 *
 * @property solver 求解器 / solver
*/
class Csp2dMilp(
    private val solver: ColumnGenerationSolver
) {

    /**
     * 求解问题。 / Solve the problem.
     *
     * @param problem 问题 / problem
     * @return 解 / solution
    */
    suspend fun solve(problem: Csp2dProblem): Ret<Csp2dSolution> {
        when (val validated = problem.validate()) {
            is Ok -> {}
            is Failed -> return Failed(validated.error)
            is Fatal -> return Fatal(validated.errors)
        }
        val products = problem.demands.map { it.product to (it.piecesOrNull() ?: UInt64.one) }
        val plans = when (val built = buildCsp2dPlans(problem = problem, products = products)) {
            is Ok -> built.value
            is Failed -> return Failed(built.error)
            is Fatal -> return Fatal(built.errors)
        }
        val assembly = when (val result = assembleCsp2dMaster(name = "csp2d-milp", problem = problem, plans = plans)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val (model, assignment) = assembly

        val solved = when (
            val result = solver.solveMILPAs(
                name = "csp2d-milp",
                metaModel = model
            )
        ) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        when (val normalized = normalizeCsp2dSolution(solved.values)) {
            is Ok -> model.setSolution(normalized.value)
            is Failed -> return Failed(normalized.error)
            is Fatal -> return Fatal(normalized.errors)
        }
        val usages = readCsp2dUsages(model = model, assignment = assignment)
        return csp2dSolutionOf(problem = problem, plans = plans, usages = usages, iterations = 1)
    }
}

/**
 * 列生成求解器：按剩余需求逐轮追加图案，最后解一次整数主问题。 / The column generation solver: append patterns round by
 * round against the residual demand and solve one integer master problem at the end.
 *
 * 每轮的松弛主问题给出当前能覆盖多少需求；剩余需求不为零时按剩余量重新生成图案，只追加**新图案**（按放置集合与
 * 料域去重）。这样循环一定终止：图案集合有限且每轮至少新增一个，或剩余需求被覆盖。
 * / Each round's relaxed master problem shows how much demand the current columns cover; while residual demand remains,
 * patterns are regenerated against the residual and only **new** patterns (deduplicated by placement set and region) are
 * appended. The loop therefore terminates: either the pattern set is finite and every round adds at least one, or the
 * residual demand is covered.
 *
 * @property solver 求解器 / solver
*/
class Csp2dColumnGeneration(
    private val solver: ColumnGenerationSolver
) {

    /**
     * 求解问题。 / Solve the problem.
     *
     * @param problem 问题 / problem
     * @return 解 / solution
    */
    suspend fun solve(problem: Csp2dProblem): Ret<Csp2dSolution> {
        when (val validated = problem.validate()) {
            is Ok -> {}
            is Failed -> return Failed(validated.error)
            is Fatal -> return Fatal(validated.errors)
        }
        val required = problem.demands.associate { demand ->
            demand.product.id.value to (demand.piecesOrNull()?.toInt() ?: 0)
        }
        var plans = when (
            val built = buildCsp2dPlans(problem = problem, products = initialProducts(problem))
        ) {
            is Ok -> built.value
            is Failed -> return Failed(built.error)
            is Fatal -> return Fatal(built.errors)
        }
        val known = LinkedHashSet(plans.map { patternKey(it.pattern) })
        var iteration = 1
        var usages = List(plans.size) { 0 }

        while (iteration < problem.config.iterationLimit) {
            val assembly = when (
                val result = assembleCsp2dMaster(
                    name = "csp2d-rmp-$iteration",
                    problem = problem,
                    plans = plans
                )
            ) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            val (rmpModel, rmpAssignment) = assembly
            val relaxed = when (
                val result = solver.solveLPAs(
                    name = "csp2d-rmp-$iteration",
                    metaModel = rmpModel
                )
            ) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            when (val normalized = normalizeCsp2dSolution(relaxed.solution)) {
                is Ok -> rmpModel.setSolution(normalized.value)
                is Failed -> return Failed(normalized.error)
                is Fatal -> return Fatal(normalized.errors)
            }
            usages = readCsp2dUsages(model = rmpModel, assignment = rmpAssignment)

            val produced = coverageOf(plans = plans, usages = usages)
            val residual = required.mapValues { (productId, count) ->
                (count - (produced[productId] ?: 0)).coerceAtLeast(0)
            }
            if (residual.values.all { it == 0 }) {
                break
            }
            val residualProducts = problem.demands
                .filter { (residual[it.product.id.value] ?: 0) > 0 }
                .map { it.product to UInt64(residual[it.product.id.value] ?: 0) }
            if (residualProducts.isEmpty()) {
                break
            }

            val added = when (
                val built = buildCsp2dPlans(
                    problem = problem,
                    products = residualProducts,
                    iteration = iteration
                )
            ) {
                is Ok -> built.value
                is Failed -> return Failed(built.error)
                is Fatal -> return Fatal(built.errors)
            }
            var appended = 0
            val merged = ArrayList(plans)
            for (plan in added) {
                if (known.add(patternKey(plan.pattern))) {
                    merged.add(plan)
                    appended += 1
                }
            }
            if (appended == 0) {
                // 没有新列，收敛。 / No new column, so the loop has converged.
                break
            }
            plans = merged
            iteration += 1
        }

        // 最终整数主问题。 / The final integer master problem.
        val finalAssembly = when (
            val result = assembleCsp2dMaster(
                name = "csp2d-final",
                problem = problem,
                plans = plans
            )
        ) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val (model, assignment) = finalAssembly
        val solved = when (
            val result = solver.solveMILPAs(
                name = "csp2d-final",
                metaModel = model
            )
        ) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        when (val normalized = normalizeCsp2dSolution(solved.values)) {
            is Ok -> model.setSolution(normalized.value)
            is Failed -> return Failed(normalized.error)
            is Fatal -> return Fatal(normalized.errors)
        }
        val finalUsages = readCsp2dUsages(model = model, assignment = assignment)
        return csp2dSolutionOf(
            problem = problem,
            plans = plans,
            usages = finalUsages,
            iterations = iteration
        )
    }

    /**
     * 初始参与生成的产品与件数。 / The products and counts taking part in the first generation.
     *
     * @param problem 问题 / problem
     * @return 产品与件数 / products and counts
    */
    private fun initialProducts(problem: Csp2dProblem): List<Pair<Product2<FltX>, UInt64>> {
        return problem.demands.map { demand ->
            demand.product to (demand.piecesOrNull() ?: UInt64.one)
        }
    }

    /**
     * 统计当前方案集合的产出件数。 / Count the produced pieces of the current plan set.
     *
     * @param plans 方案 / plans
     * @param usages 使用次数 / usage counts
     * @return 产品编号到件数 / product identifier to piece count
    */
    private fun coverageOf(plans: List<CuttingPlan2<FltX>>, usages: List<Int>): Map<String, Int> {
        val coverage = LinkedHashMap<String, Int>()
        for ((index, plan) in plans.withIndex()) {
            val usage = usages[index]
            if (usage <= 0) {
                continue
            }
            for (contribution in plan.contributions) {
                val productId = contribution.product.id.value
                coverage[productId] = (coverage[productId] ?: 0) + contribution.amount.toInt() * usage
            }
        }
        return coverage
    }
}
