package il.ac.bgu.cs.bp.statespacemapper;

import il.ac.bgu.cs.bp.bpjs.internal.ScriptableUtils;
import il.ac.bgu.cs.bp.bpjs.model.BEvent;
import il.ac.bgu.cs.bp.bpjs.model.BProgramSyncSnapshot;

import java.io.PrintStream;
import java.util.Map;

public abstract class TraceResultWriter {
  protected final PrintStream out;
  protected final GenerateAllTracesInspection.MapperResult result;
  protected final String name;
  protected boolean printStatements = false;
  protected boolean printStore = false;

  protected TraceResultWriter(PrintStream out, GenerateAllTracesInspection.MapperResult result, String name) {
    this.out = out;
    this.result = result;
    this.name = name;
  }

  public void setPrintStatements(boolean printStatements) {
    this.printStatements = printStatements;
  }

  public void setPrintStore(boolean printStore) {
    this.printStore = printStore;
  }

  protected abstract void innerWrite();

  public final void write() {
    innerWrite();
    out.flush();
  }

  protected String nodeName(BProgramSyncSnapshot node) {
    return result.states.get(node).toString();
  }

  protected String linkName(BEvent event) {
    String e = event.name;
    if (event.maybeData != null)
      e += ": " + ScriptableUtils.stringify(event.maybeData);
    return e;
  }

  protected abstract String printLink(GenerateAllTracesInspection.Link link);

  protected abstract String printBpss(Map.Entry<BProgramSyncSnapshot, Integer> bpssEntry);
}
