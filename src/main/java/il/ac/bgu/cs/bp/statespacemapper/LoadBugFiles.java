package il.ac.bgu.cs.bp.statespacemapper;

import il.ac.bgu.cs.bp.bpjs.bprogramio.BProgramSyncSnapshotIO;
import il.ac.bgu.cs.bp.bpjs.context.ContextBProgram;
import il.ac.bgu.cs.bp.bpjs.model.BProgram;
import il.ac.bgu.cs.bp.bpjs.model.BProgramSyncSnapshot;
import il.ac.bgu.cs.bp.bpjs.model.BThreadSyncSnapshot;
import org.mozilla.javascript.NativeContinuation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LoadBugFiles {
  public static void main(String[] args) throws Exception {
    System.out.println("//start");
    var bprog = new ContextBProgram("HotCold/dal.js","HotCold/bl.js");
    bprog.setup();
    var ss1 = loadBPSS(bprog, "snapshots/bpss1.bin");
    var ss2 = loadBPSS(bprog, "snapshots/bpss2.bin");
    var listSS1 = loadStates(bprog);
    listSS1.stream().distinct();
    compareSnapshots(listSS1, ss1, ss2);
  }

  private static BProgramSyncSnapshot loadBPSS(BProgram bProgram, String filename) {
    BProgramSyncSnapshotIO io = new BProgramSyncSnapshotIO(bProgram);

    try {
      byte[] arr = Files.readAllBytes(Path.of(filename));
      return io.deserialize(arr);
    } catch (IOException | ClassNotFoundException e) {
      throw new RuntimeException("Failed to serialize snapshot: " + e.getMessage(), e);
    }
  }

  private static List<BProgramSyncSnapshot> loadStates(BProgram bProgram) throws IOException {
    var statesSize = Files.list(Path.of("snapshots/")).count() - 2;
    var res = new ArrayList<BProgramSyncSnapshot>();
    for (int i = 0; i < statesSize; i++) {
      res.add(loadBPSS(bProgram, "snapshots/states"+i+".bin"));
    }
    return res;
  }

  private static void compareSnapshots(List<BProgramSyncSnapshot> listSS1, BProgramSyncSnapshot ss1, BProgramSyncSnapshot ss2) {
    System.out.println("listSS1.get(0).equals(listSS1.get(1)) = " + listSS1.get(0).equals(listSS1.get(1)));
    System.out.println("listSS1.get(1).equals(listSS1.get(0)) = " + listSS1.get(1).equals(listSS1.get(0)));
    BThreadSyncSnapshot list0bt = null;
    BThreadSyncSnapshot list1bt = null;
    BThreadSyncSnapshot ss1bt = null;
    BThreadSyncSnapshot ss2bt = null;
    for (var bt : listSS1.get(0).getBThreadSnapshots()) {
      if (!listSS1.get(1).getBThreadSnapshots().contains(bt)) {
        list0bt = bt;
        list1bt = listSS1.get(1).getBThreadSnapshots().stream().filter(snapshot -> snapshot.getName().equals(bt.getName())).findFirst().get();
        ss1bt = ss1.getBThreadSnapshots().stream().filter(snapshot -> snapshot.getName().equals(bt.getName())).findFirst().get();
        ss2bt = ss2.getBThreadSnapshots().stream().filter(snapshot -> snapshot.getName().equals(bt.getName())).findFirst().get();
      }
    }
    System.out.println("Name of conflicting b-thread: " + ss1bt.getName());
    System.out.println("NativeContinuation.equalImplementations(list0bt.getContinuation(),list1bt.getContinuation()) = " + NativeContinuation.equalImplementations(list0bt.getContinuation(), list1bt.getContinuation()));
    System.out.println("NativeContinuation.equalImplementations(ss1bt.getContinuation(),list0bt.getContinuation()) = " + NativeContinuation.equalImplementations(ss1bt.getContinuation(), list0bt.getContinuation()));
    System.out.println("NativeContinuation.equalImplementations(ss1bt.getContinuation(),list1bt.getContinuation()) = " + NativeContinuation.equalImplementations(ss1bt.getContinuation(), list1bt.getContinuation()));
    System.out.println("NativeContinuation.equalImplementations(ss2bt.getContinuation(),list0bt.getContinuation()) = " + NativeContinuation.equalImplementations(ss2bt.getContinuation(), list0bt.getContinuation()));
    System.out.println("NativeContinuation.equalImplementations(ss2bt.getContinuation(),list1bt.getContinuation()) = " + NativeContinuation.equalImplementations(ss2bt.getContinuation(), list1bt.getContinuation()));
    System.out.println("listSS1.get(0).equals(ss1) = " + listSS1.get(0).equals(ss1));
    System.out.println("ss1.equals(listSS1.get(0)) = " + ss1.equals(listSS1.get(0)));
    System.out.println("listSS1.get(1).equals(ss1) = " + listSS1.get(1).equals(ss1));
    System.out.println("ss1.equals(listSS1.get(1)) = " + ss1.equals(listSS1.get(1)));
    System.out.println("listSS1.get(0).equals(ss2) = " + listSS1.get(0).equals(ss2));
    System.out.println("ss2.equals(listSS1.get(0)) = " + ss2.equals(listSS1.get(0)));
    System.out.println("listSS1.get(1).equals(ss2) = " + listSS1.get(1).equals(ss2));
    System.out.println("ss2.equals(listSS1.get(1)) = " + ss2.equals(listSS1.get(1)));
  }
}
