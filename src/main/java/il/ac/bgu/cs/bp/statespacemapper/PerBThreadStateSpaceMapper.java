package il.ac.bgu.cs.bp.statespacemapper;

import il.ac.bgu.cs.bp.bpjs.analysis.DfsForStateMapper;
import il.ac.bgu.cs.bp.bpjs.analysis.listeners.PrintDfsVerifierListener;
import il.ac.bgu.cs.bp.bpjs.model.BProgram;
import il.ac.bgu.cs.bp.bpjs.model.ResourceBProgram;
import il.ac.bgu.cs.bp.bpjs.model.eventselection.PrioritizedBSyncEventSelectionStrategy;
import il.ac.bgu.cs.bp.statespacemapper.writers.*;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class PerBThreadStateSpaceMapper {
  private Driver neo4jDriver;
  private final String name;
  private final DfsForStateMapper vfr = new DfsForStateMapper();
  private String outputPath = "graphs";
  private List<TraceResultWriter> writers = new ArrayList<>();
  private boolean generateTraces = true;

  public PerBThreadStateSpaceMapper(String name) {
    this(name, null);
  }

  public PerBThreadStateSpaceMapper(String name, Driver neo4jDriver) {
    this.neo4jDriver = neo4jDriver;
    this.name = name;
  }

  public void setOutputPath(String path) {
    this.outputPath = path;
  }

  public boolean isGenerateTraces() {
    return generateTraces;
  }

  public void setGenerateTraces(boolean generateTraces) {
    this.generateTraces = generateTraces;
  }

  public void mapSpace(BProgram bprog, int currentBThread) throws Exception {
    Files.createDirectories(Paths.get(outputPath));
    initGlobalScope(bprog, currentBThread);
    var tracesInspection = new GenerateAllTracesInspection();
    tracesInspection.setGenerateTraces(isGenerateTraces());
    vfr.addInspection(tracesInspection);

    bprog.setEventSelectionStrategy(new EssForPerBThread());
    vfr.setProgressListener(new PrintDfsVerifierListener());
//    vfr.setDebugMode(true);
    vfr.verify(bprog);

    var mapperRes = tracesInspection.getResult();
    System.out.println(mapperRes.toString());
    var w = new TraceResultYamlWriter(name);
    var path = Paths.get(outputPath, name + "." + w.filetype);
    try (var out = new PrintStream(path.toString())) {
      w.write(out, mapperRes);
    }
  }

  public void initGlobalScope(BProgram bprog, int currentBThread) {
    bprog.putInGlobalScope("currentBT", currentBThread);
    bprog.putInGlobalScope("use_accepting_states", true);
    bprog.putInGlobalScope("AcceptingState", new AcceptingStateProxy());
  }


  public static void main(String[] args) throws Exception {
    if (args.length == 0) {
      System.err.println("Missing input files");
      System.exit(1);
    }

    var bprogName = args[0].substring(0, args[0].lastIndexOf('.'));
    final int numberOfBthreads = 2;
    for (int i = 0; i < numberOfBthreads; i++) {
      String runName = bprogName + "." + i;
      System.out.println("// start " + i);
      var bprog = new ResourceBProgram(args[0]);
      var ess = new PrioritizedBSyncEventSelectionStrategy();
      ess.setDefaultPriority(0);
      bprog.setEventSelectionStrategy(ess);
      var mpr = new PerBThreadStateSpaceMapper(runName);
      mpr.setGenerateTraces(true); // Generates a set of all possible traces.
      mpr.setOutputPath("graphs");
      mpr.mapSpace(bprog, i);
      System.out.println("// done");
    }
  }
}
