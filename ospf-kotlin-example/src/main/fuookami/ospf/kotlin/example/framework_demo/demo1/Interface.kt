package fuookami.ospf.kotlin.example.framework_demo.demo1

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.example.framework_demo.demo1.infrastructure.*

const val data = """28 45 12
    100
    0 16 8 2
    0 26 13 2
    0 9 14 2
    0 8 36 2
    0 7 25 2
    0 6 13 2
    0 1 20 1
    0 2 16 1
    0 3 13 1
    1 19 26 2
    1 18 31 2
    1 16 24 2
    1 15 16 2
    1 2 4 1
    1 3 11 1
    2 4 37 2
    2 25 24 2
    2 21 5 2
    2 20 2 2
    2 3 7 1
    3 19 24 2
    3 24 17 2
    3 27 26 2
    4 5 26 1
    4 6 12 1
    5 6 14 1
    8 21 36 5
    9 10 6 1
    9 11 14 1
    10 26 11 5
    10 11 9 1
    12 13 15 1
    12 14 9 1
    12 15 12 1
    13 14 11 1
    13 15 27 1
    14 15 19 1
    17 18 22 1
    21 22 22 1
    21 23 18 1
    21 24 14 1
    22 23 23 1
    22 24 11 1
    23 24 23 1
    26 27 19 1
    0 8 40
    1 11 13
    2 22 28
    3 3 45
    4 17 11
    5 19 26
    6 16 15
    7 13 13
    8 5 18
    9 25 15
    10 7 10
    11 24 23
    """

class ConsoleApplication {
    suspend operator fun invoke(): Try {
        return when (val input = read(data)) {
            is Failed -> {
                Failed(input.error)
            }

            is Ok -> {
                val prob = SSP()
                val ret = prob(input.value)
                Ok(success)
            }
        }
    }

    private fun read(data: String): Ret<Input> {
        val lines = data.split("\n")
        val firstLine = lines[0].trim()
        val normalNodeAmount = UInt64(firstLine.split(" ")[0].toULong())
        val edgeAmount = UInt64(firstLine.split(" ")[1].toULong())
        val clientNodeAmount = UInt64(firstLine.split(" ")[2].toULong())
        val serviceCost = UInt64(lines[1].trim().toULong())
        val edges: MutableList<EdgeDTO> = ArrayList()
        for (i in 2 until edgeAmount.toString().toInt() + 2) {
            val str = lines[i].trim().split(" ")
            val edgeData = EdgeDTO(
                UInt64(str[0].toULong()),
                UInt64(str[1].toULong()),
                UInt64(str[2].toULong()),
                UInt64(str[3].toULong()),
            )
            edges.add(edgeData)
        }
        val clientNodes: MutableList<ClientNodeDTO> = ArrayList()
        for (i in edgeAmount.toString().toInt() + 2 until edgeAmount.toString().toInt() + clientNodeAmount.toString()
            .toInt() + 2) {
            val str = lines[i].trim().split(" ")
            val clientNodeData = ClientNodeDTO(
                UInt64(str[0].toULong()),
                UInt64(str[1].toULong()),
                UInt64(str[2].toULong()),
            )
            clientNodes.add(clientNodeData)
        }
        return Ok(
            Input(
                serviceCost,
                normalNodeAmount,
                edges,
                clientNodes
            )
        )
    }
}
