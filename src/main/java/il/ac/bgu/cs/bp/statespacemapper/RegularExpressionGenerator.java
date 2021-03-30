package il.ac.bgu.cs.bp.statespacemapper;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class RegularExpressionGenerator implements Closeable {
  private final Context cx;
  private final Scriptable scope;
  private final String noamGraph;
  private String regex;

  public RegularExpressionGenerator(String noamGraph) {
    this.noamGraph = noamGraph;
    cx = Context.enter();
    String noamjs = getNoamJs();
    scope = cx.initStandardObjects();
    Object jsOut = Context.javaToJS(System.out, scope);
    ScriptableObject.putProperty(scope, "out", jsOut);
    cx.evaluateString(scope, noamjs, "noam.js", 1, null);
  }

  private String getNoamJs() {
    try (InputStream resource = Thread.currentThread().getContextClassLoader().getResourceAsStream("noam.js");
         InputStreamReader streamReader = new InputStreamReader(resource, StandardCharsets.UTF_8);
         BufferedReader br = new BufferedReader(streamReader)) {
      StringBuilder sb = new StringBuilder();
      String line;
      while ((line = br.readLine()) != null) {
        sb.append(line).append("\n");
      }
      return sb.toString();
    } catch (IOException ex) {
      throw new RuntimeException("Error reading resource: 'noam.js': " + ex.getMessage(), ex);
    }
  }

  public String generateRegex() {
    var getRE = (Function) scope.get("getRE", scope);
    regex = (String)getRE.call(cx, scope, scope, new Object[]{ noamGraph });
    return regex;
  }

  public String simplifyRegex() {
    var simplify = (Function) scope.get("simplify", scope);
    regex = (String)simplify.call(cx, scope, scope, new Object[]{ regex });
    return regex;
  }

  @Override
  public void close() {
    if(cx != null)
      Context.exit();
  }
}
