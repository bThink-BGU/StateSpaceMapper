package il.ac.bgu.cs.bp.statespacemapper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RegularExpressionGeneratorTest {
  private void test(String pattern, String[][] strings) {
    for (int i = 0; i < strings.length; i++) {
      assertEquals(strings[i][1], RegularExpressionGenerator.simplify(strings[i][0], pattern));
    }
  }

  @Test
  void testDefinitionsElement() {
    String[][] element = {
        {"a*(bc+d)+e*+()","(?'element')","<${element}>","<a>*<(bc+d)>+<e>*+<()>"},
        {"(a*(bc+d)+e*+())f(gh*+i)","(?'element')","<${element}>","<(a*(bc+d)+e*+())><f><(gh*+i)>"},
    };
    for (int i = 0; i < element.length; i++) {
      assertEquals(element[i][3], RegularExpressionGenerator.testPattern(element[i][0], element[i][1], element[i][2]));
    }
  }

  @Test
  void test3() { //({2})=>()
    test("({2})=>()",
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
    test("(a()) => ()",
        new String[][]{
            {"(a())", "()"},
            {"(()a)", "()"},
            {"a()", "()"},
            {"()a", "()"},
            {"ab()cd", "()"},
            {"ab+()cd", "ab+()"},
            {"ab()+cd", "()+cd"},
            {"ab()+cd", "()+cd"},
            {"ab()(a+b)", "()+cd"},
        });
  }
}