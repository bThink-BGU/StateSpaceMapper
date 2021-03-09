package il.ac.bgu.cs.bp.statespacemapper;

import il.ac.bgu.cs.bp.bpjs.analysis.DfsForStateMapper;
import il.ac.bgu.cs.bp.bpjs.analysis.listeners.PrintDfsVerifierListener;
import il.ac.bgu.cs.bp.bpjs.model.BProgram;
import il.ac.bgu.cs.bp.statespacemapper.writers.*;
import org.neo4j.driver.Driver;

import java.io.PrintStream;

public class StateSpaceMapper {
  private Driver neo4jDriver;
  private final String name;

  public StateSpaceMapper(String name) {
    this(name, null);
  }

  public StateSpaceMapper(String name, Driver neo4jDriver) {
    this.neo4jDriver = neo4jDriver;
    this.name = name;
  }

  public void setNeo4jDriver(Driver driver) {
    this.neo4jDriver = driver;
  }

  public void mapSpace(BProgram bprog) throws Exception {
    bprog.putInGlobalScope("use_accepting_states", true);
    bprog.putInGlobalScope("AcceptingState", new AcceptingStateProxy());
    var vfr = new DfsForStateMapper();
    var tracesInspection = new GenerateAllTracesInspection();
    vfr.addInspection(tracesInspection);

    vfr.setProgressListener(new PrintDfsVerifierListener());
//    vfr.setDebugMode(true);
    vfr.verify(bprog);

    var mapperRes = tracesInspection.getResult();

    System.out.println(mapperRes.toString());

    try (PrintStream jsonOut = new PrintStream("graphs/" + name + ".json");
         PrintStream graphVisOut = new PrintStream("graphs/" + name + ".dot");
         PrintStream noamOut = new PrintStream("graphs/" + name + ".noam");
         PrintStream goalOut = new PrintStream("graphs/" + name + ".gff")) {
      new TraceResultJsonWriter(jsonOut, mapperRes, name).write();
      new TraceResultGVWriter(graphVisOut, mapperRes, name).write();
      new TraceResultNoamWriter(noamOut, mapperRes, name).write();
      new TraceResultGoalWriter(goalOut, mapperRes, name).write();
    }
    if (neo4jDriver != null) {
      new TraceResultNeo4JWriter(mapperRes, name, neo4jDriver).write();
    }
  }
}
