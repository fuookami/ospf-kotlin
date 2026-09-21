/**
 * 影子价格模型单元测试。 / Unit tests for the shadow price model.
 *
 * ShadowPriceMap 的累加、收缩与提取器语义直接影响列生成/Benders 的对偶信息，
 * 这些行为此前无任何测试覆盖。
 *
 * ShadowPriceMap's accumulation, shrinking, and extractor semantics feed dual information
 * into column generation and Benders; none of it was covered before.
 *
 * 注意：基类 `ShadowPriceKey` 是**普通类且不覆写 equals/hashCode**，因此以它直接作键时
 * 只有同一个实例才能命中。真实用法（见 gantt 的 FleetBalanceLimit / FlightLinkLimit）
 * 都是继承并用 `data class` 提供结构化相等性，测试据此建模。
 *
 * Note: the base `ShadowPriceKey` is a plain class that does **not** override
 * equals/hashCode, so a bare key is only matched by the very same instance. Real usage
 * (e.g. gantt's FleetBalanceLimit / FlightLinkLimit) subclasses it with a `data class` to
 * get structural equality, which is how these tests model keys.
 */
package fuookami.ospf.kotlin.framework.model

import kotlin.reflect.KClass
import kotlin.test.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

class ShadowPriceTest {

    /** 结构化的容量限制键 / Structural capacity-limit key. */
    private data class CapacityKey(val id: String) : ShadowPriceKey(CapacityKey::class)

    /** 结构化的需求限制键 / Structural demand-limit key. */
    private data class DemandKey(val id: String) : ShadowPriceKey(DemandKey::class)

    /** 具体影子价格映射，用于实例化抽象基类。 / Concrete map instantiating the abstract base. */
    private class TestShadowPriceMap : AbstractShadowPriceMap<String, TestShadowPriceMap>()

    @Test
    fun baseKeyShouldNotProvideStructuralEquality() {
        // 现状记录：基类 ShadowPriceKey 只有引用相等性，作为键时必须传入同一实例。
        // 固定该行为，以免误以为可以按 limit 类型构造等值键。
        //
        // Characterizes current behavior: the base ShadowPriceKey only has reference
        // equality, so a key must be the very same instance. Pinned so nobody assumes keys
        // can be rebuilt from the limit type.
        val first = ShadowPriceKey(String::class)
        val second = ShadowPriceKey(String::class)

        assertSame(first, first)
        assertNotEquals(first as Any, second as Any, "基类键不做结构化比较")
        assertNotEquals(first.hashCode(), second.hashCode())
    }

    @Test
    fun dataClassSubclassShouldProvideStructuralEquality() {
        // 真实用法：用 data class 子类提供结构化相等性，同值键相等。
        // Real usage: a data-class subclass gives structural equality, so equal-value keys match.
        assertEquals(CapacityKey("c1"), CapacityKey("c1"))
        assertNotEquals(CapacityKey("c1"), CapacityKey("c2"))
        assertEquals(CapacityKey("c1").hashCode(), CapacityKey("c1").hashCode())
        assertEquals(CapacityKey::class, CapacityKey("c1").limit)
    }

    @Test
    fun keysOfDifferentLimitTypesShouldNotBeEqual() {
        // 不同限制类型的键必须不相等，即使字段值相同。
        // Keys of different limit types are never equal, even with identical fields.
        assertNotEquals(CapacityKey("x") as Any, DemandKey("x") as Any)
    }

    @Test
    fun priceShouldCarryKeyAndValue() {
        // ShadowPrice 必须保留键与数值，且 toString 可读。
        // ShadowPrice keeps its key and value, with a readable toString.
        val target = CapacityKey("c1")
        val price = ShadowPrice(target, Flt64(2.5))

        assertEquals(target, price.key)
        assertEquals(Flt64(2.5), price.price)
        assertTrue(price.toString().isNotEmpty())
    }

    @Test
    fun priceEqualityShouldConsiderKeyAndValue() {
        // 相等性必须同时比较键与值 / Equality considers both key and value.
        assertEquals(
            ShadowPrice(CapacityKey("c1"), Flt64.one),
            ShadowPrice(CapacityKey("c1"), Flt64.one)
        )
        assertNotEquals(
            ShadowPrice(CapacityKey("c1"), Flt64.one),
            ShadowPrice(CapacityKey("c1"), Flt64.two)
        )
        assertNotEquals(
            ShadowPrice(CapacityKey("c1"), Flt64.one),
            ShadowPrice(CapacityKey("c2"), Flt64.one)
        )
    }

    @Test
    fun putShouldReplaceTheValueForTheSameKey() {
        // put 是覆盖语义：同一键重复写入只保留最后一个值。
        // `put` overwrites: writing the same key twice keeps only the last value.
        val map = TestShadowPriceMap()
        val target = CapacityKey("c1")

        map.put(ShadowPrice(target, Flt64.one))
        map.put(ShadowPrice(target, Flt64(9.0)))

        assertEquals(1, map.map.size)
        assertEquals(Flt64(9.0), map[target]!!.price)
    }

    @Test
    fun putOrAddShouldAccumulateForTheSameKey() {
        // putOrAdd 是累加语义：同一键重复写入必须求和，这是列生成里同一限制被多条约束
        // 共享时的关键行为。
        //
        // `putOrAdd` accumulates: repeated writes for the same key must sum. This matters in
        // column generation where one limit is shared by several constraints.
        val map = TestShadowPriceMap()
        val target = CapacityKey("c1")

        map.putOrAdd(ShadowPrice(target, Flt64.one))
        map.putOrAdd(ShadowPrice(target, Flt64.two))
        map.putOrAdd(ShadowPrice(target, Flt64(0.5)))

        assertEquals(1, map.map.size)
        assertEquals(Flt64(3.5), map[target]!!.price)
    }

    @Test
    fun putOrAddShouldStartFromZeroWhenAbsent() {
        // 首次累加必须等价于直接写入，不得引入额外偏移。
        // The first accumulation equals a plain write, with no extra offset.
        val map = TestShadowPriceMap()
        val target = CapacityKey("c1")

        map.putOrAdd(ShadowPrice(target, Flt64(2.5)))

        assertEquals(Flt64(2.5), map[target]!!.price)
    }

    @Test
    fun putOrAddShouldKeepAccumulationPerKey() {
        // 累加必须按键隔离，不同键不能相互污染。
        // Accumulation is per key; distinct keys must not contaminate each other.
        val map = TestShadowPriceMap()
        val capacity = CapacityKey("c1")
        val demand = DemandKey("d1")

        map.putOrAdd(ShadowPrice(capacity, Flt64.one))
        map.putOrAdd(ShadowPrice(demand, Flt64(10.0)))
        map.putOrAdd(ShadowPrice(capacity, Flt64.two))

        assertEquals(2, map.map.size)
        assertEquals(Flt64(3.0), map[capacity]!!.price)
        assertEquals(Flt64(10.0), map[demand]!!.price)
    }

    @Test
    fun removeShouldDropOnlyThatKey() {
        // remove 只删除指定键，其余条目保留。
        // `remove` drops only the given key and keeps the rest.
        val map = TestShadowPriceMap()
        val kept = CapacityKey("c1")
        val removed = DemandKey("d1")
        map.put(ShadowPrice(kept, Flt64.one))
        map.put(ShadowPrice(removed, Flt64.two))

        map.remove(removed)

        assertEquals(1, map.map.size)
        assertNull(map[removed])
        assertEquals(Flt64.one, map[kept]!!.price)
    }

    @Test
    fun removingAnAbsentKeyShouldBeANoOp() {
        // 删除不存在的键不得抛异常或影响其它条目。
        // Removing an absent key must neither throw nor disturb other entries.
        val map = TestShadowPriceMap()
        map.put(ShadowPrice(CapacityKey("c1"), Flt64.one))

        map.remove(DemandKey("absent"))

        assertEquals(1, map.map.size)
    }

    @Test
    fun shrinkShouldDropExactlyTheZeroValuedPrices() {
        // shrink 必须移除零值影子价格，这是"只有非零对偶才有信息量"的编码。
        // `shrink` drops exactly the zero-valued prices: only non-zero duals carry information.
        val map = TestShadowPriceMap()
        val zero = CapacityKey("zero")
        val positive = CapacityKey("positive")
        val negative = CapacityKey("negative")
        map.put(ShadowPrice(zero, Flt64.zero))
        map.put(ShadowPrice(positive, Flt64.one))
        map.put(ShadowPrice(negative, Flt64(-1.0)))

        map.shrink()

        assertEquals(2, map.map.size, "零值应被移除，正负非零值应保留")
        assertNull(map[zero])
        assertEquals(Flt64.one, map[positive]!!.price)
        assertEquals(Flt64(-1.0), map[negative]!!.price)
    }

    @Test
    fun shrinkOnAnEmptyMapShouldBeSafe() {
        // 空映射收缩不得报错 / Shrinking an empty map must be safe.
        val map = TestShadowPriceMap()
        map.shrink()
        assertTrue(map.map.isEmpty())
    }

    @Test
    fun indexerShouldMirrorPutAndGet() {
        // 下标读写必须与 put/get 一致 / Indexed access agrees with put/get.
        val map = TestShadowPriceMap()
        val target = CapacityKey("c1")

        map[target] = ShadowPrice(target, Flt64(4.0))

        assertEquals(Flt64(4.0), map[target]!!.price)
        assertNull(map[DemandKey("d1")])
    }

    @Test
    fun invokeShouldSumAllRegisteredExtractors() {
        // 调用映射必须把所有已注册提取器的结果按 Flt64 求和。
        // Invoking the map sums every registered extractor's result as Flt64.
        val map = TestShadowPriceMap()
        map.put(ShadowPriceExtractor<String, TestShadowPriceMap> { _, _ -> Flt64.one })
        map.put(ShadowPriceExtractor<String, TestShadowPriceMap> { _, _ -> Flt64(2.5) })

        assertEquals(Flt64(3.5), map("any"))
    }

    @Test
    fun invokeWithoutExtractorsShouldBeZero() {
        // 未注册提取器时调用必须得到 0（求和单位元），而不是抛异常。
        // With no extractors, invoking yields 0 (the sum identity) rather than throwing.
        val map = TestShadowPriceMap()

        assertEquals(Flt64.zero, map("any"))
    }

    @Test
    fun invokeShouldPassTheArgumentThroughToExtractors() {
        // 提取器必须收到调用方传入的参数，用于按业务自变量取值。
        // Extractors receive the caller's argument so they can look up by business key.
        val map = TestShadowPriceMap()
        val seen = ArrayList<String>()
        map.put(
            ShadowPriceExtractor<String, TestShadowPriceMap> { _, args ->
                seen += args
                Flt64.zero
            }
        )

        map("alpha")
        map("beta")

        assertEquals(listOf("alpha", "beta"), seen)
    }

    @Test
    fun extractorsShouldBeIndependentOfStoredPrices() {
        // 提取器求和与已存储的影子价格互不影响，两者是独立通道。
        // Extractor summation and stored prices are independent channels.
        val map = TestShadowPriceMap()
        val target = CapacityKey("c1")
        map.put(ShadowPrice(target, Flt64(100.0)))
        map.put(ShadowPriceExtractor<String, TestShadowPriceMap> { _, _ -> Flt64.one })

        assertEquals(Flt64.one, map("arg"), "只反映提取器")
        assertEquals(Flt64(100.0), map[target]!!.price, "存储值不受影响")
    }

    @Test
    fun shrinkShouldNotRemovePricesAddedByExtractors() {
        // shrink 只作用于存储的 map，不得清除已注册的提取器。
        // `shrink` only touches stored prices; registered extractors survive.
        val map = TestShadowPriceMap()
        map.put(ShadowPrice(CapacityKey("zero"), Flt64.zero))
        map.put(ShadowPriceExtractor<String, TestShadowPriceMap> { _, _ -> Flt64(7.0) })

        map.shrink()

        assertTrue(map.map.isEmpty(), "零值存储项被移除")
        assertEquals(Flt64(7.0), map("arg"), "提取器仍然生效")
    }
}
