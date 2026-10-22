/**
 * 搜索骨架 / Search skeleton
*/
package fuookami.ospf.kotlin.core.solver.heuristic.runner

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.heuristic.*
import fuookami.ospf.kotlin.core.solver.heuristic.operator.archive.Archive
import fuookami.ospf.kotlin.core.solver.heuristic.operator.stopping.StoppingCriterion

/**
 * 一级搜索骨架，只负责「迭代 / 档案 / 回调」这三件与算法无关的事。 / First-level search skeleton that owns only the three algorithm-agnostic concerns: iteration, archive, and callbacks.
 *
 * 具体算法只需实现三个钩子：[initialize] 建立初始状态、[advance] 推进一步、[membersOf] 把状态摊平成个体列表。
 * 这一步把「循环怎么转」与「一步算什么」彻底分开。 / A concrete algorithm implements three hooks only: [initialize] builds the initial state, [advance] performs one step,
 * and [membersOf] flattens the state into individuals. This fully separates "how the loop runs" from "what one step computes".
 *
 * @param State 算法状态类型 / Algorithm state type
 * @param ObjValue 目标值类型 / Objective value type
 * @param V 值类型 / Value type
 * @property archive 收敛档案 / Convergence archive
 * @property stopping 终止判据 / Stopping criterion
 * @property converter 值转换器 / Value converter
 * @property observers 迭代观测钩子，每轮结束后广播「是否改进」/ Iteration observers broadcasting "did this round improve" after each round
*/
abstract class AbstractRunner<State, ObjValue, V>(
    val archive: Archive<ObjValue, V>,
    val stopping: StoppingCriterion,
    val converter: IntoValue<V>,
    val observers: List<IterationObserver> = emptyList()
) where V : RealNumber<V>, V : NumberField<V> {

    /**
     * 建立初始状态 / Build the initial state
     *
     * @param model 回调模型接口 / Callback model interface
     * @param iteration 当前迭代 / Current iteration
     * @return 初始状态 / Initial state
    */
    protected abstract suspend fun initialize(
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        iteration: Iteration
    ): State

    /**
     * 推进一步 / Advance one step
     *
     * @param model 回调模型接口 / Callback model interface
     * @param iteration 当前迭代 / Current iteration
     * @param state 当前状态 / Current state
     * @return 推进后的状态 / Advanced state
    */
    protected abstract suspend fun advance(
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        iteration: Iteration,
        state: State
    ): State

    /**
     * 把状态摊平为个体列表 / Flatten the state into individuals
     *
     * @param state 当前状态 / Current state
     * @return 个体列表 / Individual list
    */
    protected abstract fun membersOf(state: State): List<Individual<ObjValue, V>>

    internal suspend fun initializeStep(
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        iteration: Iteration
    ): State = initialize(model, iteration)

    internal suspend fun advanceStep(
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        iteration: Iteration,
        state: State
    ): State = advance(model, iteration, state)

    internal fun stateMembers(state: State): List<Individual<ObjValue, V>> = membersOf(state)

    /** 创建保留原生状态的会话 / Open a native-state session.
     * @param problem 独占问题 / Exclusively owned problem
     * @param codec 状态编码器 / State codec
     * @param importer 解导入策略 / Import policy
     * @return 新会话 / New session
     */
    fun openSession(
        problem: SearchProblem<ObjValue, V>,
        codec: SearchCodec<State>,
        importer: ((State, List<Individual<ObjValue, V>>) -> Ret<State>)? = null
    ): RunnerSearchSession<State, ObjValue, V> {
        archive.clear()
        observers.forEach { it.reset() }
        return RunnerSearchSession(
            runner = this,
            problem = problem,
            codec = codec,
            importer = importer
        )
    }

    /** 执行完整搜索，复用会话步进 / Run a complete search using session steps.
     * @param model 回调模型 / Callback model
     * @param iteration 迭代计数器 / Iteration counter
     * @param runningCallBack 运行回调 / Running callback
     * @return 档案或错误 / Archive or error
     */
    open suspend fun run(
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        iteration: Iteration = Iteration(),
        runningCallBack: ((Iteration, List<Individual<ObjValue, V>>) -> Try)? = null
    ): Ret<List<Individual<ObjValue, V>>> {
        archive.clear()
        observers.forEach { it.reset() }
        val problem = SearchProblem(
            id = "standalone",
            fingerprint = "standalone",
            model = model,
            objectiveCodec = unavailableCodec<ObjValue>(),
            valueCodec = unavailableCodec<V>(),
            dominates = { lhs, rhs -> fuookami.ospf.kotlin.core.solver.heuristic.operator.compareStrictly(model, lhs, rhs) < 0 }
        )
        val session = RunnerSearchSession(
            runner = this,
            problem = problem,
            codec = null,
            importer = null,
            initialIteration = iteration
        )
        val budget = SearchBudget(Long.MAX_VALUE)
        var first = true
        while (first || !stopping(iteration)) {
            first = false
            val result = session.step(SearchSlice(budget, Long.MAX_VALUE))
            if (result !is Ok) return searchFailure(result)
            if (result.value.termination == SearchTermination.Failed) {
                return fuookami.ospf.kotlin.utils.functional.Failed(
                    fuookami.ospf.kotlin.utils.error.ErrorCode.ApplicationFailed,
                    result.value.diagnostics.joinToString("; "),
                    archive.individuals
                )
            }
            if (result.value.termination == SearchTermination.NoCandidate) break
            if (runningCallBack != null) {
                val callback = searchBoundary { runningCallBack(iteration, archive.individuals) }.then { it }
                if (callback !is Ok) return searchFailure(callback)
            }
        }
        return Ok(archive.individuals)
    }

    private fun <T> unavailableCodec(): SearchCodec<T> {
        return object : SearchCodec<T> {
            override fun encode(value: T): Ret<kotlinx.serialization.json.JsonElement> = searchInvalid("standalone searches do not expose checkpoints")
            override fun decode(value: kotlinx.serialization.json.JsonElement): Ret<T> = searchInvalid("standalone searches do not expose checkpoints")
        }
    }
}
