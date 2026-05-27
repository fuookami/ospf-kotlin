package fuookami.ospf.kotlin.example.core_demo


import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.error.Error
import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.solver.scip.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.example.solveLinearMetaModel
import fuookami.ospf.kotlin.math.algebra.number.Flt64

private val flt64Converter = object : IntoValue<Flt64> {
        override fun intoValue(value: Flt64) = value
        override val zero get() = Flt64.zero
        override val one get() = Flt64.one
        override fun fromValue(value: Flt64) = value
    }

/**
 * @see     https://fuookami.github.io/ospf/examples/example5.html
 */
data object Demo5 {
    data class Cargo(
        val weight: UInt64,
        val value: UInt64
    ) : AutoIndexed(Cargo::class)

    private val cargos = listOf(
        Cargo(UInt64(2U), UInt64(6U)),
        Cargo(UInt64(2U), UInt64(3U)),
        Cargo(UInt64(6U), UInt64(5U)),
        Cargo(UInt64(5U), UInt64(4U)),
        Cargo(UInt64(4U), UInt64(6U))
    )
    private val maxWeight = UInt64(10U)

    private lateinit var x: BinVariable1
    private lateinit var cargoWeight: LinearIntermediateSymbol<Flt64>
    private lateinit var cargoValue: LinearIntermediateSymbol<Flt64>

    private val metaModel = LinearMetaModel<Flt64>("demo5", converter = flt64Converter)

    private val subProcesses = listOf(
        Demo5::initVariable,
        Demo5::initSymbol,
        Demo5::initObject,
        Demo5::initConstraint,
        Demo5::solve,
        Demo5::analyzeSolution
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
        x = BinVariable1("x", Shape1(cargos.size))
        for (c in cargos) {
            x[c].name = "${x.name}_${c.index}"
        }
        metaModel.add(x)
        return ok
    }

    private suspend fun initSymbol(): Try {
        cargoValue = LinearExpressionSymbol(
            sum(cargos) { c -> c.value * x[c] },
            name = "value"
        )
        metaModel.add(cargoValue)

        cargoWeight = LinearExpressionSymbol(
            sum(cargos) { c -> c.weight * x[c] },
            name = "weight"
        )
        metaModel.add(cargoWeight)
        return ok
    }

    private suspend fun initObject(): Try {
        metaModel.maximize(cargoValue, "value")
        return ok
    }

    private suspend fun initConstraint(): Try {
        metaModel.addConstraint(
            cargoWeight leq maxWeight,
            name = "weight"
        )
        return ok
    }

    private suspend fun solve(): Try {
        val solver = ScipLinearSolver()
        when (val ret = solveLinearMetaModel(solver, metaModel)) {
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
        val ret = HashSet<Cargo>()
        for (token in metaModel.tokens.tokens) {
            if (token.result!! eq Flt64.one
                && token.variable.belongsTo(x)
            ) {
                ret.add(cargos[token.variable.vectorView[0]])
            }
        }
        return ok
    }
}


















