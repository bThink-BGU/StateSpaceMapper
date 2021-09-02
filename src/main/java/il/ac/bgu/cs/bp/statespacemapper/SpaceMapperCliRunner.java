package il.ac.bgu.cs.bp.statespacemapper;

import il.ac.bgu.cs.bp.bpjs.model.BProgram;
import il.ac.bgu.cs.bp.bpjs.model.ResourceBProgram;
import il.ac.bgu.cs.bp.statespacemapper.jgrapht.exports.DotExporter;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Scriptable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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

    var exporter = new DotExporter(res, path, runName);
    // exporter parameters can be changed. For example:
    /*exporter.setVertexAttributeProvider(v ->
        Map.of("hash", DefaultAttribute.createAttribute(v.hashCode()))
    );*/
    // See DotExporter for another option that uses the base provider.
    exporter.export();

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

  public static void printAllPaths(GenerateAllTracesInspection.MapperResult res) {
    System.out.println("// Generated paths:");
    // The resulted paths are sorted according to BEvent.toString(). Other Comparator<List<BEvent>> can be passed to res.generatePaths(comparator)
    var paths = res.generatePaths();
    System.out.println(paths);
  }
}
