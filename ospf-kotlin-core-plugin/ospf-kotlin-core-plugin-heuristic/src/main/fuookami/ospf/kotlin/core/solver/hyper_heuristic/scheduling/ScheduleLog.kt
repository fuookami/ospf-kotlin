package fuookami.ospf.kotlin.core.solver.hyper_heuristic.scheduling

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.solver.heuristic.*
import fuookami.ospf.kotlin.core.solver.hyper_heuristic.configuration.fingerprint

/** 协调器事件，不替代真实目标计算 / Coordinator event, never a substitute for real objective evaluation.
 * @property kind begin/dispatch/complete/drain/yield/end / begin/dispatch/complete/drain/yield/end
 * @property decision 派发序号 / Dispatch sequence
 * @property action worker 标识 / Worker identifier
 * @property evaluations 预留或实际请求 / Reserved or actual requests
 * @property steps 预留或提交步数 / Reserved or committed steps
 * @property random 协调器随机位模式 / Coordinator random bits
 * @property available 选择器输入顺序的动作 / Actions in selector input order
 * @property stopped 已观察到停止 / Observed stop
 * @property failed 失败标志 / Failure flag
 */
@Serializable
data class ScheduleEvent(
    val kind: String,
    val decision: Long = 0,
    val action: String = "",
    val evaluations: Long = 0,
    val steps: Long = 0,
    val random: Long = 0,
    val available: List<String> = emptyList(),
    val stopped: Boolean = false,
    val failed: Boolean = false
)

/** 版本化调度日志 / Versioned schedule log.
 * @property version 协议版本 / Protocol version
 * @property configuration 配置指纹 / Configuration fingerprint
 * @property problem 问题标识与数据指纹 / Problem identifier and data fingerprint
 * @property events 有序事件 / Ordered events
 * @property checksum 规范 JSON SHA-256 / Canonical JSON SHA-256
 */
@Serializable
data class ScheduleLog(
    val version: Int = 1,
    val configuration: String,
    val problem: String,
    val events: List<ScheduleEvent>,
    val checksum: String
) {
    internal fun digest(): String = fingerprint(JsonObject(Json.encodeToJsonElement(this).jsonObject - "checksum"))

    /** 验证完整源日志 / Validate a complete source log.
     * @return 校验结果 / Validation result
     */
    fun validate(): Try = validatePrefix(true)

    internal fun validatePrefix(sealed: Boolean): Try {
        if (version != 1 || configuration.isEmpty() || problem.isEmpty() || checksum != digest()) return searchInvalid("invalid schedule identity, version or checksum")
        val active = linkedMapOf<String, ScheduleEvent>()
        var decision = 0L
        var evaluationsTotal = 0L
        var stepsTotal = 0L
        var advancing = false
        var ended = false
        for (event in events) {
            if (ended || event.decision < 0 || event.evaluations < 0 || event.steps < 0) return searchInvalid("invalid schedule counters or events after end")
            when {
                event.kind == "begin" && !advancing -> advancing = true
                event.kind == "dispatch" && advancing && event.decision == decision && event.evaluations > 0 && event.steps > 0 && event.action in event.available && event.action !in active -> {
                    active[event.action] = event
                    ++decision
                }
                event.kind == "complete" -> {
                    val dispatch = active.remove(event.action) ?: return searchInvalid("unexpected schedule completion")
                    if (dispatch.decision != event.decision || event.evaluations > dispatch.evaluations || event.steps > dispatch.steps) return searchInvalid("inconsistent schedule completion allowances")
                    if (Long.MAX_VALUE - evaluationsTotal < event.evaluations || Long.MAX_VALUE - stepsTotal < event.steps) return searchInvalid("schedule counter overflow")
                    evaluationsTotal += event.evaluations
                    stepsTotal += event.steps
                }
                event.kind == "drain" && active.isNotEmpty() -> Unit
                event.kind == "yield" && advancing -> advancing = false
                event.kind == "end" && !advancing && active.isEmpty() && event.evaluations == evaluationsTotal && event.steps == stepsTotal -> ended = true
                else -> return searchInvalid("invalid schedule event ordering")
            }
        }
        return if (advancing || active.isNotEmpty() || (sealed && !ended)) searchInvalid("truncated or non-quiescent schedule") else ok
    }
}

/** 静止检查点的已消费前缀及游标 / Consumed prefix and cursor of a quiescent checkpoint.
 * @property cursor 下一事件下标 / Index of the next event
 * @property log 带身份和校验和的前缀 / Prefix with identity and checksum
 */
@Serializable
data class ScheduleCheckpoint(val cursor: Int, val log: ScheduleLog)

internal class ScheduleControl {
    private var history: ScheduleLog? = null
    private var source: ScheduleLog? = null
    private val recorded = ArrayList<ScheduleEvent>()
    private var bound = false
    var diverged = false
    val enabled: Boolean get() = history != null
    val replaying: Boolean get() = source != null && !diverged
    val peek: ScheduleEvent? get() = source?.events?.getOrNull(recorded.size)
    val ended: Boolean get() = recorded.lastOrNull()?.kind == "end"

    fun bind(configuration: String, problem: String, source: ScheduleLog?): Try {
        if (source != null) {
            val validated = source.validate()
            if (validated !is Ok) return validated
            if (source.configuration != configuration || source.problem != problem) return searchInvalid("incompatible schedule identity")
        }
        val previous = history ?: ScheduleLog(configuration = configuration, problem = problem, events = emptyList(), checksum = "")
        if (source != null && source.events.take(recorded.size) != recorded) return searchInvalid("schedule checkpoint prefix mismatch")
        history = previous
        this.source = source
        bound = true
        return ok
    }

    fun ready(): Try = if (enabled && !bound) searchInvalid("bind recordSchedule or replaySchedule after checkpoint restoration") else ok

    fun event(event: ScheduleEvent): Try {
        val ready = ready()
        if (ready !is Ok) return ready
        if (history == null) return ok
        if (diverged) return ok
        if (source != null && peek != event) {
            diverged = true
            return searchInvalid("schedule diverged at event ${recorded.size}: expected $peek, actual $event")
        }
        if (ended) return searchInvalid("schedule already ended")
        recorded.add(event)
        return ok
    }

    fun snapshot(): Ret<ScheduleCheckpoint?> {
        val previous = history ?: return Ok(null)
        if (diverged) return searchInvalid("cannot checkpoint a diverged replay")
        val prefix = previous.copy(events = recorded.toList())
        val log = prefix.copy(checksum = prefix.digest())
        return log.validatePrefix(false).map { ScheduleCheckpoint(log.events.size, log) }
    }

    fun restore(saved: ScheduleCheckpoint?): Try {
        if (saved != null) {
            val validated = saved.log.validatePrefix(false)
            if (validated !is Ok) return validated
            if (saved.cursor != saved.log.events.size) return searchInvalid("invalid schedule cursor")
            history = saved.log.copy(events = emptyList())
            recorded.clear()
            recorded.addAll(saved.log.events)
            source = null
            bound = false
        }
        return ok
    }

    fun export(): Ret<ScheduleLog> = snapshot().then { saved ->
        val log = saved?.log ?: return@then searchInvalid("schedule recording is not enabled")
        log.validate().map { log }
    }
}
