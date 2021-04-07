package il.ac.bgu.cs.bp.statespacemapper;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class RegularExpressionGeneratorTest {
  private void testSimplify(String pattern, String[][] strings) {
    for (int i = 0; i < strings.length; i++) {
      assertEquals(strings[i][1], RegularExpressionGenerator.simplify(strings[i][0], pattern), "\nOriginal string was " + strings[i][0]);
    }
  }

  private void testDefinition(String string, String pattern, String replace, String desired) {
    assertEquals(desired, RegularExpressionGenerator.testPattern(string, pattern, replace),"\nOriginal string was " + string);
  }

  @Test
  void testDefinitionsElement() {
    String[][] element = {
        {"a*(bc+d)+e*+()+$", "(?'element')", "<${element}>", "<a>*<(bc+d)>+<e>*+<()>+<$>"},
        {"(a*(bc+d)+e*+())$f(gh*+i)", "(?'element')", "<${element}>", "<(a*(bc+d)+e*+())><$><f><(gh*+i)>"},
    };
    Arrays.stream(element).forEach(arr -> testDefinition(arr[0], arr[1], arr[2], arr[3]));
  }

  @Test
  void testDefinitionsElementStar() {
    String[][] element = {
        {"(a*(bc*+d))+(e*)*+()*", "(?'element_star')", "<${element_star}>", "(<a*>(b<c*>+d))+<(e*)*>+<()*>"},
        {"(a*(bc+d)+ae*+())fgh*", "(?'element_star')", "<${element_star}>", "(<a*>(bc+d)+a<e*>+())fg<h*>"},
    };
    Arrays.stream(element).forEach(arr -> testDefinition(arr[0], arr[1], arr[2], arr[3]));
  }

  @Test
  void testDefinitionsAnyElement() {
    String[][] element = {
        {"(a())", "(?'any_element')", "<${any_element}>", "<(a())>"},
        {"(a*(bc*+d))+(e*)*+()*", "(?'any_element')", "<${any_element}>", "<(a*(bc*+d))>+<(e*)*>+<()*>"},
        {"a*(bc+d)+ae*+()fgh*", "(?'any_element')", "<${any_element}>", "<a*><(bc+d)>+<a><e*>+<()><f><g><h*>"},
    };
    Arrays.stream(element).forEach(arr -> testDefinition(arr[0], arr[1], arr[2], arr[3]));
  }

  @Test
  void testDefinitionsStartAnyElement() {
    String[][] element = {
        {"(a*(bc*+d))+(e*)*+()*", "(?'start_any_element')", "<${start_any_element}>", "<(a*(bc*+d))>+(<e*>)*+()*"},
        {"a*(bc+d)+ae*+()fgh*", "(?'start_any_element')", "<${start_any_element}>", "<a*>(<b>c+d)+ae*+()fgh*"},
        {"a+b", "(?'start_any_element')", "<${start_any_element}>", "<a>+b"},
        {"ab", "(?'start_any_element')", "<${start_any_element}>", "<a>b"},
    };
    Arrays.stream(element).forEach(arr -> testDefinition(arr[0], arr[1], arr[2], arr[3]));
  }

  @Test
  void testDefinitionsStartAnyElementOr() {
    String[][] element = {
        {"(a*(bc*+d))+(e*)*+()*", "(?'end_any_element')", "<${end_any_element}>", "(a*<(bc*+d)>)+(<e*>)*+<()*>"},
        {"a*(bc+d)+ae*+()fgh*", "(?'end_any_element')", "<${end_any_element}>", "a*(bc+<d>)+ae*+()fg<h*>"},
        {"a+b", "(?'end_any_element')", "<${end_any_element}>", "a+<b>"},
        {"a+bc", "(?'end_any_element')", "<${end_any_element}>", "a+b<c>"},
    };
    Arrays.stream(element).forEach(arr -> testDefinition(arr[0], arr[1], arr[2], arr[3]));
  }

  @Test
  void testDefinitionsOrSequence() {
    String[][] element = {
        {"a+b", "(?'or_sequence')", "<${or_sequence}>", "<a+b>"},
        {"a+bc", "(?'or_sequence')", "<${or_sequence}>", "<a+bc>"},
        {"a*+bc", "(?'or_sequence')", "<${or_sequence}>", "<a*+bc>"},
        {"a", "(?'or_sequence')", "<${or_sequence}>", "a"},
        {"a*", "(?'or_sequence')", "<${or_sequence}>", "a*"},
        {"(a*)", "(?'or_sequence')", "<${or_sequence}>", "(a*)"},
        {"(a*(bc*+d))+(e*)+()*", "(?'or_sequence')", "<${or_sequence}>", "<(a*(bc*+d))+(e*)+()*>"},
        {"ab(c+d)e((f+g)h)", "(?'or_sequence')", "<${or_sequence}>", "ab(<c+d>)e((<f+g>)h)"},
    };
    Arrays.stream(element).forEach(arr -> testDefinition(arr[0], arr[1], arr[2], arr[3]));
  }

  @Test
  void testDefinitionsOrAnyElementSequence() {
    String[][] element = {
        {"a+b", "(?'or_any_element_sequence')", "<${or_any_element_sequence}>", "<a+b>"},
        {"a+bc", "(?'or_any_element_sequence')", "<${or_any_element_sequence}>", "a+bc"},
        {"a*+b", "(?'or_any_element_sequence')", "<${or_any_element_sequence}>", "<a*+b>"},
        {"a", "(?'or_any_element_sequence')", "<${or_any_element_sequence}>", "a"},
        {"a*", "(?'or_any_element_sequence')", "<${or_any_element_sequence}>", "a*"},
        {"(a*)", "(?'or_any_element_sequence')", "<${or_any_element_sequence}>", "(a*)"},
        {"(a*(bc*+d))+(e*)+()*", "(?'or_any_element_sequence')", "<${or_any_element_sequence}>", "<(a*(bc*+d))+(e*)+()*>"},
        {"ab(c+d)e((f+g)+(h+i))", "(?'or_any_element_sequence')", "<${or_any_element_sequence}>", "ab(<c+d>)e(<(f+g)+(h+i)>)"},
    };
    Arrays.stream(element).forEach(arr -> testDefinition(arr[0], arr[1], arr[2], arr[3]));
  }

  @Test
  void testDefinitionsAndSequence() {
    String[][] element = {
        {"a+b", "(?'and_sequence')", "<${and_sequence}>", "a+b"},
        {"a+bc", "(?'and_sequence')", "<${and_sequence}>", "a+<bc>"},
        {"a+bc*", "(?'and_sequence')", "<${and_sequence}>", "a+<bc*>"},
        {"a", "(?'and_sequence')", "<${and_sequence}>", "a"},
        {"a*", "(?'and_sequence')", "<${and_sequence}>", "a*"},
        {"(a*)", "(?'and_sequence')", "<${and_sequence}>", "(a*)"},
        {"(a*(bc*d))+(e*)+()*", "(?'and_sequence')", "<${and_sequence}>", "(<a*(bc*d)>)+(e*)+()*"},
        {"ab(c+d)e((f+g)h)+(abc)", "(?'and_sequence')", "<${and_sequence}>", "<ab(c+d)e((f+g)h)>+(<abc>)"},
    };
    Arrays.stream(element).forEach(arr -> testDefinition(arr[0], arr[1], arr[2], arr[3]));
  }

  @Test
  void testDefinitionsStarSequence() {
    String[][] element = {
        {"a", "(?'star_sequence')", "<${star_sequence}>", "a"},
        {"a*", "(?'star_sequence')", "<${star_sequence}>", "a*"},
        {"a+bc", "(?'star_sequence')", "<${star_sequence}>", "a+bc"},
        {"a*+bc", "(?'star_sequence')", "<${star_sequence}>", "a*+bc"},
        {"(a*)", "(?'star_sequence')", "<${star_sequence}>", "(a*)"},
        {"(a*)*", "(?'star_sequence')", "<${star_sequence}>", "(a*)*"},
        {"ab*", "(?'star_sequence')", "<${star_sequence}>", "ab*"},
        {"a*b", "(?'star_sequence')", "<${star_sequence}>", "a*b"},
        {"a*b*c", "(?'star_sequence')", "<${star_sequence}>", "a*b*c"},
        {"(a*b*)*", "(?'star_sequence')", "<${star_sequence}>", "(<a*b*>)*"},
        {"(a*(b*c*+d)*)+(e*())*", "(?'star_sequence')", "<${star_sequence}>", "(<a*(b*c*+d)*>)+(e*())*"},
    };
    Arrays.stream(element).forEach(arr -> testDefinition(arr[0], arr[1], arr[2], arr[3]));
  }

  @Test
  void testDefinitionsEndAnyElement() {
    String[][] element = {
        {"(a*(bc*+d))+(e*)*+()*", "(?'start_any_element_or')", "<${start_any_element_or}>", "<(a*(bc*+d))>+<(e*)*>+<()*>"},
        {"a*(bc+d)+ae*+()fgh*", "(?'start_any_element_or')", "<${start_any_element_or}>", "<a*>(<b>c+<d>)+<a>e*+<()>fgh*"},
        {"a+b", "(?'start_any_element_or')", "<${start_any_element_or}>", "<a>+<b>"},
    };
    Arrays.stream(element).forEach(arr -> testDefinition(arr[0], arr[1], arr[2], arr[3]));
  }

  @Test
  void test3() { //({2})=>()
    testSimplify("({2})=>()",
        new String[][]{
            {"(a)", "(a)"},
            {"((a))", "(a)"},
            {"(((a)))", "((a))"},
            {"((aa))", "(aa)"},
            {"((a*))", "(a*)"},
            {"((a*+b*))", "(a*+b*)"},
            {"((a*+b*(a*b*+c(a*b*+c))))", "(a*+b*(a*b*+c(a*b*+c)))"},
        });
  }

  @Test
  void test4() {
    testSimplify("a()=>()",
        new String[][]{
            {"(a())", "(())"},
            {"(()a)", "(())"},
            {"a()", "()"},
            {"()a", "()"},
            {"ab()cd", "()"},
            {"ab+()cd", "ab+()"},
            {"ab()+cd", "()+cd"},
            {"ab()+cd", "()+cd"},
            {"ab()(a+b)", "()"},
            {"ab+(ab())", "ab+(())"},
        });
  }

  @Test
  void test5() {
    testSimplify("()*=>()",
        new String[][]{
            {"()*", "()"},
            {"(a)*", "(a)*"},
            {"()", "()"},
        });
  }

  @Test
  void test6() {
    testSimplify("(a)*=>a*",
        new String[][]{
            {"()", "()"},
            {"(a)", "(a)"},
            {"(a)*", "a*"},
            {"b(a)*c", "ba*c"},
            {"(a*)", "(a*)"},
        });
  }

  @Test
  void test7() {
    testSimplify("(bc)d=>abcd",
        new String[][]{
            {"a(bc)d", "abcd"},
            {"(ab)cd", "abcd"},
            {"(ab)*cd", "(ab)*cd"},
            {"ab(cd)*", "ab(cd)*"},
            {"ab(cd)", "abcd"},
            {"(cd)", "cd"},
        });
  }

  @Test
  void test8() {
    testSimplify("$*=>$",
        new String[][]{
            {"aaa$*aa+$*+(a$*a($*b))($*)", "aaa$aa+$+(a$a($b))($)"},
        });
  }

  @Test
  void test9() {
    testSimplify("(a*)*=>a*",
        new String[][]{
            {"(a*)*", "a*"},
            {"(ab*)*", "(ab*)*"},
            {"(a*b*)*", "a*b*"},
            {"(a*b*)", "(a*b*)"},
            {"a*b*", "a*b*"},
            {"c+(a*b*)*+(a*(b*c*)*)*", "c+a*b*+a*(b*c*)*"},
        });
  }

  @Test
  void test10() {
//    testDefinition("(a+b*)*", "\\((?=(?>(?'element')\\+)*(?'element_star'))(?'or_any_element_sequence')\\)\\*", "()", "(a+b)*");
    testSimplify("(a+b*)*=>(a+b)*",
        new String[][]{
            {"(a*+b+c)*", "(a+b+c)*"},
            {"(a+b*+c)*", "(a+b+c)*"},
            {"(a+b+c*)*", "(a+b+c)*"},
            {"(a*+b*+c)*", "(a+b+c)*"},
            {"(a*+b*+c*)*", "(a+b+c)*"},
            {"(a*+b*+c)", "(a*+b*+c)"},
            {"(aa*+b*+c)*", "(aa*+b*+c)*"},
            {"a*+b*+(c*+d*+(e*+f)*)*", "a*+b*+(c+d+(e*+f))*"},
        });
  }

  @Test
  void test11() {
//    testDefinition("(a+b*)*", "\\((?=(?>(?'element')\\+)*(?'element_star'))(?'or_any_element_sequence')\\)\\*", "()", "(a+b)*");
    testSimplify("$+a*=>a*",
        new String[][]{
            {"$+a*", "a*"},
            {"a*+$", "a*"},
            {"$+a*+b", "a*+b"},
            {"a*+$+b", "a*+b"},
            {"b+$+a*", "b+a*"},
            {"b+a*+$", "b+a*"},
            {"(a*)+$", "(a*)+$"},
            {"a*+$a", "a*+$a"},
            {"a*+$()", "a*+$()"},
            {"(a*+$)", "(a*)"},
            {"()$+a*", "()$+a*"},
            {"a$+a*", "a$+a*"},
            {"($+a*)", "(a*)"},
            {"($()+a*)", "($()+a*)"},
            {"$+ab*", "$+ab*"},
            {"$+a*b", "$+a*b"},
            {"$+a*b+c", "$+a*b+c"},
        });
  }

  @Test
  void test12() {
//    testDefinition("(a+b*)*", "\\((?=(?>(?'element')\\+)*(?'element_star'))(?'or_any_element_sequence')\\)\\*", "()", "(a+b)*");
    testSimplify("(a*b*)*=>(a*+b*)*",
        new String[][]{
            {"(a*b*)*", "(a*+b*)*"},
            {"(a*b*)*c*", "(a*+b*)*c*"},
            {"(a*b*)*+c*", "(a*+b*)*+c*"},
            {"(a*b*)", "(a*b*)"},
            {"(a*b*b)*", "(a*b*b)*"},
            {"(a*bb*)*", "(a*bb*)*"},
            {"(aa*b*)*", "(aa*b*)*"},
            {"(a*ab*)*", "(a*ab*)*"},
            {"(a*b*c*)*", "(a*+b*+c*)*"},
            {"((a*b*c*)*d*)*", "((a*b*c*)*+d*)*"},
        });
  }

  @Test
  void test13() {
//    testDefinition("(a+b*)*", "\\((?=(?>(?'element')\\+)*(?'element_star'))(?'or_any_element_sequence')\\)\\*", "()", "(a+b)*");
    testSimplify("$a=>a",
        new String[][]{
            {"$a", "a"},
            {"a$", "a"},
            {"a$b", "ab"},
            {"$ab", "ab"},
            {"ab$", "ab"},
            {"ab$cd", "abcd"},
        });
  }

  @Test
  void test14() {
//    testDefinition("a+a", "(?<first>(?'start_any_element_or')(?'any_element')*)(?<middle>(\\+(?'any_element')+)*)\\+\\k<first>(?![^)+])", "|${first}|${middle}", "(a+b)*");
    testSimplify("a+a=>a",
        new String[][]{
            {"a+a", "a"},
            {"aa+a", "aa+a"},
            {"a+aa", "a+aa"},
            {"()a+a", "()a+a"},
            {"a()+a", "a()+a"},
            {"aa+aa", "aa"},
            {"c+aa+aa", "c+aa"},
            {"aa+aa+c", "aa+c"},
            {"aa+c+aa", "aa+c"},
            {"aa+cc+aa", "aa+cc"},
            {"dd+aa+cc+aac", "dd+aa+cc+aac"},
            {"dd+aa+cc+aa*", "dd+aa+cc+aa*"},
        });
  }

  @Test
  void test15() {
//    testDefinition("a+a*", "(?<first>(?<![^(+])(?'element'))(?<middle>(\\+(?'any_element')+)*)", "|${first}|${middle}", "(a+b)*");
    testSimplify("a+a*=>a*",
        new String[][]{
            {"a+a*", "a*"},
            {"(a+b)+(a+b)*", "(a+b)*"},
            {"aa*+a", "aa*+a"},
            {"c+a+a*", "c+a*"},
            {"a*+a*+c", "a*+a*+c"},
            {"aa+c+aa*", "aa+c+aa*"},
        });
  }

  @Test
  void test16() {
//    testDefinition("a*+a*+c", "(?<first>(?<![^(+])(?'element'))\\*(?<middle>(\\+(?'any_element')+)*)\\+\\k<first>(?![^)])", "|${first}|-${middle}-", "(a+b)*");
    testSimplify("a*+a=>a*",
        new String[][]{
            {"a*+a", "a*"},
            {"(a+b)*+(a+b)", "(a+b)*"},
            {"aa*+a", "aa*+a"},
            {"c+a*+a", "c+a*"},
            {"c+a+a*", "c+a+a*"},
            {"a*+a+c", "a*+c"},
            {"a*+a*+c", "a*+a*+c"},
            {"aa*+c+aa", "aa*+c+aa"},
        });
  }

  @Test
  void test17() {
//    testDefinition("a*+a*+c", "(?<first>(?<![^(+])(?'element'))\\*(?<middle>(\\+(?'any_element')+)*)\\+\\k<first>(?![^)])", "|${first}|-${middle}-", "(a+b)*");
    testSimplify("a*a*=>a*",
        new String[][]{
            {"a*a*", "a*"},
            {"a*b*a*", "a*b*a*"},
            {"(a+b)*(a+b)*", "(a+b)*"},
            {"aa*a*", "aa*"},
            {"a*ba*", "a*ba*"},
            {"aa*a*", "aa*"},
            {"aa*aa*", "aa*aa*"},
            {"a*a*a", "a*a"},
            {"ca*a*d", "ca*d"},
            {"aa*", "aa*"},
            {"a*a", "a*a"},
            {"a*aa", "a*aa"},
        });
  }

  @Test
  void test18() {
//    testDefinition("(aa+a)*", "(?>\\()(?=(?'or_sequence')\\)\\*)(?<before>((?'any_element')+\\+)*)(?<first>(?<![^(+])(?'any_element'))(?<middle>(\\k<first>)*(\\+(?'any_element')+)*)\\+(\\k<first>)+(?![^)+])", "B<${before}>F<${first}>M<${middle}>", "(a+b)*");
    testSimplify("(aaaa+aa)*=>(aa)*",
        new String[][]{
            {"(aa+a)*","(a)*"},
            {"(a+aa)*","(a+aa)*"},
            {"(aaa+aa)*","(aaa+aa)*"},
            {"(aa+aaa)*","(aa+aaa)*"},
            {"(aaaa+aa)*","(aa)*"},
            {"(aa+aaaa+aa)*","(aa+aa)*"},
            {"(a+aa+b)*","(a+aa+b)*"},
            {"(aa+a+b)*","(a+b)*"},
            {"(b+a+aa)*","(b+a+aa)*"},
            {"(b+aa+a)*","(b+a)*"},
            {"(a+b+aa)*","(a+b+aa)*"},
            {"(aa+b+a)*","(a+b)*"},
            {"(a*+b+a*a)*","(a*+b+a*a)*"},
            {"(a*a+b+a*)*","(a*a+b+a*)*"},
            {"(a*a*+b+a*)*","(a*+b)*"},
            {"(a*+b+a*a*)*","(a*+b+a*a*)*"},
        });
  }

  @Test
  void test19() {
//    testDefinition("(aa+a)*", "(?>\\()(?=(?'or_sequence')\\)\\*)(?<before>((?'any_element')+\\+)*)(?<first>(?<![^(+])(?'any_element'))(?<middle>(\\k<first>)*(\\+(?'any_element')+)*)\\+(\\k<first>)+(?![^)+])", "B<${before}>F<${first}>M<${middle}>", "(a+b)*");
    testSimplify("(aa+aaaa)*=>(aa)*",
        new String[][]{
            {"(aa+a)*","(aa+a)*"},
            {"(a+aa)*","(a)*"},
            {"(aaa+aa)*","(aaa+aa)*"},
            {"(aa+aaa)*","(aa+aaa)*"},
            {"(aaaa+aa)*","(aaaa+aa)*"},
            {"(aa+aaaa+aa)*","(aa+aaaa)*"},
            {"(a+aa+b)*","(a+b)*"},
            {"(aa+a+b)*","(aa+a+b)*"},
            {"(b+a+aa)*","(b+a)*"},
            {"(b+aa+a)*","(b+aa+a)*"},
            {"(a+b+aa)*","(a+b)*"},
            {"(aa+b+a)*","(aa+b+a)*"},
            {"(a*+b+a*a)*","(a*+b+a*a)*"},
            {"(a*a+b+a*)*","(a*a+b+a*)*"},
            {"(a*a*+b+a*)*","(a*a*+b+a*)*"},
            {"(a*+b+a*a*)*","(a*+b)*"},
        });
  }

  @Test
  void test20() {
//    testDefinition("a*+a*+c", "(?<first>(?<![^(+])(?'element'))\\*(?<middle>(\\+(?'any_element')+)*)\\+\\k<first>(?![^)])", "|${first}|-${middle}-", "(a+b)*");
    testSimplify("(ab+d+ac+a)=>(d+a(b+c+$))",
        new String[][]{
            {"ab+ac","a(b+c)"},
            {"a(b+c)+a(d+e)","a((b+c)+(d+e))"},
            {"(ab+ac)", "(a(b+c))"},
            {"(ab+ac+ad)", "(a(b+c+d))"},
            {"(ab+ac+a)", "(a(b+c+$))"},
            {"(a+ab+ac)", "(a($+b+c))"},
            {"(ba+ca+da)", "(ba+ca+da)"},
            {"(b+ac+ad)", "(b+a(c+d))"},
            {"(ab+c+ad)", "(c+a(b+d))"},
            {"(ab+ac+d)", "(a(b+c)+d)"},
        });
  }

  @Test
  void test21() {
//    testDefinition("a*+a*+c", "(?<first>(?<![^(+])(?'element'))\\*(?<middle>(\\+(?'any_element')+)*)\\+\\k<first>(?![^)])", "|${first}|-${middle}-", "(a+b)*");
    testSimplify("(ba+d+ca)=>(d+(b+c)a)",
        new String[][]{
            {"(ba+ca)", "((b+c)a)"},
            {"ba+ca", "(b+c)a"},
            {"(ba+ca+da)", "((b+c+d)a)"},
            {"(ba+ca+a)", "((b+c+$)a)"},
            {"(a+ba+ca)", "(($+b+c)a)"},
            {"a+ba+ca", "($+b+c)a"},
            {"c+a+ba+ca+b", "c+($+b+c)a+b"},
            {"(ab+ac+ad)", "(ab+ac+ad)"},
            {"(ab+ac+ad)", "(ab+ac+ad)"},
            {"(b+ca+da)", "(b+(c+d)a)"},
            {"(ba+c+da)", "(c+(b+d)a)"},
            {"ba+c+da", "c+(b+d)a"},
            {"(ba+ca+d)", "((b+c)a+d)"},
            {"ba+ca+d", "(b+c)a+d"},
        });
  }

  @Test
  void test22() {
//    testDefinition("(aa+a)*", "(?>\\()(?=(?'or_sequence')\\)\\*)(?<before>((?'any_element')+\\+)*)(?<first>(?<![^(+])(?'any_element'))(?<middle>(\\k<first>)*(\\+(?'any_element')+)*)\\+(\\k<first>)+(?![^)+])", "B<${before}>F<${first}>M<${middle}>", "(a+b)*");
    testSimplify("(a+$)*=>(a)*",
        new String[][]{
            {"(a+$)*","(a)*"},
            {"($+a)*","(a)*"},
            {"(a+$+b)*","(a+b)*"},
        });
  }


  @Test
  void test23() {
//    testDefinition("(aa+a)*", "(?>\\()(?=(?'or_sequence')\\)\\*)(?<before>((?'any_element')+\\+)*)(?<first>(?<![^(+])(?'any_element'))(?<middle>(\\k<first>)*(\\+(?'any_element')+)*)\\+(\\k<first>)+(?![^)+])", "B<${before}>F<${first}>M<${middle}>", "(a+b)*");
    testSimplify("a*aa*=>aa*",
        new String[][]{
            {"a*aa*","aa*"},
            {"aa*a*","aa*"},
            {"a*b*aa*","a*b*aa*"},
        });
  }

  @Test
  void test25() {
//    testDefinition("a*+a*+c", "(?<first>(?<![^(+])(?'element'))\\*(?<middle>(\\+(?'any_element')+)*)\\+\\k<first>(?![^)])", "|${first}|-${middle}-", "(a+b)*");
    testSimplify("a+(b+c)=>a+b+c",
        new String[][]{
            {"a+(b+c)", "a+b+c"},
            {"(a+(b+c))d", "(a+b+c)d"},
            {"(a+(b+cd))z", "(a+b+cd)z"},
            {"(a+b)+c", "a+b+c"},
            {"z((a+b)+c)", "z(a+b+c)"},
            {"((ab+c)+d)z", "(ab+c+d)z"},
            {"a+(b+c)+d", "a+b+c+d"},
            {"a+((b+c)+d)", "a+(b+c)+d"},
        });
  }
}