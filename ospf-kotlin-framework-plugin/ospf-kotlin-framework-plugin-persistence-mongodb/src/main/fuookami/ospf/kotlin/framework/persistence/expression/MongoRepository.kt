/**
 * MongoDB 仓储实现
 * MongoDB Repository Implementation
 *
 * 提供基于 MongoDB 的仓储实现。 / Provides MongoDB-based repository implementation.
*/
package fuookami.ospf.kotlin.framework.persistence.expression

import java.util.concurrent.CancellationException
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import org.bson.Document
import org.bson.conversions.Bson
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.expression.BooleanExpression
import fuookami.ospf.kotlin.framework.persistence.expression.translator.*

/**
 * MongoDB 仓储实现
 * MongoDB Repository Implementation
 *
 * 提供基于 MongoDB 的仓储基类实现。 / Provides base repository implementation based on MongoDB.
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
    protected val unsupportedPredicatePolicy: UnsupportedPredicatePolicy = UnsupportedPredicatePolicy.FailFast
) : ExpressionRepository<E> {

    /**
     * 布尔表达式翻译器实例 / Boolean expression translator instance
    */
    private val booleanTranslator = MongoBooleanTranslator(resolveFieldName, unsupportedPredicatePolicy)

    /**
     * 排序翻译器实例 / Order by translator instance
    */
    private val orderByTranslator = MongoOrderByTranslator(resolveFieldName)

    /**
     * 更新翻译器实例 / Update translator instance
    */
    private val updateTranslator = MongoUpdateTranslator(resolveFieldName)

    /**
     * MongoDB 集合实例
     * MongoDB collection instance
    */
    protected val collection: MongoCollection<Document>
        get() = database.getCollection(collectionName)

    /**
     * 根据条件查询实体列表 / Find entity list by condition
     *
     * @param where 查询条件 / Query condition
     * @return 实体列表 / Entity list
    */
    override fun find(where: BooleanExpression): Ret<List<E>> {
        return find(where, null, null, null)
    }

    /**
     * 根据条件查询实体列表（支持排序和分页） / Find entity list by condition with sorting and pagination
     *
     * @param where 查询条件 / Query condition
     * @param sortBy 排序条件（可选）/ Sort conditions (optional)
     * @param limit 返回数量限制（可选）/ Limit (optional)
     * @param offset 偏移量（可选）/ Offset (optional)
     * @return 实体列表 / Entity list
    */
    override fun find(
        where: BooleanExpression,
        sortBy: SortBy?,
        limit: Int?,
        offset: Int?
    ): Ret<List<E>> {
        if (limit != null && limit < 0) {
            return paginationFailure("limit", limit)
        }
        if (offset != null && offset < 0) {
            return paginationFailure("offset", offset)
        }
        if (limit == 0) return Ok(emptyList())

        return mongoBoundary("find") {
            val translatedFilter = booleanTranslator.translate(where)
            translatedFilter.propagateFailure<List<E>>()?.let { return@mongoBoundary it }
            val filter = translatedFilter.value
                ?: return@mongoBoundary missingFilterFailure()

            val translatedSort = orderByTranslator.translate(sortBy)
            translatedSort.propagateFailure<List<E>>()?.let { return@mongoBoundary it }
            val sort = translatedSort.value

            var findIterable = collection.find(filter)
            if (sort != null) {
                findIterable = findIterable.sort(sort)
            }
            if (offset != null) {
                findIterable = findIterable.skip(offset)
            }
            if (limit != null) {
                findIterable = findIterable.limit(limit)
            }

            Ok(findIterable.mapNotNull { mapToEntity(it) }.toList())
        }
    }

    /**
     * 统计满足条件的实体数量 / Count entities matching condition
     *
     * @param where 查询条件 / Query condition
     * @return 实体数量 / Entity count
    */
    override fun count(where: BooleanExpression): Ret<Long> {
        return mongoBoundary("count") {
            val translatedFilter = booleanTranslator.translate(where)
            translatedFilter.propagateFailure<Long>()?.let { return@mongoBoundary it }
            val filter = translatedFilter.value
                ?: return@mongoBoundary missingFilterFailure()
            Ok(collection.countDocuments(filter))
        }
    }

    /**
     * 更新满足条件的实体 / Update entities matching condition
     *
     * @param where 更新条件 / Update condition
     * @param assignments 更新赋值列表 / Update assignment list
     * @return 受影响的行数 / Number of affected rows
    */
    override fun update(where: BooleanExpression, assignments: UpdateAssignments): Ret<Int> {
        if (assignments.isEmpty()) return Ok(0)

        return mongoBoundary("update") {
            val translatedFilter = booleanTranslator.translate(where)
            val translatedUpdate = updateTranslator.translate(assignments)

            translatedFilter.propagateFailure<Int>()?.let { return@mongoBoundary it }
            translatedUpdate.propagateFailure<Int>()?.let { return@mongoBoundary it }

            val filter = translatedFilter.value
                ?: return@mongoBoundary missingFilterFailure()
            val update = translatedUpdate.value
                ?: return@mongoBoundary Failed(
                    ErrorCode.ApplicationFailed,
                    "MongoDB 更新翻译未生成更新文档 / MongoDB update translation produced no update document"
                )

            val result = collection.updateMany(filter, update)
            Ok(result.modifiedCount.toInt())
        }
    }

    /**
     * 删除满足条件的实体 / Delete entities matching condition
     *
     * @param where 删除条件 / Delete condition
     * @return 受影响的行数 / Number of affected rows
    */
    override fun delete(where: BooleanExpression): Ret<Int> {
        return mongoBoundary("delete") {
            val translatedFilter = booleanTranslator.translate(where)
            translatedFilter.propagateFailure<Int>()?.let { return@mongoBoundary it }
            val filter = translatedFilter.value
                ?: return@mongoBoundary missingFilterFailure()

            val result = collection.deleteMany(filter)
            Ok(result.deletedCount.toInt())
        }
    }

    /**
     * 将 Document 映射为实体
     * Map Document to entity
     *
     * 子类需要实现此方法以进行实体映射。 / Subclasses must implement this method for entity mapping.
     *
     * @param document 需要映射的 MongoDB Document / MongoDB Document to map
     * @return 映射后的实体实例，映射失败时返回 null / Mapped entity instance, or null if mapping fails
    */
    protected abstract fun mapToEntity(document: Document): E?

    private fun paginationFailure(parameter: String, value: Int): Ret<List<E>> {
        return Failed(
            ErrorCode.IllegalArgument,
            "分页参数 $parameter 不能为负数：$value / Pagination parameter $parameter cannot be negative: $value"
        )
    }

    private fun <T> missingFilterFailure(): Ret<T> {
        return Failed(
            ErrorCode.ApplicationFailed,
            "MongoDB 条件翻译未生成过滤器 / MongoDB predicate translation produced no filter"
        )
    }

    private inline fun <T> mongoBoundary(operation: String, block: () -> Ret<T>): Ret<T> {
        return try {
            block()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            persistenceFailure("MongoDB $operation", error)
        }
    }

    companion object {
        /**
         * 简单字段名解析器：直接使用路径最后一部分作为字段名 / Simple field resolver: use last part of path as field name
         *
         * @return 字段名解析器函数 / Field name resolver function
        */
        fun simpleFieldResolver(): MongoFieldNameResolver = MongoFieldNameResolver { path: String ->
            path.substringAfterLast(".")
        }
    }
}
