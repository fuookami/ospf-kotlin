package fuookami.ospf.kotlin.framework.csp1d.application.service

import kotlin.math.roundToLong
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.solver.output.FeasibleSolverOutput
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.framework.solver.ColumnGenerationSolver
import fuookami.ospf.kotlin.framework.csp1d.application.model.Csp1dAssignment
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlan
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Machine
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Material
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.ProductDemand
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.ShadowPriceKey
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.ShadowPriceMap
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.ProductDemandShadowPriceKey
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.MaterialUsageShadowPriceKey
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.MachineCapacityShadowPriceKey
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.ProduceInput
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.CuttingPlanUsage
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.MachineCapacityUsage
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.MaterialUsage
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.Produce

class Csp1dMilpSolver(
    private val solver: ColumnGenerationSolver
) {
    data class MilpResult<V : RealNumber<V>>(
        val produce: Produce<V>,
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
        input: ProduceInput<V>
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

        addDemandConstraints(
            input = input,
            assignment = assignment,
            model = model
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
        setObjective(
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
        return MilpResult(
            produce = produce,
            model = model,
            assignment = assignment,
            output = output
        )
    }

    suspend fun <V : RealNumber<V>> solveLP(
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
        ensureTry(assignment.register(model), "register assignment")

        addDemandConstraints(
            input = input,
            assignment = assignment,
            model = model
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

        val shadowPrices = extractShadowPrices<V>(lpResult)
        return LpResult(
            shadowPrices = shadowPrices,
            model = model,
            assignment = assignment,
            lpOutput = lpResult
        )
    }

    private fun <V : RealNumber<V>> addDemandConstraints(
        input: ProduceInput<V>,
        assignment: Csp1dAssignment,
        model: LinearMetaModel<Flt64>
    ) {
        for (demand in input.demands) {
            val lhs = LinearPolynomial(
                monomials = input.cuttingPlans.mapIndexedNotNull { index, plan ->
                    val contribution = plan.demandContributions.find {
                        it.product.id == demand.product.id && it.quantity.unit == demand.quantity.unit
                    } ?: return@mapIndexedNotNull null
                    LinearMonomial(
                        coefficient = contribution.quantity.value.toFlt64(),
                        symbol = assignment[index]
                    )
                },
                constant = Flt64.zero
            )
            val rhs = constantPolynomial(demand.quantity.value.toFlt64())
            ensureTry(
                model.addConstraint(
                    relation = LinearInequality(
                        lhs = lhs,
                        rhs = rhs,
                        comparison = Comparison.GE
                    ),
                    name = "demand_${demand.product.id}_${demand.quantity.unit.symbol}"
                ),
                "build demand constraint"
            )
        }
    }

    private fun <V : RealNumber<V>> addMaterialConstraints(
        input: ProduceInput<V>,
        assignment: Csp1dAssignment,
        model: LinearMetaModel<Flt64>
    ) {
        for (material in input.materials) {
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

            ensureTry(
                model.addConstraint(
                    relation = LinearInequality(
                        lhs = lhs,
                        rhs = constantPolynomial(material.availableBatches.toFlt64()),
                        comparison = Comparison.LE
                    ),
                    name = "material_${material.id}"
                ),
                "build material constraint"
            )
        }
    }

    private fun <V : RealNumber<V>> addMachineConstraints(
        input: ProduceInput<V>,
        assignment: Csp1dAssignment,
        model: LinearMetaModel<Flt64>
    ) {
        for (machine in input.machines) {
            val maxBatchCount = machine.maxBatchCount ?: continue
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
                continue
            }

            ensureTry(
                model.addConstraint(
                    relation = LinearInequality(
                        lhs = lhs,
                        rhs = constantPolynomial(maxBatchCount.toFlt64()),
                        comparison = Comparison.LE
                    ),
                    name = "machine_${machine.id}_batch"
                ),
                "build machine batch constraint"
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

    @Suppress("UNCHECKED_CAST")
    private fun <V : RealNumber<V>> extractShadowPrices(
        lpResult: ColumnGenerationSolver.LPResult
    ): ShadowPriceMap<V> {
        val shadowPrices = HashMap<ShadowPriceKey, V>()
        val dualSolution = lpResult.dualSolution

        for ((constraint, dualValue) in dualSolution) {
            val key = shadowPriceKeyFromName(constraint.name)
            if (key == null) continue

            @Suppress("UNCHECKED_CAST")
            val vDual = dualValue as? V ?: continue
            val existingValue = shadowPrices[key]
            shadowPrices[key] = if (existingValue != null) existingValue + vDual else vDual
        }
        return ShadowPriceMap(shadowPrices)
    }

    private fun shadowPriceKeyFromName(name: String): ShadowPriceKey? {
        if (name.startsWith("demand_")) {
            val productId = name.removePrefix("demand_").split("_").first()
            return ProductDemandShadowPriceKey(productId)
        }
        if (name.startsWith("material_")) {
            val materialId = name.removePrefix("material_")
            return MaterialUsageShadowPriceKey(materialId)
        }
        if (name.startsWith("machine_")) {
            val machineId = name.removePrefix("machine_").split("_").first()
            return MachineCapacityShadowPriceKey(machineId)
        }
        return null
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
            val used = cuttingPlans.any { usage -> usage.plan.machineId == machine.id }
            if (used) {
                MachineCapacityUsage(
                    machine = machine,
                    used = machine.capacity
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
