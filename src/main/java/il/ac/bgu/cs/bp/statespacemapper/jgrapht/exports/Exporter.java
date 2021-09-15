package il.ac.bgu.cs.bp.statespacemapper.jgrapht.exports;

import il.ac.bgu.cs.bp.bpjs.internal.ScriptableUtils;
import il.ac.bgu.cs.bp.bpjs.model.BEvent;
import il.ac.bgu.cs.bp.bpjs.model.BProgramSyncSnapshot;
import il.ac.bgu.cs.bp.bpjs.model.BThreadSyncSnapshot;
import il.ac.bgu.cs.bp.bpjs.model.SyncStatement;
import il.ac.bgu.cs.bp.statespacemapper.MapperResult;
import il.ac.bgu.cs.bp.statespacemapper.jgrapht.MapperEdge;
import il.ac.bgu.cs.bp.statespacemapper.jgrapht.MapperVertex;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.BaseExporter;
import org.jgrapht.nio.DefaultAttribute;
import org.jgrapht.nio.GraphExporter;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.stream.Collectors.joining;

public class Exporter {
  protected final MapperResult res;
  protected final String path;
  protected final String runName;
  protected final BaseExporter<MapperVertex, MapperEdge> exporter;
  private Function<MapperVertex, Map<String, Attribute>> vertexAttributeProvider;
  private Function<MapperEdge, Map<String, Attribute>> edgeAttributeProvider;
  private Supplier<Map<String, Attribute>> graphAttributeProvider;
  private Function<String, String> sanitizerProvider;

  public Exporter(MapperResult res, String path, String runName,
                  BaseExporter<MapperVertex, MapperEdge> exporter) {
    this.res = res;
    this.path = path;
    this.runName = runName;
    this.exporter = exporter;
    this.vertexAttributeProvider = vertexAttributeProvider();
    this.edgeAttributeProvider = edgeAttributeProvider();
    this.graphAttributeProvider = graphAttributeProvider();
    this.sanitizerProvider = sanitizerProvider();
  }

  public void setVertexAttributeProvider(Function<MapperVertex, Map<String, Attribute>> vertexAttributeProvider) {
    this.vertexAttributeProvider = vertexAttributeProvider;
  }

  public void setEdgeAttributeProvider(Function<MapperEdge, Map<String, Attribute>> edgeAttributeProvider) {
    this.edgeAttributeProvider = edgeAttributeProvider;
  }

  public void setGraphAttributeProvider(Supplier<Map<String, Attribute>> graphAttributeProvider) {
    this.graphAttributeProvider = graphAttributeProvider;
  }

  public void setSanitizerProvider(Function<String, String> sanitizerProvider) {
    this.sanitizerProvider = sanitizerProvider;
  }

  public Function<MapperVertex, Map<String, Attribute>> getVertexAttributeProvider() {
    return vertexAttributeProvider;
  }

  public Function<MapperEdge, Map<String, Attribute>> getEdgeAttributeProvider() {
    return edgeAttributeProvider;
  }

  public Supplier<Map<String, Attribute>> getGraphAttributeProvider() {
    return graphAttributeProvider;
  }

  public Function<String, String> getSanitizerProvider() {
    return sanitizerProvider;
  }

  public void export() throws IOException {
    exporter.setEdgeAttributeProvider(this.edgeAttributeProvider);
    exporter.setVertexAttributeProvider(this.vertexAttributeProvider);
    exporter.setGraphAttributeProvider(this.graphAttributeProvider);
    Files.createDirectories(Paths.get(path).getParent());
    try (var out = new PrintStream(path)) {
      ((GraphExporter<MapperVertex, MapperEdge>)exporter).exportGraph(res.graph, out);
    }
  }

  protected Function<MapperVertex, Map<String, Attribute>> vertexAttributeProvider() {
    return v -> {
      boolean startVertex = v.equals(res.startNode);
      boolean acceptingVertex = res.acceptingStates.contains(v);
      return new HashMap<>(Map.of(
          "hash", DefaultAttribute.createAttribute(v.hashCode()),
          "store", DefaultAttribute.createAttribute(sanitizerProvider.apply(getStore(v.bpss))),
          "statements", DefaultAttribute.createAttribute(sanitizerProvider.apply(getStatments(v.bpss))),
          "bthreads", DefaultAttribute.createAttribute(sanitizerProvider.apply(getBThreads(v.bpss))),
          "start", DefaultAttribute.createAttribute(startVertex),
          "accepting", DefaultAttribute.createAttribute(acceptingVertex)
      ));
    };
  }

  protected Supplier<Map<String, Attribute>> graphAttributeProvider() {
    return () -> new HashMap<>(Map.of(
        "name", DefaultAttribute.createAttribute("\"" + sanitizerProvider.apply(runName) + "\""),
        "run_date", DefaultAttribute.createAttribute("\"" + DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.now()) + "\""),
        "num_of_vertices", DefaultAttribute.createAttribute(res.states().size()),
        "num_of_edges", DefaultAttribute.createAttribute(res.edges().size()),
        "num_of_events", DefaultAttribute.createAttribute(res.events.size())
    ));
  }

  protected Function<MapperEdge, Map<String, Attribute>> edgeAttributeProvider() {
    return e -> new HashMap<>(Map.of(
        "label", DefaultAttribute.createAttribute(sanitizerProvider.apply(e.event.toString())),
        "Event", DefaultAttribute.createAttribute(sanitizerProvider.apply(e.event.toString())),
        "Event_name", DefaultAttribute.createAttribute(sanitizerProvider.apply(e.event.name)),
        "Event_value", DefaultAttribute.createAttribute(sanitizerProvider.apply(Objects.toString(e.event.maybeData)))
    ));
  }


  protected String getBThreads(BProgramSyncSnapshot bpss) {
    return bpss.getBThreadSnapshots().stream().map(BThreadSyncSnapshot::getName).collect(joining(","));
  }

  protected Function<String, String> sanitizerProvider() {
    return in -> in
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
