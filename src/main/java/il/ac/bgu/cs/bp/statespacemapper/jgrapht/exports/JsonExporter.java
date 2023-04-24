package il.ac.bgu.cs.bp.statespacemapper.jgrapht.exports;

import il.ac.bgu.cs.bp.statespacemapper.MapperResult;
import org.jgrapht.nio.json.JSONExporter;

public class JsonExporter extends Exporter {
  public JsonExporter(MapperResult res) {
    super("JSON", ".json", res, new JSONExporter<>());
  }
}
