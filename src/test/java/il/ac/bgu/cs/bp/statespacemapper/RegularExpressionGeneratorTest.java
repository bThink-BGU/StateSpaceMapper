package il.ac.bgu.cs.bp.statespacemapper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RegularExpressionGeneratorTest {
  private void test(String pattern, String[][] strings) {
    for (int i = 0; i < strings.length; i++) {
      assertEquals(strings[i][1],RegularExpressionGenerator.simplify(strings[i][0], pattern));
    }
  }

  @Test
  void test3() { //({2})=>()
    String[][] strings = {
        {"((a))", "(a)"},
        {"((aa))", "(aa)"},
        {"((a*))", "(a*)"},
        {"((a*+b*))", "(a*+b*)"},
        {"((a*+b*(a*b*+c(a*b*+c))))", "(a*+b*(a*b*+c(a*b*+c)))"},
    };
    test("({2})=>()", strings);
  }
}