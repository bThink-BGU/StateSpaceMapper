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

  @Override
  public String title() {
    return "GenerateAllTracesInspector";
  }

  @Override
  public Optional<Violation> inspectTrace(ExecutionTrace aTrace) {
    Optional<Violation> inspection = ExecutionTraceInspections.FAILED_ASSERTIONS.inspectTrace(aTrace);
    int stateCount = aTrace.getStateCount();
    var lastNode = aTrace.getNodes().get(stateCount - 1);
    var accepting = false;
    var startVertex = false;
    if (inspection.isPresent()) {
      accepting = true;
    }
    if(stateCount == 1) {
      startVertex = true;
    }
    if (aTrace.isCyclic()) {
      // If cycle length < 2 then the last event is the first event
      addEdge(aTrace.getLastState(), aTrace.getFinalCycle().get(0).getState(), aTrace.getLastEvent().orElse(aTrace.getFinalCycle().get(0).getEvent().get()));
    } else {

      var lastNodeVertex = new MapperVertex(lastNode.getState(), startVertex, accepting);
      graph.addVertex(lastNodeVertex);
      if (stateCount != 1) {
        var src = aTrace.getNodes().get(stateCount - 2);
        addEdge(new MapperVertex(src.getState(), startVertex, accepting), lastNodeVertex, src.getEvent().get());
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
    return new MapperResult(graph);
  }
}