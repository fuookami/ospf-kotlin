package fuookami.ospf.kotlin.benchmark

import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.openjdk.jmh.annotations.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.framework.solver.*

/** 区分不同消费面的新版本准备与同版本复用。 / Separate cold-revision preparation from same-revision reuse by consumer.
 * @property size 变量数，行数为其一半 / Variable count, with half as many rows
 * @property consumer snapshot、pricing或master / Snapshot, pricing or master consumer
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
open class ProjectionIndexBuildBenchmark {
    @Param("256", "1024", "4096")
    var size = 0

    @Param("snapshot", "pricing", "master")
    lateinit var consumer: String

    private lateinit var source: LinearMetaModel<Flt64>
    private lateinit var decisions: List<ColumnDecision>
    private lateinit var columns: List<ProjectedColumn<Flt64>>
    private lateinit var warm: ColumnProjection<Flt64>
    private val blocks = (0 until 4).map { "b$it" }.toSet()

    /** 固定源模型，刷新及消费仍在计时内。 / Fix the source model; refresh and consumption remain timed. */
    @Setup
    fun setup(): Unit = runBlocking {
        source = LinearMetaModel(name = "projection_index_build")
        val variables = List(size) { BinVar("v$it") }
        check(source.add(variables) is Ok)
        repeat(size / 2) { row ->
            check(source.addConstraint(
                relation = LinearPolynomial(
                    monomials = (0..2).map { offset -> LinearMonomial(Flt64.one, variables[(row + offset * 4) % size]) },
                    constant = Flt64.zero
                ) leq Flt64.two,
                name = "r$row"
            ) is Ok)
        }
        check(source.minimize(LinearPolynomial(variables.map { LinearMonomial(Flt64.one, it) }, Flt64.zero)) is Ok)
        decisions = variables.mapIndexed { index, variable ->
            ColumnDecision(id = "d$index", blockId = "b${index % 4}", variable = variable)
        }
        columns = blocks.map { block ->
            ProjectedColumn(
                id = block,
                blockId = block,
                values = decisions.filter { it.blockId == block }.associate { it.id to Flt64.zero }
            )
        }
        warm = create()
        consume(warm)
    }

    /** 释放模型回调。 / Release model callbacks. */
    @TearDown
    fun tearDown() {
        source.close()
    }

    /** @return 新投影的完整入口结果 / Complete entry result for a new projection */
    @Benchmark
    fun coldRevision(): Any = runBlocking { consume(create()) }

    /** @return 同版本重复入口结果 / Repeated entry result within one revision */
    @Benchmark
    fun warmRevision(): Any = runBlocking { consume(warm) }

    private fun create() = ColumnProjection(source = source, decisions = decisions, blockIds = blocks)

    private suspend fun consume(projection: ColumnProjection<Flt64>): Any {
        return when (consumer) {
            "pricing" -> projection.pricingRequest(blockId = "b0", phaseOne = false, seed = true).value!!
            "master" -> {
                val registration = projection.master(columns = columns, phaseOne = false, integer = false).value!!
                try {
                    registration.rows.size
                } finally {
                    registration.model.close()
                }
            }
            else -> projection.snapshot().value!!
        }
    }
}
