package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.service.limits

import fuookami.ospf.kotlin.core.intermediate_symbol.LinearExpressionSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearExpressionSymbols1
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbols1
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbsoluteHangingPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbstractCargoAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.ActualItem
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BPP3DShadowPriceArguments
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BPP3DShadowPriceMap
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bpp3dDemandKey
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bpp3dDemandMode
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.FilterStackingOnPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Item
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.LinearDeformationAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Material
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.MaterialType
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Package
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageShape
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.WeightAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.Bpp3dDemandEntry
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.DefaultBpp3dSolverValueAdapter
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.Load
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.demandEntriesFromMaterialAmounts
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.demandEntriesFromMaterialWeights
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.BatchNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.MaterialNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Orientation
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageType
import fuookami.ospf.kotlin.framework.model.ShadowPrice
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.value_range.Interval
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.multiarray.Shape1
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.utils.functional.Ok
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ItemDemandConstraintModeKeyTest {
    private val cargo = object : AbstractCargoAttribute {}

    private fun fixedRange(value: Flt64): ValueRange<Flt64> {
        return ValueRange(value, value, Interval.Closed, Interval.Closed, Flt64).value!!
    }

    private fun defaultPackageAttribute(type: PackageType = PackageType.CartonContainer): PackageAttribute {
        return PackageAttribute(
            packageType = type,
            weightAttribute = WeightAttribute(),
            deformationAttribute = LinearDeformationAttribute(Flt64.zero),
            hangingPolicy = AbsoluteHangingPolicy(Flt64.zero),
            stackingOnPolicy = FilterStackingOnPolicy()
        )
    }

    private fun createItem(
        id: String,
        material: Material,
        materialAmount: UInt64 = UInt64(2)
    ): ActualItem {
        return ActualItem(
            id = id,
            name = id,
            pack = Package.innerPackage(
                shape = PackageShape(
                    width = 1.0 * Meter,
                    height = 1.0 * Meter,
                    depth = 1.0 * Meter,
                    weight = 0.1 * Kilogram,
                    packageType = PackageType.CartonContainer
                ),
                materials = mapOf(material to materialAmount)
            ),
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-$id"),
            packageAttribute = defaultPackageAttribute()
        )
    }

    private fun testLoad(demandEntries: List<Bpp3dDemandEntry>): Load {
        val symbols = LinearExpressionSymbols1<Flt64>(
            "load",
            Shape1(demandEntries.size)
        ) { i, _ ->
            LinearExpressionSymbol(Flt64.zero, name = "load_$i")
        }
        return object : Load {
            override val demandEntries: List<Bpp3dDemandEntry> = demandEntries
            override val demandValueAdapter = DefaultBpp3dSolverValueAdapter
            override val load: LinearIntermediateSymbols1<Flt64> = symbols
            override val overLoad: LinearIntermediateSymbols1<Flt64> = symbols
            override val lessLoad: LinearIntermediateSymbols1<Flt64> = symbols
            override val overEnabled: Boolean = true
            override val lessEnabled: Boolean = true
        }
    }

    private fun demandModesInModel(model: LinearMetaModel<Flt64>): Set<Bpp3dDemandMode> {
        return model.relationConstraints.mapNotNull {
            (it.args as? DemandShadowPriceKey)?.mode
        }.toSet()
    }

    @Test
    fun materialAmountOnlyDemandShouldNotCreateItemAmountConstraints() {
        val material = Material(
            no = MaterialNo("M-AMOUNT"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "M-AMOUNT",
            weight = 1.0 * Kilogram
        )
        val load = testLoad(
            demandEntriesFromMaterialAmounts(
                materials = listOf(Pair(material, UInt64(5)))
            )
        )
        val model = LinearMetaModel()
        val constraint = DemandConstraint<BPP3DShadowPriceArguments, Item>(load)

        assertTrue(constraint(model) is Ok)
        assertEquals(setOf(Bpp3dDemandMode.ItemMaterialAmount), demandModesInModel(model))
    }

    @Test
    fun materialWeightOnlyDemandShouldNotCreateItemAmountConstraints() {
        val material = Material(
            no = MaterialNo("M-WEIGHT"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "M-WEIGHT",
            weight = 2.0 * Kilogram
        )
        val load = testLoad(
            demandEntriesFromMaterialWeights(
                materials = listOf(Pair(material, 12.5 * Kilogram))
            )
        )
        val model = LinearMetaModel()
        val constraint = DemandConstraint<BPP3DShadowPriceArguments, Item>(load)

        assertTrue(constraint(model) is Ok)
        assertEquals(setOf(Bpp3dDemandMode.ItemMaterialWeight), demandModesInModel(model))
    }

    @Test
    fun mixedItemAmountAndMaterialWeightDemandShouldCreateBothConstraints() {
        val material = Material(
            no = MaterialNo("M-MIX"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "M-MIX",
            weight = 3.0 * Kilogram
        )
        val item = createItem("item-mix", material, UInt64(3))
        val load = testLoad(
            listOf(
                Bpp3dDemandEntry(
                    mode = Bpp3dDemandMode.ItemAmount,
                    key = Bpp3dDemandKey.Item(item),
                    demand = Flt64.one,
                    demandRange = fixedRange(Flt64.one)
                )
            ) + demandEntriesFromMaterialWeights(listOf(Pair(material, 9.0 * Kilogram)))
        )
        val model = LinearMetaModel()
        val constraint = DemandConstraint<BPP3DShadowPriceArguments, Item>(load)

        assertTrue(constraint(model) is Ok)
        assertEquals(
            setOf(Bpp3dDemandMode.ItemAmount, Bpp3dDemandMode.ItemMaterialWeight),
            demandModesInModel(model)
        )
    }

    @Test
    fun extractorShouldSeparateShadowPriceByModeAndKey() {
        val material = Material(
            no = MaterialNo("A"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "A",
            weight = 5.0 * Kilogram
        )
        val item = createItem("item-1", material, UInt64(2))

        val amountKey = DemandShadowPriceKey(Bpp3dDemandMode.ItemAmount, Bpp3dDemandKey.Item(item))
        val materialAmountKey = DemandShadowPriceKey(Bpp3dDemandMode.ItemMaterialAmount, Bpp3dDemandKey.Material(material.key))
        val materialWeightKey = DemandShadowPriceKey(Bpp3dDemandMode.ItemMaterialWeight, Bpp3dDemandKey.Material(material.key))

        assertNotEquals(amountKey, materialAmountKey)
        assertNotEquals(materialAmountKey, materialWeightKey)
        assertNotEquals(amountKey, materialWeightKey)

        val load = object : Load {
            override val demandEntries: List<Bpp3dDemandEntry> = listOf(
                Bpp3dDemandEntry(
                    mode = Bpp3dDemandMode.ItemAmount,
                    key = Bpp3dDemandKey.Item(item),
                    demand = Flt64.one,
                    demandRange = fixedRange(Flt64.one)
                ),
                Bpp3dDemandEntry(
                    mode = Bpp3dDemandMode.ItemMaterialAmount,
                    key = Bpp3dDemandKey.Material(material.key),
                    demand = Flt64(2.0),
                    demandRange = fixedRange(Flt64(2.0))
                ),
                Bpp3dDemandEntry(
                    mode = Bpp3dDemandMode.ItemMaterialWeight,
                    key = Bpp3dDemandKey.Material(material.key),
                    demand = Flt64(10.0),
                    demandRange = fixedRange(Flt64(10.0))
                )
            )
            override val demandValueAdapter = DefaultBpp3dSolverValueAdapter
            override val load: LinearIntermediateSymbols1<Flt64>
                get() = error("not used in this test")
            override val overLoad: LinearIntermediateSymbols1<Flt64>
                get() = error("not used in this test")
            override val lessLoad: LinearIntermediateSymbols1<Flt64>
                get() = error("not used in this test")
            override val overEnabled: Boolean = true
            override val lessEnabled: Boolean = true
        }

        val constraint = DemandConstraint<BPP3DShadowPriceArguments, Item>(load)
        val extractor = constraint.extractor()!!

        val map = BPP3DShadowPriceMap()
        map[amountKey] = ShadowPrice(amountKey, Flt64(2.0))
        map[materialAmountKey] = ShadowPrice(materialAmountKey, Flt64(3.0))
        map[materialWeightKey] = ShadowPrice(materialWeightKey, Flt64(4.0))

        val reducedCost = extractor(map, BPP3DShadowPriceArguments(item))

        assertEquals(48.0, reducedCost.toDouble(), 1e-10)
    }

    @Test
    fun extractorShouldOnlyUseActiveDemandEntries() {
        val material = Material(
            no = MaterialNo("M-ACTIVE"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "M-ACTIVE",
            weight = 4.0 * Kilogram
        )
        val item = createItem("item-active", material, UInt64(3))
        val load = testLoad(
            demandEntriesFromMaterialWeights(
                materials = listOf(Pair(material, 12.0 * Kilogram))
            )
        )
        val constraint = DemandConstraint<BPP3DShadowPriceArguments, Item>(load)
        val extractor = constraint.extractor()!!

        val amountKey = DemandShadowPriceKey(Bpp3dDemandMode.ItemAmount, Bpp3dDemandKey.Item(item))
        val materialAmountKey = DemandShadowPriceKey(Bpp3dDemandMode.ItemMaterialAmount, Bpp3dDemandKey.Material(material.key))
        val materialWeightKey = DemandShadowPriceKey(Bpp3dDemandMode.ItemMaterialWeight, Bpp3dDemandKey.Material(material.key))

        val map = BPP3DShadowPriceMap()
        map[amountKey] = ShadowPrice(amountKey, Flt64(100.0))
        map[materialAmountKey] = ShadowPrice(materialAmountKey, Flt64(90.0))
        map[materialWeightKey] = ShadowPrice(materialWeightKey, Flt64(2.0))

        val reducedCost = extractor(map, BPP3DShadowPriceArguments(item))

        assertEquals(24.0, reducedCost.toDouble(), 1e-10)
    }
}
