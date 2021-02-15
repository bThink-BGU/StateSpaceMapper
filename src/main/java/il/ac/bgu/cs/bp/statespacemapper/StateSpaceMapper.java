package il.ac.bgu.cs.bp.statespacemapper;

import il.ac.bgu.cs.bp.bpjs.analysis.DfsBProgramVerifier;
import il.ac.bgu.cs.bp.bpjs.analysis.listeners.PrintDfsVerifierListener;
import il.ac.bgu.cs.bp.bpjs.model.BProgram;
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
    var vfr = new DfsBProgramVerifier();
    var tracesInspection = new GenerateAllTracesInspection();
    vfr.addInspection(tracesInspection);

    vfr.setProgressListener(new PrintDfsVerifierListener());
//    vfr.setDebugMode(true);
    vfr.verify(bprog);

    var mapperRes = tracesInspection.getResult();

    System.out.println(mapperRes.toString());

    try (PrintStream jsonOut = new PrintStream("graphs/" + name + ".json");
         PrintStream graphVisOut = new PrintStream("graphs/" + name + ".dot")) {
      new TraceResultJsonWriter(jsonOut, mapperRes, name).write();
      new TraceResultGVWriter(graphVisOut, mapperRes, name).write();
    }
    if (neo4jDriver != null) {
      new TraceResultNeo4JWriter(mapperRes, name, neo4jDriver).write();
    }
  }
}
