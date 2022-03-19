package il.ac.bgu.cs.bp.statespacemapper;

import il.ac.bgu.cs.bp.bpjs.context.ContextBProgram;
import il.ac.bgu.cs.bp.bpjs.model.BProgram;
import il.ac.bgu.cs.bp.bpjs.model.ResourceBProgram;
import il.ac.bgu.cs.bp.bpjs.model.eventselection.PrioritizedBSyncEventSelectionStrategy;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Scriptable;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.GraphDatabase;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SpaceMapperRunner {

  private static final boolean useNeo4j = false;

  public static void main(String[] args) throws Exception {
    var bprog = new ContextBProgram("HotCold/dal.js","HotCold/bl.js");
    var runName = bprog.getName();
    StateSpaceMapper mpr = new StateSpaceMapper(runName);
    mpr.setGenerateTraces(true); // Generates a set of all possible traces.
    mpr.setOutputPath("graphs");
    if (useNeo4j) {
      try (var driver = GraphDatabase.driver("bolt://localhost:11002", AuthTokens.basic("neo4j", "StateMapper"))) {
        mpr.setNeo4jDriver(driver);
        mpr.mapSpace(bprog);
      }
    } else
      mpr.mapSpace(bprog);

    System.out.println("// done");
  }
}
