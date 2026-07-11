/** 迭代任务编译上下文 / Iterative task compilation context */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.framework.model.invoke
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.service.*

/**
 * 迭代任务编译上下文接口 / Iterative task compilation context interface
 *
 * @param Args 影子价格参数类型 / Shadow price arguments type
 * @param IT 迭代任务类型 / Iterative task type
 * @param T 任务类型 / Task type
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
*/
interface IterativeTaskCompilationContext<
        Args : AbstractGanttSchedulingShadowPriceArguments<E, A>,
        V : RealNumber<V>,
        IT : IterativeAbstractTask<E, A>,
        T : AbstractTask<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>
        > {

    /** Iterative task compilation aggregation / 迭代任务编译聚合 */
    val aggregation: IterativeTaskCompilationAggregation<V, IT, T, E, A>

    /** List of column generation pipelines / 列生成管道列表 */
    val pipelineList: AbstractGanttSchedulingCGPipelineList<Args, E, A>

    /** Number of columns, derived from aggregation task count / 列数量，从聚合任务数派生 */
    val columnAmount get() = UInt64(aggregation.tasks.size)

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
     * @param newTasks 新任务列表 / List of new tasks
     * @param model 线性元模型 / Linear meta model
     * @return 去重后的任务列表 / Deduplicated task list
    */
    suspend fun addColumns(
        iteration: UInt64,
        newTasks: List<IT>,
        model: AbstractLinearMetaModel<Flt64>
    ): Ret<List<AbstractTask<E, A>>> {
        val unduplicatedTasks = when (val result = aggregation.addColumns(
            iteration = iteration,
            newTasks = newTasks,
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

        return Ok(unduplicatedTasks)
    }

    /**
     * 移除列 / Remove columns
     *
     * @param maximumReducedCost 最大约简成本 / Maximum reduced cost
     * @param maximumColumnAmount 最大列数 / Maximum column amount
     * @param reducedCost 约简成本函数 / Reduced cost function
     * @param fixedTasks 固定任务集合 / Set of fixed tasks
     * @param keptTasks 保留任务集合 / Set of kept tasks
     * @param model 线性元模型 / Linear meta model
     * @return 更新后的最大约简成本 / Updated maximum reduced cost
    */
    fun removeColumns(
        maximumReducedCost: Flt64,
        maximumColumnAmount: UInt64,
        reducedCost: (IT) -> Flt64,
        fixedTasks: Set<IT>,
        keptTasks: Set<IT>,
        model: AbstractLinearMetaModel<Flt64>
    ): Ret<Flt64> {
        return aggregation.removeColumns(
            maximumReducedCost = maximumReducedCost,
            maximumColumnAmount = maximumColumnAmount,
            reducedCost = reducedCost,
            fixedTasks = fixedTasks,
            keptTasks = keptTasks,
            model = model
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
            when (val ret = pipeline.refresh(
                shadowPriceMap = shadowPriceMap,
                model = model,
                shadowPrices = shadowPrices
            )) {
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
     * 提取固定任务 / Extract fixed tasks
     *
     * @param iteration 迭代次数 / Iteration count
     * @param model 线性元模型 / Linear meta model
     * @return 固定任务集合 / Set of fixed tasks
    */
    fun extractFixedTasks(iteration: UInt64, model: AbstractLinearMetaModel<Flt64>): Ret<Set<IT>> {
        return aggregation.extractFixedTasks(iteration, model)
    }

    /**
     * 提取保留任务 / Extract kept tasks
     *
     * @param iteration 迭代次数 / Iteration count
     * @param model 线性元模型 / Linear meta model
     * @return 保留任务集合 / Set of kept tasks
    */
    fun extractKeptTasks(iteration: UInt64, model: AbstractLinearMetaModel<Flt64>): Ret<Set<IT>> {
        return aggregation.extractKeptTasks(iteration, model)
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
     * @param fixedTasks 固定任务集合 / Set of fixed tasks
     * @param hiddenExecutors 隐藏执行器集合 / Set of hidden executors
     * @param shadowPriceMap 影子价格映射 / Shadow price map
     * @param model 线性元模型 / Linear meta model
     * @return 空闲执行器集合 / Set of free executors
    */
    fun <Map : AbstractGanttSchedulingShadowPriceMap<Args, E, A>> selectFreeExecutors(
        fixedTasks: Set<IT>,
        hiddenExecutors: Set<E>,
        shadowPriceMap: Map,
        model: AbstractLinearMetaModel<Flt64>,
    ): Ret<Set<E>>

    /**
     * 全局固定 / Globally fix
     *
     * @param fixedTasks 固定任务集合 / Set of fixed tasks
     * @return 操作结果 / Operation result
    */
    fun globallyFix(fixedTasks: Set<IT>): Try {
        return aggregation.globallyFix(fixedTasks)
    }

    /**
     * 局部固定 / Locally fix
     *
     * @param iteration 迭代次数 / Iteration count
     * @param bar 阈值 / Threshold
     * @param fixedTasks 固定任务集合 / Set of fixed tasks
     * @param model 线性元模型 / Linear meta model
     * @return 新固定的任务集合 / Set of newly fixed tasks
    */
    fun locallyFix(
        iteration: UInt64,
        bar: Flt64,
        fixedTasks: Set<IT>,
        model: AbstractLinearMetaModel<Flt64>
    ): Ret<Set<IT>> {
        return aggregation.locallyFix(
            iteration = iteration,
            bar = bar,
            fixedTasks = fixedTasks,
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
        return aggregation.logResult(iteration, model)
    }

    /**
     * 记录任务成本 / Log task cost
     *
     * @param iteration 迭代次数 / Iteration count
     * @param model 线性元模型 / Linear meta model
     * @return 操作结果 / Operation result
    */
    fun logTaskCost(iteration: UInt64, model: AbstractLinearMetaModel<Flt64>): Try {
        return aggregation.logTaskCost(iteration, model)
    }

    /**
     * 刷新变量范围 / Flush variable ranges
     *
     * @param iteration 迭代次数 / Iteration count
     * @return 操作结果 / Operation result
    */
    fun flush(iteration: UInt64): Try {
        return aggregation.flush(iteration, emptyList())
    }

    /**
     * 分析解 / Analyze solution
     *
     * @param iteration 迭代次数 / Iteration count
     * @param tasks 任务列表 / List of tasks
     * @param model 线性元模型 / Linear meta model
     * @param solution 解向量 / Solution vector
     * @return 任务解 / Task solution
    */
    fun analyzeSolution(
        iteration: UInt64,
        tasks: List<T>,
        model: AbstractLinearMetaModel<Flt64>,
        solution: List<Flt64>? = null
    ): Ret<TaskSolution<T, E, A>> {
        return SolutionAnalyzer(
            iteration = iteration,
            originTasks = tasks,
            tasks = aggregation.tasksIteration,
            compilation = aggregation.compilation,
            model = model,
            solution = solution
        )
    }
}

/**
 * 提取迭代任务编译上下文接口 / Extract iterative task compilation context interface
 *
 * @param Args 影子价格参数类型 / Shadow price arguments type
 * @param IT 迭代任务类型 / Iterative task type
 * @param T 任务类型 / Task type
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
*/
interface ExtractIterativeTaskCompilationContext<
        Args : AbstractGanttSchedulingShadowPriceArguments<E, A>,
        V : RealNumber<V>,
        IT : IterativeAbstractTask<E, A>,
        T : AbstractTask<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>
        > {

    /** Base iterative task compilation context / 基础迭代任务编译上下文 */
    val baseContext: IterativeTaskCompilationContext<Args, V, IT, T, E, A>

/**
 * Register extraction context into the meta model.
 * 将提取上下文注册到元模型中。
 * @param model Meta model to register extraction context into / 要注册提取上下文的元模型
 * @return Operation result / 操作结果
*/
    fun register(model: MetaModel<Flt64>): Try

/**
 * Add new task columns for the given iteration.
 * 为给定迭代添加新的任务列。
 * @param iteration Current iteration index / 当前迭代索引
 * @param newTasks New tasks to add as columns / 要添加为新列的任务列表
 * @param model Linear meta model to add columns into / 要添加列的线性元模型
 * @return Operation result / 操作结果
*/
    fun addColumns(
        iteration: UInt64,
        newTasks: List<T>,
        model: AbstractLinearMetaModel<Flt64>
    ): Try

/**
 * Extract shadow prices from the dual solution and refresh pipelines.
 * 从对偶解中提取影子价格并刷新管道。
 * @param shadowPriceMap Shadow price map to populate / 要填充的影子价格映射
 * @param model Linear meta model containing dual solution / 包含对偶解的线性元模型
 * @param shadowPrices Dual solution from the solver / 来自求解器的对偶解
 * @return Operation result / 操作结果
*/
    fun extractShadowPrice(
        shadowPriceMap: AbstractGanttSchedulingShadowPriceMap<Args, E, A>,
        model: AbstractLinearMetaModel<Flt64>,
        shadowPrices: MetaDualSolution
    ): Try

/**
 * Log the current iteration's solved result.
 * 记录当前迭代的求解结果。
 * @param iteration Current iteration index / 当前迭代索引
 * @param model Linear meta model containing solved tokens / 包含已求解令牌的线性元模型
 * @return Operation result / 操作结果
*/
    fun logResult(iteration: UInt64, model: AbstractLinearMetaModel<Flt64>): Try {
        return ok
    }
}
