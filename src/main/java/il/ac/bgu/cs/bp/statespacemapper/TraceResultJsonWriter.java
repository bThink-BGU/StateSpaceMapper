package il.ac.bgu.cs.bp.statespacemapper;

import il.ac.bgu.cs.bp.bpjs.internal.ScriptableUtils;
import il.ac.bgu.cs.bp.bpjs.model.BProgramSyncSnapshot;
import il.ac.bgu.cs.bp.bpjs.model.SyncStatement;

import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static java.util.stream.Collectors.joining;

public class TraceResultJsonWriter extends TraceResultWriter {
  private int level;
  public TraceResultJsonWriter(PrintStream out, GenerateAllTracesInspection.MapperResult result, String name) {
    super(out, result, name);
  }

  @Override
  public void write() {
    int level = 0;
    out.println("{");
    level++;
    out.println("  ".repeat(level) + "\"name\": \"" + name + "\", ");
    out.println("  ".repeat(level) + "\"start\": \"" + nodeName(result.startNode) + "\", ");
    out.println("  ".repeat(level) + "\"runDate\": \"" + (DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.now())) + "\",");
    out.println("  ".repeat(level) + "\"# states\": " + result.states.size() + ", ");
    out.println("  ".repeat(level) + "\"# transitions\": " + result.links.size() + ", ");
    out.println("  ".repeat(level) + "\"# traces\": " + result.traces.size() + ", ");
    out.println("  ".repeat(level) + "\"states\": [\n");
    level++;
    out.println(result.states.entrySet().stream()
            .map(this::printBpss)
            .collect(joining(",\n")));
    level--;
    out.println("  ".repeat(level) + "],");
    out.println("  ".repeat(level) + "\"links\": [");
    level++;
    out.println(result.links.stream().map(this::printLink).collect(joining(",\n")));
    level--;
    out.println("  ".repeat(level) + "]");
    out.println("}");
  }

  private static String getGuardedString(Object o) {
    return o.toString().replace("\"", "\\\"").replace("\n", "").replace("JS_Obj ", "");
  }

  protected String printLink(GenerateAllTracesInspection.Link link) {
    return "  ".repeat(level) + "{\"source\":\"" + nodeName(link.src) + "\", \"target\":\"" + nodeName(link.dst) + "\", \"eventData\":\"" + getGuardedString(link.event) + "\"}";
  }

  protected String printBpss(Map.Entry<BProgramSyncSnapshot, Integer> bpssEntry) {
    var bpss = bpssEntry.getKey();
    StringBuilder out = new StringBuilder();
    out.append("  ".repeat(level) + "{\n");
    out.append("  ".repeat(level + 1) + "\"id\":\"" + nodeName(bpss) + "\",\n");
    out.append("  ".repeat(level + 1) + "\"store\": [\n");
    out.append(bpss.getDataStore().entrySet().stream()
        .map(entry -> "  ".repeat(level + 2) + "\"" + getGuardedString(ScriptableUtils.stringify(entry.getValue())) + "\"")
        .collect(joining(",\n")));
    out.append("\n" + "  ".repeat(level + 1) + "],\n");
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
    out.append("\n" + "  ".repeat(level + 1) + "]\n");
    out.append("  ".repeat(level) + "}");
    return out.toString();
  }
}
