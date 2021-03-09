package il.ac.bgu.cs.bp.statespacemapper;

import il.ac.bgu.cs.bp.bpjs.analysis.DfsForStateMapper;
import il.ac.bgu.cs.bp.bpjs.analysis.listeners.PrintDfsVerifierListener;
import il.ac.bgu.cs.bp.bpjs.model.BProgram;
import il.ac.bgu.cs.bp.statespacemapper.writers.*;
import org.neo4j.driver.Driver;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class StateSpaceMapper {
  private Driver neo4jDriver;
  private final String name;
  private final DfsForStateMapper vfr = new DfsForStateMapper();
  private String basePath = ".";
  private List<TraceResultWriter> writers = new ArrayList<>();

  public StateSpaceMapper(String name) {
    this(name, null);
  }

  public StateSpaceMapper(String name, Driver neo4jDriver) {
    this.neo4jDriver = neo4jDriver;
    this.name = name;
  }

  public void setBasePath(String basePath) {
    this.basePath = basePath;
  }

  public void addWriter(TraceResultWriter writer) {
    writers.add(writer);
  }

  protected void addDefaultWriters() {
    writers.add(new TraceResultJsonWriter(name));
    writers.add(new TraceResultGVWriter(name));
    writers.add(new TraceResultNoamWriter(name));
    writers.add(new TraceResultGoalWriter(name));
  }

  public void setNeo4jDriver(Driver driver) {
    this.neo4jDriver = driver;
  }

  public void mapSpace(BProgram bprog) throws Exception {
    initGlobalScope(bprog);
    var tracesInspection = new GenerateAllTracesInspection();
    vfr.addInspection(tracesInspection);

    vfr.setProgressListener(new PrintDfsVerifierListener());
//    vfr.setDebugMode(true);
    vfr.verify(bprog);

    var mapperRes = tracesInspection.getResult();
    System.out.println(mapperRes.toString());

    if (writers.isEmpty())
      addDefaultWriters();
    for (var w : writers) {
      try (var out = new PrintStream(basePath + "/graphs/" + name + "." + w.filetype)) {
        w.write(out, mapperRes);
      }
    }
    if (neo4jDriver != null) {
      new TraceResultNeo4JWriter(name, neo4jDriver).write(mapperRes);
    }
  }

  public void initGlobalScope(BProgram bprog) {
    bprog.putInGlobalScope("use_accepting_states", true);
    bprog.putInGlobalScope("AcceptingState", new AcceptingStateProxy());
  }
}
