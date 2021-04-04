package il.ac.bgu.cs.bp.statespacemapper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RegularExpressionGeneratorTest {
  @Test
  void test2ParanthesesTo1() { //({2})=>()
    String[][] strings = { {"((a))", "(a)"}};
    for (int i = 0; i < strings.length; i++) {
      assertEquals(strings[i][1],RegularExpressionGenerator.simplify(strings[i][0], "({2})=>()"));
    }
  }
}