package fuookami.ospf.kotlin.core.solver.heuristic

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface

/** 搜索终止原因 / Search termination reason. */
@Serializable
enum class SearchTermination { SliceComplete, EvaluationLimit, StepLimit, TimeLimit, Stopped, NoCandidate, Failed }

/** 解编码能力，未知编码必须显式适配 / Solution encoding capability; custom encodings require explicit adaptation. */
@Serializable
enum class SearchEncoding { Continuous, Permutation, Custom }

/** 无损状态编码协议 / Lossless state codec.
 * @param T 被编码的状态 / Encoded state
 */
interface SearchCodec<T> {
    /** 编码状态 / Encode state.
     * @param value 原始值 / Original value
     * @return JSON 值 / JSON value
     */
    fun encode(value: T): Ret<JsonElement>

    /** 解码状态 / Decode state.
     * @param value JSON 值 / JSON value
     * @return 原始值 / Original value
     */
    fun decode(value: JsonElement): Ret<T>
}

/** 问题回调闭包的可恢复状态，令牌缓存无需保存 / Recoverable callback state; disposable token caches are not saved. */
interface SearchProblemState {
    /** 保存回调状态 / Save callback state.
     * @return 状态 / State
     */
    fun save(): Ret<JsonElement>

    /** 恢复回调状态 / Restore callback state.
     * @param state 状态 / State
     * @return 恢复结果 / Restore result
     */
    fun load(state: JsonElement): Try
}

/** 问题边界，目标比较与支配由调用方定义 / Problem boundary with caller-defined ordering or dominance.
 * @property id 问题标识 / Problem identifier
 * @property fingerprint 输入及目标定义指纹 / Input and objective fingerprint
 * @property model 独占的回调模型 / Exclusively owned callback model
 * @property objectiveCodec 目标编码 / Objective codec
 * @property valueCodec 解值编码 / Solution value codec
 * @property dominates 严格优于或支配 / Strict improvement or dominance
 * @property equivalent 目标等价 / Objective equivalence
 * @property multiObjective 是否使用偏序 / Whether ordering is partial
 * @property valid 目标有效性 / Objective validity
 * @property feasible 可行性策略 / Feasibility policy
 * @property state 回调恢复状态 / Recoverable callback state
 * @property encoding 解编码 / Solution encoding
 * @property validSolution 编码与维度校验 / Encoding and dimension validation
 */
class SearchProblem<ObjValue, V>(
    val id: String,
    val fingerprint: String,
    val model: AbstractCallBackModelInterface<*, ObjValue, V>,
    val objectiveCodec: SearchCodec<ObjValue>,
    val valueCodec: SearchCodec<V>,
    val dominates: (ObjValue, ObjValue) -> Boolean,
    val equivalent: (ObjValue, ObjValue) -> Boolean = { lhs, rhs -> lhs == rhs },
    val multiObjective: Boolean = false,
    val valid: (ObjValue) -> Boolean = { true },
    val feasible: (Solution<V>) -> Boolean = { model.constraintSatisfied(it) == true },
    val state: SearchProblemState? = null,
    val encoding: SearchEncoding = SearchEncoding.Continuous,
    val validSolution: (Solution<V>) -> Boolean = { it.size == model.tokens.tokensInSolver.size }
) where V : RealNumber<V>, V : NumberField<V>

/** 一次片段的执行报告 / Execution report for one slice.
 * @property candidates 已评估的有效候选 / Valid evaluated candidates
 * @property evaluations 已消耗评估请求 / Consumed evaluation requests
 * @property steps 完整提交的低层步数 / Fully committed low-level steps
 * @property termination 片段终止原因 / Slice termination
 * @property diagnostics 步进失败诊断 / Step failure diagnostics
 */
data class SearchStepResult<ObjValue, V>(
    val candidates: List<Individual<ObjValue, V>>,
    val evaluations: Long,
    val steps: Long,
    val termination: SearchTermination,
    val diagnostics: List<String> = emptyList()
) where V : RealNumber<V>, V : NumberField<V>

/** 搜索运行结果，多目标不伪造唯一最优 / Search result without fabricating a unique optimum for multiobjective search.
 * @property archive 有效解档案 / Valid solution archive
 * @property multiObjective 是否为多目标 / Whether multiobjective
 * @property termination 终止原因 / Termination reason
 * @property evaluations 已用评估 / Consumed evaluations
 * @property decisions 已完成高层决策 / Completed high-level decisions
 * @property steps 完整低层步数 / Complete low-level steps
 * @property activeNanos 有效运行纳秒数 / Active runtime in nanoseconds
 * @property diagnostics 运行诊断 / Runtime diagnostics
 * @property objectiveCalls 实际模型目标调用数 / Actual model objective invocations
 */
data class SearchResult<O, V>(
    val archive: List<Individual<O, V>>,
    val multiObjective: Boolean,
    val termination: SearchTermination,
    val evaluations: Long,
    val decisions: Long,
    val steps: Long,
    val activeNanos: Long,
    val diagnostics: List<String> = emptyList(),
    val objectiveCalls: Long = 0
) where V : RealNumber<V>, V : NumberField<V> {
    /** 单目标最优，多目标为 null / Scalar optimum, null for multiobjective search. */
    val best: Individual<O, V>? get() = if (multiObjective) null else archive.firstOrNull()
}

/** 会话快照，不保存调用栈 / Session snapshot without execution stacks.
 * @property version 协议版本 / Protocol version
 * @property initialized 是否已初始化 / Whether initialized
 * @property iteration 局部步数 / Local iterations
 * @property stagnation 无改进步数 / Stagnation
 * @property state 算法原生状态 / Native algorithm state
 * @property archive 局部档案 / Local archive
 * @property activeNanos 有效运行纳秒数 / Active runtime in nanoseconds
 */
@Serializable
data class SearchSessionSnapshot(
    val version: Int = 1,
    val initialized: Boolean,
    val iteration: Long,
    val stagnation: Long,
    val state: JsonElement,
    val archive: JsonElement,
    val activeNanos: Long = 0
)

/** 可分片执行的搜索会话 / Sliceable search session.
 * @param ObjValue 目标类型 / Objective type
 * @param V 解值类型 / Solution value type
 */
interface SearchSession<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
    /** 是否能在步边界导入解 / Whether solutions can be imported at step boundaries. */
    val supportsImport: Boolean get() = false

    /** 执行预算片段 / Execute a budget slice.
     * @param budget 片段预算 / Slice budget
     * @return 执行报告 / Execution report
     */
    suspend fun step(budget: SearchSlice): Ret<SearchStepResult<ObjValue, V>>

    /** 在步进边界导入解 / Import solutions at a step boundary.
     * @param solutions 待重新评估的解 / Solutions to re-evaluate
     * @param budget 导入评估预算 / Import budget
     * @return 评估报告 / Evaluation report
     */
    suspend fun importSolutions(solutions: List<Solution<V>>, budget: SearchSlice): Ret<SearchStepResult<ObjValue, V>>

    /** 获取完整状态 / Capture complete state.
     * @return 会话快照 / Session snapshot
     */
    fun snapshot(): Ret<SearchSessionSnapshot>

    /** 恢复完整状态 / Restore complete state.
     * @param snapshot 会话快照 / Session snapshot
     * @return 恢复结果 / Restore result
     */
    fun restore(snapshot: SearchSessionSnapshot): Try

    /** 关闭会话，模型所有权由运行器管理 / Close session; the execution owns the model. */
    fun close(): Try = ok
}

/** 创建独立会话 / Create an independent session.
 * @param ObjValue 目标类型 / Objective type
 * @param V 解值类型 / Solution value type
 */
fun interface SearchSessionFactory<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
    /** 创建会话 / Create session.
     * @param problem 独立问题实例 / Independent problem
     * @param random 可恢复随机流 / Restorable random stream
     * @return 新会话 / New session
     */
    fun create(problem: SearchProblem<ObjValue, V>, random: SearchRandom): Ret<SearchSession<ObjValue, V>>
}

/** 将外部回调或序列化异常转换为返回错误 / Convert callback or serialization exceptions into returned errors.
 * @param block 外部边界操作 / External boundary operation
 * @return 操作结果 / Operation result
 */
internal inline fun <T> searchBoundary(block: () -> T): Ret<T> {
    return try {
        Ok(block())
    } catch (exception: Exception) {
        Failed(ErrorCode.ApplicationFailed, "搜索边界失败 / Search boundary failed: ${exception.message}")
    }
}
