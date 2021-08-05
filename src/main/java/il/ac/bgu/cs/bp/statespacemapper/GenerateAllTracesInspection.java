package il.ac.bgu.cs.bp.statespacemapper;

import il.ac.bgu.cs.bp.bpjs.analysis.ExecutionTrace;
import il.ac.bgu.cs.bp.bpjs.analysis.ExecutionTraceInspection;
import il.ac.bgu.cs.bp.bpjs.analysis.ExecutionTraceInspections;
import il.ac.bgu.cs.bp.bpjs.analysis.violations.Violation;
import il.ac.bgu.cs.bp.bpjs.model.BEvent;
import il.ac.bgu.cs.bp.bpjs.model.BProgramSyncSnapshot;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedPseudograph;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public class GenerateAllTracesInspection implements ExecutionTraceInspection {
  private final Graph<MapperVertex, MapperEdge> graph = new DirectedPseudograph<>(MapperEdge.class);
  private final Set<MapperVertex> acceptingStates = new HashSet<>();
  private MapperVertex startNode;

  @Override
  public String title() {
    return "GenerateAllTracesInspector";
  }

  @Override
  public Optional<Violation> inspectTrace(ExecutionTrace aTrace) {
    Optional<Violation> inspection = ExecutionTraceInspections.FAILED_ASSERTIONS.inspectTrace(aTrace);
    int stateCount = aTrace.getStateCount();
    var lastNode = aTrace.getNodes().get(stateCount - 1);
    var lastNodeVertex = new MapperVertex(lastNode.getState());
    if (inspection.isPresent()) {
      acceptingStates.add(lastNodeVertex);
    }
    if (aTrace.isCyclic()) {
      addEdge(aTrace.getLastState(), aTrace.getFinalCycle().get(0).getState(), aTrace.getLastEvent().get());
    } else {
      if (stateCount == 1) {
        startNode = new MapperVertex(aTrace.getNodes().get(0).getState());
      } else {
        var src = aTrace.getNodes().get(stateCount - 2);
        addEdge(new MapperVertex(src.getState()), lastNodeVertex, src.getEvent().get());
      }
    }
    if (inspection.isPresent()) {
      if (inspection.get().decsribe().contains("ContinuingAcceptingState")) {
        return Optional.empty();
      }
    }
    return inspection;
  }

  protected void addEdge(BProgramSyncSnapshot src, BProgramSyncSnapshot dst, BEvent e) {
    addEdge(new MapperVertex(src), new MapperVertex(dst), e);
  }

  protected void addEdge(MapperVertex src, MapperVertex dst, BEvent e) {
    graph.addVertex(src); //does not affect the graph if vertex exists.
    graph.addVertex(dst);
    graph.addEdge(src, dst, new MapperEdge(e)); //does not affect the graph if e already connects src and dst.
  }

  public MapperResult getResult() {
    return new MapperResult(graph, startNode, acceptingStates);
  }

  public static class MapperResult {
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

    public List<List<BEvent>> generatePaths(boolean simplePathsOnly, int maxPathLength) {
      return new AllDirectedPaths<>(graph).getAllPaths(Set.of(startNode), acceptingStates, simplePathsOnly, maxPathLength)
          .stream()
          .map(GraphPath::getEdgeList)
          .map(l -> l.stream().map(MapperEdge::getEvent).collect(Collectors.toUnmodifiableList()))
          .collect(Collectors.toUnmodifiableList());
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
          "StateMapper stats\n" +
              "=================\n" +
              "# States: " + states().size() + "\n" +
              "# Events: " + events.size() + "\n" +
              "# Transition: " + graph.edgeSet().size() + "\n" +
              "=================\n";
    }
  }

  public static class MapperVertex {
    private static final long serialVersionUID = 2565436616076859325L;
    public final BProgramSyncSnapshot bpss;

    public MapperVertex(BProgramSyncSnapshot bpss) {
      this.bpss = bpss;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      MapperVertex that = (MapperVertex) o;
      return Objects.equals(bpss, that.bpss);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(bpss);
    }
  }

  public static class MapperEdge extends DefaultEdge {
    private static final long serialVersionUID = -7176704143413508648L;
    public final BEvent event;

    public MapperEdge(BEvent event) {
      this.event = event;
    }

    public BEvent getEvent() {
      return event;
    }

    @Override
    public String toString() {
      return "(" + getSource() + " : " + getTarget() + " : " + event + ")";
    }
  }

}