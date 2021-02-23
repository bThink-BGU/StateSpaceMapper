package il.ac.bgu.cs.bp.statespacemapper;

import il.ac.bgu.cs.bp.bpjs.internal.ScriptableUtils;
import il.ac.bgu.cs.bp.bpjs.model.BProgramSyncSnapshot;
import il.ac.bgu.cs.bp.bpjs.model.SyncStatement;

import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;

public class TraceResultGoalWriter extends TraceResultWriter {
  private int level;
  private final AtomicInteger edgeCounter = new AtomicInteger();

  public TraceResultGoalWriter(PrintStream out, GenerateAllTracesInspection.MapperResult result, String name) {
    super(out, result, name);
  }

  @Override
  protected void writePre() {
    out.println("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>");
    out.println("<Structure label-on=\"Transition\" type=\"FiniteStateAutomaton\">");
    level = 1;
    out.println(MessageFormat.format("{0}<Name>{1}</Name>", "    ".repeat(level), sanitize(name)));
    out.println("    ".repeat(level) + "<Description/>");
    out.println("    ".repeat(level) + "<Properties/>");
    out.println("    ".repeat(level) + "<Formula/>");
    out.println("    ".repeat(level) + "<Alphabet type=\"Classical\">");
    level++;
    result.edges.stream().map(e -> sanitize(eventToString(e.event))).collect(Collectors.toSet())
        .forEach(e -> out.println(MessageFormat.format("{0}<Symbol>{1}</Symbol>", "    ".repeat(level), e)));
    level--;
    out.println("    ".repeat(level) + "</Alphabet>");
  }

  @Override
  protected void writeNodesPre() {
    out.println("    ".repeat(level) + "<StateSet>");
    level++;
  }

  @Override
  protected void writeNodesPost() {
    level--;
    out.println("    ".repeat(level) + "</StateSet>");
    out.println("    ".repeat(level) + "<InitialStateSet>");
    out.println(MessageFormat.format("{0}<StateID>{1}</StateID>", "    ".repeat(level + 1), result.startNodeId));
    out.println("    ".repeat(level) + "</InitialStateSet>");
  }

  @Override
  protected void writeEdgesPre() {
    out.println("    ".repeat(level) + "<TransitionSet complete=\"false\">");
    level++;
  }

  @Override
  protected void writeEdgesPost() {
    level--;
    out.println("    ".repeat(level) + "</TransitionSet>");
  }

  @Override
  protected String nodeToString(int id, BProgramSyncSnapshot bpss) {
    StringBuilder out = new StringBuilder();
    int hash = bpss.hashCode();
    String store = !printStore ? "" : getStore(bpss);
    String statements = !printStatements ? "" : getStatments(bpss);
    out.append(MessageFormat.format("{0}<State sid=\"{1}\">\n", "    ".repeat(level), id));
    out.append(MessageFormat.format("{0}<Description>Hash={1}{2}{3}</Description>\n", "    ".repeat(level+1), hash, store, statements));
    out.append("    ".repeat(level + 1)).append("<Properties/>\n");
    out.append("    ".repeat(level)).append("</State>");
    return out.toString();
  }

  protected String getStore(BProgramSyncSnapshot bpss) {
    return bpss.getDataStore().entrySet().stream()
        .map(entry -> "{" + sanitize(ScriptableUtils.stringify(entry.getKey())) + "," + sanitize(ScriptableUtils.stringify(entry.getValue())) + "}")
        .collect(joining(",", "; Store: [", "]"));
  }

  protected String getStatments(BProgramSyncSnapshot bpss) {
    return bpss.getBThreadSnapshots().stream()
        .map(btss -> {
          SyncStatement syst = btss.getSyncStatement();
          return
              "{name: " + sanitize(btss.getName()) + ", " +
                  "isHot: " + syst.isHot() + ", " +
                  "request: " + syst.getRequest().stream().map(e -> sanitize(eventToString(e))).collect(joining(",", "[", "]")) + ", " +
                  "waitFor: " + sanitize(syst.getWaitFor()) + ", " +
                  "block: " + sanitize(syst.getBlock()) + ", " +
                  "interrupt: " + sanitize(syst.getInterrupt()) + "}";
        })
        .collect(joining(", ", "; Statements: [", "]"));
  }

  @Override
  protected String edgeToString(GenerateAllTracesInspection.Edge edge) {
    StringBuilder out = new StringBuilder();
    out.append(MessageFormat.format("{0}<Transition tid=\"{1}\">\n", "    ".repeat(level), edgeCounter.getAndIncrement()));
    out.append(MessageFormat.format("{0}<From>{1}</From>\n", "    ".repeat(level+1), edge.srcId));
    out.append(MessageFormat.format("{0}<To>{1}</To>\n", "    ".repeat(level+1), edge.dstId));
    out.append(MessageFormat.format("{0}<Label>{1}</Label>\n", "    ".repeat(level+1), sanitize(eventToString(edge.event))));
    out.append("    ".repeat(level + 1)).append("<Properties/>\n");
    out.append("    ".repeat(level)).append("</Transition>");
    return out.toString();
  }

  @Override
  protected void writePost() {
    out.println("    ".repeat(level) + "<Acc type=\"Classic\">");
    result.endStates.forEach(s->out.println(
        MessageFormat.format("{0}<StateID>{1}</StateID>", "    ".repeat(level+1), s.getLeft())));
    out.println("    ".repeat(level) + "</Acc>");
    level--;
    out.println("</Structure>");
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
