package il.ac.bgu.cs.bp.statespacemapper.writers;

import il.ac.bgu.cs.bp.bpjs.model.BEvent;
import il.ac.bgu.cs.bp.bpjs.model.BProgramSyncSnapshot;
import il.ac.bgu.cs.bp.bpjs.model.BThreadSyncSnapshot;
import il.ac.bgu.cs.bp.statespacemapper.GenerateAllTracesInspection;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class TraceResultYamlWriter extends TraceResultWriter {
  private int level;

  public TraceResultYamlWriter(String name) {
    super(name, "yaml","\n","\n");
    this.printStatements = true;
    this.printStore = true;
  }

  @Override
  protected String eventToString(BEvent event) {
    return event.name;
  }

  @Override
  protected void writePre() {
    level = 0;
    out.println("  ".repeat(level) + "\"name\": \"" + name + "\", ");
    out.println("  ".repeat(level) + "\"start\": \"" + result.states.get(result.startNode) + "\", ");
    out.println("  ".repeat(level) + "\"runDate\": \"" + (DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.now())) + "\",");
    out.println("  ".repeat(level) + "\"# states\": " + result.states.size() + ", ");
    out.println("  ".repeat(level) + "\"# transitions\": " + result.edges.size() + ", ");
    if (result.traces != null)
      out.println("  ".repeat(level) + "\"# traces\": " + result.traces.size() + ", ");
  }

  @Override
  protected void writeNodesPre() {
    out.println("  ".repeat(level) + "\"states\":");
    level++;
  }

  @Override
  protected String nodeToString(int id, BProgramSyncSnapshot bpss) {
    StringBuilder out = new StringBuilder();
    out.append("  ".repeat(level) + "- id: " + id + "\n");
    Set<String> R = new HashSet<>();
    Set<String> W = new HashSet<>();
    Set<String> B = new HashSet<>();
    bpss.getBThreadSnapshots().stream()
        .map(BThreadSyncSnapshot::getSyncStatement).forEach(s -> {
      R.addAll(s.getRequest().stream().map(this::eventToString).collect(Collectors.toList()));
      W.add(s.getWaitFor().toString());
      B.add(s.getBlock().toString());
    });
    out.append("  ".repeat(level) + "  R: " + R + "\n");
    out.append("  ".repeat(level) + "  W: " + W + "\n");
    out.append("  ".repeat(level) + "  B: " + B + "");
    return out.toString();
  }

  @Override
  protected void writeNodesPost() {
    level--;
  }

  @Override
  protected void writeEdgesPre() {
    out.println("  ".repeat(level) + "\"transitions\":");
    level++;
  }

  @Override
  protected void writeEdgesPost() {
    level--;
  }

  private static String getGuardedString(Object o) {
    return o.toString().replace("\"", "\\\"").replace("\n", "").replace("JS_Obj ", "");
  }

  @Override
  protected String edgeToString(GenerateAllTracesInspection.Edge edge) {
    StringBuilder out = new StringBuilder();
    out.append("  ".repeat(level) + "- source: " + edge.srcId + "\n");
    out.append("  ".repeat(level) + "  target: " + edge.dstId + "\n");
    out.append("  ".repeat(level) + "  event: " + eventToString(edge.event));
    return out.toString();
  }
}
