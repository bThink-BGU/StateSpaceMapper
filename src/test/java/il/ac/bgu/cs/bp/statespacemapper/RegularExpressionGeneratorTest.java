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
        {"a*(bc+d)+e*+()", "(?'element')", "<${element}>", "<a>*<(bc+d)>+<e>*+<()>"},
        {"(a*(bc+d)+e*+())f(gh*+i)", "(?'element')", "<${element}>", "<(a*(bc+d)+e*+())><f><(gh*+i)>"},
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
            {"((a))", "(a)"},
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
    testSimplify("(a)=>a",
        new String[][]{
            {"()", "()"},
            {"(a)", "a"},
            {"(a*)", "(a*)"},
        });
  }

  @Test
  void test7() {
    testSimplify("(a*)=>a*",
        new String[][]{
            {"()", "()"},
            {"(a)", "(a)"},
            {"(a*)", "a*"},
            {"(a*)*", "(a*)*"},
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
}