package il.ac.bgu.cs.bp.statespacemapper;

import il.ac.bgu.cs.bp.bpjs.model.BEvent;
import il.ac.bgu.cs.bp.bpjs.model.BProgram;
import il.ac.bgu.cs.bp.bpjs.model.ResourceBProgram;
import il.ac.bgu.cs.bp.bpjs.model.eventselection.PrioritizedBSyncEventSelectionStrategy;
import il.ac.bgu.cs.bp.statespacemapper.jgrapht.MapperEdge;
import il.ac.bgu.cs.bp.statespacemapper.jgrapht.exports.DotExporter;
import il.ac.bgu.cs.bp.statespacemapper.jgrapht.exports.Exporter;
import il.ac.bgu.cs.bp.statespacemapper.jgrapht.exports.GoalExporter;
import il.ac.bgu.cs.bp.statespacemapper.jgrapht.exports.JsonExporter;
import org.jgrapht.GraphPath;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;
import org.jgrapht.util.VertexToIntegerMapping;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Scriptable;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.util.stream.Collectors.joining;

public class SpaceMapperCliRunner {
  public void run(String[] args) throws Exception {
    if (args.length == 0) {
      System.err.println("Missing input files");
      System.err.println("Sample execution argument: \"hot-cold.js\"");
      System.exit(1);
    }

    BProgram bprog = getBProgram(args);
    var runName = bprog.getName();

    System.out.println("// start");

    // You can use a different EventSelectionStrategy, for example:
//    var ess = new PrioritizedBSyncEventSelectionStrategy();
//    ess.setDefaultPriority(0);
//    bprog.setEventSelectionStrategy(ess);

    MapperResult res = mapSpace(bprog);
    exportSpace(runName, res);
    res.findBug();

//    WARNING: May take extremely long time and may generate extremely large files
//    writeCompressedPaths(runName + ".csv", null, res, "exports");

    System.out.println("// done");
  }

  public static void main(String[] args) throws Exception {
    new SpaceMapperCliRunner().run(args);
  }

  protected void setExporterProviders(Exporter exporter, String runName, MapperResult res) {
    // exporter parameters can be changed. For example:
    var superGraphProvider = exporter.getGraphAttributeProvider().get();
    exporter.setGraphAttributeProvider(() -> {
      var m = new HashMap<>(superGraphProvider);
      m.put("rankdir", DefaultAttribute.createAttribute("LR"));
      return m;
    });
    // See DotExporter for another option that uses the base provider.

  }

  public void exportSpace(String runName, MapperResult res) throws IOException {
    var outputDir = "exports";

    System.out.println("// Export to GraphViz...");
    var path = Paths.get(outputDir, runName + ".dot").toString();
    var dotExporter = new DotExporter(res, path, runName);
    setExporterProviders(dotExporter, runName, res);
    dotExporter.export();

    /*System.out.println("// Export to JSON...");
    path = Paths.get(outputDir, runName + ".json").toString();
    var jsonExporter = new JsonExporter(res, path, runName);
    setExporterProviders(jsonExporter, runName, res);
    jsonExporter.export();

    System.out.println("// Export to GOAL...");
    boolean simplifyTransitions = true;
    path = Paths.get(outputDir, runName + ".gff").toString();
    var goalExporter = new GoalExporter(res, path, runName, simplifyTransitions);
    setExporterProviders(goalExporter, runName, res);
    goalExporter.export();*/
  }

  public MapperResult mapSpace(BProgram bprog) throws Exception {
    System.out.println("// Start mapping space");
    var mpr = new StateSpaceMapper();
    // the maximal trace length can be limited: mpr.setMaxTraceLength(50);
    var res = mpr.mapSpace(bprog);

    System.out.println("// completed mapping the states graph");
    System.out.println(res.toString());
    return res;
  }

  public static BProgram getBProgram(String[] args) {
    BProgram bprog = null;
    try {
      bprog = new ResourceBProgram(args);
    } catch (Exception ignored) {
      bprog = new BProgram(args[0].replaceAll("\\.\\.[/\\\\]", "")) {
        @Override
        protected void setupProgramScope(Scriptable scope) {
          for (String arg : args) {
            if (arg.equals("-")) {
              System.out.println(" [READ] stdin");
              try {
                evaluate(System.in, "stdin", Context.getCurrentContext());
              } catch (EvaluatorException ee) {
                logScriptExceptionAndQuit(ee, arg);
              }
            } else {
              if (!arg.startsWith("-")) {
                Path inFile = Paths.get(arg);
                System.out.printf(" [READ] %s\n", inFile.toAbsolutePath());
                if (!Files.exists(inFile)) {
                  System.out.printf("File %s does not exit\n", inFile.toAbsolutePath());
                  System.exit(-2);
                }
                try (InputStream in = Files.newInputStream(inFile)) {
                  evaluate(in, arg, Context.getCurrentContext());
                } catch (EvaluatorException ee) {
                  logScriptExceptionAndQuit(ee, arg);
                } catch (IOException ex) {
                  System.out.printf("Exception while processing %s: %s\n", arg, ex.getMessage());
                }
              }
            }
            System.out.printf(" [ OK ] %s\n", arg);
          }
        }

        private void logScriptExceptionAndQuit(EvaluatorException ee, String arg) {
          System.out.printf("Error in source %s:\n", arg);
          System.out.println(ee.details());
          System.out.printf("line: %d: %d\n", ee.lineNumber(), ee.columnNumber());
          System.out.printf("source: %s\n", ee.lineSource());
          System.exit(-3);
        }
      };
    }
    return bprog;
  }

  /*
   * TODO: should be moved to test...
   */
  public void testAllPaths(MapperResult res) {
    System.out.println("// Generated paths:");

    var allDirectedPathsAlgorithm1 = res.createAllDirectedPathsBuilder()
        .setSimplePathsOnly(true)
        .setIncludeReturningEdgesInSimplePaths(true)
        .setLongestPathsOnly(false)
        .build();
    var allDirectedPathsAlgorithm2 = res.createAllDirectedPathsBuilder()
        .setSimplePathsOnly(true)
        .setIncludeReturningEdgesInSimplePaths(true)
        .setLongestPathsOnly(true)
        .build();
    var graphPaths1 = allDirectedPathsAlgorithm1.getAllPaths();
    var graphPaths2 = allDirectedPathsAlgorithm2.getAllPaths();
    var eventPaths1 = MapperResult.GraphPaths2BEventPaths(graphPaths1)
        .stream()
        .map(l -> l.stream()
            .map(BEvent::toString)
            .map(s -> s.replaceAll("\\[BEvent name:([^]]+)\\]", "$1"))
            .collect(Collectors.joining(", ")))
        .distinct()
        .sorted()
        .collect(Collectors.joining("\n"));
    var eventPaths2longest = MapperResult.GraphPaths2BEventPaths(graphPaths2);
    var eventPaths2All = new ArrayList<List<BEvent>>();
    for (var l : eventPaths2longest) {
      for (int i = 0; i <= l.size(); i++) {
        eventPaths2All.add(l.subList(0, i));
      }
    }
    var eventPaths2 = eventPaths2All.stream()
        .map(l -> l.stream()
            .map(BEvent::toString)
            .map(s -> s.replaceAll("\\[BEvent name:([^]]+)\\]", "$1"))
            .collect(Collectors.joining(", ")))
        .distinct()
        .sorted()
        .collect(Collectors.joining("\n"));

    System.out.println("EventPath1 = " + eventPaths1);
    System.out.println("EventPath2 = " + eventPaths2);

    System.out.println("ep1==ep2: " + (eventPaths1.equals(eventPaths2)));
  }

  /**
   * Generate all paths and write them to a zip file containing a csv file with the paths.
   * See {@link il.ac.bgu.cs.bp.statespacemapper.jgrapht.AllDirectedPaths} for all the possible algorithm configurations.
   */
  public void writeCompressedPaths(String csvFileName, Integer maxPathLength, MapperResult res, String outputDir) throws IOException {
    System.out.println("// Generating paths...");
    var allDirectedPathsAlgorithm = res.createAllDirectedPathsBuilder()
        .setSimplePathsOnly(maxPathLength == null)
        .setIncludeReturningEdgesInSimplePaths(maxPathLength == null)
        .setLongestPathsOnly(false)
        .setMaxPathLength(maxPathLength)
        .build();
    var graphPaths = allDirectedPathsAlgorithm.getAllPaths();

    int maxLength = graphPaths.parallelStream().map(GraphPath::getLength).max(Integer::compareTo).orElse(0);
    System.out.println("// Number of paths = " + graphPaths.size());
    System.out.println("// Max path length = " + maxLength);

    System.out.println("// Writing paths...");
    try (var fos = new FileOutputStream(Paths.get(outputDir, csvFileName) + ".zip");
         var zipOut = new ZipOutputStream(fos)) {
      var zipEntry = new ZipEntry(csvFileName);
      zipOut.putNextEntry(zipEntry);
      zipOut.setLevel(9);
      MapperResult.GraphPaths2BEventPaths(graphPaths)
          .parallelStream()
          .map(l -> l.stream()
              .map(BEvent::getName)
//            .filter(s -> !List.of("KeepDown", "ClosingRequest", "OpeningRequest").contains(s))
              .collect(Collectors.joining(",", "", "\n")))
          .distinct()
          .sorted()
          .forEachOrdered(s -> {
            try {
              zipOut.write(s.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
              e.printStackTrace();
            }
          });
    }
  }
}

