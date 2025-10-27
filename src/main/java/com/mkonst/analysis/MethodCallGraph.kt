package com.mkonst.analysis

import com.mkonst.types.ClassMethod
import org.jgrapht.Graph
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge

class MethodCallGraph {
    // Create a directed graph
    val graph: Graph<ClassMethod, DefaultEdge> = DefaultDirectedGraph(DefaultEdge::class.java)

    /**
     * Converts the given Strings into ClassMethod instances and appends a new edge. If the graph does not contain
     * the given methods are Vertices, new vertices will be generated
     *
     */
    @Throws(Exception::class)
    fun addEdge(callerClassMethod: String, calleeClassMethod: String) {
        val caller = ClassMethod(callerClassMethod)
        val callee = ClassMethod(calleeClassMethod)

        if (!graph.containsVertex(caller)) {
            graph.addVertex(caller)
        }
        if (!graph.containsVertex(callee)) {
            graph.addVertex(callee)
        }

        // Adds a directed edge: caller -> callee
        graph.addEdge(caller, callee)
    }

    /**
     * Returns a list of all method invocations made from a given ClassMethod instance
     *
     * @param fromClassMethod Represents the Edge's source
     * @return All edge targets given fromClassMethod as an edge source
     */
    fun getMethodCallsFrom(fromClassMethod: ClassMethod): MutableList<ClassMethod> {
        val methodCalls: MutableList<ClassMethod> = mutableListOf()

        // If Vertex/Node does not exist, catch the exception and return an empty list
        try {
            for (edge in graph.outgoingEdgesOf(fromClassMethod)) {
                methodCalls.add(graph.getEdgeTarget(edge))
            }
        } catch (ex: IllegalArgumentException) {
            // No methods calls found
            return mutableListOf()
        }

        return methodCalls
    }

    override fun toString(): String {
        val output = StringBuilder()

        for (edge in graph.edgeSet()) {
            val source = graph.getEdgeSource(edge)
            val target = graph.getEdgeTarget(edge)
            val weight = graph.getEdgeWeight(edge)
            output.append(source).append(" -> ").append(target).append(" (weight: ").append(weight).append(")\n")
        }

        return output.toString()
    }

    fun size(): Int {
        return graph.vertexSet().size
    }
}
