package il.ac.bgu.cs.bp.statespacemapper;

import il.ac.bgu.cs.bp.bpjs.model.BEvent;
import il.ac.bgu.cs.bp.statespacemapper.jgrapht.AllDirectedPathsBuilder;
import il.ac.bgu.cs.bp.statespacemapper.jgrapht.MapperEdge;
import il.ac.bgu.cs.bp.statespacemapper.jgrapht.MapperVertex;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MapperResult {
  public final Graph<MapperVertex, MapperEdge> graph;
  public final MapperVertex startNode;
  public final Set<MapperVertex> acceptingStates;
  public final Map<BEvent, Integer> events;

  protected MapperResult(Graph<MapperVertex, MapperEdge> graph, MapperVertex startNode, Set<MapperVertex> acceptingStates) {
    this.graph = graph;
    AtomicInteger counter = new AtomicInteger();
    events = graph.edgeSet().stream().map(MapperEdge::getEvent).distinct().collect(Collectors.toMap(Function.identity(), e -> counter.getAndIncrement()));
    this.acceptingStates = acceptingStates;
    this.startNode = startNode;
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
    return new AllDirectedPathsBuilder<>(graph, startNode, acceptingStates).setPathComparator((p1, p2) -> {
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

  @Override
  public String toString() {
    return
        "StateMapper stats:\n" +
            "======================\n" +
            "# States: " + states().size() + "\n" +
            "# Events: " + events.size() + "\n" +
            "# Transition: " + graph.edgeSet().size() + "\n" +
            "# Accepting States: " + acceptingStates.size() + "\n" +
            "======================\n";
  }

  public enum TraceGeneratorAlgorithm {
    SourceToFewTargets, SourceToManyTargets, AUTO
  }
}
