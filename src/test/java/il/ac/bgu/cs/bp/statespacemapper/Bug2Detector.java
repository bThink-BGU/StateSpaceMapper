package il.ac.bgu.cs.bp.statespacemapper;

import il.ac.bgu.cs.bp.bpjs.model.BProgram;
import il.ac.bgu.cs.bp.bpjs.model.StringBProgram;
import il.ac.bgu.cs.bp.statespacemapper.jgrapht.exports.Exporter;
import org.jgrapht.Graphs;
import org.jgrapht.nio.DefaultAttribute;

import java.io.*;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Bug2Detector extends SpaceMapperCliRunner {
  private static final int MAX_ROUNDS = 20;
  private String code = "";

  public static void main(String[] args) throws Exception {
    run2();
  }

  public static void run() throws Exception {
    BProgram bprog = SpaceMapperCliRunner.getBProgram(new String[]{"test.js"});
    bprog.setup();
    String btTemplate = bprog.getFromGlobalScope("btTemplate", String.class).get();
    String whenTemplate = bprog.getFromGlobalScope("whenTemplate", String.class).get();
    String template = bprog.getFromGlobalScope("template", String.class).get();
    String[] btFuncBody = bprog.getFromGlobalScope("btFuncBody", String[].class).get();
    String[] beforeBeforeSyncOptions = bprog.getFromGlobalScope("beforeBeforeSyncOptions", String[].class).get();
    String[] dataTypes = bprog.getFromGlobalScope("dataTypes", String[].class).get();
    String[] btFuncOptions = bprog.getFromGlobalScope("btFuncOptions", String[].class).get();
    String[] helperCallOptions = bprog.getFromGlobalScope("helperCallOptions", String[].class).get();
    Map<String, String>[] whenOptions = bprog.getFromGlobalScope("whenOptions", Map[].class).get();

    int iteration = 0;
    try (var out = new PrintStream("exports/test.csv")) {
      out.println("iteration,round,states,events,transitions,accepting,code");

      for (int btFuncBodyI = 0; btFuncBodyI < btFuncBody.length; btFuncBodyI++) {
        for (int btFuncOptionsI = 0; btFuncOptionsI < btFuncOptions.length; btFuncOptionsI++) {
          for (int helperCallOptionsI = 0; helperCallOptionsI < helperCallOptions.length; helperCallOptionsI++) {
            for (int whenOptionsI = 0; whenOptionsI < whenOptions.length; whenOptionsI++) {
              for (int dataTypesI = 0; dataTypesI < dataTypes.length; dataTypesI++) {
                for (int beforeBeforeSyncOptionsI = 0; beforeBeforeSyncOptionsI < beforeBeforeSyncOptions.length; beforeBeforeSyncOptionsI++) {
                  var when = whenOptions[whenOptionsI];
                  if (helperCallOptionsI == 1 && when.getOrDefault("%%data-variable%%", "").equals("bp.sync({ waitFor: eventSet }).data"))
                    continue;
                  var runner = new Bug2Detector();
                  runner.code = template
                      .replace("%%btFuncOptions%%", btFuncOptions[btFuncOptionsI])
                      .replace("%%btFuncBody%%", btFuncBody[btFuncBodyI])
                      .replace("%%helper-call%%", format(helperCallOptions[helperCallOptionsI], 2))
                      .replace("%%before-before-sync%%", format(beforeBeforeSyncOptions[beforeBeforeSyncOptionsI], 2))
                      .replace("%%before-while%%", format(when.get("%%before-while%%"), 1))
                      .replace("%%before-sync%%", format(when.get("%%before-sync%%"), 2))
                      .replace("%%sync%%", when.getOrDefault("%%sync%%", ""))
                      .replace("%%after-sync%%", when.getOrDefault("%%after-sync%%", ""))
                      .replace("%%before-end-while%%", format(when.get("%%before-end-while%%"), 2))
                      .replace("%%data-variable%%", format(when.get("%%data-variable%%"), 0))
                      .replace("%%after-while%%", format(when.get("%%after-while%%"), 1))
                      .replace("%%dataTypes%%", dataTypes[dataTypesI]);
                  System.out.println("**** iteration: " + iteration + "****");
                  System.out.println("**** code ****\n" + runner.code + "\n\n\n");
                  for (int round = 0; round < MAX_ROUNDS; round++) {
                    bprog = SpaceMapperCliRunner.getBProgram(new String[]{"test.js"});
                    bprog.appendSource(runner.code);
                    //    bprog.putInGlobalScope("removeParent", removeParent);
                    var runName = "test_" + iteration + "_" + round;
                    bprog.setName(runName);
                    System.out.println("// start");

                    // You can use a different EventSelectionStrategy, for example:
                    //    var ess = new PrioritizedBSyncEventSelectionStrategy();
                    //    ess.setDefaultPriority(0);
                    //    bprog.setEventSelectionStrategy(ess);
                    MapperResult res = runner.mapSpace(bprog);
                    runner.exportSpace(runName, res);
                    out.println(new Stats(iteration, round, res.states().size(), res.events.size(), res.edges().size(), (int) res.states().stream().map(vertex -> !Graphs.vertexHasSuccessors(res.graph, vertex)).filter(v -> v).count(), null, null, null, null, null, null, runner.code));
                    //    res.findBug();

                    //    WARNING: May take extremely long time and may generate extremely large files
                    //    writeCompressedPaths(runName + ".csv", null, res, "exports");
                  }
                  iteration++;
                }
              }
            }
          }
        }
      }
    }
    System.out.println("// done");
  }

  static String format(String str, int numberOfTabs) {
    if (str == null) return "";
    if (str.equals("")) return str;
    String tabs = String.join("", Collections.nCopies(numberOfTabs, "  "));
    var ret = tabs + str
        .replace(";", ";\n" + tabs)
//        .replace("{", "{\n" + tabs)
//        .replace("}", "}\n");
        ;
    if (ret.endsWith(tabs)) ret = ret.substring(0, ret.length() - tabs.length());
    return ret;
  }

  /**
   * Reads given resource file as a string.
   *
   * @param fileName path to the resource file
   * @return the file's contents
   * @throws IOException if read fails for any reason
   */
  static String getResourceFileAsString(String fileName) throws IOException {
    ClassLoader classLoader = ClassLoader.getSystemClassLoader();
    try (InputStream is = classLoader.getResourceAsStream(fileName)) {
      if (is == null) return null;
      try (InputStreamReader isr = new InputStreamReader(is);
           BufferedReader reader = new BufferedReader(isr)) {
        return reader.lines().collect(Collectors.joining("\n"));
      }
    }
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
    private final FuncName funcName;
    private final FuncBody funcBody;
    private final RemoveObjectParent removeObjectParent;
    private final HelperType helperType;
    private final DataDeclaration dataDeclaration;
    private final ResetData resetData;
    public final String code;

    private Stats(int iteration, int round, int states, int events, int transitions, int accepting, FuncName funcName, FuncBody funcBody, RemoveObjectParent removeObjectParent, HelperType helperType, DataDeclaration dataDeclaration, ResetData resetData, String code) {
      this.iteration = iteration;
      this.round = round;
      this.states = states;
      this.events = events;
      this.transitions = transitions;
      this.accepting = accepting;
      this.funcName = funcName;
      this.funcBody = funcBody;
      this.removeObjectParent = removeObjectParent;
      this.helperType = helperType;
      this.dataDeclaration = dataDeclaration;
      this.resetData = resetData;
      this.code = code;
    }

    @Override
    public String toString() {
      return MessageFormat.format("{0},{1},{2},{3},{4},{5},\"{6}\",\"{7}\",\"{8}\",\"{9}\",\"{10}\",\"{11}\",\"{12}\",\"{13}\"",
          iteration, round, states, events, transitions, accepting,
          funcName.name(), funcBody.name(),
          removeObjectParent.name(),
          helperType.name(), dataDeclaration.name(), resetData.name(),
          code);
    }
  }

  enum HelperType {
    global("globalWhenHelper(%%data-variable-name%%, f);\n"),
    inner("innerWhenHelper(%%data-variable-name%%, f);\n"),
    inline("bp.registerBThread('when helper', function () {\n" +
        "    f(%%data-variable-name%%);\n" +
        "  });\n"),
    inlineGuarded("(function(d) {\n" +
        "  bp.registerBThread('when helper', function () {\n" +
        "    f(d);\n" +
        "  });\n" +
        "})(%%data-variable-name%%);\n");

    private final String code;

    HelperType(String code) {
      this.code = code;
    }

    public String replace(String code, DataDeclaration dataDeclaration) {
      return code
          .replace("%%helper%%\n", Bug2Detector.format(this.code, 2))
          .replace("%%data-variable-name%%", dataDeclaration.dataVariableName());
    }
  }

  enum DataType {
    var,
    let;

    public String replace(String code) {
      return code.replace("%%data-type%%", this.name());
    }
  }

  enum DataDeclaration {
    none,
    beforeWhile,
    insideWhile;

    public String dataVariableName() {
      return this == none ? "bp.sync({ waitFor: eventSet }).data" : "data";
    }

    public String replace(String code) {
      switch (this) {
        case none:
          return code
              .replace("%%declare-data%%\n", "")
              .replace("%%sync-data%%\n", "")
              .replace("%%reset-data%%\n", "");
        case beforeWhile:
          return code
              .replace("%%declare-data%%\n", Bug2Detector.format("%%data-type%% data = null;", 1))
              .replace("%%sync-data%%\n", Bug2Detector.format("data = bp.sync({ waitFor: eventSet }).data;", 2));
        case insideWhile:
          return code
              .replace("%%declare-data%%\n", "")
              .replace("%%sync-data%%\n", Bug2Detector.format("%%data-type%% data = bp.sync({ waitFor: eventSet }).data;", 2));
      }
      throw new IllegalArgumentException();
    }
  }

  enum FuncName {
    func,
    inline;

    public String replace(String code) {
      switch (this) {
        case func:
          return code.replace("%%bt-func-name%%", "func");
        case inline:
          return code.replace("%%bt-func-name%%", "function(e) {" +
              "  %%bt-func-body%%" +
              "  }");
      }
      throw new IllegalArgumentException();
    }
  }

  enum FuncBody {
    oneLine,
    twoLines;

    public String replace(String code) {
      switch (this) {
        case oneLine:
          return code.replace("%%bt-func-body%%\n", Bug2Detector.format("addToCart({ s: e.s });", 1));
        case twoLines:
          return code.replace("%%bt-func-body%%\n", Bug2Detector.format("addToCart({ s: e.s });checkOut({ s: e.s });", 1));
      }
      throw new IllegalArgumentException();
    }
  }

  enum ResetData {
    True(true),
    False(false);
    private final boolean value;

    ResetData(boolean value) {
      this.value = value;
    }

    public String replace(String code) {
      return code.replace("%%reset-data%%\n", Bug2Detector.format(value ? "data = null;" : "", 2));
    }
  }

  enum RemoveObjectParent {
    True(true),
    False(false);
    private final boolean value;

    RemoveObjectParent(boolean value) {
      this.value = value;
    }

    public String replace(String code) {
      return code.replace("%%remove-parent%%\n", Bug2Detector.format(value ? "SpaceMapperCliRunner.removeParent.accept(data);" : "", 1));
    }
  }

  enum SyncFoo {
    True(true),
    False(false);
    private final boolean value;

    SyncFoo(boolean value) {
      this.value = value;
    }

    public String replace(String code) {
      return code.replace("%%sync-foo%%\n", Bug2Detector.format(value ? "bp.sync({ request: bp.Event('foo') });" : "", 2));
    }
  }

  private static final String whenTemplate =
      "const when = function (eventSet, f) {\n" +
          "  const innerWhenHelper = function(d) {\n" +
          "    bp.registerBThread('when helper', function () {\n" +
          "      f(d);\n" +
          "    });\n" +
          "  };\n" +
          "%%declare-data%%" +
          "  while (true) {\n" +
          "%%sync-foo%%" +
          "%%sync-data%%" +
          "%%helper%%" +
          "%%reset-data%%" +
          "  }" +
          "};\n\n";

  public static void run2() throws Exception {
    String template = getResourceFileAsString("test.js");
    var iteration = 0;
    try (var out = new PrintStream("exports/test.csv")) {
      out.println("Iteration,Round,States,Events,Transitions,Accepting,RemoveObjectParent,HelperType,DataDeclaration,ResetData,Code");
      for (var funcName : FuncName.values()) {
        for (var funcBody : FuncBody.values()) {
          for (var syncFoo : SyncFoo.values()) {
            for (var removeObjectParent : RemoveObjectParent.values()) {
              for (var helperType : HelperType.values()) {
                for (var dataDeclaration : DataDeclaration.values()) {
                  for (var resetData : ResetData.values()) {
                    if (dataDeclaration == DataDeclaration.none && resetData == ResetData.True) {
                      // We need only once since there is no data variable
                      continue;
                    }
                    for (var dataType : DataType.values()) {
                      if (dataDeclaration == DataDeclaration.none && dataType == DataType.let) {
                        // We need only once since there is no data variable
                        continue;
                      }

                      String code = template;
                      code = funcName.replace(code);
                      code = funcBody.replace(code);
                      code = syncFoo.replace(code);
                      code = removeObjectParent.replace(code);
                      code = helperType.replace(code, dataDeclaration);
                      code = dataDeclaration.replace(code);
                      code = dataType.replace(code);
                      code = resetData.replace(code);

                      var runner = new Bug2Detector();
                      runner.code = code;
                      System.out.println("**** iteration: " + iteration + "****");
                      System.out.println("**** code ****\n" + runner.code + "\n\n\n");
                      for (int round = 0; round < MAX_ROUNDS; round++) {
                        var bprog = new StringBProgram(code);
                        var runName = "test_" + iteration + "_" + round;
                        bprog.setName(runName);
                        System.out.println("// start");

                        MapperResult res = runner.mapSpace(bprog);
                        runner.exportSpace(runName, res);
                        out.println(new Stats(iteration, round, res.states().size(), res.events.size(), res.edges().size(),
                            (int) res.states().stream().map(vertex -> !Graphs.vertexHasSuccessors(res.graph, vertex)).filter(v -> v).count(),
                            funcName, funcBody,
                            removeObjectParent,
                            helperType, dataDeclaration, resetData,
                            runner.code));
                      }
                      iteration++;
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}