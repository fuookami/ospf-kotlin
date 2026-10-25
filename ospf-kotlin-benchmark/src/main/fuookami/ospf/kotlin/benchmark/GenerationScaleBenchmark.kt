package fuookami.ospf.kotlin.benchmark

import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.*
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.*
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.*
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.service.*

/** 三种生成器在缓存开关与 worker 梯度下的完整报告成本。 / Full-report cost of all three generators across cache modes and worker levels.
 *
 * 现有 `GenerationCacheBuildBenchmark` 只执行 DFS 且 workers 仅 1/4；这里补齐 NSum/FullSum 与 1,2,4,8 梯度，
 * 并区分完整输出负载与预算截断负载。计时内是完整生成与报告汇总，不复制生产算法。
 * The existing DFS-only fixture covers workers 1/4; this completes NSum/FullSum and the 1,2,4,8 gradient, and separates
 * full-output from budget-truncated loads. Timing covers complete generation and report aggregation only. Internal
 * parallelism is the `workers` parameter, so JMH caller threads stay at 1 (`@Threads(1)`) to avoid multiplying threads.
 * @property scenario 物料与策略场景 / Material and policy scenario
 * @property generator 生成器 / Generator
 * @property workers 内部并行度 / Internal parallelism
 * @property budget 方案上限 / Plan budget
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Threads(1)
open class GenerationScaleBenchmark {
    @Param("equivalent", "distinct", "custom")
    lateinit var scenario: String

    @Param("dfs", "nsum", "fullsum")
    lateinit var generator: String

    @Param("1", "2", "4", "8")
    var workers = 0

    @Param("100000", "200")
    var budget = 0

    private lateinit var input: CuttingPlanGenerationInput<Flt64>
    private lateinit var instance: Csp1dInitialCuttingPlanGenerator<Flt64>

    /** 固定输入与生成器在计时外构造。 / Fixed input and generator are built outside timing. */
    @Setup
    fun setup() {
        val products = List(8) { index ->
            Product(
                id = productIdOf("p$index"),
                name = "p$index",
                width = listOf(Quantity(Flt64(0.17 + index * 0.03), Meter))
            )
        }
        input = CuttingPlanGenerationInput(
            products = products,
            materials = List(12) { index ->
                Material(
                    id = materialIdOf("m$index"),
                    name = "m$index",
                    widthRange = WidthRange(
                        width = QuantityRange(
                            lowerBound = Quantity(Flt64.zero, Meter),
                            upperBound = Quantity(Flt64(if (scenario == "distinct") 1.2 + index * 0.01 else 1.2), Meter)
                        ),
                        step = Quantity(Flt64(0.01), Meter)
                    )
                )
            },
            machines = emptyList(),
            demands = products.map { ProductDemand.roll(it, Quantity(Flt64(8), RollCountUnit)) }
        )
        val constraints = mutableListOf<CuttingPlanConstraint<Flt64>>(MaxKnifeCountConstraint(UInt64(5)))
        if (scenario == "custom") {
            constraints.add(object : CuttingPlanConstraint<Flt64> {
                override fun isSatisfied(context: CuttingPlanConstraintContext<Flt64>): Boolean = true
            })
        }
        val arithmetic = DefaultQuantityArithmetic.resolveFor(Flt64.zero).value!!
        instance = when (generator) {
            "nsum" -> NSumGenerator(
                constraints = constraints,
                arithmetic = arithmetic,
                maxPlans = Int64(budget.toLong()),
                parallelism = Int64(workers.toLong())
            )
            "fullsum" -> FullSumGenerator(
                constraints = constraints,
                arithmetic = arithmetic,
                maxPlans = Int64(budget.toLong()),
                parallelism = Int64(workers.toLong())
            )
            else -> DFSGenerator(
                constraints = constraints,
                arithmetic = arithmetic,
                maxPlans = Int64(budget.toLong()),
                parallelism = Int64(workers.toLong())
            )
        }
    }

    /** @return 包含方案与统计的完整报告 / Full report containing plans and statistics */
    @Benchmark
    fun generateWithReport(): CuttingPlanGenerationReport<Flt64> = instance.generateWithReport(input)

    /** @return 方案数量与终止原因，确认结果被消费 / Plan count and stop reason, proving the result is consumed */
    @Benchmark
    fun generateSummary(): String {
        val report = instance.generateWithReport(input)
        return "${report.plans.size}:${report.statistics.stopReason.name}"
    }
}
