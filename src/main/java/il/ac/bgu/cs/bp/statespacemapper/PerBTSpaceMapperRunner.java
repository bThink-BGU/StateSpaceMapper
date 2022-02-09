package il.ac.bgu.cs.bp.statespacemapper;

import il.ac.bgu.cs.bp.bpjs.model.*;
import il.ac.bgu.cs.bp.statespacemapper.jgrapht.MapperEdge;
import il.ac.bgu.cs.bp.statespacemapper.jgrapht.MapperVertex;
import il.ac.bgu.cs.bp.statespacemapper.jgrapht.exports.DotExporter;
import il.ac.bgu.cs.bp.statespacemapper.jgrapht.exports.Exporter;
import il.ac.bgu.cs.bp.statespacemapper.jgrapht.exports.JsonExporter;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.Graphs;
import org.jgrapht.graph.AbstractBaseGraph;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;
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
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.util.stream.Collectors.joining;

public class PerBTSpaceMapperRunner {

  public static void main(String[] args) throws Exception {
    if (args.length == 0) {
      System.err.println("Missing input files");
      System.err.println("Sample execution argument: \"hot-cold.js\"");
      System.exit(1);
    }
    var bprog = getBProgram(args);
    var runName = bprog.getName().replace(".js+", "");

    System.out.println("// start");

    bprog.setup();
    var mapperResults =
        ((Map<String, Object>) bprog.getFromGlobalScope("bthreads", Map.class).get()).keySet().stream()
            .collect(Collectors.toMap(
                name -> name,
                bt -> {
                  var btBProg = getBProgram(args);
                  btBProg.appendSource("bp.registerBThread('" + bt + "',bthreads['" + bt + "'])");
                  btBProg.setEventSelectionStrategy(new EssForPerBThread());
                  try {
                    return mapSpace(btBProg);
                  } catch (Exception e) {
                    throw new RuntimeException(e);
                  }
                }));
    Graph<MapperVertex, MapperEdge> union = null;
    for (var res : mapperResults.values()) {
      if (union == null) {
        union = (Graph<MapperVertex, MapperEdge>) ((AbstractBaseGraph) res.graph).clone();
      } else {
        Graphs.addGraph(union, res.graph);
      }
    }
    exportSpace(runName, new MapperResult(union));
  }

  public static void exportSpace(String runName, MapperResult res) throws IOException {
    var outputDir = "exports";

    System.out.println("// Export to JSON...");
    var path = Paths.get(outputDir, runName + ".json").toString();
    var jsonExporter = new JsonExporter(res, path, runName);
    setExporterProviders(res, jsonExporter);
    jsonExporter.export();


    System.out.println("// Export to GraphViz...");
    path = Paths.get(outputDir, runName + ".dot").toString();
    var dotExporter = new DotExporter(res, path, runName);
    setExporterProviders(res, dotExporter);
    dotExporter.export();
  }

  public static void setExporterProviders(MapperResult res, Exporter exporter) {
    exporter.setVertexAttributeProvider(v -> {
      var snapshot = v.bpss.getBThreadSnapshots().stream()
          .findFirst();
      if (snapshot.isPresent()) {
        var syst = snapshot.get().getSyncStatement();
        return Map.of(
            "isHot", DefaultAttribute.createAttribute(syst.isHot()),
            "request", DefaultAttribute.createAttribute(syst.getRequest().stream().map(BEvent::toString).collect(joining(","))),
            "waitFor", DefaultAttribute.createAttribute(Utils.eventSetToList(syst.getWaitFor()).stream().map(BEvent::toString).collect(joining(","))),
            "block", DefaultAttribute.createAttribute(Utils.eventSetToList(syst.getBlock()).stream().map(BEvent::toString).collect(joining(","))),
            "interrupt", DefaultAttribute.createAttribute(Utils.eventSetToList(syst.getInterrupt()).stream().map(BEvent::toString).collect(joining(","))),
            "start", DefaultAttribute.createAttribute(v.startVertex),
            "accepting", DefaultAttribute.createAttribute(v.accepting),
            "bthread", DefaultAttribute.createAttribute(exporter.getSanitizerProvider().apply(getBThreads(v.bpss))));
      } else {
        return Map.of();
      }
    });
    var oldGraphProvider = exporter.getGraphAttributeProvider().get();
    oldGraphProvider.put("edges", DefaultAttribute.createAttribute(res.edges().stream().map(MapperEdge::getEvent).map(BEvent::toString).collect(joining(",", "\"[", "]\""))));
    oldGraphProvider.put("bthreads", DefaultAttribute.createAttribute(res.states().stream().flatMap(v->v.bpss.getBThreadSnapshots().stream()).map(BThreadSyncSnapshot::getName).distinct().sorted().collect(joining(",","\"[","]\""))));
    exporter.setGraphAttributeProvider(() -> oldGraphProvider);
  }

  public static String getBThreads(BProgramSyncSnapshot bpss) {
    return bpss.getBThreadSnapshots().stream().map(BThreadSyncSnapshot::getName).collect(joining(","));
  }

  public static MapperResult mapSpace(BProgram bprog) throws Exception {
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
  private static void testAllPaths(MapperResult res) {
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
  private static void writeCompressedPaths(String csvFileName, Integer maxPathLength, MapperResult res, String outputDir) throws IOException {
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

