package il.ac.bgu.cs.bp.statespacemapper;

import il.ac.bgu.cs.bp.bpjs.analysis.DfsForStateMapper;
import il.ac.bgu.cs.bp.bpjs.analysis.listeners.PrintDfsVerifierListener;
import il.ac.bgu.cs.bp.bpjs.model.BProgram;

public class StateSpaceMapper {
  private int maxTraceLength = Integer.MAX_VALUE;
  private int iterationCountGap = 1000;

  public void setMaxTraceLength(int limit) {
    maxTraceLength = limit;
  }

  public void setIterationCountGap(int iterationCountGap) {
    this.iterationCountGap = iterationCountGap;
  }

  public GenerateAllTracesInspection.MapperResult mapSpace(BProgram bprog) throws Exception {
    initGlobalScope(bprog);
    var tracesInspection = new GenerateAllTracesInspection();
    var vfr = new DfsForStateMapper();
    vfr.addInspection(tracesInspection);
    vfr.setMaxTraceLength(maxTraceLength);
    vfr.setIterationCountGap(iterationCountGap);
    vfr.setProgressListener(new PrintDfsVerifierListener());
    vfr.verify(bprog);
    var res = tracesInspection.getResult();
    return res;
  }

  protected void initGlobalScope(BProgram bprog) {
    bprog.putInGlobalScope("use_accepting_states", true);
    bprog.putInGlobalScope("AcceptingState", new AcceptingStateProxy());
  }
}
