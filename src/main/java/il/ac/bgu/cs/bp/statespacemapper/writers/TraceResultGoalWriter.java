package il.ac.bgu.cs.bp.statespacemapper.writers;

import il.ac.bgu.cs.bp.bpjs.internal.ScriptableUtils;
import il.ac.bgu.cs.bp.bpjs.model.BEvent;
import il.ac.bgu.cs.bp.bpjs.model.BProgramSyncSnapshot;
import il.ac.bgu.cs.bp.bpjs.model.SyncStatement;
import il.ac.bgu.cs.bp.statespacemapper.GenerateAllTracesInspection;
import il.ac.bgu.cs.bp.statespacemapper.GoalTool;
import org.svvrl.goal.core.Preference;
import org.svvrl.goal.core.UnsupportedException;
import org.svvrl.goal.core.aut.AlphabetType;
import org.svvrl.goal.core.aut.ClassicAcc;
import org.svvrl.goal.core.aut.Position;
import org.svvrl.goal.core.aut.fsa.FSA;
import org.svvrl.goal.core.io.FileHandler;
import org.svvrl.goal.core.io.GFFCodec;
import org.svvrl.goal.core.logic.ParseException;
import org.svvrl.goal.core.logic.re.RegularExpression;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;

/**
 * A Writer for the <a href="http://goal.im.ntu.edu.tw/wiki/doku.php?id=start">GOAL</a> - a graphical interactive tool for defining and manipulating Büchi automata and temporal logic formulae.<br/>
 */
public class TraceResultGoalWriter extends TraceResultWriter implements AutoCloseable {
  protected int level;
  protected final AtomicInteger edgeCounter = new AtomicInteger();
  protected boolean generateRegularExpression = true;
  protected boolean simplifyAutomaton = true;
  protected final PrintStream gff;
  protected final ByteArrayOutputStream baos;
  public String regularExpression = null;
  protected final FSA fsa;

  public TraceResultGoalWriter(String name) {
    super(name, "gff");
    setPrintStatements(true);
    setPrintStore(true);
    baos = new ByteArrayOutputStream();
    gff = new PrintStream(baos);
    fsa = new FSA(AlphabetType.CLASSICAL, Position.OnTransition);
    fsa.setName(sanitize(name));
  }


  @Override
  protected void writePre() {

  }

  @Override
  protected String nodeToString(int id, BProgramSyncSnapshot bpss) {
    int hash = bpss.hashCode();
    String store = !printStore ? "" : getStore(bpss);
    String statements = !printStatements ? "" : getStatments(bpss);

    var state = fsa.newState(id);
    state.getProperties().setProperty("Hash", hash);
    state.getProperties().setProperty("Store", store);
    state.getProperties().setProperty("Statements", statements);
    fsa.addState(state);
    return "";
  }

  protected String getStore(BProgramSyncSnapshot bpss) {
    return bpss.getDataStore().entrySet().stream()
        .map(entry -> "{" + sanitize(ScriptableUtils.stringify(entry.getKey())) + "," + sanitize(ScriptableUtils.stringify(entry.getValue())) + "}")
        .collect(joining(",", "[", "]"));
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
        .collect(joining(",\n", "[", "]"));
  }

  @Override
  protected String edgeToString(GenerateAllTracesInspection.Edge edge) {
    var src = fsa.getStateByID(edge.srcId);
    var dst = fsa.getStateByID(edge.dstId);
    var transition = fsa.createTransition(src, dst, sanitize(eventToString(edge.event)));
    transition.setDescription(edge.event.maybeData == null ? edge.event.name :
        "{" + edge.event.name + ", " + edge.event.maybeData + "}");
    transition.getProperties().setProperty("EventName", edge.event.name);
    transition.getProperties().setProperty("EventData", edge.event.getDataField().orElse("").toString());
    fsa.addTransition(transition);
    return "";
  }

  @Override
  protected String eventToString(BEvent event) {
    return eventToString(result.events.get(event));
  }

  protected String eventToString(int eventId) {
    return "" + (char) ('a' + eventId);
  }

  @Override
  public void write(PrintStream out, GenerateAllTracesInspection.MapperResult result) {
    this.out = out;
    this.result = result;
    Preference.addUserPropertyName("Hash");
    Preference.addUserPropertyName("Statements");
    Preference.addUserPropertyName("Store");
    Preference.addUserPropertyName("EventName");
    Preference.addUserPropertyName("EventData");
    fsa.getProperties().setProperty("AboveTransition", "Description");
    result.states.entrySet().stream()
        .sorted(Map.Entry.comparingByValue())
        .forEach(this::nodeToString);
    fsa.expandAlphabet(result.events.values().stream().map(this::eventToString).collect(Collectors.toList()).toArray(new String[]{}));
    result.edges.forEach(this::edgeToString);
    fsa.setInitialState(fsa.getStateByID(result.startNodeId));
    var acc = new ClassicAcc();
    acc.addAll(result.acceptingStates.keySet().stream().map(fsa::getStateByID).collect(Collectors.toList()));
    fsa.setAcc(acc);
    finalizeOutput();
  }

  protected void finalizeOutput() {
    if (simplifyAutomaton) {
      System.out.println("Simplifying automaton using GOAL");
      fsa.simplifyTransitions();
    }
    FileHandler.save(fsa, out, new GFFCodec());
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
  public void close() {
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
    if (generateRegularExpression) {
      try {
        System.out.println("Generating regular expression using GOAL");
        RegularExpression re = GoalTool.fsa2re(fsa);
        this.regularExpression = GoalTool.regex2string(re);
        return Optional.of(regularExpression);
      } catch (UnsupportedException | ParseException e) {
        e.printStackTrace();
      }
    }
    return Optional.empty();
  }
}
