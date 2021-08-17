package il.ac.bgu.cs.bp.statespacemapper;

import il.ac.bgu.cs.bp.bpjs.internal.ScriptableUtils;
import il.ac.bgu.cs.bp.bpjs.model.*;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;
import org.jgrapht.nio.dot.DOTExporter;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Scriptable;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.stream.Collectors.joining;

public class SpaceMapperRunner {

  public static void main(String[] args) throws Exception {
    System.out.println("// start");
    if (args.length == 0) {
      System.err.println("Missing input files");
      System.exit(1);
    }
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
    var runName = bprog.getName();

    // You can use a different EventSelectionStrategy, for example:
    /* var ess = new PrioritizedBSyncEventSelectionStrategy();
    bprog.setEventSelectionStrategy(ess); */
    StateSpaceMapper mpr = new StateSpaceMapper();
    var res = mpr.mapSpace(bprog);
    System.out.println("// completed mapping the states graph");
    System.out.println(res.toString());

    exportGraph(runName, res);

    getAllPaths(res);

    System.out.println("// done");
  }

  public static List<List<BEvent>> getAllPaths(GenerateAllTracesInspection.MapperResult res) {
    System.out.println("// Generated paths:");
    boolean findSimplePathsOnly = true; // acyclic paths
    int maxPathLength = Integer.MAX_VALUE;
    var paths = res.generatePaths(findSimplePathsOnly, maxPathLength);
    System.out.println(paths);
    return paths;
  }

  private static void exportGraph(String runName, GenerateAllTracesInspection.MapperResult res) throws IOException {
    Function<GenerateAllTracesInspection.MapperEdge, Map<String, Attribute>> edgeAttributeProvider = e -> Map.of(
        "Event", DefaultAttribute.createAttribute(dotSanitizer(e.event.toString())),
        "Event_name", DefaultAttribute.createAttribute(e.event.name),
        "Event_value", DefaultAttribute.createAttribute(dotSanitizer(Objects.toString(e.event.maybeData))),
        "label", DefaultAttribute.createAttribute(dotSanitizer(e.event.toString()))
    );
    Function<GenerateAllTracesInspection.MapperVertex, Map<String, Attribute>> vertexAttributeProvider = v -> {
      boolean startNode = v.equals(res.startNode);
      boolean acceptingNode = res.acceptingStates.contains(v);
      return Map.of(
          "hash", DefaultAttribute.createAttribute(v.hashCode()),
          "store", DefaultAttribute.createAttribute(dotSanitizer(getStore(v.bpss))),
          "statements", DefaultAttribute.createAttribute(dotSanitizer(getStatments(v.bpss))),
          "bthreads", DefaultAttribute.createAttribute(dotSanitizer(getBThreads(v.bpss))),
          "shape", DefaultAttribute.createAttribute(startNode ? "none " : acceptingNode ? "doublecircle" : "circle")
      );
    };
    Supplier<Map<String, Attribute>> graphAttributeProvider = () -> Map.of(
        "name", DefaultAttribute.createAttribute("\"" + runName + "\""),
        "run_date", DefaultAttribute.createAttribute("\"" + DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.now()) + "\""),
        "num_of_vertices", DefaultAttribute.createAttribute(res.states().size()),
        "num_of_edges", DefaultAttribute.createAttribute(res.edges().size()),
        "num_of_events", DefaultAttribute.createAttribute(res.events.size())
    );

    System.out.println("// Export to GraphViz...");
    var outputDir = "exports";
    Files.createDirectories(Paths.get(outputDir));
    var path = Paths.get(outputDir, runName + ".dot");
    var dotExporter = new DOTExporter<GenerateAllTracesInspection.MapperVertex, GenerateAllTracesInspection.MapperEdge>();
    dotExporter.setEdgeAttributeProvider(edgeAttributeProvider);
    dotExporter.setVertexAttributeProvider(vertexAttributeProvider);
    dotExporter.setGraphAttributeProvider(graphAttributeProvider);
    try (var out = new PrintStream(path.toString())) {
      dotExporter.exportGraph(res.graph, out);
    }
  }

  private static String getBThreads(BProgramSyncSnapshot bpss) {
    return bpss.getBThreadSnapshots().stream()
        .map(BThreadSyncSnapshot::getName)
        .sorted()
        .collect(joining(","));
  }

  private static String dotSanitizer(String in) {
    return in
        .replace("\r\n", "")
        .replace("\n", "")
        .replace("\"", "'")
        .replace("JS_Obj ", "")
        ;
  }

  private static String getStore(BProgramSyncSnapshot bpss) {
    return bpss.getDataStore().entrySet().stream()
        .map(entry -> "{" + ScriptableUtils.stringify(entry.getKey()) + "," + ScriptableUtils.stringify(entry.getValue()) + "}")
        .sorted()
        .collect(joining(",", "[", "]"));
  }

  private static String getStatments(BProgramSyncSnapshot bpss) {
    return bpss.getBThreadSnapshots().stream()
        .map(btss -> {
          SyncStatement syst = btss.getSyncStatement();
          return
              "{name: " + btss.getName() + ", " +
                  "isHot: " + syst.isHot() + ", " +
                  "request: " + syst.getRequest().stream().map(BEvent::toString).collect(joining(",", "[", "]")) + ", " +
                  "waitFor: " + syst.getWaitFor().toString() + ", " +
                  "block: " + syst.getBlock().toString() + ", " +
                  "interrupt: " + syst.getInterrupt().toString() + "}";
        })
        .sorted()
        .collect(joining(",\n", "[", "]"));
  }
}
