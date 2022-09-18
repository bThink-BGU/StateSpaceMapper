package il.ac.bgu.cs.bp.statespacemapper;

import il.ac.bgu.cs.bp.bpjs.context.ContextBProgram;
import org.mozilla.javascript.NativeContinuation;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.GraphDatabase;

import java.io.FileWriter;
import java.io.PrintWriter;

public class LoadBug {
  public static int id = 4;
  public static String dir = "bug-run" + id;

  public static void main(String[] args) throws Exception {
    System.out.println("//reading continuations from '" + dir + "'");
    var bprog = new ContextBProgram("HotCold/dal.js", "HotCold/bl.js").setup();
    var ss1bt = (NativeContinuation) GenerateAllTracesInspection.deserializeContinuation(bprog, dir + "/ss1bt.bin");
    var ss2bt = (NativeContinuation) GenerateAllTracesInspection.deserializeContinuation(bprog, dir + "/ss2bt.bin");
    var list0bt = (NativeContinuation) GenerateAllTracesInspection.deserializeContinuation(bprog, dir + "/list0bt.bin");
    var list1bt = (NativeContinuation) GenerateAllTracesInspection.deserializeContinuation(bprog, dir + "/list1bt.bin");

    try (FileWriter fileWriter = new FileWriter(dir + "/stdout.txt", true);
         PrintWriter out = new PrintWriter(fileWriter);) {
      out.println();
      out.println();
      out.println();
      out.println("now deserialize...");
      out.println();
      out.println("NativeContinuation.equalImplementations(list0bt,list1bt) = " + NativeContinuation.equalImplementations(list0bt, list1bt));
      out.println("NativeContinuation.equalImplementations(ss1bt,list0bt) = " + NativeContinuation.equalImplementations(ss1bt, list0bt));
      out.println("NativeContinuation.equalImplementations(ss1bt,list1bt) = " + NativeContinuation.equalImplementations(ss1bt, list1bt));
      out.println("NativeContinuation.equalImplementations(ss2bt,list0bt) = " + NativeContinuation.equalImplementations(ss2bt, list0bt));
      out.println("NativeContinuation.equalImplementations(ss2bt,list1bt) = " + NativeContinuation.equalImplementations(ss2bt, list1bt));
      out.flush();
    }
  }
}
