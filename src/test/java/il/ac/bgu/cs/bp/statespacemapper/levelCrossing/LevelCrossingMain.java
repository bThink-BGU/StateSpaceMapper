package il.ac.bgu.cs.bp.statespacemapper.levelCrossing;

import il.ac.bgu.cs.bp.bpjs.model.BEvent;
import il.ac.bgu.cs.bp.bpjs.model.BProgram;
import il.ac.bgu.cs.bp.bpjs.model.ResourceBProgram;
import il.ac.bgu.cs.bp.statespacemapper.MapperResult;
import il.ac.bgu.cs.bp.statespacemapper.StateSpaceMapper;
import il.ac.bgu.cs.bp.statespacemapper.jgrapht.MapperEdge;
import il.ac.bgu.cs.bp.statespacemapper.jgrapht.MapperVertex;
import il.ac.bgu.cs.bp.statespacemapper.jgrapht.exports.DotExporter;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.graph.DirectedPseudograph;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;
import org.jgrapht.nio.dot.DOTImporter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class LevelCrossingMain {
  private static Logger logger = Logger.getLogger(LevelCrossingMain.class.getName());


  // Program arguments =
  //    args[0] = lc_bp | lc_pn | lc_bp_faults | lc_pn_faults
  //    args[1] = number of railways
  //    args[2] (optional) = max path length
  // For example: args = ["lc_pn", "3", "14"]

  // To start from a ready dot file and only generate the paths:
  // 1) the first parameter must be the path to the dot file
  // 2) the rest of the parameters are the same as before, only shifted one to the right. For example:
  //    args[0] = path to dot file
  //    args[1] = lc_bp | lc_pn | lc_bp_faults | lc_pn_faults
  //    args[2] = number of railways
  //    args[3] (optional) = max path length
  // For example: args = ["exports/lc_bp_R-3.dot", "lc_pn", "3", "14"]
  public static void main(String[] args) throws Exception {
    setupLogger();
    logger.info("Args: " + Arrays.toString(args)+"\n");
    String dotFile = null;
    if (args[0].contains(".dot")) {
      dotFile = args[0];
      args = Arrays.copyOfRange(args, 1, args.length);
    }
    var railways = Integer.parseInt(args[1]);
    var filename = "levelCrossing/" + args[0] + ".js";
    var runName = args[0] + "_R-" + railways;
    var csvName = runName + ".csv";
    var outputDir = "exports";
    Integer maxPathLength = null;
    if (args.length == 3) {
      maxPathLength = Integer.valueOf(args[2]);
      csvName = runName + "_L-" + maxPathLength + ".csv";
    }

    printJVMStats();

    MapperResult res;
    if (dotFile == null) {
      res = mapSpace(railways, filename);
      exportGraph(outputDir, runName, res);

      if (runName.startsWith("lc_pn")) {
        res = PNMapperResults.removeHelperEvents(res);
        exportGraph(outputDir, runName + "_compressed", res);
      }
    } else {
      res = importStateSpace(dotFile);
    }
    generatePaths(csvName, maxPathLength, res, outputDir);

    logger.info("// done");

    System.exit(0); // To complete the garbage collection before terminating the program. Solves Maven exceptions.
  }

  private static void setupLogger() {
    logger.setUseParentHandlers(false);
    ConsoleHandler handler = new ConsoleHandler();
    handler.setFormatter(new SimpleFormatter() {
      private static final String format = "[%1$tF %1$tT] [%2$-7s] %3$s %n";

      @Override
      public synchronized String format(LogRecord lr) {
        return String.format(format,
            new Date(lr.getMillis()),
            lr.getLevel().getLocalizedName(),
            lr.getMessage()
        );
      }
    });
    logger.addHandler(handler);
  }

  private static MapperResult mapSpace(int railways, String filename) throws Exception {
    final BProgram bprog = new ResourceBProgram(filename);
    bprog.putInGlobalScope("n", railways);
    logger.info("// Start mapping the states graph");
    MapperResult res = new StateSpaceMapper(bprog).mapSpace();
    logger.info("// Completed mapping the states graph");
    logger.info(res.toString());
    logger.info("-------------\n");
    return res;
  }

  private static MapperResult importStateSpace(String dotFile) {
    logger.info("// Importing the states graph");
    var graph = new DirectedPseudograph<MapperVertex, MapperEdge>(MapperEdge.class);
    var importer = new DOTImporter<MapperVertex, MapperEdge>();
    importer.setVertexWithAttributesFactory(MapperVertexExtended::new);
    importer.setEdgeWithAttributesFactory(MapperEdgeExtended::new);
    importer.importGraph(graph, new File(dotFile));
//    var startVertex = graph.vertexSet().stream().filter(v -> ((MapperVertexExtended)v).start).findFirst().get();
//    var acceptingVertices = graph.vertexSet().stream().filter(v -> ((MapperVertexExtended)v).accepting).collect(Collectors.toSet());
    return new PNMapperResults(graph);
  }

  private static void generatePaths(String csvName, Integer maxPathLength, MapperResult res, String outputDir) throws IOException {
    logger.info("// Generating paths...");
    var allDirectedPathsAlgorithm = res.createAllDirectedPathsBuilder()
        .setSimplePathsOnly(maxPathLength == null)
        .setIncludeReturningEdgesInSimplePaths(maxPathLength == null)
        .setLongestPathsOnly(false)
        .setMaxPathLength(maxPathLength)
        .build();
    var graphPaths = allDirectedPathsAlgorithm.getAllPaths();

    int maxLength = graphPaths.parallelStream().map(GraphPath::getLength).max(Integer::compareTo).orElse(0);
    logger.info("// Number of paths = " + graphPaths.size());
    logger.info("// Max path length = " + maxLength);

    logger.info("// Writing paths...");
    try (var fos = new FileOutputStream(Paths.get(outputDir, csvName) + ".zip");
         var zipOut = new ZipOutputStream(fos)) {
      var zipEntry = new ZipEntry(csvName);
      zipOut.putNextEntry(zipEntry);
      zipOut.setLevel(9);
      MapperResult.GraphPaths2BEventPaths(graphPaths)
          .parallelStream()
          .map(l -> l.stream()
              .map(BEvent::getName)
//            .filter(s -> !List.of("KeepDown", "ClosingRequest", "OpeningRequest").contains(s))
              .collect(Collectors.joining(",", "", "\n")))
          .distinct()
          .sorted()
          .forEachOrdered(s -> {
            try {
              zipOut.write(s.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
              e.printStackTrace();
            }
          });
    }
  }

  private static void exportGraph(String outputDir, String runName, MapperResult res) throws IOException {
    logger.info("// Export to GraphViz...");
    var path = Paths.get(outputDir, runName + ".dot").toString();
    var exporter = new DotExporter(res);
    var vertexProvider = exporter.getVertexAttributeProvider();
    exporter.setVertexAttributeProvider(v -> {
      var map = vertexProvider.apply(v);
        map.remove("store");
        map.remove("statements");
        map.remove("bthreads");
        return map;
      });
    exporter.setEdgeAttributeProvider(v -> Map.of(
        "label", DefaultAttribute.createAttribute(v.event.name)
    ));
    exporter.export(path, runName);
  }

  private static void printJVMStats() {
    logger.info("-------------");
    logger.info("Available processors (cores): " +
        Runtime.getRuntime().availableProcessors());

    /* Total amount of free memory available to the JVM */
    logger.info("Free memory (bytes): " +
        Runtime.getRuntime().freeMemory());

    /* This will return Long.MAX_VALUE if there is no preset limit */
    long maxMemory = Runtime.getRuntime().maxMemory();
    /* Maximum amount of memory the JVM will attempt to use */
    logger.info("Maximum memory (bytes): " +
        (maxMemory == Long.MAX_VALUE ? "no limit" : maxMemory));

    /* Total memory currently in use by the JVM */
    logger.info("Total memory (bytes): " +
        Runtime.getRuntime().totalMemory());
    logger.info("-------------\n");
  }

  private static class PNMapperResults extends MapperResult {
    private PNMapperResults(Graph<MapperVertex, MapperEdge> graph) {
      super(graph);
    }

    public static PNMapperResults removeHelperEvents(MapperResult base) {
      logger.info("// Compressing the PN graph");

      var startNode = base.startVertex();
      var graph = base.graph;
//      var i = 0;
      while (true) {
        var edge = graph.edgeSet().parallelStream()
            .filter(e -> List.of(KeepDown.NAME, ClosingRequest.NAME, OpeningRequest.NAME).contains(e.event.name))
            .findAny().orElse(null);
        if (edge == null) break;
//        logger.info("Removing " + edge.event.name);
        var source = graph.getEdgeSource(edge);
        var target = graph.getEdgeTarget(edge);
        var targetOut = new ArrayList<>(graph.outgoingEdgesOf(target));
        for (var e : targetOut) {
          var eTarget = graph.getEdgeTarget(e);
          if (graph.outgoingEdgesOf(source).parallelStream().map(e1 -> e1.event.name).noneMatch(e1 -> e1.equals(e.event.name)))
            graph.addEdge(source, eTarget, new MapperEdge(e.event));
        }
        graph.removeEdge(edge);
//        graph.removeVertex(source); // this line make the graph look like the one in the paper, however it is not correct to do so in all cases...
//        exportGraph("exports", "log" + i, new PNMapperResults(graph, startNode, graph.vertexSet()));
//        i++;
      }
      while (true) {
        var zeroInDegree = graph.vertexSet().stream().filter(v -> !v.equals(startNode) && graph.inDegreeOf(v) == 0).collect(Collectors.toList());
        if (zeroInDegree.isEmpty()) break;
        graph.removeAllVertices(zeroInDegree);
      }
      var res = new PNMapperResults(graph);
      logger.info(res.toString());
      return res;
    }
  }

  static class MapperVertexExtended extends MapperVertex {
    private final int id;
    public final boolean start;
    public final boolean accepting;

    public MapperVertexExtended(String s, Map<String, Attribute> stringAttributeMap) {
      super(null);
      id = Integer.parseInt(s);
      if(stringAttributeMap.containsKey("start")) {
        start = Boolean.parseBoolean(stringAttributeMap.get("start").getValue());
        accepting = Boolean.parseBoolean(stringAttributeMap.get("accepting").getValue());
      } else {
        start = s.equals("1");
        accepting = true;
      }
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof MapperVertexExtended)) return false;
      return id == ((MapperVertexExtended) o).id;
    }

    @Override
    public int hashCode() {
      return id;
    }
  }

  static class MapperEdgeExtended extends MapperEdge {
    private static BEvent getEvent(String name) {
      switch (name.charAt(0)) {
        case 'A':
          if (name.length() == 2)
            return new Approaching(Integer.parseInt(name.substring(1)));
          else
            return new Approaching();
        case 'C':
          if (name.length() == 3)
            return new ClosingRequest(Integer.parseInt(name.substring(2)));
          else
            return new ClosingRequest();
        case 'E':
          if (name.length() == 2)
            return new Entering(Integer.parseInt(name.substring(1)));
          else
            return new Entering();
        case 'F':
          if (name.charAt(1) == 'R') {
            return new FaultRaise();
          } else {
            if (name.length() == 3)
              return new FaultEntering(Integer.parseInt(name.substring(2)));
            else
              return new FaultEntering();
          }
        case 'K':
          if (name.length() == 3)
            return new KeepDown(Integer.parseInt(name.substring(2)));
          else
            return new KeepDown();
        case 'L':
          if (name.charAt(1) == 'e') {
            if (name.length() == 3) {
              return new Leaving(Integer.parseInt(name.substring(2)));
            } else {
              return new Leaving();
            }
          } else {
            return new Lower();
          }
        case 'O':
          if (name.length() == 3)
            return new OpeningRequest(Integer.parseInt(name.substring(2)));
          else
            return new OpeningRequest();
        case 'R':
          return new Raise();
        default:
          throw new IllegalArgumentException();
      }
    }

    public MapperEdgeExtended(Map<String, Attribute> attributes) {
      super(getEvent(attributes.get("label").getValue()));
    }
  }
}