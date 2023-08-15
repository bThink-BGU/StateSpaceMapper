import il.ac.bgu.cs.bp.bpjs.analysis.DfsForStateMapper;
import il.ac.bgu.cs.bp.bpjs.analysis.listeners.PrintDfsVerifierListener;
import il.ac.bgu.cs.bp.bpjs.bprogramio.BProgramSyncSnapshotCloner;
import il.ac.bgu.cs.bp.bpjs.model.ResourceBProgram;
import il.ac.bgu.cs.bp.statespacemapper.GenerateAllTracesInspection;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContinuationPending;
import org.mozilla.javascript.NativeContinuation;

import java.io.InputStreamReader;

public class Atilla {
  public static void mainOld(String[] args) throws Exception {
    try (var cx = Context.enter()) {
      cx.setOptimizationLevel(-1);
      cx.setLanguageVersion(Context.VERSION_ES6);
      var scope = cx.initStandardObjects();
      Callable capture = (cx0, scope0, thisObj, args0) -> {
        throw cx0.captureContinuation();
      };

      scope.put("capture", scope, capture);
      try (var scriptSrc = new InputStreamReader(
        Atilla.class.getResourceAsStream("contOld.js"))) {
        var script = cx.compileReader(scriptSrc, "contOld.js", 1, null);
        try {
          cx.executeScriptWithContinuations(script, scope);
        } catch (ContinuationPending cp1) {
          var nc1 = cp1.getContinuation();
          var c1 = ((NativeContinuation) nc1).getImplementation();
          try {
            cx.resumeContinuation(nc1, scope, 42d);
          } catch (ContinuationPending cp2) {
            var nc2 = cp2.getContinuation();
            var c2 = ((NativeContinuation) nc2).getImplementation();
            System.out.println(c1.equals(c2));
          }
        }
      }
    }
  }

  public static void main(String[] args) throws Exception {
    mainCreateContinuations();
  }

  public static void mainCreateContinuations() throws Exception {
    var bprog = new ResourceBProgram("cont.js");
    var tracesInspection = new GenerateAllTracesInspection();
    var atillaInspection = new AtillaInspection();
    var vfr = new DfsForStateMapper();
    vfr.addInspection(tracesInspection);
    vfr.addInspection(atillaInspection);
    vfr.setProgressListener(new PrintDfsVerifierListener());
    vfr.verify(bprog);

    var cont1 = atillaInspection.bpss1.getBThreadSnapshots().stream().filter(btss -> btss.getName().equals("Add women jacket story")).findFirst().get().getContinuation();
    var cont2 = atillaInspection.bpss2.getBThreadSnapshots().stream().filter(btss -> btss.getName().equals("Add women jacket story")).findFirst().get().getContinuation();
    System.out.println("equals before serialization: " + NativeContinuation.equalImplementations(cont1, cont2));

    var clonedBpss1 = BProgramSyncSnapshotCloner.clone(atillaInspection.bpss1);
    var clonedBpss2 = BProgramSyncSnapshotCloner.clone(atillaInspection.bpss2);
    var clonedCont1 = clonedBpss1.getBThreadSnapshots().stream().filter(btss -> btss.getName().equals("Add women jacket story")).findFirst().get().getContinuation();
    var clonedCont2 = clonedBpss2.getBThreadSnapshots().stream().filter(btss -> btss.getName().equals("Add women jacket story")).findFirst().get().getContinuation();
    System.out.println("equals after serialization: " + NativeContinuation.equalImplementations(clonedCont1, clonedCont2));
  }
}
