package il.ac.bgu.cs.bp.statespacemapper;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import java.io.*;

import com.florianingerl.util.regex.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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

  public static void main(String[] args) {
    String p = "(?(DEFINE)(?<brace>\\((?>[^()]|(?'brace'))*\\)))(?<braces>\\({2}(?<body>(?>[^()]|(?'braces')|(?'brace'))*)\\){2})";
    String s = "aab((af(t)wet(a(a())f)ff))b((b))";
    Matcher m = Pattern.compile(p).matcher(s);
    System.out.println("original: " + s);
    while (m.find()) {
      System.out.println("group: " + m.group());
      System.out.println("body: " + m.group("body"));
    }
    System.out.println(m.replaceAll("---(${body})---"));
  }

  public String generateRegex() {
    var getRE = (Function) scope.get("getRE", scope);
    regex = (String) getRE.call(cx, scope, scope, new Object[]{noamGraph});
    return regex;
  }

  public String preProcessSimplifyRegex() {
    String patterns[][] = {
        {"(?(DEFINE)(?<brace>\\((?>[^()]|(?'brace'))*\\)))(?<braces>\\({20}(?<body>(?>[^()]|(?'braces')|(?'brace'))*)\\){20})", "(${body})"},
        {"(?(DEFINE)(?<brace>\\((?>[^()]|(?'brace'))*\\)))(?<braces>\\({10}(?<body>(?>[^()]|(?'braces')|(?'brace'))*)\\){10})", "(${body})"},
        {"(?(DEFINE)(?<brace>\\((?>[^()]|(?'brace'))*\\)))(?<braces>\\({5}(?<body>(?>[^()]|(?'braces')|(?'brace'))*)\\){5})", "(${body})"},
        {"(?(DEFINE)(?<brace>\\((?>[^()]|(?'brace'))*\\)))(?<braces>\\({2}(?<body>(?>[^()]|(?'braces')|(?'brace'))*)\\){2})", "(${body})"},
//        {"\\(([^\\[\\]\\(\\)]*)\\)([^\\*\\+])", "$1$2"},          //(a) => a
//        {"^((.*)\\({5})(.*)(\\){5}[^\\*\\+](.*))$", "$2$3$5"},          //(a) => a
        {"\\((.)\\)", "$1"},                                      //(a) => a
//        {"([^()\\[\\]]*\\*){2}", "($1)"},                       //a*a* => a*
//        {"(\\w*)\\((\\w*)\\)([\\(\\[])", "$1$2$3"},   //ab(cd) => abcd
    };
    int iter;
    for (iter = 0; iter < 4000; iter++) {
      boolean found = false;
      for (int i = 0; i < patterns.length; i++) {
        Matcher m = Pattern.compile(patterns[i][0]).matcher(regex);
        String regex2 = m.replaceAll(patterns[i][1]);
        if (!regex2.equals(regex)) {
          regex = regex2;
          found = true;
          break;
        }
      }
      if (!found) break;
    }
    System.out.println("finished preprocessing in " + iter + " iterations.");
    return regex;
  }

  public String simplifyRegex() {
    var simplify = (Function) scope.get("simplify", scope);
    regex = (String) simplify.call(cx, scope, scope, new Object[]{regex});
    return regex;
  }

  @Override
  public void close() {
    if (cx != null)
      Context.exit();
  }
}
