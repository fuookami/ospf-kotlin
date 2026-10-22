package fuookami.ospf.kotlin.core.solver.heuristic

import java.util.concurrent.atomic.AtomicLong
import kotlin.time.*
import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.functional.Generator
import fuookami.ospf.kotlin.math.algebra.number.Flt64

/** 全局评估令牌，所有子会话共享 / Global evaluation tokens shared by all child sessions.
 * @property limit 评估上限 / Evaluation limit
 * @param consumed 已消费额度 / Already consumed tokens
 */
class SearchBudget(val limit: Long, consumed: Long = 0, objectiveCalls: Long = 0) {
    private val calls = AtomicLong(objectiveCalls)

    /** 实际模型目标调用次数 / Actual model objective invocations. */
    val objectiveCalls: Long get() = calls.get()

    internal fun objectiveCalled() {
        calls.incrementAndGet()
    }
    private val counter = AtomicLong(consumed)

    /** 已使用额度 / Consumed tokens. */
    val used: Long get() = counter.get()

    /** 原子领取一个评估令牌 / Atomically claim one evaluation token.
     * @return 是否成功 / Whether granted
     */
    fun acquire(): Boolean {
        while (true) {
            val current = counter.get()
            if (current >= limit) {
                return false
            }
            if (counter.compareAndSet(current, current + 1)) {
                return true
            }
        }
    }
}

/** 单会话片段，不能并发复用 / Single-session slice, not reusable concurrently.
 * @property global 全局令牌 / Global tokens
 * @property evaluations 本片段额度 / Slice allowance
 * @property steps 最大完整步数 / Maximum complete steps
 * @property duration 协作式时间限制 / Cooperative time limit
 */
class SearchSlice(
    val global: SearchBudget,
    val evaluations: Long,
    val steps: Long = 1,
    val duration: Duration = Duration.INFINITE,
    private val parent: SearchSlice? = null
) {
    private val begin = TimeSource.Monotonic.markNow()
    private val counter = AtomicLong(0)

    /** 本片段已用请求 / Requests consumed by this slice. */
    val used: Long get() = counter.get()

    /** 协作式剩余额度视图，不代替原子令牌领取 / Cooperative remaining allowance, never replacing atomic admission. */
    val remaining: Long get() = minOf(
        (evaluations - used).coerceAtLeast(0),
        (global.limit - global.used).coerceAtLeast(0),
        parent?.remaining ?: Long.MAX_VALUE
    )

    /** 时间额度是否用尽 / Whether the deadline expired. */
    val expired: Boolean get() = begin.elapsedNow() >= duration || parent?.expired == true

    /** 创建嵌套片段，评估同时记入父级 / Create a nested slice charging its parent.
     * @param evaluations 子额度 / Child allowance
     * @param steps 子步数 / Child steps
     * @return 子片段 / Child slice
     */
    fun child(evaluations: Long, steps: Long): SearchSlice {
        return SearchSlice(
            global = global,
            evaluations = evaluations,
            steps = steps,
            duration = duration,
            parent = this
        )
    }

    /** 申请请求，只有成功时才消费全局额度 / Admit a request, consuming global allowance only on success.
     * @return 是否允许评估 / Whether evaluation is admitted
     */
    @Synchronized
    fun acquire(): Boolean {
        if (expired || used >= evaluations || !(parent?.acquire() ?: global.acquire())) {
            return false
        }
        counter.incrementAndGet()
        return true
    }
}

/** SplitMix64 随机流，状态可无损保存 / SplitMix64 stream with lossless state.
 * @property state 当前状态 / Current state
 */
class SearchRandom(var state: Long) : Generator<Flt64> {
    override fun invoke(): Flt64 {
        state += -7046029254386353131L
        var bits = state
        bits = (bits xor (bits ushr 30)) * -4658895280553007687L
        bits = (bits xor (bits ushr 27)) * -7723592293110705685L
        bits = bits xor (bits ushr 31)
        return Flt64((bits ushr 11).toDouble() / 9007199254740992.0)
    }

    /** 派生稳定子流，不消费父流 / Derive a stable child stream without consuming the parent.
     * @param id 稳定组件标识 / Stable component identifier
     * @return 子随机流 / Child random stream
     */
    fun split(id: String): SearchRandom {
        var seed = state
        for (character in id) {
            seed = (seed xor character.code.toLong()) * 1099511628211L
        }
        return SearchRandom(seed)
    }
}

/** 外层并行时禁止内层任务抢占共享随机流 / Prevent nested tasks from racing a session's random stream. */
internal object SearchExecution {
    val serial = ThreadLocal.withInitial { false }

    fun dispatcher(): CoroutineDispatcher {
        return if (serial.get()) Dispatchers.Unconfined else Dispatchers.Default
    }
}
