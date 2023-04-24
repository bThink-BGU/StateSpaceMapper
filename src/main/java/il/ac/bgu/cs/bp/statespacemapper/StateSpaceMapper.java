package il.ac.bgu.cs.bp.statespacemapper;

import il.ac.bgu.cs.bp.bpjs.analysis.DfsForStateMapper;
import il.ac.bgu.cs.bp.bpjs.analysis.listeners.PrintDfsVerifierListener;
import il.ac.bgu.cs.bp.bpjs.model.BEvent;
import il.ac.bgu.cs.bp.bpjs.model.BProgram;
import il.ac.bgu.cs.bp.statespacemapper.jgrapht.AllDirectedPaths;
import il.ac.bgu.cs.bp.statespacemapper.jgrapht.AllDirectedPathsBuilder;
import il.ac.bgu.cs.bp.statespacemapper.jgrapht.MapperEdge;
import il.ac.bgu.cs.bp.statespacemapper.jgrapht.MapperVertex;
import il.ac.bgu.cs.bp.statespacemapper.jgrapht.exports.DotExporter;
import il.ac.bgu.cs.bp.statespacemapper.jgrapht.exports.Exporter;
import il.ac.bgu.cs.bp.statespacemapper.jgrapht.exports.GoalExporter;
import il.ac.bgu.cs.bp.statespacemapper.jgrapht.exports.JsonExporter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jgrapht.GraphPath;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class StateSpaceMapper {
  private static final Logger logger = LogManager.getLogger(StateSpaceMapper.class);

  private int maxTraceLength = Integer.MAX_VALUE;
  private int iterationCountGap = 1000;
  private final BProgram bprog;
  private MapperResult mpr = null;
  public final String programName;
  private String exportDirectory;

  public StateSpaceMapper(BProgram bprog) {
    this(bprog, bprog.getName());
  }

  public StateSpaceMapper(BProgram bprog, String programName) {
    this(bprog, programName, "exports");
  }

  public StateSpaceMapper(BProgram bprog, String programName, String exportDirectory) {
    this.bprog = bprog;
    this.programName = programName;
    this.exportDirectory = exportDirectory;
  }

  public void setMaxTraceLength(int limit) {
    maxTraceLength = limit;
  }

  public void setIterationCountGap(int iterationCountGap) {
    this.iterationCountGap = iterationCountGap;
  }

  public MapperResult mapSpace() throws Exception {
    logger.info("// Space mapping started");
    initGlobalScope();
    var tracesInspection = new GenerateAllTracesInspection();
    var vfr = new DfsForStateMapper();
    vfr.addInspection(tracesInspection);
    vfr.setMaxTraceLength(maxTraceLength);
    vfr.setIterationCountGap(iterationCountGap);
    vfr.setProgressListener(new PrintDfsVerifierListener());
    vfr.verify(bprog);
    logger.info("// Space mapping completed");
    mpr = tracesInspection.getResult();
    return mpr;
  }

  protected void initGlobalScope() {
    bprog.putInGlobalScope("use_accepting_states", true);
    bprog.putInGlobalScope("AcceptingState", new AcceptingStateProxy());
  }

  /**
   * Set the attributes providers for the graph {@link Exporter}.
   * The function can be use the set the verbosity level of the exporter (see {@link Exporter#setVerbose(boolean)}).
   * <p>
   * Another option is to explicitly call one or more of the following methods:
   * {@link Exporter#setVertexAttributeProvider(Function)},
   * {@link Exporter#setEdgeAttributeProvider(Function)}, or
   * {@link Exporter#setGraphAttributeProvider(Supplier)}
   * </p>
   *
   * @param exporter The exporter to change
   */
  protected void setExporterProviders(final Exporter exporter) {
  }

  public void exportSpace(Exporter... exporters) throws IOException {
    logger.info("// Exporting space to: " + Paths.get(exportDirectory).toAbsolutePath());
    for (int i = 0; i < exporters.length; i++) {
      var exporter = exporters[i];
      logger.info("// Space exporting to " + exporter.name + " started");
      var path = Paths.get(exportDirectory, programName + exporter.fileType).toString();
      setExporterProviders(exporter);
      exporter.export(path, programName);
      logger.info("// Space exporting to " + exporter.name + " completed");
    }
  }

  public void exportSpace() throws IOException {
    exportSpace(new DotExporter(mpr),
      new GoalExporter(mpr),
      new JsonExporter(mpr));
  }

  /**
   * Generate all paths and write them to a zip file containing a csv file with the paths.
   * The file path will be {@link #getExportDirectory()}/{@link #programName}.csv.zip.
   * <p>
   * <b>WARNING:</b> This action may take extremely long time and may generate extremely large files.
   * </p>
   *
   * @param pathsBuilder An {@link AllDirectedPathsBuilder} for {@link AllDirectedPaths}.
   */
  public void writeCompressedPaths(AllDirectedPathsBuilder<MapperVertex, MapperEdge> pathsBuilder) throws IOException {
    logger.info("// All paths generation started");
    var allDirectedPathsAlgorithm = pathsBuilder.build();
    var graphPaths = allDirectedPathsAlgorithm.getAllPaths();
    logger.info("// All paths generation completed");
    int maxLength = graphPaths.parallelStream().map(GraphPath::getLength).max(Integer::compareTo).orElse(0);

    logger.info("// Number of paths = " + graphPaths.size());
    logger.info("// Max path length = " + maxLength);

    var path = Paths.get(exportDirectory, programName + ".csv") + ".zip";
    logger.info("// Writing paths to " + path);
    try (var fos = new FileOutputStream(path);
         var zipOut = new ZipOutputStream(fos)) {
      var zipEntry = new ZipEntry(programName + ".csv");
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
    logger.info("// Completed writing paths");
  }

  /**
   * Write paths with a limit on path length, unless maxPathLength is null.
   * @see #writeCompressedPaths(AllDirectedPathsBuilder)
   *
   * @param maxPathLength the maximal path length. no limit if the parameter is null.
   * @throws IOException if file could not be created
   */
  public void writeCompressedPaths(Integer maxPathLength) throws IOException {
    writeCompressedPaths(
      mpr.createAllDirectedPathsBuilder()
        .setSimplePathsOnly(maxPathLength == null)
        .setIncludeReturningEdgesInSimplePaths(maxPathLength == null)
        .setLongestPathsOnly(false)
        .setMaxPathLength(maxPathLength)
    );
  }

  /**
   * Write paths with no limit on path length.
   * @see #writeCompressedPaths(AllDirectedPathsBuilder)
   *
   * @throws IOException if file could not be created
   */
  public void writeCompressedPaths() throws IOException {
    writeCompressedPaths((Integer) null);
  }

  public String getExportDirectory() {
    return exportDirectory;
  }

  public void setExportDirectory(String exportDirectory) {
    this.exportDirectory = exportDirectory;
  }
}
