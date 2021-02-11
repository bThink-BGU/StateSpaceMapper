package il.ac.bgu.cs.bp.statespacemapper;

import il.ac.bgu.cs.bp.bpjs.analysis.ExecutionTrace;
import il.ac.bgu.cs.bp.bpjs.analysis.ExecutionTraceInspection;
import il.ac.bgu.cs.bp.bpjs.analysis.violations.Violation;
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
    if (stateCount == 1) {
      startNode = aTrace.getNodes().get(0).getState();
    } else {
      var src = aTrace.getNodes().get(stateCount - 2);
      Map<BProgramSyncSnapshot, Set<BEvent>> srcNode = graph.computeIfAbsent(src.getState(), k -> new HashMap<>());
      srcNode.putIfAbsent(lastNode.getState(), new HashSet<>());
      srcNode.get(lastNode.getState()).add(src.getEvent().get());
    }
    return Optional.empty();
  }

  private Collection<List<BEvent>> dfsFrom(BProgramSyncSnapshot id, ArrayDeque<BProgramSyncSnapshot> nodeStack, ArrayDeque<BEvent> eventStack) {
    var outbounds = graph.get(id);
    nodeStack.push(id);
    if (outbounds == null || outbounds.isEmpty()) {
      nodeStack.pop();
      return new ArrayList<>() {{
        add(new ArrayList<>(eventStack));
      }};
    } else {
      Collection<List<BEvent>> res = outbounds.entrySet().stream()
          .filter(o -> !nodeStack.contains(o.getKey()))
          .map(o -> {
            o.getValue().forEach(eventStack::push);
            Collection<List<BEvent>> innerDfs = dfsFrom(o.getKey(), nodeStack, eventStack);
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
        .flatMap(idArr -> graph.get(idArr[0]).get(idArr[1]).stream().map(e -> new Link(idArr[0], idArr[1], e)))
        .sorted(new LinkComparator(indexedStates))
        .collect(Collectors.toUnmodifiableList());

    var traces = dfsFrom(startNode, new ArrayDeque<>(), new ArrayDeque<>());

    return new MapperResult(indexedStates, links, traces, startNode);
  }

  public static class Link {
    public final BProgramSyncSnapshot src;
    public final BProgramSyncSnapshot dst;
    public final BEvent event;

    public Link(BProgramSyncSnapshot src, BProgramSyncSnapshot dst, BEvent event) {
      this.src = src;
      this.dst = dst;
      this.event = event;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Link link = (Link) o;
      return Objects.equals(src, link.src) && Objects.equals(dst, link.dst) && Objects.equals(event, link.event);
    }

    @Override
    public int hashCode() {
      return Objects.hash(src, dst, event);
    }
  }

  private static class LinkComparator implements Comparator<Link> {
    private final Map<BProgramSyncSnapshot, Integer> states;
    public LinkComparator(Map<BProgramSyncSnapshot, Integer> states) {
      this.states = states;
    }

    @Override
    public int compare(Link o1, Link o2) {
      if(states.get(o1.src) < states.get(o2.src)) return -1;
      if(states.get(o1.src) > states.get(o2.src)) return 1;
      if(states.get(o1.dst) < states.get(o2.dst)) return -1;
      if(states.get(o1.dst) > states.get(o2.dst)) return 1;
      return o1.event.toString().compareTo(o2.event.toString());
    }
  }

  public static class MapperResult {
    public final List<Link> links;
    public final Map<BProgramSyncSnapshot, Integer> states;
    public final Collection<List<BEvent>> traces;
    public final BProgramSyncSnapshot startNode;

    public MapperResult(Map<BProgramSyncSnapshot, Integer> states, List<Link> links, Collection<List<BEvent>> traces, BProgramSyncSnapshot startNode) {
      this.links = links;
      this.states = states;
      this.traces = traces;
      this.startNode = startNode;
    }

    @Override
    public String toString() {
      return
          "StateMapper stats\n" +
          "=================\n" +
          "# States: " + states.size() + "\n" +
          "# Transition: " + links.size() + "\n" +
          "# Traces: " + traces.size() + "\n" +
          "=================";
    }
  }
}
