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

public class RegularExpressionGenerator implements Closeable {
  private final static String definitions = "(?(DEFINE)" +
      "(?<brace>\\((?<body>(?>[^()]|(?'brace'))*)\\))" +
      "(?<element>(?'brace')|[\\w$])" +
      "(?<element_star>(?'element')\\*)" +
      "(?<any_element>(?'element_star')|(?'element'))" +
      "(?<start_any_element>(?<![^(])(?'any_element'))" +
      "(?<start_any_element_or>(?<![^(+])(?'any_element'))" +
      "(?<end_any_element>(?'any_element')(?=$|\\)))" +
      "(?<or_sequence>(?>(?'any_element')+\\+)+(?'any_element')+)" +
      "(?<or_any_element_sequence>(?>(?'any_element')\\+)+(?'any_element')(?!(?'any_element')))" +
      "(?<and_sequence>(?'any_element'){2,})" +
      "(?<star_sequence>(?<![\\w$)])(?'element_star'){2,}(?!(?'element')))" +
      ")";
  private final static REPattern[] patterns = {
      new REPattern(0, "({20})=>()", "(?<braces>\\({20}(?<braces_body>(?>[^()]|(?'braces')|(?'brace'))*)\\){20})", "(${braces_body})"),
      new REPattern(1, "({10})=>()", "(?<braces>\\({10}(?<braces_body>(?>[^()]|(?'braces')|(?'brace'))*)\\){10})", "(${braces_body})"),
      new REPattern(2, "({5})=>()", "(?<braces>\\({5}(?<braces_body>(?>[^()]|(?'braces')|(?'brace'))*)\\){5})", "(${braces_body})"),
      new REPattern(3, "({2})=>()", "(?<braces>\\({2}(?<braces_body>(?>[^()]|(?'braces')|(?'brace'))*)\\){2})", "(${braces_body})"),
      new REPattern(4, "a()=>()", "(?=(?'and_sequence'))(?'any_element')*\\(\\)(?'any_element')*", "()"),
      new REPattern(5, "()*=>()", "\\(\\)\\*", "()"),
      new REPattern(6, "(a)*=>a*", "\\((?'element')\\)\\*", "${element}*"),
      new REPattern(7, "(bc)d=>abcd", "\\((?<seq>(?'any_element')+)\\)(?!\\*)", "${seq}"),
      new REPattern(8, "$*=>$", "\\$\\*", "\\$"),
      new REPattern(9, "(a*)*=>a*", "\\((?<seq>(?'element_star')+)\\)\\*", "${seq}"),
      new REPattern(10, "(a+b*)*=>(a+b)*", "(?>\\()(?=(?'or_any_element_sequence')\\))(?>(?'element')\\+)*(?'element_star')(\\+(?'any_element'))*(?>\\)\\*)", new DefaultCaptureReplacer() {
        @Override
        public String replace(CaptureTreeNode node) {
          if ("element_star".equals(node.getGroupName()))
            return replace(node.getChildren().get(0));
          return super.replace(node);
        }
      }),
      new REPattern(11, "$+a*=>a*", "(?<![\\w$)])(?>\\$\\+(?'element_star')(?!(?'any_element')))|(?>(?<![^(+])(?'element_star')\\+\\$(?!(?'any_element')))", "${element_star}"),
      new REPattern(12, "(a*b*)*=>(a*+b*)*", "(?>\\()(?'element_star')+(?=(?'element_star')\\)\\*)", new DefaultCaptureReplacer() {
        @Override
        public String replace(CaptureTreeNode node) {
          if ("element_star".equals(node.getGroupName()))
            return super.replace(node) + "+";
          return super.replace(node);
        }
      }),
      new REPattern(13, "$a=>a", "(?>\\$(?'any_element'))|(?>(?'any_element')\\$)", "${any_element}"),
      new REPattern(14, "a+a=>a", "(?<first>(?'start_any_element_or')(?'any_element')*)(?<middle>(\\+(?'any_element')+)*)\\+\\k<first>(?![^)+])", "${first}${middle}"),
      new REPattern(15, "a+a*=>a*", "(?<first>(?<![^(+])(?'element'))(?<middle>(\\+(?'any_element')+)*)\\+\\k<first>\\*(?![^)+])", "${first}*${middle}"),
      new REPattern(16, "a*+a=>a*", "(?<first>(?<![^(+])(?'element'))\\*(?<middle>(\\+(?'any_element')+)*)\\+\\k<first>(?![^)+])", "${first}*${middle}"),
      new REPattern(17, "a*a*=>a*", "(?'element_star')\\k<element_star>", "${element_star}"),
      new REPattern(18, "(aaaa+aa)*=>(aa)*", "(?>\\()(?=(?'or_sequence')\\)\\*)(?<before>((?'any_element')+\\+)*)(?<first>(?<![^(+])(?'any_element')+)\\k<first>+(?<middle>(\\+(?'any_element')+)*)\\+\\k<first>(?![^)+])", "(${before}${first}${middle}"),
      new REPattern(19, "(aa+aaaa)*=>(aa)*", "(?>\\()(?=(?'or_sequence')\\)\\*)(?<before>((?'any_element')+\\+)*)(?<first>(?<![^(+])(?'any_element')+)(?<middle>(\\+(?'any_element')+)*)\\+\\k<first>+(?![^)+])", "(${before}${first}${middle}"),
      new REPattern(20, "(ab+d+ac+a)=>(d+a(b+c+$))", "(?<![^(+])(?<start>(?'any_element')+)(?<as>(?'any_element')*)(?<middle>(?>\\+(?'any_element')*)*?)((?<repeat>\\+\\k<start>)(?<ar>(?'any_element')*))+(?![^+)])(?<end>)", new DefaultCaptureReplacer() {
        private ArrayList<String> matches;
        private String start;
        private boolean hasMiddle;

        @Override
        public String replace(CaptureTreeNode node) {
          if (node.getGroupNumber() == 0) {
            matches = new ArrayList<>();
            hasMiddle = false;
          }
          if ("end".equals(node.getGroupName())) {
            return super.replace(node) + (hasMiddle ? "+" : "") + this.start + "(" + String.join("+", matches) + ")";
          }
          if (Arrays.asList("as", "ar").contains(node.getGroupName())) {
            String capture = node.getCapture().getValue();
            if (capture.length() == 0) capture = "$";
            matches.add(capture);
            return "";
          }
          if ("start".equals(node.getGroupName())) {
            this.start = node.getCapture().getValue();
            return "";
          }
          if ("repeat".equals(node.getGroupName())) {
            return "";
          }
          if ("middle".equals(node.getGroupName())) {
            String capture = super.replace(node);
            if (capture.length() > 1) {
              hasMiddle = true;
              capture = capture.substring(1);
            }
            return capture;
          }
          return super.replace(node);
        }
      }),
      new REPattern(21, "(ba+d+ca)=>(d+(b+c)a)", "(?<![^+(])(?<bs>(?'any_element')*)(?<start>(?'any_element')+)(?<middle>(?>\\+(?'any_element')*)*?)((?<drop>\\+)(?<br>(?'any_element')*)(?<repeat>\\k<start>))+(?![^+)])(?<end>)", new DefaultCaptureReplacer() {
        private ArrayList<String> matches;
        private String start;
        private boolean hasMiddle;

        @Override
        public String replace(CaptureTreeNode node) {
          if (node.getGroupNumber() == 0) {
            matches = new ArrayList<>();
            hasMiddle = false;
          }
          if ("end".equals(node.getGroupName())) {
            return super.replace(node) + (hasMiddle ? "+" : "") + "(" + String.join("+", matches) + ")" + this.start;
          }
          if (Arrays.asList("bs", "br").contains(node.getGroupName())) {
            String capture = node.getCapture().getValue();
            if (capture.length() == 0) capture = "$";
            matches.add(capture);
            return "";
          }
          if ("start".equals(node.getGroupName())) {
            this.start = node.getCapture().getValue();
            return "";
          }
          if (Arrays.asList("repeat","drop").contains(node.getGroupName())) {
            return "";
          }
          if ("middle".equals(node.getGroupName())) {
            String capture = super.replace(node);
            if (capture.length() > 1) {
              hasMiddle = true;
              capture = capture.substring(1);
            }
            return capture;
          }
          return super.replace(node);
        }
      }),
      new REPattern(22, "(a+$)*=>(a)*", "(?>\\()(?=(?'or_sequence')\\)\\*)((?<before>\\$\\+)|((?>(?'any_element')+\\+)*(?'any_element')+(?<after>\\+\\$)(?![^+)])))", new DefaultCaptureReplacer() {
        @Override
        public String replace(CaptureTreeNode node) {
          if (Arrays.asList("before", "after").contains(node.getGroupName())) {
            return "";
          }
          return super.replace(node);
        }
      }),
      new REPattern(23, "a*aa*=>aa*", "(?<first>(?'element'))\\*(?<second>\\k<first>+)\\*", "${second}*"),
/*
      new REPattern(23, "a*($+b(a+b)*)=>(a+b)*", "", ""),
      new REPattern(24, "($+(a+b)*a)b*=>(a+b)*", "", ""),*/
      new REPattern(25, "a+(b+c)=>a+b+c", "(?<![^+(])\\((?'or_sequence')\\)(?![^+)])", "${or_sequence}"),

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
    return simplify(regex, Arrays.stream(patterns).filter(p -> p.name.equals(pattern)).findFirst().orElseThrow(() -> {
      throw new RuntimeException("No such pattern: " + pattern);
    }));
  }

  static String testPattern(String searchString, String pattern, String replace) {
    Matcher m = Pattern.compile(definitions + pattern).matcher(searchString);
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