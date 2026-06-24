@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.crew.model

import kotlin.time.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*

/** 基于飞机和机场关系枚举中转时间场景。Enumerates the transit time scenarios based on aircraft and airport relationships. */
enum class TransitTimeScene {
    SameAircraft,
    DomainNotSameAircraft,
    InternationNotSameAircraft;

    companion object {
        /**
         * 判定给定连续航班任务的中转时间场景。Determines the transit time scene for the given consecutive flight tasks.
 *
         * @param prevTask 参数。
         * @param nextTask 参数。
         * @return 返回结果。
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
 * @property scene 参数。
 * @property duration 参数。
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
 * @param prevTask 前一个任务。
 * @param nextTask 后一个任务。
 * @return 中转时间，如果不存在则返回 null。
 */
operator fun TransitTimeMap.get(prevTask: FlightTask, nextTask: FlightTask): TransitTime? {
    return TransitTimeScene.invoke(prevTask, nextTask)?.let {
        this[it]
    }
}
