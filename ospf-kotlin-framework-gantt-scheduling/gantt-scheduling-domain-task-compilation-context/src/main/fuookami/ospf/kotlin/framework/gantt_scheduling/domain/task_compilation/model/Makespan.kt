/** 最大完工时间 / Makespan */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.symbol.function.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.value.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*

/** 最大完工时间物理量 / Makespan quantity */
typealias MakespanQuantity<V> = Quantity<V>

/**
 * 最大完工时间 / Makespan
 *
 * @param T 任务类型 / Task type
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
 * @param tasks 任务列表 / List of tasks
 * @param taskTime 任务时间对象 / Task time object
 * @param extra 是否使用额外的最小最大函数 / Whether to use extra min-max function
*/
class Makespan<
        out T : AbstractTask<E, A>,
        out E : Executor,
        out A : AssignmentPolicy<E>
        >(
    private val tasks: List<T>,
    private val taskTime: TaskTime,
    private val extra: Boolean = false
) {
    lateinit var makespan: LinearIntermediateSymbol<Flt64>

    /**
     * 注册最大完工时间到模型 / Register makespan to model
     *
     * @param model 元模型 / Meta model
     * @return 操作结果 / Operation result
    */
    fun register(model: MetaModel<Flt64>): Try {
        if (!::makespan.isInitialized) {
            makespan = if (extra) {
                MinMaxFunction.fromSymbols(
                    tasks.map {
                        taskTime.estimateEndTime[it]
                    },
                    converter = IntoValue.Identity,
                    name = "makespan"
                )
            } else {
                MaxFunction.fromSymbols(
                    tasks.map {
                        taskTime.estimateEndTime[it]
                    },
                    converter = IntoValue.Identity,
                    name = "makespan"
                )
            }
        }
        when (val result = model.add(makespan)) {
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
     * 读取最大完工时间物理量 / Read makespan as a physical quantity
     *
     * @param V 目标数值类型 / Target numeric type
     * @param model 元模型 / Meta model
     * @param adapter solver 数值适配器 / Solver value adapter
     * @param unit 时间单位 / Time unit
     * @return 最大完工时间物理量 / Makespan quantity
    */
    fun <V : RealNumber<V>> quantity(
        model: MetaModel<Flt64>,
        adapter: SchedulingSolverValueAdapter<V>,
        unit: PhysicalUnit = NoneUnit
    ): MakespanQuantity<V>? {
        val value = (makespan as IntermediateSymbol<Flt64>).evaluate(
            tokenTable = model.tokens,
            converter = schedulingSolverValueAdapter,
            zeroIfNone = true
        ) ?: makespan.toLinearPolynomial().constant
        return Quantity(adapter.intoValue(value), unit)
    }
}
