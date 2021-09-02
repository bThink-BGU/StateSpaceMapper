package il.ac.bgu.cs.bp.statespacemapper.jgrapht.exports;

import il.ac.bgu.cs.bp.statespacemapper.GenerateAllTracesInspection;
import il.ac.bgu.cs.bp.statespacemapper.jgrapht.MapperVertex;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;
import org.jgrapht.nio.dot.DOTExporter;

import java.util.Map;
import java.util.function.Function;

public class DotExporter extends Exporter {
  public DotExporter(GenerateAllTracesInspection.MapperResult res, String path, String runName) {
    super(res, path, runName, new DOTExporter<>());
  }

  @Override
  protected Function<MapperVertex, Map<String, Attribute>> vertexAttributeProvider() {
    var provider = super.vertexAttributeProvider();
    return v -> {
      var map = provider.apply(v);
      var start = Boolean.getBoolean(map.get("start").getValue());
      var accepting = Boolean.getBoolean(map.get("accepting").getValue());
      map.put("shape", DefaultAttribute.createAttribute(start ? "none " : accepting ? "doublecircle" : "circle"));
      return map;
    };
  }
}
