package il.ac.bgu.cs.bp.statespacemapper.jgrapht.exports;

import il.ac.bgu.cs.bp.statespacemapper.GenerateAllTracesInspection;
import org.jgrapht.nio.dot.DOTExporter;

public class DotExporter extends Exporter {
  public DotExporter(GenerateAllTracesInspection.MapperResult res, String path, String runName) {
    super(res, path, runName, new DOTExporter<>());
  }
}
