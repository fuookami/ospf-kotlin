package fuookami.ospf.kotlin.example.core_demo

import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.core.intermediate_symbol.*
import fuookami.ospf.kotlin.core.intermediate_symbol.function.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.example.exampleAbsoluteSlack
import fuookami.ospf.kotlin.example.flt64Constant
import fuookami.ospf.kotlin.example.flt64Linear
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.utils.concept.AutoIndexed
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.multiarray._a
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass
import kotlin.test.assertTrue

class CoreDemoBuildOnlyStructureTest {

    private val flt64Converter = object : IntoValue<Flt64> {
        override fun intoValue(value: Flt64) = value
        override val zero get() = Flt64.zero
        override val one get() = Flt64.one
        override fun fromValue(value: Flt64) = value
    }

    @BeforeEach
    fun resetAutoIndices() {
        flushIndices(
            D1Company::class,
            D2Product::class, D2Company::class,
            D3Product::class, D3Material::class,
            D4Material::class, D4Product::class,
            D5Cargo::class,
            D6Cargo::class,
            D7Store::class, D7Warehouse::class,
            D8Product::class, D8Equipment::class
        )
    }

    private fun flushIndices(vararg classes: KClass<*>) {
        for (cls in classes) {
            AutoIndexed.flush(cls)
        }
    }

    private fun buildAndAssert(
        name: String,
        model: LinearMetaModel<Flt64>,
        minVariables: Int
    ) {
        val ret = runBlocking { LinearMechanismModel.invoke(metaModel = model) }
        assertTrue(ret is Ok, "$name: mechanism model build should succeed")
        val mm = ret.value!!
        assertTrue(
            mm.tokens.tokens.size >= minVariables,
            "$name: expected >= $minVariables variable groups, got ${mm.tokens.tokens.size}"
        )
    }

    // region Demo1
    private data class D1Company(
        val capital: Flt64,
        val liability: Flt64,
        val profit: Flt64
    ) : AutoIndexed(D1Company::class)

    private val d1Companies = listOf(
        D1Company(Flt64(3.48), Flt64(1.28), Flt64(5400.0)),
        D1Company(Flt64(5.62), Flt64(2.53), Flt64(2300.0)),
        D1Company(Flt64(7.33), Flt64(1.02), Flt64(4600.0)),
        D1Company(Flt64(6.27), Flt64(3.55), Flt64(3300.0)),
        D1Company(Flt64(2.14), Flt64(0.53), Flt64(980.0))
    )

    @Test
    fun demo1ShouldBuildLinearMetaModelWithCorrectStructure() {
        val model = LinearMetaModel(name = "demo1", converter = flt64Converter)
        try {
            val x = BinVariable1("x", Shape1(d1Companies.size))
            for (c in d1Companies) { x[c].name = "${x.name}_${c.index}" }
            assertTrue(model.add(x) is Ok)

            val capital = LinearExpressionSymbol(sum(d1Companies) { it.capital * x[it] }, name = "capital")
            model.add(capital)
            val liability = LinearExpressionSymbol(sum(d1Companies) { it.liability * x[it] }, name = "liability")
            model.add(liability)
            val profit = LinearExpressionSymbol(sum(d1Companies) { it.profit * x[it] }, name = "profit")
            model.add(profit)

            model.maximize(profit)

            buildAndAssert("demo1", model, 1)
        } finally {
            model.close()
        }
    }
    // endregion

    // region Demo2
    private class D2Product : AutoIndexed(D2Product::class)
    private data class D2Company(val cost: Map<D2Product, Flt64>) : AutoIndexed(D2Company::class)

    private val d2Products = listOf(D2Product(), D2Product(), D2Product(), D2Product())
    private val d2Companies = listOf(
        D2Company(mapOf(d2Products[0] to Flt64(920.0), d2Products[1] to Flt64(480.0), d2Products[2] to Flt64(650.0), d2Products[3] to Flt64(340.0))),
        D2Company(mapOf(d2Products[0] to Flt64(870.0), d2Products[1] to Flt64(510.0), d2Products[2] to Flt64(700.0), d2Products[3] to Flt64(350.0))),
        D2Company(mapOf(d2Products[0] to Flt64(880.0), d2Products[1] to Flt64(500.0), d2Products[2] to Flt64(720.0), d2Products[3] to Flt64(400.0))),
        D2Company(mapOf(d2Products[0] to Flt64(930.0), d2Products[1] to Flt64(490.0), d2Products[2] to Flt64(680.0), d2Products[3] to Flt64(410.0)))
    )

    @Test
    fun demo2ShouldBuildLinearMetaModelWithCorrectStructure() {
        val model = LinearMetaModel(name = "demo2", converter = flt64Converter)
        try {
            val x = BinVariable2("x", Shape2(d2Companies.size, d2Products.size))
            for (c in d2Companies) { for (p in d2Products) { x[c, p].name = "${x.name}_${c.index},${p.index}" } }
            assertTrue(model.add(x) is Ok)

            val cost = LinearExpressionSymbol(
                sum(d2Companies.flatMap { c -> d2Products.mapNotNull { p -> c.cost[p]?.let { it * x[c, p] } } }),
                name = "cost"
            )
            model.add(cost)

            val assignmentCompany = LinearIntermediateSymbols(
                "assignment_company", Shape1(d2Companies.size), Flt64
            )
            for (c in d2Companies) {
                assignmentCompany[c].asMutable() += sum(d2Products.mapNotNull { p -> c.cost[p]?.let { x[c, p] } })
            }
            model.add(assignmentCompany)

            val assignmentProduct = LinearIntermediateSymbols(
                "assignment_product", Shape1(d2Products.size), Flt64
            )
            for (p in d2Products) {
                assignmentProduct[p].asMutable() += sum(d2Companies.mapNotNull { c -> c.cost[p]?.let { x[c, p] } })
            }
            model.add(assignmentProduct)

            model.minimize(cost)

            buildAndAssert("demo2", model, 1)
        } finally {
            model.close()
        }
    }
    // endregion

    // region Demo3
    private data class D3Product(val minYield: Flt64) : AutoIndexed(D3Product::class)
    private data class D3Material(val cost: Flt64, val yieldQuantity: Map<D3Product, Flt64>) : AutoIndexed(D3Material::class)

    private val d3Products = listOf(D3Product(Flt64(15000.0)), D3Product(Flt64(15000.0)), D3Product(Flt64(10000.0)))
    private val d3Materials = listOf(
        D3Material(Flt64(115.0), mapOf(d3Products[0] to Flt64(30.0), d3Products[1] to Flt64(10.0))),
        D3Material(Flt64(97.0), mapOf(d3Products[0] to Flt64(15.0), d3Products[2] to Flt64(20.0))),
        D3Material(Flt64(82.0), mapOf(d3Products[1] to Flt64(25.0), d3Products[2] to Flt64(15.0))),
        D3Material(Flt64(76.0), mapOf(d3Products[0] to Flt64(15.0), d3Products[1] to Flt64(15.0), d3Products[2] to Flt64(15.0)))
    )

    @Test
    fun demo3ShouldBuildLinearMetaModelWithCorrectStructure() {
        val model = LinearMetaModel(name = "demo3", converter = flt64Converter)
        try {
            val x = UIntVariable1("x", Shape1(d3Materials.size))
            for (m in d3Materials) { x[m].name = "${x.name}_${m.index}" }
            assertTrue(model.add(x) is Ok)

            val cost = LinearExpressionSymbol(sum(d3Materials) { it.cost * x[it] }, name = "cost")
            model.add(cost)

            val yield_ = LinearIntermediateSymbols("yield", Shape1(d3Products.size), Flt64)
            for (p in d3Products) {
                yield_[p].asMutable() += sum(d3Materials.filter { it.yieldQuantity.contains(p) }) { m -> m.yieldQuantity[p]!! * x[m] }
            }
            model.add(yield_)

            model.minimize(cost)

            buildAndAssert("demo3", model, 1)
        } finally {
            model.close()
        }
    }
    // endregion

    // region Demo4
    private data class D4Material(val available: Flt64) : AutoIndexed(D4Material::class)
    private data class D4Product(val profit: Flt64, val maxYield: Flt64, val use: Map<D4Material, Flt64>) : AutoIndexed(D4Product::class)

    private val d4Materials = listOf(D4Material(Flt64(24.0)), D4Material(Flt64(8.0)))
    private val d4Products = listOf(
        D4Product(Flt64(5.0), Flt64(3.0), mapOf(d4Materials[0] to Flt64(6.0), d4Materials[1] to Flt64(1.0))),
        D4Product(Flt64(4.0), Flt64(2.0), mapOf(d4Materials[0] to Flt64(4.0), d4Materials[1] to Flt64(2.0)))
    )

    @Test
    fun demo4ShouldBuildLinearMetaModelWithCorrectStructure() {
        val model = LinearMetaModel(name = "demo4", converter = flt64Converter)
        try {
            val x = RealVariable1("x", Shape1(d4Products.size))
            for (p in d4Products) { x[p].name = "${x.name}_${p.index}" }
            assertTrue(model.add(x) is Ok)

            val profit = LinearExpressionSymbol(sum(d4Products) { p -> p.profit * x[p] }, name = "profit")
            model.add(profit)
            val use = LinearIntermediateSymbols("use", Shape1(d4Materials.size), Flt64)
            for (m in d4Materials) {
                use[m].asMutable() += sum(d4Products) { p -> p.use[m]!! * x[p] }
            }
            model.add(use)

            model.maximize(profit, "profit")
            for (p in d4Products) { x[p].range.leq(p.maxYield) }

            buildAndAssert("demo4", model, 1)
        } finally {
            model.close()
        }
    }
    // endregion

    // region Demo5
    private data class D5Cargo(val weight: UInt64, val value: UInt64) : AutoIndexed(D5Cargo::class)

    private val d5Cargos = listOf(
        D5Cargo(UInt64(2U), UInt64(6U)),
        D5Cargo(UInt64(2U), UInt64(3U)),
        D5Cargo(UInt64(6U), UInt64(5U)),
        D5Cargo(UInt64(5U), UInt64(4U)),
        D5Cargo(UInt64(4U), UInt64(6U))
    )

    @Test
    fun demo5ShouldBuildLinearMetaModelWithCorrectStructure() {
        val model = LinearMetaModel(name = "demo5", converter = flt64Converter)
        try {
            val x = BinVariable1("x", Shape1(d5Cargos.size))
            for (c in d5Cargos) { x[c].name = "${x.name}_${c.index}" }
            assertTrue(model.add(x) is Ok)

            val cargoValue = LinearExpressionSymbol(sum(d5Cargos) { c -> c.value.toFlt64() * x[c] }, name = "value")
            model.add(cargoValue)
            val cargoWeight = LinearExpressionSymbol(sum(d5Cargos) { c -> c.weight.toFlt64() * x[c] }, name = "weight")
            model.add(cargoWeight)

            model.maximize(cargoValue, "value")

            buildAndAssert("demo5", model, 1)
        } finally {
            model.close()
        }
    }
    // endregion

    // region Demo6
    private data class D6Cargo(val weight: UInt64, val value: UInt64, val amount: UInt64) : AutoIndexed(D6Cargo::class)

    private val d6Cargos = listOf(
        D6Cargo(UInt64(1U), UInt64(6U), UInt64(10U)),
        D6Cargo(UInt64(2U), UInt64(10U), UInt64(5U)),
        D6Cargo(UInt64(2U), UInt64(20U), UInt64(2U))
    )

    @Test
    fun demo6ShouldBuildLinearMetaModelWithCorrectStructure() {
        val model = LinearMetaModel(name = "demo6", converter = flt64Converter)
        try {
            val x = UIntVariable1("x", Shape1(d6Cargos.size))
            for (c in d6Cargos) { x[c].name = "${x.name}_${c.index}" }
            assertTrue(model.add(x) is Ok)

            val cargoValue = LinearExpressionSymbol(sum(d6Cargos) { c -> c.value.toFlt64() * x[c] }, name = "value")
            model.add(cargoValue)
            val cargoWeight = LinearExpressionSymbol(sum(d6Cargos) { c -> c.weight.toFlt64() * x[c] }, name = "weight")
            model.add(cargoWeight)

            model.maximize(cargoValue, "value")
            for (c in d6Cargos) { x[c].range.leq(c.amount) }

            buildAndAssert("demo6", model, 1)
        } finally {
            model.close()
        }
    }
    // endregion

    // region Demo7
    private data class D7Store(val demand: Flt64) : AutoIndexed(D7Store::class)
    private data class D7Warehouse(val stowage: Flt64, val cost: Map<D7Store, Flt64>) : AutoIndexed(D7Warehouse::class)

    private val d7Stores = listOf(D7Store(Flt64(200.0)), D7Store(Flt64(400.0)), D7Store(Flt64(600.0)), D7Store(Flt64(300.0)))
    private val d7Warehouses = listOf(
        D7Warehouse(Flt64(510.0), mapOf(d7Stores[0] to Flt64(12.0), d7Stores[1] to Flt64(13.0), d7Stores[2] to Flt64(21.0), d7Stores[3] to Flt64(7.0))),
        D7Warehouse(Flt64(470.0), mapOf(d7Stores[0] to Flt64(14.0), d7Stores[1] to Flt64(17.0), d7Stores[2] to Flt64(8.0), d7Stores[3] to Flt64(18.0))),
        D7Warehouse(Flt64(520.0), mapOf(d7Stores[0] to Flt64(10.0), d7Stores[1] to Flt64(11.0), d7Stores[2] to Flt64(9.0), d7Stores[3] to Flt64(15.0)))
    )

    @Test
    fun demo7ShouldBuildLinearMetaModelWithCorrectStructure() {
        val model = LinearMetaModel(name = "demo7", converter = flt64Converter)
        try {
            val x = UIntVariable2("x", Shape2(d7Warehouses.size, d7Stores.size))
            for (w in d7Warehouses) { for (s in d7Stores) { x[w, s].name = "${x.name}_${w.index},${s.index}" } }
            assertTrue(model.add(x) is Ok)

            val cost = LinearExpressionSymbol(
                sum(d7Warehouses.map { w -> sum(d7Stores.filter { w.cost.contains(it) }.map { s -> w.cost[s]!! * x[w, s] }) }),
                name = "cost"
            )
            model.add(cost)

            val shipment = LinearIntermediateSymbols("shipment", Shape1(d7Warehouses.size), Flt64)
            for (w in d7Warehouses) {
                shipment[w].asMutable() += sum(d7Stores.filter { w.cost.contains(it) }.map { s -> x[w, s] })
            }
            model.add(shipment)

            val purchase = LinearIntermediateSymbols("purchase", Shape1(d7Stores.size), Flt64)
            for (s in d7Stores) {
                purchase[s].asMutable() += sum(d7Warehouses.filter { w -> w.cost.contains(s) }.map { w -> x[w, s] })
            }
            model.add(purchase)

            model.minimize(cost, "cost")

            buildAndAssert("demo7", model, 1)
        } finally {
            model.close()
        }
    }
    // endregion

    // region Demo8
    private data class D8Product(val profit: Flt64) : AutoIndexed(D8Product::class)
    private data class D8Equipment(val amount: UInt64, val manHours: Map<D8Product, Flt64>) : AutoIndexed(D8Equipment::class)

    private val d8Products = listOf(D8Product(Flt64(123.0)), D8Product(Flt64(94.0)), D8Product(Flt64(105.0)), D8Product(Flt64(132.0)), D8Product(Flt64(118.0)))
    private val d8Equipments = listOf(
        D8Equipment(UInt64(12U), mapOf(d8Products[0] to Flt64(0.23), d8Products[1] to Flt64(0.44), d8Products[2] to Flt64(0.17), d8Products[3] to Flt64(0.08), d8Products[4] to Flt64(0.36))),
        D8Equipment(UInt64(14U), mapOf(d8Products[0] to Flt64(0.13), d8Products[2] to Flt64(0.20), d8Products[3] to Flt64(0.37), d8Products[4] to Flt64(0.19))),
        D8Equipment(UInt64(8U), mapOf(d8Products[1] to Flt64(0.25), d8Products[2] to Flt64(0.34), d8Products[4] to Flt64(0.18))),
        D8Equipment(UInt64(6U), mapOf(d8Products[0] to Flt64(0.55), d8Products[1] to Flt64(0.72), d8Products[3] to Flt64(0.61)))
    )

    @Test
    fun demo8ShouldBuildLinearMetaModelWithCorrectStructure() {
        val model = LinearMetaModel(name = "demo8", converter = flt64Converter)
        try {
            val x = UIntVariable1("x", Shape1(d8Products.size))
            for (p in d8Products) { x[p].name = "${x.name}_${p.index}" }
            assertTrue(model.add(x) is Ok)

            val profit = LinearExpressionSymbol(sum(d8Products) { p -> p.profit * x[p] }, name = "profit")
            model.add(profit)
            val manHours = LinearIntermediateSymbols1<Flt64>("man_hours", Shape1(d8Equipments.size)) { i, _ ->
                val e = d8Equipments[i]
                LinearExpressionSymbol(
                    sum(d8Products.mapNotNull { p -> e.manHours[p]?.let { it * x[p] } }),
                    name = "man_hours_${e.index}"
                )
            }
            model.add(manHours)

            model.maximize(profit, "profit")

            buildAndAssert("demo8", model, 1)
        } finally {
            model.close()
        }
    }
    // endregion

    @Test
    fun demo9ShouldBuildLinearMetaModelWithSlackFunctions() {
        val model = LinearMetaModel(name = "demo9", converter = flt64Converter)
        try {
            val x = IntVar("demo9_build_x")
            val y = IntVar("demo9_build_y")
            assertTrue(model.add(x) is Ok)
            assertTrue(model.add(y) is Ok)

            val settlements = listOf(
                Flt64(9.0) to Flt64(2.0),
                Flt64(2.0) to Flt64(1.0),
                Flt64(3.0) to Flt64(8.0),
                Flt64(3.0) to Flt64(-2.0),
                Flt64(5.0) to Flt64(9.0),
                Flt64(4.0) to Flt64(-2.0)
            )

            val dx = LinearIntermediateSymbols1<Flt64>("dx", Shape1(settlements.size)) { i, _ ->
                exampleAbsoluteSlack(
                    type = UInteger,
                    x = flt64Linear(x),
                    y = flt64Constant(settlements[i].first),
                    name = "dx_$i"
                )
            }
            model.add(dx)

            val dy = LinearIntermediateSymbols1<Flt64>("dy", Shape1(settlements.size)) { i, _ ->
                exampleAbsoluteSlack(
                    type = UInteger,
                    x = flt64Linear(y),
                    y = flt64Constant(settlements[i].second),
                    name = "dy_$i"
                )
            }
            model.add(dy)

            val distance = LinearIntermediateSymbols1<Flt64>("distance", Shape1(settlements.size)) { i, _ ->
                LinearExpressionSymbol(
                    dx[i] + dy[i],
                    name = "distance_$i"
                )
            }
            model.add(distance)

            model.minimize(x)

            buildAndAssert("demo9", model, 2)
        } finally {
            model.close()
        }
    }

    @Test
    fun demo10ShouldBuildLinearMetaModelForTSP() {
        val model = LinearMetaModel(name = "demo10", converter = flt64Converter)
        try {
            val n = 5
            val x = BinVariable2("x", Shape2(n, n))
            val u = IntVariable1("u", Shape1(n))
            assertTrue(model.add(x) is Ok)
            assertTrue(model.add(u) is Ok)

            val distance = LinearExpressionSymbol(LinearPolynomial(Flt64.zero), name = "distance")
            model.add(distance)
            model.minimize(distance, "distance")

            val depart = LinearIntermediateSymbols1<Flt64>("depart", Shape1(n)) { i, _ ->
                LinearExpressionSymbol(
                    sum(x[i, _a]),
                    name = "depart_$i"
                )
            }
            model.add(depart)

            val reached = LinearIntermediateSymbols1<Flt64>("reached", Shape1(n)) { i, _ ->
                LinearExpressionSymbol(
                    sum(x[_a, i]),
                    name = "reached_$i"
                )
            }
            model.add(reached)

            buildAndAssert("demo10", model, 2)
        } finally {
            model.close()
        }
    }

    @Test
    fun demo11ShouldBuildLinearMetaModelForMaxFlow() {
        val model = LinearMetaModel(name = "demo11", converter = flt64Converter)
        try {
            val n = 9
            val x = UIntVariable2("x", Shape2(n, n))
            val flow = UIntVar("demo11_build_flow")
            assertTrue(model.add(x) is Ok)
            assertTrue(model.add(flow) is Ok)

            model.maximize(flow, "flow")

            buildAndAssert("demo11", model, 2)
        } finally {
            model.close()
        }
    }

    @Test
    fun demo12ShouldBuildLinearMetaModelWithBinaryzationAndMax() {
        val model = LinearMetaModel(name = "demo12", converter = flt64Converter)
        try {
            val x = UIntVariable1("x", Shape1(5))
            assertTrue(model.add(x) is Ok)

            val assignment = LinearIntermediateSymbols1<Flt64>("assignment", Shape1(5)) { i, _ ->
                LinearFunctionSymbolAdapter(
                    delegate = BinaryzationFunction(
                        polynomial = LinearPolynomial(x[i]),
                        converter = flt64Converter,
                        name = "assignment_$i"
                    ),
                    converter = flt64Converter
                )
            }
            model.add(assignment)

            val yield_ = LinearExpressionSymbol(LinearPolynomial(Flt64.zero), name = "yield")
            model.add(yield_)
            model.maximize(yield_, "yield")

            buildAndAssert("demo12", model, 1)
        } finally {
            model.close()
        }
    }

    @Test
    fun demo13ShouldBuildLinearMetaModelForTransport() {
        val model = LinearMetaModel(name = "demo13", converter = flt64Converter)
        try {
            val x = UIntVariable2("x", Shape2(5, 3))
            val y = UIntVariable2("y", Shape2(5, 3))
            assertTrue(model.add(x) is Ok)
            assertTrue(model.add(y) is Ok)

            val cost = LinearExpressionSymbol(LinearPolynomial(Flt64.zero), name = "cost")
            model.add(cost)
            model.minimize(cost, "cost")

            buildAndAssert("demo13", model, 2)
        } finally {
            model.close()
        }
    }

    @Test
    fun demo14ShouldBuildLinearMetaModelForNetworkFlow() {
        val model = LinearMetaModel(name = "demo14", converter = flt64Converter)
        try {
            val x = UIntVariable2("x", Shape2(8, 8))
            assertTrue(model.add(x) is Ok)

            val cost = LinearExpressionSymbol(LinearPolynomial(Flt64.zero), name = "cost")
            model.add(cost)
            model.minimize(cost)

            buildAndAssert("demo14", model, 1)
        } finally {
            model.close()
        }
    }

    @Test
    fun demo15ShouldBuildLinearMetaModelForCarDistribution() {
        val model = LinearMetaModel(name = "demo15", converter = flt64Converter)
        try {
            val x = UIntVariable3("x", Shape3(3, 2, 4))
            assertTrue(model.add(x) is Ok)

            val cost = LinearExpressionSymbol(LinearPolynomial(Flt64.zero), name = "cost")
            model.add(cost)
            model.minimize(cost, "cost")

            buildAndAssert("demo15", model, 1)
        } finally {
            model.close()
        }
    }

    @Test
    fun demo16ShouldBuildLinearMetaModelForProductionSchedule() {
        val model = LinearMetaModel(name = "demo16", converter = flt64Converter)
        try {
            val x = UIntVariable2("x", Shape2(4, 4))
            assertTrue(model.add(x) is Ok)

            val cost = LinearExpressionSymbol(LinearPolynomial(Flt64.zero), name = "cost")
            model.add(cost)
            model.minimize(cost, "cost")

            buildAndAssert("demo16", model, 1)
        } finally {
            model.close()
        }
    }

    @Test
    fun demo17ShouldBuildLinearMetaModelForVRPTW() {
        val model = LinearMetaModel(name = "demo17", converter = flt64Converter)
        try {
            val nodes = 10
            val vehicles = 3
            val x = BinVariable3("x", Shape3(nodes, nodes, vehicles))
            val s = URealVariable2("s", Shape2(nodes, vehicles))
            assertTrue(model.add(x) is Ok)
            assertTrue(model.add(s) is Ok)

            val cost = LinearExpressionSymbol(LinearPolynomial(Flt64.zero), name = "cost")
            model.add(cost)
            model.minimize(cost, "cost")

            buildAndAssert("demo17", model, 2)
        } finally {
            model.close()
        }
    }
}
