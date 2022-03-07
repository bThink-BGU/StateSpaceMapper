package il.ac.bgu.cs.bp.statespacemapper;

import il.ac.bgu.cs.bp.bpjs.model.BEvent;
import il.ac.bgu.cs.bp.statespacemapper.jgrapht.AllDirectedPathsBuilder;
import il.ac.bgu.cs.bp.statespacemapper.jgrapht.MapperEdge;
import il.ac.bgu.cs.bp.statespacemapper.jgrapht.MapperVertex;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.util.VertexToIntegerMapping;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MapperResult {
  public final Graph<MapperVertex, MapperEdge> graph;
  public final Map<BEvent, Integer> events;
  public final Map<MapperVertex, Integer> vertexMapper;

  protected MapperResult(Graph<MapperVertex, MapperEdge> graph) {
    this.graph = graph;
    AtomicInteger counter = new AtomicInteger();
    events = graph.edgeSet().stream().map(MapperEdge::getEvent).distinct().collect(Collectors.toMap(Function.identity(), e -> counter.getAndIncrement()));
    vertexMapper = Collections.unmodifiableMap(new VertexToIntegerMapping<>(graph.vertexSet()).getVertexMap());
  }

  public static List<List<BEvent>> GraphPaths2BEventPaths(List<GraphPath<MapperVertex, MapperEdge>> paths) {
    return paths.stream()
        .map(GraphPath::getEdgeList)
        .map(l -> l.stream().map(MapperEdge::getEvent)
            .collect(Collectors.toUnmodifiableList())).collect(Collectors.toUnmodifiableList());
  }

  /**
   * Create an {@link AllDirectedPathsBuilder} for generating a list of all directed paths in the graph.
   * The builder has options for setting the algorithm's parameters.
   * <p>
   * By default, the {@link AllDirectedPathsBuilder#setPathComparator(Comparator)} is set to compare the paths lexicographic according to the event's name.
   */
  public AllDirectedPathsBuilder<MapperVertex, MapperEdge> createAllDirectedPathsBuilder() {
    return new AllDirectedPathsBuilder<>(graph, startVertex(), acceptingVertices()).setPathComparator((p1, p2) -> {
      var o1 = p1.getEdgeList();
      var o2 = p2.getEdgeList();
      for (int i = 0; i < o1.size(); i++) {
        if (i == o2.size()) return 1;
        var c = o1.get(i).toString().compareTo(o2.get(i).toString());
        if (c != 0) return c;
      }
      if (o2.size() > o1.size()) return -1;
      return 0;
    });
  }

  public Set<MapperVertex> states() {
    return graph.vertexSet();
  }

  public Set<MapperEdge> edges() {
    return graph.edgeSet();
  }

  public Set<MapperVertex> acceptingVertices() {
    return graph.vertexSet().stream().filter(v->v.accepting).collect(Collectors.toSet());
  }

  public MapperVertex startVertex() {
    var res = graph.vertexSet().stream().filter(v->v.startVertex).findFirst();
    if(!res.isPresent()) throw new IllegalArgumentException("There is no start vertex");
    return res.get();
  }

  @Override
  public String toString() {
    return
        "StateMapper stats:\n" +
            "======================\n" +
            "# States: " + states().size() + "\n" +
            "# Events: " + events.size() + "\n" +
            "# Transition: " + edges().size() + "\n" +
            "# Accepting States: " + acceptingVertices().size() + "\n" +
            "======================\n";
  }
}
