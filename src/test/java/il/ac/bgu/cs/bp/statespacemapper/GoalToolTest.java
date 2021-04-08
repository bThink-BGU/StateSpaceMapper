package il.ac.bgu.cs.bp.statespacemapper;

import org.junit.jupiter.api.Test;
import org.svvrl.goal.core.UnsupportedException;
import org.svvrl.goal.core.logic.ParseException;

import static org.junit.jupiter.api.Assertions.*;

class GoalToolTest {

  @Test
  void goal() throws ParseException, UnsupportedException {
    assertEquals("a+", GoalTool.simplifyGoalRegex("a a*"));
    assertEquals("a+", GoalTool.simplifyGoalRegex("a* a"));

    var result = GoalTool.compareAutomata(GoalTool.re2fsa("a* a"), GoalTool.re2fsa("a a*"));
    assertEquals(true, result.isEquivalent());

    assertEquals("(a a* a) g (a b) | b e E", GoalTool.noam2goalRegexFormat("(aa*a)g(ab)+b$()"), "Original string was: (aa*a)+b$()");
  }
}