/**
 * Kafka 消息插件异步作用域
 * Kafka message plugin async scope
 */
package fuookami.ospf.kotlin.framework.message

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * 框架插件异步协程作用域，用于 Kafka 消息消费的后台任务调度
 * Framework plugin async coroutine scope for background task scheduling of Kafka message consumption
 */
internal val frameworkPluginAsyncScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)