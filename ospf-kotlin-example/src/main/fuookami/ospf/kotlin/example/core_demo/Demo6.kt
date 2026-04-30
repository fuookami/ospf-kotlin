package fuookami.ospf.kotlin.example.core_demo


import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.*
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
 * @see     https://fuookami.github.io/ospf/examples/example6.html
 */
data object Demo6 {
    data class Cargo(
        val weight: UInt64,
        val value: UInt64,
        val amount: UInt64
    ) : AutoIndexed(Cargo::class)

    private val cargos = listOf(
        Cargo(UInt64(1), UInt64(6), UInt64(10)),
        Cargo(UInt64(2), UInt64(10), UInt64(5)),
        Cargo(UInt64(2), UInt64(20), UInt64(2))
    )
    private val maxWeight = UInt64(8)

    private lateinit var x: UIntVariable1

    private lateinit var cargoWeight: LinearIntermediateSymbolF64
    private lateinit var cargoValue: LinearIntermediateSymbolF64

    private val metaModel = LinearMetaModelF64("demo6")

    private val subProcesses = listOf(
        Demo6::initVariable,
        Demo6::initSymbol,
        Demo6::initObject,
        Demo6::initConstraint,
        Demo6::solve,
        Demo6::analyzeSolution
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
        x = UIntVariable1("x", Shape1(cargos.size))
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
        for(c in cargos){
            x[c].range.ls(c.amount)
        }

        metaModel.addConstraint(
            cargoWeight leq maxWeight,
            name = "weight"
        )
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
        val ret = HashMap<Cargo, UInt64>()
        for (token in metaModel.tokens.tokens) {
            if (token.result!! geq Flt64.one && token.variable.belongsTo(x)) {
                ret[cargos[token.variable.vectorView[0]]] = token.result!!.round().toUInt64()
            }
        }
        return ok
    }
}



















