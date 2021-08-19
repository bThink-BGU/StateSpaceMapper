package il.ac.bgu.cs.bp.statespacemapper;

import org.jgrapht.*;
import org.jgrapht.graph.*;

import java.util.*;

public class PathsGenerationDFS<V, E> {

    private final Graph<V, E> graph;
    private final List<PathGenerationNode<V,E>> currentPath = new ArrayList<>();
    private final List<PathGenerationNode<V,E>> visited = new ArrayList<>();
    private boolean simplePathsOnly;

    public PathsGenerationDFS(Graph<V, E> graph)
    {
        this.graph = GraphTests.requireDirected(graph);
    }

    public List<List<E>> getAllPaths(V sourceVertex, Set<V> targetVertices, boolean simplePathsOnly, Integer maxPathLength){
        this.simplePathsOnly = simplePathsOnly;
        PathGenerationNode<V,E> startNode = new PathGenerationNode<V,E>(sourceVertex, null, graph.outgoingEdgesOf(sourceVertex), null);
        push(startNode);
        while (!isPathEmpty()) {
            PathGenerationNode<V,E> currentNode = peek();
            if (pathLength() == maxPathLength + 1){
                pop();
            } else {
                PathGenerationNode<V,E> nextNode = getNextUnvisitedNode(currentNode);
                if (nextNode == null){
                    pop();
                } else {
                    push(nextNode);
                }
            }
        }
        return startNode.traces;
    }

    private PathGenerationNode<V,E> getNextUnvisitedNode(PathGenerationNode<V,E> n){
        PathGenerationNode<V,E> result = null;
        while (true){
            E nextE  = n.getNextEvent();
            if (nextE == null){
                break;
            } else {
                V nextV = graph.getEdgeTarget(nextE);
                result = new PathGenerationNode<V,E>(nextV, nextE, graph.outgoingEdgesOf(nextV), null);
                if (this.simplePathsOnly && visited.contains(result)){
                    if (result.traces == null){
                        result.traces = new ArrayList<>();
                        result.traces.add(new ArrayList<>());
                    }
                    //n.addRuns(result.traces, result.lastEvent);
                    result = null;
                } else {
                    break;
                }
            }
        }
        return result;
    }

    private GraphPath<V, E> makePath(List<E> edges)
    {
        V source = graph.getEdgeSource(edges.get(0));
        V target = graph.getEdgeTarget(edges.get(edges.size() - 1));
        double weight = edges.stream().mapToDouble(edge -> graph.getEdgeWeight(edge)).sum();
        return new GraphWalk<>(graph, source, target, edges, weight);
    }
    
    private void push(PathGenerationNode<V,E> n) {
        visited.add(n);
        currentPath.add(n);
        // if ( trace.getStateCount() == 0 ) {
        //   trace.push( n.getSystemState() );
        // } else {
        //   trace.advance(n.getLastEvent(), n.getSystemState());
        // }
      }
    
      private PathGenerationNode<V,E> pop() {
        PathGenerationNode<V,E> popped = currentPath.remove(currentPath.size() - 1);
        //trace.pop();
        if (currentPath.size() > 0){
            if (popped.traces == null){
                popped.traces = new ArrayList<>();
                popped.traces.add(new ArrayList<>());
            }
            currentPath.get(currentPath.size()-1).addRuns(popped.traces, popped.lastEvent);
        }
        return popped;
      }
    
      private int pathLength() {
        return currentPath.size();
      }
    
      private boolean isPathEmpty() {
        return pathLength() == 0;
      }
    
      private PathGenerationNode<V,E> peek() {
        return isPathEmpty() ? null : currentPath.get(currentPath.size() - 1);
      }
}
