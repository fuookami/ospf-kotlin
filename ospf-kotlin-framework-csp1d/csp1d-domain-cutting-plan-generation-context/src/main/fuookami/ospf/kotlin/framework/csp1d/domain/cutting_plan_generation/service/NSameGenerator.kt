package fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.service

import kotlin.time.Duration
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.Csp1dInitialCuttingPlanGenerator
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.CuttingPlanGenerationInput
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.CuttingPlanGenerationReport
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.CuttingPlanConstraint
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.CuttingPlanConstraintContext
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.GenerationConstraints
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.MaxKnifeCountConstraint
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.MaxOverProduceLengthConstraint
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlan
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlanSlice
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Machine
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Material
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Product
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.ProductDemand
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.QuantityArithmetic
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.contribution
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.partialOrd
import fuookami.ospf.kotlin.utils.functional.Order

/**
 * N-Same 方案生成器，为每个产品-宽度组合生成单产品方案 / N-Same generator producing single-product plans per product-width combination
 *
 * @param V 数值类型 / Numeric value type
 * @property constraints 约束列表 / Constraint list
 * @property arithmetic 物理量算术策略 / Quantity arithmetic strategy
 * @property allAmount 是否生成所有可行数量（1 到 max），默认只生成 max / Whether to generate all amounts (1 to max)
 * @property timeout 超时限制 / Timeout limit
 * @property maxPlans 最大方案数（提前终止）/ Max plans (early termination)
 * @property parallelism 按物料并行生成的协程并发度，1 表示关闭 / Coroutine parallelism by material, 1 means disabled
 * @property enableDominancePruning 是否启用同贡献候选 dominance 剪枝 / Whether to enable dominance pruning for same-contribution candidates
 */
class NSameGenerator<V : RealNumber<V>>(
    private val constraints: List<CuttingPlanConstraint<V>> = emptyList(),
    private val arithmetic: QuantityArithmetic<V>,
    private val allAmount: Boolean = false,
    private val timeout: Duration? = null,
    private val maxPlans: Int = 1000,
    private val parallelism: Int = 1,
    private val enableDominancePruning: Boolean = false
) : Csp1dInitialCuttingPlanGenerator<V> {

    constructor(
        constraints: GenerationConstraints<V>,
        arithmetic: QuantityArithmetic<V>,
        allAmount: Boolean = false,
        timeout: Duration? = null,
        maxPlans: Int = 1000
    ) : this(
        constraints = constraints.toConstraints(),
        arithmetic = arithmetic,
        allAmount = allAmount,
        timeout = timeout,
        maxPlans = maxPlans,
        parallelism = constraints.parallelism,
        enableDominancePruning = constraints.enableDominancePruning
    )

    /** 从约束中提取 maxKnifeCount，用于枚举范围限制 */
    private val maxKnifeCount: UInt64? = constraints.filterIsInstance<MaxKnifeCountConstraint<V>>().firstOrNull()?.value

    /** 从约束中提取 maxOverProduceLength，用于枚举范围限制 */
    private val maxOverProduceLength: Quantity<V>? = constraints.filterIsInstance<MaxOverProduceLengthConstraint<V>>().firstOrNull()?.value

    override fun generate(input: CuttingPlanGenerationInput<V>): List<CuttingPlan<V>> {
        return generateWithReport(input).plans
    }

    override fun generateWithReport(input: CuttingPlanGenerationInput<V>): CuttingPlanGenerationReport<V> {
        val startTime = System.nanoTime()
        val planIndex = java.util.concurrent.atomic.AtomicInteger(0)
        val deadline = timeout?.let { System.nanoTime() + it.inWholeNanoseconds }
        val collector = GenerationCollector<V>(
            maxPlans = maxPlans,
            deadline = deadline,
            enableDominancePruning = enableDominancePruning
        )
        val quantityCache = GenerationQuantityCache(arithmetic)

        if (parallelism > 1 && input.materials.size > 1) {
            val reports = runGenerationTasks(
                parallelism = parallelism,
                tasks = input.materials.map { material ->
                    {
                        val localCollector = GenerationCollector<V>(
                            maxPlans = maxPlans,
                            deadline = deadline,
                            enableDominancePruning = enableDominancePruning
                        )
                        generateMaterial(
                            material = material,
                            demands = input.demands,
                            machines = input.machines,
                            planIndex = planIndex,
                            collector = localCollector,
                            quantityCache = GenerationQuantityCache(arithmetic)
                        )
                        localCollector.report()
                    }
                }
            )
            return mergeGenerationReports(
                reports = reports,
                maxPlans = maxPlans,
                startedAt = startTime,
                deadline = deadline
            )
        }

        for (material in input.materials) {
            generateMaterial(
                material = material,
                demands = input.demands,
                machines = input.machines,
                planIndex = planIndex,
                collector = collector,
                quantityCache = quantityCache
            )
            if (collector.shouldStop()) break
        }
        return collector.report()
    }

    private fun generateMaterial(
        material: Material<V>,
        demands: List<ProductDemand<V>>,
        machines: List<Machine<V>>,
        planIndex: java.util.concurrent.atomic.AtomicInteger,
        collector: GenerationCollector<V>,
        quantityCache: GenerationQuantityCache<V>
    ) {
        for (demand in demands) {
            if (collector.shouldStop()) break

            for (productWidth in demand.product.width) {
                if (collector.shouldStop()) break
                if (!material.widthRange.canCut(productWidth)) continue

                val maxAmount = computeMaxAmount(
                    productWidth = productWidth,
                    material = material,
                    product = demand.product,
                    quantityCache = quantityCache
                )
                if (maxAmount == UInt64.zero) continue

                val amounts = if (allAmount) {
                    val generatedAmounts = ArrayList<UInt64>()
                    var amount = UInt64.one
                    while (amount <= maxAmount) {
                        generatedAmounts.add(amount)
                        amount += UInt64.one
                    }
                    generatedAmounts
                } else {
                    listOf(maxAmount)
                }

                for (amount in amounts) {
                    if (collector.shouldStop()) break

                    collector.visitNode()
                    val slices = listOf(
                        CuttingPlanSlice(
                            production = demand.product,
                            width = productWidth,
                            amount = amount
                        )
                    )
                    val usedWidth = quantityCache.repeatWidth(productWidth, amount)

                    // 约束检查 / Constraint check
                    if (!satisfiesConstraints(slices, usedWidth, material.widthRange.upperBound, material)) {
                        continue
                    }

                    val contribution = demand.contribution(
                        width = productWidth,
                        amount = amount,
                        arithmetic = arithmetic,
                        length = generationContributionLength(
                            product = demand.product,
                            material = material
                        )
                    )

                    val plan = CuttingPlan(
                        id = "nsame-${material.id}-${demand.product.id}-${planIndex.getAndIncrement()}",
                        material = material,
                        slices = slices,
                        demandContributions = listOf(contribution),
                        arithmetic = arithmetic
                    )
                    collector.record(
                        plan = plan,
                        feasible = material.enabled(plan, machines)
                    )
                }
            }
        }
    }

    private fun computeMaxAmount(
        productWidth: Quantity<V>,
        material: Material<V>,
        product: Product<V>,
        quantityCache: GenerationQuantityCache<V>
    ): UInt64 {
        var maxAmount = UInt64.maximum

        // 宽度约束 / Width constraint
        val upperBound = material.widthRange.upperBound
        if ((productWidth.value partialOrd upperBound.value) is Order.Greater) return UInt64.zero
        val maxWidthAmount = quantityCache.maxRepeatCount(productWidth, upperBound)
        if (maxWidthAmount < maxAmount) maxAmount = maxWidthAmount

        // 刀数约束 / Knife count constraint
        maxKnifeCount?.let { if (it < maxAmount) maxAmount = it }

        // 超产长度约束 / Over-produce length constraint
        maxOverProduceLength?.let { maxLen ->
            product.length?.let { productLength ->
                if ((productLength.value partialOrd maxLen.value) is Order.Greater) {
                    return UInt64.zero
                }
            }
        }

        return maxAmount
    }

    private fun satisfiesConstraints(
        slices: List<CuttingPlanSlice<V>>,
        totalWidth: Quantity<V>,
        upperBound: Quantity<V>,
        material: Material<V>
    ): Boolean {
        if (constraints.isEmpty()) return true
        val context = CuttingPlanConstraintContext(slices, totalWidth, upperBound, material)
        return constraints.all { it.isSatisfied(context) }
    }

}
