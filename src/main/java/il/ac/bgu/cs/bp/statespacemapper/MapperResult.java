package il.ac.bgu.cs.bp.statespacemapper;

import il.ac.bgu.cs.bp.bpjs.model.BEvent;
import il.ac.bgu.cs.bp.bpjs.model.BProgramSyncSnapshot;
import il.ac.bgu.cs.bp.bpjs.model.BThreadSyncSnapshot;
import il.ac.bgu.cs.bp.statespacemapper.jgrapht.AllDirectedPathsBuilder;
import il.ac.bgu.cs.bp.statespacemapper.jgrapht.MapperEdge;
import il.ac.bgu.cs.bp.statespacemapper.jgrapht.MapperVertex;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.Graphs;
import org.jgrapht.util.VertexToIntegerMapping;
import org.mozilla.javascript.NativeContinuation;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MapperResult {
  public final Graph<MapperVertex, MapperEdge> graph;
  public final Map<BEvent, Integer> events;
  public final Map<MapperVertex, Integer> vertexMapper;

  protected MapperResult(Graph<MapperVertex, MapperEdge> graph) {
    this(graph, null, null);
  }

  protected MapperResult(Graph<MapperVertex, MapperEdge> graph, MapperVertex startNode, Set<MapperVertex> acceptingStates) {
    this.graph = graph;
    AtomicInteger counter = new AtomicInteger();
    events = graph.edgeSet().stream().map(MapperEdge::getEvent).distinct().collect(Collectors.toMap(Function.identity(), e -> counter.getAndIncrement()));
    vertexMapper = new VertexToIntegerMapping<>(graph.vertexSet()).getVertexMap();
  }

  public void findBug() {
    var vertexMapping = Graphs.getVertexToIntegerMapping(graph).getVertexMap();
    var states = states().stream().filter(v -> v.accepting).collect(Collectors.toList());
    System.out.println("Finish states = " + states.stream().map(vertexMapping::get).collect(Collectors.toList()));
    if (states.size() != 2)
      return;
    var asi = states.get(0);
    var asj = states.get(1);
    var bpssi = asi.bpss;
    var bpssj = asj.bpss;
    System.out.println("bpssi.equals(bpssj) = " + bpssi.equals(bpssj));
    for (var bti : bpssi.getBThreadSnapshots()) {
      if (!bpssj.getBThreadSnapshots().contains(bti)) {
        var btName = bti.getName();
        var btj = bpssj.getBThreadSnapshots().stream().filter(snapshot -> snapshot.getName().equals(btName)).findFirst().get();
        System.out.println("Name of conflicting b-thread: " + btName);
        System.out.println("NativeContinuation.equalImplementations(bti.getContinuation(),btj.getContinuation()) = " + NativeContinuation.equalImplementations(bti.getContinuation(), btj.getContinuation()));
      }
    }
    //    System.exit(1);
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
    return graph.vertexSet().stream().filter(v -> v.accepting).collect(Collectors.toSet());
  }

  public MapperVertex startVertex() {
    var res = graph.vertexSet().stream().filter(v -> v.startVertex).findFirst();
    if (!res.isPresent()) throw new IllegalArgumentException("There is no start vertex");
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
