/**
 * 索引概念接口
 *
 * Indexed concept interface providing index management for types.
 * 为类型提供索引管理的索引概念接口。
 *
 * This file provides interfaces and classes for automatic index assignment
 * and management, useful for entity tracking and identification.
 * 此文件提供用于自动索引分配和管理的接口和类，
 * 适用于实体跟踪和标识。
 */
package fuookami.ospf.kotlin.utils.concept

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * 索引实现基类
 *
 * Sealed base class providing index generation functionality.
 * 提供索引生成功能的密封基类。
 *
 * This class manages index counters for different types using a thread-safe
 * ConcurrentHashMap with AtomicInteger counters.
 * 此类使用线程安全的 ConcurrentHashMap 和 AtomicInteger 计数器
 * 管理不同类型的索引计数器。
 */
sealed class IndexedImpl {
    /**
     * 获取下一个索引（内联函数，使用具体化类型参数）
     *
     * Gets the next index for the specified type using reified type parameter.
     * 使用具体化类型参数获取指定类型的下一个索引。
     *
     * @param T 要获取索引的类型 / The type to get the index for
     * @return 该类型的下一个索引 / The next index for the type
     */
    inline fun <reified T> nextIndex(): Int {
        return nextIndex(T::class)
    }

    /**
     * 获取下一个索引
     *
     * Gets the next index for the specified class.
     * 获取指定类的下一个索引。
     *
     * @param cls 要获取索引的类 / The class to get the index for
     * @return 该类的下一个索引 / The next index for the class
     */
    fun nextIndex(cls: KClass<*>): Int {
        return impls.getOrPut(cls) { AtomicInteger(0) }.getAndIncrement()
    }

    /**
     * 重置索引计数器（内联函数，使用具体化类型参数）
     *
     * Resets the index counter for the specified type using reified type parameter.
     * 使用具体化类型参数重置指定类型的索引计数器。
     *
     * @param T 要重置索引的类型 / The type to reset the index for
     */
    inline fun <reified T> flush() {
        flush(T::class)
    }

    /**
     * 重置索引计数器
     *
     * Resets the index counter for the specified class.
     * 重置指定类的索引计数器。
     *
     * @param cls 要重置索引的类 / The class to reset the index for
     */
    fun flush(cls: KClass<*>) {
        impls[cls] = AtomicInteger(0)
    }
}

/**
 * 索引存储映射
 *
 * Private map storing index counters for each class type.
 * 私有映射，存储每个类类型的索引计数器。
 */
private val impls = ConcurrentHashMap<KClass<*>, AtomicInteger>()

/**
 * 可索引接口
 *
 * Interface for types that have an index.
 * 具有索引的类型接口。
 *
 * Types implementing this interface have a unique integer index
 * that can be used for identification and lookup.
 * 实现此接口的类型具有唯一的整数索引，
 * 可用于标识和查找。
 */
interface Indexed {
    /**
     * 索引值
     *
     * The unique integer index for this instance.
     * 此实例的唯一整数索引。
     */
    val index: Int

    /**
     * 无符号长整型索引
     *
     * The index as an unsigned long value.
     * 索引的无符号长整型值。
     */
    val uindex: ULong get() = index.toULong()

    /**
     * 是否已设置索引
     *
     * Returns true if the index has been set.
     * 如果索引已设置则返回 true。
     */
    val indexed: Boolean get() = true
}

/**
 * 手动索引类
 *
 * Class for types where index assignment is manual.
 * 索引手动分配的类型类。
 *
 * Use this class when you need to control when the index is assigned.
 * 当需要控制索引分配时机时使用此类。
 *
 * @property _index 内部索引存储 / Internal index storage
 */
open class ManualIndexed internal constructor(
    private var _index: Int? = null
) : Indexed {
    /**
     * 是否已设置索引
     *
     * Returns true if the index has been set.
     * 如果索引已设置则返回 true。
     */
    override val indexed: Boolean get() = _index != null

    /**
     * 索引值
     *
     * The unique integer index for this instance.
     * 此实例的唯一整数索引。
     *
     * @throws AssertionError 如果索引未设置 / If the index has not been set
     */
    override val index: Int
        get() {
            assert(indexed)
            return _index!!
        }

    /**
     * 创建未设置索引的实例
     *
     * Creates an instance without an index set.
     * 创建未设置索引的实例。
     */
    constructor() : this(null)

    companion object : IndexedImpl()

    /**
     * 设置索引
     *
     * Sets the index using the instance's class type.
     * 使用实例的类类型设置索引。
     */
    fun setIndexed() {
        assert(!indexed)
        _index = nextIndex(this::class)
    }

    /**
     * 设置索引（指定类类型）
     *
     * Sets the index using the specified class type.
     * 使用指定的类类型设置索引。
     *
     * @param cls 要使用的类类型 / The class type to use
     */
    fun setIndexed(cls: KClass<*>) {
        assert(!indexed)
        _index = nextIndex(cls)
    }

    /**
     * 刷新索引
     *
     * Refreshes the index with a new value using the instance's class type.
     * 使用实例的类类型刷新索引为新值。
     */
    fun refreshIndex() {
        assert(indexed)
        _index = nextIndex(this::class)
    }

    /**
     * 刷新索引（指定类类型）
     *
     * Refreshes the index with a new value using the specified class type.
     * 使用指定的类类型刷新索引为新值。
     *
     * @param cls 要使用的类类型 / The class type to use
     */
    fun refreshIndex(cls: KClass<*>) {
        assert(indexed)
        _index = nextIndex(cls)
    }
}

/**
 * 自动索引类
 *
 * Class for types where index assignment is automatic upon creation.
 * 索引在创建时自动分配的类型类。
 *
 * Use this class when you want the index to be automatically assigned
 * when an instance is created.
 * 当希望实例创建时自动分配索引时使用此类。
 *
 * @property mIndex 内部索引存储 / Internal index storage
 */
open class AutoIndexed internal constructor(
    private var mIndex: Int
) : Indexed {
    /**
     * 索引值
     *
     * The unique integer index for this instance.
     * 此实例的唯一整数索引。
     */
    override val index: Int by ::mIndex

    /**
     * 创建带有指定类类型索引的实例
     *
     * Creates an instance with an index from the specified class type.
     * 创建带有指定类类型索引的实例。
     *
     * @param cls 要使用的类类型 / The class type to use
     */
    constructor(cls: KClass<*>) : this(nextIndex(cls))

    companion object : IndexedImpl()

    /**
     * 刷新索引
     *
     * Refreshes the index with a new value using the instance's class type.
     * 使用实例的类类型刷新索引为新值。
     */
    fun refreshIndex() {
        mIndex = nextIndex(this::class)
    }

    /**
     * 刷新索引（指定类类型）
     *
     * Refreshes the index with a new value using the specified class type.
     * 使用指定的类类型刷新索引为新值。
     *
     * @param cls 要使用的类类型 / The class type to use
     */
    fun refreshIndex(cls: KClass<*>) {
        mIndex = nextIndex(cls)
    }
}

/**
 * 在列表中查找或获取指定索引的元素
 *
 * Finds an element by index in the list, or gets the element at that position if not found.
 * 在列表中按索引查找元素，如果未找到则获取该位置的元素。
 *
 * This function first attempts to find an element with the matching index property.
 * If not found, it returns the element at the given index position.
 * 此函数首先尝试查找具有匹配 index 属性的元素。
 * 如果未找到，则返回给定索引位置的元素。
 *
 * @param T 可索引类型 / The indexed type
 * @param index 要查找的索引 / The index to find
 * @return 找到的元素或指定位置的元素 / The found element or the element at the given position
 */
fun <T : Indexed> List<T>.findOrGet(index: Int): T {
    return find { it.index == index } ?: get(index)
}
