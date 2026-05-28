/**
 * MongoDB 仓储集成测试
 * MongoDB Repository Integration Tests
 */
package fuookami.ospf.kotlin.framework.persistence.expression

import com.mongodb.client.FindIterable
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.result.DeleteResult
import com.mongodb.client.result.UpdateResult
import com.mongodb.MongoClientSettings
import fuookami.ospf.kotlin.framework.persistence.expression.translator.MongoFieldNameResolver
import fuookami.ospf.kotlin.math.symbol.expression.*
import java.lang.reflect.Proxy
import org.bson.BsonDocument
import org.bson.conversions.Bson
import org.bson.Document
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("MongoRepository Tests / MongoDB 仓储测试")
class MongoRepositoryTest {
    private data class User(val name: String?)

    private data class Recorder(
        var lastFindFilter: Bson? = null,
        var lastSort: Bson? = null,
        var lastSkip: Int? = null,
        var lastLimit: Int? = null,
        var lastCountFilter: Bson? = null,
        var lastUpdateFilter: Bson? = null,
        var lastUpdateDoc: Bson? = null,
        var lastDeleteFilter: Bson? = null
    )

    private data class FindState(
        val docs: List<Document>,
        var skip: Int = 0,
        var limit: Int? = null
    )

    private class TestRepository(
        database: MongoDatabase,
        resolver: MongoFieldNameResolver,
        unsupportedPredicatePolicy: UnsupportedPredicatePolicy = UnsupportedPredicatePolicy.AlwaysFalse
    ) : MongoRepository<User>(database, "users", resolver, unsupportedPredicatePolicy) {
        override fun mapToEntity(document: Document): User {
            return User(document.getString("name"))
        }
    }

    private val codec = MongoClientSettings.getDefaultCodecRegistry()
    private val resolver: MongoFieldNameResolver = { path: String -> path.substringAfterLast(".") }

    @Test
    @DisplayName("should pass where sort page update delete to collection / 应将 where sort page update delete 传递到集合层")
    fun shouldPassWhereSortPageUpdateDeleteToCollection() {
        val recorder = Recorder()
        val findState = FindState(
            docs = listOf(
                Document(mapOf("name" to "a")),
                Document(mapOf("name" to "b")),
                Document(mapOf("name" to "c"))
            )
        )
        val database = createDatabaseProxy(recorder, findState)
        val repository = TestRepository(database, resolver)
        val activeWhere = Comparison(
            ComparisonOperator.Eq,
            ScalarReference(PropertyPath.parse("status")),
            ScalarConstant("active")
        )

        val result = repository.find(
            where = activeWhere,
            sortBy = SortBy.desc("age"),
            limit = 1,
            offset = 1
        )
        val count = repository.count(activeWhere)
        val updated = repository.update(activeWhere, UpdateAssignments.set("status", "inactive"))
        val deleted = repository.delete(
            Comparison(
                ComparisonOperator.Eq,
                ScalarReference(PropertyPath.parse("status")),
                ScalarConstant("pending")
            )
        )

        assertEquals(1, result.size)
        assertEquals("b", result[0].name)
        assertEquals(2L, count)
        assertEquals(1, updated)
        assertEquals(1, deleted)

        assertNotNull(recorder.lastFindFilter)
        assertNotNull(recorder.lastSort)
        assertEquals(1, recorder.lastSkip)
        assertEquals(1, recorder.lastLimit)
        assertTrue(json(recorder.lastFindFilter).contains("status"))
        assertTrue(json(recorder.lastSort).contains("age"))
        assertTrue(json(recorder.lastUpdateDoc).contains("\"\$set\""))
        assertTrue(json(recorder.lastDeleteFilter).contains("pending"))
    }

    @Test
    @DisplayName("complex predicate should pass expr to collection / 复杂谓词应将 expr 传递到集合层")
    fun complexPredicateShouldPassExprToCollection() {
        val recorder = Recorder()
        val database = createDatabaseProxy(
            recorder,
            FindState(docs = listOf(Document(mapOf("name" to "a"))))
        )
        val repository = TestRepository(database, resolver)
        val where = Comparison(
            ComparisonOperator.Gt,
            ScalarBinary(
                BinaryOperator.Multiply,
                ScalarReference<Int>(PropertyPath.parse("price")),
                ScalarReference(PropertyPath.parse("quantity"))
            ),
            ScalarConstant(100)
        )

        repository.find(where)

        val filterJson = json(recorder.lastFindFilter)
        assertTrue(filterJson.contains("\"\$expr\""))
        assertTrue(filterJson.contains("\"\$multiply\""))
    }

    @Test
    @DisplayName("unsupported policy should fail explicitly / 不支持策略应明确失败")
    fun unsupportedPolicyShouldFailExplicitly() {
        val failFastRepository = TestRepository(
            createDatabaseProxy(Recorder(), FindState(emptyList())),
            resolver,
            UnsupportedPredicatePolicy.FailFast
        )
        val clientFilterRepository = TestRepository(
            createDatabaseProxy(Recorder(), FindState(emptyList())),
            resolver,
            UnsupportedPredicatePolicy.ClientFilter
        )

        assertThrows(IllegalArgumentException::class.java) {
            failFastRepository.find(BooleanCustom("x"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            clientFilterRepository.find(BooleanCustom("x"))
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun createDatabaseProxy(recorder: Recorder, findState: FindState): MongoDatabase {
        lateinit var findIterableProxy: FindIterable<Document>
        findIterableProxy = Proxy.newProxyInstance(
            FindIterable::class.java.classLoader,
            arrayOf(FindIterable::class.java)
        ) { _, method, args ->
            when (method.name) {
                "sort" -> {
                    recorder.lastSort = args?.getOrNull(0) as? Bson
                    findIterableProxy
                }
                "skip" -> {
                    findState.skip = args?.getOrNull(0) as? Int ?: 0
                    recorder.lastSkip = findState.skip
                    findIterableProxy
                }
                "limit" -> {
                    findState.limit = args?.getOrNull(0) as? Int
                    recorder.lastLimit = findState.limit
                    findIterableProxy
                }
                "iterator" -> {
                    val dropped = findState.docs.drop(findState.skip)
                    val limited = findState.limit?.let { dropped.take(it) } ?: dropped
                    limited.iterator()
                }
                else -> null
            }
        } as FindIterable<Document>

        val collectionProxy = Proxy.newProxyInstance(
            MongoCollection::class.java.classLoader,
            arrayOf(MongoCollection::class.java)
        ) { _, method, args ->
            when (method.name) {
                "find" -> {
                    recorder.lastFindFilter = args?.getOrNull(0) as? Bson
                    findState.skip = 0
                    findState.limit = null
                    findIterableProxy
                }
                "countDocuments" -> {
                    recorder.lastCountFilter = args?.getOrNull(0) as? Bson
                    2L
                }
                "updateMany" -> {
                    recorder.lastUpdateFilter = args?.getOrNull(0) as? Bson
                    recorder.lastUpdateDoc = args?.getOrNull(1) as? Bson
                    UpdateResult.acknowledged(1L, 1L, null)
                }
                "deleteMany" -> {
                    recorder.lastDeleteFilter = args?.getOrNull(0) as? Bson
                    DeleteResult.acknowledged(1L)
                }
                else -> null
            }
        } as MongoCollection<Document>

        return Proxy.newProxyInstance(
            MongoDatabase::class.java.classLoader,
            arrayOf(MongoDatabase::class.java)
        ) { _, method, args ->
            when (method.name) {
                "getCollection" -> collectionProxy
                else -> null
            }
        } as MongoDatabase
    }

    private fun json(bson: Bson?): String {
        return (bson ?: error("bson is null")).toBsonDocument(BsonDocument::class.java, codec).toJson()
    }
}
