/**
 * 懒加载委托工具
 *
 * Lazy delegate utilities for Kotlin property delegation.
 * 提供属性委托模式下的懒加载实现，支持同步和异步懒加载。
 *
 * Provides lazy loading implementations for Kotlin property delegation,
 * supporting both synchronous and asynchronous lazy initialization.
 */
package fuookami.ospf.kotlin.utils.meta_programming

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.atomic.AtomicReference
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1

/**
 * 创建懒加载委托
 *
 * Creates a lazy delegate for property delegation.
 * 创建一个懒加载委托，用于属性委托模式。
 *
 * @param T 宿主类型 / Host type
 * @param U 值类型 / Value type
 * @param lazyFunc 懒加载函数 / Lazy initialization function
 * @return 懒加载委托实例 / Lazy delegate instance
 */
fun <T, U : Any> lazyDelegate(lazyFunc: () -> U): LazyDelegate<T, U> {
    return LazyDelegate(lazyFunc)
}

/**
 * 创建自引用懒加载委托
 *
 * Creates a self-referencing lazy delegate for property delegation.
 * 创建一个自引用懒加载委托，允许懒加载函数访问宿主对象。
 *
 * @param T 宿主类型 / Host type
 * @param U 值类型 / Value type
 * @param lazyFunc 懒加载函数，接收宿主对象作为参数 / Lazy initialization function with host object parameter
 * @return 自引用懒加载委托实例 / Self-referencing lazy delegate instance
 */
@JvmName("selfLazyDelegate")
fun <T, U : Any> lazyDelegate(lazyFunc: (T) -> U): SelfLazyDelegate<T, U> {
    return SelfLazyDelegate(lazyFunc)
}

/**
 * 从属性引用创建自引用懒加载委托
 *
 * Creates a self-referencing lazy delegate from a property reference.
 * 从属性引用创建自引用懒加载委托，用于委托到其他属性。
 *
 * @param T 宿主类型 / Host type
 * @param U 值类型 / Value type
 * @param lazyKProperty 属性引用 / Property reference
 * @return 自引用懒加载委托实例 / Self-referencing lazy delegate instance
 */
fun <T, U : Any> lazyDelegate(lazyKProperty: KProperty1<T, U>): SelfLazyDelegate<T, U> {
    return SelfLazyDelegate { lazyKProperty(it) }
}

/**
 * 创建挂起懒加载
 *
 * Creates a suspend lazy initializer for coroutine-based lazy initialization.
 * 创建一个挂起懒加载初始化器，用于协程环境下的懒加载。
 *
 * @param T 值类型 / Value type
 * @param lazyFunc 挂起懒加载函数 / Suspend lazy initialization function
 * @return 挂起懒加载实例 / Suspend lazy instance
 */
fun <T> suspendLazy(lazyFunc: suspend () -> T): SuspendLazy<T> {
    return SuspendLazy(lazyFunc)
}

/**
 * 懒加载委托类
 *
 * Lazy delegate class for property delegation with deferred initialization.
 * 懒加载委托类，支持属性委托模式下的延迟初始化。
 *
 * 值在首次访问时通过懒加载函数初始化，之后缓存该值。
 * The value is initialized via the lazy function on first access and then cached.
 *
 * @param T 宿主类型 / Host type
 * @param U 值类型 / Value type
 * @property lazyFunc 懒加载函数 / Lazy initialization function
 */
class LazyDelegate<T, U : Any>(
    private val lazyFunc: () -> U
) {
    /**
     * 缓存的值
     *
     * The cached value.
     * 缓存的懒加载值，使用 lateinit 延迟初始化。
     */
    lateinit var range: U

    /**
     * 获取属性值（委托操作符）
     *
     * Gets the property value (delegation operator).
     * 获取属性值，首次访问时调用懒加载函数初始化。
     *
     * @param self 宿主对象 / Host object
     * @param property 属性元数据 / Property metadata
     * @return 属性值 / Property value
     */
    operator fun getValue(self: T, property: KProperty<*>): U {
        if (!::range.isInitialized) {
            range = lazyFunc()
        }
        return range
    }

    /**
     * 设置属性值（委托操作符）
     *
     * Sets the property value (delegation operator).
     * 设置属性值，首次设置时仍会调用懒加载函数初始化（确保值被正确初始化）。
     *
     * @param self 宿主对象 / Host object
     * @param property 属性元数据 / Property metadata
     * @param value 要设置的值 / Value to set
     */
    operator fun setValue(self: T, property: KProperty<*>, value: U) {
        if (!::range.isInitialized) {
            range = lazyFunc()
        }
        range = value
    }
}

/**
 * 自引用懒加载委托类
 *
 * Self-referencing lazy delegate class for property delegation with host object access.
 * 自引用懒加载委托类，允许懒加载函数访问宿主对象。
 *
 * 与 [LazyDelegate] 类似，但懒加载函数接收宿主对象作为参数，
 * Similar to [LazyDelegate], but the lazy function receives the host object as a parameter,
 * 允许基于宿主对象的状态进行初始化。
 * allowing initialization based on the host object's state.
 *
 * @param T 宿主类型 / Host type
 * @param U 值类型 / Value type
 * @property lazyFunc 懒加载函数，接收宿主对象 / Lazy initialization function with host object parameter
 */
class SelfLazyDelegate<T, U : Any>(
    private val lazyFunc: (T) -> U
) {
    /**
     * 缓存的值
     *
     * The cached value.
     * 缓存的懒加载值，使用 lateinit 延迟初始化。
     */
    lateinit var range: U

    /**
     * 获取属性值（委托操作符）
     *
     * Gets the property value (delegation operator).
     * 获取属性值，首次访问时调用懒加载函数初始化（传入宿主对象）。
     *
     * @param self 宿主对象 / Host object
     * @param property 属性元数据 / Property metadata
     * @return 属性值 / Property value
     */
    operator fun getValue(self: T, property: KProperty<*>): U {
        if (!::range.isInitialized) {
            range = lazyFunc(self)
        }
        return range
    }

    /**
     * 设置属性值（委托操作符）
     *
     * Sets the property value (delegation operator).
     * 设置属性值，首次设置时仍会调用懒加载函数初始化（传入宿主对象）。
     *
     * @param self 宿主对象 / Host object
     * @param property 属性元数据 / Property metadata
     * @param value 要设置的值 / Value to set
     */
    operator fun setValue(self: T, property: KProperty<*>, value: U) {
        if (!::range.isInitialized) {
            range = lazyFunc(self)
        }
        range = value
    }
}

/**
 * 挂起懒加载类
 *
 * Suspend lazy class for coroutine-based lazy initialization.
 * 挂起懒加载类，用于协程环境下的懒加载初始化。
 *
 * 使用原子引用和 Deferred 实现线程安全的懒加载，
 * Uses atomic reference and Deferred for thread-safe lazy initialization,
 * 确保在并发环境下只初始化一次。
 * ensuring single initialization in concurrent environments.
 *
 * @param T 值类型 / Value type
 * @property lazyFunc 挂起懒加载函数 / Suspend lazy initialization function
 */
class SuspendLazy<T>(
    private val lazyFunc: suspend () -> T,
) {
    /**
     * 原子引用的 Deferred 值
     *
     * Atomically referenced Deferred value.
     * 使用 AtomicReference 确保线程安全的懒加载。
     * Uses AtomicReference for thread-safe lazy initialization.
     */
    private val value = AtomicReference<Deferred<T>>()

    /**
     * 获取懒加载值（挂起操作符）
     *
     * Gets the lazy value (suspend operator).
     * 获取懒加载值，首次调用时启动协程异步初始化。
     *
     * 使用双重检查锁定模式确保并发安全：
     * Uses double-checked locking pattern for concurrency safety:
     * 1. 首先检查是否已有 Deferred / First checks if Deferred exists
     * 2. 若无，在协程作用域中创建新的 Deferred / If not, creates new Deferred in coroutine scope
     * 3. 使用 updateAndGet 确保原子性 / Uses updateAndGet for atomicity
     *
     * @return 懒加载值 / Lazy value
     */
    suspend operator fun invoke(): T = (
            value.get()
                ?: coroutineScope {
                    value.updateAndGet { actual ->
                        actual ?: async { lazyFunc() }
                    }
                }
            ).await()
}
