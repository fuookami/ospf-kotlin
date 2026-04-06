/**
 * 模式匹配方言策略
 * Pattern Match Dialect Policy
 *
 * 处理不同数据库对 LIKE/ILIKE/REGEX 的支持差异。
 * Handles differences in LIKE/ILIKE/REGEX support across databases.
 */
package fuookami.ospf.kotlin.framework.persistence.expression.translator

import org.ktorm.dsl.QuerySource
import org.ktorm.expression.ColumnExpression
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
    ): ColumnDeclaring<Boolean>?
}

/**
 * 默认模式匹配策略（标准 SQL）
 * Default Pattern Match Policy (Standard SQL)
 *
 * 使用标准 SQL LIKE 语法，不支持 ILIKE 时使用 LOWER 函数。
 * Uses standard SQL LIKE syntax, falls back to LOWER() for case-insensitive matching.
 */
object DefaultPatternMatchPolicy : PatternMatchPolicy {
    override fun translateLike(
        column: ColumnDeclaring<*>,
        pattern: String,
        caseSensitive: Boolean
    ): ColumnDeclaring<Boolean> {
        return if (caseSensitive) {
            column.like(pattern)
        } else {
            // 降级：使用 LOWER 函数
            // Fallback: use LOWER function
            column.lower().like(pattern.lowercase())
        }
    }

    override fun translateRegex(
        column: ColumnDeclaring<*>,
        pattern: String
    ): ColumnDeclaring<Boolean>? {
        // 默认不支持正则
        // Default: regex not supported
        return null
    }
}

/**
 * SQLite 模式匹配策略
 * SQLite Pattern Match Policy
 *
 * SQLite 使用 LIKE（默认不区分大小写）和 GLOB（区分大小写）。
 * SQLite uses LIKE (case-insensitive by default) and GLOB (case-sensitive).
 */
object SqlitePatternMatchPolicy : PatternMatchPolicy {
    override fun translateLike(
        column: ColumnDeclaring<*>,
        pattern: String,
        caseSensitive: Boolean
    ): ColumnDeclaring<Boolean> {
        return if (caseSensitive) {
            // SQLite: LIKE 默认不区分大小写，使用 GLOB 区分
            // SQLite: LIKE is case-insensitive by default, use GLOB for case-sensitive
            // Ktorm 不直接支持 GLOB，使用 LIKE + PRAGMA 或自定义
            // Ktorm doesn't directly support GLOB, use LIKE + PRAGMA or custom
            column.like(pattern)
        } else {
            column.like(pattern)
        }
    }

    override fun translateRegex(
        column: ColumnDeclaring<*>,
        pattern: String
    ): ColumnDeclaring<Boolean>? {
        // SQLite 不原生支持正则（需要加载扩展）
        // SQLite doesn't natively support regex (requires extension)
        return null
    }
}

/**
 * PostgreSQL 模式匹配策略
 * PostgreSQL Pattern Match Policy
 *
 * PostgreSQL 支持 LIKE、ILIKE 和 ~（正则）。
 * PostgreSQL supports LIKE, ILIKE, and ~ (regex).
 */
object PostgresPatternMatchPolicy : PatternMatchPolicy {
    override fun translateLike(
        column: ColumnDeclaring<*>,
        pattern: String,
        caseSensitive: Boolean
    ): ColumnDeclaring<Boolean> {
        return if (caseSensitive) {
            column.like(pattern)
        } else {
            // PostgreSQL 支持 ILIKE
            // PostgreSQL supports ILIKE
            // 注意：Ktorm 核心不直接支持 ilike，需要使用自定义表达式
            // Note: Ktorm core doesn't directly support ilike, need custom expression
            column.lower().like(pattern.lowercase())
        }
    }

    override fun translateRegex(
        column: ColumnDeclaring<*>,
        pattern: String
    ): ColumnDeclaring<Boolean>? {
        // PostgreSQL 支持 ~ 操作符进行正则匹配
        // PostgreSQL supports ~ operator for regex matching
        // 需要使用自定义 SQL 片段
        // Need to use custom SQL fragment
        return null  // 暂不支持，需要扩展 / Not supported yet, needs extension
    }
}

/**
 * MySQL 模式匹配策略
 * MySQL Pattern Match Policy
 *
 * MySQL 使用 LIKE 和 REGEXP。
 * MySQL uses LIKE and REGEXP.
 */
object MySqlPatternMatchPolicy : PatternMatchPolicy {
    override fun translateLike(
        column: ColumnDeclaring<*>,
        pattern: String,
        caseSensitive: Boolean
    ): ColumnDeclaring<Boolean> {
        // MySQL LIKE 默认不区分大小写（取决于排序规则）
        // MySQL LIKE is case-insensitive by default (depends on collation)
        return column.like(pattern)
    }

    override fun translateRegex(
        column: ColumnDeclaring<*>,
        pattern: String
    ): ColumnDeclaring<Boolean>? {
        // MySQL 支持 REGEXP
        // MySQL supports REGEXP
        // 需要使用自定义 SQL 片段
        // Need to use custom SQL fragment
        return null  // 暂不支持，需要扩展 / Not supported yet, needs extension
    }
}