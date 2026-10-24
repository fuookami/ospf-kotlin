package fuookami.ospf.kotlin.benchmark

import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.openjdk.jmh.annotations.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.parallel.mapParallelly
import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.multiarray.einsum.contract
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.expression.*
import fuookami.ospf.kotlin.math.symbol.expression.operation.*
import fuookami.ospf.kotlin.math.symbol.monomial.CanonicalMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.CanonicalPolynomial
import fuookami.ospf.kotlin.math.symbol.operation.compileGradientCanonical

/**
 * 数学、数组与有界并发的真实入口对照夹具。 / Production-entry fixtures for math, arrays and bounded concurrency.
 *
 * @property dataset 按N/2N/4N增长的规模 / Dataset growing at N/2N/4N
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
open class MathPreparationBenchmark {
    @Param("small", "medium", "large")
    lateinit var dataset: String

    private lateinit var array: MultiArray<Flt64, Shape3>
    private lateinit var a: MultiArray<Flt64, Shape2>
    private lateinit var b: MultiArray<Flt64, Shape2>
    private lateinit var floatGradient: (List<Flt64>) -> List<Flt64>
    private lateinit var integerGradient: (List<Int64>) -> List<Int64>
    private lateinit var floatValues: List<Flt64>
    private lateinit var integerValues: List<Int64>
    private lateinit var sharedExpression: BooleanExpression
    private lateinit var flatExpression: BooleanExpression
    private lateinit var tasks: List<Int>
    private val reductionAxes = intArrayOf(0, 2)
    private val normalization = NormalizeConfig(sortOperands = true)

    private data class Variable(override val name: String) : Symbol {
        override val displayName: String? get() = null
    }

    /** 在计时外创建输入及编译闭包。 / Create inputs and compile closures outside timing. */
    @Setup
    fun setup() {
        val factor = when (dataset) {
            "medium" -> 2
            "large" -> 4
            else -> 1
        }
        array = MultiArray.newBy(Shape3(
            d1 = 32,
            d2 = 16 * factor,
            d3 = 8
        )) { index, _ -> Flt64((index % 17 - 8) / 10.0) }
        a = MultiArray.newBy(Shape2(16, 32 * factor)) { index, _ -> Flt64((index % 7 - 3) / 10.0) }
        b = MultiArray.newBy(Shape2(32 * factor, 16)) { index, _ -> Flt64((index % 11 - 5) / 10.0) }
        val symbols: List<Symbol> = List(16 * factor) { Variable("x$it") }
        val powers = symbols.associateWith { Int32(4) }
        floatGradient = CanonicalPolynomial(
            monomials = listOf(CanonicalMonomial(Flt64(0.1), powers)),
            constant = Flt64.zero
        ).compileGradientCanonical(
            order = symbols,
            combineTerms = false,
            zero = Flt64.zero
        ).value!!
        integerGradient = CanonicalPolynomial(
            monomials = listOf(CanonicalMonomial(Int64(7L), powers)),
            constant = Int64.zero
        ).compileGradientCanonical(
            order = symbols,
            combineTerms = false,
            zero = Int64.zero
        ).value!!
        floatValues = List(symbols.size) { Flt64(0.99 + it / 10000.0) }
        integerValues = List(symbols.size) { Int64(if (it % 2 == 0) 1L else -1L) }
        var shared: ScalarExpression<Int> = ScalarReference(PropertyPath.parse("shared"))
        repeat(6) { shared = ScalarFunction(name = "dag", arguments = listOf(shared, shared)) }
        sharedExpression = AndExpression(List(16 * factor) { index ->
            Comparison(
                operator = ComparisonOperator.Eq,
                left = shared,
                right = ScalarConstant(index % 7)
            )
        })
        flatExpression = AndExpression(List(16 * factor) { index ->
            Comparison(
                operator = ComparisonOperator.Eq,
                left = ScalarReference<Int>(PropertyPath.parse("p$index")),
                right = ScalarConstant(index)
            )
        })
        tasks = List(512 * factor) { it }
    }

    /** @return 浮点梯度 / Floating gradient */
    @Benchmark
    fun strictGradient(): List<Flt64> = floatGradient(floatValues)

    /** @return 整数梯度 / Integer gradient */
    @Benchmark
    fun exactGradient(): List<Int64> = integerGradient(integerValues)

    /** @return 多轴归约结果 / Multi-axis reduction result */
    @Benchmark
    fun sumAxes(): Ret<MultiArray<Flt64, DynShape>> = array.sumAxesSafe(reductionAxes, Flt64.zero)

    /** @return 前缀归约结果 / Prefix reduction result */
    @Benchmark
    fun cumsumAxis(): Ret<MultiArray<Flt64, DynShape>> = array.cumsumAxisSafe(1, Flt64.zero)

    /** @return 缩并结果 / Contraction result */
    @Benchmark
    fun contraction(): Ret<MultiArray<Flt64, DynShape>> {
        return contract(
            a = a,
            axisA = 1,
            b = b,
            axisB = 0,
            zero = Flt64.zero
        )
    }

    /** @return 共享标量DAG的规范化结果 / Normalized shared-scalar DAG */
    @Benchmark
    fun normalizeShared(): BooleanExpression = normalize(sharedExpression, normalization)

    /** @return 普通浅树的规范化结果 / Normalized ordinary shallow tree */
    @Benchmark
    fun normalizeFlat(): BooleanExpression = normalize(flatExpression, normalization)

    /** @return 同序遍历的真实累加结果 / Production matching-order traversal accumulation */
    @Benchmark
    fun matchingTraversal(): Double {
        var sum = Flt64.zero
        for (value in array.iterWithOrder()) sum += value
        return sum.toDouble()
    }

    /** @return 四worker按序映射结果 / Ordered four-worker mapping result */
    @Benchmark
    fun boundedWorkerMap(): List<Int> = runBlocking {
        tasks.mapParallelly(concurrentAmount = 4uL) { it + 1 }
    }
}

/** 单因子入口的回归哨兵，不重复扩展同一固定规模。 / Fixed-size one-factor regression sentinel. */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
open class CanonicalLinearPreparationBenchmark {
    private lateinit var floating: (List<Flt64>) -> List<Flt64>
    private lateinit var integer: (List<Int64>) -> List<Int64>
    private val floatingValues = listOf(Flt64(1.5))
    private val integerValues = listOf(Int64(3L))

    /** 编译不计入求值计时。 / Exclude compilation from evaluation timing. */
    @Setup
    fun setup() {
        val symbol = object : Symbol {
            override val name = "linear"
            override val displayName: String? get() = null
        }
        floating = CanonicalPolynomial(
            monomials = listOf(CanonicalMonomial(Flt64(0.1), listOf(symbol))),
            constant = Flt64.zero
        ).compileGradientCanonical(
            order = listOf(symbol),
            combineTerms = false,
            zero = Flt64.zero
        ).value!!
        integer = CanonicalPolynomial(
            monomials = listOf(CanonicalMonomial(Int64(7L), listOf(symbol))),
            constant = Int64.zero
        ).compileGradientCanonical(
            order = listOf(symbol),
            combineTerms = false,
            zero = Int64.zero
        ).value!!
    }

    /** @return 单因子浮点梯度 / Single-factor floating gradient */
    @Benchmark
    fun strictLinear(): List<Flt64> = floating(floatingValues)

    /** @return 单因子整数梯度 / Single-factor integer gradient */
    @Benchmark
    fun exactLinear(): List<Int64> = integer(integerValues)
}
