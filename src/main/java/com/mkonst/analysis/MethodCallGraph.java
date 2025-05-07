package com.mkonst.analysis;

import com.mkonst.types.ClassMethod;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.util.ArrayList;
import java.util.List;

public class MethodCallGraph {
    private Graph<ClassMethod, DefaultEdge> graph;

    public MethodCallGraph() {

        // Create a directed graph
        graph = new DefaultDirectedGraph<>(DefaultEdge.class);
    }

    /**
     * Converts the given Strings into ClassMethod instances and appends a new edge. If the graph does not contain
     * the given methods are Vertices, new vertices will be generated
     *
     * @param callerClassMethod
     * @param calleeClassMethod
     * @throws Exception
     */
    public void addEdge(String callerClassMethod, String calleeClassMethod) throws Exception {
        ClassMethod caller = new ClassMethod(callerClassMethod);
        ClassMethod callee = new ClassMethod(calleeClassMethod);

        if (!graph.containsVertex(caller)) {
            graph.addVertex(caller);
        }
        if (!graph.containsVertex(callee)) {
            graph.addVertex(callee);
        }

        // Adds a directed edge: caller -> callee
        graph.addEdge(caller, callee);
    }

    /**
     * Returns a list of all method invocations made from a given ClassMethod instance
     *
     * @param fromClassMethod Represents the Edge's source
     * @return All edge targets given fromClassMethod as an edge source
     */
    public List<ClassMethod> getMethodCallsFrom(ClassMethod fromClassMethod) {
        List<ClassMethod> methodCalls = new ArrayList<>();

        // If Vertex/Node does not exist, catch the exception and return an empty list
        try {
            for (DefaultEdge edge : graph.outgoingEdgesOf(fromClassMethod)) {
                methodCalls.add(graph.getEdgeTarget(edge));
            }
        } catch (IllegalArgumentException ex) {
            // No methods calls found
            return new ArrayList<>();
        }

        return methodCalls;
    }

    /**
     * Returns a list of all method invocations made to a given ClassMethod instance
     *
     * @param fromClassMethod Represents the Edge's source
     * @return All edge targets given fromClassMethod as an edge source
     */
    public List<ClassMethod> getMethodCallsTo(ClassMethod fromClassMethod) {
        List<ClassMethod> methodCalls = new ArrayList<>();

        // If Vertex/Node does not exist, catch the exception and return an empty list
        try {
            for (DefaultEdge edge : graph.incomingEdgesOf(fromClassMethod)) {
                methodCalls.add(graph.getEdgeTarget(edge));
            }
        } catch (IllegalArgumentException ex) {
            // No methods calls found
            return new ArrayList<>();
        }

        return methodCalls;
    }

    public String toString() {
        StringBuilder output = new StringBuilder();

        for (DefaultEdge edge : graph.edgeSet()) {
            ClassMethod source = graph.getEdgeSource(edge);
            ClassMethod target = graph.getEdgeTarget(edge);
            double weight = graph.getEdgeWeight(edge);
            output.append(source).append(" -> ").append(target).append(" (weight: ").append(weight).append(")\n");
        }

        return output.toString();
    }

    public int size() {
        return graph.vertexSet().size();
    }
}
