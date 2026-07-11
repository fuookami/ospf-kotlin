/**
 * MongoDB 持久化插件异步作用域
 * MongoDB persistence plugin async scope
*/
package fuookami.ospf.kotlin.framework.persistence

import kotlinx.coroutines.*

/**
 * 框架插件异步协程作用域，用于 MongoDB 异步读写操作的后台任务调度
 * Framework plugin async coroutine scope for background task scheduling of MongoDB async read/write operations
*/
internal val frameworkPluginAsyncScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)