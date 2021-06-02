package il.ac.bgu.cs.bp.statespacemapper.writers;

import il.ac.bgu.cs.bp.bpjs.internal.ScriptableUtils;
import il.ac.bgu.cs.bp.bpjs.model.BProgramSyncSnapshot;
import il.ac.bgu.cs.bp.bpjs.model.SyncStatement;
import il.ac.bgu.cs.bp.statespacemapper.GenerateAllTracesInspection;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static java.util.stream.Collectors.joining;

public class TraceResultJsonWriter extends TraceResultWriter {
  protected int level;

  public TraceResultJsonWriter(String name) {
    super(name, "json", ",\n", ",\n");
    this.printStatements = true;
    this.printStore = true;
  }

  @Override
  protected void writePre() {
    level = 0;
    out.println("{");
    level++;
    out.println("  ".repeat(level) + "\"name\": \"" + name + "\", ");
    out.println("  ".repeat(level) + "\"start\": \"" + result.states.get(result.startNode) + "\", ");
    out.println("  ".repeat(level) + "\"runDate\": \"" + (DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.now())) + "\",");
    out.println("  ".repeat(level) + "\"# states\": " + result.states.size() + ", ");
    out.println("  ".repeat(level) + "\"# transitions\": " + result.edges.size() + ", ");
    if(result.traces != null)
      out.println("  ".repeat(level) + "\"# traces\": " + result.traces.size() + ", ");
  }

  @Override
  protected void writeNodesPre() {
    out.println("  ".repeat(level) + "\"states\": [\n");
    level++;
  }

  @Override
  protected String nodeToString(int id, BProgramSyncSnapshot bpss) {
    StringBuilder out = new StringBuilder();
    out.append("  ".repeat(level) + "{\n");
    out.append("  ".repeat(level + 1) + "\"id\":\"" + id + "\"");
    if (printStore) {
      out.append(",\n");
      out.append("  ".repeat(level + 1) + "\"store\": [\n");
      out.append(bpss.getDataStore().entrySet().stream()
          .map(entry -> "  ".repeat(level + 2) + "\"" + getGuardedString(ScriptableUtils.stringify(entry.getValue())) + "\"")
          .collect(joining(",\n")));
      out.append("\n" + "  ".repeat(level + 1) + "]");
    }
    if (printStatements) {
      out.append(",\n");
      out.append("  ".repeat(level + 1) + "\"statements\": [\n");
      out.append(bpss.getBThreadSnapshots().stream()
          .map(btss -> {
            SyncStatement syst = btss.getSyncStatement();
            return
                "  ".repeat(level + 2) + "{\n" +
                    "  ".repeat(level + 3) + "\"name\":\"" + getGuardedString(btss.getName()) + "\",\n" +
                    "  ".repeat(level + 3) + "\"isHot\":" + syst.isHot() + ",\n" +
                    "  ".repeat(level + 3) + "\"request\":" + syst.getRequest().stream().map(e -> "\"" + getGuardedString(e) + "\"").collect(joining(",", "[", "]")) + ",\n" +
                    "  ".repeat(level + 3) + "\"waitFor\":\"" + getGuardedString(syst.getWaitFor()) + "\",\n" +
                    "  ".repeat(level + 3) + "\"block\":\"" + getGuardedString(syst.getBlock()) + "\",\n" +
                    "  ".repeat(level + 3) + "\"interrupt\":\"" + getGuardedString(syst.getInterrupt()) + "\"\n" +
                    "  ".repeat(level + 2) + "}";
          })
          .collect(joining(",\n")));
      out.append("\n" + "  ".repeat(level + 1) + "]");
    }
    out.append("\n" + "  ".repeat(level) + "}");
    return out.toString();
  }

  @Override
  protected void writeNodesPost() {
    level--;
    out.println("  ".repeat(level) + "],");
  }

  @Override
  protected void writeEdgesPre() {
    out.println("  ".repeat(level) + "\"links\": [");
    level++;
  }

  @Override
  protected void writeEdgesPost() {
    level--;
    out.println("  ".repeat(level) + "]");
  }

  @Override
  protected void writePost() {
    out.println("}");
  }

  protected static String getGuardedString(Object o) {
    return o.toString().replace("\"", "\\\"").replace("\n", "").replace("JS_Obj ", "");
  }

  @Override
  protected String edgeToString(GenerateAllTracesInspection.Edge edge) {
    return "  ".repeat(level) + "{\"source\":\"" + edge.srcId + "\", \"target\":\"" + edge.dstId + "\", \"eventData\":\"" + getGuardedString(edge.event) + "\"}";
  }
}
