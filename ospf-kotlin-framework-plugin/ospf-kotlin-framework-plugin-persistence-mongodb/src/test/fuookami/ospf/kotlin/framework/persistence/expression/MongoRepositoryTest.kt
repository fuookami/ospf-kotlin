/**
 * MongoDB 仓储集成测试
 * MongoDB Repository Integration Tests
 */
package fuookami.ospf.kotlin.framework.persistence.expression

import java.lang.reflect.Proxy
import java.util.concurrent.CancellationException
import com.mongodb.MongoClientSettings
import com.mongodb.client.FindIterable
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.result.DeleteResult
import com.mongodb.client.result.UpdateResult
import org.bson.BsonDocument
import org.bson.Document
import org.bson.conversions.Bson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.Trivalent
import fuookami.ospf.kotlin.math.symbol.expression.*
import fuookami.ospf.kotlin.framework.persistence.expression.translator.MongoFieldNameResolver
import fuookami.ospf.kotlin.framework.persistence.expression.translator.valueOrFail

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
        unsupportedPredicatePolicy: UnsupportedPredicatePolicy = UnsupportedPredicatePolicy.FailFast
    ) : MongoRepository<User>(database, "users", resolver, unsupportedPredicatePolicy) {
        override fun mapToEntity(document: Document): User {
            return User(document.getString("name"))
        }
    }

    private val codec = MongoClientSettings.getDefaultCodecRegistry()
    private val resolver = MongoFieldNameResolver { path: String -> path.substringAfterLast(".") }

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
        ).valueOrFail()
        val count = repository.count(activeWhere).valueOrFail()
        val updated = repository.update(activeWhere, UpdateAssignments.set("status", "inactive")).valueOrFail()
        val deleted = repository.delete(
            Comparison(
                ComparisonOperator.Eq,
                ScalarReference(PropertyPath.parse("status")),
                ScalarConstant("pending")
            )
        ).valueOrFail()

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

        repository.find(where).valueOrFail()

        val filterJson = json(recorder.lastFindFilter)
        assertTrue(filterJson.contains("\"\$expr\""))
        assertTrue(filterJson.contains("\"\$multiply\""))
    }

    @Test
    @DisplayName("unsupported policy should fail without collection query / 不支持谓词应失败且不查询集合")
    fun unsupportedPolicyShouldFailWithoutCollectionQuery() {
        val failFastRecorder = Recorder()
        val clientFilterRecorder = Recorder()
        val failFastRepository = TestRepository(
            createDatabaseProxy(
                failFastRecorder,
                FindState(docs = listOf(Document(mapOf("name" to "a"))))
            ),
            resolver
        )
        val clientFilterRepository = TestRepository(
            createDatabaseProxy(
                clientFilterRecorder,
                FindState(docs = listOf(Document(mapOf("name" to "b"))))
            ),
            resolver,
            UnsupportedPredicatePolicy.ClientFilter
        )

        val failFastResult = failFastRepository.find(BooleanCustom("x"))
        val clientFilterResult = clientFilterRepository.find(BooleanCustom("x"))

        assertTrue(failFastResult.failed)
        assertTrue(clientFilterResult.failed)
        assertNull(failFastRecorder.lastFindFilter)
        assertNull(clientFilterRecorder.lastFindFilter)
    }

    @Test
    @DisplayName("pagination should reject negatives and short-circuit zero limit / 分页应拒绝负数并短路零限制")
    fun paginationShouldRejectNegativesAndShortCircuitZeroLimit() {
        val recorder = Recorder()
        val repository = TestRepository(
            createDatabaseProxy(recorder, FindState(docs = emptyList())),
            resolver
        )

        val zeroLimit = repository.find(
            BooleanCustom("unsupported"),
            sortBy = null,
            limit = 0,
            offset = 0
        )
        val negativeLimit = repository.find(
            BooleanConstant(Trivalent.True),
            sortBy = null,
            limit = -1,
            offset = 0
        )
        val negativeOffset = repository.find(
            BooleanConstant(Trivalent.True),
            sortBy = null,
            limit = null,
            offset = -1
        )

        assertTrue(zeroLimit.valueOrFail().isEmpty())
        assertTrue(negativeLimit.failed)
        assertTrue(negativeOffset.failed)
        assertNull(recorder.lastFindFilter)
    }

    @Test
    @DisplayName("invalid assignment should fail before MongoDB update / 赋值无效时应在 MongoDB 更新前失败")
    fun invalidAssignmentShouldFailBeforeMongoUpdate() {
        val recorder = Recorder()
        val strictResolver = MongoFieldNameResolver { path: String ->
            if (path == "name") path else null
        }
        val repository = TestRepository(
            createDatabaseProxy(recorder, FindState(docs = emptyList())),
            strictResolver
        )
        val where = Comparison(
            ComparisonOperator.Eq,
            ScalarReference(PropertyPath.parse("name")),
            ScalarConstant("neo")
        )

        val result = repository.update(where, UpdateAssignments.set("missing", "value"))

        assertTrue(result.failed)
        assertNull(recorder.lastUpdateFilter)
        assertNull(recorder.lastUpdateDoc)
    }

    @Test
    @DisplayName("unknown under NOT must not broaden update or delete / NOT 下的 Unknown 不得扩大更新或删除范围")
    fun unknownUnderNotMustNotBroadenUpdateOrDelete() {
        val recorder = Recorder()
        val repository = TestRepository(
            createDatabaseProxy(recorder, FindState(docs = emptyList())),
            resolver
        )
        val directUnknown = NotExpression(BooleanConstant(Trivalent.Unknown))
        val nestedAndUnknown = NotExpression(AndExpression(listOf(
            BooleanConstant(Trivalent.True),
            BooleanConstant(Trivalent.Unknown)
        )))
        val nestedOrUnknown = NotExpression(OrExpression(listOf(
            BooleanConstant(Trivalent.False),
            BooleanConstant(Trivalent.Unknown)
        )))

        repository.update(directUnknown, UpdateAssignments.set("status", "inactive")).valueOrFail()
        assertNeverMatches(recorder.lastUpdateFilter)

        repository.delete(nestedAndUnknown).valueOrFail()
        assertNeverMatches(recorder.lastDeleteFilter)

        repository.delete(nestedOrUnknown).valueOrFail()
        assertNeverMatches(recorder.lastDeleteFilter)
    }

    @Test
    @DisplayName("Mongo and cursor exceptions should become failed results / MongoDB 与游标异常应转为失败结果")
    fun mongoAndCursorExceptionsShouldBecomeFailedResults() {
        val findFailureRepository = TestRepository(
            createDatabaseProxy(
                Recorder(),
                FindState(docs = emptyList()),
                failureOn = "find",
                failure = IllegalStateException("find")
            ),
            resolver
        )
        val cursorFailureRepository = TestRepository(
            createDatabaseProxy(
                Recorder(),
                FindState(docs = emptyList()),
                failureOn = "iterator",
                failure = IllegalStateException("cursor")
            ),
            resolver
        )

        assertTrue(findFailureRepository.find(BooleanConstant(Trivalent.True)).failed)
        assertTrue(cursorFailureRepository.find(BooleanConstant(Trivalent.True)).failed)
    }

    @Test
    @DisplayName("mapping exceptions should fail and cancellation should pass through / 映射异常应失败且取消异常继续抛出")
    fun mappingExceptionsShouldFailAndCancellationShouldPassThrough() {
        val database = createDatabaseProxy(
            Recorder(),
            FindState(docs = listOf(Document(mapOf("name" to "neo"))))
        )
        val failingRepository = object : MongoRepository<User>(database, "users", resolver) {
            override fun mapToEntity(document: Document): User? {
                throw IllegalStateException("mapping")
            }
        }
        val cancellingRepository = object : MongoRepository<User>(database, "users", resolver) {
            override fun mapToEntity(document: Document): User? {
                throw CancellationException("cancelled")
            }
        }

        assertTrue(failingRepository.find(BooleanConstant(Trivalent.True)).failed)
        assertThrows(CancellationException::class.java) {
            cancellingRepository.find(BooleanConstant(Trivalent.True))
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun createDatabaseProxy(
        recorder: Recorder,
        findState: FindState,
        failureOn: String? = null,
        failure: Exception? = null
    ): MongoDatabase {
        lateinit var findIterableProxy: FindIterable<Document>
        findIterableProxy = Proxy.newProxyInstance(
            FindIterable::class.java.classLoader,
            arrayOf(FindIterable::class.java)
        ) { _, method, args ->
            if (method.name == failureOn) throw failure ?: IllegalStateException("proxy failure")
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
            if (method.name == failureOn) throw failure ?: IllegalStateException("proxy failure")
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
            if (method.name == failureOn) throw failure ?: IllegalStateException("proxy failure")
            when (method.name) {
                "getCollection" -> collectionProxy
                else -> null
            }
        } as MongoDatabase
    }

    private fun json(bson: Bson?): String {
        return (bson ?: error("bson is null")).toBsonDocument(BsonDocument::class.java, codec).toJson()
    }

    private fun assertNeverMatches(filter: Bson?) {
        val filterJson = json(filter)
        assertTrue(filterJson.contains("\"_id\""))
        assertTrue(filterJson.contains("false"))
    }
}
