/*
 * (C) Copyright 2015-2021, by Vera-Licona Research Group and Contributors.
 *
 * JGraphT : a free Java graph-theory library
 *
 * See the CONTRIBUTORS.md file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the
 * GNU Lesser General Public License v2.1 or later
 * which is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1-standalone.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR LGPL-2.1-or-later
 */
package il.ac.bgu.cs.bp.statespacemapper.jgrapht;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.GraphTests;
import org.jgrapht.alg.shortestpath.PathValidator;
import org.jgrapht.graph.GraphWalk;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiPredicate;

/**
 * A class for finding all paths between a source vertex and a set of target vertices, with
 * options to search only simple paths, to limit the path length and choose the path-generation algorithm.
 * <p>
 * There path generation is done using a BFS-like algorithm.
 * It is possible to improve the algorithm by pruning edges that do not lead to a target vertex.
 * However, the pruning is <b>not recommended</b> if the number of target vertices is large.
 * By default, the pruning is done iff the number of target vertices is smaller than {@code Math.max(1, Math.sqrt(graph.vertexSet().size()))}.
 *
 * @param <V> the graph vertex type
 * @param <E> the graph edge type
 * @author Andrew Gainer-Dewar, Google LLC
 * @author Achiya Elyasaf, Ben-Gurion University
 */
public class AllDirectedPaths<V, E> {
  /**
   * The input graph
   */
  private final Graph<V, E> graph;
  /**
   * The source vertex
   */
  private final V sourceVertex;
  /**
   * The target vertices. If null or empty, then {@code targetVertices = graph.vertexSet()}
   */
  private Set<V> targetVertices;
  /**
   * If true, only return the longest paths that includes target vertices
   */
  private final boolean longestPathsOnly;
  /**
   * If true, only search simple (non-self-intersecting) paths
   */
  private final boolean simplePathsOnly;
  private final boolean includeReturningEdgesInSimplePaths;
  /**
   * Maximum number of edges to allow in a path (if null, all paths are considered, which may be very slow due to potentially huge output)
   */
  private final Integer maxPathLength;
  /**
   * A GraphPath comparator for sorting the paths.
   */
  private final Comparator<GraphPath<V, E>> pathComparator;

  /**
   * A predicate that receives a graph and a set of target vertices and decide whether to prone edges that do not lead to a target vertex.
   */
  private final BiPredicate<Graph<V, E>, Set<V>> proneUnreachableEdges;

  /**
   * Provides validation for the paths which will be computed. If the validator is {@code null},
   * this means that all paths are valid.
   */
  private final PathValidator<V, E> pathValidator;

  /**
   * Create a new instance with given {@code pathValidator} and {@code proneUnreachableEdges}.
   * <p>
   * If non-{@code null}, the {@code pathValidator} will be used while searching for paths, validating the addition
   * of any edge to a partial path. Zero-length paths will therefore not be subject to {@code pathValidator};
   * length-1 paths will.
   *
   * @param graph                              the input graph
   * @param proneUnreachableEdges              a predicate that gets a graph and a set of target vertices and decides whether to prone unreachable edges.
   * @param pathValidator                      validator for computed paths; may be null
   * @param sourceVertex                       the source vertex
   * @param targetVertices                     the target vertices. If null or empty, then {@code targetVertices = graph.vertexSet()}
   * @param longestPathsOnly                   if true, only return the longest paths that includes target vertices
   * @param simplePathsOnly                    if true, only search simple (non-self-intersecting) paths
   * @param includeReturningEdgesInSimplePaths if true and also {@param simplePathsOnly} is true, then the edge that creates the self intersecting path is also returned
   * @param maxPathLength                      maximum number of edges to allow in a path (if null, all paths are considered, which may be very slow due to potentially huge output)
   * @param pathComparator                     A GraphPath comparator for sorting the resulted paths
   * @throws IllegalArgumentException if the graph is not directed
   */
  public AllDirectedPaths(Graph<V, E> graph, PathValidator<V, E> pathValidator, BiPredicate<Graph<V, E>, Set<V>> proneUnreachableEdges,
                          V sourceVertex, Set<V> targetVertices, boolean longestPathsOnly, boolean simplePathsOnly,
                          boolean includeReturningEdgesInSimplePaths, Integer maxPathLength, Comparator<GraphPath<V, E>> pathComparator) {
    this.graph = GraphTests.requireDirected(graph);
    this.sourceVertex = sourceVertex;
    this.targetVertices = targetVertices;
    this.longestPathsOnly = longestPathsOnly;
    this.simplePathsOnly = simplePathsOnly;
    this.includeReturningEdgesInSimplePaths = includeReturningEdgesInSimplePaths;
    this.maxPathLength = maxPathLength;
    this.pathComparator = pathComparator;
    this.proneUnreachableEdges = proneUnreachableEdges != null ?
        proneUnreachableEdges :
        (veGraph, target) -> target.size() >= Math.max(1, Math.sqrt(graph.vertexSet().size()));
    this.pathValidator = pathValidator;
  }

  /**
   * Calculate (and return) all paths from the source vertices to the target vertices.
   * <p>
   * Since the number of paths may grow exponentially fast, it is possible to optimize the algorithm a bit by setting <@link #longestPathsOnly> true.
   * This way, the method will only return the longest paths that includes target vertices.
   * For example, given that s is a source vertex and t1, t2 are target vertices, given the path (s, v1, t1, v2, t2, v3),
   * the method will return only (s, v1, t1, v2, t2) instead of returning [(s, v1, t1), (s, v1, t1, v2, t2)].
   *
   * @return list of all paths from the sources to the targets containing no more than
   * maxPathLength edges
   */
  public List<GraphPath<V, E>> getAllPaths() {
    if ((maxPathLength != null) && (maxPathLength < 0)) {
      throw new IllegalArgumentException("maxPathLength must be non-negative if defined");
    }

    if (!simplePathsOnly && (maxPathLength == null)) {
      throw new IllegalArgumentException(
          "If search is not restricted to simple paths, a maximum path length must be set to avoid infinite cycles");
    }

    if (!simplePathsOnly && includeReturningEdgesInSimplePaths) {
      throw new IllegalArgumentException(
          "Cannot include returning edges in simple paths if search is not restricted to simple paths");
    }

    if (targetVertices == null || targetVertices.isEmpty()) {
      targetVertices = graph.vertexSet();
    }

    // Decorate the edges with the minimum path lengths through them
    Map<E, Integer> edgeMinDistancesFromTargets =
        proneUnreachableEdges.test(graph, targetVertices) ?
            edgeMinDistancesBackwards() :
            new HashMap<>() {
              @Override
              public Integer get(Object key) {
                return 1;
              }

              @Override
              public boolean containsKey(Object key) {
                return true;
              }
            };
    ;

    // Generate all the paths

    var res = generatePaths(edgeMinDistancesFromTargets);

    if (pathComparator != null)
      res.sort(pathComparator);
    return res;
  }

  private List<GraphPath<V, E>> removeAfterTargetEdges(List<GraphPath<V, E>> paths, Set<V> targetVertices) {
    var result = new ArrayList<GraphPath<V, E>>();
    for (var path : paths) {
      var vertices = path.getVertexList();
      var edges = path.getEdgeList();
      int i;
      for (i = vertices.size() - 1; i >= 0 && !targetVertices.contains(vertices.get(i)); i--) ;
      if (i > 0) {
        result.add(makePath(edges.subList(0, i - 1)));
      }
    }
    return result;
  }

  /**
   * Compute the minimum number of edges in a path to the targets through each edge, so long as it
   * is not greater than a bound.
   *
   * @return the minimum number of edges in a path from each edge to the targets, encoded in a Map
   */
  private Map<E, Integer> edgeMinDistancesBackwards() {
    /*
     * We walk backwards through the network from the target vertices, marking edges and
     * vertices with their minimum distances as we go.
     */
    Map<E, Integer> edgeMinDistances = new HashMap<>();
    Map<V, Integer> vertexMinDistances = new HashMap<>();
    Queue<V> verticesToProcess = new ArrayDeque<>();

    // Input sanity checking
    if (maxPathLength != null) {
      if (maxPathLength < 0) {
        throw new IllegalArgumentException("maxPathLength must be non-negative if defined");
      }
      if (maxPathLength == 0) {
        return edgeMinDistances;
      }
    }

    // Bootstrap the process with the target vertices
    for (V target : targetVertices) {
      vertexMinDistances.put(target, 0);
      verticesToProcess.add(target);
    }

    // Work through the node queue. When it's empty, we're done!
    for (V vertex; (vertex = verticesToProcess.poll()) != null; ) {
      assert vertexMinDistances.containsKey(vertex);

      Integer childDistance = vertexMinDistances.get(vertex) + 1;

      // Check whether the incoming edges of this node are correctly
      // decorated
      for (E edge : graph.incomingEdgesOf(vertex)) {
        // Mark the edge if needed
        if (!edgeMinDistances.containsKey(edge)
            || (edgeMinDistances.get(edge) > childDistance)) {
          edgeMinDistances.put(edge, childDistance);
        }

        // Mark the edge's source vertex if needed
        V edgeSource = graph.getEdgeSource(edge);
        if (!vertexMinDistances.containsKey(edgeSource)
            || (vertexMinDistances.get(edgeSource) > childDistance)) {
          vertexMinDistances.put(edgeSource, childDistance);

          if ((maxPathLength == null) || (childDistance < maxPathLength)) {
            verticesToProcess.add(edgeSource);
          }
        }
      }
    }

    assert verticesToProcess.isEmpty();
    return edgeMinDistances;
  }

  /**
   * Generate all paths from the sources to the targets, using pre-computed minimum distances.
   *
   * @param edgeMinDistancesFromTargets the minimum number of edges in a path to a target through
   *                                    each edge, as computed by {@code
   *                                    edgeMinDistancesBackwards}.
   * @return a List of all GraphPaths from the sources to the targets satisfying the given constraints
   */
  private List<GraphPath<V, E>> generatePaths(Map<E, Integer> edgeMinDistancesFromTargets) {
    /*
     * We walk forwards through the network from the source vertices, exploring all outgoing
     * edges whose minimum distances is small enough.
     */
    var completePaths = new ConcurrentLinkedQueue<GraphPath<V, E>>();
    final var incompletePaths = new ConcurrentLinkedQueue<List<E>>();

    // Input sanity checking
    if (maxPathLength != null && maxPathLength < 0) {
      throw new IllegalArgumentException("maxPathLength must be non-negative if defined");
    }

    for (E edge : graph.outgoingEdgesOf(sourceVertex)) {
      assert graph.getEdgeSource(edge).equals(sourceVertex);

      if (pathValidator == null || pathValidator.isValidPath(GraphWalk.emptyWalk(graph), edge)) {
        if (!longestPathsOnly && targetVertices.contains(graph.getEdgeTarget(edge))) { // We generate complete at the end
          completePaths.add(makePath(Collections.singletonList(edge)));
        }

        if (edgeMinDistancesFromTargets.containsKey(edge)
            && (maxPathLength == null || maxPathLength > 1)) {
          List<E> path = Collections.singletonList(edge);
          incompletePaths.add(path);
        }
      }
    }

    // Walk through the queue of incomplete paths
    for (List<E> incompletePath; (incompletePath = incompletePaths.poll()) != null; ) { //TODO make it parallel
      Integer lengthSoFar = incompletePath.size();
      assert (maxPathLength == null) || (lengthSoFar < maxPathLength);

      E leafEdge = incompletePath.get(lengthSoFar - 1);
      V leafNode = graph.getEdgeTarget(leafEdge);

      Set<V> pathVertices = new HashSet<>();
      for (E pathEdge : incompletePath) {
        pathVertices.add(graph.getEdgeSource(pathEdge));
        pathVertices.add(graph.getEdgeTarget(pathEdge));
      }

      var newPathFound = new AtomicBoolean(false);
      var finalIncompletePath = incompletePath;
      graph.outgoingEdgesOf(leafNode).parallelStream().forEach(outEdge -> {
        // Proceed if the outgoing edge is marked and the mark is sufficiently small
        if (edgeMinDistancesFromTargets.containsKey(outEdge) && ((maxPathLength == null)
            || ((edgeMinDistancesFromTargets.get(outEdge) + lengthSoFar) <= maxPathLength))) {
          List<E> newPath = new ArrayList<>(finalIncompletePath);
          newPath.add(outEdge);

          // If requested, validate the path
          if (pathValidator != null && !pathValidator.isValidPath(makePath(finalIncompletePath), outEdge)) {
            return;
          }

          // If requested, make sure this path isn't self-intersecting
          if (simplePathsOnly && pathVertices.contains(graph.getEdgeTarget(outEdge))) {
            if (includeReturningEdgesInSimplePaths && targetVertices.contains(graph.getEdgeTarget(outEdge))) {
              GraphPath<V, E> completePath = makePath(newPath);
              assert sourceVertex.equals(completePath.getStartVertex());
              assert targetVertices.contains(completePath.getEndVertex());
              assert maxPathLength == null || completePath.getLength() <= maxPathLength;
              completePaths.add(completePath);
            }
            return;
          }

          // If this path reaches a target, add it to completePaths
          if (!longestPathsOnly && targetVertices.contains(graph.getEdgeTarget(outEdge))) {
            GraphPath<V, E> completePath = makePath(newPath);
            assert sourceVertex.equals(completePath.getStartVertex());
            assert targetVertices.contains(completePath.getEndVertex());
            assert (maxPathLength == null)
                || (completePath.getLength() <= maxPathLength);
            completePaths.add(completePath);
          }

          // If this path is short enough, consider further extensions of it
          if ((maxPathLength == null) || (newPath.size() <= maxPathLength)) {
            newPathFound.set(true);
            incompletePaths.add(newPath);
            // We use incompletePaths in FIFO mode to avoid memory blowup
          }
        }
      });
      if (!newPathFound.get()) {
        // End of strong component or depth limit reached, add the incompletePath to Complete.
        makePath(incompletePath);
      }
    }

    assert incompletePaths.isEmpty();
    if (targetVertices.contains(sourceVertex) && (completePaths.isEmpty() || !longestPathsOnly)) {
      // pathValidator intentionally not invoked here
      completePaths.add(GraphWalk.singletonWalk(graph, sourceVertex, 0d));
    }
    List<GraphPath<V, E>> res = new ArrayList<>(completePaths);
    if (longestPathsOnly && targetVertices.size() < graph.vertexSet().size()) {
      res = removeAfterTargetEdges(res, targetVertices);
    }
    return res;
  }

  /**
   * Transform an ordered list of edges into a GraphPath.
   * <p>
   * The weight of the generated GraphPath is set to the sum of the weights of the edges.
   *
   * @param edges the edges
   * @return the corresponding GraphPath
   */
  private GraphPath<V, E> makePath(List<E> edges) {
    V source = graph.getEdgeSource(edges.get(0));
    V target = graph.getEdgeTarget(edges.get(edges.size() - 1));
    double weight = edges.stream().mapToDouble(graph::getEdgeWeight).sum();
    return new GraphWalk<>(graph, source, target, edges, weight);
  }
}