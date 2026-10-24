package fuookami.ospf.kotlin.benchmark

import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.framework.bpp.infrastructure.Flt64PackingValueAdapter
import fuookami.ospf.kotlin.framework.bpp1d.domain.item.model.*
import fuookami.ospf.kotlin.framework.bpp1d.domain.pattern_assignment.*

/** 真实 BPP1D 多批加列入口，不混入求解器耗时。 / Production BPP1D multi-batch registration without solver time. */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
open class PatternColumnBenchmark {
    @Param("small", "medium", "large")
    lateinit var dataset: String

    @Param("1", "32")
    var batchSize: Int = 0

    private lateinit var problem: PackingProblem<Flt64>
    private lateinit var batches: List<List<PackingPattern<Flt64>>>

    /** 准备固定单位、仅列数和批大小变化的输入。 / Prepare fixed-unit inputs varying only column count and batch size. */
    @Setup
    fun setup() {
        val columns = when (dataset) {
            "medium" -> 512
            "large" -> 1024
            else -> 256
        }
        problem = PackingProblem(
            items = listOf(Item(
                id = "item",
                length = Quantity(Flt64.one, Meter),
                weight = Quantity(Flt64.one, Kilogram),
                amount = UInt64(10_000)
            )),
            binTypes = listOf(BinType(
                id = "bin",
                length = Quantity(Flt64(10_000), Meter),
                maxWeight = Quantity(Flt64(10_000), Kilogram),
                cost = Flt64.one
            )),
            spacing = Quantity(Flt64.zero, Meter)
        )
        batches = (1..columns).map { PackingPattern<Flt64>("bin", mapOf("item" to UInt64(it))) }.chunked(batchSize)
    }

    /** 注册、追加、关闭完整模型，消费实际生成的项数。 / Register, append and close a full model, consuming its actual term count.
     * @return 最终基础目标项数 / Final base-objective term count
     */
    @Benchmark
    fun addColumnBatches(): Int {
        val model = LinearMetaModel<Flt64>(name = "pattern-columns", converter = IntoValue.Identity)
        try {
            val context = PatternAssignmentContext(
                problem = problem,
                adapter = Flt64PackingValueAdapter,
                model = model,
                stage = PackingModelStage.LP
            )
            check(context.register(emptyList()).ok)
            for (batch in batches) check(context.addColumns(batch).ok)
            return context.aggregation.objective.asMutable().monomials.size
        } finally {
            model.close()
        }
    }
}
