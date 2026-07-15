@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_selection

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_compilation.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_compilation.service.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_generation.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_generation.service.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.passenger.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.rule.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*

/**
 * 分支定价算法中批次选择操作的上下文。Context for bunch selection operations in the branch-and-price algorithm.
 *
 * 持有批次生成和编译上下文，提供两者共享的输入。
 *
 * @property aircrafts all aircraft / 所有飞机
 * @property recoveryNeededAircrafts aircraft that need recovery / 需要恢复的飞机
 * @property recoveryNeededFlightTasks flight tasks that need recovery / 需要恢复的航班任务
 * @property timeWindow time window / 时间窗口
 * @property flows flow control list / 流量控制列表
 * @property links link map / 链接映射
 * @property bunchGenerationContext bunch generation context / 批次生成上下文
 * @property bunchCompilationContext bunch compilation context / 批次编译上下文
 * @property parameter column generation coefficient parameters / 列生成系数参数
 * @property freeAircraftSelectorConfiguration free aircraft selector configuration / 自由飞机选择器配置
*/
class BunchSelectionContext(
    val aircrafts: List<Aircraft>,
    val recoveryNeededAircrafts: List<Aircraft>,
    val recoveryNeededFlightTasks: List<FlightTask>,
    val timeWindow: TimeWindow<*>,
    val flows: List<Flow>,
    val links: LinkMap,
    val bunchGenerationContext: BunchGenerationContext,
    val bunchCompilationContext: BunchCompilationContext,
    val parameter: Parameter = Parameter(),
    val freeAircraftSelectorConfiguration: FreeAircraftSelectorConfiguration = FreeAircraftSelectorConfiguration()
) {

    /**
     * 初始化批次编译聚合。Initializes the bunch compilation aggregation.
     *
     * @param originBunches original flight task bunches / 原始航班任务束
     * @return initialization result / 初始化结果
    */
    fun initCompilation(originBunches: List<FlightTaskBunch>): Try {
        val aggregation = fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_compilation.Aggregation(
            timeWindow = timeWindow,
            recoveryNeededAircrafts = recoveryNeededAircrafts,
            recoveryNeededFlightTasks = recoveryNeededFlightTasks,
            originBunches = originBunches,
            flows = flows,
            links = links
        )
        bunchCompilationContext.aggregation = aggregation
        return ok
    }
}
