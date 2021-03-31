package il.ac.bgu.cs.bp.statespacemapper;

import il.ac.bgu.cs.bp.bpjs.analysis.DfsForStateMapper;
import il.ac.bgu.cs.bp.bpjs.analysis.listeners.PrintDfsVerifierListener;
import il.ac.bgu.cs.bp.bpjs.model.BProgram;
import il.ac.bgu.cs.bp.statespacemapper.writers.*;
import org.neo4j.driver.Driver;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class StateSpaceMapper {
  private Driver neo4jDriver;
  private final String name;
  private final DfsForStateMapper vfr = new DfsForStateMapper();
  private String outputPath = "graphs";
  private List<TraceResultWriter> writers = new ArrayList<>();
  private boolean generateTraces = true;

  private boolean generateRegularExpression = false;

  public StateSpaceMapper(String name) {
    this(name, null);
  }

  public StateSpaceMapper(String name, Driver neo4jDriver) {
    this.neo4jDriver = neo4jDriver;
    this.name = name;
  }

  public void setOutputPath(String path) {
    this.outputPath = path;
  }

  public boolean isGenerateRegularExpression() {
    return generateRegularExpression;
  }

  public void setGenerateRegularExpression(boolean generateRegularExpression) {
    this.generateRegularExpression = generateRegularExpression;
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

  public boolean isGenerateTraces() {
    return generateTraces;
  }

  public void setGenerateTraces(boolean generateTraces) {
    this.generateTraces = generateTraces;
  }

  public void setNeo4jDriver(Driver driver) {
    this.neo4jDriver = driver;
  }

  public void mapSpace(BProgram bprog) throws Exception {
    Files.createDirectories(Paths.get(outputPath));
    initGlobalScope(bprog);
    var tracesInspection = new GenerateAllTracesInspection();
    tracesInspection.setGenerateTraces(isGenerateTraces());
    vfr.addInspection(tracesInspection);

    vfr.setProgressListener(new PrintDfsVerifierListener());
//    vfr.setDebugMode(true);
    vfr.verify(bprog);

    var mapperRes = tracesInspection.getResult();
    System.out.println(mapperRes.toString());

    String noam = null;

    if (writers.isEmpty())
      addDefaultWriters();
    for (var w : writers) {
      var path = Paths.get(outputPath, name + "." + w.filetype);
      try (var out = new PrintStream(path.toString())) {
        w.write(out, mapperRes);
        if (w.filetype.equals("noam")) {
          if (isGenerateRegularExpression()) {
            try (var baos = new ByteArrayOutputStream();
                 var strOut = new PrintStream(baos)) {
              w.write(strOut, mapperRes);
              noam = baos.toString();
            }
          }
        }
      }
    }
    if (neo4jDriver != null) {
      new TraceResultNeo4JWriter(name, neo4jDriver).write(mapperRes);
    }
    if (noam != null) {
      try(var reGenerator = new RegularExpressionGenerator(noam)) {
        System.out.println("// Generating regular expression from automata");
        var re = reGenerator.generateRegex();
        re = reGenerator.preProcessSimplifyRegex();
        var rePath = Paths.get(outputPath, name + ".re");
        try (var out = new PrintStream(rePath.toString())) {
          out.println(re);
        }
        System.out.println("// Simplifying regular expression");
        re = reGenerator.simplifyRegex();
        rePath = Paths.get(outputPath, name + "-simplified.re");
        try (var out = new PrintStream(rePath.toString())) {
          out.println(re);
        }
      }
    }
  }

  public void initGlobalScope(BProgram bprog) {
    bprog.putInGlobalScope("use_accepting_states", true);
    bprog.putInGlobalScope("AcceptingState", new AcceptingStateProxy());
  }
}
