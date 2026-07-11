@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.crew.model

import kotlin.time.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*

/** 基于飞机和机场关系枚举中转时间场景。Enumerates the transit time scenarios based on aircraft and airport relationships. */
enum class TransitTimeScene {
    /** Same aircraft for consecutive flights / 连续航班使用同一飞机 */
    SameAircraft,
    /** Domestic airport with different aircraft / 国内机场且飞机不同 */
    DomainNotSameAircraft,
    /** International airport with different aircraft / 国际机场且飞机不同 */
    InternationNotSameAircraft;

    companion object {
        /**
         * 判定给定连续航班任务的中转时间场景。Determines the transit time scene for the given consecutive flight tasks.
         *
         * @param prevTask Previous flight task / 前一个航班任务
         * @param nextTask Next flight task / 后一个航班任务
         * @return The transit time scene, or null if not applicable / 中转时间场景，不适用则返回 null
        */
        operator fun invoke(prevTask: FlightTask, nextTask: FlightTask): TransitTimeScene? {
            return if (prevTask.aircraft == nextTask.aircraft) {
                SameAircraft
            } else if (prevTask.arr == nextTask.dep) {
                when (prevTask.arr.type) {
                    AirportType.Domestic -> DomainNotSameAircraft

                    else -> InternationNotSameAircraft
                }
            } else {
                null
            }
        }
    }
}

/**
 * 将场景与其所需时长关联的中转时间条目。A transit time entry associating a scene with its required duration.
 *
 * @property scene Transit time scene / 中转时间场景
 * @property duration Required transit duration / 所需中转时长
*/
data class TransitTime(
    val scene: TransitTimeScene,
    val duration: Duration
)

/** 中转时间场景到其中转时间条目映射的类型别名。Type alias for a map of transit time scenes to their transit time entries. */
typealias TransitTimeMap = Map<TransitTimeScene, TransitTime>

/**
 * 查找给定连续航班任务的中转时间。
 * Looks up the transit time for the given consecutive flight tasks.
 *
 * @param prevTask Previous flight task / 前一个航班任务
 * @param nextTask Next flight task / 后一个航班任务
 * @return The transit time entry, or null if not found / 中转时间条目，未找到则返回 null
*/
operator fun TransitTimeMap.get(prevTask: FlightTask, nextTask: FlightTask): TransitTime? {
    return TransitTimeScene.invoke(prevTask, nextTask)?.let {
        this[it]
    }
}
