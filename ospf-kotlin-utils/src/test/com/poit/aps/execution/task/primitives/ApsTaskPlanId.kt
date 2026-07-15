package com.poit.aps.execution.task.primitives

/**
 * PoC2：aps TaskPlanId（value class），实现 ospf 同名接口（全限定符消歧）。
 *
 * 类 simple name = TaskPlanId，实现的接口 simple name = TaskPlanId（不同包）。
 * 验证 Kotlin 是否允许此类“同名跨包实现”。
 */
@JvmInline
value class TaskPlanId(val value: Long) : fuookami.ospf.kotlin.utils.concept.TaskPlanId {
    override fun toString(): String = value.toString()
}
