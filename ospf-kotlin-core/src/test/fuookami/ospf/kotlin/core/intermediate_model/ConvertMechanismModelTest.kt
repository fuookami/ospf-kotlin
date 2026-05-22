package fuookami.ospf.kotlin.core.intermediate_model

import fuookami.ospf.kotlin.core.test.flt64TestConverter
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.mechanism.LinearMechanismModel
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.LinearSubObject
import fuookami.ospf.kotlin.core.model.mechanism.QuadraticMechanismModel
import fuookami.ospf.kotlin.core.model.mechanism.QuadraticMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.QuadraticSubObject
import fuookami.ospf.kotlin.core.model.mechanism.SingleObject
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.core.model.mechanism.convertMechanismModelToFlt64
import fuookami.ospf.kotlin.core.token.AutoTokenTable
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Quadratic
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConvertMechanismModelTest {
    @Test
    fun shouldConvertLinearMechanismModelToFlt64() {
        val x = RealVar("x")
        val tokens = AutoTokenTable<Flt64>(Linear, false)
        tokens.add(x)

        val metaModel = LinearMetaModel<Flt64>(name = "test-linear", converter = flt64TestConverter)
        val model = LinearMechanismModel<Flt64>(
            parent = metaModel,
            name = "test-mech",
            constraints = emptyList(),
            objectFunction = SingleObject(ObjectCategory.Minimum, emptyList<LinearSubObject<Flt64>>()),
            tokens = tokens
        )

        val result = convertMechanismModelToFlt64(model)
        assertTrue(result.ok)
        assertEquals(0, result.value!!.constraints.size)

        metaModel.close()
    }

    @Test
    fun shouldConvertQuadraticMechanismModelToFlt64() {
        val x = RealVar("x")
        val tokens = AutoTokenTable<Flt64>(Quadratic, false)
        tokens.add(x)

        val metaModel = QuadraticMetaModel<Flt64>(name = "test-quadratic", converter = flt64TestConverter)
        val model = QuadraticMechanismModel<Flt64>(
            parent = metaModel,
            name = "test-mech",
            constraints = emptyList(),
            objectFunction = SingleObject(ObjectCategory.Minimum, emptyList<QuadraticSubObject<Flt64>>()),
            tokens = tokens
        )

        val result = convertMechanismModelToFlt64(model)
        assertTrue(result.ok)
        assertEquals(0, result.value!!.constraints.size)

        metaModel.close()
    }
}
