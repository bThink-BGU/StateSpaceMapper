package il.ac.bgu.cs.bp.statespacemapper;

import il.ac.bgu.cs.bp.bpjs.analysis.ExecutionTrace;
import il.ac.bgu.cs.bp.bpjs.analysis.ExecutionTraceInspection;
import il.ac.bgu.cs.bp.bpjs.analysis.ExecutionTraceInspections;
import il.ac.bgu.cs.bp.bpjs.analysis.violations.Violation;
import il.ac.bgu.cs.bp.bpjs.model.BEvent;
import il.ac.bgu.cs.bp.bpjs.model.BProgramSyncSnapshot;
import il.ac.bgu.cs.bp.statespacemapper.jgrapht.MapperEdge;
import il.ac.bgu.cs.bp.statespacemapper.jgrapht.MapperVertex;
import org.jgrapht.Graph;
import org.jgrapht.graph.DirectedPseudograph;

import java.util.*;

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
        startNode = lastNodeVertex;
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
}