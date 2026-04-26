package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.payload_maximization

import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*

class Aggregation(
    internal val aircraftModel: AircraftModel,
    internal val payload: Payload
)
