package il.ac.bgu.cs.bp.statespacemapper;

import il.ac.bgu.cs.bp.bpjs.internal.ScriptableUtils;
import il.ac.bgu.cs.bp.bpjs.model.BEvent;
import il.ac.bgu.cs.bp.bpjs.model.BProgramSyncSnapshot;
import il.ac.bgu.cs.bp.bpjs.model.SyncStatement;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;

import static java.util.stream.Collectors.joining;

public class TraceResultNeo4JWriter extends TraceResultWriter {
  private final Driver driver;

  public TraceResultNeo4JWriter(GenerateAllTracesInspection.MapperResult result, String runName) {
    this(result, runName, "bolt://localhost:11002", "neo4j", "StateMapper");
  }

  public TraceResultNeo4JWriter(GenerateAllTracesInspection.MapperResult result, String runName, String uri, String user, String password) {
    super(null, result, runName);
    this.driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));
  }

  protected void innerWrite() {
    deleteAll();
    result.states.entrySet().forEach(this::nodeToString);
    result.edges.forEach(this::edgeToString);
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
//                  "request: " + syst.getRequest().stream().map(TraceResultNeo4JWriter::eventToString).collect(joining(",", "[", "]")) + ",\n" +
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
    /*int srcId = states.get(src);
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
    }*/
    return null;
  }

  @Override
  protected void writePre() {

  }

  @Override
  protected String nodeToString(int id, BProgramSyncSnapshot bpss) {
    /*var bpss = bpssEntry.getKey();

    String id = bpssEntry.getValue().toString();

    try (Session session = driver.session()) {
      session.writeTransaction(tx -> tx.run("CREATE (a:Node {id: $id, hash: $hash, store:$store, statements:$stmt, start: $start})",
          parameters(
              "id", id,
              "hash", bpss.hashCode(),
              "store", getStore(bpss),
              "start", isStart,
              "stmt", getStatements(bpss)
          )));
    }*/
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
