import java.io.InputStreamReader;

import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContinuationPending;
import org.mozilla.javascript.NativeContinuation;

public class Atilla {
  public static void main(String[] args) throws Exception {
    try (var cx = Context.enter()) {
      cx.setOptimizationLevel(-1);
      cx.setLanguageVersion(Context.VERSION_ES6);
      var scope = cx.initStandardObjects();
      Callable capture = (cx0, scope0, thisObj, args0) -> {
        throw cx0.captureContinuation();
      };

      scope.put("capture", scope, capture);
      try (var scriptSrc = new InputStreamReader(
        Atilla.class.getResourceAsStream("cont.js"))) {
        var script = cx.compileReader(scriptSrc, "cont.js", 1, null);
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
}
