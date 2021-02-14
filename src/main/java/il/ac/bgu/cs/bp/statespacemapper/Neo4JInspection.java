package il.ac.bgu.cs.bp.statespacemapper;

import il.ac.bgu.cs.bp.bpjs.analysis.ExecutionTrace;
import il.ac.bgu.cs.bp.bpjs.analysis.ExecutionTraceInspection;
import il.ac.bgu.cs.bp.bpjs.analysis.violations.Violation;
import il.ac.bgu.cs.bp.bpjs.internal.ScriptableUtils;
import il.ac.bgu.cs.bp.bpjs.model.BEvent;
import il.ac.bgu.cs.bp.bpjs.model.BProgramSyncSnapshot;
import il.ac.bgu.cs.bp.bpjs.model.SyncStatement;
import org.neo4j.driver.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.stream.Collectors.joining;
import static org.neo4j.driver.Values.parameters;

public class Neo4JInspection implements ExecutionTraceInspection, AutoCloseable {
  private final Driver driver;
  private final AtomicInteger counter = new AtomicInteger();
  private final HashMap<BProgramSyncSnapshot, Integer> states = new HashMap<>();

  public Neo4JInspection() {
    this("bolt://localhost:11002", "neo4j", "StateMapper");
  }

  public Neo4JInspection(String uri, String user, String password) {
    this.driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));
    deleteAll();
  }

  @Override
  public String title() {
    return "Neo4JInspection";
  }

  @Override
  public Optional<Violation> inspectTrace(ExecutionTrace aTrace) {
    int stateCount = aTrace.getStateCount();
    var lastNode = aTrace.getNodes().get(stateCount - 1);
    if (aTrace.isCyclic()) {
      addBpss(lastNode.getState(), false);
      addEdge(aTrace.getLastState(), aTrace.getFinalCycle().get(0).getState(), aTrace.getLastEvent().get());
    } else {
      if (stateCount == 1) {
        addBpss(aTrace.getNodes().get(0).getState(), true);
      } else {
        addBpss(lastNode.getState(), false);
        var src = aTrace.getNodes().get(stateCount - 2);
        addEdge(src.getState(), lastNode.getState(), src.getEvent().get());
      }
    }
    return Optional.empty();
  }

  @Override
  public void close() throws Exception {
    driver.close();
  }

  private void addBpss(BProgramSyncSnapshot bpss, boolean isStart) {
    if (states.containsKey(bpss)) return;
    var id = counter.getAndIncrement();
    states.put(bpss, id);
    try (Session session = driver.session()) {
      session.writeTransaction(tx -> tx.run("CREATE (a:Node {id: $id, hash: $hash, store:$store, statements:$stmt, start: $start})",
          parameters(
              "id", id,
              "hash", bpss.hashCode(),
              "store", getStore(bpss),
              "start", isStart,
              "stmt", getStatements(bpss)
          )));
    }
  }

  private static String getStore(BProgramSyncSnapshot bpss) {
    return bpss.getDataStore().entrySet().stream()
        .map(entry -> "{" + ScriptableUtils.stringify(entry.getKey()) + "," + ScriptableUtils.stringify(entry.getValue()) + "}")
        .collect(joining(",", "[", "]"));
  }

  private static String getStatements(BProgramSyncSnapshot bpss) {
    return bpss.getBThreadSnapshots().stream()
        .map(btss -> {
          SyncStatement syst = btss.getSyncStatement();
          return
              "{name: " + btss.getName() + ",\n" +
                  "isHot: " + syst.isHot() + ",\n" +
                  "request: " + syst.getRequest().stream().map(e -> eventToString(e)).collect(joining(",", "[", "]")) + ",\n" +
                  "waitFor: " + syst.getWaitFor() + ",\n" +
                  "block: " + syst.getBlock() + ",\n" +
                  "interrupt: " + syst.getInterrupt() + "}";
        })
        .collect(joining(",\n", "\nStatements: [", "]"));
  }

  private void deleteAll() {
    try (Session session = driver.session()) {
      session.writeTransaction(tx -> tx.run(
          "MATCH (n) DETACH DELETE n"));
    }
  }

  private void addEdge(BProgramSyncSnapshot src, BProgramSyncSnapshot dst, BEvent event) {
    int srcId = states.get(src);
    int dstId = states.get(dst);
    try (Session session = driver.session()) {
      session.writeTransaction(tx -> tx.run(
          "MATCH (src:Node {id:$srcId}) MATCH(dst:Node {id:$dstId}) " +
              "CREATE (src)-[e:EVENT {name:$eName, data:$eData}]->(dst)",
          parameters(
              "srcId", srcId,
              "dstId", dstId,
              "eName", event.name,
              "eData", ScriptableUtils.stringify(event.maybeData)
          )));
    }
  }

  private static String eventToString(BEvent event) {
    String e = "{" + event.name;
    if (event.maybeData != null)
      e += ": " + ScriptableUtils.stringify(event.maybeData);
    return e + "}";
  }
}
