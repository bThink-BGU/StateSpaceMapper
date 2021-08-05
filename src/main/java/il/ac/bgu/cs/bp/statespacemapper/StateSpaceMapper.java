package il.ac.bgu.cs.bp.statespacemapper;

import il.ac.bgu.cs.bp.bpjs.analysis.DfsForStateMapper;
import il.ac.bgu.cs.bp.bpjs.analysis.listeners.PrintDfsVerifierListener;
import il.ac.bgu.cs.bp.bpjs.model.BProgram;

public class StateSpaceMapper {
  private final DfsForStateMapper vfr = new DfsForStateMapper();

  public GenerateAllTracesInspection.MapperResult mapSpace(BProgram bprog) throws Exception {

    initGlobalScope(bprog);
    var tracesInspection = new GenerateAllTracesInspection();
    vfr.addInspection(tracesInspection);

    vfr.setProgressListener(new PrintDfsVerifierListener());
    vfr.verify(bprog);
    return tracesInspection.getResult();
  }

  public void initGlobalScope(BProgram bprog) {
    bprog.putInGlobalScope("use_accepting_states", true);
    bprog.putInGlobalScope("AcceptingState", new AcceptingStateProxy());
  }
}
