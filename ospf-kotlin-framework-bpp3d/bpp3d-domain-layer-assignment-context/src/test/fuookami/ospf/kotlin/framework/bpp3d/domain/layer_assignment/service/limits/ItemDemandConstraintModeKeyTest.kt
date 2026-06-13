/**
 * 物品需求约束模式键测试。
 * Item demand constraint mode key test.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.service.limits

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.multiarray.Shape1
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.math.geometry.Axis3
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.*
import fuookami.ospf.kotlin.framework.model.ShadowPrice

class ItemDemandConstraintModeKeyTest {
    private val cargo = object : AbstractCargoAttribute {}

    private fun fixedRange(value: FltX): ValueRange<FltX> {
        return ValueRange(value, value, Interval.Closed, Interval.Closed, FltX).value!!
    }

    private fun defaultPackageAttribute(type: PackageType = PackageType.CartonContainer): PackageAttribute {
        return PackageAttribute(
            packageType = type,
            weightAttribute = WeightAttribute(),
            deformationAttribute = LinearDeformationAttribute(FltX.zero),
            hangingPolicy = AbsoluteHangingPolicy(FltX.zero),
            stackingOnPolicy = FilterStackingOnPolicy()
        )
    }

    private fun createItem(
        id: String,
        material: Material<FltX>,
        materialAmount: UInt64 = UInt64(2)
    ): ActualItem {
        return ActualItem(
            id = id,
            name = id,
            pack = Package.innerPackage(
                shape = PackageShape(
                    width = fltX(1.0) * Meter,
                    height = fltX(1.0) * Meter,
                    depth = fltX(1.0) * Meter,
                    weight = fltX(0.1) * Kilogram,
                    packageType = PackageType.CartonContainer
                ),
                materials = mapOf(material to materialAmount)
            ),
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-$id"),
            packageAttribute = defaultPackageAttribute()
        )
    }

    private fun createCylinderItem(
        id: String,
        material: Material<FltX>,
        materialAmount: UInt64 = UInt64(3)
    ): ActualItem {
        return ActualItem(
            id = id,
            name = id,
            pack = Package.innerPackage(
                shape = PackageShape(
                    width = fltX(1.0) * Meter,
                    height = fltX(1.0) * Meter,
                    depth = fltX(1.0) * Meter,
                    weight = fltX(0.1) * Kilogram,
                    packageType = PackageType.CartonContainer,
                    shapeSpec = PackageShapeSpec.VerticalCylinder(
                        radius = fltX(0.5) * Meter,
                        axis = Axis3.Y
                    )
                ),
                materials = mapOf(material to materialAmount)
            ),
            width = fltX(1.0) * Meter,
            height = fltX(1.0) * Meter,
            depth = fltX(1.0) * Meter,
            weight = fltX(0.1) * Kilogram,
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-$id"),
            packageAttribute = defaultPackageAttribute(),
            shapeSpecOverride = PackageShapeSpec.VerticalCylinder(
                radius = fltX(0.5) * Meter,
                axis = Axis3.Y
            )
        )
    }

    private fun testLoad(demandEntries: List<Bpp3dDemandEntry<FltX>>): Load<FltX> {
        val symbols = LinearExpressionSymbols1<FltX>(
            "load",
            Shape1(demandEntries.size)
        ) { i, _ ->
            LinearExpressionSymbol(FltX.zero, name = "load_$i")
        }
        return object : Load<FltX> {
            override val demandEntries: List<Bpp3dDemandEntry<FltX>> = demandEntries
            override val demandValueAdapter = DefaultBpp3dSolverValueAdapter
            override val load: LinearIntermediateSymbols1<FltX> = symbols
            override val overLoad: LinearIntermediateSymbols1<FltX> = symbols
            override val lessLoad: LinearIntermediateSymbols1<FltX> = symbols
            override val overEnabled: Boolean = true
            override val lessEnabled: Boolean = true
        }
    }

    private fun demandModesInModel(model: LinearMetaModel<FltX>): Set<Bpp3dDemandMode> {
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
            weight = fltX(1.0) * Kilogram
        )
        val load = testLoad(
            demandEntriesFromMaterialAmounts(
                materials = listOf(Pair(material, UInt64(5)))
            )
        )
        val model = LinearMetaModel(
            name = "material-amount-only-demand",
            converter = FltX
        )
        val constraint = itemDemandConstraint(load)

        assertTrue(constraint(model) is Ok)
        assertEquals(setOf(Bpp3dDemandMode.Material), demandModesInModel(model))
    }

    @Test
    fun materialWeightOnlyDemandShouldNotCreateItemAmountConstraints() {
        val material = Material(
            no = MaterialNo("M-WEIGHT"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "M-WEIGHT",
            weight = fltX(2.0) * Kilogram
        )
        val load = testLoad(
            demandEntriesFromMaterialWeights(
                materials = listOf(Pair(material, fltX(12.5) * Kilogram))
            )
        )
        val model = LinearMetaModel(
            name = "material-weight-only-demand",
            converter = FltX
        )
        val constraint = itemDemandConstraint(load)

        assertTrue(constraint(model) is Ok)
        assertEquals(setOf(Bpp3dDemandMode.Material), demandModesInModel(model))
    }

    @Test
    fun mixedItemAmountAndMaterialWeightDemandShouldCreateBothConstraints() {
        val material = Material(
            no = MaterialNo("M-MIX"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "M-MIX",
            weight = fltX(3.0) * Kilogram
        )
        val item = createItem("item-mix", material, UInt64(3))
        val load = testLoad(
            listOf(
                Bpp3dDemandEntry(
                    mode = Bpp3dDemandMode.ItemAmount,
                    key = Bpp3dDemandKey.Item(item),
                    demand = FltX.one,
                    demandRange = fixedRange(FltX.one)
                )
            ) + demandEntriesFromMaterialWeights(listOf(Pair(material, fltX(9.0) * Kilogram)))
        )
        val model = LinearMetaModel(
            name = "mixed-demand-modes",
            converter = FltX
        )
        val constraint = itemDemandConstraint(load)

        assertTrue(constraint(model) is Ok)
        assertEquals(
            setOf(Bpp3dDemandMode.ItemAmount, Bpp3dDemandMode.Material),
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
            weight = fltX(5.0) * Kilogram
        )
        val item = createItem("item-1", material, UInt64(2))

        val amountKey = DemandShadowPriceKey(Bpp3dDemandMode.ItemAmount, Bpp3dDemandKey.Item(item))
        val materialAmountKey = DemandShadowPriceKey(Bpp3dDemandMode.ItemMaterialAmount, Bpp3dDemandKey.Material(material.key))
        val materialWeightKey = DemandShadowPriceKey(Bpp3dDemandMode.ItemMaterialWeight, Bpp3dDemandKey.Material(material.key))

        assertNotEquals(amountKey, materialAmountKey)
        assertNotEquals(materialAmountKey, materialWeightKey)
        assertNotEquals(amountKey, materialWeightKey)

        val load = object : Load<FltX> {
            override val demandEntries: List<Bpp3dDemandEntry<FltX>> = listOf(
                Bpp3dDemandEntry(
                    mode = Bpp3dDemandMode.ItemAmount,
                    key = Bpp3dDemandKey.Item(item),
                    demand = FltX.one,
                    demandRange = fixedRange(FltX.one)
                ),
                Bpp3dDemandEntry(
                    mode = Bpp3dDemandMode.ItemMaterialAmount,
                    key = Bpp3dDemandKey.Material(material.key),
                    demand = FltX(2.0),
                    demandRange = fixedRange(FltX(2.0))
                ),
                Bpp3dDemandEntry(
                    mode = Bpp3dDemandMode.ItemMaterialWeight,
                    key = Bpp3dDemandKey.Material(material.key),
                    demand = FltX(10.0),
                    demandRange = fixedRange(FltX(10.0))
                )
            )
            override val demandValueAdapter = DefaultBpp3dSolverValueAdapter
            override val load: LinearIntermediateSymbols1<FltX>
                get() = error("not used in this test")
            override val overLoad: LinearIntermediateSymbols1<FltX>
                get() = error("not used in this test")
            override val lessLoad: LinearIntermediateSymbols1<FltX>
                get() = error("not used in this test")
            override val overEnabled: Boolean = true
            override val lessEnabled: Boolean = true
        }

        val constraint = itemDemandConstraint(load)
        val extractor = constraint.extractor()!!

        val map = AbstractBPP3DShadowPriceMap<BPP3DShadowPriceArguments, FltX, Item>()
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
            weight = fltX(4.0) * Kilogram
        )
        val item = createItem("item-active", material, UInt64(3))
        val load = testLoad(
            demandEntriesFromMaterialWeights(
                materials = listOf(Pair(material, fltX(12.0) * Kilogram))
            )
        )
        val constraint = itemDemandConstraint(load)
        val extractor = constraint.extractor()!!

        val amountKey = DemandShadowPriceKey(Bpp3dDemandMode.ItemAmount, Bpp3dDemandKey.Item(item))
        val materialAmountKey = DemandShadowPriceKey(Bpp3dDemandMode.ItemMaterialAmount, Bpp3dDemandKey.Material(material.key))
        val materialWeightKey = DemandShadowPriceKey(Bpp3dDemandMode.ItemMaterialWeight, Bpp3dDemandKey.Material(material.key))

        val map = AbstractBPP3DShadowPriceMap<BPP3DShadowPriceArguments, FltX, Item>()
        map[amountKey] = ShadowPrice(amountKey, Flt64(100.0))
        map[materialAmountKey] = ShadowPrice(materialAmountKey, Flt64(90.0))
        map[materialWeightKey] = ShadowPrice(materialWeightKey, Flt64(2.0))

        val reducedCost = extractor(map, BPP3DShadowPriceArguments(item))

        assertEquals(24.0, reducedCost.toDouble(), 1e-10)
    }

    @Test
    fun extractorShouldKeepCylinderItemDemandSemanticsAcrossModes() {
        val material = Material(
            no = MaterialNo("M-CYL-SEM"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "M-CYL-SEM",
            weight = fltX(2.0) * Kilogram
        )
        val cylinder = createCylinderItem(
            id = "cylinder-semantics",
            material = material,
            materialAmount = UInt64(3)
        )

        val demandEntries = listOf(
            Bpp3dDemandEntry(
                mode = Bpp3dDemandMode.ItemAmount,
                key = Bpp3dDemandKey.Item(cylinder),
                demand = FltX.one,
                demandRange = fixedRange(FltX.one)
            )
        ) + demandEntriesFromMaterialAmounts(
            materials = listOf(Pair(material, UInt64(3)))
        ) + demandEntriesFromMaterialWeights(
            materials = listOf(Pair(material, fltX(6.0) * Kilogram))
        )
        val load = object : Load<FltX> {
            override val demandEntries: List<Bpp3dDemandEntry<FltX>> = demandEntries
            override val demandValueAdapter = DefaultBpp3dSolverValueAdapter
            override val load: LinearIntermediateSymbols1<FltX>
                get() = error("not used in this test")
            override val overLoad: LinearIntermediateSymbols1<FltX>
                get() = error("not used in this test")
            override val lessLoad: LinearIntermediateSymbols1<FltX>
                get() = error("not used in this test")
            override val overEnabled: Boolean = true
            override val lessEnabled: Boolean = true
        }

        val amountKey = DemandShadowPriceKey(Bpp3dDemandMode.ItemAmount, Bpp3dDemandKey.Item(cylinder))
        val materialAmountKey = DemandShadowPriceKey(Bpp3dDemandMode.ItemMaterialAmount, Bpp3dDemandKey.Material(material.key))
        val materialWeightKey = DemandShadowPriceKey(Bpp3dDemandMode.ItemMaterialWeight, Bpp3dDemandKey.Material(material.key))

        val constraint = itemDemandConstraint(load)
        val extractor = constraint.extractor()!!

        val map = AbstractBPP3DShadowPriceMap<BPP3DShadowPriceArguments, FltX, Item>()
        map[amountKey] = ShadowPrice(amountKey, Flt64(2.0))
        map[materialAmountKey] = ShadowPrice(materialAmountKey, Flt64(3.0))
        map[materialWeightKey] = ShadowPrice(materialWeightKey, Flt64(4.0))

        val reducedCost = extractor(map, BPP3DShadowPriceArguments(cylinder))

        assertEquals(35.0, reducedCost.toDouble(), 1e-10)
    }
}
