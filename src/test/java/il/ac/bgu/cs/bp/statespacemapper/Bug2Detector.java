package il.ac.bgu.cs.bp.statespacemapper;

import il.ac.bgu.cs.bp.bpjs.model.StringBProgram;
import il.ac.bgu.cs.bp.statespacemapper.jgrapht.exports.Exporter;
import org.jgrapht.Graphs;
import org.jgrapht.nio.DefaultAttribute;
import org.svvrl.goal.core.aut.fsa.Equivalence;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.stream.Collectors;

public class Bug2Detector extends SpaceMapperCliRunner {
  private static final int MAX_ROUNDS = 1;
  private String code = "";

  public static void main(String[] args) throws Exception {
    Files.createDirectories(Path.of("exports"));
    if (args.length == 0)
      run2();
    else {
      String template = getResourceFileAsString(args[0]);
      var stats = new Stats(template.substring(1, template.length() - 1).split("\\$"));
      try (var out = new PrintStream("exports/test-eval.csv")) {
        out.println(Stats.header());
        runOnce(out, stats);
      }
    }
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
  public static String getResourceFileAsString(String fileName) throws IOException {
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
      vertex.accepting = accepting;
      map.put("accepting", DefaultAttribute.createAttribute(accepting));
      map.put("shape", DefaultAttribute.createAttribute(accepting ? "doublecircle" : "circle"));
      map.put("label", DefaultAttribute.createAttribute(""));
      return map;
    });
  }

  public static class Stats {
    public final int iteration;
    public final int round;
    public final int states;
    public final int events;
    public final int transitions;
    public final int accepting;
    public final FuncName funcName;
    public final FuncBody funcBody;
    public final RemoveObjectParent removeObjectParent;
    public final SyncFoo syncFoo;
    public final HelperType helperType;
    public final DataDeclaration dataDeclaration;
    public final ResetData resetData;
    public final DataType dataType;
    public final String code;

    public Stats(int iteration, int round, int states, int events, int transitions, int accepting,
                  FuncName funcName, FuncBody funcBody, RemoveObjectParent removeObjectParent, SyncFoo syncFoo,
                  HelperType helperType, DataDeclaration dataDeclaration, ResetData resetData, DataType dataType,
                  String code) {
      this.iteration = iteration;
      this.round = round;
      this.states = states;
      this.events = events;
      this.transitions = transitions;
      this.accepting = accepting;
      this.funcName = funcName;
      this.funcBody = funcBody;
      this.removeObjectParent = removeObjectParent;
      this.syncFoo = syncFoo;
      this.helperType = helperType;
      this.dataDeclaration = dataDeclaration;
      this.resetData = resetData;
      this.dataType = dataType;
      this.code = code;
    }

    public Stats(String[] args) {
      this(
          Integer.parseInt(args[0]),
          Integer.parseInt(args[1]),
          Integer.parseInt(args[2]),
          Integer.parseInt(args[3]),
          Integer.parseInt(args[4]),
          Integer.parseInt(args[5]),
          FuncName.valueOf(args[6]),
          FuncBody.valueOf(args[7]),
          Boolean.parseBoolean(args[8]) ? RemoveObjectParent.True : RemoveObjectParent.False,
          Boolean.parseBoolean(args[9]) ? SyncFoo.True : SyncFoo.False,
          HelperType.valueOf(args[10]),
          DataDeclaration.valueOf(args[11]),
          Boolean.parseBoolean(args[12]) ? ResetData.True : ResetData.False,
          DataType.valueOf(args[13]),
          args[14]
      );
    }

    public static String header() {
      return "Iteration,Round,States,Events,Transitions,Accepting,FuncName,FuncBody,RemoveObjectParent,SyncFoo,HelperType,DataDeclaration,ResetData,DataType,Code";
    }

    @Override
    public String toString() {
      return MessageFormat.format("{0},{1},{2},{3},{4},{5},\"{6}\",\"{7}\",\"{8}\",\"{9}\",\"{10}\",\"{11}\",\"{12}\",\"{13}\",\"{14}\"",
          iteration, round, states, events, transitions, accepting,
          funcName.name(), funcBody.name(),
          removeObjectParent.name(), syncFoo.name(),
          helperType.name(), dataDeclaration.name(), resetData.name(), dataType.name(),
          code);
    }

    public String toStringNoCode() {
      return MessageFormat.format("{0},{1},{2},{3},{4},{5},\"{6}\",\"{7}\",\"{8}\",\"{9}\",\"{10}\",\"{11}\",\"{12}\",\"{13}\",",
          iteration, round, states, events, transitions, accepting,
          funcName.name(), funcBody.name(),
          removeObjectParent.name(), syncFoo.name(),
          helperType.name(), dataDeclaration.name(), resetData.name(), dataType.name()
      );
    }
  }

  public enum HelperType {
    global("globalWhenHelper(%%data-variable-name%%, f);\n"),
    inner("innerWhenHelper(%%data-variable-name%%, f);\n"),
    inline("(function(d) {\n" +
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

  public enum DataType {
    var,
    let;

    public String replace(String code) {
      return code.replace("%%data-type%%", this.name());
    }
  }

  public enum DataDeclaration {
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

  public enum FuncName {
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

  public enum FuncBody {
    oneLine,
    twoLines;

    public String replace(String code) {
      switch (this) {
        case oneLine:
          return code.replace("%%bt-func-body%%", Bug2Detector.format("addToCart({ s: e.s });", 1));
        case twoLines:
          return code.replace("%%bt-func-body%%", Bug2Detector.format("addToCart({ s: e.s });checkOut({ s: e.s });", 1));
      }
      throw new IllegalArgumentException();
    }
  }

  public enum ResetData {
    True(true),
    False(false);
    private final boolean value;

    ResetData(boolean value) {
      this.value = value;
    }

    public String replace(String code, DataDeclaration dataDeclaration) {
      return code.replace("%%reset-data%%\n", Bug2Detector.format(value ? dataDeclaration == DataDeclaration.insideWhile ? "delete data;" : "data = null;" : "", 2));
    }
  }

  public enum RemoveObjectParent {
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

  public enum SyncFoo {
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

  public static void run2() throws Exception {
    String template = getResourceFileAsString("test.js");
    var iteration = 0;
    try (var out1 = new PrintStream("exports/test.csv");
         var out2 = new PrintStream("exports/test.txt")) {
      out1.println(Stats.header());
      for (var funcBody : FuncBody.values()) {
        for (var syncFoo : SyncFoo.values()) {
          var list = new ArrayList<Stats>();
          for (var funcName : FuncName.values()) {
            for (var removeObjectParent : RemoveObjectParent.values()) {
              for (var helperType : HelperType.values()) {
                for (var dataDeclaration : DataDeclaration.values()) {
                  for (var resetData : ResetData.values()) {
                    if ((dataDeclaration == DataDeclaration.beforeWhile/* || dataDeclaration == DataDeclaration.insideWhile*/) && resetData == ResetData.False) {
                      // When checking semantics - no need to check this combination because it makes no sense
                      continue;
                    }
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
                      code = resetData.replace(code, dataDeclaration);

                      System.out.println("**** iteration: " + iteration + "****");
                      System.out.println("**** code ****\n" + code + "\n\n\n");
                      for (int round = 0; round < MAX_ROUNDS; round++) {
                        list.add(runOnce(out1, iteration, round, funcName, funcBody, syncFoo, removeObjectParent, helperType, dataDeclaration, resetData, dataType, code));
                      }
                      iteration++;
                    }
                  }
                }
              }
            }
          }
          for (int i = 1; i < list.size(); i++) {
            var first = list.get(0);
            var fsaFirst = GoalEqualCheck.getFSA(first.iteration, first.round);
            var second = list.get(i);
            var fsaSecond = GoalEqualCheck.getFSA(second.iteration, second.round);
            var eq = new Equivalence();
            boolean found = false;
            if (first.states != second.states || first.transitions != second.transitions) {
              out2.println("states/transitions:\n" + first.toStringNoCode() + "\n" + second.toStringNoCode());
              found = true;
            }
            if (!eq.isEquivalent(fsaFirst, fsaSecond).isEquivalent()) {
              out2.println("equivalence:\n" + first.toStringNoCode() + "\n" + second.toStringNoCode());
              found = true;
            }
            if (found) out2.println();
          }
        }
      }
    }
  }

  public static Stats runOnce(PrintStream out, Stats stats) throws Exception {
    return runOnce(out, stats.iteration, stats.round,
        stats.funcName, stats.funcBody, stats.syncFoo,
        stats.removeObjectParent, stats.helperType,
        stats.dataDeclaration, stats.resetData, stats.dataType, stats.code);
  }

  private static Stats runOnce(PrintStream out, int iteration, int round,
                               FuncName funcName, FuncBody funcBody, SyncFoo syncFoo,
                               RemoveObjectParent removeObjectParent, HelperType helperType,
                               DataDeclaration dataDeclaration, ResetData resetData, DataType dataType, String code) throws Exception {
    var runner = new Bug2Detector();
    runner.code = code;
    var bprog = new StringBProgram(code);
    var runName = "test_" + iteration + "_" + round;
    bprog.setName(runName);
    System.out.println("// start");

    MapperResult res = runner.mapSpace(bprog);
    runner.exportSpace(runName, res);
    var stats = new Stats(iteration, round, res.states().size(), res.events.size(), res.edges().size(),
        (int) res.states().stream().map(vertex -> !Graphs.vertexHasSuccessors(res.graph, vertex)).filter(v -> v).count(),
        funcName, funcBody,
        removeObjectParent, syncFoo,
        helperType, dataDeclaration, resetData, dataType,
        runner.code);
    out.println(stats);
    return stats;
  }
}