package fuookami.ospf.kotlin.core.model.constraint_programming

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.MathConstraint
import fuookami.ospf.kotlin.core.model.mechanism.MetaConstraintGroup
import fuookami.ospf.kotlin.math.algebra.number.Flt64

class ConstraintGroupRegistryTest {
    @Test
    fun metaModelShouldExposeConstraintGroupRegistryWithoutChangingGroupQueries() {
        val model = LinearMetaModel<Flt64>(
            name = "constraint-group-registry",
            objectCategory = ObjectCategory.Minimum,
            converter = fuookami.ospf.kotlin.core.solver.value.IntoValue.fromConverter(Flt64)
        )
        val group = TestConstraintGroup("probe")
        try {
            val registry: ConstraintGroupRegistry = model
            registry.registerConstraintGroup(group)

            assertTrue(model is ConstraintGroupRegistry)
            assertEquals(emptyList<MathConstraint>(), model.constraintsOfGroup(group))
        } finally {
            model.close()
        }
    }

    private class TestConstraintGroup(override val name: String) : MetaConstraintGroup
}
