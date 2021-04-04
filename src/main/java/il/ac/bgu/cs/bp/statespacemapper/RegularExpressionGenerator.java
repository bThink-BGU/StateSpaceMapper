package il.ac.bgu.cs.bp.statespacemapper;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import java.io.*;

import com.florianingerl.util.regex.*;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class RegularExpressionGenerator implements Closeable {
  private final static String definitions = "(?(DEFINE)" +
      "(?<brace>\\((?<body>(?>[^()]|(?'brace'))*)\\))" +
      "(?<element>(?'brace')|\\w)" +
      "(?<element_star>(?'element')\\*)" +
      "(?<any_element>(?'element')|(?'element_star'))" +
      "(?<start_element>(?<![^(])(?'element'))" +
      "(?<start_element_or>(?<![^(+])(?'element'))" +
      "(?<end_element>(?'element')(?=$|\\)))" +
      "(?<or_sequence>(?>(?'any_element')\\+)+(?'any_element'))" +
      "(?<star_sequence>(?'element_star'){2,})" +
      ")";
  private final static REPattern[] patterns = {
      new REPattern(0, "({20})=>()", "(?<braces>\\({20}(?<braces_body>(?>[^()]|(?'braces')|(?'brace'))*)\\){20})", "(${braces_body})"),
      new REPattern(1, "({10})=>()", "(?<braces>\\({10}(?<braces_body>(?>[^()]|(?'braces')|(?'brace'))*)\\){10})", "(${braces_body})"),
      new REPattern(2, "({5})=>()", "(?<braces>\\({5}(?<braces_body>(?>[^()]|(?'braces')|(?'brace'))*)\\){5})", "(${braces_body})"),
      new REPattern(3, "({2})=>()", "(?<braces>\\({2}(?<braces_body>(?>[^()]|(?'braces')|(?'brace'))*)\\){2})", "(${braces_body})"),
      new REPattern(4, "(a()) => ()", "\\(( ? 'element')\\(\\)\\)", " () "),
      new REPattern(5, "()* => ()", "\\(\\)\\*", "()"),
      new REPattern(6, "(a) => a", "\\((?'element')\\)", "${element}"),
      new REPattern(7, "$* => $", "\\$\\*", "\\$"),
      new REPattern(8, "(a*)* => a*", "\\((?'element')\\*\\)\\*", "${element}*"),
      new REPattern(9, "(a+b*)* => (a+b)*", "\\((?=(?>(?'any_element')\\+)*(?'element_star'))(?'or_sequence')\\)\\*", new DefaultCaptureReplacer() {
        @Override
        public String replace(CaptureTreeNode node) {
          if ("element_star".equals(node.getGroupName()))
            return replace(node.getChildren().get(0));
          return super.replace(node);
        }
      }),
      new REPattern(10, "$+a* => a*", "\\$\\+(?'element_star')", "${element_star}"),
      new REPattern(11, "(a*b*)* => (a*+b*)*", "(?<=\\()(?'star_sequence')(?=(?'element_star')\\)\\*)", new DefaultCaptureReplacer() {
        @Override
        public String replace(CaptureTreeNode node) {
          if ("element_star".equals(node.getGroupName()))
            return super.replace(node) + "+";
          return super.replace(node);
        }
      }),
      new REPattern(12, "$a => a", "\\$(?'element')", "${element}"),
      new REPattern(13, "a+a => a", "(?<before>(?>(?'any_element')\\+)*)(?<first>(?'any_element'))\\+(?<after>(?>(?'any_element')\\+){0,}?)\\k<first>(?![^)+])",
          "${before}${after}${first}"),                     //
      new REPattern(14, "", "", ""),       // a+a* => a*
      new REPattern(15, "", "", ""),       // a*a* => a*
      new REPattern(16, "", "", ""),       // (aa+a)* => (a)*
      new REPattern(17, "", "", ""),       // (a + $)* => (a)*
      new REPattern(18, "", "", ""),       // (ab+ac) => a(b+c)
      new REPattern(19, "", "", ""),       // a*aa* => aa*
      new REPattern(20, "", "", ""),       // (ab+cb) => (a+c)b
      new REPattern(21, "", "", ""),       // a*($+b(a+b)*) => (a+b)*
      new REPattern(22, "", "", ""),       // ($+(a+b)*a)b* => (a+b)*
      new REPattern(23, "", "", ""),       // ab(cd) => abcd
      new REPattern(24, "", "", ""),       // (a+(b+c)) => a+b+c
  };
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
    regex = (String) getRE.call(cx, scope, scope, new Object[]{noamGraph});
    return regex;
  }

  public String preProcessSimplifyRegex() {
    regex = RegularExpressionGenerator.preProcessSimplifyRegex(regex);
    return regex;
  }

  public static String simplify(String regex, String pattern) {
    return simplify(regex, Arrays.stream(patterns).filter(p->p.name.equals(pattern)).findFirst().orElseThrow());
  }

  static String testPattern(String regex, String pattern, String replace) {
    Matcher m = Pattern.compile(definitions + pattern).matcher(regex);
    return m.replaceAll(replace);
  }

  public static String simplify(String regex, REPattern pattern) {
    Matcher m = Pattern.compile(definitions + pattern.pattern).matcher(regex);
    String regex2;
    if (pattern.replace instanceof String)
      regex2 = m.replaceAll((String) pattern.replace);
    else
      regex2 = m.replaceAll((CaptureReplacer) pattern.replace);
    return regex2;
  }

  public static String preProcessSimplifyRegex(String regex) {
    int iter;
    for (iter = 0; iter < 4000; iter++) {
      boolean found = false;
      for (int i = 0; i < patterns.length; i++) {
        String regex2 = simplify(regex, patterns[i]);
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


  static class REPattern {
    public final int id;
    public final String name;
    public final String pattern;
    public final Object replace;

    REPattern(int id, String name, String pattern, String replace) {
      this.id = id;
      this.name = name;
      this.pattern = pattern;
      this.replace = replace;
    }

    REPattern(int id, String name, String pattern, DefaultCaptureReplacer replace) {
      this.id = id;
      this.name = name;
      this.pattern = pattern;
      this.replace = replace;
    }
  }
}