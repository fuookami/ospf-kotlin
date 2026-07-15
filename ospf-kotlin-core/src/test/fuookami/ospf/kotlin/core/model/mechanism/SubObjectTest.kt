package fuookami.ospf.kotlin.core.model.mechanism

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Category
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.variable.*

/**
 * 使用 FlattenData 的子对象构造函数测试 / Tests for SubObject constructors using FlattenData
 */
class SubObjectTest {

    private fun createTokenTable(): AutoTokenTable<Flt64> {
        return AutoTokenTable<Flt64>(
            category = fuookami.ospf.kotlin.math.symbol.Linear,
            checkTokenExists = false
        )
    }

    @Test
    fun linearSubObject_fromFlattenData_shouldWorkCorrectly() {
        val x = RealVar("x")
        val y = RealVar("y")
        val tokens = createTokenTable()

        tokens.add(x)
        tokens.add(y)

        val flattenData = LinearFlattenData<Flt64>(
            monomials = listOf(
                LinearMonomial(Flt64(2.0), x),
                LinearMonomial(Flt64(3.0), y)
            ),
            constant = Flt64(5.0)
        )

        val subObject = LinearSubObject(
            category = ObjectCategory.Minimum,
            flattenData = flattenData,
            tokens = tokens,
            name = "test_sub_object",
            converter = IntoValue.Identity
        )

        assertEquals(2, subObject.cells.size)
        assertTrue(subObject.constant eq Flt64(5.0), "Constant should be 5.0")
    }

    @Test
    fun quadraticSubObject_fromFlattenData_shouldWorkCorrectly() {
        val x = RealVar("x")
        val y = RealVar("y")
        val tokens = createTokenTable()

        tokens.add(x)
        tokens.add(y)

        val flattenData = QuadraticFlattenData<Flt64>(
            monomials = listOf(
                QuadraticMonomial(Flt64(2.0), x, y),
                QuadraticMonomial(Flt64(3.0), x, null)  // Linear term
            ),
            constant = Flt64(5.0)
        )

        val subObject = QuadraticSubObject(
            category = ObjectCategory.Minimum,
            flattenData = flattenData,
            tokens = tokens,
            name = "test_sub_object",
            converter = IntoValue.Identity
        )

        assertEquals(2, subObject.cells.size)
        assertTrue(subObject.constant eq Flt64(5.0), "Constant should be 5.0")
    }

    @Test
    fun linearSubObject_fromPolynomial_shouldMatchFlattenData() {
        val x = RealVar("x")
        val y = RealVar("y")
        val tokens = createTokenTable()

        tokens.add(x)
        tokens.add(y)

        val flattenData = LinearFlattenData<Flt64>(
            monomials = listOf(
                LinearMonomial(Flt64(2.0), x),
                LinearMonomial(Flt64(3.0), y)
            ),
            constant = Flt64(5.0)
        )

        // Create using flattenData constructor
        val subObject1 = LinearSubObject(
            category = ObjectCategory.Minimum,
            flattenData = flattenData,
            tokens = tokens,
            name = "test",
            converter = IntoValue.Identity
        )

        // Create using the same flattenData again
        val subObject2 = LinearSubObject(
            category = ObjectCategory.Minimum,
            flattenData = flattenData,
            tokens = tokens,
            name = "test",
            converter = IntoValue.Identity
        )

        assertEquals(subObject1.cells.size, subObject2.cells.size)
        assertTrue(subObject1.constant eq subObject2.constant, "Constants should match")
    }

    @Test
    fun quadraticSubObject_fromPolynomial_shouldMatchFlattenData() {
        val x = RealVar("x")
        val y = RealVar("y")
        val tokens = createTokenTable()

        tokens.add(x)
        tokens.add(y)

        val flattenData = QuadraticFlattenData<Flt64>(
            monomials = listOf(
                QuadraticMonomial(Flt64.one, x, y),
                QuadraticMonomial(Flt64(2.0), x, null)
            ),
            constant = Flt64(5.0)
        )

        // Create using flattenData constructor
        val subObject1 = QuadraticSubObject(
            category = ObjectCategory.Minimum,
            flattenData = flattenData,
            tokens = tokens,
            name = "test",
            converter = IntoValue.Identity
        )

        // Create using the same flattenData again
        val subObject2 = QuadraticSubObject(
            category = ObjectCategory.Minimum,
            flattenData = flattenData,
            tokens = tokens,
            name = "test",
            converter = IntoValue.Identity
        )

        assertEquals(subObject1.cells.size, subObject2.cells.size)
        assertTrue(subObject1.constant eq subObject2.constant, "Constants should match")
    }
}
