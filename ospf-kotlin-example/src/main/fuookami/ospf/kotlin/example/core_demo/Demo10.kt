package fuookami.ospf.kotlin.example.core_demo


import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.utils.concept.*import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.error.Error
import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.intermediate_symbol.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.solver.scip.*

/**
 * @see     https://fuookami.github.io/ospf/examples/example10.html
 */
data object Demo10 {
    data class City(
        val name: String
    ) : AutoIndexed(City::class)

    private const val beginCity = "北京"

    private val cities = listOf(
        City("上海"),
        City("合肥"),
        City("广州"),
        City("成都"),
        City("北京")
    )

    private val distances = mapOf(
        Pair(cities[0], cities[1]) to Flt64(472.0),
        Pair(cities[0], cities[2]) to Flt64(1520.0),
        Pair(cities[0], cities[3]) to Flt64(2095.0),
        Pair(cities[0], cities[4]) to Flt64(1244.0),

        Pair(cities[1], cities[0]) to Flt64(472.0),
        Pair(cities[1], cities[2]) to Flt64(1257.0),
        Pair(cities[1], cities[3]) to Flt64(1615.0),
        Pair(cities[1], cities[4]) to Flt64(1044.0),

        Pair(cities[2], cities[0]) to Flt64(1529.0),
        Pair(cities[2], cities[1]) to Flt64(1257.0),
        Pair(cities[2], cities[3]) to Flt64(1954.0),
        Pair(cities[2], cities[4]) to Flt64(2174.0),

        Pair(cities[3], cities[0]) to Flt64(2095.0),
        Pair(cities[3], cities[1]) to Flt64(1615.0),
        Pair(cities[3], cities[2]) to Flt64(1954.0),
        Pair(cities[3], cities[4]) to Flt64(1854.0),

        Pair(cities[4], cities[0]) to Flt64(1244.0),
        Pair(cities[4], cities[1]) to Flt64(1044.0),
        Pair(cities[4], cities[2]) to Flt64(2174.0),
        Pair(cities[4], cities[3]) to Flt64(1854.0)
    )

    private lateinit var x: BinVariable2
    private lateinit var u: IntVariable1

    private lateinit var distance: LinearIntermediateSymbolFlt64
    private lateinit var depart: LinearIntermediateSymbols1Flt64
    private lateinit var reached: LinearIntermediateSymbols1Flt64

    private val metaModel = LinearMetaModelFlt64("demo10")

    private val subProcesses = listOf(
        Demo10::initVariable,
        Demo10::initSymbol,
        Demo10::initObject,
        Demo10::initConstraint,
        Demo10::solve,
        Demo10::analyzeSolution
    )

    suspend operator fun invoke(): Try {
        for (process in subProcesses) {
            when (val result = process()) {
                is Ok -> {}

                is Failed -> {
                    return result
                }

                is Fatal -> {
                    return result
                }
            }
        }
        return ok
    }

    private suspend fun initVariable(): Try {
        x = BinVariable2("x", Shape2(cities.size, cities.size))
        for (city1 in cities) {
            for (city2 in cities) {
                val xi = x[city1, city2]
                xi.name = "${x.name}_(${city1.name},${city2.name})"
                if (city1 != city2) {
                    metaModel.add(xi)
                } else {
                    xi.range.eq(false)
                }
            }
        }
        u = IntVariable1("u", Shape1(cities.size))
        for (city in cities) {
            val ui = u[city]
            ui.name = "${u.name}_${city.name}"
            if (city.name != beginCity) {
                ui.range.set(ValueRange(Int64(-cities.size.toLong()), Int64(cities.size.toLong())).value!!)
                metaModel.add(ui)
            } else {
                ui.range.eq(Int64.zero)
            }
        }
        return ok
    }

    private suspend fun initSymbol(): Try {
        distance = LinearExpressionSymbol(
            sum(cities.flatMap { city1 ->
                cities.mapNotNull { city2 ->
                    if (city1 == city2) {
                        null
                    } else {
                        distances[city1 to city2]?.let { it * x[city1, city2] }
                    }
                }
            }),
            name = "distance"
        )
        depart = LinearIntermediateSymbols1Flt64(
            "depart",
            Shape1(cities.size)
        ) { i, _ ->
            val city = cities[i]
            LinearExpressionSymbol(
                sum(x[city, _a]),
                name = "depart_${city.name}"
            )
        }
        reached = LinearIntermediateSymbols1Flt64("reached", Shape1(cities.size)) { i, _ ->
            val city = cities[i]
            LinearExpressionSymbol(
                sum(x[_a, city]),
                name = "reached_${city.name}"
            )
        }
        return ok
    }

    private suspend fun initObject(): Try {
        metaModel.minimize(distance, "distance")
        return ok
    }

    private suspend fun initConstraint(): Try {
        for (city in cities) {
            metaModel.addConstraint(
                depart[city] geq Flt64.one,
                name = "depart_lb_${city.name}"
            )
            metaModel.addConstraint(
                depart[city] leq Flt64.one,
                name = "depart_ub_${city.name}"
            )
        }
        for (city in cities) {
            metaModel.addConstraint(
                reached[city] geq Flt64.one,
                name = "reached_lb_${city.name}"
            )
            metaModel.addConstraint(
                reached[city] leq Flt64.one,
                name = "reached_ub_${city.name}"
            )
        }
        val notBeginCities = cities.filter { it.name != beginCity }
        for (city1 in notBeginCities) {
            for (city2 in notBeginCities) {
                if (city1 != city2) {
                    metaModel.addConstraint(
                        u[city1] - u[city2] + Flt64(cities.size.toDouble()) * x[city1, city2] leq Flt64((cities.size - 1).toDouble()),
                        name = "child_route_(${city1.name},${city2.name})"
                    )
                }
            }
        }
        return ok
    }

    private suspend fun solve(): Try {
        val solver = ScipLinearSolver()
        when (val ret = solver(metaModel)) {
            is Ok -> {
                metaModel.tokens.setSolution(ret.value.solution)
            }

            is Failed -> {
                return Failed(ret.error)
            }

                is Fatal -> {
                return Fatal(ret.errors)
            }
        }
        return ok
    }

    private suspend fun analyzeSolution(): Try {
        val route: MutableMap<City, City> = hashMapOf()
        for (token in metaModel.tokens.tokens) {
            if (token.result!! eq Flt64.one && token.variable.belongsTo(x)) {
                val vector = token.variable.vectorView
                val city1 = cities[vector[0]]
                val city2 = cities[vector[1]]
                route[city1] = city2
            }
        }
        return ok
    }
}



















