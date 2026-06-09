package fuookami.ospf.kotlin.framework.csp1d.application.service

import kotlin.math.roundToLong
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.solver.output.FeasibleSolverOutput
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.URealVar
import fuookami.ospf.kotlin.framework.solver.ColumnGenerationSolver
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.CuttingPlanCanonicalKey
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.canonicalKey
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlan
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Machine
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.MachineBatchShadowPriceKey
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.MachineCapacityShadowPriceKey
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Material
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.MaterialUsageShadowPriceKey
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.ProductDemand
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.ProductDemandShadowPriceKey
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.ShadowPriceKey
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.ShadowPriceMap
import fuookami.ospf.kotlin.framework.csp1d.domain.length_assignment.model.LengthAssignmentModelingConfig
import fuookami.ospf.kotlin.framework.csp1d.domain.length_assignment.model.LengthAssignmentModelingResult
import fuookami.ospf.kotlin.framework.csp1d.domain.length_assignment.model.ModeledAssignedLength
import fuookami.ospf.kotlin.framework.csp1d.domain.length_assignment.model.ModeledOverLength
import fuookami.ospf.kotlin.framework.csp1d.domain.yield.model.YieldModelingConfig
import fuookami.ospf.kotlin.framework.csp1d.domain.yield.model.YieldModelingResult
import fuookami.ospf.kotlin.framework.csp1d.domain.yield.model.ModeledUnderProduction
import fuookami.ospf.kotlin.framework.csp1d.domain.yield.model.ModeledOverProduction
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.ProduceInput
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.CuttingPlanUsage
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.MachineCapacityUsage
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.MaterialUsage
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.Produce
import fuookami.ospf.kotlin.framework.csp1d.application.model.Csp1dAssignment

class Csp1dMilpSolver(
    private val solver: ColumnGenerationSolver
) {
    data class MilpResult<V : RealNumber<V>>(
        val produce: Produce<V>,
        val yieldResult: YieldModelingResult<V>? = null,
        val wasteResult: WasteMinimizationResult<V>? = null,
        val lengthResult: LengthAssignmentModelingResult<V>? = null,
        val model: LinearMetaModel<Flt64>,
        val assignment: Csp1dAssignment,
        val output: FeasibleSolverOutput<Flt64>
    )

    data class LpResult<V : RealNumber<V>>(
        val shadowPrices: ShadowPriceMap<V>,
        val model: LinearMetaModel<Flt64>,
        val assignment: Csp1dAssignment,
        val lpOutput: ColumnGenerationSolver.LPResult
    )

    suspend fun <V : RealNumber<V>> solve(
        input: ProduceInput<V>,
        yieldConfig: YieldModelingConfig<V>? = null,
        wasteConfig: WasteMinimizationConfig<V>? = null,
        lengthConfig: LengthAssignmentModelingConfig<V>? = null
    ): MilpResult<V>? {
        return try {
            solveInternal(input, yieldConfig, wasteConfig, lengthConfig)
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun <V : RealNumber<V>> solveInternal(
        input: ProduceInput<V>,
        yieldConfig: YieldModelingConfig<V>?,
        wasteConfig: WasteMinimizationConfig<V>?,
        lengthConfig: LengthAssignmentModelingConfig<V>?
    ): MilpResult<V>? {
        if (input.cuttingPlans.isEmpty()) {
            return null
        }

        val model = LinearMetaModel(
            name = "csp1d_produce",
            converter = IntoValue.Identity
        )
        val assignment = Csp1dAssignment.create(input.cuttingPlans.size)
        ensureTry(assignment.register(model), "register assignment")

        val yieldSlackVars = addYieldSlackVariables(
            demands = input.demands,
            yieldConfig = yieldConfig,
            wasteConfig = wasteConfig,
            model = model
        )

        val resolvedLengthConfig = resolveDefaultLengthBounds(
            input = input,
            lengthConfig = lengthConfig
        )

        val lengthSlackVars = addLengthAssignmentSlackVariables(
            demands = input.demands,
            lengthConfig = resolvedLengthConfig,
            model = model
        )

        addDemandConstraints(
            input = input,
            assignment = assignment,
            model = model,
            yieldSlackVars = yieldSlackVars,
            yieldConfig = yieldConfig
        )
        addMaterialConstraints(
            input = input,
            assignment = assignment,
            model = model
        )
        addMachineConstraints(
            input = input,
            assignment = assignment,
            model = model
        )
        addOverProductionUpperBoundConstraints(
            demands = input.demands,
            yieldConfig = yieldConfig,
            yieldSlackVars = yieldSlackVars,
            model = model
        )
        addLengthAssignmentConstraints(
            demands = input.demands,
            lengthConfig = resolvedLengthConfig,
            lengthSlackVars = lengthSlackVars,
            model = model
        )
        setObjective(
            assignment = assignment,
            yieldConfig = yieldConfig,
            wasteConfig = wasteConfig,
            lengthConfig = resolvedLengthConfig,
            demands = input.demands,
            cuttingPlans = input.cuttingPlans,
            yieldSlackVars = yieldSlackVars,
            lengthSlackVars = lengthSlackVars,
            model = model
        )
        applyWarmStartInitialSolution(
            input = input,
            assignment = assignment,
            model = model
        )

        val output = ensureRet(
            result = solver.solveMILP(
                name = "csp1d-produce",
                metaModel = model
            ),
            stage = "solve CSP1D produce MILP"
        )
        model.setSolution(output.solution)

        val produce = extractProduce(
            input = input,
            assignment = assignment,
            model = model
        )
        val yieldResult = extractYieldResult(
            demands = input.demands,
            yieldConfig = yieldConfig,
            yieldSlackVars = yieldSlackVars,
            model = model
        )
        val wasteResult = extractWasteResult(
            cuttingPlans = input.cuttingPlans,
            wasteConfig = wasteConfig,
            demands = input.demands,
            yieldConfig = yieldConfig,
            yieldSlackVars = yieldSlackVars,
            assignment = assignment,
            model = model
        )
        val lengthResult = extractLengthResult(
            demands = input.demands,
            lengthConfig = resolvedLengthConfig,
            lengthSlackVars = lengthSlackVars,
            model = model
        )
        return MilpResult(
            produce = produce,
            yieldResult = yieldResult,
            wasteResult = wasteResult,
            lengthResult = lengthResult,
            model = model,
            assignment = assignment,
            output = output
        )
    }

    suspend fun <V : RealNumber<V>> solveLP(
        input: ProduceInput<V>
    ): LpResult<V>? {
        return try {
            solveLPInternal(input)
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun <V : RealNumber<V>> solveLPInternal(
        input: ProduceInput<V>
    ): LpResult<V>? {
        if (input.cuttingPlans.isEmpty()) {
            return null
        }

        val model = LinearMetaModel(
            name = "csp1d_produce_lp",
            converter = IntoValue.Identity
        )
        val assignment = Csp1dAssignment.create(input.cuttingPlans.size)
        val shadowPriceKeys = LinkedHashMap<String, ShadowPriceKey>()
        ensureTry(assignment.register(model), "register assignment")

        // LP 求解不使用 yield slack 变量——保持 demand >= constraint 以提取 shadow price
        addDemandConstraints(
            input = input,
            assignment = assignment,
            model = model,
            shadowPriceKeys = shadowPriceKeys
        )
        addMaterialConstraints(
            input = input,
            assignment = assignment,
            model = model,
            shadowPriceKeys = shadowPriceKeys
        )
        addMachineConstraints(
            input = input,
            assignment = assignment,
            model = model,
            shadowPriceKeys = shadowPriceKeys
        )
        setObjective(
            assignment = assignment,
            model = model
        )

        val lpResult = ensureRet(
            result = solver.solveLP(
                name = "csp1d-produce-lp",
                metaModel = model
            ),
            stage = "solve CSP1D produce LP"
        )

        val shadowPrices = extractShadowPrices<V>(
            lpResult = lpResult,
            shadowPriceKeys = shadowPriceKeys
        )
        return LpResult(
            shadowPrices = shadowPrices,
            model = model,
            assignment = assignment,
            lpOutput = lpResult
        )
    }

    /**
     * 将 warm start 方案使用量写入模型初始解 / Write warm-start plan usages into model initial solution
     */
    private fun <V : RealNumber<V>> applyWarmStartInitialSolution(
        input: ProduceInput<V>,
        assignment: Csp1dAssignment,
        model: LinearMetaModel<Flt64>
    ) {
        if (input.warmStartPlanUsages.isEmpty()) {
            return
        }
        val usageByKey = warmStartUsageByKey(input.warmStartPlanUsages)
        if (usageByKey.isEmpty()) {
            return
        }
        val initialSolution = LinkedHashMap<AbstractVariableItem<*, *>, Flt64>()
        for ((index, plan) in input.cuttingPlans.withIndex()) {
            val usage = usageByKey[plan.canonicalKey()] ?: continue
            if (usage > UInt64.zero) {
                initialSolution[assignment[index]] = usage.toFlt64()
            }
        }
        if (initialSolution.isNotEmpty()) {
            model.setSolution(initialSolution)
        }
    }

    private fun <V : RealNumber<V>> warmStartUsageByKey(
        usages: List<CuttingPlanUsage<V>>
    ): Map<CuttingPlanCanonicalKey, UInt64> {
        val result = LinkedHashMap<CuttingPlanCanonicalKey, UInt64>()
        for (usage in usages) {
            if (usage.amount <= UInt64.zero) {
                continue
            }
            val key = usage.plan.canonicalKey()
            result[key] = (result[key] ?: UInt64.zero) + usage.amount
        }
        return result
    }

    /**
     * yield slack 变量容器 / Yield slack variable container
     *
     * @param underProduction 欠产松弛变量，按 demand 索引；不需要时为 null / Under-production slack variables, indexed by demand; null when not needed
     * @param overProduction 超产松弛变量，按 demand 索引；不需要时为 null / Over-production slack variables, indexed by demand; null when not needed
     */
    private data class YieldSlackVars(
        val underProduction: List<URealVar?> = emptyList(),
        val overProduction: List<URealVar?> = emptyList()
    ) {
        val hasAny: Boolean get() = underProduction.any { it != null } || overProduction.any { it != null }
    }

    /**
     * 长度分配变量容器 / Length assignment variable container
     *
     * @param assignedLength 已分配卷长变量，按 demand 索引；仅对动态长度产品有效 / Assigned length variables, indexed by demand; only for dynamic-length products
     * @param overLength 超长松弛变量，按 demand 索引；仅对动态长度产品有效 / Over-length slack variables, indexed by demand; only for dynamic-length products
     */
    private data class LengthSlackVars(
        val assignedLength: List<URealVar?> = emptyList(),
        val overLength: List<URealVar?> = emptyList()
    ) {
        val hasAny: Boolean get() = assignedLength.any { it != null } || overLength.any { it != null }
    }

    /**
     * 当 yieldConfig 存在时，为每个 demand 创建欠产和超产松弛变量 / When yieldConfig is present, create under/over production slack variables for each demand
     */
    private fun <V : RealNumber<V>> addYieldSlackVariables(
        demands: List<ProductDemand<V>>,
        yieldConfig: YieldModelingConfig<V>?,
        wasteConfig: WasteMinimizationConfig<V>?,
        model: LinearMetaModel<Flt64>
    ): YieldSlackVars {
        if (yieldConfig == null) return YieldSlackVars()

        val underVars = ArrayList<URealVar?>()
        val overVars = ArrayList<URealVar?>()

        for ((demandIndex, demand) in demands.withIndex()) {
            val unitSymbol = shadowPriceUnitSymbol(demand.quantity.unit)
            val demandKey = ProductDemandShadowPriceKey(
                productId = demand.product.id,
                unitSymbol = unitSymbol
            )

            val needsUnderSlack = yieldConfig.underProductionPenalty.containsKey(demandKey)
            val needsOverSlack = yieldConfig.overProductionPenalty.containsKey(demandKey) ||
                    yieldConfig.overProductionUpperBound.containsKey(demandKey) ||
                    wasteConfig?.overProductionAreaPenalty != null

            if (needsUnderSlack) {
                val underVar = URealVar("under_production_$demandIndex")
                when (val result = model.add(underVar)) {
                    is Ok -> {}
                    is Failed -> throw IllegalStateException("register under-production variable failed: ${result.error}")
                    is Fatal -> throw IllegalStateException("register under-production variable fatal: ${result.errors}")
                }
                underVars.add(underVar)
            } else {
                underVars.add(null)
            }

            if (needsOverSlack) {
                val overVar = URealVar("over_production_$demandIndex")
                when (val result = model.add(overVar)) {
                    is Ok -> {}
                    is Failed -> throw IllegalStateException("register over-production variable failed: ${result.error}")
                    is Fatal -> throw IllegalStateException("register over-production variable fatal: ${result.errors}")
                }
                overVars.add(overVar)
            } else {
                overVars.add(null)
            }
        }

        return YieldSlackVars(
            underProduction = underVars,
            overProduction = overVars
        )
    }

    private fun <V : RealNumber<V>> addDemandConstraints(
        input: ProduceInput<V>,
        assignment: Csp1dAssignment,
        model: LinearMetaModel<Flt64>,
        shadowPriceKeys: MutableMap<String, ShadowPriceKey>? = null,
        yieldSlackVars: YieldSlackVars = YieldSlackVars(),
        yieldConfig: YieldModelingConfig<V>? = null
    ) {
        val hasYieldSlack = yieldSlackVars.hasAny

        for ((demandIndex, demand) in input.demands.withIndex()) {
            val contributionMonomials = input.cuttingPlans.mapIndexedNotNull { index, plan ->
                val contribution = plan.demandContributions.find {
                    it.product.id == demand.product.id && it.quantity.unit == demand.quantity.unit
                } ?: return@mapIndexedNotNull null
                LinearMonomial(
                    coefficient = contribution.quantity.value.toFlt64(),
                    symbol = assignment[index]
                )
            }

            val constraintName = "demand_$demandIndex"
            val unitSymbol = shadowPriceUnitSymbol(demand.quantity.unit)
            shadowPriceKeys?.set(
                constraintName,
                ProductDemandShadowPriceKey(
                    productId = demand.product.id,
                    unitSymbol = unitSymbol
                )
            )

            if (hasYieldSlack && yieldConfig != null) {
                val demandKey = ProductDemandShadowPriceKey(
                    productId = demand.product.id,
                    unitSymbol = unitSymbol
                )
                val underVar = yieldSlackVars.underProduction.getOrNull(demandIndex)
                val overVar = yieldSlackVars.overProduction.getOrNull(demandIndex)

                if (underVar != null || overVar != null) {
                    // 等式约束: sum(contrib * x) - over + under = demand
                    val lhs = LinearPolynomial(
                        monomials = buildList {
                            addAll(contributionMonomials)
                            if (underVar != null) {
                                add(LinearMonomial(Flt64.one, underVar))
                            }
                            if (overVar != null) {
                                add(LinearMonomial(Flt64(-1.0), overVar))
                            }
                        },
                        constant = Flt64.zero
                    )
                    val rhs = constantPolynomial(demand.quantity.value.toFlt64())
                    ensureTry(
                        model.addConstraint(
                            relation = LinearInequality(lhs = lhs, rhs = rhs, comparison = Comparison.EQ),
                            name = constraintName
                        ),
                        "build demand equality constraint"
                    )
                } else {
                    // 无 yield slack 的 demand 保持 >= 约束
                    addDemandGeConstraint(contributionMonomials, demand, constraintName, model)
                }
            } else {
                // 无 yield slack 的原始 >= 约束
                addDemandGeConstraint(contributionMonomials, demand, constraintName, model)
            }
        }
    }

    private fun <V : RealNumber<V>> addDemandGeConstraint(
        contributionMonomials: List<LinearMonomial<Flt64>>,
        demand: ProductDemand<V>,
        constraintName: String,
        model: LinearMetaModel<Flt64>
    ) {
        val lhs = LinearPolynomial(
            monomials = contributionMonomials,
            constant = Flt64.zero
        )
        val rhs = constantPolynomial(demand.quantity.value.toFlt64())
        ensureTry(
            model.addConstraint(
                relation = LinearInequality(lhs = lhs, rhs = rhs, comparison = Comparison.GE),
                name = constraintName
            ),
            "build demand constraint"
        )
    }

    private fun <V : RealNumber<V>> addMaterialConstraints(
        input: ProduceInput<V>,
        assignment: Csp1dAssignment,
        model: LinearMetaModel<Flt64>,
        shadowPriceKeys: MutableMap<String, ShadowPriceKey>? = null
    ) {
        for ((materialIndex, material) in input.materials.withIndex()) {
            if (material.availableBatches == UInt64.maximum) {
                continue
            }

            val lhs = LinearPolynomial(
                monomials = input.cuttingPlans.mapIndexedNotNull { index, plan ->
                    if (plan.material.id != material.id) {
                        return@mapIndexedNotNull null
                    }
                    LinearMonomial(
                        coefficient = Flt64.one,
                        symbol = assignment[index]
                    )
                },
                constant = Flt64.zero
            )
            if (lhs.monomials.isEmpty()) {
                continue
            }

            val constraintName = "material_$materialIndex"
            shadowPriceKeys?.set(
                constraintName,
                MaterialUsageShadowPriceKey(material.id)
            )
            ensureTry(
                model.addConstraint(
                    relation = LinearInequality(
                        lhs = lhs,
                        rhs = constantPolynomial(material.availableBatches.toFlt64()),
                        comparison = Comparison.LE
                    ),
                    name = constraintName
                ),
                "build material constraint"
            )
        }
    }

    private fun <V : RealNumber<V>> addMachineConstraints(
        input: ProduceInput<V>,
        assignment: Csp1dAssignment,
        model: LinearMetaModel<Flt64>,
        shadowPriceKeys: MutableMap<String, ShadowPriceKey>? = null
    ) {
        for ((machineIndex, machine) in input.machines.withIndex()) {
            addMachineBatchConstraint(
                input = input,
                assignment = assignment,
                model = model,
                shadowPriceKeys = shadowPriceKeys,
                machineIndex = machineIndex,
                machine = machine
            )
            addMachineCapacityConstraint(
                input = input,
                assignment = assignment,
                model = model,
                shadowPriceKeys = shadowPriceKeys,
                machineIndex = machineIndex,
                machine = machine
            )
        }
    }

    private fun <V : RealNumber<V>> addMachineBatchConstraint(
        input: ProduceInput<V>,
        assignment: Csp1dAssignment,
        model: LinearMetaModel<Flt64>,
        shadowPriceKeys: MutableMap<String, ShadowPriceKey>?,
        machineIndex: Int,
        machine: Machine<V>
    ) {
        val maxBatchCount = machine.maxBatchCount ?: return
        val lhs = LinearPolynomial(
            monomials = input.cuttingPlans.mapIndexedNotNull { index, plan ->
                if (plan.machineId != machine.id) {
                    return@mapIndexedNotNull null
                }
                LinearMonomial(
                    coefficient = Flt64.one,
                    symbol = assignment[index]
                )
            },
            constant = Flt64.zero
        )
        if (lhs.monomials.isEmpty()) {
            return
        }

        val constraintName = "machine_batch_$machineIndex"
        shadowPriceKeys?.set(
            constraintName,
            MachineBatchShadowPriceKey(machine.id)
        )
        ensureTry(
            model.addConstraint(
                relation = LinearInequality(
                    lhs = lhs,
                    rhs = constantPolynomial(maxBatchCount.toFlt64()),
                    comparison = Comparison.LE
                ),
                name = constraintName
            ),
            "build machine batch constraint"
        )
    }

    private fun <V : RealNumber<V>> addMachineCapacityConstraint(
        input: ProduceInput<V>,
        assignment: Csp1dAssignment,
        model: LinearMetaModel<Flt64>,
        shadowPriceKeys: MutableMap<String, ShadowPriceKey>?,
        machineIndex: Int,
        machine: Machine<V>
    ) {
        val capacity = machine.capacity ?: return
        val lhs = LinearPolynomial(
            monomials = input.cuttingPlans.mapIndexedNotNull { index, plan ->
                if (plan.machineId != machine.id) {
                    return@mapIndexedNotNull null
                }
                val consumption = machineCapacityConsumptionValue(
                    plan = plan,
                    machine = machine
                ) ?: return@mapIndexedNotNull null
                LinearMonomial(
                    coefficient = consumption.toFlt64(),
                    symbol = assignment[index]
                )
            },
            constant = Flt64.zero
        )
        if (lhs.monomials.isEmpty()) {
            return
        }

        val constraintName = "machine_capacity_$machineIndex"
        shadowPriceKeys?.set(
            constraintName,
            MachineCapacityShadowPriceKey(machine.id)
        )
        ensureTry(
            model.addConstraint(
                relation = LinearInequality(
                    lhs = lhs,
                    rhs = constantPolynomial(capacity.value.toFlt64()),
                    comparison = Comparison.LE
                ),
                name = constraintName
            ),
            "build machine capacity constraint"
        )
    }

    private fun <V : RealNumber<V>> machineCapacityConsumptionValue(
        plan: CuttingPlan<V>,
        machine: Machine<V>
    ): V? {
        val capacity = machine.capacity ?: return null
        val consumption = plan.capacityConsumption ?: return null
        if (consumption.unit != capacity.unit) {
            return null
        }
        if (consumption.value <= consumption.value.constants.zero) {
            return null
        }
        return consumption.value
    }

    /**
     * 添加超产上限约束: over_production_i <= overProductionUpperBound / Add over-production upper bound constraints
     */
    private fun <V : RealNumber<V>> addOverProductionUpperBoundConstraints(
        demands: List<ProductDemand<V>>,
        yieldConfig: YieldModelingConfig<V>?,
        yieldSlackVars: YieldSlackVars,
        model: LinearMetaModel<Flt64>
    ) {
        if (yieldConfig == null) return
        if (yieldConfig.overProductionUpperBound.isEmpty()) return

        for ((demandIndex, demand) in demands.withIndex()) {
            val unitSymbol = shadowPriceUnitSymbol(demand.quantity.unit)
            val demandKey = ProductDemandShadowPriceKey(
                productId = demand.product.id,
                unitSymbol = unitSymbol
            )
            val upperBound = yieldConfig.overProductionUpperBound[demandKey] ?: continue
            val overVar = yieldSlackVars.overProduction.getOrNull(demandIndex) ?: continue

            ensureTry(
                model.addConstraint(
                    relation = LinearInequality(
                        lhs = LinearPolynomial(
                            monomials = listOf(LinearMonomial(Flt64.one, overVar)),
                            constant = Flt64.zero
                        ),
                        rhs = constantPolynomial(upperBound.toFlt64()),
                        comparison = Comparison.LE
                    ),
                    name = "over_production_bound_$demandIndex"
                ),
                "build over-production upper bound constraint"
            )
        }
    }

    private fun setObjective(
        assignment: Csp1dAssignment,
        model: LinearMetaModel<Flt64>
    ) {
        val objective = LinearPolynomial(
            monomials = (0 until assignment.planCount).map { index ->
                LinearMonomial(
                    coefficient = Flt64.one,
                    symbol = assignment[index]
                )
            },
            constant = Flt64.zero
        )
        ensureTry(
            model.minimize(
                polynomial = objective,
                name = "batch_minimization"
            ),
            "build objective"
        )
    }

    private fun <V : RealNumber<V>> setObjective(
        assignment: Csp1dAssignment,
        yieldConfig: YieldModelingConfig<V>?,
        wasteConfig: WasteMinimizationConfig<V>?,
        lengthConfig: LengthAssignmentModelingConfig<V>?,
        demands: List<ProductDemand<V>>,
        cuttingPlans: List<CuttingPlan<V>>,
        yieldSlackVars: YieldSlackVars,
        lengthSlackVars: LengthSlackVars,
        model: LinearMetaModel<Flt64>
    ) {
        val monomials = ArrayList<LinearMonomial<Flt64>>()

        // 基础目标: 最小化批次 / Base objective: minimize batches
        // 当 lengthConfig.batchMinPenalty 非空时，对 Σx_j 施加额外加权 / Apply extra weight to Σx_j when batchMinPenalty is present
        val batchMinPenalty = lengthConfig?.batchMinPenalty
        val batchCoefficient = if (batchMinPenalty != null) {
            Flt64.one + batchMinPenalty.toFlt64()
        } else {
            Flt64.one
        }
        for (index in 0 until assignment.planCount) {
            monomials.add(LinearMonomial(batchCoefficient, assignment[index]))
        }

        // yield 惩罚目标 / Yield penalty objectives
        if (yieldConfig != null && yieldSlackVars.hasAny) {
            for ((demandIndex, demand) in demands.withIndex()) {
                val unitSymbol = shadowPriceUnitSymbol(demand.quantity.unit)
                val demandKey = ProductDemandShadowPriceKey(
                    productId = demand.product.id,
                    unitSymbol = unitSymbol
                )

                val underPenalty = yieldConfig.underProductionPenalty[demandKey]
                if (underPenalty != null) {
                    val underVar = yieldSlackVars.underProduction.getOrNull(demandIndex)
                    if (underVar != null) {
                        monomials.add(LinearMonomial(underPenalty.toFlt64(), underVar))
                    }
                }

                val overPenalty = yieldConfig.overProductionPenalty[demandKey]
                if (overPenalty != null) {
                    val overVar = yieldSlackVars.overProduction.getOrNull(demandIndex)
                    if (overVar != null) {
                        monomials.add(LinearMonomial(overPenalty.toFlt64(), overVar))
                    }
                }
            }
        }

        // 浪费惩罚目标 / Waste penalty objectives
        if (wasteConfig != null) {
            // 余宽惩罚: sum(restWidth * x_plan * trimWidthPenalty)
            val trimPenalty = wasteConfig.trimWidthPenalty
            if (trimPenalty != null) {
                for (index in 0 until assignment.planCount) {
                    val plan = cuttingPlans[index]
                    val restWidthValue = plan.restWidth?.value ?: continue
                    if (restWidthValue > restWidthValue.constants.zero) {
                        val coeff = restWidthValue.toFlt64() * trimPenalty.toFlt64()
                        monomials.add(LinearMonomial(coeff, assignment[index]))
                    }
                }
            }

            // 余料面积代理惩罚: sum(restWidth * material.length * x_plan * restMaterialPenalty) / Rest material area proxy penalty
            val restMaterialPenalty = wasteConfig.restMaterialPenalty
            if (restMaterialPenalty != null) {
                for (index in 0 until assignment.planCount) {
                    val plan = cuttingPlans[index]
                    val restMaterialValue = restMaterialValue(
                        plan = plan,
                        measure = wasteConfig.restMaterialMeasure
                    ) ?: continue
                    val coeff = restMaterialValue.toFlt64() * restMaterialPenalty.toFlt64()
                    monomials.add(LinearMonomial(coeff, assignment[index]))
                }
            }

            // 物料成本惩罚: sum(x_plan * materialCostPenalty[plan.material.id])
            if (wasteConfig.materialCostPenalty.isNotEmpty()) {
                for (index in 0 until assignment.planCount) {
                    val plan = cuttingPlans[index]
                    val costPenalty = wasteConfig.materialCostPenalty[plan.material.id]
                    if (costPenalty != null) {
                        monomials.add(LinearMonomial(costPenalty.toFlt64(), assignment[index]))
                    }
                }
            }

            // 超产面积代理惩罚: 需要超产松弛变量 / Over-production area proxy penalty: requires over-production slack variables
            val overAreaPenalty = wasteConfig.overProductionAreaPenalty
            if (overAreaPenalty != null && yieldConfig != null && yieldSlackVars.overProduction.any { it != null }) {
                // 超产面积代理 = sum(over_production * product.maxWidth) / Over-production area proxy uses product max width
                for ((demandIndex, demand) in demands.withIndex()) {
                    val overVar = yieldSlackVars.overProduction.getOrNull(demandIndex) ?: continue
                    val productWidthValue = overProductionAreaWidthValue(
                        demand = demand,
                        measure = wasteConfig.overProductionAreaMeasure
                    )?.toFlt64() ?: continue
                    val coeff = productWidthValue * overAreaPenalty.toFlt64()
                    monomials.add(LinearMonomial(coeff, overVar))
                }
            }
        }

        // 长度分配惩罚目标 / Length assignment penalty objectives
        if (lengthConfig != null && lengthSlackVars.hasAny) {
            val totalLengthPenalty = lengthConfig.totalLengthPenalty
            if (totalLengthPenalty != null) {
                for ((demandIndex, demand) in demands.withIndex()) {
                    if (demand.product.id !in lengthConfig.dynamicProductIds) continue
                    val assignedVar = lengthSlackVars.assignedLength.getOrNull(demandIndex) ?: continue
                    monomials.add(LinearMonomial(totalLengthPenalty.toFlt64(), assignedVar))
                }
            }

            for ((demandIndex, demand) in demands.withIndex()) {
                if (demand.product.id !in lengthConfig.dynamicProductIds) continue
                val overVar = lengthSlackVars.overLength.getOrNull(demandIndex) ?: continue
                val overLengthPenalty = lengthConfig.overLengthPenalty[demand.product.id] ?: continue
                monomials.add(LinearMonomial(overLengthPenalty.toFlt64(), overVar))
            }
        }

        val objective = LinearPolynomial(
            monomials = monomials,
            constant = Flt64.zero
        )
        ensureTry(
            model.minimize(
                polynomial = objective,
                name = "batch_yield_waste_length_minimization"
            ),
            "build objective"
        )
    }

    /**
     * 为缺省的 assignedLengthLowerBound/UpperBound 提供默认推导 / Provide default derivation for missing assignedLengthLowerBound/UpperBound
     *
     * 推导规则：
     * - lowerBound: 0（由需求约束间接控制最小值）/ 0 (minimum controlled by demand constraints)
     * - upperBound: maxOverProduceLength（若产品配置了该字段）/ maxOverProduceLength if product has it
     *
     * 当调用方仅配置 dynamicProductIds 而不配置边界时，此方法确保 assignedLength 变量能被注册。
     */
    private fun <V : RealNumber<V>> resolveDefaultLengthBounds(
        input: ProduceInput<V>,
        lengthConfig: LengthAssignmentModelingConfig<V>?
    ): LengthAssignmentModelingConfig<V>? {
        if (lengthConfig == null) return null
        if (lengthConfig.dynamicProductIds.isEmpty()) return lengthConfig

        val needsDerivation = lengthConfig.dynamicProductIds.any { pid ->
            pid !in lengthConfig.assignedLengthLowerBound.keys ||
            pid !in lengthConfig.assignedLengthUpperBound.keys
        }
        if (!needsDerivation) return lengthConfig

        val derivedLowerBounds = lengthConfig.assignedLengthLowerBound.toMutableMap()
        val derivedUpperBounds = lengthConfig.assignedLengthUpperBound.toMutableMap()

        for (productId in lengthConfig.dynamicProductIds) {
            // 查找对应产品的需求 / Find demand for this product
            val demand = input.demands.find { it.product.id == productId }
            val contribution = input.cuttingPlans
                .firstOrNull { plan -> plan.demandContributions.any { it.product.id == productId } }
                ?.demandContributions?.firstOrNull { it.product.id == productId }
            val product = demand?.product ?: contribution?.product
            val zero = demand?.quantity?.value?.constants?.zero
                ?: contribution?.quantity?.value?.constants?.zero
                ?: lengthConfig.overLengthPenalty.values.firstOrNull()?.constants?.zero
                ?: lengthConfig.totalLengthPenalty?.constants?.zero
                ?: lengthConfig.batchMinPenalty?.constants?.zero

            // 推导下界：0（由需求约束间接控制）/ Derive lower bound: 0 (controlled by demand constraints)
            if (productId !in derivedLowerBounds && zero != null) {
                derivedLowerBounds[productId] = zero
            }

            // 推导上界：maxOverProduceLength / Derive upper bound: maxOverProduceLength
            if (productId !in derivedUpperBounds) {
                val maxOverLength = product?.maxOverProduceLength
                if (maxOverLength != null) {
                    derivedUpperBounds[productId] = maxOverLength.value
                }
            }
        }

        return lengthConfig.copy(
            assignedLengthLowerBound = derivedLowerBounds,
            assignedLengthUpperBound = derivedUpperBounds
        )
    }

    /**
     * 为动态长度产品创建卷长和超长变量 / Create assigned-length and over-length variables for dynamic-length products
     */
    private fun <V : RealNumber<V>> addLengthAssignmentSlackVariables(
        demands: List<ProductDemand<V>>,
        lengthConfig: LengthAssignmentModelingConfig<V>?,
        model: LinearMetaModel<Flt64>
    ): LengthSlackVars {
        if (lengthConfig == null) return LengthSlackVars()
        if (lengthConfig.dynamicProductIds.isEmpty()) return LengthSlackVars()

        val assignedLengthVars = ArrayList<URealVar?>()
        val overLengthVars = ArrayList<URealVar?>()

        for ((demandIndex, demand) in demands.withIndex()) {
            if (demand.product.id !in lengthConfig.dynamicProductIds) {
                assignedLengthVars.add(null)
                overLengthVars.add(null)
                continue
            }

            val hasAssignedLengthBound = lengthConfig.assignedLengthLowerBound.containsKey(demand.product.id) ||
                    lengthConfig.assignedLengthUpperBound.containsKey(demand.product.id)
            val hasOverLengthConfig = lengthConfig.overLengthPenalty.containsKey(demand.product.id) ||
                    lengthConfig.overLengthUpperBound.containsKey(demand.product.id)
            val needsAssignedLength = hasAssignedLengthBound ||
                    lengthConfig.totalLengthPenalty != null ||
                    (demand.product.maxOverProduceLength != null && hasOverLengthConfig)
            val needsOverLength = hasOverLengthConfig ||
                    (needsAssignedLength && demand.product.maxOverProduceLength != null)

            if (needsAssignedLength) {
                val assignedVar = URealVar("assigned_length_$demandIndex")
                when (val result = model.add(assignedVar)) {
                    is Ok -> {}
                    is Failed -> throw IllegalStateException("register assigned-length variable failed: ${result.error}")
                    is Fatal -> throw IllegalStateException("register assigned-length variable fatal: ${result.errors}")
                }
                assignedLengthVars.add(assignedVar)
            } else {
                assignedLengthVars.add(null)
            }

            if (needsOverLength) {
                val overVar = URealVar("over_length_$demandIndex")
                when (val result = model.add(overVar)) {
                    is Ok -> {}
                    is Failed -> throw IllegalStateException("register over-length variable failed: ${result.error}")
                    is Fatal -> throw IllegalStateException("register over-length variable fatal: ${result.errors}")
                }
                overLengthVars.add(overVar)
            } else {
                overLengthVars.add(null)
            }
        }

        return LengthSlackVars(
            assignedLength = assignedLengthVars,
            overLength = overLengthVars
        )
    }

    /**
     * 添加长度分配约束 / Add length assignment constraints
     */
    private fun <V : RealNumber<V>> addLengthAssignmentConstraints(
        demands: List<ProductDemand<V>>,
        lengthConfig: LengthAssignmentModelingConfig<V>?,
        lengthSlackVars: LengthSlackVars,
        model: LinearMetaModel<Flt64>
    ) {
        if (lengthConfig == null) return

        for ((demandIndex, demand) in demands.withIndex()) {
            if (demand.product.id !in lengthConfig.dynamicProductIds) continue
            val assignedVar = lengthSlackVars.assignedLength.getOrNull(demandIndex)
            val overVar = lengthSlackVars.overLength.getOrNull(demandIndex)

            val assignedLowerBound = lengthConfig.assignedLengthLowerBound[demand.product.id]
            if (assignedLowerBound != null && assignedVar != null) {
                ensureTry(
                    model.addConstraint(
                        relation = LinearInequality(
                            lhs = LinearPolynomial(
                                monomials = listOf(LinearMonomial(Flt64.one, assignedVar)),
                                constant = Flt64.zero
                            ),
                            rhs = constantPolynomial(assignedLowerBound.toFlt64()),
                            comparison = Comparison.GE
                        ),
                        name = "assigned_length_lower_bound_$demandIndex"
                    ),
                    "build assigned-length lower bound constraint"
                )
            }

            val assignedUpperBound = lengthConfig.assignedLengthUpperBound[demand.product.id]
            if (assignedUpperBound != null && assignedVar != null) {
                ensureTry(
                    model.addConstraint(
                        relation = LinearInequality(
                            lhs = LinearPolynomial(
                                monomials = listOf(LinearMonomial(Flt64.one, assignedVar)),
                                constant = Flt64.zero
                            ),
                            rhs = constantPolynomial(assignedUpperBound.toFlt64()),
                            comparison = Comparison.LE
                        ),
                        name = "assigned_length_upper_bound_$demandIndex"
                    ),
                    "build assigned-length upper bound constraint"
                )
            }

            val overUpperBound = lengthConfig.overLengthUpperBound[demand.product.id]
            if (overUpperBound != null && overVar != null) {
                ensureTry(
                    model.addConstraint(
                        relation = LinearInequality(
                            lhs = LinearPolynomial(
                                monomials = listOf(LinearMonomial(Flt64.one, overVar)),
                                constant = Flt64.zero
                            ),
                            rhs = constantPolynomial(overUpperBound.toFlt64()),
                            comparison = Comparison.LE
                        ),
                        name = "over_length_bound_$demandIndex"
                    ),
                    "build over-length upper bound constraint"
                )
            }

            val maxOverProduceLength = demand.product.maxOverProduceLength
            if (maxOverProduceLength != null && assignedVar != null && overVar != null) {
                ensureTry(
                    model.addConstraint(
                        relation = LinearInequality(
                            lhs = LinearPolynomial(
                                monomials = listOf(
                                    LinearMonomial(Flt64.one, assignedVar),
                                    LinearMonomial(Flt64(-1.0), overVar)
                                ),
                                constant = Flt64.zero
                            ),
                            rhs = constantPolynomial(maxOverProduceLength.value.toFlt64()),
                            comparison = Comparison.LE
                        ),
                        name = "assigned_over_length_link_$demandIndex"
                    ),
                    "build assigned-over-length link constraint"
                )
            }
        }
    }

    /**
     * 从 solver solution 提取长度分配建模结果 / Extract length assignment modeling result from solver solution
     */
    @Suppress("UNCHECKED_CAST")
    private fun <V : RealNumber<V>> extractLengthResult(
        demands: List<ProductDemand<V>>,
        lengthConfig: LengthAssignmentModelingConfig<V>?,
        lengthSlackVars: LengthSlackVars,
        model: LinearMetaModel<Flt64>
    ): LengthAssignmentModelingResult<V>? {
        if (lengthConfig == null) return null
        if (!lengthSlackVars.hasAny) return null

        val assignedLengths = ArrayList<ModeledAssignedLength<V>>()
        val overLengths = ArrayList<ModeledOverLength<V>>()

        for ((demandIndex, demand) in demands.withIndex()) {
            if (demand.product.id !in lengthConfig.dynamicProductIds) continue
            val assignedVar = lengthSlackVars.assignedLength.getOrNull(demandIndex)
            if (assignedVar != null) {
                val assignedDouble = model.tokens.find(assignedVar)?.doubleResult
                if (assignedDouble != null && assignedDouble >= 0.0) {
                    val assignedAmount = solverValueLike(demand.quantity.value, Flt64(assignedDouble))
                    assignedLengths.add(
                        ModeledAssignedLength(
                            productId = demand.product.id,
                            assignedLength = assignedAmount
                        )
                    )
                }
            }

            val overVar = lengthSlackVars.overLength.getOrNull(demandIndex) ?: continue
            val overDouble = model.tokens.find(overVar)?.doubleResult ?: continue
            if (overDouble > 0.0) {
                val overAmount = solverValueLike(demand.quantity.value, Flt64(overDouble))
                overLengths.add(
                    ModeledOverLength(
                        productId = demand.product.id,
                        overLength = overAmount
                    )
                )
            }
        }

        return LengthAssignmentModelingResult(
            assignedLengths = assignedLengths,
            overLengths = overLengths
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun <V : RealNumber<V>> extractShadowPrices(
        lpResult: ColumnGenerationSolver.LPResult,
        shadowPriceKeys: Map<String, ShadowPriceKey>
    ): ShadowPriceMap<V> {
        val shadowPrices = HashMap<ShadowPriceKey, V>()
        val dualSolution = lpResult.dualSolution

        for ((constraint, dualValue) in dualSolution) {
            val key = shadowPriceKeys[constraint.name] ?: continue

            @Suppress("UNCHECKED_CAST")
            val vDual = dualValue as? V ?: continue
            val existingValue = shadowPrices[key]
            shadowPrices[key] = if (existingValue != null) existingValue + vDual else vDual
        }
        return ShadowPriceMap(shadowPrices)
    }

    /**
     * 从 solver solution 提取浪费最小化结果 / Extract waste minimization result from solver solution
     */
    @Suppress("UNCHECKED_CAST")
    private fun <V : RealNumber<V>> extractWasteResult(
        cuttingPlans: List<CuttingPlan<V>>,
        wasteConfig: WasteMinimizationConfig<V>?,
        demands: List<ProductDemand<V>>,
        yieldConfig: YieldModelingConfig<V>?,
        yieldSlackVars: YieldSlackVars,
        assignment: Csp1dAssignment,
        model: LinearMetaModel<Flt64>
    ): WasteMinimizationResult<V>? {
        if (wasteConfig == null) return null

        // 计算总余宽 / Calculate total trim width
        var totalTrimWidth: V? = null
        if (wasteConfig.trimWidthPenalty != null) {
            var sum: V? = null
            for (index in 0 until assignment.planCount) {
                val plan = cuttingPlans[index]
                val batchCount = solutionAmount(model, assignment, index)
                if (batchCount > UInt64.zero) {
                    val restWidthValue = plan.restWidth?.value ?: continue
                    val contribution = restWidthValue * solverValueLike(restWidthValue, batchCount.toFlt64())
                    sum = if (sum != null) sum + contribution else contribution
                }
            }
            totalTrimWidth = sum
        }

        // 计算总余料面积代理 / Calculate total rest material area proxy
        var totalRestMaterial: V? = null
        if (wasteConfig.restMaterialPenalty != null) {
            var sum: V? = null
            for (index in 0 until assignment.planCount) {
                val plan = cuttingPlans[index]
                val batchCount = solutionAmount(model, assignment, index)
                if (batchCount > UInt64.zero) {
                    val restMaterialValue = restMaterialValue(
                        plan = plan,
                        measure = wasteConfig.restMaterialMeasure
                    ) ?: continue
                    val contribution = restMaterialValue * solverValueLike(restMaterialValue, batchCount.toFlt64())
                    sum = if (sum != null) sum + contribution else contribution
                }
            }
            totalRestMaterial = sum
        }

        // 计算物料成本 / Calculate material costs
        val materialCosts = ArrayList<ModeledMaterialCost<V>>()
        if (wasteConfig.materialCostPenalty.isNotEmpty()) {
            val costByMaterial = HashMap<String, V>()
            for (index in 0 until assignment.planCount) {
                val plan = cuttingPlans[index]
                val batchCount = solutionAmount(model, assignment, index)
                if (batchCount > UInt64.zero) {
                    val costPenalty = wasteConfig.materialCostPenalty[plan.material.id] ?: continue
                    val cost = costPenalty * solverValueLike(costPenalty, batchCount.toFlt64())
                    val existing = costByMaterial[plan.material.id]
                    costByMaterial[plan.material.id] = if (existing != null) existing + cost else cost
                }
            }
            for ((materialId, cost) in costByMaterial) {
                materialCosts.add(ModeledMaterialCost(materialId, cost))
            }
        }

        // 计算超产面积代理 / Calculate over-production area proxy
        var overProductionArea: V? = null
        if (wasteConfig.overProductionAreaPenalty != null && yieldConfig != null && yieldSlackVars.overProduction.any { it != null }) {
            var areaSum: V? = null
            for ((demandIndex, demand) in demands.withIndex()) {
                val overVar = yieldSlackVars.overProduction.getOrNull(demandIndex) ?: continue
                val overDouble = model.tokens.find(overVar)?.doubleResult ?: continue
                if (overDouble > 0.0) {
                    val productWidthValue = overProductionAreaWidthValue(
                        demand = demand,
                        measure = wasteConfig.overProductionAreaMeasure
                    ) ?: continue
                    val area = productWidthValue * solverValueLike(productWidthValue, Flt64(overDouble))
                    areaSum = if (areaSum != null) areaSum + area else area
                }
            }
            overProductionArea = areaSum
        }

        return WasteMinimizationResult(
            totalTrimWidth = totalTrimWidth,
            totalRestMaterial = totalRestMaterial,
            materialCosts = materialCosts,
            overProductionArea = overProductionArea,
            overProductionAreaMeasure = wasteConfig.overProductionAreaMeasure,
            restMaterialMeasure = wasteConfig.restMaterialMeasure
        )
    }

    /**
     * 从 solver solution 提取 yield 建模结果 / Extract yield modeling result from solver solution
     */
    @Suppress("UNCHECKED_CAST")
    private fun <V : RealNumber<V>> extractYieldResult(
        demands: List<ProductDemand<V>>,
        yieldConfig: YieldModelingConfig<V>?,
        yieldSlackVars: YieldSlackVars,
        model: LinearMetaModel<Flt64>
    ): YieldModelingResult<V>? {
        if (yieldConfig == null) return null
        if (!yieldSlackVars.hasAny) return null

        val underProductions = ArrayList<ModeledUnderProduction<V>>()
        val overProductions = ArrayList<ModeledOverProduction<V>>()

        for ((demandIndex, demand) in demands.withIndex()) {
            val unitSymbol = shadowPriceUnitSymbol(demand.quantity.unit)
            val demandKey = ProductDemandShadowPriceKey(
                productId = demand.product.id,
                unitSymbol = unitSymbol
            )

            val hasUnder = yieldConfig.underProductionPenalty.containsKey(demandKey)
            if (hasUnder) {
                val underVar = yieldSlackVars.underProduction.getOrNull(demandIndex) ?: continue
                val underDouble = model.tokens.find(underVar)?.doubleResult ?: continue
                if (underDouble > 0.0) {
                    val underAmount = solverValueLike(demand.quantity.value, Flt64(underDouble))
                    underProductions.add(
                        ModeledUnderProduction(
                            productId = demand.product.id,
                            unitSymbol = unitSymbol,
                            amount = underAmount
                        )
                    )
                }
            }

            val hasOver = yieldConfig.overProductionPenalty.containsKey(demandKey) ||
                    yieldConfig.overProductionUpperBound.containsKey(demandKey)
            if (hasOver) {
                val overVar = yieldSlackVars.overProduction.getOrNull(demandIndex) ?: continue
                val overDouble = model.tokens.find(overVar)?.doubleResult ?: continue
                if (overDouble > 0.0) {
                    val overAmount = solverValueLike(demand.quantity.value, Flt64(overDouble))
                    overProductions.add(
                        ModeledOverProduction(
                            productId = demand.product.id,
                            unitSymbol = unitSymbol,
                            amount = overAmount
                        )
                    )
                }
            }
        }

        return YieldModelingResult(
            underProductions = underProductions,
            overProductions = overProductions
        )
    }

    private fun <V : RealNumber<V>> extractProduce(
        input: ProduceInput<V>,
        assignment: Csp1dAssignment,
        model: LinearMetaModel<Flt64>
    ): Produce<V> {
        val cuttingPlans = ArrayList<CuttingPlanUsage<V>>()
        for ((index, plan) in input.cuttingPlans.withIndex()) {
            val amount = solutionAmount(
                model = model,
                assignment = assignment,
                index = index
            )
            if (amount > UInt64.zero) {
                cuttingPlans.add(
                    CuttingPlanUsage(
                        plan = plan,
                        amount = amount
                    )
                )
            }
        }

        return Produce(
            cuttingPlans = cuttingPlans,
            materialUsages = materialUsages(
                materials = input.materials,
                cuttingPlans = cuttingPlans
            ),
            machineUsages = machineUsages(
                machines = input.machines,
                cuttingPlans = cuttingPlans
            ),
            unmetDemands = unmetDemands(
                demands = input.demands,
                cuttingPlans = cuttingPlans
            )
        )
    }

    private fun <V : RealNumber<V>> materialUsages(
        materials: List<Material<V>>,
        cuttingPlans: List<CuttingPlanUsage<V>>
    ): List<MaterialUsage<V>> {
        return materials.mapNotNull { material ->
            val amount = cuttingPlans.fold(UInt64.zero) { acc, usage ->
                if (usage.plan.material.id == material.id) {
                    acc + usage.amount
                } else {
                    acc
                }
            }
            if (amount > UInt64.zero) {
                MaterialUsage(
                    material = material,
                    amount = amount
                )
            } else {
                null
            }
        }
    }

    private fun <V : RealNumber<V>> machineUsages(
        machines: List<Machine<V>>,
        cuttingPlans: List<CuttingPlanUsage<V>>
    ): List<MachineCapacityUsage<V>> {
        return machines.mapNotNull { machine ->
            val machineCapacity = machine.capacity
            val used = cuttingPlans
                .filter { usage -> usage.plan.machineId == machine.id }
                .fold(null as Quantity<V>?) { acc, usage ->
                    val consumption = usage.plan.capacityConsumption ?: return@fold acc
                    if (machineCapacity != null && consumption.unit != machineCapacity.unit) {
                        return@fold acc
                    }
                    if (acc != null && acc.unit != consumption.unit) {
                        return@fold acc
                    }
                    val contribution = Quantity(
                        consumption.value * solverValueLike(
                            sample = consumption.value,
                            value = usage.amount.toFlt64()
                        ),
                        consumption.unit
                    )
                    if (acc == null) {
                        contribution
                    } else {
                        Quantity(acc.value + contribution.value, acc.unit)
                    }
                }
            if (used != null) {
                MachineCapacityUsage(
                    machine = machine,
                    used = used
                )
            } else {
                null
            }
        }
    }

    private fun <V : RealNumber<V>> unmetDemands(
        demands: List<ProductDemand<V>>,
        cuttingPlans: List<CuttingPlanUsage<V>>
    ): List<ProductDemand<V>> {
        return demands.filter { demand ->
            val supplied = cuttingPlans.fold(Flt64.zero) { acc, usage ->
                val amount = usage.amount.toFlt64()
                val contribution = usage.plan.demandContributions.find {
                    it.product.id == demand.product.id && it.quantity.unit == demand.quantity.unit
                } ?: return@fold acc
                acc + contribution.quantity.value.toFlt64() * amount
            }
            supplied < demand.quantity.value.toFlt64()
        }
    }

    private fun solutionAmount(
        model: LinearMetaModel<Flt64>,
        assignment: Csp1dAssignment,
        index: Int
    ): UInt64 {
        val value = model.tokens.find(assignment[index])?.doubleResult ?: return UInt64.zero
        if (value <= 0.0) {
            return UInt64.zero
        }
        return UInt64(value.roundToLong().toULong())
    }

    private fun constantPolynomial(value: Flt64): LinearPolynomial<Flt64> {
        return LinearPolynomial(
            monomials = emptyList(),
            constant = value
        )
    }

    private fun <V : RealNumber<V>> restMaterialValue(
        plan: CuttingPlan<V>,
        measure: RestMaterialMeasure
    ): V? {
        return when (measure) {
            RestMaterialMeasure.RestWidthByMaterialLengthProxy -> {
                val restWidthValue = plan.restWidth?.value ?: return null
                if (restWidthValue <= restWidthValue.constants.zero) {
                    return null
                }
                val materialLengthValue = plan.material.length?.value ?: return null
                if (materialLengthValue <= materialLengthValue.constants.zero) {
                    return null
                }
                restWidthValue * materialLengthValue
            }
        }
    }

    private fun <V : RealNumber<V>> overProductionAreaWidthValue(
        demand: ProductDemand<V>,
        measure: OverProductionAreaMeasure
    ): V? {
        return when (measure) {
            OverProductionAreaMeasure.ProductMaxWidthProxy -> demand.product.maxWidth()?.value
        }
    }

    private fun shadowPriceUnitSymbol(unit: PhysicalUnit): String {
        return unit.symbol ?: unit.name ?: unit.toString()
    }

    @Suppress("UNCHECKED_CAST")
    private fun <V : RealNumber<V>> solverValueLike(sample: V, value: Flt64): V {
        return when (sample) {
            is Flt64 -> value as V
            is FltX -> value.toFltX() as V
            else -> throw IllegalArgumentException("Unsupported RealNumber type: ${sample::class}")
        }
    }

    private fun ensureTry(result: Try, stage: String) {
        when (result) {
            is Ok -> {}
            is Failed -> throw IllegalStateException("$stage failed: ${result.error}")
            is Fatal -> throw IllegalStateException("$stage fatal: ${result.errors}")
        }
    }

    private fun <T> ensureRet(result: Ret<T>, stage: String): T {
        return when (result) {
            is Ok -> result.value
            is Failed -> throw IllegalStateException("$stage failed: ${result.error}")
            is Fatal -> throw IllegalStateException("$stage fatal: ${result.errors}")
        }
    }
}
