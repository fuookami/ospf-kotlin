package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.service

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*

/**
 * Initializes the aircraft aggregation from input request data.
 * 从输入请求数据初始化飞机聚合。
*/
data object AggregationInitializer {

    /**
     * Initialize the aircraft aggregation from the given request data.
     * 从给定的请求数据初始化飞机聚合。
     *
     * @param input 请求输入数据 / The request input data.
    * @return 初始化的聚合结果 / The initialized aggregation result.
    */
    operator fun invoke(input: RequestDTO): Ret<Aggregation> {
        if (input.aircraftType == AircraftTypeInput.Unknown) {
            return Failed(
                ErrorCode.IllegalArgument,
                "未知机型不能初始化飞机聚合 / Unknown aircraft type cannot initialize aircraft aggregation"
            )
        }
        if (input.positions.isEmpty()) {
            return Failed(
                ErrorCode.IllegalArgument,
                "飞机至少需要一个可用舱位 / Aircraft requires at least one available position"
            )
        }
        if (input.positions.size > UByte.MAX_VALUE.toInt() + 1) {
            return Failed(
                ErrorCode.IllegalArgument,
                "舱位数量超过装载顺序索引上限：${input.positions.size} / Position count exceeds loading-order index limit: ${input.positions.size}"
            )
        }
        if (input.positions.any { it.name.isBlank() }) {
            return Failed(
                ErrorCode.IllegalArgument,
                "舱位名称不能为空 / Position name must not be blank"
            )
        }
        if (input.positions.map { it.name }.distinct().size != input.positions.size) {
            return Failed(
                ErrorCode.IllegalArgument,
                "舱位名称必须唯一 / Position names must be unique"
            )
        }
        if (input.positions.any { !it.maxWeight.isFinite() || it.maxWeight <= 0.0 }) {
            return Failed(
                ErrorCode.IllegalArgument,
                "舱位最大载重必须为正且有限 / Position maximum weight must be positive and finite"
            )
        }
        if (input.positions.any { !it.longitudinalArm.isFinite() || !it.lateralArm.isFinite() }) {
            return Failed(
                ErrorCode.IllegalArgument,
                "舱位力臂必须为有限值 / Position arms must be finite"
            )
        }
        val spaces = input.spaces.associateBy { it.id }
        if (spaces.size != input.spaces.size || spaces.keys.any { it.isBlank() } ||
            input.positions.any { it.spaceId !in spaces || it.partitionId.isBlank() || it.loadingOrder !in 0..255 } ||
            input.positions.map { it.loadingOrder }.distinct().size != input.positions.size) {
            return Failed(ErrorCode.IllegalArgument, "物理空间或装载顺序无效 / Invalid physical space or loading order")
        }
        if (input.positions.any { position ->
                val g = position.geometry
                listOf(g.front, g.back, g.left, g.right).any { !it.isFinite() } || g.front >= g.back || g.left >= g.right
            }) {
            return Failed(ErrorCode.IllegalArgument, "舱位必须提供有限正矩形边界 / Positions require finite positive rectangle bounds")
        }

        val aircraftModel = AircraftModel(AircraftMinorModel(input.aircraftType.name))
        val formula = Formula(
            aircraftModel = aircraftModel,
            lip = Quantity(Flt64.zero, aircraftModel.lengthUnit),
            chord = Quantity(Flt64.one, aircraftModel.lengthUnit),
            standardDatum = Quantity(Flt64.zero, aircraftModel.lengthUnit),
            forceDistanceCoefficient = Flt64.one,
            doiCorrection = Quantity(Flt64.zero, aircraftModel.torqueUnit)
        )
        val fuselage = Fuselage(
            liferaft = null,
            dow = Quantity(Flt64.zero, aircraftModel.weightUnit),
            doi = Quantity(Flt64.zero, aircraftModel.torqueUnit),
            balancedArm = Quantity(Flt64.zero, aircraftModel.lengthUnit)
        )
        val basePositions = input.positions.mapIndexed { index, position ->
            fun length(value: Double) = Quantity(Flt64(value), input.lengthUnit.unit).to(aircraftModel.lengthUnit)!!
            val g = position.geometry
            val lengthInMeter = Quantity(Flt64(g.back - g.front), input.lengthUnit.unit).to(Meter)!!.value
            val widthInMeter = Quantity(Flt64(g.right - g.left), input.lengthUnit.unit).to(Meter)!!.value
            Position(
                aircraftModel = aircraftModel,
                id = UInt64(index.toULong()),
                spaceName = position.name,
                sizeCode = "N",
                frontArm = length(g.front),
                backArm = length(g.back),
                leftArm = length(g.left),
                rightArm = length(g.right),
                volume = Quantity(Flt64.zero, aircraftModel.volumeUnit),
                offsets = HashMap(),
                location = when (spaces.getValue(position.spaceId).location) {
                    DeckLocationInput.Main -> PositionLocation.normalMain
                    DeckLocationInput.LowForward -> PositionLocation.lowForwardNotBulk
                    DeckLocationInput.LowAft -> PositionLocation.lowAftNotBulk
                },
                linearLoadingOrder = UInt8(position.loadingOrder.toUByte()),
                declaredArea = Quantity(lengthInMeter * widthInMeter, SquareMeter).to(aircraftModel.areaUnit)!!,
                declaredLongitudinalArm = length(position.longitudinalArm),
                declaredLateralArm = length(position.lateralArm)
            )
        }
        val orderedPositions = basePositions.sortedBy { it.linearLoadingOrder }
        for ((index, position) in orderedPositions.withIndex()) {
            position._loadingOrder = LoadingOrder(
                location = position.location.location,
                order = position.linearLoadingOrder,
                directPrec = if (index == 0) emptySet() else setOf(orderedPositions[index - 1]),
                directSucc = if (index == basePositions.lastIndex) emptySet() else setOf(orderedPositions[index + 1])
            )
        }
        val adjacentNeighbours = orderedPositions.zipWithNext { first, second ->
            Neighbour(NeighbourType.Physics, first to second)
        }
        val spacesByPosition = input.positions.associate { it.name to it.spaceId }
        val physicalNeighbours = orderedPositions.groupBy { spacesByPosition.getValue(it.spaceName) }.values.flatMap { space ->
            space.zipWithNext { first, second -> Neighbour(NeighbourType.Physics, first to second) }
        }
        val neighbours = hashMapOf<NeighbourType, List<Neighbour>>(
            NeighbourType.Physics to physicalNeighbours,
            NeighbourType.IndirectPhysics to physicalNeighbours.map { neighbour ->
                Neighbour(NeighbourType.IndirectPhysics, neighbour.pair)
            },
            NeighbourType.LinearLoadingOrder to adjacentNeighbours.map { neighbour ->
                Neighbour(NeighbourType.LinearLoadingOrder, neighbour.pair)
            },
            NeighbourType.TopologicalLoadingOrder to adjacentNeighbours.map { neighbour ->
                Neighbour(NeighbourType.TopologicalLoadingOrder, neighbour.pair)
            }
        )
        val defaultFuelTank = FuelTank(
            type = FuelTankType.Main,
            name = "DEFAULT",
            maxVolume = Quantity(Flt64.zero, aircraftModel.fuelVolumeUnit),
            balancedArm = FuelTankBalancedArm(emptyList())
        )
        val defaultFuel = FuelConstant(
            density = Quantity(Flt64.zero, aircraftModel.fuelDensityUnit),
            weight = Quantity(Flt64.zero, aircraftModel.weightUnit),
            index = Quantity(Flt64.zero, aircraftModel.torqueUnit)
        )

        return Ok(Aggregation(
            regNo = RegNo("DEFAULT"),
            aircraftModel = aircraftModel,
            formula = formula,
            fuselage = fuselage,
            fuelTanks = Quantity(defaultFuelTank, NoneUnit),
            fuel = FlightPhase.entries.associateWith { defaultFuel },
            decks = basePositions.groupBy { it.location.location }.map { (location, positions) ->
                Deck(location = location, doors = emptyList(), positions = positions, doorUbieties = emptyMap())
            },
            neighbours = neighbours
        ))
    }
}
