package il.ac.bgu.cs.bp.statespacemapper.jgrapht.exports;

import il.ac.bgu.cs.bp.statespacemapper.MapperResult;
import il.ac.bgu.cs.bp.statespacemapper.jgrapht.MapperEdge;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;
import org.jgrapht.nio.dot.GOALExporter;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public class GoalExporter extends Exporter {
  public GoalExporter(MapperResult res) {
    this(res, false);
  }

  public GoalExporter(MapperResult res, boolean simplifyTransitions) {
    super("Goal", ".gff", res, new GOALExporter<>(v -> v.startVertex, v -> v.accepting, simplifyTransitions));
  }

  @Override
  protected Function<MapperEdge, Map<String, Attribute>> edgeAttributeProvider() {
    return e -> {
      var provider = super.edgeAttributeProvider();
      var map = provider.apply(e);
      map.put("Description", DefaultAttribute.createAttribute(e.event.toString()));
      return map;
    };
  }

  @Override
  protected Supplier<Map<String, Attribute>> graphAttributeProvider() {
    var provider = super.graphAttributeProvider();
    return () -> {
      var map = provider.get();
      map.put("AboveTransition", DefaultAttribute.createAttribute("Description"));
      return map;
    };
  }
}
