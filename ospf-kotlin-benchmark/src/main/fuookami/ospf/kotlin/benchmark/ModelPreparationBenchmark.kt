package fuookami.ospf.kotlin.benchmark

import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.openjdk.jmh.annotations.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.framework.solver.*

/**
 * 真实依赖注册、稀疏主问题装配及局部定价准备。 / Production dependency registration, sparse master assembly and local pricing preparation.
 *
 * @property dataset 单维度按N/2N/4N扩展 / Single-dimension N/2N/4N scale
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
open class ModelPreparationBenchmark {
    @Param("small", "medium", "large")
    lateinit var dataset: String

    private lateinit var chain: List<IntermediateSymbol<*>>
    private lateinit var source: LinearMetaModel<Flt64>
    private lateinit var projection: ColumnProjection<Flt64>
    private lateinit var columns: List<ProjectedColumn<Flt64>>
    private val blocks = (0 until 8).map { "b$it" }

    /** 创建只读输入，不计时；实际注册/模型创建在每次调用内。 / Create untimed immutable inputs; register/build anew per invocation. */
    @Setup
    fun setup(): Unit = runBlocking {
        val factor = when (dataset) {
            "medium" -> 2
            "large" -> 4
            else -> 1
        }
        val symbols = mutableListOf<IntermediateSymbol<*>>()
        repeat(256 * factor) { index ->
            val required = if (index == 0) emptySet() else setOf(symbols[index - 1])
            val base = LinearExpressionSymbol(constant = Flt64.one, name = "chain_$index")
            symbols.add(object : LinearIntermediateSymbol<Flt64> by base {
                override val dependencies = required
            })
        }
        chain = symbols
        source = LinearMetaModel(name = "preparation_source")
        val variables = List(128 * factor) { BinVar("v$it") }
        check(source.add(variables) is Ok)
        repeat(32) { row ->
            val first = row % variables.size
            val second = (first + variables.size / 2 + 1) % variables.size
            check(source.addConstraint(
                relation = LinearPolynomial(
                    listOf(LinearMonomial(Flt64.one, variables[first]), LinearMonomial(Flt64.two, variables[second])),
                    Flt64.zero
                ) leq Flt64(100),
                name = "r$row"
            ) is Ok)
        }
        check(source.minimize(LinearPolynomial(variables.map { LinearMonomial(Flt64.one, it) }, Flt64.zero)) is Ok)
        val decisions = variables.mapIndexed { index, variable ->
            ColumnDecision(
                id = "d$index",
                blockId = blocks[index % blocks.size],
                variable = variable
            )
        }
        projection = ColumnProjection(
            source = source,
            decisions = decisions,
            blockIds = blocks.toSet()
        )
        columns = blocks.flatMap { block ->
            List(8) { column ->
                ProjectedColumn(
                    id = "${block}_$column",
                    blockId = block,
                    values = decisions.filter { it.blockId == block }.associate { it.id to Flt64(column % 2) }
                )
            }
        }
    }

    /** 释放源模型的token回调。 / Release source-model token callbacks. */
    @TearDown
    fun tearDown() {
        source.close()
    }

    /** @return 真实注册后缓存的末尾值 / Last cached value after production registration */
    @Benchmark
    fun registerDependencyChain(): Double {
        val table = ManualTokenTable<Flt64>(Linear, false)
        try {
            check(table.add(chain) is Ok)
            check(chain.register(table) is Ok)
            return table.cachedLinearFlattenValue(chain.last())!!.constant.toDouble()
        } finally {
            table.close()
        }
    }

    /** @return 完整装配后的行数 / Row count after full master assembly */
    @Benchmark
    fun assembleSparseMaster(): Int = runBlocking {
        val registration = projection.master(
            columns = columns,
            phaseOne = false,
            integer = false
        ).value!!
        try {
            registration.rows.size
        } finally {
            registration.model.close()
        }
    }

    /** @return 标量入口准备的局部变量数量 / Local-variable count prepared through scalar APIs */
    @Benchmark
    fun prepareScalarPricingModels(): Int = runBlocking {
        var amount = 0
        for (block in blocks) {
            val request = projection.pricingRequest(
                blockId = block,
                phaseOne = false,
                seed = true
            ).value!!
            val model = projection.pricingModel(request).value!!
            try {
                amount += model.tokens.tokens.size
            } finally {
                model.close()
            }
        }
        amount
    }
}
