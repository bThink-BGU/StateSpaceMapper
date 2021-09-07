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
    CrossComponentIterator<V, E, AllDirectedPathsDFS.VisitColor> {
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
  protected static enum VisitColor {
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

  private Deque<Object> verticeStack = new ArrayDeque<>();
  private Deque<Object> edgeStack = new ArrayDeque<>();
  private List<GraphPath<V, E>> paths = new ArrayList<>();

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
    return paths;
  }

  @Override
  protected boolean isConnectedComponentExhausted() {
    for (; ; ) {
      if (verticeStack.isEmpty()) {
        return true;
      }
      if (verticeStack.getLast() != SENTINEL) {
        // Found a non-sentinel.
        return false;
      }

      // Found a sentinel: pop it, record the finish time,
      // and then loop to check the rest of the stack.

      // Pop null we peeked at above.
      verticeStack.removeLast();
      edgeStack.removeLast();

      // This will pop corresponding vertex to be recorded as finished.
      recordFinish();
    }
  }

  @Override
  protected void encounterVertex(V vertex, E edge) {
    putSeenData(vertex, VisitColor.WHITE);
    if (edge != null)
      edgeStack.addLast(edge);
    verticeStack.addLast(vertex);
  }

  @Override
  protected void encounterVertexAgain(V vertex, E edge) {
    VisitColor color = getSeenData(vertex);
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
//    boolean found = verticeStack.removeLastOccurrence(vertex);
//    assert (found);
    verticeStack.addLast(vertex);
    edgeStack.addLast(edge);
  }

  @Override
  protected V provideNextVertex() {
    V v;
    E e;
    for (; ; ) {
      Object ov = verticeStack.removeLast();
      Object oe = edgeStack.isEmpty() ? null : edgeStack.removeLast();
      if (ov == SENTINEL) {
        // This is a finish-time sentinel we previously pushed.
        recordFinish();
        // Now carry on with another pop until we find a non-sentinel
      } else {
        // Got a real vertex to start working on
        v = TypeUtil.uncheckedCast(ov);
        e = oe == null ? null : TypeUtil.uncheckedCast(oe);
        break;
      }
    }

    // Push a sentinel for v onto the stack so that we'll know
    // when we're done with it.
    verticeStack.addLast(v);
    if (e != null)
      edgeStack.addLast(e);
    verticeStack.addLast(SENTINEL);
    edgeStack.addLast(SENTINEL);
    putSeenData(v, VisitColor.GRAY);
    return v;
  }

  private void recordFinish() {
    makePath();
    V v = TypeUtil.uncheckedCast(verticeStack.removeLast());
    if (!edgeStack.isEmpty())
      edgeStack.removeLast();
    //turn it back to white for other paths
    putSeenData(v, VisitColor.WHITE);
    // putSeenData(v, VisitColor.BLACK);

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
  public Deque<Object> getVerticeStack() {
    return verticeStack;
  }

  private void makePath() {
    assert verticeStack.size() == edgeStack.size() + 1;
    var vList = new ArrayList<>(verticeStack);
    var eList = new ArrayList<>(edgeStack);
    var vertexList = new ArrayList<V>();
    var edgeList = new ArrayList<E>();
    for (int i = 0; i < vList.size(); i++) {
      if (vList.get(i) != SENTINEL && getSeenData(TypeUtil.uncheckedCast(vList.get(i))) == VisitColor.GRAY) {
        vertexList.add(TypeUtil.uncheckedCast(vList.get(i)));
        if (i > 0)
          edgeList.add(TypeUtil.uncheckedCast(eList.get(i - 1)));
      }
    }
    double weight = edgeList.stream().mapToDouble(graph::getEdgeWeight).sum();
    paths.add(new GraphWalk<>(graph, vertexList.get(0), vertexList.get(vertexList.size() - 1), vertexList, edgeList, weight));
  }
}