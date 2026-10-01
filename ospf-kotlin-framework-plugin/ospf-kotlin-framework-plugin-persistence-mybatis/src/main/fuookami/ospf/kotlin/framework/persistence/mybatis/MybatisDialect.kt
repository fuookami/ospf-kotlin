/**
 * MyBatis SQL dialect contract and built-in relational dialects.
 * MyBatis SQL 方言契约及内置关系型数据库方言。
 */
package fuookami.ospf.kotlin.framework.persistence.mybatis

import fuookami.ospf.kotlin.framework.persistence.expression.NullsOrder
import fuookami.ospf.kotlin.framework.persistence.expression.NullsOrderSupport
import fuookami.ospf.kotlin.framework.persistence.expression.SortDirection
import fuookami.ospf.kotlin.framework.persistence.expression.translator.MybatisScalarSql
import fuookami.ospf.kotlin.math.symbol.expression.ScalarFunctionNames
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * MyBatis SQL 子句 / MyBatis SQL clause
 *
 * @property sql SQL 子句文本 / SQL clause text
 */
data class MybatisSqlClause(val sql: String)

/**
 * MyBatis 方言错误详情 / MyBatis dialect failure details
 *
 * @property dialectName 方言名称 / Dialect name
 * @property feature 不支持的功能 / Unsupported feature
 * @property reason 失败原因 / Failure reason
 */
data class MybatisDialectFailure(
    val dialectName: String,
    val feature: String,
    val reason: String
)

/**
 * MyBatis 数据库方言 / MyBatis database dialect
 *
 * 分页、NULL 排序和标量函数均经该接口显式处理。方言不能表达的功能必须返回结构化失败，不能生成近似 SQL。
 * Pagination, NULL ordering, and scalar functions are handled explicitly by this contract. Unsupported features return a structured failure rather than approximate SQL.
 *
 * @property name 方言名称 / Dialect name
 * @property nullsOrderSupport 原生 NULL 排序语法支持情况 / Native NULL ordering support
 */
interface MybatisDialect {
    val name: String
    val nullsOrderSupport: NullsOrderSupport

    /**
     * 构建分页子句 / Build a pagination clause
     *
     * @param limit 最大返回行数 / Maximum number of returned rows
     * @param offset 跳过的行数 / Number of rows to skip
     * @return SQL 子句或结构化错误 / SQL clause or structured failure
     */
    fun pagination(limit: Int?, offset: Int?): Ret<MybatisSqlClause>

    /**
     * 引用列名或限定列名 / Quote a column or qualified column name
     *
     * @param identifier 列名或点分隔的限定列名 / Column name or dot-separated qualified name
     * @return 引用后的列名或结构化错误 / Quoted identifier or structured failure
     */
    fun quoteIdentifier(identifier: String): Ret<String>

    /**
     * 构建含 NULL 顺序的排序项 / Build an order item with requested NULL ordering
     *
     * @param column 已解析的列名 / Resolved column name
     * @param direction 排序方向 / Sort direction
     * @param nulls NULL 顺序，可为 null / Requested NULL order, if any
     * @param support 原生 NULL 排序支持策略 / Native NULL ordering override
     * @return 排序子句或结构化错误 / Order clause or structured failure
     */
    fun orderBy(
        column: String,
        direction: SortDirection,
        nulls: NullsOrder?,
        support: NullsOrderSupport = nullsOrderSupport
    ): Ret<MybatisSqlClause> {
        return when (val quoted = quoteIdentifier(column)) {
            is Ok -> {
                val directionSql = when (direction) {
                    SortDirection.Asc -> "ASC"
                    SortDirection.Desc -> "DESC"
                }
                if (nulls == null) {
                    Ok(MybatisSqlClause("${quoted.value} $directionSql"))
                } else {
                    val nativeOrder = when (support) {
                        NullsOrderSupport.Auto -> nullsOrderSupport == NullsOrderSupport.Always ||
                            nullsOrderSupport == NullsOrderSupport.OnlyAsc && direction == SortDirection.Asc
                        NullsOrderSupport.Always -> true
                        NullsOrderSupport.Never -> false
                        NullsOrderSupport.OnlyAsc -> direction == SortDirection.Asc
                    }
                    if (nativeOrder) {
                        val nullsSql = when (nulls) {
                            NullsOrder.NullsFirst -> "FIRST"
                            NullsOrder.NullsLast -> "LAST"
                        }
                        Ok(MybatisSqlClause("${quoted.value} $directionSql NULLS $nullsSql"))
                    } else {
                        val nullRank = when (nulls) {
                            NullsOrder.NullsFirst -> "0 ELSE 1"
                            NullsOrder.NullsLast -> "1 ELSE 0"
                        }
                        Ok(MybatisSqlClause("CASE WHEN ${quoted.value} IS NULL THEN $nullRank END ASC, ${quoted.value} $directionSql"))
                    }
                }
            }
            is Failed -> Failed(quoted.error)
            is Fatal -> Fatal(quoted.errors)
        }
    }

    /**
     * 翻译标量函数；返回 null 表示该方言不支持该函数或参数数量。
     * Translate a scalar function; null means the dialect does not support the function or arity.
     *
     * @param name 逻辑函数名 / Logical function name
     * @param arguments 已翻译的函数参数 / Translated function arguments
     * @return 标量 SQL；不支持时返回 null / Scalar SQL, or null when unsupported
     */
    fun scalarFunction(name: String, arguments: List<MybatisScalarSql>): MybatisScalarSql?

    companion object {
        /** SQLite 方言 / SQLite dialect */
        val SQLite: MybatisDialect = StandardMybatisDialect(
            name = "SQLite",
            quoteCharacter = '`',
            nullsOrderSupport = NullsOrderSupport.Never,
            offsetOnlyClause = { offset -> "LIMIT -1 OFFSET $offset" },
            lengthFunction = "LENGTH"
        )

        /** MySQL 方言 / MySQL dialect */
        val MySQL: MybatisDialect = StandardMybatisDialect(
            name = "MySQL",
            quoteCharacter = '`',
            nullsOrderSupport = NullsOrderSupport.Never,
            offsetOnlyClause = { offset -> "LIMIT 18446744073709551615 OFFSET $offset" },
            lengthFunction = "CHAR_LENGTH"
        )

        /** PostgreSQL 方言 / PostgreSQL dialect */
        val PostgreSQL: MybatisDialect = StandardMybatisDialect(
            name = "PostgreSQL",
            quoteCharacter = '"',
            nullsOrderSupport = NullsOrderSupport.Always,
            offsetOnlyClause = { offset -> "OFFSET $offset ROWS" },
            lengthFunction = "CHAR_LENGTH"
        )

        /** H2 方言 / H2 dialect */
        val H2: MybatisDialect = StandardMybatisDialect(
            name = "H2",
            quoteCharacter = '"',
            nullsOrderSupport = NullsOrderSupport.Always,
            offsetOnlyClause = { offset -> "OFFSET $offset ROWS" },
            lengthFunction = "CHAR_LENGTH"
        )

        /**
         * 数据库未知时供旧仓储构造入口使用的兼容方言。
         * Compatibility dialect for the legacy repository constructor when the database is unknown.
         */
        val Portable: MybatisDialect = StandardMybatisDialect(
            name = "Portable",
            quoteCharacter = '"',
            nullsOrderSupport = NullsOrderSupport.Never,
            offsetOnlyClause = null,
            lengthFunction = null
        )
    }
}

private class StandardMybatisDialect(
    override val name: String,
    private val quoteCharacter: Char,
    override val nullsOrderSupport: NullsOrderSupport,
    private val offsetOnlyClause: ((Int) -> String)?,
    private val lengthFunction: String?
) : MybatisDialect {
    override fun pagination(limit: Int?, offset: Int?): Ret<MybatisSqlClause> {
        if (limit != null && limit < 0 || offset != null && offset < 0) {
            return Failed(ErrorCode.IllegalArgument, "分页参数不能为负数 / Pagination values cannot be negative")
        }
        val sql = when {
            limit != null && offset != null -> "LIMIT $limit OFFSET $offset"
            limit != null -> "LIMIT $limit"
            offset != null -> offsetOnlyClause?.invoke(offset) ?: return unsupported(
                feature = "offset-only pagination",
                reason = "the database is unknown; configure an explicit dialect"
            )
            else -> ""
        }
        return Ok(MybatisSqlClause(sql))
    }

    override fun quoteIdentifier(identifier: String): Ret<String> {
        val segments = identifier.split('.')
        if (segments.isEmpty() || segments.any { !identifierSegment.matches(it) }) {
            return dialectFailure(
                dialectName = name,
                feature = "identifier quoting",
                reason = "invalid identifier: $identifier"
            )
        }
        val quote = quoteCharacter.toString()
        return Ok(segments.joinToString(".") { segment ->
            "$quote${segment.replace(quote, quote + quote)}$quote"
        })
    }

    override fun scalarFunction(name: String, arguments: List<MybatisScalarSql>): MybatisScalarSql? {
        val sqlName = when (name.lowercase()) {
            ScalarFunctionNames.Abs.lowercase() -> "ABS"
            ScalarFunctionNames.Lower.lowercase() -> "LOWER"
            ScalarFunctionNames.Upper.lowercase() -> "UPPER"
            ScalarFunctionNames.Trim.lowercase() -> "TRIM"
            ScalarFunctionNames.Length.lowercase() -> lengthFunction ?: return null
            ScalarFunctionNames.Coalesce.lowercase() -> "COALESCE"
            else -> return null
        }
        if (sqlName == "COALESCE") {
            if (arguments.isEmpty()) return null
        } else if (arguments.size != 1) {
            return null
        }
        return MybatisScalarSql(
            sql = "$sqlName(${arguments.joinToString(", ") { it.sql }})",
            params = arguments.flatMap { it.params },
            isColumnOnly = false
        )
    }

    private fun unsupported(feature: String, reason: String): Ret<MybatisSqlClause> {
        return dialectFailure(name, feature, reason)
    }

    companion object {
        private val identifierSegment = Regex("[A-Za-z_][A-Za-z0-9_$]*")
    }
}

private fun <T> dialectFailure(
    dialectName: String,
    feature: String,
    reason: String
): Ret<T> {
    val detail = MybatisDialectFailure(dialectName, feature, reason)
    return Failed(ExErr(
        ErrorCode.ApplicationFailed,
        "MyBatis 方言不支持 $feature：$reason（dialect=$dialectName）/ MyBatis dialect does not support $feature: $reason (dialect=$dialectName)",
        detail
    ))
}
