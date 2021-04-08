package il.ac.bgu.cs.bp.statespacemapper;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.svvrl.goal.core.UnsupportedException;
import org.svvrl.goal.core.logic.LogicSimplifier;
import org.svvrl.goal.core.logic.ParseException;
import org.svvrl.goal.core.logic.re.REParser;
import org.svvrl.goal.core.logic.re.RESimplifier;
import org.svvrl.goal.core.logic.re.RegularExpression;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


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
    String goal = simplified.replace('e', 'z').replace('$', 'e');
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
    System.out.println("finished pattern <"+pattern+"> in " + i + " iterations");
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
    String goal = moodleOriginal.replace('e', 'z').replace('$', 'e').replaceAll("\\(\\)","E");
    var re = new REParser().parse(goal);
    var simplified = ((RegularExpression)LogicSimplifier.simplify(re)).toString();
    write(simplified);
  }

  @Test
  void testSimplifyAll() throws IOException {
    pathSimplified = Paths.get("graphs/moodle-all-simplified.re");
    pathSimplifiedGOAL = Paths.get("graphs/moodle-all-simplified-goal.re");
    String simplified = RegularExpressionGenerator.preProcessSimplifyRegex(moodleOriginal);
    write(simplified);
  }
}
