@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_compilation.model


import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.intermediate_symbol.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*

class FlightCapacity(
    private val tasks: List<FlightTask>,
    private val compilation: Compilation,
    val withPassenger: Boolean = tasks.any { it.capacity is AircraftCapacity.Passenger },
    val withCargo: Boolean = tasks.any { it.capacity is AircraftCapacity.Cargo }
) {
    lateinit var passenger: Map<FlightTask, Map<PassengerClass, LinearExpressionSymbol<Flt64>>>
    lateinit var cargo: Map<FlightTask, LinearExpressionSymbol<Flt64>>

    fun register(model: AbstractLinearMetaModel<Flt64>): Try {
        if (withPassenger) {
            if (!::passenger.isInitialized) {
                passenger = tasks
                    .filter { it.capacity is AircraftCapacity.Passenger }
                    .associateWith { task ->
                        PassengerClass.entries.associateWith { cls ->
                            LinearExpressionSymbol(
                                MutableLinearPolynomial(),
                                name = "${task}_capacity_${cls.name}"
                            )
                        }
                    }
            }

            when (val result = model.add(passenger)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }

        if (withCargo) {
            if (!::cargo.isInitialized) {
                cargo = tasks
                    .filter { it.capacity is AircraftCapacity.Cargo }
                    .associateWith { task ->
                        LinearExpressionSymbol(
                            MutableLinearPolynomial(),
                            name = "${task}_capacity"
                        )
                    }
            }

            when (val result = model.add(cargo)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }

        return ok
    }

    fun addColumns(
        iteration: UInt64,
        bunches: List<FlightTaskBunch>,
    ): Try {
        val xi = compilation.x[iteration.toInt()]

        if (withPassenger) {
            for ((task, thisPassengers) in passenger) {
                for ((cls, thisPassenger) in thisPassengers) {
                    val thisBunches = bunches.mapNotNull {
                        val thisTask = it.get(task)
                        val capacity = thisTask?.aircraft?.capacity?.let { capacity ->
                            when (capacity) {
                                is AircraftCapacity.Passenger -> {
                                    val thisCapacity = capacity[cls]
                                    if (thisCapacity neq UInt64.zero) {
                                        thisCapacity
                                    } else {
                                        null
                                    }
                                }

                                else -> {
                                    null
                                }
                            }
                        } ?: return@mapNotNull null
                        it to capacity
                    }
                    if (thisBunches.isNotEmpty()) {
                        thisPassenger.flush()
                        thisPassenger.asMutable() += sum(thisBunches.map {
                            it.second * xi[it.first]
                        })
                    }
                }
            }
        }

        if (withCargo) {
            for ((task, thisCargo) in cargo) {
                val thisBunches = bunches.mapNotNull {
                    val thisTask = it.get(task)
                    val capacity = thisTask?.aircraft?.capacity?.let { capacity ->
                        when (capacity) {
                            is AircraftCapacity.Cargo -> {
                                if (capacity.capacity neq Flt64.zero) {
                                    capacity.capacity
                                } else {
                                    null
                                }
                            }

                            else -> {
                                null
                            }
                        }
                    } ?: return@mapNotNull null
                    it to capacity
                }
                if (thisBunches.isNotEmpty()) {
                    thisCargo.flush()
                    thisCargo.asMutable() += sum(thisBunches.map {
                        it.second * xi[it.first]
                    })
                }
            }
        }

        return ok
    }
}













