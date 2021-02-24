package il.ac.bgu.cs.bp.statespacemapper.writers;

import il.ac.bgu.cs.bp.bpjs.internal.ScriptableUtils;
import il.ac.bgu.cs.bp.bpjs.model.BEvent;
import il.ac.bgu.cs.bp.bpjs.model.BProgramSyncSnapshot;
import il.ac.bgu.cs.bp.statespacemapper.GenerateAllTracesInspection;

import java.io.PrintStream;
import java.util.Comparator;
import java.util.Map;

import static java.util.stream.Collectors.joining;

public abstract class TraceResultWriter {
  protected final PrintStream out;
  protected final GenerateAllTracesInspection.MapperResult result;
  protected final String name;
  protected boolean printStatements = false;
  protected boolean printStore = false;
  private final String nodesDelimiter;
  private final String edgesDelimiter;

  protected TraceResultWriter(PrintStream out, GenerateAllTracesInspection.MapperResult result, String name, String nodesDelimiter, String edgesDelimiter) {
    this.out = out;
    this.result = result;
    this.name = name;
    this.nodesDelimiter = nodesDelimiter;
    this.edgesDelimiter = edgesDelimiter;
  }

  protected TraceResultWriter(PrintStream out, GenerateAllTracesInspection.MapperResult result, String name) {
    this(out, result, name, "\n", "\n");
  }

  public void setPrintStatements(boolean printStatements) {
    this.printStatements = printStatements;
  }

  public void setPrintStore(boolean printStore) {
    this.printStore = printStore;
  }

  public final void write() {
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
    return e;
  }

  protected final String nodeToString(Map.Entry<BProgramSyncSnapshot, Integer> bpssEntry) {
    return nodeToString(bpssEntry.getValue(), bpssEntry.getKey());
  }
}
