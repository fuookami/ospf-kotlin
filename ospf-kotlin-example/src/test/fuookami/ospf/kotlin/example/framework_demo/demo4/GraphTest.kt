@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4

import kotlin.time.Instant
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_generation.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for Graph structure and node/edge operations.
 */
class GraphTest {

    @Test
    fun `RootNode has index zero`() {
        assertEquals(UInt64.zero, RootNode.index)
    }

    @Test
    fun `EndNode has maximum index`() {
        assertEquals(UInt64.maximum, EndNode.index)
    }

    @Test
    fun `Graph put and get node`() {
        val graph = Graph()
        graph.put(RootNode)
        graph.put(EndNode)

        assertEquals(RootNode, graph[UInt64.zero])
        assertEquals(EndNode, graph[UInt64.maximum])
    }

    @Test
    fun `Graph get non-existent node returns null`() {
        val graph = Graph()
        assertNull(graph[UInt64(999UL)])
    }

    @Test
    fun `Graph put and get edges`() {
        val graph = Graph()
        graph.put(RootNode)
        graph.put(EndNode)
        graph.put(RootNode, EndNode)

        val edges = graph[RootNode]
        assertEquals(1, edges.size)
        assertEquals(EndNode, edges.first().to)
    }

    @Test
    fun `Graph connected returns true for existing edge`() {
        val graph = Graph()
        graph.put(RootNode)
        graph.put(EndNode)
        graph.put(RootNode, EndNode)

        assertTrue(graph.connected(RootNode, EndNode))
    }

    @Test
    fun `Graph connected returns false for non-existing edge`() {
        val graph = Graph()
        graph.put(RootNode)
        graph.put(EndNode)

        assertFalse(graph.connected(RootNode, EndNode))
    }

    @Test
    fun `Graph get edges for node with no edges returns empty set`() {
        val graph = Graph()
        graph.put(RootNode)

        val edges = graph[RootNode]
        assertTrue(edges.isEmpty())
    }

    @Test
    fun `Edge toString format`() {
        val edge = Edge(RootNode, EndNode)
        assertEquals("Root -> End", edge.toString())
    }
}
