package il.ac.bgu.cs.bp.statespacemapper;

import il.ac.bgu.cs.bp.bpjs.internal.ScriptableUtils;
import il.ac.bgu.cs.bp.bpjs.model.BEvent;
import il.ac.bgu.cs.bp.bpjs.model.BProgramSyncSnapshot;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class TraceResultWriter {
  protected final PrintStream out;
  protected final GenerateAllTracesInspection.MapperResult result;
  protected final String name;

  protected TraceResultWriter(PrintStream out, GenerateAllTracesInspection.MapperResult result, String name) {
    this.out = out;
    this.result = result;
    this.name = name;
  }
  public abstract void write();

  protected String nodeName(BProgramSyncSnapshot node) {
    return Integer.toHexString(node.hashCode());
  }

  protected String linkName(BEvent event) {
    String e = event.name;
    if (event.maybeData != null)
      e += ": " + ScriptableUtils.stringify(event.maybeData);
    return e;
  }

  protected abstract String printLink(GenerateAllTracesInspection.Link link);

  protected abstract String printBpss(BProgramSyncSnapshot bpss);




}
