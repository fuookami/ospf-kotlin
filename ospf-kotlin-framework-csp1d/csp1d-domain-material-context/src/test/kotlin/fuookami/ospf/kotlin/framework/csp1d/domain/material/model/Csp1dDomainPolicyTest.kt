package fuookami.ospf.kotlin.framework.csp1d.domain.material.model

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.Meter

class Csp1dDomainPolicyTest {

    // region test data helpers

    private fun material(id: String = "m", upperBound: Double = 2.0, lowerBound: Double = 0.5): Material<Flt64> {
        return Material(
            id = id,
            name = "material-$id",
            widthRange = WidthRange(
                width = QuantityRange(
                    lowerBound = Quantity(Flt64(lowerBound), Meter),
                    upperBound = Quantity(Flt64(upperBound), Meter)
                ),
                step = Quantity(Flt64(0.1), Meter)
            )
        )
    }

    private fun product(id: String, widths: List<Quantity<Flt64>>): Product<Flt64> {
        return Product(id = id, name = "product-$id", width = widths)
    }

    // endregion

    // region overridesWidthFeasibility guard

    /**
     * 非宽度类 domain policy（overridesWidthFeasibility = false）不应绕过 canCut。
     * Non-width domain policy should NOT bypass canCut.
     */
    @Test
    fun nonWidthPolicy_doesNotBypassCanCut() {
        val m = material(upperBound = 2.0)
        val p = product("p1", listOf(Quantity(Flt64(1.0), Meter)))
        val overWidth = Quantity(Flt64(3.0), Meter) // exceeds material upperBound 2.0

        // A domain policy that does NOT override width feasibility (default)
        val feasibilityOnlyPolicy = object : Csp1dDomainPolicy<Flt64> {
            override val name = "feasibility-only"
            // overridesWidthFeasibility defaults to false
        }

        // widthFeasibilityCheckFromPolicies should return null
        val check = widthFeasibilityCheckFromPolicies(listOf(feasibilityOnlyPolicy), Flt64(0.0))
        assertNull(check, "Non-width-overriding policy should produce null widthCheck, preserving canCut")
    }

    /**
     * 多个非宽度 policy 混合时仍不绕过 canCut。
     * Multiple non-width policies still should not bypass canCut.
     */
    @Test
    fun multipleNonWidthPolicies_doNotBypassCanCut() {
        val policy1 = object : Csp1dDomainPolicy<Flt64> {
            override val name = "policy-1"
        }
        val policy2 = object : Csp1dDomainPolicy<Flt64> {
            override val name = "policy-2"
        }

        val check = widthFeasibilityCheckFromPolicies(listOf(policy1, policy2), Flt64(0.0))
        assertNull(check, "Multiple non-width policies should still produce null widthCheck")
    }

    // endregion

    // region width-overriding policy

    /**
     * 宽度接管 policy（overridesWidthFeasibility = true）可以放宽宽度：允许超出 canCut 范围。
     * Width-overriding policy can relax width: allow beyond canCut range.
     */
    @Test
    fun widthOverridingPolicy_canRelaxWidth() {
        val m = material(upperBound = 2.0)
        val p = product("p1", listOf(Quantity(Flt64(1.0), Meter)))
        val overWidth = Quantity(Flt64(3.0), Meter) // exceeds material upperBound 2.0

        // Policy that overrides width and always allows
        val relaxingPolicy = object : Csp1dDomainPolicy<Flt64> {
            override val name = "relax-width"
            override val overridesWidthFeasibility = true
            override fun isWidthFeasible(context: Csp1dDomainCalculationContext<Flt64>): Boolean = true
        }

        val check = widthFeasibilityCheckFromPolicies(listOf(relaxingPolicy), Flt64(0.0))
        assertNotNull(check, "Width-overriding policy should produce non-null widthCheck")

        // canCut rejects this width, but widthCheck should accept it
        assertFalse(m.widthRange.canCut(overWidth), "Sanity: canCut should reject overWidth")
        assertTrue(check(m, p, overWidth), "Width-overriding policy should allow overWidth")
    }

    /**
     * 宽度接管 policy 可以收窄宽度：拒绝 canCut 允许的宽度。
     * Width-overriding policy can narrow width: reject width that canCut allows.
     */
    @Test
    fun widthOverridingPolicy_canNarrowWidth() {
        val m = material(upperBound = 2.0)
        val p = product("p1", listOf(Quantity(Flt64(1.0), Meter)))
        val normalWidth = Quantity(Flt64(1.0), Meter) // within canCut range

        // Policy that overrides width and rejects this product
        val narrowingPolicy = object : Csp1dDomainPolicy<Flt64> {
            override val name = "narrow-width"
            override val overridesWidthFeasibility = true
            override fun isWidthFeasible(context: Csp1dDomainCalculationContext<Flt64>): Boolean = false
        }

        val check = widthFeasibilityCheckFromPolicies(listOf(narrowingPolicy), Flt64(0.0))
        assertNotNull(check, "Width-overriding policy should produce non-null widthCheck")

        // canCut accepts this width, but widthCheck should reject it
        assertTrue(m.widthRange.canCut(normalWidth), "Sanity: canCut should accept normalWidth")
        assertFalse(check(m, p, normalWidth), "Width-overriding policy should reject normalWidth")
    }

    /**
     * 混合 policy：宽度接管 policy 与非宽度 policy 共存时，只有宽度接管 policy 参与 widthCheck。
     * Mixed policies: only width-overriding policies participate in widthCheck.
     */
    @Test
    fun mixedPolicies_onlyWidthOverridingParticipate() {
        val m = material(upperBound = 2.0)
        val p = product("p1", listOf(Quantity(Flt64(1.0), Meter)))
        val overWidth = Quantity(Flt64(3.0), Meter)

        // Non-width policy that rejects everything (should NOT affect widthCheck)
        val rejectAllPolicy = object : Csp1dDomainPolicy<Flt64> {
            override val name = "reject-all"
            override fun isWidthFeasible(context: Csp1dDomainCalculationContext<Flt64>): Boolean = false
        }

        // Width-overriding policy that allows everything
        val allowWidthPolicy = object : Csp1dDomainPolicy<Flt64> {
            override val name = "allow-width"
            override val overridesWidthFeasibility = true
            override fun isWidthFeasible(context: Csp1dDomainCalculationContext<Flt64>): Boolean = true
        }

        val check = widthFeasibilityCheckFromPolicies(listOf(rejectAllPolicy, allowWidthPolicy), Flt64(0.0))
        assertNotNull(check)

        // rejectAllPolicy's isWidthFeasible=false should NOT participate in widthCheck
        // because it does not declare overridesWidthFeasibility=true
        assertTrue(check(m, p, overWidth), "Non-width policy should not affect widthCheck result")
    }

    // endregion

    // region Material.enabledWithoutWidthCheck

    /**
     * enabledWithoutWidthCheck 只检查 material id 和 machine id，不检查宽度。
     * enabledWithoutWidthCheck only checks material id and machine id, not width.
     */
    @Test
    fun enabledWithoutWidthCheck_skipsWidthCheck() {
        val m = material(id = "m1", upperBound = 2.0)
        val p = product("p1", listOf(Quantity(Flt64(3.0), Meter))) // over-width product
        val overWidth = Quantity(Flt64(3.0), Meter)

        val plan = CuttingPlan(
            id = "test-plan",
            material = m,
            slices = listOf(CuttingPlanSlice(production = p, width = overWidth)),
            demandContributions = emptyList()
        )

        // enabled rejects because canCut fails
        assertFalse(m.enabled(plan), "enabled() should reject over-width plan")

        // enabledWithoutWidthCheck accepts because it skips canCut
        assertTrue(m.enabledWithoutWidthCheck(plan), "enabledWithoutWidthCheck() should accept over-width plan")
    }

    /**
     * enabledWithoutWidthCheck 仍拒绝 material id 不匹配的方案。
     * enabledWithoutWidthCheck still rejects plan with wrong material id.
     */
    @Test
    fun enabledWithoutWidthCheck_rejectsWrongMaterial() {
        val m1 = material(id = "m1", upperBound = 2.0)
        val m2 = material(id = "m2", upperBound = 5.0)
        val p = product("p1", listOf(Quantity(Flt64(1.0), Meter)))

        val plan = CuttingPlan(
            id = "test-plan",
            material = m2, // different material
            slices = listOf(CuttingPlanSlice(production = p, width = Quantity(Flt64(1.0), Meter))),
            demandContributions = emptyList()
        )

        assertFalse(m1.enabledWithoutWidthCheck(plan), "Should reject plan with different material id")
    }

    // endregion

    // region empty policies

    @Test
    fun emptyPolicies_returnsNull() {
        val check = widthFeasibilityCheckFromPolicies(emptyList<Csp1dDomainPolicy<Flt64>>(), Flt64(0.0))
        assertNull(check, "Empty policy list should produce null widthCheck")
    }

    // endregion
}
