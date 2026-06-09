
@file:OptIn(kotlin.time.ExperimentalTime::class)

/**
 * 执行者及其初始可用性 / Executor and its initial usability
 */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model

import fuookami.ospf.kotlin.utils.concept.ManualIndexed
import kotlin.time.Instant

/**
 * 执行者初始可用性，记录上一个任务和可用时间 / Executor initial usability recording the last task and enabled time
 *
 * @param T 任务类型 / The task type
 * @param E 执行者类型 / The executor type
 * @param A 分配策略类型 / The assignment policy type
 * @property lastTask 上一个任务 / The last task
 * @property enabledTime 可用时间 / The enabled time
 */
open class ExecutorInitialUsability<
        out T : AbstractTask<E, A>,
        out E : Executor,
        out A : AssignmentPolicy<E>
        >(
    open val lastTask: T?,
    val enabledTime: Instant
) {
    /** 是否处于可用状态 / Whether the executor is on */
    open val on: Boolean get() = lastTask != null
}

/**
 * 执行者，表示可执行任务的实体 / Executor representing an entity that can execute tasks
 *
 * @property id 执行者ID / The executor ID
 * @property name 执行者名称 / The executor name
 */
open class Executor(
    val id: String,
    val name: String
) : ManualIndexed() {
    /** 实际ID / The actual ID */
    open val actualId: String by ::id
    /** 显示名称 / The display name */
    open val displayName: String by ::name
}