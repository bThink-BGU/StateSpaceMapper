package il.ac.bgu.cs.bp.statespacemapper;

import il.ac.bgu.cs.bp.bpjs.internal.ScriptableUtils;
import il.ac.bgu.cs.bp.bpjs.model.BProgramSyncSnapshot;
import il.ac.bgu.cs.bp.bpjs.model.SyncStatement;

import java.io.PrintStream;
import java.text.MessageFormat;

import static java.util.stream.Collectors.joining;

public class TraceResultGVWriter extends TraceResultWriter {
  private int level;

  public TraceResultGVWriter(PrintStream out, GenerateAllTracesInspection.MapperResult result, String name) {
    super(out, result, name);
  }

  @Override
  public void write() {
    level = 0;
    out.println("digraph " + GVUtils.sanitize(name) + " {");
    level++;
    out.println(result.states.stream()
        .map(this::printBpss)
        .collect(joining("\n")));
    out.println(result.links.stream().map(this::printLink)
        .collect(joining("\n")));
    level--;
    out.println("}");
  }

  private static String getGuardedString(Object o) {
    return o.toString().replace("\"", "\\\"").replace("\n", "").replace("JS_Obj ", "");
  }

  protected String printLink(GenerateAllTracesInspection.Link link) {
    return MessageFormat.format("{0}\"{1}\" -> \"{2}\" [label=\"{3}\"]", "  ".repeat(level), nodeName(link.src), nodeName(link.dst), linkName(link.event));
  }

  protected String printBpss(BProgramSyncSnapshot bpss) {
    String id = nodeName(bpss);
    String store = bpss.getDataStore().entrySet().stream()
        .map(entry -> "{" + getGuardedString(ScriptableUtils.stringify(entry.getKey())) + "," + getGuardedString(ScriptableUtils.stringify(entry.getValue())) + "}")
        .collect(joining(",", "Store: [", "]"));
    String statements = bpss.getBThreadSnapshots().stream()
        .map(btss -> {
          SyncStatement syst = btss.getSyncStatement();
          return
              "{name: " + getGuardedString(btss.getName()) + ",\n" +
                  "isHot: " + syst.isHot() + ",\n" +
                  "request: " + syst.getRequest().stream().map(e -> getGuardedString(linkName(e))).collect(joining(",", "[", "]")) + ",\n" +
                  "waitFor: " + getGuardedString(syst.getWaitFor()) + ",\n" +
                  "block: " + getGuardedString(syst.getBlock()) + ",\n" +
                  "interrupt: " + getGuardedString(syst.getInterrupt()) + "}";
        })
        .collect(joining(",\n", "Statements: [", "]"));
    String pattern = bpss.equals(result.startNode) ? "{0}\"{1}\" [fontcolor=blue label=\"start {1}\"]" : "{0}\"{1}\" [label=\"{1}\"]";
    return MessageFormat.format(pattern, "  ".repeat(level), id);
//    String pattern = bpss.equals(result.startNode) ? "{0}{1} [fontcolor=blue label=\"start {1}\n{2}\n{3}\"]" : "{0}{1} [label=\"{1}\n{2}\n{3}\"]";
//    return MessageFormat.format(pattern, "  ".repeat(level), id, store, statements);
  }
}
