package il.ac.bgu.cs.bp.statespacemapper.jgrapht.exports;

import il.ac.bgu.cs.bp.statespacemapper.GenerateAllTracesInspection;
import org.jgrapht.nio.json.JSONExporter;

public class JsonExporter extends Exporter {
  public JsonExporter(GenerateAllTracesInspection.MapperResult res, String path, String runName) {
    super(res, path, runName, new JSONExporter<>());
  }
}
