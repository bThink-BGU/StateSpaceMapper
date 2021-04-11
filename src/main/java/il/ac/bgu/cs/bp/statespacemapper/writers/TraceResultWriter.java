package il.ac.bgu.cs.bp.statespacemapper.writers;

import il.ac.bgu.cs.bp.bpjs.internal.ScriptableUtils;
import il.ac.bgu.cs.bp.bpjs.model.BEvent;
import il.ac.bgu.cs.bp.bpjs.model.BProgramSyncSnapshot;
import il.ac.bgu.cs.bp.statespacemapper.GenerateAllTracesInspection;

import java.io.PrintStream;
import java.util.Map;

import static java.util.stream.Collectors.joining;

public abstract class TraceResultWriter {
  protected PrintStream out;
  protected GenerateAllTracesInspection.MapperResult result;
  protected final String name;
  public final String filetype;
  protected boolean printStatements = false;
  protected boolean printStore = false;
  private final String nodesDelimiter;
  private final String edgesDelimiter;

  protected TraceResultWriter(String name, String filetype, String nodesDelimiter, String edgesDelimiter) {
    this.name = name;
    this.filetype = filetype;
    this.nodesDelimiter = nodesDelimiter;
    this.edgesDelimiter = edgesDelimiter;
  }

  protected TraceResultWriter(String name, String filetype) {
    this(name, filetype, "\n", "\n");
  }

  public void setPrintStatements(boolean printStatements) {
    this.printStatements = printStatements;
  }

  public void setPrintStore(boolean printStore) {
    this.printStore = printStore;
  }

  public void write(PrintStream out, GenerateAllTracesInspection.MapperResult result) {
    if (out == null || result == null)
      throw new IllegalArgumentException("Result argument cannot be null");
    this.out = out;
    this.result = result;
    writePre();
    writeNodesPre();
    out.println(result.states.entrySet().stream()
        .sorted(Map.Entry.comparingByValue())
        .map(this::nodeToString)
        .collect(joining(nodesDelimiter)));
    writeNodesPost();
    writeEdgesPre();
    out.println(result.edges.stream().map(this::edgeToString)
        .collect(joining(edgesDelimiter)));
    writeEdgesPost();
    writePost();

    out.flush();
  }

  protected abstract void writePre();

  protected void writeNodesPre() {
  }

  protected abstract String nodeToString(int id, BProgramSyncSnapshot bpss);

  protected void writeNodesPost() {
  }

  protected void writeEdgesPre() {
  }

  protected abstract String edgeToString(GenerateAllTracesInspection.Edge edge);

  protected void writeEdgesPost() {
  }

  protected void writePost() {
  }

  protected String eventToString(BEvent event) {
    String e = event.name;
    if (event.maybeData != null)
      e += ": " + ScriptableUtils.stringify(event.maybeData);
    return sanitize(e);
  }

  protected final String nodeToString(Map.Entry<BProgramSyncSnapshot, Integer> bpssEntry) {
    return nodeToString(bpssEntry.getValue(), bpssEntry.getKey());
  }

  protected final String sanitize(Object in) {
    return sanitize(in.toString());
  }

  protected String sanitize(String in) {
    return in
        .replace(" ", "_")
        .replace("\n", "_")
        .replace("\"", "\\\"")
        .replace("JS_Obj ", "")
        .replaceAll("[\\. \\-+]", "_");
  }
}
