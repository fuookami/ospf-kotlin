/**
 * 关系查询计划校验测试 / Relational query plan validation tests
 */
package fuookami.ospf.kotlin.framework.persistence.query

import java.util.concurrent.atomic.AtomicInteger
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.Trivalent
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.expression.*
import fuookami.ospf.kotlin.utils.functional.Failed

@DisplayName("RelationalQueryPlan Tests / 关系查询计划测试")
class RelationalQueryPlanTest {
    private enum class MutableEnum {
        Value;

        var state: Int = 1
    }

    @Test
    @DisplayName("should reject duplicate names and aliases / 应拒绝重复名称与别名")
    fun shouldRejectDuplicateNamesAndAliases() {
        val duplicateName = RelationalQueryPlan(
            root = QuerySource("orders"),
            joins = listOf(
                JoinSpec(
                    type = JoinType.Inner,
                    source = QuerySource("orders", "o2"),
                    condition = BooleanConstant(Trivalent.True),
                    cardinality = JoinCardinality.OneToOne
                )
            )
        )
        val duplicateAlias = RelationalQueryPlan(
            root = QuerySource("orders", "o"),
            joins = listOf(
                JoinSpec(
                    type = JoinType.Inner,
                    source = QuerySource("items", "o"),
                    condition = BooleanConstant(Trivalent.True),
                    cardinality = JoinCardinality.OneToOne
                )
            )
        )

        assertTrue(duplicateName.validate().failed)
        assertTrue(duplicateAlias.validate().failed)
    }

    @Test
    @DisplayName("should reject invalid paging / 应拒绝非法分页")
    fun shouldRejectInvalidPaging() {
        val result = RelationalQueryPlan(
            root = QuerySource("orders"),
            page = PageSpec(limit = 0, offset = -1)
        ).validate()

        assertTrue(result.failed)
    }

    @Test
    @DisplayName("should encode offset-only pagination / 应编码仅偏移分页")
    fun shouldEncodeOffsetOnlyPagination() {
        val offsetOnly = RelationalQueryPlan(
            root = QuerySource("orders"),
            page = PageSpec(limit = null, offset = 7)
        )
        val withoutPaging = RelationalQueryPlan(root = QuerySource("orders"))

        assertTrue(offsetOnly.validate().ok)
        assertTrue(offsetOnly.canonical().contains("page=unbounded:7"))
        assertNotEquals(withoutPaging.canonical(), offsetOnly.canonical())
        assertNotEquals(withoutPaging.hash(), offsetOnly.hash())
    }

    @Test
    @DisplayName("should reject uncorrelated or invalid exists joins / 应拒绝无关联或非法基数的 Exists Join")
    fun shouldRejectUncorrelatedOrInvalidExistsJoins() {
        val uncorrelated = RelationalQueryPlan(
            root = QuerySource("orders"),
            joins = listOf(
                JoinSpec(
                    type = JoinType.Exists,
                    source = QuerySource("items"),
                    condition = BooleanConstant(Trivalent.True),
                    cardinality = JoinCardinality.OneToMany
                )
            )
        )
        val invalidCardinality = RelationalQueryPlan(
            root = QuerySource("orders"),
            joins = listOf(
                JoinSpec(
                    type = JoinType.Exists,
                    source = QuerySource("items"),
                    condition = BooleanConstant(Trivalent.True),
                    cardinality = JoinCardinality.ManyToOne
                )
            )
        )

        assertTrue(uncorrelated.validate().failed)
        assertTrue(invalidCardinality.validate().failed)
    }

    @Test
    @DisplayName("should reject constant and non-correlated joins / 应拒绝常量和非关联 Join")
    fun shouldRejectConstantAndNonCorrelatedJoins() {
        val constantJoin = RelationalQueryPlan(
            root = QuerySource("orders"),
            joins = listOf(
                JoinSpec(
                    type = JoinType.Inner,
                    source = QuerySource("items"),
                    condition = BooleanConstant(Trivalent.True),
                    cardinality = JoinCardinality.OneToMany
                )
            )
        )
        val nonCorrelatedJoin = RelationalQueryPlan(
            root = QuerySource("orders", "o"),
            joins = listOf(
                JoinSpec(
                    type = JoinType.Inner,
                    source = QuerySource("items", "i"),
                    condition = Comparison(
                        operator = ComparisonOperator.Eq,
                        left = ScalarReference(PropertyPath.parse("i.orderId")),
                        right = ScalarConstant(1)
                    ),
                    cardinality = JoinCardinality.OneToMany
                )
            )
        )
        val partiallyCorrelatedOrJoin = RelationalQueryPlan(
            root = QuerySource("orders", "o"),
            joins = listOf(
                JoinSpec(
                    type = JoinType.Inner,
                    source = QuerySource("items", "i"),
                    condition = OrExpression(
                        listOf(
                            Comparison(
                                operator = ComparisonOperator.Eq,
                                left = ScalarReference(PropertyPath.parse("o.id")),
                                right = ScalarReference(PropertyPath.parse("i.orderId"))
                            ),
                            Comparison(
                                operator = ComparisonOperator.Eq,
                                left = ScalarReference(PropertyPath.parse("i.orderId")),
                                right = ScalarConstant(1)
                            )
                        )
                    ),
                    cardinality = JoinCardinality.OneToMany
                )
            )
        )
        val arithmeticCorrelatedJoin = RelationalQueryPlan(
            root = QuerySource("orders", "o"),
            joins = listOf(
                JoinSpec(
                    type = JoinType.Inner,
                    source = QuerySource("items", "i"),
                    condition = Comparison(
                        operator = ComparisonOperator.Eq,
                        left = ScalarReference(PropertyPath.parse("o.id")),
                        right = ScalarBinary(
                            operator = BinaryOperator.Add,
                            left = ScalarReference(PropertyPath.parse("i.orderId")),
                            right = ScalarConstant(1)
                        )
                    ),
                    cardinality = JoinCardinality.OneToMany
                )
            )
        )

        assertTrue(constantJoin.validate().failed)
        assertTrue(nonCorrelatedJoin.validate().failed)
        assertTrue(partiallyCorrelatedOrJoin.validate().failed)
        assertTrue(arithmeticCorrelatedJoin.validate().failed)
    }

    @Test
    @DisplayName("should defensively copy and canonicalize plans / 应防御性复制并规范化计划")
    fun shouldDefensivelyCopyAndCanonicalizePlans() {
        val joins = mutableListOf<JoinSpec>()
        val projections = mutableListOf<ProjectionSpec>()
        val plan = RelationalQueryPlan(
            root = QuerySource("orders"),
            joins = joins,
            projections = projections
        )
        joins += JoinSpec(
            type = JoinType.Inner,
            source = QuerySource("items"),
            condition = BooleanConstant(Trivalent.True),
            cardinality = JoinCardinality.OneToMany
        )
        projections += ProjectionSpec(ColumnRef("orders", "id"))

        assertTrue(plan.joins.isEmpty())
        assertTrue(plan.projections.isEmpty())
        assertEquals(plan.canonicalHash(), plan.copy().canonicalHash())
        assertEquals(plan.canonicalHash(), plan.hash())
        assertEquals(64, plan.canonicalHash().length)

        val operands = mutableListOf<BooleanExpression>(
            Comparison(
                operator = ComparisonOperator.Eq,
                left = ScalarReference(PropertyPath.parse("orders.id")),
                right = ScalarConstant(1)
            )
        )
        val expressionPlan = RelationalQueryPlan(
            root = QuerySource("orders"),
            predicate = AndExpression(operands)
        )
        val canonical = expressionPlan.canonical()
        operands.clear()
        assertEquals(canonical, expressionPlan.canonical())
    }

    @Test
    @DisplayName("should snapshot known custom payload containers / 应快照已知自定义负载容器")
    fun shouldSnapshotKnownCustomPayloadContainers() {
        val nestedMap = mutableMapOf<String, Any?>("key" to mutableListOf("value"))
        val booleanPayload = mutableListOf<Any?>("first", nestedMap)
        val booleanPlan = RelationalQueryPlan(
            root = QuerySource("orders"),
            predicate = BooleanCustom(booleanPayload)
        )
        val frozenBooleanPayload = (booleanPlan.predicate as BooleanCustom).value as List<*>
        val frozenMap = frozenBooleanPayload[1] as Map<*, *>
        val frozenNestedList = frozenMap["key"] as List<*>

        assertNotSame(booleanPayload, frozenBooleanPayload)
        assertNotSame(nestedMap, frozenMap)
        assertThrows(UnsupportedOperationException::class.java) {
            @Suppress("UNCHECKED_CAST")
            (frozenBooleanPayload as MutableList<Any?>).add("mutated")
        }
        assertThrows(UnsupportedOperationException::class.java) {
            @Suppress("UNCHECKED_CAST")
            (frozenMap as MutableMap<String, Any?>)["key"] = "mutated"
        }
        assertThrows(UnsupportedOperationException::class.java) {
            @Suppress("UNCHECKED_CAST")
            (frozenNestedList as MutableList<Any?>).add("mutated")
        }

        val booleanCanonical = booleanPlan.canonical()
        booleanPayload += "mutated"
        nestedMap["key"] = "changed"
        assertEquals(booleanCanonical, booleanPlan.canonical())

        val scalarPayload = mutableListOf("first")
        val scalarPlan = RelationalQueryPlan(
            root = QuerySource("orders"),
            predicate = Comparison<Any?>(
                operator = ComparisonOperator.Eq,
                left = ScalarReference(PropertyPath.parse("orders.status")),
                right = ScalarCustom<Any?>(scalarPayload)
            )
        )
        val frozenScalarPayload = (scalarPlan.predicate as Comparison<*>).right
            .let { (it as ScalarCustom<*>).value as List<*> }
        assertNotSame(scalarPayload, frozenScalarPayload)
        scalarPayload += "mutated"
        assertEquals(scalarPlan.canonical(), scalarPlan.copy().canonical())
    }

    @Test
    @DisplayName("should canonicalize expression shapes without literal values / 规范化表达式形状且不包含字面量值")
    fun shouldCanonicalizeExpressionShapesWithoutLiteralValues() {
        fun planWithLiteral(value: Int): RelationalQueryPlan {
            return RelationalQueryPlan(
                root = QuerySource("orders"),
                predicate = Comparison<Any?>(
                    operator = ComparisonOperator.Eq,
                    left = ScalarReference(PropertyPath.parse("orders.id")),
                    right = ScalarConstant(value)
                )
            )
        }

        val one = planWithLiteral(1)
        val two = planWithLiteral(2)
        assertEquals(one.canonical(), two.canonical())
        assertEquals(one.canonicalHash(), two.canonicalHash())

        val customA = RelationalQueryPlan(
            root = QuerySource("orders"),
            predicate = BooleanCustom(mapOf("key" to "a"), "custom")
        )
        val customB = RelationalQueryPlan(
            root = QuerySource("orders"),
            predicate = BooleanCustom(mapOf("other" to "b"), "custom")
        )
        assertEquals(customA.canonical(), customB.canonical())
        assertEquals(customA.canonicalHash(), customB.canonicalHash())
    }

    @Test
    @DisplayName("should reject opaque custom payloads structurally / 应结构化拒绝不透明自定义负载")
    fun shouldRejectOpaqueCustomPayloadsStructurally() {
        class MutablePayload(var value: String)

        val booleanResult = RelationalQueryPlan(
            root = QuerySource("orders"),
            predicate = BooleanCustom(MutablePayload("boolean"))
        ).validate()
        val booleanFailure = (booleanResult as Failed<*, *, *>).error.value
            as RelationalQueryValidationError
        assertEquals("predicate.value", booleanFailure.field)
        assertTrue(booleanFailure.reason.contains("cannot be safely copied"))

        val mutableNumberResult = RelationalQueryPlan(
            root = QuerySource("orders"),
            predicate = BooleanCustom(AtomicInteger(1))
        ).validate()
        val mutableNumberFailure = (mutableNumberResult as Failed<*, *, *>).error.value
            as RelationalQueryValidationError
        assertEquals("predicate.value", mutableNumberFailure.field)

        val cyclicPayload = mutableListOf<Any?>()
        cyclicPayload.add(cyclicPayload)
        val cyclicPlan = RelationalQueryPlan(
            root = QuerySource("orders"),
            predicate = BooleanCustom(cyclicPayload)
        )
        val cyclicFailure = (cyclicPlan.validate() as Failed<*, *, *>).error.value
            as RelationalQueryValidationError
        assertEquals("predicate.value[0]", cyclicFailure.field)
        assertEquals(cyclicPlan.canonical(), cyclicPlan.copy().canonical())

        val scalarResult = RelationalQueryPlan(
            root = QuerySource("orders"),
            predicate = Comparison<Any?>(
                operator = ComparisonOperator.Eq,
                left = ScalarReference(PropertyPath.parse("orders.id")),
                right = ScalarCustom<Any?>(MutablePayload("scalar"))
            )
        ).validate()
        val scalarFailure = (scalarResult as Failed<*, *, *>).error.value
            as RelationalQueryValidationError
        assertEquals("predicate.right.value", scalarFailure.field)

        val joinResult = RelationalQueryPlan(
            root = QuerySource("orders", "o"),
            joins = listOf(
                JoinSpec(
                    type = JoinType.Inner,
                    source = QuerySource("items", "i"),
                    condition = AndExpression(
                        listOf(
                            Comparison<Any?>(
                                operator = ComparisonOperator.Eq,
                                left = ScalarReference<Any?>(PropertyPath.parse("o.id")),
                                right = ScalarReference<Any?>(PropertyPath.parse("i.orderId"))
                            ),
                            BooleanCustom(MutablePayload("join"))
                        )
                    ),
                    cardinality = JoinCardinality.OneToMany
                )
            ),
            predicate = BooleanConstant(Trivalent.True)
        ).validate()
        val joinFailure = (joinResult as Failed<*, *, *>).error.value
            as RelationalQueryValidationError
        assertEquals("joins[0].condition.operands[1].value", joinFailure.field)

        val mutableEnumResult = RelationalQueryPlan(
            root = QuerySource("orders"),
            predicate = Comparison<Any?>(
                operator = ComparisonOperator.Eq,
                left = ScalarReference(PropertyPath.parse("orders.status")),
                right = ScalarConstant<Any?>(MutableEnum.Value)
            )
        ).validate()
        val mutableEnumFailure = (mutableEnumResult as Failed<*, *, *>).error.value
            as RelationalQueryValidationError
        assertEquals("predicate.right.value", mutableEnumFailure.field)
    }

    @Test
    @DisplayName("should canonicalize without reading custom literal text / 规范化不应读取自定义字面量文本")
    fun shouldCanonicalizeWithoutReadingCustomLiteralText() {
        class ExplosivePayload {
            override fun toString(): String = throw AssertionError("literal text must not be read")
        }

        val predicate = AndExpression(
            listOf(
                Comparison<Any?>(
                    operator = ComparisonOperator.Eq,
                    left = ScalarReference(PropertyPath.parse("orders.id")),
                    right = ScalarConstant<Any?>(ExplosivePayload())
                ),
                NullCheck(
                    path = PropertyPath.parse("orders.deletedAt"),
                    type = NullCheckType.IsNull
                )
            )
        )
        val plan = RelationalQueryPlan(
            root = QuerySource("orders"),
            predicate = predicate
        )

        assertDoesNotThrow {
            plan.canonical()
            plan.canonicalHash()
        }
        assertTrue(plan.validate().failed)
        assertEquals(plan.canonical(), plan.copy().canonical())
    }

    @Test
    @DisplayName("should normalize nested expressions and membership candidates / 应规范化嵌套表达式与成员候选集合")
    fun shouldNormalizeNestedExpressionsAndMembershipCandidates() {
        fun comparison(path: String, literal: Int): BooleanExpression {
            return Comparison<Any?>(
                operator = ComparisonOperator.Eq,
                left = ScalarReference(PropertyPath.parse(path)),
                right = ScalarConstant<Any?>(literal)
            )
        }

        val first = comparison("orders.id", 1)
        val second = comparison("orders.status", 2)
        val third = comparison("orders.version", 3)
        val nestedCondition = AndExpression(listOf(AndExpression(listOf(first, second)), third))
        val flatCondition = AndExpression(listOf(third, second, first))
        val nested = RelationalQueryPlan(
            root = QuerySource("orders"),
            predicate = Comparison<Any?>(
                operator = ComparisonOperator.Eq,
                left = ScalarReference(PropertyPath.parse("orders.id")),
                right = ScalarConditional<Any?>(
                    condition = nestedCondition,
                    thenBranch = ScalarConstant(1),
                    elseBranch = ScalarConstant(2)
                )
            )
        )
        val flat = RelationalQueryPlan(
            root = QuerySource("orders"),
            predicate = Comparison<Any?>(
                operator = ComparisonOperator.Eq,
                left = ScalarReference(PropertyPath.parse("orders.id")),
                right = ScalarConditional<Any?>(
                    condition = flatCondition,
                    thenBranch = ScalarConstant(1),
                    elseBranch = ScalarConstant(2)
                )
            )
        )

        assertEquals(flat.canonical(), nested.canonical())
        assertEquals(flat.canonicalHash(), nested.canonicalHash())

        fun membership(candidates: List<ScalarExpression<Any?>>): RelationalQueryPlan {
            return RelationalQueryPlan(
                root = QuerySource("orders"),
                predicate = InExpression(
                    value = ScalarReference(PropertyPath.parse("orders.status")),
                    candidates = candidates
                )
            )
        }
        val firstOrder = membership(
            listOf(
                ScalarConstant<Any?>(1),
                ScalarConstant<Any?>("active")
            )
        )
        val secondOrder = membership(
            listOf(
                ScalarConstant<Any?>("active"),
                ScalarConstant<Any?>(1)
            )
        )
        assertEquals(firstOrder.canonical(), secondOrder.canonical())
        assertEquals(firstOrder.canonicalHash(), secondOrder.canonicalHash())

        val differentShape = membership(listOf(ScalarConstant<Any?>(1L), ScalarConstant<Any?>("active")))
        assertNotEquals(firstOrder.canonical(), differentShape.canonical())
    }

    @Test
    @DisplayName("should snapshot nested expression collections / 应快照嵌套表达式集合")
    fun shouldSnapshotNestedExpressionCollections() {
        val conditionOperands = mutableListOf<BooleanExpression>(
            BooleanConstant(Trivalent.True),
            BooleanConstant(Trivalent.False)
        )
        val functionArguments = mutableListOf<ScalarExpression<Any?>>(
            ScalarBoolean<Any?>(AndExpression(conditionOperands))
        )
        val candidates = mutableListOf<ScalarExpression<Any?>>(ScalarConstant("active"))
        val plan = RelationalQueryPlan(
            root = QuerySource("orders"),
            predicate = InExpression(
                value = ScalarFunction<Any?>(
                    name = "coalesce",
                    arguments = functionArguments
                ),
                candidates = candidates
            )
        )
        val originalCanonical = plan.canonical()
        functionArguments += ScalarConstant(2)
        candidates += ScalarConstant(2)
        conditionOperands.clear()

        val frozenPredicate = plan.predicate as InExpression<*>
        val frozenArguments = (frozenPredicate.value as ScalarFunction<*>).arguments
        val frozenCondition = ((frozenArguments.first() as ScalarBoolean<*>).expr as AndExpression).operands
        val frozenCandidates = frozenPredicate.candidates
        assertEquals(1, frozenArguments.size)
        assertEquals(2, frozenCondition.size)
        assertEquals(1, frozenCandidates.size)
        assertNotSame(functionArguments, frozenArguments)
        assertNotSame(candidates, frozenCandidates)
        assertThrows(UnsupportedOperationException::class.java) {
            @Suppress("UNCHECKED_CAST")
            (frozenArguments as MutableList<ScalarExpression<*>>).add(ScalarConstant(3))
        }
        assertThrows(UnsupportedOperationException::class.java) {
            @Suppress("UNCHECKED_CAST")
            (frozenCandidates as MutableList<ScalarExpression<*>>).add(ScalarConstant(3))
        }
        assertThrows(UnsupportedOperationException::class.java) {
            @Suppress("UNCHECKED_CAST")
            (frozenCondition as MutableList<BooleanExpression>).add(BooleanConstant(Trivalent.True))
        }
        assertEquals(originalCanonical, plan.canonical())
    }

    @Test
    @DisplayName("should reject cyclic expression trees without recursion / 应在不递归溢出的情况下拒绝循环表达式树")
    fun shouldRejectCyclicExpressionTreesWithoutRecursion() {
        val operands = mutableListOf<BooleanExpression>(BooleanConstant(Trivalent.True))
        val cyclic = AndExpression(operands)
        operands[0] = cyclic
        val plan = RelationalQueryPlan(
            root = QuerySource("orders"),
            predicate = cyclic
        )

        val result = plan.validate()
        assertTrue(result.failed)
        val failure = (result as Failed<*, *, *>).error.value
            as RelationalQueryValidationError
        assertTrue(failure.field.startsWith("predicate.operands"))
        assertDoesNotThrow {
            plan.canonical()
            plan.canonicalHash()
        }
    }

    @Test
    @DisplayName("should preserve array container shapes in snapshots / 应在快照中保留数组容器形状")
    fun shouldPreserveArrayContainerShapesInSnapshots() {
        val objectArray = arrayOf<Any?>("first")
        val intArray = intArrayOf(1)
        val plan = RelationalQueryPlan(
            root = QuerySource("orders"),
            predicate = BooleanCustom(
                mapOf(
                    "objectArray" to objectArray,
                    "intArray" to intArray
                )
            )
        )
        val canonical = plan.canonical()
        objectArray[0] = "changed"
        intArray[0] = 2

        assertEquals(canonical, plan.canonical())
        assertEquals(canonical, plan.copy().canonical())
        assertTrue(plan.canonical().contains("array"))
        assertTrue(plan.canonical().contains("intArray"))
    }

    @Test
    @DisplayName("should snapshot mutable symbol references / 应快照可变符号引用")
    fun shouldSnapshotMutableSymbolReferences() {
        class MutableSymbol(var currentName: String) : Symbol {
            override val name: String get() = currentName
            override val displayName: String get() = currentName
        }

        val symbol = MutableSymbol("orders.total")
        val plan = RelationalQueryPlan(
            root = QuerySource("orders"),
            predicate = Comparison<Any?>(
                operator = ComparisonOperator.Eq,
                left = ScalarSymbolReference<Any?>(symbol),
                right = ScalarConstant<Any?>(1)
            )
        )
        val canonical = plan.canonical()
        symbol.currentName = "orders.changed"

        val frozenSymbol = ((plan.predicate as Comparison<*>).left as ScalarSymbolReference<*>).symbol
        assertNotSame(symbol, frozenSymbol)
        assertEquals("orders.total", frozenSymbol.name)
        assertEquals(canonical, plan.canonical())
    }
}
