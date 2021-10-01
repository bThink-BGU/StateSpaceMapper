package il.ac.bgu.cs.bp.statespacemapper;

import il.ac.bgu.cs.bp.bpjs.model.BEvent;
import il.ac.bgu.cs.bp.bpjs.model.BProgram;
import il.ac.bgu.cs.bp.bpjs.model.ResourceBProgram;
import il.ac.bgu.cs.bp.statespacemapper.jgrapht.exports.DotExporter;
import il.ac.bgu.cs.bp.statespacemapper.jgrapht.exports.GoalExporter;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Scriptable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SpaceMapperCliRunner {

  public static void main(String[] args) throws Exception {
    System.out.println("// start");
    if (args.length == 0) {
      System.err.println("Missing input files");
      System.exit(1);
    }
    BProgram bprog = getBProgram(args);

    var runName = bprog.getName();

    // You can use a different EventSelectionStrategy, for example:
    /* var ess = new PrioritizedBSyncEventSelectionStrategy();
    bprog.setEventSelectionStrategy(ess); */

    var mpr = new StateSpaceMapper();
    // the maximal trace length can be limited: mpr.setMaxTraceLength(50);
    var res = mpr.mapSpace(bprog);

    System.out.println("// completed mapping the states graph");
    System.out.println(res.toString());

    System.out.println("// Export to GraphViz...");
    var outputDir = "exports";
    var path = Paths.get(outputDir, runName + ".dot").toString();

    var dotExporter = new DotExporter(res, path, runName);
    // exporter parameters can be changed. For example:
    /*exporter.setVertexAttributeProvider(v ->
        Map.of("hash", DefaultAttribute.createAttribute(v.hashCode()))
    );*/
    // See DotExporter for another option that uses the base provider.
    dotExporter.export();
    System.out.println("// Export to GOAL...");
    path = Paths.get(outputDir, runName + ".gff").toString();
    var goalExporter = new GoalExporter(res, path, runName);
    goalExporter.export();
    printAllPaths(res);

    System.out.println("// done");
  }

  private static BProgram getBProgram(String[] args) {
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

  /**
   * Generate all paths. See {@link il.ac.bgu.cs.bp.statespacemapper.jgrapht.AllDirectedPaths} for all the possible algorithm configurations.
   */
  public static void printAllPaths(MapperResult res) {
    System.out.println("// Generated paths:");

    var allDirectedPathsAlgorithm = res.createAllDirectedPathsBuilder()
        .setSimplePathsOnly(true)
        .setIncludeReturningEdgesInSimplePaths(true)
        .setLongestPathsOnly(false)
        .build();
    var graphPaths = allDirectedPathsAlgorithm.getAllPaths();
    var eventPaths = MapperResult.GraphPaths2BEventPaths(graphPaths)
        .stream()
        .map(l -> l.stream()
            .map(BEvent::toString)
            .map(s -> s.replaceAll("\\[BEvent name:([^]]+)\\]", "$1"))
            .collect(Collectors.joining(", ")))
        .distinct()
        .sorted()
        .collect(Collectors.joining("\n"));
    System.out.println(eventPaths);
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
}

