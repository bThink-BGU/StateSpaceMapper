package il.ac.bgu.cs.bp.statespacemapper;

import il.ac.bgu.cs.bp.bpjs.model.BEvent;
import il.ac.bgu.cs.bp.bpjs.model.BThreadSyncSnapshot;
import il.ac.bgu.cs.bp.statespacemapper.jgrapht.AllDirectedPathsBuilder;
import il.ac.bgu.cs.bp.statespacemapper.jgrapht.MapperEdge;
import il.ac.bgu.cs.bp.statespacemapper.jgrapht.MapperVertex;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.mozilla.javascript.NativeContinuation;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MapperResult {
  public final Graph<MapperVertex, MapperEdge> graph;
  public final Map<BEvent, Integer> events;

  protected MapperResult(Graph<MapperVertex, MapperEdge> graph) {
    this(graph, null, null);
  }

  protected MapperResult(Graph<MapperVertex, MapperEdge> graph, MapperVertex startNode, Set<MapperVertex> acceptingStates) {
    this.graph = graph;
    AtomicInteger counter = new AtomicInteger();
    events = graph.edgeSet().stream().map(MapperEdge::getEvent).distinct().collect(Collectors.toMap(Function.identity(), e -> counter.getAndIncrement()));
    findBug();
  }

  private void findBug() {
    /*var acceptingStates = acceptingVertices();
    for (var ss1 : acceptingStates) {
      for (var ss2 : acceptingStates) {
        if (ss1 != ss2 &&
            indexedStates.get(ss1).equals(indexedStates.get(ss2)) &&
            !ss1.equals(ss2) &&
            states.stream().filter(s -> s.equals(ss1)).count() > 1
        ) {
          var listSS1 = states.stream().filter(s -> s.equals(ss1)).collect(Collectors.toList());
          System.out.println("listSS1.get(0).equals(listSS1.get(1)) = " + listSS1.get(0).equals(listSS1.get(1)));
          System.out.println("listSS1.get(1).equals(listSS1.get(0)) = " + listSS1.get(1).equals(listSS1.get(0)));
          BThreadSyncSnapshot list0bt=null;
          BThreadSyncSnapshot list1bt=null;
          BThreadSyncSnapshot ss1bt=null;
          BThreadSyncSnapshot ss2bt=null;
          for (var bt : listSS1.get(0).getBThreadSnapshots()) {
            if(!listSS1.get(1).getBThreadSnapshots().contains(bt)){
              list0bt = bt;
              list1bt = listSS1.get(1).getBThreadSnapshots().stream().filter(snapshot->snapshot.getName().equals(bt.getName())).findFirst().get();
              ss1bt = ss1.getBThreadSnapshots().stream().filter(snapshot->snapshot.getName().equals(bt.getName())).findFirst().get();
              ss2bt = ss2.getBThreadSnapshots().stream().filter(snapshot->snapshot.getName().equals(bt.getName())).findFirst().get();
            }
          }
          System.out.println("Name of conflicting b-thread: "+ss1bt.getName());
          System.out.println("NativeContinuation.equalImplementations(list0bt.getContinuation(),list1bt.getContinuation()) = " + NativeContinuation.equalImplementations(list0bt.getContinuation(),list1bt.getContinuation()));
          System.out.println("NativeContinuation.equalImplementations(ss1bt.getContinuation(),list0bt.getContinuation()) = " + NativeContinuation.equalImplementations(ss1bt.getContinuation(),list0bt.getContinuation()));
          System.out.println("NativeContinuation.equalImplementations(ss1bt.getContinuation(),list1bt.getContinuation()) = " + NativeContinuation.equalImplementations(ss1bt.getContinuation(),list1bt.getContinuation()));
          System.out.println("NativeContinuation.equalImplementations(ss2bt.getContinuation(),list0bt.getContinuation()) = " + NativeContinuation.equalImplementations(ss2bt.getContinuation(),list0bt.getContinuation()));
          System.out.println("NativeContinuation.equalImplementations(ss2bt.getContinuation(),list1bt.getContinuation()) = " + NativeContinuation.equalImplementations(ss2bt.getContinuation(),list1bt.getContinuation()));
          System.out.println("listSS1.get(0).equals(ss1) = " + listSS1.get(0).equals(ss1));
          System.out.println("ss1.equals(listSS1.get(0)) = " + ss1.equals(listSS1.get(0)));
          System.out.println("listSS1.get(1).equals(ss1) = " + listSS1.get(1).equals(ss1));
          System.out.println("ss1.equals(listSS1.get(1)) = " + ss1.equals(listSS1.get(1)));
          System.out.println("listSS1.get(0).equals(ss2) = " + listSS1.get(0).equals(ss2));
          System.out.println("ss2.equals(listSS1.get(0)) = " + ss2.equals(listSS1.get(0)));
          System.out.println("listSS1.get(1).equals(ss2) = " + listSS1.get(1).equals(ss2));
          System.out.println("ss2.equals(listSS1.get(1)) = " + ss2.equals(listSS1.get(1)));
          System.exit(1);
//          }
        }
      }
    }*/
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
