/**
 * 模式匹配方言策略
 * Pattern Match Dialect Policy
 *
 * 处理不同数据库对 LIKE/ILIKE/REGEX 的支持差异。
 * Handles differences in LIKE/ILIKE/REGEX support across databases.
 */
package fuookami.ospf.kotlin.framework.persistence.expression.translator

import org.ktorm.dsl.*
import org.ktorm.schema.ColumnDeclaring

/**
 * 模式匹配策略
 * Pattern Match Policy
 *
 * 定义如何将 PatternMatch 表达式翻译为数据库特定的语法。
 * Defines how to translate PatternMatch expressions to database-specific syntax.
 */
interface PatternMatchPolicy {
    /**
     * 翻译 LIKE 模式匹配
     * Translate LIKE pattern match
     *
     * @param column 列表达式 / Column expression
     * @param pattern 匹配模式 / Match pattern (SQL LIKE pattern with % and _)
     * @param caseSensitive 是否区分大小写 / Whether case-sensitive
     * @return 翻译后的条件表达式 / Translated condition expression
     */
    fun translateLike(
        column: ColumnDeclaring<*>,
        pattern: String,
        caseSensitive: Boolean
    ): ColumnDeclaring<Boolean>

    /**
     * 翻译正则匹配
     * Translate regex match
     *
     * @param column 列表达式 / Column expression
     * @param pattern 正则表达式模式 / Regex pattern
     * @return 翻译后的条件表达式，不支持时返回 null / Translated condition, null if not supported
     */
    fun translateRegex(
        column: ColumnDeclaring<*>,
        pattern: String
    ): ColumnDeclaring<Boolean>? {
        // 默认不支持正则
        // Default: regex not supported
        return null
    }
}

/**
 * 默认模式匹配策略（标准 SQL）
 * Default Pattern Match Policy (Standard SQL)
 */
object DefaultPatternMatchPolicy : PatternMatchPolicy {
    override fun translateLike(
        column: ColumnDeclaring<*>,
        pattern: String,
        caseSensitive: Boolean
    ): ColumnDeclaring<Boolean> {
        return column.like(pattern)
    }
}

/**
 * SQLite 模式匹配策略
 * SQLite Pattern Match Policy
 */
object SqlitePatternMatchPolicy : PatternMatchPolicy {
    override fun translateLike(
        column: ColumnDeclaring<*>,
        pattern: String,
        caseSensitive: Boolean
    ): ColumnDeclaring<Boolean> {
        return column.like(pattern)
    }
}

/**
 * PostgreSQL 模式匹配策略
 * PostgreSQL Pattern Match Policy
 */
object PostgresPatternMatchPolicy : PatternMatchPolicy {
    override fun translateLike(
        column: ColumnDeclaring<*>,
        pattern: String,
        caseSensitive: Boolean
    ): ColumnDeclaring<Boolean> {
        return column.like(pattern)
    }
}

/**
 * MySQL 模式匹配策略
 * MySQL Pattern Match Policy
 */
object MySqlPatternMatchPolicy : PatternMatchPolicy {
    override fun translateLike(
        column: ColumnDeclaring<*>,
        pattern: String,
        caseSensitive: Boolean
    ): ColumnDeclaring<Boolean> {
        return column.like(pattern)
    }
}