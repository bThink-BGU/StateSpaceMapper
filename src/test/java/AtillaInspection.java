import il.ac.bgu.cs.bp.bpjs.analysis.ExecutionTrace;
import il.ac.bgu.cs.bp.bpjs.analysis.ExecutionTraceInspection;
import il.ac.bgu.cs.bp.bpjs.analysis.violations.Violation;
import il.ac.bgu.cs.bp.bpjs.model.BProgramSyncSnapshot;
import org.mozilla.javascript.NativeObject;

import java.util.Optional;

public class AtillaInspection implements ExecutionTraceInspection {
  public BProgramSyncSnapshot bpss1 = null;
  public BProgramSyncSnapshot bpss2 = null;

  @Override
  public String title() {
    return "AtillaInspection";
  }

  @Override
  public Optional<Violation> inspectTrace(ExecutionTrace trace) {
    int stateCount = trace.getStateCount();
    if (stateCount == 3) {
      var nodes = trace.getNodes();
      var firstEvent = nodes.get(0).getEvent().get();
      var secondEvent = nodes.get(1).getEvent().get();
      if (firstEvent.name.equals("Login") && secondEvent.name.equals("Login")) {
        if(((NativeObject)firstEvent.maybeData).get("s").toString().equals("C1") &&
          ((NativeObject)secondEvent.maybeData).get("s").toString().equals("C2")) {
          bpss1 = nodes.get(2).getState();
        } else if(((NativeObject)firstEvent.maybeData).get("s").toString().equals("C2") &&
          ((NativeObject)secondEvent.maybeData).get("s").toString().equals("C1")) {
          bpss2 = nodes.get(2).getState();
        }
      }
    }
    return Optional.empty();
  }
}
