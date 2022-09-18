package il.ac.bgu.cs.bp.statespacemapper;

import il.ac.bgu.cs.bp.bpjs.BPjs;
import il.ac.bgu.cs.bp.bpjs.analysis.ExecutionTrace;
import il.ac.bgu.cs.bp.bpjs.analysis.ExecutionTraceInspection;
import il.ac.bgu.cs.bp.bpjs.analysis.ExecutionTraceInspections;
import il.ac.bgu.cs.bp.bpjs.analysis.violations.Violation;
import il.ac.bgu.cs.bp.bpjs.bprogramio.*;
import il.ac.bgu.cs.bp.bpjs.execution.jsproxy.BProgramJsProxy;
import il.ac.bgu.cs.bp.bpjs.internal.Pair;
import il.ac.bgu.cs.bp.bpjs.model.BEvent;
import il.ac.bgu.cs.bp.bpjs.model.BProgram;
import il.ac.bgu.cs.bp.bpjs.model.BProgramSyncSnapshot;
import il.ac.bgu.cs.bp.bpjs.model.BThreadSyncSnapshot;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeContinuation;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class GenerateAllTracesInspection implements ExecutionTraceInspection {
  /**
   * Maps <sourceNode, Map<targetNode, eventFromSourceToTarget>>
   */
  private final Map<BProgramSyncSnapshot, Map<BProgramSyncSnapshot, Set<BEvent>>> graph = new HashMap<>();
  private final Set<BProgramSyncSnapshot> acceptingStates = new HashSet<>();
  private BProgramSyncSnapshot startNode;

  private boolean generateTraces = true;

  @Override
  public String title() {
    return "GenerateAllTracesInspector";
  }

  @Override
  public Optional<Violation> inspectTrace(ExecutionTrace aTrace) {
    Optional<Violation> inspection = ExecutionTraceInspections.FAILED_ASSERTIONS.inspectTrace(aTrace);
    int stateCount = aTrace.getStateCount();
    var lastNode = aTrace.getNodes().get(stateCount - 1);
    if (inspection.isPresent()) {
      acceptingStates.add(lastNode.getState());
    }
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
    if (inspection.isPresent()) {
      if (inspection.get().decsribe().contains("ContinuingAcceptingState")) {
        return Optional.empty();
      }
    }
    return inspection;
  }

  protected void addEdge(BProgramSyncSnapshot src, BProgramSyncSnapshot dst, BEvent edge) {
    Map<BProgramSyncSnapshot, Set<BEvent>> srcNode = graph.computeIfAbsent(src, k -> new HashMap<>());
    var events = srcNode.computeIfAbsent(dst, k -> new HashSet<>());
    events.add(edge);
  }

  public boolean isGenerateTraces() {
    return generateTraces;
  }

  public void setGenerateTraces(boolean generateTraces) {
    this.generateTraces = generateTraces;
  }

  private void dfsFrom(ArrayDeque<BProgramSyncSnapshot> nodeStack, ArrayDeque<BEvent> eventStack, Set<BProgramSyncSnapshot> endStates, List<List<BEvent>> paths) {
    var id = nodeStack.pop();
    var outbounds = graph.get(id);
    if (outbounds == null || outbounds.isEmpty() || this.acceptingStates.contains(id) || nodeStack.contains(id)) {
      endStates.add(id);
      var l = new ArrayList<>(eventStack);
      Collections.reverse(l);
      paths.add(l);
    }

    if (outbounds != null && !outbounds.isEmpty() && !nodeStack.contains(id)) {
      nodeStack.push(id);
      outbounds.entrySet().stream()
          .flatMap(entry -> entry.getValue().stream().map(e -> new Pair<>(entry.getKey(), e)))
          .forEach(p -> {
            nodeStack.push(p.getLeft());
            eventStack.push(p.getRight());
            dfsFrom(nodeStack, eventStack, endStates, paths);
            eventStack.pop();
            nodeStack.pop();
          });
    } else {
      nodeStack.push(id);
    }
  }

  private List<List<BEvent>> dfsFrom(BProgramSyncSnapshot startNode, HashSet<BProgramSyncSnapshot> tmpEndStates) {
    var paths = new ArrayList<List<BEvent>>();
    dfsFrom(new ArrayDeque<>() {{
      push(startNode);
    }}, new ArrayDeque<>(), tmpEndStates, paths);
//    Collections.reverse(paths);
    return paths;
  }

  public MapperResult getResult() {
    var dir = "bug-run" + 3;
    var states = Stream.concat(graph.keySet().stream(), graph.values().stream().flatMap(map -> map.keySet().stream())).distinct().collect(Collectors.toUnmodifiableList());

    var indexedStates = IntStream.range(0, states.size()).boxed().collect(Collectors.toUnmodifiableMap(states::get, Function.identity()));

    var links = graph.entrySet().stream()
        .flatMap(e -> e.getValue().keySet().stream().map(c -> new BProgramSyncSnapshot[]{e.getKey(), c}))
        .flatMap(idArr -> graph.get(idArr[0]).get(idArr[1]).stream().map(e -> new Edge(indexedStates.get(idArr[0]), idArr[0], indexedStates.get(idArr[1]), idArr[1], e)))
        .sorted(new LinkComparator(indexedStates))
        .collect(Collectors.toUnmodifiableList());

    var tmpEndStates = new HashSet<BProgramSyncSnapshot>();

    var traces = generateTraces ? dfsFrom(startNode, tmpEndStates)
        .stream()
        .map(l -> l.stream().collect(Collectors.toUnmodifiableList()))
        .collect(Collectors.toUnmodifiableList()) : null;

    var acceptingStates = Stream.concat(this.acceptingStates.stream(), tmpEndStates.stream())
        .distinct()
//        .map(s->new Pair<>(indexedStates.get(s), s))
//        .sorted(Comparator.comparing(Pair::getLeft))
        .collect(Collectors.toList());
//        .collect(Collectors.toUnmodifiableMap(indexedStates::get, Function.identity()));
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
          BThreadSyncSnapshot list0bt = null;
          BThreadSyncSnapshot list1bt = null;
          BThreadSyncSnapshot ss1bt = null;
          BThreadSyncSnapshot ss2bt = null;
          for (var bt : listSS1.get(0).getBThreadSnapshots()) {
            if (!listSS1.get(1).getBThreadSnapshots().contains(bt)) {
              list0bt = bt;
              list1bt = listSS1.get(1).getBThreadSnapshots().stream().filter(snapshot -> snapshot.getName().equals(bt.getName())).findFirst().get();
              ss1bt = ss1.getBThreadSnapshots().stream().filter(snapshot -> snapshot.getName().equals(bt.getName())).findFirst().get();
              ss2bt = ss2.getBThreadSnapshots().stream().filter(snapshot -> snapshot.getName().equals(bt.getName())).findFirst().get();
            }
          }
          try (FileWriter fileWriter = new FileWriter(dir + "/stdout.txt", false);
               PrintWriter out = new PrintWriter(fileWriter);) {
            out.println("Name of conflicting b-thread: " + ss1bt.getName());
            out.println("NativeContinuation.equalImplementations(list0bt.getContinuation(),list1bt.getContinuation()) = " + NativeContinuation.equalImplementations(list0bt.getContinuation(), list1bt.getContinuation()));
            out.println("NativeContinuation.equalImplementations(ss1bt.getContinuation(),list0bt.getContinuation()) = " + NativeContinuation.equalImplementations(ss1bt.getContinuation(), list0bt.getContinuation()));
            out.println("NativeContinuation.equalImplementations(ss1bt.getContinuation(),list1bt.getContinuation()) = " + NativeContinuation.equalImplementations(ss1bt.getContinuation(), list1bt.getContinuation()));
            out.println("NativeContinuation.equalImplementations(ss2bt.getContinuation(),list0bt.getContinuation()) = " + NativeContinuation.equalImplementations(ss2bt.getContinuation(), list0bt.getContinuation()));
            out.println("NativeContinuation.equalImplementations(ss2bt.getContinuation(),list1bt.getContinuation()) = " + NativeContinuation.equalImplementations(ss2bt.getContinuation(), list1bt.getContinuation()));
            out.println("listSS1.get(0).equals(ss1) = " + listSS1.get(0).equals(ss1));
            out.println("ss1.equals(listSS1.get(0)) = " + ss1.equals(listSS1.get(0)));
            out.println("listSS1.get(1).equals(ss1) = " + listSS1.get(1).equals(ss1));
            out.println("ss1.equals(listSS1.get(1)) = " + ss1.equals(listSS1.get(1)));
            out.println("listSS1.get(0).equals(ss2) = " + listSS1.get(0).equals(ss2));
            out.println("ss2.equals(listSS1.get(0)) = " + ss2.equals(listSS1.get(0)));
            out.println("listSS1.get(1).equals(ss2) = " + listSS1.get(1).equals(ss2));
            out.println("ss2.equals(listSS1.get(1)) = " + ss2.equals(listSS1.get(1)));
            out.flush();
            var bprog = ss1.getBProgram();
            writeContinuation(bprog, ss1bt, dir+"/ss1bt.bin");
            writeContinuation(bprog, ss2bt, dir+"/ss2bt.bin");
            writeContinuation(bprog, list0bt, dir+"/list0bt.bin");
            writeContinuation(bprog, list1bt, dir+"/list1bt.bin");
            System.out.println("found bug!");
            System.exit(1);
//          }
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }
      }
    }
    System.exit(1);
    var acceptingStatesMap = Stream.concat(this.acceptingStates.stream(), tmpEndStates.stream())
        .distinct()
        .collect(Collectors.toUnmodifiableMap(indexedStates::get, Function.identity()));

    return new MapperResult(indexedStates, links, traces, startNode, acceptingStatesMap);
  }

  private static void writeContinuation(BProgram bprog, BThreadSyncSnapshot btss, String filename) {
    try {
      BPjs.enterRhinoContext();
      try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
           BPJSStubOutputStream outs = new BPJSStubOutputStream(bytes, bprog.getGlobalScope())) {
        outs.writeObject(btss.getContinuation());
        Files.write(Path.of(filename), bytes.toByteArray());
      } catch (IOException e) {
        throw new RuntimeException("Failed to clone snapshot: " + e.getMessage(), e);
      }
    } finally {
      Context.exit();
    }
  }

  private static StubProvider getStubProvider(BProgramSyncSnapshot bprogram) {
    BProgramJsProxy bpProxy = bprogram.getBProgram().getFromGlobalScope("bp", BProgramJsProxy.class).get();
    return (stub) -> {
      if (stub == StreamObjectStub.BP_PROXY) {
        return bpProxy;
      } else {
        throw new IllegalArgumentException("Unknown stub " + stub);
      }
    };
  }

  public static Object deserializeContinuation(BProgramSyncSnapshot bprog, String filename) throws IOException, ClassNotFoundException {
    try {
      BPjs.enterRhinoContext();
      var bytes = Files.readAllBytes(Paths.get(filename));
      try (var bris = new ByteArrayInputStream(bytes);
           var sis = new BPJSStubInputStream(bris, bprog.getBProgram().getGlobalScope(), getStubProvider(bprog))) {
        return sis.readObject();
      }
    } finally {
      Context.exit();
    }
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
    public final Map<Integer, BProgramSyncSnapshot> acceptingStates;
    public final Map<BEvent, Integer> events;

    public MapperResult(Map<BProgramSyncSnapshot, Integer> states, List<Edge> edges, Collection<List<BEvent>> traces, BProgramSyncSnapshot startNode, Map<Integer, BProgramSyncSnapshot> acceptingStates) {
      this.edges = edges;
      this.states = states;
      this.traces = traces;
      this.startNode = startNode;
      this.startNodeId = states.get(startNode);
      this.acceptingStates = acceptingStates;
      AtomicInteger counter = new AtomicInteger();
      events = edges.stream().map(edge -> edge.event).distinct().collect(Collectors.toMap(Function.identity(), e -> counter.getAndIncrement()));
    }

    @Override
    public String toString() {
      return
          "StateMapper stats\n" +
              "=================\n" +
              "# States: " + states.size() + "\n" +
              "# Events: " + events.size() + "\n" +
              "# Transition: " + edges.size() + "\n" +
              (traces == null ? "" : "# Traces: " + traces.size() + "\n") +
              "=================\n";
//              (traces == null ? "" : "# Traces: " + traces + "\n");
    }
  }
}
