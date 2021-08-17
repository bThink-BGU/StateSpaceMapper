package il.ac.bgu.cs.bp.statespacemapper.exports;

import il.ac.bgu.cs.bp.bpjs.internal.ScriptableUtils;
import il.ac.bgu.cs.bp.bpjs.model.BEvent;
import il.ac.bgu.cs.bp.bpjs.model.BProgramSyncSnapshot;
import il.ac.bgu.cs.bp.bpjs.model.BThreadSyncSnapshot;
import il.ac.bgu.cs.bp.bpjs.model.SyncStatement;
import il.ac.bgu.cs.bp.statespacemapper.GenerateAllTracesInspection;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.stream.Collectors.joining;

public abstract class Exporter {
  protected final GenerateAllTracesInspection.MapperResult res;
  protected final String path;
  protected final String runName;

  protected Exporter(GenerateAllTracesInspection.MapperResult res, String path, String runName) {
    this.res = res;
    this.path = path;
    this.runName = runName;
  }

  protected abstract void exportGraph(PrintStream out);

  public void export() throws IOException {
    Files.createDirectories(Paths.get(path).getParent());
    try (var out = new PrintStream(path)) {
      exportGraph(out);
    }
  }

  protected Function<GenerateAllTracesInspection.MapperVertex, Map<String, Attribute>> vertexAttributeProvider() {
    return v -> {
      boolean startNode = v.equals(res.startNode);
      boolean acceptingNode = res.acceptingStates.contains(v);
      return Map.of(
          "hash", DefaultAttribute.createAttribute(v.hashCode()),
          "store", DefaultAttribute.createAttribute(sanitize(getStore(v.bpss))),
          "statements", DefaultAttribute.createAttribute(sanitize(getStatments(v.bpss))),
          "bthreads", DefaultAttribute.createAttribute(sanitize(getBThreads(v.bpss))),
          "shape", DefaultAttribute.createAttribute(startNode ? "none " : acceptingNode ? "doublecircle" : "circle")
      );
    };
  }

  protected Supplier<Map<String, Attribute>> graphAttributeProvider() {
    return () -> Map.of(
        "name", DefaultAttribute.createAttribute("\"" + sanitize(runName) + "\""),
        "run_date", DefaultAttribute.createAttribute("\"" + DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.now()) + "\""),
        "num_of_vertices", DefaultAttribute.createAttribute(res.states().size()),
        "num_of_edges", DefaultAttribute.createAttribute(res.edges().size()),
        "num_of_events", DefaultAttribute.createAttribute(res.events.size())
    );
  }

  protected Function<GenerateAllTracesInspection.MapperEdge, Map<String, Attribute>> edgeAttributeProvider() {
    return e -> Map.of(
        "label", DefaultAttribute.createAttribute(sanitize(e.event.toString())),
        "Event", DefaultAttribute.createAttribute(sanitize(e.event.toString())),
        "Event_name", DefaultAttribute.createAttribute(sanitize(e.event.name)),
        "Event_value", DefaultAttribute.createAttribute(sanitize(Objects.toString(e.event.maybeData)))
    );
  }


  protected String getBThreads(BProgramSyncSnapshot bpss) {
    return bpss.getBThreadSnapshots().stream().map(BThreadSyncSnapshot::getName).collect(joining(","));
  }

  protected String sanitize(String in) {
    return in
        .replace("\r\n", "")
        .replace("\n", "")
        .replace("\"", "'")
        .replace("JS_Obj ", "");
  }

  protected String getStore(BProgramSyncSnapshot bpss) {
    return bpss.getDataStore().entrySet().stream()
        .map(entry -> "{" + ScriptableUtils.stringify(entry.getKey()) + "," + ScriptableUtils.stringify(entry.getValue()) + "}")
        .collect(joining(",", "[", "]"));
  }

  protected String getStatments(BProgramSyncSnapshot bpss) {
    return bpss.getBThreadSnapshots().stream()
        .map(btss -> {
          SyncStatement syst = btss.getSyncStatement();
          return
              "{name: " + btss.getName() + ", " +
                  "isHot: " + syst.isHot() + ", " +
                  "request: " + syst.getRequest().stream().map(BEvent::toString).collect(joining(",", "[", "]")) + ", " +
                  "waitFor: " + syst.getWaitFor().toString() + ", " +
                  "block: " + syst.getBlock().toString() + ", " +
                  "interrupt: " + syst.getInterrupt().toString() + "}";
        })
        .collect(joining(",\n", "[", "]"));
  }
}
