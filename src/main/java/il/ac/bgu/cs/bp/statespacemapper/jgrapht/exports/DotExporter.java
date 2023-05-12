package il.ac.bgu.cs.bp.statespacemapper.jgrapht.exports;

import il.ac.bgu.cs.bp.statespacemapper.MapperResult;
import il.ac.bgu.cs.bp.statespacemapper.jgrapht.MapperVertex;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;
import org.jgrapht.nio.dot.DOTExporter;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public class DotExporter extends Exporter {
  public DotExporter(MapperResult res) {
    super("GraphViz", ".dot", res, new DOTExporter<>());
  }

  @Override
  protected Function<MapperVertex, Map<String, Attribute>> vertexAttributeProvider() {
    return v -> {
      var provider = super.vertexAttributeProvider();
      var map = provider.apply(v);
      if(v.startVertex) {
        map.put("color", DefaultAttribute.createAttribute("#338866"));
        map.put("shape", DefaultAttribute.createAttribute("circle"));
      } else if(v.accepting) {
        map.put("shape", DefaultAttribute.createAttribute("doublecircle"));
      } else {
        map.put("shape", DefaultAttribute.createAttribute("point"));
      }
      return map;
    };
  }

  @Override
  protected Supplier<Map<String, Attribute>> graphAttributeProvider() {
    var provider = super.graphAttributeProvider();

    return () -> {
      var map = provider.get();
      map.put("fontname", DefaultAttribute.createAttribute("Courier"));
      map.put("color", DefaultAttribute.createAttribute("\"#000000\""));
      return map;
    };
  }
}
