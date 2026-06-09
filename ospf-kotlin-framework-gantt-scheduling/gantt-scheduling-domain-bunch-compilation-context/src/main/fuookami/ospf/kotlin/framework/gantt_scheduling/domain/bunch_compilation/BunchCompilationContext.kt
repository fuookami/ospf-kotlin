
/** 任务束编译上下文 / Bunch compilation context */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.MetaDualSolution
import fuookami.ospf.kotlin.core.model.mechanism.MetaModel
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model.BunchSolution
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.service.BunchSolutionAnalyzer
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.service.TaskSolutionAnalyzer
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.TaskSolution
import fuookami.ospf.kotlin.framework.model.invoke
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64

/**
 * 任务束编译上下文接口 / Bunch compilation context interface
 *
 * @param Args 影子价格参数类型 / Shadow price arguments type
 * @param B 任务束类型 / Bunch type
 * @param T 任务类型 / Task type
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
 */
interface BunchCompilationContext<
        Args : AbstractGanttSchedulingShadowPriceArguments<E, A>,
        B : AbstractTaskBunch<T, E, A, V>,
        V : RealNumber<V>,
        T : AbstractTask<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>
        > {
    val aggregation: BunchCompilationAggregation<B, V, T, E, A>
    val pipelineList: AbstractGanttSchedulingCGPipelineList<Args, E, A>

    val columnAmount get() = UInt64(aggregation.bunches.size)

    /**
     * 注册到模型 / Register to model
     *
     * @param model 线性元模型 / Linear meta model
     * @return 操作结果 / Operation result
     */
    fun register(model: AbstractLinearMetaModel<Flt64>): Try {
        when (val result = aggregation.register(model)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        when (val result = pipelineList(model)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        return ok
    }

    /**
     * 添加列 / Add columns
     *
     * @param iteration 迭代次数 / Iteration count
     * @param newBunches 新任务束列表 / List of new bunches
     * @param model 线性元模型 / Linear meta model
     * @return 去重后的任务束列表 / Deduplicated bunch list
     */
    suspend fun addColumns(
        iteration: UInt64,
        newBunches: List<B>,
        model: AbstractLinearMetaModel<Flt64>
    ): Ret<List<B>> {
        val unduplicatedBunches = when (val result = aggregation.addColumns(
            iteration = iteration,
            newBunches = newBunches,
            model = model
        )) {
            is Ok -> {
                result.value
            }

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        return Ok(unduplicatedBunches)
    }

    /**
     * 移除列 / Remove columns
     *
     * @param maximumReducedCost 最大约简成本 / Maximum reduced cost
     * @param maximumColumnAmount 最大列数 / Maximum column amount
     * @param reducedCost 约简成本函数 / Reduced cost function
     * @param fixedBunches 固定任务束集合 / Set of fixed bunches
     * @param keptBunches 保留任务束集合 / Set of kept bunches
     * @param model 线性元模型 / Linear meta model
     * @return 更新后的最大约简成本 / Updated maximum reduced cost
     */
    fun removeColumns(
        maximumReducedCost: Flt64,
        maximumColumnAmount: UInt64,
        reducedCost: (B) -> Flt64,
        fixedBunches: Set<B>,
        keptBunches: Set<B>,
        model: AbstractLinearMetaModel<Flt64>
    ): Ret<Flt64> {
        return aggregation.removeColumns(
            maximumReducedCost,
            maximumColumnAmount,
            reducedCost,
            fixedBunches,
            keptBunches,
            model
        )
    }

    /**
     * 提取影子价格 / Extract shadow price
     *
     * @param shadowPriceMap 影子价格映射 / Shadow price map
     * @param model 线性元模型 / Linear meta model
     * @param shadowPrices 对偶解 / Dual solution
     * @return 操作结果 / Operation result
     */
    fun extractShadowPrice(
        shadowPriceMap: AbstractGanttSchedulingShadowPriceMap<Args, E, A>,
        model: AbstractLinearMetaModel<Flt64>,
        shadowPrices: MetaDualSolution
    ): Try {
        for (pipeline in pipelineList) {
            when (val ret = pipeline.refresh(shadowPriceMap, model, shadowPrices)) {
                is Ok -> {}
                is Failed -> {
                    return Failed(ret.error)
                }

                is Fatal -> {
                    return Fatal(ret.errors)
                }
            }
            val extractor = pipeline.extractor() ?: continue
            shadowPriceMap.put(extractor)
        }
        return ok
    }

    /**
     * 提取固定任务束 / Extract fixed bunches
     *
     * @param iteration 迭代次数 / Iteration count
     * @param model 线性元模型 / Linear meta model
     * @return 固定任务束集合 / Set of fixed bunches
     */
    fun extractFixedBunches(
        iteration: UInt64,
        model: AbstractLinearMetaModel<Flt64>
    ): Ret<Set<B>> {
        return aggregation.extractFixedBunches(iteration, model)
    }

    /**
     * 提取保留任务束 / Extract kept bunches
     *
     * @param iteration 迭代次数 / Iteration count
     * @param model 线性元模型 / Linear meta model
     * @return 保留任务束集合 / Set of kept bunches
     */
    fun extractKeptBunches(
        iteration: UInt64,
        model: AbstractLinearMetaModel<Flt64>
    ): Ret<Set<B>> {
        return aggregation.extractKeptBunches(iteration, model)
    }

    /**
     * 提取保留任务束及比率 / Extract kept bunches with ratio
     *
     * @param iteration 迭代次数 / Iteration count
     * @param model 线性元模型 / Linear meta model
     * @return 任务束到比率的映射 / Map of bunch to ratio
     */
    fun extractKeptBunchesWithRatio(
        iteration: UInt64,
        model: AbstractLinearMetaModel<Flt64>
    ): Ret<Map<B, Flt64>> {
        return aggregation.extractKeptBunchesWithRatio(iteration, model)
    }

    /**
     * 提取隐藏执行器 / Extract hidden executors
     *
     * @param executors 执行器列表 / List of executors
     * @param model 线性元模型 / Linear meta model
     * @return 隐藏执行器集合 / Set of hidden executors
     */
    fun extractHiddenExecutors(
        executors: List<E>,
        model: AbstractLinearMetaModel<Flt64>
    ): Ret<Set<E>> {
        return aggregation.extractHiddenExecutors(executors, model)
    }

    /**
     * 选择空闲执行器 / Select free executors
     *
     * @param fixedBunches 固定任务束集合 / Set of fixed bunches
     * @param hiddenExecutors 隐藏执行器集合 / Set of hidden executors
     * @param shadowPriceMap 影子价格映射 / Shadow price map
     * @param model 线性元模型 / Linear meta model
     * @return 空闲执行器集合 / Set of free executors
     */
    fun <Map : AbstractGanttSchedulingShadowPriceMap<Args, E, A>> selectFreeExecutors(
        fixedBunches: Set<B>,
        hiddenExecutors: Set<E>,
        shadowPriceMap: Map,
        model: AbstractLinearMetaModel<Flt64>,
    ): Ret<Set<E>>

    /**
     * 全局固定 / Globally fix
     *
     * @param fixedBunches 固定任务束集合 / Set of fixed bunches
     * @return 操作结果 / Operation result
     */
    fun globallyFix(fixedBunches: Set<B>): Try {
        return aggregation.globallyFix(fixedBunches)
    }

    /**
     * 局部固定 / Locally fix
     *
     * @param iteration 迭代次数 / Iteration count
     * @param bar 阈值 / Threshold
     * @param fixedBunches 固定任务束集合 / Set of fixed bunches
     * @param model 线性元模型 / Linear meta model
     * @return 新固定的任务束集合 / Set of newly fixed bunches
     */
    fun locallyFix(
        iteration: UInt64,
        bar: Flt64,
        fixedBunches: Set<B>,
        model: AbstractLinearMetaModel<Flt64>
    ): Ret<Set<B>> {
        return aggregation.locallyFix(
            iteration = iteration,
            bar = bar,
            fixedBunches = fixedBunches,
            model = model
        )
    }

    /**
     * 记录结果 / Log result
     *
     * @param iteration 迭代次数 / Iteration count
     * @param model 线性元模型 / Linear meta model
     * @return 操作结果 / Operation result
     */
    fun logResult(iteration: UInt64, model: AbstractLinearMetaModel<Flt64>): Try {
        return aggregation.logResult(
            iteration = iteration,
            model = model
        )
    }

    /**
     * 记录任务束成本 / Log bunch cost
     *
     * @param iteration 迭代次数 / Iteration count
     * @param model 线性元模型 / Linear meta model
     * @return 操作结果 / Operation result
     */
    fun logBunchCost(iteration: UInt64, model: AbstractLinearMetaModel<Flt64>): Try {
        return aggregation.logBunchCost(
            iteration = iteration,
            model = model
        )
    }

    /**
     * 刷新变量范围 / Flush variable ranges
     *
     * @param iteration 迭代次数 / Iteration count
     * @return 操作结果 / Operation result
     */
    fun flush(iteration: UInt64): Try {
        return aggregation.flush(
            iteration = iteration,
            tasks = emptyList()
        )
    }

    /**
     * 分析任务解 / Analyze task solution
     *
     * @param iteration 迭代次数 / Iteration count
     * @param tasks 任务列表 / List of tasks
     * @param model 线性元模型 / Linear meta model
     * @param solution 解向量 / Solution vector
     * @return 任务解 / Task solution
     */
    fun analyzeTaskSolution(
        iteration: UInt64,
        tasks: List<T>,
        model: AbstractLinearMetaModel<Flt64>,
        solution: List<Flt64>? = null
    ): Ret<TaskSolution<T, E, A>> {
        return TaskSolutionAnalyzer(
            iteration = iteration,
            tasks = tasks,
            bunches = aggregation.bunchesIteration,
            compilation = aggregation.compilation,
            model = model,
            solution = solution
        )
    }

    /**
     * 分析任务束解 / Analyze bunch solution
     *
     * @param iteration 迭代次数 / Iteration count
     * @param tasks 任务列表 / List of tasks
     * @param model 线性元模型 / Linear meta model
     * @param solution 解向量 / Solution vector
     * @return 任务束解 / Bunch solution
     */
    fun analyzeBunchSolution(
        iteration: UInt64,
        tasks: List<T>,
        model: AbstractLinearMetaModel<Flt64>,
        solution: List<Flt64>? = null
    ): Ret<BunchSolution<B, V, T, E, A>> {
        return BunchSolutionAnalyzer(
            iteration = iteration,
            tasks = tasks,
            bunches = aggregation.bunchesIteration,
            compilation = aggregation.compilation,
            model = model,
            solution = solution
        )
    }
}

/**
 * 提取任务束编译上下文接口 / Extract bunch compilation context interface
 *
 * @param Args 影子价格参数类型 / Shadow price arguments type
 * @param B 任务束类型 / Bunch type
 * @param T 任务类型 / Task type
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
 */
interface ExtractBunchCompilationContext<
        Args : AbstractGanttSchedulingShadowPriceArguments<E, A>,
        B : AbstractTaskBunch<T, E, A, V>,
        V : RealNumber<V>,
        T : AbstractTask<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>
        > {
    val baseContext: BunchCompilationContext<Args, B, V, T, E, A>

    fun register(model: MetaModel<Flt64>): Try

    fun addColumns(
        iteration: UInt64,
        newBunches: List<B>,
        model: AbstractLinearMetaModel<Flt64>
    ): Try

    fun extractShadowPrice(
        shadowPriceMap: AbstractGanttSchedulingShadowPriceMap<Args, E, A>,
        model: AbstractLinearMetaModel<Flt64>,
        shadowPrices: MetaDualSolution
    ): Try

    fun logResult(iteration: UInt64, model: AbstractLinearMetaModel<Flt64>): Try {
        return ok
    }
}