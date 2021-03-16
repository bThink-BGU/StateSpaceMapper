package il.ac.bgu.cs.bp.statespacemapper.writers;

import il.ac.bgu.cs.bp.bpjs.model.BProgramSyncSnapshot;
import il.ac.bgu.cs.bp.statespacemapper.GenerateAllTracesInspection;

import java.text.MessageFormat;

/**
 * A Writer for the <a href="https://github.com/izuzak/noam">NOAM tool</a><br/>
 * The resulted automaton can be translated to a regular expression using <a href="http://ivanzuzak.info/noam/webapps/fsm2regex/">fsm2regex</a>
 */
public class TraceResultNoamWriter extends TraceResultWriter {

  public TraceResultNoamWriter(String name) {
    super(name, "noam");
  }

  @Override
  protected void writePre() {
  }

  @Override
  protected void writeNodesPre() {
    out.println("#states");
  }

  @Override
  protected void writeNodesPost() {
    out.println("#initial");
    out.println(nodeToString(result.startNodeId));
    out.println("#accepting");
    result.acceptingStates.forEach((key, bpss) -> out.println(nodeToString(key)));
  }

  @Override
  protected void writeEdgesPre() {
    out.println("#alphabet");
    result.edges.stream().map(e -> eventToString(e.event)).distinct().sorted().forEachOrdered(out::println);
    out.println("#transitions");
  }

  @Override
  protected String nodeToString(int id, BProgramSyncSnapshot bpss) {
    return nodeToString(id);
  }

  protected String nodeToString(int id) {
    return "s" + sanitize(id);
  }

  @Override
  protected String edgeToString(GenerateAllTracesInspection.Edge edge) {
    return MessageFormat.format("{0}:{1}>{2}", nodeToString(edge.srcId), eventToString(edge.event), nodeToString(edge.dstId));
  }

  @Override
  protected String sanitize(String in) {
    return in.replaceAll("[^a-zA-Z0-9]", "");
  }
}
