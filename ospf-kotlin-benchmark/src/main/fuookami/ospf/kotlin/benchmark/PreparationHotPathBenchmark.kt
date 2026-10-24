package fuookami.ospf.kotlin.benchmark

import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations.*
import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.ordinary.PrimeCache
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.constraint_programming.*
import fuookami.ospf.kotlin.core.solver.constraint_programming.*
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingSolution
import fuookami.ospf.kotlin.core.solver.report.VariableId

/**
 * 使用真实入口测量批量准备，规模按 N/2N/4N 增长。 / Measure production preparation APIs at N/2N/4N sizes.
 *
 * @property dataset 数据规模 small/medium/large / Dataset size small/medium/large
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
open class PreparationHotPathBenchmark {
    @Param("small", "medium", "large")
    lateinit var dataset: String

    private var primeLimit = 0
    private lateinit var shape: Shape2
    private lateinit var snapshot: ConstraintProgrammingModelSnapshot
    private lateinit var solution: ConstraintProgrammingSolution
    private lateinit var envelope: ConstraintProgrammingCheckpointEnvelope
    private lateinit var tokenMap: Map<VariableItemKey, Token<Flt64>>
    private lateinit var sparseDomain: IntegerDomain.Values
    private lateinit var registrationVariables: List<RealVar>
    private lateinit var bulkWriter: ManualTokenList<Flt64>
    private lateinit var bulkOwner: TokenList<Flt64>
    private lateinit var bulkValues: List<Flt64>
    private val solver = FakeConstraintProgrammingSolver()

    /** 初始化只读输入，缓存捕获夹具包含完整 incumbent。 / Prepare immutable inputs and a checkpoint containing a complete incumbent. */
    @Setup
    fun setup() {
        val factor = when (dataset) {
            "medium" -> 2
            "large" -> 4
            else -> 1
        }
        primeLimit = 10_000 * factor
        shape = Shape2(64, 16 * factor)
        val tokenList = ManualTokenList<Flt64>(checkTokenExisted = true)
        tokenList.add(List(1_024 * factor) { RealVar("token_$it") })
        tokenMap = tokenList.tokens.associateBy { it.key }
        tokenList.close()
        sparseDomain = IntegerDomain.values(List(4_096 * factor) { it * 2 }).value!!
        registrationVariables = List(1_024 * factor) { RealVar("registration_$it") }
        bulkWriter = ManualTokenList(checkTokenExisted = true)
        bulkWriter.add(registrationVariables)
        bulkOwner = TokenList(bulkWriter.tokensInSolver.reversed().associateBy { it.key })
        bulkValues = List(registrationVariables.size) { Flt64.one }
        val variables = List(128 * factor) { index ->
            ConstraintProgrammingVariableSnapshot(
                id = VariableId("v$index"),
                name = "v$index",
                typeName = "Integer",
                domain = IntegerDomain.boolean
            )
        }
        snapshot = ConstraintProgrammingModelSnapshot(
            name = "preparation",
            objectCategory = ObjectCategory.Minimum,
            variables = variables,
            intervals = emptyList(),
            expressions = emptyList(),
            constraints = emptyList(),
            objectives = emptyList(),
            constraintGroups = emptyList()
        )
        solution = ConstraintProgrammingSolution(values = variables.associate { it.id to Int64.one })
        envelope = captureCheckpoint()
    }

    /** 解除共享 owner 回调，避免夹具保留 token 列表。 / Detach shared-owner callbacks so fixtures do not retain token lists. */
    @TearDown
    fun tearDown() {
        bulkOwner.close()
        bulkWriter.close()
    }

    /**
     * 冷缓存连续素性查询，不复刻筛法实现。 / Consecutive primality queries from a cold production cache.
     * @return 素数数量 / Number of primes
     */
    @Benchmark
    fun coldPrimeSequence(): Int {
        val cache = PrimeCache()
        var count = 0
        for (value in 2..primeLimit) {
            if (cache.isPrime(UInt64(value))) count++
        }
        return count
    }

    /**
     * 直接返回数组以消费实际分配结果。 / Return the allocated array to consume the actual result.
     * @return 常量数组 / Constant-filled array
     */
    @Benchmark
    fun constantArray(): MutableMultiArray<Int, Shape2> {
        return MutableMultiArray.newWith(shape, 7)
    }

    /**
     * 冷 token 位置索引，包括创建/排序和全部不同 key 的首次查询。 / Cold token positions, including construction/sorting and first queries for all distinct keys.
     * @return 索引校验和 / Position checksum
     */
    @Benchmark
    fun coldTokenIndices(): Int {
        val tokens = TokenList(tokenMap)
        try {
            var sum = 0
            for (token in tokenMap.values) {
                sum += tokens.indexOf(token) ?: -1
            }
            return sum
        } finally {
            tokens.close()
        }
    }

    /**
     * 固定查询数，仅改变离散域基数。 / Keep query count fixed while varying sparse-domain cardinality.
     * @return 命中次数 / Membership count
     */
    @Benchmark
    fun sparseDomainMembership(): Int {
        var hits = 0
        for (query in -128L until 0L) {
            if (Int64(query) in sparseDomain) ++hits
        }
        return hits
    }

    /**
     * 每轮先清空解，使逆序共享 owner 暴露重复存在性扫描。 / Clear results per operation so a reversed shared owner exposes repeated existence scans.
     * @return 共享 owner 是否观察到结果 / Whether the shared owner observes results
     */
    @Benchmark
    fun bulkSolutionWithSharedOwner(): Boolean {
        bulkWriter.clearSolution()
        bulkWriter.setSolution(bulkValues)
        return bulkOwner.cachedSolution
    }

    /**
     * 单项生产注册，包含事务与 callback 成本。 / Production single-item registration, including transactions and callbacks.
     * @return 注册数量 / Registered count
     */
    @Benchmark
    fun singleTokenRegistration(): Int {
        val tokens = ManualTokenList<Flt64>(checkTokenExisted = true)
        try {
            for (variable in registrationVariables) {
                tokens.add(variable)
            }
            return tokens.tokens.size
        } finally {
            tokens.close()
        }
    }

    /**
     * 同一数据的一次批量生产注册，供对照单项入口。 / One production batch over the same data, alongside single-item registration.
     * @return 注册数量 / Registered count
     */
    @Benchmark
    fun batchTokenRegistration(): Int {
        val tokens = ManualTokenList<Flt64>(checkTokenExisted = true)
        try {
            tokens.add(registrationVariables)
            return tokens.tokens.size
        } finally {
            tokens.close()
        }
    }

    /**
     * 捕获完整解，覆盖逐变量值域校验。 / Capture a full incumbent, including per-variable domain validation.
     * @return checkpoint envelope / Checkpoint envelope
     */
    @Benchmark
    fun captureCheckpoint(): ConstraintProgrammingCheckpointEnvelope {
        return ConstraintProgrammingCheckpointCodec.capture(
            snapshot = snapshot,
            descriptor = solver.descriptor,
            checkpointId = "preparation-checkpoint",
            createdAtEpochMs = 0L,
            incumbent = solution
        ).value!!
    }

    /**
     * 恢复完整解，覆盖同一批次变量索引。 / Restore a full incumbent through the batch variable-index path.
     * @return 恢复的变量数量 / Number of restored variables
     */
    @Benchmark
    fun restoreCheckpoint(): Int {
        return ConstraintProgrammingCheckpointCodec.restore(
            envelope = envelope,
            snapshot = snapshot,
            expectedConfigurationFingerprint = null,
            expectedSolverFingerprint = envelope.solverFingerprint,
            allowLegacyConfigurationFingerprint = true
        ).value!!.incumbent!!.values.size
    }
}
