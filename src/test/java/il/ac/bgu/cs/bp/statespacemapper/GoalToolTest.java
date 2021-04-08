package il.ac.bgu.cs.bp.statespacemapper;

import org.junit.jupiter.api.Test;
import org.svvrl.goal.core.UnsupportedException;
import org.svvrl.goal.core.logic.ParseException;

import static org.junit.jupiter.api.Assertions.*;

class GoalToolTest {

  @Test
  void simplifyGoalRegex() throws ParseException, UnsupportedException {
    assertEquals("a+", GoalTool.simplifyGoalRegex("a a*"));
    assertEquals("a+", GoalTool.simplifyGoalRegex("a* a"));
    var result = GoalTool.compareAutomata(GoalTool.re2fsa("a*a"), GoalTool.re2fsa("aa*"));
    assertEquals(true, result.isEquivalent());
  }
}