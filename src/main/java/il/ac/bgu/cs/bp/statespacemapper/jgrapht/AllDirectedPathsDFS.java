/*
 * (C) Copyright 2003-2021, by Liviu Rau and Contributors.
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
import org.jgrapht.graph.GraphWalk;
import org.jgrapht.traverse.CrossComponentIterator;
import org.jgrapht.util.TypeUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A DFS-based algorithm to find all paths from a source vertex to a set of nodes in a directed graph, with options to search only simple paths and to limit the path length.
 * The algorithm is based on org.jgrapht.traverse.DepthFirstIterator
 * <p>
 * Type parameters:
 * <V> – the graph vertex type
 * <E> – the graph edge type
 *
 * @author Achiya Elyasaf
 * @author Liviu Rau
 * @author Barak Naveh
 */
public class AllDirectedPathsDFS<V, E>
    extends
    CrossComponentIterator<V, E, AllDirectedPathsDFS.DfsVertexData<V, E>> {
  /**
   * Sentinel object. Unfortunately, we can't use null, because ArrayDeque won't accept those. And
   * we don't want to rely on the caller to provide a sentinel object for us. So we have to play
   * typecasting games.
   */
  public static final Object SENTINEL = new Object();
  private final Set<V> targetVertices;

  /**
   * Standard vertex visit state enumeration.
   */
  protected enum VisitColor {
    /**
     * Vertex has not been returned via iterator yet.
     */
    WHITE,

    /**
     * Vertex has been returned via iterator, but we're not done with all of its out-edges yet.
     */
    GRAY,

    /**
     * Vertex has been returned via iterator, and we're done with all of its out-edges.
     */
    BLACK
  }

  protected static class DfsVertexData<V, E> {
    public final LinkedList<GraphPath<V, E>> paths = new LinkedList<>();
    public VisitColor color = VisitColor.WHITE;

    public DfsVertexData(GraphPath<V, E> path) {
      if (path != null)
        paths.add(path);
    }

    public void addPath(GraphPath<V, E> path) {
      paths.add(path);
    }

    public E getLastEvent() {
      if (paths.isEmpty()) return null;
      var edges = paths.getLast().getEdgeList();
      return edges.get(edges.size() - 1);
    }
  }

  private Deque<Object> stack = new ArrayDeque<>();
  private Deque<E> edgeStack = new ArrayDeque<>();

  /**
   * Creates a new depth-first iterator for the specified graph. Iteration will start at the
   * specified start vertex and will be limited to the connected component that includes that
   * vertex. If the specified start vertex is <code>null</code>, iteration will start at an
   * arbitrary vertex and will not be limited, that is, will be able to traverse all the graph.
   *
   * @param g              the graph to be iterated.
   * @param startVertex    the vertex iteration to be started.
   * @param targetVertices the target vertices.
   */
  public AllDirectedPathsDFS(Graph<V, E> g, V startVertex, Set<V> targetVertices) {
    super(g, startVertex);
    this.targetVertices = targetVertices;
  }


  public List<GraphPath<V, E>> getAllPaths() {
    // iterate all graph:
    while (this.hasNext()) this.next();
    // return paths:
    return targetVertices.stream()
        .filter(v -> getSeenData(v) != null)
        .map(v -> getSeenData(v).paths)
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }


  @Override
  protected boolean isConnectedComponentExhausted() {
    for (; ; ) {
      if (stack.isEmpty()) {
        return true;
      }
      if (stack.getLast() != SENTINEL) {
        // Found a non-sentinel.
        return false;
      }

      // Found a sentinel: pop it, record the finish time,
      // and then loop to check the rest of the stack.

      // Pop null we peeked at above.
      stack.removeLast();

      // This will pop corresponding vertex to be recorded as finished.
      recordFinish();
    }
  }

  private GraphPath<V, E> pathTo(V vertex, E edge) {
    if (stack.size() == 0)
      return null;
    var vertexList = stack.stream()
        .sequential() //redundant, but just for case...
        .filter(o -> o != SENTINEL)
        .map(TypeUtil::<V>uncheckedCast)
        .filter(v -> getSeenData(v).color == VisitColor.GRAY).collect(Collectors.toList());
    vertexList.add(vertex);
    var edgeList = new ArrayList<>(edgeStack);
    if (edge != null)
      edgeList.add(edge);
    return new GraphWalk<>(graph, TypeUtil.uncheckedCast(stack.getFirst()), vertex, vertexList, edgeList, 0);
  }

  @Override
  protected void encounterVertex(V vertex, E edge) {
    var data = new DfsVertexData<>(pathTo(vertex, edge));
    putSeenData(vertex, data);
    stack.addLast(vertex);
  }

  @Override
  protected void encounterVertexAgain(V vertex, E edge) {
    var data = getSeenData(vertex);
    data.addPath(pathTo(vertex, edge));
    VisitColor color = data.color;
    if (color != VisitColor.WHITE) {
      // We've already visited this vertex; no need to mess with the
      // stack (either it's BLACK and not there at all, or it's GRAY
      // and therefore just a sentinel).
      return;
    }

    // Since we've encountered it before, and it's still WHITE, it
    // *must* be on the stack. Use removeLastOccurrence on the
    // assumption that for typical topologies and traversals,
    // it's likely to be nearer the top of the stack than
    // the bottom of the stack.
    boolean found = stack.removeLastOccurrence(vertex);
    assert (found);
    stack.addLast(vertex);
  }

  @Override
  protected V provideNextVertex() {
    V v;
    for (; ; ) {
      Object o = stack.removeLast();
      if (o == SENTINEL) {
        // This is a finish-time sentinel we previously pushed.
        recordFinish();
        // Now carry on with another pop until we find a non-sentinel
      } else {
        // Got a real vertex to start working on
        v = TypeUtil.uncheckedCast(o);
        break;
      }
    }

    // Push a sentinel for v onto the stack so that we'll know
    // when we're done with it.
    stack.addLast(v);
    stack.addLast(SENTINEL);
    var data = getSeenData(v);
    var lastEvent = data.getLastEvent();
    if (lastEvent != null)
      edgeStack.add(lastEvent);
    data.color = VisitColor.GRAY;
//    putSeenData(v, VisitColor.GRAY);
    return v;
  }

  private void recordFinish() {
    V v = TypeUtil.uncheckedCast(stack.removeLast());
    if (!edgeStack.isEmpty())
      edgeStack.removeLast();
    var data = getSeenData(v);
    data.color = VisitColor.BLACK;
//    putSeenData(v, VisitColor.BLACK);
    finishVertex(v);
  }

  /**
   * Retrieves the LIFO stack of vertices which have been encountered but not yet visited (WHITE).
   * This stack also contains <em>sentinel</em> entries representing vertices which have been
   * visited but are still GRAY. A sentinel entry is a sequence (v, SENTINEL), whereas a
   * non-sentinel entry is just (v).
   *
   * @return stack
   */
  public Deque<Object> getStack() {
    return stack;
  }
}