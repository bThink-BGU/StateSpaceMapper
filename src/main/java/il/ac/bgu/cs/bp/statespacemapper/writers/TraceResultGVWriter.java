package il.ac.bgu.cs.bp.statespacemapper.writers;

import il.ac.bgu.cs.bp.bpjs.internal.ScriptableUtils;
import il.ac.bgu.cs.bp.bpjs.model.BEvent;
import il.ac.bgu.cs.bp.bpjs.model.BProgramSyncSnapshot;
import il.ac.bgu.cs.bp.bpjs.model.SyncStatement;
import il.ac.bgu.cs.bp.statespacemapper.GenerateAllTracesInspection;

import java.io.PrintStream;
import java.text.MessageFormat;

import static java.util.stream.Collectors.joining;

public class TraceResultGVWriter extends TraceResultWriter {
  private int level;

  public TraceResultGVWriter(PrintStream out, GenerateAllTracesInspection.MapperResult result, String name) {
    super(out, result, name);
  }

  @Override
  protected void writePre() {
    level = 0;
    out.println("digraph " + sanitize(name) + " {");
    level++;
  }

  @Override
  protected void writeNodesPost() {
    out.println(nodeToString(-1, null));
  }

  @Override
  protected void writeEdgesPost() {
    out.println(edgeToString(new GenerateAllTracesInspection.Edge(-1, null, result.startNodeId, null, new BEvent(""))));
  }

  @Override
  protected String nodeToString(int id, BProgramSyncSnapshot bpss) {
    boolean startNode = id == -1;
    boolean acceptingNode = result.acceptingStates.containsKey(id);

    String store = startNode || !printStore ? "" : getStore(bpss);

    String statements = startNode || !printStatements ? "" : getStatments(bpss);

    String shape = "shape="+ (startNode ? "none " : acceptingNode? "doublecircle " : "circle ");

    String label = startNode ? "label=\"start\" " : printNodeLabel() ? MessageFormat.format("label=\"{0}{1}{2}\" ",id,store,statements) : "label=\"\" ";

    String fillcolor = "";//printNodeLabel() ? "" : "fillcolor=black style=filled ";

    return MessageFormat.format("{0}{1} [{2}{3}{4}]", "  ".repeat(level), id, shape, label, fillcolor);
  }

  protected boolean printNodeLabel() {
    return printStatements || printStore;
  }

  protected String getStore(BProgramSyncSnapshot bpss) {
    return bpss.getDataStore().entrySet().stream()
        .map(entry -> "{" + sanitize(ScriptableUtils.stringify(entry.getKey())) + "," + sanitize(ScriptableUtils.stringify(entry.getValue())) + "}")
        .collect(joining(",", "\nStore: [", "]"));
  }

  protected String getStatments(BProgramSyncSnapshot bpss) {
    return bpss.getBThreadSnapshots().stream()
        .map(btss -> {
          SyncStatement syst = btss.getSyncStatement();
          return
              "{name: " + sanitize(btss.getName()) + ",\n" +
                  "isHot: " + syst.isHot() + ",\n" +
                  "request: " + syst.getRequest().stream().map(e -> sanitize(eventToString(e))).collect(joining(",", "[", "]")) + ",\n" +
                  "waitFor: " + sanitize(syst.getWaitFor()) + ",\n" +
                  "block: " + sanitize(syst.getBlock()) + ",\n" +
                  "interrupt: " + sanitize(syst.getInterrupt()) + "}";
        })
        .collect(joining(",\n", "\nStatements: [", "]"));
  }

  @Override
  protected String edgeToString(GenerateAllTracesInspection.Edge edge) {
    return MessageFormat.format("{0}{1} -> {2} [label=\"{3}\"]", "  ".repeat(level), edge.srcId, edge.dstId, sanitize(eventToString(edge.event)));
  }

  @Override
  protected void writePost() {
    level--;
    out.println("}");
  }

  private static String sanitize(Object in) {
    return in.toString()
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("JS_Obj ", "")
        .replaceAll("[\\. \\-+]", "_");
  }
}
