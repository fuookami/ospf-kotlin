/**
 * Ktorm 仓储实现
 * Ktorm Repository Implementation
 *
 * 提供基于 Ktorm 的仓储实现。
 * Provides Ktorm-based repository implementation.
 */
package fuookami.ospf.kotlin.framework.persistence.expression

import fuookami.ospf.kotlin.math.symbol.expression.BooleanExpression
import fuookami.ospf.kotlin.framework.persistence.expression.translator.*
import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.ktorm.schema.Table

/**
 * Ktorm 仓储实现
 * Ktorm Repository Implementation
 *
 * 提供基于 Ktorm 的仓储基类实现。
 * Provides base repository implementation based on Ktorm.
 *
 * @param E 实体类型 / Entity type
 * @property database Ktorm 数据库实例 / Ktorm database instance
 * @property meta 实体元数据 / Entity metadata
 * @property table Ktorm 表定义 / Ktorm table definition
 * @property patternMatchPolicy 模式匹配策略 / Pattern match policy
 * @property nullsOrderSupport 空值排序支持 / Nulls order support
 */
abstract class KtormRepository<E : Any>(
    protected val database: Database,
    protected val meta: EntityMeta<E>,
    protected val table: Table<*>,
    protected val patternMatchPolicy: PatternMatchPolicy = DefaultPatternMatchPolicy,
    protected val nullsOrderSupport: NullsOrderSupport = NullsOrderSupport.Auto
) : ExpressionRepository<E> {

    private val booleanTranslator = KtormBooleanTranslator(meta, patternMatchPolicy)
    private val orderByTranslator = KtormOrderByTranslator(meta, nullsOrderSupport)
    private val updateTranslator = KtormUpdateTranslator(meta, table)

    override fun find(where: BooleanExpression): List<E> {
        return find(where, null, null, null)
    }

    override fun find(
        where: BooleanExpression,
        sortBy: SortBy?,
        limit: Int?,
        offset: Int?
    ): List<E> {
        val condition = booleanTranslator.translate(where)
        if (condition == null) return emptyList()

        var query = database.from(table).select().where(condition)

        // 应用排序
        // Apply order by
        if (sortBy != null && sortBy.isNotEmpty()) {
            query = orderByTranslator.apply(query, sortBy)
        }

        // 应用分页
        // Apply pagination
        if (limit != null) {
            query = query.limit(limit)
        }
        if (offset != null) {
            query = query.offset(offset)
        }

        return query.mapNotNull { mapToEntity(it) }
    }

    override fun count(where: BooleanExpression): Long {
        val condition = booleanTranslator.translate(where)
        if (condition == null) return 0L

        val totalRecords = database.from(table).select().where(condition).totalRecords
        return totalRecords.toLong()
    }

    override fun update(where: BooleanExpression, assignments: UpdateAssignments): Int {
        if (assignments.isEmpty()) return 0

        val condition = booleanTranslator.translate(where)
        if (condition == null) return 0

        return updateTranslator.executeUpdate(database, condition, assignments)
    }

    override fun delete(where: BooleanExpression): Int {
        val condition = booleanTranslator.translate(where)
        if (condition == null) return 0

        return database.delete(table) { condition }
    }

    /**
     * 将行映射为实体
     * Map row to entity
     *
     * 子类需要实现此方法以进行实体映射。
     * Subclasses must implement this method for entity mapping.
     */
    protected abstract fun mapToEntity(row: QueryRowSet): E?
}

/**
 * 仓储构建器
 * Repository Builder
 *
 * 用于创建简单的 Ktorm 仓储实例。
 * Creates simple Ktorm repository instances.
 */
class RepositoryBuilder<E : Any>(
    private val database: Database,
    private val meta: EntityMeta<E>,
    private val table: Table<*>
) {
    private var patternMatchPolicy: PatternMatchPolicy = DefaultPatternMatchPolicy
    private var nullsOrderSupport: NullsOrderSupport = NullsOrderSupport.Auto
    private var mapper: ((QueryRowSet) -> E?)? = null

    /**
     * 设置模式匹配策略
     * Set pattern match policy
     */
    fun patternMatchPolicy(policy: PatternMatchPolicy): RepositoryBuilder<E> {
        this.patternMatchPolicy = policy
        return this
    }

    /**
     * 设置空值排序支持
     * Set nulls order support
     */
    fun nullsOrderSupport(support: NullsOrderSupport): RepositoryBuilder<E> {
        this.nullsOrderSupport = support
        return this
    }

    /**
     * 设置实体映射器
     * Set entity mapper
     */
    fun mapper(mapper: (QueryRowSet) -> E?): RepositoryBuilder<E> {
        this.mapper = mapper
        return this
    }

    /**
     * 构建仓储
     * Build repository
     */
    fun build(): ExpressionRepository<E> {
        val finalMapper = mapper ?: throw IllegalStateException("Mapper must be set")

        return object : KtormRepository<E>(
            database, meta, table, patternMatchPolicy, nullsOrderSupport
        ) {
            override fun mapToEntity(row: QueryRowSet): E? = finalMapper(row)
        }
    }
}