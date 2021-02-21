package il.ac.bgu.cs.bp.statespacemapper;

import il.ac.bgu.cs.bp.bpjs.analysis.ExecutionTrace;
import il.ac.bgu.cs.bp.bpjs.analysis.ExecutionTraceInspection;
import il.ac.bgu.cs.bp.bpjs.analysis.violations.Violation;
import il.ac.bgu.cs.bp.bpjs.internal.Pair;
import il.ac.bgu.cs.bp.bpjs.model.BEvent;
import il.ac.bgu.cs.bp.bpjs.model.BProgramSyncSnapshot;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class GenerateAllTracesInspection implements ExecutionTraceInspection {
  /**
   * Maps <sourceNode, <targetNode, eventFromSourceToTarget>>
   */
  private final Map<BProgramSyncSnapshot, Map<BProgramSyncSnapshot, Set<BEvent>>> graph = new HashMap<>();
  private BProgramSyncSnapshot startNode;

  @Override
  public String title() {
    return "GenerateAllTracesInspector";
  }

  @Override
  public Optional<Violation> inspectTrace(ExecutionTrace aTrace) {
    int stateCount = aTrace.getStateCount();
    var lastNode = aTrace.getNodes().get(stateCount - 1);
    if (aTrace.isCyclic()) {
      addEdge(aTrace.getLastState(), aTrace.getFinalCycle().get(0).getState(), aTrace.getLastEvent().get());
    } else {
      if (stateCount == 1) {
        startNode = aTrace.getNodes().get(0).getState();
      } else {
        var src = aTrace.getNodes().get(stateCount - 2);
        addEdge(src.getState(), lastNode.getState(), src.getEvent().get());
      }
    }
    return Optional.empty();
  }

  protected void addEdge(BProgramSyncSnapshot src, BProgramSyncSnapshot dst, BEvent edge) {
    Map<BProgramSyncSnapshot, Set<BEvent>> srcNode = graph.computeIfAbsent(src, k -> new HashMap<>());
    var events = srcNode.computeIfAbsent(dst, k -> new HashSet<>());
    events.add(edge);
  }

  private Collection<List<BEvent>> dfsFrom(BProgramSyncSnapshot id, ArrayDeque<BProgramSyncSnapshot> nodeStack, ArrayDeque<BEvent> eventStack, Set<BProgramSyncSnapshot> endStates) {
    var outbounds = graph.get(id);
    nodeStack.push(id);
    if (outbounds == null || outbounds.isEmpty()) {
      nodeStack.pop();
      endStates.add(id);
      return new ArrayList<>() {{
        add(new ArrayList<>(eventStack));
      }};
    } else {
      Collection<List<BEvent>> res = outbounds.entrySet().stream()
          .filter(o -> !nodeStack.contains(o.getKey()))
          .map(o -> {
            o.getValue().forEach(eventStack::push);
            Collection<List<BEvent>> innerDfs = dfsFrom(o.getKey(), nodeStack, eventStack, endStates);
            eventStack.pop();
            return innerDfs;
          })
          .flatMap(Collection::stream)
          .collect(Collectors.toUnmodifiableList());
      nodeStack.pop();
      return res;
    }
  }

  public MapperResult getResult() {
    var states = Stream.concat(graph.keySet().stream(), graph.values().stream().flatMap(map -> map.keySet().stream())).distinct().collect(Collectors.toList());

    var indexedStates = IntStream.range(0, states.size()).boxed().collect(Collectors.toUnmodifiableMap(states::get, Function.identity()));

    var links = graph.entrySet().stream()
        .flatMap(e -> e.getValue().keySet().stream().map(c -> new BProgramSyncSnapshot[]{e.getKey(), c}))
        .flatMap(idArr -> graph.get(idArr[0]).get(idArr[1]).stream().map(e -> new Edge(indexedStates.get(idArr[0]), idArr[0], indexedStates.get(idArr[1]), idArr[1], e)))
        .sorted(new LinkComparator(indexedStates))
        .collect(Collectors.toUnmodifiableList());

    var tmpEndStates = new HashSet<BProgramSyncSnapshot>();

    var traces = dfsFrom(startNode, new ArrayDeque<>(), new ArrayDeque<>(), tmpEndStates);

    var endStates = tmpEndStates.stream().map(bpss-> new Pair<>(indexedStates.get(bpss), bpss)).collect(Collectors.toList());

    return new MapperResult(indexedStates, links, traces, startNode, endStates);
  }

  public static class Edge {
    public final int srcId;
    public final BProgramSyncSnapshot src;
    public final int dstId;
    public final BProgramSyncSnapshot dst;
    public final BEvent event;

    public Edge(int srcId, BProgramSyncSnapshot src, int dstId, BProgramSyncSnapshot dst, BEvent event) {
      this.srcId = srcId;
      this.src = src;
      this.dstId = dstId;
      this.dst = dst;
      this.event = event;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Edge edge = (Edge) o;
      return Objects.equals(src, edge.src) && Objects.equals(dst, edge.dst) && Objects.equals(event, edge.event);
    }

    @Override
    public int hashCode() {
      return Objects.hash(src, dst, event);
    }
  }

  private static class LinkComparator implements Comparator<Edge> {
    private final Map<BProgramSyncSnapshot, Integer> states;

    public LinkComparator(Map<BProgramSyncSnapshot, Integer> states) {
      this.states = states;
    }

    @Override
    public int compare(Edge o1, Edge o2) {
      if (states.get(o1.src) < states.get(o2.src)) return -1;
      if (states.get(o1.src) > states.get(o2.src)) return 1;
      if (states.get(o1.dst) < states.get(o2.dst)) return -1;
      if (states.get(o1.dst) > states.get(o2.dst)) return 1;
      return o1.event.toString().compareTo(o2.event.toString());
    }
  }

  public static class MapperResult {
    public final List<Edge> edges;
    public final Map<BProgramSyncSnapshot, Integer> states;
    public final Collection<List<BEvent>> traces;
    public final BProgramSyncSnapshot startNode;
    public final int startNodeId;
    public final List<Pair<Integer, BProgramSyncSnapshot>> endStates;

    public MapperResult(Map<BProgramSyncSnapshot, Integer> states, List<Edge> edges, Collection<List<BEvent>> traces, BProgramSyncSnapshot startNode, List<Pair<Integer, BProgramSyncSnapshot>> endStates) {
      this.edges = edges;
      this.states = states;
      this.traces = traces;
      this.startNode = startNode;
      this.startNodeId = states.get(startNode);
      this.endStates = endStates;
    }

    @Override
    public String toString() {
      return
          "StateMapper stats\n" +
              "=================\n" +
              "# States: " + states.size() + "\n" +
              "# Transition: " + edges.size() + "\n" +
              "# Traces: " + traces.size() + "\n" +
              "=================";
    }
  }
}
