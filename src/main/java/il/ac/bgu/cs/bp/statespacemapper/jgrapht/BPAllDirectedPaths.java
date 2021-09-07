package il.ac.bgu.cs.bp.statespacemapper.jgrapht;


import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.GraphTests;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;
import org.jgrapht.alg.shortestpath.PathValidator;
import org.jgrapht.graph.GraphWalk;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A BFS-like algorithm to find all paths from a source node in a directed graph, with
 * options to search only simple paths and to limit the path length.
 *
 * @param <V> the graph vertex type
 * @param <E> the graph edge type
 * @author Andrew Gainer-Dewar, Google LLC
 */
public class BPAllDirectedPaths<V, E> {
  private final Graph<V, E> graph;

  /**
   * Provides validation for the paths which will be computed. If the validator is {@code null},
   * this means that all paths are valid.
   */
  private final PathValidator<V, E> pathValidator;

  /**
   * Create a new instance.
   *
   * @param graph the input graph
   * @throws IllegalArgumentException if the graph is not directed
   */
  public BPAllDirectedPaths(Graph<V, E> graph) {
    this(graph, null);
  }

  /**
   * Create a new instance with given {@code pathValidator}.
   * <p>
   * If non-{@code null}, the {@code pathValidator} will be used while searching for paths, validating the addition
   * of any edge to a partial path. Zero-length paths will therefore not be subject to {@code pathValidator};
   * length-1 paths will.
   *
   * @param graph         the input graph
   * @param pathValidator validator for computed paths; may be null
   * @throws IllegalArgumentException if the graph is not directed
   */
  public BPAllDirectedPaths(Graph<V, E> graph, PathValidator<V, E> pathValidator) {
    this.graph = GraphTests.requireDirected(graph);
    this.pathValidator = pathValidator;
  }

  /**
   * Calculate (and return) all paths from the source vertices to the target vertices.
   *
   * @param sourceVertex    the source vertices
   * @param targetVertices  the target vertices
   * @param simplePathsOnly if true, only search simple (non-self-intersecting) paths
   * @param maxPathLength   maximum number of edges to allow in a path (if null, all paths are
   *                        considered, which may be very slow due to potentially huge output)
   * @return list of all paths from the sources to the targets containing no more than
   * maxPathLength edges
   */
  public List<GraphPath<V, E>> getAllPaths(V sourceVertex, Set<V> targetVertices, boolean simplePathsOnly, Integer maxPathLength) {
    if ((maxPathLength != null) && (maxPathLength < 0)) {
      throw new IllegalArgumentException("maxPathLength must be non-negative if defined");
    }

    if (!simplePathsOnly && (maxPathLength == null)) {
      throw new IllegalArgumentException(
          "If search is not restricted to simple paths, a maximum path length must be set to avoid infinite cycles");
    }

    if (targetVertices.isEmpty()) {
      return Collections.emptyList();
    }

    // If there are only few target vertices, then use AllDirectedPaths.
    if (targetVertices.size() <= Math.max(1, Math.sqrt(graph.vertexSet().size()))) {
      return new AllDirectedPaths<>(graph).getAllPaths(Set.of(sourceVertex), targetVertices, simplePathsOnly, maxPathLength);
    }

    return generateLongestPaths(sourceVertex, targetVertices, simplePathsOnly, maxPathLength)
        .stream()
        .filter(p -> p.getVertexList().stream().anyMatch(targetVertices::contains))
        .map(p -> {
          var vertices = p.getVertexList();
          int i;
          for (i = vertices.size() - 1; i >= 0; i--) {
            if (targetVertices.contains(vertices.get(i))) break;
          }
          return makePath(p.getEdgeList().subList(0, i));
        })
        .filter(p -> p.getLength() > 0)
        .collect(Collectors.toList());
  }

  private List<GraphPath<V, E>> generateLongestPaths(V sourceVertex, Set<V> targetVertices, boolean simplePathsOnly, Integer maxPathLength) {
    /*
     * We walk forwards through the network from the source vertices, exploring all outgoing
     * edges whose minimum distances is small enough.
     */
    List<GraphPath<V, E>> completePaths = new ArrayList<>();
    Deque<List<E>> incompletePaths = new LinkedList<>();

    // Input sanity checking
    if (maxPathLength != null && maxPathLength < 0) {
      throw new IllegalArgumentException("maxPathLength must be non-negative if defined");
    }

    // Bootstrap the search with the source vertices

    /*if (targetVertices.contains(sourceVertex)) {
      // pathValidator intentionally not invoked here
      completePaths.add(GraphWalk.singletonWalk(graph, sourceVertex, 0d));
    }*/

    if (maxPathLength == null || maxPathLength != 0) {
      for (E edge : graph.outgoingEdgesOf(sourceVertex)) {
        assert graph.getEdgeSource(edge).equals(sourceVertex);

        if (pathValidator == null || pathValidator.isValidPath(GraphWalk.emptyWalk(graph), edge)) {
          /*if (targetVertices.contains(graph.getEdgeTarget(edge))) {
            completePaths.add(makePath(Collections.singletonList(edge)));
          }*/

          if (maxPathLength == null || maxPathLength > 1) {
            List<E> path = Collections.singletonList(edge);
            incompletePaths.add(path);
          }
        }
      }
    }

    if (maxPathLength != null && maxPathLength == 0) {
      return completePaths;
    }

    // Walk through the queue of incomplete paths
    for (List<E> incompletePath; (incompletePath = incompletePaths.poll()) != null; ) {
      int lengthSoFar = incompletePath.size();
      assert (maxPathLength == null) || (lengthSoFar < maxPathLength);

      E leafEdge = incompletePath.get(lengthSoFar - 1);
      V leafNode = graph.getEdgeTarget(leafEdge);

      Set<V> pathVertices = new HashSet<>();
      for (E pathEdge : incompletePath) {
        pathVertices.add(graph.getEdgeSource(pathEdge));
        pathVertices.add(graph.getEdgeTarget(pathEdge));
      }

      boolean newPathFound = false;
      for (E outEdge : graph.outgoingEdgesOf(leafNode)) {
        List<E> newPath = new ArrayList<>(incompletePath);
        newPath.add(outEdge);

        // If requested, make sure this path isn't self-intersecting
        if (simplePathsOnly && pathVertices.contains(graph.getEdgeTarget(outEdge))) {
          continue;
        }

        // If requested, validate the path
        if (pathValidator != null && !pathValidator.isValidPath(makePath(incompletePath), outEdge)) {
          continue;
        }

        // If this path is short enough, consider further
        // extensions of it
        if ((maxPathLength == null) || (newPath.size() < maxPathLength)) {
          newPathFound = true;
          incompletePaths.addFirst(newPath); // We use
          // incompletePaths in
          // FIFO mode to avoid
          // memory blowup
        }
      }
      if (!newPathFound) {
        // End of strong component or depth limit reached, add the incompletePath to Complete.
        addPathToComplete(sourceVertex, maxPathLength, completePaths, incompletePath);
      }
    }

    assert incompletePaths.isEmpty();
    return completePaths;
  }

  private void addPathToComplete(V sourceVertex, Integer maxPathLength, List<GraphPath<V, E>> completePaths, List<E> newPath) {
    GraphPath<V, E> completePath = makePath(newPath);
    assert sourceVertex.equals(completePath.getStartVertex());
    assert (maxPathLength == null)
        || (completePath.getLength() <= maxPathLength);
    completePaths.add(completePath);
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
    if (edges.isEmpty()) return GraphWalk.emptyWalk(graph);
    V source = graph.getEdgeSource(edges.get(0));
    V target = graph.getEdgeTarget(edges.get(edges.size() - 1));
    double weight = edges.stream().mapToDouble(graph::getEdgeWeight).sum();
    return new GraphWalk<>(graph, source, target, edges, weight);
  }
}
