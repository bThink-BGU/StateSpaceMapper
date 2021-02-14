package il.ac.bgu.cs.bp.statespacemapper;

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
  private boolean useNeo4j = false;
  private final String filename;

  public StateSpaceMapper(String filename) {
    this.filename = filename;
  }

  public void mapSpace() throws Exception {
    Neo4JInspection neo4j = null;

    var bprog = createBProgram();
    var ess = new PrioritizedBSyncEventSelectionStrategy();
    ess.setDefaultPriority(0);
    bprog.setEventSelectionStrategy(ess);
    var vfr = new DfsBProgramVerifier();


    var tracesInspection = new GenerateAllTracesInspection();
    vfr.addInspection(tracesInspection);

    try {
      if (useNeo4j) {
        neo4j = new Neo4JInspection();
        vfr.addInspection(neo4j);
      }

      vfr.setProgressListener(new PrintDfsVerifierListener());
//    vfr.setDebugMode(true);
      vfr.verify(bprog);
    } finally {
      if (neo4j != null)
        neo4j.close();
    }

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
