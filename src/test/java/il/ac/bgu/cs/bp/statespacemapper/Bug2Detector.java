package il.ac.bgu.cs.bp.statespacemapper;

import il.ac.bgu.cs.bp.bpjs.model.BProgram;
import il.ac.bgu.cs.bp.statespacemapper.jgrapht.MapperEdge;
import il.ac.bgu.cs.bp.statespacemapper.jgrapht.MapperVertex;
import il.ac.bgu.cs.bp.statespacemapper.jgrapht.exports.Exporter;
import org.jgrapht.Graphs;
import org.jgrapht.nio.DefaultAttribute;
import org.jgrapht.nio.GraphExporter;

import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class Bug2Detector extends SpaceMapperCliRunner {
  private String code = "";

  public static void main(String[] args) throws Exception {
    run();
  }

  public static void run() throws Exception {
    BProgram bprog = SpaceMapperCliRunner.getBProgram(new String[]{"test.js"});
    bprog.setup();
    String btTemplate = bprog.getFromGlobalScope("btTemplate", String.class).get();
    String whenTemplate = bprog.getFromGlobalScope("whenTemplate", String.class).get();
    String template = bprog.getFromGlobalScope("template", String.class).get();
    String[] btFuncBody = bprog.getFromGlobalScope("btFuncBody", String[].class).get();
    String[] dataTypes = bprog.getFromGlobalScope("dataTypes", String[].class).get();
    String[] btFuncOptions = bprog.getFromGlobalScope("btFuncOptions", String[].class).get();
    String[] helperCallOptions = bprog.getFromGlobalScope("helperCallOptions", String[].class).get();
    Map<String, String>[] whenOptions = bprog.getFromGlobalScope("whenOptions", Map[].class).get();

    int counter = 0;
    try (var out = new PrintStream("exports/test.tsv")) {
      for (int btFuncBodyI = 0; btFuncBodyI < btFuncBody.length; btFuncBodyI++) {
        for (int btFuncOptionsI = 0; btFuncOptionsI < btFuncOptions.length; btFuncOptionsI++) {
          for (int helperCallOptionsI = 0; helperCallOptionsI < helperCallOptions.length; helperCallOptionsI++) {
            for (int whenOptionsI = 0; whenOptionsI < whenOptions.length; whenOptionsI++) {
              for (int dataTypesI = 0; dataTypesI < dataTypes.length; dataTypesI++) {
                var when = whenOptions[whenOptionsI];
                var runner = new Bug2Detector();
                runner.code = template
                    .replace("%%btFuncOptions%%", btFuncOptions[btFuncOptionsI])
                    .replace("%%btFuncBody%%", btFuncBody[btFuncBodyI])
                    .replace("%%helper-call%%", format(helperCallOptions[helperCallOptionsI], 2))
                    .replace("%%before-while%%", format(when.get("%%before-while%%"), 1))
                    .replace("%%before-sync%%", format(when.get("%%before-sync%%"), 2))
                    .replace("%%sync%%", when.getOrDefault("%%sync%%", ""))
                    .replace("%%after-sync%%", when.getOrDefault("%%after-sync%%", ""))
                    .replace("%%before-end-while%%", format(when.get("%%before-end-while%%"), 2))
                    .replace("%%data-variable%%", format(when.get("%%data-variable%%"), 0))
                    .replace("%%after-while%%", format(when.get("%%after-while%%"), 1))
                    .replace("%%dataTypes%%", dataTypes[dataTypesI]);
                System.out.println("**** iteration: " + counter + "****");
                System.out.println("**** code ****\n" + runner.code + "\n\n\n");
                for (int i = 0; i < 5; i++) {
                  bprog = SpaceMapperCliRunner.getBProgram(new String[]{"test.js"});
                  bprog.appendSource(runner.code);
                  //    bprog.putInGlobalScope("removeParent", removeParent);
                  var runName = "test_" + counter + "_" + i;
                  bprog.setName(runName);
                  System.out.println("// start");

                  // You can use a different EventSelectionStrategy, for example:
                  //    var ess = new PrioritizedBSyncEventSelectionStrategy();
                  //    ess.setDefaultPriority(0);
                  //    bprog.setEventSelectionStrategy(ess);
                  MapperResult res = runner.mapSpace(bprog);
                  runner.exportSpace(runName, res);
                  out.println(new Stats(counter, i, res.states().size(), res.events.size(), res.edges().size(), res.acceptingVertices().size()));
                  //    res.findBug();

                  //    WARNING: May take extremely long time and may generate extremely large files
                  //    writeCompressedPaths(runName + ".csv", null, res, "exports");
                }
                counter++;
              }
            }
          }
        }
      }
    }
    System.out.println("// done");
  }

  private static String format(String str, int numberOfTabs) {
    if (str == null) return "";
    if (str.equals("")) return str;
    String tabs = String.join("", Collections.nCopies(numberOfTabs, "  "));
    var ret = tabs + str.replace("\n", "\n" + tabs);
    if (ret.endsWith(tabs)) ret = ret.substring(0, ret.length() - tabs.length());
    return ret;
  }

  @Override
  protected void setExporterProviders(Exporter exporter, String runName, MapperResult res) {
    // exporter parameters can be changed. For example:
    var superGraphProvider = exporter.getGraphAttributeProvider().get();
    exporter.setGraphAttributeProvider(() -> {
      var m = new HashMap<>(superGraphProvider);
      m.put("rankdir", DefaultAttribute.createAttribute("LR"));
      m.put("code", DefaultAttribute.createAttribute("\"" + code + "\""));
      return m;
    });
    // See DotExporter for another option that uses the base provider.
    /*var vE = exporter.getEdgeAttributeProvider();
    exporter.setEdgeAttributeProvider(mapperEdge -> {
      var map = vE.apply(mapperEdge);
      map.put("label", DefaultAttribute.createAttribute(""));
      return map;
    });*/
    var vA = exporter.getVertexAttributeProvider();
    exporter.setVertexAttributeProvider(vertex -> {
      var map = vA.apply(vertex);
      boolean accepting = !Graphs.vertexHasSuccessors(res.graph, vertex);
      map.put("accepting", DefaultAttribute.createAttribute(accepting));
      map.put("shape", DefaultAttribute.createAttribute(accepting ? "doublecircle" : "circle"));
      map.put("label", DefaultAttribute.createAttribute(""));
      return map;
    });
  }

  private static class Stats {
    public final int iteration;
    public final int round;
    public final int states;
    public final int events;
    public final int transitions;
    public final int accepting;

    private Stats(int iteration, int round, int states, int events, int transitions, int accepting) {
      this.iteration = iteration;
      this.round = round;
      this.states = states;
      this.events = events;
      this.transitions = transitions;
      this.accepting = accepting;
    }

    @Override
    public String toString() {
      return MessageFormat.format("{0}\t{1}\t{2}\t{3}\t{4}\t{5}", iteration, round, states, events, transitions, accepting);
    }
  }
}
