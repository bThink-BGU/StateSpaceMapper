package il.ac.bgu.cs.bp.statespacemapper;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.svvrl.goal.core.UnsupportedException;
import org.svvrl.goal.core.io.CodecException;
import org.svvrl.goal.core.logic.ParseException;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;


public class MoodleTester {
  private static Path pathOriginal = Paths.get("graphs/moodle.re");
  private static Path pathSimplified = Paths.get("graphs/moodle-simplified.re");
  private static Path pathSimplifiedGOAL = Paths.get("graphs/moodle-simplified-goal.re");
  private static String moodleOriginal;

  @BeforeAll
  static void loadMoodle() throws IOException {
    moodleOriginal = Files.readAllLines(pathOriginal).get(0);
  }

  private static void write(String simplified) throws IOException {
    Files.write(pathSimplified, simplified.getBytes());
    String goal = GoalTool.noam2goalRegexFormat(simplified);
    Files.write(pathSimplifiedGOAL, goal.getBytes());
  }

  private String simplify(String origin, String pattern) {
    String simplified = origin;
    int i;
    for (i = 0; i < 40; i++) {
      String simplified2 = RegularExpressionGenerator.simplify(simplified, pattern);
      if (simplified.equals(simplified2)) break;
      simplified = simplified2;
    }
    System.out.println("finished pattern <" + pattern + "> in " + i + " iterations");
    return simplified;
  }

  @Test
  void testCertainPattern() throws IOException {
    String simplified = moodleOriginal;
    simplified = simplify(simplified, "({20})=>()");
    simplified = simplify(simplified, "({10})=>()");
    simplified = simplify(simplified, "({5})=>()");
    simplified = simplify(simplified, "({2})=>()");
    write(simplified);
  }

  @Test
  void testSimplifyWithGoal() throws IOException, ParseException, UnsupportedException {
    String goal = GoalTool.noam2goalRegexFormat(moodleOriginal);
    var simplified = GoalTool.simplifyGoalRegex(goal);
    write(simplified);
  }

  @Test
  void testSimplifyAll() throws IOException {
//    pathSimplified = Paths.get("graphs/moodle-all-simplified.re");
//    pathSimplifiedGOAL = Paths.get("graphs/moodle-all-simplified-goal.re");
    String simplified = RegularExpressionGenerator.preProcessSimplifyRegex(moodleOriginal);
    write(simplified);
  }

  @Test
  void testEquals() throws IOException, ParseException, UnsupportedException, CodecException {
    String goal = GoalTool.noam2goalRegexFormat(moodleOriginal);
    var goalSimplified = GoalTool.simplifyGoalRegex(goal);
    Files.write(Paths.get("graphs/moodle-all-simplified-goal.re"), goalSimplified.getBytes());
    String mineSimplified = RegularExpressionGenerator.preProcessSimplifyRegex(moodleOriginal);
    Files.write(Paths.get("graphs/moodle-all-simplified-mine.re"), mineSimplified.getBytes());
    String mineInGoal = GoalTool.noam2goalRegexFormat(mineSimplified);

    var autGoal = GoalTool.re2fsa(goalSimplified);
    var autMine = GoalTool.re2fsa(mineInGoal);
    var equality = GoalTool.compareAutomata(autGoal, autMine);
    assertTrue(equality.isEquivalent(),
        MessageFormat.format("counter 1: {0}\n\ncounter 2: {1}",
            equality.isContained1() ? "" : equality.getCounterexample1().toString(),
            equality.isContained2() ? "" : equality.getCounterexample2().toString()
        )
    );
  }
}
