/**
 * 属性路後
 * Property Path
 *
 * 统一的路径抽象，用于字段/属性引用。路径以点分隔的形式表示，如 `a.b.c`。
 * Provides unified path abstraction for field/property references.
 * Paths are represented in dot-separated form, e.g., `a.b.c`.
 *
 * 核心特怌/ Core Features:
 * - 统一存储丌`value: String` / Unified storage as `value: String`
 * - 支持分段解析 `segments` / Supports segment parsing `segments`
 * - 工厂方法 `of("a", "b", "c")` / Factory method `of("a", "b", "c")`
 * - 文本解析支持 `a.b.c` / Text parsing supports `a.b.c`
 */
package fuookami.ospf.kotlin.math.symbol.expression

/**
 * 属性路後
 * Property Path
 *
 * 表示字段或属性的引用路径，如 `user.address.city`。
 * Represents a reference path for fields or properties, e.g., `user.address.city`.
 *
 * @property value 路径的字符串表示 / String representation of the path
 */
@JvmInline
value class PropertyPath(val value: String) {
    /**
     * 获取路径分段
     * Get path segments
     *
     * 将路径按点分隔符拆分为多个分段。
     * Splits the path by dot separator into segments.
     *
     * @return 路径分段列表 / List of path segments
     */
    val segments: List<String>
        get() = if (value.isEmpty()) emptyList() else value.split('.')

    /**
     * 路径是否为空
     * Whether path is empty
     */
    val isEmpty: Boolean
        get() = value.isEmpty()

    /**
     * 路径是否非空
     * Whether path is non-empty
     */
    val isNotEmpty: Boolean
        get() = value.isNotEmpty()

    /**
     * 获取分段数量
     * Get number of segments
     */
    val depth: Int
        get() = segments.size

    /**
     * 获取根分段（第一个分段）
     * Get root segment (first segment)
     *
     * @return 根分段，如果路径为空则返囌null / Root segment, null if path is empty
     */
    val root: String?
        get() = segments.firstOrNull()

    /**
     * 获取叶分段（最后一个分段）
     * Get leaf segment (last segment)
     *
     * @return 叶分段，如果路径为空则返囌null / Leaf segment, null if path is empty
     */
    val leaf: String?
        get() = segments.lastOrNull()

    /**
     * 获取父路後
     * Get parent path
     *
     * 返回去掉最后一个分段后的路径。
     * Returns the path without the last segment.
     *
     * @return 父路径，如果只有一个分段则返回 null / Parent path, null if only one segment
     */
    val parent: PropertyPath?
        get() {
            val segs = segments
            return if (segs.size <= 1) null else PropertyPath(segs.dropLast(1).joinToString("."))
        }

    /**
     * 获取子路後
     * Get child path
     *
     * 返回去掉第一个分段后的路径。
     * Returns the path without the first segment.
     *
     * @return 子路径，如果只有一个分段则返回 null / Child path, null if only one segment
     */
    val child: PropertyPath?
        get() {
            val segs = segments
            return if (segs.size <= 1) null else PropertyPath(segs.drop(1).joinToString("."))
        }

    /**
     * 判断是否是另一个路径的子路後
     * Check if this is a sub-path of another path
     *
     * @param other 父路後/ Parent path
     * @return 是否是子路径 / Whether this is a sub-path
     */
    fun isSubPathOf(other: PropertyPath): Boolean {
        if (isEmpty || other.isEmpty) return false
        val otherSegs = other.segments
        val thisSegs = segments
        if (thisSegs.size <= otherSegs.size) return false
        return thisSegs.take(otherSegs.size) == otherSegs
    }

    /**
     * 判断是否是另一个路径的父路後
     * Check if this is a parent path of another path
     *
     * @param other 子路後/ Child path
     * @return 是否是父路径 / Whether this is a parent path
     */
    fun isParentPathOf(other: PropertyPath): Boolean {
        return other.isSubPathOf(this)
    }

    /**
     * 拼接路径
     * Concatenate paths
     *
     * @param other 要拼接的路径 / Path to concatenate
     * @return 拼接后的新路後/ New concatenated path
     */
    fun concat(other: PropertyPath): PropertyPath {
        return if (isEmpty) other
        else if (other.isEmpty) this
        else PropertyPath("$value.${other.value}")
    }

    /**
     * 拼接分段
     * Concatenate segment
     *
     * @param segment 要追加的分段 / Segment to append
     * @return 拼接后的新路後/ New concatenated path
     */
    fun concat(segment: String): PropertyPath {
        return if (isEmpty) PropertyPath(segment)
        else PropertyPath("$value.$segment")
    }

    override fun toString(): String = value

    companion object {
        /**
         * 空路後
         * Empty path
         */
        val empty = PropertyPath("")

        /**
         * 从分段创建路後
         * Create path from segments
         *
         * @param segments 路径分段 / Path segments
         * @return 创建的路後/ Created path
         */
        fun of(segments: List<String>): PropertyPath {
            return PropertyPath(segments.joinToString("."))
        }

        /**
         * 从可变参数创建路後
         * Create path from vararg segments
         *
         * @param segments 路径分段 / Path segments
         * @return 创建的路後/ Created path
         */
        fun of(vararg segments: String): PropertyPath {
            return of(segments.toList())
        }

        /**
         * 从字符串解析路径
         * Parse path from string
         *
         * 支持点分隔路後`a.b.c` 和单个标识符 `a`。
         * Supports dot-separated paths `a.b.c` and single identifiers `a`.
         *
         * @param text 路径文本 / Path text
         * @return 解析的路後/ Parsed path
         */
        fun parse(text: String): PropertyPath {
            return PropertyPath(text.trim())
        }

        /**
         * 尝试从字符串解析路径
         * Try to parse path from string
         *
         * @param text 路径文本 / Path text
         * @return 解析的路径，如果文本无效则返囌null / Parsed path, null if text is invalid
         */
        fun parseOrNull(text: String): PropertyPath? {
            val trimmed = text.trim()
            if (trimmed.isEmpty()) return null
            // 验证每个分段是有效的标识笌
            // Validate each segment is a valid identifier
            val segments = trimmed.split('.')
            return if (segments.all { isValidIdentifier(it) }) {
                PropertyPath(trimmed)
            } else null
        }

        /**
         * 验证标识符是否有敌
         * Validate if identifier is valid
         *
         * 有效标识符：非空，以字母或下划线开头，只包含字母、数字、下划线。
         * Valid identifier: non-empty, starts with letter or underscore,
         * contains only letters, digits, underscores.
         *
         * @param id 标识笌/ Identifier
         * @return 是否有效 / Whether valid
         */
        private fun isValidIdentifier(id: String): Boolean {
            if (id.isEmpty()) return false
            val first = id.first()
            if (!first.isLetter() && first != '_') return false
            return id.all { it.isLetterOrDigit() || it == '_' }
        }
    }
}

/**
 * 扩展函数：字符串转属性路後
 * Extension function: String to PropertyPath
 */
fun String.toPropertyPath(): PropertyPath = PropertyPath.parse(this)

/**
 * 扩展函数：字符串尝试转属性路後
 * Extension function: String to PropertyPathOrNull
 */
fun String.toPropertyPathOrNull(): PropertyPath? = PropertyPath.parseOrNull(this)