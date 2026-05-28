/**
 * MongoDB 仓储实现
 * MongoDB Repository Implementation
 *
 * 提供基于 MongoDB 的仓储实现。
 * Provides MongoDB-based repository implementation.
 */
package fuookami.ospf.kotlin.framework.persistence.expression

import com.mongodb.client.model.Filters
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import fuookami.ospf.kotlin.framework.persistence.expression.translator.*
import fuookami.ospf.kotlin.math.symbol.expression.BooleanExpression
import org.bson.conversions.Bson
import org.bson.Document

/**
 * MongoDB 仓储实现
 * MongoDB Repository Implementation
 *
 * 提供基于 MongoDB 的仓储基类实现。
 * Provides base repository implementation based on MongoDB.
 *
 * @param E 实体类型 / Entity type
 * @property database MongoDB 数据库实例 / MongoDB database instance
 * @property collectionName 集合名称 / Collection name
 * @property resolveFieldName 字段名解析函数 / Field name resolver function
 * @property unsupportedPredicatePolicy 不支持谓词策略 / Unsupported predicate policy
 */
abstract class MongoRepository<E : Any>(
    protected val database: MongoDatabase,
    protected val collectionName: String,
    protected val resolveFieldName: MongoFieldNameResolver,
    protected val unsupportedPredicatePolicy: UnsupportedPredicatePolicy = UnsupportedPredicatePolicy.AlwaysFalse
) : ExpressionRepository<E> {

    private val booleanTranslator = MongoBooleanTranslator(resolveFieldName, unsupportedPredicatePolicy)
    private val orderByTranslator = MongoOrderByTranslator(resolveFieldName)
    private val updateTranslator = MongoUpdateTranslator(resolveFieldName)

    protected val collection: MongoCollection<Document>
        get() = database.getCollection(collectionName)

    override fun find(where: BooleanExpression): List<E> {
        return find(where, null, null, null)
    }

    override fun find(
        where: BooleanExpression,
        sortBy: SortBy?,
        limit: Int?,
        offset: Int?
    ): List<E> {
        val filter = booleanTranslator.translate(where) ?: Filters.empty()

        var findIterable = collection.find(filter)

        // 应用排序
        // Apply order by
        val sort = orderByTranslator.translate(sortBy)
        if (sort != null) {
            findIterable = findIterable.sort(sort)
        }

        // 应用分页
        // Apply pagination
        if (offset != null) {
            findIterable = findIterable.skip(offset)
        }
        if (limit != null) {
            findIterable = findIterable.limit(limit)
        }

        return findIterable.mapNotNull { mapToEntity(it) }.toList()
    }

    override fun count(where: BooleanExpression): Long {
        val filter = booleanTranslator.translate(where) ?: Filters.empty()
        return collection.countDocuments(filter)
    }

    override fun update(where: BooleanExpression, assignments: UpdateAssignments): Int {
        if (assignments.isEmpty()) return 0

        val filter = booleanTranslator.translate(where) ?: return 0
        val update = updateTranslator.translate(assignments) ?: return 0

        val result = collection.updateMany(filter, update)
        return result.modifiedCount.toInt()
    }

    override fun delete(where: BooleanExpression): Int {
        val filter = booleanTranslator.translate(where) ?: return 0

        val result = collection.deleteMany(filter)
        return result.deletedCount.toInt()
    }

    /**
     * 将 Document 映射为实体
     * Map Document to entity
     *
     * 子类需要实现此方法以进行实体映射。
     * Subclasses must implement this method for entity mapping.
     */
    protected abstract fun mapToEntity(document: Document): E?

    companion object {
        /**
         * 简单字段名解析器：直接使用路径最后一部分作为字段名
         * Simple field resolver: use last part of path as field name
         */
        fun simpleFieldResolver(): MongoFieldNameResolver = { path: String ->
            path.substringAfterLast(".")
        }
    }
}
