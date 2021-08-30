package il.ac.bgu.cs.bp.statespacemapper.jgrapht.exports;


import org.jgrapht.Graph;
import org.jgrapht.nio.*;
import org.svvrl.goal.core.Preference;
import org.svvrl.goal.core.aut.AlphabetType;
import org.svvrl.goal.core.aut.ClassicAcc;
import org.svvrl.goal.core.aut.Position;
import org.svvrl.goal.core.aut.fsa.FSA;
import org.svvrl.goal.core.io.FileHandler;
import org.svvrl.goal.core.io.GFFCodec;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;

/**
 * Exports a graph into a GOAL (gff) file.
 *
 * @param <V> the graph vertex type
 * @param <E> the graph edge type
 * @author Trevor Harmon
 * @author Dimitrios Michail
 */
public class GOALExporter<V, E> extends
    BaseExporter<V, E>
    implements
    GraphExporter<V, E> {
  /**
   * Default graph id used by the exporter.
   */
  public static final String DEFAULT_GRAPH_ID = "G";

  private static final String INDENT = "  ";

  /**
   * Constructs a new DOTExporter object with an integer id provider.
   */
  public GOALExporter() {
    this(new IntegerIdProvider<>(), new IntegerIdProvider<>());
  }

  /**
   * Constructs a new DOTExporter object with the given id provider. Additional providers such as
   * attributes can be given using the appropriate setter methods.
   *
   * @param vertexIdProvider for generating vertex IDs. Must not be null.
   */
  public GOALExporter(Function<V, String> vertexIdProvider, Function<E, String> edgeIdProvider) {
    super(vertexIdProvider);
    setEdgeIdProvider(edgeIdProvider);
  }

  /**
   * Exports a graph into a plain text file in DOT format.
   *
   * @param g      the graph to be exported
   * @param writer the writer to which the graph to be exported
   */
  @Override
  public void exportGraph(Graph<V, E> g, Writer writer) {
    var attributesNames = new HashSet<String>();

    FSA fsa = new FSA(AlphabetType.CLASSICAL, Position.OnTransition);
    fsa.setName(getGraphId().orElse(DEFAULT_GRAPH_ID));

    // graph attributes
    for (Map.Entry<String, Attribute> attr : graphAttributeProvider
        .orElse(Collections::emptyMap).get().entrySet()) {
      attributesNames.add(attr.getKey());
    }

    // vertex set
    for (V v : g.vertexSet()) {
      var state = fsa.newState(getVertexID(v));
      getVertexAttributes(v).ifPresent(m -> {
        for (var entry : m.entrySet()) {
          state.getProperties().setProperty(entry.getKey(), renderAttribute(entry.getValue()));
          attributesNames.add(entry.getKey());
        }
      });
      fsa.addState(state);
    }

    // edge set
    for (E e : g.edgeSet()) {
      var src = fsa.getStateByID(getVertexID(g.getEdgeSource(e)));
      var dst = fsa.getStateByID(getVertexID(g.getEdgeTarget(e)));
      var id = getEdgeId(e).get();
      var transition = fsa.createTransition(src, dst, id);
      transition.setDescription(id);
      getEdgeAttributes(e).ifPresent(m -> {
        for (var entry : m.entrySet()) {
          transition.getProperties().setProperty(entry.getKey(), renderAttribute(entry.getValue()));
          attributesNames.add(entry.getKey());
        }
      });
      fsa.addTransition(transition);
    }

    attributesNames.forEach(Preference::addUserPropertyName);

//    fsa.setInitialState(fsa.getStateByID(g.));
    var acc = new ClassicAcc();
//    acc.addAll(result.acceptingStates.keySet().stream().map(fsa::getStateByID).collect(Collectors.toList()));
    fsa.setAcc(acc);

    var baos = new ByteArrayOutputStream();
    try {
      FileHandler.save(fsa, baos, new GFFCodec());
      writer.write(baos.toString(StandardCharsets.UTF_8));
    } catch (Exception e) {
      throw new ExportException("Failed to write " + e.getMessage());
    }
  }

  private int getVertexID(V v) {
    return Integer.parseInt(getVertexId(v));
  }

  private String renderAttribute(Attribute attribute) {
    final String attrValue = attribute.getValue();
    if (AttributeType.HTML.equals(attribute.getType())) {
      return "<" + attrValue + ">";
    } else if (AttributeType.IDENTIFIER.equals(attribute.getType())) {
      return attrValue;
    } else {
      return "\"" + escapeDoubleQuotes(attrValue) + "\"";
    }
  }

  private static String escapeDoubleQuotes(String labelName) {
    return labelName.replaceAll("\"", Matcher.quoteReplacement("\\\""));
  }
}
