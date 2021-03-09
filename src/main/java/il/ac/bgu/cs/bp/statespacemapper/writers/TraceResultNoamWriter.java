package il.ac.bgu.cs.bp.statespacemapper.writers;

import il.ac.bgu.cs.bp.bpjs.internal.ScriptableUtils;
import il.ac.bgu.cs.bp.bpjs.model.BProgramSyncSnapshot;
import il.ac.bgu.cs.bp.bpjs.model.SyncStatement;
import il.ac.bgu.cs.bp.statespacemapper.GenerateAllTracesInspection;

import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;

/**
 * A Writer for the <a href="https://github.com/izuzak/noam">NOAM tool</a><br/>
 * The resulted automaton can be translated to a regular expression using <a href="http://ivanzuzak.info/noam/webapps/fsm2regex/">fsm2regex</a>
 */
public class TraceResultNoamWriter extends TraceResultWriter {
  private int level;
  private final AtomicInteger edgeCounter = new AtomicInteger();

  public TraceResultNoamWriter(PrintStream out, GenerateAllTracesInspection.MapperResult result, String name) {
    super(out, result, name);
  }

  @Override
  protected void writePre() {
  }

  @Override
  protected void writeNodesPre() {
    out.println("#states");
  }

  @Override
  protected void writeNodesPost() {
    out.println("#initial");
    out.println("s" + result.startNodeId);
    out.println("#accepting");
    result.acceptingStates.forEach((key, bpss) -> out.println("s" + key));
  }

  @Override
  protected void writeEdgesPre() {
    out.println("#alphabet");
    result.edges.stream().map(e -> sanitize(eventToString(e.event))).distinct().sorted().forEachOrdered(out::println);
    out.println("#transitions");
  }

  @Override
  protected String nodeToString(int id, BProgramSyncSnapshot bpss) {
    return "s" + id;
  }

  @Override
  protected String edgeToString(GenerateAllTracesInspection.Edge edge) {
    return MessageFormat.format("s{0}:{1}>s{2}", edge.srcId, sanitize(eventToString(edge.event)), edge.dstId);
  }

  private static String sanitize(Object in) {
    return in.toString()
        .replace(" ", "_")
        .replace("\n", "_")
        .replace("\"", "\\\"")
        .replace("JS_Obj ", "")
        .replaceAll("[\\. \\-+]", "_");
  }
}
