/**
 * 路径符号
 * Path Symbol
 *
 * 实现 `Symbol` 和`IdentifiedSymbol` 接口，用于将 `PropertyPath` 桥接到符号系统。
 * Implements `Symbol` and `IdentifiedSymbol` interfaces, bridging `PropertyPath` to the symbol system.
 *
 * 核心特怌/ Core Features:
 * - `name = path.value` / Symbol name equals path value
 * - `symbolId = "path:${path.value}"` / Unique identifier format
 * - 支持丌`PropertyPath` 双向转换 / Supports bidirectional conversion with `PropertyPath`
 */
package fuookami.ospf.kotlin.math.symbol.expression

import fuookami.ospf.kotlin.math.symbol.*

/**
 * 路径符号
 * Path Symbol
 *
 * 将属性路径包装为符号，用于在表达式系统中引用字段或属性。
 * Wraps a property path as a symbol for referencing fields or properties in the expression system.
 *
 * @property path 属性路後/ Property path
 */
data class PathSymbol(
    val path: PropertyPath
) : Symbol, IdentifiedSymbol {

    /**
     * 符号名称，等于路径倌
     * Symbol name, equals path value
     */
    override val name: String = path.value

    /**
     * 显示名称，默认为路径倌
     * Display name, defaults to path value
     */
    override val displayName: String? = path.value

    /**
     * 符号唯一标识，格式为 `path:${path.value}`
     * Symbol unique identifier, format is `path:${path.value}`
     */
    override val symbolId: String = "path:${path.value}"

    override fun toString(): String = "PathSymbol($path)"

    companion object {
        /**
         * 从属性路径创建路径符双
         * Create path symbol from property path
         *
         * @param path 属性路後/ Property path
         * @return 路径符号 / Path symbol
         */
        fun from(path: PropertyPath): PathSymbol = PathSymbol(path)

        /**
         * 从字符串创建路径符号
         * Create path symbol from string
         *
         * @param path 路径字符丌/ Path string
         * @return 路径符号 / Path symbol
         */
        fun from(path: String): PathSymbol = PathSymbol(PropertyPath.parse(path))

        /**
         * 从分段创建路径符双
         * Create path symbol from segments
         *
         * @param segments 路径分段 / Path segments
         * @return 路径符号 / Path symbol
         */
        fun of(vararg segments: String): PathSymbol = PathSymbol(PropertyPath.of(*segments))
    }
}

/**
 * 扩展函数：属性路径转路径符号
 * Extension function: PropertyPath to PathSymbol
 */
fun PropertyPath.toPathSymbol(): PathSymbol = PathSymbol.from(this)

/**
 * 扩展函数：字符串转路径符双
 * Extension function: String to PathSymbol
 */
fun String.toPathSymbol(): PathSymbol = PathSymbol.from(this)

/**
 * 扩展函数：符号尝试转属性路後
 * Extension function: Symbol to PropertyPathOrNull
 *
 * 仅对 `PathSymbol` 类型有效，其他类型返囌null。
 * Only works for `PathSymbol` type, returns null for other types.
 */
fun Symbol.toPropertyPathOrNull(): PropertyPath? {
    return when (this) {
        is PathSymbol -> path
        else -> null
    }
}

/**
 * 扩展函数：符号是否是路径符号
 * Extension function: Symbol is PathSymbol
 */
fun Symbol.isPathSymbol(): Boolean = this is PathSymbol

/**
 * 扩展函数：识别符号尝试转属性路後
 * Extension function: IdentifiedSymbol to PropertyPathOrNull
 *
 * 基于 symbolId 格式 `path:${value}` 解析。
 * Parses based on symbolId format `path:${value}`.
 */
fun IdentifiedSymbol.toPropertyPathFromIdOrNull(): PropertyPath? {
    val id = symbolId
    if (!id.startsWith("path:")) return null
    val value = id.removePrefix("path:")
    return PropertyPath.parseOrNull(value)
}
