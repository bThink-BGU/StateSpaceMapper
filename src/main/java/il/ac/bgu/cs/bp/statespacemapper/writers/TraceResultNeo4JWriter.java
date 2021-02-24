package il.ac.bgu.cs.bp.statespacemapper.writers;

import il.ac.bgu.cs.bp.bpjs.internal.ScriptableUtils;
import il.ac.bgu.cs.bp.bpjs.model.BEvent;
import il.ac.bgu.cs.bp.bpjs.model.BProgramSyncSnapshot;
import il.ac.bgu.cs.bp.bpjs.model.SyncStatement;
import il.ac.bgu.cs.bp.statespacemapper.GenerateAllTracesInspection;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;

import java.io.OutputStream;
import java.io.PrintStream;

import static java.util.stream.Collectors.joining;
import static org.neo4j.driver.Values.parameters;

public class TraceResultNeo4JWriter extends TraceResultWriter {
  private final Driver driver;

  public TraceResultNeo4JWriter(GenerateAllTracesInspection.MapperResult result, String runName, Driver driver) {
    super(new PrintStream(OutputStream.nullOutputStream()), result, runName);
    this.driver = driver;
  }

  private static String getStore(BProgramSyncSnapshot bpss) {
    return bpss.getDataStore().entrySet().stream()
        .map(entry -> "{" + ScriptableUtils.stringify(entry.getKey()) + ": " + ScriptableUtils.stringify(entry.getValue()) + "}")
        .collect(joining(",", "[", "]"));
  }

  private String getStatements(BProgramSyncSnapshot bpss) {
    return bpss.getBThreadSnapshots().stream()
        .map(btss -> {
          SyncStatement syst = btss.getSyncStatement();
          return
              "{name: " + btss.getName() + ",\n" +
                  "isHot: " + syst.isHot() + ",\n" +
                  "request: " + syst.getRequest().stream().map(this::eventToString).collect(joining(",", "[", "]")) + ",\n" +
                  "waitFor: " + syst.getWaitFor() + ",\n" +
                  "block: " + syst.getBlock() + ",\n" +
                  "interrupt: " + syst.getInterrupt() + "}";
        })
        .collect(joining(",\n", "\nStatements: [", "]"));
  }

  @Override
  protected String eventToString(BEvent event) {
    String e = "{" + event.name;
    if (event.maybeData != null)
      e += ": " + ScriptableUtils.stringify(event.maybeData);
    return e + "}";
  }

  private void deleteAll() {
    try (Session session = driver.session()) {
      session.writeTransaction(tx -> tx.run(
          "MATCH (n) DETACH DELETE n"));
    }
  }

  @Override
  protected String edgeToString(GenerateAllTracesInspection.Edge edge) {
    try (Session session = driver.session()) {
      session.writeTransaction(tx -> tx.run(
          "MATCH (src:Node {id:$srcId}) MATCH(dst:Node {id:$dstId}) " +
              "CREATE (src)-[e:EVENT {name:$eName, data:$eData}]->(dst)",
          parameters(
              "srcId", edge.srcId,
              "dstId", edge.dstId,
              "eName", edge.event.name,
              "eData", ScriptableUtils.stringify(edge.event.maybeData)
          )));
    }
    return null;
  }

  @Override
  protected void writePre() {
    deleteAll();
  }

  @Override
  protected String nodeToString(int id, BProgramSyncSnapshot bpss) {
    try (Session session = driver.session()) {
      session.writeTransaction(tx -> tx.run("CREATE (a:Node {id: $id, hash: $hash, store:$store, statements:$stmt, start: $start})",
          parameters(
              "id", id,
              "hash", bpss.hashCode(),
              "store", getStore(bpss),
              "start", bpss.equals(result.startNode),
              "stmt", getStatements(bpss)
          )));
    }
    return null;
  }

  private static String sanitize(Object in) {
    return in.toString()
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("JS_Obj ", "")
        .replaceAll("[. -+]", "_");
  }
}
