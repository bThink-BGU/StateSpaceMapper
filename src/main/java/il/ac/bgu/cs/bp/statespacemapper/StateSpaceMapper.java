package il.ac.bgu.cs.bp.statespacemapper;

import il.ac.bgu.cs.bp.bpjs.analysis.BProgramSnapshotVisitedStateStore;
import il.ac.bgu.cs.bp.bpjs.analysis.DfsBProgramVerifier;
import il.ac.bgu.cs.bp.bpjs.analysis.listeners.PrintDfsVerifierListener;
import il.ac.bgu.cs.bp.bpjs.model.BProgram;
import il.ac.bgu.cs.bp.bpjs.model.ResourceBProgram;
import il.ac.bgu.cs.bp.bpjs.model.eventselection.PrioritizedBSyncEventSelectionStrategy;

import java.io.PrintStream;

/**
 * @author michael
 */
public class StateSpaceMapper {

  private final String filename;

  public StateSpaceMapper(String filename) {
    this.filename = filename;
  }

  public void mapSpace() throws Exception {
    var bprog = createBProgram();
    var ess = new PrioritizedBSyncEventSelectionStrategy();
    ess.setDefaultPriority(0);
    bprog.setEventSelectionStrategy(ess);
    var vfr = new DfsBProgramVerifier();

    vfr.setVisitedStateStore(new BProgramSnapshotVisitedStateStore());
    var tracesInspection = new GenerateAllTracesInspection();

    vfr.addInspection(tracesInspection);
    vfr.setProgressListener(new PrintDfsVerifierListener());
//    vfr.setDebugMode(true);
    var res = vfr.verify(bprog);
    var mapperRes = tracesInspection.getResult();

    System.out.println(mapperRes.toString());

    try (PrintStream jsonOut = new PrintStream("graphs/" + filename + ".json");
         PrintStream graphVisOut = new PrintStream("graphs/" + filename + ".dot")) {
      new TraceResultJsonWriter(jsonOut, mapperRes, filename).write();
      new TraceResultGVWriter(graphVisOut, mapperRes, filename).write();
    }
  }

  private BProgram createBProgram() {
    return new ResourceBProgram(filename);
  }


}
