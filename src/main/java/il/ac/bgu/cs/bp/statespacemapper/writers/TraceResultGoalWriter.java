package il.ac.bgu.cs.bp.statespacemapper.writers;

import il.ac.bgu.cs.bp.bpjs.internal.ScriptableUtils;
import il.ac.bgu.cs.bp.bpjs.model.BEvent;
import il.ac.bgu.cs.bp.bpjs.model.BProgramSyncSnapshot;
import il.ac.bgu.cs.bp.bpjs.model.SyncStatement;
import il.ac.bgu.cs.bp.statespacemapper.GenerateAllTracesInspection;
import il.ac.bgu.cs.bp.statespacemapper.GoalTool;
import org.svvrl.goal.core.UnsupportedException;
import org.svvrl.goal.core.aut.fsa.FSA;
import org.svvrl.goal.core.io.CodecException;
import org.svvrl.goal.core.logic.ParseException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.stream.Collectors.joining;

/**
 * A Writer for the <a href="http://goal.im.ntu.edu.tw/wiki/doku.php?id=start">GOAL</a> - a graphical interactive tool for defining and manipulating Büchi automata and temporal logic formulae.<br/>
 */
public class TraceResultGoalWriter extends TraceResultWriter implements AutoCloseable {
  private int level;
  private final AtomicInteger edgeCounter = new AtomicInteger();
  private boolean generateRegularExpression = true;
  private boolean simplifyAutomaton = true;
  private final PrintStream gff;
  private final ByteArrayOutputStream baos;
  public String regularExpression = null;

  public TraceResultGoalWriter(String name) {
    super(name, "gff");
    baos = new ByteArrayOutputStream();
    gff = new PrintStream(baos);
  }

  @Override
  protected void writePre() {
    gff.println("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>");
    gff.println("<Structure label-on=\"Transition\" type=\"FiniteStateAutomaton\">");
    level = 1;
    gff.println(MessageFormat.format("{0}<Name>{1}</Name>", "    ".repeat(level), sanitize(name)));
    gff.println("    ".repeat(level) + "<Description/>");
    gff.println("    ".repeat(level) + "<Properties/>");
    gff.println("    ".repeat(level) + "<Formula/>");
    gff.println("    ".repeat(level) + "<Alphabet type=\"Classical\">");
    level++;
    result.events.values().forEach(e -> gff.println(MessageFormat.format("{0}<Symbol>{1}</Symbol>", "    ".repeat(level), e)));
    level--;
    gff.println("    ".repeat(level) + "</Alphabet>");
  }

  @Override
  protected void writeNodesPre() {
    gff.println("    ".repeat(level) + "<StateSet>");
    level++;
  }

  @Override
  protected void writeNodesPost() {
    level--;
    gff.println("    ".repeat(level) + "</StateSet>");
    gff.println("    ".repeat(level) + "<InitialStateSet>");
    gff.println(MessageFormat.format("{0}<StateID>{1}</StateID>", "    ".repeat(level + 1), result.startNodeId));
    gff.println("    ".repeat(level) + "</InitialStateSet>");
  }

  @Override
  protected void writeEdgesPre() {
    gff.println("    ".repeat(level) + "<TransitionSet complete=\"false\">");
    level++;
  }

  @Override
  protected void writeEdgesPost() {
    level--;
    gff.println("    ".repeat(level) + "</TransitionSet>");
  }

  @Override
  protected String nodeToString(int id, BProgramSyncSnapshot bpss) {
    StringBuilder out = new StringBuilder();
    int hash = bpss.hashCode();
    String store = !printStore ? "" : getStore(bpss);
    String statements = !printStatements ? "" : getStatments(bpss);
    out.append(MessageFormat.format("{0}<State sid=\"{1}\">\n", "    ".repeat(level), id));
    out.append(MessageFormat.format("{0}<Description>Hash={1}{2}{3}</Description>\n", "    ".repeat(level + 1), hash, store, statements));
    out.append("    ".repeat(level + 1)).append("<Properties/>\n");
    out.append("    ".repeat(level)).append("</State>");
    return out.toString();
  }

  protected String getStore(BProgramSyncSnapshot bpss) {
    return bpss.getDataStore().entrySet().stream()
        .map(entry -> "{" + sanitize(ScriptableUtils.stringify(entry.getKey())) + "," + sanitize(ScriptableUtils.stringify(entry.getValue())) + "}")
        .collect(joining(",", "; Store: [", "]"));
  }

  protected String getStatments(BProgramSyncSnapshot bpss) {
    return bpss.getBThreadSnapshots().stream()
        .map(btss -> {
          SyncStatement syst = btss.getSyncStatement();
          return
              "{name: " + sanitize(btss.getName()) + ", " +
                  "isHot: " + syst.isHot() + ", " +
                  "request: " + syst.getRequest().stream().map(e -> sanitize(eventToString(e))).collect(joining(",", "[", "]")) + ", " +
                  "waitFor: " + sanitize(syst.getWaitFor()) + ", " +
                  "block: " + sanitize(syst.getBlock()) + ", " +
                  "interrupt: " + sanitize(syst.getInterrupt()) + "}";
        })
        .collect(joining(", ", "; Statements: [", "]"));
  }

  @Override
  protected String edgeToString(GenerateAllTracesInspection.Edge edge) {
    StringBuilder out = new StringBuilder();
    out.append(MessageFormat.format("{0}<Transition tid=\"{1}\">\n", "    ".repeat(level), edgeCounter.getAndIncrement()));
    out.append(MessageFormat.format("{0}<From>{1}</From>\n", "    ".repeat(level + 1), edge.srcId));
    out.append(MessageFormat.format("{0}<To>{1}</To>\n", "    ".repeat(level + 1), edge.dstId));
    out.append(MessageFormat.format("{0}<Label>{1}</Label>\n", "    ".repeat(level + 1), sanitize(eventToString(edge.event))));
    out.append("    ".repeat(level + 1)).append("<Properties/>\n");
    out.append("    ".repeat(level)).append("</Transition>");
    return out.toString();
  }

  @Override
  protected String eventToString(BEvent event) {
    return "" + (char) ('a' + result.events.get(event));
  }

  @Override
  protected void writePost() {
    gff.println("    ".repeat(level) + "<Acc type=\"Classic\">");
    result.acceptingStates.forEach((key, bpss) -> gff.println(
        MessageFormat.format("{0}<StateID>{1}</StateID>", "    ".repeat(level + 1), key)));
    gff.println("    ".repeat(level) + "</Acc>");
    level--;
    gff.println("</Structure>");
    finalizeOutput();
  }

  private void finalizeOutput() {
    String s = baos.toString();
    FSA fsa = null;
    if (simplifyAutomaton) {
      try {
        fsa = GoalTool.string2fsa(s);
        s = GoalTool.fsa2string(fsa);
      } catch (CodecException | IOException e) {
        System.out.println("Could not simplify the automaton using GOAL. Exception:" + e.getMessage());
      }
    }
    if (generateRegularExpression) {
      if (fsa == null) {
        try {
          fsa = GoalTool.string2fsa(s);
          var re = GoalTool.fsa2re(fsa);
          this.regularExpression = GoalTool.regex2string(re);
        } catch (CodecException | IOException | UnsupportedException | ParseException e) {
          System.out.println("Could not simplify the automaton using GOAL. Exception:" + e.getMessage());
        }
      }
    }
    out.println(s);
  }

  public void setGenerateRegularExpression(boolean createRegularExpression) {
    this.generateRegularExpression = createRegularExpression;
  }

  public boolean isSimplifyAutomaton() {
    return simplifyAutomaton;
  }

  public void setSimplifyAutomaton(boolean simplifyAutomaton) {
    this.simplifyAutomaton = simplifyAutomaton;
  }

  @Override
  public void close() throws Exception {
    try {
      gff.close();
    } catch (Exception ignored) {
    } finally {
      try {
        baos.close();
      } catch (Exception ignored) {
      }
    }
  }

  public Optional<String> getRegularExpression() {
    if(regularExpression == null) return Optional.empty();
    return Optional.of(regularExpression);
  }
}
