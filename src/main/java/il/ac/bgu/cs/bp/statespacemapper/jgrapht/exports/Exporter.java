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
  public final String name;
  public final String fileType;
  protected final MapperResult res;
  protected String programName = null;
  protected final BaseExporter<MapperVertex, MapperEdge> exporter;
  private Function<MapperVertex, Map<String, Attribute>> vertexAttributeProvider;
  private Function<MapperEdge, Map<String, Attribute>> edgeAttributeProvider;
  private Supplier<Map<String, Attribute>> graphAttributeProvider;
  private Function<String, String> sanitizerProvider;
  private boolean verbose = false;

  public Exporter(String name, String fileType, MapperResult res,
                  BaseExporter<MapperVertex, MapperEdge> exporter) {
    this.name = name;
    this.fileType = fileType;
    this.res = res;
    this.exporter = exporter;
    this.vertexAttributeProvider = vertexAttributeProvider();
    this.edgeAttributeProvider = edgeAttributeProvider();
    this.graphAttributeProvider = graphAttributeProvider();
    this.sanitizerProvider = sanitizerProvider();
    this.exporter.setVertexIdProvider(v -> String.valueOf(res.vertexMapper.get(v)));
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

  public void export(String path, String programName) throws IOException {
    this.programName = programName;
    exporter.setEdgeAttributeProvider(this.edgeAttributeProvider);
    exporter.setVertexAttributeProvider(this.vertexAttributeProvider);
    exporter.setGraphAttributeProvider(this.graphAttributeProvider);
    Files.createDirectories(Paths.get(path).getParent());
    try (var out = new PrintStream(path)) {
      ((GraphExporter<MapperVertex, MapperEdge>) exporter).exportGraph(res.graph, out);
    }
  }

  protected Function<MapperVertex, Map<String, Attribute>> vertexAttributeProvider() {
    if (verbose) {
      return v -> new HashMap<>(Map.of(
        "id", DefaultAttribute.createAttribute(res.vertexMapper.get(v)),
        "hash", DefaultAttribute.createAttribute(v.hashCode()),
        "store", DefaultAttribute.createAttribute(sanitizerProvider.apply(getStore(v.bpss))),
        "statements", DefaultAttribute.createAttribute(sanitizerProvider.apply(getStatements(v.bpss))),
        "bthreads", DefaultAttribute.createAttribute(sanitizerProvider.apply(getBThreads(v.bpss))),
        "start", DefaultAttribute.createAttribute(v.startVertex),
        "accepting", DefaultAttribute.createAttribute(v.accepting)
      ));
    } else {
      return v -> new HashMap<>(Map.of(
        "id", DefaultAttribute.createAttribute(res.vertexMapper.get(v))
      ));
    }
  }

  protected Supplier<Map<String, Attribute>> graphAttributeProvider() {
    return () -> {
      var map = new HashMap<>(Map.of(
        "run_date", DefaultAttribute.createAttribute("\"" + DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.now()) + "\""),
        "num_of_vertices", DefaultAttribute.createAttribute(res.states().size()),
        "num_of_edges", DefaultAttribute.createAttribute(res.edges().size()),
        "num_of_events", DefaultAttribute.createAttribute(res.events.size())));
      if (programName != null)
        map.put("name", DefaultAttribute.createAttribute("\"" + sanitizerProvider.apply(programName) + "\""));
      return map;
    };
  }

  protected Function<MapperEdge, Map<String, Attribute>> edgeAttributeProvider() {
    if (verbose) {
      return e -> new HashMap<>(Map.of(
        "label", DefaultAttribute.createAttribute(sanitizerProvider.apply(eventToString(e.event))),
        "Event", DefaultAttribute.createAttribute(sanitizerProvider.apply(e.event.toString())),
        "Event_name", DefaultAttribute.createAttribute(sanitizerProvider.apply(e.event.name)),
        "Event_value", DefaultAttribute.createAttribute(sanitizerProvider.apply(Objects.toString(e.event.maybeData)))
      ));
    } else {
      return e -> new HashMap<>(Map.of(
        "label", DefaultAttribute.createAttribute(sanitizerProvider.apply(eventToString(e.event)))
      ));
    }
  }

  public String eventToString(BEvent event) {
    return event.name + event.getDataField().map(v -> " (" + ScriptableUtils.stringify(event.maybeData)).orElse("");
  }


  public String getBThreads(BProgramSyncSnapshot bpss) {
    return bpss.getBThreadSnapshots().stream().map(BThreadSyncSnapshot::getName).collect(joining(","));
  }

  protected Function<String, String> sanitizerProvider() {
    return in -> in
      .replace("\r\n", "")
      .replace("\n", "")
      .replace("\"", "'")
      .replace("JS_Obj ", "");
  }

  public String getStore(BProgramSyncSnapshot bpss) {
    return bpss.getDataStore().entrySet().stream()
      .map(entry -> "{" + ScriptableUtils.stringify(entry.getKey()) + "," + ScriptableUtils.stringify(entry.getValue()) + "}")
      .collect(joining(",", "[", "]"));
  }

  public String getStatements(BProgramSyncSnapshot bpss) {
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

  /**
   * Set verbose mode. In verbose mode, the exporter will add more information to the exported graph.
   * Specifically, it will add the following attributes to the vertices: hash, store, statements, bthreads, start, accepting
   * and the following attributes to the edges: Event, Event_name, Event_value
   */
  public void setVerbose(boolean verbose) {
    this.verbose = verbose;
  }
}
