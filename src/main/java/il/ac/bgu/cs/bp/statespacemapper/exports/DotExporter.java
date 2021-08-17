package il.ac.bgu.cs.bp.statespacemapper.exports;

import il.ac.bgu.cs.bp.statespacemapper.GenerateAllTracesInspection;
import org.jgrapht.nio.dot.DOTExporter;

import java.io.PrintStream;

public class DotExporter extends Exporter {
  public DotExporter(GenerateAllTracesInspection.MapperResult res, String path, String runName) {
    super(res, path, runName);
  }

  @Override
  protected void exportGraph(PrintStream out) {
    var dotExporter = new DOTExporter<GenerateAllTracesInspection.MapperVertex, GenerateAllTracesInspection.MapperEdge>();
    dotExporter.setEdgeAttributeProvider(edgeAttributeProvider());
    dotExporter.setVertexAttributeProvider(vertexAttributeProvider());
    dotExporter.setGraphAttributeProvider(graphAttributeProvider());
    dotExporter.exportGraph(res.graph, out);
  }
}
