package fuookami.ospf.kotlin.core.intermediate_model

import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.core.model.mechanism.convertMechanismModelToF64
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Quadratic
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConvertMechanismModelTest {
    @Test
    fun shouldConvertLinearMechanismModelToF64() {
        val x = RealVar("x")
        val tokens = AutoTokenTable<Flt64>(Linear, false)
        tokens.add(x)

        val metaModel = LinearMetaModel<Flt64>(name = "test-linear")
        val model = LinearMechanismModel<Flt64>(
            parent = metaModel,
            name = "test-mech",
            constraints = emptyList(),
            objectFunction = SingleObject(ObjectCategory.Minimum, emptyList<LinearSubObject<Flt64>>()),
            tokens = tokens
        )

        val result = convertMechanismModelToF64(model)
        assertTrue(result.ok)
        assertEquals(0, result.value!!.constraints.size)

        metaModel.close()
    }

    @Test
    fun shouldConvertQuadraticMechanismModelToF64() {
        val x = RealVar("x")
        val tokens = AutoTokenTable<Flt64>(Quadratic, false)
        tokens.add(x)

        val metaModel = QuadraticMetaModel<Flt64>(name = "test-quadratic")
        val model = QuadraticMechanismModel<Flt64>(
            parent = metaModel,
            name = "test-mech",
            constraints = emptyList(),
            objectFunction = SingleObject(ObjectCategory.Minimum, emptyList<QuadraticSubObject<Flt64>>()),
            tokens = tokens
        )

        val result = convertMechanismModelToF64(model)
        assertTrue(result.ok)
        assertEquals(0, result.value!!.constraints.size)

        metaModel.close()
    }
}
