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

/** 真实DFS的等价/不同物料及自定义策略回退。 / Production DFS with equivalent/distinct materials and custom-strategy fallback.
 * @property scenario 物料和策略场景 / Material and policy scenario
 * @property workers 现有物料并行度 / Existing material parallelism
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
open class GenerationCacheBuildBenchmark {
    @Param("equivalent", "distinct", "custom")
    lateinit var scenario: String

    @Param("1", "4")
    var workers = 1

    private lateinit var input: CuttingPlanGenerationInput<Flt64>
    private lateinit var generator: DFSGenerator<Flt64>

    /** 固定输入，计时内完整生成及报告汇总。 / Fix inputs; time full generation and report aggregation. */
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
        generator = DFSGenerator(
            constraints = constraints,
            arithmetic = DefaultQuantityArithmetic.resolveFor(Flt64.zero).value!!,
            maxPlans = Int64(100_000),
            parallelism = Int64(workers.toLong())
        )
    }

    /** @return 包含现有缓存命中、miss和访问节点计数的完整报告 / Full report containing existing cache hits, misses and visited-node counts */
    @Benchmark
    fun generate(): CuttingPlanGenerationReport<Flt64> = generator.generateWithReport(input)
}
