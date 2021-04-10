package il.ac.bgu.cs.bp.statespacemapper;

import org.junit.jupiter.api.Test;
import org.svvrl.goal.core.UnsupportedException;
import org.svvrl.goal.core.aut.fsa.FSA;
import org.svvrl.goal.core.io.CodecException;
import org.svvrl.goal.core.logic.ParseException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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

  @Test
  void testAutomatonLoading() throws IOException, CodecException {
    String vault = String.join("\n", Files.readAllLines(Path.of("graphs", "vault.gff")));
    FSA automaton = GoalTool.string2automaton(vault);
    System.out.println("File automaton: " + vault);
    System.out.println("Parsed automaton: " + automaton.toString());
  }
}