package il.ac.bgu.cs.bp.statespacemapper;

import il.ac.bgu.cs.bp.bpjs.model.ResourceBProgram;
import il.ac.bgu.cs.bp.bpjs.model.eventselection.PrioritizedBSyncEventSelectionStrategy;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.GraphDatabase;

public class SpaceMapperRunner {

  private static final boolean useNeo4j = false;

  public static void main(String[] args) throws Exception {
    System.out.println("// start");
    if (args.length == 0) {
      System.err.println("Missing input files");
      System.exit(1);
    }

    var runName = args[0].substring(0, args[0].lastIndexOf('.'));
    var bprog = new ResourceBProgram(args[0]);
    var ess = new PrioritizedBSyncEventSelectionStrategy();
    ess.setDefaultPriority(0);
    bprog.setEventSelectionStrategy(ess);
    StateSpaceMapper mpr = new StateSpaceMapper(runName);
    mpr.setGenerateTraces(true); // Generates a set of all possible traces.
    mpr.setOutputPath("graphs");
    if(useNeo4j) {
        try (var driver = GraphDatabase.driver("bolt://localhost:11002", AuthTokens.basic("neo4j", "StateMapper"))) {
          mpr.setNeo4jDriver(driver);
          mpr.mapSpace(bprog);
        }
    } else
      mpr.mapSpace(bprog);

    System.out.println("// done");
  }
}
