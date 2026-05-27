/**
 * 符号标识
 * Symbol Identity
 *
 * 提供符号的唯一标识机制，用于在符号计算过程中区分不同的符号实例。
 * 即使两个符号具有相同的名称，它们也可以通过唯一标识进行区分。
 * Provides unique identification mechanism for symbols,
 * used to distinguish different symbol instances during symbolic computation.
 * Even if two symbols have the same name, they can be distinguished by unique identifiers.
 */
package fuookami.ospf.kotlin.math.symbol

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

/**
 * 符号标识值类
 * Symbol Identifier Value Class
 *
 * 封装符号的唯一标识字符串，提供类型安全的标识符。
 * Wraps the unique identifier string of a symbol, providing type-safe identification.
 *
 * @property value 符号标识的字符串倌/ String value of the symbol identifier
 */
@JvmInline
value class SymbolId(val value: String) {
    /**
     * 返回符号标识的字符串表示。
     * Returns the string representation of the symbol identifier.
     *
     * @return 符号标识的字符串倌/ String value of the symbol identifier
     */
    override fun toString(): String = value
}

/**
 * 稳定符号接口
 * Stable Symbol Interface
 *
 * 提供稳定标识的符号接口，实现此接口的符号具有在生命周期内不变的标识符。
 * Symbol interface providing stable identification. Symbols implementing this
 * interface have identifiers that remain constant throughout their lifecycle.
 *
 * @property stableSymbolId 符号的稳定标识符 / Stable identifier of the symbol
 */
interface StableSymbol : Symbol {
    val stableSymbolId: SymbolId
}

/**
 * 可标识符号接双
 * Identifiable Symbol Interface
 *
 * 提供符号标识能力的接口，实现此接口的符号具有唯一标识符。
 * Interface providing symbol identification capability.
 * Symbols implementing this interface have unique identifiers.
 *
 * @property symbolId 符号的唯一标识字符丌/ Unique identifier string of the symbol
 */
interface IdentifiedSymbol : StableSymbol {
    val symbolId: String

    override val stableSymbolId: SymbolId
        get() = SymbolId(symbolId)
}

/**
 * 拥有者符号基类接双
 * Owned Symbol Base Interface
 *
 * 继承臌[Symbol] 接口，为符号添加唯一标识符属性。
 * Extends the [Symbol] interface, adding unique identifier property to symbols.
 *
 * @property id 符号的唯一标识笌/ Unique identifier of the symbol
 */
interface OwnedSymbolLike : StableSymbol {
    val id: SymbolId

    override val stableSymbolId: SymbolId
        get() = id
}

/**
 * 拥有者符号数据类
 * Owned Symbol Data Class
 *
 * 具有唯一标识符的符号实现，用于在符号计算中区分相同名称的不同符号实例。
 * Symbol implementation with unique identifier,
 * used to distinguish different symbol instances with the same name in symbolic computation.
 *
 * @property id 符号的唯一标识笌/ Unique identifier of the symbol
 * @property name 符号的名秌/ Name of the symbol
 * @property displayName 符号的显示名称（可选），用于输出和展示 / Display name (optional) for output and visualization
 */
data class OwnedSymbol(
    override val id: SymbolId,
    override val name: String,
    override val displayName: String? = null
) : OwnedSymbolLike {
    /**
     * 从现有符号创建拥有者符号。
     * Creates an owned symbol from an existing symbol.
     *
     * @param symbol 源符双/ Source symbol
     * @param id 符号标识符，默认使用符号的稳定标诌/ Symbol identifier, defaults to the symbol's stable ID
     */
    constructor(symbol: Symbol, id: SymbolId = symbol.stableId()) : this(
        id = id,
        name = symbol.name,
        displayName = symbol.displayName
    )
}

/**
 * 获取符号的稳定标识符。
 * Gets the stable identifier of a symbol.
 *
 * 根据符号类型返回其稳定标识：
 * - [OwnedSymbolLike] 直接返回兌id
 * - [IdentifiedSymbol] 使用 symbolId 创建标识笌
 * - 其他类型使用名称和对象哈希码组合
 *
 * Returns the stable identifier based on symbol type:
 * - [OwnedSymbolLike] returns its id directly
 * - [IdentifiedSymbol] creates identifier from symbolId
 * - Other types use name and object hash code combination
 *
 * @return 符号的稳定标识符 / Stable identifier of the symbol
 */
fun Symbol.stableIdOrNull(): SymbolId? {
    return (this as? StableSymbol)?.stableSymbolId
}

/**
 * 检查符号是否具有稳定标识。
 * Checks whether the symbol has a stable identifier.
 *
 * @return 如果符号具有稳定标识则返回true / True if the symbol has a stable identifier
 */
fun Symbol.hasStableId(): Boolean {
    return stableIdOrNull() != null
}

/**
 * 获取符号的稳定标识符，如果没有则抛出异常。
 * Gets the stable identifier of the symbol, throws if absent.
 *
 * @return 符号的稳定标识符 / Stable identifier of the symbol
 * @throws IllegalStateException 如果符号没有显式的稳定标识 / If the symbol has no explicit stable identity
 */
fun Symbol.requireStableId(): SymbolId {
    return stableIdOrNull()
        ?: throw IllegalStateException("Symbol $name has no explicit stable identity.")
}

/**
 * 获取或生成符号的稳定标识符。
 * Gets or generates the stable identifier of the symbol.
 *
 * 如果符号实现了 [StableSymbol]，返回其稳定标识；否则使用名称和对象哈希码生成。
 * If the symbol implements [StableSymbol], returns its stable ID; otherwise generates one
 * from the name and object hash code.
 *
 * @return 符号的稳定标识符 / Stable identifier of the symbol
 */
fun Symbol.stableId(): SymbolId {
    return stableIdOrNull() ?: SymbolId("${name}#${System.identityHashCode(this)}")
}

/**
 * 创建符号的拥有者副本。
 * Creates an owned copy of the symbol.
 *
 * @param id 符号标识符，默认使用符号的稳定标诌/ Symbol identifier, defaults to the symbol's stable ID
 * @return 拥有者符号实侌/ Owned symbol instance
 */
fun Symbol.owned(id: SymbolId = stableId()): OwnedSymbol {
    return OwnedSymbol(this, id)
}

/**
 * 获取符号的身份字符串。
 * Gets the identity string of a symbol.
 *
 * @return 符号的唯一身份字符丌/ Unique identity string of the symbol
 */
fun Symbol.identity(): String {
    return stableId().value
}

/**
 * 默认符号比较噌
 * Default Symbol Comparator
 *
 * 用于符号排序的比较器，首先按名称比较，名称相同则按身份标识比较。
 * Comparator for sorting symbols, first by name, then by identity if names are equal.
 */
val defaultSymbolComparator: Comparator<Symbol> = Comparator { lhs, rhs ->
    val byName = lhs.name.compareTo(rhs.name)
    if (byName != 0) {
        byName
    } else {
        lhs.identity().compareTo(rhs.identity())
    }
}

/**
 * 默认稳定符号比较器
 * Default Stable Symbol Comparator
 *
 * 用于符号排序的比较器，首先按名称比较，名称相同则按稳定标识比较。
 * 要求符号具有显式的稳定标识，否则抛出异常。
 * Comparator for sorting symbols, first by name, then by stable identifier if names are equal.
 * Requires symbols to have explicit stable identifiers, throws otherwise.
 */
val defaultStableSymbolComparator: Comparator<Symbol> = Comparator { lhs, rhs ->
    val byName = lhs.name.compareTo(rhs.name)
    if (byName != 0) {
        byName
    } else {
        lhs.requireStableId().value.compareTo(rhs.requireStableId().value)
    }
}
