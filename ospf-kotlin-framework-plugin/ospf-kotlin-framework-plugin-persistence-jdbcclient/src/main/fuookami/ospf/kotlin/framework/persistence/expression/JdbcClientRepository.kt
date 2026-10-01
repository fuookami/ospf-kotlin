/**
 * Spring JdbcClient 仓储实现 / Spring JdbcClient repository implementation
 *
 * 提供单表表达式查询、计数、更新和删除；字段和值分别经 resolver 与 JDBC 参数绑定处理。
 * Provides single-table expression queries, counts, updates, and deletes; fields and values are handled through a resolver and JDBC binding.
 */
package fuookami.ospf.kotlin.framework.persistence.expression

import java.util.concurrent.CancellationException
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.simple.JdbcClient
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.expression.BooleanExpression
import fuookami.ospf.kotlin.framework.persistence.expression.translator.*
import fuookami.ospf.kotlin.framework.persistence.jdbcclient.*

/**
 * JdbcClient 仓储基类 / JdbcClient repository base class
 *
 * @param E 实体类型 / Entity type
 * @property backend 客户端及数据库方言 / Client and database dialect
 * @property tableName 表名或限定表名 / Table name or qualified table name
 * @property rowMapper 实体映射器 / Entity row mapper
 * @property resolveColumnName 属性路径解析器 / Property path resolver
 * @property unsupportedPredicatePolicy 不支持谓词策略 / Unsupported predicate policy
 */
open class JdbcClientRepository<E : Any>(
    protected val backend: JdbcClientBackend,
    protected val tableName: String,
    protected val rowMapper: RowMapper<E>,
    protected val resolveColumnName: JdbcClientColumnNameResolver,
    protected val unsupportedPredicatePolicy: UnsupportedPredicatePolicy = UnsupportedPredicatePolicy.FailFast
) : ExpressionRepository<E> {
    private val booleanTranslator = JdbcClientBooleanTranslator(
        resolveColumnName = resolveColumnName,
        dialect = backend.dialect,
        unsupportedPredicatePolicy = unsupportedPredicatePolicy
    )
    private val orderByTranslator = JdbcClientOrderByTranslator(resolveColumnName, backend.dialect)
    private val updateTranslator = JdbcClientUpdateTranslator(resolveColumnName, backend.dialect)

    /**
     * 使用外部 JdbcClient 和显式方言创建仓储 / Create a repository from an external client and explicit dialect
     *
     * @param client 外部 Spring JdbcClient / External Spring JdbcClient
     * @param dialect 与客户端匹配的 SQL 方言 / SQL dialect paired with the client
     * @param tableName 表名或限定表名 / Table name or qualified table name
     * @param rowMapper 实体映射器 / Entity row mapper
     * @param resolveColumnName 属性路径解析器 / Property path resolver
     * @param unsupportedPredicatePolicy 不支持谓词策略 / Unsupported predicate policy
     */
    constructor(
        client: JdbcClient,
        dialect: JdbcClientDialect,
        tableName: String,
        rowMapper: RowMapper<E>,
        resolveColumnName: JdbcClientColumnNameResolver,
        unsupportedPredicatePolicy: UnsupportedPredicatePolicy = UnsupportedPredicatePolicy.FailFast
    ) : this(
        backend = JdbcClientBackend(client, dialect),
        tableName = tableName,
        rowMapper = rowMapper,
        resolveColumnName = resolveColumnName,
        unsupportedPredicatePolicy = unsupportedPredicatePolicy
    )

    /**
     * 根据条件查询 / Find by condition
     *
     * @param where 查询条件 / Query condition
     * @return 查询结果 / Query results
     */
    override fun find(where: BooleanExpression): Ret<List<E>> {
        return find(where, null, null, null)
    }

    /**
     * 根据条件、排序和分页查询 / Find by condition, sort, and pagination
     *
     * @param where 查询条件 / Query condition
     * @param sortBy 排序规则 / Sort rules
     * @param limit 最大返回数量 / Maximum number of rows
     * @param offset 跳过数量 / Number of rows to skip
     * @return 查询结果 / Query results
     */
    override fun find(
        where: BooleanExpression,
        sortBy: SortBy?,
        limit: Int?,
        offset: Int?
    ): Ret<List<E>> = safely("查询失败 / Query failed") {
        if (limit != null && limit < 0 || offset != null && offset < 0) {
            return@safely Failed(ErrorCode.IllegalArgument, "分页参数不能为负数 / Pagination values cannot be negative")
        }
        if (limit == 0) return@safely Ok(emptyList())

        val table = when (val result = quoteIdentifierPath(tableName, backend.dialect)) {
            is Ok -> result.value
            is Failed -> return@safely Failed(result.error)
            is Fatal -> return@safely Fatal(result.errors)
        }
        val predicate = when (val result = booleanTranslator.translate(where)) {
            is Ok -> result.value
            is Failed -> return@safely Failed(result.error)
            is Fatal -> return@safely Fatal(result.errors)
        }
        val order = if (sortBy == null || sortBy.isEmpty()) null else when (val result = orderByTranslator.translate(sortBy)) {
            is Ok -> result.value
            is Failed -> return@safely Failed(result.error)
            is Fatal -> return@safely Fatal(result.errors)
        }
        val pagination = when (val result = backend.dialect.pagination(limit, offset)) {
            is Ok -> result.value
            is Failed -> return@safely Failed(result.error)
            is Fatal -> return@safely Fatal(result.errors)
        }
        val sql = buildString {
            append("SELECT * FROM ")
            append(table)
            append(" WHERE ")
            append(predicate.sql)
            if (!order?.sql.isNullOrBlank()) {
                append(" ORDER BY ")
                append(order!!.sql)
            }
            if (pagination.sql.isNotBlank()) {
                append(' ')
                append(pagination.sql)
            }
        }
        val parameters = predicate.parameters + (order?.parameters ?: emptyList()) + pagination.parameters
        Ok(backend.client.sql(sql).params(parameters).query(rowMapper).list())
    }

    /**
     * 统计匹配数量 / Count matching rows
     *
     * @param where 查询条件 / Query condition
     * @return 匹配行数 / Number of matching rows
     */
    override fun count(where: BooleanExpression): Ret<Long> = safely("计数失败 / Count failed") {
        val table = when (val result = quoteIdentifierPath(tableName, backend.dialect)) {
            is Ok -> result.value
            is Failed -> return@safely Failed(result.error)
            is Fatal -> return@safely Fatal(result.errors)
        }
        val predicate = when (val result = booleanTranslator.translate(where)) {
            is Ok -> result.value
            is Failed -> return@safely Failed(result.error)
            is Fatal -> return@safely Fatal(result.errors)
        }
        val sql = "SELECT COUNT(*) FROM $table WHERE ${predicate.sql}"
        Ok(backend.client.sql(sql).params(predicate.parameters).query(Long::class.javaObjectType).single())
    }

    /**
     * 更新匹配行 / Update matching rows
     *
     * @param where 更新条件 / Update condition
     * @param assignments 更新赋值 / Update assignments
     * @return 影响行数 / Number of affected rows
     */
    override fun update(where: BooleanExpression, assignments: UpdateAssignments): Ret<Int> = safely("更新失败 / Update failed") {
        if (assignments.isEmpty()) return@safely Ok(0)
        val table = when (val result = quoteIdentifierPath(tableName, backend.dialect)) {
            is Ok -> result.value
            is Failed -> return@safely Failed(result.error)
            is Fatal -> return@safely Fatal(result.errors)
        }
        val set = when (val result = updateTranslator.translate(assignments)) {
            is Ok -> result.value
            is Failed -> return@safely Failed(result.error)
            is Fatal -> return@safely Fatal(result.errors)
        }
        val predicate = when (val result = booleanTranslator.translate(where)) {
            is Ok -> result.value
            is Failed -> return@safely Failed(result.error)
            is Fatal -> return@safely Fatal(result.errors)
        }
        val sql = "UPDATE $table SET ${set.sql} WHERE ${predicate.sql}"
        Ok(backend.client.sql(sql).params(set.parameters + predicate.parameters).update())
    }

    /**
     * 删除匹配行 / Delete matching rows
     *
     * @param where 删除条件 / Delete condition
     * @return 影响行数 / Number of affected rows
     */
    override fun delete(where: BooleanExpression): Ret<Int> = safely("删除失败 / Delete failed") {
        val table = when (val result = quoteIdentifierPath(tableName, backend.dialect)) {
            is Ok -> result.value
            is Failed -> return@safely Failed(result.error)
            is Fatal -> return@safely Fatal(result.errors)
        }
        val predicate = when (val result = booleanTranslator.translate(where)) {
            is Ok -> result.value
            is Failed -> return@safely Failed(result.error)
            is Fatal -> return@safely Fatal(result.errors)
        }
        val sql = "DELETE FROM $table WHERE ${predicate.sql}"
        Ok(backend.client.sql(sql).params(predicate.parameters).update())
    }

    /**
     * 检查是否存在匹配行 / Check whether any row matches
     *
     * @param where 查询条件 / Query condition
     * @return 是否存在 / Whether a matching row exists
     */
    override fun exists(where: BooleanExpression): Ret<Boolean> {
        return count(where).map { it > 0L }
    }

    private inline fun <T> safely(operation: String, block: () -> Ret<T>): Ret<T> {
        return try {
            block()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            exceptionFailure(operation, exception)
        }
    }
}
