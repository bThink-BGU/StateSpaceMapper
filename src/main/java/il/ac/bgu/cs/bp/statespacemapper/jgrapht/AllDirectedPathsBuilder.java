package il.ac.bgu.cs.bp.statespacemapper.jgrapht;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.PathValidator;

import java.util.Comparator;
import java.util.Set;
import java.util.function.BiPredicate;

public class AllDirectedPathsBuilder<V, E> {
  private final Graph<V, E> graph;
  private final V sourceVertex;
  private final Set<V> targetVertices;
  private PathValidator<V, E> pathValidator;
  private BiPredicate<Graph<V, E>, Set<V>> proneUnreachableEdges;
  private boolean longestPathsOnly;
  private boolean simplePathsOnly;
  private boolean includeReturningEdgesInSimplePaths;
  private Integer maxPathLength;
  private Comparator<GraphPath<V, E>> pathComparator;

  /**
   * Create an AllDirectedPathsBuilder
   *
   * @param graph          the input graph
   * @param sourceVertex   the source vertex
   * @param targetVertices the target vertices
   */
  public AllDirectedPathsBuilder(Graph<V, E> graph, V sourceVertex, Set<V> targetVertices) {
    this.graph = graph;
    this.sourceVertex = sourceVertex;
    this.targetVertices = targetVertices;
  }

  /**
   * Set a validator for computed paths; may be null
   */
  public AllDirectedPathsBuilder<V, E> setPathValidator(PathValidator<V, E> pathValidator) {
    this.pathValidator = pathValidator;
    return this;
  }

  /**
   * Set a predicate that gets a graph and a set of target vertices and decides whether to prone unreachable edges.
   * <p>
   * The path generation is done using a BFS-like algorithm.
   * It is possible to improve the algorithm by pruning edges that do not lead to a target vertex.
   * However, the pruning is <b>not recommended</b> if the number of target vertices is large.
   * By default, the pruning is done iff the number of target vertices is smaller than {@code Math.max(1, Math.sqrt(graph.vertexSet().size()))}.
   */
  public AllDirectedPathsBuilder<V, E> setProneUnreachableEdges(BiPredicate<Graph<V, E>, Set<V>> proneUnreachableEdges) {
    this.proneUnreachableEdges = proneUnreachableEdges;
    return this;
  }

  /**
   * Set the {@param AllDirectedPaths#longestPathsOnly}. If true, only return the longest paths that includes target vertices.
   */
  public AllDirectedPathsBuilder<V, E> setLongestPathsOnly(boolean longestPathsOnly) {
    this.longestPathsOnly = longestPathsOnly;
    return this;
  }

  /**
   * Set the {@param AllDirectedPaths#simplePathsOnly}. If true, only search simple (non-self-intersecting) paths
   */
  public AllDirectedPathsBuilder<V, E> setSimplePathsOnly(boolean simplePathsOnly) {
    this.simplePathsOnly = simplePathsOnly;
    return this;
  }

  /**
   * Set the maximum number of edges to allow in a path (if null, all paths are considered, which may be very slow due to potentially huge output)
   */
  public AllDirectedPathsBuilder<V, E> setMaxPathLength(Integer maxPathLength) {
    this.maxPathLength = maxPathLength;
    return this;
  }

  /**
   * Set a GraphPath comparator for sorting the resulted paths
   */
  public AllDirectedPathsBuilder<V, E> setPathComparator(Comparator<GraphPath<V, E>> pathComparator) {
    this.pathComparator = pathComparator;
    return this;
  }

  /**
   * if true and also {@param simplePathsOnly} is also true, then the edge that creates the self intersecting path is also returned
   */
  public AllDirectedPathsBuilder<V, E> setIncludeReturningEdgesInSimplePaths(boolean includeReturningEdgesInSimplePaths) {
    this.includeReturningEdgesInSimplePaths = includeReturningEdgesInSimplePaths;
    return this;
  }

  /**
   * Create the {@link AllDirectedPaths} instance.
   */
  public AllDirectedPaths<V, E> build() {
    return new AllDirectedPaths<>(graph, pathValidator, proneUnreachableEdges, sourceVertex, targetVertices, longestPathsOnly, simplePathsOnly, includeReturningEdgesInSimplePaths, maxPathLength, pathComparator);
  }
}