package il.ac.bgu.cs.bp.statespacemapper;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import java.io.*;

import com.florianingerl.util.regex.*;

import java.nio.charset.StandardCharsets;

public class RegularExpressionGenerator implements Closeable {
  private final static String definitions = "(?(DEFINE)" +
      "(?<brace>\\((?<body>(?>[^()]|(?'brace'))*)\\))" +
      "(?<![^(+])(?<element>(?'brace')|\\w+)" +
      "(?<element_star>(?'element')\\*)" +
      "(?<any_element>(?'element')|(?'element_star'))" +
      "(?<start_element>(?<![^(])(?'element'))" +
      "(?<start_element_or>(?<![^(+])(?'element'))" +
      "(?<end_element>(?'element')(?=$|\\)))" +
      "(?<or_sequence>(?>(?'any_element')\\+)+(?'any_element'))" +
      "(?<star_sequence>(?'element_star'){2,})" +
      ")";
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
//    Object[] p = {"(?<before>(?>(?'any_element')\\+)*)(?<first>(?'any_element'))\\+(?<after>(?>(?'any_element')\\+){0,}?)(?=\\k<first>[)+])", "b: ${before}--- a: ${after} --- f: ${first} 000: "/*new DefaultCaptureReplacer() {
    Object[] p = {"(?<![^(+])(?<before>(?>(?'any_element')\\+)*)(?<first>(?'any_element'))\\+(?<after>(?>(?'any_element')\\+){0,}?)\\k<first>(?![^)+])", "${before}${after}${first}"/*new DefaultCaptureReplacer() {
//    Object[] p = {"(?'start_element_or')", "--${start_element_or}"/*new DefaultCaptureReplacer() {
      @Override
      public String replace(CaptureTreeNode node) {
        if ("element_star".equals(node.getGroupName()))
          return super.replace(node)+"+";
        return super.replace(node);
      }
    }*/};
//    String s = "a$b$t(a$g)a";
    String s = "aa+z+c+s+a+(b+a)";
    Matcher m = Pattern.compile(definitions + p[0]).matcher(s);
    System.out.println("original: " + s);
    while (m.find()) {
      System.out.println("group: " + m.group());
//      System.out.println("body: " + m.group("body"));
    }
    String regex2;
    if (p[1] instanceof String)
      regex2 = m.replaceAll((String) p[1]);
    else
      regex2 = m.replaceAll((CaptureReplacer) p[1]);
    System.out.println(regex2);
  }

  public String generateRegex() {
    var getRE = (Function) scope.get("getRE", scope);
    regex = (String) getRE.call(cx, scope, scope, new Object[]{noamGraph});
    return regex;
  }

//  specials.ALT.toString = function() { return "+"; };
//      specials.KSTAR.toString = function() { return "*"; };
//      specials.LEFT_PAREN.toString = function() { return "("; };
//      specials.RIGHT_PAREN.toString = function() { return ")"; };
//      specials.EPS.toString = function() { return "$"; };
//      makeEps Returns a node representing the empty string regular expression.
//      LIT=literal


  public String preProcessSimplifyRegex() {
    Object[][] patterns = {
        // Remove redundant (((())))
        {"(?<braces>\\({20}(?<braces_body>(?>[^()]|(?'braces')|(?'brace'))*)\\){20})", "(${braces_body})"},
        {"(?<braces>\\({10}(?<braces_body>(?>[^()]|(?'braces')|(?'brace'))*)\\){10})", "(${braces_body})"},
        {"(?<braces>\\({5}(?<braces_body>(?>[^()]|(?'braces')|(?'brace'))*)\\){5})", "(${braces_body})"},
        {"(?<braces>\\({2}(?<braces_body>(?>[^()]|(?'braces')|(?'brace'))*)\\){2})", "(${braces_body})"},
        // End of remove redundant (((())))
        {"\\((?'element')\\(\\)\\)", "()"},                   // (a()) => ()
        {"\\(\\)\\*", "()"},                                  // ()* => ()
        {"\\((?'element')\\)", "${element}"},                 // (a) => a
        {"\\$\\*", "\\$"},                                    // $* => $
        {"\\((?'element')\\*\\)\\*", "${element}*"},          // (a*)* => a*
        {"\\((?=(?>(?'any_element')\\+)*(?'element_star'))(?'or_sequence')\\)\\*", new DefaultCaptureReplacer() {
          @Override
          public String replace(CaptureTreeNode node) {
            if ("element_star".equals(node.getGroupName()))
              return replace(node.getChildren().get(0));
            return super.replace(node);
          }
        }},                                                   // (a+b*)* => (a+b)*
        {"\\$\\+(?'element_star')", "${element_star}"},       // $+a* => a*
        {"(?<=\\()(?'star_sequence')(?=(?'element_star')\\)\\*)", new DefaultCaptureReplacer() {
          @Override
          public String replace(CaptureTreeNode node) {
            if ("element_star".equals(node.getGroupName()))
              return super.replace(node) + "+";
            return super.replace(node);
          }
        }},                                                   // (a*b*)* => (a*+b*)*
        {"\\$(?'element')", "${element}"},                    // $a => a
        {"(?<before>(?>(?'any_element')\\+)*)(?<first>(?'any_element'))\\+(?<after>(?>(?'any_element')\\+){0,}?)\\k<first>(?![^)+])",
            "${before}${after}${first}"},                     // a+a => a
        {"", ""},       // a+a* => a*
        {"", ""},       // a*a* => a*
        {"", ""},       // (aa+a)* => (a)*
        {"", ""},       // (a + $)* => (a)*
        {"", ""},       // (ab+ac) => a(b+c)
        {"", ""},       // a*aa* => aa*
        {"", ""},       // (ab+cb) => (a+c)b
        {"", ""},       // a*($+b(a+b)*) => (a+b)*
        {"", ""},       // ($+(a+b)*a)b* => (a+b)*
        {"", ""},       // ab(cd) => abcd
        {"", ""},       // (a+(b+c)) => a+b+c


        {"(?(DEFINE)(?<brace>\\((?>[^()]|(?'brace'))*\\))(?<kstar>(?>\\w|(?'brace'))(?<star>\\*)))" +
            "\\((?'kstar')+(?>\\w|(?'brace'))\\*\\)\\*", new DefaultCaptureReplacer() {
          @Override
          public String replace(CaptureTreeNode node) {
            if ("star".equals(node.getGroupName()))
              return "*+";
            return super.replace(node);
          }
        }},   //(a*b*)* => (a*+b*)*
        {"(?<estar>(?'element')\\*)\\k<estar>", "${estar}"},  // a*a* => a*
        {"(?(DEFINE)(?<brace>\\((?>[^()]|(?'brace'))*\\))(?<t>(?>(?'brace')|\\w)*))(?'t')(?<eps>\\$)(?'t')", new DefaultCaptureReplacer() {
          @Override
          public String replace(CaptureTreeNode node) {
            if ("eps".equals(node.getGroupName()))
              return "";
            return super.replace(node);
          }
        }},   // $a => a
        /*{"(?(DEFINE)(?<brace>\\((?>[^()]|(?'brace'))*\\))(?<t>(?>(?'brace')|\\w)*))(?'t')(?<eps>\\$)(?'t')", new DefaultCaptureReplacer() {
          @Override
          public String replace(CaptureTreeNode node) {
            if ("eps".equals(node.getGroupName()))
              return "";
            return super.replace(node);
          }
        }},   // a+a => a*/
//        {"(\\w*)\\((\\w*)\\)([\\(\\[])", "$1$2$3"},   // ab(cd) => abcd
    };
    int iter;
    for (iter = 0; iter < 4000; iter++) {
      boolean found = false;
      for (int i = 0; i < patterns.length; i++) {
        Matcher m = Pattern.compile(definitions + patterns[i][0]).matcher(regex);
        String regex2;
        if (patterns[i][1] instanceof String)
          regex2 = m.replaceAll((String) patterns[i][1]);
        else
          regex2 = m.replaceAll((CaptureReplacer) patterns[i][1]);
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
