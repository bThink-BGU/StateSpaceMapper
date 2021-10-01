package org.jgrapht.nio.dot;


import org.jgrapht.Graph;
import org.jgrapht.nio.*;
import org.svvrl.goal.core.Preference;
import org.svvrl.goal.core.aut.AlphabetType;
import org.svvrl.goal.core.aut.ClassicAcc;
import org.svvrl.goal.core.aut.Position;
import org.svvrl.goal.core.aut.fsa.FSA;
import org.svvrl.goal.core.io.FileHandler;
import org.svvrl.goal.core.io.GFFCodec;

import java.io.ByteArrayOutputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.stream.Stream;

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
  private Predicate<V> isStartingVertex;
  private Predicate<V> isAcceptingVertex;

  /**
   * Constructs a new GOALExporter object with an integer id provider.
   */
  public GOALExporter() {
    this(V -> false, V -> false);
  }

  public GOALExporter(Predicate<V> isStartingVertex, Predicate<V> isAcceptingVertex) {
    this(new IntegerIdProvider<>(), new IntegerIdProvider<>(), isStartingVertex, isAcceptingVertex);
  }

  public GOALExporter(Function<V, String> vertexIdProvider, Function<E, String> edgeIdProvider, Predicate<V> isStartingVertex, Predicate<V> isAcceptingVertex) {
    super(vertexIdProvider);
    setEdgeIdProvider(edgeIdProvider);
    this.isStartingVertex = isStartingVertex;
    this.isAcceptingVertex = isAcceptingVertex;
  }

  public void setIsStartingVertex(Predicate<V> isStartingVertex) {
    this.isStartingVertex = isStartingVertex;
  }

  public void setIsAcceptingVertex(Predicate<V> isAcceptingVertex) {
    this.isAcceptingVertex = isAcceptingVertex;
  }

  /**
   * Exports a graph into a plain text file in DOT format.
   *
   * @param g      the graph to be exported
   * @param writer the writer to which the graph to be exported
   */
  @Override
  public void exportGraph(Graph<V, E> g, Writer writer) {
    FSA fsa = createFSA(g);

    try (var baos = new ByteArrayOutputStream()) {
      FileHandler.save(fsa, baos, new GFFCodec());
      writer.write(baos.toString(StandardCharsets.UTF_8));
      writer.flush();
    } catch (Exception e) {
      throw new ExportException("Failed to write " + e.getMessage());
    }
  }

  public FSA createFSA(Graph<V, E> g) {
    FSA fsa = new FSA(AlphabetType.CLASSICAL, Position.OnTransition);
    fsa.setName(getGraphId().orElse(DEFAULT_GRAPH_ID));


    // Add user attributes
    Stream.concat(
        Stream.concat(
            graphAttributeProvider.orElse(Collections::emptyMap).get().keySet().stream(),
            g.vertexSet().stream().flatMap(v -> getVertexAttributes(v).orElse(Collections.emptyMap()).keySet().stream())
        ),
        g.edgeSet().stream().flatMap(e -> getEdgeAttributes(e).orElse(Collections.emptyMap()).keySet().stream())
    ).distinct().forEach(Preference::addUserPropertyName);

    // add graph attributes
    graphAttributeProvider.orElse(Collections::emptyMap).get().forEach((key, value) -> fsa.getProperties().setProperty(key, value.getValue()));

    var acc = new ClassicAcc();

    // vertex set
    for (V v : g.vertexSet()) {
      int id = getVertexID(v);
      var state = fsa.newState(id);
      getVertexAttributes(v).ifPresent(m -> {
        for (var entry : m.entrySet()) {
          state.getProperties().setProperty(entry.getKey(), renderAttribute(entry.getValue()));
        }
      });
      fsa.addState(state);
      if (isStartingVertex.test(v)) {
        fsa.addInitialState(state);
      }

      if (isAcceptingVertex.test(v)) {
        acc.add(state);
      }
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
        }
      });
      fsa.addTransition(transition);
    }

    fsa.setAcc(acc);
    return fsa;
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
