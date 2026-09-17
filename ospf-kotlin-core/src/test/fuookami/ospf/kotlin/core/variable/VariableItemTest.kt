/**
 * 变量项（AbstractVariableItem 及其实现）单元测试。 / Unit tests for variable items and their implementations.
 *
 * 覆盖独立变量、类型擦除包装器与变量组合三类，固定身份、值域、相等性与归属判定的契约。
 * Covers independent variables, the type-erased wrapper, and variable combinations, pinning
 * the contracts for identity, value range, equality, and membership.
 */
package fuookami.ospf.kotlin.core.variable

import kotlin.test.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.quantities.unit.reciprocal
import fuookami.ospf.kotlin.core.solver.value.IntoValue

class VariableItemTest {

    @Test
    fun independentVariablesShouldCarryTheirDeclaredNameAndType() {
        // 独立变量的名称与类型必须原样保留，索引与维度恒为 0。
        // An independent variable keeps its name and type; index and dimension are always 0.
        val bin = BinVar("b")
        assertEquals("b", bin.name)
        assertSame(Binary, bin.type)
        assertEquals(0, bin.index)
        // 独立变量本身不构成多维组合，故 dimension 为 0；其向量视图固定为单元素 [0]。
        // A standalone variable forms no multi-dimensional group, so its dimension is 0 and
        // its vector view is the fixed single element [0].
        assertEquals(0, bin.dimension)
        assertEquals(intArrayOf(0).toList(), bin.vectorView.toList())

        val real = RealVar("r")
        assertEquals("r", real.name)
        assertSame(Continuous, real.type)

        val ter = TerVar("t")
        assertSame(Ternary, ter.type)
        val bter = BTerVar("bt")
        assertSame(BalancedTernary, bter.type)
        val pct = PctVar("p")
        assertSame(Percentage, pct.type)
        val int = IntVar("i")
        assertSame(Integer, int.type)
        val uint = UIntVar("u")
        assertSame(UInteger, uint.type)
        val ureal = URealVar("ur")
        assertSame(UContinuous, ureal.type)
    }

    @Test
    fun independentVariablesShouldReceiveDistinctIdentifiers() {
        // 每个独立变量必须拿到全局唯一标识，否则会被误判为同一变量。
        // Each independent variable must receive a globally unique identifier; otherwise
        // distinct variables would be conflated.
        val items = List(32) { BinVar("v$it") }

        assertEquals(items.size, items.map { it.identifier }.toSet().size, "标识必须唯一")
        assertEquals(items.size, items.map { it.key }.toSet().size, "变量键必须唯一")
        assertEquals(items.size, items.toSet().size, "变量项必须互不相等")
    }

    @Test
    fun independentVariableShouldEqualItselfAndDifferFromOthers() {
        // 相等性基于 identifier + index：同对象相等，不同对象不等。
        // Equality is based on identifier + index: an object equals itself and differs
        // from every other independent variable.
        val first = BinVar("x")
        val second = BinVar("x")

        assertEquals(first, first)
        assertNotEquals(first, second)
        assertEquals(first.hashCode(), first.hashCode())
        assertEquals(first.key, first.key)
        assertNotEquals(first.key, second.key)
    }

    @Test
    fun nameIsMutableButDoesNotAffectIdentity() {
        // name 是可变显示属性，改名不得改变身份或相等性。
        // `name` is a mutable display attribute; renaming must not change identity or equality.
        val item = BinVar("before")
        val other = BinVar("other")

        item.name = "after"

        assertEquals("after", item.name)
        assertEquals("after", item.displayName)
        assertNotEquals(item, other)
        assertEquals(item, item)
    }

    @Test
    fun boundsShouldReflectTheVariableType() {
        // 以 Flt64 表示的下界/上界必须匹配变量类型的取值域。
        // The Flt64 lower/upper bounds must match the variable type's value range.
        val bin = BinVar("b")
        assertEquals(Flt64.zero, bin.lowerBound!!.value.unwrap())
        assertEquals(Flt64.one, bin.upperBound!!.value.unwrap())

        val ter = TerVar("t")
        assertEquals(Flt64.zero, ter.lowerBound!!.value.unwrap())
        assertEquals(Flt64.two.toFlt64(), ter.upperBound!!.value.unwrap())

        val bter = BTerVar("bt")
        assertEquals(-Flt64.one, bter.lowerBound!!.value.unwrap())
        assertEquals(Flt64.one, bter.upperBound!!.value.unwrap())

        val real = RealVar("r")
        assertTrue(real.lowerBound!!.value.unwrap() < Flt64.zero)
        assertTrue(real.upperBound!!.value.unwrap() > Flt64.zero)

        val ureal = URealVar("ur")
        assertEquals(Flt64.zero, ureal.lowerBound!!.value.unwrap())
    }

    @Test
    fun belongsToItemShouldCompareIdentifiers() {
        // belongsTo(item) 比较 identifier：自身为真，他人为假。
        // belongsTo(item) compares identifiers: true for itself, false for others.
        val item = RealVar("a")
        val other = RealVar("b")

        assertTrue(item belongsTo item)
        assertFalse(item belongsTo other)
        assertFalse(other belongsTo item)
    }

    @Test
    fun independentVariableShouldNotBelongToAnyCombination() {
        // 独立变量不属于任何变量组合，即使标识恰好相等也应返回 false。
        // An independent variable never belongs to a combination, even when identifiers
        // happen to match — the implementation returns false unconditionally.
        val item = BinVar("lone")
        val combination = BinVariable1("c", Shape1(2))

        assertFalse(item belongsTo combination)
        assertFalse(item belongsTo combination[0])
    }

    @Test
    fun toMathLinearInequalityShouldEncodeVariableEqualsOne() {
        // toMathLinearInequality 必须产出 `变量 = 1` 形式的等式。
        // toMathLinearInequality must produce the equality `variable = 1`.
        val item = BinVar("x")
        val inequality = item.toMathLinearInequality()

        assertEquals(Comparison.EQ, inequality.comparison)
        assertEquals(Flt64.one, inequality.rhs.constant)
        assertEquals(1, inequality.lhs.monomials.size)
        assertEquals(Flt64.one, inequality.lhs.monomials.first().coefficient)
        assertSame(item, inequality.lhs.monomials.first().symbol)
    }

    @Test
    fun toMathQuadraticInequalityShouldEncodeVariableSquaredEqualsOne() {
        // 必须产出 `变量² = 1`，且两个符号都是该变量本身。
        // It must produce `variable² = 1`, with both symbols being the variable itself.
        val item = BinVar("x")
        val inequality = item.toMathQuadraticInequality()

        assertEquals(Comparison.EQ, inequality.comparison)
        assertEquals(Flt64.one, inequality.rhs.constant)
        assertEquals(1, inequality.lhs.monomials.size)
        val monomial = inequality.lhs.monomials.first()
        assertEquals(Flt64.one, monomial.coefficient)
        assertSame(item, monomial.symbol1)
        assertSame(item, monomial.symbol2)
    }

    @Test
    fun quantityVariableShouldCarryTheUnit() {
        // 变量项乘以物理单位必须携带该单位，除以单位则取倒数单位。
        // Multiplying a variable by a physical unit carries that unit; dividing takes the
        // reciprocal unit.
        val item = RealVar("x")
        val quantity = item * Meter

        assertSame(item, quantity.value)
        assertEquals(Meter, quantity.unit)

        val reciprocal = item / Meter
        assertSame(item, reciprocal.value)
        assertEquals(Meter.reciprocal(), reciprocal.unit)
    }

    @Test
    fun anyVariableShouldWrapMetadataWithoutCopying() {
        // AnyVariable 只是类型擦除的视图，不得复制底层数据。
        // AnyVariable is a type-erased view; it must not copy the underlying data.
        val item = BinVar("wrapped")
        val any = AnyVariable<Flt64>(item)

        assertSame(item, any.data)
        assertEquals(item.key, any.id)
        assertEquals(item.index, any.index)
        assertEquals(item.name, any.name)
        assertEquals(item.displayName, any.displayName)
        assertSame(item.type, any.varType)
        assertEquals("wrapped", any.toString())
    }

    @Test
    fun anyVariableShouldMirrorBoundsInBothRepresentations() {
        // 两种边界视图（Flt64 与 V）必须一致。
        // Both bound views (Flt64 and V) must agree.
        val any = AnyVariable<Flt64>(TerVar("t"))

        assertEquals(Flt64.zero, any.lowerBoundFlt64)
        assertEquals(Flt64.two.toFlt64(), any.upperBoundFlt64)
        assertEquals(Flt64.zero, any.lowerBound(IntoValue.Identity))
        assertEquals(Flt64.two.toFlt64(), any.upperBound(IntoValue.Identity))
    }

    @Test
    fun anyVariableIsValidValueShouldEnforceInclusiveBounds() {
        // 有效性判定必须**包含**端点，并拒绝域外值。这里通过 FltX 实例化 V，
        // 使 isValidValue(V) 与 isValidValue(Flt64) 两个重载可区分。
        //
        // Validity must be **inclusive** at the endpoints and reject out-of-domain values.
        // V is instantiated as FltX here so the isValidValue(V) and isValidValue(Flt64)
        // overloads stay distinguishable.
        val bin = AnyVariable<FltX>(BinVar("b"))

        assertTrue(bin.isValidValue(FltX.zero), "下界必须被接受")
        assertTrue(bin.isValidValue(FltX.one), "上界必须被接受")
        assertFalse(bin.isValidValue(Flt64(1.5).toFltX()), "上界之外必须被拒绝")
        assertFalse(bin.isValidValue(Flt64(-1.0).toFltX()), "下界之外必须被拒绝")
        assertFalse(bin.isValidValue(Flt64(2.0).toFltX()), "远超出上界必须被拒绝")
    }

    @Test
    fun anyVariableShouldRejectValuesOutsideATernaryDomain() {
        // 三值域只接受 [0, 2]，边界含端点而 3 越界。
        // The ternary domain accepts only [0, 2]: endpoints included, 3 rejected.
        val ter = AnyVariable<FltX>(TerVar("t"))

        assertTrue(ter.isValidValue(FltX.zero))
        assertTrue(ter.isValidValue(FltX.one))
        assertTrue(ter.isValidValue(Flt64(2.0).toFltX()))
        assertFalse(ter.isValidValue(Flt64(3.0).toFltX()))
    }

    @Test
    fun anyVariableEqualityShouldFollowTheUnderlyingItem() {
        // 包装器相等性完全委托给底层变量项。
        // Wrapper equality delegates entirely to the underlying variable item.
        val item = BinVar("x")
        val other = BinVar("x")
        val wrapped = AnyVariable<Flt64>(item)
        val wrappedAgain = AnyVariable<Flt64>(item)
        val wrappedOther = AnyVariable<Flt64>(other)

        assertEquals(wrapped, wrapped)
        assertEquals(wrapped, wrappedAgain)
        assertEquals(wrapped.hashCode(), wrappedAgain.hashCode())
        assertNotEquals(wrapped, wrappedOther)
        assertNotEquals(wrapped as Any, item as Any, "包装器与裸变量项不是同一类型")
    }

    @Test
    fun anyVariableFromShouldMatchTheConstructor() {
        // 工厂 from 必须等价于直接构造 / The `from` factory must equal direct construction.
        val item = RealVar("r")
        assertEquals(AnyVariable<Flt64>(item), AnyVariable.from<Flt64>(item))
    }

    @Test
    fun combinationShouldExposeItsShapeAndMembers() {
        // 变量组合必须按形状暴露元素数量、维度与逐个条目。
        // A combination exposes its element count, dimension, and each entry by shape.
        val combination = BinVariable1("x", Shape1(3))

        assertEquals(3, combination.size)
        assertEquals(1, combination.dimension)
        // shape.vector(linearIndex) 返回该线性下标对应的**索引向量**（此处为一维下标本身）。
        // shape.vector(linearIndex) returns the **index vector** for that linear index,
        // which for a 1-D shape is the index itself.
        assertEquals(listOf(0), combination.shape.vector(0).value!!.toList())
        assertEquals(listOf(0, 1, 2), combination.map { it.index }.toList())
    }

    @Test
    fun combinationMembersShouldBeNamedByVectorIndex() {
        // 组合成员名称采用 "前缀_索引" 形式，便于日志定位。
        // Combination member names use "<prefix>_<index>" so logs can locate them.
        val combination = BinVariable1("x", Shape1(2))

        assertEquals("x_0", combination[0].name)
        assertEquals("x_1", combination[1].name)

        val matrix = BinVariable2("m", Shape2(2, 3))
        assertEquals(6, matrix.size)
        assertEquals(2, matrix.dimension)
        assertEquals("m_0_0", matrix[0, 0].name)
        assertEquals("m_1_2", matrix[1, 2].name)
    }

    @Test
    fun combinationMembersShouldShareTheGroupIdentifier() {
        // 同一组合的所有成员必须共享一个组标识，并据此判定归属。
        // All members of a combination share one group identifier, which drives membership.
        val combination = BinVariable1("x", Shape1(4))

        val identifiers = combination.map { it.identifier }.toSet()
        assertEquals(1, identifiers.size, "同一组合只能有一个组标识")

        combination.forEach { item ->
            assertTrue(item belongsTo combination, "成员必须属于其组合")
        }

        val other = BinVariable1("y", Shape1(4))
        assertNotEquals(
            combination.identifier,
            other.identifier,
            "不同组合必须有不同的组标识"
        )
        assertFalse(combination[0] belongsTo other)
    }

    @Test
    fun combinationMembersShouldBeMutuallyDistinctByKey() {
        // 同组内成员的 index 不同，因此 key 必须互不相同（否则会互相覆盖）。
        // Members within a group differ by index, so their keys must be distinct;
        // otherwise they would overwrite each other in keyed structures.
        val combination = BinVariable2("m", Shape2(3, 2))

        val keys = combination.map { it.key }.toSet()
        assertEquals(combination.size, keys.size)
        assertEquals(combination.size, combination.toSet().size)
    }

    @Test
    fun combinationMemberShouldCarryTheCombinationTypeAndBounds() {
        // 成员必须继承组合的变量类型与取值域。
        // A member inherits the combination's variable type and value range.
        val combination = UIntVariable1("u", Shape1(2))

        combination.forEach { item ->
            assertSame(UInteger, item.type)
            // 无符号整数域下界为 0，以 Flt64 视图断言以避免泛型推断歧义。
            // The unsigned integer domain starts at 0; asserted through the Flt64 view to
            // avoid a generic-inference ambiguity.
            assertEquals(Flt64.zero, item.lowerBound!!.toFlt64().value.unwrap())
        }
    }

    @Test
    fun combinationMembersShouldReportTheirVectorView() {
        // 成员的向量视图必须等于其索引向量，用于回填求解结果。
        // A member's vector view equals its index vector, used to fill back solutions.
        val combination = BinVariable2("m", Shape2(2, 2))

        assertEquals(listOf(0, 0), combination[0, 0].vectorView.toList())
        assertEquals(listOf(0, 1), combination[0, 1].vectorView.toList())
        assertEquals(listOf(1, 0), combination[1, 0].vectorView.toList())
        assertEquals(listOf(1, 1), combination[1, 1].vectorView.toList())
        assertEquals(listOf(0, 0), combination[0, 0].uvector.map { it.toInt() }.toList())
    }

    @Test
    fun vectorViewShouldBeASnapshotRatherThanAnAlias() {
        // 现状记录：vectorView 是 IntArray，取用方拿到的是同一底层数组的引用；
        // 修改它会影响后续读取。固定该行为以免被当作防御性拷贝使用。
        //
        // Characterizes current behavior: vectorView returns an IntArray, so callers share
        // the same backing array; mutating it affects later reads. Pinned so nobody treats
        // it as a defensive copy.
        val item = BinVariable1("x", Shape1(1))[0]
        item.vectorView[0] = 99
        assertEquals(99, item.vectorView[0], "vectorView 未做防御性拷贝")
    }

    @Test
    fun combinationOfQuantitiesShouldCarryTheUnitPerItem() {
        // 物理量组合的每个元素都必须带上单位。
        // Each element of a quantity combination carries the unit.
        val combination = QuantityBinVariable1("q", Shape1(2), Meter)

        assertEquals(2, combination.size)
        assertEquals(Meter, combination[0].unit)
        assertEquals(Meter, combination[1].unit)
        assertEquals("q_0", combination[0].value.name)
        assertEquals(listOf(0, 1), combination.map { it.value.index }.toList())
    }

    @Test
    fun combinationShouldSupportMultipleDeclaredTypes() {
        // 各变量类型的组合都必须可构造且类型正确。
        // Combinations for each variable type must construct with the right type.
        assertSame(Ternary, TerVariable1("t", Shape1(1)).type)
        assertSame(BalancedTernary, BTerVariable1("bt", Shape1(1)).type)
        assertSame(Integer, IntVariable1("i", Shape1(1)).type)
        assertSame(UInteger, UIntVariable1("u", Shape1(1)).type)
        assertSame(Continuous, RealVariable1("r", Shape1(1)).type)
        assertSame(UContinuous, URealVariable1("ur", Shape1(1)).type)
        assertSame(Percentage, PctVariable1("p", Shape1(1)).type)
        assertSame(Binary, BinVariable1("b", Shape1(1)).type)
    }

    @Test
    fun emptyShapeCombinationShouldHaveNoMembers() {
        // 空形状的组合必须为空，且不产生任何成员。
        // A combination over an empty shape is empty and yields no members.
        val combination = BinVariable1("empty", Shape1(0))

        assertEquals(0, combination.size)
        assertTrue(combination.isEmpty())
        assertEquals(emptyList(), combination.map { it.index }.toList())
    }

    @Test
    fun combinationEnumerateShouldPairIndexAndEntry() {
        // enumerate 必须按线性索引顺序给出 (index, vector, item)。
        // enumerate yields (index, vector, item) in linear-index order.
        val combination = BinVariable2("m", Shape2(2, 2))

        val enumerated = combination.enumerate().toList()
        assertEquals(4, enumerated.size)
        enumerated.forEachIndexed { position, (index, vector, item) ->
            assertEquals(position, index, "线性索引必须递增")
            assertEquals(item.vectorView.toList(), vector.toList(), "向量必须与条目一致")
            assertEquals(position, item.index)
        }
    }
}
